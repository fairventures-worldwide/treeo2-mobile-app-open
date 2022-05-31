package org.treeo.treeo.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.treeo.treeo.models.*
import org.treeo.treeo.network.models.ActivityDTO
import org.treeo.treeo.network.models.ActivityUploadDTO
import org.treeo.treeo.network.models.MeasurementDTO
import org.treeo.treeo.network.models.TreeSpeciesDTO
import org.treeo.treeo.util.errors
import org.treeo.treeo.util.getErrorMessageFromJson
import javax.inject.Inject

open class RequestManager @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun createUser(registerUser: RegisterUser): NewRegisteredUser? {
        var items: NewRegisteredUser? = null
        try {
            val response = apiService.createUser(registerUser)

            if (response.isSuccessful) {
                items = response.body()
            } else {
                val errorResponse =
                    response.errorBody()!!.charStream().readText()
                errors.postValue(getErrorMessageFromJson(errorResponse))
            }
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return items
    }

    suspend fun login(loginDetails: LoginDetails): LoginResponse? {
        var items: LoginResponse? = null
        try {
            val response = apiService.login(loginDetails)
            if (response.isSuccessful) {
                items = response.body()!!.data
            } else {
                val jsonResponse =
                    response.errorBody()!!.charStream().readText()
                errors.postValue(getErrorMessageFromJson(jsonResponse))
            }
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return items
    }

    suspend fun logout(token: String): LogoutResponse? {
        var items: LogoutResponse? = null
        try {
            val response = apiService.logOut(token)
            if (response.isSuccessful) {
                items = response.body()
            } else {
                val jsonResponse =
                    response.errorBody()!!.charStream().readText()
                errors.postValue(getErrorMessageFromJson(jsonResponse))
            }
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return items
    }

    suspend fun saveUserProject(
        userProject: UserProject
    ) {
        try {
            val response = apiService.saveUserToProject(userProject)
            if (!response.isSuccessful) {
                val jsonResponse =
                    response.errorBody()!!.charStream().readText()
                errors.postValue(getErrorMessageFromJson(jsonResponse))
            }
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
    }

    suspend fun postDeviceData(
        deviceInformation: DeviceInformation,
        userToken: String
    ) {
        try {
            val response = apiService.postDeviceInfo(deviceInformation)
            if (!response.isSuccessful) {
                val jsonResponse =
                    response.errorBody()!!.charStream().readText()
                errors.postValue(getErrorMessageFromJson(jsonResponse))
            }
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
    }

    suspend fun getAvailableTreeProjects(countryName: String): List<TreeoProject> {
        var projects = listOf<TreeoProject>()
        try {
            val response = apiService.retrieveTreeoOrganizations(countryName)
            if (response.isSuccessful) {
                projects = response.body()!!.data
            } else {
                val jsonResponse =
                    response.errorBody()!!.charStream().readText()
                errors.postValue(getErrorMessageFromJson(jsonResponse))
            }
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return projects;
    }


    suspend fun validatePhoneNumber(phoneNumber: String): ValidateResponseData? {
        var items: ValidateResponseData? = null
        try {
            val response = apiService.validatePhoneNumber(phoneNumber)

            if (response.isSuccessful) {
                items = response.body()?.data
            } else {
                if (response.code() == 404) {
                    items = ValidateResponseData(
                        errorStatus = "",
                        phoneNumber = "",
                        valid = false
                    )
                }
                val jsonResponse =
                    response.errorBody()!!.charStream().readText()
                errors.postValue(getErrorMessageFromJson(jsonResponse))
            }
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return items
    }

    suspend fun requestOTP(phoneNumber: RequestOTP): String? {
        var items: String? = null
        try {
            val response = apiService.requestOTP(phoneNumber)

            if (response.isSuccessful) {
                items = response.body()?.message
            } else {
                val jsonResponse =
                    response.errorBody()!!.charStream().readText()
                errors.postValue(getErrorMessageFromJson(jsonResponse))
            }
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return items
    }


    suspend fun registerMobileUser(mobileUser: RegisterMobileUser): RegisteredMobileUser? {
        var items: RegisteredMobileUser? = null
        try {
            val response = apiService.registerMobileUser(mobileUser)

            if (response.isSuccessful) {
                items = response.body()
            } else {
                val jsonResponse =
                    response.errorBody()!!.charStream().readText()
                errors.postValue(getErrorMessageFromJson(jsonResponse))
            }
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return items
    }

    suspend fun validateOTPRegistration(
        validateOTPRegistration: ValidateOTPRegistration
    ): ValidateOTPRegistrationResponse? {
        var items: ValidateOTPRegistrationResponse? = null
        try {
            val response =
                apiService.validateOTPRegistration(validateOTPRegistration)

            if (response.isSuccessful) {
                items = response.body()?.data
            } else {
                val jsonResponse =
                    response.errorBody()!!.charStream().readText()
                errors.postValue(getErrorMessageFromJson(jsonResponse))
            }
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return items
    }

    suspend fun loginWithOTP(loginWithOTP: LoginWithOTP): SmsLoginResponse? {
        var items: SmsLoginResponse? = null
        try {
            val response = apiService.loginWitOTP(loginWithOTP)

            if (response.isSuccessful) {
                items = response.body()?.data
            } else {
                val jsonResponse =
                    response.errorBody()!!.charStream().readText()
                errors.postValue(getErrorMessageFromJson(jsonResponse))
            }
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return items
    }

    suspend fun retrievePlannedActivities(): List<ActivityDTO> {
        var activities = listOf<ActivityDTO>()
        try {
            val response = apiService.retrievePlannedActivities()
            if (response.isSuccessful) {
                activities = response.body()!!.plannedActivities
            } else {
                val jsonResponse =
                    response.errorBody()!!.charStream().readText()
                errors.postValue(getErrorMessageFromJson(jsonResponse))
            }
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return activities
    }

    suspend fun createActivity(
        dataMap: Map<String, ActivityUploadDTO>
    ): Int {
        var code = 0
        try {
            val response = apiService.createActivity(dataMap)
            code = response.code()
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return code
    }

    suspend fun uploadMeasurementData(measurement: MeasurementDTO): Int {
        var code = 0
        try {
            val response = apiService.uploadMeasurementData(measurement)
            code = response.code()
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return code
    }

    suspend fun uploadBinaryData(
        file: MultipartBody.Part,
        measurementId: RequestBody
    ): Int {
        var code = 0
        try {
            val response = apiService.uploadBinaryData(file, measurementId)
            code = response.code()
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return code
    }

    suspend fun getTreeSpecies(): List<TreeSpeciesDTO.Specie> {
        var treeSpecies = listOf<TreeSpeciesDTO.Specie>()
        try {
            val response = apiService.getTreeSpecies()
            if (response.isSuccessful) {
                treeSpecies = response.body()!!.rows
            } else {
                val jsonResponse =
                    response.errorBody()!!.charStream().readText()
                errors.postValue(getErrorMessageFromJson(jsonResponse))
            }
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return treeSpecies
    }
}
