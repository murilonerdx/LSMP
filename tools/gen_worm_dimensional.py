from PIL import Image
import random
# 32x16 casando com BloodWormModel:
#   head 5x4x4 @ (0,0) -> (0,0)-(18,8)
#   seg  ~4x4 @ (0,8)  -> (0,8)-(16,16)
#   tail 2x2x4 @ (16,8)-> (16,8)-(28,14)
A = "src/main/resources/assets/liberthia/textures/entity"
S = 32; H = 16
img = Image.new("RGBA", (S, H), (0, 0, 0, 0))
random.seed(487)
def fill(x0,y0,x1,y1,col):
    for y in range(y0,y1):
        for x in range(x0,x1):
            img.putpixel((x,y), col+(255,))
def stars(x0,y0,x1,y1,n):
    for _ in range(n):
        x=random.randint(x0,x1-1); y=random.randint(y0,y1-1)
        c=random.choice([(180,210,255),(255,255,255),(120,255,235),(200,160,255)])
        img.putpixel((x,y), c+(255,))
# corpo indigo profundo (espaço)
fill(0,0,18,8,(42,30,72))      # cabeça
fill(0,8,16,16,(36,26,64))     # segmentos
fill(16,8,28,14,(30,22,54))    # cauda
stars(0,0,28,16,40)
# aneis ciano luminosos entre segmentos (faces frontais)
for x in range(0,16):
    img.putpixel((x,8),(90,255,235,255))
    if x<18: img.putpixel((x,0),(120,220,255,255))
img.save(A+"/worm_dimensional.png")
print("worm_dimensional.png gerado (32x16 cosmico: indigo+estrelas+aneis ciano)")
