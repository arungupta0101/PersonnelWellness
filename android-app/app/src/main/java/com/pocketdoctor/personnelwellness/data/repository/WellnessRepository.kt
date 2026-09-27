package com.pocketdoctor.personnelwellness.data.repository

import com.pocketdoctor.personnelwellness.data.api.WellnessApiService
import com.pocketdoctor.personnelwellness.data.model.CheckInRequest
import com.pocketdoctor.personnelwellness.data.model.PredictRequest
import com.pocketdoctor.personnelwellness.data.model.WellnessRecord
import com.pocketdoctor.personnelwellness.data.model.WellnessRisk
import com.pocketdoctor.personnelwellness.data.model.mapMoodToScore
import com.pocketdoctor.personnelwellness.data.model.parseDetailMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface WellnessRepository {
    fun getWellnessHistory(): Flow<List<WellnessRecord>>
    fun getLatestRiskResult(): Flow<WellnessRisk>
    suspend fun submitDailyCheckIn(mood: String, sleepHours: Float, stressLevel: Int)
    suspend fun submitStressAssessment(answers: List<Int>)
    suspend fun submitWorkloadEntry(hours: Float, dutyType: String)
    
    // Real backend API calls
    suspend fun getWellnessHistoryApi(): Result<List<WellnessRecord>>
    suspend fun submitDailyCheckInApi(mood: String, sleepHours: Float, stressLevel: Int): Result<Unit>
    suspend fun predictRiskApi(mood: String, sleepHours: Float, stressLevel: Int, workloadHours: Float, dutyType: String): Result<WellnessRisk>
}

class AppWellnessRepository(private val apiServiceProvider: () -> WellnessApiService) : WellnessRepository {
    
    private val apiService get() = apiServiceProvider()

    override fun getWellnessHistory(): Flow<List<WellnessRecord>> = flowOf(emptyList())

    override fun getLatestRiskResult(): Flow<WellnessRisk> = flowOf(
        WellnessRisk(riskLevel = "LOW", rawScore = 0.15, welfareRecommendations = listOf("Maintain optimal duty-rest balance."))
    )

    override suspend fun submitDailyCheckIn(mood: String, sleepHours: Float, stressLevel: Int) {}
    override suspend fun submitStressAssessment(answers: List<Int>) {}
    override suspend fun submitWorkloadEntry(hours: Float, dutyType: String) {}

    override suspend fun getWellnessHistoryApi(): Result<List<WellnessRecord>> {
        return try {
            val response = apiService.getWellnessHistory()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val records = body.map { it.toWellnessRecord() }
                Result.success(records)
            } else {
                val errBody = response.errorBody()?.string()
                val serverDetail = parseDetailMessage(errBody)
                Result.failure(Exception(serverDetail ?: "Failed to fetch history (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitDailyCheckInApi(mood: String, sleepHours: Float, stressLevel: Int): Result<Unit> {
        val moodScore = mapMoodToScore(mood)
        val request = CheckInRequest(
            mood = moodScore,
            sleepHours = sleepHours,
            stressLevel = stressLevel,
            notes = null
        )
        return try {
            val response = apiService.submitCheckIn(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else if (response.code() == 409) {
                val errBody = response.errorBody()?.string()
                val serverDetail = parseDetailMessage(errBody)
                Result.failure(Exception(serverDetail ?: "Today's wellness check-in has already been submitted."))
            } else {
                val errBody = response.errorBody()?.string()
                val serverDetail = parseDetailMessage(errBody)
                Result.failure(Exception(serverDetail ?: "Check-in failed (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun predictRiskApi(
        mood: String,
        sleepHours: Float,
        stressLevel: Int,
        workloadHours: Float,
        dutyType: String
    ): Result<WellnessRisk> {
        val moodScore = mapMoodToScore(mood).toFloat()
        val isNight = if (dutyType.equals("Night Shift", ignoreCase = true)) 1 else 0
        val isOvertime = dutyType.equals("Overtime", ignoreCase = true)
        val isStandby = dutyType.equals("On-Call / Standby", ignoreCase = true)
        val workloadScore = if (isOvertime) 8.5f else if (isStandby) 6.5f else 5.0f
        val fatigueScore = (12.0f - sleepHours).coerceIn(1.0f, 10.0f)

        val request = PredictRequest(
            dutyHoursDay = workloadHours,
            dutyHours = workloadHours,
            weeklyDutyHours = (workloadHours * 5).coerceIn(10.0f, 168.0f),
            nightShifts = isNight,
            workloadScore = workloadScore,
            sleepHours = sleepHours,
            selfReportedStress = stressLevel.toFloat(),
            stressScore = stressLevel.toFloat(),
            fatigueScore = fatigueScore,
            moodScore = moodScore
        )
        return try {
            val response = apiService.predictRisk(request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                val errBody = response.errorBody()?.string()
                val serverDetail = parseDetailMessage(errBody)
                Result.failure(Exception(serverDetail ?: "Prediction failed (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class MockWellnessRepository : WellnessRepository {
    override fun getWellnessHistory(): Flow<List<WellnessRecord>> = flowOf(emptyList())

    override fun getLatestRiskResult(): Flow<WellnessRisk> = flowOf(
        WellnessRisk(riskLevel = "LOW", rawScore = 0.15, welfareRecommendations = listOf("Maintain optimal duty-rest balance."))
    )

    override suspend fun submitDailyCheckIn(mood: String, sleepHours: Float, stressLevel: Int) {}
    override suspend fun submitStressAssessment(answers: List<Int>) {}
    override suspend fun submitWorkloadEntry(hours: Float, dutyType: String) {}

    override suspend fun getWellnessHistoryApi(): Result<List<WellnessRecord>> {
        return Result.success(emptyList())
    }

    override suspend fun submitDailyCheckInApi(mood: String, sleepHours: Float, stressLevel: Int): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun predictRiskApi(mood: String, sleepHours: Float, stressLevel: Int, workloadHours: Float, dutyType: String): Result<WellnessRisk> {
        return Result.success(WellnessRisk(riskLevel = "LOW", rawScore = 0.15, welfareRecommendations = listOf("Maintain optimal duty-rest balance.")))
    }
}
