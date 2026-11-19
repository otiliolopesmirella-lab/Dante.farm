package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.R
import com.example.monetization.SubscriptionPlanOffer
import com.example.monetization.SubscriptionSource
import com.example.monetization.SubscriptionStatus
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonError
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
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    subscription: SubscriptionStatus,
    licenseFeedback: String?,
    onDismiss: () -> Unit,
    onStartDailyTrial: () -> Unit,
    onActivateLicenseKey: (String) -> Unit,
    onRemoveLicenseKey: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedProductId by remember { mutableStateOf(SubscriptionStatus.PRODUCT_MONTHLY) }
    var licenseInput by remember { mutableStateOf("") }
    var copiedDeviceId by remember { mutableStateOf(false) }
    var whatsAppError by remember { mutableStateOf<String?>(null) }

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
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DanteHeroBannerCard(
                drawableRes = R.drawable.dante_payment_banner,
                aspectRatio = 16f / 9f,
                showBottomVignette = false,
                modifier = Modifier.testTag("paywall_payment_banner"),
            )

            // Header with Dante Avatar & PRO Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dante_avatar),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, CrimsonNeon, CircleShape),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "DANTE FARM IDLE PRO",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Surface(
                            color = DeepCrimsonContainer,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, CrimsonNeon.copy(alpha = 0.85f)),
                        ) {
                            Text(
                                text = "PIX",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonGlow,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = "Diário R$ 2 • Semanal R$ 15 • Mensal R$ 30",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            // Current Subscription Status Banner
            Surface(
                color = when {
                    subscription.isPaidPro -> DeepEmeraldContainer.copy(alpha = 0.5f)
                    subscription.isTrialActive -> DeepCyanContainer.copy(alpha = 0.55f)
                    else -> CyberCardSurface
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    when {
                        subscription.isPaidPro -> EmeraldNeon
                        subscription.isTrialActive -> ElectricCyan
                        else -> AmberGold.copy(alpha = 0.55f)
                    },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = when {
                            subscription.isPaidPro -> Icons.Rounded.Verified
                            subscription.isTrialActive -> Icons.Rounded.Timer
                            else -> Icons.Rounded.Star
                        },
                        contentDescription = null,
                        tint = when {
                            subscription.isPaidPro -> EmeraldNeon
                            subscription.isTrialActive -> ElectricCyan
                            else -> AmberGold
                        },
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                subscription.isPaidPro -> "Assinatura PRO Ativa"
                                subscription.isTrialActive -> "Teste Grátis Diário Ativo (30 min/dia)"
                                else -> "Plano Gratuito (1 de 4 telas liberadas)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                        val detailText = when {
                            subscription.isPaidPro -> {
                                val remaining = subscription.remainingTimeLabel().orEmpty()
                                val formattedDate = subscription.expiresAtMs?.let { exp ->
                                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
                                        .format(Date(exp))
                                }.orEmpty()
                                val holder = subscription.holderName.orEmpty()
                                "$holder • $remaining (até $formattedDate)"
                            }
                            subscription.isTrialActive -> {
                                val remaining = subscription.remainingTimeLabel().orEmpty()
                                "4 contas liberadas para teste • $remaining"
                            }
                            subscription.canStartDailyTrial -> {
                                "Teste todas as 4 telas grátis por 30 min hoje ou assine via PIX."
                            }
                            else -> {
                                "Teste grátis de 30 min já usado hoje. Volta amanhã ou assine via PIX."
                            }
                        }
                        Text(
                            text = detailText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            // Daily Free Trial CTA Card when user has not activated paid PRO yet
            if (!subscription.isPaidPro) {
                Surface(
                    color = if (subscription.canStartDailyTrial) {
                        DeepCyanContainer.copy(alpha = 0.45f)
                    } else {
                        CyberCardSurface.copy(alpha = 0.55f)
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (subscription.canStartDailyTrial) ElectricCyan else CyberBorder,
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
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Teste Grátis • 30 minutos por dia",
                                style = MaterialTheme.typography.titleSmall,
                                color = ElectricCyan,
                            )
                        }
                        Text(
                            text = when {
                                subscription.isTrialActive -> {
                                    "Seu teste de 30 minutos de hoje está rodando agora nas 4 telas."
                                }
                                subscription.canStartDailyTrial -> {
                                    "Quer testar as 4 contas no seu celular antes de pagar? Libere agora 30 minutos gratuitos (renova todo dia)."
                                }
                                else -> {
                                    "Você já usou os 30 minutos gratuitos de hoje. Amanhã libera novamente ou assine um plano abaixo."
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        if (subscription.canStartDailyTrial) {
                            Button(
                                onClick = onStartDailyTrial,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("activate_daily_trial_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = ObsidianBg,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Ativar Teste Grátis de 30 Minutos",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                        }
                    }
                }
            }

            // PRO Benefits List
            Surface(
                color = CyberCardSurface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyberBorder),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BenefitRow("4 telas simultâneas isoladas para qualquer jogo web ou Idle")
                    BenefitRow("Modo 2º Plano contínuo (farma enquanto você usa YouTube)")
                    BenefitRow("Login salvo com usuário e senha em cada tela")
                    BenefitRow("Pulso Anti-AFK (45s) e Auto-Recarregar programável")
                }
            }

            // Section 1: Plan Selection & PIX via WhatsApp
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCode2,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "1. Escolha o Plano e Pague via PIX",
                        style = MaterialTheme.typography.titleSmall,
                        color = EmeraldNeon,
                    )
                }

                subscription.offers.forEach { offer ->
                    PlanOfferCard(
                        offer = offer,
                        selected = selectedProductId == offer.productId,
                        onSelect = {
                            selectedProductId = offer.productId
                            whatsAppError = null
                        },
                    )
                }

                val selectedOffer = subscription.offers.find { it.productId == selectedProductId }
                    ?: subscription.offers.last()

                Button(
                    onClick = {
                        whatsAppError = null
                        val url = SubscriptionStatus.buildWhatsAppPixUrl(
                            offer = selectedOffer,
                            deviceId = subscription.deviceId,
                        )
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching {
                            context.startActivity(intent)
                        }.onFailure {
                            whatsAppError = "Não foi possível abrir o WhatsApp. Chame no ${SubscriptionStatus.WHATSAPP_PHONE_DISPLAY}."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("whatsapp_pix_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldNeon,
                        contentColor = ObsidianBg,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Pagar ${selectedOffer.displayPrice} via PIX no WhatsApp",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                Text(
                    text = "Atendimento WhatsApp: ${SubscriptionStatus.WHATSAPP_PHONE_DISPLAY} • O plano de ${selectedOffer.displayPrice} e seu ID já vão preenchidos na mensagem.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                whatsAppError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonError,
                    )
                }
            }

            HorizontalDivider(color = CyberBorder)

            // Section 2: Activate License Key received after PIX
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "2. Já pagou o PIX? Ative sua Chave",
                    style = MaterialTheme.typography.titleSmall,
                    color = ElectricCyan,
                )

                // Device ID Card
                Surface(
                    color = ObsidianBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyberBorder),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "ID deste Aparelho",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                            Text(
                                text = subscription.deviceId,
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCyan,
                                modifier = Modifier.testTag("device_id_text"),
                            )
                        }
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("Dante Device ID", subscription.deviceId))
                                copiedDeviceId = true
                            },
                            modifier = Modifier.testTag("copy_device_id_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copiar ID do aparelho",
                                tint = if (copiedDeviceId) EmeraldNeon else TextPrimary,
                            )
                        }
                    }
                }
                if (copiedDeviceId) {
                    Text(
                        text = "ID ${subscription.deviceId} copiado!",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldNeon,
                    )
                }

                OutlinedTextField(
                    value = licenseInput,
                    onValueChange = { licenseInput = it },
                    label = { Text("Cole aqui a Chave recebida no WhatsApp (DF1...)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = ElectricCyan,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("license_key_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedLabelColor = ElectricCyan,
                        cursorColor = ElectricCyan,
                    ),
                )

                licenseFeedback?.let { feedback ->
                    val isError = feedback.contains("inválid", ignoreCase = true) ||
                        feedback.contains("expir", ignoreCase = true) ||
                        feedback.contains("pertence", ignoreCase = true) ||
                        feedback.contains("corromp", ignoreCase = true) ||
                        feedback.contains("ilegível", ignoreCase = true)
                    Text(
                        text = feedback,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) CrimsonError else EmeraldNeon,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { onActivateLicenseKey(licenseInput) },
                        enabled = licenseInput.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("activate_license_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = ObsidianBg,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Ativar Chave PRO",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }

                    if (subscription.source == SubscriptionSource.LICENSE_KEY) {
                        OutlinedButton(
                            onClick = onRemoveLicenseKey,
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                        ) {
                            Text("Remover", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = EmeraldNeon,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
        )
    }
}

@Composable
private fun PlanOfferCard(
    offer: SubscriptionPlanOffer,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        color = if (selected) DeepCrimsonContainer.copy(alpha = 0.72f) else CyberCardSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) CrimsonNeon else CyberBorder,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .testTag("plan_card_${offer.planCode.lowercase()}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = offer.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                    offer.badge?.let { badgeText ->
                        Surface(
                            color = if (offer.isHighlighted) DeepAmberContainer else ObsidianBg,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(
                                0.8.dp,
                                if (offer.isHighlighted) AmberGold.copy(alpha = 0.7f) else CyberBorder,
                            ),
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (offer.isHighlighted) AmberGold else ElectricCyan,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Text(
                    text = offer.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) EmeraldNeon.copy(alpha = 0.16f) else ObsidianBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = offer.displayPrice,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) EmeraldNeon else AmberGold,
                )
            }
        }
    }
}
