#!/usr/bin/env python3
"""Look up Mulberry symbol names for a curated list of English words.

Usage:
    python3 tools/mulberry/lookup.py <word1> [word2 ...]

Outputs lines as:  word<TAB>symbol<TAB>category
Symbols that are not found in the Mulberry set are listed at the end.
"""

import csv
import sys

CSV_PATH = "tools/mulberry/symbol-info.csv"


def load_symbols():
    symbols = []
    with open(CSV_PATH, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            symbols.append(row)
    return symbols


def normalize(name: str) -> str:
    return name.strip().lower().replace(" ", "_").replace("-", "_")


def main():
    words = [w.strip() for w in sys.argv[1:] if w.strip()]
    symbols = load_symbols()

    # index by normalized symbol name
    by_name: dict[str, dict] = {}
    for row in symbols:
        by_name.setdefault(normalize(row["symbol-en"]), []).append(row)

    found: list[tuple[str, str, str]] = []
    missing: list[str] = []
    for word in words:
        hits = by_name.get(normalize(word))
        if hits:
            row = hits[0]
            found.append((word, row["symbol-en"], row["category-en"]))
        else:
            missing.append(word)

    for word, symbol, category in found:
        print(f"{word}\t{symbol}\t{category}")
    if missing:
        print("\nMISSING:")
        for word in missing:
            print(f"  {word}")


if __name__ == "__main__":
    main()
