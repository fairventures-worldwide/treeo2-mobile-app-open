package org.treeo.treeo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.treeo.treeo.R
import org.treeo.treeo.models.ActivitySummaryItem
import org.treeo.treeo.models.LandSurveySummaryItem
import org.treeo.treeo.models.Option
import org.treeo.treeo.models.QuestionnaireSummaryItem


private const val ITEM_VIEW_TYPE_QUESTIONNAIRE = 1
private const val ITEM_VIEW_TYPE_PHOTOS = 2

class ActivitySummaryAdapter(
    private val context: Context,
    private val summaryListener: ActivitySummaryListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var list: List<ActivitySummaryItem> = listOf()
    var language = ""

    class QuestionSummaryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val summaryContainer: LinearLayout =
            itemView.findViewById(R.id.activitySummaryContainer)
        val inCompleteLayout: LinearLayout =
            itemView.findViewById(R.id.durationInCompleteLinearLayout)
        val completeLayout: LinearLayout =
            itemView.findViewById(R.id.durationCompletedLinearLayout)
        val titleTextview: TextView =
            itemView.findViewById(R.id.activityDetailsTitleTextview)
        val startButton: Button =
            itemView.findViewById(R.id.activityDetailsStartButton)
        val editButton: Button =
            itemView.findViewById(R.id.activityDetailsEditButton)

        companion object {
            fun from(parent: ViewGroup): QuestionSummaryHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(
                    R.layout.activity_summary_item_questionnaire,
                    parent,
                    false
                )
                return QuestionSummaryHolder(view)
            }
        }
    }

    class PhotosSummaryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val inCompleteLayout: LinearLayout =
            itemView.findViewById(R.id.durationPhotosInCompleteLinearLayout)
        val inFieldLayout: LinearLayout =
            itemView.findViewById(R.id.durationPhotosInFieldLinearLayout)
        val completeLayout: LinearLayout =
            itemView.findViewById(R.id.durationPhotosCompletedLinearLayout)
        val titleTextview: TextView = itemView.findViewById(R.id.activityPhotosTitleTextview)
        val startButton: Button =
            itemView.findViewById(R.id.activityPhotosStartButton)
        val editButton: Button =
            itemView.findViewById(R.id.activityPhotosEditButton)

        val landPhotosIncompleteIcon: ImageView =
            itemView.findViewById(R.id.landPhotosIncompleteIcon)

        val landPhotosCompleteIcon: ImageView =
            itemView.findViewById(R.id.landPhotosCompleteIcon)

        val soilPhotosIncompleteIcon: ImageView =
            itemView.findViewById(R.id.soilPhotosIncompleteIcon)

        val soilPhotosCompleteIcon: ImageView =
            itemView.findViewById(R.id.soilPhotosCompleteIcon)

        val soilPhotosLinearLayout: LinearLayout =
            itemView.findViewById(R.id.soilPhotosLinearLayout)

        companion object {
            fun from(parent: ViewGroup): PhotosSummaryHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(
                    R.layout.activity_summary_item_photo,
                    parent,
                    false
                )
                return PhotosSummaryHolder(view)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_QUESTIONNAIRE -> QuestionSummaryHolder.from(parent)
            ITEM_VIEW_TYPE_PHOTOS -> PhotosSummaryHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            list[position] is QuestionnaireSummaryItem -> {
                ITEM_VIEW_TYPE_QUESTIONNAIRE
            }
            list[position] is LandSurveySummaryItem -> {
                ITEM_VIEW_TYPE_PHOTOS
            }
            else -> {
                0
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is QuestionSummaryHolder -> {
                holder.apply {
                    itemView.setOnClickListener {
                        summaryListener.onActivityClick(list[position], position)
                    }
                    startButton.setOnClickListener {
                        summaryListener.onActivityClick(list[position], position)
                    }
                    editButton.setOnClickListener {
                        summaryListener.onActivityClick(list[position], position)
                    }

                    val cardHeader =
                        context.getString(R.string.part) + (position + 1) + " - " + context.getString(
                            R.string.form
                        )
                    titleTextview.text = cardHeader

                    if (list[position] is QuestionnaireSummaryItem) {
                        val questionnaireItem = list[position] as QuestionnaireSummaryItem
                        if (questionnaireItem.isCompleted) {
                            editButton.visibility = View.VISIBLE
                            startButton.visibility = View.GONE
                            completeLayout.visibility = View.VISIBLE
                            inCompleteLayout.visibility = View.GONE

                            clearContainer(summaryContainer)
                            questionnaireItem.pages.forEach {
                                summaryContainer.addView(
                                    inflateCompleteLayout(
                                        it.header[language].toString(),
                                        it.options
                                    )
                                )
                            }
                        } else {
                            editButton.visibility = View.GONE
                            startButton.visibility = View.VISIBLE
                            completeLayout.visibility = View.GONE
                            inCompleteLayout.visibility = View.VISIBLE

                            clearContainer(summaryContainer)
                            questionnaireItem.pages.forEach {
                                summaryContainer.addView(
                                    inflateIncompleteLayout(
                                        it.header[language].toString(),
                                        context.resources.getString(R.string.placeholder_dashes)
                                    )
                                )
                            }
                        }
                    }
                }
            }
            is PhotosSummaryHolder -> {
                holder.apply {
                    itemView.setOnClickListener {
                        summaryListener.onActivityClick(list[position], position)
                    }
                    startButton.setOnClickListener {
                        summaryListener.onActivityClick(list[position], position)
                    }
                    editButton.setOnClickListener {
                        summaryListener.onActivityClick(list[position], position)
                    }

                    val cardHeader =
                        context.getString(R.string.part) + (position + 1) + " - " + context.getString(
                            R.string.photos
                        )

                    titleTextview.text = cardHeader

                    if (list[position] is LandSurveySummaryItem) {
                        val landSurveyItem = list[position] as LandSurveySummaryItem
                        if (!landSurveyItem.activity?.template?.configuration?.soilPhotoRequired!!) {
                            soilPhotosLinearLayout.visibility = View.GONE
                        }

                        if (landSurveyItem.landSurvey.isCompleted) {
                            inCompleteLayout.visibility = View.INVISIBLE
                            inFieldLayout.visibility = View.INVISIBLE
                            completeLayout.visibility = View.VISIBLE
                            landPhotosCompleteIcon.visibility = View.VISIBLE
                            landPhotosIncompleteIcon.visibility = View.GONE
                            soilPhotosCompleteIcon.visibility = View.VISIBLE
                            soilPhotosIncompleteIcon.visibility = View.GONE
                            startButton.visibility = View.GONE
                        } else {
                            inCompleteLayout.visibility = View.VISIBLE
                            inFieldLayout.visibility = View.VISIBLE
                            completeLayout.visibility = View.GONE
                            landPhotosCompleteIcon.visibility = View.GONE
                            landPhotosIncompleteIcon.visibility = View.VISIBLE
                            soilPhotosCompleteIcon.visibility = View.GONE
                            soilPhotosIncompleteIcon.visibility = View.VISIBLE
                            startButton.visibility = View.VISIBLE
                            editButton.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private fun inflateIncompleteLayout(header: String?, answer: String?): View {
        val inflater = context
            .applicationContext
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.page_summary_template_incomplete, null)
        val actionTextView: TextView = view.findViewById(R.id.activitySummaryIncompleteTextview)
        val resultTextView: TextView = view.findViewById(R.id.activityAnswerIncompleteTextview)
        actionTextView.text = header
        resultTextView.text = answer

        return view
    }

    private fun inflateCompleteLayout(header: String?, options: List<Option>?): View {
        val inflater = context
            .applicationContext
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.page_summary_template_complete, null)
        val actionTextView: TextView = view.findViewById(R.id.activitySummaryCompleteTextview)
        val resultTextView: TextView = view.findViewById(R.id.activityAnswerCompleteTextview)
        actionTextView.text = header
        options?.forEach {
            if (it.isSelected) {
                resultTextView.text = it.title[language].toString()
            }
        }
        return view
    }

    private fun clearContainer(view: LinearLayout) {
        if (view.childCount > 0) {
            view.removeAllViews()
        }
    }

    fun submitList(activities: List<ActivitySummaryItem>, selectedLanguage: String) {
        list = activities
        language = selectedLanguage
        notifyDataSetChanged()
    }


}

interface ActivitySummaryListener {
    fun onActivityClick(summaryItem: ActivitySummaryItem, itemPosition: Int)
}

