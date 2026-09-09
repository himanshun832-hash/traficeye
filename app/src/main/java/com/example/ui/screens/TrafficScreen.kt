package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
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

@Composable
fun TrafficScreen(
    repository: TrafficRepository,
    onNavigateToScreen: (AppScreen) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()

    var baseCycleSec by remember { mutableStateOf(120f) }
    var isCorridorPriorityForced by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Corridor Signal state
    var northSeconds by remember { mutableStateOf(34) }
    var southSeconds by remember { mutableStateOf(10) }
    var eastSeconds by remember { mutableStateOf(28) }
    var westSeconds by remember { mutableStateOf(3) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (northSeconds > 0) northSeconds-- else northSeconds = 45
            if (southSeconds > 0) southSeconds-- else southSeconds = 60
            if (eastSeconds > 0) eastSeconds-- else eastSeconds = 50
            if (westSeconds > 0) westSeconds-- else westSeconds = 15
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
        // TOP CORRIDOR SIGNAL MATRIX HEADER
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                text = "CORRIDOR SIGNAL MATRIX",
                                color = PrimaryNeon,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.08.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(TertiaryEmerald.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "PHASE 4 ACTIVE",
                                    color = TertiaryFixed,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Central Arterial Intelligent Phasing",
                            color = PrimaryText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceContainerHigh)
                            .border(1.dp, PrimaryNeon.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SYNC: 100%",
                            color = PrimaryNeon,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4-WAY CORRIDOR 2x2 SIGNAL GRID
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: North & South
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // NORTHBOUND (Green, Active Surge)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerLow)
                            .border(1.5.dp, TertiaryEmerald, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("NORTHBOUND", color = OnSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(TertiaryEmerald.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("+15s AUTO SURGE", color = TertiaryFixed, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(TertiaryEmerald)
                                )
                                Text("${northSeconds}s", color = TertiaryFixed, fontSize = 24.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text("GREEN", color = TertiaryEmerald, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Text("Queue: 14m • Fluid Discharge", color = OnSurfaceVariant, fontSize = 9.sp)
                        }
                    }

                    // SOUTHBOUND (Red)
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SOUTHBOUND", color = OnSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text("HOLD", color = ErrorCrimson, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(ErrorBright)
                                )
                                Text("${southSeconds}s", color = ErrorBright, fontSize = 24.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text("RED", color = ErrorBright, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Text("Queue: 88m • Standby", color = OnSurfaceVariant, fontSize = 9.sp)
                        }
                    }
                }

                // Row 2: East & West
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // EASTBOUND (Red)
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("EASTBOUND", color = OnSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text("HOLD", color = ErrorCrimson, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(ErrorBright)
                                )
                                Text("${eastSeconds}s", color = ErrorBright, fontSize = 24.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text("RED", color = ErrorBright, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Text("Queue: 112m • Heavy", color = OnSurfaceVariant, fontSize = 9.sp)
                        }
                    }

                    // WESTBOUND (Yellow)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, SecondaryBlue.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("WESTBOUND", color = OnSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text("TRANSITION", color = SecondaryBlue, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(SecondaryBlue)
                                )
                                Text("${westSeconds}s", color = SecondaryBlue, fontSize = 24.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Text("YELLOW", color = SecondaryBlue, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Text("Clear Intersection", color = OnSurfaceVariant, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        // NEURAL DURATION EXTENSION PIPELINE BAR
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerLow)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "NEURAL DURATION EXTENSION PIPELINE",
                        color = PrimaryNeon,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "60s TOTAL PHASING",
                        color = TertiaryFixed,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 3 Segment Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceContainerHighest)
                ) {
                    // Base
                    Box(
                        modifier = Modifier
                            .weight(30f)
                            .fillMaxHeight()
                            .background(SecondaryBlue.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("BASE 30s", color = Color.Black, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    // AI Boost
                    Box(
                        modifier = Modifier
                            .weight(15f)
                            .fillMaxHeight()
                            .background(TertiaryEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+15s BOOST", color = Color.Black, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    // Buffer
                    Box(
                        modifier = Modifier
                            .weight(15f)
                            .fillMaxHeight()
                            .background(PrimaryNeon.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("BUFFER 15s", color = Color.Black, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "AI dynamic allocation based on upstream optical density counters and emergency dispatch alerts.",
                    color = OnSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }

        // BASE CYCLE MODULATION SLIDER & OVERRIDES (Admin Police Only)
        if (currentUser?.role == "ADMIN_POLICE") {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainerLow)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CYCLE QUOTA MODULATION",
                            color = PrimaryText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${baseCycleSec.toInt()}s BASE CYCLE",
                            color = PrimaryNeon,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = baseCycleSec,
                        onValueChange = { baseCycleSec = it },
                        valueRange = 60f..180f,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryNeon,
                            activeTrackColor = PrimaryNeon,
                            inactiveTrackColor = SurfaceContainerHighest
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("60s (RAPID)", color = OutlineColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("120s (NOMINAL)", color = SecondaryBlue, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("180s (HEAVY)", color = OutlineColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            isCorridorPriorityForced = !isCorridorPriorityForced
                            coroutineScope.launch {
                                repository.recordAudit(
                                    if (isCorridorPriorityForced) "GREEN_CORRIDOR_SEIZED" else "CORRIDOR_RESTORED",
                                    "All signals locked to Green for Ambulance / VIP Route clearance."
                                )
                                toastMessage = if (isCorridorPriorityForced) {
                                    "🚑 GREEN CORRIDOR ENGAGED: Signals Force-Cleared"
                                } else {
                                    "Normal intelligent cyclic phasing restored."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCorridorPriorityForced) TertiaryEmerald else SurfaceContainerHighest
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("force_green_corridor_button")
                    ) {
                        Icon(
                            Icons.Filled.LocalHospital,
                            contentDescription = null,
                            tint = if (isCorridorPriorityForced) OnTertiary else TertiaryEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCorridorPriorityForced) "FORCE GREEN CORRIDOR: ACTIVE (TAP TO RELEASE)" else "FORCE GREEN CORRIDOR (AMBULANCE / VIP)",
                            color = if (isCorridorPriorityForced) OnTertiary else TertiaryFixed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Traffic, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                text = "Intelligent Traffic Phasing Active",
                                color = PrimaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Corridor cycle dynamically coordinated with active junction IoT density sensors.",
                                color = OnSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // CORRIDOR VELOCITY & LANE-BY-LANE TELEMETRY
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
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
                        text = "Lane-by-Lane Telemetry Feed",
                        color = PrimaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("3 Lanes Monitored", color = TertiaryFixed, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }

                // Lane 1
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("L1 (FAST TRACK):", color = PrimaryNeon, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("52 km/h avg", color = PrimaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Text("DENSITY: 24% (FLUID)", color = TertiaryFixed, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                // Lane 2
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("L2 (CRUISING):", color = SecondaryBlue, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("38 km/h avg", color = PrimaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Text("DENSITY: 58% (OPTIMAL)", color = SecondaryBlue, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                // Lane 3
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("L3 (BUS/TRANSIT):", color = ErrorBright, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("21 km/h avg", color = PrimaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Text("DENSITY: 82% (HEAVY)", color = ErrorBright, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Toast message
    toastMessage?.let { msg ->
        LaunchedEffect(msg) {
            delay(2500)
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
                border = BorderStroke(1.dp, TertiaryEmerald),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = msg,
                    color = TertiaryFixed,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
