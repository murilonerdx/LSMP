from PIL import Image, ImageDraw
import os

BASE = r"C:\Users\T-GAMER\Desktop\liberthia_mod\src\main\resources\assets\liberthia\textures"

def save(img, path):
    full = f"{BASE}/{path}"
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)
    print("saved", path)

# Geiger Counter - yellow/black industrial device
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
# Main body - yellow casing
d.rectangle([2, 3, 13, 14], fill=(200, 180, 60, 255), outline=(100, 90, 20, 255))
# Top handle
d.rectangle([5, 1, 10, 3], fill=(80, 80, 80, 255), outline=(40, 40, 40, 255))
# Screen - green
d.rectangle([3, 5, 12, 9], fill=(30, 80, 40, 255), outline=(20, 40, 20, 255))
# Screen display - radiation symbol
d.point((6, 6), fill=(200, 255, 100, 255))
d.point((9, 6), fill=(200, 255, 100, 255))
d.point((7, 7), fill=(200, 255, 100, 255))
d.point((8, 7), fill=(200, 255, 100, 255))
d.rectangle([5, 8, 10, 8], fill=(100, 200, 80, 255))
# Dial/buttons
d.ellipse([3, 10, 5, 12], fill=(150, 30, 30, 255), outline=(50, 10, 10, 255))
d.ellipse([10, 10, 12, 12], fill=(30, 30, 150, 255), outline=(10, 10, 50, 255))
# Speaker grille
d.point((6, 11), fill=(50, 50, 50, 255))
d.point((7, 11), fill=(50, 50, 50, 255))
d.point((8, 11), fill=(50, 50, 50, 255))
d.point((6, 12), fill=(50, 50, 50, 255))
d.point((7, 12), fill=(50, 50, 50, 255))
d.point((8, 12), fill=(50, 50, 50, 255))
d.point((6, 13), fill=(50, 50, 50, 255))
d.point((8, 13), fill=(50, 50, 50, 255))
# Radiation warning triangle
d.polygon([(12, 13), (14, 13), (13, 11)], fill=(40, 40, 40, 255))
d.point((13, 12), fill=(255, 220, 0, 255))
save(img, "item/geiger_counter.png")

# Clear Matter Pill - white/cyan capsule
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
# Capsule shape - horizontal
d.ellipse([2, 6, 7, 10], fill=(255, 255, 255, 255), outline=(180, 220, 240, 255))
d.ellipse([8, 6, 14, 10], fill=(100, 200, 230, 255), outline=(60, 140, 180, 255))
d.rectangle([5, 6, 10, 10], fill=(180, 230, 245, 255))
# Seam
d.line([(7, 6), (7, 10)], fill=(80, 120, 160, 255))
d.line([(8, 6), (8, 10)], fill=(80, 120, 160, 255))
# Glow on white side
d.point((3, 7), fill=(255, 255, 255, 255))
d.point((4, 7), fill=(250, 250, 255, 200))
# Cyan shimmer on other side
d.point((11, 7), fill=(200, 240, 255, 255))
d.point((12, 8), fill=(150, 220, 245, 255))
# Sparkle effect
d.point((5, 5), fill=(220, 255, 255, 255))
d.point((11, 4), fill=(200, 240, 255, 255))
d.point((2, 11), fill=(180, 230, 245, 255))
save(img, "item/clear_matter_pill.png")

# Host Journal / Field Journal - leather-bound book (cover)
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
# Book cover - dark brown leather
d.rectangle([2, 2, 13, 14], fill=(80, 45, 25, 255), outline=(40, 20, 10, 255))
# Spine
d.rectangle([2, 2, 3, 14], fill=(50, 28, 15, 255))
# Pages edge
d.rectangle([13, 3, 14, 13], fill=(230, 210, 170, 255))
# Emblem/symbol - golden eye
d.ellipse([5, 5, 10, 9], fill=(180, 140, 60, 255), outline=(100, 80, 30, 255))
d.ellipse([6, 6, 9, 8], fill=(60, 30, 20, 255))
d.point((7, 7), fill=(255, 220, 100, 255))
# Decorative lines
d.line([(4, 11), (11, 11)], fill=(140, 100, 40, 255))
d.line([(4, 12), (11, 12)], fill=(100, 70, 30, 255))
# Bookmark
d.rectangle([8, 13, 9, 15], fill=(180, 30, 30, 255))
# Corner wear
d.point((2, 2), fill=(30, 15, 5, 255))
d.point((13, 14), fill=(30, 15, 5, 255))
save(img, "item/field_journal.png")
save(img, "item/host_journal.png")

print("Done!")
