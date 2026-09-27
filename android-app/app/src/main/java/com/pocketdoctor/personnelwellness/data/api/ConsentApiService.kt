package com.pocketdoctor.personnelwellness.data.api

import com.pocketdoctor.personnelwellness.data.model.ConsentStatusResponse
import com.pocketdoctor.personnelwellness.data.model.ConsentUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ConsentApiService {
    @GET("consent/status")
    suspend fun getConsentStatus(
        @Query("consent_type") consentType: String = "data_sharing_wellness"
    ): Response<ConsentStatusResponse>

    @POST("consent/grant")
    suspend fun grantConsent(
        @Body request: ConsentUpdateRequest = ConsentUpdateRequest()
    ): Response<ConsentStatusResponse>

    @POST("consent/revoke")
    suspend fun revokeConsent(
        @Body request: ConsentUpdateRequest = ConsentUpdateRequest()
    ): Response<ConsentStatusResponse>
}
