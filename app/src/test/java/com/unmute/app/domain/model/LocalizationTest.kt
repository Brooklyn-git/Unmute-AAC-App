package com.unmute.app.domain.model

import com.unmute.app.data.local.BoardEntity
import com.unmute.app.data.local.CardEntity
import com.unmute.app.data.local.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizationTest {

    @Test
    fun `resolveLanguage maps system Spanish to es`() {
        assertEquals("es", resolveLanguage(AppLanguage.SYSTEM, "es"))
        assertEquals("es", resolveLanguage(AppLanguage.SYSTEM, "es-ES"))
        assertEquals("en", resolveLanguage(AppLanguage.SYSTEM, "en"))
        assertEquals("en", resolveLanguage(AppLanguage.SYSTEM, "fr"))
    }

    @Test
    fun `resolveLanguage honours explicit choice`() {
        assertEquals("en", resolveLanguage(AppLanguage.EN, "es"))
        assertEquals("es", resolveLanguage(AppLanguage.ES, "en"))
    }

    @Test
    fun `board label resolves language`() {
        val board = BoardEntity(nameEn = "Unmute", nameEs = "Unmute", orderIndex = 0)
        assertEquals("Unmute", board.label("en"))
        assertEquals("Unmute", board.label("es"))
    }

    @Test
    fun `category label falls back to English when Spanish is blank`() {
        val category = CategoryEntity(boardId = 1, nameEn = "Food", nameEs = "", color = 0L, orderIndex = 0)
        assertEquals("Food", category.label("es"))
        assertEquals("Food", category.label("en"))
    }

    @Test
    fun `card label and phrase resolve by language`() {
        val card = CardEntity(
            categoryId = 1,
            labelEn = "Hungry",
            labelEs = "Hambre",
            phraseEn = "I am hungry",
            phraseEs = "Tengo hambre",
            imageType = ImageType.EMOJI,
            imageValue = "😋",
            color = null,
            orderIndex = 0,
        )
        assertEquals("Hungry", card.label("en"))
        assertEquals("Hambre", card.label("es"))
        assertEquals("I am hungry", card.phrase("en"))
        assertEquals("Tengo hambre", card.phrase("es"))
    }

    @Test
    fun `withEditedLabels updates only the active language on existing cards`() {
        val card = CardEntity(
            categoryId = 1,
            labelEn = "Hungry",
            labelEs = "Hambre",
            phraseEn = "I am hungry",
            phraseEs = "Tengo hambre",
            imageType = ImageType.EMOJI,
            imageValue = "😋",
            color = null,
            orderIndex = 0,
        )

        val editedEn = card.withEditedLabels(isNew = false, lang = "en", label = "Thirsty", phrase = "I am thirsty")
        assertEquals("Thirsty", editedEn.labelEn)
        assertEquals("I am thirsty", editedEn.phraseEn)
        assertEquals("Hambre", editedEn.labelEs)
        assertEquals("Tengo hambre", editedEn.phraseEs)

        val editedEs = card.withEditedLabels(isNew = false, lang = "es", label = "Sed", phrase = "Tengo sed")
        assertEquals("Sed", editedEs.labelEs)
        assertEquals("Tengo sed", editedEs.phraseEs)
        assertEquals("Hungry", editedEs.labelEn)
        assertEquals("I am hungry", editedEs.phraseEn)
    }

    @Test
    fun `withEditedLabels sets both languages for new cards`() {
        val card = CardEntity(
            categoryId = 1,
            labelEn = "",
            labelEs = "",
            phraseEn = "",
            phraseEs = "",
            imageType = ImageType.EMOJI,
            imageValue = "😋",
            color = null,
            orderIndex = 0,
        )

        val newCard = card.withEditedLabels(isNew = true, lang = "en", label = "Hello", phrase = "Hello world")
        assertEquals("Hello", newCard.labelEn)
        assertEquals("Hello", newCard.labelEs)
        assertEquals("Hello world", newCard.phraseEn)
        assertEquals("Hello world", newCard.phraseEs)
    }
}
