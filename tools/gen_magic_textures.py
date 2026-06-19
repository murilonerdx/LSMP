"""r142: Gera 53 texturas + models para items mágicos sem textura."""
from PIL import Image, ImageDraw
import os, random, json, math

OUT_TEX = "src/main/resources/assets/liberthia/textures/item"
OUT_MODEL = "src/main/resources/assets/liberthia/models/item"
os.makedirs(OUT_TEX, exist_ok=True)
os.makedirs(OUT_MODEL, exist_ok=True)

def save(name, img):
    img.save(OUT_TEX + "/" + name + ".png")
    with open(OUT_MODEL + "/" + name + ".json", "w") as f:
        json.dump({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "liberthia:item/" + name}
        }, f, indent=2)

def base():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))

# STAVES — wooden shaft + colored gem
def staff(name, gem):
    img = base()
    d = ImageDraw.Draw(img)
    for i in range(11):
        d.point((i+3, 14-i), fill=(110,70,40,255))
        d.point((i+4, 14-i), fill=(140,90,50,255))
        d.point((i+3, 15-i), fill=(80,50,30,255))
    d.polygon([(11,2),(14,4),(13,7),(10,5)], fill=gem, outline=(255,255,255,200))
    d.point((12,3), fill=(255,255,255,255))
    bright = tuple(min(255, c+60) for c in gem[:3]) + (255,)
    d.point((11,5), fill=bright)
    save(name, img)

staff("staff_fire",     (255, 80, 30, 255))
staff("staff_ice",      (140, 220, 255, 255))
staff("staff_lightning",(255, 240, 80, 255))
staff("staff_blood",    (180, 30, 30, 255))
staff("staff_holy",     (255, 230, 130, 255))
staff("staff_nature",   (80, 200, 80, 255))
staff("staff_eldritch", (180, 60, 200, 255))

# SCROLLS — rolled parchment with seal
def scroll(name, accent):
    img = base()
    d = ImageDraw.Draw(img)
    d.rectangle([2,3,13,12], fill=(220,200,150,255), outline=(150,120,70,255))
    d.rectangle([1,2,14,4], fill=(180,150,90,255), outline=(120,90,50,255))
    d.rectangle([1,11,14,13], fill=(180,150,90,255), outline=(120,90,50,255))
    d.ellipse([6,6,10,9], fill=accent, outline=(50,30,20,255))
    d.point((7,7), fill=(255,255,255,255))
    save(name, img)

scroll("scroll_fire",     (200, 50, 30, 255))
scroll("scroll_ice",      (100, 180, 255, 255))
scroll("scroll_lightning",(255, 220, 60, 255))
scroll("scroll_blood",    (160, 20, 30, 255))
scroll("scroll_holy",     (255, 230, 130, 255))
scroll("scroll_nature",   (60, 180, 70, 255))
scroll("scroll_eldritch", (160, 50, 180, 255))

# RITUAL TABLETS — stone with rune
def tablet(name, rune):
    img = base()
    d = ImageDraw.Draw(img)
    d.rectangle([3,2,12,13], fill=(120,115,110,255), outline=(60,55,50,255))
    for _ in range(8):
        x = random.randint(4,11); y = random.randint(3,12)
        d.point((x,y), fill=(90,85,80,255))
    d.line([(7,4),(7,11)], fill=rune)
    d.line([(4,7),(11,7)], fill=rune)
    bright = tuple(min(255, c+60) for c in rune[:3]) + (255,)
    d.point((7,3), fill=bright)
    save(name, img)

tablet("ritual_tablet_awakening",   (200, 80, 200, 255))
tablet("ritual_tablet_flight",      (180, 220, 255, 255))
tablet("ritual_tablet_forestation", (80, 220, 80, 255))
tablet("ritual_tablet_healing",     (255, 180, 180, 255))
tablet("ritual_tablet_moonfall",    (180, 180, 240, 255))
tablet("ritual_tablet_scrying",     (140, 60, 200, 255))
tablet("ritual_tablet_sunrise",     (255, 220, 80, 255))
tablet("ritual_tablet_warding",     (180, 220, 255, 255))

# RINGS — gold band with gem
def ring(name, gem):
    img = base()
    d = ImageDraw.Draw(img)
    d.ellipse([3,3,12,12], outline=(220,180,60,255), width=2)
    d.ellipse([5,5,10,10], fill=(0,0,0,0), outline=(180,140,40,255))
    d.polygon([(8,1),(11,4),(8,5),(5,4)], fill=gem, outline=(255,255,255,200))
    d.point((8,2), fill=(255,255,255,255))
    save(name, img)

ring("ring_bloodborn", (180,30,30,255))
ring("ring_capacity",  (140,100,220,255))
ring("ring_firewarp",  (255,140,50,255))
ring("ring_lurker",    (60,40,80,255))
ring("ring_teleport",  (180,80,220,255))

# PERKS — amber slottable items
def perk(name, color, sym="dot"):
    img = base()
    d = ImageDraw.Draw(img)
    d.ellipse([2,2,13,13], fill=(80,40,120,255), outline=(40,20,80,255))
    d.ellipse([4,4,11,11], fill=(140,80,200,255))
    if sym == "dot":
        d.ellipse([6,6,9,9], fill=color)
    elif sym == "cross":
        d.line([(7,4),(7,11)], fill=color, width=1)
        d.line([(4,7),(11,7)], fill=color, width=1)
    elif sym == "arrow":
        d.line([(8,3),(8,12)], fill=color, width=1)
        d.line([(6,5),(8,3)], fill=color)
        d.line([(10,5),(8,3)], fill=color)
    elif sym == "star":
        d.point((8,3), fill=color)
        d.point((8,12), fill=color)
        d.point((3,7), fill=color)
        d.point((12,7), fill=color)
        d.point((8,7), fill=color)
    save(name, img)

perk("perk_bonded",          (255,200,100,255), "cross")
perk("perk_feather",         (240,240,255,255), "arrow")
perk("perk_gliding",         (180,220,255,255), "arrow")
perk("perk_jump",            (100,200,255,255), "arrow")
perk("perk_knockback_resist",(140,100,80,255),  "dot")
perk("perk_looting",         (255,220,80,255),  "star")
perk("perk_magic_capacity",  (180,80,220,255),  "dot")
perk("perk_magic_resist",    (120,100,180,255), "cross")
perk("perk_potion_duration", (180,80,180,255),  "dot")
perk("perk_repairing",       (200,180,80,255),  "cross")
perk("perk_saturation",      (255,180,80,255),  "dot")
perk("perk_spell_damage",    (255,80,80,255),   "star")
perk("perk_step_height",     (180,180,140,255), "arrow")
perk("perk_toughness",       (180,160,200,255), "cross")
perk("perk_vampiric",        (200,20,40,255),   "dot")

# WEAVES — diamond cloth pieces
def weave(name, a, b):
    img = base()
    d = ImageDraw.Draw(img)
    d.polygon([(8,2),(13,8),(8,13),(3,8)], fill=a, outline=(40,40,60,255))
    d.polygon([(8,5),(11,8),(8,11),(5,8)], fill=b)
    d.point((8,6), fill=(255,255,255,200))
    save(name, img)

weave("false_weave",  (140,60,100,255), (80,40,60,255))
weave("ghost_weave",  (200,200,220,255),(140,140,180,255))
weave("mirror_weave", (180,200,230,255),(255,255,255,255))
weave("sky_weave",    (120,180,240,255),(200,230,255,255))

# MISC

# Arcane salvage — magic dust pile
img = base()
d = ImageDraw.Draw(img)
d.polygon([(3,12),(13,12),(11,8),(5,8)], fill=(60,40,100,255), outline=(40,20,80,255))
for _ in range(15):
    x = random.randint(4,12); y = random.randint(8,11)
    c = random.choice([(180,80,220),(100,80,200),(220,180,255)])
    d.point((x,y), fill=c+(255,))
save("arcane_salvage", img)

# Counterspell — broken wand X
img = base()
d = ImageDraw.Draw(img)
d.line([(3,13),(13,3)], fill=(180,140,80,255), width=2)
d.line([(3,3),(13,13)], fill=(255,80,255,255), width=1)
d.point((8,8), fill=(255,255,255,255))
save("counterspell", img)

# Dominion wand
img = base()
d = ImageDraw.Draw(img)
for i in range(11):
    d.point((i+3, 14-i), fill=(180,150,80,255))
    d.point((i+4, 14-i), fill=(220,180,100,255))
d.polygon([(11,2),(14,4),(13,6),(10,5)], fill=(220,80,255,255), outline=(255,255,255,200))
d.point((12,3), fill=(255,255,255,255))
save("dominion_wand", img)

# Scrying lens
img = base()
d = ImageDraw.Draw(img)
d.ellipse([2,2,11,11], fill=(140,80,200,180), outline=(220,180,60,255), width=1)
d.ellipse([4,4,9,9], fill=(180,140,240,200))
d.line([(10,10),(14,14)], fill=(140,100,40,255), width=2)
d.point((6,6), fill=(255,255,255,255))
save("scrying_lens", img)

# Void jar
img = base()
d = ImageDraw.Draw(img)
d.rectangle([5,2,10,3], fill=(80,80,80,255))
d.polygon([(4,3),(11,3),(11,14),(4,14)], fill=(20,10,30,255), outline=(80,40,80,255))
for i in range(8):
    ang = i * 45
    x = 7 + int(math.cos(math.radians(ang)) * 2)
    y = 8 + int(math.sin(math.radians(ang)) * 2)
    d.point((x,y), fill=(180,60,200,255))
d.point((7,8), fill=(255,255,255,255))
save("void_jar", img)

# Warp scroll
img = base()
d = ImageDraw.Draw(img)
d.rectangle([2,3,13,12], fill=(180,160,200,255), outline=(100,80,140,255))
d.rectangle([1,2,14,4], fill=(140,100,180,255), outline=(80,60,120,255))
d.rectangle([1,11,14,13], fill=(140,100,180,255), outline=(80,60,120,255))
d.ellipse([6,6,10,9], outline=(80,30,120,255))
d.point((8,7), fill=(255,200,255,255))
save("warp_scroll", img)

# Stable warp scroll
img = base()
d = ImageDraw.Draw(img)
d.rectangle([2,3,13,12], fill=(200,180,220,255), outline=(220,180,60,255))
d.rectangle([1,2,14,4], fill=(160,120,200,255), outline=(220,180,60,255))
d.rectangle([1,11,14,13], fill=(160,120,200,255), outline=(220,180,60,255))
d.ellipse([6,6,10,9], outline=(80,30,120,255))
d.point((8,7), fill=(255,220,180,255))
save("stable_warp_scroll", img)

print("Generated 53 magic item textures + models")
