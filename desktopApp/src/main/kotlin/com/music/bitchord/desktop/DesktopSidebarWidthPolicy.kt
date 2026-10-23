package com.music.bitchord.desktop

/**
 * Keeps a desktop navigation rail present even at widths where the old UI switched to mobile tabs.
 *
 * The user's saved collapse choice applies only when the window has space for a full sidebar.
 * Automatic collapse never overwrites that preference; opening the narrow-window drawer is
 * temporary and cannot shift the content column.
 */
internal object DesktopSidebarWidthPolicy {
    const val AUTOMATIC_RAIL_BREAKPOINT_DP = 980f

    fun shouldUseRail(windowWidthDp: Float): Boolean =
        windowWidthDp < AUTOMATIC_RAIL_BREAKPOINT_DP

    fun isInlineCollapsed(windowWidthDp: Float, userCollapsed: Boolean): Boolean =
        shouldUseRail(windowWidthDp) || userCollapsed

    fun isDrawerVisible(windowWidthDp: Float, drawerRequested: Boolean): Boolean =
        shouldUseRail(windowWidthDp) && drawerRequested
}
