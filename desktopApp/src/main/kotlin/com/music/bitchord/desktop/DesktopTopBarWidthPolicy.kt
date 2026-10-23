package com.music.bitchord.desktop

/**
 * Widths refer to the actual content space beside the sidebar, not window width.
 * Even the narrowest tier keeps a play/pause button and a now-playing track card.
 */
internal object DesktopTopBarWidthPolicy {
    private const val FULL_CONTROLS_DP = 720f
    private const val COMPACT_SKIP_DP = 430f
    private const val COMPACT_PANELS_DP = 350f

    fun showFullControls(availableWidthDp: Float): Boolean =
        availableWidthDp >= FULL_CONTROLS_DP

    fun showCompactSkipControls(availableWidthDp: Float): Boolean =
        availableWidthDp >= COMPACT_SKIP_DP

    fun showCompactSidePanels(availableWidthDp: Float): Boolean =
        availableWidthDp >= COMPACT_PANELS_DP
}
