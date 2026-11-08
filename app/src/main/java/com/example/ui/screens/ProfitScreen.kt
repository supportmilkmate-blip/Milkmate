package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.MilkWastageEntity
import com.example.data.repository.ProfitReport
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.ReportExportHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitScreen(
    viewModel: MilkMateViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentRange by viewModel.selectedReportRange.collectAsStateWithLifecycle()
    val customDateRange by viewModel.customProfitDateRange.collectAsStateWithLifecycle()
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val allDeliveries by viewModel.allDeliveries.collectAsStateWithLifecycle()
    val milkWastageList by viewModel.milkWastageList.collectAsStateWithLifecycle()
    val inventoryTransactions by viewModel.inventoryTransactions.collectAsStateWithLifecycle()

    val supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH"
    val isCowEnabled = BusinessModeFeatures.isCowEnabled(supportedMilkTypes)
    val isBuffaloEnabled = BusinessModeFeatures.isBuffaloEnabled(supportedMilkTypes)
    val isCowOnly = isCowEnabled && !isBuffaloEnabled
    val isBuffaloOnly = isBuffaloEnabled && !isCowEnabled

    var calcBasis by remember { mutableStateOf("BASE_2") } // BASE_1: Gross, BASE_2: Operating EBITDA, BASE_3: Cashflow
    var profitReport by remember { mutableStateOf<ProfitReport?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Dialog state for custom date range
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var tempStartDate by remember { mutableStateOf(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L) }
    var tempEndDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var customRangeError by remember { mutableStateOf<String?>(null) }

    // Drill-down sheet state
    var selectedDrillDownCategory by remember { mutableStateOf<String?>(null) }

    // Automatically recalculate report on period or data changes
    LaunchedEffect(currentRange, business, customDateRange, allDeliveries, expenses, payments, milkWastageList) {
        isLoading = true
        profitReport = viewModel.getProfitReport()
        isLoading = false
    }

    val report = profitReport ?: ProfitReport(
        startDate = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L,
        endDate = System.currentTimeMillis(),
        milkSalesRevenue = 0.0,
        farmMilkProductionValue = 0.0,
        otherSalesRevenue = 0.0,
        totalRevenue = 0.0,
        milkPurchaseCost = 0.0,
        inventoryPurchaseCost = 0.0,
        feedCost = 0.0,
        veterinaryCost = 0.0,
        breedingCost = 0.0,
        laborCost = 0.0,
        totalPurchaseCost = 0.0,
        operatingExpenses = 0.0,
        personalExpenses = 0.0,
        totalCost = 0.0,
        base1Profit = 0.0,
        base2Profit = 0.0,
        base3Profit = 0.0,
        incomeOverFeedCost = 0.0,
        totalMilkLiters = 0.0,
        costPerLiter = 0.0,
        revenuePerLiter = 0.0,
        profitPerLiter = 0.0,
        feedCostPerLiter = 0.0,
        netProfit = 0.0,
        profitMarginPercent = 0.0,
        expenseBreakdown = emptyMap(),
        cowLiters = 0.0,
        cowRevenue = 0.0,
        buffaloLiters = 0.0,
        buffaloRevenue = 0.0,
        costOfProductionPerLiter = 0.0,
        breakEvenRatePerLiter = 0.0
    )

    val displayedProfit = when (calcBasis) {
        "BASE_1" -> report.base1Profit
        "BASE_3" -> report.base3Profit
        else -> report.base2Profit
    }

    val isProfitable = displayedProfit >= 0
    val heroColor = if (isProfitable) ForestGreenAccent else DangerRed

    val animalLabel = when {
        isCowOnly -> "Cow Milk Dairy"
        isBuffaloOnly -> "Buffalo Milk Dairy"
        else -> "Dairy Farm"
    }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val mode = business?.businessMode ?: "FARMER"
    val showFeed = BusinessModeFeatures.showCattleFeed(mode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Top Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "$animalLabel • Financial Performance".uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RoyalBluePrimary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Farm Profit & Loss",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isProfitable) DairyGreenLight else Color(0xFFFFEBEE)
            ) {
                Text(
                    text = if (isProfitable) "🟢 Profitable" else "🔴 Deficit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isProfitable) DairyGreen else DangerRed,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // --- 2. Accounting Period Selector with Minimum 1-Month Custom Picker ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Accounting Period", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    }
                    Text(
                        text = "${sdf.format(Date(report.startDate))} – ${sdf.format(Date(report.endDate))}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }

                // Period Chips (Minimum 1 Month compliant intervals)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "THIS_MONTH" to "This Month",
                        "LAST_MONTH" to "Last Month",
                        "LAST_30_DAYS" to "Last 30 Days",
                        "QUARTER_3M" to "3 Months",
                        "THIS_FY" to "This FY"
                    ).forEach { (key, label) ->
                        val isSel = currentRange == key && customDateRange == null
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.setCustomProfitDateRange(null)
                                    viewModel.setReportRange(key)
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else TextPrimary,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Custom Date Range Button
                val isCustomActive = currentRange == "CUSTOM" || customDateRange != null
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCustomActive) RoyalBlueLight else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isCustomActive) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            tempStartDate = report.startDate
                            tempEndDate = report.endDate
                            customRangeError = null
                            showCustomDateDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (isCustomActive) RoyalBluePrimary else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCustomActive) "Custom Range (Active): ${sdf.format(Date(report.startDate))} to ${sdf.format(Date(report.endDate))}" else "Select Custom Date Range (≥ 1 Month)",
                            fontSize = 11.sp,
                            fontWeight = if (isCustomActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCustomActive) RoyalBluePrimary else TextSecondary
                        )
                    }
                }
            }
        }

        // --- 3. Prominent Download PDF & Export Excel Buttons ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // PDF Download Button
                Button(
                    onClick = {
                        val periodStr = "${sdf.format(Date(report.startDate))} to ${sdf.format(Date(report.endDate))}"
                        val headers = listOf("Financial Stream", "Particulars & Description", "Volume (L)", "Rate (₹/L)", "Total Amount (₹)", "Flow Type")
                        val rows = listOf(
                            listOf("REVENUE", "Milk Sales & Customer Dispatches", "${report.totalMilkLiters} L", "₹${report.revenuePerLiter}/L", "₹${report.totalRevenue.toInt()}", "Inflow (+)"),
                            listOf("REVENUE SPLIT", "Cow Milk Sales", "${report.cowLiters} L", "₹${if (report.cowLiters > 0) String.format(Locale.US, "%.1f", report.cowRevenue / report.cowLiters) else "0.0"}/L", "₹${report.cowRevenue.toInt()}", "Inflow (+)"),
                            listOf("REVENUE SPLIT", "Buffalo Milk Sales", "${report.buffaloLiters} L", "₹${if (report.buffaloLiters > 0) String.format(Locale.US, "%.1f", report.buffaloRevenue / report.buffaloLiters) else "0.0"}/L", "₹${report.buffaloRevenue.toInt()}", "Inflow (+)"),
                            listOf("DIRECT COSTS", "Farmer Milk Procurement Outlay", "-", "-", "₹${report.milkPurchaseCost.toInt()}", "Outflow (-)"),
                            listOf("HERD FEED", "Cattle Feed & Consumables Consumption", "-", "-", "₹${report.actualFeedConsumptionCost.toInt()}", "Outflow (-)"),
                            listOf("HEALTHCARE", "Veterinary, Medicine & Deworming", "-", "-", "₹${report.veterinaryCost.toInt()}", "Outflow (-)"),
                            listOf("BREEDING", "Artificial Insemination & Semen Straws", "-", "-", "₹${report.breedingCost.toInt()}", "Outflow (-)"),
                            listOf("LABOR", "Farm Labor & Milker Salaries", "-", "-", "₹${report.laborCost.toInt()}", "Outflow (-)"),
                            listOf("OVERHEADS", "Electricity, Fuel & Shed Upkeep", "-", "-", "₹${(report.operatingExpenses - report.veterinaryCost - report.breedingCost - report.laborCost).coerceAtLeast(0.0).toInt()}", "Outflow (-)"),
                            listOf("SPOILAGE", "Milk Spillage & Testing Loss", "${report.milkWastageLiters} L", "-", "₹${report.milkWastageCost.toInt()}", "Outflow (-)"),
                            listOf("PROFIT SUMMARY", "NET FARM OPERATING PROFIT (EBITDA)", "${report.totalMilkLiters} L", "₹${report.profitPerLiter}/L", "₹${displayedProfit.toInt()}", if (displayedProfit >= 0) "SURPLUS" else "DEFICIT")
                        )
                        val summary = listOf(
                            "Net Profit" to "₹${displayedProfit.toInt()}",
                            "Profit Margin" to "${report.profitMarginPercent}%",
                            "Cost of Prod (COPL)" to "₹${report.costOfProductionPerLiter}/L",
                            "Break-Even Price" to "₹${report.breakEvenRatePerLiter}/L"
                        )
                        ReportExportHelper.generateAndSharePdf(context, "Dairy Farm Profit & Loss Statement", periodStr, headers, rows, summary)
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Excel Export Button
                Button(
                    onClick = {
                        val headers = listOf("Financial Category", "Line Item Description", "Volume (L)", "Unit Realization (₹/L)", "Total Amount (₹)", "P&L Impact")
                        val rows = listOf(
                            listOf("REVENUE", "Milk Sales & Customer Dispatches", "${report.totalMilkLiters} L", "₹${report.revenuePerLiter}/L", "₹${report.totalRevenue.toInt()}", "Inflow (+)"),
                            listOf("REVENUE SPLIT", "Cow Milk Deliveries & Counter Sales", "${report.cowLiters} L", "₹${if (report.cowLiters > 0) String.format(Locale.US, "%.1f", report.cowRevenue / report.cowLiters) else "0.0"}/L", "₹${report.cowRevenue.toInt()}", "Inflow (+)"),
                            listOf("REVENUE SPLIT", "Buffalo Milk Deliveries & Counter Sales", "${report.buffaloLiters} L", "₹${if (report.buffaloLiters > 0) String.format(Locale.US, "%.1f", report.buffaloRevenue / report.buffaloLiters) else "0.0"}/L", "₹${report.buffaloRevenue.toInt()}", "Inflow (+)"),
                            listOf("DIRECT COSTS", "Farmer Milk Sourcing / Supplier Intake", "${report.totalMilkLiters} L", "-", "₹${report.milkPurchaseCost.toInt()}", "Outflow (-)"),
                            listOf("HERD FEED", "Cattle Feed, Fodder & Silage Consumption", "-", "-", "₹${report.actualFeedConsumptionCost.toInt()}", "Outflow (-)"),
                            listOf("HEALTHCARE", "Veterinary, Meds, Deworming & Vaccination", "-", "-", "₹${report.veterinaryCost.toInt()}", "Outflow (-)"),
                            listOf("BREEDING", "Artificial Insemination & Semen Straws", "-", "-", "₹${report.breedingCost.toInt()}", "Outflow (-)"),
                            listOf("LABOR", "Milker Wages & Farm Labor Salaries", "-", "-", "₹${report.laborCost.toInt()}", "Outflow (-)"),
                            listOf("OVERHEADS", "Electricity, Fuel, Repairs & Shed Upkeep", "-", "-", "₹${(report.operatingExpenses - report.veterinaryCost - report.breedingCost - report.laborCost).coerceAtLeast(0.0).toInt()}", "Outflow (-)"),
                            listOf("LOSS AUDIT", "Milk Spoilage & Quality Testing Losses", "${report.milkWastageLiters} L", "-", "₹${report.milkWastageCost.toInt()}", "Outflow (-)"),
                            listOf("NET SUMMARY", "NET FARM OPERATING PROFIT (EBITDA)", "${report.totalMilkLiters} L", "₹${report.profitPerLiter}/L", "₹${displayedProfit.toInt()}", if (displayedProfit >= 0) "SURPLUS" else "DEFICIT")
                        )
                        ReportExportHelper.generateAndShareExcel(context, "Dairy_Farm_Profit_Loss_Statement", headers, rows)
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Excel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // --- 4. Main Profit Hero Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isProfitable) Color(0xFF1B5E20) else Color(0xFFB71C1C)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (calcBasis) {
                                "BASE_1" -> "GROSS MARGIN (SALES − PURCHASES)"
                                "BASE_3" -> "CASH FLOW PROFIT (COLLECTIONS − OUTFLOW)"
                                else -> "NET FARM OPERATING PROFIT (EBITDA)"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = String.format(Locale.US, "₹%,.0f", displayedProfit),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isProfitable) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Profit Margin", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(
                            text = "${report.profitMarginPercent}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Revenue per L", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(
                            text = "₹${String.format(Locale.US, "%.1f", report.revenuePerLiter)}/L",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Net Profit per L", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(
                            text = "₹${String.format(Locale.US, "%.1f", report.profitPerLiter)}/L",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // --- 5. Visual Revenue vs Expenses Proportional Split Chart ---
        val totalVolume = report.totalRevenue + report.totalCost
        val revenueRatio = if (totalVolume > 0) (report.totalRevenue / totalVolume).toFloat() else 0.5f

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Visual Distribution: Revenue vs Expenses", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Text(
                        text = if (report.totalCost > 0) "${((report.totalRevenue / maxOf(1.0, report.totalCost)) * 100).toInt()}% Cost Coverage" else "100% Retained",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isProfitable) DairyGreen else DangerRed
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Inflow: ₹${report.totalRevenue.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DairyGreen)
                    Text("Outflow: ₹${report.totalCost.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
                }

                LinearProgressIndicator(
                    progress = { revenueRatio },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = DairyGreen,
                    trackColor = DangerRed
                )

                // Cost Category Distribution Breakdown Badges
                val feedCostVal = if (report.feedCost > 0) report.feedCost else report.actualFeedConsumptionCost
                val costItems = listOf(
                    "Feed" to feedCostVal,
                    "Labor" to report.laborCost,
                    "Vet/Meds" to report.veterinaryCost,
                    "Breeding" to report.breedingCost,
                    "Overhead" to (report.operatingExpenses - report.veterinaryCost - report.breedingCost - report.laborCost).coerceAtLeast(0.0)
                ).filter { it.second > 0 }

                if (costItems.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        costItems.forEach { (lbl, amt) ->
                            val pct = if (report.totalCost > 0) ((amt / report.totalCost) * 100).toInt() else 0
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(lbl, fontSize = 9.sp, color = TextSecondary, maxLines = 1)
                                    Text("₹${amt.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                                    Text("$pct%", fontSize = 9.sp, color = RoyalBluePrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 6. Cow vs Buffalo Profit & Margin Split Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CompareArrows, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cow vs. Buffalo Profit & Realization Split", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Cow Milk Block
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🐄 COW MILK", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = Color(0xFF1976D2))
                            Text("Vol: ${String.format(Locale.US, "%.1f", report.cowLiters)} L", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Sales: ₹${report.cowRevenue.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                            val cowRealization = if (report.cowLiters > 0) report.cowRevenue / report.cowLiters else 0.0
                            Text("Avg: ₹${String.format(Locale.US, "%.1f", cowRealization)}/L", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    // Buffalo Milk Block
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF388E3C).copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🐃 BUFFALO MILK", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = Color(0xFF388E3C))
                            Text("Vol: ${String.format(Locale.US, "%.1f", report.buffaloLiters)} L", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Sales: ₹${report.buffaloRevenue.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                            val bufRealization = if (report.buffaloLiters > 0) report.buffaloRevenue / report.buffaloLiters else 0.0
                            Text("Avg: ₹${String.format(Locale.US, "%.1f", bufRealization)}/L", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // --- 7. Cost of Production per Liter (COPL) & Break-Even Calculator Card ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("copl_break_even_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, WarmHoney.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = FreshGoldLight, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Cost of Production & Break-Even Rate", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text("Breakeven threshold needed to avoid losses", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Cost of Prod (COPL)", fontSize = 10.sp, color = TextSecondary)
                        Text("₹${String.format(Locale.US, "%.1f", report.costOfProductionPerLiter)}/L", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Break-Even Price", fontSize = 10.sp, color = TextSecondary)
                        Text("₹${String.format(Locale.US, "%.1f", report.breakEvenRatePerLiter)}/L", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val safetyMargin = report.revenuePerLiter - report.breakEvenRatePerLiter
                        Text("Safety Buffer", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            text = "${if (safetyMargin >= 0) "+" else ""}₹${String.format(Locale.US, "%.1f", safetyMargin)}/L",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (safetyMargin >= 0) DairyGreen else DangerRed
                        )
                    }
                }
            }
        }

        // --- 8. Golden Metric: Income Over Feed Cost (IOFC) ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("profit_key_metric_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = if (showFeed) FreshGoldLight else RoyalBlueLight, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (showFeed) Icons.Default.Grass else Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = if (showFeed) WarmHoney else RoyalBluePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            val metricTitle = when (mode) {
                                "TRADER" -> "Gross Trading Margin (Spread)"
                                "COLLECTION_CENTER" -> "Center Chilling Spread"
                                else -> "Income Over Feed Cost (IOFC)"
                            }
                            val metricSubtitle = when (mode) {
                                "TRADER" -> "Milk Sales Revenue − Farmer Collection Cost"
                                "COLLECTION_CENTER" -> "Bulk Tanker Invoicing − Farmer Payouts"
                                else -> "Primary Dairy Producer Profit Indicator"
                            }
                            Text(metricTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text(metricSubtitle, fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    val mainMetricVal = if (showFeed) report.incomeOverFeedCost else (report.totalRevenue - report.milkPurchaseCost)
                    Text(
                        text = String.format(Locale.US, "₹%,.0f", mainMetricVal),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (mainMetricVal >= 0) DairyGreen else DangerRed
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                if (showFeed) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val feedRatio = if (report.totalRevenue > 0) ((report.feedCost / report.totalRevenue) * 100).toInt() else 0
                        Column {
                            Text("Feed Cost / Liter", fontSize = 10.sp, color = TextSecondary)
                            Text("₹${String.format(Locale.US, "%.1f", report.feedCostPerLiter)} / L", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Feed % of Milk Sales", fontSize = 10.sp, color = TextSecondary)
                            Text("$feedRatio%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (feedRatio <= 55) DairyGreen else DangerRed)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Benchmark Target", fontSize = 10.sp, color = TextSecondary)
                            Text("< 50% ideal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // --- 9. Calculation Accounting Model Basis Switcher ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Calculation Accounting Model:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "BASE_2" to "Net Operating (All Exp)",
                        "BASE_1" to if (showFeed) "Gross (Sales − Feed)" else "Gross (Sales − Stock)",
                        "BASE_3" to "Cash Flow (In − Out)"
                    ).forEach { (code, label) ->
                        val isSel = calcBasis == code
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) RoyalBlueLight else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { calcBasis = code }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) RoyalBluePrimary else TextSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // --- 10. Comprehensive Clickable Revenue & Cost Sheet ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Comprehensive Revenue & Cost Sheet", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Text("Tap row to inspect", fontSize = 10.sp, color = RoyalBluePrimary, fontWeight = FontWeight.SemiBold)
                }

                // 1. Incomes - Milk Sales
                ClickableProfitLine(
                    icon = Icons.Default.WaterDrop,
                    title = "Milk Sales & Dispatches (${String.format(Locale.US, "%.1f", report.totalMilkLiters)}L)",
                    value = String.format(Locale.US, "+₹%,.0f", report.milkSalesRevenue),
                    valueColor = DairyGreen,
                    isPositive = true,
                    onClick = { selectedDrillDownCategory = "MILK_SALES" }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                if (BusinessModeFeatures.showHerdBreeding(mode)) {
                    // Cattle Farm Producer Breakdown
                    // 2. Feed Cost
                    ClickableProfitLine(
                        icon = Icons.Default.Grass,
                        title = "Cattle Feed & Consumables (Actual Consumption)",
                        value = String.format(Locale.US, "-₹%,.0f", report.actualFeedConsumptionCost),
                        valueColor = if (report.actualFeedConsumptionCost > 0) DangerRed else TextSecondary,
                        isPositive = false,
                        onClick = { selectedDrillDownCategory = "FEED_CONSUMABLES" }
                    )

                    // 3. Veterinary & Meds
                    ClickableProfitLine(
                        icon = Icons.Default.MedicalServices,
                        title = "Veterinary, Meds & Deworming",
                        value = String.format(Locale.US, "-₹%,.0f", report.veterinaryCost),
                        valueColor = if (report.veterinaryCost > 0) DangerRed else TextSecondary,
                        isPositive = false,
                        onClick = { selectedDrillDownCategory = "VET_MEDS" }
                    )

                    // 4. Breeding & A.I.
                    ClickableProfitLine(
                        icon = Icons.Default.Pets,
                        title = "Breeding & A.I. Semen Straws",
                        value = String.format(Locale.US, "-₹%,.0f", report.breedingCost),
                        valueColor = if (report.breedingCost > 0) DangerRed else TextSecondary,
                        isPositive = false,
                        onClick = { selectedDrillDownCategory = "BREEDING_AI" }
                    )

                    // 5. Labor & Milker
                    ClickableProfitLine(
                        icon = Icons.Default.People,
                        title = "Farm Labor & Milker Wages",
                        value = String.format(Locale.US, "-₹%,.0f", report.laborCost),
                        valueColor = if (report.laborCost > 0) DangerRed else TextSecondary,
                        isPositive = false,
                        onClick = { selectedDrillDownCategory = "LABOR_WAGES" }
                    )

                    // 6. Milk Wastage / Spoilage Loss
                    if (report.milkWastageCost > 0) {
                        ClickableProfitLine(
                            icon = Icons.Default.DeleteSweep,
                            title = "Milk Wastage & Spoilage Loss (${String.format(Locale.US, "%.1f", report.milkWastageLiters)}L)",
                            value = String.format(Locale.US, "-₹%,.0f", report.milkWastageCost),
                            valueColor = DangerRed,
                            isPositive = false,
                            onClick = { selectedDrillDownCategory = "MILK_WASTAGE" }
                        )
                    }

                    // 7. Other Farm Overheads
                    val otherExp = (report.operatingExpenses - report.veterinaryCost - report.breedingCost - report.laborCost).coerceAtLeast(0.0)
                    ClickableProfitLine(
                        icon = Icons.Default.ElectricBolt,
                        title = "Electricity, Diesel & Shed Maintenance",
                        value = String.format(Locale.US, "-₹%,.0f", otherExp),
                        valueColor = if (otherExp > 0) DangerRed else TextSecondary,
                        isPositive = false,
                        onClick = { selectedDrillDownCategory = "MAINTENANCE_UTILITIES" }
                    )
                } else {
                    // Trader / Aggregator / Retail Breakdown
                    // 2. Farmer Milk Collection Outlay
                    ClickableProfitLine(
                        icon = Icons.Default.WaterDrop,
                        title = "Farmer Milk Collection Outlay",
                        value = String.format(Locale.US, "-₹%,.0f", report.milkPurchaseCost),
                        valueColor = if (report.milkPurchaseCost > 0) DangerRed else TextSecondary,
                        isPositive = false,
                        onClick = { selectedDrillDownCategory = "FARMER_PROCUREMENT" }
                    )

                    // 3. Milk Wastage / Spoilage Loss
                    if (report.milkWastageCost > 0) {
                        ClickableProfitLine(
                            icon = Icons.Default.DeleteSweep,
                            title = "Milk Spoilage / Wastage Loss (${String.format(Locale.US, "%.1f", report.milkWastageLiters)}L)",
                            value = String.format(Locale.US, "-₹%,.0f", report.milkWastageCost),
                            valueColor = DangerRed,
                            isPositive = false,
                            onClick = { selectedDrillDownCategory = "MILK_WASTAGE" }
                        )
                    }

                    // 4. Logistics & Fuel
                    ClickableProfitLine(
                        icon = Icons.Default.LocalShipping,
                        title = "Transport, Route Fuel & Delivery Freight",
                        value = String.format(Locale.US, "-₹%,.0f", report.operatingExpenses * 0.4),
                        valueColor = if (report.operatingExpenses > 0) DangerRed else TextSecondary,
                        isPositive = false,
                        onClick = { selectedDrillDownCategory = "MAINTENANCE_UTILITIES" }
                    )

                    // 5. Staff & Helper Salaries
                    ClickableProfitLine(
                        icon = Icons.Default.People,
                        title = "Staff, Drivers & Center Helpers",
                        value = String.format(Locale.US, "-₹%,.0f", report.laborCost),
                        valueColor = if (report.laborCost > 0) DangerRed else TextSecondary,
                        isPositive = false,
                        onClick = { selectedDrillDownCategory = "LABOR_WAGES" }
                    )

                    // 6. Rent & Equipment Maintenance
                    val tradeOverheads = (report.operatingExpenses * 0.6 - report.laborCost).coerceAtLeast(0.0)
                    ClickableProfitLine(
                        icon = Icons.Default.Build,
                        title = "Can Washing, Rent & Maintenance",
                        value = String.format(Locale.US, "-₹%,.0f", tradeOverheads),
                        valueColor = if (tradeOverheads > 0) DangerRed else TextSecondary,
                        isPositive = false,
                        onClick = { selectedDrillDownCategory = "MAINTENANCE_UTILITIES" }
                    )
                }

                HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Net Result Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (BusinessModeFeatures.showHerdBreeding(mode)) "NET DAIRY FARM PROFIT" else "NET BUSINESS PROFIT",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = String.format(Locale.US, "₹%,.0f", displayedProfit),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = heroColor
                    )
                }
            }
        }

        // --- 11. WhatsApp Share Action ---
        OutlinedButton(
            onClick = {
                val shareSummary = buildString {
                    val isFarm = BusinessModeFeatures.showHerdBreeding(mode)
                    append("📊 *${if (isFarm) "MILKMATE DAIRY FARM P&L" else "MILKMATE DAIRY BUSINESS P&L"}*\n")
                    append("🏢 Business: ${business?.businessName ?: "My Dairy"}\n")
                    append("📅 Period: ${sdf.format(Date(report.startDate))} to ${sdf.format(Date(report.endDate))}\n\n")
                    append("🥛 Total Milk Volume: ${String.format(Locale.US, "%.1f", report.totalMilkLiters)} Liters\n")
                    append("💵 Total Revenue: ₹${String.format(Locale.US, "%,.0f", report.totalRevenue)}\n")
                    append("   • Cow Milk: ${String.format(Locale.US, "%.1f", report.cowLiters)}L (₹${report.cowRevenue.toInt()})\n")
                    append("   • Buffalo Milk: ${String.format(Locale.US, "%.1f", report.buffaloLiters)}L (₹${report.buffaloRevenue.toInt()})\n")
                    if (isFarm) {
                        append("🌾 Cattle Feed Cost: ₹${String.format(Locale.US, "%,.0f", report.feedCost)}\n")
                        append("💉 Medical/Vet: ₹${String.format(Locale.US, "%,.0f", report.veterinaryCost)}\n")
                        append("👨‍🌾 Farm Labor: ₹${String.format(Locale.US, "%,.0f", report.laborCost)}\n")
                    } else {
                        append("🥛 Farmer Milk Purchases: ₹${String.format(Locale.US, "%,.0f", report.milkPurchaseCost)}\n")
                        append("💼 Operating Costs: ₹${String.format(Locale.US, "%,.0f", report.operatingExpenses)}\n")
                    }
                    append("⚡ Total Operating Cost: ₹${String.format(Locale.US, "%,.0f", report.totalCost)}\n")
                    append("----------------------------\n")
                    append("💰 *NET PROFIT: ₹${String.format(Locale.US, "%,.0f", displayedProfit)}* (${report.profitMarginPercent}% Margin)\n")
                    append("📊 Cost per Liter (COPL): ₹${String.format(Locale.US, "%.1f", report.costOfProductionPerLiter)}/L\n")
                    append("🎯 Break-Even Price: ₹${String.format(Locale.US, "%.1f", report.breakEvenRatePerLiter)}/L\n")
                }
                ReportExportHelper.shareWhatsApp(context, shareSummary)
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share P&L Summary via WhatsApp", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // --- Custom Date Range Picker Dialog (Minimum 1 Month Constraint) ---
    if (showCustomDateDialog) {
        val cal = Calendar.getInstance()
        AlertDialog(
            onDismissRequest = { showCustomDateDialog = false },
            title = {
                Text("Select Accounting Range", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Dairy profit and loss requires an accounting period of at least 1 month (30 days) to accurately calculate operating margins and herd feed cycles.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    // Start Date Button
                    OutlinedButton(
                        onClick = {
                            cal.timeInMillis = tempStartDate
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(y, m, d, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    tempStartDate = newCal.timeInMillis
                                    customRangeError = null
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("From: ${sdf.format(Date(tempStartDate))}", fontSize = 13.sp)
                    }

                    // End Date Button
                    OutlinedButton(
                        onClick = {
                            cal.timeInMillis = tempEndDate
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(y, m, d, 23, 59, 59)
                                        set(Calendar.MILLISECOND, 999)
                                    }
                                    tempEndDate = newCal.timeInMillis
                                    customRangeError = null
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("To: ${sdf.format(Date(tempEndDate))}", fontSize = 13.sp)
                    }

                    // Error warning if less than 28 days
                    if (customRangeError != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = customRangeError ?: "",
                                color = DangerRed,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val diffDays = (tempEndDate - tempStartDate) / (24L * 60 * 60 * 1000L)
                        if (diffDays < 28) {
                            customRangeError = "Please select at least 1 month (30 days) to accurately calculate dairy farm profit & loss."
                        } else {
                            viewModel.setCustomProfitDateRange(Pair(tempStartDate, tempEndDate))
                            showCustomDateDialog = false
                            Toast.makeText(context, "Accounting period updated (≥ 1 Month)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
                ) {
                    Text("Apply Range")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Interactive Itemized Drill-Down Modal Sheet ---
    if (selectedDrillDownCategory != null) {
        val cat = selectedDrillDownCategory!!
        ProfitItemizedDrillDownSheet(
            category = cat,
            report = report,
            allDeliveries = allDeliveries,
            expenses = expenses,
            milkWastageList = milkWastageList,
            inventoryTransactions = inventoryTransactions,
            onDismiss = { selectedDrillDownCategory = null }
        )
    }
}

@Composable
fun ClickableProfitLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    valueColor: Color,
    isPositive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(icon, contentDescription = null, tint = if (isPositive) DairyGreen else TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 12.sp, color = TextPrimary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Inspect",
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitItemizedDrillDownSheet(
    category: String,
    report: ProfitReport,
    allDeliveries: List<DeliveryEntity>,
    expenses: List<ExpenseEntity>,
    milkWastageList: List<MilkWastageEntity>,
    inventoryTransactions: List<com.example.data.local.entity.InventoryTransactionEntity>,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val title = when (category) {
        "MILK_SALES" -> "Milk Sales & Dispatches"
        "FARMER_PROCUREMENT" -> "Farmer Milk Collections"
        "FEED_CONSUMABLES" -> "Feed & Consumables Ledger"
        "VET_MEDS" -> "Veterinary, Meds & Deworming"
        "BREEDING_AI" -> "Breeding & A.I. Straws"
        "LABOR_WAGES" -> "Farm Labor & Milker Wages"
        "MAINTENANCE_UTILITIES" -> "Electricity, Diesel & Shed Maintenance"
        "MILK_WASTAGE" -> "Milk Spoilage & Wastage Log"
        else -> "Transaction Details"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ITEMIZED FINANCIAL LEDGER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RoyalBluePrimary,
                        letterSpacing = 0.8.sp
                    )
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search records, names, notes...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Dynamic items rendering based on category
            when (category) {
                "MILK_SALES" -> {
                    val items = remember(allDeliveries, report, searchQuery) {
                        allDeliveries.filter {
                            it.deliveryDate in report.startDate..report.endDate &&
                            it.isDelivered &&
                            it.customerType != "SUPPLIER" &&
                            (searchQuery.isBlank() || it.customerName.contains(searchQuery, ignoreCase = true) || it.notes.contains(searchQuery, ignoreCase = true))
                        }
                    }
                    Text("Total: ${items.size} deliveries • ₹${items.sumOf { it.totalAmount }.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DairyGreen)
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items) { d ->
                            DrillDownCard(
                                title = d.customerName,
                                subtitle = "${sdf.format(Date(d.deliveryDate))} • ${d.shift} Shift • ${d.milkType}",
                                rightPrimary = "+₹${d.totalAmount.toInt()}",
                                rightSecondary = "${d.quantityLiters}L @ ₹${d.ratePerLiter}/L",
                                accentColor = DairyGreen
                            )
                        }
                    }
                }
                "FARMER_PROCUREMENT" -> {
                    val items = remember(allDeliveries, report, searchQuery) {
                        allDeliveries.filter {
                            it.deliveryDate in report.startDate..report.endDate &&
                            it.customerType == "SUPPLIER" &&
                            (searchQuery.isBlank() || it.customerName.contains(searchQuery, ignoreCase = true))
                        }
                    }
                    Text("Total: ${items.size} collections • ₹${items.sumOf { it.totalAmount }.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items) { d ->
                            DrillDownCard(
                                title = d.customerName,
                                subtitle = "${sdf.format(Date(d.deliveryDate))} • FAT: ${d.fat}% SNF: ${d.snf}%",
                                rightPrimary = "-₹${d.totalAmount.toInt()}",
                                rightSecondary = "${d.quantityLiters}L @ ₹${d.ratePerLiter}/L",
                                accentColor = DangerRed
                            )
                        }
                    }
                }
                "FEED_CONSUMABLES" -> {
                    val items = remember(expenses, inventoryTransactions, report, searchQuery) {
                        expenses.filter {
                            it.date in report.startDate..report.endDate &&
                            it.isBusiness() &&
                            (it.category.contains("FEED", ignoreCase = true) || it.category.contains("FODDER", ignoreCase = true) || it.isInventoryPurchase) &&
                            (searchQuery.isBlank() || it.notes.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true))
                        }
                    }
                    Text("Total: ${items.size} feed entries • ₹${items.sumOf { it.amount }.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items) { e ->
                            DrillDownCard(
                                title = e.notes.ifBlank { e.category },
                                subtitle = "${sdf.format(Date(e.date))} • ${e.category}",
                                rightPrimary = "-₹${e.amount.toInt()}",
                                rightSecondary = e.paymentMethod,
                                accentColor = DangerRed
                            )
                        }
                    }
                }
                "VET_MEDS" -> {
                    val items = remember(expenses, report, searchQuery) {
                        expenses.filter {
                            it.date in report.startDate..report.endDate &&
                            it.isBusiness() &&
                            (it.category.contains("VET", ignoreCase = true) || it.category.contains("MEDICINE", ignoreCase = true) || it.category.contains("DOCTOR", ignoreCase = true) || it.category.contains("TREATMENT", ignoreCase = true) || it.category.contains("VACCIN", ignoreCase = true) || it.category.contains("DEWORM", ignoreCase = true)) &&
                            (searchQuery.isBlank() || it.notes.contains(searchQuery, ignoreCase = true))
                        }
                    }
                    Text("Total: ${items.size} vet entries • ₹${items.sumOf { it.amount }.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items) { e ->
                            DrillDownCard(
                                title = e.notes.ifBlank { "Veterinary Treatment" },
                                subtitle = "${sdf.format(Date(e.date))} • ${e.category}",
                                rightPrimary = "-₹${e.amount.toInt()}",
                                rightSecondary = e.paymentMethod,
                                accentColor = DangerRed
                            )
                        }
                    }
                }
                "BREEDING_AI" -> {
                    val items = remember(expenses, report, searchQuery) {
                        expenses.filter {
                            it.date in report.startDate..report.endDate &&
                            it.isBusiness() &&
                            (it.category.contains("BREED", ignoreCase = true) || it.category.contains("INSEMIN", ignoreCase = true) || it.category.contains("SEMEN", ignoreCase = true) || it.category.contains("STRAW", ignoreCase = true)) &&
                            (searchQuery.isBlank() || it.notes.contains(searchQuery, ignoreCase = true))
                        }
                    }
                    Text("Total: ${items.size} breeding entries • ₹${items.sumOf { it.amount }.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items) { e ->
                            DrillDownCard(
                                title = e.notes.ifBlank { "A.I. Insemination Straw" },
                                subtitle = "${sdf.format(Date(e.date))} • ${e.category}",
                                rightPrimary = "-₹${e.amount.toInt()}",
                                rightSecondary = e.paymentMethod,
                                accentColor = DangerRed
                            )
                        }
                    }
                }
                "LABOR_WAGES" -> {
                    val items = remember(expenses, report, searchQuery) {
                        expenses.filter {
                            it.date in report.startDate..report.endDate &&
                            it.isBusiness() &&
                            (it.category.contains("LABOR", ignoreCase = true) || it.category.contains("LABOUR", ignoreCase = true) || it.category.contains("SALARY", ignoreCase = true) || it.category.contains("WAGE", ignoreCase = true) || it.category.contains("MILKER", ignoreCase = true)) &&
                            (searchQuery.isBlank() || it.notes.contains(searchQuery, ignoreCase = true))
                        }
                    }
                    Text("Total: ${items.size} labor entries • ₹${items.sumOf { it.amount }.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items) { e ->
                            DrillDownCard(
                                title = e.notes.ifBlank { "Milker / Farm Labor Wages" },
                                subtitle = "${sdf.format(Date(e.date))} • ${e.category}",
                                rightPrimary = "-₹${e.amount.toInt()}",
                                rightSecondary = e.paymentMethod,
                                accentColor = DangerRed
                            )
                        }
                    }
                }
                "MILK_WASTAGE" -> {
                    val items = remember(milkWastageList, report, searchQuery) {
                        milkWastageList.filter {
                            it.date in report.startDate..report.endDate &&
                            (searchQuery.isBlank() || it.reason.contains(searchQuery, ignoreCase = true) || it.batchSource.contains(searchQuery, ignoreCase = true))
                        }
                    }
                    Text("Total: ${items.size} wastage events • ₹${items.sumOf { it.totalLossAmount }.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items) { w ->
                            DrillDownCard(
                                title = w.reason,
                                subtitle = "${sdf.format(Date(w.date))} • ${w.batchSource} (${w.quantityLiters}L)",
                                rightPrimary = "-₹${w.totalLossAmount.toInt()}",
                                rightSecondary = "${w.milkType} Milk",
                                accentColor = DangerRed
                            )
                        }
                    }
                }
                else -> {
                    val items = remember(expenses, report, searchQuery) {
                        expenses.filter {
                            it.date in report.startDate..report.endDate &&
                            it.isBusiness() &&
                            (searchQuery.isBlank() || it.notes.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true))
                        }
                    }
                    Text("Total: ${items.size} overhead entries • ₹${items.sumOf { it.amount }.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items) { e ->
                            DrillDownCard(
                                title = e.notes.ifBlank { e.category },
                                subtitle = "${sdf.format(Date(e.date))} • ${e.category}",
                                rightPrimary = "-₹${e.amount.toInt()}",
                                rightSecondary = e.paymentMethod,
                                accentColor = DangerRed
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary)
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
fun DrillDownCard(
    title: String,
    subtitle: String,
    rightPrimary: String,
    rightSecondary: String,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(rightPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = accentColor)
                Spacer(modifier = Modifier.height(2.dp))
                Text(rightSecondary, fontSize = 10.sp, color = TextSecondary)
            }
        }
    }
}
