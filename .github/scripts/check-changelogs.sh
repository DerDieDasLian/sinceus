#!/usr/bin/env bash
# Prüft in einem Pull Request die Änderungsnotizen:
#  1. Notizen von schon veröffentlichten Versionen dürfen sich nicht mehr ändern.
#     Sonst landet eine neue Funktion in einer alten Notiz und dem nächsten Release fehlt seine.
#  2. Ändert der PR die App, braucht er die Notiz für die nächste Version.
#     Reine Übersetzungen (z. B. von Weblate) brauchen keine.
#
# Aufruf: check-changelogs.sh <Basis-Commit>
set -euo pipefail

base="$1"
released=$(git show "$base:app/build.gradle.kts" | grep -oP 'versionCode = \K[0-9]+')
next=$((released + 1))
changed=$(git diff --name-only "$base"...HEAD)
# Zurücksetzen auf den Stand des letzten Releases ist erlaubt (Reparatur)
tag=$(git describe --tags --abbrev=0 --match 'v[0-9]*' "$base" 2>/dev/null || true)
ok=true

while read -r file; do
  if [[ "$file" =~ ^fastlane/metadata/android/[^/]+/changelogs/([0-9]+)\.txt$ ]]; then
    code="${BASH_REMATCH[1]}"
    if [ -n "$tag" ] && [ -f "$file" ] && [ "$(git show "$tag:$file" 2>/dev/null)" = "$(cat "$file")" ]; then
      continue
    fi
    if [ "$code" -le "$released" ]; then
      echo "::error file=$file::Version $code ist schon veröffentlicht. Bitte die Notiz in changelogs/$next.txt schreiben statt in $code.txt."
      ok=false
    fi
  fi
done <<< "$changed"

app=$(grep -E '^app/' <<< "$changed" | grep -vE '^app/src/test/|^app/src/main/res/values-[^/]+/strings\.xml$' || true)
if [ -n "$app" ]; then
  # Fehlt die Notiz, sagt check-release.sh genau welche
  .github/scripts/check-release.sh "$next" || ok=false
fi

$ok
