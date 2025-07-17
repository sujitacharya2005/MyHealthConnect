package com.sssk.myhealthconnect

import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class StepViewModel(private val repository: StepRepository) : ViewModel() {
    val message = mutableStateOf("")
    val loading = mutableStateOf(false)
    val permissionsGranted = mutableStateOf(false)
    val lastStepCount = mutableStateOf<Long?>(null)

    val stepPermissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getWritePermission(SpeedRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class)
    )

    fun checkPermissions(healthConnectManager: HealthConnectManager) {
        viewModelScope.launch {
            permissionsGranted.value = healthConnectManager.hasAllPermissions(stepPermissions)
        }
    }

    fun fetchAndPostSteps(healthConnectManager: HealthConnectManager) {
        viewModelScope.launch {
            loading.value = true
            try {
                val steps = repository.fetchTodaySteps()
                lastStepCount.value = repository.fetchTodayTotalSteps() // Use aggregate API for display
                val result = repository.postStepsData(steps)
                message.value = result.getOrElse { e -> "Error: ${e::class.java.simpleName} - ${e.message}" }
            } catch (e: Exception) {
                message.value = "Error: ${e::class.java.simpleName} - ${e.message}" }
            loading.value = false
        }
    }
} 