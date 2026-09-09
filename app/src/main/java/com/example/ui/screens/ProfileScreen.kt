package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import coil.compose.AsyncImage
import com.example.data.OwnerProfile
import com.example.data.TrafficRepository
import com.example.data.ViolationRecord
import com.example.ui.components.AppScreen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: TrafficRepository,
    onNavigateToBillboard: (ViolationRecord) -> Unit,
    onLogoutClick: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val currentOwner by repository.currentOwnerProfile.collectAsState()
    val allOwners by repository.allOwners.collectAsState(initial = emptyList())
    val allViolations by repository.allViolations.collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()

    // Determine current active owner profile
    val activeProfile = currentOwner ?: allOwners.firstOrNull() ?: OwnerProfile(
        id = 1,
        fullName = currentUser?.fullName ?: "Citizen Driver",
        photoUrl = "",
        vehicleRegistrationNumber = currentUser?.badgeOrVehicleNo ?: "OD 02 AB 1234"
    )

    // Filter violations for this owner/vehicle
    val ownerViolations = allViolations.filter {
        it.ownerProfileId == activeProfile.id ||
                it.numberPlate.equals(activeProfile.vehicleRegistrationNumber, ignoreCase = true)
    }
    val unpaidCount = ownerViolations.count { !it.isPaid }
    val totalUnpaidFine = ownerViolations.filter { !it.isPaid }.sumOf { it.fineAmount }
    val hasViolations = ownerViolations.isNotEmpty()

    var showEditDialog by remember { mutableStateOf(false) }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                repository.updateOwnerProfile(activeProfile.copy(photoUrl = uri.toString()))
                snackbarMessage = "Profile photo updated from gallery!"
            }
        }
    }

    Scaffold(
        containerColor = SurfaceContainerLowest,
        snackbarHost = {
            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK", color = PrimaryNeon)
                        }
                    },
                    containerColor = SurfaceContainerHigh,
                    contentColor = PrimaryText
                ) {
                    Text(msg)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))

                // Profile Section Header & Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Citizen Vehicle Dossier",
                                color = PrimaryText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (currentUser?.role == "ADMIN_POLICE") PrimaryNeon.copy(alpha = 0.2f)
                                        else SecondaryBlue.copy(alpha = 0.2f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (currentUser?.role == "ADMIN_POLICE") "OFFICER VIEW" else "OWNER PORTAL",
                                    color = if (currentUser?.role == "ADMIN_POLICE") PrimaryNeon else SecondaryBlue,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Centralized Vahan 4.0 Driver & Vehicle Registry",
                            color = OnSurfaceVariant,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Logout Button
                    OutlinedButton(
                        onClick = onLogoutClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorBright),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorBright.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("profile_logout_button")
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Logout", fontSize = 11.sp)
                    }
                }
            }

            // Profile Quick Switcher (Only visible to Traffic Police Administrator for inspecting citizen profiles)
            if (currentUser?.role == "ADMIN_POLICE") {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "SWITCH REGISTERED OWNER PROFILE:",
                                color = OutlineColor,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allOwners.forEach { owner ->
                                    val isSelected = owner.id == activeProfile.id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) PrimaryNeon.copy(alpha = 0.2f) else SurfaceContainer)
                                            .border(
                                                1.dp,
                                                if (isSelected) PrimaryNeon else OutlineVariant.copy(alpha = 0.3f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                coroutineScope.launch {
                                                    repository.quickSwitchUser("VEHICLE_OWNER", owner.vehicleRegistrationNumber)
                                                }
                                            }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = owner.fullName.split(" ").firstOrNull() ?: owner.fullName,
                                                color = if (isSelected) PrimaryNeon else PrimaryText,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                text = if (owner.photoUrl.isEmpty()) "No Photo" else "Photo OK",
                                                color = if (owner.photoUrl.isEmpty()) WarningAmber else TertiaryEmerald,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Main Profile Identity Hero Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_profile_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Owner Profile Photograph Box
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceContainerHigh)
                                    .border(2.dp, PrimaryNeon.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (activeProfile.photoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = activeProfile.photoUrl,
                                        contentDescription = "Owner Profile Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    // Requirement: Display "Owner Photo Not Available"
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(SurfaceContainerHigh)
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.NoAccounts,
                                            contentDescription = "Owner Photo Not Available",
                                            tint = WarningAmber,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
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

                                // Edit photo overlay button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 4.dp, y = 4.dp)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryNeon)
                                        .clickable { showPhotoPickerSheet = true }
                                        .testTag("change_photo_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.CameraAlt,
                                        contentDescription = "Change Photo",
                                        tint = OnPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Owner Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeProfile.fullName,
                                    color = PrimaryText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Number Plate Banner
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE5A910))
                                        .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = activeProfile.vehicleRegistrationNumber,
                                        color = Color.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${activeProfile.vehicleModel} (${activeProfile.vehicleColor})",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "DL: ${activeProfile.drivingLicense}",
                                    color = SecondaryBlue,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: Edit Profile & Change Photo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showEditDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("edit_profile_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile", color = OnPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showPhotoPickerSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("upload_photo_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryNeon),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Quick Stats Banner: Violations & Fine Overview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "VIOLATIONS",
                        value = "${ownerViolations.size}",
                        subtitle = "Total Recorded",
                        icon = Icons.Filled.Warning,
                        accentColor = if (ownerViolations.isEmpty()) TertiaryEmerald else WarningAmber,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "UNPAID FINE",
                        value = "₹$totalUnpaidFine",
                        subtitle = if (unpaidCount == 0) "All Clear" else "$unpaidCount Pending",
                        icon = Icons.Filled.ReceiptLong,
                        accentColor = if (unpaidCount == 0) TertiaryEmerald else ErrorBright,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "FINE STATUS",
                        value = if (unpaidCount == 0) "CLEAR" else "UNPAID",
                        subtitle = if (unpaidCount == 0) "Zero Due" else "Action Req.",
                        icon = if (unpaidCount == 0) Icons.Filled.Verified else Icons.Filled.PendingActions,
                        accentColor = if (unpaidCount == 0) TertiaryEmerald else ErrorBright,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Full Profile Specifications Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "OFFICIAL RECORD SPECIFICATIONS",
                            color = PrimaryNeon,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        ProfileFieldRow(icon = Icons.Outlined.Phone, label = "Mobile Number", value = activeProfile.mobileNumber)
                        ProfileFieldRow(icon = Icons.Outlined.Email, label = "Email Address", value = activeProfile.email)
                        ProfileFieldRow(icon = Icons.Outlined.DirectionsCar, label = "Vehicle Type", value = activeProfile.vehicleType)
                        ProfileFieldRow(icon = Icons.Outlined.Speed, label = "Vehicle Model", value = activeProfile.vehicleModel)
                        ProfileFieldRow(icon = Icons.Outlined.Palette, label = "Vehicle Color", value = activeProfile.vehicleColor)
                        ProfileFieldRow(icon = Icons.Outlined.Badge, label = "Driving Licence No", value = activeProfile.drivingLicense)
                        ProfileFieldRow(icon = Icons.Outlined.Home, label = "Registered Address", value = activeProfile.address)
                        ProfileFieldRow(icon = Icons.Outlined.ContactEmergency, label = "Emergency Contact", value = activeProfile.emergencyContact)
                        ProfileFieldRow(icon = Icons.Outlined.CalendarMonth, label = "Registry Date", value = activeProfile.registeredDate)
                    }
                }
            }

            // Violations Incurred by this Vehicle / Owner Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VIOLATION HISTORY FOR ${activeProfile.vehicleRegistrationNumber}",
                        color = PrimaryNeon,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${ownerViolations.size} Records",
                        color = OnSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (ownerViolations.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = TertiaryEmerald,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text("No Active Violations", color = PrimaryText, fontWeight = FontWeight.Bold)
                                Text(
                                    "Vehicle ${activeProfile.vehicleRegistrationNumber} has a clean record. No outstanding fines.",
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else {
                items(ownerViolations) { violation ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (violation.isPaid) OutlineVariant.copy(alpha = 0.3f) else ErrorBright.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        if (violation.isPaid) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = if (violation.isPaid) TertiaryEmerald else ErrorBright,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Challan #${violation.challanNumber}",
                                        color = PrimaryText,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (violation.isPaid) TertiaryEmerald.copy(alpha = 0.2f) else ErrorBright.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (violation.isPaid) "PAID" else "UNPAID (${violation.daysOverdue}D OVERDUE)",
                                        color = if (violation.isPaid) TertiaryEmerald else ErrorBright,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = violation.violationType,
                                color = PrimaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "${violation.location} • ${violation.dateFormatted} at ${violation.timeFormatted}",
                                color = OnSurfaceVariant,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Fine: ₹${violation.fineAmount}",
                                    color = if (violation.isPaid) TertiaryEmerald else ErrorBright,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!violation.isPaid) {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    repository.markAsPaid(violation.id, true)
                                                    snackbarMessage = "Fine of ₹${violation.fineAmount} paid successfully!"
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TertiaryEmerald),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Pay Fine", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            repository.setBillboardViolation(violation)
                                            onNavigateToBillboard(violation)
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryNeon),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Tv, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Billboard", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Edit Profile Modal Dialog
    if (showEditDialog) {
        var editFullName by remember { mutableStateOf(activeProfile.fullName) }
        var editMobile by remember { mutableStateOf(activeProfile.mobileNumber) }
        var editEmail by remember { mutableStateOf(activeProfile.email) }
        var editPlate by remember { mutableStateOf(activeProfile.vehicleRegistrationNumber) }
        var editType by remember { mutableStateOf(activeProfile.vehicleType) }
        var editModel by remember { mutableStateOf(activeProfile.vehicleModel) }
        var editColor by remember { mutableStateOf(activeProfile.vehicleColor) }
        var editDL by remember { mutableStateOf(activeProfile.drivingLicense) }
        var editAddress by remember { mutableStateOf(activeProfile.address) }
        var editEmergency by remember { mutableStateOf(activeProfile.emergencyContact) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.EditNote, contentDescription = null, tint = PrimaryNeon)
                    Text("Update Citizen Dossier", color = PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = editFullName,
                            onValueChange = { editFullName = it },
                            label = { Text("Full Name") },
                            singleLine = true,
                            colors = profileTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editPlate,
                            onValueChange = { editPlate = it.uppercase() },
                            label = { Text("Vehicle Registration No") },
                            singleLine = true,
                            colors = profileTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editMobile,
                            onValueChange = { editMobile = it },
                            label = { Text("Mobile Number") },
                            singleLine = true,
                            colors = profileTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("Email Address") },
                            singleLine = true,
                            colors = profileTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editModel,
                            onValueChange = { editModel = it },
                            label = { Text("Vehicle Model") },
                            singleLine = true,
                            colors = profileTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editType,
                            onValueChange = { editType = it },
                            label = { Text("Vehicle Type") },
                            singleLine = true,
                            colors = profileTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editColor,
                            onValueChange = { editColor = it },
                            label = { Text("Vehicle Color") },
                            singleLine = true,
                            colors = profileTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editDL,
                            onValueChange = { editDL = it.uppercase() },
                            label = { Text("Driving Licence No") },
                            singleLine = true,
                            colors = profileTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editAddress,
                            onValueChange = { editAddress = it },
                            label = { Text("Address") },
                            colors = profileTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editEmergency,
                            onValueChange = { editEmergency = it },
                            label = { Text("Emergency Contact") },
                            singleLine = true,
                            colors = profileTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = activeProfile.copy(
                            fullName = editFullName.trim(),
                            mobileNumber = editMobile.trim(),
                            email = editEmail.trim(),
                            vehicleRegistrationNumber = editPlate.trim(),
                            vehicleType = editType.trim(),
                            vehicleModel = editModel.trim(),
                            vehicleColor = editColor.trim(),
                            drivingLicense = editDL.trim(),
                            address = editAddress.trim(),
                            emergencyContact = editEmergency.trim()
                        )
                        coroutineScope.launch {
                            repository.updateOwnerProfile(updated)
                            showEditDialog = false
                            snackbarMessage = "Vehicle owner profile updated successfully!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
                ) {
                    Text("Save Changes", color = OnPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = OnSurfaceVariant)
                }
            },
            containerColor = SurfaceContainerHigh,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Photo Upload / Change Sheet
    if (showPhotoPickerSheet) {
        AlertDialog(
            onDismissRequest = { showPhotoPickerSheet = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = PrimaryNeon)
                    Text("Update Profile Photograph", color = PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Upload a photo from your device, pick a realistic driver portrait preset, or clear to test 'Owner Photo Not Available' state:",
                        color = OnSurfaceVariant,
                        fontSize = 12.sp
                    )

                    // Button 1: Device Photo Picker
                    Button(
                        onClick = {
                            showPhotoPickerSheet = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = OnPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick from Device Gallery", color = OnPrimary, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        "OR CHOOSE CITIZEN PORTRAIT PRESET:",
                        color = OutlineColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    // Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetAvatarButton(
                            name = "Portrait 1",
                            url = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80",
                            modifier = Modifier.weight(1f)
                        ) { url ->
                            coroutineScope.launch {
                                repository.updateOwnerProfile(activeProfile.copy(photoUrl = url))
                                showPhotoPickerSheet = false
                                snackbarMessage = "Photo updated!"
                            }
                        }

                        PresetAvatarButton(
                            name = "Portrait 2",
                            url = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
                            modifier = Modifier.weight(1f)
                        ) { url ->
                            coroutineScope.launch {
                                repository.updateOwnerProfile(activeProfile.copy(photoUrl = url))
                                showPhotoPickerSheet = false
                                snackbarMessage = "Photo updated!"
                            }
                        }

                        PresetAvatarButton(
                            name = "Portrait 3",
                            url = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80",
                            modifier = Modifier.weight(1f)
                        ) { url ->
                            coroutineScope.launch {
                                repository.updateOwnerProfile(activeProfile.copy(photoUrl = url))
                                showPhotoPickerSheet = false
                                snackbarMessage = "Photo updated!"
                            }
                        }
                    }

                    // Button: Remove photo (to test Owner Photo Not Available)
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.updateOwnerProfile(activeProfile.copy(photoUrl = ""))
                                showPhotoPickerSheet = false
                                snackbarMessage = "Photo removed. Status set to 'Owner Photo Not Available'."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.NoAccounts, contentDescription = null, tint = WarningAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove Photo (Test 'Not Available')", color = WarningAmber, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoPickerSheet = false }) {
                    Text("Close", color = OnSurfaceVariant)
                }
            },
            containerColor = SurfaceContainerHigh,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun PresetAvatarButton(
    name: String,
    url: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceContainer)
            .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onSelect(url) }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = url,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, color = PrimaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = OutlineColor,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(13.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = accentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitle,
                color = OnSurfaceVariant,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ProfileFieldRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(0.45f)
        ) {
            Icon(icon, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(15.dp))
            Text(label, color = OnSurfaceVariant, fontSize = 11.sp)
        }
        Text(
            text = value.ifBlank { "Not provided" },
            color = PrimaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(0.55f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun profileTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryNeon,
    unfocusedBorderColor = OutlineVariant,
    focusedLabelColor = PrimaryNeon,
    unfocusedLabelColor = OnSurfaceVariant,
    cursorColor = PrimaryNeon,
    focusedTextColor = PrimaryText,
    unfocusedTextColor = PrimaryText
)
