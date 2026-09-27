package com.pocketdoctor.personnelwellness.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdoctor.personnelwellness.data.model.AuthState
import com.pocketdoctor.personnelwellness.data.model.UserRole
import com.pocketdoctor.personnelwellness.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ConnectionState {
    object Idle : ConnectionState()
    object Loading : ConnectionState()
    data class Success(val message: String = "Backend Connected ✅") : ConnectionState()
    data class Error(val title: String = "Backend Connection Failed ❌", val message: String) : ConnectionState()
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        if (authRepository.isUserLoggedIn()) {
            _authState.value = AuthState(
                isLoggedIn = true,
                userRole = authRepository.getSessionRole()
            )
        }
    }

    fun testBackendConnection() {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Loading
            val result = authRepository.checkHealth()
            result.onSuccess {
                _connectionState.value = ConnectionState.Success("Backend Connected ✅")
            }
            result.onFailure { exception ->
                val detailMsg = exception.message ?: "Unknown network failure"
                _connectionState.value = ConnectionState.Error(
                    title = "Backend Connection Failed ❌",
                    message = detailMsg
                )
            }
        }
    }

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _authState.value = AuthState(error = "Please fill in all fields.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState(isLoading = true)
            val result = authRepository.login(identifier, password)
            result.onSuccess { response ->
                _authState.value = AuthState(
                    isLoggedIn = true,
                    userRole = response.role
                )
            }
            result.onFailure { exception ->
                _authState.value = AuthState(error = exception.message ?: "Authentication failed")
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _authState.value = AuthState(isLoggedIn = false, userRole = null)
        _connectionState.value = ConnectionState.Idle
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
}
