package com.pocketdoctor.personnelwellness.data.api

import com.pocketdoctor.personnelwellness.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface WelfareApiService {
    @GET("welfare/stats")
    suspend fun getWelfareStats(): Response<WelfareStats>

    @GET("welfare/alerts")
    suspend fun getRecentAlerts(): Response<List<WelfareAlert>>

    @GET("welfare/personnel/{id}")
    suspend fun getPersonnelWelfareDetail(@Path("id") id: String): Response<PersonnelWelfareDetail>

    @POST("welfare/personnel/{id}/intervention")
    suspend fun updateInterventionStatus(
        @Path("id") id: String,
        @Body request: InterventionUpdateRequest
    ): Response<Unit>

    @POST("welfare/personnel/{id}/referral")
    suspend fun referToSupport(@Path("id") id: String): Response<Unit>

    @POST("welfare/personnel/{id}/checkin-request")
    suspend fun requestWelfareCheckIn(@Path("id") id: String): Response<Unit>

    @POST("welfare/personnel/{id}/workload-review")
    suspend fun reviewWorkload(@Path("id") id: String): Response<Unit>
}
