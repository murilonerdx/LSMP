#!/usr/bin/env python3
"""r163: Decorative block patches in Spirit World + class spawn eggs prep.

Generates:
- configured_feature/decorative_patch_*.json — patches de spirit_stone, soul_brick,
  astral_lantern, halo_marble, ethereal_stone scattered through biome
- placed_feature/decorative_patch_*.json — rarity placements
- Updates biome spirit_world.json features array slot 7 (UNDERGROUND_DECORATION)
  to include these patches
"""
import json
from pathlib import Path

ROOT = Path(r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources")
CONF_DIR = ROOT / "data" / "liberthia" / "worldgen" / "configured_feature"
PLACED_DIR = ROOT / "data" / "liberthia" / "worldgen" / "placed_feature"
BIOME_DIR = ROOT / "data" / "liberthia" / "worldgen" / "biome"


# Decorative blocks to scatter — small patches replacing stone
DECORATIVE = {
    # block_id: (patch_size, count_per_chunk, y_min, y_max)
    "spirit_stone":         (16, 5, -30, 80),
    "soul_brick":           (8,  2, -10, 50),
    "halo_marble":          (12, 3, 20, 80),
    "halo_marble_bricks":   (10, 2, 30, 80),
    "ethereal_stone":       (14, 4, 0, 60),
    "ethereal_stone_bricks":(8,  2, 0, 60),
    "sanctum_ward":         (4,  1, 40, 80),  # rare
    "astral_lantern":       (3,  1, 30, 80),  # very rare, top-area
}


def main():
    for block, (size, count, y_min, y_max) in DECORATIVE.items():
        # Configured feature — disk-like patch replacing stone
        cf = {
            "type": "minecraft:disk",
            "config": {
                "state_provider": {
                    "type": "minecraft:simple_state_provider",
                    "state": {"Name": f"liberthia:{block}"}
                },
                "target": {
                    "type": "minecraft:tag_match",
                    "tag": "minecraft:base_stone_overworld"
                },
                "radius": {"type": "minecraft:uniform",
                           "value": {"min_inclusive": max(1, size // 4), "max_inclusive": max(2, size // 2)}},
                "half_height": 1,
                "target_states": []
            }
        }
        # Use ore-style feature instead for simpler placement
        cf = {
            "type": "minecraft:ore",
            "config": {
                "size": size,
                "discard_chance_on_air_exposure": 0.2,
                "targets": [
                    {
                        "target": {"predicate_type": "minecraft:tag_match",
                                   "tag": "minecraft:stone_ore_replaceables"},
                        "state": {"Name": f"liberthia:{block}"}
                    },
                    {
                        "target": {"predicate_type": "minecraft:tag_match",
                                   "tag": "minecraft:deepslate_ore_replaceables"},
                        "state": {"Name": f"liberthia:{block}"}
                    }
                ]
            }
        }
        feat_id = f"deco_{block}"
        (CONF_DIR / f"{feat_id}.json").write_text(
            json.dumps(cf, indent=2), encoding="utf-8")

        pf = {
            "feature": f"liberthia:{feat_id}",
            "placement": [
                {"type": "minecraft:count", "count": count},
                {"type": "minecraft:in_square"},
                {
                    "type": "minecraft:height_range",
                    "height": {
                        "type": "minecraft:uniform",
                        "min_inclusive": {"absolute": y_min},
                        "max_inclusive": {"absolute": y_max}
                    }
                },
                {"type": "minecraft:biome"}
            ]
        }
        (PLACED_DIR / f"{feat_id}.json").write_text(
            json.dumps(pf, indent=2), encoding="utf-8")
        print(f"  OK deco_{block} (count={count}, Y {y_min}..{y_max})")

    # Update biome — add deco patches to UNDERGROUND_DECORATION (slot 7)
    bio_path = BIOME_DIR / "spirit_world.json"
    bio = json.loads(bio_path.read_text(encoding="utf-8"))
    bio["features"][7] = [f"liberthia:deco_{b}" for b in DECORATIVE]
    bio_path.write_text(json.dumps(bio, indent=2), encoding="utf-8")
    print(f"\nOK Updated biome — {len(DECORATIVE)} decorative patches added to spirit_world")


if __name__ == "__main__":
    main()
