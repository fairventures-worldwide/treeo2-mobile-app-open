package org.treeo.treeo.ui.landsurvey.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.fragment_land_corners.*
import kotlinx.android.synthetic.main.fragment_request_camera_use.view.*
import org.treeo.treeo.R
import org.treeo.treeo.models.ActivitySummaryItem
import org.treeo.treeo.models.LandSurveySummaryItem
import org.treeo.treeo.ui.landsurvey.LandSurveyViewModel
import org.treeo.treeo.util.enableView

class LandCornersFragment : Fragment() {

    private lateinit var indexMap: Map<String, Int>
    private val surveyViewModel: LandSurveyViewModel by activityViewModels()

    private var bundleItem: ActivitySummaryItem? = null
    private lateinit var summaryItem: LandSurveySummaryItem

    private var numberOfCorners = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bundleItem = arguments?.getParcelable("summaryItem")
        summaryItem = bundleItem as LandSurveySummaryItem
        indexMap = arguments?.get("indexMap") as Map<String, Int>
        return inflater.inflate(R.layout.fragment_land_corners, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        view.toolbar.inflateMenu(R.menu.main_menu)
        view.toolbar.setNavigationOnClickListener {
            view.findNavController().popBackStack()
        }
        initializeUI()

        view.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.exit_btn -> {
                    view.findNavController()
                        .navigate(R.id.action_landCornersFragment_to_homeFragment)
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun initializeUI() {
        questionnaireOptionTextView3.setOnClickListener {
            radioButtonChecked(
                questionnaireOptionTextView3
            )
        }
        questionnaireOptionTextView4.setOnClickListener {
            radioButtonChecked(
                questionnaireOptionTextView4
            )
        }
        questionnaireOptionTextView5.setOnClickListener {
            radioButtonChecked(
                questionnaireOptionTextView5
            )
        }
        questionnaireOptionTextView6.setOnClickListener {
            radioButtonChecked(
                questionnaireOptionTextView6
            )
        }
        questionnaireOptionTextView6andmore.setOnClickListener {
            radioButtonChecked(
                questionnaireOptionTextView6andmore
            )
        }

        corners_btn_continue.setOnClickListener {
            if (numberOfCorners == 0) {
                Toast.makeText(requireContext(), "First Pick a choice", Toast.LENGTH_LONG).show()
            } else {
                view?.findNavController()
                    ?.navigate(
                        R.id.takeLandPhotos,
                        bundleOf(
                            "numberOfCorners" to numberOfCorners,
                            "summaryItem" to summaryItem,
                            "indexMap" to indexMap
                        )
                    )
            }
        }
    }

    private fun radioButtonChecked(questionnaireOptionTextView: RadioButton?) {
        val radioButtons = listOf<RadioButton>(
            questionnaireOptionTextView3, questionnaireOptionTextView4,
            questionnaireOptionTextView5, questionnaireOptionTextView6,
            questionnaireOptionTextView6andmore
        )
        radioButtons.forEach { radioButton ->
            if (radioButton.isChecked && radioButton != questionnaireOptionTextView) {
                radioButton.isChecked = false
            }
        }

        when (questionnaireOptionTextView?.text.toString()) {
            "3" -> {
                numberOfCorners = 3
                surveyViewModel.setNumberOfCornersToLandSurvey(
                    numberOfCorners,
                    summaryItem.activity?.id!!
                )
            }
            "4" -> {
                numberOfCorners = 4
                surveyViewModel.setNumberOfCornersToLandSurvey(
                    numberOfCorners,
                    summaryItem.activity?.id!!
                )
            }
            "5" -> {
                numberOfCorners = 5
                surveyViewModel.setNumberOfCornersToLandSurvey(
                    numberOfCorners,
                    summaryItem.activity?.id!!
                )
            }
            "6" -> {
                numberOfCorners = 6
                surveyViewModel.setNumberOfCornersToLandSurvey(
                    numberOfCorners,
                    summaryItem.activity?.id!!
                )
            }
            "6 and more" -> {
                numberOfCorners = 50
                surveyViewModel.setNumberOfCornersToLandSurvey(
                    numberOfCorners,
                    summaryItem.activity?.id!!
                )
            }
            else -> {
                enableView(corners_btn_continue, false)
            }
        }
        enableView(corners_btn_continue)
    }
}
