package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.LayoutMode
import com.example.model.SLOT_COUNT
import com.example.ui.components.GlobalSettingsSheet
import com.example.ui.components.SlotConfigSheet
import com.example.ui.components.SlotPane
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDeckSurface
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlotAccents
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private data class PaneBounds(
    val x: Dp,
    val y: Dp,
    val width: Dp,
    val height: Dp,
    val zIndex: Float,
)

@Composable
fun FarmDeckScreen(viewModel: FarmViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusedSlot = uiState.settings.focusedSlotIndex

    KeepScreenOnEffect(enabled = uiState.settings.keepScreenOn)

    // Handle system Back button gracefully when a slot is expanded to fullscreen
    BackHandler(enabled = focusedSlot != null) {
        val idx = focusedSlot ?: return@BackHandler
        val runtime = uiState.runtimeStates.getOrNull(idx)
        if (runtime?.canGoBack == true) {
            viewModel.goBackSlot(idx)
        } else {
            viewModel.exitFocusMode()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianBg, CyberDeckSurface.copy(alpha = 0.7f), ObsidianBg)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Tactical Command Bar
            AnimatedVisibility(
                visible = !uiState.settings.isHeaderCollapsed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                CommandDeckHeader(
                    uiState = uiState,
                    onSelectLayout = viewModel::setLayoutMode,
                    onSelectFocusSlot = viewModel::toggleFocusSlot,
                    onExitFocus = viewModel::exitFocusMode,
                    onReloadAll = viewModel::reloadAllSlots,
                    onOpenSettings = viewModel::openGlobalSettings,
                    onCollapse = viewModel::toggleHeaderCollapsed,
                )
            }

            // 4-Screen WebView Viewport
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                val gap = 4.dp
                val isLandscape = maxWidth > maxHeight

                for (slotIndex in 0 until SLOT_COUNT) {
                    key(slotIndex) {
                        val slotConfig = uiState.slots[slotIndex]
                        val slotRuntime = uiState.runtimeStates[slotIndex]
                        val targetBounds = computePaneBounds(
                            slotIndex = slotIndex,
                            totalWidth = maxWidth,
                            totalHeight = maxHeight,
                            gap = gap,
                            layoutMode = uiState.settings.layoutMode,
                            focusedSlotIndex = focusedSlot,
                            isLandscape = isLandscape,
                        )

                        val animSpec = spring<Dp>(stiffness = Spring.StiffnessMediumLow)
                        val animX by animateDpAsState(targetBounds.x, animationSpec = animSpec, label = "x_$slotIndex")
                        val animY by animateDpAsState(targetBounds.y, animationSpec = animSpec, label = "y_$slotIndex")
                        val animW by animateDpAsState(targetBounds.width, animationSpec = animSpec, label = "w_$slotIndex")
                        val animH by animateDpAsState(targetBounds.height, animationSpec = animSpec, label = "h_$slotIndex")

                        SlotPane(
                            config = slotConfig,
                            runtime = slotRuntime,
                            isFocused = focusedSlot == slotIndex,
                            webViewManager = viewModel.webViewManager,
                            onToggleFocus = { viewModel.toggleFocusSlot(slotIndex) },
                            onToggleMute = { viewModel.toggleMuteSlot(slotIndex) },
                            onReload = { viewModel.reloadSlot(slotIndex) },
                            onGoBack = { viewModel.goBackSlot(slotIndex) },
                            onGoHome = { viewModel.goHomeSlot(slotIndex) },
                            onTogglePause = { viewModel.togglePauseSlot(slotIndex) },
                            onToggleDesktop = { viewModel.toggleDesktopModeSlot(slotIndex) },
                            onOpenSettings = { viewModel.openSlotEditor(slotIndex) },
                            modifier = Modifier
                                .offset(x = animX, y = animY)
                                .size(width = animW, height = animH)
                                .zIndex(targetBounds.zIndex),
                        )
                    }
                }
            }
        }

        // Floating restore button when Command Bar is collapsed for maximum farm view
        if (uiState.settings.isHeaderCollapsed) {
            Surface(
                onClick = viewModel::toggleHeaderCollapsed,
                shape = RoundedCornerShape(20.dp),
                color = CyberDeckSurface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
                    .zIndex(20f)
                    .testTag("expand_header_pill"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.action_expand_bar),
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp),
                    )
                    Image(
                        painter = painterResource(id = R.drawable.dante_farm_logo),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape),
                    )
                    Text(
                        text = "Dante Farm",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                    )
                    if (focusedSlot != null) {
                        Text(
                            text = "• C${focusedSlot + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SlotAccents[focusedSlot % SlotAccents.size].primary,
                        )
                    }
                }
            }
        }

        // Per-Slot Settings Sheet
        val editingIdx = uiState.editingSlotIndex
        if (editingIdx != null && editingIdx in 0 until SLOT_COUNT) {
            SlotConfigSheet(
                config = uiState.slots[editingIdx],
                runtime = uiState.runtimeStates[editingIdx],
                onDismiss = viewModel::closeSlotEditor,
                onSave = { label, url, zoom, desktop ->
                    viewModel.saveSlotConfig(editingIdx, label, url, zoom, desktop)
                },
                onReload = { viewModel.reloadSlot(editingIdx) },
                onGoBack = { viewModel.goBackSlot(editingIdx) },
                onGoHome = { viewModel.goHomeSlot(editingIdx) },
                onTogglePause = { viewModel.togglePauseSlot(editingIdx) },
                onClearSession = { viewModel.clearSlotSession(editingIdx) },
            )
        }

        // Global Farm Settings Sheet
        if (uiState.showGlobalSettingsSheet) {
            GlobalSettingsSheet(
                settings = uiState.settings,
                onDismiss = viewModel::closeGlobalSettings,
                onToggleKeepScreenOn = viewModel::setKeepScreenOn,
                onToggleAntiAfk = viewModel::setAntiAfkHeartbeat,
                onSelectAutoRefreshMinutes = viewModel::setAutoRefreshMinutes,
                onApplyUrlToAll = viewModel::applyUrlToAllSlots,
                onApplyZoomToAll = viewModel::applyZoomToAllSlots,
            )
        }
    }
}

@Composable
private fun CommandDeckHeader(
    uiState: FarmUiState,
    onSelectLayout: (LayoutMode) -> Unit,
    onSelectFocusSlot: (Int) -> Unit,
    onExitFocus: () -> Unit,
    onReloadAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onCollapse: () -> Unit,
) {
    val focusedSlot = uiState.settings.focusedSlotIndex

    Surface(
        color = CyberDeckSurface,
        border = BorderStroke(1.dp, CyberBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: Brand + Isolation Status + KeepScreenOn indicator
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dante_farm_logo),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.dp, com.example.ui.theme.CrimsonError.copy(alpha = 0.85f), CircleShape),
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Dante Farm",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Surface(
                            color = if (uiState.settings.multiProfileSupported) {
                                EmeraldNeon.copy(alpha = 0.16f)
                            } else {
                                AmberGold.copy(alpha = 0.16f)
                            },
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (uiState.settings.multiProfileSupported) {
                                                EmeraldNeon
                                            } else {
                                                AmberGold
                                            }
                                        )
                                )
                                Text(
                                    text = if (uiState.settings.multiProfileSupported) {
                                        stringResource(R.string.badge_isolated_4x)
                                    } else {
                                        "4× COMP"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.settings.multiProfileSupported) {
                                        EmeraldNeon
                                    } else {
                                        AmberGold
                                    },
                                )
                            }
                        }
                    }

                    // Secondary status line: countdown or keep-screen-on
                    val remainingSec = uiState.remainingAutoRefreshSeconds
                    if (remainingSec != null || uiState.settings.keepScreenOn) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (uiState.settings.keepScreenOn) {
                                Icon(
                                    imageVector = Icons.Rounded.WbSunny,
                                    contentDescription = null,
                                    tint = AmberGold,
                                    modifier = Modifier.size(11.dp),
                                )
                                Text(
                                    text = "Tela ativa",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                            if (remainingSec != null) {
                                val mins = remainingSec / 60
                                val secs = remainingSec % 60
                                Text(
                                    text = "• ↻ %02d:%02d".format(mins, secs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                )
                            }
                        }
                    }
                }
            }

            // Center / Right: Either Quick Slot Switcher (in Focus mode) or Layout Switcher (in Multi-view)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (focusedSlot != null) {
                    // Quick switch C1 C2 C3 C4 + 4× exit chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberCardSurface)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (i in 0 until SLOT_COUNT) {
                            val accent = SlotAccents[i]
                            val selected = focusedSlot == i
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) accent.container else CyberCardSurface)
                                    .clickable { onSelectFocusSlot(i) }
                                    .padding(horizontal = 7.dp, vertical = 6.dp)
                                    .testTag("quick_focus_c${i + 1}"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "C${i + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) accent.primary else TextSecondary,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DeepCyanContainer)
                                .clickable(onClick = onExitFocus)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("quick_exit_focus"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "4×",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                            )
                        }
                    }
                } else {
                    // Layout mode selector pills
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberCardSurface)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        LayoutPill(
                            selected = uiState.settings.layoutMode == LayoutMode.GRID_2X2,
                            icon = Icons.Rounded.GridView,
                            contentDescription = LayoutMode.GRID_2X2.labelPt,
                            onClick = { onSelectLayout(LayoutMode.GRID_2X2) },
                            tag = "layout_grid_2x2",
                        )
                        LayoutPill(
                            selected = uiState.settings.layoutMode == LayoutMode.FOCUS_1_PLUS_3,
                            icon = Icons.Rounded.Dashboard,
                            contentDescription = LayoutMode.FOCUS_1_PLUS_3.labelPt,
                            onClick = { onSelectLayout(LayoutMode.FOCUS_1_PLUS_3) },
                            tag = "layout_focus_1_3",
                        )
                        LayoutPill(
                            selected = uiState.settings.layoutMode == LayoutMode.STACK_4,
                            icon = Icons.Rounded.ViewAgenda,
                            contentDescription = LayoutMode.STACK_4.labelPt,
                            onClick = { onSelectLayout(LayoutMode.STACK_4) },
                            tag = "layout_stack_4",
                        )
                    }
                }

                IconButton(
                    onClick = onReloadAll,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("reload_all_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.action_reload_all),
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("global_settings_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.action_settings),
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("collapse_header_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.action_collapse_bar),
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LayoutPill(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tag: String,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) DeepCyanContainer else CyberCardSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 6.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) ElectricCyan else TextSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun computePaneBounds(
    slotIndex: Int,
    totalWidth: Dp,
    totalHeight: Dp,
    gap: Dp,
    layoutMode: LayoutMode,
    focusedSlotIndex: Int?,
    isLandscape: Boolean,
): PaneBounds {
    if (focusedSlotIndex != null) {
        // Focused slot fills viewport on top; other 3 stay active underneath without throttling JS
        return PaneBounds(
            x = 0.dp,
            y = 0.dp,
            width = totalWidth,
            height = totalHeight,
            zIndex = if (slotIndex == focusedSlotIndex) 10f else 0f,
        )
    }

    return when (layoutMode) {
        LayoutMode.GRID_2X2 -> {
            val col = slotIndex % 2
            val row = slotIndex / 2
            val paneW = ((totalWidth - gap) / 2f).coerceAtLeast(1.dp)
            val paneH = ((totalHeight - gap) / 2f).coerceAtLeast(1.dp)
            PaneBounds(
                x = if (col == 0) 0.dp else paneW + gap,
                y = if (row == 0) 0.dp else paneH + gap,
                width = paneW,
                height = paneH,
                zIndex = 1f,
            )
        }

        LayoutMode.FOCUS_1_PLUS_3 -> {
            if (isLandscape) {
                val mainW = ((totalWidth - gap) * 0.62f).coerceAtLeast(1.dp)
                val sideW = ((totalWidth - gap) * 0.38f).coerceAtLeast(1.dp)
                val sideH = ((totalHeight - gap * 2f) / 3f).coerceAtLeast(1.dp)
                if (slotIndex == 0) {
                    PaneBounds(0.dp, 0.dp, mainW, totalHeight, 1f)
                } else {
                    val subIdx = slotIndex - 1
                    PaneBounds(
                        x = mainW + gap,
                        y = (sideH + gap) * subIdx,
                        width = sideW,
                        height = sideH,
                        zIndex = 1f,
                    )
                }
            } else {
                val mainH = ((totalHeight - gap) * 0.56f).coerceAtLeast(1.dp)
                val bottomH = ((totalHeight - gap) * 0.44f).coerceAtLeast(1.dp)
                val bottomW = ((totalWidth - gap * 2f) / 3f).coerceAtLeast(1.dp)
                if (slotIndex == 0) {
                    PaneBounds(0.dp, 0.dp, totalWidth, mainH, 1f)
                } else {
                    val subIdx = slotIndex - 1
                    PaneBounds(
                        x = (bottomW + gap) * subIdx,
                        y = mainH + gap,
                        width = bottomW,
                        height = bottomH,
                        zIndex = 1f,
                    )
                }
            }
        }

        LayoutMode.STACK_4 -> {
            if (isLandscape) {
                val colW = ((totalWidth - gap * 3f) / 4f).coerceAtLeast(1.dp)
                PaneBounds(
                    x = (colW + gap) * slotIndex,
                    y = 0.dp,
                    width = colW,
                    height = totalHeight,
                    zIndex = 1f,
                )
            } else {
                val rowH = ((totalHeight - gap * 3f) / 4f).coerceAtLeast(1.dp)
                PaneBounds(
                    x = 0.dp,
                    y = (rowH + gap) * slotIndex,
                    width = totalWidth,
                    height = rowH,
                    zIndex = 1f,
                )
            }
        }
    }
}

@Composable
private fun KeepScreenOnEffect(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(context, enabled) {
        val window = context.findActivity()?.window
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
