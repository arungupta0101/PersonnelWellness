package com.pocketdoctor.personnelwellness.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdoctor.personnelwellness.data.model.ApiResult
import com.pocketdoctor.personnelwellness.data.model.WellnessRecord
import com.pocketdoctor.personnelwellness.data.model.WellnessRisk
import com.pocketdoctor.personnelwellness.data.repository.WellnessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WellnessViewModel(private val repository: WellnessRepository) : ViewModel() {

    private val _history = MutableStateFlow<List<WellnessRecord>>(emptyList())
    val history: StateFlow<List<WellnessRecord>> = _history.asStateFlow()

    private val _latestRisk = MutableStateFlow<ApiResult<WellnessRisk>?>(null)
    val latestRisk: StateFlow<ApiResult<WellnessRisk>?> = _latestRisk.asStateFlow()

    private val _checkInStatus = MutableStateFlow<ApiResult<Unit>?>(null)
    val checkInStatus: StateFlow<ApiResult<Unit>?> = _checkInStatus.asStateFlow()

    // Temporary storage for risk prediction inputs
    private var lastMood: String = "Happy"
    private var lastSleep: Float = 7f
    private var lastStress: Int = 3
    private var lastWorkload: Float = 8f
    private var lastDutyType: String = "Day Shift"

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            repository.getWellnessHistory().collect {
                _history.value = it
            }
        }
    }

    fun submitDailyCheckIn(mood: String, sleepHours: Float, stressLevel: Int) {
        lastMood = mood
        lastSleep = sleepHours
        lastStress = stressLevel
        
        viewModelScope.launch {
            _checkInStatus.value = ApiResult.Loading
            val result = repository.submitDailyCheckInApi(mood, sleepHours, stressLevel)
            result.onSuccess {
                _checkInStatus.value = ApiResult.Success(Unit)
            }
            result.onFailure {
                _checkInStatus.value = ApiResult.Error(it.message ?: "Network error occurred")
            }
        }
    }

    fun submitWorkloadEntry(hours: Float, dutyType: String) {
        lastWorkload = hours
        lastDutyType = dutyType
        // Also triggers prediction for demonstration flow
        fetchRiskPrediction()
    }

    fun fetchRiskPrediction() {
        viewModelScope.launch {
            _latestRisk.value = ApiResult.Loading
            val result = repository.predictRiskApi(
                lastMood, lastSleep, lastStress, lastWorkload, lastDutyType
            )
            result.onSuccess {
                _latestRisk.value = ApiResult.Success(it)
            }
            result.onFailure {
                _latestRisk.value = ApiResult.Error(it.message ?: "Failed to get AI prediction")
            }
        }
    }

    fun resetCheckInStatus() {
        _checkInStatus.value = null
    }

    fun submitStressAssessment(answers: List<Int>) {
        // Mock assessment logic
        viewModelScope.launch {
            repository.submitStressAssessment(answers)
        }
    }
}
