package org.treeo.treeo.ui.authentication.registration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.shuhart.stepview.StepView
import kotlinx.android.synthetic.main.fragment_tree_measurement_camera.*
import org.treeo.treeo.R
import org.treeo.treeo.adapters.MainPagerAdapter
import org.treeo.treeo.ui.authentication.AuthViewModel
import org.treeo.treeo.ui.authentication.login.screens.PhoneOtpFragment
import org.treeo.treeo.ui.authentication.registration.screens.SelectUserProjectFragment
import org.treeo.treeo.ui.authentication.registration.screens.UserActivationFragment
import org.treeo.treeo.ui.authentication.registration.screens.UserInfoFragment
import org.treeo.treeo.ui.authentication.registration.screens.UserPhoneFragment

class RegistrationHostFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    private lateinit var viewPager: ViewPager2
    private lateinit var stepView: StepView

    private lateinit var pagerAdapter: MainPagerAdapter

    private val fragments = listOf(
        UserPhoneFragment.newInstance(),
        PhoneOtpFragment.newInstance(),
        UserInfoFragment.newInstance(),
        SelectUserProjectFragment.newInstance(),
        UserActivationFragment.newInstance()
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_registration_host, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setObservers()

        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.exit_btn -> {
                    view.findNavController().popBackStack(R.id.onBoardingHostFragment, false)
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun initializeViews(view: View) {
        pagerAdapter = MainPagerAdapter(
            fragments,
            requireActivity().supportFragmentManager,
            lifecycle
        )
        viewPager = view.findViewById(R.id.registrationViewPager)
        viewPager.isUserInputEnabled = false
        viewPager.adapter = pagerAdapter
        setUpViewPagerIndicator(view)
    }

    private fun setUpViewPagerIndicator(view: View) {
        stepView = view.findViewById(R.id.registrationIndicatorView)
        stepView.state
            .animationType(StepView.ANIMATION_LINE)
            .stepsNumber(fragments.size)
            .animationDuration(resources.getInteger(android.R.integer.config_shortAnimTime))
            .commit()
    }

    private fun setObservers() {
        viewModel.registrationStep.observe(viewLifecycleOwner) {
            if (it < 0) {
                viewModel.resetRegistrationStep()
                findNavController().popBackStack()
            } else {
                viewPager.currentItem = it
                stepView.go(it, true)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RegistrationHostFragment()
    }
}
