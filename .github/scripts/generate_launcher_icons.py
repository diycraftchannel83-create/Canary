#!/usr/bin/env python3
"""Generate Android legacy and adaptive launcher icon assets from a farm logo."""
from pathlib import Path
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__import__("sys").argv[1])
if not SOURCE.is_absolute():
    SOURCE = ROOT / SOURCE
if not SOURCE.exists():
    raise SystemExit(f"Logo source not found: {SOURCE}")

img = Image.open(SOURCE).convert("RGBA")
# Preserve complete logo; square crop is avoided and image is scaled into a square canvas.
square = Image.new("RGBA", (1024, 1024), (25, 68, 48, 255))
contained = ImageOps.contain(img, (820, 820), Image.Resampling.LANCZOS)
square.alpha_composite(contained, ((1024-contained.width)//2, (1024-contained.height)//2))

# Adaptive launcher foreground: transparent canvas and safe-zone content.
foreground = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
safe = ImageOps.contain(img, (680, 680), Image.Resampling.LANCZOS)
foreground.alpha_composite(safe, ((1024-safe.width)//2, (1024-safe.height)//2))
nodpi = ROOT / "app/src/main/res/drawable-nodpi"
nodpi.mkdir(parents=True, exist_ok=True)
foreground.save(nodpi / "farm_launcher_foreground.png")

sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for density, size in sizes.items():
    out = ROOT / f"app/src/main/res/mipmap-{density}"
    out.mkdir(parents=True, exist_ok=True)
    icon = square.resize((size, size), Image.Resampling.LANCZOS)
    icon.save(out / "ic_launcher.png")
    icon.save(out / "ic_launcher_round.png")
print(f"Generated adaptive foreground and {len(sizes)} density icon pairs from {SOURCE}")
