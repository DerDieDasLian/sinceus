# Love ❤

Android App für Paare: zählt, wie lange ihr zusammen seid, und erinnert an besondere Tage.

## Funktionen

- Einrichtung beim ersten Start (Namen, Datum mit optionaler Uhrzeit, Foto, Mitteilungen)
- Eigenes Foto mit anpassbarem Ausschnitt oder Standardmotiv als großes Titelbild
- Live-Modus mit sekundengenauem Zähler
- Zähler in Tagen, Wochen, Monaten und Stunden
- Liste der nächsten besonderen Tage mit Countdown
- Mitteilungen an Monatstagen, Jahrestagen, 50/100/200… Tagen, Schnapszahlen (111, 222…) und alle 50 Wochen, zur Wunschuhrzeit
- Homescreen-Widget „Tage zusammen“
- Hell- und Dunkelmodus
- Keine Internet-Berechtigung: alle Daten bleiben auf dem Gerät
- Alle Daten löschen mit einem Tipp

Veröffentlichung im Play Store: siehe [PLAY_STORE.md](PLAY_STORE.md).

## Installieren

1. Unter **Actions → Build** den neuesten Lauf öffnen und das Artefakt `love-app-apk` herunterladen (ZIP entpacken).
2. `app-release.apk` aufs Handy kopieren und öffnen, „Aus unbekannten Quellen installieren“ erlauben.
3. Beim ersten Start Mitteilungen erlauben.

Tipp: Wenn Mitteilungen nicht zuverlässig ankommen, in den Android-Einstellungen unter *Apps → Love → Akku* „Nicht eingeschränkt“ wählen.

## Selbst bauen

```bash
./gradlew assembleRelease   # APK: app/build/outputs/apk/release/app-release.apk
./gradlew bundleRelease     # App Bundle für den Play Store
./gradlew testDebugUnitTest # Tests, Screenshots und Store-Grafiken
```

Ohne eigenen Upload-Schlüssel wird mit dem mitgelieferten `app/debug.keystore` signiert, damit Test-Versionen ohne Deinstallieren über die alte installiert werden können.
