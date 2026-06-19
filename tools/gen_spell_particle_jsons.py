"""r42: Gera os 10 particle JSONs (spell_*.json) referenciando 4 frames cada."""
import os
import json

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources',
                   'assets', 'liberthia', 'particles')
os.makedirs(OUT, exist_ok=True)

SPELLS = [
    'fire_blast', 'ice_lance', 'void_orb', 'light_ray', 'blood_shot',
    'arcane_missile', 'earth_spike', 'lightning_bolt', 'shadow_dart', 'nature_thorn'
]

for name in SPELLS:
    data = {
        "textures": [f"liberthia:spell_{name}_{i}" for i in range(4)]
    }
    path = os.path.join(OUT, f'spell_{name}.json')
    with open(path, 'w') as f:
        json.dump(data, f, indent=2)
    print(f'[OK] {path}')

print(f'[OK] {len(SPELLS)} particle JSONs generated')
