package com.pocketdoctor.personnelwellness.data.repository

import com.pocketdoctor.personnelwellness.data.api.WellnessApiService
import com.pocketdoctor.personnelwellness.data.model.CheckInRequest
import com.pocketdoctor.personnelwellness.data.model.PredictRequest
import com.pocketdoctor.personnelwellness.data.model.WellnessRecord
import com.pocketdoctor.personnelwellness.data.model.WellnessRisk
import com.pocketdoctor.personnelwellness.data.model.mapMoodToScore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface WellnessRepository {
    fun getWellnessHistory(): Flow<List<WellnessRecord>>
    fun getLatestRiskResult(): Flow<WellnessRisk>
    suspend fun submitDailyCheckIn(mood: String, sleepHours: Float, stressLevel: Int)
    suspend fun submitStressAssessment(answers: List<Int>)
    suspend fun submitWorkloadEntry(hours: Float, dutyType: String)
    
    // Real backend API calls
    suspend fun submitDailyCheckInApi(mood: String, sleepHours: Float, stressLevel: Int): Result<Unit>
    suspend fun predictRiskApi(mood: String, sleepHours: Float, stressLevel: Int, workloadHours: Float, dutyType: String): Result<WellnessRisk>
}

class AppWellnessRepository(private val apiServiceProvider: () -> WellnessApiService) : WellnessRepository {
    
    private val apiService get() = apiServiceProvider()

    override fun getWellnessHistory(): Flow<List<WellnessRecord>> = flowOf(
        listOf(
            WellnessRecord("1", "2025-01-15", "Happy", 8f, 2),
            WellnessRecord("2", "2025-01-14", "Tired", 6f, 5),
            WellnessRecord("3", "2025-01-13", "Stressed", 5f, 8)
        )
    )

    override fun getLatestRiskResult(): Flow<WellnessRisk> = flowOf(
        WellnessRisk(riskLevel = "LOW", rawScore = 0.15, recommendationsList = listOf("Maintain optimal duty-rest balance."))
    )

    override suspend fun submitDailyCheckIn(mood: String, sleepHours: Float, stressLevel: Int) {}
    override suspend fun submitStressAssessment(answers: List<Int>) {}
    override suspend fun submitWorkloadEntry(hours: Float, dutyType: String) {}

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
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception("Check-in failed (${response.code()}): $errBody"))
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
        val request = PredictRequest(
            dutyHours = workloadHours,
            sleepHours = sleepHours,
            stressScore = stressLevel.toFloat(),
            moodScore = moodScore
        )
        return try {
            val response = apiService.predictRisk(request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception("Prediction failed (${response.code()}): $errBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class MockWellnessRepository : WellnessRepository {
    override fun getWellnessHistory(): Flow<List<WellnessRecord>> = flowOf(
        listOf(
            WellnessRecord("1", "2025-01-15", "Happy", 8f, 2),
            WellnessRecord("2", "2025-01-14", "Tired", 6f, 5)
        )
    )

    override fun getLatestRiskResult(): Flow<WellnessRisk> = flowOf(
        WellnessRisk(riskLevel = "LOW", rawScore = 0.15, recommendationsList = listOf("Maintain optimal duty-rest balance."))
    )

    override suspend fun submitDailyCheckIn(mood: String, sleepHours: Float, stressLevel: Int) {}
    override suspend fun submitStressAssessment(answers: List<Int>) {}
    override suspend fun submitWorkloadEntry(hours: Float, dutyType: String) {}

    override suspend fun submitDailyCheckInApi(mood: String, sleepHours: Float, stressLevel: Int): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun predictRiskApi(mood: String, sleepHours: Float, stressLevel: Int, workloadHours: Float, dutyType: String): Result<WellnessRisk> {
        return Result.success(WellnessRisk(riskLevel = "LOW", rawScore = 0.15, recommendationsList = listOf("Maintain optimal duty-rest balance.")))
    }
}
