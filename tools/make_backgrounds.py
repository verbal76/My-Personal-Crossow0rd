#!/usr/bin/env python3
"""Procedurally generates the 25 puzzle backgrounds (5 categories x 5).

Original art made entirely from noise, gradients and drawn shapes: no photos, no text,
no logos, no third-party content. Deterministic (fixed seeds). Needs numpy, scipy, Pillow.

    python3 tools/make_backgrounds.py [output_dir]      # default: app/src/main/assets/backgrounds

Images are portrait 1200x2600, soft and low in fine detail on purpose, with the busiest
and brightest parts pushed toward the top and bottom edges so the crossword board in the
middle stays easy to read. The app adds a per-image dimming scrim on top (see
core/BackgroundCatalog.kt).
"""
import os, sys, math
import numpy as np
from scipy import ndimage as ndi
from PIL import Image, ImageDraw, ImageFilter

W, H = 1200, 2600
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'assets', 'backgrounds')

def rng(seed): return np.random.default_rng(seed)

def _up(g):
    return np.asarray(Image.fromarray(g.astype(np.float32), mode='F').resize((W, H), Image.BICUBIC))

def noise(seed, scale):
    r = rng(seed); gw = max(3, int(W / scale) + 3); gh = max(3, int(H / scale) + 3)
    return _up(r.random((gh, gw)))

def fbm(seed, scale, octaves=5, gain=0.55):
    t = np.zeros((H, W), np.float32); a = 1.0; tot = 0.0
    for i in range(octaves):
        t += a * noise(seed * 31 + i, max(4.0, scale / (2 ** i))); tot += a; a *= gain
    t /= tot
    lo, hi = np.percentile(t, 1), np.percentile(t, 99)
    return np.clip((t - lo) / (hi - lo + 1e-6), 0, 1)

def warp(img, seed, amount, scale=420):
    """Domain-warp a (H,W) or (H,W,3) array with smooth noise."""
    dx = (noise(seed + 101, scale) - 0.5) * 2 * amount; dy = (noise(seed + 202, scale) - 0.5) * 2 * amount
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    coords = [yy + dy, xx + dx]
    if img.ndim == 2: return ndi.map_coordinates(img, coords, order=1, mode='reflect')
    return np.stack([ndi.map_coordinates(img[..., c], coords, order=1, mode='reflect') for c in range(3)], -1)

def ramp(t, stops):
    pos = [p for p, _ in stops]; cols = np.array([c for _, c in stops], np.float32) / 255.0
    return np.stack([np.interp(t, pos, cols[:, i]) for i in range(3)], -1).astype(np.float32)

def vgrad(stops):
    t = np.linspace(0, 1, H, dtype=np.float32)[:, None] * np.ones((1, W), np.float32)
    return ramp(t, stops)

def yy_xx():
    y, x = np.mgrid[0:H, 0:W].astype(np.float32); return y / H, x / W

def blur(a, s):
    if a.ndim == 2: return ndi.gaussian_filter(a, s)
    return np.stack([ndi.gaussian_filter(a[..., c], s) for c in range(3)], -1)

def stars(seed, n, bright=1.0, tint=((255, 255, 255),), maxr=1.4, flare=0):
    r = rng(seed); layer = np.zeros((H, W, 3), np.float32)
    ys = r.integers(0, H, n); xs = r.integers(0, W, n)
    mag = (r.random(n) ** 3.2) * bright
    for y, x, m in zip(ys, xs, mag):
        c = np.array(tint[r.integers(0, len(tint))], np.float32) / 255.0
        layer[y, x] += c * (0.25 + 0.75 * m)
    small = blur(layer, 0.9) * 5.5
    out = small
    if flare:
        idx = np.argsort(-mag)[:flare]
        big = np.zeros((H, W, 3), np.float32)
        for i in idx: big[ys[i], xs[i]] += np.array(tint[0], np.float32) / 255.0
        g = blur(big, 5.0) * 70 + blur(big, 1.6) * 30
        out = out + g
    return out

def finish(img, vignette=0.28, contrast=0.92, lift=0.0, floor=0.0, ceil=0.97):
    """Tone-map for readability: soften contrast, vignette, grain-free."""
    img = np.clip(img, 0, 1)
    img = 0.5 + (img - 0.5) * contrast + lift
    y, x = yy_xx(); d = np.sqrt(((x - 0.5) / 0.75) ** 2 + ((y - 0.5) / 0.85) ** 2)
    img = img * (1 - vignette * np.clip(d - 0.35, 0, 1)[..., None] ** 1.3)
    img = np.clip(img, floor, ceil)
    return img

def save(name, img):
    os.makedirs(OUT, exist_ok=True)
    im = Image.fromarray((np.clip(img, 0, 1) * 255 + 0.5).astype(np.uint8), 'RGB')
    im.save(os.path.join(OUT, name + '.webp'), 'WEBP', quality=80, method=6)
    return im

# ───────────────────────────── SPACE ─────────────────────────────
def space_nebula():
    n1 = fbm(1, 1000, 4); n1 = warp(n1, 11, 110); n2 = fbm(2, 800, 4); n2 = warp(n2, 12, 90)
    base = ramp(n1, [(0, (6, 4, 28)), (0.35, (40, 14, 96)), (0.6, (150, 40, 150)), (0.8, (235, 110, 140)), (1, (255, 190, 160))])
    teal = ramp(n2, [(0, (0, 0, 0)), (0.5, (0, 40, 70)), (0.8, (20, 150, 190)), (1, (110, 230, 230))])
    mask = np.clip((n2 - 0.45) * 2.0, 0, 1)[..., None]
    img = base * 0.9 + teal * mask * 0.8
    img += stars(21, 1800, 0.9, ((255, 255, 255), (200, 220, 255), (255, 220, 200)), flare=6) * 0.8
    return finish(blur(img, 3) * 0.72, vignette=0.38, contrast=0.85)

def space_starfield():
    n = fbm(3, 900, 5)
    y, x = yy_xx(); band = np.exp(-(((x - 0.35 - 0.5 * y) * 2.4) ** 2)) * (0.35 + 0.65 * fbm(4, 260, 5))
    img = vgrad([(0, (3, 5, 22)), (0.5, (8, 12, 44)), (1, (5, 8, 30))])
    img += ramp(band, [(0, (0, 0, 0)), (1, (70, 80, 150))]) * 0.55
    img += stars(31, 3500, 1.0, ((255, 255, 255), (190, 210, 255), (255, 230, 200)), flare=9)
    img += stars(32, 14000, 0.4) * 0.45
    return finish(img, vignette=0.3)

def space_planet():
    y, x = yy_xx(); cx, cy, R = 0.5, 1.62, 1.0
    d = np.sqrt(((x - cx) * (W / H)) ** 2 + (y - cy) ** 2)
    img = vgrad([(0, (4, 6, 26)), (0.6, (9, 16, 52)), (1, (20, 36, 90))])
    img += stars(41, 2200, 0.9, flare=4) * (1 - np.clip(y * 1.1, 0, 1))[..., None] * 0.9
    glow = np.exp(-np.clip(d - R, 0, None) * 9.0) * (d > R - 0.01)
    atmos = ramp(np.clip(glow, 0, 1), [(0, (0, 0, 0)), (0.3, (30, 80, 190)), (0.7, (110, 190, 255)), (1, (230, 245, 255))])
    img = img * (1 - np.clip(glow, 0, 1)[..., None] * 0.5) + atmos * 0.85
    surf = fbm(42, 300, 5); surf = warp(surf, 43, 90)
    planet = ramp(surf, [(0, (14, 28, 70)), (0.5, (30, 70, 130)), (1, (80, 130, 190))]) * (0.55 + 0.45 * (1 - np.clip((d - 0.8) * 3, 0, 1))[..., None])
    inside = (d <= R)[..., None].astype(np.float32); inside = blur(inside[..., 0], 2.0)[..., None]
    rim = np.exp(-np.abs(d - R) * 70)[..., None] * np.array([0.55, 0.8, 1.0], np.float32)
    img = img * (1 - inside) + planet * inside + rim * 0.9
    return finish(img, vignette=0.3)

def space_galaxy():
    y, x = yy_xx(); u = (x - 0.5) * (W / H) * 2.0; v = (y - 0.38) * 2.0
    ang = -0.6; uu = u * math.cos(ang) - v * math.sin(ang); vv = (u * math.sin(ang) + v * math.cos(ang)) * 1.9
    r = np.sqrt(uu ** 2 + vv ** 2) + 1e-4; th = np.arctan2(vv, uu)
    arms = 0.5 + 0.5 * np.cos(2 * (th - 3.2 * np.log(r + 0.05)))
    dens = np.exp(-r * 2.2) * (0.25 + 0.75 * arms ** 1.6) + np.exp(-r * 9) * 1.4
    dens *= 0.6 + 0.8 * fbm(51, 90, 5)
    img = vgrad([(0, (3, 4, 18)), (1, (6, 8, 26))])
    col = ramp(np.clip(dens, 0, 1.5) / 1.5, [(0, (0, 0, 0)), (0.25, (60, 50, 140)), (0.55, (200, 140, 190)), (0.85, (255, 215, 170)), (1, (255, 250, 235))])
    img += col * 1.05
    img += stars(52, 3000, 1.0, ((255, 255, 255), (200, 215, 255)), flare=7)
    return finish(img, vignette=0.3)

def space_clouds():
    a = warp(fbm(61, 1100, 4), 62, 120); b = warp(fbm(63, 900, 4), 64, 90)
    y, x = yy_xx()
    img = ramp(a, [(0, (10, 8, 34)), (0.4, (60, 28, 100)), (0.62, (200, 80, 90)), (0.82, (255, 160, 90)), (1, (255, 225, 170))])
    cool = ramp(b, [(0, (0, 0, 0)), (0.55, (10, 40, 100)), (1, (90, 170, 255))])
    img = img * 0.8 + cool * np.clip((0.5 - a) * 1.6, 0, 1)[..., None] * 0.9
    img += stars(65, 1200, 0.8) * 0.6
    return finish(blur(img, 3) * 0.72, vignette=0.4, contrast=0.85)

# ───────────────────────────── SKY ─────────────────────────────
def cloud_layer(seed, scale, cover, soft=0.18, wf=120):
    t = warp(fbm(seed, scale, 6), seed + 7, wf)
    return np.clip((t - (1 - cover)) / soft, 0, 1)

def sky_bright():
    img = vgrad([(0, (40, 110, 214)), (0.55, (96, 168, 240)), (1, (176, 216, 250))])
    c = blur(cloud_layer(71, 950, 0.5, 0.32, 45), 5); shade = blur(c, 14)
    cl = ramp(np.clip(shade * 0.9 + (1 - shade) * 0.0, 0, 1), [(0, (255, 255, 255)), (1, (255, 255, 255))])
    lit = np.clip(1 - 0.35 * np.clip(blur(c, 40) - blur(c, 3), 0, 1) * 8, 0.62, 1)[..., None]
    img = img * (1 - c[..., None]) + cl * lit * c[..., None]
    c2 = blur(cloud_layer(72, 600, 0.3, 0.35, 40), 4) * 0.55; img = img * (1 - c2[..., None]) + np.array([1, 1, 1], np.float32) * c2[..., None]
    return finish(img * 0.97, vignette=0.12, contrast=0.9)

def sky_sunrise():
    y, x = yy_xx()
    img = vgrad([(0, (60, 76, 150)), (0.35, (150, 120, 180)), (0.62, (255, 170, 140)), (0.8, (255, 205, 130)), (1, (255, 230, 170))])
    sun = np.exp(-((((x - 0.5) * (W / H)) ** 2 + (y - 0.84) ** 2)) * 38)
    img += ramp(sun, [(0, (0, 0, 0)), (1, (255, 200, 120))]) * 0.9
    c = cloud_layer(81, 520, 0.4, 0.25) * np.clip((y - 0.45) * 2.4, 0, 1)
    tint = ramp(np.clip(y, 0, 1), [(0, (255, 210, 220)), (1, (255, 190, 130))])
    img = img * (1 - 0.8 * c[..., None]) + tint * 0.8 * c[..., None]
    return finish(img, vignette=0.15, contrast=0.9)

def sky_storm():
    t = warp(fbm(91, 520, 7, 0.6), 92, 240); t2 = warp(fbm(93, 260, 6), 94, 120)
    img = ramp(t * 0.8 + t2 * 0.2, [(0, (18, 24, 36)), (0.45, (52, 66, 88)), (0.75, (100, 116, 136)), (1, (176, 190, 205))])
    y, x = yy_xx(); gap = np.exp(-((((x - 0.62) * (W / H)) ** 2 + (y - 0.18) ** 2)) * 26)
    img += ramp(gap, [(0, (0, 0, 0)), (1, (230, 210, 160))]) * 0.65
    return finish(img * 0.88, vignette=0.3)

def sky_sunset():
    y, x = yy_xx()
    img = vgrad([(0, (46, 40, 108)), (0.3, (150, 70, 120)), (0.55, (240, 120, 70)), (0.75, (255, 176, 70)), (1, (255, 214, 120))])
    sun = np.exp(-((((x - 0.35) * (W / H)) ** 2 + (y - 0.8) ** 2)) * 70)
    img += ramp(np.clip(sun, 0, 1), [(0, (0, 0, 0)), (1, (255, 230, 150))]) * 0.9
    streak = warp(fbm(101, 900, 5)[:, :], 102, 100)
    sx = blur(noise(103, 1300) * 0 + fbm(104, 700, 5), (2, 90))
    c = np.clip((sx - 0.5) * 3.2, 0, 1) * np.clip((y - 0.25) * 2, 0, 1)
    img = img * (1 - 0.7 * c[..., None]) + ramp(y, [(0, (110, 50, 100)), (1, (255, 150, 80))]) * 0.7 * c[..., None]
    return finish(img, vignette=0.2, contrast=0.9)

def sky_twilight():
    y, x = yy_xx()
    img = vgrad([(0, (14, 18, 62)), (0.35, (52, 50, 128)), (0.65, (150, 90, 160)), (0.85, (240, 150, 150)), (1, (255, 196, 150))])
    cir = blur(fbm(111, 260, 5), (1.5, 70)); cir = warp(cir, 112, 90, 600)
    c = np.clip((cir - 0.48) * 3.0, 0, 1) * 0.55
    img = img * (1 - c[..., None]) + np.array([0.93, 0.84, 0.95], np.float32) * c[..., None] * (0.4 + 0.6 * y[..., None])
    img += stars(113, 500, 0.8) * np.clip(0.5 - y, 0, 1)[..., None] * 1.6
    return finish(img, vignette=0.22, contrast=0.9)

# ───────────────────────────── WOODS ─────────────────────────────
def trunks(img, seed, n, depth, fog, wmin, wmax, color, blur_s, sway=14):
    r = rng(seed); layer = Image.new('L', (W, H), 0); d = ImageDraw.Draw(layer)
    for _ in range(n):
        x = r.uniform(-60, W + 60); w = r.uniform(wmin, wmax); s = r.uniform(-sway, sway)
        d.polygon([(x - w / 2, H), (x + s - w * 0.36, 0), (x + s + w * 0.36, 0), (x + w / 2, H)], fill=255)
    m = blur(np.asarray(layer, np.float32) / 255.0, blur_s)
    shade = color * (0.8 + 0.4 * noise(seed + 5, 90)[..., None])
    mix = shade * (1 - fog) + fog * np.array([1, 1, 1], np.float32) * 0
    return img * (1 - m[..., None]) + shade * m[..., None]

def conifers(img, seed, n, color, blur_s, wscale=1.0):
    r = rng(seed); layer = Image.new('L', (W, H), 0); d = ImageDraw.Draw(layer)
    for _ in range(n):
        x = r.uniform(-40, W + 40); base = r.uniform(H * 0.55, H * 1.02); hgt = r.uniform(H * 0.45, H * 0.9) * wscale
        top = base - hgt; tiers = 14
        for i in range(tiers):
            ty = top + hgt * i / tiers; wid = (hgt * 0.11) * (0.25 + 0.75 * i / tiers)
            d.polygon([(x, ty - hgt * 0.035), (x - wid, ty + hgt / tiers * 1.5), (x + wid, ty + hgt / tiers * 1.5)], fill=255)
        d.rectangle([x - 6, min(base - 10, H - 2), x + 6, H], fill=255)
    m = blur(np.asarray(layer, np.float32) / 255.0, blur_s)
    return img * (1 - m[..., None]) + color * m[..., None]

def rays(seed, angle=0.32, n=7, strength=1.0):
    r = rng(seed); L = Image.new('L', (W, H), 0); d = ImageDraw.Draw(L)
    for _ in range(n):
        x0 = r.uniform(0.2, 1.3) * W; w = r.uniform(30, 120)
        d.polygon([(x0 - w * 0.2, -50), (x0 + w * 0.2, -50), (x0 - H * angle + w, H), (x0 - H * angle - w, H)], fill=int(r.uniform(90, 200)))
    a = blur(np.asarray(L, np.float32) / 255.0, 38)
    y = np.linspace(1, 0.1, H, dtype=np.float32)[:, None]
    return a * y * strength

def woods_green():
    img = vgrad([(0, (190, 226, 150)), (0.5, (110, 170, 90)), (1, (40, 90, 54))])
    for i, (n, w0, w1, bl, col, fg) in enumerate([(26, 26, 54, 12, (0.30, 0.46, 0.26), 0.5), (16, 40, 80, 7, (0.18, 0.32, 0.17), 0.3), (9, 66, 120, 3.5, (0.10, 0.20, 0.10), 0.1)]):
        img = img * (1 - fg * 0.5) + vgrad([(0, (200, 235, 160)), (1, (120, 170, 100))]) * (fg * 0.5)
        img = trunks(img, 120 + i, n, i, fg, w0, w1, np.array(col, np.float32), bl)
    img += rays(125, 0.3, 8) [..., None] * np.array([1.0, 0.95, 0.65], np.float32) * 0.55
    leaf = blur(np.clip((fbm(126, 180, 5) - 0.45) * 3, 0, 1), 10) * np.clip(1 - np.linspace(0, 1, H)[:, None] * 2.2, 0, 1)
    img = img * (1 - leaf[..., None] * 0.6) + np.array([0.18, 0.36, 0.14], np.float32) * leaf[..., None] * 0.6
    return finish(img, vignette=0.3)

def woods_mist():
    img = vgrad([(0, (200, 212, 206)), (0.6, (160, 178, 170)), (1, (110, 130, 122))])
    for i, (n, w0, w1, bl, col, fg) in enumerate([(24, 24, 50, 16, (0.58, 0.64, 0.60), 0.6), (15, 38, 76, 9, (0.40, 0.48, 0.44), 0.45), (8, 64, 110, 5, (0.24, 0.30, 0.28), 0.3)]):
        img = img * (1 - fg * 0.6) + vgrad([(0, (214, 222, 218)), (1, (170, 186, 178))]) * (fg * 0.6)
        img = trunks(img, 130 + i, n, i, fg, w0, w1, np.array(col, np.float32), bl)
    m = blur(fbm(135, 500, 5), (30, 90)); img = img * (1 - 0.35 * m[..., None]) + np.array([0.86, 0.9, 0.88], np.float32) * 0.35 * m[..., None]
    return finish(img, vignette=0.22, contrast=0.88)

def woods_autumn():
    img = vgrad([(0, (255, 214, 130)), (0.5, (214, 130, 60)), (1, (96, 46, 28))])
    for i, (n, w0, w1, bl, col, fg) in enumerate([(26, 26, 54, 12, (0.55, 0.32, 0.18), 0.5), (16, 40, 80, 7, (0.34, 0.18, 0.10), 0.3), (9, 66, 120, 3.5, (0.2, 0.1, 0.07), 0.1)]):
        img = img * (1 - fg * 0.5) + vgrad([(0, (255, 220, 150)), (1, (220, 150, 80))]) * (fg * 0.5)
        img = trunks(img, 140 + i, n, i, fg, w0, w1, np.array(col, np.float32), bl)
    leaves = blur(np.clip((fbm(145, 120, 5) - 0.5) * 4, 0, 1), 6)
    pal = ramp(fbm(146, 320, 3), [(0, (170, 40, 20)), (0.5, (232, 120, 30)), (1, (255, 190, 60))])
    top = np.clip(1 - np.linspace(0, 1, H)[:, None] * 1.6, 0, 1) + 0.15
    mk = np.clip(leaves * top, 0, 1)[..., None]; img = img * (1 - mk * 0.8) + pal * mk * 0.8
    img += rays(147, 0.3, 6)[..., None] * np.array([1, 0.8, 0.5], np.float32) * 0.4
    return finish(img * 0.95, vignette=0.32)

def woods_evergreen():
    img = vgrad([(0, (50, 84, 92)), (0.5, (26, 54, 60)), (1, (10, 24, 28))])
    img = img + blur(fbm(150, 600, 5), (30, 80))[..., None] * np.array([0.12, 0.2, 0.2], np.float32)
    img = conifers(img, 151, 14, np.array([0.12, 0.24, 0.26], np.float32), 14, 0.9)
    img = conifers(img, 152, 10, np.array([0.07, 0.15, 0.17], np.float32), 7, 1.0)
    img = conifers(img, 153, 7, np.array([0.035, 0.085, 0.09], np.float32), 2.5, 1.1)
    img += rays(154, 0.2, 4, 0.7)[..., None] * np.array([0.5, 0.8, 0.8], np.float32) * 0.35
    return finish(img * 1.05, vignette=0.28)

def woods_shafts():
    img = vgrad([(0, (60, 96, 70)), (0.5, (30, 56, 40)), (1, (14, 28, 20)) ])
    for i, (n, w0, w1, bl, col) in enumerate([(22, 24, 46, 10, (0.12, 0.2, 0.13)), (13, 40, 76, 5, (0.07, 0.12, 0.08)), (7, 64, 110, 2.5, (0.04, 0.07, 0.05))]):
        img = trunks(img, 160 + i, n, i, 0.2, w0, w1, np.array(col, np.float32), bl)
    sh = rays(165, 0.34, 9, 1.6) + 0.5 * rays(166, 0.25, 5, 1.0)
    img = img + sh[..., None] * np.array([1.0, 0.92, 0.6], np.float32) * 0.55
    dust = stars(167, 500, 0.8, ((255, 240, 200),)) * 0.35 * np.clip(sh * 3, 0, 1)[..., None]
    return finish(img + dust, vignette=0.34)

# ───────────────────────────── MOTION CITY ─────────────────────────────
def trails(img, seed, specs, glow=22):
    lay = Image.new('RGB', (W, H), (0, 0, 0)); d = ImageDraw.Draw(lay)
    for pts, width, color in specs: d.line(pts, fill=color, width=width, joint='curve')
    a = np.asarray(lay, np.float32) / 255.0
    return img + blur(a, 1.2) * 0.8 + blur(a, glow) * 1.4 + blur(a, glow * 3) * 0.7

def curve(p0, p1, p2, steps=60):
    pts = []
    for i in range(steps + 1):
        t = i / steps; x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]; y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
        pts.append((x, y))
    return pts

def city_traffic():
    r = rng(171); img = vgrad([(0, (8, 12, 34)), (0.5, (14, 18, 48)), (1, (6, 8, 22))])
    vx, vy = W * 0.5, H * 0.46; specs = []
    for i in range(46):
        side = r.choice([-1, 1]); off = r.uniform(0.04, 0.5) * W * side
        col = (255, int(r.uniform(40, 90)), int(r.uniform(40, 80))) if side > 0 else (255, int(r.uniform(190, 235)), int(r.uniform(140, 190)))
        y_end = r.uniform(H * 0.58, H * 1.05); y_start = vy + r.uniform(0, 80)
        pts = curve((vx + off * 0.04, y_start), (vx + off * 0.5, (y_start + y_end) / 2 + r.uniform(-80, 80)), (vx + off * 2.2, y_end))
        specs.append((pts, int(r.uniform(5, 16)), col))
    img = trails(img, 172, specs, 18)
    skyline = Image.new('L', (W, H), 0); d = ImageDraw.Draw(skyline); x = 0
    while x < W:
        bw = int(r.uniform(50, 120)); bh = int(r.uniform(80, 360)); d.rectangle([x, vy - bh, x + bw, vy + 10], fill=255); x += bw + int(r.uniform(0, 10))
    sk = blur(np.asarray(skyline, np.float32) / 255.0, (2, 14))
    img = img * (1 - sk[..., None] * 0.9) + np.array([0.04, 0.05, 0.12], np.float32) * sk[..., None] * 0.9
    wins = stars(173, 700, 0.8, ((255, 214, 150), (150, 200, 255))) * (sk > 0.5)[..., None] * 0.7
    return finish(img + wins, vignette=0.34)

def city_blur_street():
    r = rng(181); img = vgrad([(0, (12, 16, 40)), (0.5, (30, 24, 56)), (1, (14, 12, 28))])
    lay = np.zeros((H, W, 3), np.float32)
    cols = [(255, 170, 70), (255, 90, 80), (90, 170, 255), (255, 230, 150), (255, 120, 190)]
    for _ in range(260):
        x = r.uniform(0, W); y = r.uniform(H * 0.12, H * 0.92); rad = r.uniform(5, 26); c = np.array(cols[r.integers(0, len(cols))], np.float32) / 255.0
        yy0, yy1 = int(max(0, y - rad)), int(min(H, y + rad)); xx0, xx1 = int(max(0, x - rad)), int(min(W, x + rad))
        lay[yy0:yy1, xx0:xx1] += c * r.uniform(0.3, 1.0)
    streak = blur(lay, (24, 2)) * 1.0 + blur(lay, (90, 6)) * 0.8
    verts = blur(lay, (1.5, 30)) * 0.8
    img = img + streak * 1.4 + verts
    return finish(img * 0.9, vignette=0.34)

def city_glass():
    r = rng(191); img = vgrad([(0, (18, 30, 62)), (0.5, (30, 44, 84)), (1, (10, 18, 40)) ])
    lay = np.zeros((H, W, 3), np.float32); yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    cols = [(255, 190, 110), (150, 210, 255), (255, 120, 150), (255, 245, 200), (130, 255, 220)]
    for _ in range(90):
        x = r.uniform(0, W); y = r.uniform(H * 0.1, H * 0.95); rad = r.uniform(36, 130); c = np.array(cols[r.integers(0, len(cols))], np.float32) / 255.0
        y0, y1, x0, x1 = int(max(0, y - rad)), int(min(H, y + rad)), int(max(0, x - rad)), int(min(W, x + rad))
        sub = np.sqrt((yy[y0:y1, x0:x1] - y) ** 2 + (xx[y0:y1, x0:x1] - x) ** 2) / rad
        disc = np.clip(1 - sub, 0, 1) ** 0.35 * (1 + 0.4 * np.clip(sub - 0.8, 0, 1) * 5)
        lay[y0:y1, x0:x1] += c[None, None, :] * disc[..., None] * r.uniform(0.18, 0.5)
    img = img + blur(lay, 3) * 0.9
    drops = np.zeros((H, W), np.float32)
    for _ in range(70):
        x = r.uniform(0, W); y = r.uniform(0, H); a = r.uniform(5, 14)
        drops[int(y), int(x)] = 1
    dr = blur(drops, 5.5); img += (dr / (dr.max() + 1e-6))[..., None] * np.array([0.5, 0.65, 0.8], np.float32) * 0.5
    return finish(img, vignette=0.34)

def city_neon():
    r = rng(201); img = vgrad([(0, (8, 6, 26)), (0.5, (16, 10, 42)), (1, (6, 6, 22))]); specs = []
    cols = [(255, 40, 170), (40, 220, 255), (170, 90, 255), (255, 120, 60)]
    for i in range(40):
        x0 = r.uniform(-0.2, 1.2) * W; y0 = r.uniform(0, H); L = r.uniform(500, 1500); a = -0.62 + r.uniform(-0.08, 0.08)
        pts = curve((x0, y0), (x0 + L * 0.5 * math.cos(a) + r.uniform(-60, 60), y0 + L * 0.5 * math.sin(a)), (x0 + L * math.cos(a), y0 + L * math.sin(a)))
        specs.append((pts, int(r.uniform(4, 20)), cols[r.integers(0, len(cols))]))
    img = trails(img, 202, specs, 16)
    return finish(img * 0.9, vignette=0.36)

def city_warm_motion():
    r = rng(211); img = vgrad([(0, (22, 14, 36)), (0.5, (46, 24, 44)), (1, (16, 10, 26))])
    lay = np.zeros((H, W, 3), np.float32); cols = [(255, 190, 90), (255, 130, 70), (255, 230, 170), (255, 90, 90)]
    for _ in range(170):
        x = r.uniform(0, W); y = r.uniform(H * 0.1, H * 0.95); c = np.array(cols[r.integers(0, len(cols))], np.float32) / 255.0
        ln = int(r.uniform(80, 360)); lay[int(y), int(max(0, x - ln)):int(min(W, x))] += c * r.uniform(0.2, 0.9)
    img = img + blur(lay, (3.5, 12)) * 7 + blur(lay, (16, 60)) * 9
    return finish(img * 0.9, vignette=0.34)

# ───────────────────────────── OCEAN ─────────────────────────────
def caustics(seed, scale=160):
    a = fbm(seed, scale, 4); b = fbm(seed + 1, scale * 0.8, 4)
    c = 1 - np.abs(a - b) * 3.2
    return np.clip(c, 0, 1) ** 3

def ocean_tropical():
    y, x = yy_xx()
    img = vgrad([(0, (40, 200, 214)), (0.5, (30, 170, 200)), (1, (20, 110, 160))])
    sand = np.clip((0.2 - y) * 4, 0, 1) * blur(fbm(221, 300, 4), 20)
    img = img * (1 - sand[..., None] * 0.55) + np.array([0.9, 0.88, 0.74], np.float32) * sand[..., None] * 0.55
    c = blur(caustics(222, 190), 2.5); img += c[..., None] * np.array([0.35, 0.55, 0.5], np.float32) * 0.4 * (0.6 + 0.4 * np.clip(1 - y, 0, 1))[..., None]
    return finish(img, vignette=0.2, contrast=0.9)

def ocean_underwater():
    y, x = yy_xx(); img = vgrad([(0, (30, 130, 190)), (0.4, (10, 70, 130)), (1, (4, 20, 60))])
    img += rays(231, 0.18, 9, 1.4)[..., None] * np.array([0.3, 0.6, 0.8], np.float32) * 0.55
    r = rng(232); b = np.zeros((H, W), np.float32)
    for _ in range(80): b[int(r.uniform(0, H)), int(r.uniform(0, W))] = r.uniform(0.4, 1.0)
    bub = blur(b, 4.0); ring = bub - blur(b, 2.4) * 0.9
    img += np.clip(bub * 8, 0, 1)[..., None] * np.array([0.5, 0.75, 0.9], np.float32) * 0.28
    return finish(img, vignette=0.3)

def ocean_waves():
    y, x = yy_xx(); hor = 0.34
    sky = vgrad([(0, (112, 176, 230)), (0.34, (206, 232, 250)), (1, (206, 232, 250))])
    img = sky.copy(); sea = ramp(np.clip((y - hor) / (1 - hor), 0, 1), [(0, (60, 150, 190)), (0.5, (24, 100, 150)), (1, (8, 52, 96))])
    wave = fbm(241, 260, 5); wave2 = blur(fbm(242, 200, 4), (1.5, 30)); band = np.sin((y - hor) * 60 * (1 + (y - hor) * 3) + wave * 6) * 0.5 + 0.5
    sea = sea * (0.85 + 0.25 * band[..., None] * np.clip((y - hor) * 2, 0, 1)[..., None]) + np.clip(wave2 - 0.6, 0, 1)[..., None] * 0.2
    m = blur((y > hor).astype(np.float32), 1.5)[..., None]
    img = img * (1 - m) + sea * m
    cl = cloud_layer(243, 520, 0.35) * np.clip((hor + 0.05 - y) * 4, 0, 1); img = img * (1 - cl[..., None] * 0.7) + cl[..., None] * 0.7
    return finish(img, vignette=0.2, contrast=0.9)

def ocean_reef():
    y, x = yy_xx(); img = vgrad([(0, (30, 170, 190)), (0.5, (14, 100, 150)), (1, (6, 40, 80))])
    img += rays(251, 0.15, 7, 0.9)[..., None] * np.array([0.3, 0.6, 0.6], np.float32) * 0.5
    r = rng(252); lay = Image.new('L', (W, H), 0); d = ImageDraw.Draw(lay)
    for _ in range(26):
        cx = r.uniform(-40, W + 40); cy = r.uniform(H * 0.72, H * 1.02)
        for _ in range(int(r.uniform(5, 12))):
            ang = r.uniform(-1.2, 1.2) - math.pi / 2; L = r.uniform(80, 280); x1 = cx + math.cos(ang) * L; y1 = cy + math.sin(ang) * L
            d.line([(cx, cy), (x1, y1)], fill=255, width=int(r.uniform(14, 40)))
    m = blur(np.asarray(lay, np.float32) / 255.0, 6)
    col = ramp(fbm(253, 240, 3), [(0, (10, 36, 70)), (0.5, (40, 70, 110)), (1, (170, 80, 110))]) * 0.55
    img = img * (1 - m[..., None] * 0.85) + col * m[..., None] * 0.85
    img += caustics(254, 140)[..., None] * np.array([0.25, 0.45, 0.45], np.float32) * 0.35 * np.clip(1 - y * 1.1, 0, 1)[..., None]
    return finish(img, vignette=0.3)

def ocean_sunset():
    y, x = yy_xx(); hor = 0.52
    sky = vgrad([(0, (60, 52, 130)), (0.25, (190, 80, 120)), (0.5, (255, 150, 70)), (1, (255, 200, 110))])
    sun = np.exp(-((((x - 0.5) * (W / H)) ** 2 + (y - hor + 0.03) ** 2)) * 90)
    sky += ramp(np.clip(sun, 0, 1), [(0, (0, 0, 0)), (1, (255, 236, 170))]) * 0.9
    sea = ramp(np.clip((y - hor) / (1 - hor), 0, 1), [(0, (230, 130, 90)), (0.35, (110, 66, 110)), (1, (22, 24, 70))])
    refl = np.exp(-((x - 0.5) * 7) ** 2) * np.clip(1 - (y - hor) * 1.6, 0, 1) * (0.5 + 0.5 * noise(261, 1) * 0 + 0.5 * blur(fbm(262, 120, 4), (0.8, 18)))
    sea += refl[..., None] * np.array([1.0, 0.75, 0.4], np.float32) * 0.8
    m = blur((y > hor).astype(np.float32), 1.5)[..., None]
    img = sky * (1 - m) + sea * m
    return finish(img, vignette=0.22, contrast=0.9)

GENERATORS = [
    ('space_1_nebula_bloom', space_nebula), ('space_2_deep_starfield', space_starfield), ('space_3_planet_horizon', space_planet),
    ('space_4_distant_galaxy', space_galaxy), ('space_5_cosmic_clouds', space_clouds),
    ('sky_1_bright_blue_clouds', sky_bright), ('sky_2_sunrise', sky_sunrise), ('sky_3_storm_clouds', sky_storm),
    ('sky_4_golden_sunset', sky_sunset), ('sky_5_twilight_cirrus', sky_twilight),
    ('woods_1_sunlit_forest', woods_green), ('woods_2_misty_woodland', woods_mist), ('woods_3_autumn_forest', woods_autumn),
    ('woods_4_dark_evergreen', woods_evergreen), ('woods_5_light_shafts', woods_shafts),
    ('motioncity_1_traffic_trails', city_traffic), ('motioncity_2_blurred_downtown', city_blur_street), ('motioncity_3_city_through_glass', city_glass),
    ('motioncity_4_neon_streaks', city_neon), ('motioncity_5_urban_motion', city_warm_motion),
    ('ocean_1_tropical_water', ocean_tropical), ('ocean_2_underwater_blue', ocean_underwater), ('ocean_3_wave_level', ocean_waves),
    ('ocean_4_reef_depth', ocean_reef), ('ocean_5_sunset_water', ocean_sunset),
]

if __name__ == '__main__':
    only = set(sys.argv[2:])
    for name, fn in GENERATORS:
        if only and name not in only: continue
        im = save(name, fn()); print(name, os.path.getsize(os.path.join(OUT, name + '.webp')) // 1024, 'KB', flush=True)
