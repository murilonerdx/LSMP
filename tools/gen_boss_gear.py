from PIL import Image, ImageDraw
import json, os, random
A = "src/main/resources/assets/liberthia"; D = "src/main/resources/data/liberthia"
for p in ["models/item","textures/item","textures/models/armor"]: os.makedirs(f"{A}/{p}", exist_ok=True)
os.makedirs(f"{D}/recipes", exist_ok=True)
def C(v): return ((v>>16)&255,(v>>8)&255,v&255,255)

# ── armor worn-layers (64x32) ──
def armor_layer(name, base, speck, w=64):
    im=Image.new("RGBA",(w,32),(0,0,0,0)); d=ImageDraw.Draw(im); random.seed(hash(name)&0xffff)
    # preenche regiões típicas do template de armadura com cor base + ruído (cobre o suficiente)
    d.rectangle([0,0,w-1,31], fill=C(base))
    for _ in range(int(w*32*0.18)):
        x,y=random.randint(0,w-1),random.randint(0,31); d.point((x,y), fill=C(speck))
    im.save(f"{A}/textures/models/armor/{name}.png")
armor_layer("astral_layer_1",0x1A2050,0x88CCFF); armor_layer("astral_layer_2",0x1A2050,0x88CCFF)
armor_layer("parasitic_layer_1",0x162812,0x9AE060); armor_layer("parasitic_layer_2",0x162812,0x9AE060)

# ── item icons 16x16 ──
def helm(c,a):
    im=Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.rectangle([3,3,12,9], fill=C(c), outline=C(a)); d.rectangle([3,9,4,12], fill=C(c)); d.rectangle([11,9,12,12], fill=C(c))
    d.line([5,6,10,6], fill=C(a)); return im
def chest(c,a):
    im=Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.rectangle([4,3,11,13], fill=C(c), outline=C(a)); d.rectangle([2,3,3,8], fill=C(c)); d.rectangle([12,3,13,8], fill=C(c)); return im
def legs(c,a):
    im=Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.rectangle([4,2,11,7], fill=C(c), outline=C(a)); d.rectangle([4,7,6,14], fill=C(c), outline=C(a)); d.rectangle([9,7,11,14], fill=C(c), outline=C(a)); return im
def boots(c,a):
    im=Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.rectangle([3,6,6,13], fill=C(c), outline=C(a)); d.rectangle([3,12,8,14], fill=C(c))
    d.rectangle([9,6,12,13], fill=C(c), outline=C(a)); d.rectangle([9,12,14,14], fill=C(c)); return im
def staff(c,a):
    im=Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.line([4,14,11,4], fill=C(0x6B4226), width=2); d.ellipse([9,1,15,7], fill=C(c), outline=C(a)); d.point((12,4), fill=C(0xFFFFFF)); return im
def telescope(c,a):
    im=Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.line([3,12,13,4], fill=C(0xA0A0B0), width=3); d.line([3,12,13,4], fill=C(c), width=1); d.ellipse([11,2,15,6], fill=C(a)); return im
def pick(c,a):
    im=Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.line([3,3,13,3], fill=C(c), width=2); d.line([8,3,7,14], fill=C(0x6B4226), width=2); d.point((3,4),fill=C(a)); d.point((13,4),fill=C(a)); return im
def sword(c,a):
    im=Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.line([4,13,12,4], fill=C(c), width=2); d.line([3,12,5,14], fill=C(0x6B4226), width=2); d.line([10,2,13,5], fill=C(a)); return im
def axe(c,a):
    im=Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.line([6,14,9,3], fill=C(0x6B4226), width=2); d.polygon([(9,3),(13,5),(12,9),(8,8)], fill=C(c), outline=C(a)); return im
def backpack(c,a):
    im=Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.rectangle([4,5,11,14], fill=C(c), outline=C(a)); d.rectangle([5,2,10,5], fill=C(c), outline=C(a)); d.line([6,9,9,9], fill=C(a)); return im

AST=0x4A6AD0; ASTA=0xC0D8FF; PAR=0x4FA02A; PARA=0x9AE060
icons = {
 "astral_helmet":helm(AST,ASTA),"astral_chestplate":chest(AST,ASTA),"astral_leggings":legs(AST,ASTA),"astral_boots":boots(AST,ASTA),
 "parasitic_helmet":helm(PAR,PARA),"parasitic_chestplate":chest(PAR,PARA),"parasitic_leggings":legs(PAR,PARA),"parasitic_boots":boots(PAR,PARA),
 "gravity_staff":staff(0xAA00FF,0xE0A0FF),"dimensional_telescope":telescope(0x4A6AD0,0x88CCFF),
 "parasitic_pickaxe":pick(0x6A30B0,0x9AE060),"parasitic_sword":sword(0x6A30B0,0x9AE060),"parasitic_axe":axe(0x6A30B0,0x9AE060),
 "living_backpack":backpack(0x2A4018,0x9AE060),
}
HANDHELD={"gravity_staff","parasitic_pickaxe","parasitic_sword","parasitic_axe"}
for name,img in icons.items():
    img.save(f"{A}/textures/item/{name}.png")
    parent = "minecraft:item/handheld" if name in HANDHELD else "minecraft:item/generated"
    json.dump({"parent":parent,"textures":{"layer0":f"liberthia:item/{name}"}}, open(f"{A}/models/item/{name}.json","w"))

# ── lang ──
PT={"astral_helmet":"Elmo Astral","astral_chestplate":"Peitoral Astral","astral_leggings":"Calças Astrais","astral_boots":"Botas Astrais",
 "parasitic_helmet":"Elmo Parasítico","parasitic_chestplate":"Peitoral Parasítico","parasitic_leggings":"Calças Parasíticas","parasitic_boots":"Botas Parasíticas",
 "gravity_staff":"Cajado Gravitacional","dimensional_telescope":"Telescópio Dimensional","parasitic_pickaxe":"Picareta Parasítica",
 "parasitic_sword":"Espada Parasítica","parasitic_axe":"Machado Parasítico","living_backpack":"Mochila Viva"}
EN={"astral_helmet":"Astral Helmet","astral_chestplate":"Astral Chestplate","astral_leggings":"Astral Leggings","astral_boots":"Astral Boots",
 "parasitic_helmet":"Parasitic Helmet","parasitic_chestplate":"Parasitic Chestplate","parasitic_leggings":"Parasitic Leggings","parasitic_boots":"Parasitic Boots",
 "gravity_staff":"Gravity Staff","dimensional_telescope":"Dimensional Telescope","parasitic_pickaxe":"Parasitic Pickaxe",
 "parasitic_sword":"Parasitic Sword","parasitic_axe":"Parasitic Axe","living_backpack":"Living Backpack"}
for fn,mp in [("pt_br.json",PT),("en_us.json",EN)]:
    p=f"{A}/lang/{fn}"; d=json.load(open(p,encoding="utf-8"))
    for k,v in mp.items(): d[f"item.liberthia.{k}"]=v
    json.dump(d, open(p,"w",encoding="utf-8"), ensure_ascii=False, indent=2)

# ── recipes ──
def shaped(out,pattern,key,count=1):
    json.dump({"type":"minecraft:crafting_shaped","pattern":pattern,"key":key,"result":{"item":f"liberthia:{out}","count":count}}, open(f"{D}/recipes/{out}.json","w"))
def shapeless(out,items,count=1):
    json.dump({"type":"minecraft:crafting_shapeless","ingredients":[{"item":i} for i in items],"result":{"item":f"liberthia:{out}","count":count}}, open(f"{D}/recipes/{out}.json","w"))
DI={"item":"minecraft:diamond"}; CO={"item":"liberthia:constellation_core"}; HE={"item":"liberthia:hive_heart"}; LE={"item":"minecraft:leather"}
shaped("astral_helmet",["DCD","D D"],{"D":DI,"C":CO})
shaped("astral_chestplate",["D D","DCD","DDD"],{"D":DI,"C":CO})
shaped("astral_leggings",["DCD","D D","D D"],{"D":DI,"C":CO})
shaped("astral_boots",["D D","DCD"],{"D":DI,"C":CO})
shaped("parasitic_helmet",["DHD","D D"],{"D":DI,"H":HE})
shaped("parasitic_chestplate",["D D","DHD","DDD"],{"D":DI,"H":HE})
shaped("parasitic_leggings",["DHD","D D","D D"],{"D":DI,"H":HE})
shaped("parasitic_boots",["D D","DHD"],{"D":DI,"H":HE})
shapeless("gravity_staff",["liberthia:constellation_core","minecraft:blaze_rod","minecraft:blaze_rod","minecraft:stick"])
shapeless("dimensional_telescope",["liberthia:constellation_core","minecraft:spyglass","minecraft:amethyst_shard"])
shapeless("parasitic_pickaxe",["liberthia:hive_heart","minecraft:diamond_pickaxe"])
shapeless("parasitic_sword",["liberthia:hive_heart","minecraft:diamond_sword"])
shapeless("parasitic_axe",["liberthia:hive_heart","minecraft:diamond_axe"])
shaped("living_backpack",["LLL","LHL","LCL"],{"L":LE,"H":HE,"C":{"item":"minecraft:chest"}})
print("boss gear: 4 armor layers + 14 icons + models + lang + 15 recipes OK")
