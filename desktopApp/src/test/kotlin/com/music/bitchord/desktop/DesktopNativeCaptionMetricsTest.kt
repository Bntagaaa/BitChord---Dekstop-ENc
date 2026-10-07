package com.music.bitchord.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopNativeCaptionMetricsTest {
    @Test fun normalScaleUsesActualBounds() {
        assertEquals(DesktopNativeCaptionMetrics(32f, 138f), desktopNativeCaptionMetrics(intArrayOf(32, 138), 1.0, 1.0))
    }
    @Test fun fractionalDpiIsConvertedBackToLogicalSpace() {
        assertEquals(DesktopNativeCaptionMetrics(32f, 144f), desktopNativeCaptionMetrics(intArrayOf(40, 180), 1.25, 1.25))
    }
    @Test fun doubleDpiDoesNotDoubleReservedLayoutSpace() {
        assertEquals(DesktopNativeCaptionMetrics(32f, 144f), desktopNativeCaptionMetrics(intArrayOf(64, 288), 2.0, 2.0))
    }
    @Test fun monitorsMayHaveDifferentTransforms() {
        assertEquals(DesktopNativeCaptionMetrics(32f, 144f), desktopNativeCaptionMetrics(intArrayOf(48, 288), 2.0, 1.5))
    }
    @Test fun changingMonitorRefreshesMetricsWithoutAccumulatedScale() {
        val physical = intArrayOf(48, 216)
        assertEquals(DesktopNativeCaptionMetrics(32f, 144f), desktopNativeCaptionMetrics(physical, 1.5, 1.5))
        assertEquals(DesktopNativeCaptionMetrics(48f, 216f), desktopNativeCaptionMetrics(physical, 1.0, 1.0))
    }
    @Test fun tallerSystemCaptionIsNotClipped() {
        assertEquals(DesktopNativeCaptionMetrics(44f, 156f), desktopNativeCaptionMetrics(intArrayOf(44, 156), 1.0, 1.0))
    }
    @Test fun unavailableOrTruncatedNativeResultUsesSafeFallback() {
        for (raw in listOf(null, intArrayOf(), intArrayOf(32))) {
            assertEquals(DesktopNativeCaptionMetrics(), desktopNativeCaptionMetrics(raw, 1.0, 1.0))
        }
    }
    @Test fun hiddenOrInvalidBoundsNeverRemoveTheTitleBar() {
        for (raw in listOf(intArrayOf(0, 0), intArrayOf(-1, 180), intArrayOf(32, -1), intArrayOf(10000, 10000))) {
            assertEquals(DesktopNativeCaptionMetrics(), desktopNativeCaptionMetrics(raw, 1.0, 1.0))
        }
    }
    @Test fun invalidDpiNeverProducesNaNOrInfiniteLayout() {
        for (scale in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(DesktopNativeCaptionMetrics(), desktopNativeCaptionMetrics(intArrayOf(32, 144), scale, 1.0))
            assertEquals(DesktopNativeCaptionMetrics(), desktopNativeCaptionMetrics(intArrayOf(32, 144), 1.0, scale))
        }
    }
}
