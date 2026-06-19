"""
r166: Sprite sheet slicer for the Davit Masia / Pixel FX Designer pack.

Each sheet is a uniform NxM grid of 100×100 cells (per the pack README).
Some cells in the grid are completely transparent (empty frames). We:

1. Split every sheet by a 100×100 grid based on its dimensions.
2. Detect & skip cells that are 100% transparent.
3. Save each non-empty cell as `frame_NNN.png` (sequential index) into
   `assets/liberthia/textures/vfx/<sheet_name>/`.
4. Optionally downscale to TARGET_SIZE (default 64×64) for Minecraft.
5. Write a `metadata.json` per effect with frame count + original size.

Run from repo root:  python3 slice_spritesheets.py
"""
from PIL import Image
import os
import json
import re

ROOT = r'C:/Users/T-GAMER/Desktop/liberthia_mod'
SRC_DIR = os.path.join(ROOT, 'src/main/resources/assets/liberthia/newsprites')
OUT_DIR = os.path.join(ROOT, 'src/main/resources/assets/liberthia/textures/vfx')

# Per-pack constants from the README ("Frame size is 100x100px.")
CELL_W = 100
CELL_H = 100

# Final size each frame is saved at. 64x64 is a good MC default — POT, preserves
# detail, keeps memory low. Set to None to keep native 100×100.
TARGET_SIZE = (64, 64)

# Alpha threshold for "non-empty" cell detection. Anything below = treated as transparent.
ALPHA_THRESHOLD = 5
MIN_OPAQUE_PIXELS = 8  # cells with fewer opaque pixels than this are skipped


def sanitize_name(filename: str) -> str:
    """`1_magicspell_spritesheet.png` → `magicspell`."""
    base = os.path.splitext(filename)[0]
    # strip leading number_ prefix and trailing _spritesheet
    base = re.sub(r'^\d+_', '', base)
    base = re.sub(r'_spritesheet$', '', base)
    return base


def count_opaque(crop: Image.Image) -> int:
    if crop.mode != 'RGBA':
        return crop.size[0] * crop.size[1]
    alpha = crop.split()[-1]
    histo = alpha.histogram()
    return sum(histo[ALPHA_THRESHOLD + 1:])


def slice_sheet(sheet_path: str, out_subdir: str) -> dict:
    img = Image.open(sheet_path)
    if img.mode != 'RGBA':
        img = img.convert('RGBA')

    w, h = img.size
    cols = w // CELL_W
    rows = h // CELL_H

    os.makedirs(out_subdir, exist_ok=True)

    saved = 0
    grid_map = []  # records original (row, col) for each saved frame
    for r in range(rows):
        for c in range(cols):
            x0 = c * CELL_W
            y0 = r * CELL_H
            cell = img.crop((x0, y0, x0 + CELL_W, y0 + CELL_H))
            opq = count_opaque(cell)
            if opq < MIN_OPAQUE_PIXELS:
                continue
            if TARGET_SIZE is not None and TARGET_SIZE != (CELL_W, CELL_H):
                cell = cell.resize(TARGET_SIZE, Image.Resampling.LANCZOS)
            cell.save(os.path.join(out_subdir, f'frame_{saved:03d}.png'))
            grid_map.append({'index': saved, 'row': r, 'col': c, 'opaque_pixels': opq})
            saved += 1

    return {
        'source_file': os.path.basename(sheet_path),
        'sheet_size': [w, h],
        'grid': [cols, rows],
        'cell_size_native': [CELL_W, CELL_H],
        'output_frame_size': list(TARGET_SIZE) if TARGET_SIZE else [CELL_W, CELL_H],
        'frame_count': saved,
        'frames': grid_map,
    }


def main() -> None:
    if not os.path.isdir(SRC_DIR):
        raise SystemExit(f'Source directory not found: {SRC_DIR}')

    summary = {}
    sheets = sorted(f for f in os.listdir(SRC_DIR) if f.endswith('_spritesheet.png'))
    print(f'Found {len(sheets)} spritesheets')

    for sheet_file in sheets:
        name = sanitize_name(sheet_file)
        out_subdir = os.path.join(OUT_DIR, name)
        sheet_path = os.path.join(SRC_DIR, sheet_file)
        info = slice_sheet(sheet_path, out_subdir)
        meta_path = os.path.join(out_subdir, 'metadata.json')
        with open(meta_path, 'w', encoding='utf-8') as fp:
            json.dump(info, fp, indent=2)
        summary[name] = info['frame_count']
        print(f'  {name:18s}  {info["sheet_size"][0]}x{info["sheet_size"][1]}  '
              f'grid={info["grid"][0]}x{info["grid"][1]}  ->  {info["frame_count"]} frames')

    # Top-level summary
    summary_path = os.path.join(OUT_DIR, '_summary.json')
    os.makedirs(OUT_DIR, exist_ok=True)
    with open(summary_path, 'w', encoding='utf-8') as fp:
        json.dump(summary, fp, indent=2, sort_keys=True)
    print(f'\nTotal effects: {len(summary)}')
    print(f'Total frames: {sum(summary.values())}')
    print(f'Summary: {summary_path}')


if __name__ == '__main__':
    main()
