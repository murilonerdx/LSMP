"""r176: gera emf_meter_off.png — a tela do medidor EMF APAGADA.

Base: emf_meter_0.png (estado calmo). Escurece os LEDs/tela para quase-preto,
mantendo a silhueta do aparelho. Usado como o frame "apagado" do piscar real
(a ItemProperty alterna entre o nivel e este frame).
"""
from PIL import Image
from pathlib import Path

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/liberthia/textures/item"
src = Image.open(TEX / "emf_meter_0.png").convert("RGBA")
w, h = src.size
out = Image.new("RGBA", (w, h))
sp, op = src.load(), out.load()

for y in range(h):
    for x in range(w):
        r, g, b, a = sp[x, y]
        if a == 0:
            op[x, y] = (0, 0, 0, 0)
            continue
        # escurece tudo bem forte -> "tela/luzes apagadas", mantendo o corpo visivel
        nr, ng, nb = int(r * 0.18), int(g * 0.18), int(b * 0.18)
        # piso minimo pra nao virar buraco preto puro (leve cinza-chumbo no corpo)
        lum = (r + g + b) / 3
        if lum > 24:  # corpo (nao o fundo): garante um cinza escuro visivel
            nr = max(nr, 10); ng = max(ng, 10); nb = max(nb, 12)
        op[x, y] = (nr, ng, nb, a)

out.save(TEX / "emf_meter_off.png")
print("wrote", TEX / "emf_meter_off.png")
