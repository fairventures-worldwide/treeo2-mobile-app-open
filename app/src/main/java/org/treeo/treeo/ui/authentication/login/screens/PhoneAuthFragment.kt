package org.treeo.treeo.ui.authentication.login.screens

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_phone_auth.*
import org.treeo.treeo.R
import org.treeo.treeo.adapters.CountrySpinnerAdapter
import org.treeo.treeo.models.Country
import org.treeo.treeo.models.RequestOTP
import org.treeo.treeo.ui.authentication.AuthViewModel
import org.treeo.treeo.util.*

@AndroidEntryPoint
class PhoneAuthFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    val countryCode = MutableLiveData<String>()
    val phoneNumber = MutableLiveData<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_phone_auth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
    }

    private fun initializeViews() {
        createSpinner()
        onTextChangeWatcher()
        setUpButton()
        setObservers()
    }

    private fun setUpButton() {
        phoneLoginContinueButton.setOnClickListener {
            viewModel.loginContinue()
            viewModel.setPhoneNumber(phoneNumber.value.toString())
            viewModel.requestOTP(RequestOTP(phoneNumber.value.toString()))
        }
    }

    private fun createSpinner() {
        val arrayAdapter = CountrySpinnerAdapter(
            requireContext(),
            R.layout.country_spinner_item,
            getCountries()
        )
        countrySpinner.adapter = arrayAdapter
        countrySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val country: Country = parent?.getItemAtPosition(position) as Country
                countryCode.postValue(country.code)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    private fun onTextChangeWatcher() {
        phoneInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val phone = countryCode.value + s.toString()
                phoneNumber.postValue(phone)
            }

            override fun afterTextChanged(s: Editable?) {
                val phone = countryCode.value + s.toString()
                phoneNumber.postValue(phone)
            }
        })
    }

    private fun setObservers() {
        countryCode.observe(viewLifecycleOwner, {
            if (it != null) {
                phoneTextInputLayout.prefixText = countryCode.value
            }
        })

        phoneNumber.observe(viewLifecycleOwner, {
            if (it != null && isNumberLengthValid(it)) {
                enableView(phoneLoginContinueButton)
            } else {
                disableView(phoneLoginContinueButton)
            }
        })

        viewModel.phoneNumberOTPResponse.observe(viewLifecycleOwner, {
            if (it != null) {
                enableView(phoneLoginContinueButton)
                disableView(checkNumberProgressBar)
            } else {
                disableView(checkNumberProgressBar)
                Toast.makeText(requireContext(), errors.value, Toast.LENGTH_LONG).show()
            }
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() = PhoneAuthFragment()
    }
}
