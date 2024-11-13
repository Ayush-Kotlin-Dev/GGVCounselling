package ayush.ggv.counselling.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Define custom colors
val Blue80 = Color(0xFF90CAF9)
val Blue40 = Color(0xFF2196F3)
val LightBlue80 = Color(0xFFBBDEFB)
val LightBlue40 = Color(0xFF64B5F6)
val DarkBlue80 = Color(0xFF1565C0)
val DarkBlue40 = Color(0xFF0D47A1)

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = White,
    secondary = LightBlue40,
    onSecondary = Black,
    tertiary = DarkBlue40,
    onTertiary = White,
    background = White,
    onBackground = Black,
    surface = Color(0xFFF5F5F5),
    onSurface = Black
)

// Define typography
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun GGVCounsellingTheme(
    content: @Composable () -> Unit
) {

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}