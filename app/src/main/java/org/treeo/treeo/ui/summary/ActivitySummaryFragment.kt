package org.treeo.treeo.ui.summary

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_activity_summary.*
import org.treeo.treeo.R
import org.treeo.treeo.adapters.ActivitySummaryAdapter
import org.treeo.treeo.adapters.ActivitySummaryListener
import org.treeo.treeo.models.Activity
import org.treeo.treeo.models.ActivitySummaryItem
import org.treeo.treeo.models.LandSurveySummaryItem
import org.treeo.treeo.models.QuestionnaireSummaryItem
import org.treeo.treeo.util.SELECTED_LANGUAGE
import javax.inject.Inject

@AndroidEntryPoint
class ActivitySummaryFragment : Fragment(), ActivitySummaryListener {
    @Inject
    lateinit var sharedPref: SharedPreferences

    private val viewModel: ActivitySummaryViewModel by activityViewModels()

    private var selectedLanguage: String? = null
    private var activatePhotos: String? = null
    private var plannedActivity: Activity? = null
    private lateinit var adapter: ActivitySummaryAdapter
    private lateinit var summaryList: List<ActivitySummaryItem>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_activity_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        plannedActivity = arguments?.getParcelable("activity")
        activatePhotos = arguments?.getString("activatePhotos")
        selectedLanguage = sharedPref.getString(SELECTED_LANGUAGE, "en")
        setActivityId()
        setActivityUUID()
        initializeViews()
        setObservers()
    }

    override fun onResume() {
        super.onResume()
        getSummaryItems(plannedActivity!!)
    }

    private fun getSummaryItems(activity: Activity) {
        viewModel.getActivitySummaryItems(activity.id)
    }

    private fun initializeViews() {
        initializeTextViews()
        initializeRecycler()
        initializeButtons()
    }

    private fun initializeTextViews() {
        activityTitleTextView.text = plannedActivity!!.title[selectedLanguage].toString()
        activityDescriptionTextView.text = plannedActivity!!
            .description[selectedLanguage]
            .toString()
    }

    private fun initializeRecycler() {
        adapter = ActivitySummaryAdapter(requireContext(), this)
        activitySummaryRecyclerview.adapter = adapter
        activitySummaryRecyclerview.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )
    }

    private fun initializeButtons() {
        btn_continue_to_photos.setOnClickListener {
//            view?.findNavController()
//                ?.navigate(R.id.action_activitySummaryFragment_to_requestCameraFragment)
        }
    }

    private fun setObservers() {
        viewModel.activitySummaryItems.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                selectedLanguage = sharedPref.getString(SELECTED_LANGUAGE, "en")!!
                updateRecyclerview(it, selectedLanguage!!)
                summaryList = it
            }
        })
    }

    private fun updateRecyclerview(list: List<ActivitySummaryItem>, language: String) {
        adapter.submitList(list, language)
    }

    override fun onActivityClick(summaryItem: ActivitySummaryItem, itemPosition: Int) {
        if (summaryItem is LandSurveySummaryItem) {
            if (summaryItem.photos.isNotEmpty() && !summaryItem.landSurvey.isCompleted) {
                findNavController()
                    .navigate(
                        R.id.takeLandPhotos,
                        bundleOf(
                            "summaryItem" to summaryItem,
                            "indexMap" to mapOf(
                                "itemPosition" to itemPosition + 1,
                                "listSize" to summaryList.size
                            )
                        )
                    )
            } else if (summaryItem.landSurvey.isCompleted) {
                findNavController()
                    .navigate(
                        R.id.landSpecificationFragment,
                        bundleOf(
                            "summaryItem" to summaryItem,
                            "isAlreadyComplete" to true,
                            "indexMap" to mapOf(
                                "itemPosition" to itemPosition + 1,
                                "listSize" to summaryList.size
                            )
                        )
                    )
            } else {
                findNavController()
                    .navigate(
                        R.id.requestCameraFragment,
                        bundleOf(
                            "navigateTo" to "landSurveyCamera",
                            "summaryItem" to summaryItem,
                            "indexMap" to mapOf(
                                "itemPosition" to itemPosition + 1,
                                "listSize" to summaryList.size
                            )
                        )
                    )
            }
        } else if (summaryItem is QuestionnaireSummaryItem) {
            viewModel.markActivityInProgress(summaryItem.activity?.id!!)
            findNavController().navigate(
                R.id.questionnaireFragment,
                bundleOf(
                    "summaryItem" to summaryItem,
                    "indexMap" to mapOf(
                        "itemPosition" to itemPosition + 1,
                        "listSize" to summaryList.size
                    )
                )
            )
        }
    }

    private fun setActivityId() {
        with(sharedPref.edit()) {
            putLong("activityId", plannedActivity!!.id)
            apply()
        }
    }

    private fun setActivityUUID() {
        with(sharedPref.edit()) {
            putString("activityUUID", plannedActivity?.uuid.toString())
            apply()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ActivitySummaryFragment()
    }
}

