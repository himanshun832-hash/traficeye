package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.AppDatabase
import com.example.data.TrafficRepository
import com.example.ui.components.AppScreen
import com.example.ui.components.BottomCyberNav
import com.example.ui.components.TopCyberHeader
import com.example.ui.screens.*
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SmartTrafficTheme
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.SurfaceDark

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "FirebaseApp init: ${e.message}")
        }

        val database = AppDatabase.getDatabase(this)
        val repository = TrafficRepository(database.violationDao(), applicationContext)

        setContent {
            SmartTrafficTheme {
                val currentUser by repository.currentUser.collectAsState()
                var currentScreen by remember { mutableStateOf(AppScreen.PROFILE) }

                // Strict role-based navigation guard: non-admin users must NEVER access ADMIN_CONTROL, SCANNER, or DASHBOARD
                LaunchedEffect(currentUser, currentScreen) {
                    if (currentUser != null && currentUser?.role != "ADMIN_POLICE") {
                        if (currentScreen == AppScreen.ADMIN_CONTROL || currentScreen == AppScreen.SCANNER || currentScreen == AppScreen.DASHBOARD) {
                            currentScreen = AppScreen.PROFILE
                        }
                    }
                }

                // Adaptive Multi-Device Responsive Frame:
                // Ensures that whether running on a mobile phone (360-420dp) or a computer / desktop / tablet (600-1920dp+),
                // the UI maintains identical visual balance, centered layout, and crisp cyber proportions.
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceDark),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val isWideScreen = maxWidth > 640.dp

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 620.dp)
                            .fillMaxWidth()
                            .then(
                                if (isWideScreen) {
                                    Modifier
                                        .background(SurfaceContainerLowest)
                                        .border(1.dp, PrimaryNeon.copy(alpha = 0.22f))
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        if (currentUser == null) {
                            LoginScreen(
                                repository = repository,
                                onLoginSuccess = { user ->
                                    currentScreen = if (user.role == "ADMIN_POLICE") AppScreen.DASHBOARD else AppScreen.PROFILE
                                }
                            )
                        } else {
                            Scaffold(
                                topBar = {
                                    TopCyberHeader(
                                        currentScreen = currentScreen,
                                        currentUser = currentUser,
                                        onNotificationsClick = {
                                            currentScreen = AppScreen.VIOLATIONS
                                        },
                                        onProfileClick = {
                                            currentScreen = AppScreen.PROFILE
                                        },
                                        onAdminControlClick = {
                                            if (currentUser?.role == "ADMIN_POLICE") {
                                                currentScreen = AppScreen.ADMIN_CONTROL
                                            }
                                        },
                                        onLogoutClick = {
                                            repository.logout()
                                        }
                                    )
                                },
                                bottomBar = {
                                    BottomCyberNav(
                                        currentScreen = currentScreen,
                                        currentUser = currentUser,
                                        onScreenSelected = { screen ->
                                            if (screen == AppScreen.ADMIN_CONTROL && currentUser?.role != "ADMIN_POLICE") {
                                                currentScreen = AppScreen.PROFILE
                                            } else {
                                                currentScreen = screen
                                            }
                                        }
                                    )
                                },
                                containerColor = SurfaceDark,
                                contentWindowInsets = WindowInsets(0, 0, 0, 0)
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                        .background(SurfaceDark)
                                ) {
                                    Crossfade(
                                        targetState = currentScreen,
                                        label = "ScreenCrossfade"
                                    ) { screen ->
                                        when (screen) {
                                            AppScreen.SCANNER -> {
                                                if (currentUser?.role == "ADMIN_POLICE") {
                                                    ScannerScreen(
                                                        repository = repository,
                                                        onNavigateToScreen = { currentScreen = it }
                                                    )
                                                } else {
                                                    ProfileScreen(
                                                        repository = repository,
                                                        onNavigateToBillboard = { violation ->
                                                            repository.setBillboardViolation(violation)
                                                            currentScreen = AppScreen.VIOLATIONS
                                                        },
                                                        onLogoutClick = { repository.logout() }
                                                    )
                                                }
                                            }
                                            AppScreen.DASHBOARD -> DashboardScreen(
                                                repository = repository,
                                                onNavigateToScreen = { currentScreen = it }
                                            )
                                            AppScreen.TRAFFIC -> TrafficScreen(
                                                repository = repository,
                                                onNavigateToScreen = { currentScreen = it }
                                            )
                                            AppScreen.VIOLATIONS -> ViolationsScreen(
                                                repository = repository,
                                                onNavigateToScreen = { currentScreen = it }
                                            )
                                            AppScreen.PROFILE -> ProfileScreen(
                                                repository = repository,
                                                onNavigateToBillboard = { violation ->
                                                    repository.setBillboardViolation(violation)
                                                    currentScreen = AppScreen.VIOLATIONS
                                                },
                                                onLogoutClick = {
                                                    repository.logout()
                                                }
                                            )
                                            AppScreen.ANALYTICS -> AnalyticsScreen(
                                                repository = repository,
                                                onNavigateToScreen = { currentScreen = it }
                                            )
                                            AppScreen.ADMIN_CONTROL -> {
                                                if (currentUser?.role == "ADMIN_POLICE") {
                                                    AdminControlScreen(
                                                        repository = repository,
                                                        onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
                                                    )
                                                } else {
                                                    ProfileScreen(
                                                        repository = repository,
                                                        onNavigateToBillboard = { violation ->
                                                            repository.setBillboardViolation(violation)
                                                            currentScreen = AppScreen.VIOLATIONS
                                                        },
                                                        onLogoutClick = { repository.logout() }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
