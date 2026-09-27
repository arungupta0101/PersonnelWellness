package com.pocketdoctor.personnelwellness.data.api

import com.pocketdoctor.personnelwellness.data.model.HealthResponse
import com.pocketdoctor.personnelwellness.data.model.LoginRequest
import com.pocketdoctor.personnelwellness.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("health")
    suspend fun checkHealth(): Response<HealthResponse>
}
