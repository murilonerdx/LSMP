# Generates the EMF Meter item sprites for Liberthia — a realistic K-II style
# ghost-hunting meter: dark handheld body, small LCD, and a vertical column of
# 5 round LEDs that glow green -> yellow -> orange -> red.
#
# Rendered at 4x supersampling (256x256) and downscaled to 64x64 with LANCZOS so
# the curves/LED glows are smooth (no pixel-blob). emf_meter_N.png lights the
# bottom N LEDs; 0 = all dark, 5 = full glowing column.
from PIL import Image, ImageDraw

OUT = "src/main/resources/assets/liberthia/textures/item"

SIZE = 64          # final texture size
S = 4              # supersample factor
W = SIZE * S       # working canvas size

# ---- palette ----
BODY_TOP = (74, 80, 92)        # body gradient (top, lighter)
BODY_BOT = (30, 33, 40)        # body gradient (bottom, darker)
EDGE_HI = (120, 128, 142)      # top-left bevel highlight
EDGE_LO = (14, 15, 19)         # bottom-right bevel shadow
SCREEN_BG = (10, 20, 16)       # LCD glass
SCREEN_TEXT = (60, 150, 90)    # faint LCD digits
LABEL = (150, 156, 168)

# LED colors bottom(0)->top(4): green ramp to red
LED_ON = [
    (60, 235, 80),
    (150, 235, 55),
    (245, 220, 50),
    (250, 140, 35),
    (250, 50, 45),
]


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def rounded_body(draw):
    # body rect in final coords, scaled up
    x0, y0, x1, y1 = 17, 3, 47, 61
    r = 7
    box = (x0 * S, y0 * S, x1 * S, y1 * S)
    # vertical gradient fill (draw row by row, clipped to a rounded mask)
    mask = Image.new("L", (W, W), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle(box, radius=r * S, fill=255)
    grad = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    gp = grad.load()
    for y in range(box[1], box[3]):
        t = (y - box[1]) / max(1, (box[3] - box[1]))
        col = lerp(BODY_TOP, BODY_BOT, t) + (255,)
        for x in range(box[0], box[2]):
            gp[x, y] = col
    return grad, mask, box, r


def add_bevel(base, box, r):
    d = ImageDraw.Draw(base)
    # highlight (top-left) and shadow (bottom-right) outlines
    d.rounded_rectangle(box, radius=r * S, outline=EDGE_HI + (255,), width=2 * S)
    inner = (box[0] + S, box[1] + S, box[2] - S, box[3] - S)
    d.rounded_rectangle(inner, radius=(r - 1) * S, outline=EDGE_LO + (90,), width=S)


def draw_screen(base):
    d = ImageDraw.Draw(base)
    sx0, sy0, sx1, sy1 = 22, 7, 42, 15
    d.rounded_rectangle((sx0 * S, sy0 * S, sx1 * S, sy1 * S), radius=2 * S,
                        fill=SCREEN_BG + (255,), outline=(4, 6, 6, 255), width=S)
    # faint seven-seg-ish digits
    d.line((26 * S, 9 * S, 26 * S, 13 * S), fill=SCREEN_TEXT + (160,), width=S)
    d.line((26 * S, 9 * S, 29 * S, 9 * S), fill=SCREEN_TEXT + (160,), width=S)
    d.line((29 * S, 9 * S, 29 * S, 13 * S), fill=SCREEN_TEXT + (160,), width=S)
    d.line((33 * S, 9 * S, 36 * S, 9 * S), fill=SCREEN_TEXT + (120,), width=S)
    d.line((33 * S, 11 * S, 36 * S, 11 * S), fill=SCREEN_TEXT + (120,), width=S)
    d.line((33 * S, 13 * S, 36 * S, 13 * S), fill=SCREEN_TEXT + (120,), width=S)


def draw_label(base):
    d = ImageDraw.Draw(base)
    # small "EMF" style ticks + speaker grille near bottom
    for i, x in enumerate((25, 29, 33, 37)):
        d.ellipse(((x - 0.6) * S, 55 * S, (x + 0.6) * S, 56.2 * S),
                  fill=(18, 19, 24, 255))
    d.line((23 * S, 52 * S, 41 * S, 52 * S), fill=LABEL + (70,), width=max(1, S // 2))


def stamp_glow(base, cx, cy, color, radius):
    """Additive radial glow patch centered at (cx,cy) in working coords."""
    R = int(radius * S)
    patch = Image.new("RGBA", (2 * R + 1, 2 * R + 1), (0, 0, 0, 0))
    pp = patch.load()
    for j in range(2 * R + 1):
        for i in range(2 * R + 1):
            dx, dy = i - R, j - R
            dist = (dx * dx + dy * dy) ** 0.5 / R
            if dist >= 1.0:
                continue
            a = (1.0 - dist) ** 2.2
            pp[i, j] = color + (int(a * 200),)
    base.alpha_composite(patch, (int(cx * S) - R, int(cy * S) - R))


def draw_leds(base, level):
    d = ImageDraw.Draw(base)
    cx = 32
    ys = [50, 41, 32, 23, 14]   # bottom -> top in final coords
    rad = 3.4                    # bulb radius
    for idx, cy in enumerate(ys):
        on = idx < level
        col = LED_ON[idx]
        # recessed dark socket
        d.ellipse(((cx - rad - 1) * S, (cy - rad - 1) * S,
                   (cx + rad + 1) * S, (cy + rad + 1) * S),
                  fill=(12, 13, 17, 255))
        if on:
            stamp_glow(base, cx, cy, col, rad + 4.5)   # halo
            d = ImageDraw.Draw(base)
            d.ellipse(((cx - rad) * S, (cy - rad) * S, (cx + rad) * S, (cy + rad) * S),
                      fill=col + (255,))
            # bright hot core + glossy glint
            hot = tuple(min(255, c + 40) for c in col)
            d.ellipse(((cx - rad * 0.5) * S, (cy - rad * 0.5) * S,
                       (cx + rad * 0.5) * S, (cy + rad * 0.5) * S), fill=hot + (255,))
            d.ellipse(((cx - rad * 0.55) * S, (cy - rad * 0.75) * S,
                       (cx - rad * 0.05) * S, (cy - rad * 0.25) * S),
                      fill=(255, 255, 255, 220))
        else:
            dim = lerp(col, (0, 0, 0), 0.78)
            d.ellipse(((cx - rad) * S, (cy - rad) * S, (cx + rad) * S, (cy + rad) * S),
                      fill=dim + (255,))
            d.ellipse(((cx - rad) * S, (cy - rad) * S, (cx + rad) * S, (cy + rad) * S),
                      outline=(0, 0, 0, 160), width=S)


def make(level):
    base = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    grad, mask, box, r = rounded_body(base)
    base = Image.composite(grad, base, mask)
    add_bevel(base, box, r)
    draw_screen(base)
    draw_label(base)
    draw_leds(base, level)
    out = base.resize((SIZE, SIZE), Image.LANCZOS)
    out.save(f"{OUT}/emf_meter_{level}.png")
    print(f"wrote emf_meter_{level}.png ({level} LED lit) {SIZE}x{SIZE}")


for lvl in range(6):
    make(lvl)
print("done: 6 EMF meter sprites (64x64, supersampled, glowing LEDs)")
