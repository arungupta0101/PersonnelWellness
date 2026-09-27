package com.pocketdoctor.personnelwellness.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object WelfareDashboard : Screen("welfare_dashboard")
    object CommanderDashboard : Screen("commander_dashboard")
    object AdminDashboard : Screen("admin_dashboard")
    object DailyWellness : Screen("daily_wellness")
    object StressAssessment : Screen("stress_assessment")
    object WorkloadEntry : Screen("workload_entry")
    object WellnessRiskResult : Screen("wellness_risk_result")
    object WellnessHistory : Screen("wellness_history")
    object Support : Screen("support")
    object Profile : Screen("profile")
    object PrivacyConsent : Screen("privacy_consent")
}
