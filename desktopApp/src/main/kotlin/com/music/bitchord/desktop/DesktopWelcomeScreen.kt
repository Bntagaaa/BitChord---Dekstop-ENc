package com.music.bitchord.desktop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bitchord.desktopapp.generated.resources.Res
import bitchord.desktopapp.generated.resources.bitchord_logo_original
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Image

/**
 * A single uninterrupted logo motion, followed by gently staggered text and actions.
 * Uses the same dialog surface, theme, logo and Material buttons as the desktop app.
 */
@Composable
internal fun DesktopWelcomeScreen(
    youtubeConnected: Boolean,
    spotifyConnected: Boolean,
    busy: String?,
    error: String?,
    onYouTube: () -> Unit,
    onSpotify: () -> Unit,
    onFinish: () -> Unit,
) {
    val reduceMotion by DesktopAppearanceSettings.reduceAnimation.collectAsState()
    val motion = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val title = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val subtitle = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val youtube = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val spotify = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val finish = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val ease = remember { CubicBezierEasing(0.22f, 1f, 0.36f, 1f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            listOf(motion, title, subtitle, youtube, spotify, finish).forEach { it.snapTo(1f) }
        } else {
            delay(1200) // let the mark stand alone before the reveal
            launch { motion.animateTo(1f, tween(1550, easing = ease)) }
            delay(1220)
            launch { title.animateTo(1f, tween(740, easing = ease)) }
            delay(170)
            launch { subtitle.animateTo(1f, tween(740, easing = ease)) }
            delay(160)
            launch { youtube.animateTo(1f, tween(740, easing = ease)) }
            delay(130)
            launch { spotify.animateTo(1f, tween(740, easing = ease)) }
            delay(130)
            launch { finish.animateTo(1f, tween(740, easing = ease)) }
        }
    }

    // This overlay is deliberately not dismissible by backdrop or Escape: completion is explicit.
    DesktopDialogPanel(onDismiss = {}, maxWidth = 460) {
        Box(Modifier.fillMaxWidth().heightIn(min = 445.dp), contentAlignment = Alignment.TopCenter) {
            // Only the logo is visible at first. One graphics-layer transform moves it up and
            // scales it down simultaneously; no intermediate jump or second logo animation.
            Image(
                painter = painterResource(Res.drawable.bitchord_logo_original),
                contentDescription = "BitChord",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 22.dp)
                    .size(width = 130.dp, height = 95.dp)
                    .graphicsLayer {
                        val t = motion.value
                        val scale = 1.0f - t * 0.57f
                        scaleX = scale
                        scaleY = scale
                        translationY = (1f - t) * 155.dp.toPx()
                    },
            )
            Column(
                Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, top = 113.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Welcome to BitChord",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(title.value).graphicsLayer { translationY = (1f - title.value) * 17.dp.toPx() },
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    "Your music, your way. Let's get you connected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DesktopSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(subtitle.value).graphicsLayer { translationY = (1f - subtitle.value) * 14.dp.toPx() },
                )
                Spacer(Modifier.height(33.dp))
                WelcomeAction(
                    text = when {
                        youtubeConnected -> "✓  YouTube Music Connected"
                        busy == "youtube" -> "Connecting YouTube Music…"
                        else -> "Connect YouTube Music"
                    },
                    connected = youtubeConnected,
                    enabled = busy == null && !youtubeConnected,
                    progress = youtube.value,
                    onClick = onYouTube,
                )
                Spacer(Modifier.height(12.dp))
                WelcomeAction(
                    text = when {
                        spotifyConnected -> "✓  Spotify Connected"
                        busy == "spotify" -> "Connecting Spotify…"
                        else -> "Connect Spotify"
                    },
                    connected = spotifyConnected,
                    enabled = busy == null && !spotifyConnected,
                    progress = spotify.value,
                    onClick = onSpotify,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().alpha(finish.value)
                        .graphicsLayer { translationY = (1f - finish.value) * 16.dp.toPx() },
                ) {
                    OutlinedButton(
                        onClick = onFinish,
                        enabled = busy == null,
                        modifier = Modifier.fillMaxWidth().height(49.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (youtubeConnected || spotifyConnected) "Start Listening" else "Skip For Now")
                    }
                }
                Spacer(Modifier.height(13.dp))
                if (busy != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                        Text("Finish signing in in the browser window", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                } else if (error != null) {
                    Text(error, color = DesktopDestructive, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                } else {
                    Text(
                        "You can connect both accounts later in Settings.",
                        color = DesktopSecondary,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(finish.value),
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeAction(
    text: String,
    connected: Boolean,
    enabled: Boolean,
    progress: Float,
    onClick: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().alpha(progress)
            .graphicsLayer { translationY = (1f - progress) * 20.dp.toPx() },
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(49.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (connected) Color(0xFF254A37) else Color.White,
                contentColor = if (connected) Color(0xFFB8F4CF) else Color(0xFF191919),
                disabledContainerColor = if (connected) Color(0xFF254A37) else Color(0xFF6B6B6B),
                disabledContentColor = if (connected) Color(0xFFB8F4CF) else Color.White,
            ),
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
