package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.model.AccountSlotConfig
import com.example.model.SlotRuntimeState
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDeckSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlotAccents
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.webview.SlotWebViewManager

@Composable
fun SlotPane(
    config: AccountSlotConfig,
    runtime: SlotRuntimeState,
    isFocused: Boolean,
    webViewManager: SlotWebViewManager,
    onToggleFocus: () -> Unit,
    onToggleMute: () -> Unit,
    onReload: () -> Unit,
    onGoBack: () -> Unit,
    onGoHome: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleDesktop: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = SlotAccents[config.index % SlotAccents.size]
    val shape = RoundedCornerShape(if (isFocused) 8.dp else 12.dp)
    val borderColor = if (isFocused) accent.primary else accent.glowBorder.copy(alpha = 0.45f)

    Surface(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .testTag("slot_pane_${config.index + 1}"),
        color = ObsidianBg,
        shape = shape,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Slim tactical slot header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberDeckSurface)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Slot Badge & Label (tap opens config sheet)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenSettings)
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .testTag("slot_header_button_${config.index + 1}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    config.isPaused -> TextMuted
                                    runtime.hasError -> CrimsonError
                                    else -> accent.primary
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = config.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isFocused) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = accent.container,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = "${config.zoomPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }

                // Expanded quick controls when focused full-screen
                if (isFocused) {
                    IconButton(
                        onClick = onGoBack,
                        enabled = runtime.canGoBack,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("slot_back_${config.index + 1}"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Voltar página",
                            tint = if (runtime.canGoBack) TextPrimary else TextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onGoHome,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("slot_home_${config.index + 1}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = "Ir para página inicial",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onReload,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("slot_reload_${config.index + 1}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Recarregar conta",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onToggleDesktop,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("slot_desktop_${config.index + 1}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DesktopWindows,
                            contentDescription = "Alternar modo desktop",
                            tint = if (config.desktopMode) accent.primary else TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Always available compact controls (Mute, Config, Expand/Exit)
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("slot_mute_${config.index + 1}"),
                ) {
                    Icon(
                        imageVector = if (config.isMuted) {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = stringResource(R.string.action_mute_toggle),
                        tint = if (config.isMuted) TextMuted else accent.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("slot_tune_${config.index + 1}"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = stringResource(R.string.action_slot_settings),
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }

                IconButton(
                    onClick = onToggleFocus,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("slot_focus_${config.index + 1}"),
                ) {
                    Icon(
                        imageVector = if (isFocused) {
                            Icons.Rounded.FullscreenExit
                        } else {
                            Icons.Rounded.Fullscreen
                        },
                        contentDescription = if (isFocused) {
                            stringResource(R.string.action_exit_focus)
                        } else {
                            stringResource(R.string.action_fullscreen)
                        },
                        tint = accent.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Thin progress indicator
            if (runtime.isLoading && !config.isPaused) {
                LinearProgressIndicator(
                    progress = { (runtime.progress.coerceIn(5, 100)) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = accent.primary,
                    trackColor = Color.Transparent,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CyberCardSurface)
                )
            }

            // WebView Content + Paused/Error overlays
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = {
                        webViewManager.getOrCreateWebView(
                            slotIndex = config.index,
                            initialConfig = config,
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Error Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = runtime.hasError && !config.isPaused,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianBg.copy(alpha = 0.92f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WifiOff,
                                contentDescription = null,
                                tint = CrimsonError,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                text = stringResource(R.string.error_load_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            runtime.errorDescription?.let { desc ->
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Button(
                                onClick = onReload,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent.container,
                                    contentColor = accent.primary,
                                ),
                                border = BorderStroke(1.dp, accent.primary),
                                modifier = Modifier.testTag("slot_retry_${config.index + 1}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.action_retry),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }

                // Paused Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = config.isPaused,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianBg.copy(alpha = 0.9f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Pause,
                                contentDescription = null,
                                tint = accent.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                text = stringResource(R.string.status_paused),
                                style = MaterialTheme.typography.labelMedium,
                                color = accent.primary,
                            )
                            Button(
                                onClick = onTogglePause,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent.container,
                                    contentColor = accent.primary,
                                ),
                                border = BorderStroke(1.dp, accent.primary),
                                modifier = Modifier.testTag("slot_resume_${config.index + 1}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.action_resume_slot),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
