"""Diagnostico: mede a PROFUNDIDADE de aninhamento de arquivos NBT (.dat).

Um StackOverflowError em "Couldn't load level list" = algum NBT aninhado fundo
demais. Este script varre uma pasta, descomprime cada .dat (gzip ou raw) e calcula
a profundidade maxima de aninhamento SEM usar recursao do Python (walker iterativo),
apontando o arquivo culpado.

Uso:
  python tools/nbt_depth.py "C:\\...\\Instances\\liberthia\\saves"
"""
import gzip
import struct
import sys
from pathlib import Path

# tamanho (bytes) dos payloads escalares por tipo de tag
SCALAR = {1: 1, 2: 2, 3: 4, 4: 8, 5: 4, 6: 8}
ARRAY_ELEM = {7: 1, 11: 4, 12: 8}  # ByteArray/IntArray/LongArray: int len + n*elem


def read_raw(path):
    data = path.read_bytes()
    if data[:2] == b"\x1f\x8b":  # magic gzip
        return gzip.decompress(data)
    return data


def max_depth(buf):
    """Profundidade maxima de aninhamento (walker iterativo, sem recursao)."""
    pos = 0
    n = len(buf)

    def u16():
        nonlocal pos
        v = struct.unpack_from(">H", buf, pos)[0]; pos += 2; return v

    def i32():
        nonlocal pos
        v = struct.unpack_from(">i", buf, pos)[0]; pos += 4; return v

    def skip_payload(tag):
        """Pula payload de um tipo escalar/array/string. Containers tratados no loop."""
        nonlocal pos
        if tag in SCALAR:
            pos += SCALAR[tag]
        elif tag == 8:                       # String
            ln = u16(); pos += ln
        elif tag in ARRAY_ELEM:              # Byte/Int/LongArray
            ln = i32(); pos += ln * ARRAY_ELEM[tag]
        else:
            raise ValueError(f"tag escalar inesperada {tag} @ {pos}")

    if n == 0:
        return 0
    root = buf[pos]; pos += 1
    if root != 10:                            # raiz tem que ser Compound
        return 0
    nl = u16(); pos += nl                     # nome da raiz
    # stack de frames: ('C',) compound | ['L', elem_type, remaining] list
    stack = [["C"]]
    depth = 1
    best = 1
    while stack:
        best = max(best, len(stack))
        top = stack[-1]
        if top[0] == "C":
            if pos >= n:
                break
            tag = buf[pos]; pos += 1
            if tag == 0:                      # End -> fecha compound
                stack.pop(); continue
            nl = u16(); pos += nl             # nome
            if tag == 10:
                stack.append(["C"])
            elif tag == 9:
                et = buf[pos]; pos += 1
                ln = i32()
                stack.append(["L", et, ln])
            else:
                skip_payload(tag)
        else:                                 # frame de Lista
            _, et, rem = top
            if rem <= 0:
                stack.pop(); continue
            top[2] -= 1
            if et == 10:
                stack.append(["C"])
            elif et == 9:
                et2 = buf[pos]; pos += 1
                ln = i32()
                stack.append(["L", et2, ln])
            elif et == 0:                     # lista de End (vazia) -> ignora
                pass
            else:
                skip_payload(et)
    return best


def main():
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
    rows = []
    for p in root.rglob("*"):
        if p.suffix.lower() not in (".dat", ".dat_old"):
            continue
        try:
            d = max_depth(read_raw(p))
            rows.append((d, p.stat().st_size, str(p)))
        except Exception as e:
            rows.append((-1, p.stat().st_size if p.exists() else 0, f"{p}  [ERRO: {type(e).__name__}: {e}]"))
    rows.sort(reverse=True)
    print(f"{'DEPTH':>6}  {'BYTES':>9}  FILE")
    for d, sz, path in rows[:30]:
        flag = "  <<< SUSPEITO" if d > 200 else ""
        print(f"{d:>6}  {sz:>9}  {path}{flag}")


if __name__ == "__main__":
    main()
