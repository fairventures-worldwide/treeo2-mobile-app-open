package org.treeo.treeo.ui.authentication.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import kotlinx.android.synthetic.main.fragment_on_boarding_screen2.*
import org.treeo.treeo.R
import org.treeo.treeo.ui.authentication.AuthViewModel

class OnBoardingScreen2 : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_on_boarding_screen2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
    }

    private fun initializeViews() {
        onBoardingScreen2ContinueButton.setOnClickListener {
            viewModel.onBoardingContinue()
        }

        onBoardingScreen2BackButton.setOnClickListener {
            viewModel.onBoardingBack()
        }
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = OnBoardingScreen2()
    }
}