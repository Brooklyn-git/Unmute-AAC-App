package com.unmute.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {
    @Query("SELECT * FROM boards ORDER BY orderIndex")
    fun observeBoards(): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards WHERE id = :id")
    fun observeBoard(id: Long): Flow<BoardEntity?>

    @Query("SELECT COUNT(*) FROM boards")
    suspend fun count(): Int

    @Insert
    suspend fun insert(board: BoardEntity): Long

    @Update
    suspend fun update(board: BoardEntity)

    @Delete
    suspend fun delete(board: BoardEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE boardId = :boardId ORDER BY orderIndex")
    fun observeCategories(boardId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE boardId = :boardId ORDER BY orderIndex")
    suspend fun getCategories(boardId: Long): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategory(id: Long): CategoryEntity?

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE categoryId = :categoryId ORDER BY orderIndex")
    fun observeCards(categoryId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE categoryId = :categoryId ORDER BY orderIndex")
    suspend fun getCards(categoryId: Long): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCard(id: Long): CardEntity?

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun count(): Int

    @Insert
    suspend fun insert(card: CardEntity): Long

    @Update
    suspend fun update(card: CardEntity)

    @Delete
    suspend fun delete(card: CardEntity)
}

@Dao
interface GridProfileDao {
    @Query("SELECT * FROM grid_profiles ORDER BY isPreset DESC, id ASC")
    fun observeAll(): Flow<List<GridProfileEntity>>

    @Query("SELECT * FROM grid_profiles WHERE id = :id")
    suspend fun getById(id: Long): GridProfileEntity?

    @Query("SELECT COUNT(*) FROM grid_profiles")
    suspend fun count(): Int

    @Insert
    suspend fun insert(profile: GridProfileEntity): Long

    @Update
    suspend fun update(profile: GridProfileEntity)

    @Delete
    suspend fun delete(profile: GridProfileEntity)
}
