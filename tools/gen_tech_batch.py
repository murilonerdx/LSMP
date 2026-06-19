from PIL import Image, ImageDraw
import json, os, random

A = "src/main/resources/assets/liberthia"
D = "src/main/resources/data/liberthia"
for p in ["blockstates", "models/block", "models/item", "textures/block", "textures/item"]:
    os.makedirs(f"{A}/{p}", exist_ok=True)
os.makedirs(f"{D}/loot_tables/blocks", exist_ok=True)

def C(v, a=255): return ((v >> 16) & 255, (v >> 8) & 255, v & 255, a)
def dark(v, f=0.45): return ((int(((v>>16)&255)*f)), int(((v>>8)&255)*f), int((v&255)*f), 255)

# ---- (id, accentColor, nomePT, nomeEN) ----
BLOCKS = [
 ("quantum_pulverizer",0x60A0FF,"Pulverizador Quântico","Quantum Pulverizer"),
 ("crystallization_chamber",0x80E0FF,"Câmara de Cristalização","Crystallization Chamber"),
 ("coil_winder",0xC08040,"Enrolador de Bobinas","Coil Winder"),
 ("matter_condenser_t",0x6030A0,"Condensador de Matéria","Matter Condenser"),
 ("nano_assembler",0x40FFC0,"Nano-Montador","Nano Assembler"),
 ("energy_distiller",0xFF4040,"Destilador de Energia","Energy Distiller"),
 ("flux_forge",0xFFA030,"Forja de Fluxo","Flux Forge"),
 ("mana_crystallizer",0xE060E0,"Cristalizador de Mana","Mana Crystallizer"),
 ("arcane_circuit_printer",0x40C0FF,"Impressora de Circuitos Arcanos","Arcane Circuit Printer"),
 ("dark_alloy_smelter",0x6A40A0,"Fundidor de Liga Escura","Dark Alloy Smelter"),
 ("photon_infuser",0xFFF080,"Infusor de Fótons","Photon Infuser"),
 ("matter_replicator",0x2080FF,"Replicador de Matéria","Matter Replicator"),
 ("crystal_grower",0xB060FF,"Cultivador de Cristais","Crystal Grower"),
 ("essence_compressor",0xFF6060,"Compressor de Essência","Essence Compressor"),
 ("rune_etcher",0x2BD6D6,"Gravador de Runas","Rune Etcher"),
 ("singularity_press",0x8040C0,"Prensa de Singularidade","Singularity Press"),
]
ITEMS = [
 ("quantum_dust",0x80C0FF,"Pó Quântico","Quantum Dust"),
 ("photon_crystal",0xC0F0FF,"Cristal de Fóton","Photon Crystal"),
 ("resonance_coil",0xE0A040,"Bobina de Ressonância","Resonance Coil"),
 ("dark_matter_cell_core",0x9030D0,"Núcleo de Célula de Matéria Escura","Dark Matter Cell Core"),
 ("assembled_matrix",0x40FFC0,"Matriz Montada","Assembled Matrix"),
 ("energy_essence",0xFF5050,"Essência de Energia","Energy Essence"),
 ("mana_crystal",0xE060E0,"Cristal de Mana","Mana Crystal"),
 ("void_alloy_ingot",0x504070,"Lingote de Liga do Vazio","Void Alloy Ingot"),
]

def machine_tex(name, accent):
    im = Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    base=0x2A2E36
    d.rectangle([0,0,15,15], fill=C(base), outline=C(0x14161C))
    # chapas/parafusos
    for (x,y) in [(1,1),(14,1),(1,14),(14,14)]: d.point((x,y), fill=C(0x60656E))
    # faceplate escura
    d.rectangle([3,3,12,12], fill=dark(accent,0.30), outline=C(0x14161C))
    # núcleo brilhante
    d.rectangle([5,5,10,10], fill=C(accent))
    d.rectangle([6,6,9,9], fill=C(min(0xFFFFFF, accent|0x404040)))
    d.point((7,7), fill=C(0xFFFFFF))
    # leds inferiores
    d.point((4,13), fill=C(accent)); d.point((11,13), fill=C(accent))
    im.save(f"{A}/textures/block/{name}.png")

def item_tex(name, col):
    im = Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    random.seed(hash(name)&0xffff)
    if "ingot" in name:
        d.polygon([(3,10),(5,8),(13,8),(11,10),(11,12),(3,12)], fill=C(col), outline=C(dark(col,0.5)[0]<<16|dark(col,0.5)[1]<<8|dark(col,0.5)[2]))
        d.line([4,11,10,11], fill=C(min(0xFFFFFF,col|0x303030)))
    elif "crystal" in name or "photon" in name:
        d.polygon([(8,1),(12,7),(8,15),(4,7)], fill=C(col), outline=C(0xFFFFFF))
        d.line([8,3,8,13], fill=C(min(0xFFFFFF,col|0x505050)))
    elif "coil" in name:
        for yy in range(4,13,2): d.line([4,yy,12,yy], fill=C(col), width=1)
        d.rectangle([3,3,12,13], outline=C(dark(col,0.6)[0]<<16|dark(col,0.6)[1]<<8|dark(col,0.6)[2]))
    elif "dust" in name:
        for _ in range(40):
            x,y=random.randint(2,13),random.randint(2,13); d.point((x,y), fill=C(col if random.random()>0.4 else min(0xFFFFFF,col|0x303030)))
    else: # core/matrix/essence — orbe
        d.ellipse([3,3,12,12], fill=C(col), outline=C(dark(col,0.5)[0]<<16|dark(col,0.5)[1]<<8|dark(col,0.5)[2]))
        d.ellipse([5,5,9,9], fill=C(min(0xFFFFFF,col|0x505050)))
        d.point((6,6), fill=C(0xFFFFFF))
    im.save(f"{A}/textures/item/{name}.png")

lang_pt, lang_en = {}, {}
for (bid,acc,pt,en) in BLOCKS:
    machine_tex(bid,acc)
    json.dump({"variants":{"":{"model":f"liberthia:block/{bid}"}}}, open(f"{A}/blockstates/{bid}.json","w"))
    json.dump({"parent":"minecraft:block/cube_all","textures":{"all":f"liberthia:block/{bid}"}}, open(f"{A}/models/block/{bid}.json","w"))
    json.dump({"parent":f"liberthia:block/{bid}"}, open(f"{A}/models/item/{bid}.json","w"))
    json.dump({"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":f"liberthia:{bid}"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]}, open(f"{D}/loot_tables/blocks/{bid}.json","w"))
    lang_pt[f"block.liberthia.{bid}"]=pt; lang_en[f"block.liberthia.{bid}"]=en
for (iid,col,pt,en) in ITEMS:
    item_tex(iid,col)
    json.dump({"parent":"minecraft:item/generated","textures":{"layer0":f"liberthia:item/{iid}"}}, open(f"{A}/models/item/{iid}.json","w"))
    lang_pt[f"item.liberthia.{iid}"]=pt; lang_en[f"item.liberthia.{iid}"]=en

# merge lang into existing files (json load -> update -> dump)
for fn, extra in [("pt_br.json",lang_pt),("en_us.json",lang_en)]:
    p=f"{A}/lang/{fn}"; data=json.load(open(p,encoding="utf-8")); data.update(extra)
    json.dump(data, open(p,"w",encoding="utf-8"), ensure_ascii=False, indent=2)

print(f"OK: {len(BLOCKS)} blocos + {len(ITEMS)} itens (texturas, blockstate/model/item/loot, lang pt+en)")
