#!/usr/bin/env python3
"""Derives the desktop-only assets from the shared Android/branding masters (deterministic).

  * desktop/src/main/resources/backgrounds/*.jpg  <- app/src/main/assets/backgrounds/*.webp
    (Java's ImageIO cannot read WebP; same pictures, same file names, same catalogue)
  * desktop/packaging/icon.png (512) and icon.ico (16..256) <- branding/my_personal_crossword_app_icon.png
    (the master has baked-in rounded black corners; they are made transparent)

Run after changing the masters:  python3 tools/make_desktop_assets.py
"""
import glob, os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
out = os.path.join(ROOT, 'desktop', 'src', 'main', 'resources', 'backgrounds'); os.makedirs(out, exist_ok=True)
for f in sorted(glob.glob(os.path.join(ROOT, 'app/src/main/assets/backgrounds/*.webp'))):
    name = os.path.splitext(os.path.basename(f))[0]
    Image.open(f).convert('RGB').save(os.path.join(out, name + '.jpg'), 'JPEG', quality=82, optimize=True)

src = Image.open(os.path.join(ROOT, 'branding', 'my_personal_crossword_app_icon.png')).convert('RGB')
N = 1024; m = Image.new('L', (N * 4, N * 4), 0)
ImageDraw.Draw(m).rounded_rectangle([7 * 4, 7 * 4, (N - 7) * 4 - 1, (N - 7) * 4 - 1], radius=218 * 4, fill=255)
art = src.convert('RGBA'); art.putalpha(m.resize((N, N), Image.LANCZOS))
pk = os.path.join(ROOT, 'desktop', 'packaging'); os.makedirs(pk, exist_ok=True)
art.resize((512, 512), Image.LANCZOS).save(os.path.join(pk, 'icon.png'))
art.save(os.path.join(pk, 'icon.ico'), sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
print('desktop assets written')
