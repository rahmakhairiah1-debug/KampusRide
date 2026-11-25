package com.example.kampusride.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kampusride.data.model.User
import com.example.kampusride.data.repository.AuthRepository
import com.example.kampusride.util.AppConfig
import com.example.kampusride.util.ErrorMapper
import com.example.kampusride.util.UiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()

    private val _state = MutableLiveData<UiState<User>>(UiState.Loading)
    val state: LiveData<UiState<User>> = _state

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val user = withTimeout(AppConfig.TIMEOUT_MS) { repository.getUserProfile() }
                _state.value = UiState.Success(user)
            } catch (e: Exception) {
                _state.value = UiState.Error(ErrorMapper.message(getApplication(), e))
            }
        }
    }

    fun logout() {
        repository.logout()
    }
}
