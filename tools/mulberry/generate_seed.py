#!/usr/bin/env python3
"""Generate the default AAC board seed data for the Unmute app.

Reads tools/mulberry/seed.tsv, validates every referenced Mulberry symbol
against tools/mulberry/symbol-info.csv, and writes
app/src/main/java/com/unmute/app/data/DefaultSeed.kt.
"""

import csv
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CSV_PATH = ROOT / "tools" / "mulberry" / "symbol-info.csv"
SEED_PATH = ROOT / "tools" / "mulberry" / "seed.tsv"
OUT_PATH = ROOT / "app" / "src" / "main" / "java" / "com" / "unmute" / "app" / "data" / "DefaultSeed.kt"

CATEGORY_COLORS = {
    "Greetings": 0xFF4FC3F7,
    "People": 0xFFF06292,
    "Feelings": 0xFFFFD54F,
    "Food & Drink": 0xFFFFB74D,
    "Actions": 0xFF81C784,
    "Places": 0xFFBA68C8,
    "Things": 0xFF4DB6AC,
    "Body": 0xFFAED581,
}

CATEGORY_ES = {
    "Greetings": "Saludos",
    "People": "Personas",
    "Feelings": "Sentimientos",
    "Food & Drink": "Comida y bebida",
    "Actions": "Acciones",
    "Places": "Lugares",
    "Things": "Objetos",
    "Body": "Cuerpo",
}


def load_symbol_names():
    names = set()
    with open(CSV_PATH, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            names.add(row["symbol-en"].strip())
    return names


def normalize(name: str) -> str:
    return name.strip().lower().replace(" ", "_").replace("-", "_")


def kotlin_str(value: str) -> str:
    escaped = value.replace("\\", "\\\\").replace('"', '\\"')
    return f'"{escaped}"'


def main():
    symbol_names = {normalize(n) for n in load_symbol_names()}

    categories: list[tuple[str, list[list[str]]]] = []
    current = None
    with open(SEED_PATH, newline="", encoding="utf-8") as f:
        reader = csv.reader(f, delimiter="\t")
        next(reader)
        for row in reader:
            if not row or len(row) < 6:
                continue
            category, label_en, label_es, phrase_en, phrase_es, image = row
            if current is None or current[0] != category:
                current = (category, [])
                categories.append(current)
            current[1].append([label_en, label_es, phrase_en, phrase_es, image])

    missing = []
    for _, cards in categories:
        for card in cards:
            image = card[4]
            if image.startswith("emoji:"):
                continue
            if normalize(image) not in symbol_names:
                missing.append(f"{card[0]} -> {image}")

    if missing:
        print("ERROR: symbols missing from Mulberry set:")
        for m in missing:
            print(f"  {m}")
        sys.exit(1)

    lines = []
    lines.append("package com.unmute.app.data")
    lines.append("")
    lines.append("import com.unmute.app.domain.model.ImageType")
    lines.append("")
    lines.append("internal object DefaultSeed {")
    lines.append("    val boardNameEn = \"My Board\"")
    lines.append("    val boardNameEs = \"Mi tablero\"")
    lines.append("")
    lines.append("    val categories: List<DefaultCategory> = listOf(")

    for category, cards in categories:
        color = CATEGORY_COLORS.get(category, 0xFF9E9E9E)
        name_es = CATEGORY_ES.get(category, category)
        lines.append("        DefaultCategory(")
        lines.append(f"            nameEn={kotlin_str(category)},")
        lines.append(f"            nameEs={kotlin_str(name_es)},")
        lines.append(f"            color=0x{color:08X}L,")
        lines.append("            cards = listOf(")
        for card in cards:
            label_en, label_es, phrase_en, phrase_es, image = card
            if image.startswith("emoji:"):
                image_type = "ImageType.EMOJI"
                image_value = image.removeprefix("emoji:")
            else:
                image_type = "ImageType.SYMBOL"
                image_value = f"symbols/{image}.svg"
            lines.append("                DefaultCard(")
            lines.append(f"                    labelEn={kotlin_str(label_en)},")
            lines.append(f"                    labelEs={kotlin_str(label_es)},")
            lines.append(f"                    phraseEn={kotlin_str(phrase_en)},")
            lines.append(f"                    phraseEs={kotlin_str(phrase_es)},")
            lines.append(f"                    imageType={image_type},")
            lines.append(f"                    imageValue={kotlin_str(image_value)},")
            lines.append("                ),")
        lines.append("            ),")
        lines.append("        ),")

    lines.append("    )")
    lines.append("")
    lines.append("    data class DefaultCategory(")
    lines.append("        val nameEn: String,")
    lines.append("        val nameEs: String,")
    lines.append("        val color: Long,")
    lines.append("        val cards: List<DefaultCard>,")
    lines.append("    )")
    lines.append("")
    lines.append("    data class DefaultCard(")
    lines.append("        val labelEn: String,")
    lines.append("        val labelEs: String,")
    lines.append("        val phraseEn: String,")
    lines.append("        val phraseEs: String,")
    lines.append("        val imageType: ImageType,")
    lines.append("        val imageValue: String,")
    lines.append("    )")
    lines.append("}")

    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {OUT_PATH.relative_to(ROOT)} ({len(categories)} categories)")


if __name__ == "__main__":
    main()
