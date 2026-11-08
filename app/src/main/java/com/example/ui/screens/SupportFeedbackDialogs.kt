package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserSession
import com.example.ui.theme.*
import java.net.URLEncoder

object SupportConfig {
    const val SUPPORT_EMAIL = "support.milkmate@gmail.com"
    const val SUPPORT_WHATSAPP_NUMBER = "918847881109"
    const val SUPPORT_PHONE_DISPLAY = "+91 8847881109"

    fun openWhatsApp(context: Context, message: String) {
        try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$SUPPORT_WHATSAPP_NUMBER&text=$encodedMsg")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openEmail(context: Context, subject: String, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$SUPPORT_EMAIL")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(intent, "Send Email via"))
        } catch (e: Exception) {
            Toast.makeText(context, "No email app found: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * 1. Helpdesk & Support Modal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportHelpModal(
    session: UserSession,
    businessName: String,
    onDismiss: () -> Unit,
    onOpenBugReport: () -> Unit,
    onOpenFeatureSuggest: () -> Unit,
    onClearDemoData: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = CircleShape, color = RoyalBlueLight, modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(24.dp))
                        }
                    }
                    Column {
                        Text("Dairy Help & Support", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("Instant assistance via WhatsApp & Email", fontSize = 11.5.sp, color = TextSecondary)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            // Contact Channels Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Official Support Desk", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RoyalBluePrimary)
                    
                    // WhatsApp Button
                    Button(
                        onClick = {
                            val msg = "Hello MilkMate Support Team,\nI need help with my dairy account:\n• Business: $businessName\n• Business ID: ${session.businessId}\n• Role: ${session.role}\n\nMy Query:"
                            SupportConfig.openWhatsApp(context, msg)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("support_whatsapp_btn")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chat on WhatsApp (${SupportConfig.SUPPORT_PHONE_DISPLAY})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Email Button
                    OutlinedButton(
                        onClick = {
                            val subject = "[Support Request] $businessName (ID: ${session.businessId})"
                            val body = "Hello MilkMate Support,\n\nI need assistance regarding:\n\n---\nAccount Details:\nBusiness: $businessName\nBusiness ID: ${session.businessId}\nRole: ${session.role}\nDevice: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
                            SupportConfig.openEmail(context, subject, body)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("support_email_btn")
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Email Support (${SupportConfig.SUPPORT_EMAIL})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBluePrimary)
                    }
                }
            }

            // Quick Actions: Report Bug & Suggest Feature
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenBugReport()
                    },
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report Bug", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenFeatureSuggest()
                    },
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = FreshGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Suggest Feature", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                }
            }

            // Frequently Asked Questions
            Text("Frequently Asked Dairy Questions:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FaqItem("Deliveries", "Select Morning/Evening shift and tap Enter or +/- 0.5L on each customer row.")
                FaqItem("WhatsApp Invoicing", "Open Customer Ledger and tap 'Send Bill' to share receipt directly with the customer.")
                FaqItem("Milk Production Table", "Enter Morning & Evening liters per cattle; totals calculate automatically in real time.")
                FaqItem("Offline Backup", "All entries are saved locally first on your device and will sync when internet is connected.")
            }

            // Clean Demo Data Button
            OutlinedButton(
                onClick = onClearDemoData,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🧹 Clear All Sample / Test Data", color = DangerRed, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FaqItem(topic: String, answer: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("• $topic:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
            Text(answer, fontSize = 10.5.sp, color = TextSecondary)
        }
    }
}

/**
 * 2. Report a Bug Dialog with Auto-Diagnostics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBugDialog(
    session: UserSession,
    businessName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var bugTitle by remember { mutableStateOf("") }
    var bugDescription by remember { mutableStateOf("") }
    var selectedModule by remember { mutableStateOf("Deliveries & Shifts") }
    val modules = listOf("Deliveries & Shifts", "Milk Entry Table", "Payments & Khata", "Herd & Breeding", "Inventory & Feed", "Reports & P&L", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = DangerRed)
                Text("Report a Bug", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text("Describe what happened so our engineering team can fix it quickly:", fontSize = 12.sp, color = TextSecondary)
                }

                item {
                    Text("Affected Module", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        modules.chunked(2).forEach { rowMods ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowMods.forEach { mod ->
                                    val isSel = selectedModule == mod
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSel) DangerRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) DangerRed else Color.Transparent),
                                        modifier = Modifier.weight(1f).clickable { selectedModule = mod }
                                    ) {
                                        Text(
                                            text = mod,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) DangerRed else TextPrimary,
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = bugTitle,
                        onValueChange = { bugTitle = it },
                        label = { Text("Issue Title (Short Summary)") },
                        placeholder = { Text("e.g. Milk total not updating in table") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = bugDescription,
                        onValueChange = { bugDescription = it },
                        label = { Text("What happened / Steps to reproduce") },
                        placeholder = { Text("Describe the error or unexpected behavior...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Attached System Diagnostics:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TextPrimary)
                            Text("• Business: $businessName (ID: ${session.businessId})", fontSize = 9.5.sp, color = TextSecondary)
                            Text("• Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})", fontSize = 9.5.sp, color = TextSecondary)
                            Text("• App: MilkMate Dairy OS v2.0", fontSize = 9.5.sp, color = TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Submit via WhatsApp
                Button(
                    onClick = {
                        if (bugTitle.isBlank()) {
                            Toast.makeText(context, "Please enter an issue title", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val msg = "🐞 *BUG REPORT - MilkMate*\n" +
                                "• *Module:* $selectedModule\n" +
                                "• *Title:* $bugTitle\n" +
                                "• *Description:* $bugDescription\n\n" +
                                "📱 *Device Info:*\n" +
                                "• Business: $businessName (ID: ${session.businessId})\n" +
                                "• Phone: ${session.registeredPhone}\n" +
                                "• Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})\n" +
                                "• App Version: MilkMate v2.0"
                        SupportConfig.openWhatsApp(context, msg)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 11.sp)
                }

                // Submit via Email
                Button(
                    onClick = {
                        if (bugTitle.isBlank()) {
                            Toast.makeText(context, "Please enter an issue title", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val subject = "[BUG REPORT] [$selectedModule] $bugTitle - $businessName"
                        val body = "BUG REPORT:\n" +
                                "Module: $selectedModule\n" +
                                "Title: $bugTitle\n" +
                                "Description:\n$bugDescription\n\n" +
                                "-------------------------------------\n" +
                                "SYSTEM DIAGNOSTICS:\n" +
                                "Business Name: $businessName\n" +
                                "Business ID: ${session.businessId}\n" +
                                "User Role: ${session.role}\n" +
                                "User Phone: ${session.registeredPhone}\n" +
                                "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                                "Android OS: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n" +
                                "App Version: MilkMate Dairy OS v2.0"
                        SupportConfig.openEmail(context, subject, body)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Email", fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * 3. Suggest a Feature Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestFeatureDialog(
    session: UserSession,
    businessName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var featureTitle by remember { mutableStateOf("") }
    var featureDetails by remember { mutableStateOf("") }
    var targetArea by remember { mutableStateOf("Herd Management") }
    val areas = listOf("Herd Management", "Deliveries & Routes", "Farmer Procurement", "Reports & Accounts", "Billing & Payments", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = FreshGold)
                Text("Suggest a Feature", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("What new feature or workflow would make your dairy farm operations easier?", fontSize = 12.sp, color = TextSecondary)

                Text("Target Area", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    areas.chunked(2).forEach { rowAreas ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowAreas.forEach { area ->
                                val isSel = targetArea == area
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) RoyalBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RoyalBluePrimary else Color.Transparent),
                                    modifier = Modifier.weight(1f).clickable { targetArea = area }
                                ) {
                                    Text(
                                        text = area,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) RoyalBluePrimary else TextPrimary,
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = featureTitle,
                    onValueChange = { featureTitle = it },
                    label = { Text("Feature Title") },
                    placeholder = { Text("e.g. Export cattle vaccination card as PDF") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = featureDetails,
                    onValueChange = { featureDetails = it },
                    label = { Text("How should this work & how it helps") },
                    placeholder = { Text("Describe the feature and your daily routine...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Submit via WhatsApp
                Button(
                    onClick = {
                        if (featureTitle.isBlank()) {
                            Toast.makeText(context, "Please enter a feature title", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val msg = "💡 *FEATURE SUGGESTION - MilkMate*\n" +
                                "• *Area:* $targetArea\n" +
                                "• *Feature:* $featureTitle\n" +
                                "• *Details:* $featureDetails\n\n" +
                                "🏢 *Dairy Profile:*\n" +
                                "• Business: $businessName (ID: ${session.businessId})\n" +
                                "• Owner Phone: ${session.registeredPhone}"
                        SupportConfig.openWhatsApp(context, msg)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 11.sp)
                }

                // Submit via Email
                Button(
                    onClick = {
                        if (featureTitle.isBlank()) {
                            Toast.makeText(context, "Please enter a feature title", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val subject = "[FEATURE SUGGESTION] [$targetArea] $featureTitle - $businessName"
                        val body = "FEATURE SUGGESTION:\n" +
                                "Area: $targetArea\n" +
                                "Title: $featureTitle\n\n" +
                                "Description & Use Case:\n$featureDetails\n\n" +
                                "-------------------------------------\n" +
                                "SUBMITTER DETAILS:\n" +
                                "Business Name: $businessName\n" +
                                "Business ID: ${session.businessId}\n" +
                                "Phone: ${session.registeredPhone}\n" +
                                "Role: ${session.role}"
                        SupportConfig.openEmail(context, subject, body)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Email", fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
