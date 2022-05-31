package org.treeo.treeo.ui.authentication

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.treeo.treeo.MainCoroutineRule
import org.treeo.treeo.models.NewRegisteredUser
import org.treeo.treeo.models.RegisterMobileUser
import org.treeo.treeo.models.RegisterUser
import org.treeo.treeo.models.ValidateOTPRegistration
import org.treeo.treeo.repositories.FakeMainRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RegistrationViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        viewModel = AuthViewModel(
            FakeMainRepository(),
            mainCoroutineRule.testDispatcherProvider
        )
    }

    @Test
    fun `test createUser returns NewRegisteredUser`() =
        mainCoroutineRule.testDispatcher.runBlockingTest {
            val user = RegisterUser(
                "firstName",
                "lastName",
                "secret",
                "test@gmail.com",
                "Uganda",
                "test-user",
                "99999"
            )

            val expectedUser = NewRegisteredUser(
                "test@gmail.com",
                "firstName",
                false,
                "lastName"
            )

            viewModel.createUser(user)
            assertThat(viewModel.newUser.value?.email).isEqualTo(expectedUser.email)
        }


    @Test
    fun `test validatePhoneNumber_Registration returns ValidateResponseData`() =
        mainCoroutineRule.testDispatcher.runBlockingTest {
            val expectedNumber = "111"
            viewModel.validatePhoneNumberRegistration(expectedNumber)
            assertThat(viewModel.phoneNumberValidationResponseRegistration.value?.phoneNumber)
                .isEqualTo(expectedNumber)
        }

    @Test
    fun `test registerMobileUser returns RegisteredMobileUser`() =
        mainCoroutineRule.testDispatcher.runBlockingTest {
            val mobileUser = RegisterMobileUser(
                firstName = "firstname",
                lastName = "lastname",
                country = "Uganda",
                password = "password",
                phoneNumber = "123",
                username = "username"
            )
            viewModel.registerMobileUser(
                mobileUser
            )
            assertThat(viewModel.registeredMobileUser.value?.firstName)
                .isEqualTo(mobileUser.firstName)
        }

    @Test
    fun `test validateOTPRegistration returns ValidateOTPRegistrationResponse`() =
        mainCoroutineRule.testDispatcher.runBlockingTest {
            val validateOTPRegistration = ValidateOTPRegistration(
                code = "123",
                phoneNumber = "123"
            )
            viewModel.validateOTPRegistration(
                validateOTPRegistration
            )
            assertThat(viewModel.validateOTPRegistrationResponse.value?.token)
                .isEqualTo("thisisatesttoken")
        }
}

