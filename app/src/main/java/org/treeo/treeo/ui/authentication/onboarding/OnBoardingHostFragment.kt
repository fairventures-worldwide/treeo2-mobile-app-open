package org.treeo.treeo.ui.authentication.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import com.shuhart.stepview.StepView
import dagger.hilt.android.AndroidEntryPoint
import org.treeo.treeo.R
import org.treeo.treeo.adapters.ViewPagerAdapter
import org.treeo.treeo.ui.authentication.AuthViewModel
import org.treeo.treeo.ui.authentication.onboarding.screens.OnBoardingScreen1
import org.treeo.treeo.ui.authentication.onboarding.screens.OnBoardingScreen2
import org.treeo.treeo.ui.authentication.onboarding.screens.OnBoardingScreen3
import org.treeo.treeo.ui.authentication.onboarding.screens.OnBoardingScreen4

@AndroidEntryPoint
class OnBoardingHostFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    private lateinit var viewPager: ViewPager2
    private lateinit var stepView: StepView

    private lateinit var pagerAdapter: ViewPagerAdapter

    private val fragments = listOf(
        OnBoardingScreen1(),
        OnBoardingScreen2(),
        OnBoardingScreen3(),
        OnBoardingScreen4()
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_on_boarding_host, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setObservers()
    }

    private fun initializeViews(view: View) {
        setUpViewPager(view)
    }

    private fun setUpViewPager(view: View) {
        pagerAdapter = ViewPagerAdapter(fragments, this)
        viewPager = view.findViewById(R.id.onBoardingViewPager)
        viewPager.isUserInputEnabled = false
        viewPager.adapter = pagerAdapter
        setUpViewPagerIndicator(view)
    }

    private fun setUpViewPagerIndicator(view: View) {
        stepView = view.findViewById(R.id.onBoardingIndicatorView)
        stepView.state
            .animationType(StepView.ANIMATION_LINE)
            .stepsNumber(fragments.size)
            .animationDuration(resources.getInteger(android.R.integer.config_shortAnimTime))
            .commit()
    }

    private fun setObservers() {
        viewModel.onBoardingStep.observe(viewLifecycleOwner, Observer {
            viewPager.setCurrentItem(it, true)
            stepView.go(it, true)
        })
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = OnBoardingHostFragment()
    }
}
