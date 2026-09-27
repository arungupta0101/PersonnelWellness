package com.pocketdoctor.personnelwellness.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pocketdoctor.personnelwellness.data.model.ApiResult
import com.pocketdoctor.personnelwellness.data.model.PersonnelWelfareDetail
import com.pocketdoctor.personnelwellness.data.model.WelfareAlert
import com.pocketdoctor.personnelwellness.data.model.WelfareStats
import com.pocketdoctor.personnelwellness.ui.components.*
import com.pocketdoctor.personnelwellness.ui.navigation.Screen
import com.pocketdoctor.personnelwellness.ui.theme.*
import com.pocketdoctor.personnelwellness.ui.viewmodel.AuthViewModel
import com.pocketdoctor.personnelwellness.ui.viewmodel.WelfareViewModel

@Composable
fun WelfareOfficerDashboard(
    navController: NavController,
    authViewModel: AuthViewModel,
    welfareViewModel: WelfareViewModel
) {
    val statsState by welfareViewModel.statsState.collectAsStateWithLifecycle()
    val alertsState by welfareViewModel.alertsState.collectAsStateWithLifecycle()

    var viewingPersonnelId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        welfareViewModel.loadDashboardData()
    }

    val welfareNavItems = listOf(
        GlassNavItem(Screen.WelfareDashboard.route, "Dashboard", Icons.Default.Home),
        GlassNavItem(Screen.WelfareDashboard.route, "Alerts", Icons.Default.Warning),
        GlassNavItem(Screen.Profile.route, "Profile", Icons.Default.AccountCircle)
    )

    LiquidBackground {
        Scaffold(
            topBar = {
                GlassTopBar(
                    title = "Welfare Officer Portal",
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = ElectricCyan)
                        }
                    }
                )
            },
            bottomBar = {
                GlassBottomNavigation(
                    items = welfareNavItems,
                    selectedRoute = Screen.WelfareDashboard.route,
                    onItemSelect = { route ->
                        if (route != Screen.WelfareDashboard.route) {
                            navController.navigate(route)
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (viewingPersonnelId != null) {
                    PersonnelDetailSubView(
                        personnelId = viewingPersonnelId!!,
                        welfareViewModel = welfareViewModel,
                        onBack = {
                            viewingPersonnelId = null
                            welfareViewModel.clearPersonnelDetail()
                            welfareViewModel.loadDashboardData()
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SectionHeader(
                            title = "Operational Health Command",
                            actionText = "Refresh",
                            onActionClick = { welfareViewModel.loadDashboardData() }
                        )

                        // Stats Grid View
                        when (statsState) {
                            is ApiResult.Loading -> LoadingCard(height = 140.dp)
                            is ApiResult.Error -> ErrorStateCard(
                                message = "Failed to load risk metrics: ${(statsState as ApiResult.Error).message}",
                                onRetry = { welfareViewModel.loadDashboardData() }
                            )
                            is ApiResult.Success -> {
                                val stats = (statsState as ApiResult.Success).data
                                WelfareStatsGrid(stats)
                            }
                        }

                        SectionHeader(title = "Priority Risk Alerts Queue")

                        // Alerts Queue View
                        when (alertsState) {
                            is ApiResult.Loading -> LoadingCard(height = 160.dp)
                            is ApiResult.Error -> ErrorStateCard(
                                message = "Error loading risk alerts queue.",
                                onRetry = { welfareViewModel.loadDashboardData() }
                            )
                            is ApiResult.Success -> {
                                val alerts = (alertsState as ApiResult.Success).data
                                if (alerts.isEmpty()) {
                                    EmptyStateCard(
                                        title = "No High Risk Alerts",
                                        message = "All monitored personnel are currently operating within safe wellness indicators."
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        alerts.forEach { alert ->
                                            AlertRow(alert = alert, onClick = {
                                                viewingPersonnelId = alert.personnelId
                                                welfareViewModel.loadPersonnelDetail(alert.personnelId)
                                            })
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        GlassButton(
                            text = "Session Logout",
                            onClick = {
                                authViewModel.logout()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            containerColor = HighRiskRed,
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.ExitToApp
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WelfareStatsGrid(stats: WelfareStats) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Monitored Force Strength", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${stats.totalPersonnel} Personnel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ElectricCyan)
            }
            Surface(
                color = SoftCyanGlow,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Active Scan", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.2f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Low Risk", style = MaterialTheme.typography.labelSmall, color = LowRiskGreen)
                Text("${stats.lowRiskCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LowRiskGreen)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Moderate", style = MaterialTheme.typography.labelSmall, color = ModerateRiskYellow)
                Text("${stats.moderateRiskCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ModerateRiskYellow)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("High Risk", style = MaterialTheme.typography.labelSmall, color = HighRiskRed)
                Text("${stats.highRiskCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HighRiskRed)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Interventions", style = MaterialTheme.typography.labelSmall, color = CoolBlueLight)
                Text("${stats.interventionActiveCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CoolBlueLight)
            }
        }
    }
}

@Composable
fun AlertRow(alert: WelfareAlert, onClick: () -> Unit) {
    val riskColor = when (alert.riskLevel.uppercase()) {
        "HIGH", "HIGH RISK" -> HighRiskRed
        "MODERATE", "MODERATE RISK" -> ModerateRiskYellow
        else -> LowRiskGreen
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(riskColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Alert", tint = riskColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = alert.personnelName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = "Trigger: ${alert.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                color = riskColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = alert.riskLevel, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = riskColor)
            }
        }
    }
}

@Composable
fun PersonnelDetailSubView(
    personnelId: String,
    welfareViewModel: WelfareViewModel,
    onBack: () -> Unit
) {
    val detailState by welfareViewModel.detailState.collectAsStateWithLifecycle()
    val actionState by welfareViewModel.actionState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var notesText by remember { mutableStateOf("") }
    var showingInterventionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(actionState) {
        if (actionState is ApiResult.Success) {
            Toast.makeText(context, "Welfare action updated successfully", Toast.LENGTH_SHORT).show()
            welfareViewModel.resetActionState()
        } else if (actionState is ApiResult.Error) {
            Toast.makeText(context, (actionState as ApiResult.Error).message, Toast.LENGTH_LONG).show()
            welfareViewModel.resetActionState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return", tint = ElectricCyan)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Least-Privilege Confidential Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        when (detailState) {
            is ApiResult.Loading -> LoadingCard(height = 200.dp)
            is ApiResult.Error -> ErrorStateCard(
                message = "Access unauthorized or profile non-existent: ${(detailState as ApiResult.Error).message}",
                onRetry = { welfareViewModel.loadPersonnelDetail(personnelId) }
            )
            is ApiResult.Success -> {
                val detail = (detailState as ApiResult.Success).data

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Anonymous ID Hash: ${detail.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Operational Label: ${detail.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Duty Department: ${detail.department}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Risk Tier: ${detail.currentRiskLevel}", fontWeight = FontWeight.Bold, color = if (detail.currentRiskLevel == "High") HighRiskRed else ElectricCyan)
                        Text("Status: ${detail.interventionStatus}", fontWeight = FontWeight.SemiBold, color = CoolBlueLight)
                    }
                }

                SectionHeader(title = "Normalized Trend Indicators")

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Workload Vector: ${detail.workloadTrend.joinToString(" hrs, ")} hrs", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Fatigue Vector: ${detail.fatigueTrend.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sleep Duration Vector: ${detail.sleepTrend.joinToString("h, ")}h", style = MaterialTheme.typography.bodyMedium)
                }

                SectionHeader(title = "Authorized Welfare Actions")

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(
                        text = "Request Welfare Check-in",
                        onClick = { welfareViewModel.requestCheckIn(detail.id) },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.Info
                    )

                    GlassOutlinedButton(
                        text = "Trigger Support/Counselling Referral",
                        onClick = { welfareViewModel.referToSupport(detail.id) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    GlassOutlinedButton(
                        text = "Trigger Workload Review",
                        onClick = { welfareViewModel.triggerWorkloadReview(detail.id) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    GlassButton(
                        text = "Update Intervention Status",
                        onClick = { showingInterventionDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = CoolBlue
                    )
                }

                if (showingInterventionDialog) {
                    AlertDialog(
                        onDismissRequest = { showingInterventionDialog = false },
                        title = { Text("Update Action Plan", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Assign structured action flag or notes for review.", style = MaterialTheme.typography.bodyMedium)
                                GlassTextField(
                                    value = notesText,
                                    onValueChange = { notesText = it },
                                    label = "Authorization / Action Notes"
                                )
                            }
                        },
                        confirmButton = {
                            GlassButton(
                                text = "Apply Status",
                                onClick = {
                                    welfareViewModel.updateIntervention(detail.id, "ACTIVE_MANAGED", notesText)
                                    showingInterventionDialog = false
                                }
                            )
                        },
                        dismissButton = {
                            TextButton(onClick = { showingInterventionDialog = false }) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
fun CommanderDashboard(navController: NavController, authViewModel: AuthViewModel) {
    LiquidBackground {
        Scaffold(
            topBar = {
                GlassTopBar(
                    title = "Commander Portal",
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = ElectricCyan)
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
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Unit Aggregated Insights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Privacy Protection active. Individual medical logs are obscured under Privacy Regulations. Displays unit status vectors only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                MetricCard(title = "Total Operational Strength", value = "142 Officers", icon = Icons.Default.Person)
                MetricCard(title = "Unit Fatigue Factor", value = "18% (Low)", icon = Icons.Default.Info, accentColor = LowRiskGreen)
                MetricCard(title = "Aggregated Workload Index", value = "42.4 (Normal)", icon = Icons.Default.Home, accentColor = CoolBlueLight)

                GlassButton(
                    text = "Logout",
                    onClick = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    containerColor = HighRiskRed,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.ExitToApp
                )
            }
        }
    }
}

@Composable
fun AdminDashboard(navController: NavController, authViewModel: AuthViewModel) {
    DashboardBase("Admin Portal", navController, authViewModel) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("System Management & User Access Control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Manage role RBAC permissions, audit log tracking, and API token policies.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DashboardBase(
    title: String,
    navController: NavController,
    authViewModel: AuthViewModel,
    content: @Composable ColumnScope.() -> Unit
) {
    LiquidBackground {
        Scaffold(
            topBar = {
                GlassTopBar(
                    title = title,
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = ElectricCyan)
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
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                content()

                GlassButton(
                    text = "Logout",
                    onClick = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    containerColor = HighRiskRed,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.ExitToApp
                )
            }
        }
    }
}
