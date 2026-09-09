package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TrafficRepository
import com.example.ui.components.AppScreen
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun AnalyticsScreen(
    repository: TrafficRepository,
    onNavigateToScreen: (AppScreen) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val allViolations by repository.allViolations.collectAsState(initial = emptyList())

    var activeTab by remember { mutableStateOf("Acoustic Feed") }
    var searchQuery by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Equalizer oscillating bars animation
    val infiniteTransition = rememberInfiniteTransition(label = "eqAnim")
    val eqFrequencies = List(16) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f + (index % 4) * 0.15f,
            targetValue = 0.9f - (index % 3) * 0.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(280 + index * 35, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eqBar$index"
        )
    }

    // Filtered search list
    val searchResults = remember(allViolations, searchQuery) {
        if (searchQuery.isBlank()) allViolations
        else allViolations.filter {
            it.numberPlate.contains(searchQuery, ignoreCase = true) ||
                    it.challanNumber.contains(searchQuery) ||
                    it.violationType.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // TOP ANALYTICS HEADER & SUB-TABS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ACOUSTIC HUD & ENFORCEMENT",
                                color = PrimaryNeon,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.08.sp
                            )
                        }
                        Text(
                            text = "Sound Triangulation & Revenue Matrix",
                            color = PrimaryText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LIVE SENSORS",
                            color = TertiaryFixed,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Dual Tab Switcher: Acoustic Feed vs Revenue & Challans
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainer)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Acoustic Feed", "Revenue & Challans").forEach { tab ->
                        val isSelected = activeTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) SurfaceContainerHighest else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) PrimaryNeon.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { activeTab = tab }
                                .padding(vertical = 8.dp)
                                .testTag("analytics_tab_${tab.lowercase().replace(" ", "_")}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) PrimaryNeon else OnSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        if (activeTab == "Acoustic Feed") {
            // ==================== ACOUSTIC FEED SECTION ====================

            // 1. SILENT ZONE SENSOR ARRAY #08: 92 dB (+12 dB Breach)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, ErrorBright.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Campaign, contentDescription = null, tint = ErrorBright, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Silent Zone Sensor Array #08",
                                color = PrimaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ErrorBright)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+12 dB BREACH",
                                color = OnError,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Large SPL Decibel Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("CURRENT DECIBEL SPL", color = OnSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("92", color = ErrorBright, fontSize = 42.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
                                Text("dB", color = ErrorCrimson, fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("HOSPITAL CEILING", color = OnSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("80 dB MAX", color = SecondaryBlue, fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("Zone: MG Road AIIMS", color = OnSurfaceVariant, fontSize = 9.sp)
                        }
                    }

                    // Real-time Oscillating Equalizer Spectrum Bars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceContainerLowest)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        eqFrequencies.forEachIndexed { i, anim ->
                            val h = anim.value
                            val barColor = if (h > 0.7f) ErrorBright else if (h > 0.45f) Color(0xFFFFB800) else TertiaryEmerald
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .fillMaxHeight(h)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor)
                            )
                        }
                    }
                }
            }

            // 2. MINI BENTO METRICS: Peak Sustained, Burst Rate, Zone Health
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("PEAK SUSTAINED", color = OnSurfaceVariant, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text("4.2s", color = ErrorBright, fontSize = 17.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("Heavy Horn Spike", color = OnSurfaceVariant, fontSize = 8.sp)
                        }
                    }

                    // Card 2
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("BURST RATE", color = OnSurfaceVariant, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text("14 / min", color = SecondaryBlue, fontSize = 17.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("Junction Transit", color = OnSurfaceVariant, fontSize = 8.sp)
                        }
                    }

                    // Card 3
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("ZONE HEALTH", color = OnSurfaceVariant, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text("71%", color = TertiaryFixed, fontSize = 17.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("Hospital Pass Rate", color = OnSurfaceVariant, fontSize = 8.sp)
                        }
                    }
                }
            }

            // 3. ACOUSTIC BREACH TIMELINE SPLINE CURVE CHART
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLow)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Acoustic Breach Timeline (24h)",
                            color = PrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "-- 80 dB SILENT CEILING",
                            color = ErrorCrimson,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Spline Vector Chart Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceContainerLowest)
                            .padding(6.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // 80 dB Ceiling Line (dashed red)
                            val ceilingY = h * 0.4f
                            drawLine(
                                color = ErrorCrimson.copy(alpha = 0.6f),
                                start = Offset(0f, ceilingY),
                                end = Offset(w, ceilingY),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )

                            // Spline curve points (dB over 24h)
                            val points = listOf(
                                Offset(0f, h * 0.75f),
                                Offset(w * 0.15f, h * 0.8f),
                                Offset(w * 0.3f, h * 0.55f),
                                Offset(w * 0.45f, h * 0.25f), // 94 dB peak!
                                Offset(w * 0.6f, h * 0.45f),
                                Offset(w * 0.75f, h * 0.2f), // 96 dB peak!
                                Offset(w * 0.9f, h * 0.5f),
                                Offset(w, h * 0.65f)
                            )

                            val path = Path()
                            path.moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val curr = points[i]
                                val cx = (prev.x + curr.x) / 2f
                                path.cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                            }

                            // Draw stroke
                            drawPath(
                                path = path,
                                color = PrimaryNeon,
                                style = Stroke(width = 2.5f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("00:00", color = OutlineColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("06:00", color = OutlineColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("12:00 (PEAK)", color = ErrorBright, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("18:00 (EVENING SURGE)", color = PrimaryNeon, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("23:59", color = OutlineColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        } else {
            // ==================== REVENUE & CHALLANS SECTION ====================

            // 1. E-CHALLAN COLLECTION MATRIX SUMMARY
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, TertiaryEmerald.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "E-Challan Collection Matrix",
                            color = PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+18.4% WoW REVENUE",
                            color = TertiaryFixed,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("GROSS PENALTIES GENERATED", color = OnSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("₹3,42,000", color = PrimaryText, fontSize = 28.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("RECOVERY EFFICIENCY", color = OnSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("63.8% SETTLED", color = TertiaryFixed, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Segmented progress bar: Paid vs Unpaid
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceContainerHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(63.8f)
                                    .fillMaxHeight()
                                    .background(TertiaryEmerald)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(36.2f)
                                    .fillMaxHeight()
                                    .background(ErrorBright)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("₹2,18,000 Settled (63.8%)", color = TertiaryFixed, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("₹1,24,000 Unpaid (36.2%)", color = ErrorCrimson, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Overdue alert
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(ErrorContainer.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = ErrorBright, modifier = Modifier.size(14.dp))
                            Text(
                                text = "42 Overdue Cases (>30 days) marked for Regional Transport Office RTO summons.",
                                color = ErrorCrimson,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // 2. SEARCH VEHICLE CHALLAN DATABASE
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SEARCH VEHICLE CHALLAN DATABASE",
                        color = OutlineColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by Plate (e.g. OD 02) or Challan #...", color = OutlineColor, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryNeon,
                            unfocusedBorderColor = OutlineVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("challan_search_field")
                    )
                }
            }

            // 3. ACTION DISPATCH BUTTONS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                repository.recordAudit(
                                    "BULK_SMS_REMINDERS_DISPATCHED",
                                    "Dispatched automated bulk SMS payment warnings to 34 unpaid offenders."
                                )
                                toastMessage = "Bulk SMS Reminders dispatched to 34 pending vehicle owners."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("bulk_sms_button")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TRIGGER BULK SMS REMINDERS", color = OnPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    repository.recordAudit("PDF_AUDIT_EXPORTED", "Exported formal government PDF challan ledger.")
                                    toastMessage = "Compliance PDF audit report generated & saved."
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AUDIT REPORT (PDF)", color = PrimaryNeon, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    repository.recordAudit("CSV_EXPORTED", "Exported CSV analytics dataset.")
                                    toastMessage = "CSV telemetry dataset exported to downloads."
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXPORT CSV", color = SecondaryBlue, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // 4. SEARCH RESULTS / CHALLAN LIST
            items(searchResults.take(6), key = { "search_${it.id}" }) { challan ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerLow)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(challan.numberPlate, color = PrimaryText, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("#${challan.challanNumber}", color = PrimaryNeon, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        Text(challan.violationType, color = OnSurfaceVariant, fontSize = 10.sp)
                        Text(challan.location, color = OutlineColor, fontSize = 9.sp)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("₹${challan.fineAmount}", color = PrimaryText, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (challan.isPaid) TertiaryEmerald.copy(alpha = 0.2f) else ErrorBright.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (challan.isPaid) "PAID" else "UNPAID",
                                color = if (challan.isPaid) TertiaryFixed else ErrorBright,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Toast alert
    toastMessage?.let { msg ->
        LaunchedEffect(msg) {
            delay(2400)
            toastMessage = null
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 70.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = SurfaceContainerHighest,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, PrimaryNeon),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = msg,
                    color = PrimaryText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
