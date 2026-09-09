package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
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
import com.example.data.AuditLog
import com.example.data.ScanResultData
import com.example.data.TrafficRepository
import com.example.data.VideoTimestampMarker
import com.example.ui.components.AppScreen
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ScannerInputMode {
    LIVE_CAMERA,
    FILE_UPLOAD
}

@Composable
fun ScannerScreen(
    repository: TrafficRepository,
    onNavigateToScreen: (AppScreen) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var inputMode by remember { mutableStateOf(ScannerInputMode.LIVE_CAMERA) }
    var isLiveScanning by remember { mutableStateOf(true) }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedSampleIndex by remember { mutableStateOf(0) }
    var isAnalyzingFile by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableStateOf(0f) }
    var analysisStatusText by remember { mutableStateOf("Ready to scan") }
    var scanCompleted by remember { mutableStateOf(true) }

    // Video scrubbing state
    var isVideoMedia by remember { mutableStateOf(false) }
    var currentVideoTimeSec by remember { mutableStateOf(24) }
    var isVideoPlaying by remember { mutableStateOf(false) }

    // Plate verification state
    var currentPlateNumber by remember { mutableStateOf("OD 02 AB 1234") }
    var showEditPlateDialog by remember { mutableStateOf(false) }
    var plateConfirmed by remember { mutableStateOf(true) }

    // Privacy & Security state
    var showAuditDialog by remember { mutableStateOf(false) }
    var isPrivacyMasked by remember { mutableStateOf(false) }
    var adminClearanceLevel by remember { mutableStateOf("SUPERVISOR (LEVEL 4)") }

    // Action Feedback Snackbars / Dialogs
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessPipelineModal by remember { mutableStateOf(false) }
    var latestChallanNumber by remember { mutableStateOf("89410") }

    // Scan Result Data
    var currentScanResult by remember {
        mutableStateOf(
            ScanResultData(
                mediaType = "IMAGE",
                mediaUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuBK6Q96CW_G_sVE-zpZhtMxKSd-pQ70YA0KEzZOsWVq_FwfL3cpGPXbIdn8hfSGq0E0MiYcSKKQH4zEdkrwetBUAppkZi5N3GQh2yQ6b3sXYUNvuu5pLz3YMgcrQYFLtaIS9_xHFlOgG5v769bypf7PoBldrs3OQdIMY72c_JLc49hZ2f3O2DxqgVBrDkkyVcu5vwzMnnzp_YR8MN71ESeBco2X9p_EHQO96KCVKng7JxeBFJ2FeBQ",
                vehiclesDetected = 7,
                platesDetected = 5,
                violationsDetected = 2,
                noiseLevelDb = 91,
                confidencePercent = 97,
                vehicleType = "Car (Black SUV)",
                plateNumber = "OD 02 AB 1234",
                violationDescription = "Excessive Honking",
                location = "Intersection 04 • Hospital Silent Zone",
                timeString = "08:42 PM",
                speedKmh = 48
            )
        )
    }

    // Photo & Video Picker
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            isVideoMedia = false // Treat as custom uploaded media
            scanCompleted = false
            analysisProgress = 0f
            analysisStatusText = "File loaded. Tap 'ANALYZE FILE' to execute AI scan."
            snackbarMessage = "Media successfully staged from device"
        }
    }

    // Preset test sample data
    val sampleFiles = listOf(
        Triple(
            "Hospital Zone (Honking)",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuBK6Q96CW_G_sVE-zpZhtMxKSd-pQ70YA0KEzZOsWVq_FwfL3cpGPXbIdn8hfSGq0E0MiYcSKKQH4zEdkrwetBUAppkZi5N3GQh2yQ6b3sXYUNvuu5pLz3YMgcrQYFLtaIS9_xHFlOgG5v769bypf7PoBldrs3OQdIMY72c_JLc49hZ2f3O2DxqgVBrDkkyVcu5vwzMnnzp_YR8MN71ESeBco2X9p_EHQO96KCVKng7JxeBFJ2FeBQ",
            false
        ),
        Triple(
            "Live Traffic Feed (Video 30s)",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuDW5dwPtOW0lcJP1SbKMJnTe7cqQ8m97x38ygOo0Gs26b3fFjj3885lEh2HB1hZSJkTl9ZzwlIHD2I9TToPfPavmDI5tnN0WLUBoZGfQ0ynimNrKbsncvlMrSBmACtwpkbN32rPO4c5u4EnCKdBI4KvBJg-pdnFusrwwtyCN5dmthjaO0TMS6u0jMP8TL_z0O-JfdDFrS3XrzbrH0RHXBwnySlawSVJ1VLIVumO8LIKm_AM6acdvjo",
            true
        ),
        Triple(
            "Red Light Intersection",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuBCJ0oiIIEPmOXRk5csgkTJKKhsP7fjLC3m_RJC34vmxHxmv2T2EcLTHmmvN8OI-rfy2agJRT5Qhl7Dici8Oq94lmQ0QpWMyb6PaWqGzCz-v9tNujG7Cwwf_v726ZOZ_qDX9eIN0_uf_XHkPIrinM_vOLWI_gA9c4KIYoiUrIXlVy-K4gnmogZoPc4rrSx_m4OgLfNIawikCrC8AwI4PD1TuZEatGxgDCay0BduoBB4Z4o6EysbNXM",
            false
        ),
        Triple(
            "Radar Highway Gun (Speeding)",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuBqp5qmM8ifvVtWIJX4eE2XuTXNoIBy88lSpgJGEWU-lNHnZrHt6btdi-heKohhVds4p98hijznEWDpUS7u2JS_A0y3xJNUc_l89bQnTAVwLq2UBJ6eWnd4w2e4j2ixs8YsIEFM2qvG189C3ybYKUGP8r2m1IDBzH1yBLSC_C9teGo1L0TKqqsS9C-h9Iu_yFMl53FlTYzq3Aq8vm01ZXa5lvxoLneDIGYQejtupzPTyE-0iGYGIN0",
            false
        )
    )

    // Scanning laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "scannerLaser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laserPos"
    )

    // Video auto playback simulation
    LaunchedEffect(isVideoPlaying) {
        if (isVideoPlaying) {
            while (isVideoPlaying) {
                delay(1000)
                currentVideoTimeSec = if (currentVideoTimeSec >= 30) 0 else currentVideoTimeSec + 1
            }
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
        // TOP BANNER: Smart Scanner Mode Selector & Privacy Status
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
                                text = "SMART SCANNER & EVIDENCE TOOL",
                                color = PrimaryNeon,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.08.sp
                            )
                        }
                        Text(
                            text = "Dual-Engine AI Traffic Enforcement",
                            color = PrimaryText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Security / Audit Trail Badge Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceContainerHigh)
                            .border(1.dp, TertiaryEmerald.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .clickable { showAuditDialog = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("security_audit_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Security,
                                contentDescription = "Security Audit",
                                tint = TertiaryEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "AUDIT LOG",
                                color = TertiaryFixed,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Dual Mode Switcher Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainer)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 1: Live Scanner
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (inputMode == ScannerInputMode.LIVE_CAMERA) SurfaceContainerHighest
                                else Color.Transparent
                            )
                            .border(
                                width = if (inputMode == ScannerInputMode.LIVE_CAMERA) 1.dp else 0.dp,
                                color = if (inputMode == ScannerInputMode.LIVE_CAMERA) PrimaryNeon.copy(alpha = 0.5f)
                                else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                inputMode = ScannerInputMode.LIVE_CAMERA
                                isLiveScanning = true
                            }
                            .padding(vertical = 8.dp)
                            .testTag("tab_live_scanner"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isLiveScanning) ErrorBright else OutlineColor)
                            )
                            Text(
                                text = "🔴 LIVE SCANNER",
                                color = if (inputMode == ScannerInputMode.LIVE_CAMERA) PrimaryNeon else OnSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Tab 2: File Scanner
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (inputMode == ScannerInputMode.FILE_UPLOAD) SurfaceContainerHighest
                                else Color.Transparent
                            )
                            .border(
                                width = if (inputMode == ScannerInputMode.FILE_UPLOAD) 1.dp else 0.dp,
                                color = if (inputMode == ScannerInputMode.FILE_UPLOAD) PrimaryNeon.copy(alpha = 0.5f)
                                else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                inputMode = ScannerInputMode.FILE_UPLOAD
                            }
                            .padding(vertical = 8.dp)
                            .testTag("tab_file_scanner"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.UploadFile,
                                contentDescription = null,
                                tint = if (inputMode == ScannerInputMode.FILE_UPLOAD) PrimaryNeon else OnSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "📁 FILE SCANNER",
                                color = if (inputMode == ScannerInputMode.FILE_UPLOAD) PrimaryNeon else OnSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // SECTION 1: LIVE SCANNER OR FILE SCANNER VIEWPORT
        item {
            if (inputMode == ScannerInputMode.LIVE_CAMERA) {
                // ==================== 1. LIVE CAMERA SCANNER ====================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLowest)
                        .border(1.dp, PrimaryNeon.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Viewport Container with Live Camera simulation & Overlays
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerHighest)
                    ) {
                        // Base Camera Frame
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDW5dwPtOW0lcJP1SbKMJnTe7cqQ8m97x38ygOo0Gs26b3fFjj3885lEh2HB1hZSJkTl9ZzwlIHD2I9TToPfPavmDI5tnN0WLUBoZGfQ0ynimNrKbsncvlMrSBmACtwpkbN32rPO4c5u4EnCKdBI4KvBJg-pdnFusrwwtyCN5dmthjaO0TMS6u0jMP8TL_z0O-JfdDFrS3XrzbrH0RHXBwnySlawSVJ1VLIVumO8LIKm_AM6acdvjo",
                            contentDescription = "Live surveillance camera stream",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Vignette & HUD grid
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            SurfaceContainerLowest.copy(alpha = 0.6f),
                                            Color.Transparent,
                                            SurfaceContainerLowest.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )

                        // Animated Scanning Laser line
                        if (isLiveScanning) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val y = size.height * laserPosition
                                drawLine(
                                    color = PrimaryNeon.copy(alpha = 0.85f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 2.5f
                                )
                                drawLine(
                                    color = PrimaryNeon.copy(alpha = 0.25f),
                                    start = Offset(0f, y - 6f),
                                    end = Offset(size.width, y - 6f),
                                    strokeWidth = 5f
                                )
                            }
                        }

                        // Top camera telemetry header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceContainerLowest.copy(alpha = 0.85f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isLiveScanning) ErrorBright else OutlineColor)
                                    )
                                    Text(
                                        text = if (isLiveScanning) "REC [LIVE 60 FPS]" else "PAUSED",
                                        color = if (isLiveScanning) ErrorBright else OutlineColor,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceContainerLowest.copy(alpha = 0.85f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.LensBlur,
                                        contentDescription = null,
                                        tint = PrimaryNeon,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "AI-YOLOv9 + ANPR",
                                        color = PrimaryNeon,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Vehicle Detection Overlay Box 1: CAR
                        Box(
                            modifier = Modifier
                                .offset(x = 16.dp, y = 50.dp)
                                .width(160.dp)
                                .border(1.5.dp, TertiaryEmerald, RoundedCornerShape(4.dp))
                                .background(TertiaryEmerald.copy(alpha = 0.12f))
                                .padding(5.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "CAR 96%",
                                        color = OnTertiary,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(TertiaryEmerald, RoundedCornerShape(2.dp))
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                    Text(
                                        text = "48 km/h",
                                        color = TertiaryFixed,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Highlighted Bounding Box around Plate
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(SurfaceContainerLowest.copy(alpha = 0.9f))
                                        .border(1.dp, PrimaryNeon, RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isPrivacyMasked) "OD 02 ** ****" else currentPlateNumber,
                                            color = PrimaryNeon,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "OCR 97%",
                                            color = TertiaryFixed,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Vehicle Detection Overlay Box 2: BIKE
                        Box(
                            modifier = Modifier
                                .offset(x = 190.dp, y = 110.dp)
                                .width(130.dp)
                                .border(1.dp, PrimaryNeon, RoundedCornerShape(4.dp))
                                .background(PrimaryNeon.copy(alpha = 0.12f))
                                .padding(4.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "BIKE 99%",
                                        color = OnPrimary,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(PrimaryNeon, RoundedCornerShape(2.dp))
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                    Text(
                                        text = "38 km/h",
                                        color = PrimaryFixedDim,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SurfaceContainerLowest.copy(alpha = 0.9f))
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (isPrivacyMasked) "MH 12 ** ****" else "MH 12 PQ 9912",
                                        color = PrimaryText,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Bottom Optical & Azimuth Strip
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(SurfaceContainerLowest.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CAM-04 NORTH JUNCTION • 3840x2160",
                                color = OnSurfaceVariant,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "FOV: 112° AZ: 014°N",
                                color = PrimaryNeon,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Live Acoustic Noise Breach Telemetry Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ErrorContainer.copy(alpha = 0.25f))
                            .border(1.dp, ErrorBright.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Campaign,
                                contentDescription = "Excessive Noise",
                                tint = ErrorBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "ACOUSTIC SENSOR ARRAY #08: 91 dB",
                                    color = ErrorBright,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Excessive honking violation detected in Silent Zone (+11 dB breach)",
                                    color = OnSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ErrorBright)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "BREACH",
                                color = OnError,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Live Scan Controls: START/STOP LIVE SCAN + AUTO EVIDENCE CAPTURE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isLiveScanning = !isLiveScanning
                                if (isLiveScanning) {
                                    snackbarMessage = "Live Camera Scan Active (AI Tracking On)"
                                } else {
                                    snackbarMessage = "Live Stream Paused"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLiveScanning) SurfaceContainerHighest else PrimaryNeon
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("start_live_scan_button")
                        ) {
                            Icon(
                                if (isLiveScanning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = if (isLiveScanning) PrimaryNeon else OnPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLiveScanning) "PAUSE SCAN" else "START LIVE SCAN",
                                color = if (isLiveScanning) PrimaryNeon else OnPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val id = repository.createViolationFromScan(currentScanResult)
                                    latestChallanNumber = ((10000..99999).random()).toString()
                                    showSuccessPipelineModal = true
                                    repository.recordAudit(
                                        "LIVE_EVIDENCE_AUTO_CAPTURED",
                                        "Live evidence auto-captured for plate $currentPlateNumber at ${currentScanResult.location}"
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorBright),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("capture_evidence_button")
                        ) {
                            Icon(
                                Icons.Filled.Camera,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CAPTURE EVIDENCE",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // ==================== 2. FILE SCANNER ====================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLowest)
                        .border(1.dp, SecondaryBlue.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Drag & Drop / Upload Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .border(
                                width = 1.5.dp,
                                color = if (selectedMediaUri != null) PrimaryNeon else OutlineColor.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            }
                            .padding(12.dp)
                            .testTag("file_drop_area"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedMediaUri != null) {
                            AsyncImage(
                                model = selectedMediaUri,
                                contentDescription = "Uploaded Media Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceContainerLowest.copy(alpha = 0.85f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "CHANGE FILE",
                                    color = PrimaryNeon,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.CloudUpload,
                                    contentDescription = "Upload",
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "📁 DROP IMAGE OR VIDEO HERE",
                                    color = PrimaryText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "or UPLOAD FROM DEVICE",
                                    color = SecondaryBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Supports: JPG, JPEG, PNG, MP4, MOV, WebM",
                                    color = OnSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Preset Evidence Samples for instant testing
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PRELOADED EVIDENCE SAMPLES (1-TAP TEST):",
                            color = OnSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(sampleFiles.indices.toList()) { index ->
                                val sample = sampleFiles[index]
                                val isSelected = selectedSampleIndex == index && selectedMediaUri == null
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) SurfaceContainerHighest else SurfaceContainerLow)
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) PrimaryNeon else Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            selectedSampleIndex = index
                                            selectedMediaUri = null
                                            isVideoMedia = sample.third
                                            scanCompleted = true
                                            currentScanResult = currentScanResult.copy(
                                                mediaType = if (sample.third) "VIDEO" else "IMAGE",
                                                mediaUri = sample.second,
                                                violationDescription = if (index == 0) "Excessive Honking"
                                                else if (index == 1) "Multi-vehicle Lane Breach"
                                                else if (index == 2) "Red Light Jump"
                                                else "Speeding 78 km/h"
                                            )
                                            snackbarMessage = "Loaded sample: ${sample.first}"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .testTag("sample_chip_$index")
                                ) {
                                    Text(
                                        text = sample.first,
                                        color = if (isSelected) PrimaryNeon else OnSurface,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // "ANALYZE FILE" Button & Progress
                    if (isAnalyzingFile) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerLow)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "AI NEURAL CLASSIFIER",
                                    color = PrimaryNeon,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${(analysisProgress * 100).toInt()}%",
                                    color = TertiaryFixed,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { analysisProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = PrimaryNeon,
                                trackColor = SurfaceContainerHighest
                            )
                            Text(
                                text = analysisStatusText,
                                color = OnSurfaceVariant,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isAnalyzingFile = true
                                    analysisProgress = 0.1f
                                    analysisStatusText = "Extracting frames & detecting vehicle silhouettes..."
                                    delay(600)
                                    analysisProgress = 0.35f
                                    analysisStatusText = "Running ANPR OCR on number plates (OD 02 AB 1234)..."
                                    delay(700)
                                    analysisProgress = 0.68f
                                    analysisStatusText = "Acoustic envelope triangulation: 91 dB horn spike detected..."
                                    delay(600)
                                    analysisProgress = 1.0f
                                    analysisStatusText = "Analysis complete. Violations verified."
                                    delay(300)
                                    isAnalyzingFile = false
                                    scanCompleted = true
                                    snackbarMessage = "AI Scan Completed Successfully ✓"
                                    repository.recordAudit(
                                        "FILE_EVIDENCE_ANALYSIS_COMPLETED",
                                        "Analyzed evidence media: Detected 7 vehicles, 5 plates, 2 violations (Plate: $currentPlateNumber)"
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("analyze_file_button")
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = OnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🔍 ANALYZE FILE",
                                color = OnPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.05.sp
                            )
                        }
                    }
                }
            }
        }

        // ==================== 3. VIDEO SCANNING TIMELINE (If Video Active) ====================
        if (isVideoMedia) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainer)
                        .border(1.dp, PrimaryNeon.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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
                            Icon(
                                Icons.Filled.Videocam,
                                contentDescription = null,
                                tint = PrimaryNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "VIDEO ANALYSIS: 100%",
                                color = PrimaryNeon,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Play/Pause toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceContainerHigh)
                                .clickable { isVideoPlaying = !isVideoPlaying }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (isVideoPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isVideoPlaying) "PLAYING" else "PAUSED",
                                    color = PrimaryText,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Video Scrubber Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "00:${if (currentVideoTimeSec < 10) "0$currentVideoTimeSec" else currentVideoTimeSec}",
                                color = PrimaryNeon,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "00:30 (SURVEILLANCE CLIP)",
                                color = OnSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }

                        Slider(
                            value = currentVideoTimeSec.toFloat(),
                            onValueChange = { currentVideoTimeSec = it.toInt() },
                            valueRange = 0f..30f,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryNeon,
                                activeTrackColor = PrimaryNeon,
                                inactiveTrackColor = SurfaceContainerHighest
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    // Clickable Timeline Violation Markers
                    Text(
                        text = "VIOLATION TIMESTAMPS (TAP TO JUMP):",
                        color = OutlineColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        currentScanResult.videoMarkers.forEach { marker ->
                            val isAtMarker = currentVideoTimeSec in (marker.timeSeconds - 1)..(marker.timeSeconds + 1)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isAtMarker) PrimaryNeon.copy(alpha = 0.2f) else SurfaceContainerLow)
                                    .border(
                                        width = if (isAtMarker) 1.dp else 0.dp,
                                        color = if (isAtMarker) PrimaryNeon else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        currentVideoTimeSec = marker.timeSeconds
                                        snackbarMessage = "Jumped to ${marker.timeLabel}: ${marker.eventDescription}"
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isAtMarker) PrimaryNeon else SurfaceContainerHigh)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = marker.timeLabel,
                                            color = if (isAtMarker) OnPrimary else PrimaryText,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = marker.eventDescription,
                                        color = if (isAtMarker) PrimaryNeon else OnSurface,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    Icons.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = if (isAtMarker) PrimaryNeon else OutlineColor,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==================== 4. 🧠 AI SCAN RESULTS SCREEN ====================
        if (scanCompleted) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainer)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = TertiaryEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "SCAN COMPLETED ✓",
                                color = TertiaryFixed,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TertiaryEmerald.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CONFIDENCE: 94%",
                                color = TertiaryFixed,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Key Summary Metrics 4-Col Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Vehicles
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerLow)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "VEHICLES",
                                    color = OnSurfaceVariant,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${currentScanResult.vehiclesDetected}",
                                    color = PrimaryText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Plates
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerLow)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "PLATES",
                                    color = OnSurfaceVariant,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${currentScanResult.platesDetected}",
                                    color = TertiaryFixed,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Violations
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerLow)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "VIOLATIONS",
                                    color = OnSurfaceVariant,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${currentScanResult.violationsDetected}",
                                    color = ErrorBright,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Noise
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerLow)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "NOISE SPL",
                                    color = OnSurfaceVariant,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${currentScanResult.noiseLevelDb} dB",
                                    color = ErrorBright,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // DETECTED VEHICLE BREAKDOWN
                    Text(
                        text = "DETECTED VEHICLE EVIDENCE:",
                        color = OnSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Vehicle Image Thumbnail
                        AsyncImage(
                            model = currentScanResult.mediaUri.ifEmpty {
                                "https://lh3.googleusercontent.com/aida-public/AB6AXuBK6Q96CW_G_sVE-zpZhtMxKSd-pQ70YA0KEzZOsWVq_FwfL3cpGPXbIdn8hfSGq0E0MiYcSKKQH4zEdkrwetBUAppkZi5N3GQh2yQ6b3sXYUNvuu5pLz3YMgcrQYFLtaIS9_xHFlOgG5v769bypf7PoBldrs3OQdIMY72c_JLc49hZ2f3O2DxqgVBrDkkyVcu5vwzMnnzp_YR8MN71ESeBco2X9p_EHQO96KCVKng7JxeBFJ2FeBQ"
                            },
                            contentDescription = "Vehicle Crop",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, PrimaryNeon.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        )

                        // Meta details
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Vehicle Type: ${currentScanResult.vehicleType}",
                                color = OnSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Number Plate: ${if (isPrivacyMasked) "OD 02 ** ****" else currentPlateNumber}",
                                color = PrimaryNeon,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Detection Confidence: ${currentScanResult.confidencePercent}%",
                                color = TertiaryFixed,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Violation: ${currentScanResult.violationDescription}",
                                color = ErrorBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Location: ${currentScanResult.location}",
                                color = OnSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Time: ${currentScanResult.timeString}",
                                color = SecondaryBlue,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // NUMBER PLATE VERIFICATION CARD
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerHigh)
                            .border(1.dp, PrimaryNeon.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Pin,
                                        contentDescription = null,
                                        tint = PrimaryNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "NUMBER PLATE DETECTED",
                                        color = PrimaryText,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (plateConfirmed) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(TertiaryEmerald.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "CONFIRMED ✓",
                                            color = TertiaryFixed,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Indian Standard HSRP High-Contrast Plate Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF0038A8))
                                            .padding(horizontal = 3.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "IND",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = if (isPrivacyMasked) "OD 02 ** ****" else currentPlateNumber,
                                        color = Color(0xFF1E293B),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 2.sp
                                    )
                                }

                                Text(
                                    text = "OCR 99.4%",
                                    color = Color(0xFF0F766E),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Action buttons: Confirm Plate & Edit Plate
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        plateConfirmed = true
                                        snackbarMessage = "Plate $currentPlateNumber confirmed by authorized officer."
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (plateConfirmed) TertiaryEmerald.copy(alpha = 0.25f) else TertiaryEmerald
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("confirm_plate_button")
                                ) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = if (plateConfirmed) TertiaryFixed else OnTertiary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "✓ Confirm Plate",
                                        color = if (plateConfirmed) TertiaryFixed else OnTertiary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showEditPlateDialog = true },
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = Brush.linearGradient(listOf(PrimaryNeon, SecondaryBlue))
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("edit_plate_button")
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = null,
                                        tint = PrimaryNeon,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "✏️ Edit Plate",
                                        color = PrimaryNeon,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // EVIDENCE SUMMARY & ACTIONS: SAVE EVIDENCE, CREATE VIOLATION, GENERATE FINE, VIEW ON BILLBOARD
                    Text(
                        text = "VIOLATION DISPATCH ACTIONS:",
                        color = OutlineColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Primary Big Action: "CREATE VIOLATION RECORD"
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val id = repository.createViolationFromScan(
                                    currentScanResult.copy(plateNumber = currentPlateNumber)
                                )
                                latestChallanNumber = ((10000..99999).random()).toString()
                                showSuccessPipelineModal = true
                                repository.recordAudit(
                                    "VIOLATION_PIPELINE_EXECUTED",
                                    "Dispatched: Violation -> Fine -> Alert -> Billboard -> Analytics for $currentPlateNumber"
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("create_violation_record_button")
                    ) {
                        Icon(
                            Icons.Filled.Gavel,
                            contentDescription = null,
                            tint = OnPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CREATE VIOLATION RECORD (AUTO-PIPELINE)",
                            color = OnPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // 4 Quick Secondary Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. SAVE EVIDENCE
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    repository.recordAudit(
                                        "EVIDENCE_ARCHIVED",
                                        "Archived evidence file for plate $currentPlateNumber to cold storage."
                                    )
                                    snackbarMessage = "Evidence securely archived in encrypted vault"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("save_evidence_button"),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "SAVE EVIDENCE",
                                color = PrimaryText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 2. GENERATE FINE
                        Button(
                            onClick = {
                                snackbarMessage = "E-Challan fine of ₹2,000 generated & debited to Vahan ledger"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("generate_fine_button"),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "GENERATE FINE",
                                color = TertiaryFixed,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 3. VIEW ON BILLBOARD
                        Button(
                            onClick = {
                                onNavigateToScreen(AppScreen.VIOLATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighest),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(38.dp)
                                .testTag("view_billboard_button"),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            Icon(
                                Icons.Filled.LiveTv,
                                contentDescription = null,
                                tint = PrimaryNeon,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "BILLBOARD",
                                color = PrimaryNeon,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ==================== 5. BOTTOM HUD TELEMETRY STRIP ====================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, OutlineColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vehicle Detection",
                    color = PrimaryNeon,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "|", color = OutlineColor, fontSize = 9.sp)
                Text(
                    text = "Number Plate",
                    color = TertiaryFixed,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "|", color = OutlineColor, fontSize = 9.sp)
                Text(
                    text = "Noise 91dB",
                    color = ErrorBright,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "|", color = OutlineColor, fontSize = 9.sp)
                Text(
                    text = "Violation",
                    color = ErrorCrimson,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "|", color = OutlineColor, fontSize = 9.sp)
                Text(
                    text = "Evidence",
                    color = SecondaryBlue,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // PRIVACY / PURGE OPTIONS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { isPrivacyMasked = !isPrivacyMasked }
                ) {
                    Icon(
                        if (isPrivacyMasked) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = "Mask PII",
                        tint = SecondaryBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isPrivacyMasked) "Privacy Mask: ENABLED" else "Privacy Mask: DISABLED",
                        color = SecondaryBlue,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "Purge Demo Cache",
                    color = ErrorBright,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable {
                        snackbarMessage = "Demo cache & uploaded files purged cleanly."
                    }
                )
            }
        }
    }

    // ==================== MODALS & DIALOGS ====================

    // 1. Edit Number Plate Dialog
    if (showEditPlateDialog) {
        var editedPlate by remember { mutableStateOf(currentPlateNumber) }
        Dialog(onDismissRequest = { showEditPlateDialog = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerHighest,
                border = BorderStroke(1.dp, PrimaryNeon),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "EDIT NUMBER PLATE OCR",
                        color = PrimaryNeon,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Authorized officers may manually calibrate OCR reading before issuing fine notices.",
                        color = OnSurfaceVariant,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = editedPlate,
                        onValueChange = { editedPlate = it.uppercase() },
                        label = { Text("Plate Number", color = SecondaryBlue) },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditPlateDialog = false }) {
                            Text("CANCEL", color = OutlineColor, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                currentPlateNumber = editedPlate
                                plateConfirmed = true
                                showEditPlateDialog = false
                                snackbarMessage = "Plate updated to $editedPlate"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
                        ) {
                            Text("SAVE & CONFIRM", color = OnPrimary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

    // 2. Success Automated Pipeline Modal (Violation -> Fine -> Alert -> Billboard -> Analytics)
    if (showSuccessPipelineModal) {
        Dialog(onDismissRequest = { showSuccessPipelineModal = false }) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceContainerLowest,
                border = BorderStroke(1.5.dp, TertiaryEmerald),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = TertiaryEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "PIPELINE DISPATCH SUCCESS",
                            color = TertiaryFixed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Vehicle plate $currentPlateNumber automatically processed:",
                        color = OnSurface,
                        fontSize = 12.sp
                    )

                    // Visual Pipeline sequence
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "1. VIOLATION RECORD: ", color = OnSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(text = "SAVED TO ROOM DB", color = PrimaryNeon, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "2. E-CHALLAN GENERATED: ", color = OnSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(text = "₹2,000 (CHALLAN #$latestChallanNumber)", color = TertiaryFixed, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "3. SMS ALERT: ", color = OnSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(text = "DISPATCHED TO VAHAN OWNER", color = SecondaryBlue, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "4. HIGHWAY BILLBOARD: ", color = OnSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(text = "NODE #08 VMS BROADCASTING", color = ErrorBright, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "5. CORRIDOR ANALYTICS: ", color = OnSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(text = "TELEMETRY SYNCHRONISED", color = TertiaryEmerald, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showSuccessPipelineModal = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CLOSE", color = PrimaryText, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                showSuccessPipelineModal = false
                                onNavigateToScreen(AppScreen.VIOLATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text("VIEW BILLBOARD", color = OnPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 3. Security Audit Log Dialog
    if (showAuditDialog) {
        val auditLogs by repository.auditLogs.collectAsState(initial = emptyList())
        Dialog(onDismissRequest = { showAuditDialog = false }) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceContainerLowest,
                border = BorderStroke(1.dp, SecondaryBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .padding(10.dp)
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
                            text = "SECURITY & AUDIT TRAIL",
                            color = PrimaryNeon,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(SurfaceContainerHigh)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = adminClearanceLevel,
                                color = TertiaryFixed,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = "Immutable event log of evidence analysis, plate confirmations, and e-challan issuances.",
                        color = OnSurfaceVariant,
                        fontSize = 10.sp
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(auditLogs) { log ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceContainerLow)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.action,
                                        color = TertiaryFixed,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = log.adminUser,
                                        color = PrimaryNeon,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = log.details,
                                    color = OnSurface,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showAuditDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CLOSE AUDIT LOG", color = OnPrimary, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }

    // Snackbar alert
    snackbarMessage?.let { msg ->
        LaunchedEffect(msg) {
            delay(2600)
            snackbarMessage = null
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
