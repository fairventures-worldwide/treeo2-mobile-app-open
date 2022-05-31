package org.treeo.treeo.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.treeo.treeo.models.*
import org.treeo.treeo.network.models.ActivityUploadDTO
import org.treeo.treeo.network.models.MeasurementDTO
import org.treeo.treeo.network.models.TreeSpeciesDTO
import retrofit2.Response
import retrofit2.http.*


interface ApiService {
    @POST("users")
    suspend fun createUser(@Body registerUser: RegisterUser): Response<NewRegisteredUser>

    @GET("projects/all/projects")
    suspend fun retrieveTreeoOrganizations(
        @Query("country") countryName: String
    ): Response<ProjectsResponse>

    @POST("user-projects/assign/user")
    suspend fun saveUserToProject(
        @Body userProject: UserProject
    ): Response<Any>

    @POST("auth/google")
    suspend fun googleSignUp(@Body googleAuthToken: GoogleToken): Response<GoogleUser>

    @GET("auth/facebook")
    suspend fun facebookSignUp(@Query("access_token") access_token: String): Response<FacebookUser>

    @POST("auth/login")
    suspend fun login(@Body loginDetails: LoginDetails): Response<BaseResponse<LoginResponse>>

    @GET("auth/refresh")
    suspend fun requestRefreshToken(
        @Header("Authorization") token: String
    ): Response<TokenRefreshResponse>

    @GET("auth/logout")
    suspend fun logOut(@Header("Token") token: String): Response<LogoutResponse>

    @POST("device-info")
    suspend fun postDeviceInfo(
        @Body deviceInformation: DeviceInformation
    ): Response<Any>

    @GET("auth/validate-phonenumber/{phoneNumber}")
    suspend fun validatePhoneNumber(
        @Path(value = "phoneNumber", encoded = true) phoneNumber: String
    ): Response<BaseResponse<ValidateResponseData>>

    @POST("auth/request-otp")
    suspend fun requestOTP(
        @Body phoneNumber: RequestOTP
    ): Response<BaseResponse<String>>

    @POST("users/mobile/register")
    suspend fun registerMobileUser(
        @Body mobileUser: RegisterMobileUser
    ): Response<RegisteredMobileUser>

    @POST("auth/validate-mobile-user")
    suspend fun validateOTPRegistration(
        @Body validateOTPRegistration: ValidateOTPRegistration
    ): Response<BaseResponse<ValidateOTPRegistrationResponse>>

    @POST("auth/verify-otp")
    suspend fun loginWitOTP(
        @Body loginWithOTP: LoginWithOTP
    ): Response<BaseResponse<SmsLoginResponse>>

    @GET("planned-activity/user")
    suspend fun retrievePlannedActivities(
    ): Response<UserActivity>

    @POST("activities")
    suspend fun createActivity(
        @Body dataMap: Map<String, ActivityUploadDTO>
    ): Response<Any>

    @POST("measurements")
    suspend fun uploadMeasurementData(
        @Body measurement: MeasurementDTO
    ): Response<Any>

    @Multipart
    @POST("photos/upload")
    suspend fun uploadBinaryData(
        @Part file: MultipartBody.Part,
        @Part("measurementId") measurementId: RequestBody
    ): Response<Any>

    @GET("tree-species")
    suspend fun getTreeSpecies(): Response<TreeSpeciesDTO>
}
