"""r45: Gera particle JSONs + item model JSONs para Cosmic Horror v2."""
import os
import json

ROOT = os.path.dirname(__file__)
PART_OUT = os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets',
                       'liberthia', 'particles')
MODEL_OUT = os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets',
                        'liberthia', 'models', 'item')
os.makedirs(PART_OUT, exist_ok=True)
os.makedirs(MODEL_OUT, exist_ok=True)

PARTICLES = ['tentacle_writhe', 'vulto_shadow', 'glaring_eye_pulse',
             'cursed_pulse', 'whisper_wisp', 'demon_glyph']

ITEMS = ['cursed_effigy', 'watcher_mark', 'phantom_caller', 'vulto_lens',
         'insanity_crown', 'tendril_sigil', 'voice_curse_amulet',
         'silent_witness_cloak']

for p in PARTICLES:
    data = {"textures": [f"liberthia:{p}_{i}" for i in range(4)]}
    with open(os.path.join(PART_OUT, f'{p}.json'), 'w') as f:
        json.dump(data, f, indent=2)
    print(f'[OK] particles/{p}.json')

for it in ITEMS:
    data = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"liberthia:item/{it}"}
    }
    with open(os.path.join(MODEL_OUT, f'{it}.json'), 'w') as f:
        json.dump(data, f, indent=2)
    print(f'[OK] models/item/{it}.json')

print(f'[OK] {len(PARTICLES)} particles + {len(ITEMS)} item models')
