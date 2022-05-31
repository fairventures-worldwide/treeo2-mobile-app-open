package org.treeo.treeo.repositories

import org.treeo.treeo.models.*
import org.treeo.treeo.network.models.*

class FakeMainRepository : IMainRepository {
    private fun returnFakeUser(registerUser: RegisterUser): NewRegisteredUser {
        return NewRegisteredUser(
            registerUser.email,
            registerUser.firstName,
            false,
            registerUser.lastName
        )
    }

    override suspend fun getCountryProjects(countryName: String): List<TreeoProject>? {
        TODO("Not yet implemented")
    }

    override suspend fun saveUserProject(userProject: UserProject) {
        TODO("Not yet implemented")
    }

    override suspend fun createUser(registerUser: RegisterUser): NewRegisteredUser? {
        return returnFakeUser(registerUser)
    }

    private fun returnFakeLoginResponse(loginDetails: LoginDetails): LoginResponse {
        return LoginResponse(
            "username",
            loginDetails.email,
            1,
            "thisisatesttoken"
        )
    }

    private fun returnFakeValidatePhoneNumberResponse(): ValidateResponseData {
        return ValidateResponseData(
            errorStatus = "none",
            phoneNumber = "111",
            valid = true
        )
    }

    private fun returnFakeOTP(): String {
        return "OTP Sent"
    }

    private fun returnFakeRegisteredMobileUser(): RegisteredMobileUser {
        return RegisteredMobileUser(
            email = "",
            firstName = "firstname",
            id = 2,
            isActive = true,
            lastName = "lastname"
        )
    }

    private fun returnFakeOTPRegistrationResponse(): ValidateOTPRegistrationResponse {
        return ValidateOTPRegistrationResponse(
            token = "thisisatesttoken",
            userId = 1243,
            username = "the name",
            refreshToken = "therefreshToken"
        )
    }

    override suspend fun login(loginDetails: LoginDetails): LoginResponse {
        return returnFakeLoginResponse(loginDetails)
    }

    private fun returnFakeLogoutResponse(): LogoutResponse {
        return LogoutResponse("logged out", 200)
    }

    private fun returnFakeSmsLoginResponse(): SmsLoginResponse? {
        return SmsLoginResponse(
            email = "email",
            token = "token",
            userId = 1,
            username = "username",
            refreshToken = "refreshToken"
        )
    }

//    private fun returnFakeRetrievePlannedActivities(): List<ActivityDTO> {
//        val configuration = ConfigurationDTO(
//            listOf(),
//            mapOf()
//        )
//        val questionnaire = QuestionnaireDTO(
//            1,
//            1,
//            "pre_questionnaire",
//            ConfigurationDTO(listOf(), mapOf())
//        )
//        val activityTemplate = ActivityTemplateDTO(
//            1,
//            "test",
//            1,
//            1,
//            1,
//            TemplateConfigurationDTO(soilPhotoRequired = false, "small_big_tree"),
//            1,
//            listOf()
//        )
//
//        val plot = ActivityPlotDTO(
//            1,
//            2,
//            "test",
//            1,
//            "1",
//            "1",
//        )
//
//        val config = ActivityConfigurationDTO(
//            listOf()
//        )
//
//        return listOf(
//            ActivityDTO(
//                0,
//                "",
//                false,
//                mapOf(),
//                mapOf(),
//                config,
//                "",
//                "",
//                "",
//                null
//                plot,
//                activityTemplate
//            )
//        )
//    }

    override suspend fun logout(token: String): LogoutResponse? {
        return returnFakeLogoutResponse()
    }

    override suspend fun postDeviceData(deviceInformation: DeviceInformation, userToken: String) {
        TODO("Not yet implemented")
    }

    override suspend fun validatePhoneNumber(phoneNumber: String): ValidateResponseData? {
        return returnFakeValidatePhoneNumberResponse()
    }

    override suspend fun registerMobileUser(mobileUser: RegisterMobileUser): RegisteredMobileUser? {
        return returnFakeRegisteredMobileUser()
    }

    override suspend fun validateOTPRegistration(validateOTPRegistration: ValidateOTPRegistration): ValidateOTPRegistrationResponse? {
        return returnFakeOTPRegistrationResponse()
    }

    override suspend fun requestOTP(phoneNumber: RequestOTP): String? {
        return returnFakeOTP()
    }

    override suspend fun loginWithOTP(loginWithOTP: LoginWithOTP): SmsLoginResponse? {
        return returnFakeSmsLoginResponse()
    }

    override suspend fun retrievePlannedActivities(): List<ActivityDTO> {
//        return returnFakeRetrievePlannedActivities()
        TODO("Not yet implemented")
    }

    override suspend fun getBasicUserInfo(): BasicUserInfo {
        TODO("Not yet implemented")
    }
}
