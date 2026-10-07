package com.music.bitchord.desktop

/** Logical layout space for the dedicated native Windows caption, not the player toolbar. */
internal data class DesktopNativeCaptionMetrics(
    val heightDp: Float = 32f,
    val rightInsetDp: Float = 160f,
)

/** DWM reports physical pixels; AWT's per-monitor transform converts them to Compose dp. */
internal fun desktopNativeCaptionMetrics(
    physical: IntArray?,
    scaleX: Double,
    scaleY: Double,
): DesktopNativeCaptionMetrics {
    val fallback = DesktopNativeCaptionMetrics()
    if (physical == null || physical.size < 2 ||
        !scaleX.isFinite() || !scaleY.isFinite() || scaleX <= 0.0 || scaleY <= 0.0
    ) return fallback
    val height = physical[0] / scaleY
    val rightInset = physical[1] / scaleX
    // Hidden/minimized windows may report undefined caption bounds. Never collapse the header.
    if (height !in 24.0..128.0 || rightInset !in 48.0..640.0) return fallback
    return DesktopNativeCaptionMetrics(height.toFloat(), rightInset.toFloat())
}
