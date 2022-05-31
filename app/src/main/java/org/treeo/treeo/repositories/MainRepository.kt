package org.treeo.treeo.repositories

import android.content.SharedPreferences
import org.treeo.treeo.models.*
import org.treeo.treeo.network.RequestManager
import org.treeo.treeo.network.models.ActivityDTO
import org.treeo.treeo.util.USER_NAME
import org.treeo.treeo.util.USER_PHONE_NUMBER
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val requestManager: RequestManager,
    private val preferences: SharedPreferences
) : IMainRepository {
    override suspend fun createUser(registerUser: RegisterUser) =
        requestManager.createUser(registerUser)?.apply {
            saveNameAndPhoneNumber(firstName, registerUser.phoneNumber)
        }

    private fun saveNameAndPhoneNumber(name: String, phoneNumber: String) {
        with(preferences.edit()) {
            putString(USER_NAME, name)
            putString(USER_PHONE_NUMBER, phoneNumber)
            apply()
        }
    }

    override suspend fun login(loginDetails: LoginDetails) =
        requestManager.login(loginDetails)

    override suspend fun logout(token: String) =
        requestManager.logout(token)

    override  suspend fun getCountryProjects(countryName: String) = requestManager.getAvailableTreeProjects(countryName)

    override suspend fun saveUserProject(
        userProject: UserProject
    ) = requestManager.saveUserProject(userProject)

    override suspend fun postDeviceData(
        deviceInformation: DeviceInformation,
        userToken: String
    ) = requestManager.postDeviceData(deviceInformation, userToken)

    override suspend fun validatePhoneNumber(phoneNumber: String) =
        requestManager.validatePhoneNumber(phoneNumber)

    override suspend fun requestOTP(phoneNumber: RequestOTP): String? =
        requestManager.requestOTP(phoneNumber)

    override suspend fun registerMobileUser(mobileUser: RegisterMobileUser) =
        requestManager.registerMobileUser(mobileUser).apply {
            saveNameAndPhoneNumber("${mobileUser.firstName} ${mobileUser.lastName}", mobileUser.phoneNumber)
        }

    override suspend fun validateOTPRegistration(validateOTPRegistration: ValidateOTPRegistration) =
        requestManager.validateOTPRegistration(
            validateOTPRegistration
        )

    override suspend fun loginWithOTP(loginWithOTP: LoginWithOTP): SmsLoginResponse? =
        requestManager.loginWithOTP(loginWithOTP).apply {
            saveNameAndPhoneNumber(
                this?.username ?: "",
                loginWithOTP.phoneNumber
            )
        }

    override suspend fun retrievePlannedActivities(): List<ActivityDTO> =
        requestManager.retrievePlannedActivities()

    override suspend fun getBasicUserInfo(): BasicUserInfo {
        // Note: Info should be pulled from the backend if not in SharedPrefs
        val username = preferences.getUsername()
        val phoneNumber = preferences.getString(USER_PHONE_NUMBER, "")
        return BasicUserInfo(username, phoneNumber ?: "")
    }

    private fun SharedPreferences.getUsername(): String {
        val username = getString(USER_NAME, null)
        return if (username?.contains(".") == true) {
            val nameParts = username.split(".")
            var lastName = ""
            for (x in nameParts[1]) {
                if (x.isDigit()) {
                    break
                } else {
                    lastName += x
                }
            }
            "${nameParts[0]} $lastName"
        } else {
            username ?: ""
        }
    }
}
