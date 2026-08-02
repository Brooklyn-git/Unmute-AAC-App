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
