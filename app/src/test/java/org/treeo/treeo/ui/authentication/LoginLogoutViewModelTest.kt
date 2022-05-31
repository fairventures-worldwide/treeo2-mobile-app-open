package org.treeo.treeo.ui.authentication

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.treeo.treeo.MainCoroutineRule
import org.treeo.treeo.models.LoginDetails
import org.treeo.treeo.models.LoginWithOTP
import org.treeo.treeo.models.LogoutResponse
import org.treeo.treeo.models.RequestOTP
import org.treeo.treeo.repositories.FakeMainRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class LoginLogoutViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        viewModel = AuthViewModel(
            FakeMainRepository(),
            mainCoroutineRule.testDispatcherProvider
        )
    }

    @Test
    fun `test login`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val loginDetails = LoginDetails("test@gmail.com", "secret")
        val expectedToken = "thisisatesttoken"
        viewModel.login(loginDetails.email, loginDetails.password)
        assertThat(viewModel.loginToken.value?.token).isEqualTo(expectedToken)
    }

    @Test
    fun `test logout`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val expectedResponse = LogoutResponse("logged out", 200)
        viewModel.logout("thisisanauthtoken")
        assertThat(viewModel.logoutResponse.value?.message).isEqualTo(expectedResponse.message)
    }

    @Test
    fun `test requestOTP`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val requestOTP = RequestOTP(
            "+123"
        )
        viewModel.requestOTP(requestOTP)
        assertThat(viewModel.phoneNumberOTPResponse.value).isEqualTo("OTP Sent")
    }

    @Test
    fun `test loginWithOTP`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val loginWithOTP = LoginWithOTP(
            "+123",
            "123"
        )
        viewModel.loginWithOTP(loginWithOTP)
        assertThat(viewModel.smsLoginResponse.value?.token).isEqualTo("token")
    }

}
