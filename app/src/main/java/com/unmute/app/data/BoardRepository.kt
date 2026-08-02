package com.unmute.app.data

import com.unmute.app.data.local.BoardEntity
import com.unmute.app.data.local.BoardDao
import com.unmute.app.data.local.CardDao
import com.unmute.app.data.local.CardEntity
import com.unmute.app.data.local.CategoryDao
import com.unmute.app.data.local.CategoryEntity
import com.unmute.app.data.local.GridProfileDao
import com.unmute.app.data.local.GridProfileEntity
import kotlinx.coroutines.flow.Flow

class BoardRepository(
    private val boardDao: BoardDao,
    private val categoryDao: CategoryDao,
    private val cardDao: CardDao,
    private val gridProfileDao: GridProfileDao,
) {
    fun observeBoards(): Flow<List<BoardEntity>> = boardDao.observeBoards()

    fun observeBoard(id: Long): Flow<BoardEntity?> = boardDao.observeBoard(id)

    fun observeCategories(boardId: Long): Flow<List<CategoryEntity>> =
        categoryDao.observeCategories(boardId)

    fun observeCards(categoryId: Long): Flow<List<CardEntity>> =
        cardDao.observeCards(categoryId)

    fun observeGridProfiles(): Flow<List<GridProfileEntity>> = gridProfileDao.observeAll()

    suspend fun insertCard(card: CardEntity): Long = cardDao.insert(card)

    suspend fun updateCard(card: CardEntity) = cardDao.update(card)

    suspend fun deleteCard(card: CardEntity) = cardDao.delete(card)

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
    }
}
