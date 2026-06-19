#!/usr/bin/env python3
"""
Gerador de texturas dos 12 blocos infectados v0.1.13.

Cria texturas 16x16 PNG pixeladas pra dirt/sand/stone/grass × DM/WM/YM
+ blockstates + models + item models + ajuste do blood_bucket.

Requires: Pillow (`pip install pillow`).

Uso:
    python tools/generate-infected-textures.py
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import json
import random

ROOT = Path(__file__).parent.parent
RES = ROOT / "src/main/resources/assets/liberthia"
TEX_DIR = RES / "textures/block"
ITEM_TEX_DIR = RES / "textures/item"
BLOCKSTATE_DIR = RES / "blockstates"
BLOCK_MODEL_DIR = RES / "models/block"
ITEM_MODEL_DIR = RES / "models/item"


# Paletas base — cada matter type tem 3 tons (escuro/médio/claro) + accent
PALETTES = {
    "DM": {  # Dark Matter — roxo/preto/cinza escuro
        "shadow":  (8, 0, 24),
        "base":    (32, 8, 56),
        "mid":     (72, 32, 100),
        "highlight": (140, 70, 180),
        "accent":  (210, 110, 255),  # neon glow
    },
    "WM": {  # White Matter — branco/azul gelo
        "shadow":  (180, 200, 220),
        "base":    (220, 230, 240),
        "mid":     (235, 240, 248),
        "highlight": (250, 252, 255),
        "accent":  (170, 220, 255),
    },
    "YM": {  # Yellow Matter — amarelo/dourado/laranja
        "shadow":  (140, 100, 0),
        "base":    (200, 160, 30),
        "mid":     (230, 200, 70),
        "highlight": (255, 230, 110),
        "accent":  (255, 180, 50),
    },
}


def seeded_rng(seed_str: str) -> random.Random:
    """Cada textura tem seed deterministico — mesmo bloco gera mesma textura sempre."""
    return random.Random(hash(seed_str) & 0xFFFFFFFF)


def base_dirt(pal: dict, rng: random.Random) -> Image.Image:
    """Textura tipo dirt — granulada, irregular, opaca."""
    img = Image.new("RGB", (16, 16), pal["base"])
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = rng.random()
            if r < 0.30:
                px[x, y] = pal["shadow"]
            elif r < 0.55:
                px[x, y] = pal["mid"]
            elif r < 0.65:
                px[x, y] = pal["highlight"]
    # Adiciona pontos de accent (veias de matéria)
    for _ in range(rng.randint(2, 5)):
        ax, ay = rng.randint(0, 15), rng.randint(0, 15)
        px[ax, ay] = pal["accent"]
    return img


def base_sand(pal: dict, rng: random.Random) -> Image.Image:
    """Textura tipo sand — finamente granulada, mais clara."""
    img = Image.new("RGB", (16, 16), pal["mid"])
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = rng.random()
            if r < 0.40:
                px[x, y] = pal["base"]
            elif r < 0.65:
                px[x, y] = pal["highlight"]
            elif r < 0.70:
                px[x, y] = pal["shadow"]
    # Sand tem partículas brilhantes pontuais
    for _ in range(rng.randint(4, 8)):
        ax, ay = rng.randint(0, 15), rng.randint(0, 15)
        px[ax, ay] = pal["accent"]
    return img


def base_stone(pal: dict, rng: random.Random) -> Image.Image:
    """Textura tipo stone — blocada, fissuras visíveis."""
    img = Image.new("RGB", (16, 16), pal["mid"])
    px = img.load()
    # Base: blobs irregulares
    for y in range(16):
        for x in range(16):
            r = rng.random()
            if r < 0.35:
                px[x, y] = pal["base"]
            elif r < 0.55:
                px[x, y] = pal["shadow"]
            elif r < 0.70:
                px[x, y] = pal["highlight"]
    # Fissuras (linhas escuras)
    for _ in range(rng.randint(2, 4)):
        x0 = rng.randint(0, 15); y0 = rng.randint(0, 15)
        for step in range(rng.randint(3, 6)):
            dx = rng.randint(-1, 1); dy = rng.randint(-1, 1)
            x0 = max(0, min(15, x0 + dx)); y0 = max(0, min(15, y0 + dy))
            px[x0, y0] = pal["shadow"]
    # Accent: veias de matéria nas fissuras
    for _ in range(rng.randint(1, 3)):
        ax, ay = rng.randint(0, 15), rng.randint(0, 15)
        px[ax, ay] = pal["accent"]
    return img


def base_grass(pal: dict, rng: random.Random) -> Image.Image:
    """Textura tipo grass — top tem 'pelos' de grama, base é dirt."""
    img = base_dirt(pal, rng).copy()
    px = img.load()
    # Top 4 rows: faixa de grama
    for x in range(16):
        for y in range(4):
            r = rng.random()
            if r < 0.5:
                px[x, y] = pal["highlight"]
            elif r < 0.8:
                px[x, y] = pal["accent"]
            else:
                px[x, y] = pal["mid"]
    # Pelos espinhosos na linha y=3..4 saindo pra baixo
    for x in range(16):
        if rng.random() < 0.4:
            depth = rng.randint(0, 2)
            for d in range(depth):
                py = 4 + d
                if py < 16:
                    px[x, py] = pal["accent"] if rng.random() < 0.3 else pal["highlight"]
    return img


# Mapeamento bloco → função geradora
GENERATORS = {
    "dirt":  base_dirt,
    "sand":  base_sand,
    "stone": base_stone,
    "grass": base_grass,
}

# Prefixos por matter
PREFIX = {
    "DM": "dm_infected",
    "WM": "wm_bleached",
    "YM": "ym_unstable",
}


def make_blockstate(name: str) -> dict:
    return {
        "variants": {
            "": {"model": f"liberthia:block/{name}"}
        }
    }


def make_block_model(name: str) -> dict:
    return {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"liberthia:block/{name}"}
    }


def make_item_model(name: str) -> dict:
    return {"parent": f"liberthia:block/{name}"}


def generate_block(matter: str, variant: str):
    name = f"{PREFIX[matter]}_{variant}"
    pal = PALETTES[matter]
    rng = seeded_rng(name)
    img = GENERATORS[variant](pal, rng)

    # Salva textura
    out_tex = TEX_DIR / f"{name}.png"
    out_tex.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_tex)

    # blockstate
    (BLOCKSTATE_DIR / f"{name}.json").write_text(
        json.dumps(make_blockstate(name), indent=2), encoding="utf-8")
    # block model
    (BLOCK_MODEL_DIR / f"{name}.json").write_text(
        json.dumps(make_block_model(name), indent=2), encoding="utf-8")
    # item model
    (ITEM_MODEL_DIR / f"{name}.json").write_text(
        json.dumps(make_item_model(name), indent=2), encoding="utf-8")

    print(f"OK -> block {name} (texture {out_tex.stat().st_size}B)")


def blood_bucket_texture():
    """Substitui blood_bucket.png — bucket de metal cinza com líquido vermelho-escuro dentro."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # Bucket shape (metal cinza)
    GREY_DARK = (60, 60, 65, 255)
    GREY_MID = (100, 100, 110, 255)
    GREY_LIGHT = (160, 160, 170, 255)
    RIM = (200, 200, 210, 255)
    BLOOD_DEEP = (90, 0, 0, 255)
    BLOOD_MID = (140, 10, 10, 255)
    BLOOD_BRIGHT = (200, 30, 30, 255)
    # Bucket body (rectangular trapezoid)
    for y in range(4, 15):
        for x in range(3, 13):
            # Side walls
            if x == 3 or x == 12:
                px[x, y] = GREY_DARK
            elif x == 4 or x == 11:
                px[x, y] = GREY_MID
            else:
                # Inside — fill with blood
                if y >= 5:
                    if y == 5:
                        px[x, y] = BLOOD_BRIGHT  # surface highlight
                    elif (x + y) % 3 == 0:
                        px[x, y] = BLOOD_MID
                    else:
                        px[x, y] = BLOOD_DEEP
                else:
                    px[x, y] = GREY_LIGHT
    # Top rim
    for x in range(2, 14):
        px[x, 3] = RIM
    px[1, 3] = GREY_DARK; px[14, 3] = GREY_DARK
    # Handle (arc above)
    for x in range(4, 12):
        px[x, 1] = GREY_DARK
    px[3, 2] = GREY_DARK; px[12, 2] = GREY_DARK
    # Bottom
    for x in range(4, 12):
        px[x, 15] = GREY_DARK
    px[3, 14] = GREY_DARK; px[12, 14] = GREY_DARK

    out = ITEM_TEX_DIR / "blood_bucket.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"OK -> blood_bucket.png ({out.stat().st_size}B)")

    # Also create the item model JSON if missing
    model = ITEM_MODEL_DIR / "blood_bucket.json"
    if not model.exists():
        model.write_text(json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "liberthia:item/blood_bucket"}
        }, indent=2), encoding="utf-8")
        print(f"OK -> blood_bucket.json model criado")


def main():
    print(f"ROOT = {ROOT}")
    print(f"TEX_DIR = {TEX_DIR}")
    print()
    matters = ["DM", "WM", "YM"]
    variants = ["dirt", "sand", "stone", "grass"]
    for m in matters:
        for v in variants:
            generate_block(m, v)
    print()
    blood_bucket_texture()
    print()
    print("Done. 12 blocos infectados + blood_bucket gerados.")
    print("Lembre de adicionar lang entries no en_us.json/pt_br.json se quiser nomes traduzidos.")


if __name__ == "__main__":
    main()
