"""
r170: GERAR 12 cores REAIS por part via HSV hue rotation.

Problema: source PNGs do pack tem 12 PNGs mas todos são variações da MESMA cor
(diferentes intensidades de vermelho/laranja, não cores diferentes). Não importa
qual cKK o spell escolhe, sempre fica vermelho.

Fix: Tomar UMA textura base de cada part (a "mais rica"), converter pra HSL,
gerar 12 versões com hue rotacionado em 0°, 30°, 60°, ..., 330°. Isso dá:
  c00 = red       (0°)
  c01 = orange    (30°)
  c02 = yellow    (60°)
  c03 = green     (120°)
  c04 = cyan      (180°)
  c05 = blue      (240°)
  c06 = purple    (270°)
  c07 = magenta   (300°)
  c08 = pink      (330°)
  c09 = white     (no saturation)
  c10 = light blue (lower saturation, 200°)
  c11 = gold      (yellow + low saturation)

Aplica em CADA frame de CADA part (row-0). Final: cada cKK folder tem TODAS as
frames na mesma cor consistente, e a cor é OBVIAMENTE diferente entre cKK.
"""
from PIL import Image
import colorsys
import os
import shutil
import json
import re

ROOT = r'C:/Users/T-GAMER/Desktop/liberthia_mod'
SRC_DIR = os.path.join(ROOT, 'src/main/resources/assets/liberthia/pack-sprites')
OUT_DIR = os.path.join(ROOT, 'src/main/resources/assets/liberthia/textures/vfx_pack2')

CELL = 64
MIN_OPAQUE = 32
ALPHA_THRESHOLD = 5

# Color table — hue in degrees, sat multiplier 0..1, value bias
# c00=red, c01=orange, c02=yellow, c03=green, c04=cyan, c05=blue,
# c06=purple, c07=magenta, c08=white, c09=brown, c10=pink, c11=teal
COLORS = [
    ("red",       0.00, 1.0, 1.0),
    ("orange",    0.083, 1.0, 1.0),
    ("yellow",    0.167, 1.0, 1.0),
    ("green",     0.333, 0.9, 1.0),
    ("cyan",      0.500, 1.0, 1.0),
    ("blue",      0.667, 1.0, 1.0),
    ("purple",    0.750, 0.9, 1.0),
    ("magenta",   0.833, 1.0, 1.0),
    ("white",     0.000, 0.0, 1.1),  # desaturated → near white
    ("brown",     0.083, 0.5, 0.6),  # darkened orange
    ("pink",      0.917, 0.4, 1.0),  # light magenta
    ("teal",      0.500, 0.6, 0.8),  # darker cyan
]


def opaque_count(crop: Image.Image) -> int:
    if crop.mode != 'RGBA':
        return crop.size[0] * crop.size[1]
    alpha = crop.split()[-1]
    return sum(alpha.histogram()[ALPHA_THRESHOLD + 1:])


def shift_hue(img: Image.Image, target_hue: float, sat_mul: float, val_bias: float) -> Image.Image:
    """Replace dominant hue with target hue. Keeps brightness from source.

    Mode: convert each opaque pixel to HSV, replace H with target_hue, scale S,
    multiply V by val_bias. Preserve transparent pixels untouched.
    """
    if img.mode != 'RGBA':
        img = img.convert('RGBA')
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 5:
                continue
            # Source HSV (use the V/S from source so brightness/intensity is kept)
            _, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            # Use source brightness, but force the target hue + scaled saturation
            new_s = min(1.0, s * sat_mul) if sat_mul > 0 else 0.0
            new_v = min(1.0, v * val_bias)
            nr, ng, nb = colorsys.hsv_to_rgb(target_hue, new_s, new_v)
            px[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    return img


def best_source_png(part_dir: str) -> str:
    """Return the source PNG with the MOST opaque pixels (richest texture)."""
    best, best_score = None, 0
    for f in sorted(os.listdir(part_dir)):
        if not f.lower().endswith('.png'):
            continue
        img = Image.open(os.path.join(part_dir, f)).convert('RGBA')
        score = opaque_count(img)
        if score > best_score:
            best_score = score
            best = f
    return best


def slice_row0_base(png_path: str) -> list:
    """Return list of RGBA Image objects, one per non-empty cell in row 0."""
    img = Image.open(png_path).convert('RGBA')
    w, _ = img.size
    cols = w // CELL
    frames = []
    for c in range(cols):
        cell = img.crop((c * CELL, 0, (c + 1) * CELL, CELL))
        if opaque_count(cell) < MIN_OPAQUE:
            continue
        frames.append(cell)
    return frames


def main():
    if not os.path.isdir(SRC_DIR):
        raise SystemExit(f'Source not found: {SRC_DIR}')

    if os.path.exists(OUT_DIR):
        shutil.rmtree(OUT_DIR)
        print(f'Wiped: {OUT_DIR}')

    summary = {}
    parts = sorted(f for f in os.listdir(SRC_DIR) if f.lower().startswith('part '))

    for part_name in parts:
        m = re.match(r'Part\s*(\d+)', part_name, re.IGNORECASE)
        if not m:
            continue
        part_num = int(m.group(1))
        part_slug = f'part{part_num:02d}'
        part_dir = os.path.join(SRC_DIR, part_name)

        # Pick the richest source PNG for this part
        best = best_source_png(part_dir)
        if not best:
            print(f'  {part_slug}: NO source PNG found, skipping')
            continue
        print(f'  {part_slug}: base = {best}', end='  ')

        base_frames = slice_row0_base(os.path.join(part_dir, best))
        if not base_frames:
            print('NO frames in row 0')
            continue
        print(f'{len(base_frames)} base frames')

        part_out = os.path.join(OUT_DIR, part_slug)
        os.makedirs(part_out, exist_ok=True)

        # For each of the 12 colors, generate hue-shifted versions of each frame
        for ci, (name, hue, sat, val) in enumerate(COLORS):
            cdir = os.path.join(part_out, f'c{ci:02d}')
            os.makedirs(cdir, exist_ok=True)
            for fi, base in enumerate(base_frames):
                # Make a copy to avoid mutating the original
                shifted = shift_hue(base.copy(), hue, sat, val)
                shifted.save(os.path.join(cdir, f'frame_{fi:03d}.png'))

        summary[part_slug] = {
            'base_source': best,
            'frames_per_color': len(base_frames),
            'colors': [{'idx': ci, 'name': name} for ci, (name, *_) in enumerate(COLORS)],
        }

    with open(os.path.join(OUT_DIR, '_summary.json'), 'w', encoding='utf-8') as fp:
        json.dump(summary, fp, indent=2)

    total = sum(s['frames_per_color'] * len(COLORS) for s in summary.values())
    print(f'\nTotal output frames: {total}')
    print(f'Colors per part: c00=red, c01=orange, c02=yellow, c03=green, c04=cyan,')
    print(f'                 c05=blue, c06=purple, c07=magenta, c08=white,')
    print(f'                 c09=brown, c10=pink, c11=teal')


if __name__ == '__main__':
    main()
