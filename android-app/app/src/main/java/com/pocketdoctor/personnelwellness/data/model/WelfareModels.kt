package com.pocketdoctor.personnelwellness.data.model

import com.google.gson.annotations.SerializedName

data class WelfareStats(
    @SerializedName("total_personnel") val totalPersonnel: Int,
    @SerializedName("low_risk_count") val lowRiskCount: Int,
    @SerializedName("moderate_risk_count") val moderateRiskCount: Int,
    @SerializedName("high_risk_count") val highRiskCount: Int,
    @SerializedName("intervention_active_count") val interventionActiveCount: Int
)

data class WelfareAlert(
    val id: String,
    @SerializedName("personnel_id") val personnelId: String,
    @SerializedName("personnel_name") val personnelName: String,
    @SerializedName("risk_level") val riskLevel: String,
    val reason: String,
    val timestamp: String,
    val status: String // PENDING, REVIEWED, RESOLVED
)

data class PersonnelWelfareDetail(
    val id: String,
    val name: String,
    val department: String,
    @SerializedName("current_risk_level") val currentRiskLevel: String,
    @SerializedName("workload_trend") val workloadTrend: List<Float>,
    @SerializedName("fatigue_trend") val fatigueTrend: List<Float>,
    @SerializedName("sleep_trend") val sleepTrend: List<Float>,
    @SerializedName("intervention_status") val interventionStatus: String,
    @SerializedName("last_check_in") val lastCheckIn: String
)

data class InterventionUpdateRequest(
    val status: String,
    val notes: String
)
