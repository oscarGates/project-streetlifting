package com.streetlifting.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Brand = Color(0xFF305496)
private val BrandLight = Color(0xFF5B7FBE)
private val Amrap = Color(0xFFFCE4D6)

private val LightColors = lightColorScheme(
    primary = Brand,
    secondary = BrandLight,
    tertiary = Color(0xFFB45309),
)

private val DarkColors = darkColorScheme(
    primary = BrandLight,
    secondary = Brand,
    tertiary = Color(0xFFF59E0B),
)

/** Color de resaltado para celdas AMRAP (igual que el PDF). */
val AmrapColor = Amrap

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
