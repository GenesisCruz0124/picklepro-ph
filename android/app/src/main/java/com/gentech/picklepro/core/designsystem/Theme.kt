package com.gentech.picklepro.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PickleGreen40,
    onPrimary = Neutral99,
    primaryContainer = PickleGreen80,
    onPrimaryContainer = PickleGreen20,
    secondary = PickleBall40,
    onSecondary = Neutral99,
    secondaryContainer = PickleBall80,
    onSecondaryContainer = PickleBall20,
    tertiary = PickleCourtBlue40,
    tertiaryContainer = PickleCourtBlue80,
    error = ErrorRed40,
    errorContainer = ErrorRed80,
    background = Neutral99,
    surface = Neutral99,
    onBackground = Neutral10,
    onSurface = Neutral10,
)

private val DarkColors = darkColorScheme(
    primary = PickleGreen80,
    onPrimary = PickleGreen20,
    primaryContainer = PickleGreen40,
    onPrimaryContainer = PickleGreen80,
    secondary = PickleBall80,
    onSecondary = PickleBall20,
    secondaryContainer = PickleBall40,
    onSecondaryContainer = PickleBall80,
    tertiary = PickleCourtBlue80,
    tertiaryContainer = PickleCourtBlue40,
    error = ErrorRed80,
    errorContainer = ErrorRed40,
    background = Neutral10,
    surface = Neutral10,
    onBackground = Neutral99,
    onSurface = Neutral99,
)

@Composable
fun PickleProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PickleTypography,
        content = content,
    )
}
