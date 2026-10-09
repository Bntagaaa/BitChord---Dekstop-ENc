package com.music.bitchord.desktop

import androidx.compose.runtime.DisposableEffect
import com.music.bitchord.ui.player.PlayerPlatform
import com.music.bitchord.data.DebugLog
import com.music.bitchord.data.TrackLog
import com.music.bitchord.data.innertube.InnerTubeXResolver
import com.music.bitchord.data.innertube.StreamResolver
import com.music.bitchord.data.innertube.potoken.PoTokenGenerator
import com.music.bitchord.data.lyrics.LyricsTranslation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import bitchord.desktopapp.generated.resources.Res
import bitchord.desktopapp.generated.resources.logo
import java.awt.Dimension
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

fun main() {
    // The player is the phone's, from the shared UI module; this is what it reads underneath.
    PlayerPlatform.install(DesktopPlayerHost)
    com.music.bitchord.ui.AppUi.install(DesktopAppUiHost)
    // The data layer both apps share logs through here, and keeps translated
    // lyrics beside the rest of the desktop's cache.
    DebugLog.sink = DebugLog.Sink { level, tag, message, error ->
        DesktopTrackLog.log("$tag/$level: $message" + (error?.let { " (${it.message})" } ?: ""))
    }
    LyricsTranslation.cacheDir = DesktopMediaCache.directory.toFile()
    // YouTube playback is the phone's StreamResolver over InnerTubeX, with BotGuard PoTokens
    // minted in JavaFX's WebView where the phone uses Android's.
    TrackLog.echo = { level, tag, message, error ->
        DesktopTrackLog.log("$tag/$level: $message" + (error?.let { " (${it.message})" } ?: ""))
    }
    StreamResolver.maxKbps = {
        DesktopStreamClient.ceiling.get() ?: DesktopSourceRegistry.ceiling(null).maxKbps
    }
    InnerTubeXResolver.init(
        filesDir = DesktopMediaCache.directory.toFile(),
        store = DesktopInnerTubeXStore,
        poTokenProvider = PoTokenGenerator(
            createMinter = { DesktopPoTokenWebView.getNewPoTokenGenerator() },
            available = { DesktopPoTokenWebView.available },
        ).asTokenProvider(),
    )
    // The public "apps open right now" count; see Presence.
    DesktopPresence.install()
    desktopMain()
}

private fun desktopMain() = application {
    // Closing puts the window away rather than ending the process, while there is a tray icon to
    // bring it back from — see [DesktopWindowVisibility].
    val visible by DesktopWindowVisibility.visible.collectAsState()
    val raiseRequest by DesktopWindowVisibility.raiseRequest.collectAsState()
    // Hoisted so the player can fill the screen and system caption actions stay synchronized.
    val placement by DesktopWindowMode.placement.collectAsState()
    val state = rememberWindowState(width = 1_220.dp, height = 780.dp)
    LaunchedEffect(placement) { state.placement = placement }
    // ...and back, for the times the window is moved between placements by something that is not
    // us; see [DesktopWindowMode.adopt].
    LaunchedEffect(state) {
        snapshotFlow { state.placement }.collect(DesktopWindowMode::adopt)
    }
    Window(
        onCloseRequest = { if (DesktopWindowVisibility.onCloseRequest()) exitApplication() },
        visible = visible,
        title = "BitChord",
        icon = painterResource(Res.drawable.logo),
        state = state,
        // The peer must be opaque and decorated from creation. Windows owns the entire
        // non-client frame: caption, drag, resize, system menu and Snap.
        undecorated = false,
        transparent = false,
        resizable = true,
    ) {
        val composeWindow = window
        DisposableEffect(composeWindow) {
            onDispose { DesktopWindowsCaptionAppearance.detach(composeWindow) }
        }
        LaunchedEffect(raiseRequest) {
            if (raiseRequest == 0L) return@LaunchedEffect

            // MPRIS Raise is also what desktop-shell media widgets use for "open player".
            // Restore a minimized/hidden BitChord first, then defer the foreground request by one
            // AWT turn so the compositor sees a mapped window before toFront/requestFocus.
            state.isMinimized = false
            composeWindow.isVisible = true
            java.awt.EventQueue.invokeLater {
                composeWindow.toFront()
                composeWindow.requestFocus()
            }
        }
        val openingSize = remember { state.size }
        LaunchedEffect(composeWindow) {
            // The client stays opaque even when DWM accepts a caption material.
            composeWindow.background = java.awt.Color.BLACK
            composeWindow.contentPane.background = java.awt.Color.BLACK
            if (DesktopPlatform.isWindows) {
                // AWT dimensions use logical screen coordinates; its peer applies monitor DPI.
                // Leave the opening size to rememberWindowState, including native frame insets.
                composeWindow.minimumSize = Dimension(MIN_WINDOW_WIDTH_DP, MIN_WINDOW_HEIGHT_DP)
                // Styling may need the HWND created a frame after composition. Its failure never
                // changes or recreates the decorated window.
                repeat(10) {
                    if (DesktopWindowsCaptionAppearance.attach(composeWindow)) {
                        DesktopWindowBackdrop.apply()
                        return@LaunchedEffect
                    }
                    delay(50)
                }
                DesktopWindowBackdrop.apply()
            } else {
                // Preserve the established Linux sizing path, including its startup correction.
                val transform = composeWindow.graphicsConfiguration.defaultTransform
                composeWindow.minimumSize = Dimension(
                    (MIN_WINDOW_WIDTH_DP * transform.scaleX).roundToInt(),
                    (MIN_WINDOW_HEIGHT_DP * transform.scaleY).roundToInt(),
                )
                composeWindow.size = Dimension(
                    (openingSize.width.value * transform.scaleX).roundToInt(),
                    (openingSize.height.value * transform.scaleY).roundToInt(),
                )
            }
        }
        DesktopFlyoutHost {
            BitChordDesktopApp()
        }
    }
}

private const val MIN_WINDOW_WIDTH_DP = 900
private const val MIN_WINDOW_HEIGHT_DP = 600
