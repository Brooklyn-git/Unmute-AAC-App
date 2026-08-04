package com.unmute.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SentenceTokenTest {

    @Test
    fun `toSentenceText joins card phrases and typed text`() {
        val tokens = listOf(
            SentenceToken.Card(
                cardId = 1,
                phrase = "I am hungry",
                label = "Hungry",
                imageType = ImageType.EMOJI,
                imageValue = "😋",
                color = null,
            ),
            SentenceToken.Text("please"),
            SentenceToken.Card(
                cardId = 2,
                phrase = "some water",
                label = "Water",
                imageType = ImageType.EMOJI,
                imageValue = "💧",
                color = null,
            ),
        )
        assertEquals("I am hungry please some water", tokens.toSentenceText())
    }

    @Test
    fun `toSentenceText collapses extra whitespace and trims`() {
        val tokens = listOf(
            SentenceToken.Text("  hello   "),
            SentenceToken.Text("world "),
        )
        assertEquals("hello world", tokens.toSentenceText())
    }

    @Test
    fun `empty sentence is blank`() {
        assertEquals("", emptyList<SentenceToken>().toSentenceText())
    }

    @Test
    fun `deleteWordBefore removes the last word at the end of the sentence`() {
        assertEquals("hello" to 5, deleteWordBefore("hello world", 11))
        assertEquals("" to 0, deleteWordBefore("hello", 5))
    }

    @Test
    fun `deleteWordBefore removes only the word before the caret`() {
        val (text, caret) = deleteWordBefore("I want food", 7)
        assertEquals("I food", text)
        assertEquals(1, caret)
    }

    @Test
    fun `deleteWordBefore keeps text after the caret`() {
        assertEquals("I to go", deleteWordBefore("I want to go", 7).first)
        assertEquals(1, deleteWordBefore("I want to go", 7).second)
    }

    @Test
    fun `deleteWordBefore removes the partial word when caret is inside it`() {
        val (text, caret) = deleteWordBefore("hello world", 3)
        assertEquals("lo world", text)
        assertEquals(0, caret)
    }

    @Test
    fun `moved relocates a token to a later position`() {
        val tokens = listOf(
            SentenceToken.Card(1, "one", "One", ImageType.EMOJI, "1", null),
            SentenceToken.Text("two"),
            SentenceToken.Card(3, "three", "Three", ImageType.EMOJI, "3", null),
        )
        val moved = tokens.moved(0, 2)
        assertEquals(listOf("two", "three", "one"), moved.map { it.text })
    }

    @Test
    fun `moved relocates a token to an earlier position`() {
        val tokens = listOf(
            SentenceToken.Card(1, "one", "One", ImageType.EMOJI, "1", null),
            SentenceToken.Text("two"),
            SentenceToken.Card(3, "three", "Three", ImageType.EMOJI, "3", null),
        )
        val moved = tokens.moved(2, 0)
        assertEquals(listOf("three", "one", "two"), moved.map { it.text })
    }

    @Test
    fun `moved ignores out of range source indexes and clamps the target`() {
        val tokens = listOf(SentenceToken.Text("a"), SentenceToken.Text("b"))
        assertEquals(tokens, tokens.moved(5, 0))
        assertEquals(listOf("b", "a"), tokens.moved(0, 5).map { it.text })
    }

    @Test
    fun `moved is a no-op when moving onto itself`() {
        val tokens = listOf(SentenceToken.Text("a"), SentenceToken.Text("b"))
        assertEquals(tokens, tokens.moved(1, 1))
    }

    @Test
    fun `dropTargetIndex moves a card into a gap between words`() {
        val centers = listOf(50f, 150f, 250f)
        assertEquals(1, dropTargetIndex(from = 0, dropX = 200f, centerXs = centers))
    }

    @Test
    fun `dropTargetIndex moves a card to the front`() {
        val centers = listOf(50f, 150f, 250f)
        assertEquals(0, dropTargetIndex(from = 2, dropX = 10f, centerXs = centers))
    }

    @Test
    fun `dropTargetIndex moves a card to the end`() {
        val centers = listOf(50f, 150f, 250f)
        assertEquals(2, dropTargetIndex(from = 0, dropX = 500f, centerXs = centers))
    }

    @Test
    fun `dropTargetIndex returns null when dropped back at its own slot`() {
        val centers = listOf(50f, 150f, 250f)
        assertEquals(null, dropTargetIndex(from = 0, dropX = 50f, centerXs = centers))
    }

    @Test
    fun `dropTargetIndex ignores the dragged card's own center`() {
        val centers = listOf(50f, 150f, 250f)
        assertEquals(2, dropTargetIndex(from = 1, dropX = 230f, centerXs = centers))
    }

    @Test
    fun `dropTargetIndex returns null for an out of range source`() {
        val centers = listOf(50f, 150f, 250f)
        assertEquals(null, dropTargetIndex(from = 5, dropX = 200f, centerXs = centers))
    }

    @Test
    fun `dropTargetIndex combined with moved inserts a card between words`() {
        val tokens = listOf(
            SentenceToken.Card(1, "I am hungry", "Hungry", ImageType.EMOJI, "😋", null),
            SentenceToken.Text("please"),
            SentenceToken.Text("water"),
        )
        val target = dropTargetIndex(from = 0, dropX = 200f, centerXs = listOf(50f, 150f, 250f))!!
        assertEquals(listOf("please", "I am hungry", "water"), tokens.moved(0, target).map { it.text })
    }

    @Test
    fun `withTextAt replaces the typed text at the given index`() {
        val tokens = listOf(
            SentenceToken.Text("please"),
            SentenceToken.Card(2, "water", "Water", ImageType.EMOJI, "💧", null),
        )
        val updated = tokens.withTextAt(0, "p")
        assertEquals(listOf("p", "water"), updated.map { it.text })
    }

    @Test
    fun `withTextAt removes the token when the text becomes empty`() {
        val tokens = listOf(
            SentenceToken.Text("please"),
            SentenceToken.Card(2, "water", "Water", ImageType.EMOJI, "💧", null),
        )
        val updated = tokens.withTextAt(0, "")
        assertEquals(listOf("water"), updated.map { it.text })
    }

    @Test
    fun `withTextAt ignores indexes that are not text tokens`() {
        val tokens = listOf(
            SentenceToken.Card(2, "water", "Water", ImageType.EMOJI, "💧", null),
        )
        assertEquals(tokens, tokens.withTextAt(0, "x"))
        assertEquals(tokens, tokens.withTextAt(3, "x"))
    }

    @Test
    fun `withTextAfter null anchor updates the trailing text`() {
        val tokens = listOf(
            SentenceToken.Card(1, "I", "I", ImageType.EMOJI, "1", null),
            SentenceToken.Text("old"),
        )
        val updated = tokens.withTextAfter(null, "new")
        assertEquals(listOf("I", "new"), updated.map { it.text })
    }

    @Test
    fun `withTextAfter null anchor appends when the sentence ends with a card`() {
        val tokens = listOf(
            SentenceToken.Card(1, "I", "I", ImageType.EMOJI, "1", null),
            SentenceToken.Card(2, "want", "Want", ImageType.EMOJI, "2", null),
        )
        val updated = tokens.withTextAfter(null, "to")
        assertEquals(listOf("I", "want", "to"), updated.map { it.text })
    }

    @Test
    fun `withTextAfter null anchor removes the trailing text when empty`() {
        val tokens = listOf(
            SentenceToken.Card(1, "I", "I", ImageType.EMOJI, "1", null),
            SentenceToken.Text("to"),
        )
        val updated = tokens.withTextAfter(null, "")
        assertEquals(listOf("I"), updated.map { it.text })
    }

    @Test
    fun `withTextAfter inserts text between cards`() {
        val tokens = listOf(
            SentenceToken.Card(1, "I", "I", ImageType.EMOJI, "1", null),
            SentenceToken.Card(2, "want", "Want", ImageType.EMOJI, "2", null),
            SentenceToken.Card(3, "water", "Water", ImageType.EMOJI, "3", null),
        )
        val updated = tokens.withTextAfter(1, "to")
        assertEquals(listOf("I", "want", "to", "water"), updated.map { it.text })
    }

    @Test
    fun `withTextAfter updates existing text after the anchor`() {
        val tokens = listOf(
            SentenceToken.Card(1, "I", "I", ImageType.EMOJI, "1", null),
            SentenceToken.Text("old"),
            SentenceToken.Card(2, "water", "Water", ImageType.EMOJI, "2", null),
        )
        val updated = tokens.withTextAfter(0, "new")
        assertEquals(listOf("I", "new", "water"), updated.map { it.text })
    }

    @Test
    fun `withTextAfter removes the text after the anchor when empty`() {
        val tokens = listOf(
            SentenceToken.Card(1, "I", "I", ImageType.EMOJI, "1", null),
            SentenceToken.Text("to"),
            SentenceToken.Card(2, "water", "Water", ImageType.EMOJI, "2", null),
        )
        val updated = tokens.withTextAfter(0, "")
        assertEquals(listOf("I", "water"), updated.map { it.text })
    }

    @Test
    fun `withTextAfter null anchor is a no-op when empty and there is no trailing text`() {
        val tokens = listOf(
            SentenceToken.Card(1, "I", "I", ImageType.EMOJI, "1", null),
        )
        assertEquals(tokens, tokens.withTextAfter(null, ""))
    }
}
