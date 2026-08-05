package com.unmute.app.data.backup

import kotlinx.serialization.Serializable

const val BACKUP_VERSION = 1

const val BACKUP_PHOTOS_DIR = "photos"
const val BACKUP_MANIFEST_NAME = "backup.json"

@Serializable
data class BackupFile(
    val version: Int,
    val board: BackupBoard,
    val categories: List<BackupCategory>,
    val cards: List<BackupCard>,
    val gridProfiles: List<BackupGridProfile>,
    val settings: BackupSettings,
)

@Serializable
data class BackupBoard(
    val id: Long,
    val nameEn: String,
    val nameEs: String,
)

@Serializable
data class BackupCategory(
    val id: Long,
    val boardId: Long,
    val nameEn: String,
    val nameEs: String,
    val color: Long,
    val orderIndex: Int,
    val isPreset: Boolean,
    val symbolType: String = "EMOJI",
    val symbolValue: String = "",
)

@Serializable
data class BackupCard(
    val id: Long,
    val categoryId: Long,
    val labelEn: String,
    val labelEs: String,
    val phraseEn: String,
    val phraseEs: String,
    val imageType: String,
    val imageValue: String,
    val color: Long?,
    val orderIndex: Int,
    val shortcutCategoryId: Long? = null,
)

@Serializable
data class BackupGridProfile(
    val id: Long,
    val name: String,
    val columns: Int,
    val isPreset: Boolean,
)

@Serializable
data class BackupSettings(
    val language: String,
    val activeGridProfileId: Long,
    val audioOutput: String,
    val ttsEngine: String?,
    val autospeak: Boolean,
    val speakOnAdd: Boolean,
    val speechRate: Float,
    val speechPitch: Float,
    val cardFontSize: String,
    val secureMode: Boolean,
    val secureTapCount: Int,
    val secureResetSeconds: Int,
    val sectionLayout: String = "TABS",
    val speakSectionNames: Boolean = false,
    val showSentenceCards: Boolean = false,
    val wordPrediction: Boolean = true,
)
