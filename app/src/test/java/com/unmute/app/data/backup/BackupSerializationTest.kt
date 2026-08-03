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
        ),
    )

    @Test
    fun `backup round-trips through JSON`() {
        val encoded = json.encodeToString(BackupFile.serializer(), sampleBackup)
        val decoded = json.decodeFromString(BackupFile.serializer(), encoded)
        assertEquals(sampleBackup, decoded)
    }
}
