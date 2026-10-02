"""Wandelt die Test-Bilder aus app/build/screenshots in Website- und Store-Bilder um.

Die Dateinamen bleiben immer gleich, damit Links auf die Bilder weiter funktionieren.
"""
from pathlib import Path

from PIL import Image

SHOTS = Path("app/build/screenshots")
DOCS = Path("docs/img")
STORE = Path("fastlane/metadata/android")

# Website: Startbereich (1 bis 3) und Abschnitt Momente
SCREENS = {"1": "home", "2": "live", "3": "settings"}
# Store: Reihenfolge der Screenshots
STORE_SCREENS = ["home", "live", "moments", "settings"]
LANGS = {"de": "de-DE", "en": "en-US"}


def screen(lang: str, name: str) -> Image.Image:
    return Image.open(SHOTS / lang / f"{name}.png").convert("RGB")


for lang, store_lang in LANGS.items():
    for number, name in SCREENS.items():
        screen(lang, name).save(DOCS / f"{lang}-{number}.webp", quality=82, method=6)
    screen(lang, "moments").save(DOCS / f"moments-{lang}.webp", quality=82, method=6)

    phone = STORE / store_lang / "images" / "phoneScreenshots"
    for index, name in enumerate(STORE_SCREENS, start=1):
        screen(lang, name).save(phone / f"{index}.jpg", quality=88, optimize=True, progressive=True)

    # Liebeskarte mit durchsichtigen runden Ecken
    widget = "card_3x2" if lang == "de" else "card_3x2_en"
    Image.open(SHOTS / "widgets" / f"{widget}.png").convert("RGBA").save(
        DOCS / f"widget-{lang}.webp", quality=90, method=6
    )

    # Teilen-Bild, auf der Website in 720 x 900
    share = Image.open(SHOTS / "share" / f"{lang}.png").convert("RGB")
    share.resize((720, 900), Image.LANCZOS).save(DOCS / f"share-{lang}.webp", quality=85, method=6)

print("Bilder aktualisiert")
