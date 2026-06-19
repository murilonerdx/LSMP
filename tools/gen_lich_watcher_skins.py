"""
r179: skins 64x64 (formato PLAYER) que faltavam/estavam bugadas:
  - lich_stalker.png  (Espreitador do Lich — robe escuro, olhos cianos)
  - lich_hunter.png   (Caçador do Lich — esqueleto osso, olhos vermelhos)
  - loom_watcher.png  (Watcher — agora um VULTO DE PEDRA: rocha rachada + olhos)

Os lich usavam textura de esqueleto vanilla (64x32) num modelo PLAYER (64x64) →
braços/pernas (y>32) ficavam transparentes = "sem perna e braços". Aqui preenchemos
TODOS os 64x64 (toda região amostrada pelo HumanoidModel tem pixel) + pintamos rosto.
"""
import os, random
from PIL import Image

random.seed(417)
ENT = "C:/Users/T-GAMER/Desktop/liberthia_mod/src/main/resources/assets/liberthia/textures/entity"
os.makedirs(ENT, exist_ok=True)

def clamp(v): return max(0, min(255, int(v)))

def build(name, base, dark, accent, eye, stone=False):
    """base=cor principal, dark=sombra, accent=detalhe, eye=cor do olho."""
    W = H = 64
    img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    px = img.load()
    for y in range(H):
        for x in range(W):
            # sombreamento vertical leve + ruído
            t = y / float(H)
            f = 1.12 - 0.30 * t
            jit = random.randint(-10, 10)
            r, g, b = base
            if stone:
                # pedra: manchas de tons de cinza + rachaduras escuras
                v = random.random()
                if v < 0.18:
                    r, g, b = dark
                elif v < 0.30:
                    r, g, b = accent
                if random.random() < 0.06:           # rachadura
                    r, g, b = (12, 12, 14)
            else:
                if random.random() < 0.16:           # textura de tecido/osso
                    r, g, b = dark
            px[x, y] = (clamp(r * f + jit), clamp(g * f + jit), clamp(b * f + jit), 255)

    # rosto na FACE da cabeça (UV 8,8 .. 15,15) — escurece + olhos brilhantes
    for yy in range(8, 16):
        for xx in range(8, 16):
            r, g, b = dark
            px[xx, yy] = (clamp(r * 0.7), clamp(g * 0.7), clamp(b * 0.7), 255)
    for (ex, ey) in [(9, 11), (10, 11), (13, 11), (14, 11)]:
        px[ex, ey] = (eye[0], eye[1], eye[2], 255)
    # "boca"/maxilar sombrio
    for xx in range(10, 14):
        px[xx, 14] = (8, 8, 10, 255)

    img.save(os.path.join(ENT, name + ".png"))
    print("OK", name + ".png")

if __name__ == "__main__":
    # Espreitador do Lich — robe roxo-escuro, olhos ciano frios
    build("lich_stalker", base=(38, 30, 52), dark=(20, 16, 30), accent=(70, 55, 95), eye=(90, 230, 255))
    # Caçador do Lich — esqueleto osso pálido, olhos vermelho-sangue
    build("lich_hunter", base=(196, 190, 172), dark=(120, 114, 100), accent=(150, 145, 128), eye=(255, 60, 50))
    # Watcher — vulto de PEDRA: granito/musgo cinza rachado, olhos branco-frio
    build("loom_watcher", base=(98, 98, 104), dark=(56, 58, 60), accent=(132, 130, 126), eye=(210, 235, 255), stone=True)
