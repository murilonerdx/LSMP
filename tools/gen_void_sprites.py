"""r165: Generate sprite frames for 5 void spell entities."""
from PIL import Image, ImageDraw
import os, math, random

BASE_DIR = r'C:/Users/T-GAMER/Desktop/liberthia_mod/src/main/resources/assets/liberthia/textures/entity'

# Palette unified across void spells
DEEP = (35, 10, 60, 255)
MID = (90, 40, 130, 255)
BRIGHT = (170, 80, 230, 255)
CYAN = (100, 240, 255, 220)
PALE = (220, 200, 255, 255)
BLACK = (5, 0, 10, 255)
RED_HORROR = (200, 30, 60, 255)
EYE_YELLOW = (255, 220, 100, 255)


# ────────────── MIND SPIKE (5 frames, 16x32 vertical spike) ──────────────
def gen_mind_spike():
    out = os.path.join(BASE_DIR, 'mind_spike')
    os.makedirs(out, exist_ok=True)
    W, H = 16, 32
    for frame in range(5):
        img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        intensity = [0.5, 1.0, 1.0, 1.0, 0.6][frame]
        for y in range(H):
            progress = y / H
            tip_w = int(2 + progress * 5)
            for dx in range(-tip_w, tip_w + 1):
                x = W // 2 + dx
                if 0 <= x < W:
                    d_outside = abs(dx) / max(1, tip_w)
                    if d_outside < 0.3:
                        d.point((x, y), fill=PALE)
                    elif d_outside < 0.6:
                        d.point((x, y), fill=BRIGHT)
                    elif d_outside < 0.85:
                        d.point((x, y), fill=MID)
                    else:
                        if random.random() < intensity:
                            d.point((x, y), fill=DEEP)
        for r in range(3):
            for theta_deg in range(0, 360, 30):
                t = math.radians(theta_deg)
                px = W // 2 + int(math.cos(t) * r)
                py = 2 + int(math.sin(t) * r * 0.6)
                if 0 <= px < W and 0 <= py < H:
                    d.point((px, py), fill=CYAN if random.random() < 0.6 else PALE)
        if frame in [1, 2]:
            ey = H // 2
            d.point((W // 2, ey), fill=EYE_YELLOW)
            d.point((W // 2 - 1, ey), fill=BLACK)
            d.point((W // 2 + 1, ey), fill=BLACK)
        img.save(os.path.join(out, 'spike_{}.png'.format(frame)))
    print('mind_spike: 5 frames')


# ────────────── SOUL TEAR (6 frames, 32x32 aura around target) ──────────────
def gen_soul_tear():
    out = os.path.join(BASE_DIR, 'soul_tear')
    os.makedirs(out, exist_ok=True)
    W, H = 32, 32
    for frame in range(6):
        img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        cx, cy = W // 2, H // 2
        rotation = frame * 30
        for tendril in range(4):
            base_angle = tendril * 90 + rotation
            for step in range(20):
                t = step / 20
                a = math.radians(base_angle + t * 180)
                r = 4 + t * 11
                px = cx + int(math.cos(a) * r)
                py = cy + int(math.sin(a) * r)
                if 0 <= px < W and 0 <= py < H:
                    if step < 5:
                        col = PALE
                    elif step < 12:
                        col = BRIGHT
                    else:
                        col = MID
                    d.point((px, py), fill=col)
                if 0 < px - 1 < W and 0 <= py < H:
                    d.point((px - 1, py), fill=DEEP)
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if dx * dx + dy * dy <= 4:
                    d.point((cx + dx, cy + dy), fill=BLACK)
        d.point((cx, cy), fill=EYE_YELLOW)
        img.save(os.path.join(out, 'tear_{}.png'.format(frame)))
    print('soul_tear: 6 frames')


# ────────────── MADNESS WAVE (8 frames, 64x16 horizontal ring) ──────────────
def gen_madness_wave():
    out = os.path.join(BASE_DIR, 'madness_wave')
    os.makedirs(out, exist_ok=True)
    W, H = 64, 16
    for frame in range(8):
        img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        for x in range(W):
            dist = abs(x - (frame * 8 + 4))
            if dist < 12:
                intensity = max(0, 1 - dist / 12)
                for y in range(H):
                    y_progress = abs(y - H // 2) / (H // 2)
                    if y_progress > 0.85:
                        continue
                    alpha = intensity * (1 - y_progress)
                    if alpha > 0.7:
                        d.point((x, y), fill=PALE)
                    elif alpha > 0.4:
                        d.point((x, y), fill=BRIGHT)
                    elif alpha > 0.2:
                        d.point((x, y), fill=MID)
                    elif alpha > 0.05:
                        d.point((x, y), fill=DEEP)
        if frame in [2, 3, 4, 5]:
            seed_random = random.Random(frame * 73)
            for _ in range(5):
                ex = seed_random.randint(W // 4, W * 3 // 4)
                ey = seed_random.randint(2, H - 3)
                if abs(ex - (frame * 8 + 4)) < 10:
                    d.point((ex, ey), fill=EYE_YELLOW)
                    d.point((ex - 1, ey), fill=BLACK)
                    d.point((ex + 1, ey), fill=BLACK)
        img.save(os.path.join(out, 'wave_{}.png'.format(frame)))
    print('madness_wave: 8 frames')


# ────────────── COSMIC VOID (10 frames, 64x64 vortex with eye) ──────────────
def gen_cosmic_void():
    out = os.path.join(BASE_DIR, 'cosmic_void')
    os.makedirs(out, exist_ok=True)
    W, H = 64, 64
    for frame in range(10):
        img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        cx, cy = W // 2, H // 2
        rotation = frame * 36
        for r in range(28, 32):
            for theta_deg in range(0, 360, 4):
                t = math.radians(theta_deg)
                px = cx + int(math.cos(t) * r)
                py = cy + int(math.sin(t) * r)
                if 0 <= px < W and 0 <= py < H:
                    d.point((px, py), fill=DEEP if random.random() < 0.5 else MID)
        for arm in range(3):
            base_angle = arm * 120 + rotation
            for step in range(45):
                t = step / 45
                a = math.radians(base_angle + t * 270)
                r = 4 + t * 26
                px = cx + int(math.cos(a) * r)
                py = cy + int(math.sin(a) * r)
                if 0 <= px < W and 0 <= py < H:
                    if step < 8:
                        col = PALE
                    elif step < 18:
                        col = CYAN
                    elif step < 30:
                        col = BRIGHT
                    else:
                        col = MID
                    d.point((px, py), fill=col)
                    for dy in range(-1, 2):
                        for dx in range(-1, 2):
                            tx, ty = px + dx, py + dy
                            if 0 <= tx < W and 0 <= ty < H and (dx != 0 or dy != 0):
                                if random.random() < 0.4:
                                    d.point((tx, ty), fill=DEEP)
        for dy in range(-5, 6):
            for dx in range(-5, 6):
                d2 = dx * dx + dy * dy
                if d2 < 20:
                    d.point((cx + dx, cy + dy), fill=BLACK)
                elif d2 < 28:
                    d.point((cx + dx, cy + dy), fill=DEEP)
        eye_open = abs(math.sin(frame * 0.6))
        eye_h = int(2 + eye_open * 3)
        d.rectangle([cx - 4, cy - eye_h // 2, cx + 4, cy + eye_h // 2], fill=BLACK)
        if eye_open > 0.3:
            d.rectangle([cx - 3, cy - eye_h // 2 + 1, cx + 3, cy + eye_h // 2 - 1], fill=EYE_YELLOW)
            d.point((cx, cy), fill=BLACK)
            if eye_open > 0.7:
                d.point((cx - 1, cy), fill=BLACK)
                d.point((cx + 1, cy), fill=BLACK)
        img.save(os.path.join(out, 'vortex_{}.png'.format(frame)))
    print('cosmic_void: 10 frames')


# ────────────── ELDRITCH BLAST IMPACT (4 frames, 32x32 burst) ──────────────
def gen_eldritch_blast():
    out = os.path.join(BASE_DIR, 'eldritch_blast')
    os.makedirs(out, exist_ok=True)
    W, H = 32, 32
    for frame in range(4):
        img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        cx, cy = W // 2, H // 2
        max_r = 4 + frame * 5
        for r in range(max_r - 2, max_r + 1):
            for theta_deg in range(0, 360, 6):
                t = math.radians(theta_deg)
                px = cx + int(math.cos(t) * r)
                py = cy + int(math.sin(t) * r)
                if 0 <= px < W and 0 <= py < H:
                    if r == max_r:
                        d.point((px, py), fill=PALE)
                    else:
                        d.point((px, py), fill=BRIGHT)
        for k in range(8):
            theta = math.radians(k * 45 + frame * 11)
            for step in range(max_r):
                px = cx + int(math.cos(theta) * step)
                py = cy + int(math.sin(theta) * step)
                if 0 <= px < W and 0 <= py < H:
                    d.point((px, py), fill=CYAN if step < max_r // 2 else MID)
        img.save(os.path.join(out, 'blast_{}.png'.format(frame)))
    print('eldritch_blast: 4 frames')


if __name__ == '__main__':
    gen_mind_spike()
    gen_soul_tear()
    gen_madness_wave()
    gen_cosmic_void()
    gen_eldritch_blast()
    print('ALL DONE')
