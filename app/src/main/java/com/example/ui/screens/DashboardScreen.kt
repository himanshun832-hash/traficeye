package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.TrafficRepository
import com.example.data.ViolationRecord
import com.example.ui.components.AppScreen
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ArterialNode(
    val id: String,
    val name: String,
    val status: String,
    val vehicles: Int,
    val speedKmh: Int,
    val noiseDb: Int,
    val queueSec: Int,
    val aiGreenPhaseSec: Int,
    val xRatio: Float,
    val yRatio: Float,
    val statusColor: Color
)

@Composable
fun DashboardScreen(
    repository: TrafficRepository,
    onNavigateToScreen: (AppScreen) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val allViolations by repository.allViolations.collectAsState(initial = emptyList())
    val currentUser by repository.currentUser.collectAsState()

    // Selected violation for inspector modal
    var selectedTableViolation by remember { mutableStateOf<ViolationRecord?>(null) }
    var dashboardToast by remember { mutableStateOf<String?>(null) }

    // Simulation pipeline state
    var isSimulating by remember { mutableStateOf(false) }
    var simStep by remember { mutableStateOf(0) }
    var simLogText by remember { mutableStateOf("Awaiting trigger command...") }
    var simLogStatus by remember { mutableStateOf("IDLE") }

    // Dynamic Live Enforcement items
    val liveEnforcementFeed = remember {
        mutableStateListOf(
            Triple("EXCESSIVE HONKING (92 dB)", "OD 02 AB 1234", "Junction 4A Acoustic Sensor cluster #03 • Fine ₹2,000"),
            Triple("HEAVY CONGESTION SPIKE", "SOUTH EXPWY", "Adaptive lane phase extended to prevent gridlock"),
            Triple("RED LIGHT VIOLATION", "KA 05 MH 8821", "White SUV crossed stop line at 44 km/h • Billboard Alert Triggered")
        )
    }

    // Map Nodes
    val nodes = remember {
        listOf(
            ArterialNode("A", "Ring Road Intersect", "CRITICAL", 412, 34, 92, 180, 75, 0.32f, 0.48f, ErrorBright),
            ArterialNode("B", "Tech Park Flyover", "HEAVY", 289, 42, 78, 95, 50, 0.68f, 0.48f, SecondaryBlue),
            ArterialNode("C", "Cyber City Gateway", "OPTIMAL", 144, 58, 64, 40, 35, 0.85f, 0.22f, TertiaryEmerald)
        )
    }
    var selectedNode by remember { mutableStateOf(nodes[0]) }

    // Signal Phase Control State
    var isAiAutoRegulating by remember { mutableStateOf(true) }
    var isEmergencySeizureActive by remember { mutableStateOf(false) }
    var greenCountdown by remember { mutableStateOf(45) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (greenCountdown > 0) greenCountdown-- else greenCountdown = 45
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
        // TOP METROPOLIS GRID CORE BANNER & SIMULATE VIOLATION BUTTON
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, PrimaryNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceContainerHigh)
                                    .border(1.dp, PrimaryNeon.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.SatelliteAlt,
                                    contentDescription = null,
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Metropolis Grid Core",
                                        color = PrimaryText,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(TertiaryEmerald.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "LIVE V4.8",
                                            color = TertiaryFixed,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "Real-time edge telemetry, acoustic triangulation & ANPR relays.",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Simulate Violation Button (Admin Police Only)
                    if (currentUser?.role == "ADMIN_POLICE") {
                        Button(
                            onClick = {
                                if (!isSimulating) {
                                    coroutineScope.launch {
                                        isSimulating = true
                                        simStep = 1
                                        simLogText = "Vehicle detected crossing Ring Road loop"
                                        simLogStatus = "DETECTION 100%"
                                        delay(900)

                                        simStep = 2
                                        simLogText = "ANPR OCR Reading: OD 02 AB 1234 (Black Sedan)"
                                        simLogStatus = "OCR 99.4%"
                                        delay(900)

                                        simStep = 3
                                        simLogText = "Acoustic Sensor Triangulation: 94 dB Honk confirmed"
                                        simLogStatus = "NOISE CRITICAL"
                                        delay(900)

                                        simStep = 4
                                        simLogText = "Challan Created: Fine ₹2,000 debited to owner Vahan registry"
                                        simLogStatus = "REGISTRY NOTIFIED"
                                        delay(900)

                                        simStep = 5
                                        simLogText = "Public Shaming Billboard relay updated: Plate OD 02 AB 1234"
                                        simLogStatus = "COMPLETED"

                                        // Add to live enforcement stream
                                        liveEnforcementFeed.add(
                                            0,
                                            Triple(
                                                "SIMULATED: ACOUSTIC HONK (94 dB)",
                                                "OD 02 AB 1234",
                                                "Fine ₹2,000 issued & Billboard Feed Triggered"
                                            )
                                        )

                                        repository.recordAudit(
                                            "SIMULATED_VIOLATION_TRIGGERED",
                                            "Automated enforcement pipeline simulated: OD 02 AB 1234 debited ₹2,000"
                                        )

                                        delay(1200)
                                        isSimulating = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("simulate_violation_button")
                        ) {
                            Icon(
                                Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = OnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSimulating) "ENFORCING SIMULATION..." else "⚡ SIMULATE VIOLATION",
                                color = OnPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.08.sp
                            )
                        }

                        // Simulation Progress Bar Indicator
                        AnimatedVisibility(visible = isSimulating || simStep > 0) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceContainerLowest.copy(alpha = 0.8f))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "AUTOMATED ENFORCEMENT ENGINE",
                                        color = PrimaryNeon,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "STEP $simStep/5",
                                        color = SecondaryBlue,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // 5 Stage Indicator Bars
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (i in 1..5) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    if (simStep >= i) PrimaryNeon else SurfaceContainerHighest
                                                )
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SurfaceContainerLow, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = simLogText,
                                        color = TertiaryFixed,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = simLogStatus,
                                        color = OnSurfaceVariant,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // TELEMETRY HUD METRICS BENTO GRID
        item {
            val totalViolationsCount = allViolations.size
            val unpaidAmount = allViolations.filter { !it.isPaid }.sumOf { it.fineAmount }
            val paidAmount = allViolations.filter { it.isPaid }.sumOf { it.fineAmount }
            val honkingCount = allViolations.count { it.violationType.contains("Honk", ignoreCase = true) || it.noiseDb >= 85 }
            val activeAlertsCount = allViolations.count { !it.isPaid } + 3

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Total Vehicles & Total Violations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: Total Vehicles
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, OutlineColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TOTAL VEHICLES", color = OnSurfaceVariant, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(14.dp))
                            }
                            Text("12,458", color = PrimaryText, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("+14.2% peak surge", color = TertiaryFixed, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Card 2: Total Violations
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, OutlineColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TOTAL VIOLATIONS", color = OnSurfaceVariant, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = ErrorBright, modifier = Modifier.size(14.dp))
                            }
                            Text("$totalViolationsCount", color = ErrorBright, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("E-Challans logged", color = OnSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Row 2: Unpaid Fines & Paid Fines
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 3: Unpaid Fines
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, ErrorBright.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("UNPAID FINES", color = ErrorBright, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = ErrorBright, modifier = Modifier.size(14.dp))
                            }
                            Text("₹$unpaidAmount", color = ErrorBright, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("${allViolations.count { !it.isPaid }} Pending Notices", color = OnSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Card 4: Paid Fines
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, TertiaryEmerald.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("PAID FINES", color = TertiaryEmerald, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TertiaryEmerald, modifier = Modifier.size(14.dp))
                            }
                            Text("₹$paidAmount", color = TertiaryEmerald, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("${allViolations.count { it.isPaid }} Settled Challans", color = TertiaryFixed, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Row 3: Excessive Honking & Active Alerts & Recent Violations Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 5: Excessive Honking Violations
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, WarningAmber.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("HONKING VIOLATIONS", color = WarningAmber, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Icon(Icons.Filled.Campaign, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                            }
                            Text("$honkingCount", color = WarningAmber, fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("Silent Zone Breaches", color = OnSurfaceVariant, fontSize = 8.sp)
                        }
                    }

                    // Card 6: Active Alerts
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, OutlineColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ACTIVE ALERTS", color = OnSurfaceVariant, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(14.dp))
                            }
                            Text("$activeAlertsCount", color = SecondaryBlue, fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("Grid Saturation & Sound", color = OnSurfaceVariant, fontSize = 8.sp)
                        }
                    }
                }
            }
        }

        // INTERACTIVE ARTERIAL CARTOGRAPHY & NODES
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, PrimaryNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
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
                        Icon(Icons.Filled.Hub, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Live Arterial Cartography & Nodes",
                            color = PrimaryText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "TAP NODE FOR TELEMETRY",
                        color = OnSurfaceVariant,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Vector Map Canvas with nodes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerLowest)
                ) {
                    // Draw simulated cybernetic arterial grid roads
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Grid lines
                        for (i in 0..10) {
                            val x = w * (i / 10f)
                            drawLine(Color(0xFF161B2A), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                        }
                        for (i in 0..6) {
                            val y = h * (i / 6f)
                            drawLine(Color(0xFF161B2A), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                        }

                        // Arterial roads
                        drawLine(Color(0xFF252A39), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 12f)
                        drawLine(Color(0xFF252A39), Offset(w * 0.35f, 0f), Offset(w * 0.35f, h), strokeWidth = 10f)
                        drawLine(Color(0xFF252A39), Offset(w * 0.7f, 0f), Offset(w * 0.7f, h), strokeWidth = 10f)

                        // Glowing traffic flow pulses
                        drawLine(ErrorBright.copy(alpha = 0.8f), Offset(0f, h * 0.5f), Offset(w * 0.35f, h * 0.5f), strokeWidth = 3f)
                        drawLine(SecondaryBlue.copy(alpha = 0.8f), Offset(w * 0.35f, h * 0.5f), Offset(w * 0.7f, h * 0.5f), strokeWidth = 3f)
                        drawLine(TertiaryEmerald.copy(alpha = 0.8f), Offset(w * 0.7f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 3f)
                    }

                    // Node Buttons on Map
                    nodes.forEach { node ->
                        val isSelected = selectedNode.id == node.id
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(
                                    x = (node.xRatio * 320).dp,
                                    y = (node.yRatio * 180).dp
                                )
                                .clickable { selectedNode = node }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(node.statusColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color.White else node.statusColor.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = node.id,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Selected Node Telemetry HUD Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                            .background(SurfaceContainerHigh.copy(alpha = 0.92f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
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
                                        text = "NODE ${selectedNode.id}: ${selectedNode.name}",
                                        color = PrimaryText,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = selectedNode.status,
                                        color = selectedNode.statusColor,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${selectedNode.vehicles} Veh • ${selectedNode.speedKmh} km/h • ${selectedNode.noiseDb} dB SPL • Queue ${selectedNode.queueSec}s",
                                    color = OnSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Button(
                                onClick = { onNavigateToScreen(AppScreen.TRAFFIC) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("CONTROL", color = OnPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Quick Node Select Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    nodes.forEach { node ->
                        val isSelected = selectedNode.id == node.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) SurfaceContainerHighest else SurfaceContainer)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) node.statusColor else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedNode = node }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Node ${node.id} (${node.status})",
                                color = if (isSelected) node.statusColor else OnSurfaceVariant,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // SIGNAL PHASE CONTROL PANEL
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, TertiaryEmerald.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
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
                        Icon(Icons.Filled.Traffic, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Signal Phase Control",
                            color = PrimaryText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(TertiaryEmerald.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("ACTIVE • CYCLE 120s", color = TertiaryFixed, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                // 3 Light Phase Widgets (Red 12s, Yellow 03s, Green Run)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // RED
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainer)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.StopCircle, contentDescription = null, tint = ErrorBright.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            Text("12s", color = ErrorBright, fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("RED", color = OnSurfaceVariant, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // YELLOW
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainer)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = SecondaryBlue.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            Text("03s", color = SecondaryBlue, fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("YELLOW", color = OnSurfaceVariant, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // GREEN (Active)
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerHighest)
                            .border(1.5.dp, TertiaryEmerald, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TertiaryEmerald, modifier = Modifier.size(18.dp))
                            Text("${greenCountdown}s", color = TertiaryFixed, fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("GREEN (RUN)", color = TertiaryFixed, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // AI dynamic optimization status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceContainerLowest)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Psychology, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(16.dp))
                    Text(
                        text = "AI extended +15s green phase due to ring road congestion spike.",
                        color = PrimaryText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // AI Auto Regulation Toggle & Emergency Seizure (Admin Only)
                if (currentUser?.role == "ADMIN_POLICE") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("AI Auto-Regulation", color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Reinforcement Learning Node #4", color = OnSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Switch(
                            checked = isAiAutoRegulating,
                            onCheckedChange = { isAiAutoRegulating = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryNeon,
                                checkedTrackColor = PrimaryNeon.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Button(
                        onClick = {
                            isEmergencySeizureActive = !isEmergencySeizureActive
                            coroutineScope.launch {
                                repository.recordAudit(
                                    if (isEmergencySeizureActive) "EMERGENCY_OVERRIDE_ENGAGED" else "EMERGENCY_OVERRIDE_CLEARED",
                                    "Emergency override manual button toggled: ${if (isEmergencySeizureActive) "LOCK ACTIVATED" else "NORMAL PHASING"}"
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEmergencySeizureActive) ErrorBright else ErrorContainer
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("emergency_override_button")
                    ) {
                        Icon(Icons.Filled.Emergency, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEmergencySeizureActive) "EMERGENCY SEIZURE LOCK (ENGAGED)" else "MANUAL EMERGENCY OVERRIDE",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceContainerLowest)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TertiaryEmerald, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Dynamic AI Phasing: Active & Auto-Optimized",
                            color = PrimaryText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // LIVE ENFORCEMENT STREAM TICKER
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, OutlineColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        Icon(Icons.Filled.FmdBad, contentDescription = null, tint = ErrorBright, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Live Enforcement Stream",
                            color = PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("AUTO-REFRESH: 0.8s", color = TertiaryFixed, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }

                liveEnforcementFeed.take(4).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceContainer)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ErrorBright.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = ErrorBright, modifier = Modifier.size(15.dp))
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(item.first, color = ErrorBright, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    Text(item.second, color = PrimaryNeon, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                                Text(item.third, color = OnSurfaceVariant, fontSize = 10.sp)
                            }
                        }

                        Text("JUST NOW", color = SecondaryBlue, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // RECENT VIOLATIONS TABLE (REQUIREMENT)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, PrimaryNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                            Icon(Icons.Filled.TableChart, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Recent Violations",
                                color = PrimaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "REAL-TIME E-CHALLAN LEDGER & EVIDENCE DOSSIER",
                            color = OnSurfaceVariant,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PrimaryNeon.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${allViolations.size} RECORDS",
                            color = PrimaryNeon,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Table Entries
                allViolations.take(6).forEach { violation ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainer)
                            .border(
                                1.dp,
                                if (!violation.isPaid) ErrorBright.copy(alpha = 0.35f) else TertiaryEmerald.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Row 1: Dual Photos (Vehicle + Owner) + License Plate + Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Vehicle Snapshot
                                    AsyncImage(
                                        model = violation.evidenceImageUrl.ifEmpty {
                                            "https://lh3.googleusercontent.com/aida-public/AB6AXuBK6Q96CW_G_sVE-zpZhtMxKSd-pQ70YA0KEzZOsWVq_FwfL3cpGPXbIdn8hfSGq0E0MiYcSKKQH4zEdkrwetBUAppkZi5N3GQh2yQ6b3sXYUNvuu5pLz3YMgcrQYFLtaIS9_xHFlOgG5v769bypf7PoBldrs3OQdIMY72c_JLc49hZ2f3O2DxqgVBrDkkyVcu5vwzMnnzp_YR8MN71ESeBco2X9p_EHQO96KCVKng7JxeBFJ2FeBQ"
                                        },
                                        contentDescription = "Vehicle",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(1.dp, OutlineColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    )

                                    // Owner Photo Thumbnail
                                    if (violation.ownerPhotoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = violation.ownerPhotoUrl,
                                            contentDescription = "Owner Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, PrimaryNeon, CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SurfaceContainerHighest),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No\nPhoto",
                                                color = WarningAmber,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                lineHeight = 9.sp
                                            )
                                        }
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = violation.numberPlate,
                                                color = PrimaryText,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = "• ${violation.vehicleModel.ifEmpty { violation.vehicleType }}",
                                                color = OnSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Text(
                                            text = "Owner: ${violation.ownerName.ifEmpty { "Rahul Kumar" }}",
                                            color = PrimaryNeon,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Status Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (violation.isPaid) TertiaryEmerald.copy(alpha = 0.2f)
                                            else ErrorBright.copy(alpha = 0.25f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (violation.isPaid) "PAID" else "UNPAID",
                                        color = if (violation.isPaid) TertiaryEmerald else ErrorBright,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Row 2: Violation Type, Date/Time, Fine Amount, Unpaid Duration
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceContainerLowest.copy(alpha = 0.7f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = violation.violationType,
                                        color = if (violation.noiseDb >= 85) ErrorBright else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${violation.location} • ${violation.timeFormatted}",
                                        color = OnSurfaceVariant,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${violation.fineAmount}",
                                        color = Color(0xFFFFB800),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = if (violation.isPaid) "Settled" else "${violation.daysOverdue} Days Overdue",
                                        color = if (violation.isPaid) TertiaryEmerald else ErrorBright,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Row 3: Action Buttons (Relay to Smart Billboard / Settle Fine / Dossier)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (currentUser?.role == "ADMIN_POLICE") {
                                    Button(
                                        onClick = {
                                            repository.setBillboardViolation(violation)
                                            dashboardToast = "Broadcasting ${violation.numberPlate} to Smart Billboard!"
                                            onNavigateToScreen(AppScreen.VIOLATIONS)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon.copy(alpha = 0.2f)),
                                        border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                    ) {
                                        Icon(Icons.Filled.Tv, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Billboard", color = PrimaryNeon, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }

                                    if (!violation.isPaid) {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    repository.markAsPaid(violation.id, true)
                                                    dashboardToast = "Challan #${violation.challanNumber} fine marked as settled!"
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TertiaryEmerald),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(30.dp)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Settle Fine", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { selectedTableViolation = violation },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryBlue),
                                        border = BorderStroke(1.dp, SecondaryBlue.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .weight(0.8f)
                                            .height(30.dp)
                                    ) {
                                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Dossier", fontSize = 9.sp)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { selectedTableViolation = violation },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryBlue),
                                        border = BorderStroke(1.dp, SecondaryBlue.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp)
                                    ) {
                                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("View Violation Dossier & Details", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dossier Dialog for Selected Table Violation
    selectedTableViolation?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedTableViolation = null },
            title = {
                Text(
                    text = "Enforcement Dossier #${item.challanNumber}",
                    color = PrimaryNeon,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = item.evidenceImageUrl.ifEmpty {
                                "https://lh3.googleusercontent.com/aida-public/AB6AXuBK6Q96CW_G_sVE-zpZhtMxKSd-pQ70YA0KEzZOsWVq_FwfL3cpGPXbIdn8hfSGq0E0MiYcSKKQH4zEdkrwetBUAppkZi5N3GQh2yQ6b3sXYUNvuu5pLz3YMgcrQYFLtaIS9_xHFlOgG5v769bypf7PoBldrs3OQdIMY72c_JLc49hZ2f3O2DxqgVBrDkkyVcu5vwzMnnzp_YR8MN71ESeBco2X9p_EHQO96KCVKng7JxeBFJ2FeBQ"
                            },
                            contentDescription = "Vehicle",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        if (item.ownerPhotoUrl.isNotBlank()) {
                            AsyncImage(
                                model = item.ownerPhotoUrl,
                                contentDescription = "Owner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Owner Photo Not Available", color = WarningAmber, fontSize = 8.sp, fontFamily = FontFamily.Monospace, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }

                    Text("Plate: ${item.numberPlate} (${item.vehicleModel})", color = PrimaryText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("Owner: ${item.ownerName.ifEmpty { "Rahul Kumar" }}", color = PrimaryNeon, fontSize = 11.sp)
                    Text("Violation: ${item.violationType}", color = ErrorBright, fontSize = 11.sp)
                    Text("Time: ${item.timeFormatted} at ${item.location}", color = OnSurfaceVariant, fontSize = 10.sp)
                    Text("Fine: ₹${item.fineAmount} • Status: ${if (item.isPaid) "PAID" else "UNPAID (${item.daysOverdue} Days Overdue)"}", color = if (item.isPaid) TertiaryEmerald else ErrorBright, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.setBillboardViolation(item)
                        selectedTableViolation = null
                        onNavigateToScreen(AppScreen.VIOLATIONS)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
                ) {
                    Text("Broadcast on Billboard", color = OnPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTableViolation = null }) {
                    Text("Close", color = OnSurfaceVariant)
                }
            },
            containerColor = SurfaceContainerHigh,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Dashboard notification toast banner
    dashboardToast?.let { msg ->
        LaunchedEffect(msg) {
            delay(2500)
            dashboardToast = null
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = SurfaceContainerLowest,
                border = BorderStroke(1.dp, PrimaryNeon),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 8.dp
            ) {
                Text(
                    text = msg,
                    color = PrimaryNeon,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
