package org.treeo.treeo.ui.home.screens

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_guide.*
import org.treeo.treeo.R
import org.treeo.treeo.adapters.GuideRecyclerAdapter
import org.treeo.treeo.adapters.OnGuideClickListener
import org.treeo.treeo.models.Activity
import org.treeo.treeo.ui.home.GuideViewModel
import org.treeo.treeo.util.SELECTED_LANGUAGE
import javax.inject.Inject

@AndroidEntryPoint
class GuideFragment : Fragment(), OnGuideClickListener {

    @Inject
    lateinit var sharedPref: SharedPreferences

    private val guideViewModel: GuideViewModel by viewModels()
    private val adapter by lazy {
        GuideRecyclerAdapter(this, getSelectedLanguage())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_guide, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpViews()
        setObservers()
        getAllActivities()
    }

    private fun getSelectedLanguage() = sharedPref.getString(SELECTED_LANGUAGE, "en")!!

    private fun setUpViews() {
        initializeRecycler()
    }

    private fun initializeRecycler() {
        guideRecyclerView.adapter = adapter
        guideRecyclerView.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )
    }

    private fun getAllActivities() {
        guideViewModel.getAllActivities()
    }

    private fun setObservers() {
        guideViewModel.completedActivities.observe(viewLifecycleOwner) {
            if (it != null) {
                updateRecyclerview(it)
            }
        }
    }

    private fun updateRecyclerview(list: List<Activity>) {
        adapter.submitList(list)
    }

    override fun onClick(activity: Activity) {
        findNavController()
            .navigate(
                R.id.action_guideFragment_to_activityDetailsFragment,
                bundleOf(
                    "activity" to activity,
                    "activatePhotos" to "activate"
                )
            )
        guideViewModel.setActivityStartDate(activity.id)
    }

    companion object {
        @JvmStatic
        fun newInstance() = GuideFragment()
    }
}
