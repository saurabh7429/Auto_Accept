package com.rideautoacceptor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rideautoacceptor.data.model.FilterPrefs
import com.rideautoacceptor.data.model.RideEvent
import com.rideautoacceptor.data.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: RideRepository) : ViewModel() {

    val filterPrefs: StateFlow<FilterPrefs> = repository.filterPrefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilterPrefs())

    val todayAccepted: StateFlow<Int> = repository.getTodayAcceptedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todaySkipped: StateFlow<Int> = repository.getTodaySkippedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val latestEvent: StateFlow<RideEvent?> = repository.getLatestEvent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setMasterEnabled(enabled) }
    }

    fun refresh() {
        // StateFlows backed by Room auto-refresh; this is a no-op hook for manual triggers
    }

    class Factory(private val repository: RideRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(repository) as T
    }
}
