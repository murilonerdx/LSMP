"""Gera texturas 18x18 para efeitos do Liberthia mod que ainda não têm sprite."""
from PIL import Image, ImageDraw, ImageFilter
import os

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources',
                   'assets', 'liberthia', 'textures', 'mob_effect')
SIZE = 18  # vanilla resolution


def new_img():
    return Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))


def draw_circle_filled(img, center, r, fill):
    d = ImageDraw.Draw(img)
    x, y = center
    d.ellipse([x - r, y - r, x + r, y + r], fill=fill)


def draw_outline(img, center, r, color):
    d = ImageDraw.Draw(img)
    x, y = center
    d.ellipse([x - r, y - r, x + r, y + r], outline=color)


def sanguine_vitality():
    """Coração de sangue com brilho."""
    img = new_img()
    d = ImageDraw.Draw(img)
    # heart shape: two circles + triangle
    red = (220, 30, 40, 255)
    dark = (130, 10, 20, 255)
    bright = (255, 120, 130, 255)
    # left lobe
    d.ellipse([3, 4, 9, 10], fill=red)
    # right lobe
    d.ellipse([9, 4, 15, 10], fill=red)
    # triangle bottom
    d.polygon([(3, 8), (15, 8), (9, 16)], fill=red)
    # outline
    d.ellipse([3, 4, 9, 10], outline=dark)
    d.ellipse([9, 4, 15, 10], outline=dark)
    d.line([(3, 8), (9, 16)], fill=dark)
    d.line([(15, 8), (9, 16)], fill=dark)
    # shine
    d.point((5, 6), fill=bright)
    d.point((6, 5), fill=bright)
    d.point((11, 5), fill=bright)
    img.save(os.path.join(OUT, 'sanguine_vitality.png'))


def blood_frenzy():
    """Lâmina vermelha com chamas de raiva."""
    img = new_img()
    d = ImageDraw.Draw(img)
    red = (200, 20, 30, 255)
    bright_red = (255, 80, 50, 255)
    dark = (90, 0, 0, 255)
    flame = (255, 140, 0, 255)
    # central blade vertical
    d.polygon([(8, 2), (10, 2), (11, 13), (9, 16), (7, 13)], fill=red)
    # outline blade
    d.line([(8, 2), (7, 13)], fill=dark)
    d.line([(10, 2), (11, 13)], fill=dark)
    d.line([(7, 13), (9, 16)], fill=dark)
    d.line([(11, 13), (9, 16)], fill=dark)
    # flames around
    d.point((4, 6), fill=flame)
    d.point((3, 8), fill=flame)
    d.point((4, 10), fill=bright_red)
    d.point((14, 6), fill=flame)
    d.point((15, 8), fill=flame)
    d.point((14, 10), fill=bright_red)
    d.point((5, 4), fill=bright_red)
    d.point((13, 4), fill=bright_red)
    img.save(os.path.join(OUT, 'blood_frenzy.png'))


def hemo_sickness():
    """Líquido podre de sangue (vermelho-marrom)."""
    img = new_img()
    d = ImageDraw.Draw(img)
    rot = (130, 40, 20, 255)
    rot_light = (180, 70, 50, 255)
    rot_dark = (70, 20, 10, 255)
    bile = (90, 100, 30, 255)
    # frasco / gota irregular
    d.polygon([(8, 2), (10, 2), (12, 5), (14, 9), (13, 14), (9, 16),
               (5, 14), (4, 9), (6, 5)], fill=rot)
    # bolhas / decay
    d.point((7, 7), fill=bile)
    d.point((10, 9), fill=bile)
    d.point((8, 12), fill=rot_dark)
    d.point((11, 6), fill=rot_light)
    # outline
    d.line([(8, 2), (6, 5)], fill=rot_dark)
    d.line([(10, 2), (12, 5)], fill=rot_dark)
    d.line([(4, 9), (5, 14)], fill=rot_dark)
    d.line([(14, 9), (13, 14)], fill=rot_dark)
    d.line([(5, 14), (9, 16)], fill=rot_dark)
    d.line([(13, 14), (9, 16)], fill=rot_dark)
    img.save(os.path.join(OUT, 'hemo_sickness.png'))


def infected_sight():
    """Olho infectado com íris violeta/verde."""
    img = new_img()
    d = ImageDraw.Draw(img)
    white = (220, 220, 200, 255)
    veins = (170, 30, 40, 255)
    iris = (110, 50, 160, 255)
    iris_glow = (180, 120, 220, 255)
    pupil = (10, 5, 15, 255)
    dark = (50, 30, 30, 255)
    # eye shape — almond
    d.polygon([(2, 9), (5, 5), (13, 5), (16, 9), (13, 13), (5, 13)],
              fill=white)
    # outline
    d.line([(2, 9), (5, 5)], fill=dark)
    d.line([(5, 5), (13, 5)], fill=dark)
    d.line([(13, 5), (16, 9)], fill=dark)
    d.line([(16, 9), (13, 13)], fill=dark)
    d.line([(13, 13), (5, 13)], fill=dark)
    d.line([(5, 13), (2, 9)], fill=dark)
    # iris
    d.ellipse([6, 6, 12, 12], fill=iris)
    d.ellipse([7, 7, 11, 11], fill=iris_glow)
    # pupil
    d.ellipse([8, 8, 10, 10], fill=pupil)
    # veins
    d.point((4, 8), fill=veins)
    d.point((5, 9), fill=veins)
    d.point((14, 10), fill=veins)
    d.point((15, 9), fill=veins)
    img.save(os.path.join(OUT, 'infected_sight.png'))


def blood_step():
    """Pegada de sangue."""
    img = new_img()
    d = ImageDraw.Draw(img)
    red = (180, 25, 30, 255)
    dark = (90, 10, 15, 255)
    bright = (255, 60, 60, 255)
    # heel oval
    d.ellipse([5, 8, 12, 16], fill=red)
    d.ellipse([5, 8, 12, 16], outline=dark)
    # toe dots
    d.ellipse([6, 4, 8, 6], fill=red)
    d.ellipse([8, 3, 10, 5], fill=red)
    d.ellipse([10, 4, 12, 6], fill=red)
    d.ellipse([4, 5, 6, 7], fill=red)
    d.ellipse([12, 5, 14, 7], fill=red)
    # highlight
    d.point((8, 11), fill=bright)
    d.point((9, 12), fill=bright)
    img.save(os.path.join(OUT, 'blood_step.png'))


def feather_fall():
    """Pena branca/azul fofa."""
    img = new_img()
    d = ImageDraw.Draw(img)
    white = (240, 245, 255, 255)
    blue = (140, 180, 230, 255)
    blue_dark = (70, 100, 160, 255)
    # spine vertical curved
    d.line([(9, 2), (9, 15)], fill=blue_dark)
    # barbs (left/right) — diagonal lines forming feather
    for y_off, length in [(3, 1), (5, 2), (7, 3), (9, 4), (11, 4), (13, 3)]:
        # left side
        for dx in range(1, length + 1):
            d.point((9 - dx, y_off + dx), fill=white if dx % 2 else blue)
        # right side
        for dx in range(1, length + 1):
            d.point((9 + dx, y_off + dx), fill=white if dx % 2 else blue)
    # tip
    d.point((9, 16), fill=blue_dark)
    img.save(os.path.join(OUT, 'feather_fall.png'))


def shield_resistance(name, base_color, glow_color):
    """Escudo colorido para resistências de matter."""
    img = new_img()
    d = ImageDraw.Draw(img)
    dark = tuple(max(0, c - 80) for c in base_color[:3]) + (255,)
    # shield shape — top wider, bottom point
    d.polygon([(4, 3), (14, 3), (14, 9), (9, 16), (4, 9)], fill=base_color)
    # outline
    d.line([(4, 3), (14, 3)], fill=dark)
    d.line([(4, 3), (4, 9)], fill=dark)
    d.line([(14, 3), (14, 9)], fill=dark)
    d.line([(4, 9), (9, 16)], fill=dark)
    d.line([(14, 9), (9, 16)], fill=dark)
    # cross-glow inside
    d.line([(9, 5), (9, 12)], fill=glow_color)
    d.line([(6, 8), (12, 8)], fill=glow_color)
    # shine top-left
    d.point((6, 5), fill=(255, 255, 255, 255))
    d.point((7, 5), fill=(255, 255, 255, 255))
    img.save(os.path.join(OUT, name + '.png'))


def dark_matter_resistance():
    """Escudo violeta — DM resistance."""
    shield_resistance('dark_matter_resistance',
                      (170, 96, 255, 255),  # 0xAA60FF
                      (220, 180, 255, 255))


def clear_matter_resistance():
    """Escudo branco-perolado — WM resistance."""
    shield_resistance('clear_matter_resistance',
                      (176, 232, 255, 255),  # 0xB0E8FF
                      (255, 255, 255, 255))


def yellow_matter_resistance():
    """Escudo dourado — YM resistance."""
    shield_resistance('yellow_matter_resistance',
                      (255, 210, 63, 255),  # 0xFFD23F
                      (255, 240, 150, 255))


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    sanguine_vitality()
    blood_frenzy()
    hemo_sickness()
    infected_sight()
    blood_step()
    feather_fall()
    dark_matter_resistance()
    clear_matter_resistance()
    yellow_matter_resistance()
    print(f'Generated 9 textures in {OUT}')
