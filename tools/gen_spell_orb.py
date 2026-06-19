"""r113: gera spell_orb.png — glow radial branco 32x32 pra vertex-color tint."""
from PIL import Image
import os, math

OUT = os.path.normpath(os.path.join(
    os.path.dirname(__file__), "..",
    "src", "main", "resources",
    "assets", "liberthia", "textures", "entity"))
os.makedirs(OUT, exist_ok=True)

SIZE = 32
img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
cx = cy = SIZE / 2 - 0.5
max_r = SIZE / 2

for y in range(SIZE):
    for x in range(SIZE):
        dx = x - cx
        dy = y - cy
        d = math.sqrt(dx*dx + dy*dy)
        if d >= max_r:
            continue
        # Gradient: a^3 nas bordas, smooth core
        f = 1.0 - (d / max_r)
        # Power curve: pico no centro, falloff suave
        alpha = int(255 * (f ** 1.5))
        # White core a alpha gradiente
        img.putpixel((x, y), (255, 255, 255, alpha))

img.save(os.path.join(OUT, "spell_orb.png"))
print(f"Generated spell_orb.png ({SIZE}x{SIZE}) at {OUT}")
