package org.treeo.treeo.ui.authentication.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_on_boarding_screen1.*
import org.treeo.treeo.R
import org.treeo.treeo.ui.authentication.AuthViewModel

@AndroidEntryPoint
class OnBoardingScreen1 : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_on_boarding_screen1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.resetOnBoardingStep()
        initializeViews(view)
    }

    private fun initializeViews(view: View) {
        onBoardingScreen1ContinueButton.setOnClickListener {
            viewModel.onBoardingContinue()
        }

        onBoardingScreen1BackButton.setOnClickListener {
            viewModel.resetOnBoardingStep()
            view.findNavController()
                .navigate(R.id.action_onBoardingHostFragment_to_onBoardingLandingPage)
        }
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = OnBoardingScreen1()
    }
}
