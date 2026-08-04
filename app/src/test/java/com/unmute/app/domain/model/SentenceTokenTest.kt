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
}
