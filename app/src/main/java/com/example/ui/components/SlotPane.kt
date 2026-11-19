package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.model.AccountSlotConfig
import com.example.model.SlotRuntimeState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CrimsonNeon
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDeckSurface
import com.example.ui.theme.DeepAmberContainer
import com.example.ui.theme.DeepCrimsonContainer
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.ElectricCyan
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
    isLockedPro: Boolean = false,
    canStartDailyTrial: Boolean = false,
    webViewManager: SlotWebViewManager,
    onUnlockPro: () -> Unit = {},
    onStartDailyTrial: () -> Unit = {},
    onTriggerAutoLogin: () -> Unit = {},
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
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Ultra-slim 28dp crimson-obsidian slot header with neon accent badge & polished mini-icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isFocused) 34.dp else 28.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accent.container.copy(alpha = 0.55f),
                                Color(0xFF100407),
                                Color(0xFF0A0305),
                            )
                        )
                    )
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                // Slot Badge & Label (tap opens config sheet)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenSettings)
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                        .testTag("slot_header_button_${config.index + 1}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    val statusColor = when {
                        config.isPaused -> TextMuted
                        runtime.hasError -> CrimsonError
                        else -> accent.primary
                    }
                    // Numbered micro-pill badge (1..4)
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(statusColor.copy(alpha = 0.2f))
                            .border(0.8.dp, statusColor.copy(alpha = 0.85f), RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${config.index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = statusColor,
                        )
                    }
                    Text(
                        text = config.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (config.hasSavedCredentials) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = "Login salvo",
                            tint = accent.primary,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                    if (isFocused) {
                        Surface(
                            color = accent.container,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.6.dp, accent.primary.copy(alpha = 0.6f)),
                        ) {
                            Text(
                                text = "${config.zoomPercent}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = accent.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }

                // Expanded quick controls when focused full-screen
                if (isFocused) {
                    if (config.hasSavedCredentials) {
                        SlotMiniIconTile(
                            icon = Icons.Rounded.Key,
                            contentDescription = "Preencher login salvo",
                            tint = accent.primary,
                            borderColor = accent.primary.copy(alpha = 0.55f),
                            onClick = onTriggerAutoLogin,
                            sizeDp = 26.dp,
                            iconSizeDp = 14.dp,
                            tag = "slot_login_${config.index + 1}",
                        )
                    }
                    SlotMiniIconTile(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Voltar página",
                        tint = if (runtime.canGoBack) TextPrimary else TextMuted,
                        borderColor = Color(0xFF3A141C),
                        enabled = runtime.canGoBack,
                        onClick = onGoBack,
                        sizeDp = 26.dp,
                        iconSizeDp = 14.dp,
                        tag = "slot_back_${config.index + 1}",
                    )
                    SlotMiniIconTile(
                        icon = Icons.Rounded.Home,
                        contentDescription = "Ir para página inicial",
                        tint = TextSecondary,
                        borderColor = Color(0xFF3A141C),
                        onClick = onGoHome,
                        sizeDp = 26.dp,
                        iconSizeDp = 14.dp,
                        tag = "slot_home_${config.index + 1}",
                    )
                    SlotMiniIconTile(
                        icon = Icons.Rounded.Autorenew,
                        contentDescription = "Recarregar conta",
                        tint = accent.primary,
                        borderColor = accent.primary.copy(alpha = 0.45f),
                        onClick = onReload,
                        sizeDp = 26.dp,
                        iconSizeDp = 14.dp,
                        tag = "slot_reload_${config.index + 1}",
                    )
                    SlotMiniIconTile(
                        icon = Icons.Rounded.DesktopWindows,
                        contentDescription = "Alternar modo desktop",
                        tint = if (config.desktopMode) accent.primary else TextSecondary,
                        borderColor = if (config.desktopMode) accent.primary.copy(alpha = 0.6f) else Color(0xFF3A141C),
                        onClick = onToggleDesktop,
                        sizeDp = 26.dp,
                        iconSizeDp = 14.dp,
                        tag = "slot_desktop_${config.index + 1}",
                    )
                }

                // Always available sleek mini-icon controls (Mute, Config, Expand/Exit)
                val btnSize = if (isFocused) 26.dp else 22.dp
                val icSize = if (isFocused) 14.dp else 13.dp

                SlotMiniIconTile(
                    icon = if (config.isMuted) {
                        Icons.AutoMirrored.Rounded.VolumeOff
                    } else {
                        Icons.AutoMirrored.Rounded.VolumeUp
                    },
                    contentDescription = stringResource(R.string.action_mute_toggle),
                    tint = if (config.isMuted) TextMuted else accent.primary,
                    borderColor = if (config.isMuted) Color(0xFF2B1016) else accent.primary.copy(alpha = 0.45f),
                    onClick = onToggleMute,
                    sizeDp = btnSize,
                    iconSizeDp = icSize,
                    tag = "slot_mute_${config.index + 1}",
                )

                SlotMiniIconTile(
                    icon = Icons.Rounded.Tune,
                    contentDescription = stringResource(R.string.action_slot_settings),
                    tint = TextSecondary,
                    borderColor = Color(0xFF3A141C),
                    onClick = onOpenSettings,
                    sizeDp = btnSize,
                    iconSizeDp = icSize,
                    tag = "slot_tune_${config.index + 1}",
                )

                SlotMiniIconTile(
                    icon = if (isFocused) {
                        Icons.Rounded.CloseFullscreen
                    } else {
                        Icons.Rounded.OpenInFull
                    },
                    contentDescription = if (isFocused) {
                        stringResource(R.string.action_exit_focus)
                    } else {
                        stringResource(R.string.action_fullscreen)
                    },
                    tint = accent.primary,
                    borderColor = accent.primary.copy(alpha = 0.6f),
                    onClick = onToggleFocus,
                    sizeDp = btnSize,
                    iconSizeDp = icSize,
                    tag = "slot_focus_${config.index + 1}",
                )
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

            // WebView Content + PRO Lock / Paused / Error overlays
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLockedPro) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianBg)
                            .clickable(onClick = onUnlockPro),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.dante_avatar),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.18f),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xAA080203),
                                            Color(0xDD140407),
                                            Color(0xEE060203),
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                color = DeepCrimsonContainer,
                                shape = CircleShape,
                                border = BorderStroke(1.2.dp, CrimsonNeon),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Recurso PRO",
                                    tint = CrimsonNeon,
                                    modifier = Modifier
                                        .padding(7.dp)
                                        .size(16.dp),
                                )
                            }
                            Text(
                                text = "${config.label} • PRO",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                                color = TextPrimary,
                            )
                            Text(
                                text = if (canStartDailyTrial) {
                                    "Teste 4 telas 30 min grátis hoje ou assine PRO"
                                } else {
                                    "Teste de hoje usado • Assine a partir de R$ 2"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (canStartDailyTrial) {
                                Button(
                                    onClick = onStartDailyTrial,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeepCyanContainer,
                                        contentColor = ElectricCyan,
                                    ),
                                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.85f)),
                                    modifier = Modifier.testTag("start_trial_slot_${config.index + 1}"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Timer,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Testar 30m Grátis",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                            Button(
                                onClick = onUnlockPro,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepCrimsonContainer,
                                    contentColor = TextPrimary,
                                ),
                                border = BorderStroke(1.2.dp, CrimsonNeon),
                                modifier = Modifier.testTag("unlock_pro_slot_${config.index + 1}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = CrimsonNeon,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ASSINAR PRO",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                } else if (config.url.isBlank()) {
                    // Empty Game URL state — lets user enter any game URL they want
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianBg)
                            .clickable(onClick = onOpenSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.dante_avatar),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.14f),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xAA080203),
                                            Color(0xDD120407),
                                            Color(0xEE060203),
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                color = accent.container,
                                shape = CircleShape,
                                border = BorderStroke(1.2.dp, accent.primary),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = "Definir URL do jogo",
                                    tint = accent.primary,
                                    modifier = Modifier
                                        .padding(7.dp)
                                        .size(18.dp),
                                )
                            }
                            Text(
                                text = "Coloque a URL do Jogo",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                                color = TextPrimary,
                            )
                            Text(
                                text = "Toque para escolher qualquer jogo web para esta tela",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Button(
                                onClick = onOpenSettings,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent.container,
                                    contentColor = accent.primary,
                                ),
                                border = BorderStroke(1.dp, accent.primary),
                                modifier = Modifier.testTag("set_url_slot_${config.index + 1}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Definir URL",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                } else {
                    key(config.index, runtime.recoveryNonce) {
                        AndroidView(
                            factory = { viewContext ->
                                webViewManager.getOrCreateWebView(
                                    slotIndex = config.index,
                                    initialConfig = config,
                                    hostContext = viewContext,
                                )
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

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

@Composable
private fun SlotMiniIconTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    borderColor: Color,
    onClick: () -> Unit,
    sizeDp: Dp,
    iconSizeDp: Dp,
    tag: String,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF17060A),
        border = BorderStroke(0.75.dp, borderColor),
        modifier = Modifier
            .size(sizeDp)
            .testTag(tag),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSizeDp),
            )
        }
    }
}

