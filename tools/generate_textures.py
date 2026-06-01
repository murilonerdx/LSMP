import os
from PIL import Image, ImageDraw
import random

def create_texture(path, primary_color, secondary_color, pattern="noise"):
    img = Image.new("RGBA", (16, 16), primary_color)
    draw = ImageDraw.Draw(img)
    
    if pattern == "noise":
        for x in range(16):
            for y in range(16):
                if random.random() < 0.3:
                    draw.point((x, y), secondary_color)
    elif pattern == "border":
        draw.rectangle([0, 0, 15, 15], outline=secondary_color)
    elif pattern == "shard":
        draw.polygon([(4, 4), (12, 2), (14, 14), (2, 12)], fill=primary_color, outline=secondary_color)
    elif pattern == "bomb":
        draw.ellipse([2, 2, 13, 13], fill=primary_color, outline=secondary_color)
        draw.line([8, 2, 8, 0], fill=(255, 0, 0)) # Red fuse
    elif pattern == "staff":
        draw.line([2, 14, 12, 4], fill=(139, 69, 19), width=2) # Brown handle
        draw.point((13, 3), secondary_color) # Glowing tip
    elif pattern == "injector":
        draw.rectangle([6, 2, 10, 12], fill=(200, 200, 200), outline=(100, 100, 100))
        draw.rectangle([7, 4, 9, 10], fill=secondary_color) # Liquid
        draw.line([8, 12, 8, 15], fill=(150, 150, 150)) # Needle
    elif pattern == "ore":
        for _ in range(8):
            rx, ry = random.randint(2, 13), random.randint(2, 13)
            draw.point((rx, ry), secondary_color)
            draw.point((rx+1, ry), secondary_color)

    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print(f"Generated: {path}")

# Color Palettes
DARK_PURPLE = (90, 26, 120, 255)
LIGHT_PURPLE = (150, 50, 200, 255)
WHITE = (255, 255, 255, 255)
CYAN = (179, 230, 255, 255)
GOLD = (255, 215, 0, 255)
DARK_ROCK = (60, 60, 60, 255)

# Block Textures
base_block = "src/main/resources/assets/liberthia/textures/block/"
create_texture(base_block + "white_matter_ore.png", WHITE, CYAN, "ore")
create_texture(base_block + "infection_growth.png", (30, 0, 40, 255), DARK_PURPLE, "noise")
create_texture(base_block + "white_matter_bomb.png", WHITE, CYAN, "bomb")
create_texture(base_block + "purification_bench.png", WHITE, (200, 200, 255), "noise")
create_texture(base_block + "purity_beacon.png", CYAN, WHITE, "noise")

# Item Textures
base_item = "src/main/resources/assets/liberthia/textures/item/"
create_texture(base_item + "dark_matter_shard.png", DARK_PURPLE, LIGHT_PURPLE, "shard")
create_texture(base_item + "holy_essence.png", GOLD, WHITE, "noise")
create_texture(base_item + "cleansing_grenade.png", WHITE, (180, 180, 180), "bomb")
create_texture(base_item + "clear_matter_injector.png", (240, 240, 240), CYAN, "injector")
create_texture(base_item + "white_light_wand.png", (139, 69, 19), WHITE, "staff")
create_texture(base_item + "white_matter_finder.png", (180, 180, 180), CYAN, "noise")
create_texture(base_item + "safe_siphon.png", (200, 200, 200), (100, 100, 255), "noise")
