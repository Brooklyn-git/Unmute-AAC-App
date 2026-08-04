package com.unmute.app.data.backup

import com.unmute.app.domain.model.AppLanguage
import com.unmute.app.domain.model.CardFontSize
import com.unmute.app.domain.model.ImageType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sampleBackup = BackupFile(
        version = BACKUP_VERSION,
        board = BackupBoard(id = 1, nameEn = "Unmute", nameEs = "Unmute"),
        categories = listOf(
            BackupCategory(
                id = 1,
                boardId = 1,
                nameEn = "Food",
                nameEs = "Comida",
                color = 0xFF000000L,
                orderIndex = 0,
                isPreset = true,
                symbolType = ImageType.EMOJI.name,
                symbolValue = "🍎",
            ),
        ),
        cards = listOf(
            BackupCard(
                id = 1,
                categoryId = 1,
                labelEn = "Hungry",
                labelEs = "Hambre",
                phraseEn = "I am hungry",
                phraseEs = "Tengo hambre",
                imageType = ImageType.PHOTO.name,
                imageValue = "photos/photo_1.jpg",
                color = null,
                orderIndex = 0,
                shortcutCategoryId = 1,
            ),
        ),
        gridProfiles = listOf(
            BackupGridProfile(id = 1, name = "Big", columns = 3, isPreset = true),
        ),
        settings = BackupSettings(
            language = AppLanguage.EN.name,
            activeGridProfileId = 1,
            audioOutput = "auto",
            ttsEngine = null,
            autospeak = true,
            speakOnAdd = true,
            speechRate = 1f,
            speechPitch = 1f,
            cardFontSize = CardFontSize.NORMAL.name,
            secureMode = true,
            secureTapCount = 3,
            secureResetSeconds = 2,
            sectionLayout = "GRID",
            speakSectionNames = true,
        ),
    )

    @Test
    fun `backup round-trips through JSON`() {
        val encoded = json.encodeToString(BackupFile.serializer(), sampleBackup)
        val decoded = json.decodeFromString(BackupFile.serializer(), encoded)
        assertEquals(sampleBackup, decoded)
    }

    @Test
    fun `backup without new fields parses with defaults`() {
        val legacy = """
            {
              "version": 1,
              "board": {"id": 1, "nameEn": "Unmute", "nameEs": "Unmute"},
              "categories": [
                {
                  "id": 1, "boardId": 1, "nameEn": "Food", "nameEs": "Comida",
                  "color": 4294901760, "orderIndex": 0, "isPreset": true
                }
              ],
              "cards": [
                {
                  "id": 1, "categoryId": 1, "labelEn": "Hungry", "labelEs": "Hambre",
                  "phraseEn": "I am hungry", "phraseEs": "Tengo hambre",
                  "imageType": "PHOTO", "imageValue": "photos/photo_1.jpg",
                  "color": null, "orderIndex": 0
                }
              ],
              "gridProfiles": [],
              "settings": {
                "language": "EN", "activeGridProfileId": 1, "audioOutput": "auto",
                "ttsEngine": null, "autospeak": false, "speakOnAdd": true,
                "speechRate": 1.0, "speechPitch": 1.0, "cardFontSize": "NORMAL",
                "secureMode": false, "secureTapCount": 3, "secureResetSeconds": 2
              }
            }
        """.trimIndent()
        val decoded = json.decodeFromString(BackupFile.serializer(), legacy)
        assertEquals("EMOJI", decoded.categories[0].symbolType)
        assertEquals("", decoded.categories[0].symbolValue)
        assertEquals("TABS", decoded.settings.sectionLayout)
        assertEquals(false, decoded.settings.speakSectionNames)
        assertEquals(null, decoded.cards[0].shortcutCategoryId)
    }
}
