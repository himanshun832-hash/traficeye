package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlScreen(
    repository: TrafficRepository,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentUser by repository.currentUser.collectAsStateWithLifecycle()
    val allUsers by repository.allUsers.collectAsStateWithLifecycle(initialValue = emptyList())
    val allViolations by repository.allViolations.collectAsStateWithLifecycle(initialValue = emptyList())
    val auditLogs by repository.auditLogs.collectAsStateWithLifecycle(initialValue = emptyList())
    val fbStatus by repository.firebaseStatus.collectAsStateWithLifecycle()

    var selectedAdminTab by remember { mutableStateOf(0) } // 0: Command & Signals, 1: User Management, 2: Enforcement & Fines, 3: Firebase Cloud, 4: Audit Logs

    // Dialog states
    var showManualChallanDialog by remember { mutableStateOf(false) }
    var showAddOfficerDialog by remember { mutableStateOf(false) }
    var showWaiveDialogForViolation by remember { mutableStateOf<ViolationRecord?>(null) }
    var waiveReasonText by remember { mutableStateOf("") }
    var actionBannerMessage by remember { mutableStateOf<String?>(null) }
    var isSyncingCloud by remember { mutableStateOf(false) }

    // Guard: Only ADMIN_POLICE can access
    val isAdmin = currentUser?.role == "ADMIN_POLICE"

    if (!isAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceContainerLowest)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Security,
                        contentDescription = "Restricted",
                        tint = ErrorBright,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "Access Restricted",
                        color = ErrorBright,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "This terminal requires Traffic Police Administrator Clearance (ADMIN_POLICE). You are currently signed in as a Citizen / Vehicle Owner.",
                        color = OnSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Return to Dashboard", color = OnPrimaryContainer)
                    }
                }
            }
        }
        return
    }

    Scaffold(
        containerColor = SurfaceContainerLowest,
        topBar = {
            Surface(
                color = SurfaceContainerLowest.copy(alpha = 0.95f),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryNeon.copy(alpha = 0.15f))
                                    .border(1.dp, PrimaryNeon, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.LocalPolice,
                                    contentDescription = "Police Shield",
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "ADMIN COMMAND HUB",
                                    color = PrimaryText,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Firebase: trafficeye-99d78 • Clearance L5",
                                    color = SecondaryBlue,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Cloud Sync Quick Button
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isSyncingCloud = true
                                    val res = repository.syncAllDataToFirebase()
                                    isSyncingCloud = false
                                    actionBannerMessage = res
                                }
                            },
                            enabled = !isSyncingCloud,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PrimaryNeon
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("admin_sync_firebase_button")
                        ) {
                            if (isSyncingCloud) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = PrimaryNeon,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(
                                    Icons.Filled.CloudSync,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = PrimaryNeon
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "Sync Cloud",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Subnav Tabs
                    ScrollableTabRow(
                        selectedTabIndex = selectedAdminTab,
                        containerColor = Color.Transparent,
                        contentColor = PrimaryNeon,
                        edgePadding = 0.dp,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedAdminTab == 0,
                            onClick = { selectedAdminTab = 0 },
                            text = { Text("Signals & Grid", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            icon = { Icon(Icons.Filled.Traffic, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedAdminTab == 1,
                            onClick = { selectedAdminTab = 1 },
                            text = { Text("User Access (${allUsers.size})", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            icon = { Icon(Icons.Filled.ManageAccounts, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedAdminTab == 2,
                            onClick = { selectedAdminTab = 2 },
                            text = { Text("Violations & Fines", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            icon = { Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedAdminTab == 3,
                            onClick = { selectedAdminTab = 3 },
                            text = { Text("Firebase Cloud", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            icon = { Icon(Icons.Filled.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedAdminTab == 4,
                            onClick = { selectedAdminTab = 4 },
                            text = { Text("Audit Trail", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            icon = { Icon(Icons.Filled.HistoryEdu, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Status Banner
            actionBannerMessage?.let { msg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = PrimaryNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = msg,
                                    color = PrimaryText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(
                                onClick = { actionBannerMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = OnSurfaceVariant, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            when (selectedAdminTab) {
                0 -> {
                    // TAB 0: Signals & Emergency Grid Lockdown
                    item {
                        EmergencyGridControlSection(
                            onCommand = { cmd ->
                                coroutineScope.launch {
                                    val res = repository.emergencySignalOverride(cmd)
                                    actionBannerMessage = res
                                    Toast.makeText(context, res, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
                1 -> {
                    // TAB 1: User & Officer Management
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SYSTEM ACCOUNTS (${allUsers.size})",
                                color = PrimaryNeon,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showAddOfficerDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("admin_add_officer_button")
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = OnPrimaryContainer)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Officer", color = OnPrimaryContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(allUsers) { user ->
                        UserAccountAdminCard(
                            user = user,
                            onToggleRole = {
                                coroutineScope.launch {
                                    val newRole = if (user.role == "ADMIN_POLICE") "VEHICLE_OWNER" else "ADMIN_POLICE"
                                    repository.updateUserRole(user.id, newRole)
                                    actionBannerMessage = "Updated ${user.username} role to $newRole"
                                }
                            },
                            onToggleActive = {
                                coroutineScope.launch {
                                    repository.toggleUserActive(user.id, !user.isActive)
                                    actionBannerMessage = "User ${user.username} status set to ${if (!user.isActive) "ACTIVE" else "SUSPENDED"}"
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    repository.deleteUser(user.id)
                                    actionBannerMessage = "Account ${user.username} deleted from system"
                                }
                            }
                        )
                    }
                }
                2 -> {
                    // TAB 2: Violations & Fine Enforcement
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FINE ENFORCEMENT & E-CHALLANS",
                                color = PrimaryNeon,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val count = repository.massSmsDispatch(unpaidOnly = true)
                                            actionBannerMessage = "Dispatched SMS legal summons notices to $count owners."
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryBlue),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryBlue.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mass SMS", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }

                                Button(
                                    onClick = { showManualChallanDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("admin_issue_manual_challan_button")
                                ) {
                                    Icon(Icons.Filled.AddCard, contentDescription = null, modifier = Modifier.size(14.dp), tint = OnPrimaryContainer)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Manual Challan", color = OnPrimaryContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    items(allViolations) { v ->
                        ViolationAdminCard(
                            violation = v,
                            onWaive = { showWaiveDialogForViolation = v },
                            onTogglePaid = {
                                coroutineScope.launch {
                                    repository.markAsPaid(v.id, !v.isPaid)
                                }
                            },
                            onToggleBillboard = {
                                coroutineScope.launch {
                                    repository.toggleBillboardBroadcast(v.id, !v.isBillboardBroadcasted)
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    repository.deleteViolation(v.id)
                                    actionBannerMessage = "Purged Challan #${v.challanNumber}"
                                }
                            }
                        )
                    }
                }
                3 -> {
                    // TAB 3: Firebase Cloud Configuration
                    item {
                        FirebaseCloudHubSection(
                            status = fbStatus,
                            onForceSync = {
                                coroutineScope.launch {
                                    isSyncingCloud = true
                                    val res = repository.syncAllDataToFirebase()
                                    isSyncingCloud = false
                                    actionBannerMessage = res
                                }
                            }
                        )
                    }
                }
                4 -> {
                    // TAB 4: Audit Logs
                    item {
                        Text(
                            text = "OFFICIAL POLICE & ADMIN AUDIT TRAIL",
                            color = PrimaryNeon,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(auditLogs) { log ->
                        AuditLogItem(log = log)
                    }
                }
            }
        }
    }

    // Modal: Issue Manual E-Challan
    if (showManualChallanDialog) {
        ManualChallanDialog(
            onDismiss = { showManualChallanDialog = false },
            onSubmit = { plate, vType, vModel, ownerName, vioType, fine, loc, noise ->
                coroutineScope.launch {
                    val id = repository.createManualChallan(
                        numberPlate = plate,
                        vehicleType = vType,
                        vehicleModel = vModel,
                        ownerName = ownerName,
                        violationType = vioType,
                        fineAmount = fine,
                        location = loc,
                        noiseDb = noise
                    )
                    actionBannerMessage = "Issued manual Challan to $plate (₹$fine) registered with ID #$id"
                    showManualChallanDialog = false
                }
            }
        )
    }

    // Modal: Add Police Officer
    if (showAddOfficerDialog) {
        AddOfficerDialog(
            onDismiss = { showAddOfficerDialog = false },
            onSubmit = { username, email, pass, name, badge ->
                coroutineScope.launch {
                    val res = repository.register(
                        username = username,
                        email = email,
                        passwordEntered = pass,
                        role = "ADMIN_POLICE",
                        fullName = name,
                        vehicleOrBadge = badge
                    )
                    if (res.isSuccess) {
                        actionBannerMessage = "Successfully registered Police Officer $name (Badge: $badge) with Firebase sync."
                        showAddOfficerDialog = false
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Modal: Waive Challan
    showWaiveDialogForViolation?.let { targetV ->
        AlertDialog(
            onDismissRequest = { showWaiveDialogForViolation = null },
            containerColor = SurfaceContainer,
            title = {
                Text(
                    text = "Waive Challan #${targetV.challanNumber}",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Forgive ₹${targetV.fineAmount} fine for vehicle ${targetV.numberPlate} (${targetV.ownerName}). Enter official police waiver justification:",
                        color = OnSurfaceVariant,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = waiveReasonText,
                        onValueChange = { waiveReasonText = it },
                        placeholder = { Text("e.g., Emergency Medical Transit, False Positive Plate") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryNeon,
                            unfocusedBorderColor = OutlineColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.waiveChallan(targetV.id, waiveReasonText.ifBlank { "Official Police Discretion" })
                            actionBannerMessage = "Challan #${targetV.challanNumber} waived to ₹0."
                            showWaiveDialogForViolation = null
                            waiveReasonText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
                ) {
                    Text("Confirm Waiver", color = OnPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWaiveDialogForViolation = null }) {
                    Text("Cancel", color = OnSurfaceVariant)
                }
            }
        )
    }
}

// Subcomponent: Emergency Grid Signals
@Composable
fun EmergencyGridControlSection(
    onCommand: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
            Text(
                text = "EMERGENCY GRID OVERRIDES (METROPOLIS CORE)",
                color = WarningAmber,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // 4 Grid action cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EmergencyActionCard(
                title = "FORCE ALL RED",
                subtitle = "Metropolitan Grid Lockdown",
                icon = Icons.Filled.StopCircle,
                tintColor = ErrorBright,
                modifier = Modifier.weight(1f),
                onClick = { onCommand("FORCE_ALL_RED") }
            )
            EmergencyActionCard(
                title = "GREEN CORRIDOR",
                subtitle = "Ambulance / VIP Transit",
                icon = Icons.Filled.CheckCircle,
                tintColor = PrimaryNeon,
                modifier = Modifier.weight(1f),
                onClick = { onCommand("GREEN_CORRIDOR") }
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EmergencyActionCard(
                title = "SONIC SIREN (100 dB)",
                subtitle = "Anti-Honking Deterrent",
                icon = Icons.Filled.VolumeUp,
                tintColor = WarningAmber,
                modifier = Modifier.weight(1f),
                onClick = { onCommand("ANTI_HONKING_ALARM") }
            )
            EmergencyActionCard(
                title = "RESET TO AI MODE",
                subtitle = "Autonomous IoT Balancing",
                icon = Icons.Filled.AutoAwesome,
                tintColor = SecondaryBlue,
                modifier = Modifier.weight(1f),
                onClick = { onCommand("RESET_AI") }
            )
        }
    }
}

@Composable
fun EmergencyActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tintColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .border(1.dp, tintColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(tintColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(18.dp))
            }
            Text(
                text = title,
                color = tintColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                color = OnSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

// Subcomponent: User Account Admin Card
@Composable
fun UserAccountAdminCard(
    user: UserAccount,
    onToggleRole: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val isPolice = user.role == "ADMIN_POLICE"

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (!user.isActive) ErrorBright.copy(alpha = 0.4f)
                else if (isPolice) PrimaryNeon.copy(alpha = 0.3f)
                else OutlineVariant,
                RoundedCornerShape(10.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isPolice) PrimaryNeon else SecondaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPolice) Icons.Filled.LocalPolice else Icons.Filled.Person,
                            contentDescription = null,
                            tint = OnPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = user.fullName.ifBlank { user.username },
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${user.email} • @${user.username}",
                            color = OnSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Role badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPolice) PrimaryNeon.copy(alpha = 0.2f) else SecondaryBlue.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isPolice) "ADMIN POLICE" else "CITIZEN",
                        color = if (isPolice) PrimaryNeon else SecondaryBlue,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Identification & Firebase details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPolice) "Badge: ${user.badgeOrVehicleNo}" else "Plate: ${user.badgeOrVehicleNo}",
                    color = PrimaryFixedDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )

                if (user.firebaseUid.isNotEmpty()) {
                    Text(
                        text = "Firebase UID: ${user.firebaseUid.take(8)}...",
                        color = SecondaryBlue,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                } else {
                    Text(
                        text = "Local Cache",
                        color = OnSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
            }

            Divider(color = OutlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status badge & toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (user.isActive) PrimaryNeon else ErrorBright)
                    )
                    Text(
                        text = if (user.isActive) "ACTIVE" else "SUSPENDED",
                        color = if (user.isActive) PrimaryNeon else ErrorBright,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = onToggleRole,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isPolice) "Demote to Citizen" else "Promote to Admin",
                            color = SecondaryBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = onToggleActive,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (user.isActive) "Suspend" else "Activate",
                            color = if (user.isActive) WarningAmber else PrimaryNeon,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = ErrorBright.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Subcomponent: Violation Admin Card
@Composable
fun ViolationAdminCard(
    violation: ViolationRecord,
    onWaive: () -> Unit,
    onTogglePaid: () -> Unit,
    onToggleBillboard: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
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
                    Text(
                        text = violation.numberPlate,
                        color = PrimaryNeon,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• Challan #${violation.challanNumber}",
                        color = OnSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = "₹${violation.fineAmount}",
                    color = if (violation.isPaid) PrimaryNeon else ErrorBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "${violation.violationType} • ${violation.location}",
                color = OnSurfaceVariant,
                fontSize = 11.sp
            )

            Text(
                text = "Owner: ${violation.ownerName} • Contact: ${violation.ownerMobile}",
                color = PrimaryText,
                fontSize = 11.sp
            )

            Divider(color = OutlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

            // Admin Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Billboard broadcast toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable(onClick = onToggleBillboard)
                ) {
                    Icon(
                        if (violation.isBillboardBroadcasted) Icons.Filled.Tv else Icons.Outlined.TvOff,
                        contentDescription = null,
                        tint = if (violation.isBillboardBroadcasted) PrimaryNeon else OnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (violation.isBillboardBroadcasted) "Billboard ON" else "Billboard OFF",
                        color = if (violation.isBillboardBroadcasted) PrimaryNeon else OnSurfaceVariant,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!violation.isPaid && violation.fineAmount > 0) {
                        OutlinedButton(
                            onClick = onWaive,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Waive Fine", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onTogglePaid,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (violation.isPaid) PrimaryNeon else ErrorBright
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (violation.isPaid) PrimaryNeon.copy(alpha = 0.5f) else ErrorBright.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(if (violation.isPaid) "Mark Unpaid" else "Mark Paid", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = ErrorBright.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

// Subcomponent: Firebase Cloud Hub Section
@Composable
fun FirebaseCloudHubSection(
    status: FirebaseStatus,
    onForceSync: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PrimaryNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarningAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            text = "FIREBASE METROPOLIS CLOUD",
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Active Project: ${status.projectId}",
                            color = PrimaryNeon,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }

                Divider(color = OutlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CloudDetailRow("PROJECT ID", status.projectId)
                    CloudDetailRow("PROJECT NUMBER", "1059894291798")
                    CloudDetailRow("REALTIME DB", "https://trafficeye-99d78-default-rtdb.firebaseio.com")
                    CloudDetailRow("STORAGE BUCKET", "trafficeye-99d78.firebasestorage.app")
                    CloudDetailRow("APP ID", "1:1059894291798:android:da5dc6e20516a70f2efaf7")
                    CloudDetailRow("PACKAGE NAME", "com.trafficeye.app")
                    CloudDetailRow("SESSION UID", status.currentUid ?: "Active Service Connected")
                    CloudDetailRow("LAST STATUS", status.lastSyncMessage)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onForceSync,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Push All Records to Firebase Firestore", color = OnPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CloudDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = OnSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(
            text = value,
            color = PrimaryText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

// Subcomponent: Audit Log Item
@Composable
fun AuditLogItem(log: AuditLog) {
    val sdf = SimpleDateFormat("HH:mm:ss • dd MMM", Locale.getDefault())
    val formattedTime = sdf.format(Date(log.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.action,
                    color = PrimaryNeon,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = formattedTime,
                    color = OnSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }

            Text(
                text = log.details,
                color = PrimaryText,
                fontSize = 11.sp
            )

            Text(
                text = "Actor: ${log.adminUser}",
                color = SecondaryBlue,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
        }
    }
}

// Dialog: Issue Manual Challan
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualChallanDialog(
    onDismiss: () -> Unit,
    onSubmit: (plate: String, vType: String, vModel: String, ownerName: String, vioType: String, fine: Int, loc: String, noise: Int) -> Unit
) {
    var plate by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("Mahindra XUV700") }
    var vehicleType by remember { mutableStateOf("SUV") }
    var violationType by remember { mutableStateOf("Excessive Honking (92 dB)") }
    var fineAmount by remember { mutableStateOf("2000") }
    var location by remember { mutableStateOf("MG Road Silent Zone Pole 3") }
    var noiseDb by remember { mutableStateOf("92") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Gavel, contentDescription = null, tint = PrimaryNeon)
                Text("Issue Manual E-Challan", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = { Text("Vehicle Registration Plate") },
                    placeholder = { Text("e.g. OD 02 AB 1234") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner Full Name (Optional)") },
                    placeholder = { Text("Auto-matched from database if blank") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = violationType,
                    onValueChange = { violationType = it },
                    label = { Text("Violation Classification") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fineAmount,
                        onValueChange = { fineAmount = it },
                        label = { Text("Fine (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = noiseDb,
                        onValueChange = { noiseDb = it },
                        label = { Text("Noise (dB)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Incident Location / Camera Node") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (plate.isNotBlank()) {
                        onSubmit(
                            plate,
                            vehicleType,
                            vehicleModel,
                            ownerName,
                            violationType,
                            fineAmount.toIntOrNull() ?: 1000,
                            location,
                            noiseDb.toIntOrNull() ?: 0
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                enabled = plate.isNotBlank()
            ) {
                Text("Register & Dispatch", color = OnPrimaryContainer, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OnSurfaceVariant)
            }
        }
    )
}

// Dialog: Add Police Officer
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOfficerDialog(
    onDismiss: () -> Unit,
    onSubmit: (username: String, email: String, pass: String, name: String, badge: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var badgeNo by remember { mutableStateOf("BADGE-") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.LocalPolice, contentDescription = null, tint = PrimaryNeon)
                Text("Register Police Officer", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Officer Full Name") },
                    placeholder = { Text("e.g. Inspector A. K. Verma") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = badgeNo,
                    onValueChange = { badgeNo = it.uppercase() },
                    label = { Text("Police Badge ID") },
                    placeholder = { Text("BADGE-9912") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Terminal Username") },
                    placeholder = { Text("officer.verma") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Official Email Address") },
                    placeholder = { Text("officer.verma@trafficeye.gov.in") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank() && email.isNotBlank() && password.length >= 6) {
                        onSubmit(username, email, password, fullName, badgeNo)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                enabled = username.isNotBlank() && email.isNotBlank() && password.length >= 6
            ) {
                Text("Authorize Officer", color = OnPrimaryContainer, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OnSurfaceVariant)
            }
        }
    )
}
