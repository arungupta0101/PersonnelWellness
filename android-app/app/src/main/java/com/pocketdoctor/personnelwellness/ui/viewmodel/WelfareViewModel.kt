package com.pocketdoctor.personnelwellness.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdoctor.personnelwellness.data.model.ApiResult
import com.pocketdoctor.personnelwellness.data.model.PersonnelWelfareDetail
import com.pocketdoctor.personnelwellness.data.model.WelfareAlert
import com.pocketdoctor.personnelwellness.data.model.WelfareStats
import com.pocketdoctor.personnelwellness.data.repository.WelfareRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WelfareViewModel(private val repository: WelfareRepository) : ViewModel() {

    private val _statsState = MutableStateFlow<ApiResult<WelfareStats>>(ApiResult.Loading)
    val statsState: StateFlow<ApiResult<WelfareStats>> = _statsState.asStateFlow()

    private val _alertsState = MutableStateFlow<ApiResult<List<WelfareAlert>>>(ApiResult.Loading)
    val alertsState: StateFlow<ApiResult<List<WelfareAlert>>> = _alertsState.asStateFlow()

    private val _detailState = MutableStateFlow<ApiResult<PersonnelWelfareDetail>?>(null)
    val detailState: StateFlow<ApiResult<PersonnelWelfareDetail>?> = _detailState.asStateFlow()

    private val _actionState = MutableStateFlow<ApiResult<Unit>?>(null)
    val actionState: StateFlow<ApiResult<Unit>?> = _actionState.asStateFlow()

    fun loadDashboardData() {
        viewModelScope.launch {
            _statsState.value = ApiResult.Loading
            repository.getWelfareStats().onSuccess {
                _statsState.value = ApiResult.Success(it)
            }.onFailure {
                _statsState.value = ApiResult.Error(it.message ?: "Failed to fetch metrics")
            }
        }

        viewModelScope.launch {
            _alertsState.value = ApiResult.Loading
            repository.getRecentAlerts().onSuccess {
                _alertsState.value = ApiResult.Success(it)
            }.onFailure {
                _alertsState.value = ApiResult.Error(it.message ?: "Failed to fetch recent alerts")
            }
        }
    }

    fun loadPersonnelDetail(id: String) {
        viewModelScope.launch {
            _detailState.value = ApiResult.Loading
            repository.getPersonnelWelfareDetail(id).onSuccess {
                _detailState.value = ApiResult.Success(it)
            }.onFailure {
                _detailState.value = ApiResult.Error(it.message ?: "Unauthorized to read this profile")
            }
        }
    }

    fun updateIntervention(id: String, status: String, notes: String) {
        viewModelScope.launch {
            _actionState.value = ApiResult.Loading
            repository.updateInterventionStatus(id, status, notes).onSuccess {
                _actionState.value = ApiResult.Success(Unit)
                loadPersonnelDetail(id)
            }.onFailure {
                _actionState.value = ApiResult.Error(it.message ?: "Failed to update intervention state")
            }
        }
    }

    fun referToSupport(id: String) {
        viewModelScope.launch {
            _actionState.value = ApiResult.Loading
            repository.referToSupport(id).onSuccess {
                _actionState.value = ApiResult.Success(Unit)
                loadPersonnelDetail(id)
            }.onFailure {
                _actionState.value = ApiResult.Error(it.message ?: "Failed to execute professional referral")
            }
        }
    }

    fun requestCheckIn(id: String) {
        viewModelScope.launch {
            _actionState.value = ApiResult.Loading
            repository.requestWelfareCheckIn(id).onSuccess {
                _actionState.value = ApiResult.Success(Unit)
                loadPersonnelDetail(id)
            }.onFailure {
                _actionState.value = ApiResult.Error(it.message ?: "Failed to request check-in")
            }
        }
    }

    fun triggerWorkloadReview(id: String) {
        viewModelScope.launch {
            _actionState.value = ApiResult.Loading
            repository.reviewWorkload(id).onSuccess {
                _actionState.value = ApiResult.Success(Unit)
                loadPersonnelDetail(id)
            }.onFailure {
                _actionState.value = ApiResult.Error(it.message ?: "Failed to trigger workload review")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = null
    }

    fun clearPersonnelDetail() {
        _detailState.value = null
    }
}
