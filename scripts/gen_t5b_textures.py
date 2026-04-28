"""Generate placeholder entity + item textures for T5b blood content.

- blood_mage.png   — 64x64 player-layout sheet, dark crimson robe tint
- blood_hound.png  — 64x32 wolf-layout sheet, dark red tint
- blood_syringe.png — 16x16 item icon (vial + needle)

Uses the simplest "recolor vanilla sheet" trick: we don't have PIL access to
vanilla assets here, so we just draw flat colored sheets with a few accent
pixels. It's functional placeholder art — the models will still render
correctly because they only care about UV coordinates being non-transparent.
"""
from PIL import Image, ImageDraw
from pathlib import Path
import random

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/liberthia/textures"

def noise(img, palette, density=0.25):
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if random.random() < density:
                px[x, y] = (*random.choice(palette), 255)

def mage():
    # Player layout 64x64. We paint the whole thing dark crimson, then splatter
    # blood darker/brighter pixels. The model UVs cover the usual regions.
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Fill the 4 main boxes (head, body, arms, legs) roughly — just fill full
    # bitmap since unused pixels won't be sampled.
    d.rectangle([0, 0, 63, 63], fill=(80, 10, 15, 255))
    noise(img, [(50, 5, 8), (110, 20, 25), (40, 0, 0), (150, 30, 30)], 0.35)
    # Hood: darker top on head region (0..32,0..16) approx
    d.rectangle([0, 0, 32, 8], fill=(30, 0, 0, 255))
    noise(img, [(10, 0, 0), (50, 5, 5)], 0.5)
    p = OUT / "entity/blood_mage.png"
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)
    print("wrote", p)

def hound():
    # Wolf layout 64x32.
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 63, 31], fill=(110, 15, 15, 255))
    noise(img, [(70, 8, 8), (160, 30, 30), (40, 0, 0), (200, 50, 50)], 0.4)
    # Muzzle darker
    d.rectangle([0, 0, 20, 10], fill=(40, 5, 5, 255))
    noise(img, [(20, 0, 0), (80, 10, 10)], 0.45)
    p = OUT / "entity/blood_hound.png"
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)
    print("wrote", p)

def syringe():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Needle diagonal from top-right to center
    for i in range(6):
        d.point((13 - i, 2 + i), fill=(200, 200, 210, 255))
    # Glass barrel
    d.rectangle([5, 6, 10, 13], fill=(220, 220, 230, 255))
    d.rectangle([6, 7, 9, 12], fill=(170, 20, 25, 255))  # blood inside
    d.rectangle([6, 10, 9, 12], fill=(110, 10, 12, 255))
    # Plunger top
    d.rectangle([4, 4, 11, 5], fill=(180, 180, 180, 255))
    d.rectangle([6, 5, 9, 6], fill=(120, 120, 120, 255))
    # Tip of needle
    d.point((14, 1), fill=(240, 240, 250, 255))
    p = OUT / "item/blood_syringe.png"
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)
    print("wrote", p)

if __name__ == "__main__":
    random.seed(17)
    mage()
    hound()
    syringe()
