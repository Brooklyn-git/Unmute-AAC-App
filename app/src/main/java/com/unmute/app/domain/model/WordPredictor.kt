package com.unmute.app.domain.model

/** Origin of a vocabulary word, used to rank suggestions. Lower ordinal wins. */
enum class WordTier { LABEL, PHRASE, COMMON }

data class VocabularyWord(val word: String, val tier: WordTier)

/** Persisted usage stats for a word in one language. */
data class WordUsage(val uses: Int, val lastUsed: Long)

/** Vocabulary plus per-word usage stats, fed to [predict]. */
data class PredictionVocabulary(
    val words: List<VocabularyWord>,
    val usage: Map<String, WordUsage>,
)

/** How much the strongest usage stats can add on top of a word's tier score. */
const val MAX_PREDICTION_USAGE_BOOST = 50

const val DEFAULT_PREDICTION_LIMIT = 3

private const val TIER_LABEL_SCORE = 300
private const val TIER_PHRASE_SCORE = 200
private const val TIER_COMMON_SCORE = 100

private data class RankedWord(val word: String, val score: Int, val lastUsed: Long)

/**
 * Completes [prefix] with matching vocabulary words, ranked by tier, then usage
 * (frequency + recency), then alphabetically. Returns at most [limit] words.
 */
fun predict(
    prefix: String,
    vocabulary: List<VocabularyWord>,
    usage: Map<String, WordUsage>,
    limit: Int,
): List<String> {
    val normalized = prefix.lowercase().trim()
    if (normalized.isEmpty() || limit <= 0) return emptyList()

    val bestTier = mutableMapOf<String, WordTier>()
    for (candidate in vocabulary) {
        if (!candidate.word.lowercase().startsWith(normalized)) continue
        val existing = bestTier[candidate.word]
        if (existing == null || candidate.tier.ordinal < existing.ordinal) {
            bestTier[candidate.word] = candidate.tier
        }
    }

    return bestTier.entries
        .map { (word, tier) ->
            val stats = usage[word]
            RankedWord(
                word = word,
                score = tierScore(tier) + (stats?.uses ?: 0).coerceAtMost(MAX_PREDICTION_USAGE_BOOST),
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

/** Builds the suggestion vocabulary from bilingual card labels, phrases and common words. */
fun vocabularyFrom(
    labels: List<String>,
    phrases: List<String>,
    commonWords: List<String>,
): List<VocabularyWord> = buildList {
    labels.forEach { label -> label.splitToWords().forEach { add(VocabularyWord(it, WordTier.LABEL)) } }
    phrases.forEach { phrase -> phrase.splitToWords().forEach { add(VocabularyWord(it, WordTier.PHRASE)) } }
    commonWords.forEach { add(VocabularyWord(it, WordTier.COMMON)) }
}

private fun Char.isWordCharacter(): Boolean = isLetter() || this == '\''

private fun String.splitToWords(): List<String> =
    lowercase().split(Regex("[^\\p{L}\\p{M}']+")).filter { it.isNotEmpty() }

private fun tierScore(tier: WordTier): Int = when (tier) {
    WordTier.LABEL -> TIER_LABEL_SCORE
    WordTier.PHRASE -> TIER_PHRASE_SCORE
    WordTier.COMMON -> TIER_COMMON_SCORE
}
