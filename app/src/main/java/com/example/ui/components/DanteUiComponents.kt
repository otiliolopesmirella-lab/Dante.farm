package com.example.ui.components

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LayoutMode
import com.example.monetization.SubscriptionStatus
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonNeon
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDeckSurface
import com.example.ui.theme.DeepAmberContainer
import com.example.ui.theme.DeepCrimsonContainer
import com.example.ui.theme.DeepMatrixGreenContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DanteHeroBannerCard(
    modifier: Modifier = Modifier,
    heightDp: Int = 152,
    @DrawableRes drawableRes: Int = R.drawable.dante_hero_banner,
    aspectRatio: Float? = null,
    showBottomVignette: Boolean = true,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, CrimsonNeon.copy(alpha = 0.9f)),
        color = ObsidianBg,
        shadowElevation = 8.dp,
    ) {
        val boxModifier = if (aspectRatio != null) {
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
        } else {
            Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
        }
        Box(modifier = boxModifier) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (showBottomVignette) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x44080203),
                                    Color(0xBB060203),
                                )
                            )
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DanteControlHubSheet(
    currentLayoutMode: LayoutMode,
    backgroundFarmEnabled: Boolean,
    subscription: SubscriptionStatus,
    onDismiss: () -> Unit,
    onSelectLayout: (LayoutMode) -> Unit,
    onToggleBackgroundFarm: (Boolean) -> Unit,
    onOpenPaywall: () -> Unit,
    onStartDailyTrial: () -> Unit,
    onReloadAll: () -> Unit,
    onOpenGlobalSettings: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CyberDeckSurface,
        contentColor = TextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DanteHeroBannerCard(heightDp = 165)

            // Row matching the mockup: AUTO FARM ATIVO + PLANO 2º
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    onClick = { onToggleBackgroundFarm(!backgroundFarmEnabled) },
                    modifier = Modifier
                        .weight(1.35f)
                        .height(58.dp)
                        .testTag("hub_autofarm_toggle"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (backgroundFarmEnabled) DeepMatrixGreenContainer else CyberCardSurface,
                    border = BorderStroke(
                        1.5.dp,
                        if (backgroundFarmEnabled) MatrixGreen else CyberBorder,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = if (backgroundFarmEnabled) {
                                Icons.Rounded.CheckCircle
                            } else {
                                Icons.Rounded.PauseCircle
                            },
                            contentDescription = null,
                            tint = if (backgroundFarmEnabled) MatrixGreen else TextSecondary,
                            modifier = Modifier.size(28.dp),
                        )
                        Column {
                            Text(
                                text = "AUTO FARM",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                            )
                            Text(
                                text = if (backgroundFarmEnabled) "ATIVO" else "PAUSADO",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                                color = if (backgroundFarmEnabled) MatrixGreen else TextSecondary,
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        onDismiss()
                        onOpenGlobalSettings()
                    },
                    modifier = Modifier
                        .weight(0.95f)
                        .height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = CyberCardSurface,
                    border = BorderStroke(1.2.dp, CrimsonNeon.copy(alpha = 0.75f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SignalCellularAlt,
                            contentDescription = null,
                            tint = CrimsonNeon,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "PLANO",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                            Text(
                                text = if (backgroundFarmEnabled) "2º ATIVO" else "2º OFF",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                        }
                    }
                }
            }

            // MODO DE FARM panel matching the mockup's glowing crimson radio menu
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0A0305),
                border = BorderStroke(1.4.dp, CrimsonNeon.copy(alpha = 0.85f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TrackChanges,
                            contentDescription = null,
                            tint = CrimsonNeon,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "MODO DE FARM",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                            ),
                            color = TextPrimary,
                        )
                    }

                    FarmModeRadioRow(
                        title = "ISOLADO",
                        subtitle = "Grade 2×2 • 4 contas simultâneas separadas",
                        selected = currentLayoutMode == LayoutMode.GRID_2X2,
                        onClick = {
                            onSelectLayout(LayoutMode.GRID_2X2)
                            onDismiss()
                        },
                    )
                    FarmModeRadioRow(
                        title = "SQUAD",
                        subtitle = "Foco 1+3 • 1 conta principal + 3 auxiliares",
                        selected = currentLayoutMode == LayoutMode.FOCUS_1_PLUS_3,
                        onClick = {
                            onSelectLayout(LayoutMode.FOCUS_1_PLUS_3)
                            onDismiss()
                        },
                    )
                    FarmModeRadioRow(
                        title = "LABORATÓRIO",
                        subtitle = "Lista 4× • Todas as telas empilhadas",
                        selected = currentLayoutMode == LayoutMode.STACK_4,
                        onClick = {
                            onSelectLayout(LayoutMode.STACK_4)
                            onDismiss()
                        },
                    )
                }
            }

            // ASSINAR PRO / TESTAR 30M glowing CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (subscription.canStartDailyTrial && !subscription.isProActive) {
                    Surface(
                        onClick = {
                            onStartDailyTrial()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF06232B),
                        border = BorderStroke(1.4.dp, ElectricCyan),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "TESTAR 30M",
                                style = MaterialTheme.typography.titleSmall,
                                color = ElectricCyan,
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        onDismiss()
                        onOpenPaywall()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("hub_pro_button"),
                    shape = RoundedCornerShape(14.dp),
                    color = when {
                        subscription.isPaidPro -> DeepMatrixGreenContainer
                        subscription.isTrialActive -> DeepAmberContainer
                        else -> DeepCrimsonContainer
                    },
                    border = BorderStroke(
                        1.5.dp,
                        when {
                            subscription.isPaidPro -> MatrixGreen
                            subscription.isTrialActive -> AmberGold
                            else -> CrimsonNeon
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (subscription.isPaidPro) Icons.Rounded.Verified else Icons.Rounded.Star,
                            contentDescription = null,
                            tint = when {
                                subscription.isPaidPro -> MatrixGreen
                                subscription.isTrialActive -> AmberGold
                                else -> CrimsonNeon
                            },
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        val badge = subscription.shortRemainingBadge()
                        Text(
                            text = when {
                                subscription.isPaidPro && badge != null -> "PRO • $badge"
                                subscription.isPaidPro -> "PRO ATIVO"
                                subscription.isTrialActive && badge != null -> "TESTE • $badge"
                                else -> "ASSINAR PRO"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = TextPrimary,
                        )
                    }
                }
            }

            // 4 Crimson Glossy Action Tiles matching bottom of mockup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DanteDockTile(
                    icon = Icons.Rounded.GridView,
                    label = "PAINEL",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                DanteDockTile(
                    icon = Icons.Rounded.Tune,
                    label = "OPÇÕES",
                    onClick = {
                        onDismiss()
                        onOpenGlobalSettings()
                    },
                    modifier = Modifier.weight(1f),
                )
                DanteDockTile(
                    icon = Icons.Rounded.Refresh,
                    label = "RECARREGAR",
                    onClick = {
                        onReloadAll()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
                DanteDockTile(
                    icon = Icons.Rounded.Settings,
                    label = "CONFIG",
                    tint = Color(0xFFE2E8F0),
                    onClick = {
                        onDismiss()
                        onOpenGlobalSettings()
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            // Signature crimson divider footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 1.5.dp,
                    color = CrimsonNeon.copy(alpha = 0.7f),
                )
                Text(
                    text = "DANTE FARM IDLE",
                    style = MaterialTheme.typography.titleSmall.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = CrimsonGlow,
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 1.5.dp,
                    color = CrimsonNeon.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun FarmModeRadioRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) DeepCrimsonContainer.copy(alpha = 0.85f) else Color(0xFF120608),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) CrimsonNeon else CyberBorder.copy(alpha = 0.6f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (selected) CrimsonNeon else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (selected) CrimsonGlow else TextSecondary,
                        shape = CircleShape,
                    ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    ),
                    color = TextPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DanteDockTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = CrimsonNeon,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(66.dp),
        shape = RoundedCornerShape(12.dp),
        color = CyberCardSurface,
        border = BorderStroke(1.2.dp, CrimsonNeon.copy(alpha = 0.8f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = 4.dp, vertical = 6.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
