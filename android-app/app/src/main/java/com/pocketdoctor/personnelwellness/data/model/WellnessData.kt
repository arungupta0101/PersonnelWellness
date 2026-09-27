package com.pocketdoctor.personnelwellness.data.model

import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun mapMoodToScore(mood: String): Int {
    return when (mood.lowercase().trim()) {
        "happy", "positive" -> 8
        "neutral", "balanced" -> 6
        "tired", "fatigued" -> 4
        "anxious", "uneasy" -> 3
        "stressed", "overwhelmed" -> 2
        else -> mood.toIntOrNull() ?: 5
    }
}

fun mapScoreToMood(score: Int): String {
    return when (score) {
        in 8..10 -> "Happy"
        in 6..7 -> "Neutral"
        in 4..5 -> "Tired"
        3 -> "Anxious"
        in 1..2 -> "Stressed"
        else -> "Mood $score"
    }
}

fun formatIsoDate(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "Today"
    return try {
        isoString.take(10)
    } catch (_: Exception) {
        isoString
    }
}

fun parseDetailMessage(jsonString: String?): String? {
    if (jsonString.isNullOrBlank()) return null
    return try {
        val jsonObj = JsonParser.parseString(jsonString).asJsonObject
        if (jsonObj.has("detail")) {
            val detailElem = jsonObj.get("detail")
            if (detailElem.isJsonPrimitive) detailElem.asString else null
        } else null
    } catch (_: Exception) {
        null
    }
}

fun isTodayDate(dateString: String?): Boolean {
    if (dateString.isNullOrBlank()) return false
    return try {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        dateString.startsWith(today)
    } catch (_: Exception) {
        false
    }
}

data class TodayCheckInStatus(
    @SerializedName("has_submitted") val hasSubmitted: Boolean = false,
    @SerializedName("checkin") val checkinResponse: WellnessCheckinResponse? = null
) {
    val record: WellnessRecord?
        get() = checkinResponse?.toWellnessRecord()
}

data class FeatureContribution(
    val feature: String = "",
    @SerializedName("display_name") val displayName: String = "",
    val value: Float = 0f,
    @SerializedName("impact_score") val impactScore: Float = 0f,
    val description: String = ""
)

data class WellnessRisk(
    @SerializedName("prediction_id") val predictionId: Int? = null,
    @SerializedName("risk_level") val riskLevel: String = "LOW",
    @SerializedName("risk_score") val rawScore: Double? = null,
    @SerializedName("confidence") val confidence: Float? = null,
    @SerializedName("top_contributing_factors") val topContributingFactors: List<FeatureContribution> = emptyList(),
    @SerializedName("welfare_recommendations") val welfareRecommendations: List<String> = emptyList(),
    @SerializedName("model_version") val modelVersion: String? = null,
    @SerializedName("is_medical_diagnosis") val isMedicalDiagnosis: Boolean = false,
    val disclaimer: String = "This system is a welfare-risk screening prototype for operational decision support. It is NOT a medical diagnosis tool.",
    @SerializedName("score") val explicitScore: Int? = null,
    @SerializedName("recommendation") val explicitRecommendation: String? = null
) {
    val score: Int
        get() = explicitScore ?: ((rawScore ?: 0.15) * 100).toInt()

    val recommendation: String
        get() = explicitRecommendation ?: (welfareRecommendations.firstOrNull() ?: "Maintain optimal duty-rest balance.")

    constructor(
        riskLevel: String,
        score: Int,
        recommendation: String
    ) : this(
        riskLevel = riskLevel,
        rawScore = score / 100.0,
        explicitScore = score,
        welfareRecommendations = listOf(recommendation),
        explicitRecommendation = recommendation
    )
}

data class WellnessCheckinResponse(
    val id: Int,
    val mood: Int,
    @SerializedName("stress_level") val stressLevel: Int,
    @SerializedName("sleep_hours") val sleepHours: Float,
    val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) {
    fun toWellnessRecord(): WellnessRecord {
        return WellnessRecord(
            id = id.toString(),
            date = formatIsoDate(createdAt),
            mood = mapScoreToMood(mood),
            sleepHours = sleepHours,
            stressLevel = stressLevel,
            notes = notes
        )
    }
}

data class WellnessRecord(
    val id: String,
    val date: String,
    val mood: String,
    @SerializedName("sleep_hours") val sleepHours: Float,
    @SerializedName("stress_level") val stressLevel: Int,
    val notes: String? = null
)

data class CheckInRequest(
    val mood: Int,
    @SerializedName("sleep_hours") val sleepHours: Float,
    @SerializedName("stress_level") val stressLevel: Int,
    val notes: String? = null
)

data class PredictRequest(
    @SerializedName("duty_hours_day") val dutyHoursDay: Float? = 8.0f,
    @SerializedName("duty_hours") val dutyHours: Float? = null,
    @SerializedName("weekly_duty_hours") val weeklyDutyHours: Float = 40.0f,
    @SerializedName("deployment_days") val deploymentDays: Int = 0,
    @SerializedName("night_shifts") val nightShifts: Int = 0,
    @SerializedName("workload_score") val workloadScore: Float = 5.0f,
    @SerializedName("training_load") val trainingLoad: Float = 5.0f,
    @SerializedName("transfer_frequency") val transferFrequency: Int = 0,
    @SerializedName("sleep_hours") val sleepHours: Float = 7.0f,
    @SerializedName("leave_days") val leaveDays: Int = 10,
    @SerializedName("rest_days") val restDays: Int = 4,
    @SerializedName("self_reported_stress") val selfReportedStress: Float? = 5.0f,
    @SerializedName("stress_score") val stressScore: Float? = null,
    @SerializedName("fatigue_score") val fatigueScore: Float = 5.0f,
    @SerializedName("mood_score") val moodScore: Float = 5.0f,
    @SerializedName("social_support_score") val socialSupportScore: Float = 5.0f,
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("assessment_id") val assessmentId: Int? = null
)

sealed class ApiResult<out T> {
    object Loading : ApiResult<Nothing>()
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}
