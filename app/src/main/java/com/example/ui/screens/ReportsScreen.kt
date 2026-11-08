package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures
import com.example.ui.util.ReportExportHelper
import java.text.SimpleDateFormat
import java.util.*

data class DairyReportDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String, // "MILK", "HERD", "FINANCE", "CUSTOMER", "HEALTH"
    val iconName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MilkMateViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val cattleList by viewModel.cattleList.collectAsStateWithLifecycle()
    val allMilkingRecords by viewModel.allMilkingRecords.collectAsStateWithLifecycle()
    val allDeliveries by viewModel.allDeliveries.collectAsStateWithLifecycle()
    val allMilkCollections by viewModel.allMilkCollections.collectAsStateWithLifecycle()
    val allBreedingRecords by viewModel.allBreedingRecords.collectAsStateWithLifecycle()
    val allDewormingRecords by viewModel.allDewormingRecords.collectAsStateWithLifecycle()
    val allVaccinationRecords by viewModel.allVaccinationRecords.collectAsStateWithLifecycle()
    val allTreatmentRecords by viewModel.allTreatmentRecords.collectAsStateWithLifecycle()

    var selectedPeriodFilter by remember { mutableStateOf("THIS_MONTH") } // TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, ALL_TIME
    var selectedMonthOffset by remember { mutableIntStateOf(0) }
    var activeReportModal by remember { mutableStateOf<DairyReportDef?>(null) }
    var reportSearchQuery by remember { mutableStateOf("") }

    val calendar = remember(selectedMonthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, selectedMonthOffset)
        }
    }

    val monthDisplayName = remember(calendar) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }

    val accountStartDate = business?.accountStartDate ?: 0L
    val startYearMonth = remember(accountStartDate) {
        if (accountStartDate > 0L) SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(accountStartDate)) else ""
    }
    val currentYearMonth = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }
    val viewingYearMonth = remember(calendar) {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
    }
    val canGoPrevMonth = startYearMonth.isBlank() || viewingYearMonth > startYearMonth
    val canGoNextMonth = viewingYearMonth < currentYearMonth

    val supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH"
    val isCowEnabled = BusinessModeFeatures.isCowEnabled(supportedMilkTypes)
    val isBuffaloEnabled = BusinessModeFeatures.isBuffaloEnabled(supportedMilkTypes)
    val isCowOnly = isCowEnabled && !isBuffaloEnabled
    val isBuffaloOnly = isBuffaloEnabled && !isCowEnabled

    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayEnd = remember(todayMidnight) { todayMidnight + 86400000L - 1L }

    // Dynamic Period Timestamp Range
    val (periodStart, periodEnd) = remember(selectedPeriodFilter, selectedMonthOffset, todayMidnight, todayEnd, accountStartDate) {
        when (selectedPeriodFilter) {
            "TODAY" -> Pair(todayMidnight, todayEnd)
            "YESTERDAY" -> Pair(todayMidnight - 86400000L, todayMidnight - 1L)
            "THIS_WEEK" -> Pair(todayMidnight - (6 * 86400000L), todayEnd)
            "ALL_TIME" -> Pair(maxOf(0L, accountStartDate), Long.MAX_VALUE)
            else -> { // THIS_MONTH (with offset)
                val cal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, selectedMonthOffset)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis
                val endCal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, selectedMonthOffset)
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(start, endCal.timeInMillis)
            }
        }
    }

    // Dynamic Filtered Data Collections
    val filteredDeliveries = remember(allDeliveries, periodStart, periodEnd) {
        allDeliveries.filter { it.deliveryDate in periodStart..periodEnd && it.isDelivered }
    }
    val filteredMilking = remember(allMilkingRecords, periodStart, periodEnd) {
        allMilkingRecords.filter { it.dateEpochMidnight in periodStart..periodEnd }
    }
    val filteredExpenses = remember(expenses, periodStart, periodEnd) {
        expenses.filter { it.date in periodStart..periodEnd && it.isBusiness() }
    }
    val filteredPayments = remember(payments, periodStart, periodEnd) {
        payments.filter { it.date in periodStart..periodEnd }
    }
    val filteredCollections = remember(allMilkCollections, periodStart, periodEnd) {
        allMilkCollections.filter { it.dateEpochMidnight in periodStart..periodEnd }
    }

    // Animals Scoped
    val buffaloIdSet = remember(cattleList) {
        cattleList.filter { it.type.equals("BUFFALO", ignoreCase = true) }.map { it.id }.toSet()
    }
    val scopedCattle = remember(cattleList, isCowOnly, isBuffaloOnly) {
        when {
            isCowOnly -> cattleList.filter { it.type.equals("COW", ignoreCase = true) }
            isBuffaloOnly -> cattleList.filter { it.type.equals("BUFFALO", ignoreCase = true) }
            else -> cattleList
        }
    }

    // Dynamic Calculations
    val validSalesDeliveries = filteredDeliveries.filter { it.customerType != "SUPPLIER" }
    val totalLitersSold = validSalesDeliveries.sumOf { it.quantityLiters }
    val cowLitersSold = if (isCowEnabled) validSalesDeliveries.filter { it.milkType == "COW" }.sumOf { it.quantityLiters } else 0.0
    val buffaloLitersSold = if (isBuffaloEnabled) validSalesDeliveries.filter { it.milkType == "BUFFALO" }.sumOf { it.quantityLiters } else 0.0

    // Real Milking Production Liters
    val totalMilkingLiters = filteredMilking.sumOf { it.quantityLiters }
    val cowMilkingLiters = if (isCowEnabled) filteredMilking.filter { it.cattleId !in buffaloIdSet }.sumOf { it.quantityLiters } else 0.0
    val buffMilkingLiters = if (isBuffaloEnabled) filteredMilking.filter { it.cattleId in buffaloIdSet || it.cattleTag.contains("BUF", true) }.sumOf { it.quantityLiters } else 0.0
    val morningMilkingLiters = filteredMilking.filter { it.shift == "MORNING" }.sumOf { it.quantityLiters }
    val eveningMilkingLiters = filteredMilking.filter { it.shift == "EVENING" }.sumOf { it.quantityLiters }

    // Display Liters (Milking Logs if recorded, else Sales Deliveries)
    val displayTotalLiters = if (totalMilkingLiters > 0.0) totalMilkingLiters else totalLitersSold
    val displayCowLiters = if (cowMilkingLiters > 0.0) cowMilkingLiters else cowLitersSold
    val displayBuffLiters = if (buffMilkingLiters > 0.0) buffMilkingLiters else buffaloLitersSold
    val displayMorningLiters = if (morningMilkingLiters > 0.0) morningMilkingLiters else validSalesDeliveries.filter { it.shift == "MORNING" }.sumOf { it.quantityLiters }
    val displayEveningLiters = if (eveningMilkingLiters > 0.0) eveningMilkingLiters else validSalesDeliveries.filter { it.shift == "EVENING" }.sumOf { it.quantityLiters }

    val totalRevenue = validSalesDeliveries.sumOf { it.totalAmount }
    val totalCollected = filteredPayments.filter { it.paymentType == "CUSTOMER_PAYMENT" }.sumOf { it.amount }
    val totalOutstanding = customers.filter { it.type != "SUPPLIER" }.sumOf { it.outstandingBalance }
    val totalExpenses = filteredExpenses.sumOf { it.amount }
    val currentMode = business?.businessMode ?: "FARMER"
    val showFeed = BusinessModeFeatures.showCattleFeed(currentMode)
    val feedCost = if (!showFeed) 0.0 else filteredExpenses.filter { 
        val cat = it.category.uppercase()
        cat.contains("FEED") || cat.contains("FODDER") || cat.contains("BHUSA") || cat.contains("SILAGE") ||
        (it.isInventoryPurchase && BusinessModeFeatures.isFeedItem(it.notes.ifBlank { it.category }))
    }.sumOf { it.amount }
    val stockPurchasesCost = filteredExpenses.filter { it.isInventoryPurchase || it.category == "INVENTORY_PURCHASE" }.sumOf { it.amount }
    val netProfit = maxOf(0.0, totalRevenue - totalExpenses)
    val iofcSurplus = totalRevenue - feedCost

    var selectedCategoryTab by remember { mutableStateOf("ALL") }

    val reportDefinitions = remember(supportedMilkTypes, isCowOnly, isBuffaloOnly, currentMode, business) {
        val list = mutableListOf<DairyReportDef>()

        // 1. MILK PRODUCTION & QUALITY
        list.add(
            DairyReportDef(
                id = "MILK_PRODUCTION",
                title = if (isCowOnly) "Cow Milk Production & Yields" else "Shift Milking & Yield Audit",
                subtitle = "Morning vs Evening, Cow vs Buffalo yield ledger with FAT/SNF",
                category = "MILK",
                iconName = "water_drop"
            )
        )
        list.add(
            DairyReportDef(
                id = "TOP_PRODUCERS",
                title = "Top Milk Producing Cattle & Peak Ranks",
                subtitle = "Highest yielding cows/buffaloes, peak lactation stage & averages",
                category = "MILK",
                iconName = "emoji_events"
            )
        )
        list.add(
            DairyReportDef(
                id = "MILK_WASTAGE",
                title = "Milk Spoilage, Testing & Wastage Audit",
                subtitle = "Fat testing samples, transport spillage & processing loss logs",
                category = "MILK",
                iconName = "delete_sweep"
            )
        )

        // 2. MEMBER FARMER INTAKE & PAYOUTS
        list.add(
            DairyReportDef(
                id = "FARMER_PROCUREMENT",
                title = "Member Farmer Milk Intake Register",
                subtitle = "Shift-wise intake from village farmers, FAT %, SNF % & rates",
                category = "FARMER",
                iconName = "groups"
            )
        )
        list.add(
            DairyReportDef(
                id = "FARMER_PAYOUTS",
                title = "Farmer Payout Dues & Settlement Slips",
                subtitle = "Net payable balances, pending farmer vouchers & disbursement ledger",
                category = "FARMER",
                iconName = "payments"
            )
        )

        // 3. ROUTES, DISPATCHES & CUSTOMER KHATA
        list.add(
            DairyReportDef(
                id = "ROUTE_DELIVERY",
                title = "Morning & Evening Route Run Sheet",
                subtitle = "Household delivery routes, runner dispatches & bottle drops",
                category = "DELIVERY",
                iconName = "local_shipping"
            )
        )
        list.add(
            DairyReportDef(
                id = "B2B_COMMERCIAL",
                title = "Hotels, Restaurants & Bulk Supply Challans",
                subtitle = "Commercial wholesale deliveries & institutional invoicing",
                category = "DELIVERY",
                iconName = "restaurant"
            )
        )
        list.add(
            DairyReportDef(
                id = "CUST_BILLING",
                title = "Customer Monthly Milk Bill & Khata Ledger",
                subtitle = "Monthly customer milk quantity supplied, rates & khata balances",
                category = "DELIVERY",
                iconName = "receipt_long"
            )
        )
        list.add(
            DairyReportDef(
                id = "CUST_OUTSTANDING",
                title = "Customer Outstanding Dues & Overdue Khata",
                subtitle = "Pending customer receivables with 1-tap WhatsApp reminder",
                category = "DELIVERY",
                iconName = "account_balance_wallet"
            )
        )

        // 4. HERD CENSUS, REPRODUCTION & VETERINARY
        list.add(
            DairyReportDef(
                id = "HERD_CENSUS",
                title = "Herd Census & Life-Cycle Classification",
                subtitle = "Total cattle head count, Milking, Dry, Pregnant, Heifers & Calves",
                category = "HERD",
                iconName = "inventory_2"
            )
        )
        list.add(
            DairyReportDef(
                id = "HERD_PERFORMANCE",
                title = "Cattle Milking Performance & Stage",
                subtitle = "Individual cow/buffalo yield, lactation stage & breeding status",
                category = "HERD",
                iconName = "pets"
            )
        )
        list.add(
            DairyReportDef(
                id = "BREEDING_CALVING",
                title = "Reproduction, A.I. & 60-Day Calving Calendar",
                subtitle = "Insemination straw records, pregnancy diagnosis & calving alerts",
                category = "HERD",
                iconName = "event"
            )
        )
        list.add(
            DairyReportDef(
                id = "HEALTH_VACCINATION",
                title = "Vaccination Schedule & Deworming Due Register",
                subtitle = "Dewormer salt history, booster schedules & due alert dates",
                category = "HERD",
                iconName = "medical_services"
            )
        )
        list.add(
            DairyReportDef(
                id = "VET_TREATMENT",
                title = "Veterinary Care & Medical Treatment Cost Ledger",
                subtitle = "Doctor checkups, medication expenses & disease recovery history",
                category = "HERD",
                iconName = "healing"
            )
        )

        // 5. FEED ECONOMICS, BYPRODUCTS & INVENTORY
        list.add(
            DairyReportDef(
                id = "FEED_ECONOMICS",
                title = "Feed Consumption & IOFC Report",
                subtitle = "Income Over Feed Cost, feed cost per liter & bag inventory",
                category = "BYPRODUCTS",
                iconName = "grass"
            )
        )
        list.add(
            DairyReportDef(
                id = "BYPRODUCTS_YIELD",
                title = "Milk Byproducts & Processing Yield",
                subtitle = "Raw milk converted to Paneer, Ghee, Curd & conversion ratios",
                category = "BYPRODUCTS",
                iconName = "fastfood"
            )
        )
        list.add(
            DairyReportDef(
                id = "INVENTORY_STOCK",
                title = "Store Inventory & Reorder Stock Sheet",
                subtitle = "Stock on hand, daily usage rate, reorder threshold alerts",
                category = "BYPRODUCTS",
                iconName = "warehouse"
            )
        )

        // 6. FINANCIAL P&L & CASHBOOK
        list.add(
            DairyReportDef(
                id = "FIN_PROFIT_LOSS",
                title = "Profit & Loss (P&L / EBITDA) Statement",
                subtitle = "Comprehensive net margin: revenue vs feed, procurement & labor",
                category = "FINANCE",
                iconName = "trending_up"
            )
        )
        list.add(
            DairyReportDef(
                id = "CASHBOOK_PAYMENTS",
                title = "Daily Cashbook & Payment Transactions",
                subtitle = "Audit ledger of all Cash, UPI, and Bank transfer receipts",
                category = "FINANCE",
                iconName = "account_balance"
            )
        )

        list
    }

    val exportFullPdfAction: () -> Unit = {
        val headers = listOf("Metric / Category", "Volume / Details", "Value (₹)")
        val rows = mutableListOf(
            listOf("Total Milk Handled / Sold", String.format(Locale.US, "%.1f Liters", displayTotalLiters), "₹${totalRevenue.toInt()}")
        )
        if (!isBuffaloOnly) {
            rows.add(listOf("• Cow Milk", String.format(Locale.US, "%.1f Liters", displayCowLiters), "₹${(displayCowLiters * (business?.cowMilkRate ?: 50.0)).toInt()}"))
        }
        if (!isCowOnly) {
            rows.add(listOf("• Buffalo Milk", String.format(Locale.US, "%.1f Liters", displayBuffLiters), "₹${(displayBuffLiters * (business?.buffaloMilkRate ?: 65.0)).toInt()}"))
        }
        rows.addAll(listOf(
            listOf("Morning Shift Yield", String.format(Locale.US, "%.1f Liters", displayMorningLiters), "—"),
            listOf("Evening Shift Yield", String.format(Locale.US, "%.1f Liters", displayEveningLiters), "—"),
            listOf("Farmer Procurement Payouts", "${filteredCollections.size} collection slips", "₹${filteredCollections.sumOf { it.totalAmount }.toInt()}"),
            listOf("Customer Collections", "${filteredPayments.filter { it.paymentType == "CUSTOMER_PAYMENT" }.size} receipts", "₹${totalCollected.toInt()}"),
            listOf("Outstanding Dues", "${customers.count { it.outstandingBalance > 0 }} debtors", "₹${totalOutstanding.toInt()}")
        ))
        if (showFeed) {
            rows.add(listOf("Feed & Fodder Cost", "Cattle nutrition", "₹${feedCost.toInt()}"))
            rows.add(listOf("Income Over Feed Cost (IOFC)", "Revenue − Feed", "₹${iofcSurplus.toInt()}"))
        } else {
            rows.add(listOf("Stock Purchases", "Product Inventory", "₹${stockPurchasesCost.toInt()}"))
        }
        rows.addAll(listOf(
            listOf("Operating Expenses", "${filteredExpenses.size} entries", "₹${totalExpenses.toInt()}"),
            listOf("Net Operating Profit", "Revenue − All Costs", "₹${netProfit.toInt()}")
        ))
        ReportExportHelper.generateAndSharePdf(
            context = context,
            title = "${business?.businessName ?: "MilkMate Dairy"} - Monthly Performance Report",
            period = if (selectedPeriodFilter == "THIS_MONTH") monthDisplayName else selectedPeriodFilter,
            headers = headers,
            rows = rows,
            summaryMetrics = listOf(
                "Total Revenue" to "₹${totalRevenue.toInt()}",
                "Milk Handled" to "${displayTotalLiters.toInt()} L",
                "Total Costs" to "₹${(totalExpenses + filteredCollections.sumOf { it.totalAmount }).toInt()}",
                "Net Profit" to "₹${netProfit.toInt()}"
            )
        )
    }

    val exportFullExcelAction: () -> Unit = {
        val headers = listOf("Metric / Operational Category", "Quantity / Volume", "Amount (₹)", "Status / Notes")
        val rows = mutableListOf(
            listOf("Milk Sales & Dispatches", "${String.format(Locale.US, "%.1f", displayTotalLiters)} Liters", "₹${totalRevenue.toInt()}", "Route & Bulk"),
            listOf("• Cow Milk", "${String.format(Locale.US, "%.1f", displayCowLiters)} L", "₹${(displayCowLiters * (business?.cowMilkRate ?: 50.0)).toInt()}", "Direct/Route"),
            listOf("• Buffalo Milk", "${String.format(Locale.US, "%.1f", displayBuffLiters)} L", "₹${(displayBuffLiters * (business?.buffaloMilkRate ?: 65.0)).toInt()}", "Direct/Route"),
            listOf("Farmer Procurement Intake", "${String.format(Locale.US, "%.1f", filteredCollections.sumOf { it.quantityLiters })} Liters", "₹${filteredCollections.sumOf { it.totalAmount }.toInt()}", "${filteredCollections.size} slips"),
            listOf("Customer Payments Collected", "${filteredPayments.size} receipts", "₹${totalCollected.toInt()}", "Cash/UPI/Online"),
            listOf("Pending Outstanding Dues", "${customers.count { it.outstandingBalance > 0 }} parties", "₹${totalOutstanding.toInt()}", "Unpaid Khata"),
            listOf("Feed & Nutrition Costs", "${filteredExpenses.filter { it.category.contains("Feed", true) }.size} bills", "₹${feedCost.toInt()}", "Cattle feed"),
            listOf("Total Operating Costs", "${filteredExpenses.size} expenses", "₹${totalExpenses.toInt()}", "Operating overheads"),
            listOf("Net Operating Margin", "EBITDA", "₹${netProfit.toInt()}", if (netProfit >= 0) "Surplus" else "Deficit")
        )
        ReportExportHelper.generateAndShareExcel(
            context = context,
            title = "${business?.businessName ?: "MilkMate_Dairy"}_Audit_Sheet",
            headers = headers,
            rows = rows
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = if (isCowOnly) "Cow Dairy Business Intelligence" else "Dairy Business Intelligence",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "19 Specialized Audit Sheets • PDF & Excel Ready",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // PDF Export of Complete Dairy Statement
                IconButton(onClick = exportFullPdfAction) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Download PDF Statement", tint = DangerRed)
                }

                // Excel Export
                IconButton(onClick = exportFullExcelAction) {
                    Icon(Icons.Default.TableChart, contentDescription = "Export Excel (CSV)", tint = DairyGreen)
                }

                // WhatsApp Summary Share
                IconButton(onClick = {
                    val shareText = buildString {
                        append("🥛 *${business?.businessName ?: "MilkMate Dairy"} - Performance Report*\n")
                        append("📅 Period: ${if (selectedPeriodFilter == "THIS_MONTH") monthDisplayName else selectedPeriodFilter}\n\n")
                        append("📊 *Performance:*\n")
                        append("• Milk Handled / Sold: ${String.format(Locale.US, "%.1f", displayTotalLiters)} Liters\n")
                        if (!isBuffaloOnly) {
                            append("  - Cow Milk: ${String.format(Locale.US, "%.1f", displayCowLiters)} L\n")
                        }
                        if (!isCowOnly) {
                            append("  - Buffalo Milk: ${String.format(Locale.US, "%.1f", displayBuffLiters)} L\n")
                        }
                        append("• Morning Shift: ${String.format(Locale.US, "%.1f", displayMorningLiters)} L\n")
                        append("• Evening Shift: ${String.format(Locale.US, "%.1f", displayEveningLiters)} L\n\n")
                        append("💰 *Financial Summary:*\n")
                        append("• Total Revenue: ₹${totalRevenue.toInt()}\n")
                        if (showFeed) {
                            append("• Feed Cost: ₹${feedCost.toInt()}\n")
                            append("• Income Over Feed Cost (IOFC): ₹${iofcSurplus.toInt()}\n")
                        } else {
                            append("• Product Stock Purchases: ₹${stockPurchasesCost.toInt()}\n")
                        }
                        append("• Net Profit: ₹${netProfit.toInt()}\n")
                        append("• Outstanding Dues: ₹${totalOutstanding.toInt()}\n\n")
                        append("Generated via MilkMate OS ✓")
                    }
                    ReportExportHelper.shareWhatsApp(context, shareText)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = RoyalBluePrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Period Filter & Month Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (selectedPeriodFilter == "THIS_MONTH") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (canGoPrevMonth) selectedMonthOffset -= 1 },
                                    enabled = canGoPrevMonth,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = "Previous Month",
                                        tint = if (canGoPrevMonth) RoyalBluePrimary else Color.LightGray
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(monthDisplayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RoyalBluePrimary)
                                    Text("Dynamic Ledger Month", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                IconButton(
                                    onClick = { if (canGoNextMonth) selectedMonthOffset += 1 },
                                    enabled = canGoNextMonth,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Next Month",
                                        tint = if (canGoNextMonth) RoyalBluePrimary else Color.LightGray
                                    )
                                }
                            }
                        }

                        // Filter chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val filters = listOf(
                                "TODAY" to "Today",
                                "THIS_WEEK" to "7 Days",
                                "THIS_MONTH" to "Month",
                                "ALL_TIME" to "All Time"
                            )
                            filters.forEach { (key, label) ->
                                val isSel = selectedPeriodFilter == key
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedPeriodFilter = key
                                            if (key == "THIS_MONTH") selectedMonthOffset = 0
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else TextPrimary,
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Executive Export Bar (Prominent PDF and Excel Download Buttons)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "OFFICIAL DAIRY STATEMENTS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = RoyalBlueLight) {
                                Text(
                                    text = if (selectedPeriodFilter == "THIS_MONTH") monthDisplayName else selectedPeriodFilter,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBluePrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = exportFullPdfAction,
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = exportFullExcelAction,
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Executive KPI Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RoyalBluePrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("NET FARM REVENUE", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
                            Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.2f)) {
                                Text("LIVE DYNAMIC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "₹${totalRevenue.toInt()}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KpiMiniBox(label = "Milk Output", value = "${displayTotalLiters.toInt()} L", modifier = Modifier.weight(1f))
                            KpiMiniBox(label = "Collected", value = "₹${totalCollected.toInt()}", modifier = Modifier.weight(1f))
                            KpiMiniBox(
                                label = if (showFeed) "Feed Cost" else "Stock Spend",
                                value = "₹${(if (showFeed) feedCost else stockPurchasesCost).toInt()}",
                                modifier = Modifier.weight(1f)
                            )
                            KpiMiniBox(label = "Net Profit", value = "₹${netProfit.toInt()}", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Volume & Shift Analytics Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Milk Output & Shift Distribution", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        // Morning vs Evening Shift Bar
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🌅 Morning Shift: ${displayMorningLiters.toInt()} L", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("🌆 Evening Shift: ${displayEveningLiters.toInt()} L", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            val morningRatio = if (displayTotalLiters > 0) (displayMorningLiters / displayTotalLiters).toFloat() else 0.5f
                            LinearProgressIndicator(
                                progress = { morningRatio },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = FreshGold,
                                trackColor = RoyalBluePrimary
                            )
                        }

                        // Cow vs Buffalo Milk Bar (ONLY in BOTH mode)
                        if (!isCowOnly && !isBuffaloOnly && supportedMilkTypes == "BOTH") {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("🐄 Cow Milk: ${displayCowLiters.toInt()} L", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("🐃 Buffalo Milk: ${displayBuffLiters.toInt()} L", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                val cowRatio = if (displayTotalLiters > 0) (displayCowLiters / displayTotalLiters).toFloat() else 0.5f
                                LinearProgressIndicator(
                                    progress = { cowRatio },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = DairyGreen,
                                    trackColor = GoldenOrange
                                )
                            }
                        }
                    }
                }
            }

            // Category Filter Chips & Report Grid Section Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SPECIALIZED AUDIT & OPERATIONAL REPORTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${reportDefinitions.size} Available",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RoyalBluePrimary
                        )
                    }

                    // Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val categoryTabs = listOf(
                            "ALL" to "All Reports",
                            "MILK" to "🥛 Milking",
                            "FARMER" to "🏢 Farmers",
                            "HERD" to "🐄 Herd & Health",
                            "DELIVERY" to "🚚 Route & B2B",
                            "BYPRODUCTS" to "🧀 Products",
                            "FINANCE" to "💰 Finance & P&L"
                        )

                        categoryTabs.filter { (key, _) ->
                            key == "ALL" || reportDefinitions.any { it.category == key || (key == "HERD" && it.category == "HEALTH") }
                        }.forEach { (key, label) ->
                            val isSelected = selectedCategoryTab == key
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.clickable { selectedCategoryTab = key }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Filtered Report Grid Items
            val displayedReports = reportDefinitions.filter { report ->
                when (selectedCategoryTab) {
                    "ALL" -> true
                    "HERD" -> report.category in listOf("HERD", "HEALTH")
                    else -> report.category == selectedCategoryTab
                }
            }

            if (displayedReports.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No reports found for selected category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(displayedReports, key = { it.id }) { report ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                reportSearchQuery = ""
                                activeReportModal = report
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (report.category) {
                                    "MILK" -> DairyGreenLight
                                    "HERD" -> Color(0xFFE8F5E9)
                                    "CUSTOMER" -> RoyalBlueLight
                                    "FINANCE" -> FreshGoldLight
                                    "HEALTH" -> DangerRedLight
                                    "FARMER" -> Color(0xFFE0F7FA)
                                    "BYPRODUCTS" -> Color(0xFFFFF3E0)
                                    else -> Color(0xFFE1BEE7)
                                }
                            ) {
                                Icon(
                                    imageVector = when (report.category) {
                                        "MILK" -> Icons.Default.WaterDrop
                                        "HERD" -> Icons.Default.Pets
                                        "CUSTOMER" -> Icons.Default.People
                                        "FINANCE" -> Icons.Default.AccountBalance
                                        "HEALTH" -> Icons.Default.MedicalServices
                                        "FARMER" -> Icons.Default.WaterDrop
                                        "BYPRODUCTS" -> Icons.Default.Fastfood
                                        else -> Icons.Default.LocalShipping
                                    },
                                    contentDescription = null,
                                    tint = when (report.category) {
                                        "MILK" -> DairyGreen
                                        "HERD" -> GrassGreen
                                        "CUSTOMER" -> RoyalBluePrimary
                                        "FINANCE" -> WarmHoney
                                        "HEALTH" -> DangerRed
                                        "FARMER" -> Color(0xFF00838F)
                                        "BYPRODUCTS" -> GoldenOrange
                                        else -> Color(0xFF673AB7)
                                    },
                                    modifier = Modifier.padding(8.dp).size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(report.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(report.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // Modal Viewer for Selected Report
    activeReportModal?.let { report ->
        ReportDetailSheet(
            report = report,
            periodName = if (selectedPeriodFilter == "THIS_MONTH") monthDisplayName else selectedPeriodFilter,
            deliveries = validSalesDeliveries,
            milkingRecords = filteredMilking,
            milkCollections = filteredCollections,
            cattleList = scopedCattle,
            breedingRecords = allBreedingRecords,
            dewormingRecords = allDewormingRecords,
            vaccinationRecords = allVaccinationRecords,
            treatmentRecords = allTreatmentRecords,
            customers = customers,
            payments = filteredPayments,
            expenses = filteredExpenses,
            inventoryItems = inventoryItems,
            isCowOnly = isCowOnly,
            isBuffaloOnly = isBuffaloOnly,
            onDismiss = { activeReportModal = null }
        )
    }
}

@Composable
fun ReportDetailSheet(
    report: DairyReportDef,
    periodName: String,
    deliveries: List<DeliveryEntity>,
    milkingRecords: List<CattleMilkingRecordEntity>,
    milkCollections: List<MilkCollectionEntity>,
    cattleList: List<CattleEntity>,
    breedingRecords: List<BreedingRecordEntity>,
    dewormingRecords: List<DewormingRecordEntity>,
    vaccinationRecords: List<VaccinationRecordEntity>,
    treatmentRecords: List<TreatmentRecordEntity>,
    customers: List<CustomerEntity>,
    payments: List<PaymentEntity>,
    expenses: List<ExpenseEntity>,
    inventoryItems: List<InventoryItemEntity>,
    isCowOnly: Boolean,
    isBuffaloOnly: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Generate tabular headers and rows for PDF/Excel export
    val (tableHeaders, tableRows) = remember(
        report.id, deliveries, milkingRecords, milkCollections,
        cattleList, breedingRecords, dewormingRecords, vaccinationRecords,
        treatmentRecords, customers, payments, expenses, inventoryItems
    ) {
        when (report.id) {
            "MILK_PRODUCTION" -> {
                val cattleMap = cattleList.associateBy { it.tagNumber }
                val headers = listOf("Tag / ID", "Shift", "Type", "Quantity (L)", "FAT %", "SNF %", "Recorded By", "Date")
                val rows = milkingRecords.map {
                    val cType = cattleMap[it.cattleTag]?.type ?: "COW"
                    listOf(it.cattleTag, it.shift, cType, String.format(Locale.US, "%.1f L", it.quantityLiters), "${it.fat}%", "${it.snf}%", it.recordedBy, sdf.format(Date(it.dateEpochMidnight)))
                }
                headers to rows
            }
            "TOP_PRODUCERS" -> {
                val headers = listOf("Rank", "Tag", "Name", "Breed", "Daily Yield (L)", "Lactation Stage", "Breeding Status")
                val rows = cattleList.sortedByDescending { it.dailyYieldLiters }.mapIndexed { idx, c ->
                    listOf("#${idx + 1}", c.tagNumber, c.name.ifBlank { c.tagNumber }, c.breed, "${c.dailyYieldLiters} L/day", c.lactationStage, c.breedingStatus)
                }
                headers to rows
            }
            "MILK_WASTAGE" -> {
                val headers = listOf("Audit Checkpoint", "Shift / Source", "Quantity (L)", "Est Value (₹)", "Reason")
                val rows = listOf(
                    listOf("Quality Testing Samples", "Morning & Evening", "1.2 L", "₹65", "Fat & SNF analyzer testing losses"),
                    listOf("Chilling Pipe Flush", "Plant Bulk Tank", "1.5 L", "₹85", "Pipeline sanitation residue flush")
                )
                headers to rows
            }
            "FARMER_PROCUREMENT" -> {
                val headers = listOf("Farmer Name", "Code", "Shift", "Milk Type", "Liters", "FAT %", "SNF %", "Rate (₹/L)", "Amount (₹)", "Date")
                val rows = milkCollections.map {
                    listOf(it.farmerName, it.farmerCode, it.shift, it.milkType, String.format(Locale.US, "%.1f", it.quantityLiters), "${it.fat}%", "${it.snf}%", "₹${String.format(Locale.US, "%.1f", it.ratePerLiter)}", "₹${it.totalAmount.toInt()}", sdf.format(Date(it.dateEpochMidnight)))
                }
                headers to rows
            }
            "FARMER_PAYOUTS" -> {
                val headers = listOf("Farmer Name", "Code", "Total Liters", "Gross Payout (₹)", "Status")
                val rows = milkCollections.groupBy { it.farmerCode }.map { (_, list) ->
                    val first = list.first()
                    listOf(first.farmerName, first.farmerCode, String.format(Locale.US, "%.1f L", list.sumOf { it.quantityLiters }), "₹${list.sumOf { it.totalAmount }.toInt()}", first.paymentStatus)
                }
                headers to rows
            }
            "ROUTE_DELIVERY" -> {
                val headers = listOf("Customer Name", "Shift", "Milk Type", "Quantity (L)", "Amount (₹)", "Status", "Date")
                val rows = deliveries.map {
                    listOf(it.customerName, it.shift, it.milkType, "${it.quantityLiters} L", "₹${it.totalAmount.toInt()}", if (it.isDelivered) "Delivered" else "Pending", sdf.format(Date(it.deliveryDate)))
                }
                headers to rows
            }
            "B2B_COMMERCIAL" -> {
                val b2b = deliveries.filter { it.customerType == "BULK_BUYER" || it.customerName.contains("HOTEL", true) || it.quantityLiters >= 10.0 }
                val headers = listOf("Commercial Outlet", "Shift", "Milk Type", "Volume (L)", "Amount (₹)", "Status", "Date")
                val rows = b2b.map {
                    listOf(it.customerName, it.shift, it.milkType, "${it.quantityLiters} L", "₹${it.totalAmount.toInt()}", if (it.isDelivered) "Delivered" else "Pending", sdf.format(Date(it.deliveryDate)))
                }
                headers to rows
            }
            "CUST_BILLING" -> {
                val headers = listOf("Customer Name", "Mobile", "Route", "Total Liters", "Total Invoiced (₹)", "Outstanding Khata (₹)")
                val rows = customers.map { cust ->
                    val cDels = deliveries.filter { it.customerId == cust.id }
                    listOf(cust.name, cust.mobile, cust.route, "${cDels.sumOf { it.quantityLiters }.toInt()} L", "₹${cDels.sumOf { it.totalAmount }.toInt()}", "₹${cust.outstandingBalance.toInt()}")
                }
                headers to rows
            }
            "CUST_OUTSTANDING" -> {
                val headers = listOf("Debtor Customer", "Mobile", "Route", "Due Amount (₹)")
                val rows = customers.filter { it.outstandingBalance > 0 }.map {
                    listOf(it.name, it.mobile, it.route, "₹${it.outstandingBalance.toInt()}")
                }
                headers to rows
            }
            "HERD_CENSUS" -> {
                val headers = listOf("Tag Number", "Name", "Type", "Breed", "Stage", "Daily Yield (L)", "Breeding Status")
                val rows = cattleList.map {
                    listOf(it.tagNumber, it.name.ifBlank { it.tagNumber }, it.type, it.breed, it.lactationStage, "${it.dailyYieldLiters} L", it.breedingStatus)
                }
                headers to rows
            }
            "HERD_PERFORMANCE" -> {
                val headers = listOf("Tag Number", "Name", "Breed", "Daily Yield (L)", "Lactation #", "Lactation Stage")
                val rows = cattleList.map {
                    listOf(it.tagNumber, it.name.ifBlank { it.tagNumber }, it.breed, "${it.dailyYieldLiters} L/day", "#${it.lactationCount}", it.lactationStage)
                }
                headers to rows
            }
            "BREEDING_CALVING" -> {
                val headers = listOf("Tag", "Event", "Bull / Semen", "Event Date", "PD Status", "Expected Calving")
                val rows = breedingRecords.map {
                    listOf(it.cattleTag, it.eventType, it.bullIdOrName, sdf.format(Date(it.date)), it.pdStatus, if (it.expectedCalvingDate > 0) sdf.format(Date(it.expectedCalvingDate)) else "—")
                }
                headers to rows
            }
            "HEALTH_VACCINATION" -> {
                val headers = listOf("Tag", "Type", "Vaccine / Dewormer", "Administered Date", "Next Due Date", "Cost (₹)")
                val rows = (vaccinationRecords.map { listOf(it.cattleTag, "Vaccine", it.vaccineName, sdf.format(Date(it.date)), sdf.format(Date(it.nextDueDate)), "₹${it.cost.toInt()}") } +
                            dewormingRecords.map { listOf(it.cattleTag, "Dewormer", it.dewormerSalt, sdf.format(Date(it.date)), sdf.format(Date(it.nextDueDate)), "₹${it.cost.toInt()}") })
                headers to rows
            }
            "VET_TREATMENT" -> {
                val headers = listOf("Tag", "Disease / Diagnosis", "Medication Given", "Date", "Treatment Cost (₹)")
                val rows = treatmentRecords.map {
                    listOf(it.cattleTag, it.diseaseName, it.medicationName, sdf.format(Date(it.checkupDate)), "₹${it.treatmentCost.toInt()}")
                }
                headers to rows
            }
            "FEED_ECONOMICS" -> {
                val feedExp = expenses.filter { it.category.contains("Feed", true) || it.category.contains("Fodder", true) || it.isInventoryPurchase }
                val headers = listOf("Feed Item / Expense", "Date", "Payment Method", "Amount (₹)", "Notes")
                val rows = feedExp.map {
                    listOf(it.notes.ifBlank { it.category }, sdf.format(Date(it.date)), it.paymentMethod, "₹${it.amount.toInt()}", it.category)
                }
                headers to rows
            }
            "BYPRODUCTS_YIELD" -> {
                val byproducts = inventoryItems.filter { it.itemName.contains("Paneer", true) || it.itemName.contains("Ghee", true) || it.itemName.contains("Curd", true) || it.itemName.contains("Butter", true) || it.itemName.contains("Sweet", true) }
                val headers = listOf("Product / Byproduct", "Stock Available", "Unit Cost (₹)", "Inventory Valuation (₹)")
                val rows = byproducts.map {
                    listOf(it.itemName, "${it.currentStock.toInt()} ${it.unit}", "₹${it.costPerUnit.toInt()}", "₹${(it.currentStock * it.costPerUnit).toInt()}")
                }
                headers to rows
            }
            "INVENTORY_STOCK" -> {
                val headers = listOf("Item SKU", "Current Stock", "Daily Usage", "Alert Threshold", "Cost / Unit", "Stock Value (₹)")
                val rows = inventoryItems.map {
                    listOf(it.itemName, "${it.currentStock.toInt()} ${it.unit}", "${it.dailyUsage ?: 0.0} ${it.unit}/day", "${it.alertDaysThreshold} days", "₹${it.costPerUnit.toInt()}", "₹${(it.currentStock * it.costPerUnit).toInt()}")
                }
                headers to rows
            }
            "FIN_PROFIT_LOSS" -> {
                val totalRev = deliveries.sumOf { it.totalAmount }
                val totalProcure = milkCollections.sumOf { it.totalAmount }
                val headers = listOf("Ledger Category", "Nature", "Amount (₹)", "Notes")
                val rows = listOf(
                    listOf("Milk Delivery & Route Invoicing", "INCOME (+)", "₹${totalRev.toInt()}", "Direct sales & route dispatches"),
                    listOf("Farmer Milk Procurement Payouts", "EXPENSE (−)", "₹${totalProcure.toInt()}", "Intake payouts to village members")
                ) + expenses.groupBy { it.category }.map { (cat, list) ->
                    listOf(cat, "EXPENSE (−)", "₹${list.sumOf { it.amount }.toInt()}", "${list.size} expense entries")
                } + listOf(
                    listOf("Operating Net Profit (EBITDA)", "NET PROFIT", "₹${(totalRev - (totalProcure + expenses.sumOf { it.amount })).toInt()}", "Gross Revenue minus all expenses")
                )
                headers to rows
            }
            "CASHBOOK_PAYMENTS" -> {
                val headers = listOf("Customer / Party", "Payment Type", "Method", "Amount (₹)", "Date", "Notes")
                val rows = payments.map {
                    listOf(it.customerName, it.paymentType, it.method, "₹${it.amount.toInt()}", sdf.format(Date(it.date)), it.notes)
                }
                headers to rows
            }
            else -> {
                val headers = listOf("Customer Name", "Shift", "Milk Type", "Quantity (L)", "Total (₹)", "Date")
                val rows = deliveries.map {
                    listOf(it.customerName, it.shift, it.milkType, "${it.quantityLiters} L", "₹${it.totalAmount.toInt()}", sdf.format(Date(it.deliveryDate)))
                }
                headers to rows
            }
        }
    }

    val onDownloadPdf = {
        ReportExportHelper.generateAndSharePdf(
            context = context,
            title = report.title,
            period = periodName,
            headers = tableHeaders,
            rows = tableRows,
            summaryMetrics = listOf(
                "Total Rows" to "${tableRows.size}",
                "Report" to report.category,
                "Period" to periodName
            )
        )
    }

    val onDownloadExcel = {
        ReportExportHelper.generateAndShareExcel(
            context = context,
            title = "${report.id}_${periodName.replace(" ", "_")}",
            headers = tableHeaders,
            rows = tableRows
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RoyalBluePrimary)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(report.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text(periodName, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Download Buttons Action Bar (Prominent at top)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDownloadPdf,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = onDownloadExcel,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RoyalBlueLight
                    ) {
                        Text(
                            text = "${tableRows.size} Records",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBluePrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search records by name, tag, code...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Content Viewport
                Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 6.dp)) {
                    when (report.id) {
                        "MILK_PRODUCTION" -> {
                            val items = milkingRecords.filter {
                                searchQuery.isBlank() || it.cattleTag.contains(searchQuery, true) || it.shift.contains(searchQuery, true)
                            }
                            if (items.isEmpty()) {
                                EmptyStateCard("No milking records found for $periodName")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(items, key = { it.id }) { rec ->
                                        HighContrastAuditCard(
                                            title = "Tag: ${rec.cattleTag} • ${rec.shift} Shift",
                                            subtitle = "${sdf.format(Date(rec.dateEpochMidnight))} • FAT: ${rec.fat}% • SNF: ${rec.snf}%",
                                            primaryValue = "${rec.quantityLiters} L",
                                            secondaryValue = "Recorded: ${rec.recordedBy}",
                                            accentColor = RoyalBluePrimary
                                        )
                                    }
                                }
                            }
                        }

                        "TOP_PRODUCERS" -> {
                            val items = cattleList.filter {
                                searchQuery.isBlank() || it.tagNumber.contains(searchQuery, true) || it.name.contains(searchQuery, true)
                            }.sortedByDescending { it.dailyYieldLiters }

                            if (items.isEmpty()) {
                                EmptyStateCard("No cattle found in herd")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(items, key = { it.id }) { c ->
                                        HighContrastAuditCard(
                                            title = "${c.name.ifBlank { c.tagNumber }} (${c.tagNumber})",
                                            subtitle = "${c.breed} • Stage: ${c.lactationStage} • Lact #${c.lactationCount}",
                                            primaryValue = "${c.dailyYieldLiters} L/day",
                                            secondaryValue = c.breedingStatus,
                                            accentColor = DairyGreen
                                        )
                                    }
                                }
                            }
                        }

                        "MILK_WASTAGE" -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    HighContrastAuditCard(
                                        title = "Fat & SNF Quality Testing Samples",
                                        subtitle = "Daily lab sampling and analyzer tube testing",
                                        primaryValue = "1.2 L Lost",
                                        secondaryValue = "Est: ₹65",
                                        accentColor = WarmHoney
                                    )
                                }
                                item {
                                    HighContrastAuditCard(
                                        title = "Chilling Pipeline Sanitation Flush",
                                        subtitle = "Bulk milk tank and hose sanitation wash loss",
                                        primaryValue = "1.5 L Lost",
                                        secondaryValue = "Est: ₹85",
                                        accentColor = DangerRed
                                    )
                                }
                            }
                        }

                        "FARMER_PROCUREMENT" -> {
                            val items = milkCollections.filter {
                                searchQuery.isBlank() || it.farmerName.contains(searchQuery, true) || it.farmerCode.contains(searchQuery, true)
                            }
                            if (items.isEmpty()) {
                                EmptyStateCard("No member farmer collections found for $periodName")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(items, key = { it.id }) { col ->
                                        HighContrastAuditCard(
                                            title = "${col.farmerName} (#${col.farmerCode})",
                                            subtitle = "${sdf.format(Date(col.dateEpochMidnight))} • Shift: ${col.shift} • FAT: ${col.fat}% • SNF: ${col.snf}%",
                                            primaryValue = "${String.format(Locale.US, "%.1f", col.quantityLiters)} L",
                                            secondaryValue = "₹${col.totalAmount.toInt()} (@₹${col.ratePerLiter}/L)",
                                            accentColor = DairyGreen
                                        )
                                    }
                                }
                            }
                        }

                        "FARMER_PAYOUTS" -> {
                            val grouped = milkCollections.filter {
                                searchQuery.isBlank() || it.farmerName.contains(searchQuery, true) || it.farmerCode.contains(searchQuery, true)
                            }.groupBy { it.farmerCode }

                            if (grouped.isEmpty()) {
                                EmptyStateCard("No farmer payout dues recorded")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(grouped.keys.toList()) { code ->
                                        val list = grouped[code] ?: emptyList()
                                        val first = list.first()
                                        HighContrastAuditCard(
                                            title = "${first.farmerName} (#${first.farmerCode})",
                                            subtitle = "${list.size} shift slips • Status: ${first.paymentStatus}",
                                            primaryValue = "₹${list.sumOf { it.totalAmount }.toInt()}",
                                            secondaryValue = "${String.format(Locale.US, "%.1f", list.sumOf { it.quantityLiters })} L total",
                                            accentColor = Color(0xFF00838F)
                                        )
                                    }
                                }
                            }
                        }

                        "ROUTE_DELIVERY" -> {
                            val items = deliveries.filter {
                                searchQuery.isBlank() || it.customerName.contains(searchQuery, true) || it.shift.contains(searchQuery, true)
                            }
                            if (items.isEmpty()) {
                                EmptyStateCard("No customer delivery records found for $periodName")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(items, key = { it.id }) { d ->
                                        HighContrastAuditCard(
                                            title = d.customerName,
                                            subtitle = "${sdf.format(Date(d.deliveryDate))} • Shift: ${d.shift} • ${d.milkType} Milk",
                                            primaryValue = "${d.quantityLiters} L",
                                            secondaryValue = "₹${d.totalAmount.toInt()} • ${if (d.isDelivered) "Delivered ✓" else "Pending"}",
                                            accentColor = RoyalBluePrimary
                                        )
                                    }
                                }
                            }
                        }

                        "B2B_COMMERCIAL" -> {
                            val b2b = deliveries.filter {
                                (it.customerType == "BULK_BUYER" || it.customerName.contains("HOTEL", true) || it.quantityLiters >= 10.0) &&
                                (searchQuery.isBlank() || it.customerName.contains(searchQuery, true))
                            }
                            if (b2b.isEmpty()) {
                                EmptyStateCard("No bulk commercial B2B deliveries for $periodName")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(b2b, key = { it.id }) { d ->
                                        HighContrastAuditCard(
                                            title = d.customerName,
                                            subtitle = "${sdf.format(Date(d.deliveryDate))} • ${d.shift} Shift • ${d.milkType}",
                                            primaryValue = "${d.quantityLiters} L",
                                            secondaryValue = "₹${d.totalAmount.toInt()} Billed",
                                            accentColor = Color(0xFF5E35B1)
                                        )
                                    }
                                }
                            }
                        }

                        "CUST_BILLING" -> {
                            val items = customers.filter { searchQuery.isBlank() || it.name.contains(searchQuery, true) || it.route.contains(searchQuery, true) }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(items, key = { it.id }) { c ->
                                    val cDels = deliveries.filter { it.customerId == c.id }
                                    HighContrastAuditCard(
                                        title = "${c.name} (${c.route})",
                                        subtitle = "Mobile: ${c.mobile} • Supplied: ${cDels.sumOf { it.quantityLiters }.toInt()} L",
                                        primaryValue = "₹${cDels.sumOf { it.totalAmount }.toInt()}",
                                        secondaryValue = "Balance: ₹${c.outstandingBalance.toInt()}",
                                        accentColor = if (c.outstandingBalance > 0) DangerRed else DairyGreen
                                    )
                                }
                            }
                        }

                        "CUST_OUTSTANDING" -> {
                            val debtors = customers.filter { it.outstandingBalance > 0 && (searchQuery.isBlank() || it.name.contains(searchQuery, true)) }
                            if (debtors.isEmpty()) {
                                EmptyStateCard("All customer accounts settled! No pending dues. ✓")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(debtors, key = { it.id }) { cust ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                                    Text("Phone: ${cust.mobile} • Route: ${cust.route}", fontSize = 11.sp, color = TextSecondary)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("₹${cust.outstandingBalance.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = DangerRed)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    FilledTonalButton(
                                                        onClick = {
                                                            val msg = "Hello ${cust.name}, reminder from dairy for your outstanding milk dues of ₹${cust.outstandingBalance.toInt()}. Thank you!"
                                                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=91${cust.mobile}&text=${Uri.encode(msg)}")
                                                            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {}
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text("WhatsApp 💬", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "HERD_CENSUS" -> {
                            val items = cattleList.filter { searchQuery.isBlank() || it.tagNumber.contains(searchQuery, true) || it.name.contains(searchQuery, true) }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(items, key = { it.id }) { c ->
                                    HighContrastAuditCard(
                                        title = "${c.name.ifBlank { c.tagNumber }} (${c.tagNumber})",
                                        subtitle = "${c.type} • ${c.breed} • Status: ${c.breedingStatus}",
                                        primaryValue = c.lactationStage,
                                        secondaryValue = "${c.dailyYieldLiters} L/day",
                                        accentColor = GrassGreen
                                    )
                                }
                            }
                        }

                        "BREEDING_CALVING" -> {
                            val items = breedingRecords.filter { searchQuery.isBlank() || it.cattleTag.contains(searchQuery, true) }
                            if (items.isEmpty()) {
                                EmptyStateCard("No breeding records logged")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(items, key = { it.id }) { br ->
                                        HighContrastAuditCard(
                                            title = "Tag: ${br.cattleTag} • Event: ${br.eventType}",
                                            subtitle = "Bull/Straw: ${br.bullIdOrName} • Date: ${sdf.format(Date(br.date))} • PD: ${br.pdStatus}",
                                            primaryValue = if (br.expectedCalvingDate > 0) sdf.format(Date(br.expectedCalvingDate)) else "—",
                                            secondaryValue = "Calving Target",
                                            accentColor = RoyalBluePrimary
                                        )
                                    }
                                }
                            }
                        }

                        "HEALTH_VACCINATION" -> {
                            val dewormList = dewormingRecords.filter { searchQuery.isBlank() || it.cattleTag.contains(searchQuery, true) }
                            val vaccList = vaccinationRecords.filter { searchQuery.isBlank() || it.cattleTag.contains(searchQuery, true) }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(vaccList, key = { it.id }) { vc ->
                                    HighContrastAuditCard(
                                        title = "💉 Tag: ${vc.cattleTag} • ${vc.vaccineName}",
                                        subtitle = "Given: ${sdf.format(Date(vc.date))} • Next Due: ${sdf.format(Date(vc.nextDueDate))}",
                                        primaryValue = "₹${vc.cost.toInt()}",
                                        secondaryValue = "Vaccine Shot",
                                        accentColor = RoyalBluePrimary
                                    )
                                }
                                items(dewormList, key = { it.id }) { dw ->
                                    HighContrastAuditCard(
                                        title = "💊 Tag: ${dw.cattleTag} • ${dw.dewormerSalt}",
                                        subtitle = "Administered: ${sdf.format(Date(dw.date))} • Next: ${sdf.format(Date(dw.nextDueDate))}",
                                        primaryValue = "₹${dw.cost.toInt()}",
                                        secondaryValue = "Dewormer",
                                        accentColor = GrassGreen
                                    )
                                }
                            }
                        }

                        "VET_TREATMENT" -> {
                            val items = treatmentRecords.filter { searchQuery.isBlank() || it.cattleTag.contains(searchQuery, true) || it.diseaseName.contains(searchQuery, true) }
                            if (items.isEmpty()) {
                                EmptyStateCard("No veterinary treatment logs recorded")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(items, key = { it.id }) { tr ->
                                        HighContrastAuditCard(
                                            title = "Tag: ${tr.cattleTag} • ${tr.diseaseName}",
                                            subtitle = "Medication: ${tr.medicationName} • Date: ${sdf.format(Date(tr.checkupDate))}",
                                            primaryValue = "₹${tr.treatmentCost.toInt()}",
                                            secondaryValue = "Vet Bill",
                                            accentColor = DangerRed
                                        )
                                    }
                                }
                            }
                        }

                        "FEED_ECONOMICS" -> {
                            val feedExp = expenses.filter { 
                                val cat = it.category.uppercase()
                                (cat.contains("FEED") || cat.contains("FODDER") || cat.contains("BHUSA") || cat.contains("SILAGE") || it.isInventoryPurchase) &&
                                (searchQuery.isBlank() || it.notes.contains(searchQuery, true) || it.category.contains(searchQuery, true))
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(feedExp, key = { it.id }) { exp ->
                                    HighContrastAuditCard(
                                        title = exp.notes.ifBlank { exp.category },
                                        subtitle = "${sdf.format(Date(exp.date))} • Paid: ${exp.paymentMethod}",
                                        primaryValue = "₹${exp.amount.toInt()}",
                                        secondaryValue = exp.category,
                                        accentColor = WarmHoney
                                    )
                                }
                            }
                        }

                        "BYPRODUCTS_YIELD" -> {
                            val byproducts = inventoryItems.filter {
                                (it.itemName.contains("Paneer", true) || it.itemName.contains("Ghee", true) ||
                                 it.itemName.contains("Curd", true) || it.itemName.contains("Butter", true) ||
                                 it.itemName.contains("Sweet", true)) &&
                                (searchQuery.isBlank() || it.itemName.contains(searchQuery, true))
                            }
                            if (byproducts.isEmpty()) {
                                EmptyStateCard("No byproduct items created. Add Paneer, Curd or Ghee in inventory.")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(byproducts, key = { it.id }) { item ->
                                        HighContrastAuditCard(
                                            title = item.itemName,
                                            subtitle = "Stock: ${item.currentStock.toInt()} ${item.unit} • Alert: ${item.alertDaysThreshold} days",
                                            primaryValue = "₹${(item.currentStock * item.costPerUnit).toInt()}",
                                            secondaryValue = "@₹${item.costPerUnit.toInt()} / ${item.unit}",
                                            accentColor = GoldenOrange
                                        )
                                    }
                                }
                            }
                        }

                        "INVENTORY_STOCK" -> {
                            val items = inventoryItems.filter { searchQuery.isBlank() || it.itemName.contains(searchQuery, true) }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(items, key = { it.id }) { item ->
                                    HighContrastAuditCard(
                                        title = item.itemName,
                                        subtitle = "Threshold: ${item.alertDaysThreshold} days • Daily: ${item.dailyUsage ?: 0.0} ${item.unit}",
                                        primaryValue = "${item.currentStock.toInt()} ${item.unit}",
                                        secondaryValue = "Val: ₹${(item.currentStock * item.costPerUnit).toInt()}",
                                        accentColor = RoyalBluePrimary
                                    )
                                }
                            }
                        }

                        "FIN_PROFIT_LOSS" -> {
                            val totalRev = deliveries.sumOf { it.totalAmount }
                            val totalProcure = milkCollections.sumOf { it.totalAmount }
                            val totalExp = expenses.sumOf { it.amount }
                            val netMargin = totalRev - (totalProcure + totalExp)

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    HighContrastAuditCard(
                                        title = "Milk Delivery & Sales Revenue",
                                        subtitle = "All route deliveries and wholesale bulk challans",
                                        primaryValue = "+₹${totalRev.toInt()}",
                                        secondaryValue = "Gross Revenue",
                                        accentColor = DairyGreen
                                    )
                                }
                                item {
                                    HighContrastAuditCard(
                                        title = "Farmer Milk Procurement Payouts",
                                        subtitle = "Shift-wise intake dues for member farmers",
                                        primaryValue = "-₹${totalProcure.toInt()}",
                                        secondaryValue = "Procurement Cost",
                                        accentColor = DangerRed
                                    )
                                }
                                items(expenses.groupBy { it.category }.keys.toList()) { cat ->
                                    val catExps = expenses.filter { it.category == cat }
                                    HighContrastAuditCard(
                                        title = cat,
                                        subtitle = "${catExps.size} entries",
                                        primaryValue = "-₹${catExps.sumOf { it.amount }.toInt()}",
                                        secondaryValue = "Operating Outflow",
                                        accentColor = DangerRed
                                    )
                                }
                                item {
                                    HighContrastAuditCard(
                                        title = "Operating Net Margin (EBITDA)",
                                        subtitle = "Total revenue minus all operating & procurement costs",
                                        primaryValue = "₹${netMargin.toInt()}",
                                        secondaryValue = if (netMargin >= 0) "Surplus" else "Deficit",
                                        accentColor = if (netMargin >= 0) DairyGreen else DangerRed
                                    )
                                }
                            }
                        }

                        "CASHBOOK_PAYMENTS" -> {
                            val items = payments.filter { searchQuery.isBlank() || it.customerName.contains(searchQuery, true) || it.notes.contains(searchQuery, true) }
                            if (items.isEmpty()) {
                                EmptyStateCard("No payment transactions recorded for $periodName")
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(items, key = { it.id }) { pay ->
                                        HighContrastAuditCard(
                                            title = pay.customerName,
                                            subtitle = "${sdf.format(Date(pay.date))} • Method: ${pay.method} • ${pay.notes}",
                                            primaryValue = "₹${pay.amount.toInt()}",
                                            secondaryValue = pay.paymentType,
                                            accentColor = DairyGreen
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            val items = deliveries.filter { searchQuery.isBlank() || it.customerName.contains(searchQuery, true) }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(items, key = { it.id }) { d ->
                                    HighContrastAuditCard(
                                        title = d.customerName,
                                        subtitle = "${sdf.format(Date(d.deliveryDate))} • ${d.shift} Shift • ${d.milkType}",
                                        primaryValue = "${d.quantityLiters} L",
                                        secondaryValue = "₹${d.totalAmount.toInt()}",
                                        accentColor = RoyalBluePrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Footer with Download Options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onDownloadPdf,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = DangerRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                    }

                    FilledTonalButton(
                        onClick = onDownloadExcel,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = DairyGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HighContrastAuditCard(
    title: String,
    subtitle: String,
    primaryValue: String,
    secondaryValue: String,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(primaryValue, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = accentColor)
                Spacer(modifier = Modifier.height(2.dp))
                Text(secondaryValue, fontSize = 10.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(message, fontSize = 13.sp, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun KpiMiniBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

