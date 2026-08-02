package com.unmute.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF1E6FB8)
val PrimaryLightBlue = Color(0xFF9EC8EE)
val AccentGreen = Color(0xFF2E7D32)

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = AccentGreen,
    surface = Color(0xFFFDFBFF),
)

private val DarkColors = darkColorScheme(
    primary = PrimaryLightBlue,
    secondary = Color(0xFFA5D6A7),
)

@Composable
fun UnmuteTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
