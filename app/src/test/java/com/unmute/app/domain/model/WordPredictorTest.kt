package com.unmute.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordPredictorTest {

    private fun word(w: String, tier: WordTier) = VocabularyWord(w, tier)

    @Test
    fun `predict returns words that match the prefix`() {
        val vocab = listOf(
            word("hello", WordTier.LABEL),
            word("help", WordTier.LABEL),
            word("world", WordTier.LABEL),
        )
        assertEquals(listOf("hello", "help"), predict("hel", vocab, emptyMap(), 5))
    }

    @Test
    fun `predict is case-insensitive`() {
        val vocab = listOf(word("Hello", WordTier.LABEL), word("HELP", WordTier.LABEL))
        assertEquals(listOf("Hello", "HELP"), predict("h", vocab, emptyMap(), 5))
        assertEquals(listOf("Hello", "HELP"), predict("HEL", vocab, emptyMap(), 5))
    }

    @Test
    fun `predict ranks labels above phrase and common words`() {
        val vocab = listOf(
            word("thank", WordTier.PHRASE),
            word("thank", WordTier.LABEL),
            word("the", WordTier.COMMON),
        )
        assertEquals(listOf("thank", "the"), predict("th", vocab, emptyMap(), 5))
    }

    @Test
    fun `predict reorders same-tier words by usage`() {
        val vocab = listOf(
            word("help", WordTier.LABEL),
            word("hello", WordTier.LABEL),
        )
        val usage = mapOf("hello" to WordUsage(uses = 30, lastUsed = 0L))
        assertEquals(listOf("hello", "help"), predict("he", vocab, usage, 5))
    }

    @Test
    fun `predict caps the usage boost so tier always dominates`() {
        val vocab = listOf(
            word("bread", WordTier.COMMON),
            word("breakfast", WordTier.PHRASE),
        )
        val usage = mapOf("bread" to WordUsage(uses = 500, lastUsed = 0L))
        assertEquals(listOf("breakfast", "bread"), predict("br", vocab, usage, 5))
    }

    @Test
    fun `predict uses recency to break usage ties`() {
        val vocab = listOf(word("and", WordTier.LABEL), word("all", WordTier.LABEL))
        val usage = mapOf(
            "and" to WordUsage(uses = 1, lastUsed = 100L),
            "all" to WordUsage(uses = 1, lastUsed = 200L),
        )
        assertEquals(listOf("all", "and"), predict("a", vocab, usage, 5))
    }

    @Test
    fun `predict falls back to alphabetical order for equal scores`() {
        val vocab = listOf(word("apple", WordTier.LABEL), word("alpha", WordTier.LABEL))
        assertEquals(listOf("alpha", "apple"), predict("a", vocab, emptyMap(), 5))
    }

    @Test
    fun `predict respects the limit`() {
        val vocab = listOf(
            word("a1", WordTier.LABEL),
            word("a2", WordTier.LABEL),
            word("a3", WordTier.LABEL),
            word("a4", WordTier.LABEL),
        )
        assertEquals(listOf("a1", "a2"), predict("a", vocab, emptyMap(), 2))
    }

    @Test
    fun `predict returns nothing for an empty prefix`() {
        val vocab = listOf(word("hello", WordTier.LABEL))
        assertEquals(emptyList<String>(), predict("", vocab, emptyMap(), 5))
        assertEquals(emptyList<String>(), predict("   ", vocab, emptyMap(), 5))
    }

    @Test
    fun `predict returns nothing for a non-positive limit`() {
        assertEquals(emptyList<String>(), predict("h", listOf(word("hello", WordTier.LABEL)), emptyMap(), 0))
    }

    @Test
    fun `currentWordAt returns the partial word before the caret`() {
        assertEquals("hel", currentWordAt("hello world", 3))
    }

    @Test
    fun `currentWordAt returns empty when the caret is after a space`() {
        assertEquals("", currentWordAt("hello ", 6))
    }

    @Test
    fun `currentWordAt handles a caret inside a word`() {
        assertEquals("wor", currentWordAt("hello world", 9))
    }

    @Test
    fun `applySuggestion replaces the partial word and adds a space`() {
        val (text, caret) = applySuggestion("hello wor", 9, "world")
        assertEquals("hello world ", text)
        assertEquals(12, caret)
    }

    @Test
    fun `applySuggestion keeps text after the caret without doubling spaces`() {
        val (text, caret) = applySuggestion("hello wor done", 9, "world")
        assertEquals("hello world done", text)
        assertEquals(11, caret)
    }

    @Test
    fun `applySuggestion works at the start of the sentence`() {
        val (text, caret) = applySuggestion("hung", 4, "hungry")
        assertEquals("hungry ", text)
        assertEquals(7, caret)
    }

    @Test
    fun `vocabularyFrom extracts label and phrase words with tiers`() {
        val words = vocabularyFrom(
            labels = listOf("Good morning"),
            phrases = listOf("I am hungry"),
            commonWords = listOf("and"),
        )
        assertTrue(VocabularyWord("good", WordTier.LABEL) in words)
        assertTrue(VocabularyWord("morning", WordTier.LABEL) in words)
        assertTrue(VocabularyWord("i", WordTier.PHRASE) in words)
        assertTrue(VocabularyWord("hungry", WordTier.PHRASE) in words)
        assertTrue(VocabularyWord("and", WordTier.COMMON) in words)
    }

    @Test
    fun `vocabularyFrom lowercases and drops empty words`() {
        val words = vocabularyFrom(
            labels = listOf("  Hello   World "),
            phrases = emptyList(),
            commonWords = listOf("a"),
        )
        assertTrue(VocabularyWord("hello", WordTier.LABEL) in words)
        assertTrue(VocabularyWord("world", WordTier.LABEL) in words)
        assertTrue(VocabularyWord("a", WordTier.COMMON) in words)
    }

    @Test
    fun `common words lists are non-empty and lowercased`() {
        listOf(CommonWords.ENGLISH, CommonWords.SPANISH).forEach { list ->
            assertTrue(list.isNotEmpty())
            assertTrue(list.all { it == it.lowercase() })
        }
    }
}
