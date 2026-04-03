package com.icinema.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// 亮黑色主题配色 - 柔和蓝色强调色
private val LightBlackPrimary = Color(0xFF64B5F6)
private val LightBlackOnPrimary = Color(0xFF000000)
private val LightBlackPrimaryContainer = Color(0xFF1E3A5F)
private val LightBlackOnPrimaryContainer = Color(0xFFD1E4FF)

private val LightBlackSecondary = Color(0xFFBB86FC)
private val LightBlackOnSecondary = Color(0xFF000000)
private val LightBlackSecondaryContainer = Color(0xFF4A3000)
private val LightBlackOnSecondaryContainer = Color(0xFFFFDDB3)

private val LightBlackTertiary = Color(0xFF03DAC6)
private val LightBlackOnTertiary = Color(0xFF000000)
private val LightBlackTertiaryContainer = Color(0xFF004D40)
private val LightBlackOnTertiaryContainer = Color(0xFFA7F3EC)

private val LightBlackError = Color(0xFFCF6679)
private val LightBlackOnError = Color(0xFF000000)
private val LightBlackErrorContainer = Color(0xFF93000A)
private val LightBlackOnErrorContainer = Color(0xFFFFDAD6)

private val LightBlackBackground = Color(0xFF121212)
private val LightBlackOnBackground = Color(0xFFE1E1E1)
private val LightBlackSurface = Color(0xFF1E1E1E)
private val LightBlackOnSurface = Color(0xFFE1E1E1)
private val LightBlackSurfaceVariant = Color(0xFF2D2D2D)
private val LightBlackOnSurfaceVariant = Color(0xFFCACACA)
private val LightBlackOutline = Color(0xFF424242)
private val LightBlackOutlineVariant = Color(0xFF424242)

private val LightBlackScrim = Color(0xFF000000)

private val LightBlackInverseSurface = Color(0xFFE1E1E1)
private val LightBlackInverseOnSurface = Color(0xFF1E1E1E)
private val LightBlackInversePrimary = Color(0xFF003258)

val LightBlackColorScheme = darkColorScheme(
    primary = LightBlackPrimary,
    onPrimary = LightBlackOnPrimary,
    primaryContainer = LightBlackPrimaryContainer,
    onPrimaryContainer = LightBlackOnPrimaryContainer,
    secondary = LightBlackSecondary,
    onSecondary = LightBlackOnSecondary,
    secondaryContainer = LightBlackSecondaryContainer,
    onSecondaryContainer = LightBlackOnSecondaryContainer,
    tertiary = LightBlackTertiary,
    onTertiary = LightBlackOnTertiary,
    tertiaryContainer = LightBlackTertiaryContainer,
    onTertiaryContainer = LightBlackOnTertiaryContainer,
    error = LightBlackError,
    onError = LightBlackOnError,
    errorContainer = LightBlackErrorContainer,
    onErrorContainer = LightBlackOnErrorContainer,
    background = LightBlackBackground,
    onBackground = LightBlackOnBackground,
    surface = LightBlackSurface,
    onSurface = LightBlackOnSurface,
    surfaceVariant = LightBlackSurfaceVariant,
    onSurfaceVariant = LightBlackOnSurfaceVariant,
    outline = LightBlackOutline,
    outlineVariant = LightBlackOutlineVariant,
    scrim = LightBlackScrim,
    inverseSurface = LightBlackInverseSurface,
    inverseOnSurface = LightBlackInverseOnSurface,
    inversePrimary = LightBlackInversePrimary
)

val ICinemaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
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
fun iCinemaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = LightBlackColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
                        (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR).inv()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ICinemaTypography,
        content = content
    )
}
