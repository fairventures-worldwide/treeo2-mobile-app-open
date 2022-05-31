package org.treeo.treeo.ui.authentication.onboarding.screens

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_choose_lang.*
import org.treeo.treeo.R
import org.treeo.treeo.ui.authentication.AuthViewModel
import org.treeo.treeo.util.showToast
import javax.inject.Inject

@AndroidEntryPoint
class ChooseLangFragment : Fragment() {
    @Inject
    lateinit var sharedPref: SharedPreferences

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_choose_lang, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onRadioButtonClicked()
        initializeButton()
    }

    private fun onRadioButtonClicked() {
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_english -> {
                    viewModel.setLocaleLanguage("en")
                }
                R.id.radio_luganda -> {
                    viewModel.setLocaleLanguage("lg")
                }
                R.id.radio_bahasa -> {
                    viewModel.setLocaleLanguage("in")
                }
                R.id.radio_spanish -> {
                    viewModel.setLocaleLanguage("es")
                }
            }
        }
    }

    private fun initializeButton() {
        chooseLangContinueButton.setOnClickListener {
            val selectedLanguage = viewModel.localeLanguage.value
            if (selectedLanguage.isNullOrBlank()) {
                requireContext().showToast(getString(R.string.prompt_select_language))
            } else {
                with(sharedPref.edit()) {
                    putString(getString(R.string.selected_language), selectedLanguage)
                    apply()
                }
                this.findNavController().navigate(
                    R.id.action_chooseLangFragment_to_onBoardingScreen1
                )
            }
        }
    }
}
