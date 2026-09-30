#!/usr/bin/env bash
# Ermittelt die nächste Version für ein Release und trägt sie in app/build.gradle.kts ein.
#
# Regeln:
# - Keine Änderung unter app/ (ohne Tests) seit dem letzten Release-Tag: kein Release (release=false).
# - versionName in der Datei wurde von Hand über den letzten Tag erhöht: diese Version nehmen.
# - Sonst automatisch erhöhen: [major] oder [minor] in einer Commit-Nachricht seit dem letzten
#   Tag erhöht die entsprechende Stelle, ansonsten die letzte Stelle (Patch).
# - versionCode ist immer mindestens eins höher als beim letzten Release.
# - Fehlende Änderungsnotizen werden aus den Commit-Nachrichten erzeugt.
#
# Ausgabe (für $GITHUB_OUTPUT): release, version, code, changed
set -euo pipefail

FILE=app/build.gradle.kts
FORCE="${FORCE:-false}"

file_name=$(grep -oP 'versionName = "\K[^"]+' "$FILE")
file_code=$(grep -oP 'versionCode = \K[0-9]+' "$FILE")
last_tag=$(git describe --tags --abbrev=0 --match 'v[0-9]*' 2>/dev/null || true)

if [ -z "$last_tag" ]; then
  echo "release=true"; echo "version=$file_name"; echo "code=$file_code"; echo "changed=false"
  exit 0
fi

last_name=${last_tag#v}
last_code=$(git show "$last_tag:$FILE" | grep -oP 'versionCode = \K[0-9]+')

if [ "$FORCE" != "true" ] && git diff --quiet "$last_tag" HEAD -- app/ ':!app/src/test'; then
  echo "release=false"; echo "version=$last_name"; echo "code=$last_code"; echo "changed=false"
  exit 0
fi

highest=$(printf '%s\n%s\n' "$last_name" "$file_name" | sort -V | tail -1)
if [ "$file_name" != "$last_name" ] && [ "$highest" = "$file_name" ]; then
  # Von Hand erhöht
  name=$file_name
else
  IFS=. read -r major minor patch <<<"$last_name"
  minor=${minor:-0}; patch=${patch:-0}
  messages=$(git log --format='%s%n%b' "$last_tag"..HEAD)
  if grep -qi '\[major\]' <<<"$messages"; then
    name="$((major + 1)).0.0"
  elif grep -qi '\[minor\]' <<<"$messages"; then
    name="$major.$((minor + 1)).0"
  else
    name="$major.$minor.$((patch + 1))"
  fi
fi

code=$(( file_code > last_code ? file_code : last_code + 1 ))
changed=false
if [ "$name" != "$file_name" ] || [ "$code" != "$file_code" ]; then
  sed -i "s/versionName = \"$file_name\"/versionName = \"$name\"/; s/versionCode = $file_code/versionCode = $code/" "$FILE"
  changed=true
fi

# Änderungsnotiz erzeugen, falls keine von Hand geschrieben wurde (F-Droid erlaubt max. 500 Zeichen)
for lang in en-US de-DE; do
  notes="fastlane/metadata/android/$lang/changelogs/$code.txt"
  if [ -d "fastlane/metadata/android/$lang" ] && [ ! -f "$notes" ]; then
    mkdir -p "$(dirname "$notes")"
    git log --format='• %s' --no-merges "$last_tag"..HEAD -- app/ ':!app/src/test' \
      | sed -E 's/ *\[(major|minor)\]//I' | head -c 480 > "$notes"
    echo >> "$notes"
    changed=true
  fi
done

echo "release=true"; echo "version=$name"; echo "code=$code"; echo "changed=$changed"
