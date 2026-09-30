# Love ❤

Private Android App für Alex & Sam: zählt, wie lange ihr zusammen seid, und erinnert an besondere Tage.

## Funktionen

- Eigenes Foto oder Standardmotiv als großes Titelbild
- Zähler in Tagen, Wochen, Monaten und Stunden
- Liste der nächsten besonderen Tage mit Countdown
- Mitteilungen an Monatstagen, Jahrestagen, 50/100/200… Tagen, Schnapszahlen (111, 222…) und alle 50 Wochen, zur Wunschuhrzeit
- Homescreen-Widget „Tage zusammen“
- Hell- und Dunkelmodus
- Keine Internet-Berechtigung: alle Daten bleiben auf dem Gerät

## Installieren

1. Unter **Actions → Build APK** den neuesten Lauf öffnen und das Artefakt `love-app` herunterladen (ZIP entpacken).
2. `app-release.apk` aufs Handy kopieren und öffnen, „Aus unbekannten Quellen installieren“ erlauben.
3. Beim ersten Start Mitteilungen erlauben.

Tipp: Wenn Mitteilungen nicht zuverlässig ankommen, in den Android-Einstellungen unter *Apps → Love → Akku* „Nicht eingeschränkt“ wählen.

## Selbst bauen

```bash
./gradlew assembleRelease   # APK: app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest # Tests + Screenshots in app/build/screenshots
```

Die APK wird mit dem mitgelieferten `app/debug.keystore` signiert, damit neue Versionen ohne Deinstallieren über die alte installiert werden können.
