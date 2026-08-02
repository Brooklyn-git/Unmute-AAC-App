package com.unmute.app.ui.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val COLOR_SWATCHES = listOf(
    0xFF9E9E9E to Color(0xFF9E9E9E),
    0xFFEF5350 to Color(0xFFEF5350),
    0xFFFF7043 to Color(0xFFFF7043),
    0xFFFDD835 to Color(0xFFFDD835),
    0xFF66BB6A to Color(0xFF66BB6A),
    0xFF42A5F5 to Color(0xFF42A5F5),
    0xFFAB47BC to Color(0xFFAB47BC),
    0xFF8D6E63 to Color(0xFF8D6E63),
)

@Composable
fun ColorSwatch(
    color: Color?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val border = if (isSelected) {
        BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color ?: Color(0xFFE0E0E0),
        border = border,
        modifier = Modifier.size(36.dp),
    ) {}
}
