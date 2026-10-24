package com.music.bitchord.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.bitchord.ui.components.optimizedHazeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.sharedui.resources.*
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.ui.LyricsProviderState
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint

/**
 * Presentation of the lyrics-provider chooser. Mobile keeps the familiar drawer;
 * desktop uses a compact floating glass dialog.
 */
enum class LyricsProviderPresentation { DRAWER, GLASS_POPUP }

/**
 * Manual provider chooser for the lyrics half-player.
 *
 * It deliberately uses [PlayerDrawer], [ROW_SHAPE], and the same row geometry
 * as [AudioOutputSheet], so it behaves like another player destination rather
 * than a settings dialog. A row that the automatic lookup already completed is
 * entirely local: hits switch immediately and misses cannot be requested twice.
 */
@Composable
fun LyricsProviderSheet(
    hazeState: HazeState,
    currentSource: LyricsSource?,
    states: Map<LyricsSource, LyricsProviderState>,
    onSelect: (LyricsSource) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    presentation: LyricsProviderPresentation = LyricsProviderPresentation.DRAWER,
) {
    val savedOrder by PlayerSettings.lyricsSourceOrder.collectAsStateWithLifecycle()
    val sources = remember(savedOrder) { LyricsSource.ordered(savedOrder) }
    var requestedSource by remember { mutableStateOf<LyricsSource?>(null) }
    LaunchedEffect(currentSource, requestedSource) {
        if (requestedSource != null && currentSource == requestedSource) onDismiss()
    }

    val title = stringResource(Res.string.choose_lyrics_provider)
    val providerOptions: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            sources.forEach { source ->
                LyricsProviderRow(
                    source = source,
                    state = states[source] ?: LyricsProviderState.NOT_FETCHED,
                    current = source == currentSource,
                    onClick = {
                        val wasFound = states[source] == LyricsProviderState.FOUND
                        requestedSource = source
                        onSelect(source)
                        if (wasFound) onDismiss()
                    },
                )
            }
        }
    }

    when (presentation) {
        LyricsProviderPresentation.DRAWER -> PlayerDrawer(
            hazeState = hazeState,
            title = title,
            onDismiss = onDismiss,
            modifier = modifier,
            content = providerOptions,
        )
        LyricsProviderPresentation.GLASS_POPUP -> LyricsProviderPopup(
            hazeState = hazeState,
            title = title,
            onDismiss = onDismiss,
            modifier = modifier,
            content = providerOptions,
        )
    }
}

@Composable
private fun LyricsProviderPopup(
    hazeState: HazeState,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduceDynamicBlur by PlayerSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val shape = RoundedCornerShape(24.dp)
    val interaction = remember { MutableInteractionSource() }
    // Blurring only the dialog card leaves the whole page crisp, which is not the
    // requested Desktop presentation. Blur the captured player/page as a backdrop.
    // The existing Reduce Blur preference removes the Haze effect entirely.
    val backdropSurface = if (reduceDynamicBlur) {
        Modifier.background(Color.Black.copy(alpha = 0.58f))
    } else {
        Modifier.optimizedHazeEffect(state = hazeState) {
            blurEnabled = true
            blurRadius = 44.dp
            backgroundColor = Color(0xFF070707).copy(alpha = 0.08f)
            tints = listOf(HazeTint(Color.Black.copy(alpha = 0.18f)))
            fallbackTint = HazeTint(Color.Black.copy(alpha = 0.55f))
            noiseFactor = 0.01f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(backdropSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(170)) + scaleIn(initialScale = 0.94f, animationSpec = tween(190)),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.97f, animationSpec = tween(120)),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxWidth(0.82f)
                    .heightIn(max = 620.dp)
                    .clip(shape)
                    .then(
                        if (reduceDynamicBlur) {
                            Modifier.background(Color(0xFF171717))
                        } else {
                            Modifier
                                .optimizedHazeEffect(state = hazeState) {
                                    blurEnabled = true
                                    blurRadius = 52.dp
                                    backgroundColor = Color(0xFF090909).copy(alpha = 0.08f)
                                    tints = listOf(HazeTint(Color(0xFF101010).copy(alpha = 0.20f)))
                                    fallbackTint = HazeTint(Color(0xFF191919))
                                    noiseFactor = 0.015f
                                }
                        },
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.11f), shape)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close lyrics provider",
                            tint = Color.White.copy(alpha = 0.78f),
                        )
                    }
                }
                content()
            }
        }
    }
}

@Composable
private fun LyricsProviderRow(
    source: LyricsSource,
    state: LyricsProviderState,
    current: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberHaptics()
    // A running automatic request can still be selected: it is marked as the
    // requested source and applied when that same in-flight call completes.
    val enabled = !current && state != LyricsProviderState.NOT_FOUND
    val status = when {
        current -> stringResource(Res.string.lyrics_provider_current)
        state == LyricsProviderState.FOUND -> stringResource(Res.string.lyrics_provider_found)
        state == LyricsProviderState.NOT_FOUND -> stringResource(Res.string.lyrics_provider_not_found)
        state == LyricsProviderState.FETCHING -> stringResource(Res.string.lyrics_provider_fetching)
        else -> stringResource(Res.string.lyrics_provider_not_fetched)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ROW_SHAPE)
            .background(Color.White.copy(alpha = if (current) 0.10f else 0.05f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
            ) {
                haptics.play(Haptic.Select)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (current) 0.16f else 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state == LyricsProviderState.FETCHING -> CircularProgressIndicator(
                    color = Color.White.copy(alpha = 0.8f),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
                current || state == LyricsProviderState.FOUND -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = if (current) 1f else 0.75f),
                    modifier = Modifier.size(21.dp),
                )
                state == LyricsProviderState.NOT_FOUND -> Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp),
                )
                else -> Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = source.label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = Color.White.copy(alpha = if (current) 1f else 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = if (current) 0.7f else 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
