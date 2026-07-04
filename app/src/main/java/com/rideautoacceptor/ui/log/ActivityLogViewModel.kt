package com.rideautoacceptor.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.rideautoacceptor.data.model.RideEvent
import com.rideautoacceptor.data.repository.RideRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ActivityLogViewModel(private val repository: RideRepository) : ViewModel() {

    // Current filter: "ALL" | "ACCEPTED" | "SKIPPED"
    private val _filter = MutableStateFlow("ALL")
    val filter: StateFlow<String> = _filter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val events: Flow<PagingData<RideEvent>> = _filter
        .flatMapLatest { filterValue ->
            repository.getAllEventsPaged(filterValue)
        }
        .cachedIn(viewModelScope)

    fun setFilter(filterValue: String) {
        _filter.value = filterValue
    }

    fun deleteAll() {
        viewModelScope.launch { repository.deleteAll() }
    }

    fun deleteEvent(event: RideEvent) {
        viewModelScope.launch { repository.deleteEvent(event) }
    }

    class Factory(private val repository: RideRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ActivityLogViewModel(repository) as T
    }
}
