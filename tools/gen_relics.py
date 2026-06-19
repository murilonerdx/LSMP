from PIL import Image, ImageDraw
import json, os, math
A = "src/main/resources/assets/liberthia"; CT = "src/main/resources/data/curios/tags/items"
os.makedirs(f"{A}/textures/item", exist_ok=True); os.makedirs(f"{A}/models/item", exist_ok=True)
def C(v): return ((v>>16)&255,(v>>8)&255,v&255,255)
PUR=0xAA00FF; PURA=0xE0A0FF

# (id, slot, draw-kind)
RELICS = [
 ("astaron_eye_relic","necklace","eye"),
 ("astaron_mind_amulet","necklace","amulet"),
 ("astaron_hunter_gauntlet","hands","gauntlet"),
 ("astaron_warden_sash","belt","sash"),
 ("astaron_void_treads","feet","boots"),
 ("astaron_mirror_ring","ring","ring"),
 ("astaron_faceless_crown","head","crown"),
 ("astaron_essence_relic","charm","orb"),
 ("astaron_void_core","bracelet","core"),
 ("astaron_star_pendant","necklace","pendant"),
]
def icon(kind):
    im=Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    if kind=="eye":
        d.ellipse([2,5,13,11], fill=C(0x150E2A), outline=C(PUR)); d.ellipse([6,5,10,11], fill=C(PURA)); d.ellipse([7,7,9,9], fill=C(0x0A0614))
    elif kind=="amulet":
        d.line([4,2,11,2], fill=C(0xC0C0C0)); d.ellipse([4,6,11,13], fill=C(PUR), outline=C(0xC0C0C0)); d.point((7,9),fill=C(0xFFFFFF))
    elif kind=="gauntlet":
        d.rectangle([4,6,11,13], fill=C(0x6A30B0), outline=C(PURA)); d.rectangle([4,3,5,6],fill=C(0x6A30B0)); d.rectangle([7,3,8,6],fill=C(0x6A30B0)); d.rectangle([10,3,11,6],fill=C(0x6A30B0))
    elif kind=="sash":
        d.line([2,12,13,4], fill=C(0x6A30B0), width=3); d.line([2,12,13,4], fill=C(PURA), width=1); d.rectangle([7,7,9,9],fill=C(0xFFD040))
    elif kind=="boots":
        d.rectangle([3,6,6,13],fill=C(0x6A30B0),outline=C(PURA)); d.rectangle([3,12,8,14],fill=C(0x6A30B0)); d.rectangle([9,6,12,13],fill=C(0x6A30B0),outline=C(PURA)); d.rectangle([9,12,14,14],fill=C(0x6A30B0))
    elif kind=="ring":
        d.ellipse([3,3,12,12], outline=C(0xFFD040), width=2); d.ellipse([6,1,9,4], fill=C(PURA))
    elif kind=="crown":
        d.polygon([(3,11),(3,6),(5,8),(8,4),(11,8),(13,6),(13,11)], fill=C(0xFFD040), outline=C(0x8A6A10)); d.point((8,9),fill=C(PUR))
    elif kind=="orb":
        d.ellipse([3,3,12,12], fill=C(PUR), outline=C(PURA)); d.ellipse([5,5,9,9], fill=C(PURA)); d.point((6,6),fill=C(0xFFFFFF))
    elif kind=="core":
        d.rectangle([4,4,11,11], fill=C(0x301050), outline=C(PUR)); d.line([8,4,8,11],fill=C(PURA)); d.line([4,8,11,8],fill=C(PURA)); d.point((8,8),fill=C(0xFFFFFF))
    else: # pendant
        d.line([5,2,10,2],fill=C(0xC0C0C0)); d.polygon([(8,4),(12,8),(8,14),(4,8)], fill=C(0x4A6AD0), outline=C(0x88CCFF)); d.point((8,8),fill=C(0xFFFFFF))
    return im

# textures + models
for (rid,slot,kind) in RELICS:
    icon(kind).save(f"{A}/textures/item/{rid}.png")
    json.dump({"parent":"minecraft:item/generated","textures":{"layer0":f"liberthia:item/{rid}"}}, open(f"{A}/models/item/{rid}.json","w"))

# slot tags
from collections import defaultdict
byslot=defaultdict(list)
for (rid,slot,_) in RELICS: byslot[slot].append(f"liberthia:{rid}")
for slot,ids in byslot.items():
    p=f"{CT}/{slot}.json"; data=json.load(open(p,encoding="utf-8"))
    for i in ids:
        if i not in data["values"]: data["values"].append(i)
    json.dump(data, open(p,"w",encoding="utf-8"), ensure_ascii=False, indent=2)

# lang
PT={"astaron_eye_relic":"Olho de Astaron","astaron_mind_amulet":"Amuleto da Mente Blindada","astaron_hunter_gauntlet":"Luva do Caçador Cósmico",
 "astaron_warden_sash":"Cinto do Guardião","astaron_void_treads":"Botas do Vazio","astaron_mirror_ring":"Anel de Reflexão",
 "astaron_faceless_crown":"Coroa dos Sem Face","astaron_essence_relic":"Essência de Astaron","astaron_void_core":"Núcleo do Vazio","astaron_star_pendant":"Pingente Estelar"}
EN={"astaron_eye_relic":"Eye of Astaron","astaron_mind_amulet":"Shielded Mind Amulet","astaron_hunter_gauntlet":"Cosmic Hunter's Gauntlet",
 "astaron_warden_sash":"Warden's Sash","astaron_void_treads":"Void Treads","astaron_mirror_ring":"Mirror Ring",
 "astaron_faceless_crown":"Crown of the Faceless","astaron_essence_relic":"Astaron's Essence","astaron_void_core":"Void Core","astaron_star_pendant":"Star Pendant"}
for fn,mp in [("pt_br.json",PT),("en_us.json",EN)]:
    p=f"{A}/lang/{fn}"; d=json.load(open(p,encoding="utf-8"))
    for k,v in mp.items(): d[f"item.liberthia.{k}"]=v
    json.dump(d, open(p,"w",encoding="utf-8"), ensure_ascii=False, indent=2)
print("relics: 10 icons+models, slot tags, lang OK")
