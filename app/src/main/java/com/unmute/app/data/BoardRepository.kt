package com.unmute.app.data

import com.unmute.app.data.local.BoardEntity
import com.unmute.app.data.local.BoardDao
import com.unmute.app.data.local.CardDao
import com.unmute.app.data.local.CardEntity
import com.unmute.app.data.local.CategoryDao
import com.unmute.app.data.local.CategoryEntity
import com.unmute.app.data.local.GridProfileDao
import com.unmute.app.data.local.GridProfileEntity
import com.unmute.app.data.local.WordUsageDao
import com.unmute.app.data.local.WordUsageEntity
import com.unmute.app.domain.model.ImageType
import kotlinx.coroutines.flow.Flow

class BoardRepository(
    private val boardDao: BoardDao,
    private val categoryDao: CategoryDao,
    private val cardDao: CardDao,
    private val gridProfileDao: GridProfileDao,
    private val wordUsageDao: WordUsageDao,
) {
    fun observeBoards(): Flow<List<BoardEntity>> = boardDao.observeBoards()

    fun observeBoard(id: Long): Flow<BoardEntity?> = boardDao.observeBoard(id)

    fun observeCategories(boardId: Long): Flow<List<CategoryEntity>> =
        categoryDao.observeCategories(boardId)

    fun observeCards(categoryId: Long): Flow<List<CardEntity>> =
        cardDao.observeCards(categoryId)

    fun observeAllCards(): Flow<List<CardEntity>> = cardDao.observeAllCards()

    fun observeGridProfiles(): Flow<List<GridProfileEntity>> = gridProfileDao.observeAll()

    fun observeWordUsage(): Flow<List<WordUsageEntity>> = wordUsageDao.observeAll()

    /** Records one use of each spoken word for [language], for word prediction ranking. */
    suspend fun recordWords(words: List<String>, language: String) {
        val now = System.currentTimeMillis()
        words.map { it.lowercase().trim() }
            .filter { it.isNotEmpty() }
            .forEach { word -> wordUsageDao.record(word, language, now) }
    }

    suspend fun insertCard(card: CardEntity): Long = cardDao.insert(card)

    suspend fun updateCard(card: CardEntity) = cardDao.update(card)

    suspend fun deleteCard(card: CardEntity) = cardDao.delete(card)

    suspend fun getBoard(): BoardEntity? = boardDao.getFirst()

    suspend fun getAllGridProfiles(): List<GridProfileEntity> = gridProfileDao.getAll()

    suspend fun getCategories(boardId: Long): List<CategoryEntity> =
        categoryDao.getCategories(boardId)

    suspend fun getCards(categoryId: Long): List<CardEntity> =
        cardDao.getCards(categoryId)

    /**
     * Clears all tables and restores the given content. Callers must wrap this
     * in a Room transaction so it is atomic.
     */
    suspend fun replaceAll(
        board: BoardEntity,
        categories: List<CategoryEntity>,
        cards: List<CardEntity>,
        gridProfiles: List<GridProfileEntity>,
    ) {
        cardDao.deleteAll()
        categoryDao.deleteAll()
        gridProfileDao.deleteAll()
        boardDao.deleteAll()
        boardDao.insert(board)
        categories.forEach { categoryDao.insert(it) }
        cards.forEach { cardDao.insert(it) }
        gridProfiles.forEach { gridProfileDao.insert(it) }
    }


    suspend fun updateCardOrder(cards: List<CardEntity>) =
        cardDao.updateOrder(cards.mapIndexed { index, card -> card.id to index })

    suspend fun updateCategoryOrder(categories: List<CategoryEntity>) =
        categoryDao.updateOrder(categories.mapIndexed { index, category -> category.id to index })

    suspend fun insertCategory(
        boardId: Long,
        nameEn: String,
        nameEs: String,
        color: Long,
        symbolType: ImageType = ImageType.EMOJI,
        symbolValue: String = "",
        orderIndex: Int,
    ): Long = categoryDao.insert(
        CategoryEntity(
            boardId = boardId,
            nameEn = nameEn,
            nameEs = nameEs,
            color = color,
            orderIndex = orderIndex,
            isPreset = false,
            symbolType = symbolType,
            symbolValue = symbolValue,
        ),
    )

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)

    /** Deletes [category], clears any card shortcuts to it, and returns its cards so callers can clean up photos. */
    suspend fun deleteCategory(category: CategoryEntity): List<CardEntity> {
        if (category.isPreset) return emptyList()
        val cards = cardDao.getCards(category.id)
        cardDao.clearShortcutsTo(category.id)
        categoryDao.delete(category)
        return cards
    }

    /** Seeds the default board and grid profiles if the database is empty. */
    suspend fun ensureSeeded() {
        if (boardDao.count() > 0) {
            seedGridProfiles()
            return
        }
        val boardId = boardDao.insert(
            BoardEntity(
                nameEn = DefaultSeed.boardNameEn,
                nameEs = DefaultSeed.boardNameEs,
                orderIndex = 0,
            ),
        )
        DefaultSeed.categories.forEachIndexed { categoryIndex, category ->
            val categoryId = categoryDao.insert(
                CategoryEntity(
                    boardId = boardId,
                    nameEn = category.nameEn,
                    nameEs = category.nameEs,
                    color = category.color,
                    orderIndex = categoryIndex,
                    isPreset = true,
                    symbolType = category.symbolType,
                    symbolValue = category.symbolValue,
                ),
            )
            category.cards.forEachIndexed { cardIndex, card ->
                cardDao.insert(
                    CardEntity(
                        categoryId = categoryId,
                        labelEn = card.labelEn,
                        labelEs = card.labelEs,
                        phraseEn = card.phraseEn,
                        phraseEs = card.phraseEs,
                        imageType = card.imageType,
                        imageValue = card.imageValue,
                        color = null,
                        orderIndex = cardIndex,
                    ),
                )
            }
        }
        seedGridProfiles()
    }

    private suspend fun seedGridProfiles() {
        if (gridProfileDao.count() > 0) return
        gridProfileDao.insert(
            GridProfileEntity(id = BIG_PROFILE_ID, name = "Big", columns = 3, isPreset = true),
        )
        gridProfileDao.insert(
            GridProfileEntity(id = SMALL_PROFILE_ID, name = "Small", columns = 6, isPreset = true),
        )
    }

    suspend fun insertGridProfile(name: String, columns: Int): Long =
        gridProfileDao.insert(
            GridProfileEntity(name = name, columns = columns, isPreset = false),
        )

    suspend fun updateGridProfile(id: Long, name: String, columns: Int) {
        val existing = gridProfileDao.getById(id) ?: return
        if (existing.isPreset) return
        gridProfileDao.update(existing.copy(name = name, columns = columns))
    }

    suspend fun deleteGridProfile(id: Long) {
        val existing = gridProfileDao.getById(id) ?: return
        if (existing.isPreset) return
        gridProfileDao.delete(existing)
    }

    companion object {
        const val BIG_PROFILE_ID = 1L
        const val SMALL_PROFILE_ID = 2L
        const val DEFAULT_CUSTOM_NAME = "Custom"
        const val MIN_COLUMNS = 1
        const val MAX_COLUMNS = 10
    }
}
