package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionScreen(viewModel: MilkMateViewModel, onBack: (() -> Unit)? = null) {
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()

    val isPro = business?.subscriptionPlan == "PRO"
    val expiryDateStr = remember(business?.subscriptionExpiresAt) {
        val exp = business?.subscriptionExpiresAt
        if (exp != null && exp > 0) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(exp))
        } else "N/A"
    }

    val individualCount = customers.count { it.type == "INDIVIDUAL" }
    val bulkCount = customers.count { it.type == "BULK_BUYER" }
    val supplierCount = customers.count { it.type == "SUPPLIER" }

    var checkoutPlan by remember { mutableStateOf<String?>(null) } // "MONTHLY" or "YEARLY"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onBack != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "MilkMate Pro Subscription",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Status Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isPro) FreshGoldLight else RoyalBlueLight
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isPro) "👑 PRO SUBSCRIPTION ACTIVE" else "FREE TIER ACTIVE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPro) Color(0xFFE65100) else RoyalBluePrimary
                        )
                        Text(
                            text = if (isPro) "Expires on $expiryDateStr • Unlimited entries unlocked" else "Standard free limits apply (25 Customers max)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = if (isPro) Icons.Default.Verified else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isPro) FreshGold else RoyalBluePrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Current Account Usage:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Individual Customers: $individualCount / ${if (isPro) "∞" else "25"}", fontSize = 12.5.sp)
                Text("• Bulk Buyers: $bulkCount / ${if (isPro) "∞" else "5"}", fontSize = 12.5.sp)
                Text("• Milk Suppliers: $supplierCount / ${if (isPro) "∞" else "5"}", fontSize = 12.5.sp)
            }
        }

        // 💳 Pro Monthly Plan Card (₹49)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MilkMate Pro Monthly",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Billed monthly • Cancel anytime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "₹49",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBluePrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ProFeatureItem("Unlimited Individual Customers (overcomes 25 limit)")
                ProFeatureItem("Unlimited Bulk Buyers & Farmers")
                ProFeatureItem("Unlimited Milk Suppliers & Collection Desk")
                ProFeatureItem("PDF & Excel Statements + WhatsApp Receipts")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { checkoutPlan = "MONTHLY" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("subscribe_monthly_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isPro) "Renew Monthly Plan (₹49)" else "Upgrade Monthly (₹49)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 🌟 Pro Yearly Plan Card (₹499 - SAVE 15%)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, DairyGreen),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MilkMate Pro Yearly",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = DairyGreenLight) {
                                Text(
                                    text = "SAVE 15%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DairyGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Full year of unlimited dairy scaling",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "₹499",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = DairyGreen
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ProFeatureItem("Unlimited Individual Customers & Khatas")
                ProFeatureItem("Unlimited Bulk Buyers & Village Suppliers")
                ProFeatureItem("12 Months Priority Cloud Sync & Backup")
                ProFeatureItem("24/7 Priority Support & Bluetooth Analyzer Integration")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { checkoutPlan = "YEARLY" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("subscribe_yearly_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isPro) "Renew Yearly Plan (₹499)" else "Upgrade Yearly (₹499)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 🏦 How Funds Credit Your Account & Settlement Explanation
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How Payment Settlement & Account Credits Work",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "• Razorpay Gateway: Payments collected via Razorpay (Cards, NetBanking, UPI) are automatically settled and deposited directly into your linked registered bank account within T+1 business days.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Direct UPI Transfer: 100% of funds are transferred instantly from buyer to your UPI ID with 0% processing fees.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Policy & Transparency Note
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "MilkMate Entitlement Promise",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Even if a subscription expires, existing customer records remain 100% accessible. Deliveries, billing, reports, and backup continue to work without disruption.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // 💳 Razorpay & Payment Gateway Checkout Dialog
    if (checkoutPlan != null) {
        val planName = if (checkoutPlan == "YEARLY") "MilkMate Pro Yearly" else "MilkMate Pro Monthly"
        val amountStr = if (checkoutPlan == "YEARLY") "₹499" else "₹49"
        val upiVpa = session.upiId.ifBlank { "milkmate.pay@upi" }

        RazorpayCheckoutDialog(
            planType = checkoutPlan!!,
            planName = planName,
            amountStr = amountStr,
            ownerUpiId = upiVpa,
            onDismiss = { checkoutPlan = null },
            onPaymentComplete = { method, txnId ->
                viewModel.upgradeToPro(
                    period = checkoutPlan!!,
                    providerTxnId = txnId,
                    paymentMethod = method
                )
                checkoutPlan = null
            }
        )
    }
}

@Composable
fun ProFeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = DairyGreen,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * 💳 Razorpay & Direct UPI Payment Gateway Dialog
 */
@Composable
fun RazorpayCheckoutDialog(
    planType: String,
    planName: String,
    amountStr: String,
    ownerUpiId: String,
    onDismiss: () -> Unit,
    onPaymentComplete: (method: String, txnId: String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Razorpay, 1: Direct UPI
    var enteredUtr by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBlueLight,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("MilkMate Secure Checkout", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("$planName • $amountStr", fontSize = 12.sp, color = RoyalBluePrimary, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab Selector: Razorpay vs Direct UPI
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = RoyalBluePrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("💳 Razorpay Gateway", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("📱 Direct UPI (0% Fee)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (selectedTab == 0) {
                    // Razorpay Gateway
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Razorpay Auto-Settlement Gateway", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                            Text(
                                text = "Supports GPay, PhonePe, Paytm, Credit/Debit Cards, NetBanking & Wallets. Funds are automatically deposited to your registered bank account in T+1 business days.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Button(
                                onClick = {
                                    isProcessing = true
                                    val simulatedTxnId = "pay_RZP${(100000..999999).random()}"
                                    onPaymentComplete("Razorpay Gateway", simulatedTxnId)
                                },
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("launch_razorpay_pay_btn")
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pay $amountStr via Razorpay", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    // Direct UPI Transfer
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, DairyGreen.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Direct Bank UPI Transfer (0% Fee)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }

                            Text(
                                text = "100% of payment credits directly to your bank account instantly with no gateway fees.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // UPI App Launcher Button
                            val numericAmount = if (planType == "YEARLY") "499" else "49"
                            val upiUriStr = "upi://pay?pa=$ownerUpiId&pn=MilkMatePro&am=$numericAmount&cu=INR&tn=MilkMate%20Pro%20Subscription"

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUriStr))
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        android.widget.Toast.makeText(context, "Open PhonePe/GPay to pay $amountStr to $ownerUpiId", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open PhonePe / GPay / Paytm ($amountStr)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            OutlinedTextField(
                                value = enteredUtr,
                                onValueChange = { if (it.length <= 12 && it.all { c -> c.isLetterOrDigit() }) enteredUtr = it },
                                label = { Text("12-Digit Transaction Ref / UTR *") },
                                placeholder = { Text("e.g. 314589201948") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("upi_utr_input")
                            )

                            Button(
                                onClick = {
                                    if (enteredUtr.length >= 6) {
                                        onPaymentComplete("Direct UPI", "UTR-$enteredUtr")
                                    }
                                },
                                enabled = enteredUtr.length >= 6,
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp).testTag("verify_upi_utr_btn")
                            ) {
                                Text("Submit UTR & Activate Pro", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
