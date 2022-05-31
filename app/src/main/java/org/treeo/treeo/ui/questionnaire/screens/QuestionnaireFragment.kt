package org.treeo.treeo.ui.questionnaire.screens

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.shuhart.stepview.StepView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_questionnaire.*
import org.treeo.treeo.R
import org.treeo.treeo.adapters.ResponseListener
import org.treeo.treeo.adapters.ResponseRecyclerAdapter
import org.treeo.treeo.models.ActivitySummaryItem
import org.treeo.treeo.models.Page
import org.treeo.treeo.models.QuestionnaireSummaryItem
import org.treeo.treeo.ui.questionnaire.QuestionnaireViewModel
import org.treeo.treeo.util.SELECTED_LANGUAGE
import org.treeo.treeo.util.disableView
import org.treeo.treeo.util.enableView
import javax.inject.Inject


@AndroidEntryPoint
class QuestionnaireFragment : Fragment(), ResponseListener {
    private lateinit var indexMap: Map<String, Int>

    @Inject
    lateinit var sharedPref: SharedPreferences

    private val questionnaireViewModel: QuestionnaireViewModel by activityViewModels()

    private var bundleItem: ActivitySummaryItem? = null
    private lateinit var summaryItem: QuestionnaireSummaryItem

    private var currentPage: Int = 0
    private var selectionSum = 0
    private var pageList = listOf<Page>()

    private var selectedLanguage = "en"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bundleItem = arguments?.getParcelable("summaryItem")
        summaryItem = bundleItem as QuestionnaireSummaryItem
        indexMap = arguments?.get("indexMap") as Map<String, Int>
        return inflater.inflate(R.layout.fragment_questionnaire, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedLanguage = getSelectedLanguage()
        pageList = summaryItem.pages
        initializeViews()
    }

    private fun initializeViews() {
        initializeStepView()
        initializeTextViews()
        initializeButtons()
        initializeRecycler()
    }

    private fun initializeStepView() {
        questionnaireIndicatorView.state
            .animationType(StepView.ANIMATION_LINE)
            .stepsNumber(pageList.size)
            .animationDuration(resources.getInteger(android.R.integer.config_shortAnimTime))
            .commit()
    }

    private fun initializeTextViews() {
        questionnaireTextView.text = pageList[currentPage].header[selectedLanguage].toString()
        questionnaireDescriptionTextView.text =
            pageList[currentPage].description[selectedLanguage].toString()
    }

    private fun initializeRecycler() {
        optionRecyclerview.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )
        optionRecyclerview.adapter = ResponseRecyclerAdapter(
            selectedLanguage,
            this,
            pageList[currentPage]
        )
    }

    private fun initializeButtons() {
        questionnaireContinueButton.setOnClickListener {
            if (currentPage < pageList.size - 1) {
                currentPage += 1
                selectionSum = 0
                disableView(questionnaireContinueButton)
                updateStepView()
                updateTextViews()
                updateRecyclerView()
            } else if (currentPage == pageList.size - 1) {
                completeQuestionnaire(summaryItem)

                leaveQuestionnaire()
            }
        }
    }

    private fun leaveQuestionnaire() {
        if (indexMap["itemPosition"] == indexMap["listSize"]) {
            questionnaireViewModel.markActivityAsCompleted(summaryItem.activity?.id!!)
        }
        findNavController().popBackStack()
    }

    private fun updateStepView() {
        questionnaireIndicatorView.go(currentPage, true)
    }

    private fun updateTextViews() {
        questionnaireTextView.text = pageList[currentPage].header[selectedLanguage].toString()
        questionnaireDescriptionTextView.text =
            pageList[currentPage].description[selectedLanguage].toString()
    }

    private fun updateRecyclerView() {
        optionRecyclerview.adapter = ResponseRecyclerAdapter(
            selectedLanguage,
            this,
            pageList[currentPage]
        ).also { optionRecyclerview.adapter = it }
    }

    private fun getSelectedLanguage() = sharedPref.getString(SELECTED_LANGUAGE, "en")!!

    private fun completeQuestionnaire(summaryItem: QuestionnaireSummaryItem) {
        questionnaireViewModel.markQuestionnaireAsCompleted(summaryItem.questionnaireId)
    }

    override fun onOptionCheck(id: Long, isSelected: Boolean) {
        questionnaireViewModel.updateOption(id, isSelected)
    }

    override fun onTextInput(pageId: Long, response: String) {
        questionnaireViewModel.updateUserInput(pageId, response)
        if (pageList[currentPage].mandatory) {
            if (response.isBlank()) {
                disableView(questionnaireContinueButton)
            } else {
                enableView(questionnaireContinueButton)
            }
        }
    }

    override fun incrementSelectionSum(flag: String?) {
        if (flag != null && flag == "radio" && selectionSum == 0) {
            selectionSum += 1
            enableView(questionnaireContinueButton)
        } else {
            selectionSum += 1
            if (selectionSum > 0) {
                enableView(questionnaireContinueButton)
            } else {
                disableView(questionnaireContinueButton)
            }
        }
    }

    override fun decrementSelectionSum() {
        if (selectionSum > 0) {
            selectionSum -= 1
        }
        if (selectionSum <= 0) {
            disableView(questionnaireContinueButton)
        } else {
            enableView(questionnaireContinueButton)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = QuestionnaireFragment()
    }
}
