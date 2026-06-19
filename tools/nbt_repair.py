"""Analisa e REPARA um level.dat (ou qualquer NBT gzip) com aninhamento patologico.

- analyze: acha a profundidade maxima e o CAMINHO de nomes ate ela (qual tag explodiu).
- --prune N: reescreve o arquivo podando qualquer container mais fundo que N niveis
  (o container vira vazio), preservando todo o resto. Faz backup .bak antes.

Leitura é ITERATIVA p/ medir profundidade (nao estoura a pilha do Python).
A reconstrucao usa recursao limitada ao CAP (seguro).

Uso:
  python tools/nbt_repair.py analyze "<arquivo>"
  python tools/nbt_repair.py "<arquivo_corrompido>" --prune 16 --out "<level.dat>"
"""
import argparse
import gzip
import struct
import sys
import zlib
from pathlib import Path

SCALAR = {1: 1, 2: 2, 3: 4, 4: 8, 5: 4, 6: 8}
TYPE_NAME = {0: "End", 1: "Byte", 2: "Short", 3: "Int", 4: "Long", 5: "Float",
             6: "Double", 7: "ByteArray", 8: "String", 9: "List", 10: "Compound",
             11: "IntArray", 12: "LongArray"}


def read_raw(path):
    data = Path(path).read_bytes()
    if data[:2] != b"\x1f\x8b":
        return data
    try:
        return gzip.decompress(data)
    except (EOFError, OSError, zlib.error):
        # gzip TRUNCADO (crash no meio do save): recupera o que descomprimir
        d = zlib.decompressobj(zlib.MAX_WBITS | 16)
        out = bytearray()
        try:
            out += d.decompress(data)
            out += d.flush()
        except zlib.error:
            pass
        return bytes(out)


class Cur:
    def __init__(self, buf):
        self.b = buf
        self.p = 0
        self.n = len(buf)

    def u8(self):
        v = self.b[self.p]; self.p += 1; return v

    def u16(self):
        v = struct.unpack_from(">H", self.b, self.p)[0]; self.p += 2; return v

    def i32(self):
        v = struct.unpack_from(">i", self.b, self.p)[0]; self.p += 4; return v

    def take(self, k):
        v = self.b[self.p:self.p + k]; self.p += k; return v


# ── medir profundidade + caminho (ITERATIVO) ────────────────────────────────

def analyze(buf):
    c = Cur(buf)
    if c.n == 0 or c.b[0] != 10:
        return 0, [], {}
    c.u8(); c.u16()  # root type + name
    # stack de (kind, names...) — guardamos o nome no push
    stack = [("C", "<root>")]
    names = ["<root>"]
    best = 1
    best_path = list(names)
    top_depth = {}  # primeira chave sob <root> -> profundidade max vista nela
    cur_top = None

    def scalar_skip(t):
        if t in SCALAR:
            c.p += SCALAR[t]
        elif t == 8:
            c.p += c.u16()
        elif t == 7:
            c.p += c.i32()
        elif t == 11:
            c.p += c.i32() * 4
        elif t == 12:
            c.p += c.i32() * 8
        else:
            raise ValueError(f"escalar inesperado {t}@{c.p}")

    truncated = False
    while stack:
      try:
        if len(stack) > best:
            best = len(stack); best_path = list(names)
        if cur_top and len(stack) > top_depth.get(cur_top, 0):
            top_depth[cur_top] = len(stack)
        kind = stack[-1][0]
        if kind == "C":
            if c.p >= c.n:
                break
            t = c.u8()
            if t == 0:
                stack.pop(); names.pop(); continue
            nm = c.take(c.u16()).decode("utf-8", "replace")
            if len(stack) == 1:
                cur_top = nm; top_depth.setdefault(nm, 1)
            if t == 10:
                stack.append(("C", nm)); names.append(nm)
            elif t == 9:
                et = c.u8(); ln = c.i32()
                stack.append(["L", nm, et, ln]); names.append(nm + "[]")
            else:
                scalar_skip(t)
        else:
            fr = stack[-1]
            _, nm, et, rem = fr
            if rem <= 0:
                stack.pop(); names.pop(); continue
            fr[3] -= 1
            if et == 10:
                stack.append(("C", nm)); names.append(nm)
            elif et == 9:
                et2 = c.u8(); ln = c.i32()
                stack.append(["L", nm, et2, ln]); names.append(nm + "[]")
            elif et == 0:
                pass
            else:
                scalar_skip(et)
      except (IndexError, struct.error, ValueError):
        truncated = True
        break
    if truncated:
        top_depth["<TRUNCADO/desync @%d de %d>" % (c.p, c.n)] = 0
    return best, best_path, top_depth


# ── leitura com PODA (recursao <= cap; usa skip iterativo abaixo do cap) ─────

def skip_payload_iter(c, t):
    """Avanca c.p pra frente de um payload do tipo t (containers via stack)."""
    stack = []
    def begin(tt):
        if tt == 10:
            stack.append(["C"])
        elif tt == 9:
            et = c.u8(); ln = c.i32(); stack.append(["L", et, ln])
        elif tt in SCALAR:
            c.p += SCALAR[tt]
        elif tt == 8:
            c.p += c.u16()
        elif tt == 7:
            c.p += c.i32()
        elif tt == 11:
            c.p += c.i32() * 4
        elif tt == 12:
            c.p += c.i32() * 8
        elif tt == 0:
            pass
        else:
            raise ValueError(f"tag {tt}")
    begin(t)
    while stack:
        if c.p >= c.n:
            break  # EOF: gzip truncado no meio da bomba — para de pular
        top = stack[-1]
        if top[0] == "C":
            tt = c.u8()
            if tt == 0:
                stack.pop(); continue
            c.p += c.u16()  # nome
            begin(tt)
        else:
            if top[2] <= 0:
                stack.pop(); continue
            top[2] -= 1
            begin(top[1])


PRUNED = []


def read_payload(c, t, depth, cap, path):
    if t in SCALAR:
        return c.take(SCALAR[t])  # bytes crus do escalar (preservados no write)
    if t == 8:
        return c.take(c.u16())
    if t == 7:
        ln = c.i32(); return ("BA", c.take(ln))
    if t == 11:
        ln = c.i32(); return ("IA", c.take(ln * 4))
    if t == 12:
        ln = c.i32(); return ("LA", c.take(ln * 8))
    if t == 10:  # compound
        if depth >= cap:
            skip_payload_iter(c, 10); PRUNED.append("/".join(path)); return ("C", [])
        items = []
        while True:
            if c.p >= c.n:
                break  # EOF (gzip truncado): fecha compound com o que temos
            try:
                tt = c.u8()
                if tt == 0:
                    break
                nm = c.take(c.u16())
                val = read_payload(c, tt, depth + 1, cap, path + [nm.decode("utf-8", "replace")])
            except (IndexError, struct.error, ValueError):
                break  # dado corrompido/incompleto: para aqui (salva o resto valido)
            items.append((tt, nm, val))
        return ("C", items)
    if t == 9:  # list
        et = c.u8(); ln = c.i32()
        if depth >= cap:
            # ja li et/ln; preciso voltar a consumir os elementos
            for _ in range(ln):
                skip_payload_iter(c, et)
            PRUNED.append("/".join(path) + "[]"); return ("L", et, [])
        vals = [read_payload(c, et, depth + 1, cap, path + ["[]"]) for _ in range(ln)]
        return ("L", et, vals)
    raise ValueError(f"tipo {t}")


def write_payload(out, t, v):
    if t in SCALAR:
        out += v; return
    if t == 8:
        out += struct.pack(">H", len(v)); out += v; return
    if t == 7:
        raw = v[1]; out += struct.pack(">i", len(raw)); out += raw; return
    if t == 11:
        raw = v[1]; out += struct.pack(">i", len(raw) // 4); out += raw; return
    if t == 12:
        raw = v[1]; out += struct.pack(">i", len(raw) // 8); out += raw; return
    if t == 10:
        for (tt, nm, val) in v[1]:
            out.append(tt); out += struct.pack(">H", len(nm)); out += nm
            write_payload(out, tt, val)
        out.append(0); return
    if t == 9:
        _, et, vals = v
        out.append(et); out += struct.pack(">i", len(vals))
        for val in vals:
            write_payload(out, et, val)
        return
    raise ValueError(f"tipo {t}")


def repair(buf, cap):
    PRUNED.clear()
    c = Cur(buf)
    root_t = c.u8()
    root_name = c.take(c.u16())
    val = read_payload(c, root_t, 1, cap, ["<root>"])
    out = bytearray()
    out.append(root_t); out += struct.pack(">H", len(root_name)); out += root_name
    write_payload(out, root_t, val)
    return bytes(out), list(PRUNED)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd_or_file")
    ap.add_argument("file", nargs="?")
    ap.add_argument("--prune", type=int)
    ap.add_argument("--out")
    a = ap.parse_args()

    if a.cmd_or_file == "analyze":
        buf = read_raw(a.file)
        depth, path, tops = analyze(buf)
        print(f"max depth = {depth}")
        print("caminho ate o fundo: " + " > ".join(path[:40]) + (" ..." if len(path) > 40 else ""))
        print("profundidade por tag de topo (sob Data/root):")
        for k, d in sorted(tops.items(), key=lambda x: -x[1])[:15]:
            print(f"  {d:>6}  {k}")
        return

    src = a.cmd_or_file
    buf = read_raw(src)
    cap = a.prune if a.prune else 16
    fixed, pruned = repair(buf, cap)
    gz = gzip.compress(fixed)
    out = a.out or src
    if Path(out).exists() and out == src:
        Path(out + ".bak").write_bytes(Path(out).read_bytes())
    Path(out).write_bytes(gz)
    print(f"reparado (cap={cap}). podados {len(pruned)} container(s) profundo(s):")
    for p in pruned[:10]:
        print(f"  - {p}")
    # re-checa
    d2, _, _ = analyze(read_raw(out))
    print(f"escrito em {out}  ({len(gz)} bytes gz)  nova profundidade max = {d2}")


if __name__ == "__main__":
    main()
