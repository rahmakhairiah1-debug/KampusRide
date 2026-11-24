package com.example.kampusride.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kampusride.data.repository.AuthRepository
import com.example.kampusride.util.AppConfig
import com.example.kampusride.util.ErrorMapper
import com.example.kampusride.util.UiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()

    private val _state = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val state: LiveData<UiState<Unit>> = _state

    private val _resetState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val resetState: LiveData<UiState<Unit>> = _resetState

    fun login(email: String, password: String) {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                withTimeout(AppConfig.TIMEOUT_MS) { repository.login(email, password) }
                _state.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _state.value = UiState.Error(ErrorMapper.message(getApplication(), e))
            }
        }
    }

    fun register(nama: String, nim: String, jurusan: String, email: String, password: String) {
        _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                withTimeout(AppConfig.WRITE_TIMEOUT_MS) {
                    repository.register(nama, nim, jurusan, email, password)
                }
                _state.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _state.value = UiState.Error(ErrorMapper.message(getApplication(), e))
            }
        }
    }

    fun resetPassword(email: String) {
        _resetState.value = UiState.Loading
        viewModelScope.launch {
            try {
                withTimeout(AppConfig.TIMEOUT_MS) { repository.resetPassword(email) }
                _resetState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _resetState.value = UiState.Error(ErrorMapper.message(getApplication(), e))
            }
        }
    }
}
