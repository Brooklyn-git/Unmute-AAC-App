package com.unmute.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.unmute.app.domain.model.ImageType

@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameEn: String,
    val nameEs: String,
    val orderIndex: Int,
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = BoardEntity::class,
            parentColumns = ["id"],
            childColumns = ["boardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("boardId")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val boardId: Long,
    val nameEn: String,
    val nameEs: String,
    val color: Long,
    val orderIndex: Int,
    val isPreset: Boolean = false,
    val symbolType: ImageType = ImageType.EMOJI,
    val symbolValue: String = "",
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId")],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val labelEn: String,
    val labelEs: String,
    val phraseEn: String,
    val phraseEs: String,
    val imageType: ImageType,
    val imageValue: String,
    val color: Long?,
    val orderIndex: Int,
)

@Entity(tableName = "grid_profiles")
data class GridProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val columns: Int,
    val isPreset: Boolean,
)
