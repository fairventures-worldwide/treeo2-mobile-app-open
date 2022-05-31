package org.treeo.treeo.ui.authentication.registration.screens

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_user_activation.*
import org.treeo.treeo.R
import org.treeo.treeo.models.UserProject
import org.treeo.treeo.models.ValidateOTPRegistration
import org.treeo.treeo.ui.authentication.AuthViewModel
import org.treeo.treeo.util.closeKeyboard
import org.treeo.treeo.util.errors
import org.treeo.treeo.util.hideView
import org.treeo.treeo.util.showView
import javax.inject.Inject

@AndroidEntryPoint
class UserActivationFragment : Fragment() {

    @Inject
    lateinit var preferences: SharedPreferences

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_activation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
        setObservers(view)
    }

    private fun initializeViews() {
        initializeTextView()
        initializeButtons()
        initializeOtpView()
    }

    private fun initializeTextView() {
        viewModel.phoneNumber.observe(viewLifecycleOwner, Observer {
            val bannerText = resources.getString(R.string.otp_pass_code_message) + " " + it
            userActivationScreenMessageTextView.text = bannerText
        })
    }

    private fun initializeButtons() {
        userActivationBackButton.setOnClickListener {
            viewModel.registrationBack()
        }
    }

    private fun initializeOtpView() {
        userActivationOtpView.setOtpCompletionListener {
            showView(userActivationOtpProgressBar)
            hideView(userActivationOtpWaitProgressBar)
            closeKeyboard(userActivationOtpView, requireContext())
            viewModel.validateOTPRegistration(
                ValidateOTPRegistration(
                    it,
                    viewModel.getNewUserObj().phoneNumber
                )
            )


        }
    }

    private fun setObservers(view: View) {
        viewModel.registeredMobileUser.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                saveUserInfoToPrefs(it.firstName, it.id)
            }
        })


        viewModel.validateOTPRegistrationResponse.observe(viewLifecycleOwner, Observer {
            hideView(userActivationOtpProgressBar)
            if (it != null) {
                saveAuthToken(it.token)
                    viewModel.userProjectId.value?.let { it1 ->
                        run {
                            viewModel.saveUserProject(UserProject(it.userId, it1))
                        }
                            view.findNavController().popBackStack(R.id.homeFragment, false)

                    }
                    view.findNavController().popBackStack(R.id.homeFragment, false)

            } else {
                Toast.makeText(context, errors.value, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveUserInfoToPrefs(firstName: String, userId: Int) {
        with(preferences.edit()) {
            putBoolean(getString(org.treeo.treeo.R.string.first_time_user), false)
            putString("firstName", firstName)
            putInt(getString(R.string.user_id), userId)
            apply()
        }
    }

    private fun saveAuthToken(token: String) {
        with(preferences.edit()) {
            putString(getString(R.string.user_token), token)
            apply()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = UserActivationFragment()
    }
}
