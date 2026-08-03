package com.unmute.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun Stepper(
    value: Int,
    range: IntRange,
    decreaseLabel: String,
    increaseLabel: String,
    onChanged: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onChanged(value - 1) },
            enabled = value > range.first,
        ) {
            Icon(Icons.Default.Remove, contentDescription = decreaseLabel)
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp),
        )
        IconButton(
            onClick = { onChanged(value + 1) },
            enabled = value < range.last,
        ) {
            Icon(Icons.Default.Add, contentDescription = increaseLabel)
        }
    }
}
