package com.pocketdoctor.personnelwellness.ui.screen

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.pocketdoctor.personnelwellness.data.model.ApiResult
import com.pocketdoctor.personnelwellness.data.model.AuthState
import com.pocketdoctor.personnelwellness.data.model.UserRole
import com.pocketdoctor.personnelwellness.data.model.WellnessRecord
import com.pocketdoctor.personnelwellness.data.model.WellnessRisk
import com.pocketdoctor.personnelwellness.ui.components.*
import com.pocketdoctor.personnelwellness.ui.navigation.Screen
import com.pocketdoctor.personnelwellness.ui.theme.*
import com.pocketdoctor.personnelwellness.ui.viewmodel.AuthViewModel
import com.pocketdoctor.personnelwellness.ui.viewmodel.ConnectionState
import com.pocketdoctor.personnelwellness.ui.viewmodel.WellnessViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel? = null
) {
    val authState = authViewModel?.authState?.collectAsStateWithLifecycle()?.value ?: AuthState()

    SplashScreenContent(
        authState = authState,
        onNavigateToDestination = { destination ->
            navController.navigate(destination) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    )
}

@Composable
fun SplashScreenContent(
    authState: AuthState = AuthState(),
    onNavigateToDestination: (String) -> Unit = {}
) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000)
        if (authState.isLoggedIn) {
            val destination = when (authState.userRole) {
                UserRole.PERSONNEL -> Screen.Home.route
                UserRole.WELFARE_OFFICER -> Screen.WelfareDashboard.route
                UserRole.COMMANDER -> Screen.CommanderDashboard.route
                UserRole.ADMIN -> Screen.AdminDashboard.route
                else -> Screen.Login.route
            }
            onNavigateToDestination(destination)
        } else {
            onNavigateToDestination(Screen.PrivacyConsent.route)
        }
    }

    LiquidBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(tween(1000)) + expandVertically(tween(1000))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GlassCard(
                        cornerRadius = 32.dp,
                        elevation = 12.dp,
                        backgroundColor = Color(0x3300E5FF)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Logo",
                                tint = ElectricCyan,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Personnel Wellness",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Smarter Wellness. Stronger Workforce.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = CoolBlueLight,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = SoftCyanGlow,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "SIH26186",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyConsentScreen(navController: NavController) {
    var checked by remember { mutableStateOf(false) }

    LiquidBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ElectricCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Privacy",
                    modifier = Modifier.size(44.dp),
                    tint = ElectricCyan
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Privacy & Consent Agreement",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Your wellness data, daily logs, and self-assessments are strictly confidential. Data is stored and processed securely to analyze workplace wellness risks and provide recommendations. By checking below, you agree to allow local analysis of your submitted indicators.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        colors = CheckboxDefaults.colors(checkedColor = ElectricCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I read and agree to the Privacy terms",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlassButton(
                text = "Accept & Continue",
                onClick = { navController.navigate(Screen.Login.route) },
                enabled = checked,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel? = null
) {
    val authState = authViewModel?.authState?.collectAsStateWithLifecycle()?.value ?: AuthState()
    val connectionState = authViewModel?.connectionState?.collectAsStateWithLifecycle()?.value ?: ConnectionState.Idle

    LoginScreenContent(
        authState = authState,
        connectionState = connectionState,
        onLogin = { username, password -> authViewModel?.login(username, password) },
        onTestConnection = { authViewModel?.testBackendConnection() },
        onClearError = { authViewModel?.clearError() },
        onNavigateToDestination = { destination ->
            navController.navigate(destination) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    )
}

@Composable
fun LoginScreenContent(
    authState: AuthState = AuthState(),
    connectionState: ConnectionState = ConnectionState.Idle,
    onLogin: (String, String) -> Unit = { _, _ -> },
    onTestConnection: () -> Unit = {},
    onClearError: () -> Unit = {},
    onNavigateToDestination: (String) -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(authState) {
        if (authState.isLoggedIn) {
            val destination = when (authState.userRole) {
                UserRole.PERSONNEL -> Screen.Home.route
                UserRole.WELFARE_OFFICER -> Screen.WelfareDashboard.route
                UserRole.COMMANDER -> Screen.CommanderDashboard.route
                UserRole.ADMIN -> Screen.AdminDashboard.route
                else -> Screen.Home.route
            }
            onNavigateToDestination(destination)
        }
        authState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            onClearError()
        }
    }

    LiquidBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ElectricCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Login Logo",
                    modifier = Modifier.size(52.dp),
                    tint = ElectricCyan
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Personnel Login",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Welcome back, Officer. Enter your credentials.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                GlassTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Personnel ID / Email",
                    leadingIcon = Icons.Default.Person,
                    enabled = !authState.isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                GlassTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    leadingIcon = Icons.Default.Lock,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Done else Icons.Default.Info,
                                contentDescription = "Toggle Password",
                                tint = CoolBlueLight
                            )
                        }
                    },
                    enabled = !authState.isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                GlassButton(
                    text = "Login",
                    onClick = { onLogin(username, password) },
                    isLoading = authState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                GlassOutlinedButton(
                    text = "Test Backend Connection",
                    onClick = onTestConnection,
                    isLoading = connectionState is ConnectionState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Refresh
                )

                when (connectionState) {
                    is ConnectionState.Success -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = connectionState.message,
                            color = LowRiskGreen,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    is ConnectionState.Error -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = connectionState.title,
                                color = HighRiskRed,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = connectionState.message,
                                color = HighRiskRed,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun PersonnelHomeScreen(navController: NavController) {
    val personnelNavItems = listOf(
        GlassNavItem(Screen.Home.route, "Home", Icons.Default.Home),
        GlassNavItem(Screen.DailyWellness.route, "Check-in", Icons.Default.CheckCircle),
        GlassNavItem(Screen.WellnessRiskResult.route, "Analytics", Icons.Default.Info),
        GlassNavItem(Screen.Support.route, "Support", Icons.Default.Phone),
        GlassNavItem(Screen.Profile.route, "Profile", Icons.Default.Person)
    )

    LiquidBackground {
        Scaffold(
            topBar = {
                GlassTopBar(
                    title = "Personnel Dashboard",
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = ElectricCyan)
                        }
                    }
                )
            },
            bottomBar = {
                GlassBottomNavigation(
                    items = personnelNavItems,
                    selectedRoute = Screen.Home.route,
                    onItemSelect = { route ->
                        if (route != Screen.Home.route) {
                            navController.navigate(route)
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Greeting Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0x3300E5FF)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Welcome Back, Officer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Keep track of your health, workload, and duty stress levels.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                SectionHeader(title = "Today's Wellness Check")

                // Main Interactive Risk Card Preview
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.navigate(Screen.WellnessRiskResult.route) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Wellness Prediction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Last evaluation: Low Stress Index", style = MaterialTheme.typography.bodySmall, color = LowRiskGreen)
                        }
                        Surface(
                            color = LowRiskBg,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Optimal ✅", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = LowRiskGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                SectionHeader(title = "Quick Actions")

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeActionRow("Daily Check-in", Icons.Default.CheckCircle, "Log mood & sleep") {
                        navController.navigate(Screen.DailyWellness.route)
                    }
                    HomeActionRow("Stress Assessment", Icons.Default.Warning, "Take stress scale test") {
                        navController.navigate(Screen.StressAssessment.route)
                    }
                    HomeActionRow("Workload/Duty Entry", Icons.Default.Build, "Log duty hours & types") {
                        navController.navigate(Screen.WorkloadEntry.route)
                    }
                    HomeActionRow("AI Risk Analytics", Icons.Default.Info, "View risk predictions") {
                        navController.navigate(Screen.WellnessRiskResult.route)
                    }
                    HomeActionRow("Wellness History", Icons.Default.DateRange, "Track previous logs") {
                        navController.navigate(Screen.WellnessHistory.route)
                    }
                    HomeActionRow("Support & Counselling", Icons.Default.Phone, "Get immediate help") {
                        navController.navigate(Screen.Support.route)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HomeActionRow(title: String, icon: ImageVector, subtitle: String, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = 16.dp,
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ElectricCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = ElectricCyan)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = CoolBlueLight)
        }
    }
}

@Composable
fun DailyWellnessScreen(
    navController: NavController,
    viewModel: WellnessViewModel? = null
) {
    val checkInStatus = viewModel?.checkInStatus?.collectAsStateWithLifecycle()?.value
    val hasSubmittedToday = viewModel?.hasSubmittedToday?.collectAsStateWithLifecycle()?.value ?: false

    LaunchedEffect(Unit) {
        viewModel?.loadHistory()
    }

    DailyWellnessScreenContent(
        checkInStatus = checkInStatus,
        hasSubmittedToday = hasSubmittedToday,
        onSubmitCheckIn = { mood, sleepHours, stressLevel ->
            viewModel?.submitDailyCheckIn(mood, sleepHours, stressLevel)
        },
        onResetCheckInStatus = { viewModel?.resetCheckInStatus() },
        onPopBackStack = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyWellnessScreenContent(
    checkInStatus: ApiResult<Unit>? = null,
    hasSubmittedToday: Boolean = false,
    onSubmitCheckIn: (String, Float, Int) -> Unit = { _, _, _ -> },
    onResetCheckInStatus: () -> Unit = {},
    onPopBackStack: () -> Unit = {}
) {
    var mood by remember { mutableStateOf("Happy") }
    var sleepHours by remember { mutableFloatStateOf(7.5f) }
    var stressLevel by remember { mutableFloatStateOf(3f) }
    val context = LocalContext.current

    LaunchedEffect(checkInStatus) {
        if (checkInStatus is ApiResult.Success) {
            Toast.makeText(context, "Check-in submitted successfully!", Toast.LENGTH_SHORT).show()
            onResetCheckInStatus()
            onPopBackStack()
        } else if (checkInStatus is ApiResult.Error) {
            Toast.makeText(context, checkInStatus.message, Toast.LENGTH_LONG).show()
            onResetCheckInStatus()
        }
    }

    LiquidBackground {
        Scaffold(
            topBar = { GlassTopBar(title = "Daily Wellness Check-in", onBackClick = onPopBackStack) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (hasSubmittedToday) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0x2B10B981)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = LowRiskGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Today's wellness check-in has already been submitted.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = LowRiskGreen
                            )
                        }
                    }
                }

                // Step Indicator Header
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Step 1 of 3: Mood & Vibe", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ElectricCyan)
                        Text("Daily Tracker", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Mood Selection
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("How are you feeling today?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    val moods = listOf(
                        Triple("Happy", "😊", "Positive"),
                        Triple("Neutral", "😐", "Balanced"),
                        Triple("Tired", "🥱", "Fatigued"),
                        Triple("Anxious", "😟", "Uneasy"),
                        Triple("Stressed", "😫", "Overwhelmed")
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        moods.forEach { (item, emoji, _) ->
                            val selected = mood == item
                            FilterChip(
                                selected = selected,
                                onClick = { if (!hasSubmittedToday) mood = item },
                                label = { Text("$emoji $item") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan,
                                    selectedLabelColor = NavyBackgroundDark
                                ),
                                enabled = !hasSubmittedToday
                            )
                        }
                    }
                }

                // Sleep Duration
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sleep Duration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${"%.1f".format(sleepHours)} Hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CoolBlueLight)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = sleepHours,
                        onValueChange = { sleepHours = it },
                        valueRange = 3f..12f,
                        steps = 17,
                        enabled = !hasSubmittedToday,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                    )
                }

                // Perceived Stress Level
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Perceived Stress Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${stressLevel.toInt()}/10", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricCyan)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = stressLevel,
                        onValueChange = { stressLevel = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        enabled = !hasSubmittedToday,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                    )
                }

                if (hasSubmittedToday) {
                    GlassOutlinedButton(
                        text = "Already Submitted Today ✅",
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.CheckCircle
                    )
                } else {
                    GlassButton(
                        text = "Submit Check-in",
                        onClick = { onSubmitCheckIn(mood, sleepHours, stressLevel.toInt()) },
                        isLoading = checkInStatus is ApiResult.Loading,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.Check
                    )
                }
            }
        }
    }
}

@Composable
fun StressAssessmentScreen(
    navController: NavController,
    viewModel: WellnessViewModel? = null
) {
    StressAssessmentScreenContent(
        onSubmitAssessment = { scores ->
            viewModel?.submitStressAssessment(scores)
            navController.navigate(Screen.WellnessRiskResult.route)
        },
        onPopBackStack = { navController.popBackStack() }
    )
}

@Composable
fun StressAssessmentScreenContent(
    onSubmitAssessment: (List<Int>) -> Unit = {},
    onPopBackStack: () -> Unit = {}
) {
    var q1 by remember { mutableFloatStateOf(2f) }
    var q2 by remember { mutableFloatStateOf(2f) }
    var q3 by remember { mutableFloatStateOf(2f) }

    LiquidBackground {
        Scaffold(
            topBar = { GlassTopBar(title = "Stress Self-Assessment", onBackClick = onPopBackStack) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Answer questions below (0 = Never, 4 = Very Often)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Question 1 of 3", style = MaterialTheme.typography.labelSmall, color = ElectricCyan, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("In the last month, how often have you felt unable to control important things?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(value = q1, onValueChange = { q1 = it }, valueRange = 0f..4f, steps = 3, colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan))
                    Text("Rating: ${q1.toInt()}/4", style = MaterialTheme.typography.bodySmall, color = CoolBlueLight)
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Question 2 of 3", style = MaterialTheme.typography.labelSmall, color = ElectricCyan, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("In the last month, how often have you felt confident about handling personal problems?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(value = q2, onValueChange = { q2 = it }, valueRange = 0f..4f, steps = 3, colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan))
                    Text("Rating: ${q2.toInt()}/4", style = MaterialTheme.typography.bodySmall, color = CoolBlueLight)
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Question 3 of 3", style = MaterialTheme.typography.labelSmall, color = ElectricCyan, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("In the last month, how often have you felt difficulties piling up so high you could not overcome them?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(value = q3, onValueChange = { q3 = it }, valueRange = 0f..4f, steps = 3, colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan))
                    Text("Rating: ${q3.toInt()}/4", style = MaterialTheme.typography.bodySmall, color = CoolBlueLight)
                }

                GlassButton(
                    text = "Calculate Stress Index & Risk",
                    onClick = { onSubmitAssessment(listOf(q1.toInt(), q2.toInt(), q3.toInt())) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Info
                )
            }
        }
    }
}

@Composable
fun WorkloadEntryScreen(
    navController: NavController,
    viewModel: WellnessViewModel? = null
) {
    WorkloadEntryScreenContent(
        onSubmitWorkload = { hours, dutyType ->
            viewModel?.submitWorkloadEntry(hours, dutyType)
            navController.navigate(Screen.WellnessRiskResult.route)
        },
        onPopBackStack = { navController.popBackStack() }
    )
}

@Composable
fun WorkloadEntryScreenContent(
    onSubmitWorkload: (Float, String) -> Unit = { _, _ -> },
    onPopBackStack: () -> Unit = {}
) {
    var hours by remember { mutableStateOf("") }
    var dutyType by remember { mutableStateOf("Day Shift") }

    LiquidBackground {
        Scaffold(
            topBar = { GlassTopBar(title = "Workload & Duty Entry", onBackClick = onPopBackStack) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    GlassTextField(
                        value = hours,
                        onValueChange = { hours = it },
                        label = "Duty Duration (Hours)",
                        leadingIcon = Icons.Default.Build,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Duty Type / Shift:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val shifts = listOf("Day Shift", "Night Shift", "On-Call / Standby", "Overtime")
                    shifts.forEach { shift ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { dutyType = shift }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = dutyType == shift,
                                onClick = { dutyType = shift },
                                colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(shift, style = MaterialTheme.typography.bodyMedium, fontWeight = if (dutyType == shift) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                GlassButton(
                    text = "Log Workload & Predict Risk",
                    onClick = {
                        val hrs = hours.toFloatOrNull() ?: 8f
                        onSubmitWorkload(hrs, dutyType)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.CheckCircle
                )
            }
        }
    }
}

@Composable
fun WellnessRiskResultScreen(
    navController: NavController,
    viewModel: WellnessViewModel? = null
) {
    val riskResult = viewModel?.latestRisk?.collectAsStateWithLifecycle()?.value

    WellnessRiskResultScreenContent(
        riskResult = riskResult,
        onRetry = { viewModel?.fetchRiskPrediction() },
        onNavigateHome = {
            navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } }
        },
        onPopBackStack = { navController.popBackStack() }
    )
}

@Composable
fun WellnessRiskResultScreenContent(
    riskResult: ApiResult<WellnessRisk>? = null,
    onRetry: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onPopBackStack: () -> Unit = {}
) {
    LiquidBackground {
        Scaffold(
            topBar = { GlassTopBar(title = "AI Wellness Risk Analytics", onBackClick = onPopBackStack) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (riskResult) {
                    is ApiResult.Loading -> LoadingCard(height = 200.dp)
                    is ApiResult.Success -> {
                        val risk = riskResult.data
                        RiskCard(
                            score = risk.score,
                            riskLevel = risk.riskLevel,
                            recommendation = risk.recommendation
                        )

                        if (risk.topContributingFactors.isNotEmpty()) {
                            SectionHeader(title = "Top Contributing Factors")
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                risk.topContributingFactors.forEach { factor ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = factor.displayName.ifBlank { factor.feature },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Impact: ${"%.2f".format(factor.impactScore)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CoolBlueLight
                                        )
                                    }
                                    if (factor.description.isNotBlank()) {
                                        Text(
                                            text = factor.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color.Gray.copy(alpha = 0.2f))
                                }
                            }
                        }

                        if (risk.welfareRecommendations.isNotEmpty()) {
                            SectionHeader(title = "Welfare Recommendations")
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                risk.welfareRecommendations.forEach { recText ->
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = recText, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is ApiResult.Error -> {
                        ErrorStateCard(message = riskResult.message, onRetry = onRetry)
                    }
                    else -> {
                        EmptyStateCard(
                            title = "No Risk Data Available",
                            message = "Submit a daily check-in or workload log to generate your AI risk prediction."
                        )
                    }
                }

                GlassButton(
                    text = "Back to Dashboard",
                    onClick = onNavigateHome,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun WellnessHistoryScreen(
    navController: NavController,
    viewModel: WellnessViewModel? = null
) {
    val historyState = viewModel?.historyState?.collectAsStateWithLifecycle()?.value ?: ApiResult.Loading

    LaunchedEffect(Unit) {
        viewModel?.loadHistory()
    }

    WellnessHistoryScreenContent(
        historyState = historyState,
        onRetry = { viewModel?.loadHistory() },
        onPopBackStack = { navController.popBackStack() }
    )
}

@Composable
fun WellnessHistoryScreenContent(
    historyState: ApiResult<List<WellnessRecord>> = ApiResult.Loading,
    onRetry: () -> Unit = {},
    onPopBackStack: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("7 Days") }

    LiquidBackground {
        Scaffold(
            topBar = { GlassTopBar(title = "Wellness History Logs", onBackClick = onPopBackStack) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("7 Days", "30 Days", "All Logs").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = NavyBackgroundDark
                            )
                        )
                    }
                }

                when (historyState) {
                    is ApiResult.Loading -> LoadingCard(height = 180.dp)
                    is ApiResult.Error -> ErrorStateCard(message = historyState.message, onRetry = onRetry)
                    is ApiResult.Success -> {
                        val historyList = historyState.data
                        if (historyList.isEmpty()) {
                            EmptyStateCard(
                                title = "No History Logs",
                                message = "Your check-in logs and stress assessment history will appear here."
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(historyList) { record ->
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(record.date, fontWeight = FontWeight.Bold, color = ElectricCyan)
                                            Surface(
                                                color = SoftCyanGlow,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = record.mood,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = CoolBlueLight
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Sleep: ${record.sleepHours} hrs", style = MaterialTheme.typography.bodyMedium)
                                            Text("Stress Score: ${record.stressLevel}/10", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                        if (!record.notes.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Notes: ${record.notes}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
fun SupportScreen(navController: NavController) {
    LiquidBackground {
        Scaffold(
            topBar = { GlassTopBar(title = "Support & Counselling", onBackClick = { navController.popBackStack() }) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0x330288D1)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CoolBlueLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = CoolBlueLight, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("24/7 Helpline Support", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Reach out anytime. Your identity remains strictly confidential.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                SectionHeader(title = "Available Support Channels")

                GlassCard(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Peer Support Counselors", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Available via private voice call or encrypted chat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBox, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mental Health Professionals", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Book a scheduled telehealth consultation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("De-escalation Toolkit", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Guided breathing exercises & relaxation guides", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel? = null
) {
    ProfileScreenContent(
        onLogout = {
            authViewModel?.logout()
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        },
        onPopBackStack = { navController.popBackStack() }
    )
}

@Composable
fun ProfileScreenContent(
    onLogout: () -> Unit = {},
    onPopBackStack: () -> Unit = {}
) {
    LiquidBackground {
        Scaffold(
            topBar = { GlassTopBar(title = "Personnel Profile", onBackClick = onPopBackStack) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(64.dp),
                                tint = ElectricCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Officer Code: PW-4829", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Department: Emergency Response Services", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Account & Privacy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Data Sharing Consent")
                        Text("Active", color = LowRiskGreen, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Encryption Protocol")
                        Text("AES-256", color = CoolBlueLight)
                    }
                }

                GlassButton(
                    text = "Logout",
                    onClick = onLogout,
                    containerColor = HighRiskRed,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.ExitToApp
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "Splash Screen")
@Composable
fun SplashScreenPreview() {
    PersonnelWellnessTheme(dynamicColor = false) {
        SplashScreenContent()
    }
}

@Preview(showBackground = true, name = "Privacy Consent Screen")
@Composable
fun PrivacyConsentScreenPreview() {
    PersonnelWellnessTheme(dynamicColor = false) {
        PrivacyConsentScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true, name = "Login Screen")
@Composable
fun LoginScreenPreview() {
    PersonnelWellnessTheme(dynamicColor = false) {
        LoginScreenContent()
    }
}

@Preview(showBackground = true, name = "Personnel Home Screen")
@Composable
fun PersonnelHomeScreenPreview() {
    PersonnelWellnessTheme(dynamicColor = false) {
        PersonnelHomeScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true, name = "Daily Wellness Screen")
@Composable
fun DailyWellnessScreenPreview() {
    PersonnelWellnessTheme(dynamicColor = false) {
        DailyWellnessScreenContent()
    }
}

@Preview(showBackground = true, name = "Stress Assessment Screen")
@Composable
fun StressAssessmentScreenPreview() {
    PersonnelWellnessTheme(dynamicColor = false) {
        StressAssessmentScreenContent()
    }
}

@Preview(showBackground = true, name = "Workload Entry Screen")
@Composable
fun WorkloadEntryScreenPreview() {
    PersonnelWellnessTheme(dynamicColor = false) {
        WorkloadEntryScreenContent()
    }
}

@Preview(showBackground = true, name = "Wellness Risk Result Screen")
@Composable
fun WellnessRiskResultScreenPreview() {
    PersonnelWellnessTheme(dynamicColor = false) {
        WellnessRiskResultScreenContent(
            riskResult = ApiResult.Success(
                WellnessRisk(
                    riskLevel = "Moderate Risk",
                    score = 65,
                    recommendation = "Consider scheduling rest breaks between consecutive shifts."
                )
            )
        )
    }
}

@Preview(showBackground = true, name = "Wellness History Screen")
@Composable
fun WellnessHistoryScreenPreview() {
    PersonnelWellnessTheme(dynamicColor = false) {
        WellnessHistoryScreenContent(
            historyState = ApiResult.Success(
                listOf(
                    WellnessRecord("1", "2026-09-27", "Happy", 7.5f, 3, "Synthetic demo check-in"),
                    WellnessRecord("2", "2026-09-26", "Tired", 6.0f, 5)
                )
            )
        )
    }
}

@Preview(showBackground = true, name = "Support & Counselling Screen")
@Composable
fun SupportScreenPreview() {
    PersonnelWellnessTheme(dynamicColor = false) {
        SupportScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true, name = "Profile Screen")
@Composable
fun ProfileScreenPreview() {
    PersonnelWellnessTheme(dynamicColor = false) {
        ProfileScreenContent()
    }
}
