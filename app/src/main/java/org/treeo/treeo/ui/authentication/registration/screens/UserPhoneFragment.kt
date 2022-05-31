package org.treeo.treeo.ui.authentication.registration.screens

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_user_phone.*
import org.treeo.treeo.R
import org.treeo.treeo.adapters.CountrySpinnerAdapter
import org.treeo.treeo.models.Country
import org.treeo.treeo.models.RequestOTP
import org.treeo.treeo.ui.authentication.AuthViewModel
import org.treeo.treeo.ui.authentication.GDPRFragment
import org.treeo.treeo.util.*
import javax.inject.Inject

@AndroidEntryPoint
class UserPhoneFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    val countryCode = MutableLiveData<String>()
    private var phoneNumber = ""
    var country = ""
    private var gdprConsent = MutableLiveData<Boolean>()

    @Inject
    lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_phone, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
        setObservers()
    }

    private fun initializeViews() {
        initializeButtons()
        initializeSpinner()
        initializeEditText()
        setUpGDPRConsent()
    }

    private fun initializeSpinner() {
        val arrayAdapter = CountrySpinnerAdapter(
            requireContext(),
            R.layout.country_spinner_item,
            getCountries()
        )
        userPhoneCountrySpinner.adapter = arrayAdapter
        userPhoneCountrySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    val country: Country = parent?.getItemAtPosition(position) as Country
                    countryCode.postValue(country.code)
                    setUserCountry(country.code)
                    viewModel.setCountryName(country.country)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
    }

    private fun setUserCountry(countryCode: String) {
        when (countryCode) {
            "+256" -> {
                country = "Uganda"
            }
            "+62" -> {
                country = "Indonesia"
            }
            "+420" -> {
                country = "Czech Republic"
            }
            "+49" -> {
                country = "Germany"
            }
            "+" -> {
                country = "Other"
            }
        }
    }

    private fun initializeEditText() {
        userPhoneInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePhoneNumber(countryCode.value + s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                updatePhoneNumber(countryCode.value + s.toString())
            }
        })
    }

    private fun updatePhoneNumber(phoneNumber: String) {
        this.phoneNumber = phoneNumber
        if (isNumberLengthValid(phoneNumber)) {
            showView(userPhoneProgressBar)
            viewModel.validatePhoneNumberRegistration(
                phoneNumber.removePrefix("+")
            )
        } else {
            disableView(userPhoneContinueButton)
        }
    }

    private fun setUpGDPRConsent() {
        text_gdpr.setOnClickListener {
            GDPRFragment.display(childFragmentManager)
        }
    }

    private fun initializeButtons() {
        userPhoneContinueButton.setOnClickListener {
            viewModel.setUserPhoneNumber(phoneNumber, country)
            viewModel.setGDPRStatus(gdprConsent.value ?: true)
            viewModel.useInfoRegistrationContinue()
        }

        userPhoneBackButton.setOnClickListener {
            viewModel.registrationBack()
        }
    }

    private fun setObservers() {
        countryCode.observe(viewLifecycleOwner) {
            if (it != null) {
                userPhoneTextInputLayout.prefixText = countryCode.value
            }
        }

        viewModel.phoneNumberValidationResponseRegistration.observe(
            viewLifecycleOwner
        ) {
            hideView(userPhoneProgressBar)

            when {
                it == null -> {
                    requireContext().showToast(getString(R.string.error_could_not_validate_phone))
                }
                it.valid -> {
                    setFirstTimeUserStatus()
                    viewModel.setPhoneNumber(phoneNumber)
                    viewModel.updateRegistrationStep(1)
                    viewModel.requestOTP(RequestOTP(phoneNumber))
                    disableView(userPhoneContinueButton)
                }
                else -> {
                    enableView(userPhoneContinueButton)
                }
            }
        }
    }

    private fun setFirstTimeUserStatus() {
        with(sharedPref.edit()) {
            putBoolean(getString(R.string.first_time_user), false)
            apply()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = UserPhoneFragment()
    }
}
