"""r46: Gera os 10 item model JSONs pros Eldritch Artifacts."""
import os
import json

ROOT = os.path.dirname(__file__)
MODEL_OUT = os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets',
                        'liberthia', 'models', 'item')

ITEMS = ['watching_eye', 'black_signal_radio', 'hollow_mask', 'flesh_lantern',
         'false_totem', 'infection_needle', 'book_impossible', 'mimic_heart',
         'red_tape', 'null_bell']

for it in ITEMS:
    data = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"liberthia:item/{it}"}
    }
    with open(os.path.join(MODEL_OUT, f'{it}.json'), 'w') as f:
        json.dump(data, f, indent=2)
    print(f'[OK] models/item/{it}.json')

print(f'[OK] {len(ITEMS)} item models gerados')
