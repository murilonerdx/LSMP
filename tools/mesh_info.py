"""Inspeciona um arquivo 3D (.glb/.gltf/.obj) e mostra:
- Dimensoes em unidades originais
- Proporcoes (X:Y:Z)
- Orientacao detectada (Y-up vs Z-up)
- Tris, vertices, UVs
- Tamanho da textura
- RECOMENDACAO de resolucao voxel pro Blockbench/Minecraft
- Scale factor sugerido (quantos blocos MC ocupar)

Usage:
    python mesh_info.py <arquivo.glb>
"""
import sys, os, argparse
import trimesh
import numpy as np
from PIL import Image


def detect_orientation(extent):
    """Heuristica simples — qual eixo eh o vertical (altura)?
    Personagens humanoides costumam ter altura > largura e profundidade.
    """
    x, y, z = extent
    if y > x and y > z:
        return "Y-up (correto pro Minecraft)"
    if z > x and z > y:
        return "Z-up (Blender) — vai precisar rotacionar 90 graus em X"
    if x > y and x > z:
        return "X-up ou modelo deitado — provavelmente precisa rotacao"
    return "Indefinido (proporcoes iguais)"


def recommend_resolution(extent, target_blocks=1):
    """Quantos voxels precisa pra preencher 1 bloco Minecraft (16 unidades) com detalhe ok."""
    max_dim = max(extent)
    # Se o modelo tem altura 2.5 unidades e queremos cabe em 1 bloco (16 px MC):
    # resolution deve ser 16 ou maior. Se tem altura 30 unidades, scale!
    # Voxel size = max_dim / resolution
    # Pra resolution 16 → cada voxel tem max_dim/16 unidades
    # Pra resolution 24 → cada voxel tem max_dim/24 unidades

    # Sugere baseado em tamanho
    if max_dim < 0.5:
        # modelo muito pequeno — ele vai ficar tipo 1 bloco. Resolution 16 suficiente
        return 16, "modelo pequeno"
    elif max_dim < 2:
        return 16, "tamanho normal — humanoide compacto"
    elif max_dim < 5:
        return 24, "altura humana grande — usa resolucao maior pra preservar detalhe"
    elif max_dim < 10:
        return 32, "modelo grande — precisa mais voxels"
    else:
        return 32, "modelo muito grande — considera scale down antes ou aceita perda detalhe"


def estimate_minecraft_blocks(extent):
    """Quantos blocos MC esse modelo ocuparia se cada unidade = 1 bloco?"""
    # Minecraft block = 16 game units. Modelo block model max = 48 units (3 blocos).
    # Aqui assumimos: modelo unidade = "metro" no GLTF
    # Player Steve tem ~1.8m altura → 1 bloco = 1m no MC
    blocks_x = extent[0]
    blocks_y = extent[1]
    blocks_z = extent[2]
    return blocks_x, blocks_y, blocks_z


def calc_scale_for_one_block(extent):
    """Scale factor pra fazer o modelo caber dentro de 1 bloco MC (16 unidades)."""
    max_dim = max(extent)
    if max_dim == 0:
        return 1.0
    # Queremos max_dim ficar = 1.0 (que vira 16 units no MC quando voxelizado)
    return 1.0 / max_dim


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("input", help="Caminho do arquivo 3D (.glb/.gltf/.obj/.fbx)")
    args = ap.parse_args()

    if not os.path.exists(args.input):
        print(f"ERRO: nao encontrado: {args.input}")
        sys.exit(1)

    print("=" * 60)
    print(f"MESH INFO: {os.path.basename(args.input)}")
    print("=" * 60)

    try:
        mesh = trimesh.load(args.input, force='mesh')
    except Exception as e:
        print(f"ERRO carregando mesh: {e}")
        sys.exit(1)

    if isinstance(mesh, trimesh.Scene):
        n_geoms = len(mesh.geometry)
        print(f"  Tipo: Scene com {n_geoms} geometrias (vou concatenar)")
        mesh = trimesh.util.concatenate(tuple(mesh.dump()))

    # === Geometria ===
    print(f"\n[GEOMETRIA]")
    print(f"  Triangulos:  {len(mesh.faces):,}")
    print(f"  Vertices:    {len(mesh.vertices):,}")
    bounds = mesh.bounds
    extent = bounds[1] - bounds[0]
    print(f"  Bounding box (min):   X={bounds[0][0]:.3f}  Y={bounds[0][1]:.3f}  Z={bounds[0][2]:.3f}")
    print(f"  Bounding box (max):   X={bounds[1][0]:.3f}  Y={bounds[1][1]:.3f}  Z={bounds[1][2]:.3f}")
    print(f"  Dimensoes:            X={extent[0]:.3f}  Y={extent[1]:.3f}  Z={extent[2]:.3f}")
    print(f"  Proporcao normalizada: {extent[0]/max(extent):.2f} : {extent[1]/max(extent):.2f} : {extent[2]/max(extent):.2f}")

    # === Orientacao ===
    print(f"\n[ORIENTACAO]")
    orient = detect_orientation(extent)
    print(f"  {orient}")
    if "Z-up" in orient or "X-up" in orient or "deitado" in orient:
        print(f"  >> NO BLOCKBENCH: depois de importar, selecione tudo (Ctrl+A)")
        if "Z-up" in orient:
            print(f"  >> e aplique 'Rotate X = -90' (rotacao 90 graus em X)")
        if "X-up" in orient or "deitado" in orient:
            print(f"  >> e aplique 'Rotate Z = 90' ou tente -90 ate ficar de pe")

    # === UV / Textura ===
    print(f"\n[UV / TEXTURA]")
    has_uv = hasattr(mesh.visual, 'uv') and mesh.visual.uv is not None and len(mesh.visual.uv) > 0
    print(f"  Tem UV mapping: {'SIM' if has_uv else 'NAO'}")
    # Procura textura embed
    try:
        if hasattr(mesh.visual, 'material') and mesh.visual.material is not None:
            mat = mesh.visual.material
            if hasattr(mat, 'image') and mat.image is not None:
                w, h = mat.image.size
                print(f"  Textura embedded:  {w}x{h} pixels")
            else:
                print(f"  Material: {type(mat).__name__} (sem imagem embedded)")
    except Exception:
        pass

    # Procura textura na pasta
    base_dir = os.path.dirname(args.input)
    textures_dir = os.path.join(base_dir, "textures")
    if os.path.isdir(textures_dir):
        textures_found = [f for f in os.listdir(textures_dir) if f.lower().endswith(('.png', '.jpg', '.jpeg'))]
        if textures_found:
            print(f"  Texturas externas em ./textures/:")
            for t in textures_found:
                tp = os.path.join(textures_dir, t)
                try:
                    img = Image.open(tp)
                    print(f"    * {t}: {img.size[0]}x{img.size[1]} pixels ({os.path.getsize(tp)//1024} KB)")
                except Exception:
                    print(f"    * {t}: (nao consegui abrir)")

    # === Recomendacoes Blockbench/Minecraft ===
    print(f"\n[RECOMENDACOES BLOCKBENCH]")
    res, reason = recommend_resolution(extent)
    print(f"  Resolucao voxel sugerida: {res} ({reason})")
    voxel_size = max(extent) / res
    print(f"  Tamanho de cada voxel (unidades originais): {voxel_size:.4f}")

    print(f"\n[TAMANHO NO MINECRAFT]")
    # Se voxelizar com res N, modelo ocupa N voxels = N/16 blocos MC (cada bloco MC = 16 pixels)
    blocks_h = res / 16.0
    print(f"  Se voxelizar em {res}^3 e exportar como block model: ocupa {blocks_h:.1f} bloco(s) MC de altura")
    if blocks_h > 3:
        print(f"  >> ATENCAO: Block models MC max sao 3x3x3 blocos. Considera entity model.")
    if blocks_h < 1:
        print(f"  >> Vai ficar pequenininho dentro de 1 bloco. Pode aumentar resolution se quiser maior.")

    # === Configuracoes do Blockbench ===
    print(f"\n[CONFIG NO BLOCKBENCH]")
    if has_uv and "embedded" not in str(type(mesh.visual)).lower():
        # Tem UV — assume textura sera embed via bbmodel
        # Pega tamanho da textura embed se possivel
        try:
            tex_w, tex_h = 16, 16
            if hasattr(mesh.visual, 'material') and hasattr(mesh.visual.material, 'image'):
                if mesh.visual.material.image:
                    tex_w, tex_h = mesh.visual.material.image.size
        except Exception:
            tex_w, tex_h = 16, 16
        if textures_dir and os.path.isdir(textures_dir):
            tx_files = [f for f in os.listdir(textures_dir) if f.lower().endswith('.png')]
            if tx_files:
                try:
                    img = Image.open(os.path.join(textures_dir, tx_files[0]))
                    tex_w, tex_h = img.size
                except Exception:
                    pass
        print(f"  Project type: Java Block/Item Model")
        print(f"  Resolution (texture):   {tex_w} x {tex_h}")
    else:
        print(f"  Project type: Java Block/Item Model")
        print(f"  Resolution (texture):   16 x 16 (default)")

    print(f"\n[COMANDO PARA CONVERTER]")
    name = os.path.splitext(os.path.basename(args.input))[0].lower().replace(' ', '_').replace('-', '_')
    print(f"  python build/fix_knight_atlas_v2.py \"{args.input}\" \\")
    print(f"    --texture \"<caminho_da_textura.png>\" \\")
    print(f"    --name {name} --resolution {res}")

    print("\n" + "=" * 60)


if __name__ == "__main__":
    main()
