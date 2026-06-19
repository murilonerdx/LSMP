from PIL import Image, ImageDraw
import json, os
A = "src/main/resources/assets/liberthia"; D = "src/main/resources/data/liberthia"
def C(v): return ((v>>16)&255,(v>>8)&255,v&255,255)
def dk(v,f=0.30): return (int(((v>>16)&255)*f),int(((v>>8)&255)*f),int((v&255)*f),255)
def machine_tex(name, accent):
    im=Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.rectangle([0,0,15,15], fill=C(0x2A2E36), outline=C(0x14161C))
    for (x,y) in [(1,1),(14,1),(1,14),(14,14)]: d.point((x,y), fill=C(0x60656E))
    d.rectangle([3,3,12,12], fill=dk(accent), outline=C(0x14161C))
    d.rectangle([5,5,10,10], fill=C(accent)); d.rectangle([6,6,9,9], fill=C(min(0xFFFFFF,accent|0x404040)))
    d.point((7,7), fill=C(0xFFFFFF)); d.point((4,13), fill=C(accent)); d.point((11,13), fill=C(accent))
    im.save(f"{A}/textures/block/{name}.png")
BLOCKS=[
 ("arcane_collector",0xD060FF,"Coletor Arcano","Arcane Collector"),
 ("mana_reactor",0xE040A0,"Reator de Mana","Mana Reactor"),
 ("crystal_resonator",0x80E0FF,"Ressonador de Cristal","Crystal Resonator"),
 ("ender_condenser",0x20A080,"Condensador do End","Ender Condenser"),
 ("soul_extractor",0x5080A0,"Extrator de Almas","Soul Extractor"),
 ("blaze_reactor",0xFF8020,"Reator de Blaze","Blaze Reactor"),
 ("matter_fabricator",0x3060C0,"Fabricador de Matéria","Matter Fabricator"),
 ("circuit_assembler",0x40C080,"Montador de Circuitos","Circuit Assembler"),
 ("plate_press",0x8AA0B8,"Prensa de Placas","Plate Press"),
 ("wire_drawer",0xC08040,"Trefiladora de Fios","Wire Drawer"),
 ("gem_polisher",0x80FFFF,"Polidor de Gemas","Gem Polisher"),
 ("ingot_former",0x504070,"Moldador de Lingotes","Ingot Former"),
 ("arcane_synthesizer",0x40FFC0,"Sintetizador Arcano","Arcane Synthesizer"),
 ("flux_dynamo",0xFFD040,"Dínamo de Fluxo","Flux Dynamo"),
 ("shard_splitter",0x405080,"Divisor de Fragmentos","Shard Splitter"),
 ("cosmic_distiller",0x6030A0,"Destilador Cósmico","Cosmic Distiller"),
 ("rune_inscriber",0x2BD6D6,"Inscritor de Runas","Rune Inscriber"),
 ("singularity_core_forge",0x6A40A0,"Forja de Núcleo de Singularidade","Singularity Core Forge"),
]
lp,le={},{}
for (bid,acc,pt,en) in BLOCKS:
    machine_tex(bid,acc)
    json.dump({"variants":{"":{"model":f"liberthia:block/{bid}"}}}, open(f"{A}/blockstates/{bid}.json","w"))
    json.dump({"parent":"minecraft:block/cube_all","textures":{"all":f"liberthia:block/{bid}"}}, open(f"{A}/models/block/{bid}.json","w"))
    json.dump({"parent":f"liberthia:block/{bid}"}, open(f"{A}/models/item/{bid}.json","w"))
    json.dump({"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":f"liberthia:{bid}"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]}, open(f"{D}/loot_tables/blocks/{bid}.json","w"))
    lp[f"block.liberthia.{bid}"]=pt; le[f"block.liberthia.{bid}"]=en
for fn,extra in [("pt_br.json",lp),("en_us.json",le)]:
    p=f"{A}/lang/{fn}"; dd=json.load(open(p,encoding="utf-8")); dd.update(extra)
    json.dump(dd, open(p,"w",encoding="utf-8"), ensure_ascii=False, indent=2)
print(f"OK +{len(BLOCKS)} blocos (assets + lang)")
