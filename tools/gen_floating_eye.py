from PIL import Image, ImageDraw
import math, random

# Atlas 64x64 casando com FloatingEyeModel:
#   eyeball 10x10x10 @ texOffs(0,0)   -> regiao (0,0)-(40,20)
#   iris    6x6x1    @ texOffs(0,22)  -> regiao (0,22)-(14,29), face frontal (1,23)-(7,29)
#   tentacle 2x9x2   @ texOffs(44,22) -> regiao (44,22)-(52,33)
A = "src/main/resources/assets/liberthia/textures/entity"
S = 64
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
random.seed(187)

# ---- ESCLERA (globo) : (0,0)-(40,20) pale veiada ----
for y in range(0, 20):
    for x in range(0, 40):
        # base creme com leve sombreamento vertical
        sh = 1.0 - (y / 20.0) * 0.18
        r = int(224 * sh); g = int(216 * sh); b = int(202 * sh)
        img.putpixel((x, y), (r, g, b, 255))
# veias vermelhas finas
for _ in range(26):
    x0 = random.randint(0, 39); y0 = random.randint(0, 19)
    ang = random.random() * math.tau
    ln = random.randint(4, 11)
    x1 = max(0, min(39, int(x0 + math.cos(ang) * ln)))
    y1 = max(0, min(19, int(y0 + math.sin(ang) * ln)))
    col = random.choice([(168, 38, 38), (150, 28, 28), (190, 60, 60)])
    d.line([x0, y0, x1, y1], fill=col + (255,), width=1)

# ---- IRIS : preencher regiao toda, face frontal (1,23)-(7,29) com olho dourado ----
for y in range(22, 29):
    for x in range(0, 14):
        img.putpixel((x, y), (150, 70, 14, 255))  # aro base p/ faces laterais finas
# disco da iris na face frontal 6x6 centrado em (4,26)
cx, cy = 4.0, 26.0
for y in range(23, 29):
    for x in range(1, 7):
        dist = math.hypot(x - cx + 0.0, (y - cy) * 1.0)
        if dist <= 3.0:
            t = dist / 3.0  # 0 centro -> 1 borda
            if dist <= 1.2:
                col = (8, 6, 10, 255)              # pupila preta
            else:
                # iris radial: ambar -> laranja queimado
                r = int(255 - 70 * t); g = int(205 - 130 * t); b = int(60 - 40 * t)
                col = (max(0, r), max(0, g), max(0, b), 255)
            img.putpixel((x, y), col)
img.putpixel((3, 24), (255, 255, 255, 255))   # glint
img.putpixel((2, 24), (235, 245, 255, 255))

# ---- TENTACULOS : (44,22)-(52,33) carne roxo-vinho ----
for y in range(22, 33):
    for x in range(44, 52):
        v = (y - 22) / 11.0
        r = int(96 - 30 * v); g = int(24 + 6 * v); b = int(58 - 18 * v)
        img.putpixel((x, y), (max(0, r), max(0, g), max(0, b), 255))
# espinha clara no meio (faces frontais ~x 46..48)
for y in range(22, 33):
    img.putpixel((46, y), (150, 60, 95, 255))
    img.putpixel((47, y), (132, 48, 84, 255))

img.save(A + "/floating_eye.png")
print("floating_eye.png gerado (64x64 UV: esclera veiada + iris dourada + tentaculos)")
