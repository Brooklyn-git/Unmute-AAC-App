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

/**
 * Returns the list with the token at [from] moved to [to]. Out-of-range
 * indexes are clamped and an unchanged list is returned when nothing moves.
 */
fun List<SentenceToken>.moved(from: Int, to: Int): List<SentenceToken> {
    if (from !in indices) return this
    val target = to.coerceIn(indices)
    if (target == from) return this
    val reordered = toMutableList()
    reordered.add(target, reordered.removeAt(from))
    return reordered
}

/**
 * Returns the list with the typed text at [index] replaced by [text], or that
 * token removed when [text] is empty. Indexes that are not [SentenceToken.Text]
 * are left untouched.
 */
fun List<SentenceToken>.withTextAt(index: Int, text: String): List<SentenceToken> {
    if (getOrNull(index) !is SentenceToken.Text) return this
    if (text.isEmpty()) return filterIndexed { i, _ -> i != index }
    return mapIndexed { i, token -> if (i == index) SentenceToken.Text(text) else token }
}

/**
 * Inserts or updates typed text in the sentence. With [anchorIndex] null the
 * text is appended at the end; otherwise it is placed right after the token at
 * [anchorIndex]. An empty [text] removes the text token at that spot instead.
 */
fun List<SentenceToken>.withTextAfter(anchorIndex: Int?, text: String): List<SentenceToken> {
    val targetIndex = when {
        anchorIndex != null -> {
            val position = anchorIndex + 1
            if (getOrNull(position) is SentenceToken.Text) position else -1
        }
        text.isEmpty() -> indexOfLast { it is SentenceToken.Text }
        lastOrNull() is SentenceToken.Text -> lastIndex
        else -> -1
    }
    if (targetIndex != -1) {
        return if (text.isEmpty()) {
            filterIndexed { i, _ -> i != targetIndex }
        } else {
            mapIndexed { i, token -> if (i == targetIndex) SentenceToken.Text(text) else token }
        }
    }
    if (text.isEmpty()) return this
    val insertPosition = if (anchorIndex == null) size else anchorIndex + 1
    return toMutableList().apply { add(insertPosition, SentenceToken.Text(text)) }
}
