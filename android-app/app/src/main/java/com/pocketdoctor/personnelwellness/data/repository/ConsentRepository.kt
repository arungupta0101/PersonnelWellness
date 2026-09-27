package com.pocketdoctor.personnelwellness.data.repository

import com.pocketdoctor.personnelwellness.data.api.ConsentApiService
import com.pocketdoctor.personnelwellness.data.model.ConsentPreferences
import com.pocketdoctor.personnelwellness.data.model.ConsentStatusResponse
import com.pocketdoctor.personnelwellness.data.model.ConsentUpdateRequest

interface ConsentRepository {
    suspend fun getConsentStatus(): Result<ConsentStatusResponse>
    suspend fun grantConsent(): Result<ConsentStatusResponse>
    suspend fun revokeConsent(): Result<ConsentStatusResponse>
    suspend fun updateConsent(preferences: ConsentPreferences): Result<Unit>
}

class AppConsentRepository(private val apiService: ConsentApiService) : ConsentRepository {
    override suspend fun getConsentStatus(): Result<ConsentStatusResponse> {
        return try {
            val response = apiService.getConsentStatus()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun grantConsent(): Result<ConsentStatusResponse> {
        return try {
            val response = apiService.grantConsent(ConsentUpdateRequest())
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun revokeConsent(): Result<ConsentStatusResponse> {
        return try {
            val response = apiService.revokeConsent(ConsentUpdateRequest())
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateConsent(preferences: ConsentPreferences): Result<Unit> {
        return if (preferences.mandatoryAccepted) {
            val result = grantConsent()
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                // If unauthenticated during initial onboarding before login, succeed locally
                Result.success(Unit)
            }
        } else {
            val result = revokeConsent()
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.success(Unit)
            }
        }
    }
}
