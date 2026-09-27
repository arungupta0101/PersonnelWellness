package com.pocketdoctor.personnelwellness.data.model

import com.google.gson.annotations.SerializedName

data class WellnessRisk(
    @SerializedName("risk_level") val riskLevel: String,
    val score: Int,
    val recommendation: String
)

data class WellnessRecord(
    val id: String,
    val date: String,
    val mood: String,
    @SerializedName("sleep_hours") val sleepHours: Float,
    @SerializedName("stress_level") val stressLevel: Int
)

data class CheckInRequest(
    val mood: String,
    @SerializedName("sleep_hours") val sleepHours: Float,
    @SerializedName("stress_level") val stressLevel: Int
)

data class PredictRequest(
    val mood: String,
    @SerializedName("sleep_hours") val sleepHours: Float,
    @SerializedName("stress_level") val stressLevel: Int,
    @SerializedName("workload_hours") val workloadHours: Float,
    @SerializedName("duty_type") val dutyType: String
)

sealed class ApiResult<out T> {
    object Loading : ApiResult<Nothing>()
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}
