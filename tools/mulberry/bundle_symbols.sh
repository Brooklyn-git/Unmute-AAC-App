#!/usr/bin/env bash
# Bundle the Mulberry symbols referenced by the seed data into app assets.
#
# Usage: tools/mulberry/bundle_symbols.sh
#
# Requires: the Mulberry release zip at tools/mulberry/mulberry-symbols.zip
# (not in git; download it from the URL below — see the error hint).

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ZIP="$ROOT/tools/mulberry/mulberry-symbols.zip"
SEED="$ROOT/tools/mulberry/seed.tsv"
DEST="$ROOT/app/src/main/assets/symbols"

if [[ ! -f "$ZIP" ]]; then
    echo "error: $ZIP not found. Download it from" >&2
    echo "  https://github.com/mulberrysymbols/mulberry-symbols/releases/latest/download/mulberry-symbols.zip" >&2
    exit 1
fi

mkdir -p "$DEST"

# Collect the Mulberry filenames referenced in the seed (non-emoji images).
FILES=()
while IFS=$'\t' read -r _category _label_en _label_es _phrase_en _phrase_es image; do
    if [[ "$image" != emoji:* && -n "$image" ]]; then
        FILES+=("EN-symbols/${image}.svg")
    fi
done < <(tail -n +2 "$SEED")

for f in "${FILES[@]}"; do
    unzip -o -j "$ZIP" "$f" -d "$DEST" > /dev/null
done

# Include the license for attribution.
unzip -o -j "$ZIP" "LICENSE.txt" -d "$ROOT/app/src/main/assets" > /dev/null

COUNT=$(find "$DEST" -name '*.svg' | wc -l)
echo "Bundled $COUNT symbols into $DEST"
