package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TrafficRepository
import com.example.data.UserAccount
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    repository: TrafficRepository,
    onLoginSuccess: (UserAccount) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var vehicleOrBadge by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("VEHICLE_OWNER") } // "ADMIN_POLICE" or "VEHICLE_OWNER"
    var rememberMe by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordInput by remember { mutableStateOf("") }
    var forgotPasswordStatus by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceContainerLowest)
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Branding Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(SurfaceContainerHigh, SurfaceContainerLowest)
                        )
                    )
                    .border(1.5.dp, PrimaryNeon, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Traffic,
                    contentDescription = "Traffic Logo",
                    tint = PrimaryNeon,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Traffic",
                    color = PrimaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PrimaryNeon.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "EYE",
                        color = PrimaryNeon,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "IoT-Based Traffic Control & Enforcement Portal",
                color = OnSurfaceVariant,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryContainer.copy(alpha = 0.25f))
                    .border(1.dp, PrimaryNeon.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Firebase Auth & Firestore: trafficeye-99d78",
                    color = PrimaryNeon,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Toggle: Login vs Register Tabs
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    TabButton(
                        text = "Sign In",
                        selected = !isRegisterMode,
                        modifier = Modifier.weight(1f)
                    ) {
                        isRegisterMode = false
                        errorMessage = null
                        successMessage = null
                    }
                    TabButton(
                        text = "Register",
                        selected = isRegisterMode,
                        modifier = Modifier.weight(1f)
                    ) {
                        isRegisterMode = true
                        errorMessage = null
                        successMessage = null
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Card Form
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "Create Citizen / Officer Account" else "Authorized Access Gateway",
                        color = PrimaryNeon,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    // Error Message Banner
                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let { msg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ErrorBright.copy(alpha = 0.15f))
                                    .border(1.dp, ErrorBright.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = ErrorBright,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = msg,
                                    color = ErrorBright,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Success Message Banner
                    AnimatedVisibility(visible = successMessage != null) {
                        successMessage?.let { msg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TertiaryEmerald.copy(alpha = 0.15f))
                                    .border(1.dp, TertiaryEmerald.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Success",
                                    tint = TertiaryEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = msg,
                                    color = TertiaryEmerald,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (isRegisterMode) {
                        // Role Selector for Registration
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "SELECT ACCOUNT TYPE:",
                                color = OnSurfaceVariant,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedRole == "VEHICLE_OWNER",
                                    onClick = { selectedRole = "VEHICLE_OWNER" },
                                    label = { Text("Citizen Driver", fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.DirectionsCar,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SecondaryBlue.copy(alpha = 0.25f),
                                        selectedLabelColor = SecondaryBlue,
                                        selectedLeadingIconColor = SecondaryBlue
                                    )
                                )
                                FilterChip(
                                    selected = selectedRole == "ADMIN_POLICE",
                                    onClick = { selectedRole = "ADMIN_POLICE" },
                                    label = { Text("Traffic Police", fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.LocalPolice,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryNeon.copy(alpha = 0.25f),
                                        selectedLabelColor = PrimaryNeon,
                                        selectedLeadingIconColor = PrimaryNeon
                                    )
                                )
                            }
                        }

                        // Full Name
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text(if (selectedRole == "ADMIN_POLICE") "Officer Full Name" else "Citizen Full Name") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = PrimaryNeon)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_fullname_input"),
                            colors = cyberTextFieldColors(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(if (selectedRole == "ADMIN_POLICE") "Official Police Email" else "Email Address") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Email, contentDescription = null, tint = PrimaryNeon)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_email_input"),
                            colors = cyberTextFieldColors(),
                            keyboardOptions = KeyboardOptions(
                                autoCorrect = false,
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        // Vehicle Number or Badge Number
                        OutlinedTextField(
                            value = vehicleOrBadge,
                            onValueChange = { vehicleOrBadge = it.uppercase() },
                            label = {
                                Text(if (selectedRole == "VEHICLE_OWNER") "Vehicle Registration (e.g. OD 02 AB 1234)" else "Police Badge # (e.g. BADGE-8842)")
                            },
                            leadingIcon = {
                                Icon(
                                    if (selectedRole == "VEHICLE_OWNER") Icons.Outlined.Pin else Icons.Outlined.Badge,
                                    contentDescription = null,
                                    tint = PrimaryNeon
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_vehicle_badge_input"),
                            colors = cyberTextFieldColors(),
                            keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                    } else {
                        // Quick Autofill helpers in Login Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "QUICK FILL:",
                                color = OutlineColor,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            AssistChip(
                                onClick = {
                                    usernameOrEmail = "officer.patil"
                                    password = "password123"
                                    errorMessage = null
                                },
                                label = { Text("👮 Admin Patil", fontSize = 10.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = SurfaceContainerLowest,
                                    labelColor = PrimaryNeon
                                )
                            )
                            AssistChip(
                                onClick = {
                                    usernameOrEmail = "rahul.kumar"
                                    password = "password123"
                                    errorMessage = null
                                },
                                label = { Text("🚗 Rahul", fontSize = 10.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = SurfaceContainerLowest,
                                    labelColor = SecondaryBlue
                                )
                            )
                        }
                    }

                    // Username / Identifier
                    OutlinedTextField(
                        value = usernameOrEmail,
                        onValueChange = { usernameOrEmail = it },
                        label = { Text(if (isRegisterMode) "Choose Username" else "Username, Email, or Vehicle Plate") },
                        placeholder = { Text(if (isRegisterMode) "e.g. rahul.kumar" else "officer.patil, rahul.kumar, OD 02 AB 1234") },
                        leadingIcon = {
                            Icon(Icons.Outlined.AccountCircle, contentDescription = null, tint = PrimaryNeon)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_username_input"),
                        colors = cyberTextFieldColors(),
                        keyboardOptions = KeyboardOptions(
                            autoCorrect = false,
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    val performSubmit: () -> Unit = {
                        errorMessage = null
                        successMessage = null
                        if (isRegisterMode) {
                            if (fullName.isBlank() || email.isBlank() || usernameOrEmail.isBlank() || password.isBlank()) {
                                errorMessage = "Please fill in all mandatory fields."
                            } else if (password.length < 6) {
                                errorMessage = "Password must be at least 6 characters."
                            } else if (password != confirmPassword) {
                                errorMessage = "Passwords do not match."
                            } else if (!email.contains("@")) {
                                errorMessage = "Please enter a valid email address."
                            } else {
                                isLoading = true
                                coroutineScope.launch {
                                    val result = repository.register(
                                        username = usernameOrEmail,
                                        email = email,
                                        passwordEntered = password,
                                        role = selectedRole,
                                        fullName = fullName,
                                        vehicleOrBadge = vehicleOrBadge
                                    )
                                    isLoading = false
                                    result.onSuccess { user ->
                                        successMessage = "Account registered successfully! Welcome ${user.fullName}."
                                        onLoginSuccess(user)
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Registration failed."
                                    }
                                }
                            }
                        } else {
                            if (usernameOrEmail.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter your username/email/vehicle plate and password."
                            } else {
                                isLoading = true
                                coroutineScope.launch {
                                    val result = repository.login(usernameOrEmail, password, rememberMe)
                                    isLoading = false
                                    result.onSuccess { user ->
                                        onLoginSuccess(user)
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Authentication failed."
                                    }
                                }
                            }
                        }
                    }

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = PrimaryNeon)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Toggle password visibility",
                                    tint = OnSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        colors = cyberTextFieldColors(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = {
                                focusManager.clearFocus()
                                performSubmit()
                            }
                        )
                    )

                    if (isRegisterMode) {
                        // Confirm Password
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            leadingIcon = {
                                Icon(Icons.Outlined.LockReset, contentDescription = null, tint = PrimaryNeon)
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_confirm_password_input"),
                            colors = cyberTextFieldColors(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    performSubmit()
                                }
                            )
                        )
                    } else {
                        // Remember Me & Forgot Password Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { rememberMe = !rememberMe }
                            ) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PrimaryNeon,
                                        checkmarkColor = OnPrimary
                                    )
                                )
                                Text(
                                    text = "Remember Me",
                                    color = PrimaryText,
                                    fontSize = 12.sp
                                )
                            }

                            TextButton(
                                onClick = {
                                    showForgotPasswordDialog = true
                                    forgotPasswordStatus = null
                                }
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    color = SecondaryBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Primary Submit Button
                    Button(
                        onClick = performSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = OnPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                if (isRegisterMode) Icons.Filled.PersonAdd else Icons.Filled.LockOpen,
                                contentDescription = null,
                                tint = OnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRegisterMode) {
                                    if (selectedRole == "ADMIN_POLICE") "CREATE POLICE ACCOUNT" else "CREATE CITIZEN ACCOUNT"
                                } else "SECURE LOGIN",
                                color = OnPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Demo Credentials Selector
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = PrimaryNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "QUICK DEMO ACCESS (ONE-TAP LOGIN)",
                            color = OnSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "One-tap demo sign in for test accounts:",
                        color = OutlineColor,
                        fontSize = 11.sp
                    )

                    val performDemoClick: (String, String) -> Unit = { user, pass ->
                        errorMessage = null
                        successMessage = null
                        isLoading = true
                        usernameOrEmail = user
                        password = pass
                        coroutineScope.launch {
                            val res = repository.login(user, pass, rememberMe)
                            isLoading = false
                            res.onSuccess {
                                onLoginSuccess(it)
                            }.onFailure { err ->
                                errorMessage = err.message ?: "Authentication failed."
                            }
                        }
                    }

                    DemoLoginChip(
                        role = "TRAFFIC POLICE ADMIN",
                        name = "Officer S. Patil",
                        details = "Enforcement Control Room & Full Scanner (Admin)",
                        badge = "Police Admin",
                        badgeColor = PrimaryNeon
                    ) {
                        performDemoClick("officer.patil", "password123")
                    }

                    DemoLoginChip(
                        role = "VEHICLE OWNER (DEFAULT)",
                        name = "Rahul Kumar",
                        details = "OD 02 AB 1234 • Mahindra XUV700 (Citizen Portal)",
                        badge = "Citizen",
                        badgeColor = SecondaryBlue
                    ) {
                        performDemoClick("rahul.kumar", "password123")
                    }

                    DemoLoginChip(
                        role = "VEHICLE OWNER",
                        name = "Ananya Sharma",
                        details = "MH 04 XY 7711 • Honda City ZX (Citizen Portal)",
                        badge = "Citizen",
                        badgeColor = SecondaryBlue
                    ) {
                        performDemoClick("ananya.sharma", "password123")
                    }

                    DemoLoginChip(
                        role = "VEHICLE OWNER (COMMERCIAL)",
                        name = "Suresh Patel",
                        details = "KA 05 MN 9012 • Commercial Truck (Citizen Portal)",
                        badge = "Citizen",
                        badgeColor = WarningAmber
                    ) {
                        performDemoClick("suresh.patel", "password123")
                    }
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = PrimaryNeon)
                    Text("Password Recovery", color = PrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter your registered username or email address to recover your account credentials.",
                        color = OnSurfaceVariant,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = forgotPasswordInput,
                        onValueChange = { forgotPasswordInput = it },
                        label = { Text("Username or Email") },
                        singleLine = true,
                        colors = cyberTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    forgotPasswordStatus?.let { status ->
                        Text(
                            text = status,
                            color = PrimaryNeon,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotPasswordInput.isNotBlank()) {
                            coroutineScope.launch {
                                val res = repository.forgotPassword(forgotPasswordInput)
                                res.onSuccess {
                                    forgotPasswordStatus = it
                                }.onFailure {
                                    forgotPasswordStatus = it.message
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
                ) {
                    Text("Recover", color = OnPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Close", color = OnSurfaceVariant)
                }
            },
            containerColor = SurfaceContainerHigh,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PrimaryNeon else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) OnPrimary else OnSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun RoleOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PrimaryNeon.copy(alpha = 0.15f) else SurfaceContainer
        ),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) PrimaryNeon else OutlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) PrimaryNeon else OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = if (selected) PrimaryNeon else PrimaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = OnSurfaceVariant,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun DemoLoginChip(
    role: String,
    name: String,
    details: String,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceContainerHigh.copy(alpha = 0.5f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = name,
                    color = PrimaryText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(badgeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = details,
                color = OnSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Icon(
            Icons.Filled.ArrowForward,
            contentDescription = "Login",
            tint = PrimaryNeon,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun cyberTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryNeon,
    unfocusedBorderColor = OutlineVariant,
    focusedLabelColor = PrimaryNeon,
    unfocusedLabelColor = OnSurfaceVariant,
    cursorColor = PrimaryNeon,
    focusedTextColor = PrimaryText,
    unfocusedTextColor = PrimaryText
)
