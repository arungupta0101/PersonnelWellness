package com.pocketdoctor.personnelwellness.data.model

import com.google.gson.annotations.SerializedName

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
    if (isoString.isNullOrBlank()) return "Recent"
    return try {
        isoString.take(10)
    } catch (_: Exception) {
        isoString
    }
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

data class WellnessRisk(
    @SerializedName("risk_level") val riskLevel: String = "LOW",
    @SerializedName("risk_score") val rawScore: Double? = null,
    @SerializedName("score") val explicitScore: Int? = null,
    @SerializedName("welfare_recommendations") val recommendationsList: List<String> = emptyList(),
    @SerializedName("recommendation") val explicitRecommendation: String? = null
) {
    val score: Int
        get() = explicitScore ?: ((rawScore ?: 0.15) * 100).toInt()

    val recommendation: String
        get() = explicitRecommendation ?: (recommendationsList.firstOrNull() ?: "Maintain optimal duty-rest balance.")

    constructor(
        riskLevel: String,
        score: Int,
        recommendation: String
    ) : this(
        riskLevel = riskLevel,
        rawScore = score / 100.0,
        explicitScore = score,
        recommendationsList = listOf(recommendation),
        explicitRecommendation = recommendation
    )
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
    @SerializedName("duty_hours") val dutyHours: Float = 8f,
    @SerializedName("sleep_hours") val sleepHours: Float = 7f,
    @SerializedName("stress_score") val stressScore: Float = 5f,
    @SerializedName("mood_score") val moodScore: Float = 5f
)

sealed class ApiResult<out T> {
    object Loading : ApiResult<Nothing>()
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}
