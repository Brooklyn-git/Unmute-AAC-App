package com.unmute.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun GridProfileDialog(
    title: String,
    initialName: String,
    initialColumns: Int,
    columnRange: IntRange,
    nameLabel: String,
    columnsLabel: String,
    saveLabel: String,
    cancelLabel: String,
    onConfirm: (name: String, columns: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var columns by remember { mutableIntStateOf(initialColumns) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(nameLabel) },
                    singleLine = true,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = columnsLabel, style = MaterialTheme.typography.titleMedium)
                    ColumnStepper(
                        value = columns,
                        range = columnRange,
                        onChanged = { columns = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, columns) }) { Text(saveLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelLabel) }
        },
    )
}

@Composable
private fun ColumnStepper(
    value: Int,
    range: IntRange,
    onChanged: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onChanged(value + 1) },
            enabled = value < range.last,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase columns")
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp),
        )
        IconButton(
            onClick = { onChanged(value - 1) },
            enabled = value > range.first,
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease columns")
        }
    }
}
