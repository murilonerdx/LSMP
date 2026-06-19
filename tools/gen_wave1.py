from PIL import Image, ImageDraw
import json, os
A = "src/main/resources/assets/liberthia"
D = "src/main/resources/data/liberthia"
ITEX = A + "/textures/item"; BTEX = A + "/textures/block"
os.makedirs(ITEX, exist_ok=True); os.makedirs(BTEX, exist_ok=True)

def C(v): return ((v >> 16) & 255, (v >> 8) & 255, v & 255, 255)
def img(): return Image.new("RGBA", (16, 16), (0, 0, 0, 0))
def R(p, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < 16 and 0 <= y < 16: p[x, y] = c

DK = C(0x1A2026); MD = C(0x2E3640); LT = C(0x5A6A7A); HI = C(0xB0C0D0)

def machine_tex(name, accent, core):
    im = img(); p = im.load()
    R(p, 0, 0, 15, 15, MD); R(p, 0, 0, 15, 0, LT); R(p, 0, 15, 15, 15, DK); R(p, 0, 0, 0, 15, LT); R(p, 15, 0, 15, 15, DK)
    R(p, 3, 3, 12, 12, DK)                       # painel
    R(p, 5, 5, 10, 10, C(core)); R(p, 6, 6, 9, 9, C(accent)); p[7, 7] = HI; p[8, 8] = HI
    R(p, 4, 4, 11, 4, C(accent))                 # barra superior (acento)
    for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)]: p[x, y] = HI  # parafusos
    im.save(BTEX + "/" + name + ".png")

machine_tex("metal_press", 0x8AA0B8, 0x33384A)
machine_tex("arcane_infuser", 0xB060E0, 0x2A1840)
machine_tex("tech_assembler", 0x40C0FF, 0x103048)
machine_tex("mana_condenser", 0xE060C0, 0x401838)
machine_tex("crystal_smelter", 0x40D0B0, 0x103838)

def plate(name, base, edge, bolt):
    im = img(); p = im.load()
    R(p, 2, 3, 13, 12, C(base)); R(p, 2, 3, 13, 3, C(edge)); R(p, 2, 12, 13, 12, DK); R(p, 2, 3, 2, 12, C(edge)); R(p, 13, 3, 13, 12, DK)
    for (x, y) in [(4, 5), (11, 5), (4, 10), (11, 10)]: p[x, y] = C(bolt)
    im.save(ITEX + "/" + name + ".png")
plate("hardened_plate", 0x8AA0B8, 0xB0C0D0, 0x3A4452)
plate("warded_plate", 0x4AC0D8, 0xB0F0FF, 0x186878)

def ingot(name, base, edge):
    im = img(); p = im.load()
    R(p, 3, 6, 12, 10, C(base)); R(p, 4, 5, 11, 5, C(edge)); R(p, 3, 10, 12, 11, DK); p[5, 7] = C(edge); p[9, 8] = C(edge)
    im.save(ITEX + "/" + name + ".png")
ingot("arcane_alloy", 0x40D0B0, 0xB0FFE8)

# mana_capacitor (cilindro)
im = img(); p = im.load()
R(p, 6, 2, 9, 3, LT); R(p, 4, 3, 11, 13, C(0x401838)); R(p, 4, 3, 11, 3, C(0xE060C0)); R(p, 4, 13, 11, 13, DK)
R(p, 5, 5, 10, 11, C(0xE060C0)); R(p, 6, 6, 9, 10, C(0xFFB0E8)); p[7, 8] = HI
im.save(ITEX + "/mana_capacitor.png")

# warded_module (chip)
im = img(); p = im.load()
R(p, 2, 2, 13, 13, C(0x10202A)); R(p, 2, 2, 13, 2, C(0x305060))
R(p, 4, 4, 11, 11, C(0x18384A)); R(p, 5, 5, 10, 10, C(0x4AC0D8)); R(p, 6, 6, 9, 9, C(0x10202A)); p[7, 7] = C(0xB0F0FF)
for x in (3, 6, 9, 12): p[x, 13] = C(0xC0A040); p[x, 2] = C(0xC0A040)
im.save(ITEX + "/warded_module.png")
print("texturas wave1 OK")

def w(path, obj): os.makedirs(os.path.dirname(path), exist_ok=True); open(path, "w", encoding="utf-8").write(json.dumps(obj, indent=2))
def II(x): return {"item": x}

# models + blockstates + loot dos 5 blocos
for n in ["metal_press", "arcane_infuser", "tech_assembler", "mana_condenser", "crystal_smelter"]:
    w(A + "/models/block/" + n + ".json", {"parent": "minecraft:block/cube_all", "textures": {"all": "liberthia:block/" + n}})
    w(A + "/models/item/" + n + ".json", {"parent": "liberthia:block/" + n})
    w(A + "/blockstates/" + n + ".json", {"variants": {"": {"model": "liberthia:block/" + n}}})
    w(D + "/loot_tables/blocks/" + n + ".json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "liberthia:" + n}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
# models dos 5 itens
for n in ["hardened_plate", "warded_plate", "mana_capacitor", "arcane_alloy", "warded_module"]:
    w(A + "/models/item/" + n + ".json", {"parent": "minecraft:item/generated", "textures": {"layer0": "liberthia:item/" + n}})

# recipes: craft das MÁQUINAS (intermediários são feitos POR máquina, sem recipe de mesa)
S = II("liberthia:steel_ingot"); E = II("liberthia:energized_steel"); CC = II("liberthia:control_circuit")
W = II("liberthia:warded_core"); K = II("liberthia:dark_matter_shard"); DM = II("liberthia:dark_matter_ingot")
WM = II("liberthia:warded_module")
PI = II("minecraft:piston"); AB = II("minecraft:amethyst_block"); BF = II("minecraft:blast_furnace")
def sh(pat, keys, res, n=1): return {"type": "minecraft:crafting_shaped", "pattern": pat, "key": keys, "result": {"item": res, "count": n}}
w(D + "/recipes/metal_press.json", sh(["SPS", "SCS", "SSS"], {"S": S, "P": PI, "C": CC}, "liberthia:metal_press"))
w(D + "/recipes/arcane_infuser.json", sh(["EWE", "WKW", "ECE"], {"E": E, "W": W, "K": K, "C": CC}, "liberthia:arcane_infuser"))
w(D + "/recipes/tech_assembler.json", sh(["ECE", "CPC", "ECE"], {"E": E, "C": CC, "P": PI}, "liberthia:tech_assembler"))
w(D + "/recipes/mana_condenser.json", sh(["EAE", "ACA", "ESE"], {"E": E, "A": AB, "C": CC, "S": S}, "liberthia:mana_condenser"))
w(D + "/recipes/crystal_smelter.json", sh(["EDE", "SFS", "EEE"], {"E": E, "D": DM, "S": S, "F": BF}, "liberthia:crystal_smelter"))

# WARDED ARMOR agora exige warded_module (cadeia multi-etapa = difícil)
w(D + "/recipes/warded_helmet.json", sh(["WMW", "W W"], {"W": W, "M": WM}, "liberthia:warded_helmet"))
w(D + "/recipes/warded_chestplate.json", sh(["W W", "WMW", "WWW"], {"W": W, "M": WM}, "liberthia:warded_chestplate"))
w(D + "/recipes/warded_leggings.json", sh(["WMW", "W W", "W W"], {"W": W, "M": WM}, "liberthia:warded_leggings"))
w(D + "/recipes/warded_boots.json", sh(["W W", "M M"], {"W": W, "M": WM}, "liberthia:warded_boots"))

for n in ["metal_press", "arcane_infuser", "tech_assembler", "mana_condenser", "crystal_smelter", "warded_helmet", "warded_chestplate", "warded_leggings", "warded_boots"]:
    json.load(open(D + "/recipes/" + n + ".json", encoding="utf-8"))
print("models+recipes wave1 OK")

# lang
pt = [("block.liberthia.metal_press", "Prensa Metálica"), ("block.liberthia.arcane_infuser", "Infusor Arcano"),
      ("block.liberthia.tech_assembler", "Montadora Tecnológica"), ("block.liberthia.mana_condenser", "Condensador de Mana"),
      ("block.liberthia.crystal_smelter", "Fundidora de Cristais"),
      ("item.liberthia.hardened_plate", "Placa Endurecida"), ("item.liberthia.warded_plate", "Placa Bastião"),
      ("item.liberthia.mana_capacitor", "Capacitor de Mana"), ("item.liberthia.arcane_alloy", "Liga Arcana"),
      ("item.liberthia.warded_module", "Módulo Bastião")]
en = [("block.liberthia.metal_press", "Metal Press"), ("block.liberthia.arcane_infuser", "Arcane Infuser"),
      ("block.liberthia.tech_assembler", "Tech Assembler"), ("block.liberthia.mana_condenser", "Mana Condenser"),
      ("block.liberthia.crystal_smelter", "Crystal Smelter"),
      ("item.liberthia.hardened_plate", "Hardened Plate"), ("item.liberthia.warded_plate", "Warded Plate"),
      ("item.liberthia.mana_capacitor", "Mana Capacitor"), ("item.liberthia.arcane_alloy", "Arcane Alloy"),
      ("item.liberthia.warded_module", "Warded Module")]
for path, kv in [(A + "/lang/pt_br.json", pt), (A + "/lang/en_us.json", en)]:
    s = open(path, encoding="utf-8").read(); anc = '"block.liberthia.sawmill":'; i = s.index(anc); j = s.index("\n", i) + 1
    add = "".join('  "%s": "%s",\n' % (k, v) for k, v in kv if ('"%s"' % k) not in s)
    s = s[:j] + add + s[j:]; open(path, "w", encoding="utf-8").write(s); json.load(open(path, encoding="utf-8"))
print("lang wave1 OK")
