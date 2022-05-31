package org.treeo.treeo.ui.authentication.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.fragment_motivations.*
import org.treeo.treeo.R
import org.treeo.treeo.ui.authentication.AuthViewModel
import org.treeo.treeo.util.disableView
import org.treeo.treeo.util.enableView

class MotivationsFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_motivations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setObservers()
    }

    private fun initializeViews(view: View) {
        initializeButtons(view)
        initializeCheckboxes()
    }

    private fun initializeButtons(view: View) {
        motivationsContinueButton.setOnClickListener {
            view.findNavController()
                .navigate(R.id.action_motivationsFragment_to_registrationHostFragment)
        }

        motivationsBackButton.setOnClickListener {
            view.findNavController()
                .navigate(R.id.action_motivationsFragment_to_onBoardingHostFragment)
        }
    }

    private fun initializeCheckboxes() {
        checkBox1.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                viewModel.addMotivation(buttonView.text.toString())
            } else {
                viewModel.removeMotivation(buttonView.text.toString())
            }
        }
        checkBox2.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                viewModel.addMotivation(buttonView.text.toString())
            } else {
                viewModel.removeMotivation(buttonView.text.toString())
            }
        }
        checkBox3.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                viewModel.addMotivation(buttonView.text.toString())
            } else {
                viewModel.removeMotivation(buttonView.text.toString())
            }
        }
        checkBox4.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                viewModel.addMotivation(buttonView.text.toString())
            } else {
                viewModel.removeMotivation(buttonView.text.toString())
            }
        }
    }

    private fun setObservers() {
        viewModel.motivationList.observe(viewLifecycleOwner) {
            if (it.size > 0) {
                enableView(motivationsContinueButton)
            } else {
                disableView(motivationsContinueButton)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = MotivationsFragment()
    }
}
