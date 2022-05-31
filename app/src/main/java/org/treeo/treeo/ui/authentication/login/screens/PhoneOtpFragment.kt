package org.treeo.treeo.ui.authentication.login.screens

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_phone_otp.*
import org.treeo.treeo.R
import org.treeo.treeo.models.LoginWithOTP
import org.treeo.treeo.models.RequestOTP
import org.treeo.treeo.models.ValidateOTPRegistration
import org.treeo.treeo.ui.authentication.AuthViewModel
import org.treeo.treeo.util.errors
import javax.inject.Inject

@AndroidEntryPoint
class PhoneOtpFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()
    private var phoneNumber = ""

    @Inject
    lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_phone_otp, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        phoneNumber = "${viewModel.phoneNumber.value}"
        setUpViews()
        setObservers()
    }

    private fun setUpViews() {
        setUpButtons()
        setUpOTPView()
        setUpResendTextView()
    }

    private fun setUpButtons() {
        phoneLoginOTPBackButton.setOnClickListener {
            viewModel.loginBack()
        }
    }

    private fun setUpOTPView() {
        otpView.setOtpCompletionListener {
            if (activateThisUser) {
                viewModel.validateOTPRegistration(ValidateOTPRegistration(it, phoneNumber))
            } else {
                viewModel.loginWithOTP(LoginWithOTP(phoneNumber, it))
                showLoginProgressBar()
                enableBackButton()
            }

            hideOTPProgressBar()
            closeKeyboard(otpView)
            disableBackButton()
        }
    }

    private fun setUpResendTextView() {
        resendPassCodeTextView.setOnClickListener {
            showOTPProgressBar()
            otpView.editableText.clear()
            viewModel.requestOTP(RequestOTP(phoneNumber))
        }
    }

    private fun setObservers() {
        viewModel.phoneNumber.observe(viewLifecycleOwner) {
            phoneNumber = it
            val bannerText = resources.getString(R.string.otp_pass_code_message) + " " + phoneNumber
            otpScreenMessageTextView.text = bannerText
        }

        viewModel.validateOTPRegistrationResponse.observe(viewLifecycleOwner) {
            if (activateThisUser) {
                viewModel.userProjectId.value?.let { it1 ->
                    {
                        saveUserDetails(
                            true,
                            it1,
                            it.token,
                            it.userId,
                            it.username,
                            it.refreshToken
                        )
                    }
                }
                activateThisUser = false
                openHome()
            }
        }

        viewModel.smsLoginResponse.observe(viewLifecycleOwner) {
            if (it != null) {
                hideLoginProgressBar()
                saveUserDetails(false, 0, it.token, it.userId, it.username, it.refreshToken)
                enableBackButton()
                if (viewModel.newUser.value == null) {
                    openHome()
                } else {
                    viewModel.updateRegistrationStep(2)
                }
            } else {

                if (errors.value == "User not active") {
                    hideLoginProgressBar()
                    enableBackButton()
                    showResendPasscodeText(true)
                    Toast.makeText(requireContext(), errors.value, Toast.LENGTH_SHORT).show()
                } else {
                    hideLoginProgressBar()
                    enableBackButton()
                    showResendPasscodeText()
                    Toast.makeText(requireContext(), errors.value, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveUserDetails(
        isRegistering: Boolean = false,
        projectId: Int,
        token: String,
        userId: Int,
        username: String,
        refreshToken: String
    ) {
        with(sharedPref.edit()) {
            if (isRegistering) {
                putInt("projectId", projectId)
            }
            putString(getString(R.string.user_token), token)
            putInt(getString(R.string.user_id), userId)
            putString("refresh_token", refreshToken)
            putString("username", username)
            apply()
        }
    }

    private fun openHome() {
        findNavController().popBackStack(R.id.homeFragment, false)
    }

    private fun enableBackButton() {
        phoneLoginOTPBackButton.isEnabled = true
    }

    private fun disableBackButton() {
        phoneLoginOTPBackButton.isEnabled = false
    }

    private fun showOTPProgressBar() {
        otpWaitProgressBar.visibility = View.VISIBLE
    }

    private fun hideOTPProgressBar() {
        otpWaitProgressBar.visibility = View.GONE
    }

    private fun showLoginProgressBar() {
        otpLoginProgressBar.visibility = View.VISIBLE
    }

    private fun hideLoginProgressBar() {
        otpLoginProgressBar.visibility = View.GONE
    }

    private fun showResendPasscodeText(activateUser: Boolean = false) {

        if (activateUser) {
            activateThisUser = true
            resendPassCodeTextView.text = getString(R.string.activate_account_first)
        }
        resendPassCodeTextView.visibility = View.VISIBLE
    }

    private fun closeKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    companion object {
        @JvmStatic
        fun newInstance() = PhoneOtpFragment()
        var activateThisUser: Boolean = false
    }
}
