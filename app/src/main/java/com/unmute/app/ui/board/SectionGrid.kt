package com.unmute.app.ui.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unmute.app.R
import com.unmute.app.data.local.CategoryEntity
import com.unmute.app.domain.model.label

@Composable
fun SectionGrid(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    language: String,
    editable: Boolean,
    onSelect: (Long) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onEditCategory: (CategoryEntity) -> Unit,
    onAddSection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(categories, key = { it.id }) { category ->
            val selected = category.id == selectedCategoryId
            Box {
                SectionGridItem(
                    category = category,
                    selected = selected,
                    language = language,
                    onClick = { onSelect(category.id) },
                )
                if (editable) {
                    Surface(
                        onClick = { onEditCategory(category) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(28.dp),
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_section),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                    if (!category.isPreset) {
                        Surface(
                            onClick = { onDeleteCategory(category) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                }
            }
        }
        if (editable) {
            item(key = "add_section") {
                AddSectionGridItem(onClick = onAddSection)
            }
        }
    }
}

@Composable
private fun SectionGridItem(
    category: CategoryEntity,
    selected: Boolean,
    language: String,
    onClick: () -> Unit,
) {
    val color = Color(category.color)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) color else color.copy(alpha = 0.15f),
        contentColor = if (selected) Color.White else color,
        border = if (selected) null else BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier.aspectRatio(1f),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp),
        ) {
            SectionImage(
                category = category,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = category.label(language),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun AddSectionGridItem(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.aspectRatio(1f),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(40.dp))
            Text(
                text = stringResource(R.string.add_section),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
