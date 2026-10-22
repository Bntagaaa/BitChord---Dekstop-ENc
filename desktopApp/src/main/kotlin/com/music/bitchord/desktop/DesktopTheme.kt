package com.music.bitchord.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Dark stays the default for existing installations; the choice survives a restart. */
internal enum class DesktopThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light");

    companion object {
        fun fromStored(value: String): DesktopThemeMode =
            entries.firstOrNull { it.name == value } ?: DARK
    }
}

/**
 * One persisted theme selection, with snapshot state so reads of palette tokens inside Compose
 * automatically invalidate on a live change. No window recreation or account-state reset.
 */
internal object DesktopTheme {
    private const val KEY = "desktop_theme_mode"

    var mode: DesktopThemeMode by mutableStateOf(
        DesktopThemeMode.fromStored(DesktopPersistence().string(KEY, DesktopThemeMode.DARK.name)),
    )
        private set

    val palette: DesktopThemePalette
        get() = if (mode == DesktopThemeMode.LIGHT) LIGHT_PALETTE else DARK_PALETTE

    fun setMode(value: DesktopThemeMode) {
        if (mode == value) return
        DesktopPersistence().saveString(KEY, value.name)
        mode = value
    }
}

/** Semantic desktop tokens; artwork overlays may deliberately keep white-on-black treatment. */
internal data class DesktopThemePalette(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val glass: Color,
    val glassStrong: Color,
    val barGlass: Color,
    val barEdge: Color,
    val accent: Color,
    val onAccent: Color,
    val foreground: Color,
    val secondary: Color,
    val divider: Color,
    val destructive: Color,
    val cardFill: Color,
    val cardInsetFill: Color,
    val cardEdge: Color,
    val rowHover: Color,
    val scrim: Color,
    val searchSurface: Color,
    val topPlayerSurface: Color,
    val floatingPlayerSurface: Color,
    val subtleFill: Color,
    val stripedRow: Color,
    val navAccent: Color,
)

internal val DARK_PALETTE = DesktopThemePalette(
    background = Color(0xFF202020),
    surface = Color(0xFF0D0D0F),
    surfaceRaised = Color(0xFF1C1C1E),
    glass = Color(0x661C1C1E),
    glassStrong = Color(0xCC0D0D0F),
    barGlass = Color(0xB324242A),
    barEdge = Color(0x1AFFFFFF),
    accent = Color.White,
    onAccent = Color.Black,
    foreground = Color.White,
    secondary = Color(0xFF8E8E93),
    divider = Color(0xFF2C2C2E),
    destructive = Color(0xFFFF453A),
    cardFill = Color(0xFA1A1A1D),
    cardInsetFill = Color(0x14FFFFFF),
    cardEdge = Color(0x1FFFFFFF),
    rowHover = Color(0x1AFFFFFF),
    scrim = Color(0x8C000000),
    searchSurface = Color(0xFF27272B),
    topPlayerSurface = Color(0xFF323236),
    floatingPlayerSurface = Color(0xFF202124),
    subtleFill = Color(0x14FFFFFF),
    stripedRow = Color(0x0AFFFFFF),
    navAccent = Color(0xFFFA586A),
)

/**
 * Neutral, slightly warm daylight surfaces and charcoal controls, with BitChord's rose navigation
 * identity retained. Strong text/icon contrast also works when blur is disabled.
 */
internal val LIGHT_PALETTE = DesktopThemePalette(
    background = Color(0xFFF7F7F9),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF0F0F3),
    glass = Color(0xB3FFFFFF),
    glassStrong = Color(0xF2FFFFFF),
    barGlass = Color(0xE6FFFFFF),
    barEdge = Color(0x2418191E),
    accent = Color(0xFF1C1D21),
    onAccent = Color.White,
    foreground = Color(0xFF1B1C20),
    secondary = Color(0xFF636670),
    divider = Color(0xFFDADBE1),
    destructive = Color(0xFFBE3545),
    cardFill = Color(0xFCFFFFFF),
    cardInsetFill = Color(0x0C18191E),
    cardEdge = Color(0x2418191E),
    rowHover = Color(0x0F18191E),
    scrim = Color(0x750D0E13),
    searchSurface = Color(0xFFF0F1F4),
    topPlayerSurface = Color(0xFFECEEF2),
    floatingPlayerSurface = Color(0xFFF8F8FA),
    subtleFill = Color(0x1018191E),
    stripedRow = Color(0x0918191E),
    navAccent = Color(0xFFD43D59),
)
