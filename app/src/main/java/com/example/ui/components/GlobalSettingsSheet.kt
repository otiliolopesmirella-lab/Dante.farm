package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DIGIIDLE_BR_URL
import com.example.model.DIGIIDLE_PRIMARY_URL
import com.example.model.GlobalFarmSettings
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDeckSurface
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.DeepEmeraldContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GlobalSettingsSheet(
    settings: GlobalFarmSettings,
    onDismiss: () -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onToggleAntiAfk: (Boolean) -> Unit,
    onSelectAutoRefreshMinutes: (Int) -> Unit,
    onApplyUrlToAll: (String) -> Unit,
    onApplyZoomToAll: (Int) -> Unit,
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
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dante_farm_logo),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, com.example.ui.theme.CrimsonError, CircleShape),
                )
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Central de Farm 4×",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan,
                    )
                }
            }

            // Multi-Profile Diagnostic Card
            Surface(
                color = if (settings.multiProfileSupported) DeepEmeraldContainer.copy(alpha = 0.55f) else CyberCardSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (settings.multiProfileSupported) EmeraldNeon.copy(alpha = 0.5f) else AmberGold.copy(alpha = 0.5f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (settings.multiProfileSupported) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = if (settings.multiProfileSupported) EmeraldNeon else AmberGold,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (settings.multiProfileSupported) {
                                "Isolamento Multi-Conta Ativo"
                            } else {
                                "WebView do Sistema Desatualizado"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (settings.multiProfileSupported) {
                                "Cada uma das 4 telas usa cookies e armazenamento independentes para logar 4 contas diferentes."
                            } else {
                                "Atualize o Android System WebView na Play Store para ativar 4 sessões totalmente separadas."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            // Keep screen on
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.keep_screen_on_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Impede o celular de bloquear durante o farm AFK",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
                Switch(
                    checked = settings.keepScreenOn,
                    onCheckedChange = onToggleKeepScreenOn,
                    modifier = Modifier.testTag("switch_keep_screen_on"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberDeckSurface,
                        checkedTrackColor = ElectricCyan,
                    ),
                )
            }

            // Anti-AFK Pulse
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.anti_afk_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Mantém as abas ativas enviando sinal discreto de foco",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
                Switch(
                    checked = settings.antiAfkHeartbeat,
                    onCheckedChange = onToggleAntiAfk,
                    modifier = Modifier.testTag("switch_anti_afk"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberDeckSurface,
                        checkedTrackColor = EmeraldNeon,
                    ),
                )
            }

            HorizontalDivider(color = CyberBorder)

            // Auto-refresh interval
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.auto_refresh_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf(
                        0 to "Desligado",
                        5 to "5 min",
                        10 to "10 min",
                        15 to "15 min",
                        30 to "30 min",
                        60 to "60 min",
                    ).forEach { (minutes, label) ->
                        FilterChip(
                            selected = settings.autoRefreshMinutes == minutes,
                            onClick = { onSelectAutoRefreshMinutes(minutes) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepCyanContainer,
                                selectedLabelColor = ElectricCyan,
                            ),
                        )
                    }
                }
            }

            HorizontalDivider(color = CyberBorder)

            // Bulk Actions across all 4 slots
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.apply_all_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { onApplyUrlToAll(DIGIIDLE_PRIMARY_URL) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, CyberBorder),
                    ) {
                        Text("Todas: digiidle.com", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { onApplyUrlToAll(DIGIIDLE_BR_URL) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, CyberBorder),
                    ) {
                        Text("Todas: .com.br", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Text(
                    text = "Zoom rápido em todas as 4 telas",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(50, 65, 80, 100).forEach { z ->
                        OutlinedButton(
                            onClick = { onApplyZoomToAll(z) },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, CyberBorder),
                        ) {
                            Text("$z%", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
