"""
r180: skins de mobs (64x64 formato PLAYER, preenchidas 100% p/ não faltar membros)
+ texturas de itens (16x16).

Mobs:
  - o_observado.png        (vulto coberto de OLHOS — cresce quando observado)
  - silence_shepherd.png   (Pastor do Silêncio — figura alta encapuzada, tons mudos)
Itens:
  - ascension_seal.png     (Selo da Ascensão — selo dourado divino)
  - eternity_crown.png     (Coroa da Eternidade — coroa divina)
"""
import os, random
from PIL import Image, ImageDraw

random.seed(180)
A = "C:/Users/T-GAMER/Desktop/liberthia_mod/src/main/resources/assets/liberthia"
ENT = os.path.join(A, "textures/entity"); os.makedirs(ENT, exist_ok=True)
ITM = os.path.join(A, "textures/item");   os.makedirs(ITM, exist_ok=True)

def c(v): return max(0, min(255, int(v)))

def base_skin(name, body, dark, eye, eyes_all_over=False):
    W = H = 64
    img = Image.new("RGBA", (W, H), (0, 0, 0, 255)); px = img.load()
    for y in range(H):
        for x in range(W):
            f = 1.12 - 0.30 * (y / float(H))
            jit = random.randint(-10, 10)
            r, g, b = body
            if random.random() < 0.14:
                r, g, b = dark
            px[x, y] = (c(r*f+jit), c(g*f+jit), c(b*f+jit), 255)
    # rosto na FACE (UV 8,8..15,15)
    for yy in range(8, 16):
        for xx in range(8, 16):
            px[xx, yy] = (c(dark[0]*0.6), c(dark[1]*0.6), c(dark[2]*0.6), 255)
    for (ex, ey) in [(10, 11), (13, 11)]:
        px[ex, ey] = eye; px[ex, ey+1] = (c(eye[0]*0.5), c(eye[1]*0.5), c(eye[2]*0.5), 255)
    # olhos espalhados pelo corpo inteiro (O Observado)
    if eyes_all_over:
        for _ in range(40):
            x, y = random.randint(0, 63), random.randint(16, 63)
            px[x, y] = eye
            if x+1 < 64: px[x+1, y] = (c(eye[0]*0.4), c(eye[1]*0.4), c(eye[2]*0.4), 255)
    img.save(os.path.join(ENT, name + ".png")); print("OK", name + ".png")

def gen_ascension_seal():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    d.ellipse((2, 2, 13, 13), fill=(212, 175, 55, 255), outline=(255, 240, 180, 255))   # disco dourado
    d.ellipse((4, 4, 11, 11), outline=(120, 90, 20, 255))
    # sigilo (cruz/asa estilizada)
    d.line((8, 4, 8, 11), fill=(120, 90, 20, 255)); d.line((5, 7, 11, 7), fill=(120, 90, 20, 255))
    d.point((8, 2), fill=(255, 255, 235, 255)); d.point((13, 4), fill=(255, 255, 235, 255))
    img.save(os.path.join(ITM, "ascension_seal.png")); print("OK ascension_seal.png")

def gen_eternity_crown():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    # base da coroa
    d.rectangle((3, 9, 12, 12), fill=(230, 195, 70, 255), outline=(150, 110, 20, 255))
    # pontas
    for px0 in (3, 7, 11):
        d.polygon([(px0, 9), (px0+1, 4), (px0+2, 9)], fill=(245, 215, 110, 255))
    # gemas
    d.point((4, 11), fill=(90, 220, 255, 255)); d.point((8, 11), fill=(255, 90, 160, 255)); d.point((11, 11), fill=(120, 255, 140, 255))
    # brilho divino
    d.point((8, 3), fill=(255, 255, 245, 255))
    img.save(os.path.join(ITM, "eternity_crown.png")); print("OK eternity_crown.png")

if __name__ == "__main__":
    # O Observado — carne roxo-escura coberta de olhos amarelos que encaram
    base_skin("o_observado", body=(48, 30, 60), dark=(26, 14, 34), eye=(255, 220, 70, 255), eyes_all_over=True)
    # Pastor do Silêncio — manto cinza pálido, rosto escuro vazio (tons mudos)
    base_skin("silence_shepherd", body=(120, 122, 128), dark=(60, 62, 68), eye=(200, 210, 220, 255))
    gen_ascension_seal()
    gen_eternity_crown()
