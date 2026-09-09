package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.UserAccount
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen(val label: String, val icon: @Composable (Boolean) -> Unit) {
    DASHBOARD(
        "Dashboard",
        { selected ->
            Icon(
                if (selected) Icons.Filled.GridView else Icons.Outlined.GridView,
                contentDescription = "Dashboard",
                modifier = Modifier.size(20.dp)
            )
        }
    ),
    TRAFFIC(
        "Traffic",
        { selected ->
            Icon(
                if (selected) Icons.Filled.Traffic else Icons.Outlined.Traffic,
                contentDescription = "Traffic",
                modifier = Modifier.size(20.dp)
            )
        }
    ),
    SCANNER(
        "Smart Scanner",
        { selected ->
            Icon(
                if (selected) Icons.Filled.DocumentScanner else Icons.Outlined.DocumentScanner,
                contentDescription = "Smart Scanner",
                modifier = Modifier.size(20.dp)
            )
        }
    ),
    VIOLATIONS(
        "Violations",
        { selected ->
            Icon(
                if (selected) Icons.Filled.FeaturedVideo else Icons.Outlined.FeaturedVideo,
                contentDescription = "Violations",
                modifier = Modifier.size(20.dp)
            )
        }
    ),
    PROFILE(
        "Profile",
        { selected ->
            Icon(
                if (selected) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                contentDescription = "Profile",
                modifier = Modifier.size(20.dp)
            )
        }
    ),
    ANALYTICS(
        "Analytics",
        { selected ->
            Icon(
                if (selected) Icons.Filled.Equalizer else Icons.Outlined.Equalizer,
                contentDescription = "Analytics",
                modifier = Modifier.size(20.dp)
            )
        }
    ),
    ADMIN_CONTROL(
        "Admin Hub",
        { selected ->
            Icon(
                if (selected) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                contentDescription = "Admin Hub",
                modifier = Modifier.size(20.dp)
            )
        }
    )
}

@Composable
fun TopCyberHeader(
    currentScreen: AppScreen,
    currentUser: UserAccount? = null,
    notificationCount: Int = 3,
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAdminControlClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss 'IST'", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        color = SurfaceContainerLowest.copy(alpha = 0.92f),
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo + App Name + Online Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(SurfaceContainerHigh, SurfaceContainerLowest)
                                )
                            )
                            .border(1.dp, PrimaryNeon.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Traffic,
                            contentDescription = "Traffic Eye Logo",
                            tint = PrimaryNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Traffic",
                                color = PrimaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                letterSpacing = (-0.02).sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(PrimaryNeon.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "EYE",
                                    color = PrimaryNeon,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(TertiaryEmerald.copy(alpha = pulseAlpha))
                            )
                            Text(
                                text = "ALL SYSTEMS ONLINE",
                                color = TertiaryText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.05.sp
                            )
                        }
                    }
                }

                // Right action items: Telemetry Clock, Notification Bell, User Avatar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Text(
                            text = "TELEMETRY CLK",
                            color = OutlineColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            letterSpacing = 0.06.sp
                        )
                        Text(
                            text = if (currentTime.isEmpty()) "20:42:18 IST" else currentTime,
                            color = PrimaryFixedDim,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Notification button with badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainer.copy(alpha = 0.8f))
                            .clickable(onClick = onNotificationsClick)
                            .testTag("notification_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = SecondaryBlue,
                            modifier = Modifier.size(19.dp)
                        )
                        if (notificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(ErrorBright),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = notificationCount.toString(),
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // User Profile Chip & Role Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (currentUser != null) {
                            if (currentUser.role == "ADMIN_POLICE") {
                                OutlinedButton(
                                    onClick = onAdminControlClick,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = PrimaryNeon
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("header_admin_hub_button")
                                ) {
                                    Icon(
                                        Icons.Filled.AdminPanelSettings,
                                        contentDescription = "Admin Terminal",
                                        tint = PrimaryNeon,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "ADMIN",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SecondaryBlue.copy(alpha = 0.2f))
                                        .clickable(onClick = onProfileClick)
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "CITIZEN",
                                        color = SecondaryBlue,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (currentUser?.role == "ADMIN_POLICE") PrimaryNeon else SecondaryBlue)
                                .clickable(onClick = onProfileClick)
                                .testTag("profile_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (currentUser?.role == "ADMIN_POLICE") Icons.Filled.LocalPolice else Icons.Filled.Person,
                                contentDescription = "User Profile",
                                tint = OnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sub-bar telemetry status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.MonitorHeart,
                        contentDescription = null,
                        tint = TertiaryEmerald,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "SYS HEALTH: ",
                        color = OnSurfaceVariant,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "98.7%",
                        color = TertiaryFixed,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(10.dp)
                        .background(OutlineVariant)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.Sensors,
                        contentDescription = null,
                        tint = PrimaryNeon,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "IOT NODES: ",
                        color = OnSurfaceVariant,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "1,420 ACTIVE",
                        color = PrimaryNeon,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(10.dp)
                        .background(OutlineVariant)
                )

                Text(
                    text = currentScreen.label,
                    color = SecondaryBlue,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun BottomCyberNav(
    currentScreen: AppScreen,
    currentUser: UserAccount? = null,
    onScreenSelected: (AppScreen) -> Unit
) {
    val navScreens = remember(currentUser?.role) {
        if (currentUser?.role == "ADMIN_POLICE") {
            listOf(
                AppScreen.DASHBOARD,
                AppScreen.TRAFFIC,
                AppScreen.SCANNER,
                AppScreen.VIOLATIONS,
                AppScreen.ADMIN_CONTROL,
                AppScreen.PROFILE
            )
        } else {
            listOf(
                AppScreen.PROFILE,
                AppScreen.VIOLATIONS,
                AppScreen.TRAFFIC,
                AppScreen.ANALYTICS
            )
        }
    }

    Surface(
        color = SurfaceContainerLowest.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navScreens.forEach { screen ->
                val isSelected = currentScreen == screen
                val isScanner = screen == AppScreen.SCANNER
                val isAdminHub = screen == AppScreen.ADMIN_CONTROL

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) {
                                if (isScanner) PrimaryNeon.copy(alpha = 0.18f)
                                else SurfaceContainerHigh.copy(alpha = 0.6f)
                            } else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) {
                                if (isScanner) PrimaryNeon else PrimaryNeon.copy(alpha = 0.4f)
                            } else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onScreenSelected(screen) }
                        .padding(vertical = 4.dp)
                        .testTag("nav_tab_${screen.name.lowercase()}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CompositionLocalProvider(
                            LocalContentColor provides if (isSelected) {
                                if (isScanner) PrimaryNeon else PrimaryNeon
                            } else OnSurfaceVariant
                        ) {
                            screen.icon(isSelected)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = when (screen) {
                            AppScreen.SCANNER -> "Scanner"
                            AppScreen.ADMIN_CONTROL -> "Admin"
                            else -> screen.label
                        },
                        color = if (isSelected) PrimaryNeon else OnSurfaceVariant,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
