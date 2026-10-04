package com.hag.mypersonalcrossword.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Designed light scheme — predictable, studio-grade palette.
// User-chosen button/cell colors override these where applicable; everywhere
// else (dialogs, top bars, profile cards, etc.) inherits this palette.
private val DesignedLightScheme = lightColorScheme(
    primary              = BrandPrimary,
    onPrimary            = BrandOnPrimary,
    primaryContainer     = BrandPrimaryContainer,
    onPrimaryContainer   = BrandOnPrimaryContainer,
    secondary            = BrandSecondary,
    onSecondary          = BrandOnSecondary,
    secondaryContainer   = BrandSecondaryContainer,
    onSecondaryContainer = BrandOnSecondaryContainer,
    tertiary             = BrandTertiary,
    onTertiary           = BrandOnTertiary,
    tertiaryContainer    = BrandTertiaryContainer,
    onTertiaryContainer  = BrandOnTertiaryContainer,
    background           = BrandBackground,
    onBackground         = BrandOnBackground,
    surface              = BrandSurface,
    onSurface            = BrandOnSurface,
    surfaceVariant       = BrandSurfaceVariant,
    onSurfaceVariant     = BrandOnSurfaceVariant,
    error                = BrandError,
    onError              = BrandOnError
)

private val DesignedDarkScheme = darkColorScheme(
    primary              = DarkPrimary,
    onPrimary            = DarkOnPrimary,
    primaryContainer     = DarkPrimaryContainer,
    onPrimaryContainer   = DarkOnPrimaryContainer,
    secondary            = DarkSecondary,
    onSecondary          = DarkOnSecondary,
    secondaryContainer   = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary             = DarkTertiary,
    onTertiary           = DarkOnTertiary,
    tertiaryContainer    = DarkTertiaryContainer,
    onTertiaryContainer  = DarkOnTertiaryContainer,
    background           = DarkBackground,
    onBackground         = DarkOnBackground,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = DarkOnSurfaceVariant,
    error                = DarkError,
    onError              = DarkOnError
)

@Composable
fun MyPersonalCrosswordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DesignedDarkScheme else DesignedLightScheme,
        typography  = Typography,
        content     = content
    )
}
