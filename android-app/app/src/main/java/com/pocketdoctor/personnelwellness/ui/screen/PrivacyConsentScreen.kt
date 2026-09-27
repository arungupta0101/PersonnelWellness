package com.pocketdoctor.personnelwellness.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
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
import com.pocketdoctor.personnelwellness.data.model.ConsentPreferences
import com.pocketdoctor.personnelwellness.ui.components.*
import com.pocketdoctor.personnelwellness.ui.navigation.Screen
import com.pocketdoctor.personnelwellness.ui.theme.*
import com.pocketdoctor.personnelwellness.ui.viewmodel.ConsentViewModel

@Composable
fun PrivacyConsentScreen(navController: NavController, consentViewModel: ConsentViewModel) {
    val consentState by consentViewModel.consentState.collectAsStateWithLifecycle()
    val updateResult by consentViewModel.updateResult.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var mandatoryChecked by remember { mutableStateOf(false) }
    var biometricsChecked by remember { mutableStateOf(false) }
    var workloadChecked by remember { mutableStateOf(false) }
    var hasLoadedPreferences by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        consentViewModel.fetchConsentStatus()
    }

    LaunchedEffect(consentState) {
        if ((consentState is ApiResult.Success) && !hasLoadedPreferences) {
            val prefs = (consentState as ApiResult.Success).data.preferences
            mandatoryChecked = prefs.mandatoryAccepted
            biometricsChecked = prefs.optionalBiometricsAccepted
            workloadChecked = prefs.optionalWorkloadLogsAccepted
            hasLoadedPreferences = true
        }
    }

    LaunchedEffect(updateResult) {
        if (updateResult is ApiResult.Success) {
            Toast.makeText(context, "Consent preferences updated successfully.", Toast.LENGTH_SHORT).show()
            consentViewModel.resetUpdateResult()
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.PrivacyConsent.route) { inclusive = true }
            }
        } else if (updateResult is ApiResult.Error) {
            Toast.makeText(context, (updateResult as ApiResult.Error).message, Toast.LENGTH_LONG).show()
            consentViewModel.resetUpdateResult()
        }
    }

    LiquidBackground {
        Scaffold(containerColor = Color.Transparent) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (consentState) {
                    is ApiResult.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ElectricCyan)
                    }
                    is ApiResult.Error, is ApiResult.Success -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Privacy",
                                    modifier = Modifier.size(40.dp),
                                    tint = ElectricCyan
                                )
                            }

                            Text(
                                text = "Privacy & Consent Agreement",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            // Notice Glass Banner
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = Color(0x2B0288D1)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Notice",
                                        tint = CoolBlueLight,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "This application provides welfare-risk screening and does not provide medical diagnosis.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CoolBlueLight
                                    )
                                }
                            }

                            // Explanations Glass Card
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("What Data is Collected & Why:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Wellness Data (Mood, Sleep, Stress) to flag fatigue levels.", style = MaterialTheme.typography.bodySmall)
                                Text("• Optional Biometrics & Workload logs for enhanced safety analysis.", style = MaterialTheme.typography.bodySmall)

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.Gray.copy(alpha = 0.2f))

                                Text("Who Can Access Your Data:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Personnel: Full view of your own history logs.", style = MaterialTheme.typography.bodySmall)
                                Text("• Welfare Officer: Minimum necessary risk metrics only.", style = MaterialTheme.typography.bodySmall)
                                Text("• Commander: Aggregated unit-level status maps only.", style = MaterialTheme.typography.bodySmall)
                            }

                            SectionHeader(title = "Consent Controls")

                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = mandatoryChecked,
                                        onCheckedChange = { mandatoryChecked = it },
                                        colors = CheckboxDefaults.colors(checkedColor = ElectricCyan)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("I accept mandatory basic wellness screening (Required)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = biometricsChecked,
                                        onCheckedChange = { biometricsChecked = it },
                                        colors = CheckboxDefaults.colors(checkedColor = ElectricCyan)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Allow optional fitness/biometric sync data", style = MaterialTheme.typography.bodySmall)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = workloadChecked,
                                        onCheckedChange = { workloadChecked = it },
                                        colors = CheckboxDefaults.colors(checkedColor = ElectricCyan)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Allow historical duty workload processing", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            GlassButton(
                                text = "Save Preferences & Continue",
                                onClick = {
                                    val prefs = ConsentPreferences(
                                        mandatoryAccepted = mandatoryChecked,
                                        optionalBiometricsAccepted = biometricsChecked,
                                        optionalWorkloadLogsAccepted = workloadChecked
                                    )
                                    consentViewModel.updateConsent(prefs)
                                },
                                enabled = mandatoryChecked,
                                isLoading = updateResult is ApiResult.Loading,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
