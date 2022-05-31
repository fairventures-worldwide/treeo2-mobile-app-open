package org.treeo.treeo.ui.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.treeo.treeo.models.*
import org.treeo.treeo.repositories.IMainRepository
import org.treeo.treeo.util.IDispatcherProvider
import javax.inject.Inject

@HiltViewModel
class LoginLogoutViewModel @Inject constructor(
    private val mainRepository: IMainRepository,
    private val dispatcher: IDispatcherProvider
) : ViewModel() {

    private val _loginToken = MutableLiveData<LoginResponse>()
    val loginToken: LiveData<LoginResponse> get() = _loginToken

    private val _logoutResponse = MutableLiveData<LogoutResponse>()
    val logoutResponse: LiveData<LogoutResponse> get() = _logoutResponse

    private val _phoneNumberOTPResponse = MutableLiveData<String>()
    val phoneNumberOTPResponse: LiveData<String> get() = _phoneNumberOTPResponse

    private val _smsLoginResponse = MutableLiveData<SmsLoginResponse>()
    val smsLoginResponse: LiveData<SmsLoginResponse> get() = _smsLoginResponse

    private val _loginStep = MutableLiveData(0)
    val loginStep: LiveData<Int> get() = _loginStep

    private val _phoneNumber = MutableLiveData<String>()
    val phoneNumber: LiveData<String> get() = _phoneNumber

    fun login(email: String, password: String) {
        viewModelScope.launch(dispatcher.io()) {
            _loginToken.postValue(mainRepository.login(LoginDetails(email, password)))
        }
    }

    fun logout(token: String) {
        viewModelScope.launch(dispatcher.io()) {
            _logoutResponse.postValue(mainRepository.logout(token))
        }
    }

    fun postDeviceData(deviceInformation: DeviceInformation, userToken: String) {
        viewModelScope.launch(dispatcher.io()) {
            mainRepository.postDeviceData(deviceInformation, userToken)
        }
    }

    fun requestOTP(phoneNumber: RequestOTP) {
        viewModelScope.launch(dispatcher.io()) {
            _phoneNumberOTPResponse.postValue(mainRepository.requestOTP(phoneNumber))
        }
    }

    fun loginWithOTP(loginWithOTP: LoginWithOTP) {
        viewModelScope.launch(dispatcher.io()) {
            _smsLoginResponse.postValue(mainRepository.loginWithOTP(loginWithOTP))
        }
    }

    fun loginContinue() {
        _loginStep.value = loginStep.value!!.plus(1)
    }

    fun loginBack() {
        _loginStep.value = loginStep.value?.minus(1)
    }

    fun setPhoneNumber(phoneNumber: String) {
        _phoneNumber.value = phoneNumber
    }
}
