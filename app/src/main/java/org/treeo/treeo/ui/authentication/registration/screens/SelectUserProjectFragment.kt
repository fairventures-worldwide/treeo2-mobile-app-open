package org.treeo.treeo.ui.authentication.registration.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_select_user_project.*
import org.treeo.treeo.R
import org.treeo.treeo.adapters.ProjectsListAdapter
import org.treeo.treeo.models.RequestOTP
import org.treeo.treeo.ui.authentication.AuthViewModel
import org.treeo.treeo.util.disableView
import org.treeo.treeo.util.enableView

class SelectUserProjectFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_user_project, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews()
        setObservers()
    }

    private fun initializeViews() {
        initializeButtons()
        viewModel.countryName.value?.let { viewModel.getProjects(it) }
    }

    private fun initializeButtons() {
        project_btn_continue.setOnClickListener {
            viewModel.registerMobileUser()
            viewModel.requestOTP(RequestOTP(viewModel.phoneNumber.value.toString()))

            viewModel.updateRegistrationStep(4)
        }
    }

    private fun setObservers() {
        viewModel.userProjectId.observe(viewLifecycleOwner) {
            if (it != -1) {
                enableView(project_btn_continue)
            } else {
                disableView(project_btn_continue)
            }
        }

        viewModel.projectsList.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                projectsListProgressBar.visibility = View.VISIBLE
            }

            if (it.isNotEmpty()) {
                projectsListProgressBar.visibility = View.GONE
                val adapter = ProjectsListAdapter(it, viewModel)
                rvOrganizations.adapter = adapter
                rvOrganizations.layoutManager = LinearLayoutManager(context)

            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SelectUserProjectFragment()
    }
}
