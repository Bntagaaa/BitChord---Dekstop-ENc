package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * macOS-inspired desktop glass materials. Unlike Android's RenderEffect glass these run on
 * Compose Desktop's Haze/Skia rendering path, so they work on Windows and Linux alike.
 *
 * A shared material definition avoids the old mix of flat dialogs, very dark bars and an
 * unrelated search blur. Sampling and the optical rim MUST both stop with Reduce dynamic blur.
 * Existing wallpaper-tinted Mica navigation is intentionally not a dynamic blur surface.
 */
internal enum class DesktopGlassMaterial { CHROME, FLOATING, POPOVER, SEARCH }

internal data class DesktopGlassTuning(
    val blurRadius: Dp,
    val tintAlpha: Float,
    val noiseFactor: Float,
    val rimAlpha: Float,
)

/** Compact, static materials: no animation or per-frame shader work on integrated GPUs. */
internal fun desktopGlassTuning(material: DesktopGlassMaterial, dark: Boolean): DesktopGlassTuning =
    when (material) {
        DesktopGlassMaterial.CHROME ->
            DesktopGlassTuning(30.dp, if (dark) 0.49f else 0.61f, 0.025f, 0f)
        DesktopGlassMaterial.FLOATING ->
            DesktopGlassTuning(38.dp, if (dark) 0.33f else 0.46f, 0.03f, 0.67f)
        DesktopGlassMaterial.POPOVER ->
            DesktopGlassTuning(42.dp, if (dark) 0.40f else 0.51f, 0.025f, 0.72f)
        DesktopGlassMaterial.SEARCH ->
            DesktopGlassTuning(32.dp, if (dark) 0.30f else 0.40f, 0.02f, 0.30f)
    }

internal fun desktopGlassSamplesBackdrop(hasSource: Boolean, reduceBlur: Boolean): Boolean =
    hasSource && !reduceBlur

/**
 * Samples the app backdrop with stronger material separation and a directionally lit rim.
 * The rim gives macOS-like edge lighting without Android-only lens shaders, chromatic
 * aberration, backdrop recapture or animated shader uniforms.
 *
 * [opaque] is deliberately forced fully opaque when blur is reduced or the source is absent,
 * even if callers supply an alpha-tinted legacy bar colour.
 */
@Composable
internal fun Modifier.desktopLiquidGlass(
    haze: HazeState?,
    shape: Shape,
    opaque: Color,
    material: DesktopGlassMaterial,
    reduceBlur: Boolean,
    backdropMask: Brush? = null,
    showRim: Boolean = true,
): Modifier {
    if (!desktopGlassSamplesBackdrop(haze != null, reduceBlur)) {
        val solid = background(opaque.copy(alpha = 1f), shape)
        return if (showRim) solid.border(0.5.dp, DesktopTheme.palette.barEdge, shape) else solid
    }

    val source = checkNotNull(haze)
    val dark = DesktopTheme.isDark
    val tuning = desktopGlassTuning(material, dark)
    val tintColor = when (material) {
        DesktopGlassMaterial.CHROME ->
            if (dark) Color(0xFF1A1B21) else Color(0xFFF0F2F6)
        DesktopGlassMaterial.FLOATING ->
            if (dark) Color(0xFF282A34) else Color(0xFFE9ECF7)
        DesktopGlassMaterial.POPOVER ->
            if (dark) Color(0xFF252630) else Color(0xFFF0F2F9)
        DesktopGlassMaterial.SEARCH ->
            if (dark) Color(0xFF22232B) else Color(0xFFF1F4FA)
    }
    val base = opaque.copy(alpha = 1f)

    val glass = hazeEffect(state = source) {
        blurEnabled = true
        backgroundColor = base
        blurRadius = tuning.blurRadius
        tints = listOf(HazeTint(tintColor.copy(alpha = tuning.tintAlpha)))
        fallbackTint = HazeTint(base)
        noiseFactor = tuning.noiseFactor
        mask = backdropMask
    }
    return if (showRim) glass.desktopGlassOpticalRim(shape, tuning.rimAlpha, dark) else glass
}

/** Static directional specular reflection, without reflecting over the panel's actual content. */
private fun Modifier.desktopGlassOpticalRim(shape: Shape, alpha: Float, dark: Boolean): Modifier {
    val bright = Color.White.copy(alpha = if (dark) alpha else alpha * 0.84f)
    val dim = Color.White.copy(alpha = alpha * 0.19f)
    val shade = Color.Black.copy(alpha = if (dark) 0.10f else 0.14f)
    return border(
        width = 1.dp,
        brush = Brush.linearGradient(
            0f to bright,
            0.28f to dim,
            0.65f to shade,
            1f to Color.White.copy(alpha = alpha * 0.48f),
        ),
        shape = shape,
    ).border(
        width = 0.5.dp,
        color = Color.White.copy(alpha = alpha * if (dark) 0.22f else 0.13f),
        shape = shape,
    )
}
