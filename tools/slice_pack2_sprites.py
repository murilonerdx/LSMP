"""
r167: Slice the Part 1..15 pack-sprites.

Each Part folder contains ~12 PNGs (each one a different color variant of the
same animation). Each PNG is a grid of 64x64 cells with variable width:
  <NN>.png  -> W x 576, where W is a multiple of 64
  9 rows x (W/64) cols of 64-px cells.

For each Part:
  1. Slice every PNG into individual 64x64 frame PNGs.
  2. Group by "color" (= source filename without extension).
  3. Save as textures/vfx_pack2/part<NN>/c<NN>/frame_NNN.png
  4. Build a metadata.json per Part with frame count + color variants.

Skip empty cells (alpha < threshold for >= 99% of pixels).
"""
from PIL import Image
import os
import re
import json

ROOT = r'C:/Users/T-GAMER/Desktop/liberthia_mod'
SRC_DIR = os.path.join(ROOT, 'src/main/resources/assets/liberthia/pack-sprites')
OUT_DIR = os.path.join(ROOT, 'src/main/resources/assets/liberthia/textures/vfx_pack2')

CELL = 64
MIN_OPAQUE_PIXELS = 32   # >= 1% of 64x64 = 41 px — be lenient
ALPHA_THRESHOLD = 5


def has_content(crop: Image.Image) -> int:
    """Return the # of opaque pixels in the crop (alpha > threshold)."""
    if crop.mode != 'RGBA':
        return crop.size[0] * crop.size[1]
    alpha = crop.split()[-1]
    histo = alpha.histogram()
    return sum(histo[ALPHA_THRESHOLD + 1:])


def slice_png(png_path: str, out_dir: str) -> int:
    """Slice one PNG into individual 64x64 frame_NNN.png files. Return count."""
    img = Image.open(png_path)
    if img.mode != 'RGBA':
        img = img.convert('RGBA')
    w, h = img.size
    cols = w // CELL
    rows = h // CELL
    if cols * CELL != w or rows * CELL != h:
        print(f'  WARN: {os.path.basename(png_path)} size {w}x{h} is not multiple of {CELL}')

    os.makedirs(out_dir, exist_ok=True)
    saved = 0
    for r in range(rows):
        for c in range(cols):
            cell = img.crop((c * CELL, r * CELL, (c + 1) * CELL, (r + 1) * CELL))
            opq = has_content(cell)
            if opq < MIN_OPAQUE_PIXELS:
                continue
            cell.save(os.path.join(out_dir, f'frame_{saved:03d}.png'))
            saved += 1
    return saved


def main():
    if not os.path.isdir(SRC_DIR):
        raise SystemExit(f'Source not found: {SRC_DIR}')

    summary = {}
    parts = sorted(os.listdir(SRC_DIR))
    parts = [p for p in parts if p.lower().startswith('part ')]
    print(f'Found {len(parts)} part folders')

    for part_name in parts:
        # Normalize "Part 1" -> "part01"
        m = re.match(r'Part\s*(\d+)', part_name, re.IGNORECASE)
        if not m:
            continue
        part_num = int(m.group(1))
        part_slug = f'part{part_num:02d}'

        part_dir = os.path.join(SRC_DIR, part_name)
        pngs = sorted([f for f in os.listdir(part_dir) if f.lower().endswith('.png')])
        if not pngs:
            continue

        part_out_dir = os.path.join(OUT_DIR, part_slug)
        os.makedirs(part_out_dir, exist_ok=True)

        color_info = []  # list of {color_id, source_file, frame_count}
        total_frames_part = 0
        # r167 v2: sequential 00..11 naming so Java can iterate by index
        for idx, png_name in enumerate(pngs):
            color_slug = f'c{idx:02d}'  # c00, c01, ... c11
            out_color_dir = os.path.join(part_out_dir, color_slug)

            count = slice_png(os.path.join(part_dir, png_name), out_color_dir)
            color_info.append({
                'color_id': color_slug,
                'source_file': png_name,
                'frame_count': count,
            })
            total_frames_part += count

        with open(os.path.join(part_out_dir, 'metadata.json'), 'w', encoding='utf-8') as fp:
            json.dump({
                'part_number': part_num,
                'colors': color_info,
            }, fp, indent=2)

        summary[part_slug] = {
            'colors': len(color_info),
            'frames_per_color': color_info[0]['frame_count'] if color_info else 0,
            'total_frames': total_frames_part,
        }
        print(f'  {part_slug}  {len(color_info)} colors  '
              f'~{color_info[0]["frame_count"] if color_info else 0} frames/color  '
              f'=  {total_frames_part} total')

    os.makedirs(OUT_DIR, exist_ok=True)
    with open(os.path.join(OUT_DIR, '_summary.json'), 'w', encoding='utf-8') as fp:
        json.dump(summary, fp, indent=2)

    total = sum(s['total_frames'] for s in summary.values())
    print(f'\nTotal parts: {len(summary)}')
    print(f'Total frames extracted: {total}')


if __name__ == '__main__':
    main()
