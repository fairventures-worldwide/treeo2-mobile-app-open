package org.treeo.treeo.repositories

import org.treeo.treeo.models.*
import org.treeo.treeo.network.models.ActivityDTO

interface IMainRepository {
    suspend fun createUser(registerUser: RegisterUser): NewRegisteredUser?
    suspend fun getCountryProjects(countryName: String): List<TreeoProject>?
    suspend fun saveUserProject(userProject: UserProject)
    suspend fun login(loginDetails: LoginDetails): LoginResponse?
    suspend fun logout(token: String): LogoutResponse?
    suspend fun postDeviceData(deviceInformation: DeviceInformation, userToken: String)
    suspend fun validatePhoneNumber(phoneNumber: String): ValidateResponseData?
    suspend fun requestOTP(phoneNumber: RequestOTP): String?
    suspend fun registerMobileUser(mobileUser: RegisterMobileUser): RegisteredMobileUser?
    suspend fun validateOTPRegistration(
        validateOTPRegistration: ValidateOTPRegistration
    ): ValidateOTPRegistrationResponse?

    suspend fun loginWithOTP(loginWithOTP: LoginWithOTP): SmsLoginResponse?
    suspend fun retrievePlannedActivities(): List<ActivityDTO>?
    suspend fun getBasicUserInfo(): BasicUserInfo
}
