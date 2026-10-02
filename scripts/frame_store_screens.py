#!/usr/bin/env python3
"""Frame the CI-drawn store screenshots (artifact noisefile-store-screens, 1078x2399)
into 1080x1920 Play images with a caption on top. Usage: frame_store_screens.py <in_dir> <out_dir>"""
import sys, os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

CAPTIONS = {
    "01-home": "See your city's limit\nbefore you record",
    "02-recording": "A red mark when the noise\nreaches the limit",
    "03-review": "Checked against\nthe city's own rule",
    "04-incidents": "Every incident sealed\nand kept on your phone",
    "05-rules": "The law in the city's\nown words",
    "06-more": "Baselines, the tour,\nand one paid part",
    "07-form-guide": "The city's form,\nbox by box",
}
W, H = 1080, 1920
FONT = os.path.join(os.path.dirname(__file__), "..", "app/src/main/res/font/barlow_condensed_bold.ttf")

def frame(src, dst, caption):
    shot = Image.open(src).convert("RGB")
    canvas = Image.new("RGB", (W, H), (12, 19, 34))
    # soft vertical glow behind the phone
    glow = Image.new("RGB", (W, H), (12, 19, 34))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((-200, 500, W + 200, H + 300), fill=(22, 32, 54))
    glow = glow.filter(ImageFilter.GaussianBlur(120))
    canvas.paste(glow)
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.truetype(FONT, 92)
    y = 70
    for line in caption.split("\n"):
        tw = draw.textlength(line, font=font)
        draw.text(((W - tw) / 2, y), line, font=font, fill=(255, 247, 232))
        y += 100
    # the phone: scale to width 690, rounded corners, drop shadow, top aligned under the caption, cropped at the bottom edge
    pw = 690
    ph = int(shot.height * pw / shot.width)
    phone = shot.resize((pw, ph), Image.LANCZOS)
    radius = 44
    mask = Image.new("L", (pw, ph), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, pw, ph), radius=radius, fill=255)
    top = y + 40
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(((W - pw) // 2 - 6, top + 18, (W + pw) // 2 + 6, top + ph + 30), radius=radius + 6, fill=(0, 0, 0, 170))
    shadow = shadow.filter(ImageFilter.GaussianBlur(28))
    canvas.paste(shadow, (0, 0), shadow)
    canvas.paste(phone, ((W - pw) // 2, top), mask)
    # bezel line
    ImageDraw.Draw(canvas).rounded_rectangle(((W - pw) // 2 - 1, top - 1, (W + pw) // 2, top + ph), radius=radius, outline=(70, 80, 105), width=2)
    canvas.save(dst, optimize=True)

if __name__ == "__main__":
    src_dir, out_dir = sys.argv[1], sys.argv[2]
    os.makedirs(out_dir, exist_ok=True)
    for name, caption in CAPTIONS.items():
        src = os.path.join(src_dir, name + ".png")
        if os.path.isfile(src):
            frame(src, os.path.join(out_dir, name + ".png"), caption)
            print("framed", name)
