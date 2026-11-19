package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AccountSlotConfig
import com.example.model.SlotCredentials
import com.example.model.SlotRuntimeState
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDeckSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlotAccents
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SlotConfigSheet(
    config: AccountSlotConfig,
    credentials: SlotCredentials,
    runtime: SlotRuntimeState,
    onDismiss: () -> Unit,
    onSave: (
        label: String,
        url: String,
        zoomPercent: Int,
        desktopMode: Boolean,
        username: String,
        password: String,
        autoFillLogin: Boolean,
    ) -> Unit,
    onAutofillNow: (username: String, password: String, autoFillLogin: Boolean) -> Unit,
    onClearCredentials: () -> Unit,
    onReload: () -> Unit,
    onGoBack: () -> Unit,
    onGoHome: () -> Unit,
    onTogglePause: () -> Unit,
    onClearSession: () -> Unit,
) {
    val accent = SlotAccents[config.index % SlotAccents.size]
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var label by remember(config.index) { mutableStateOf(config.label) }
    var url by remember(config.index) { mutableStateOf(config.url) }
    var zoomPercent by remember(config.index) { mutableIntStateOf(config.zoomPercent) }
    var desktopMode by remember(config.index) { mutableStateOf(config.desktopMode) }
    var username by remember(config.index, credentials.username) { mutableStateOf(credentials.username) }
    var password by remember(config.index, credentials.password) { mutableStateOf(credentials.password) }
    var autoFillLogin by remember(config.index, credentials.autoFillLogin) { mutableStateOf(credentials.autoFillLogin) }
    var showPassword by remember { mutableStateOf(false) }
    var confirmClearSession by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Configurar Slot ${config.index + 1}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = accent.primary,
                    )
                    Text(
                        text = if (runtime.isIsolatedProfileActive) {
                            "Sessão isolada independente • Perfil ${config.index + 1}"
                        } else {
                            "Perfil compartilhado (atualize o WebView do sistema)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            // Quick navigation row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        onGoBack()
                        onDismiss()
                    },
                    enabled = runtime.canGoBack,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, CyberBorder),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Voltar", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = {
                        onReload()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, CyberBorder),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Recarregar", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = {
                        onGoHome()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, CyberBorder),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Início", style = MaterialTheme.typography.labelMedium)
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Nome da conta") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("slot_label_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent.primary,
                    unfocusedBorderColor = CyberBorder,
                    focusedLabelColor = accent.primary,
                    cursorColor = accent.primary,
                ),
            )

            // Saved Account Credentials Card (Username & Password)
            Surface(
                color = CyberCardSurface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent.primary.copy(alpha = 0.35f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = accent.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Login Salvo (Usuário e Senha)",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Salvo com criptografia neste aparelho só para esta tela",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuário ou E-mail") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = accent.primary,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slot_username_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent.primary,
                            unfocusedBorderColor = CyberBorder,
                            focusedLabelColor = accent.primary,
                            cursorColor = accent.primary,
                        ),
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha da conta") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = accent.primary,
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) {
                                        Icons.Rounded.VisibilityOff
                                    } else {
                                        Icons.Rounded.Visibility
                                    },
                                    contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha",
                                    tint = TextSecondary,
                                )
                            }
                        },
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slot_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent.primary,
                            unfocusedBorderColor = CyberBorder,
                            focusedLabelColor = accent.primary,
                            cursorColor = accent.primary,
                        ),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Auto-logar ao abrir tela de login",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = autoFillLogin,
                            onCheckedChange = { autoFillLogin = it },
                            modifier = Modifier.testTag("slot_autofill_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberDeckSurface,
                                checkedTrackColor = accent.primary,
                            ),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                onAutofillNow(username, password, autoFillLogin)
                                onDismiss()
                            },
                            enabled = username.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("slot_autofill_now_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent.container,
                                contentColor = accent.primary,
                            ),
                            border = BorderStroke(1.dp, accent.primary),
                        ) {
                            Text("Entrar Agora", style = MaterialTheme.typography.labelMedium)
                        }

                        if (credentials.hasCredentials) {
                            OutlinedButton(
                                onClick = {
                                    username = ""
                                    password = ""
                                    onClearCredentials()
                                },
                                modifier = Modifier.testTag("slot_clear_credentials_button"),
                                border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                            ) {
                                Text("Apagar", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL do jogo") },
                    placeholder = { Text("Ex: https://seujogo.com") },
                    supportingText = {
                        Text(
                            text = "Cole o link de qualquer jogo web para farmar nesta tela.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slot_url_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent.primary,
                        unfocusedBorderColor = CyberBorder,
                        focusedLabelColor = accent.primary,
                        cursorColor = accent.primary,
                    ),
                )
            }

            // Zoom slider + quick presets
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Zoom da tela",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                    Text(
                        text = "$zoomPercent%",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent.primary,
                    )
                }
                Slider(
                    value = zoomPercent.toFloat(),
                    onValueChange = { zoomPercent = (it / 5f).roundToInt() * 5 },
                    valueRange = 35f..130f,
                    colors = SliderDefaults.colors(
                        thumbColor = accent.primary,
                        activeTrackColor = accent.primary,
                        inactiveTrackColor = CyberCardSurface,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(50, 65, 80, 100).forEach { preset ->
                        FilterChip(
                            selected = zoomPercent == preset,
                            onClick = { zoomPercent = preset },
                            label = { Text("$preset%", style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.container,
                                selectedLabelColor = accent.primary,
                            ),
                        )
                    }
                }
            }

            // Desktop mode toggle
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
                        imageVector = Icons.Rounded.DesktopWindows,
                        contentDescription = null,
                        tint = accent.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Modo Desktop (PC)",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Usa layout de computador dentro deste slot",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
                Switch(
                    checked = desktopMode,
                    onCheckedChange = { desktopMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberDeckSurface,
                        checkedTrackColor = accent.primary,
                    ),
                )
            }

            HorizontalDivider(color = CyberBorder)

            // Pause & Clear Session actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        onTogglePause()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("slot_pause_toggle"),
                    border = BorderStroke(1.dp, CyberBorder),
                ) {
                    Icon(
                        imageVector = if (config.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (config.isPaused) "Retomar" else "Pausar Slot",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (confirmClearSession) {
                            onClearSession()
                            onDismiss()
                        } else {
                            confirmClearSession = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("slot_clear_session"),
                    border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CrimsonError,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (confirmClearSession) {
                            stringResource(R.string.clear_session_confirm)
                        } else {
                            "Limpar Sessão"
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    onSave(
                        label,
                        url,
                        zoomPercent,
                        desktopMode,
                        username,
                        password,
                        autoFillLogin,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("slot_save_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.primary,
                    contentColor = CyberDeckSurface,
                ),
            ) {
                Text(
                    text = "Salvar e Aplicar no Slot ${config.index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
