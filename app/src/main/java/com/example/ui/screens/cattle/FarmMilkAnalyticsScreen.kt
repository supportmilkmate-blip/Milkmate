package com.example.ui.screens.cattle

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CattleMilkingRecordEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Farm Milk Production Analytics & Record Screen
 * Matching video recording at 01:33 - 02:00:
 * - Tabs: Day | Week | Month | Year
 * - Date navigator (<, >, Calendar)
 * - Beautiful Bar Charts (7-day, 31-day, 12-month)
 * - Detailed breakdown list of production per period
 * - Bottom "+ Add Production" floating bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmMilkAnalyticsScreen(
    milkingRecords: List<CattleMilkingRecordEntity>,
    onOpenAddForEachCattle: () -> Unit,
    onOpenAddForWholeFarm: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) } // 0: Day, 1: Week, 2: Month, 3: Year (Defaults to Week)
    var dateOffset by remember { mutableIntStateOf(0) } // Offset from current period

    val dayMillis = 86400000L
    val calendar = Calendar.getInstance()

    val (periodTitle, weekDays, monthDays, yearMonths) = remember(selectedTab, dateOffset, milkingRecords) {
        val cal = Calendar.getInstance()

        when (selectedTab) {
            0 -> {
                // Day
                cal.add(Calendar.DAY_OF_YEAR, dateOffset)
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val todayStr = if (dateOffset == 0) "Today" else if (dateOffset == -1) "Yesterday" else sdf.format(cal.time)
                Tuple4(todayStr, emptyList<DailyYield>(), emptyList<DailyYield>(), emptyList<MonthlyYield>())
            }
            1 -> {
                // Week
                cal.add(Calendar.WEEK_OF_YEAR, dateOffset)
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                val days = mutableListOf<DailyYield>()
                val dayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val shortDayNames = listOf("S", "M", "T", "W", "T", "F", "S")

                for (i in 0 until 7) {
                    val dMidnight = cal.timeInMillis
                    val dayName = shortDayNames[i]
                    val dateFormatted = dayFormat.format(cal.time)
                    val recs = milkingRecords.filter { it.dateEpochMidnight in dMidnight..(dMidnight + dayMillis - 1) }
                    val totalL = recs.sumOf { it.quantityLiters }
                    days.add(DailyYield(label = dayName, dateFormatted = dateFormatted, liters = totalL, epochMidnight = dMidnight))
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }

                val title = if (dateOffset == 0) "This week" else "Week of ${days.first().dateFormatted}"
                Tuple4(title, days, emptyList(), emptyList())
            }
            2 -> {
                // Month
                cal.add(Calendar.MONTH, dateOffset)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val title = if (dateOffset == 0) "This Month" else monthFormat.format(cal.time)
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val dayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                val days = mutableListOf<DailyYield>()
                for (d in 1..daysInMonth) {
                    val dMidnight = cal.timeInMillis
                    val recs = milkingRecords.filter { it.dateEpochMidnight in dMidnight..(dMidnight + dayMillis - 1) }
                    val totalL = recs.sumOf { it.quantityLiters }
                    days.add(DailyYield(label = d.toString(), dateFormatted = dayFormat.format(cal.time), liters = totalL, epochMidnight = dMidnight))
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                Tuple4(title, emptyList(), days, emptyList())
            }
            else -> {
                // Year
                cal.add(Calendar.YEAR, dateOffset)
                val year = cal.get(Calendar.YEAR)
                val monthNames = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                val fullMonthNames = listOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )

                val months = mutableListOf<MonthlyYield>()
                for (m in 0 until 12) {
                    val monthCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, m)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val startM = monthCal.timeInMillis
                    val maxDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    monthCal.set(Calendar.DAY_OF_MONTH, maxDay)
                    monthCal.set(Calendar.HOUR_OF_DAY, 23)
                    val endM = monthCal.timeInMillis

                    val recs = milkingRecords.filter { it.dateEpochMidnight in startM..endM }
                    val totalL = recs.sumOf { it.quantityLiters }
                    months.add(MonthlyYield(shortName = monthNames[m], fullName = fullMonthNames[m], liters = totalL))
                }

                val title = if (dateOffset == 0) "This Year" else "$year"
                Tuple4(title, emptyList(), emptyList(), months)
            }
        }
    }

    val totalProducedInPeriod = remember(selectedTab, weekDays, monthDays, yearMonths) {
        when (selectedTab) {
            1 -> weekDays.sumOf { it.liters }
            2 -> monthDays.sumOf { it.liters }
            3 -> yearMonths.sumOf { it.liters }
            else -> 0.0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Milk Production",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("analytics_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onOpenAddForEachCattle,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(48.dp)
                            .testTag("add_production_bottom_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add Production", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF6F8FC))
        ) {
            // Day | Week | Month | Year Tab Switcher (matches video 01:33)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = RoyalBluePrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Day", "Week", "Month", "Year").forEachIndexed { index, text ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            dateOffset = 0
                        },
                        text = {
                            Text(
                                text = text,
                                fontSize = 13.5.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // Period Selector with < and > (matches video 01:36)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = periodTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        IconButton(onClick = { dateOffset -= 1 }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous", modifier = Modifier.size(16.dp))
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        IconButton(onClick = { dateOffset += 1 }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(16.dp))
                        }
                    }

                    if (selectedTab == 0) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalBlueLight,
                            modifier = Modifier.size(34.dp)
                        ) {
                            IconButton(onClick = { dateOffset = 0 }) {
                                Icon(Icons.Default.Today, contentDescription = "Today", tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // TAB 0: DAY VIEW (matches video 01:34)
            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "No Data",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Track your milk production by adding for each cattle or for the whole farm",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = onOpenAddForEachCattle,
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.fillMaxWidth(0.6f)
                                    ) {
                                        Text("+ Add For each cattle", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = onOpenAddForWholeFarm,
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.fillMaxWidth(0.6f)
                                    ) {
                                        Text("+ Add For whole farm", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Resting Cow Illustration (matches video 01:34)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF3F7FD),
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🐄", fontSize = 28.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Farm Milking Register", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RoyalBluePrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 1: WEEK VIEW (matches video 01:37)
            if (selectedTab == 1) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Stat Card (matches video 01:37)
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF3E8FF),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(22.dp))
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%.2f", totalProducedInPeriod)} L",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Milk Produced",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                // 7-Day Bar Chart (matches video 01:37)
                                WeekBarChart(weekDays = weekDays)
                            }
                        }
                    }

                    // Section Title
                    item {
                        Text(
                            text = "This week",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Day Breakdown List (reversed order: latest day first)
                    items(weekDays.reversed()) { day ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 0.5.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = day.dateFormatted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (day.liters > 0.0) "${String.format(Locale.getDefault(), "%.2f", day.liters)} L" else "No Data",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (day.liters > 0.0) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // TAB 2: MONTH VIEW (matches video 01:40)
            if (selectedTab == 2) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF3E8FF),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(22.dp))
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%.2f", totalProducedInPeriod)} L",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Milk Produced",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                // 31-Day Bar Chart
                                MonthBarChart(monthDays = monthDays)
                            }
                        }
                    }

                    item {
                        Text(
                            text = "This Month",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Month days breakdown list (reversed: 31st down to 1st)
                    items(monthDays.reversed()) { day ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 0.5.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = day.dateFormatted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (day.liters > 0.0) "${String.format(Locale.getDefault(), "%.2f", day.liters)} L" else "No Data",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (day.liters > 0.0) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // TAB 3: YEAR VIEW (matches video 01:50)
            if (selectedTab == 3) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF3E8FF),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(22.dp))
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%.2f", totalProducedInPeriod)} L",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Milk Produced",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                // 12-Month Bar Chart
                                YearBarChart(yearMonths = yearMonths)
                            }
                        }
                    }

                    item {
                        Text(
                            text = "This Year",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // 12 Months Breakdown List
                    items(yearMonths) { month ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 0.5.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = month.fullName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (month.liters > 0.0) "${String.format(Locale.getDefault(), "%.2f", month.liters)} L" else "No Data",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (month.liters > 0.0) TextPrimary else TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- CHART DRAWING COMPONENTS ----------------

@Composable
fun WeekBarChart(weekDays: List<DailyYield>) {
    val maxLiters = remember(weekDays) {
        val m = weekDays.maxOfOrNull { it.liters } ?: 50.0
        if (m < 50.0) 50.0 else m * 1.15
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Y-Axis labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("${maxLiters.toInt()}L", fontSize = 10.sp, color = TextSecondary)
                Text("${(maxLiters / 2).toInt()}L", fontSize = 10.sp, color = TextSecondary)
                Text("0L", fontSize = 10.sp, color = TextSecondary)
            }

            // 7 Vertical Bars Canvas
            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val barWidth = 24.dp.toPx()
                val spacing = (size.width - (barWidth * 7)) / 8
                val availableHeight = size.height - 10.dp.toPx()

                weekDays.forEachIndexed { i, day ->
                    val x = spacing + i * (barWidth + spacing)
                    val barHeight = if (maxLiters > 0) ((day.liters / maxLiters) * availableHeight).toFloat().coerceIn(4f, availableHeight.toFloat()) else 4f
                    val y = size.height - barHeight

                    drawRoundRect(
                        color = if (day.liters > 0) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }
        }

        // Day Labels (S M T W T F S)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp, top = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekDays.forEach { day ->
                Text(
                    text = day.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MonthBarChart(monthDays: List<DailyYield>) {
    val maxLiters = remember(monthDays) {
        val m = monthDays.maxOfOrNull { it.liters } ?: 50.0
        if (m < 50.0) 50.0 else m * 1.15
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("${maxLiters.toInt()}L", fontSize = 10.sp, color = TextSecondary)
                Text("${(maxLiters / 2).toInt()}L", fontSize = 10.sp, color = TextSecondary)
                Text("0L", fontSize = 10.sp, color = TextSecondary)
            }

            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val count = monthDays.size.coerceAtLeast(28)
                val barWidth = (size.width / (count * 1.4f)).coerceIn(4f, 14f)
                val spacing = (size.width - (barWidth * count)) / (count + 1)
                val availableHeight = size.height - 10.dp.toPx()

                monthDays.forEachIndexed { i, day ->
                    val x = spacing + i * (barWidth + spacing)
                    val barHeight = if (maxLiters > 0) ((day.liters / maxLiters) * availableHeight).toFloat().coerceIn(3f, availableHeight.toFloat()) else 3f
                    val y = size.height - barHeight

                    drawRoundRect(
                        color = if (day.liters > 0) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp, top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("1", "5", "10", "15", "20", "25", "31").forEach {
                Text(text = it, fontSize = 9.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun YearBarChart(yearMonths: List<MonthlyYield>) {
    val maxLiters = remember(yearMonths) {
        val m = yearMonths.maxOfOrNull { it.liters } ?: 1000.0
        if (m < 500.0) 500.0 else m * 1.15
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("${maxLiters.toInt()}L", fontSize = 10.sp, color = TextSecondary)
                Text("${(maxLiters / 2).toInt()}L", fontSize = 10.sp, color = TextSecondary)
                Text("0L", fontSize = 10.sp, color = TextSecondary)
            }

            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val barWidth = 16.dp.toPx()
                val spacing = (size.width - (barWidth * 12)) / 13
                val availableHeight = size.height - 10.dp.toPx()

                yearMonths.forEachIndexed { i, month ->
                    val x = spacing + i * (barWidth + spacing)
                    val barHeight = if (maxLiters > 0) ((month.liters / maxLiters) * availableHeight).toFloat().coerceIn(4f, availableHeight.toFloat()) else 4f
                    val y = size.height - barHeight

                    drawRoundRect(
                        color = if (month.liters > 0) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp, top = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            yearMonths.forEach { month ->
                Text(
                    text = month.shortName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

data class DailyYield(
    val label: String,
    val dateFormatted: String,
    val liters: Double,
    val epochMidnight: Long
)

data class MonthlyYield(
    val shortName: String,
    val fullName: String,
    val liters: Double
)

data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
