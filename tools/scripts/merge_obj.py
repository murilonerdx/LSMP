#!/usr/bin/env python3
"""
merge_obj.py — Junta múltiplos arquivos .obj em UM ÚNICO arquivo .obj.

PROBLEMA QUE RESOLVE:
Você baixou um modelo 3D que veio dividido em vários .obj (partes
separadas). Blockbench abre só um por vez. Esse script combina tudo
em um único arquivo, ajustando os índices de face automaticamente.

USO:
    python merge_obj.py <pasta_entrada> [arquivo_saida]
    python merge_obj.py ./modelo_dividido merged.obj
    python merge_obj.py ./modelo_dividido               # output = merged.obj na mesma pasta
    python merge_obj.py .                                # usa pasta atual

DETALHES TÉCNICOS:
    - Vertices (v), texture coords (vt), normais (vn) são concatenados.
    - Índices de face (f) são ajustados pelo deslocamento acumulado.
      Ex: arquivo 1 tem 100 vertices. Arquivo 2 face "f 1/1/1 2/2/2" vira
      "f 101/101/101 102/102/102" depois do merge.
    - Cada arquivo vira um grupo (g <filename>) pra você poder organizar
      no Blockbench depois.
    - Suporta indices negativos OBJ (relativos: -1 = último vértice).
    - Material library (mtllib) e usemtl são preservados — mas se múltiplos
      arquivos usam o mesmo mtllib, só a primeira referência é mantida.
    - Subdiretórios são VARRIDOS recursivamente.

LIMITAÇÕES:
    - Texturas/materiais não são copiados — só referências. Se os arquivos
      usam .mtl externos, mantenha eles na mesma pasta do .obj final.
    - Linhas de comentário (# ...) são preservadas no topo de cada bloco.
    - Tags 's' (smoothing groups) preservadas.

Author: Liberthia tooling — r62
"""

import argparse
import os
import sys
from pathlib import Path
from typing import List, Tuple

# Force UTF-8 stdout on Windows pra evitar cp1252 issues com nomes de
# arquivos contendo acentos.
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass


def find_obj_files(folder: Path, recursive: bool = True,
                    exclude: List[Path] = None) -> List[Path]:
    """Retorna lista ordenada de .obj na pasta (recursivo).

    Filtra automaticamente arquivos chamados 'merged.obj' ou 'merged_total.obj'
    pra evitar re-merge accidental quando o usuário roda 2x. Também respeita
    a lista 'exclude' (paths absolutos).
    """
    if recursive:
        files = sorted(folder.rglob("*.obj"))
    else:
        files = sorted(folder.glob("*.obj"))
    exclude_set = set()
    if exclude:
        for p in exclude:
            try:
                exclude_set.add(p.resolve())
            except Exception:
                pass
    out = []
    for f in files:
        if not f.is_file():
            continue
        try:
            if f.resolve() in exclude_set:
                continue
        except Exception:
            pass
        # Anti re-merge: arquivos com nome de output anterior são pulados
        if f.name.lower() in ("merged.obj", "merged_total.obj"):
            print(f"  [pulado] {f.name} (output anterior)")
            continue
        out.append(f)
    return out


def offset_face_indices(face_line: str, vert_off: int, tex_off: int, norm_off: int,
                         vert_count: int, tex_count: int, norm_count: int) -> str:
    """
    Reescreve uma linha 'f' deslocando todos os índices.

    Formato OBJ face: f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3
    Pode também ser: f v1 v2 v3 (só vertex)
                    f v1//vn1 (sem texture)
                    f v1/vt1 (sem normal)
    Indices negativos são RELATIVOS — -1 = último vertex desse arquivo.

    Retorna a linha já com offset aplicado.
    """
    tokens = face_line.strip().split()
    if not tokens or tokens[0] != "f":
        return face_line
    new_tokens = ["f"]
    for vtx in tokens[1:]:
        parts = vtx.split("/")
        # parts pode ser: [v], [v, vt], [v, vt, vn], [v, "", vn]
        new_parts = []
        for i, p in enumerate(parts):
            if p == "":
                new_parts.append("")
                continue
            try:
                idx = int(p)
            except ValueError:
                new_parts.append(p)
                continue
            # OBJ usa 1-indexed. Indices negativos = relativo ao topo
            if idx < 0:
                # Converte pra absoluto baseado no count desse arquivo
                if i == 0:
                    abs_idx = vert_count + idx + 1  # +1 porque negative -1 = last
                elif i == 1:
                    abs_idx = tex_count + idx + 1
                else:
                    abs_idx = norm_count + idx + 1
            else:
                abs_idx = idx
            # Aplica offset
            if i == 0:
                new_parts.append(str(abs_idx + vert_off))
            elif i == 1:
                new_parts.append(str(abs_idx + tex_off))
            else:
                new_parts.append(str(abs_idx + norm_off))
        new_tokens.append("/".join(new_parts))
    return " ".join(new_tokens) + "\n"


def offset_line_index(line: str, prefix: str, offset: int, count: int) -> str:
    """
    Para linhas 'l' (line) e 'p' (point) que também usam índices de vertex.
    Formato: l v1 v2 v3...   ou   p v1 v2 v3...
    """
    tokens = line.strip().split()
    if not tokens or tokens[0] != prefix:
        return line
    new_tokens = [prefix]
    for tk in tokens[1:]:
        try:
            idx = int(tk)
            if idx < 0:
                idx = count + idx + 1
            new_tokens.append(str(idx + offset))
        except ValueError:
            new_tokens.append(tk)
    return " ".join(new_tokens) + "\n"


def merge_objs(input_folder: Path, output_file: Path, recursive: bool = True) -> Tuple[int, int]:
    """
    Faz o merge. Retorna (n_arquivos_processados, n_vertices_total).
    """
    files = find_obj_files(input_folder, recursive, exclude=[output_file])
    if not files:
        raise FileNotFoundError(f"Nenhum .obj em {input_folder}")

    print(f"Encontrei {len(files)} arquivo(s) .obj:")
    for f in files:
        print(f"  - {f.relative_to(input_folder)}")

    total_v = 0
    total_vt = 0
    total_vn = 0
    mtllibs_seen = set()
    out_lines: List[str] = []

    # Header
    out_lines.append(f"# Merged from {len(files)} .obj files\n")
    out_lines.append(f"# Generated by merge_obj.py (Liberthia tooling)\n")
    out_lines.append(f"# Source folder: {input_folder}\n\n")

    for file_idx, obj_path in enumerate(files):
        # Conta vertices/tex/norm DESTE arquivo (pra resolver indices negativos)
        local_v = 0
        local_vt = 0
        local_vn = 0
        with obj_path.open("r", encoding="utf-8", errors="replace") as f:
            for line in f:
                head = line.split(maxsplit=1)
                if not head:
                    continue
                tag = head[0]
                if tag == "v":
                    local_v += 1
                elif tag == "vt":
                    local_vt += 1
                elif tag == "vn":
                    local_vn += 1

        # Adiciona group header pra esse arquivo
        group_name = obj_path.stem.replace(" ", "_")
        out_lines.append(f"\n# === Begin: {obj_path.name} ===\n")
        out_lines.append(f"o {group_name}\n")
        out_lines.append(f"g {group_name}\n")

        # Snapshot dos offsets ANTES de processar esse arquivo
        vert_off = total_v
        tex_off = total_vt
        norm_off = total_vn

        with obj_path.open("r", encoding="utf-8", errors="replace") as f:
            for raw in f:
                line = raw.rstrip("\r\n")
                if not line.strip():
                    continue
                tokens = line.split(maxsplit=1)
                if not tokens:
                    continue
                tag = tokens[0]

                # Comentários — preserva
                if tag.startswith("#"):
                    out_lines.append(line + "\n")
                    continue

                # mtllib — só inclui na primeira ocorrência única
                if tag == "mtllib":
                    if line not in mtllibs_seen:
                        mtllibs_seen.add(line)
                        out_lines.append(line + "\n")
                    continue

                # usemtl, s, o, g — preserva como está (group name pode duplicar
                # mas Blockbench lida)
                if tag in ("usemtl", "s"):
                    out_lines.append(line + "\n")
                    continue

                # 'o' ou 'g' do arquivo original — adiciona com prefixo
                if tag == "o" or tag == "g":
                    if len(tokens) > 1:
                        sub = tokens[1]
                        out_lines.append(f"{tag} {group_name}_{sub}\n")
                    else:
                        out_lines.append(line + "\n")
                    continue

                # v / vt / vn — concat direto (sem mudanças)
                if tag in ("v", "vt", "vn"):
                    out_lines.append(line + "\n")
                    continue

                # f — DESLOCA os indices
                if tag == "f":
                    fixed = offset_face_indices(
                        line, vert_off, tex_off, norm_off,
                        local_v, local_vt, local_vn)
                    out_lines.append(fixed)
                    continue

                # l (line) — desloca vertex indices
                if tag == "l":
                    fixed = offset_line_index(line, "l", vert_off, local_v)
                    out_lines.append(fixed)
                    continue

                # p (point) — idem
                if tag == "p":
                    fixed = offset_line_index(line, "p", vert_off, local_v)
                    out_lines.append(fixed)
                    continue

                # Qualquer outro tag (vp, deg, bmat, step, curv, etc) — preserva
                out_lines.append(line + "\n")

        total_v += local_v
        total_vt += local_vt
        total_vn += local_vn

        out_lines.append(f"# === End: {obj_path.name} (v={local_v} vt={local_vt} vn={local_vn}) ===\n")
        print(f"  [{file_idx+1}/{len(files)}] {obj_path.name}: "
              f"v={local_v} vt={local_vt} vn={local_vn}")

    # Footer
    out_lines.append(f"\n# === Merge Summary ===\n")
    out_lines.append(f"# Total: {len(files)} files, v={total_v} vt={total_vt} vn={total_vn}\n")

    # Escreve
    output_file.parent.mkdir(parents=True, exist_ok=True)
    with output_file.open("w", encoding="utf-8") as f:
        f.writelines(out_lines)

    return len(files), total_v


def main():
    parser = argparse.ArgumentParser(
        description="Junta múltiplos .obj em um único arquivo (ajusta índices automaticamente).",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Exemplos:
  python merge_obj.py ./modelo_dividido
  python merge_obj.py ./modelo_dividido merged_total.obj
  python merge_obj.py . output.obj --no-recursive

Saída padrão: <pasta_entrada>/merged.obj
""")
    parser.add_argument("input", help="Pasta com arquivos .obj")
    parser.add_argument("output", nargs="?", default=None,
                        help="Caminho do .obj de saída (default: merged.obj na pasta input)")
    parser.add_argument("--no-recursive", action="store_true",
                        help="Não busca subpastas")
    args = parser.parse_args()

    input_folder = Path(args.input).resolve()
    if not input_folder.is_dir():
        print(f"ERRO: '{input_folder}' não é uma pasta.", file=sys.stderr)
        sys.exit(1)

    if args.output:
        output_file = Path(args.output).resolve()
    else:
        output_file = input_folder / "merged.obj"

    print(f"Merge: {input_folder} -> {output_file}")
    print()

    try:
        n_files, total_v = merge_objs(
            input_folder, output_file, recursive=not args.no_recursive)
    except FileNotFoundError as e:
        print(f"ERRO: {e}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"ERRO inesperado: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)

    print()
    print(f"[OK] Sucesso! {n_files} arquivo(s) -> {output_file}")
    print(f"     Total: {total_v} vertices")
    print(f"     Abra '{output_file.name}' no Blockbench.")


if __name__ == "__main__":
    main()
