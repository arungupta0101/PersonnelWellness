package com.pocketdoctor.personnelwellness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pocketdoctor.personnelwellness.data.api.RetrofitClient
import com.pocketdoctor.personnelwellness.data.local.TokenManager
import com.pocketdoctor.personnelwellness.data.repository.AppAuthRepository
import com.pocketdoctor.personnelwellness.data.repository.AppConsentRepository
import com.pocketdoctor.personnelwellness.data.repository.AppWelfareRepository
import com.pocketdoctor.personnelwellness.data.repository.AppWellnessRepository
import com.pocketdoctor.personnelwellness.ui.navigation.Screen
import com.pocketdoctor.personnelwellness.ui.screen.*
import com.pocketdoctor.personnelwellness.ui.theme.PersonnelWellnessTheme
import com.pocketdoctor.personnelwellness.ui.viewmodel.AuthViewModel
import com.pocketdoctor.personnelwellness.ui.viewmodel.AuthViewModelFactory
import com.pocketdoctor.personnelwellness.ui.viewmodel.ConsentViewModel
import com.pocketdoctor.personnelwellness.ui.viewmodel.ConsentViewModelFactory
import com.pocketdoctor.personnelwellness.ui.viewmodel.WelfareViewModel
import com.pocketdoctor.personnelwellness.ui.viewmodel.WelfareViewModelFactory
import com.pocketdoctor.personnelwellness.ui.viewmodel.WellnessViewModel
import com.pocketdoctor.personnelwellness.ui.viewmodel.WellnessViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonnelWellnessTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PersonnelWellnessApp()
                }
            }
        }
    }
}

@Composable
fun PersonnelWellnessApp() {
    val context = LocalContext.current
    
    // Initialize TokenManager and pass it to RetrofitClient
    val tokenManager = remember { TokenManager(context.applicationContext) }
    remember { 
        RetrofitClient.init(tokenManager)
        true
    }

    val navController = rememberNavController()

    // Auth Repository & ViewModel Setup
    val authRepository = remember { AppAuthRepository(RetrofitClient.authApiService, tokenManager) }
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository)
    )

    // Wellness Repository & ViewModel Setup
    val wellnessRepository = remember { AppWellnessRepository { RetrofitClient.wellnessApiService } }
    val wellnessViewModel: WellnessViewModel = viewModel(
        factory = WellnessViewModelFactory(wellnessRepository)
    )

    // Welfare Repository & ViewModel Setup
    val welfareRepository = remember { AppWelfareRepository(RetrofitClient.welfareApiService) }
    val welfareViewModel: WelfareViewModel = viewModel(
        factory = WelfareViewModelFactory(welfareRepository)
    )

    // Consent Repository & ViewModel Setup
    val consentRepository = remember { AppConsentRepository(RetrofitClient.consentApiService) }
    val consentViewModel: ConsentViewModel = viewModel(
        factory = ConsentViewModelFactory(consentRepository)
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController, authViewModel)
        }
        composable(Screen.PrivacyConsent.route) {
            PrivacyConsentScreen(navController, consentViewModel)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController, authViewModel)
        }
        
        // Role-Based Dashboards
        composable(Screen.Home.route) {
            PersonnelHomeScreen(navController)
        }
        composable(Screen.WelfareDashboard.route) {
            WelfareOfficerDashboard(navController, authViewModel, welfareViewModel)
        }
        composable(Screen.CommanderDashboard.route) {
            CommanderDashboard(navController, authViewModel)
        }
        composable(Screen.AdminDashboard.route) {
            AdminDashboard(navController, authViewModel)
        }

        // Feature screens
        composable(Screen.DailyWellness.route) {
            DailyWellnessScreen(navController, wellnessViewModel)
        }
        composable(Screen.StressAssessment.route) {
            StressAssessmentScreen(navController, wellnessViewModel)
        }
        composable(Screen.WorkloadEntry.route) {
            WorkloadEntryScreen(navController, wellnessViewModel)
        }
        composable(Screen.WellnessRiskResult.route) {
            WellnessRiskResultScreen(navController, wellnessViewModel)
        }
        composable(Screen.WellnessHistory.route) {
            WellnessHistoryScreen(navController, wellnessViewModel)
        }
        composable(Screen.Support.route) {
            SupportScreen(navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController, authViewModel)
        }
    }
}
