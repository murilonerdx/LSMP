"""r120: Procedural void spell assets.

Generates:
- particle/void_infection.png — 8 frames × 16x16, purple wisp animated
- particle/mini_black_hole.png — 6 frames × 16x16, collapsing sphere
- particle/*.mcmeta — animation metadata
- entity/void_larva/void_larva.png — silverfish-shape skin recolored purple (64x32)
- entity/mini_black_hole.png — billboard texture 32x32
- mob_effect/void_infection.png — 18x18 status icon
- item/spell_void.png + spell_void_laser.png (purple scroll variants)
- item/void_reagent.png — purple amethyst-like reagent
- models JSON for items
"""
from PIL import Image, ImageDraw, ImageFilter
import os, json, math

ROOT = os.path.dirname(__file__)
TEX_PARTICLE = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/particle"))
TEX_ITEM     = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/item"))
TEX_ENT_DIR  = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/entity"))
TEX_EFF      = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/textures/mob_effect"))
MODEL_ITM    = os.path.normpath(os.path.join(ROOT, "..", "src/main/resources/assets/liberthia/models/item"))
for d in [TEX_PARTICLE, TEX_ITEM, TEX_ENT_DIR, TEX_EFF, MODEL_ITM,
          os.path.join(TEX_ENT_DIR, "void_larva")]:
    os.makedirs(d, exist_ok=True)


def make_void_infection_spritesheet():
    """8 frames de wisp roxo flutuando — cresce e depois fade-out."""
    frames = 8
    sheet = Image.new("RGBA", (16, 16 * frames), (0, 0, 0, 0))
    for f in range(frames):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        # Center radial wisp
        cx, cy = 8, 8
        # Growth + fade curve
        progress = f / (frames - 1)
        base_size = 2 + int(progress * 4)
        alpha_curve = int(255 * (1.0 - (progress - 0.5)**2 * 3.5))
        alpha_curve = max(40, min(255, alpha_curve))

        # Core bright
        core_color = (255, 180, 255, alpha_curve)
        d.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=core_color)
        # Mid
        mid_color = (180, 60, 220, max(20, alpha_curve - 80))
        for r in range(1, base_size):
            for ang_deg in range(0, 360, 30):
                a = math.radians(ang_deg + f * 12)  # ligeira rotação
                px = cx + r * math.cos(a)
                py = cy + r * math.sin(a)
                # Distância wobble
                wx = px + (math.sin(a * 3) * 0.5)
                wy = py + (math.cos(a * 3) * 0.5)
                d.point((int(wx), int(wy)), fill=mid_color)
        # Outer dim halo
        outer_color = (100, 20, 150, max(10, alpha_curve - 130))
        for r in [base_size + 1, base_size + 2]:
            for ang_deg in range(0, 360, 45):
                a = math.radians(ang_deg + f * 15)
                px = int(cx + r * math.cos(a))
                py = int(cy + r * math.sin(a))
                if 0 <= px < 16 and 0 <= py < 16:
                    d.point((px, py), fill=outer_color)
        sheet.paste(img, (0, f * 16))
    sheet.save(os.path.join(TEX_PARTICLE, "void_infection.png"))

    # .mcmeta
    with open(os.path.join(TEX_PARTICLE, "void_infection.png.mcmeta"), "w") as f:
        json.dump({"animation": {"frametime": 2, "interpolate": True}}, f)


def make_mini_black_hole_spritesheet():
    """6 frames de colapso — sphere com aro shrinkando."""
    frames = 6
    sheet = Image.new("RGBA", (16, 16 * frames), (0, 0, 0, 0))
    for f in range(frames):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        cx, cy = 8, 8
        progress = f / (frames - 1)
        # Outer halo encolhe ao longo do tempo
        outer_r = max(1, 7 - int(progress * 5))
        # Ring roxo brilhante externo
        d.ellipse([cx - outer_r, cy - outer_r, cx + outer_r, cy + outer_r],
                  outline=(180, 60, 230, 220))
        # Inner halo médio
        if outer_r > 2:
            d.ellipse([cx - outer_r + 1, cy - outer_r + 1, cx + outer_r - 1, cy + outer_r - 1],
                      outline=(120, 30, 180, 180))
        # Black core
        core_r = max(1, outer_r - 2)
        d.ellipse([cx - core_r, cy - core_r, cx + core_r, cy + core_r],
                  fill=(15, 0, 25, 255))
        # Pequeno highlight branco no centro (lensing)
        if progress < 0.5:
            d.point((cx, cy), fill=(255, 220, 255, 200))
        sheet.paste(img, (0, f * 16))
    sheet.save(os.path.join(TEX_PARTICLE, "mini_black_hole.png"))
    with open(os.path.join(TEX_PARTICLE, "mini_black_hole.png.mcmeta"), "w") as f:
        json.dump({"animation": {"frametime": 3, "interpolate": True}}, f)


def make_void_larva_texture():
    """Silverfish vanilla 64×32 com tudo recolorido pra purple/dark.
    Silverfish texture layout: standard segments. Vamos só pintar a área
    inteira como purple gradient."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Default silverfish texture has body segments around y=0..32 x=0..64
    # Fill body area with purple shades
    for y in range(32):
        for x in range(64):
            # Purple gradient — darker top, lighter mid
            base_r = 50 + int(40 * math.sin(y * 0.3 + x * 0.1))
            base_g = 10 + int(15 * math.sin(y * 0.2))
            base_b = 90 + int(50 * math.cos(x * 0.15))
            d.point((x, y), fill=(max(0, min(255, base_r)),
                                  max(0, min(255, base_g)),
                                  max(0, min(255, base_b)),
                                  255))
    # Add some bright purple dots simulating eyes/segments
    for sx in [12, 24, 36, 48]:
        d.point((sx, 6), fill=(255, 100, 250, 255))
        d.point((sx, 18), fill=(255, 100, 250, 255))
    img.save(os.path.join(TEX_ENT_DIR, "void_larva", "void_larva.png"))


def make_mini_black_hole_billboard():
    """32x32 texture pro renderer billboard — disco preto com halo roxo."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx, cy = 16, 16
    # Outer halo gradient (pixel-by-pixel pra suavidade)
    for r in range(15, 0, -1):
        if r >= 13:
            color = (140, 40, 200, max(20, 200 - (15 - r) * 50))
        elif r >= 10:
            color = (80, 15, 130, 220)
        elif r >= 6:
            color = (30, 5, 50, 240)
        else:
            color = (5, 0, 15, 255)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color)
    # Pure black core
    d.ellipse([cx - 4, cy - 4, cx + 4, cy + 4], fill=(0, 0, 0, 255))
    # Center white pin-prick (lensing point)
    d.point((cx, cy), fill=(255, 255, 255, 255))
    img.save(os.path.join(TEX_ENT_DIR, "mini_black_hole.png"))


def make_void_infection_effect_icon():
    """18x18 mob effect status icon — purple skull."""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Background bg vazio (status icons usam GUI texture sheet ideally,
    # but we'll just paint a 18x18 standalone)
    # Skull silhouette
    d.ellipse([4, 3, 13, 13], fill=(60, 10, 90, 255))
    d.ellipse([5, 4, 12, 11], fill=(150, 60, 200, 255))
    # Eyes
    d.point((7, 7), fill=(255, 255, 255, 255))
    d.point((10, 7), fill=(255, 255, 255, 255))
    # Mouth grin
    d.line([(7, 10), (10, 10)], fill=(40, 0, 60, 255))
    d.point((7, 11), fill=(40, 0, 60, 255))
    d.point((10, 11), fill=(40, 0, 60, 255))
    # Drip particles
    d.point((6, 14), fill=(180, 80, 220, 255))
    d.point((11, 14), fill=(180, 80, 220, 255))
    img.save(os.path.join(TEX_EFF, "void_infection.png"))


def make_void_scroll_icon():
    """Spell scroll icon — purple variant."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Scroll dark purple body
    d.rectangle([2, 3, 13, 12], fill=(60, 20, 90, 255))
    d.rectangle([3, 4, 12, 11], fill=(100, 40, 140, 255))
    # Side rolls
    d.line([(2, 2), (2, 13)], fill=(40, 10, 60, 255))
    d.line([(13, 2), (13, 13)], fill=(40, 10, 60, 255))
    # Purple void rune in center
    cx, cy = 7, 7
    d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(180, 60, 230, 255))
    d.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=(0, 0, 0, 255))
    # 4-point glow
    d.point((7, 4), fill=(255, 180, 255, 255))
    d.point((7, 10), fill=(255, 180, 255, 255))
    d.point((4, 7), fill=(255, 180, 255, 255))
    d.point((10, 7), fill=(255, 180, 255, 255))
    img.save(os.path.join(TEX_ITEM, "spell_void.png"))

    # Laser variant — same but with horizontal stripe (suggesting beam)
    img2 = img.copy()
    d2 = ImageDraw.Draw(img2)
    d2.line([(3, 7), (12, 7)], fill=(255, 200, 255, 255))
    img2.save(os.path.join(TEX_ITEM, "spell_void_laser.png"))


def make_void_reagent_icon():
    """Void reagent — amethyst crystal recolored to dark purple/black."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Crystal facets
    d.polygon([(8, 1), (12, 5), (12, 11), (8, 14), (4, 11), (4, 5)],
              fill=(80, 30, 120, 255), outline=(40, 10, 60, 255))
    # Inner glow
    d.polygon([(8, 4), (10, 6), (10, 9), (8, 11), (6, 9), (6, 6)],
              fill=(180, 80, 230, 255))
    # White core highlight
    d.point((8, 7), fill=(255, 255, 255, 255))
    d.point((7, 6), fill=(220, 180, 255, 255))
    img.save(os.path.join(TEX_ITEM, "void_reagent.png"))


# ───────── Run ─────────
make_void_infection_spritesheet()
make_mini_black_hole_spritesheet()
make_void_larva_texture()
make_mini_black_hole_billboard()
make_void_infection_effect_icon()
make_void_scroll_icon()
make_void_reagent_icon()

# Item models
for it in ["spell_void", "spell_void_laser", "void_reagent"]:
    with open(os.path.join(MODEL_ITM, f"{it}.json"), "w") as f:
        json.dump({"parent": "minecraft:item/generated",
                   "textures": {"layer0": f"liberthia:item/{it}"}}, f)

print("r120 Void Spell assets gerados: 2 particle spritesheets + larva texture + black hole billboard + effect icon + 3 item icons + 3 item models")
