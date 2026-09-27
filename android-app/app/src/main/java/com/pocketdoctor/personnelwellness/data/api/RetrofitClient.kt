package com.pocketdoctor.personnelwellness.data.api

import com.pocketdoctor.personnelwellness.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Default to 127.0.0.1:8000 for physical phone with `adb reverse tcp:8000 tcp:8000`
    // Can also be set to PC Wi-Fi IP (e.g. "http://192.168.1.186:8000/") or "http://10.0.2.2:8000/" for Emulator
    var BASE_URL: String = "http://127.0.0.1:8000/"
        private set

    private var tokenManager: TokenManager? = null
    private var retrofitInstance: Retrofit? = null

    fun init(manager: TokenManager, customBaseUrl: String? = null) {
        tokenManager = manager
        if (!customBaseUrl.isNullOrBlank()) {
            BASE_URL = if (customBaseUrl.endsWith("/")) customBaseUrl else "$customBaseUrl/"
            retrofitInstance = null
        }
    }

    fun updateBaseUrl(newUrl: String) {
        BASE_URL = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        retrofitInstance = null
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
        redactHeader("Authorization")
    }

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        tokenManager?.getToken()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        val response = chain.proceed(requestBuilder.build())
        
        // Handle expired/invalid tokens globally
        if (response.code == 401) {
            tokenManager?.clear()
        }
        response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun getRetrofit(): Retrofit {
        var instance = retrofitInstance
        if (instance == null) {
            instance = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            retrofitInstance = instance
        }
        return instance
    }

    val wellnessApiService: WellnessApiService
        get() = getRetrofit().create(WellnessApiService::class.java)

    val authApiService: AuthApiService
        get() = getRetrofit().create(AuthApiService::class.java)

    val welfareApiService: WelfareApiService
        get() = getRetrofit().create(WelfareApiService::class.java)

    val consentApiService: ConsentApiService
        get() = getRetrofit().create(ConsentApiService::class.java)
}
