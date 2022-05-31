package org.treeo.treeo.ui.authentication.registration.screens

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.fragment_user_info.*
import org.treeo.treeo.R
import org.treeo.treeo.models.RequestOTP
import org.treeo.treeo.ui.authentication.AuthViewModel
import org.treeo.treeo.util.disableView
import org.treeo.treeo.util.enableView


class UserInfoFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    private var firstName = ""
    private var lastName = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
    }

    private fun initializeViews(view: View) {
        initializeButtons(view)
        initializeInputs()
    }

    private fun initializeButtons(view: View) {
        userInfoContinueButton.setOnClickListener {
            viewModel.setUserInformation(firstName, lastName)
            viewModel.requestOTP(RequestOTP(viewModel.phoneNumber.value.toString()))
            viewModel.updateRegistrationStep(3)
        }

        userInfoBackButton.setOnClickListener {
            viewModel.resetRegistrationStep()
            view.findNavController()
                .navigate(R.id.action_registrationHostFragment_to_motivationsFragment)
        }
    }

    private fun initializeInputs() {
        userInfoFirstNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                firstName = s.toString()
                checkInformation()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        userInfoLastNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                lastName = s.toString()
                checkInformation()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun checkInformation() {
        if (firstName.trim().isNotEmpty() && lastName.trim().isNotEmpty()) {
            enableView(userInfoContinueButton)
        } else {
            disableView(userInfoContinueButton)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = UserInfoFragment()
    }
}
