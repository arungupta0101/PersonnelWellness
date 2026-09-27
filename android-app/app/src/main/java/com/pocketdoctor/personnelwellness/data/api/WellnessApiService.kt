package com.pocketdoctor.personnelwellness.data.api

import com.pocketdoctor.personnelwellness.data.model.CheckInRequest
import com.pocketdoctor.personnelwellness.data.model.PredictRequest
import com.pocketdoctor.personnelwellness.data.model.TodayCheckInStatus
import com.pocketdoctor.personnelwellness.data.model.WellnessCheckinResponse
import com.pocketdoctor.personnelwellness.data.model.WellnessRisk
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface WellnessApiService {
    @GET("wellness/today-status")
    suspend fun getTodayCheckInStatus(): Response<TodayCheckInStatus>

    @POST("wellness/checkin")
    suspend fun submitCheckIn(@Body request: CheckInRequest): Response<Unit>

    @GET("wellness/history")
    suspend fun getWellnessHistory(): Response<List<WellnessCheckinResponse>>

    @POST("prediction/predict")
    suspend fun predictRisk(@Body request: PredictRequest): Response<WellnessRisk>
}
