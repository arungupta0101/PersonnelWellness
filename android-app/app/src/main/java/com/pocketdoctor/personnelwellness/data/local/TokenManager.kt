package com.pocketdoctor.personnelwellness.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pocketdoctor.personnelwellness.data.model.UserRole

class TokenManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("access_token", null)
    }

    fun saveRole(role: UserRole) {
        prefs.edit().putString("user_role", role.name).apply()
    }

    fun getRole(): UserRole? {
        val roleName = prefs.getString("user_role", null)
        return roleName?.let { UserRole.valueOf(it) }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
