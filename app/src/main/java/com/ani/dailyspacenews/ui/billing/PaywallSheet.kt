package com.ani.dailyspacenews.ui.billing

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ani.dailyspacenews.billing.BillingRepository
import com.ani.dailyspacenews.ui.components.ObservatoryCard
import com.ani.dailyspacenews.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    billingRepository: BillingRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val formattedPrice by billingRepository.formattedPrice.collectAsState()
    val isPremium by billingRepository.isPremiumUser.collectAsState()
    val statusMessage by billingRepository.billingStatusMessage.collectAsState()

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            billingRepository.clearStatusMessage()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgElevated2,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderHairline) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "OBSERVATORY PRO",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentAmber,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Unrestricted Cosmic Access",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Enhance your daily exploration with high-fidelity instruments and an uninterrupted telescope feed.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Value Propositions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BenefitItem(
                    icon = Icons.Outlined.Block,
                    title = "Ad-Free Exploration",
                    description = "Zero banners, interstitials, or video interruptions."
                )
                BenefitItem(
                    icon = Icons.Outlined.HighQuality,
                    title = "Full-Resolution NASA Imagery",
                    description = "Access uncompressed lossless HD deep-sky telescope photos."
                )
                BenefitItem(
                    icon = Icons.Outlined.Public,
                    title = "Support Space Journalism",
                    description = "Directly support ongoing live rocket launch & event telemetry."
                )
            }

            Spacer(Modifier.height(24.dp))

            if (isPremium) {
                ObservatoryCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = BgElevated,
                    borderColor = SemanticSuccess
                ) {
                    Text(
                        text = "PRO MEMBERSHIP ACTIVE",
                        style = MaterialTheme.typography.labelMedium,
                        color = SemanticSuccess,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Pricing & CTA
                Text(
                    text = formattedPrice,
                    style = TelemetryMonoStyle,
                    fontSize = 18.sp,
                    color = AccentAmber
                )
                Text(
                    text = "Cancel anytime in Google Play Store",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Button(
                    onClick = {
                        activity?.let { billingRepository.launchBillingFlow(it) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentAmber,
                        contentColor = BgBase
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "UPGRADE TO PRO",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Restore Purchases Button (Mandatory for Play Store Policy)
                TextButton(
                    onClick = {
                        billingRepository.restorePurchases { restored ->
                            val msg = if (restored) "Subscription restored successfully!" else "No active subscriptions found."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(
                        text = "Restore Purchases",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BenefitItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgElevated, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = BgElevated2,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
