"""
r170: Re-slice pack-2 taking ONLY row 0 of each source PNG.

Bug fixed: original slicer iterated ALL rows of the grid, mixing different
phases (yellow intro on row 0, red peak on row 1, etc) within the same color
folder. User saw "spell cycling through many colors" — this fixes it by
keeping each color folder to ONE consistent animation row.

Old output is wiped first.
"""
from PIL import Image
import os
import re
import json
import shutil

ROOT = r'C:/Users/T-GAMER/Desktop/liberthia_mod'
SRC_DIR = os.path.join(ROOT, 'src/main/resources/assets/liberthia/pack-sprites')
OUT_DIR = os.path.join(ROOT, 'src/main/resources/assets/liberthia/textures/vfx_pack2')

CELL = 64
MIN_OPAQUE_PIXELS = 32
ALPHA_THRESHOLD = 5


def opaque_count(crop: Image.Image) -> int:
    if crop.mode != 'RGBA':
        return crop.size[0] * crop.size[1]
    alpha = crop.split()[-1]
    return sum(alpha.histogram()[ALPHA_THRESHOLD + 1:])


def slice_row0(png_path: str, out_dir: str) -> int:
    """Slice ONLY row 0 of the grid — one consistent-color animation."""
    img = Image.open(png_path)
    if img.mode != 'RGBA':
        img = img.convert('RGBA')
    w, _ = img.size
    cols = w // CELL

    os.makedirs(out_dir, exist_ok=True)
    saved = 0
    for c in range(cols):
        cell = img.crop((c * CELL, 0, (c + 1) * CELL, CELL))
        if opaque_count(cell) < MIN_OPAQUE_PIXELS:
            continue
        cell.save(os.path.join(out_dir, f'frame_{saved:03d}.png'))
        saved += 1
    return saved


def main():
    if not os.path.isdir(SRC_DIR):
        raise SystemExit(f'Source not found: {SRC_DIR}')

    # Wipe old output
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
        pngs = sorted([f for f in os.listdir(part_dir) if f.lower().endswith('.png')])
        if not pngs:
            continue

        part_out = os.path.join(OUT_DIR, part_slug)
        os.makedirs(part_out, exist_ok=True)

        color_info = []
        for idx, png_name in enumerate(pngs):
            color_slug = f'c{idx:02d}'
            out_color = os.path.join(part_out, color_slug)
            n = slice_row0(os.path.join(part_dir, png_name), out_color)
            color_info.append({'color_id': color_slug, 'source_file': png_name, 'frame_count': n})

        with open(os.path.join(part_out, 'metadata.json'), 'w', encoding='utf-8') as fp:
            json.dump({'part_number': part_num, 'colors': color_info, 'mode': 'row0'}, fp, indent=2)

        counts = [c['frame_count'] for c in color_info]
        summary[part_slug] = {'colors': len(color_info), 'min': min(counts), 'max': max(counts)}
        print(f'  {part_slug}  {len(color_info)} colors  min={min(counts)}  max={max(counts)}')

    os.makedirs(OUT_DIR, exist_ok=True)
    with open(os.path.join(OUT_DIR, '_summary.json'), 'w', encoding='utf-8') as fp:
        json.dump(summary, fp, indent=2)
    total = sum(s['min'] * s['colors'] for s in summary.values())
    print(f'\nTotal min-frame budget: ~{total} frames')


if __name__ == '__main__':
    main()
