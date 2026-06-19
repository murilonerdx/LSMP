# Generates the two Abyssal Core item textures (32x32, supersampled for smooth 3D look):
#   abyssal_core.png             — Núcleo do Abismo (deep blue orb, amber sealed core)
#   abyssal_core_crystallized.png— Núcleo do Vazio Cristalizado (violet orb + amethyst facets)
#
# Rendered as a shaded glass sphere: radial gradient body, dark rim (fresnel), a bright
# specular highlight, a glowing inner core, and a soft outer halo.
from PIL import Image

OUT = "src/main/resources/assets/liberthia/textures/item"
SIZE = 32
S = 4
W = SIZE * S
CX = CY = W / 2.0
R = W * 0.42  # sphere radius


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def make(name, rim, mid, bright, core_a, core_b, halo, facets=False):
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    px = img.load()
    # specular highlight center (upper-left)
    hlx, hly = CX - R * 0.35, CY - R * 0.38
    for y in range(W):
        for x in range(W):
            dx, dy = x + 0.5 - CX, y + 0.5 - CY
            dist = (dx * dx + dy * dy) ** 0.5
            nd = dist / R
            if nd <= 1.0:
                # body: bright center → mid → dark rim (fresnel)
                if nd < 0.5:
                    col = lerp(bright, mid, nd / 0.5)
                else:
                    col = lerp(mid, rim, (nd - 0.5) / 0.5)
                # glowing inner core (pulsing seed)
                if dist < R * 0.30:
                    ct = dist / (R * 0.30)
                    col = lerp(core_b, core_a, ct)
                    col = lerp(col, bright, 0.15)
                # specular highlight
                hdx, hdy = x + 0.5 - hlx, y + 0.5 - hly
                hdist = (hdx * hdx + hdy * hdy) ** 0.5
                if hdist < R * 0.42:
                    s = 1.0 - hdist / (R * 0.42)
                    col = lerp(col, (255, 255, 255), s * s * 0.65)
                a = 255
                # soft edge antialias handled by downscale; keep crisp here
                px[x, y] = (col[0], col[1], col[2], a)
            elif nd <= 1.18:
                # outer halo glow
                s = 1.0 - (nd - 1.0) / 0.18
                px[x, y] = (halo[0], halo[1], halo[2], int(120 * s * s))

    # crystalline facets for the crystallized version (amethyst shards radiating)
    if facets:
        import math
        for k in range(6):
            ang = k * (math.pi / 3.0) + 0.3
            for t in range(int(R * 0.2), int(R * 0.95)):
                fx = CX + math.cos(ang) * t
                fy = CY + math.sin(ang) * t
                # thin bright violet facet line
                ix, iy = int(fx), int(fy)
                if 0 <= ix < W and 0 <= iy < W and px[ix, iy][3] > 0:
                    base = px[ix, iy][:3]
                    px[ix, iy] = lerp(base, (220, 150, 255), 0.5) + (255,)

    out = img.resize((SIZE, SIZE), Image.LANCZOS)
    out.save(f"{OUT}/{name}.png")
    print("wrote", name + ".png")


# ── Núcleo do Abismo: azul profundo + núcleo âmbar (selado) ──
make("abyssal_core",
     rim=(6, 14, 40),        # deep navy rim
     mid=(20, 60, 140),      # ocean blue body
     bright=(90, 170, 240),  # bright cyan-blue
     core_a=(255, 190, 60),  # amber sealed core (outer)
     core_b=(255, 235, 150), # amber hot center
     halo=(60, 140, 230))    # blue halo

# ── Núcleo do Vazio Cristalizado: roxo + facetas de ametista ──
make("abyssal_core_crystallized",
     rim=(28, 6, 48),        # dark violet rim
     mid=(110, 40, 170),     # amethyst purple body
     bright=(200, 130, 250), # bright lavender
     core_a=(150, 70, 210),  # violet core outer
     core_b=(235, 200, 255), # near-white violet center
     halo=(170, 90, 230),    # purple halo
     facets=True)

print("done: 2 abyssal core textures")
