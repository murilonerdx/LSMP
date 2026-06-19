"""r51: Gera model JSONs dos 10 admin artifacts."""
import os, json

ROOT = os.path.dirname(__file__)
MODEL_OUT = os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets',
                        'liberthia', 'models', 'item')

ITEMS = ['black_veil', 'tendril_crown', 'false_sun', 'mirror_pulse', 'silent_bell',
         'open_eye', 'thread_of_distance', 'flesh_signal', 'deep_water', 'audience_mark']

for it in ITEMS:
    with open(os.path.join(MODEL_OUT, f'{it}.json'), 'w') as f:
        json.dump({"parent": "minecraft:item/generated",
                   "textures": {"layer0": f"liberthia:item/{it}"}}, f, indent=2)
    print(f'[OK] {it}')
