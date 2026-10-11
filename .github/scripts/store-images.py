"""Erstellt die Bilder für den Play Store aus den Test-Bildern in app/build/screenshots.

- Screenshots mit Überschrift und Handy-Rahmen (1080 x 1920), das Besondere an Since Us zuerst
- Vorstellungsgrafik (1024 x 500)

Ergebnis: fastlane/metadata/android/<Sprache>/images. Aufruf: python3 .github/scripts/store-images.py
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

SHOTS = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("app/build/screenshots")
OUT = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("fastlane/metadata/android")
FONT = Path("app/src/main/res/font/space_grotesk.ttf")

# Farben des Farbverlaufs "Wein" aus der App (Theme.kt, Presets[0])
WINE = [(0x4A, 0x0B, 0x18), (0x8B, 0x1E, 0x31), (0xE0, 0x56, 0x6B)]
PINK = (0xFF, 0x5C, 0x7A)

# Reihenfolge der Store-Screenshots: Bild aus der App, Überschrift, Unterzeile
SCREENS = {
    "en": [
        ("home", "Count your days together", "Free, no account, no ads"),
        ("home_poly", "For every kind of love", "Poly mode, pronouns and more"),
        ("sync", "Sync without a cloud", "Pair with a QR code, fully encrypted"),
        ("privacy", "Your data stays with you", "No server, no tracking, app lock"),
        ("moments", "Keep your special moments", "With notes, photos and reminders"),
        ("live", "Every second counts", "Live mode counts along with you"),
    ],
    "de": [
        ("home", "Zählt eure gemeinsamen Tage", "Kostenlos, ohne Konto, ohne Werbung"),
        ("home_poly", "Für jede Art von Liebe", "Poly-Modus, Pronomen und mehr"),
        ("sync", "Abgleich ohne Cloud", "Per QR-Code koppeln, komplett verschlüsselt"),
        ("privacy", "Eure Daten bleiben bei euch", "Kein Server, kein Tracking, App-Sperre"),
        ("moments", "Haltet besondere Momente fest", "Mit Notiz, Foto und Erinnerung"),
        ("live", "Jede Sekunde zählt", "Der Live-Modus zählt mit"),
    ],
}

FEATURE = {
    "en": ("Free, private and made for every kind of love", ["No account", "No ads", "Open source"]),
    "de": ("Kostenlos, privat und für jede Art von Liebe", ["Ohne Konto", "Ohne Werbung", "Open Source"]),
}
LANGS = {"en": "en-US", "de": "de-DE"}


def font(size: int, weight: int) -> ImageFont.FreeTypeFont:
    f = ImageFont.truetype(str(FONT), size)
    f.set_variation_by_axes([weight])
    return f


def gradient(w: int, h: int) -> Image.Image:
    """Diagonaler Verlauf von dunkel (oben links) nach hell (unten rechts)"""
    small = Image.new("RGB", (64, 64))
    px = small.load()
    for y in range(64):
        for x in range(64):
            t = (x / 63 * w + y / 63 * h) / (w + h)
            a, b, k = (WINE[0], WINE[1], t * 2) if t < 0.5 else (WINE[1], WINE[2], t * 2 - 1)
            px[x, y] = tuple(round(a[i] + (b[i] - a[i]) * k) for i in range(3))
    return small.resize((w, h), Image.BICUBIC)


def bezier(p0, p1, p2, p3, steps=24):
    for i in range(1, steps + 1):
        t = i / steps
        u = 1 - t
        yield (
            u**3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t**3 * p3[0],
            u**3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t**3 * p3[1],
        )


def heart_points():
    """Umriss des Herz-Icons aus der App (Material "Favorite", 24 x 24)"""
    pts = [(12, 21.35), (10.55, 20.03)]
    for seg in [
        ((5.4, 15.36), (2, 12.28), (2, 8.5)),
        ((2, 5.42), (4.42, 3), (7.5, 3)),
        ((9.24, 3), (10.91, 3.81), (12, 5.09)),
        ((13.09, 3.81), (14.76, 3), (16.5, 3)),
        ((19.58, 3), (22, 5.42), (22, 8.5)),
        ((22, 12.28), (18.6, 15.36), (13.45, 20.04)),
    ]:
        pts += bezier(pts[-1], *seg)
    return pts


def draw_heart(img: Image.Image, x: float, y: float, size: float, color) -> None:
    """Zeichnet ein glattes Herz (4-fach überabgetastet), size = Kantenlänge des Icons"""
    s = 4
    box = round(size)
    mask = Image.new("L", (box * s, box * s), 0)
    scale = box * s / 24
    ImageDraw.Draw(mask).polygon([(px * scale, py * scale) for px, py in heart_points()], fill=255)
    mask = mask.resize((box, box), Image.LANCZOS)
    if len(color) == 4:
        mask = mask.point(lambda v: v * color[3] // 255)
    layer = Image.new("RGBA", (box, box), color[:3] + (255,))
    layer.putalpha(mask)
    img.alpha_composite(layer, (round(x), round(y)))


def rounded(img: Image.Image, radius: int) -> Image.Image:
    s = 3
    mask = Image.new("L", (img.width * s, img.height * s), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, mask.width - 1, mask.height - 1), radius * s, fill=255)
    out = img.convert("RGBA")
    out.putalpha(mask.resize(img.size, Image.LANCZOS))
    return out


def phone(screen: Image.Image, width: int) -> Image.Image:
    """Screenshot in einem schlichten Handy-Rahmen mit Schatten"""
    bezel = max(6, width // 40)
    h = round(screen.height * width / screen.width)
    inner = rounded(screen.convert("RGB").resize((width, h), Image.LANCZOS), width // 13)
    frame = rounded(Image.new("RGB", (width + 2 * bezel, h + 2 * bezel), (18, 14, 16)), width // 13 + bezel)
    frame.alpha_composite(inner, (bezel, bezel))
    pad = width // 8
    out = Image.new("RGBA", (frame.width + 2 * pad, frame.height + 2 * pad), (0, 0, 0, 0))
    shadow = Image.new("RGBA", out.size, (0, 0, 0, 0))
    shadow.paste((20, 0, 6, 120), (pad, pad + pad // 3), frame)
    out.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(pad // 3)))
    out.alpha_composite(frame, (pad, pad))
    return out


def backdrop(w: int, h: int) -> Image.Image:
    img = gradient(w, h).convert("RGBA")
    # Große, kaum sichtbare Herzen im Hintergrund
    draw_heart(img, w * 0.55, -h * 0.06, w * 0.7, (255, 255, 255, 14))
    draw_heart(img, -w * 0.25, h * 0.55, w * 0.6, (255, 255, 255, 10))
    return img


def wrap(text: str, f: ImageFont.FreeTypeFont, width: int) -> list[str]:
    """Bricht Text um; bei zwei Zeilen werden beide etwa gleich lang"""
    words = text.split()
    if f.getlength(text) > width:
        splits = [(" ".join(words[:i]), " ".join(words[i:])) for i in range(1, len(words))]
        fits = [s for s in splits if max(f.getlength(s[0]), f.getlength(s[1])) <= width]
        if fits:
            return list(min(fits, key=lambda s: max(f.getlength(s[0]), f.getlength(s[1]))))
    lines, line = [], ""
    for word in text.split():
        test = f"{line} {word}".strip()
        if f.getlength(test) <= width or not line:
            line = test
        else:
            lines.append(line)
            line = word
    return lines + [line]


def store_screenshot(screen: Image.Image, title: str, subtitle: str) -> Image.Image:
    w, h = 1080, 1920
    img = backdrop(w, h)
    text = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(text)
    big, small = font(76, 700), font(40, 500)
    y = 120
    for line in wrap(title, big, w - 140):
        d.text((w / 2, y), line, font=big, fill="white", anchor="mt")
        y += 92
    y += 14
    for line in wrap(subtitle, small, w - 160):
        d.text((w / 2, y), line, font=small, fill=(255, 255, 255, 215), anchor="mt")
        y += 52
    img.alpha_composite(text)
    device = phone(screen, 700)
    pad = 700 // 8
    img.alpha_composite(device, ((w - device.width) // 2, max(y + 50, 420) - pad))
    return img.convert("RGB")


def feature_graphic(screen: Image.Image, tagline: str, chips: list[str]) -> Image.Image:
    w, h = 1024, 500
    img = backdrop(w, h)
    text = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(text)
    x = 64
    draw_heart(img, x, 92, 76, PINK + (255,))
    d.text((x + 92, 84), "Since Us", font=font(84, 700), fill="white")
    y = 196
    f = font(32, 500)
    for line in wrap(tagline, f, 600):
        d.text((x, y), line, font=f, fill=(255, 255, 255, 230))
        y += 42
    y += 26
    chip = font(22, 500)
    cx = x
    for label in chips:
        tw = chip.getlength(label)
        d.rounded_rectangle((cx, y, cx + tw + 36, y + 46), 23, fill=(255, 255, 255, 36), outline=(255, 255, 255, 90), width=2)
        d.text((cx + 18, y + 23), label, font=chip, fill="white", anchor="lm")
        cx += tw + 36 + 12
    img.alpha_composite(text)
    device = phone(screen, 250)
    pad = 250 // 8
    img.alpha_composite(device, (w - device.width - 40 + pad, 52 - pad))
    return img.convert("RGB")


for lang, store_lang in LANGS.items():
    images = OUT / store_lang / "images"
    phone_dir = images / "phoneScreenshots"
    for old in phone_dir.glob("*.jpg"):
        old.unlink()
    for index, (name, title, subtitle) in enumerate(SCREENS[lang], start=1):
        shot = Image.open(SHOTS / lang / f"{name}.png")
        store_screenshot(shot, title, subtitle).save(phone_dir / f"{index}.jpg", quality=90, optimize=True, progressive=True)
    tagline, chips = FEATURE[lang]
    feature_graphic(Image.open(SHOTS / lang / "home.png"), tagline, chips).save(images / "featureGraphic.png", optimize=True)

print("Store-Bilder aktualisiert")
