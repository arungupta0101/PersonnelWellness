package com.pocketdoctor.personnelwellness.data.repository

import com.pocketdoctor.personnelwellness.data.api.AuthApiService
import com.pocketdoctor.personnelwellness.data.api.RetrofitClient
import com.pocketdoctor.personnelwellness.data.local.TokenManager
import com.pocketdoctor.personnelwellness.data.model.HealthResponse
import com.pocketdoctor.personnelwellness.data.model.LoginRequest
import com.pocketdoctor.personnelwellness.data.model.LoginResponse
import com.pocketdoctor.personnelwellness.data.model.UserRole
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

interface AuthRepository {
    suspend fun login(identifier: String, password: String): Result<LoginResponse>
    suspend fun checkHealth(): Result<HealthResponse>
    fun logout()
    fun getSessionRole(): UserRole?
    fun isUserLoggedIn(): Boolean
}

class AppAuthRepository(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(identifier: String, password: String): Result<LoginResponse> {
        return try {
            val response = authApiService.login(LoginRequest(identifier, password))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                tokenManager.saveToken(body.accessToken)
                tokenManager.saveRole(body.role)
                Result.success(body)
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "Invalid credentials. Please verify your ID/email and password."
                    403 -> "Access Denied. You do not have permissions to access this role."
                    in 500..599 -> "Server error. Please try again later."
                    else -> "Authentication failed: ${response.message()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val url = RetrofitClient.BASE_URL
            val msg = when (e) {
                is ConnectException -> "Connection failed to $url. Please check your network connection."
                is SocketTimeoutException -> "Connection timed out connecting to $url. Server may be waking up (Render cold start)."
                is UnknownHostException -> "Unable to resolve host $url."
                else -> e.localizedMessage ?: "Network error occurred."
            }
            Result.failure(Exception(msg))
        }
    }

    override suspend fun checkHealth(): Result<HealthResponse> {
        return try {
            val response = authApiService.checkHealth()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.status?.equals("ok", ignoreCase = true) == true) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Unexpected response status: '${body.status ?: "null"}'"))
                }
            } else {
                val errorMsg = when (response.code()) {
                    404 -> "Endpoint /health not found (HTTP 404)."
                    in 500..599 -> "Server error (HTTP ${response.code()})."
                    else -> "HTTP ${response.code()}: ${response.message()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val url = RetrofitClient.BASE_URL
            val msg = when (e) {
                is ConnectException -> "Connection failed to $url. Please check your network connection."
                is SocketTimeoutException -> "Connection timed out connecting to $url. Server may be waking up (Render cold start)."
                is UnknownHostException -> "Unable to resolve host $url."
                else -> e.localizedMessage ?: "Network error occurred."
            }
            Result.failure(Exception(msg))
        }
    }

    override fun logout() {
        tokenManager.clear()
    }

    override fun getSessionRole(): UserRole? {
        return tokenManager.getRole()
    }

    override fun isUserLoggedIn(): Boolean {
        return tokenManager.getToken() != null
    }
}
