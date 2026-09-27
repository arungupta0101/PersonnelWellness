package com.pocketdoctor.personnelwellness.data.repository

import com.pocketdoctor.personnelwellness.data.api.WellnessApiService
import com.pocketdoctor.personnelwellness.data.model.CheckInRequest
import com.pocketdoctor.personnelwellness.data.model.PredictRequest
import com.pocketdoctor.personnelwellness.data.model.WellnessRecord
import com.pocketdoctor.personnelwellness.data.model.WellnessRisk
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
            WellnessRecord("1", "2023-10-27", "Happy", 8f, 2),
            WellnessRecord("2", "2023-10-26", "Tired", 6f, 5),
            WellnessRecord("3", "2023-10-25", "Stressed", 5f, 8)
        )
    )

    override fun getLatestRiskResult(): Flow<WellnessRisk> = flowOf(
        WellnessRisk("Moderate", 45, "Consider taking a short break and practicing mindfulness.")
    )

    override suspend fun submitDailyCheckIn(mood: String, sleepHours: Float, stressLevel: Int) {
        // Mocked implementation local fallback
    }

    override suspend fun submitStressAssessment(answers: List<Int>) {
        // Mocked implementation local fallback
    }

    override suspend fun submitWorkloadEntry(hours: Float, dutyType: String) {
        // Mocked implementation local fallback
    }

    override suspend fun submitDailyCheckInApi(mood: String, sleepHours: Float, stressLevel: Int): Result<Unit> {
        return try {
            val response = apiService.submitCheckIn(CheckInRequest(mood, sleepHours, stressLevel))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
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
        return try {
            val response = apiService.predictRisk(PredictRequest(mood, sleepHours, stressLevel, workloadHours, dutyType))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class MockWellnessRepository : WellnessRepository {
    override fun getWellnessHistory(): Flow<List<WellnessRecord>> = flowOf(
        listOf(
            WellnessRecord("1", "2023-10-27", "Happy", 8f, 2),
            WellnessRecord("2", "2023-10-26", "Tired", 6f, 5),
            WellnessRecord("3", "2023-10-25", "Stressed", 5f, 8)
        )
    )

    override fun getLatestRiskResult(): Flow<WellnessRisk> = flowOf(
        WellnessRisk("Moderate", 45, "Consider taking a short break and practicing mindfulness.")
    )

    override suspend fun submitDailyCheckIn(mood: String, sleepHours: Float, stressLevel: Int) {}
    override suspend fun submitStressAssessment(answers: List<Int>) {}
    override suspend fun submitWorkloadEntry(hours: Float, dutyType: String) {}

    override suspend fun submitDailyCheckInApi(mood: String, sleepHours: Float, stressLevel: Int): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun predictRiskApi(mood: String, sleepHours: Float, stressLevel: Int, workloadHours: Float, dutyType: String): Result<WellnessRisk> {
        return Result.success(WellnessRisk("Low", 15, "Mock recommendation"))
    }
}
