package com.unmute.app.domain.model

/**
 * Origin of a vocabulary word, used to rank suggestions. Lower ordinal wins and the
 * tier score gaps are larger than any combined boost so a higher tier always wins.
 */
enum class WordTier(val score: Int) {
    CATEGORY(600),
    LABEL(450),
    PHRASE(300),
    WORD(150),
    COMMON(0),
}

data class VocabularyWord(
    val word: String,
    val tier: WordTier,
    /** Whether this word comes from the currently open section and should rank higher. */
    val contextual: Boolean = false,
)

/** Persisted usage stats for a word in one language. */
data class WordUsage(val uses: Int, val lastUsed: Long)

/** Vocabulary plus per-word usage stats, fed to [predict]. */
data class PredictionVocabulary(
    val words: List<VocabularyWord>,
    val usage: Map<String, WordUsage>,
)

/** How much usage stats can add on top of a word's tier score. */
const val MAX_PREDICTION_USAGE_BOOST = 50

/** How much belonging to the open section adds on top of a word's tier score. */
const val MAX_PREDICTION_CONTEXT_BOOST = 60

const val DEFAULT_PREDICTION_LIMIT = 4

private data class RankedWord(val word: String, val score: Int, val lastUsed: Long)

/**
 * Completes [prefix] with matching vocabulary words, ranked by tier, then contextual
 * relevance, usage (frequency + recency), then alphabetically. Full phrases match
 * through their first word. Returns at most [limit] words.
 */
fun predict(
    prefix: String,
    vocabulary: List<VocabularyWord>,
    usage: Map<String, WordUsage>,
    limit: Int,
): List<String> {
    val normalized = prefix.lowercase().trim()
    if (normalized.isEmpty() || limit <= 0) return emptyList()

    val best = mutableMapOf<String, VocabularyWord>()
    for (candidate in vocabulary) {
        if (!candidate.matches(normalized)) continue
        val existing = best[candidate.word]
        if (
            existing == null ||
            candidate.tier.ordinal < existing.tier.ordinal ||
            (candidate.tier == existing.tier && candidate.contextual && !existing.contextual)
        ) {
            best[candidate.word] = candidate
        }
    }

    return best.values
        .map { candidate ->
            val stats = usage[candidate.word]
            RankedWord(
                word = candidate.word,
                score = candidate.tier.score +
                    (if (candidate.contextual) MAX_PREDICTION_CONTEXT_BOOST else 0) +
                    (stats?.uses ?: 0).coerceAtMost(MAX_PREDICTION_USAGE_BOOST),
                lastUsed = stats?.lastUsed ?: Long.MIN_VALUE,
            )
        }
        .sortedWith(
            compareByDescending<RankedWord> { it.score }
                .thenByDescending { it.lastUsed }
                .thenBy { it.word.lowercase() },
        )
        .take(limit)
        .map { it.word }
}

private fun VocabularyWord.matches(normalized: String): Boolean {
    val start = if (tier == WordTier.PHRASE) {
        word.lowercase().trim().substringBefore(' ')
    } else {
        word.lowercase()
    }
    return start.startsWith(normalized)
}

/** The unfinished word immediately before [caret], or "" when the caret is at a word start. */
fun currentWordAt(text: String, caret: Int): String {
    val before = text.take(caret.coerceIn(0, text.length))
    val index = before.indexOfLast { !it.isWordCharacter() }
    return before.substring(index + 1)
}

/**
 * Replaces the partial word before [caret] with [suggestion], keeping any text
 * after the caret, and returns the new text with the caret after the suggestion.
 */
fun applySuggestion(text: String, caret: Int, suggestion: String): Pair<String, Int> {
    val safeCaret = caret.coerceIn(0, text.length)
    val current = currentWordAt(text, safeCaret)
    val keepFrom = safeCaret - current.length
    val after = text.drop(safeCaret)
    val separator = if (after.startsWith(" ")) "" else " "
    val newText = text.take(keepFrom) + suggestion + separator + after
    return newText to (keepFrom + suggestion.length + separator.length)
}

/** Lowercases [this] and splits it into words, keeping apostrophes inside words. */
fun String.splitToWords(): List<String> =
    lowercase().split(Regex("[^\\p{L}\\p{M}']+")).filter { it.isNotEmpty() }

/**
 * Builds the suggestion vocabulary from bilingual section names, card labels, card
 * phrases (kept whole and also split into words) and common words. Words whose
 * lowercase form is in [contextualWords] are marked as belonging to the open section.
 */
fun vocabularyFrom(
    labels: List<String>,
    phrases: List<String>,
    categories: List<String>,
    commonWords: List<String>,
    contextualWords: Set<String> = emptySet(),
): List<VocabularyWord> {
    val contextual = contextualWords.mapTo(mutableSetOf()) { it.lowercase().trim() }
    return buildList {
        categories.forEach { name ->
            name.splitToWords().forEach { add(VocabularyWord(it, WordTier.CATEGORY, it in contextual)) }
        }
        labels.forEach { label ->
            label.splitToWords().forEach { add(VocabularyWord(it, WordTier.LABEL, it in contextual)) }
        }
        phrases.forEach { phrase ->
            val trimmed = phrase.trim()
            if (trimmed.isNotEmpty()) {
                add(VocabularyWord(trimmed, WordTier.PHRASE, trimmed.lowercase() in contextual))
            }
            phrase.splitToWords().forEach { add(VocabularyWord(it, WordTier.WORD, it in contextual)) }
        }
        commonWords.forEach { add(VocabularyWord(it, WordTier.COMMON)) }
    }
}

private fun Char.isWordCharacter(): Boolean = isLetter() || this == '\''
