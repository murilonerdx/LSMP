"""
Generate all missing placeholder textures for the Liberthia mod.
Block textures: 16x16
Item textures: 16x16
GUI textures: 256x256 (Minecraft standard container background)
Armor layers: 64x32
"""

from PIL import Image, ImageDraw
import os

BASE = "src/main/resources/assets/liberthia/textures"

def ensure_dir(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)

def save(img, rel_path):
    full = os.path.join(BASE, rel_path)
    ensure_dir(full)
    img.save(full)
    print(f"  Created: {rel_path}")

def block_texture(color, border_color=None, detail_fn=None):
    img = Image.new("RGBA", (16, 16), color)
    draw = ImageDraw.Draw(img)
    if border_color:
        draw.rectangle([0, 0, 15, 15], outline=border_color)
    if detail_fn:
        detail_fn(draw, img)
    return img

def item_texture(base_color, detail_fn=None):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    detail_fn(draw, img) if detail_fn else None
    return img

# ========== BLOCK TEXTURES ==========
print("=== Block Textures ===")

# Dark Matter Forge - front (off)
def forge_front(draw, img):
    img.paste((60, 60, 60, 255), [0, 0, 16, 16])
    draw.rectangle([0, 0, 15, 15], outline=(40, 40, 40))
    # mouth opening
    draw.rectangle([4, 8, 11, 14], fill=(30, 10, 50))
    draw.rectangle([5, 9, 10, 13], fill=(20, 5, 35))
    # rivets
    for x in [2, 13]:
        for y in [2, 6]:
            draw.point((x, y), fill=(80, 80, 80))
save(block_texture((0,0,0,0), detail_fn=forge_front), "block/dark_matter_forge_front.png")

# Dark Matter Forge - front on (lit)
def forge_front_on(draw, img):
    img.paste((60, 60, 60, 255), [0, 0, 16, 16])
    draw.rectangle([0, 0, 15, 15], outline=(40, 40, 40))
    # glowing mouth
    draw.rectangle([4, 8, 11, 14], fill=(120, 40, 160))
    draw.rectangle([5, 9, 10, 13], fill=(160, 60, 200))
    draw.rectangle([6, 10, 9, 12], fill=(200, 100, 255))
    # rivets
    for x in [2, 13]:
        for y in [2, 6]:
            draw.point((x, y), fill=(100, 80, 120))
save(block_texture((0,0,0,0), detail_fn=forge_front_on), "block/dark_matter_forge_front_on.png")

# Dark Matter Forge - side
def forge_side(draw, img):
    img.paste((55, 55, 55, 255), [0, 0, 16, 16])
    draw.rectangle([0, 0, 15, 15], outline=(40, 40, 40))
    # dark matter veins
    for y in [3, 7, 11]:
        draw.line([(2, y), (13, y)], fill=(60, 20, 80))
    draw.line([(4, 1), (4, 14)], fill=(50, 15, 70))
    draw.line([(11, 1), (11, 14)], fill=(50, 15, 70))
save(block_texture((0,0,0,0), detail_fn=forge_side), "block/dark_matter_forge_side.png")

# Dark Matter Forge - top
def forge_top(draw, img):
    img.paste((65, 65, 65, 255), [0, 0, 16, 16])
    draw.rectangle([0, 0, 15, 15], outline=(40, 40, 40))
    # vent pattern
    draw.rectangle([3, 3, 12, 12], outline=(50, 20, 70))
    draw.rectangle([5, 5, 10, 10], outline=(70, 30, 100))
    draw.point((7, 7), fill=(120, 50, 160))
    draw.point((8, 8), fill=(120, 50, 160))
save(block_texture((0,0,0,0), detail_fn=forge_top), "block/dark_matter_forge_top.png")

# Matter Infuser
def infuser(draw, img):
    img.paste((40, 80, 90, 255), [0, 0, 16, 16])
    draw.rectangle([0, 0, 15, 15], outline=(30, 60, 70))
    # three colored dots (dark/clear/yellow inputs)
    draw.ellipse([3, 3, 5, 5], fill=(90, 30, 120))  # purple
    draw.ellipse([7, 3, 9, 5], fill=(0, 180, 190))   # cyan
    draw.ellipse([11, 3, 13, 5], fill=(220, 190, 0))  # gold
    # center circle
    draw.ellipse([5, 7, 10, 12], outline=(200, 200, 200))
    draw.point((7, 9), fill=(255, 255, 255))
    draw.point((8, 10), fill=(255, 255, 255))
save(block_texture((0,0,0,0), detail_fn=infuser), "block/matter_infuser.png")

# Research Table
def research(draw, img):
    img.paste((120, 85, 50, 255), [0, 0, 16, 16])
    draw.rectangle([0, 0, 15, 15], outline=(90, 65, 35))
    # wood grain
    for y in [2, 5, 8, 11, 14]:
        draw.line([(1, y), (14, y)], fill=(100, 70, 40))
    # paper on top
    draw.rectangle([4, 3, 11, 10], fill=(230, 220, 200))
    draw.rectangle([4, 3, 11, 10], outline=(180, 170, 150))
    # text lines
    for y in [5, 7, 9]:
        draw.line([(5, y), (10, y)], fill=(60, 60, 60))
save(block_texture((0,0,0,0), detail_fn=research), "block/research_table.png")

# Containment Chamber
def containment(draw, img):
    img.paste((180, 180, 190, 255), [0, 0, 16, 16])
    draw.rectangle([0, 0, 15, 15], outline=(100, 100, 110))
    # glass window
    draw.rectangle([3, 3, 12, 12], fill=(100, 180, 200, 160))
    draw.rectangle([3, 3, 12, 12], outline=(80, 80, 90))
    # inner glow
    draw.rectangle([5, 5, 10, 10], fill=(0, 200, 210, 100))
    # corner bolts
    for x, y in [(1,1),(14,1),(1,14),(14,14)]:
        draw.point((x, y), fill=(140, 140, 150))
save(block_texture((0,0,0,0), detail_fn=containment), "block/containment_chamber.png")

# Matter Transmuter
def transmuter(draw, img):
    img.paste((50, 50, 60, 255), [0, 0, 16, 16])
    draw.rectangle([0, 0, 15, 15], outline=(35, 35, 45))
    # conversion arrows
    draw.line([(3, 8), (12, 8)], fill=(200, 200, 200))
    draw.line([(10, 6), (12, 8)], fill=(200, 200, 200))
    draw.line([(10, 10), (12, 8)], fill=(200, 200, 200))
    # energy circles
    draw.ellipse([2, 2, 6, 6], outline=(90, 30, 120))
    draw.ellipse([9, 10, 13, 14], outline=(0, 200, 210))
save(block_texture((0,0,0,0), detail_fn=transmuter), "block/matter_transmuter.png")

# Purification Bench - top/front/side (orientable model needs these)
def pb_top(draw, img):
    img.paste((200, 200, 210, 255), [0, 0, 16, 16])
    draw.rectangle([0, 0, 15, 15], outline=(160, 160, 170))
    draw.rectangle([3, 3, 12, 12], fill=(230, 240, 255))
    draw.rectangle([3, 3, 12, 12], outline=(180, 190, 200))
    # clear matter glow center
    draw.ellipse([6, 6, 9, 9], fill=(0, 220, 230))
save(block_texture((0,0,0,0), detail_fn=pb_top), "block/purification_bench_top.png")

def pb_front(draw, img):
    img.paste((190, 190, 200, 255), [0, 0, 16, 16])
    draw.rectangle([0, 0, 15, 15], outline=(150, 150, 160))
    # opening
    draw.rectangle([4, 6, 11, 13], fill=(20, 150, 160))
    draw.rectangle([5, 7, 10, 12], fill=(30, 180, 190))
    # label
    draw.line([(5, 3), (10, 3)], fill=(0, 200, 210))
save(block_texture((0,0,0,0), detail_fn=pb_front), "block/purification_bench_front.png")

def pb_side(draw, img):
    img.paste((185, 185, 195, 255), [0, 0, 16, 16])
    draw.rectangle([0, 0, 15, 15], outline=(150, 150, 160))
    for y in [3, 7, 11]:
        draw.line([(2, y), (13, y)], fill=(170, 170, 180))
    draw.line([(8, 1), (8, 14)], fill=(0, 180, 190, 80))
save(block_texture((0,0,0,0), detail_fn=pb_side), "block/purification_bench_side.png")


# ========== ITEM TEXTURES ==========
print("\n=== Item Textures ===")

def make_sword(draw, img, blade_color, handle_color, guard_color):
    # Blade diagonal
    for i in range(10):
        x, y = 12 - i, 2 + i
        draw.point((x, y), fill=blade_color)
        draw.point((x-1, y), fill=blade_color)
    # Edge highlight
    for i in range(8):
        draw.point((13 - i, 2 + i), fill=tuple(min(c+40, 255) for c in blade_color[:3]) + (255,))
    # Guard
    draw.line([(2, 11), (5, 14)], fill=guard_color)
    # Handle
    draw.line([(1, 13), (2, 14)], fill=handle_color)
    draw.point((1, 14), fill=handle_color)

def dark_sword(draw, img):
    make_sword(draw, img, (80, 20, 120, 255), (50, 50, 50, 255), (100, 40, 140, 255))
    # Purple glow particles
    draw.point((10, 4), fill=(150, 60, 200, 180))
    draw.point((7, 7), fill=(130, 40, 180, 150))
save(item_texture((0,0,0,0), dark_sword), "item/dark_matter_sword.png")

def make_pickaxe(draw, img, head_color, handle_color):
    # Head
    draw.line([(3, 2), (12, 2)], fill=head_color)
    draw.line([(3, 3), (12, 3)], fill=head_color)
    draw.line([(2, 2), (2, 4)], fill=head_color)
    draw.line([(13, 2), (13, 4)], fill=head_color)
    # Handle
    for i in range(9):
        draw.point((7 + i // 3, 4 + i), fill=handle_color)
        draw.point((8 - i // 3, 5 + i), fill=handle_color)

def dark_pickaxe(draw, img):
    make_pickaxe(draw, img, (80, 20, 120, 255), (50, 50, 50, 255))
    draw.point((5, 2), fill=(150, 60, 200, 180))
    draw.point((10, 2), fill=(150, 60, 200, 180))
save(item_texture((0,0,0,0), dark_pickaxe), "item/dark_matter_pickaxe.png")

def make_axe(draw, img, head_color, handle_color):
    # Head
    draw.rectangle([3, 2, 8, 4], fill=head_color)
    draw.rectangle([2, 3, 4, 6], fill=head_color)
    draw.rectangle([3, 5, 9, 6], fill=head_color)
    # Edge
    draw.line([(2, 2), (2, 7)], fill=tuple(min(c+40, 255) for c in head_color[:3]) + (255,))
    # Handle
    for i in range(7):
        draw.point((8 + i // 3, 7 + i), fill=handle_color)
save(item_texture((0,0,0,0), lambda d, i: (
    make_axe(d, i, (80, 20, 120, 255), (50, 50, 50, 255)),
    d.point((4, 3), (150, 60, 200, 180))
)), "item/dark_matter_axe.png")

# Clear Matter Armor
def armor_piece(draw, img, piece_type):
    cyan = (0, 200, 210, 255)
    light_cyan = (100, 230, 240, 255)
    dark_cyan = (0, 130, 140, 255)
    if piece_type == "helmet":
        draw.rectangle([4, 3, 11, 6], fill=cyan)
        draw.rectangle([3, 6, 12, 10], fill=cyan)
        draw.rectangle([5, 7, 10, 9], fill=dark_cyan)
        draw.line([(4, 3), (11, 3)], fill=light_cyan)
    elif piece_type == "chestplate":
        draw.rectangle([3, 2, 12, 13], fill=cyan)
        draw.rectangle([6, 2, 9, 4], fill=(0, 0, 0, 0))
        draw.rectangle([1, 3, 3, 10], fill=cyan)
        draw.rectangle([12, 3, 14, 10], fill=cyan)
        draw.line([(3, 2), (12, 2)], fill=light_cyan)
        draw.line([(5, 5), (5, 12)], fill=dark_cyan)
        draw.line([(10, 5), (10, 12)], fill=dark_cyan)
    elif piece_type == "leggings":
        draw.rectangle([3, 2, 12, 5], fill=cyan)
        draw.rectangle([3, 5, 7, 14], fill=cyan)
        draw.rectangle([8, 5, 12, 14], fill=cyan)
        draw.line([(3, 2), (12, 2)], fill=light_cyan)
        draw.line([(5, 6), (5, 13)], fill=dark_cyan)
        draw.line([(10, 6), (10, 13)], fill=dark_cyan)
    elif piece_type == "boots":
        draw.rectangle([2, 6, 6, 13], fill=cyan)
        draw.rectangle([9, 6, 13, 13], fill=cyan)
        draw.rectangle([1, 11, 6, 13], fill=dark_cyan)
        draw.rectangle([9, 11, 14, 13], fill=dark_cyan)
        draw.line([(2, 6), (6, 6)], fill=light_cyan)
        draw.line([(9, 6), (13, 6)], fill=light_cyan)

for piece in ["helmet", "chestplate", "leggings", "boots"]:
    save(item_texture((0,0,0,0), lambda d, i, p=piece: armor_piece(d, i, p)),
         f"item/clear_matter_{piece}.png")

# Material items
def stabilized_dm(draw, img):
    draw.ellipse([3, 3, 12, 12], fill=(90, 30, 130, 255))
    draw.ellipse([5, 5, 10, 10], fill=(120, 50, 170, 255))
    draw.ellipse([6, 6, 9, 9], fill=(160, 80, 210, 255))
    # stability ring
    draw.ellipse([2, 2, 13, 13], outline=(0, 200, 210, 200))
save(item_texture((0,0,0,0), stabilized_dm), "item/stabilized_dark_matter.png")

def void_crystal(draw, img):
    # Crystal shape
    points = [(8, 1), (12, 5), (10, 14), (6, 14), (4, 5)]
    draw.polygon(points, fill=(20, 10, 40, 255), outline=(60, 30, 90, 255))
    # Inner glow
    draw.polygon([(8, 3), (10, 6), (9, 12), (7, 12), (6, 6)], fill=(40, 20, 70, 255))
    draw.point((8, 6), fill=(100, 50, 150, 255))
    draw.point((8, 7), fill=(80, 40, 130, 255))
save(item_texture((0,0,0,0), void_crystal), "item/void_crystal.png")

def singularity_core(draw, img):
    # Outer ring
    draw.ellipse([2, 2, 13, 13], fill=(30, 10, 50, 255), outline=(150, 60, 200, 255))
    # Middle ring
    draw.ellipse([4, 4, 11, 11], outline=(200, 100, 255, 255))
    # Core
    draw.ellipse([6, 6, 9, 9], fill=(255, 200, 255, 255))
    # Energy particles
    for x, y in [(3, 7), (12, 8), (7, 2), (8, 13)]:
        draw.point((x, y), fill=(200, 100, 255, 180))
save(item_texture((0,0,0,0), singularity_core), "item/singularity_core.png")

def matter_core(draw, img):
    # Hexagonal shape approximation
    draw.ellipse([3, 3, 12, 12], fill=(80, 90, 100, 255), outline=(120, 130, 140, 255))
    # Three color bands
    draw.line([(5, 5), (7, 5)], fill=(90, 30, 120))  # purple
    draw.line([(5, 7), (10, 7)], fill=(0, 200, 210))  # cyan
    draw.line([(5, 9), (10, 9)], fill=(220, 190, 0))  # gold
    draw.ellipse([6, 6, 9, 9], fill=(200, 200, 220, 255))
save(item_texture((0,0,0,0), matter_core), "item/matter_core.png")

def purified_essence(draw, img):
    # Bottle shape
    draw.rectangle([6, 2, 9, 4], fill=(200, 200, 200, 255))
    draw.rectangle([4, 5, 11, 13], fill=(180, 240, 245, 255))
    draw.rectangle([4, 5, 11, 13], outline=(100, 200, 210, 255))
    # Liquid glow
    draw.rectangle([5, 7, 10, 12], fill=(0, 220, 230, 200))
    draw.point((7, 8), fill=(200, 255, 255, 255))
save(item_texture((0,0,0,0), purified_essence), "item/purified_essence.png")

def research_notes_item(draw, img):
    # Paper
    draw.rectangle([3, 1, 12, 14], fill=(230, 220, 200, 255))
    draw.rectangle([3, 1, 12, 14], outline=(180, 170, 150, 255))
    # Folded corner
    draw.polygon([(9, 1), (12, 1), (12, 4)], fill=(200, 190, 170, 255))
    # Text lines
    for y in [4, 6, 8, 10, 12]:
        draw.line([(5, y), (10, y)], fill=(80, 70, 60, 255))
    # Ink spots
    draw.point((6, 5), fill=(30, 10, 50, 200))
save(item_texture((0,0,0,0), research_notes_item), "item/research_notes.png")

def host_journal(draw, img):
    # Book cover
    draw.rectangle([2, 1, 13, 14], fill=(60, 30, 20, 255))
    draw.rectangle([2, 1, 13, 14], outline=(40, 20, 10, 255))
    # Spine
    draw.line([(4, 1), (4, 14)], fill=(80, 40, 25, 255))
    # Pages visible
    draw.rectangle([5, 2, 12, 13], fill=(220, 210, 190, 255))
    # Dark matter symbol on cover
    draw.ellipse([7, 5, 10, 8], fill=(90, 30, 120, 255))
    draw.point((8, 6), fill=(150, 60, 200, 255))
    # Clasp
    draw.point((13, 7), fill=(180, 160, 50, 255))
save(item_texture((0,0,0,0), host_journal), "item/host_journal.png")

def worker_badge(draw, img):
    # Badge body
    draw.rectangle([3, 2, 12, 13], fill=(200, 200, 200, 255))
    draw.rectangle([3, 2, 12, 13], outline=(150, 150, 150, 255))
    # Photo area
    draw.rectangle([5, 4, 10, 8], fill=(180, 180, 190, 255))
    draw.rectangle([6, 5, 9, 7], fill=(140, 100, 80, 255))  # face placeholder
    # Text lines
    draw.line([(5, 10), (10, 10)], fill=(60, 60, 60, 255))
    draw.line([(5, 12), (10, 12)], fill=(60, 60, 60, 255))
    # Clip
    draw.rectangle([6, 0, 9, 2], fill=(180, 180, 180, 255))
    # Color stripe
    draw.line([(3, 3), (12, 3)], fill=(0, 200, 210, 255))
save(item_texture((0,0,0,0), worker_badge), "item/worker_badge.png")


# ========== GUI TEXTURES ==========
print("\n=== GUI Textures ===")

def make_gui_base(title=""):
    """Create standard 256x256 Minecraft container GUI (176x166 visible area)."""
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Main background (176x166 at top-left)
    # Border
    draw.rectangle([0, 0, 175, 165], fill=(198, 198, 198, 255))
    # Top border highlight
    draw.rectangle([0, 0, 175, 0], fill=(255, 255, 255, 255))
    draw.rectangle([0, 0, 0, 165], fill=(255, 255, 255, 255))
    # Bottom/right shadow
    draw.rectangle([175, 0, 175, 165], fill=(85, 85, 85, 255))
    draw.rectangle([0, 165, 175, 165], fill=(85, 85, 85, 255))

    # Inner area
    draw.rectangle([3, 3, 172, 162], fill=(198, 198, 198, 255))

    # Player inventory area (at y=84 for 3 rows, y=142 for hotbar)
    # 9 columns, 18px each starting at x=7
    for row in range(3):
        for col in range(9):
            sx = 7 + col * 18
            sy = 83 + row * 18
            draw_slot(draw, sx, sy)

    # Hotbar
    for col in range(9):
        sx = 7 + col * 18
        sy = 141
        draw_slot(draw, sx, sy)

    return img, draw

def draw_slot(draw, x, y, size=18):
    """Draw a standard inventory slot at position."""
    draw.rectangle([x, y, x + size - 1, y + size - 1], fill=(139, 139, 139, 255))
    # Highlight edges
    draw.line([(x, y), (x + size - 1, y)], fill=(85, 85, 85, 255))
    draw.line([(x, y), (x, y + size - 1)], fill=(85, 85, 85, 255))
    draw.line([(x + size - 1, y), (x + size - 1, y + size - 1)], fill=(255, 255, 255, 255))
    draw.line([(x, y + size - 1), (x + size - 1, y + size - 1)], fill=(255, 255, 255, 255))

def draw_arrow(draw, x, y, width=24, height=17):
    """Draw a progress arrow area (background). Arrow pointing right."""
    draw.rectangle([x, y, x + width - 1, y + height - 1], fill=(198, 198, 198, 255))
    # Arrow shape
    mid_y = y + height // 2
    for i in range(width - 6):
        draw.line([(x + 3, mid_y - 2 + 0), (x + 3, mid_y + 2)], fill=(180, 180, 180, 255))
    # Arrow outline
    draw.rectangle([x + 2, mid_y - 3, x + width - 8, mid_y + 3], outline=(170, 170, 170, 255))
    # Arrowhead
    for i in range(5):
        draw.line([(x + width - 7 + i, mid_y - 4 + i), (x + width - 7 + i, mid_y + 4 - i)], fill=(170, 170, 170, 255))

# 1. Purification Bench GUI
# Slots: input at (80,11), output at (80,59)
# Progress arrow area for blit
img, draw = make_gui_base()
draw_slot(draw, 79, 10)  # input
draw_slot(draw, 79, 58)  # output
draw_arrow(draw, 77, 32)  # arrow between slots
# Progress arrow filled (at 176,0 in texture for blit source)
draw.rectangle([176, 0, 200, 16], fill=(0, 200, 210, 255))  # cyan fill for progress
save(img, "gui/purification_bench_gui.png")
print("  Created: gui/purification_bench_gui.png")

# 2. Dark Matter Forge GUI
# Slots: fuel(18,50), input1(66,16), input2(66,50), output(124,33)
img, draw = make_gui_base()
draw_slot(draw, 17, 49)   # fuel
draw_slot(draw, 65, 15)   # input 1
draw_slot(draw, 65, 49)   # input 2
draw_slot(draw, 123, 32)  # output
draw_arrow(draw, 88, 33)  # progress arrow
# Flame indicator area (for fuel)
draw.rectangle([18, 33, 33, 48], fill=(198, 198, 198, 255))
draw.rectangle([20, 35, 31, 46], outline=(170, 170, 170, 255))
# Progress fill at (176,0)
draw.rectangle([176, 0, 200, 16], fill=(120, 40, 160, 255))  # purple
# Flame fill at (176,17)
draw.rectangle([176, 17, 190, 31], fill=(200, 100, 50, 255))  # orange flame
save(img, "gui/dark_matter_forge_gui.png")
print("  Created: gui/dark_matter_forge_gui.png")

# 3. Matter Infuser GUI
# 5 slots: dark(26,16), clear(26,38), yellow(26,60), catalyst(80,38), output(134,38)
img, draw = make_gui_base()
draw_slot(draw, 25, 15)   # dark input
draw_slot(draw, 25, 37)   # clear input
draw_slot(draw, 25, 59)   # yellow input
draw_slot(draw, 79, 37)   # catalyst
draw_slot(draw, 133, 37)  # output
draw_arrow(draw, 101, 38) # progress
# Color indicators near input slots
draw.rectangle([11, 17, 14, 20], fill=(90, 30, 120, 255))  # purple
draw.rectangle([11, 39, 14, 42], fill=(0, 200, 210, 255))   # cyan
draw.rectangle([11, 61, 14, 64], fill=(220, 190, 0, 255))   # gold
# Progress fill
draw.rectangle([176, 0, 200, 16], fill=(200, 200, 220, 255))
save(img, "gui/matter_infuser_gui.png")
print("  Created: gui/matter_infuser_gui.png")

# 4. Research Table GUI
# 3 slots: input(30,33), paper(80,33), output(130,33)
img, draw = make_gui_base()
draw_slot(draw, 29, 32)   # input
draw_slot(draw, 79, 32)   # paper/book
draw_slot(draw, 129, 32)  # output
draw_arrow(draw, 52, 33)  # arrow 1
draw_arrow(draw, 102, 33) # arrow 2
# Progress fill
draw.rectangle([176, 0, 200, 16], fill=(120, 85, 50, 255))
save(img, "gui/research_table_gui.png")
print("  Created: gui/research_table_gui.png")

# 5. Containment Chamber GUI
# 4 slots: input1(44,16), input2(44,50), containment(80,33), output(134,33)
img, draw = make_gui_base()
draw_slot(draw, 43, 15)   # input 1
draw_slot(draw, 43, 49)   # input 2
draw_slot(draw, 79, 32)   # containment
draw_slot(draw, 133, 32)  # output
draw_arrow(draw, 101, 33) # progress
# Stability bar background (vertical, left side)
draw.rectangle([15, 10, 25, 70], fill=(139, 139, 139, 255))
draw.rectangle([15, 10, 25, 70], outline=(85, 85, 85, 255))
# Stability bar filled (at 176,0) - green gradient
for i in range(60):
    r = int(255 * i / 60)
    g = int(255 * (60 - i) / 60)
    draw.line([(176, i), (186, i)], fill=(r, g, 0, 255))
# Progress fill at (187,0)
draw.rectangle([187, 0, 211, 16], fill=(180, 180, 190, 255))
save(img, "gui/containment_chamber_gui.png")
print("  Created: gui/containment_chamber_gui.png")

# 6. Matter Transmuter GUI
# 3 slots: input(35,33), catalyst(80,33), output(125,33)
img, draw = make_gui_base()
draw_slot(draw, 34, 32)   # input
draw_slot(draw, 79, 32)   # catalyst
draw_slot(draw, 124, 32)  # output
draw_arrow(draw, 56, 33)  # arrow 1
draw_arrow(draw, 102, 33) # arrow 2
# Conversion indicator
draw.rectangle([176, 0, 200, 16], fill=(150, 150, 160, 255))
save(img, "gui/matter_transmuter_gui.png")
print("  Created: gui/matter_transmuter_gui.png")


# ========== ARMOR LAYER TEXTURES ==========
print("\n=== Armor Layer Textures ===")

# Clear matter armor layers (64x32)
def make_armor_layer(layer_num):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cyan = (0, 200, 210, 255)
    dark_cyan = (0, 140, 150, 255)
    light_cyan = (100, 230, 240, 255)

    if layer_num == 1:
        # Layer 1: helmet + chestplate + boots
        # Helmet (head area: 0,0 to 31,15 roughly)
        draw.rectangle([8, 0, 16, 8], fill=cyan)
        draw.rectangle([8, 8, 16, 16], fill=dark_cyan)
        # Right side of head
        draw.rectangle([0, 8, 8, 16], fill=cyan)
        # Body (chestplate)
        draw.rectangle([20, 20, 28, 32], fill=cyan)
        draw.rectangle([16, 20, 20, 32], fill=dark_cyan)
        draw.rectangle([28, 20, 32, 32], fill=dark_cyan)
        # Arms
        draw.rectangle([40, 20, 48, 32], fill=cyan)
        draw.rectangle([48, 20, 56, 32], fill=dark_cyan)
        # Boots
        draw.rectangle([32, 0, 40, 8], fill=dark_cyan)
        draw.rectangle([40, 0, 48, 8], fill=dark_cyan)
    elif layer_num == 2:
        # Layer 2: leggings
        draw.rectangle([0, 20, 8, 32], fill=cyan)
        draw.rectangle([8, 20, 16, 32], fill=dark_cyan)
        draw.rectangle([0, 0, 4, 12], fill=cyan)
        draw.rectangle([4, 0, 8, 12], fill=dark_cyan)

    return img

save(make_armor_layer(1), "models/armor/clear_matter_layer_1.png")
save(make_armor_layer(2), "models/armor/clear_matter_layer_2.png")

print("\n=== All textures generated! ===")
