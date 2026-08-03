package com.unmute.app.domain.model

import com.unmute.app.data.local.BoardEntity
import com.unmute.app.data.local.CardEntity
import com.unmute.app.data.local.CategoryEntity

fun resolveLanguage(selected: AppLanguage, systemLanguage: String): String = when (selected) {
    AppLanguage.SYSTEM -> if (systemLanguage.startsWith("es")) "es" else "en"
    AppLanguage.EN -> "en"
    AppLanguage.ES -> "es"
}

fun BoardEntity.label(lang: String): String = if (lang == "es") nameEs.ifBlank { nameEn } else nameEn

fun CategoryEntity.label(lang: String): String =
    if (lang == "es") nameEs.ifBlank { nameEn } else nameEn

fun CardEntity.label(lang: String): String =
    if (lang == "es") labelEs.ifBlank { labelEn } else labelEn

fun CardEntity.phrase(lang: String): String =
    if (lang == "es") phraseEs.ifBlank { phraseEn } else phraseEn

/**
 * Returns a copy of this card with [label] and [phrase] written to the fields of the active
 * [lang]. For new cards both languages get the same value; for existing cards the other
 * language's fields are preserved.
 */
fun CardEntity.withEditedLabels(isNew: Boolean, lang: String, label: String, phrase: String): CardEntity {
    val editEnglish = isNew || lang != "es"
    val editSpanish = isNew || lang == "es"
    return copy(
        labelEn = if (editEnglish) label else labelEn,
        labelEs = if (editSpanish) label else labelEs,
        phraseEn = if (editEnglish) phrase else phraseEn,
        phraseEs = if (editSpanish) phrase else phraseEs,
    )
}
