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
class AuthViewModel @Inject constructor(
    private val mainRepository: IMainRepository,
    private val dispatcher: IDispatcherProvider
) : ViewModel() {

    private val _userProjectId = MutableLiveData<Int>()
    val userProjectId: LiveData<Int> get() = _userProjectId

    private val _countryName = MutableLiveData<String>()
    val countryName: LiveData<String> get() = _countryName

    private val _newUser = MutableLiveData<NewRegisteredUser>()
    val newUser: LiveData<NewRegisteredUser> get() = _newUser

    private val _phoneNumberValidationResponseRegistration =
        MutableLiveData<ValidateResponseData?>()
    val phoneNumberValidationResponseRegistration: LiveData<ValidateResponseData?>
        get() = _phoneNumberValidationResponseRegistration

    private val _registeredMobileUser = MutableLiveData<RegisteredMobileUser>()
    val registeredMobileUser: LiveData<RegisteredMobileUser> get() = _registeredMobileUser

    private val _validateOTPRegistrationResponse =
        MutableLiveData<ValidateOTPRegistrationResponse>()
    val validateOTPRegistrationResponse: LiveData<ValidateOTPRegistrationResponse>
        get() = _validateOTPRegistrationResponse

    private val _onBoardingStep = MutableLiveData(0)
    val onBoardingStep: LiveData<Int> get() = _onBoardingStep

    private val _registrationStep = MutableLiveData(0)
    val registrationStep: LiveData<Int> get() = _registrationStep

    private val _localeLanguage = MutableLiveData<String>()
    val localeLanguage: LiveData<String> get() = _localeLanguage

    private val _phoneNumber = MutableLiveData<String>()
    val phoneNumber: LiveData<String> get() = _phoneNumber

    val motivationList = MutableLiveData<MutableList<String>>()
    private val motivations = mutableListOf<String>()

    val selectedProjectsList = MutableLiveData<MutableList<String>>()
    private val projects = mutableListOf<String>()

    private val newUserObj = RegisterMobileUser()

    private val _projectsList = MutableLiveData<List<TreeoProject>>()
    val projectsList: LiveData<List<TreeoProject>> get() = _projectsList


    fun createUser(registerUser: RegisterUser) {
        viewModelScope.launch(dispatcher.io()) {
            _newUser.postValue(mainRepository.createUser(registerUser))
        }
    }

    fun validatePhoneNumberRegistration(phoneNumber: String) {
        viewModelScope.launch(dispatcher.io()) {
            _phoneNumberValidationResponseRegistration.postValue(
                mainRepository.validatePhoneNumber(
                    phoneNumber
                )
            )
        }
    }

    fun registerMobileUser(mobileUser: RegisterMobileUser) {
        viewModelScope.launch(dispatcher.io()) {
            _registeredMobileUser.postValue((mainRepository.registerMobileUser(mobileUser)))
        }
    }

    fun validateOTPRegistration(validateOTPRegistration: ValidateOTPRegistration) {
        viewModelScope.launch(dispatcher.io()) {
            _validateOTPRegistrationResponse.postValue(
                mainRepository.validateOTPRegistration(
                    validateOTPRegistration
                )
            )
        }
    }

    fun getProjects(countryName: String) {
        viewModelScope.launch(dispatcher.io()) {
            _projectsList.postValue(mainRepository.getCountryProjects(countryName))
        }
    }


    fun saveUserProject(userProject: UserProject) {
        viewModelScope.launch(dispatcher.io()) { mainRepository.saveUserProject(userProject)
        }
    }

    fun setProjectId(projectId: Int) {
        _userProjectId.value = projectId
    }

    fun setCountryName(countryName: String) {
        _countryName.value = countryName
    }

    fun setLocaleLanguage(language: String) {
        _localeLanguage.value = language
    }

    fun registerMobileUser() {
        registerMobileUser(newUserObj)
    }

    fun setUserInformation(firstName: String, lastName: String) {
        newUserObj.firstName = firstName
        newUserObj.lastName = lastName
    }

    fun setUserPhoneNumber(phoneNumber: String, country: String) {
        _phoneNumber.value = phoneNumber
        newUserObj.phoneNumber = phoneNumber
        newUserObj.country = country
    }

    fun setGDPRStatus(checkGdpr: Boolean) {
        newUserObj.gdprAccepted = checkGdpr
    }

    fun getNewUserObj() = newUserObj

    fun onBoardingContinue() {
        _onBoardingStep.value = onBoardingStep.value!!.plus(1)
    }

    fun onBoardingBack() {
        _onBoardingStep.value = onBoardingStep.value?.minus(1)
    }

    fun resetOnBoardingStep() {
        _onBoardingStep.value = 0
    }

    fun useInfoRegistrationContinue() {
        _registrationStep.value = registrationStep.value!!.plus(2)
    }

    fun updateRegistrationStep(nextStep: Int) {
        _registrationStep.value = nextStep
    }

    fun registrationBack() {
        _registrationStep.value = registrationStep.value!!.minus(1)
    }

    fun resetRegistrationStep() {
        _registrationStep.value = 0
    }

    fun addMotivation(motivation: String) {
        motivations.add(motivation)
        motivationList.value = motivations
    }

    fun removeMotivation(motivation: String) {
        if (motivations.contains(motivation)) {
            motivations.remove(motivation)
            motivationList.value = motivations
        }
    }

    fun clearMotivations() {
        if (motivations.size > 0) {
            motivations.clear()
            motivationList.value = motivations
        }
    }

    fun addUserProject(projectName: String) {
        projects.add(projectName)
        selectedProjectsList.value = projects
    }

    fun removeUserProject(projectName: String) {
        if (motivations.contains(projectName)) {
            projects.remove(projectName)
            selectedProjectsList.value = projects
        }
    }

    fun clearUserProjects() {
        if (projects.size > 0) {
            projects.clear()
            selectedProjectsList.value = projects
        }
    }



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
