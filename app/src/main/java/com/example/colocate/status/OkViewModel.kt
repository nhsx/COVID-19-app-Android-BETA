package com.example.colocate.status

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.colocate.ViewState
import com.example.colocate.registration.RegistrationResult
import com.example.colocate.registration.RegistrationUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

class OkViewModel @Inject constructor(
    private val registrationUseCase: RegistrationUseCase
) : ViewModel() {

    private val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> {
        return viewState
    }

    fun register() {
        viewModelScope.launch {
            viewState.value = ViewState.Progress
            val startTime = Date().time
            val registrationResult = registrationUseCase.register()
            viewState.value = when (registrationResult) {
                RegistrationResult.Success, RegistrationResult.AlreadyRegistered -> ViewState.Success
                is RegistrationResult.Failure -> {
                    waitForReadability(startTime)
                    ViewState.Error(registrationResult.exception)
                }
            }
        }
    }

    private suspend fun waitForReadability(startTime: Long) {
        val elapsedTime = Date().time - startTime
        val remainingDelay = MINIMUM_DELAY_FOR_READING - elapsedTime
        delay(remainingDelay)
    }

    companion object {
        const val MINIMUM_DELAY_FOR_READING = 2_000
    }
}