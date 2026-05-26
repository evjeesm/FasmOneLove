package jekas.obps.fasmonelove.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import jekas.obps.fasmonelove.R

// ── Colors ────────────────────────────────────────────────────────────────────

val BackgroundNavy = Color(0xFF1B2B3E)
val SurfaceNavy    = Color(0xFF0F1E2D)
val BorderNavy     = Color(0xFF2A3F55)
val TextWhite      = Color(0xFFE8E8E8)
val TextMuted      = Color(0xFFAAAAAA)
val LinkBlue       = Color(0xFF7EB8D4)
val ErrorRed       = Color(0xFFCF6679)

// ── Color scheme ──────────────────────────────────────────────────────────────

private val FasmColorScheme = darkColorScheme(
    background           = BackgroundNavy,
    surface              = SurfaceNavy,
    primary              = LinkBlue,
    onPrimary            = SurfaceNavy,
    onBackground         = TextWhite,
    onSurface            = TextWhite,
    outline              = BorderNavy,
    error                = ErrorRed,
    onError              = TextWhite,
    secondaryContainer   = BorderNavy,
    onSecondaryContainer = TextWhite,
)

// ── Typography ────────────────────────────────────────────────────────────────

private val FasmTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        color      = TextWhite,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        color      = TextWhite,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        color      = TextMuted,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize   = 20.sp,
        color      = TextWhite,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize   = 16.sp,
        color      = TextWhite,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        color      = LinkBlue,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize   = 11.sp,
        color      = TextMuted,
    ),
)

// ── Mono style — code editor, output panel, file names ───────────────────────

val MonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize   = 13.sp,
    color      = TextWhite,
)

// ── Theme ─────────────────────────────────────────────────────────────────────

@Composable
fun FasmOneLoveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FasmColorScheme,
        typography  = FasmTypography,
        content     = content,
    )
}