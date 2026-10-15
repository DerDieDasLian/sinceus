#!/usr/bin/env bash
# Ermittelt die nächste Version für ein Release und trägt sie in app/build.gradle.kts ein.
#
# Regeln:
# - Keine Änderung unter app/ (ohne Tests) seit dem letzten Release-Tag: kein Release (release=false).
# - versionName in der Datei wurde von Hand über den letzten Tag erhöht: diese Version nehmen.
# - Sonst nach BUMP erhöhen (patch, minor, major). Bei BUMP=auto (Standard) erhöht [major] oder
#   [minor] in einer Commit-Nachricht seit dem letzten Tag die entsprechende Stelle, sonst Patch.
# - versionCode ist immer mindestens eins höher als beim letzten Release.
# - Die Änderungsnotizen schreibt man selbst, check-release.sh prüft sie.
#
# Ausgabe (für $GITHUB_OUTPUT): release, version, code, changed
set -euo pipefail

FILE=app/build.gradle.kts
FORCE="${FORCE:-false}"
BUMP="${BUMP:-auto}"

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
  if [ "$BUMP" != "auto" ]; then
    messages="[$BUMP]"
  fi
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

echo "release=true"; echo "version=$name"; echo "code=$code"; echo "changed=$changed"
