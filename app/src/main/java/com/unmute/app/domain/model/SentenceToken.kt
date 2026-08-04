package com.unmute.app.domain.model

/** A piece of the sentence being built: either a tapped card or typed text. */
sealed interface SentenceToken {
    val text: String

    /** A card tapped on the board. Keeps a snapshot so the bar renders even if the card changes. */
    data class Card(
        val cardId: Long,
        val phrase: String,
        val label: String,
        val imageType: ImageType,
        val imageValue: String,
        val color: Long?,
    ) : SentenceToken {
        override val text: String get() = phrase
    }

    data class Text(override val text: String) : SentenceToken
}

/** The spoken sentence, joining every token with a single space. */
fun List<SentenceToken>.toSentenceText(): String =
    joinToString(" ") { it.text }
        .replace(Regex("\\s+"), " ")
        .trim()

/**
 * Deletes the word before [caret] in [text], returning the new text and the
 * caret position in it. Any text after the caret is kept.
 */
fun deleteWordBefore(text: String, caret: Int): Pair<String, Int> {
    val before = text.take(caret)
    val after = text.drop(caret)
    val trimmedBefore = before.trimEnd()
    val lastSpace = trimmedBefore.lastIndexOf(' ')
    val keptBefore = if (lastSpace == -1) "" else trimmedBefore.substring(0, lastSpace)
    val keptAfter = after.trimStart()
    val newText = buildString {
        append(keptBefore)
        if (keptBefore.isNotEmpty() && keptAfter.isNotEmpty()) append(' ')
        append(keptAfter)
    }
    return newText to keptBefore.length
}
