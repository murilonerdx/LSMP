"""Apply vanilla-style display transforms to Liberthia armor item models.

Usage: python scripts/apply_armor_display.py

The default `minecraft:item/generated` parent gives zero positional tweaks,
so armor pieces look flat/rotated badly in GUI slots, ground, and in-hand.
This script rewrites each armor item model to include a proper `display`
block matching the values vanilla uses for handheld generated items.

Idempotent: re-running leaves already-fixed files unchanged.
"""

from __future__ import annotations

import json
import os
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
MODELS_DIR = REPO / "src" / "main" / "resources" / "assets" / "liberthia" / "models" / "item"

ARMOR_SUFFIXES = ("_helmet", "_chestplate", "_leggings", "_boots")

DISPLAY_BLOCK = {
    "thirdperson_righthand": {
        "rotation": [0, 0, 0],
        "translation": [0, 3, 1],
        "scale": [0.55, 0.55, 0.55],
    },
    "thirdperson_lefthand": {
        "rotation": [0, 0, 0],
        "translation": [0, 3, 1],
        "scale": [0.55, 0.55, 0.55],
    },
    "firstperson_righthand": {
        "rotation": [0, -90, 25],
        "translation": [1.13, 3.2, 1.13],
        "scale": [0.68, 0.68, 0.68],
    },
    "firstperson_lefthand": {
        "rotation": [0, 90, -25],
        "translation": [1.13, 3.2, 1.13],
        "scale": [0.68, 0.68, 0.68],
    },
    "gui": {
        "rotation": [0, 0, 0],
        "translation": [0, 0, 0],
        "scale": [1, 1, 1],
    },
    "ground": {
        "rotation": [0, 0, 0],
        "translation": [0, 2, 0],
        "scale": [0.5, 0.5, 0.5],
    },
    "fixed": {
        "rotation": [0, 0, 0],
        "translation": [0, 0, 0],
        "scale": [1, 1, 1],
    },
}


def is_armor_model(filename: str) -> bool:
    stem = filename.lower()
    return any(stem.endswith(suf + ".json") for suf in ARMOR_SUFFIXES)


def main() -> None:
    touched = 0
    for entry in sorted(os.listdir(MODELS_DIR)):
        if not is_armor_model(entry):
            continue
        path = MODELS_DIR / entry
        with path.open("r", encoding="utf-8") as fh:
            data = json.load(fh)
        if data.get("display") == DISPLAY_BLOCK:
            continue
        data["display"] = DISPLAY_BLOCK
        with path.open("w", encoding="utf-8") as fh:
            json.dump(data, fh, indent=2)
            fh.write("\n")
        touched += 1
        print(f"updated {entry}")
    print(f"done — {touched} file(s) touched")


if __name__ == "__main__":
    main()
