package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.model.GlobalFarmSettings
import com.example.monetization.SubscriptionStatus
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
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GlobalSettingsSheet(
    settings: GlobalFarmSettings,
    subscription: SubscriptionStatus,
    onDismiss: () -> Unit,
    onOpenPaywall: () -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onToggleBackgroundFarm: (Boolean) -> Unit,
    onToggleAntiAfk: (Boolean) -> Unit,
    onSelectAutoRefreshMinutes: (Int) -> Unit,
    onApplyUrlToAll: (String) -> Unit,
    onApplyZoomToAll: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isIgnoringBatteryOpt by remember { mutableStateOf(checkBatteryOptimizationIgnored(context)) }
    var bulkUrlInput by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOpt = checkBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            DanteHeroBannerCard(
                drawableRes = R.drawable.dante_settings_banner,
                aspectRatio = 16f / 9f,
                showBottomVignette = false,
                modifier = Modifier.testTag("settings_banner"),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dante_avatar),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.8.dp, CrimsonNeon, CircleShape),
                )
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                    )
                    Text(
                        text = "OPÇÕES E CONFIGURAÇÃO 4×",
                        style = MaterialTheme.typography.labelMedium,
                        color = CrimsonGlow,
                    )
                }
            }

            // Subscription Status Card
            Surface(
                onClick = {
                    onDismiss()
                    onOpenPaywall()
                },
                color = if (subscription.isProActive) {
                    DeepEmeraldContainer.copy(alpha = 0.6f)
                } else {
                    DeepCrimsonContainer.copy(alpha = 0.7f)
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.2.dp,
                    if (subscription.isProActive) EmeraldNeon else CrimsonNeon,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (subscription.isProActive) EmeraldNeon else AmberGold,
                            modifier = Modifier.size(22.dp),
                        )
                        Column {
                            Text(
                                text = when {
                                    subscription.isPaidPro -> "Plano PRO Ativo"
                                    subscription.isTrialActive -> "Teste Grátis Ativo (30 min/dia)"
                                    else -> "Plano Gratuito (1/4 contas)"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            Text(
                                text = when {
                                    subscription.isPaidPro || subscription.isTrialActive -> {
                                        val rem = subscription.remainingTimeLabel()
                                        if (rem != null) "$rem • Toque para gerenciar" else "Acesso 4× ativo • Toque para gerenciar"
                                    }
                                    subscription.canStartDailyTrial -> {
                                        "Teste grátis 30 min/dia ou assine PRO (PIX)"
                                    }
                                    else -> {
                                        "Diário R$ 2 • Semanal R$ 15 • Mensal R$ 30 (PIX)"
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenPaywall()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (subscription.isProActive) EmeraldNeon else CrimsonNeon,
                            contentColor = if (subscription.isProActive) CyberDeckSurface else TextPrimary,
                        ),
                    ) {
                        Text(
                            text = if (subscription.isProActive) "Ver" else "Assinar",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
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

            // Background Farm (Foreground Service)
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
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.background_farm_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Mantém o farm ativo enquanto você assiste YouTube ou usa outros apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
                Switch(
                    checked = settings.backgroundFarmEnabled,
                    onCheckedChange = onToggleBackgroundFarm,
                    modifier = Modifier.testTag("switch_background_farm"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberDeckSurface,
                        checkedTrackColor = EmeraldNeon,
                    ),
                )
            }

            // Battery Optimization Helper Card
            Surface(
                color = if (isIgnoringBatteryOpt) DeepEmeraldContainer.copy(alpha = 0.4f) else DeepAmberContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isIgnoringBatteryOpt) EmeraldNeon.copy(alpha = 0.5f) else AmberGold.copy(alpha = 0.65f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BatterySaver,
                            contentDescription = null,
                            tint = if (isIgnoringBatteryOpt) EmeraldNeon else AmberGold,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isIgnoringBatteryOpt) {
                                    "Bateria: Sem restrições (Protegido)"
                                } else {
                                    "Bateria com economia ativada"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            Text(
                                text = if (isIgnoringBatteryOpt) {
                                    "O Android não vai encerrar suas contas ao abrir o YouTube."
                                } else {
                                    "Para o Android (Xiaomi, Samsung, Realme, Motorola) não fechar o farm em 2º plano, marque Bateria como 'Sem restrições'."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    if (!isIgnoringBatteryOpt) {
                        OutlinedButton(
                            onClick = { openBatterySettings(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_battery_settings_button"),
                            border = BorderStroke(1.dp, AmberGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold),
                        ) {
                            Text(
                                text = "Liberar Bateria (Sem Restrições)",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
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
                            text = stringResource(R.string.anti_afk_label) + if (!subscription.isProActive) " • PRO" else "",
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
                        text = stringResource(R.string.auto_refresh_label) + if (!subscription.isProActive) " • PRO" else "",
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
                OutlinedTextField(
                    value = bulkUrlInput,
                    onValueChange = { bulkUrlInput = it },
                    label = { Text("URL do jogo para todas as 4 telas") },
                    placeholder = { Text("Ex: https://seujogo.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = CrimsonNeon,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bulk_url_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonNeon,
                        unfocusedBorderColor = CyberBorder,
                        focusedLabelColor = CrimsonGlow,
                        cursorColor = CrimsonNeon,
                    ),
                )
                Button(
                    onClick = {
                        onApplyUrlToAll(bulkUrlInput)
                        onDismiss()
                    },
                    enabled = bulkUrlInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_bulk_url_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepCrimsonContainer,
                        contentColor = TextPrimary,
                    ),
                    border = BorderStroke(1.2.dp, CrimsonNeon),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = CrimsonNeon,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Aplicar URL nas 4 Telas", style = MaterialTheme.typography.labelMedium)
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

private fun checkBatteryOptimizationIgnored(context: android.content.Context): Boolean {
    val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as? PowerManager
        ?: return false
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openBatterySettings(context: android.content.Context) {
    val appDetailsIntent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    val batteryListIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching {
        context.startActivity(appDetailsIntent)
    }.onFailure {
        runCatching { context.startActivity(batteryListIntent) }
    }
}
