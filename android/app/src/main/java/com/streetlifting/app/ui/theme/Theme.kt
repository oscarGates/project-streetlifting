package com.streetlifting.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.streetlifting.app.R

// ---- Colores: naranja "gym" sobre fondos cálidos ----

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF7A1A),
    onPrimary = Color(0xFF1F0D00),
    primaryContainer = Color(0xFF5C2A00),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFE6BEA5),
    onSecondary = Color(0xFF422B1A),
    secondaryContainer = Color(0xFF3A2A20),
    onSecondaryContainer = Color(0xFFFFDCC5),
    tertiary = Color(0xFFFFC24B),
    onTertiary = Color(0xFF3F2E00),
    background = Color(0xFF110E0C),
    onBackground = Color(0xFFEDE0D8),
    surface = Color(0xFF110E0C),
    onSurface = Color(0xFFEDE0D8),
    surfaceVariant = Color(0xFF2B231E),
    onSurfaceVariant = Color(0xFFD7C2B6),
    surfaceContainerLowest = Color(0xFF0C0908),
    surfaceContainerLow = Color(0xFF1A1512),
    surfaceContainer = Color(0xFF1E1916),
    surfaceContainerHigh = Color(0xFF282220),
    surfaceContainerHighest = Color(0xFF332B27),
    outline = Color(0xFFA08D82),
    outlineVariant = Color(0xFF52443B),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFB84D00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF331200),
    secondary = Color(0xFF765848),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCC5),
    onSecondaryContainer = Color(0xFF2B1709),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A15),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A15),
    surfaceVariant = Color(0xFFF4DED3),
    onSurfaceVariant = Color(0xFF52443B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EA),
    surfaceContainer = Color(0xFFFCEAE1),
    surfaceContainerHigh = Color(0xFFF6E4DB),
    surfaceContainerHighest = Color(0xFFF0DFD6),
    outline = Color(0xFF85736A),
    outlineVariant = Color(0xFFD7C2B6),
)

// ---- Tipografía: Bebas Neue para títulos y números, Inter para texto ----

private val Bebas = FontFamily(Font(R.font.bebas_neue))

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Inter = FontFamily(inter(400), inter(500), inter(600), inter(700))

private fun display(size: Int) =
    TextStyle(fontFamily = Bebas, fontSize = size.sp, lineHeight = (size * 1.1).sp, letterSpacing = 0.5.sp)

private val base = Typography()

private val AppTypography = Typography(
    displayLarge = display(57),
    displayMedium = display(45),
    displaySmall = display(36),
    headlineLarge = display(34),
    headlineMedium = display(30),
    headlineSmall = display(26),
    titleLarge = display(28),
    titleMedium = display(22),
    titleSmall = display(18),
    bodyLarge = base.bodyLarge.copy(fontFamily = Inter),
    bodyMedium = base.bodyMedium.copy(fontFamily = Inter),
    bodySmall = base.bodySmall.copy(fontFamily = Inter),
    labelLarge = base.labelLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
)

/** Tema de la app: oscuro por defecto (se lee mejor en el gimnasio). */
@Composable
fun AppTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
