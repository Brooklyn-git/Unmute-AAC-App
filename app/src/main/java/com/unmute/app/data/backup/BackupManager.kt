package com.unmute.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.unmute.app.data.AppSettings
import com.unmute.app.data.BoardRepository
import com.unmute.app.data.SettingsRepository
import com.unmute.app.data.local.BoardEntity
import com.unmute.app.data.local.CardEntity
import com.unmute.app.data.local.CategoryEntity
import com.unmute.app.data.local.GridProfileEntity
import com.unmute.app.data.local.UnmuteDatabase
import com.unmute.app.domain.model.AppLanguage
import com.unmute.app.domain.model.CardFontSize
import com.unmute.app.domain.model.ImageType
import com.unmute.app.domain.model.SectionLayout
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Backs up and restores the whole app data: board, categories, cards, grid
 * profiles, card photos and settings. A backup is a ZIP with a `backup.json`
 * manifest plus the photo files under `photos/`.
 */
class BackupManager(
    private val context: Context,
    private val database: UnmuteDatabase,
    private val boardRepository: BoardRepository,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(uri)
            ?: throw IOException("Could not open destination file")
        output.use { exportTo(it) }
    }

    suspend fun importFrom(uri: Uri) = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open backup file")
        input.use { importFrom(it) }
    }

    private suspend fun exportTo(outputStream: OutputStream) {
        val board = boardRepository.getBoard() ?: throw IOException("No board to export")
        val categories = boardRepository.getCategories(board.id)
        val cards = categories.flatMap { boardRepository.getCards(it.id) }
        val gridProfiles = boardRepository.getAllGridProfiles()
        val settings = settingsRepository.settings.first()

        val backup = BackupFile(
            version = BACKUP_VERSION,
            board = BackupBoard(id = board.id, nameEn = board.nameEn, nameEs = board.nameEs),
            categories = categories.map {
                BackupCategory(
                    id = it.id,
                    boardId = it.boardId,
                    nameEn = it.nameEn,
                    nameEs = it.nameEs,
                    color = it.color,
                    orderIndex = it.orderIndex,
                    isPreset = it.isPreset,
                    symbolType = it.symbolType.name,
                    symbolValue = it.symbolValue,
                )
            },
            cards = cards.map {
                BackupCard(
                    id = it.id,
                    categoryId = it.categoryId,
                    labelEn = it.labelEn,
                    labelEs = it.labelEs,
                    phraseEn = it.phraseEn,
                    phraseEs = it.phraseEs,
                    imageType = it.imageType.name,
                    imageValue = exportImageValue(it),
                    color = it.color,
                    orderIndex = it.orderIndex,
                )
            },
            gridProfiles = gridProfiles.map {
                BackupGridProfile(id = it.id, name = it.name, columns = it.columns, isPreset = it.isPreset)
            },
            settings = settings.toBackup(),
        )

        ZipOutputStream(outputStream).use { zip ->
            zip.putNextEntry(ZipEntry(BACKUP_MANIFEST_NAME))
            zip.write(Json.encodeToString(BackupFile.serializer(), backup).toByteArray())
            zip.closeEntry()

            cards.forEach { card ->
                if (card.imageType == ImageType.PHOTO) {
                    val file = File(card.imageValue)
                    if (file.isFile) {
                        zip.putNextEntry(ZipEntry("$BACKUP_PHOTOS_DIR/${file.name}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    private suspend fun importFrom(inputStream: InputStream) {
        val (backup, photoPathByEntry) = readBackup(inputStream)
        validateBackup(backup)

        val restored = backup.toEntities(photoPathByEntry)
        database.withTransaction {
            boardRepository.replaceAll(
                board = restored.board,
                categories = restored.categories,
                cards = restored.cards,
                gridProfiles = restored.gridProfiles,
            )
        }
        settingsRepository.restore(backup.settings.toAppSettings())
    }

    /** Parses the ZIP, saving photos to app storage, and returns backup + photo paths. */
    private fun readBackup(inputStream: InputStream): Pair<BackupFile, Map<String, String>> {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val photos = mutableMapOf<String, String>()
        var backup: BackupFile? = null

        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory) {
                    when {
                        name == BACKUP_MANIFEST_NAME ->
                            backup = Json.decodeFromString(zip.readBytes().decodeToString())

                        name.startsWith("$BACKUP_PHOTOS_DIR/") -> {
                            val dest = File(photosDir, File(name).name)
                            dest.writeBytes(zip.readBytes())
                            photos[name] = dest.absolutePath
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val manifest = backup ?: throw IOException("Invalid backup: manifest not found")
        return manifest to photos
    }

    private fun validateBackup(backup: BackupFile) {
        if (backup.version != BACKUP_VERSION) throw IOException("Unsupported backup version")
        if (backup.categories.isEmpty()) throw IOException("Invalid backup: no categories")
        val categoryIds = backup.categories.map { it.id }.toSet()
        val unknownCategory = backup.cards.any { it.categoryId !in categoryIds }
        if (unknownCategory) throw IOException("Invalid backup: card references missing category")
    }

    private fun exportImageValue(card: CardEntity): String =
        if (card.imageType == ImageType.PHOTO) {
            "$BACKUP_PHOTOS_DIR/${File(card.imageValue).name}"
        } else {
            card.imageValue
        }

    private fun importImageValue(card: BackupCard, photoPathByEntry: Map<String, String>): String =
        if (card.imageType == ImageType.PHOTO.name) {
            photoPathByEntry[card.imageValue] ?: ""
        } else {
            card.imageValue
        }

    private fun BackupFile.toEntities(
        photoPathByEntry: Map<String, String>,
    ): RestoredContent = RestoredContent(
        board = BoardEntity(
            id = board.id,
            nameEn = board.nameEn,
            nameEs = board.nameEs,
            orderIndex = 0,
        ),
        categories = categories.map {
            CategoryEntity(
                id = it.id,
                boardId = it.boardId,
                nameEn = it.nameEn,
                nameEs = it.nameEs,
                color = it.color,
                orderIndex = it.orderIndex,
                isPreset = it.isPreset,
                symbolType = runCatching { ImageType.valueOf(it.symbolType) }
                    .getOrDefault(ImageType.EMOJI),
                symbolValue = it.symbolValue,
            )
        },
        cards = cards.map {
            CardEntity(
                id = it.id,
                categoryId = it.categoryId,
                labelEn = it.labelEn,
                labelEs = it.labelEs,
                phraseEn = it.phraseEn,
                phraseEs = it.phraseEs,
                imageType = runCatching { ImageType.valueOf(it.imageType) }.getOrDefault(ImageType.EMOJI),
                imageValue = importImageValue(it, photoPathByEntry),
                color = it.color,
                orderIndex = it.orderIndex,
            )
        },
        gridProfiles = gridProfiles.map {
            GridProfileEntity(id = it.id, name = it.name, columns = it.columns, isPreset = it.isPreset)
        },
    )

    private data class RestoredContent(
        val board: BoardEntity,
        val categories: List<CategoryEntity>,
        val cards: List<CardEntity>,
        val gridProfiles: List<GridProfileEntity>,
    )

    private fun AppSettings.toBackup() = BackupSettings(
        language = language.name,
        activeGridProfileId = activeGridProfileId,
        audioOutput = audioOutput,
        ttsEngine = ttsEngine,
        autospeak = autospeak,
        speakOnAdd = speakOnAdd,
        speechRate = speechRate,
        speechPitch = speechPitch,
        cardFontSize = cardFontSize.name,
        secureMode = secureMode,
        secureTapCount = secureTapCount,
        secureResetSeconds = secureResetSeconds,
        sectionLayout = sectionLayout.name,
        speakSectionNames = speakSectionNames,
    )

    private fun BackupSettings.toAppSettings() = AppSettings(
        language = runCatching { AppLanguage.valueOf(language) }.getOrDefault(AppLanguage.SYSTEM),
        activeGridProfileId = activeGridProfileId,
        audioOutput = audioOutput,
        ttsEngine = ttsEngine,
        autospeak = autospeak,
        speakOnAdd = speakOnAdd,
        speechRate = speechRate,
        speechPitch = speechPitch,
        cardFontSize = runCatching { CardFontSize.valueOf(cardFontSize) }.getOrDefault(CardFontSize.NORMAL),
        secureMode = secureMode,
        secureTapCount = secureTapCount.coerceIn(
            SettingsRepository.MIN_SECURE_TAPS,
            SettingsRepository.MAX_SECURE_TAPS,
        ),
        secureResetSeconds = secureResetSeconds.coerceIn(
            SettingsRepository.MIN_SECURE_RESET_SECONDS,
            SettingsRepository.MAX_SECURE_RESET_SECONDS,
        ),
        sectionLayout = runCatching { SectionLayout.valueOf(sectionLayout) }
            .getOrDefault(SectionLayout.TABS),
        speakSectionNames = speakSectionNames,
    )
}
