package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertTrue

class DesktopSearchGlassTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `glass covers the panel evenly without an inset rectangle`() = panel(1f, false)

    @Test fun `glass stays uniform at 150 percent scale`() = panel(1.5f, false)

    @Test fun `solid mode covers the same panel without sampling its backdrop`() = panel(1f, true)

    private fun panel(scale: Float, reduceBlur: Boolean) {
        compose.setContent {
            val haze = remember { HazeState() }
            CompositionLocalProvider(LocalDensity provides Density(scale)) {
                Box(Modifier.size(400.dp)) {
                    Box(Modifier.fillMaxSize().hazeSource(haze).background(Color.Red))
                    Surface(
                        modifier = Modifier.padding(40.dp).size(300.dp)
                            .desktopSearchGlass(haze, RoundedCornerShape(14.dp), reduceBlur)
                            .testTag("glass"),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        shadowElevation = 0.dp,
                    ) {}
                }
            }
        }
        val pixels = compose.onNodeWithTag("glass").captureToImage().toPixelMap()
        val center = pixels[pixels.width / 2, pixels.height / 2]
        if (reduceBlur) {
            // Solid search must match the *resolved* theme. The old 0x27272B assertion
            // assumed Dark mode even after Follow System became the default.
            val expected = DesktopSearchFill
            assertTrue(abs(center.red - expected.red) < 0.01f, "solid red: $center vs $expected")
            assertTrue(abs(center.green - expected.green) < 0.01f, "solid green: $center vs $expected")
            assertTrue(abs(center.blue - expected.blue) < 0.01f, "solid blue: $center vs $expected")
        } else {
            // A solid replacement would also be uniform; require the red source to contribute.
            assertTrue(center.red > center.green + 0.1f)
        }
        val samples = listOf(
            5 to pixels.height / 2,
            15 to pixels.height / 2,
            pixels.width - 6 to pixels.height / 2,
            pixels.width / 2 to 5,
            pixels.width / 2 to 15,
            pixels.width / 2 to pixels.height - 6,
        )
        for ((x, y) in samples) {
            val edge = pixels[x, y]
            assertTrue(abs(center.red - edge.red) < 0.03f, "inset at ($x, $y): $edge vs $center")
        }
    }
}
