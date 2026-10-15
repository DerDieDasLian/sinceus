#!/usr/bin/env bash
# Prüft vor einem Release, ob die Änderungsnotizen für den neuen versionCode da sind.
# Sie erscheinen in der App (Neuigkeiten nach dem Update), bei F-Droid und auf GitHub,
# deshalb werden sie von Hand geschrieben und nicht aus Commit-Nachrichten erzeugt.
#
# Aufruf: check-release.sh <versionCode>
set -euo pipefail

code="$1"
ok=true
for lang in de-DE en-US; do
  notes="fastlane/metadata/android/$lang/changelogs/$code.txt"
  if [ ! -s "$notes" ]; then
    echo "::error file=$notes::Änderungsnotiz fehlt: $notes"
    ok=false
    continue
  fi
  # F-Droid erlaubt höchstens 500 Zeichen
  chars=$(wc -m < "$notes")
  if [ "$chars" -gt 500 ]; then
    echo "::error file=$notes::Änderungsnotiz ist zu lang ($chars von höchstens 500 Zeichen)"
    ok=false
  fi
done
$ok
