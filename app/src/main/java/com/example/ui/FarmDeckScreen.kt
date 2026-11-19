package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.core.content.ContextCompat
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
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.LayoutMode
import com.example.model.SLOT_COUNT
import com.example.ui.components.DanteControlHubSheet
import com.example.ui.components.GlobalSettingsSheet
import com.example.ui.components.PaywallSheet
import com.example.ui.components.SlotConfigSheet
import com.example.ui.components.SlotPane
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonNeon
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDeckSurface
import com.example.ui.theme.DeepAmberContainer
import com.example.ui.theme.DeepCrimsonContainer
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.DeepEmeraldContainer
import com.example.ui.theme.DeepMatrixGreenContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.MatrixGreen
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
    val context = LocalContext.current

    KeepScreenOnEffect(enabled = uiState.settings.keepScreenOn)

    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    var showControlHubSheet by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        viewModel.syncKeepAliveService()
    }

    LaunchedEffect(uiState.settings.backgroundFarmEnabled, uiState.subscription.isProActive) {
        if (uiState.settings.backgroundFarmEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !askedNotificationPermission &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                askedNotificationPermission = true
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            viewModel.syncKeepAliveService()
        } else {
            viewModel.syncKeepAliveService()
        }
    }

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
                    colors = listOf(
                        Color(0xFF060203),
                        Color(0xFF160409),
                        Color(0xFF080203),
                    )
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
                    onOpenHub = { showControlHubSheet = true },
                    onSelectLayout = viewModel::setLayoutMode,
                    onSelectFocusSlot = viewModel::toggleFocusSlot,
                    onExitFocus = viewModel::exitFocusMode,
                    onReloadAll = viewModel::reloadAllSlots,
                    onOpenSettings = viewModel::openGlobalSettings,
                    onOpenPaywall = viewModel::openPaywall,
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

                        // Stable per-slot callbacks prevent unnecessary recomposition across the other 3 panes
                        val onTriggerAutoLogin = remember(slotIndex, viewModel) { { viewModel.fillSlotLoginNow(slotIndex) } }
                        val onToggleFocus = remember(slotIndex, viewModel) { { viewModel.toggleFocusSlot(slotIndex) } }
                        val onToggleMute = remember(slotIndex, viewModel) { { viewModel.toggleMuteSlot(slotIndex) } }
                        val onReload = remember(slotIndex, viewModel) { { viewModel.reloadSlot(slotIndex) } }
                        val onGoBack = remember(slotIndex, viewModel) { { viewModel.goBackSlot(slotIndex) } }
                        val onGoHome = remember(slotIndex, viewModel) { { viewModel.goHomeSlot(slotIndex) } }
                        val onTogglePause = remember(slotIndex, viewModel) { { viewModel.togglePauseSlot(slotIndex) } }
                        val onToggleDesktop = remember(slotIndex, viewModel) { { viewModel.toggleDesktopModeSlot(slotIndex) } }
                        val onOpenSettings = remember(slotIndex, viewModel) { { viewModel.openSlotEditor(slotIndex) } }

                        // Direct crisp bounds (no 60fps animated WebView resizing, preventing Chromium surface thrashing)
                        SlotPane(
                            config = slotConfig,
                            runtime = slotRuntime,
                            isFocused = focusedSlot == slotIndex,
                            isLockedPro = slotIndex > 0 && !uiState.subscription.isProActive,
                            canStartDailyTrial = uiState.subscription.canStartDailyTrial,
                            webViewManager = viewModel.webViewManager,
                            onUnlockPro = viewModel::openPaywall,
                            onStartDailyTrial = viewModel::startDailyTrial,
                            onTriggerAutoLogin = onTriggerAutoLogin,
                            onToggleFocus = onToggleFocus,
                            onToggleMute = onToggleMute,
                            onReload = onReload,
                            onGoBack = onGoBack,
                            onGoHome = onGoHome,
                            onTogglePause = onTogglePause,
                            onToggleDesktop = onToggleDesktop,
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier
                                .offset(x = targetBounds.x, y = targetBounds.y)
                                .size(width = targetBounds.width, height = targetBounds.height)
                                .zIndex(targetBounds.zIndex),
                        )
                    }
                }
            }
        }

        // Floating ultra-slim restore pill when Command Bar is collapsed for maximum farm view
        if (uiState.settings.isHeaderCollapsed) {
            Surface(
                onClick = viewModel::toggleHeaderCollapsed,
                shape = RoundedCornerShape(50),
                color = Color(0xE6120306),
                border = BorderStroke(1.dp, CrimsonNeon.copy(alpha = 0.85f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .zIndex(20f)
                    .testTag("expand_header_pill"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.dante_avatar),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .border(1.dp, CrimsonNeon, CircleShape),
                    )
                    Text(
                        text = if (focusedSlot != null) "DANTE • C${focusedSlot + 1}" else "DANTE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                        ),
                        color = TextPrimary,
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.action_expand_bar),
                        tint = CrimsonNeon,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }

        // Per-Slot Settings Sheet
        val editingIdx = uiState.editingSlotIndex
        if (editingIdx != null && editingIdx in 0 until SLOT_COUNT) {
            SlotConfigSheet(
                config = uiState.slots[editingIdx],
                credentials = uiState.slotCredentials[editingIdx],
                runtime = uiState.runtimeStates[editingIdx],
                onDismiss = viewModel::closeSlotEditor,
                onSave = { label, url, zoom, desktop, user, pass, autoFill ->
                    viewModel.saveSlotConfig(editingIdx, label, url, zoom, desktop, user, pass, autoFill)
                },
                onAutofillNow = { user, pass, autoFill ->
                    viewModel.fillSlotLoginNow(editingIdx, user, pass, autoFill)
                },
                onClearCredentials = { viewModel.clearSlotCredentials(editingIdx) },
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
                subscription = uiState.subscription,
                onDismiss = viewModel::closeGlobalSettings,
                onOpenPaywall = viewModel::openPaywall,
                onToggleKeepScreenOn = viewModel::setKeepScreenOn,
                onToggleBackgroundFarm = viewModel::setBackgroundFarmEnabled,
                onToggleAntiAfk = viewModel::setAntiAfkHeartbeat,
                onSelectAutoRefreshMinutes = viewModel::setAutoRefreshMinutes,
                onApplyUrlToAll = viewModel::applyUrlToAllSlots,
                onApplyZoomToAll = viewModel::applyZoomToAllSlots,
            )
        }

        // PRO Paywall Sheet (PIX via WhatsApp + 30m Free Trial + Chave Offline)
        if (uiState.showPaywallSheet) {
            PaywallSheet(
                subscription = uiState.subscription,
                licenseFeedback = uiState.licenseFeedback,
                onDismiss = viewModel::closePaywall,
                onStartDailyTrial = viewModel::startDailyTrial,
                onActivateLicenseKey = viewModel::activateLicenseKey,
                onRemoveLicenseKey = viewModel::removeLicenseKey,
            )
        }

        // Dante Tactical Control Hub Sheet (styled after user's crimson mockup)
        if (showControlHubSheet) {
            DanteControlHubSheet(
                currentLayoutMode = uiState.settings.layoutMode,
                backgroundFarmEnabled = uiState.settings.backgroundFarmEnabled,
                subscription = uiState.subscription,
                onDismiss = { showControlHubSheet = false },
                onSelectLayout = viewModel::setLayoutMode,
                onToggleBackgroundFarm = viewModel::setBackgroundFarmEnabled,
                onOpenPaywall = viewModel::openPaywall,
                onStartDailyTrial = viewModel::startDailyTrial,
                onReloadAll = viewModel::reloadAllSlots,
                onOpenGlobalSettings = viewModel::openGlobalSettings,
            )
        }
    }
}

@Composable
private fun CommandDeckHeader(
    uiState: FarmUiState,
    onOpenHub: () -> Unit,
    onSelectLayout: (LayoutMode) -> Unit,
    onSelectFocusSlot: (Int) -> Unit,
    onExitFocus: () -> Unit,
    onReloadAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPaywall: () -> Unit,
    onCollapse: () -> Unit,
) {
    val focusedSlot = uiState.settings.focusedSlotIndex
    val isPro = uiState.subscription.isProActive
    val isTrial = uiState.subscription.isTrialActive
    val canTrial = uiState.subscription.canStartDailyTrial
    val proBadge = uiState.subscription.shortRemainingBadge()
    val bgFarmOn = uiState.settings.backgroundFarmEnabled
    val remainingSec = uiState.remainingAutoRefreshSeconds

    Surface(
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    CrimsonNeon.copy(alpha = 0.9f),
                    Color(0xFF6B1222),
                    CrimsonNeon.copy(alpha = 0.75f),
                )
            ),
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF23050B),
                            Color(0xFF0E0306),
                            Color(0xFF1B0409),
                        )
                    )
                )
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Left: Dante Avatar (with live green dot for 2º Plano) -> opens Painel Hub
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onOpenHub)
                    .testTag("dante_avatar_hub_button"),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dante_avatar),
                    contentDescription = "Abrir Painel Dante",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = if (bgFarmOn) CrimsonNeon else CyberBorder,
                            shape = CircleShape,
                        ),
                )
                if (bgFarmOn) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(ObsidianBg)
                            .padding(1.5.dp)
                            .clip(CircleShape)
                            .background(MatrixGreen)
                    )
                }
            }

            if (focusedSlot != null) {
                // Focused mode: compact C1..C4 + 4x switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF140508))
                        .border(0.8.dp, CyberBorder.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (i in 0 until SLOT_COUNT) {
                        val accent = SlotAccents[i]
                        val selected = focusedSlot == i
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (selected) accent.container else Color.Transparent)
                                .border(
                                    width = if (selected) 0.8.dp else 0.dp,
                                    color = if (selected) accent.primary else Color.Transparent,
                                    shape = RoundedCornerShape(7.dp),
                                )
                                .clickable { onSelectFocusSlot(i) }
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                .testTag("quick_focus_c${i + 1}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "C${i + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = if (selected) accent.primary else TextSecondary,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(DeepCrimsonContainer)
                            .border(0.8.dp, CrimsonNeon.copy(alpha = 0.7f), RoundedCornerShape(7.dp))
                            .clickable(onClick = onExitFocus)
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .testTag("quick_exit_focus"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "4×",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CrimsonGlow,
                        )
                    }
                }
            } else {
                // Sleek PRO / 30M Status Pill
                val pillBorder = when {
                    isTrial -> ElectricCyan
                    isPro -> MatrixGreen
                    canTrial -> ElectricCyan.copy(alpha = 0.9f)
                    else -> CrimsonNeon
                }
                val pillBgStart = when {
                    isTrial -> Color(0xFF062730)
                    isPro -> Color(0xFF062A14)
                    canTrial -> Color(0xFF07232C)
                    else -> Color(0xFF3A0710)
                }
                val pillBgEnd = when {
                    isTrial -> Color(0xFF03141A)
                    isPro -> Color(0xFF03150A)
                    canTrial -> Color(0xFF15050A)
                    else -> Color(0xFF180408)
                }
                Surface(
                    onClick = onOpenPaywall,
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, pillBorder),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("header_pro_pill"),
                ) {
                    Row(
                        modifier = Modifier
                            .background(Brush.horizontalGradient(listOf(pillBgStart, pillBgEnd)))
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = when {
                                isTrial -> Icons.Rounded.Timer
                                isPro -> Icons.Rounded.Verified
                                canTrial -> Icons.Rounded.Timer
                                else -> Icons.Rounded.WorkspacePremium
                            },
                            contentDescription = when {
                                isTrial -> "Teste grátis 30 minutos ativo"
                                isPro -> "Plano PRO ativo"
                                else -> "Testar grátis ou assinar PRO"
                            },
                            tint = pillBorder,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = when {
                                isTrial && proBadge != null -> proBadge
                                isTrial -> "30M"
                                isPro && proBadge != null -> "PRO $proBadge"
                                isPro -> "PRO"
                                canTrial -> "30M"
                                else -> "PRO"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                            ),
                            color = when {
                                isTrial || canTrial -> ElectricCyan
                                isPro -> MatrixGreen
                                else -> TextPrimary
                            },
                            maxLines = 1,
                        )
                    }
                }

                // Compact Auto-Refresh countdown chip (only shown when timer is active)
                if (remainingSec != null) {
                    val mins = remainingSec / 60
                    val secs = remainingSec % 60
                    Surface(
                        color = Color(0xFF1A050A),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.8.dp, CrimsonNeon.copy(alpha = 0.45f)),
                    ) {
                        Text(
                            text = "%02d:%02d".format(mins, secs),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = CrimsonGlow,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Right: Sleek Glossy Gaming Icon Dock
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (focusedSlot == null) {
                    // Single compact Layout Mode cycle button (2x2 -> 1+3 -> 4x)
                    val (layoutIcon, layoutNext, layoutTag) = when (uiState.settings.layoutMode) {
                        LayoutMode.GRID_2X2 -> Triple(
                            Icons.Rounded.GridView,
                            LayoutMode.FOCUS_1_PLUS_3,
                            "layout_grid_2x2",
                        )
                        LayoutMode.FOCUS_1_PLUS_3 -> Triple(
                            Icons.Rounded.Dashboard,
                            LayoutMode.STACK_4,
                            "layout_focus_1_3",
                        )
                        LayoutMode.STACK_4 -> Triple(
                            Icons.Rounded.ViewAgenda,
                            LayoutMode.GRID_2X2,
                            "layout_stack_4",
                        )
                    }
                    HeaderIconTile(
                        icon = layoutIcon,
                        contentDescription = "Alternar layout (${uiState.settings.layoutMode.labelPt})",
                        tint = CrimsonGlow,
                        accentBorder = CrimsonNeon.copy(alpha = 0.65f),
                        bgGradient = listOf(Color(0xFF2B070E), Color(0xFF140407)),
                        onClick = { onSelectLayout(layoutNext) },
                        tag = layoutTag,
                    )

                    // Painel Dante Hub + Auto Farm 2º Plano live indicator
                    HeaderIconTile(
                        icon = if (bgFarmOn) Icons.Rounded.Bolt else Icons.Rounded.SportsEsports,
                        contentDescription = if (bgFarmOn) "Painel Dante • 2º Plano Ativo" else "Abrir Painel Dante",
                        tint = if (bgFarmOn) MatrixGreen else CrimsonNeon,
                        accentBorder = if (bgFarmOn) MatrixGreen.copy(alpha = 0.85f) else CrimsonNeon.copy(alpha = 0.65f),
                        bgGradient = if (bgFarmOn) {
                            listOf(Color(0xFF072D15), Color(0xFF04170B))
                        } else {
                            listOf(Color(0xFF2B070E), Color(0xFF140407))
                        },
                        onClick = onOpenHub,
                        tag = "open_hub_button",
                    )
                }

                HeaderIconTile(
                    icon = Icons.Rounded.Autorenew,
                    contentDescription = stringResource(R.string.action_reload_all),
                    tint = CrimsonNeon,
                    accentBorder = CrimsonNeon.copy(alpha = 0.55f),
                    bgGradient = listOf(Color(0xFF24060C), Color(0xFF120407)),
                    onClick = onReloadAll,
                    tag = "reload_all_button",
                )

                HeaderIconTile(
                    icon = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.action_settings),
                    tint = TextPrimary,
                    accentBorder = CyberBorder.copy(alpha = 0.9f),
                    bgGradient = listOf(Color(0xFF210A10), Color(0xFF110508)),
                    onClick = onOpenSettings,
                    tag = "global_settings_button",
                )

                HeaderIconTile(
                    icon = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.action_collapse_bar),
                    tint = TextSecondary,
                    accentBorder = CyberBorder.copy(alpha = 0.65f),
                    bgGradient = listOf(Color(0xFF19070B), Color(0xFF0D0406)),
                    onClick = onCollapse,
                    tag = "collapse_header_button",
                    sizeDp = 28.dp,
                    iconSizeDp = 16.dp,
                )
            }
        }
    }
}

@Composable
private fun HeaderIconTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    accentBorder: Color,
    bgGradient: List<Color>,
    onClick: () -> Unit,
    tag: String,
    sizeDp: Dp = 31.dp,
    iconSizeDp: Dp = 17.dp,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, accentBorder),
        modifier = Modifier
            .size(sizeDp)
            .testTag(tag),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(bgGradient)),
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
