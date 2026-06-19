"""AI-to-Minecraft pipeline — pega QUALQUER modelo gerado por IA (Meshy/Tripo/Sloyd/etc)
e gera tudo necessario pro Blockbench + mod Minecraft.

Workflow:
  IA gera .glb (Meshy/Tripo/Sloyd)
       v
  python ai_model_pipeline.py <arquivo.glb>
       v
  3 outputs gerados:
    1. .bbmodel cuboide voxelizado    (pra editar no Blockbench)
    2. .obj com mesh original         (pra Forge OBJ Loader, detalhe completo)
    3. resource pack .zip             (pra drop direto no MC)

Usage:
    python ai_model_pipeline.py <arquivo.glb> --name meu_item --type item

Opcoes recomendadas:
    --type item|block                Tipo do asset
    --resolution 16|24|32            Resolucao da versao voxelizada
    --texture-mode atlas|original    Como tratar texturas multiplas
    --target voxel|obj|both          Qual workflow gerar (default both)
"""
import sys, os, json, base64, uuid, argparse, zipfile, subprocess
from datetime import datetime

import trimesh
import numpy as np
from PIL import Image


def inspect(input_path):
    """Mostra info do modelo + recomenda parametros."""
    scene = trimesh.load(input_path)
    if isinstance(scene, trimesh.Scene):
        merged = trimesh.util.concatenate(tuple(scene.dump()))
        n_submesh = len(scene.geometry)
    else:
        merged = scene
        n_submesh = 1

    bounds = merged.bounds
    extent = bounds[1] - bounds[0]
    max_dim = float(extent.max())

    # Detect orientation
    if extent[1] > extent[0] and extent[1] > extent[2]:
        orientation = "Y-up (Minecraft padrao, OK)"
        rotation_fix = None
    elif extent[2] > extent[0] and extent[2] > extent[1]:
        orientation = "Z-up (Blender)"
        rotation_fix = ("X", -90)
    else:
        orientation = "deitado (X-up)"
        rotation_fix = ("Z", 90)

    # Recommend resolution
    if max_dim < 1:
        recommended_res = 16
    elif max_dim < 3:
        recommended_res = 24
    else:
        recommended_res = 32

    info = {
        "tris": len(merged.faces),
        "verts": len(merged.vertices),
        "n_submesh": n_submesh,
        "bounds_min": bounds[0].tolist(),
        "bounds_max": bounds[1].tolist(),
        "extent": extent.tolist(),
        "max_dim": max_dim,
        "orientation": orientation,
        "rotation_fix": rotation_fix,
        "recommended_res": recommended_res,
        "merged_mesh": merged
    }
    return info


def find_or_get_texture(input_path, info, override_path=None):
    """Tenta achar textura: override > pasta ./textures/ > embedded > placeholder."""
    if override_path and os.path.exists(override_path):
        return Image.open(override_path).convert("RGBA"), "override"

    # Pasta /textures/
    base_dir = os.path.dirname(input_path)
    tex_dir = os.path.join(base_dir, "textures")
    if os.path.isdir(tex_dir):
        png_files = sorted([f for f in os.listdir(tex_dir)
                             if f.lower().endswith(".png")])
        if png_files:
            # Pega a maior
            biggest = max(png_files, key=lambda f: os.path.getsize(os.path.join(tex_dir, f)))
            return Image.open(os.path.join(tex_dir, biggest)).convert("RGBA"), f"folder ({biggest})"

    # Embedded no mesh
    try:
        merged = info["merged_mesh"]
        if hasattr(merged.visual, 'material') and merged.visual.material:
            mat = merged.visual.material
            if hasattr(mat, 'image') and mat.image:
                return mat.image.convert("RGBA"), "embedded"
    except Exception:
        pass

    # Placeholder
    return Image.new("RGBA", (16, 16), (180, 180, 180, 255)), "placeholder"


def run_subprocess(args):
    """Roda outro script Python e captura output."""
    result = subprocess.run(
        [sys.executable] + args,
        capture_output=True, text=True, encoding='utf-8', errors='replace'
    )
    return result.returncode, result.stdout, result.stderr


def main():
    ap = argparse.ArgumentParser(description="AI model -> Blockbench + Minecraft pipeline")
    ap.add_argument("input", help="Caminho do .glb/.gltf/.obj")
    ap.add_argument("--name", required=True, help="Nome do bloco/item no mod")
    ap.add_argument("--type", choices=["item", "block"], default="block")
    ap.add_argument("--resolution", type=int, default=None,
                     help="Voxel res (auto se nao informado)")
    ap.add_argument("--target", choices=["voxel", "obj", "both"], default="both",
                     help="Qual workflow gerar")
    ap.add_argument("--texture", default=None, help="Caminho da textura (auto se nao informado)")
    ap.add_argument("--mod-id", default="liberthia")
    ap.add_argument("--output-dir", default=None,
                     help="Pasta de output (default: pasta do input)")
    args = ap.parse_args()

    if not os.path.exists(args.input):
        print(f"ERRO: {args.input} nao encontrado")
        sys.exit(1)

    print("=" * 64)
    print(f"AI MODEL PIPELINE")
    print(f"Input: {args.input}")
    print(f"Name:  {args.name} (type: {args.type})")
    print("=" * 64)

    # === 1. INSPECT ===
    print(f"\n[INSPECAO]")
    info = inspect(args.input)
    print(f"  Triangulos:  {info['tris']:,}")
    print(f"  Submeshes:   {info['n_submesh']}")
    print(f"  Bounds:      X={info['extent'][0]:.2f} Y={info['extent'][1]:.2f} Z={info['extent'][2]:.2f}")
    print(f"  Orientacao:  {info['orientation']}")
    if info["rotation_fix"]:
        ax, deg = info["rotation_fix"]
        print(f"               (precisara Rotate {ax}={deg} no Blockbench)")
    res = args.resolution or info['recommended_res']
    print(f"  Resolucao sugerida: {res}^3")

    # === 2. TEXTURE ===
    print(f"\n[TEXTURA]")
    texture_img, tex_source = find_or_get_texture(args.input, info, args.texture)
    print(f"  Source: {tex_source}, size: {texture_img.size}")

    # Save texture pro output dir
    output_dir = args.output_dir or os.path.dirname(args.input) or "."
    tex_temp = os.path.join(output_dir, f"_{args.name}_texture.png")
    texture_img.save(tex_temp)

    # === 3. VOXELIZED VERSION (.bbmodel) ===
    if args.target in ("voxel", "both"):
        print(f"\n[GERANDO VERSAO VOXELIZADA]")
        bb_out = os.path.join(output_dir, f"{args.name}_voxel.bbmodel")
        textures_dir = os.path.join(os.path.dirname(args.input), "textures")

        script_v2 = os.path.join(os.path.dirname(__file__), "fix_knight_atlas_v2.py")
        if os.path.exists(script_v2):
            print(f"  Rodando fix_knight_atlas_v2.py...")
            rc, out, err = run_subprocess([
                script_v2, args.input,
                "--texture", tex_temp,
                "--name", f"{args.name}_voxel",
                "--resolution", str(res),
                "--mod-id", args.mod_id,
                "--bbmodel-out", bb_out
            ])
            if rc == 0:
                print(f"  [OK] {bb_out}")
            else:
                print(f"  FAIL: {err[:200]}")

    # === 4. OBJ LOADER VERSION (mesh detalhada) ===
    if args.target in ("obj", "both"):
        print(f"\n[GERANDO VERSAO OBJ LOADER (Forge mesh-livre)]")
        script_obj = os.path.join(os.path.dirname(__file__), "gltf_to_forge_obj.py")
        if os.path.exists(script_obj):
            print(f"  Rodando gltf_to_forge_obj.py...")
            rc, out, err = run_subprocess([
                script_obj, args.input,
                "--name", f"{args.name}_detail",
                "--texture", tex_temp,
                "--type", args.type,
                "--mod-id", args.mod_id
            ])
            if rc == 0:
                print(f"  [OK] OBJ + JSON gerados no mod")
            else:
                print(f"  FAIL: {err[:200]}")

    # === 5. RESOURCE PACK ZIP ===
    if args.target in ("voxel", "both"):
        bb_path = os.path.join(output_dir, f"{args.name}_voxel.bbmodel")
        if os.path.exists(bb_path):
            print(f"\n[GERANDO RESOURCE PACK ZIP]")
            zip_out = os.path.join(output_dir, f"{args.name}_pack.zip")
            script_pack = os.path.join(os.path.dirname(__file__), "bb_to_resourcepack.py")
            if os.path.exists(script_pack):
                rc, out, err = run_subprocess([
                    script_pack,
                    "--bbmodel", bb_path,
                    "--output", zip_out,
                    "--name", f"{args.name}_voxel",
                    "--mod-id", args.mod_id,
                    "--description", f"AI generated: {args.name}"
                ])
                if rc == 0:
                    sz = os.path.getsize(zip_out)
                    print(f"  [OK] {zip_out} ({sz//1024} KB)")
                else:
                    print(f"  FAIL: {err[:200]}")

    # Cleanup
    if os.path.exists(tex_temp):
        os.remove(tex_temp)

    print(f"\n" + "=" * 64)
    print(f"PIPELINE COMPLETO")
    print(f"=" * 64)
    print(f"\nARQUIVOS GERADOS:")
    print(f"")
    print(f"1. {args.name}_voxel.bbmodel — abrir no Blockbench (versao cubo)")
    print(f"2. {args.name}_detail.obj    — modelo detalhado pra Forge OBJ Loader")
    print(f"3. {args.name}_pack.zip       — resource pack pra drop no MC")
    print(f"")
    print(f"NO MOD:")
    print(f"  src/main/resources/assets/{args.mod_id}/...  ja registrado")
    print(f"")
    print(f"PROXIMOS PASSOS:")
    print(f"  1. Cria Block/Item Java em ModBlocks.java / ModItems.java")
    print(f"  2. Adiciona lang entry: {args.type}.{args.mod_id}.{args.name}")
    print(f"  3. ./gradlew build")


if __name__ == "__main__":
    main()
