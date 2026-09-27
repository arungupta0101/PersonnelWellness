package com.pocketdoctor.personnelwellness.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdoctor.personnelwellness.data.model.ApiResult
import com.pocketdoctor.personnelwellness.data.model.ConsentPreferences
import com.pocketdoctor.personnelwellness.data.model.ConsentStatusResponse
import com.pocketdoctor.personnelwellness.data.repository.ConsentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConsentViewModel(private val repository: ConsentRepository) : ViewModel() {

    private val _consentState = MutableStateFlow<ApiResult<ConsentStatusResponse>>(ApiResult.Loading)
    val consentState: StateFlow<ApiResult<ConsentStatusResponse>> = _consentState.asStateFlow()

    private val _updateResult = MutableStateFlow<ApiResult<Unit>?>(null)
    val updateResult: StateFlow<ApiResult<Unit>?> = _updateResult.asStateFlow()

    fun fetchConsentStatus() {
        viewModelScope.launch {
            _consentState.value = ApiResult.Loading
            repository.getConsentStatus()
                .onSuccess { _consentState.value = ApiResult.Success(it) }
                .onFailure { _consentState.value = ApiResult.Error(it.message ?: "Failed to fetch consent status") }
        }
    }

    fun updateConsent(preferences: ConsentPreferences) {
        viewModelScope.launch {
            _updateResult.value = ApiResult.Loading
            repository.updateConsent(preferences)
                .onSuccess {
                    _updateResult.value = ApiResult.Success(Unit)
                    fetchConsentStatus()
                }
                .onFailure { _updateResult.value = ApiResult.Error(it.message ?: "Failed to update consent") }
        }
    }

    fun resetUpdateResult() {
        _updateResult.value = null
    }
}
