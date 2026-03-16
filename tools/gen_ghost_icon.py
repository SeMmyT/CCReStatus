"""Render Ghost pixel grid to Android icon PNGs at all DPI buckets."""
from PIL import Image, ImageDraw

GRID = [
    [0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0],
    [0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0],
    [0, 1, 1, 4, 1, 1, 1, 1, 4, 1, 1, 0],
    [0, 1, 2, 3, 2, 1, 1, 2, 3, 2, 1, 0],
    [0, 1, 2, 3, 2, 1, 1, 2, 3, 2, 1, 0],
    [0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0],
    [0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0],
    [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
    [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
    [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
    [1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1],
    [0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0],
]

COLORS = {
    0: (0, 0, 0, 0),           # transparent
    1: (232, 230, 220, 255),   # ghost body
    2: (217, 119, 87, 255),    # accent/eye
    3: (20, 20, 19, 255),      # pupil
    4: (250, 249, 245, 255),   # highlight
}

BG_COLOR = (20, 20, 19, 255)

SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

for density, px in SIZES.items():
    cell = px // 12
    img = Image.new("RGBA", (px, px), BG_COLOR)
    draw = ImageDraw.Draw(img)
    for r, row in enumerate(GRID):
        for c, v in enumerate(row):
            if v == 0:
                continue
            x, y = c * cell, r * cell
            gap = max(1, cell // 8)
            draw.rectangle(
                [x + gap, y + gap, x + cell - gap, y + cell - gap],
                fill=COLORS[v],
            )

    out_dir = f"android/app/src/main/res/mipmap-{density}"
    img.save(f"{out_dir}/ic_launcher.png")
    img.save(f"{out_dir}/ic_launcher_round.png")
    print(f"{density}: {px}x{px} -> {out_dir}")
