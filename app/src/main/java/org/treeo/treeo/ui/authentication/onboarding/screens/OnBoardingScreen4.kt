package org.treeo.treeo.ui.authentication.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.fragment_on_boarding_screen4.*
import org.treeo.treeo.R
import org.treeo.treeo.ui.authentication.AuthViewModel

class OnBoardingScreen4 : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_on_boarding_screen4, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
    }

    private fun initializeViews(view: View) {
        onBoardingScreen4ContinueButton.setOnClickListener {
            view.findNavController()
                .navigate(R.id.action_onBoardingHostFragment_to_motivationsFragment)
        }

        onBoardingScreen4BackButton.setOnClickListener {
            viewModel.onBoardingBack()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = OnBoardingScreen4()
    }
}
