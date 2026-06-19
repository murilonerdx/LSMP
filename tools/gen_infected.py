from PIL import Image, ImageDraw
import random, os
B = "src/main/resources/assets/liberthia/textures/block"
os.makedirs(B, exist_ok=True)
def C(v): return ((v >> 16) & 255, (v >> 8) & 255, v & 255, 255)

def noisy(name, base, specks, seed, veins=None):
    random.seed(seed)
    im = Image.new("RGBA", (16, 16), C(base)); d = ImageDraw.Draw(im)
    for _ in range(70):
        x, y = random.randint(0, 15), random.randint(0, 15)
        d.point((x, y), fill=C(random.choice(specks)))
    if veins:
        for _ in range(5):
            x, y = random.randint(1, 14), random.randint(1, 14)
            for _ in range(random.randint(2, 5)):
                d.point((x, y), fill=C(veins)); x += random.randint(-1, 1); y += random.randint(-1, 1)
    im.save(B + "/" + name + ".png")

# grama infectada: topo negro-esverdeado + brilho roxo
noisy("dark_infected_grass", 0x14160E, [0x0E1208, 0x1E2410, 0x2A1240, 0x301038], 11, veins=0x7030B0)
# terra infectada: marrom-escuro -> preto + veias roxas
noisy("dark_infected_dirt", 0x1A130E, [0x100A08, 0x241810, 0x2A1240], 22, veins=0x6A2AA0)
# areia infectada: bege-escurecido granulado
noisy("dark_infected_sand", 0x201A12, [0x161009, 0x2C2418, 0x3A2A40], 33)
# pedra infectada: cinza-escuro + rachaduras roxas brilhantes
noisy("dark_infected_stone", 0x14121A, [0x0C0A12, 0x201C28, 0x2A2238], 44, veins=0x9040D0)
print("texturas dos 4 blocos infectados geradas")
