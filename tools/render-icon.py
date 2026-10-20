from math import sin, cos, pi
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "FriendlyFireLHP.png"
S = 3
size = 512 * S
image = Image.new("RGB", (size, size), (11, 18, 32))
d = ImageDraw.Draw(image)

def pt(x, y):
    return (int(x * S), int(y * S))

def polygon(points, fill, outline=None, width=1):
    points = [pt(x, y) for x, y in points]
    d.polygon(points, fill=fill)
    if outline:
        d.line(points + [points[0]], fill=outline, width=width * S, joint="curve")

d.rounded_rectangle((20*S, 20*S, 492*S, 492*S), radius=112*S,
                    fill=(21, 35, 59), outline=(80, 214, 232), width=12*S)
polygon([(256, 103), (384, 151), (384, 242), (368, 308), (330, 360),
         (256, 430), (182, 360), (144, 308), (128, 242), (128, 151)],
        (38, 58, 94), (156, 174, 255), 10)

heart = []
for i in range(181):
    t = pi * 2 * i / 180
    x = 16 * sin(t) ** 3
    y = 13 * cos(t) - 5 * cos(2*t) - 2 * cos(3*t) - cos(4*t)
    heart.append((256 + x * 5.6, 260 - y * 5.6))
d.polygon([pt(x, y) for x, y in heart], fill=(234, 76, 105))
d.line([pt(x, y) for x, y in heart] + [pt(*heart[0])], fill=(255, 215, 223), width=7*S, joint="curve")

# Two crossed blades, their guards and pommels stay readable at small sizes.
for a, b in [((178, 176), (334, 332)), ((334, 176), (178, 332))]:
    d.line([pt(*a), pt(*b)], fill=(238, 246, 255), width=14*S)
    d.line([pt(a[0]+3, a[1]+3), pt(b[0]-3, b[1]-3)], fill=(80, 214, 232), width=4*S)
for cx, cy, angle in [(178, 176, -45), (334, 176, 45)]:
    rad = angle * pi / 180
    ux, uy = cos(rad), sin(rad)
    px, py = -uy, ux
    d.line([pt(cx-22*px, cy-22*py), pt(cx+22*px, cy+22*py)], fill=(255, 214, 124), width=11*S)
    d.ellipse((int((cx-9)*S), int((cy-9)*S), int((cx+9)*S), int((cy+9)*S)), fill=(255, 214, 124))
d.ellipse((247*S, 311*S, 265*S, 329*S), fill=(80, 214, 232))

font_path = Path(r"C:\Windows\Fonts\arialbd.ttf")
font = ImageFont.truetype(str(font_path), 25*S)
label = "LHP ADDON"
box = d.textbbox((0, 0), label, font=font, stroke_width=0)
x = (size - (box[2] - box[0])) // 2
d.text((x, 421*S), label, fill=(234, 242, 255), font=font, stroke_width=0)

image.resize((512, 512), Image.Resampling.LANCZOS).save(OUT, format="PNG", optimize=True)
print(OUT)
