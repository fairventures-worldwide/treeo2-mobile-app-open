package org.treeo.treeo.network

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.treeo.treeo.enqueueResponse
import org.treeo.treeo.models.LoginWithOTP
import org.treeo.treeo.models.RegisterMobileUser
import org.treeo.treeo.models.RequestOTP
import org.treeo.treeo.models.ValidateOTPRegistration
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class RequestManagerTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val mockWebServer = MockWebServer()
    private val testPhoneNumber = "+123000000000"

    private lateinit var requestManager: RequestManager
    private lateinit var retrofitInstance: ApiService
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        mockWebServer.start(8080)

        client = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .build()

        retrofitInstance = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/").toString())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        requestManager = RequestManager(retrofitInstance)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `test logout success`() = runBlocking {
        mockWebServer.enqueueResponse("logout_response.json", 200)
        val accessToken = "thisisatesttoken"

        val response = requestManager.logout(accessToken)
        assertThat(response?.message).isEqualTo("logged out")
    }

    @Test
    fun `test validatePhoneNumber success`() = runBlocking {
        mockWebServer.enqueueResponse("validate_phone_number_response.json", 200)
        val response = requestManager.validatePhoneNumber(testPhoneNumber)
        assertThat(response?.valid).isTrue()
    }

    @Test
    fun `test registerMobileUser success`() = runBlocking {
        mockWebServer.enqueueResponse("register_mobile_user_response.json", 201)
        val mobileUser = RegisterMobileUser(
            firstName = "firstname",
            lastName = "lastname",
            country = "Uganda",
            password = "password",
            phoneNumber = testPhoneNumber,
            username = "username"
        )
        val response = requestManager.registerMobileUser(mobileUser)
        assertThat(response?.firstName).isEqualTo("firstname")
    }

    @Test
    fun `test validateOTPRegistration success`() = runBlocking {
        mockWebServer.enqueueResponse("validate_otp_registration_response.json", 200)
        val validateOTPRegistration = ValidateOTPRegistration(
            code = "1234",
            phoneNumber = testPhoneNumber
        )
        val response = requestManager.validateOTPRegistration(validateOTPRegistration)
        assertThat(response?.token).isEqualTo("thisisatesttoken")
    }

    @Test
    fun `test requestOTP success`() = runBlocking {
        mockWebServer.enqueueResponse("request_otp_response.json", 200)
        val requestOTP = RequestOTP(
            testPhoneNumber
        )
        val response = requestManager.requestOTP(requestOTP)
        assertThat(response).isEqualTo("OTP sent successfully")
    }

    @Test
    fun `test loginWithOTP success`() = runBlocking {
        mockWebServer.enqueueResponse("login_with_otp_response.json", 200)
        val loginWithOTP = LoginWithOTP(
            testPhoneNumber,
            "123"
        )
        val response = requestManager.loginWithOTP(loginWithOTP)
        assertThat(response?.token).isEqualTo("this_is_a_dummy_token")
    }

}
