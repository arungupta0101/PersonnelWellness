package com.pocketdoctor.personnelwellness.data.repository

import com.pocketdoctor.personnelwellness.data.api.WelfareApiService
import com.pocketdoctor.personnelwellness.data.model.*

interface WelfareRepository {
    suspend fun getWelfareStats(): Result<WelfareStats>
    suspend fun getRecentAlerts(): Result<List<WelfareAlert>>
    suspend fun getPersonnelWelfareDetail(id: String): Result<PersonnelWelfareDetail>
    suspend fun updateInterventionStatus(id: String, status: String, notes: String): Result<Unit>
    suspend fun referToSupport(id: String): Result<Unit>
    suspend fun requestWelfareCheckIn(id: String): Result<Unit>
    suspend fun reviewWorkload(id: String): Result<Unit>
}

class AppWelfareRepository(private val apiService: WelfareApiService) : WelfareRepository {

    override suspend fun getWelfareStats(): Result<WelfareStats> {
        return try {
            val response = apiService.getWelfareStats()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Failed to fetch stats: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentAlerts(): Result<List<WelfareAlert>> {
        return try {
            val response = apiService.getRecentAlerts()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Failed to fetch alerts: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPersonnelWelfareDetail(id: String): Result<PersonnelWelfareDetail> {
        return try {
            val response = apiService.getPersonnelWelfareDetail(id)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Access Denied or Not Found: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateInterventionStatus(id: String, status: String, notes: String): Result<Unit> {
        return try {
            val response = apiService.updateInterventionStatus(id, InterventionUpdateRequest(status, notes))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun referToSupport(id: String): Result<Unit> {
        return try {
            val response = apiService.referToSupport(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to create referral: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun requestWelfareCheckIn(id: String): Result<Unit> {
        return try {
            val response = apiService.requestWelfareCheckIn(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to request check-in: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reviewWorkload(id: String): Result<Unit> {
        return try {
            val response = apiService.reviewWorkload(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to trigger workload review: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
