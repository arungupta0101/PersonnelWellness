package com.pocketdoctor.personnelwellness.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username") val identifier: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val role: UserRole
)

enum class UserRole {
    @SerializedName("personnel", alternate = ["PERSONNEL"])
    PERSONNEL,

    @SerializedName("welfare_officer", alternate = ["WELFARE_OFFICER"])
    WELFARE_OFFICER,

    @SerializedName("commander", alternate = ["COMMANDER"])
    COMMANDER,

    @SerializedName("admin", alternate = ["ADMIN"])
    ADMIN
}

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userRole: UserRole? = null,
    val error: String? = null
)
