package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.TrafficRepository
import com.example.data.ViolationRecord
import com.example.ui.components.AppScreen
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ViolationsScreen(
    repository: TrafficRepository,
    onNavigateToScreen: (AppScreen) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val allViolations by repository.allViolations.collectAsState(initial = emptyList())
    val activeBillboardViolation by repository.activeBillboardViolation.collectAsState()
    val isAuthorizedAdminView by repository.isAuthorizedAdminView.collectAsState()
    val currentUser by repository.currentUser.collectAsState()

    val isAdmin = currentUser?.role == "ADMIN_POLICE"
    var isPublicPrivacyMode by remember(isAdmin, isAuthorizedAdminView) {
        mutableStateOf(if (isAdmin) !isAuthorizedAdminView else true)
    }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Honking/Noise", "Signal Jump", "Speeding", "Unpaid Only")

    // CCTV evidence modal preview
    var previewEvidenceRecord by remember { mutableStateOf<ViolationRecord?>(null) }

    // SMS Dispatch modal
    var smsRecord by remember { mutableStateOf<ViolationRecord?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Flashing amber beacon strobe animation for the VMS billboard
    val infiniteTransition = rememberInfiniteTransition(label = "strobe")
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

    // Filter logic: in citizen mode, show the citizen's vehicle violations
    val filteredList = remember(allViolations, selectedFilter, isAdmin, currentUser) {
        val baseList = if (isAdmin) {
            allViolations
        } else {
            val userPlate = currentUser?.badgeOrVehicleNo
            val ownerId = currentUser?.ownerProfileId
            val userSpecific = allViolations.filter { v ->
                (ownerId != null && v.ownerProfileId == ownerId) ||
                (!userPlate.isNullOrBlank() && v.numberPlate.equals(userPlate, ignoreCase = true))
            }
            if (userSpecific.isNotEmpty()) userSpecific else allViolations.take(3)
        }
        when (selectedFilter) {
            "Honking/Noise" -> baseList.filter { it.violationType.contains("Honk", ignoreCase = true) || it.noiseDb >= 85 }
            "Signal Jump" -> baseList.filter { it.violationType.contains("Red Light", ignoreCase = true) }
            "Speeding" -> baseList.filter { it.violationType.contains("Speed", ignoreCase = true) }
            "Unpaid Only" -> baseList.filter { !it.isPaid }
            else -> baseList
        }
    }

    val displayBillboard = activeBillboardViolation ?: allViolations.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // CITIZEN USER PANEL VIEW (Shown when user is NOT Admin Police)
        if (!isAdmin) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "My Vehicle E-Challans",
                                color = PrimaryText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "MoRTH Parivahan • Online Traffic Fine Settlement",
                                color = SecondaryBlue,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SecondaryBlue.copy(alpha = 0.15f))
                                .border(1.dp, SecondaryBlue.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentUser?.badgeOrVehicleNo ?: "OD 02 AB 1234",
                                color = SecondaryBlue,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Citizen Summary Card
                    val unpaidChallans = filteredList.filter { !it.isPaid }
                    val totalUnpaidAmount = unpaidChallans.sumOf { it.fineAmount }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "OUTSTANDING FINE BALANCE",
                                    color = OnSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "₹$totalUnpaidAmount",
                                    color = if (totalUnpaidAmount > 0) ErrorBright else TertiaryFixed,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (unpaidChallans.isEmpty()) "All challans cleared • Safe Driver" else "${unpaidChallans.size} pending challan(s) require payment",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }

                            if (unpaidChallans.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            unpaidChallans.forEach { ch ->
                                                repository.markAsPaid(ch.id, true)
                                            }
                                            toastMessage = "All pending fines (₹$totalUnpaidAmount) settled successfully!"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TertiaryEmerald),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Pay All", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            // TOP DIGITAL HIGHWAY VMS BILLBOARD (NODE #08)
            item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                text = "ROADSIDE VMS NODE #08",
                                color = PrimaryNeon,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.08.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFFFFB800).copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "BROADCAST ACTIVE",
                                    color = Color(0xFFFFB800),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Public Smart Billboard System",
                            color = PrimaryText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Cycle Billboard button
                    Button(
                        onClick = {
                            if (allViolations.isNotEmpty()) {
                                val currentIndex = allViolations.indexOfFirst { it.id == displayBillboard?.id }
                                val nextIndex = (currentIndex + 1) % allViolations.size
                                repository.setBillboardViolation(allViolations[nextIndex])
                                toastMessage = "Cycled Billboard to ${allViolations[nextIndex].numberPlate}"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("cycle_billboard_button")
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CYCLE", color = PrimaryNeon, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                // Privacy & Security Mode Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerHigh)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            if (isPublicPrivacyMode) Icons.Filled.Shield else Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = if (isPublicPrivacyMode) WarningAmber else PrimaryNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = if (isPublicPrivacyMode) "PUBLIC ROADSIDE MODE (MASKED)" else "POLICE AUTHORIZED MODE (CONFIDENTIAL)",
                                color = if (isPublicPrivacyMode) WarningAmber else PrimaryNeon,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPublicPrivacyMode) "Sensitive personal details masked for public safety" else "Full citizen registration dossier unmasked",
                                color = OnSurfaceVariant,
                                fontSize = 9.sp
                            )
                        }
                    }

                    if (isAdmin) {
                        Switch(
                            checked = !isPublicPrivacyMode,
                            onCheckedChange = { checked ->
                                isPublicPrivacyMode = !checked
                                repository.setAuthorizedAdminView(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryNeon,
                                checkedTrackColor = PrimaryNeon.copy(alpha = 0.3f),
                                uncheckedThumbColor = WarningAmber,
                                uncheckedTrackColor = SurfaceContainerLowest
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(WarningAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ENFORCED",
                                color = WarningAmber,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Digital Highway LED Sign Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF070B14))
                        .border(2.dp, Color(0xFFFFB800).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top warning strobes + Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Strobe
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFB800).copy(alpha = beaconAlpha))
                                    .border(1.dp, Color.White, CircleShape)
                            )

                            Text(
                                text = "⚠ PUBLIC TRAFFIC VIOLATION NOTICE ⚠",
                                color = Color(0xFFFFB800),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )

                            // Right Strobe
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFB800).copy(alpha = beaconAlpha))
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }

                        // DUAL VISUAL EVIDENCE: VEHICLE IMAGE + OWNER PHOTOGRAPH
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Left: Vehicle Snapshot
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F1422))
                                    .border(1.dp, Color(0xFFFFB800).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "VEHICLE EVIDENCE",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                AsyncImage(
                                    model = displayBillboard?.evidenceImageUrl?.ifEmpty {
                                        "https://lh3.googleusercontent.com/aida-public/AB6AXuBK6Q96CW_G_sVE-zpZhtMxKSd-pQ70YA0KEzZOsWVq_FwfL3cpGPXbIdn8hfSGq0E0MiYcSKKQH4zEdkrwetBUAppkZi5N3GQh2yQ6b3sXYUNvuu5pLz3YMgcrQYFLtaIS9_xHFlOgG5v769bypf7PoBldrs3OQdIMY72c_JLc49hZ2f3O2DxqgVBrDkkyVcu5vwzMnnzp_YR8MN71ESeBco2X9p_EHQO96KCVKng7JxeBFJ2FeBQ"
                                    },
                                    contentDescription = "Vehicle Evidence",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(86.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            }

                            // Right: Owner Photograph or "Owner Photo Not Available"
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F1422))
                                    .border(1.dp, Color(0xFFFFB800).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "REGISTERED OWNER",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                val ownerPhoto = displayBillboard?.ownerPhotoUrl ?: ""
                                if (ownerPhoto.isNotBlank()) {
                                    AsyncImage(
                                        model = ownerPhoto,
                                        contentDescription = "Owner Profile Picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(86.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                } else {
                                    // Requirement: Display "Owner Photo Not Available"
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(86.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1E2433)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.NoAccounts,
                                                contentDescription = "Owner Photo Not Available",
                                                tint = WarningAmber,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Owner Photo\nNot Available",
                                                color = WarningAmber,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                lineHeight = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Giant Monospace Plate & Owner Display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0F1422))
                                .border(1.dp, Color(0xFFFFB800).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = displayBillboard?.numberPlate ?: "OD 02 AB 1234",
                                    color = Color(0xFFFFD54F),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 3.sp
                                )

                                val displayedOwnerName = if (isPublicPrivacyMode) {
                                    val parts = (displayBillboard?.ownerName ?: "Rahul Kumar").split(" ")
                                    parts.joinToString(" ") { if (it.length > 2) it.take(2) + "***" else it }
                                } else {
                                    displayBillboard?.ownerName ?: "Rahul Kumar"
                                }

                                Text(
                                    text = "OWNER: $displayedOwnerName",
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "${displayBillboard?.vehicleModel ?: "Mahindra XUV700"} • ${displayBillboard?.vehicleColor ?: "Midnight Black"}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = displayBillboard?.violationType?.uppercase() ?: "EXCESSIVE HONKING (92 dB)",
                                    color = ErrorCrimson,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Subtitle information: Fine Amount & Payment Status & Unpaid Duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "FINE: ₹${displayBillboard?.fineAmount ?: 2000}",
                                    color = Color(0xFFFFB800),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${displayBillboard?.location ?: "MG Road"} • ${displayBillboard?.timeFormatted ?: "20:42"}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (displayBillboard?.isPaid == true) TertiaryEmerald.copy(alpha = 0.2f)
                                            else ErrorBright.copy(alpha = 0.25f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (displayBillboard?.isPaid == true) "STATUS: PAID"
                                        else "UNPAID • ${displayBillboard?.daysOverdue ?: 12} DAYS OVERDUE",
                                        color = if (displayBillboard?.isPaid == true) TertiaryEmerald else ErrorBright,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "PAY: echallan.parivahan.gov.in",
                                    color = SecondaryBlue,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Interactive Quick Actions below Billboard
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isAdmin) {
                                if (displayBillboard?.isPaid == false) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.markAsPaid(displayBillboard.id, true)
                                                toastMessage = "Fine of ₹${displayBillboard.fineAmount} marked as settled!"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TertiaryEmerald),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Settle Fine", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { smsRecord = displayBillboard },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryNeon),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Send Challan", fontSize = 10.sp)
                                }

                                OutlinedButton(
                                    onClick = { previewEvidenceRecord = displayBillboard },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB800)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                ) {
                                    Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Evidence", fontSize = 10.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { previewEvidenceRecord = displayBillboard },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB800)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                ) {
                                    Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View Billboard Evidence & Incident Report", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // Bottom LED Ticker Ribbon
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF161B2A))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = ">>> MG ROAD HOSPITAL SILENT ZONE • REPEAT OFFENDERS FACE LICENSE SUSPENSION >>>",
                                color = Color(0xFFFFB800),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // ANPR OCR ENGINE INSPECTOR
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
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(18.dp))
                        Text(
                            text = "ANPR Optical Inspection Engine",
                            color = PrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("VAHAN 4.0 VERIFIED", color = TertiaryFixed, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // License Plate High Contrast Visual
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF0038A8))
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text("IND", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = displayBillboard?.numberPlate ?: "OD 02 AB 1234",
                                    color = Color(0xFF161B2A),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text("HSRP VERIFIED", color = Color(0xFF0F766E), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Metadata details
                    Column(
                        modifier = Modifier.weight(1.3f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("Vehicle: ${displayBillboard?.vehicleType ?: "SUV (Diesel)"}", color = OnSurface, fontSize = 11.sp)
                        Text("Confidence: 99.4% OCR match", color = TertiaryFixed, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Tamper Check: Negative (Authentic)", color = SecondaryBlue, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Challan: #${displayBillboard?.challanNumber ?: "89410"}", color = PrimaryNeon, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        } // Close item
    } // Close if (isAdmin)

        // VIOLATIONS QUEUE HEADER & FILTER RIBBON
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAdmin) "Violations Queue (${filteredList.size})" else "My Vehicle Challans (${filteredList.size})",
                        color = PrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isAdmin) "REAL-TIME E-CHALLAN LEDGER" else "VAHAN & MORTH VERIFIED",
                        color = OnSurfaceVariant,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Filter Ribbon
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filters) { f ->
                        val isSelected = selectedFilter == f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) PrimaryNeon.copy(alpha = 0.2f) else SurfaceContainerLow)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) PrimaryNeon else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedFilter = f }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("filter_${f.lowercase().replace(" ", "_")}")
                        ) {
                            Text(
                                text = f,
                                color = if (isSelected) PrimaryNeon else OnSurfaceVariant,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // VIOLATION CARDS LIST
        items(filteredList, key = { it.id }) { violation ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerLow)
                    .border(
                        1.dp,
                        if (!violation.isPaid) ErrorBright.copy(alpha = 0.3f) else TertiaryEmerald.copy(alpha = 0.3f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Header row: Plate + Payment status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = violation.numberPlate,
                                color = PrimaryText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• ${violation.vehicleType}",
                                color = OnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (violation.isPaid) TertiaryEmerald.copy(alpha = 0.2f) else ErrorBright.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (violation.isPaid) "PAID" else if (violation.daysOverdue > 0) "${violation.daysOverdue}D OVERDUE" else "UNPAID",
                                color = if (violation.isPaid) TertiaryFixed else ErrorBright,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Middle row: Violation details + Thumbnail + Fine
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // CCTV Thumbnail preview
                        AsyncImage(
                            model = violation.evidenceImageUrl.ifEmpty {
                                "https://lh3.googleusercontent.com/aida-public/AB6AXuBK6Q96CW_G_sVE-zpZhtMxKSd-pQ70YA0KEzZOsWVq_FwfL3cpGPXbIdn8hfSGq0E0MiYcSKKQH4zEdkrwetBUAppkZi5N3GQh2yQ6b3sXYUNvuu5pLz3YMgcrQYFLtaIS9_xHFlOgG5v769bypf7PoBldrs3OQdIMY72c_JLc49hZ2f3O2DxqgVBrDkkyVcu5vwzMnnzp_YR8MN71ESeBco2X9p_EHQO96KCVKng7JxeBFJ2FeBQ"
                            },
                            contentDescription = "CCTV Evidence",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { previewEvidenceRecord = violation }
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = violation.violationType,
                                color = ErrorBright,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = violation.location,
                                color = OnSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${violation.dateFormatted} • ${violation.timeFormatted}",
                                color = SecondaryBlue,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹${violation.fineAmount}",
                                color = PrimaryText,
                                fontSize = 17.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "CHALLAN #${violation.challanNumber}",
                                color = OnSurfaceVariant,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Action buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isAdmin) {
                            OutlinedButton(
                                onClick = { previewEvidenceRecord = violation },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("EVIDENCE", color = PrimaryNeon, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }

                            OutlinedButton(
                                onClick = { smsRecord = violation },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            ) {
                                Icon(Icons.Filled.Sms, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("SEND SMS", color = SecondaryBlue, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.markAsPaid(violation.id, !violation.isPaid)
                                        toastMessage = if (!violation.isPaid) "Marked #${violation.challanNumber} as PAID" else "Marked as UNPAID"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (violation.isPaid) SurfaceContainerHighest else TertiaryEmerald
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(32.dp)
                            ) {
                                Text(
                                    text = if (violation.isPaid) "REOPEN" else "MARK PAID",
                                    color = if (violation.isPaid) OnSurfaceVariant else OnTertiary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { previewEvidenceRecord = violation },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = PrimaryNeon, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("EVIDENCE", color = PrimaryNeon, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }

                            if (!violation.isPaid) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.markAsPaid(violation.id, true)
                                            toastMessage = "Fine of ₹${violation.fineAmount} paid for Challan #${violation.challanNumber}!"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TertiaryEmerald),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(32.dp)
                                ) {
                                    Icon(Icons.Filled.Payment, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PAY ₹${violation.fineAmount}", color = Color.Black, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TertiaryEmerald.copy(alpha = 0.2f))
                                        .border(1.dp, TertiaryEmerald.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓ SETTLED", color = TertiaryEmerald, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== DIALOGS ====================

    // 1. CCTV Full Evidence Dialog
    previewEvidenceRecord?.let { record ->
        Dialog(onDismissRequest = { previewEvidenceRecord = null }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerLowest,
                border = BorderStroke(1.dp, PrimaryNeon),
                modifier = Modifier.padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HIGH-RES EVIDENCE ARCHIVE",
                            color = PrimaryNeon,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { previewEvidenceRecord = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = OnSurfaceVariant)
                        }
                    }

                    AsyncImage(
                        model = record.evidenceImageUrl.ifEmpty {
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuBK6Q96CW_G_sVE-zpZhtMxKSd-pQ70YA0KEzZOsWVq_FwfL3cpGPXbIdn8hfSGq0E0MiYcSKKQH4zEdkrwetBUAppkZi5N3GQh2yQ6b3sXYUNvuu5pLz3YMgcrQYFLtaIS9_xHFlOgG5v769bypf7PoBldrs3OQdIMY72c_JLc49hZ2f3O2DxqgVBrDkkyVcu5vwzMnnzp_YR8MN71ESeBco2X9p_EHQO96KCVKng7JxeBFJ2FeBQ"
                        },
                        contentDescription = "Full CCTV evidence preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Plate: ${record.numberPlate}", color = PrimaryText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("Violation: ${record.violationType}", color = ErrorBright, fontSize = 11.sp)
                        Text("Timestamp: ${record.timeFormatted} (${record.dateFormatted})", color = SecondaryBlue, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Location: ${record.location}", color = OnSurfaceVariant, fontSize = 10.sp)
                        Text("Penalty: ₹${record.fineAmount} • E-Challan #${record.challanNumber}", color = TertiaryFixed, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            repository.setBillboardViolation(record)
                            previewEvidenceRecord = null
                            toastMessage = "Broadcasting ${record.numberPlate} on VMS Billboard!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("PUSH TO VMS BILLBOARD", color = OnPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 2. Send SMS Notice Dialog
    smsRecord?.let { record ->
        Dialog(onDismissRequest = { smsRecord = null }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerLowest,
                border = BorderStroke(1.dp, SecondaryBlue),
                modifier = Modifier.padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "DISPATCH E-CHALLAN SMS",
                        color = SecondaryBlue,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceContainerLow)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Dear Citizen, Vehicle ${record.numberPlate} was recorded committing ${record.violationType} at ${record.location} on ${record.timeFormatted}. Fine amount ₹${record.fineAmount}. Challan #${record.challanNumber}. Pay at echallan.parivahan.gov.in.",
                            color = PrimaryText,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { smsRecord = null }) {
                            Text("CANCEL", color = OutlineColor, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    repository.recordAudit(
                                        "SMS_NOTICE_DISPATCHED",
                                        "SMS notice dispatched to registered owner of ${record.numberPlate} for Challan #${record.challanNumber}."
                                    )
                                    smsRecord = null
                                    toastMessage = "SMS sent to registered mobile of ${record.numberPlate}"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
                        ) {
                            Text("DISPATCH NOW", color = OnSecondary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
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
