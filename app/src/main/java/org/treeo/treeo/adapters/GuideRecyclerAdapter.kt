package org.treeo.treeo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.treeo.treeo.R
import org.treeo.treeo.models.Activity
import org.treeo.treeo.util.getActivityGuideDrawableId

class GuideRecyclerAdapter(
    private val listener: OnGuideClickListener,
    private val selectedLanguage: String
) :
    RecyclerView.Adapter<GuideRecyclerAdapter.GuidePageViewHolder>() {

    var list = listOf<Activity>()

    class GuidePageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.guideObjectImageView)
        val todayFlag: TextView = itemView.findViewById(R.id.guideTodayFlag)
        val continueFlag: TextView = itemView.findViewById(R.id.guideContinueFlag)
        val unfinishedLayout: LinearLayout = itemView.findViewById(R.id.unfinishedLayout)
        val guideCompleteFlag: TextView = itemView.findViewById(R.id.guideCompleteFlag)
        val titleTextView: TextView = itemView.findViewById(R.id.guideObjectTitleTextView)
        val detailsTextView: TextView = itemView.findViewById(R.id.guideObjectDetailTextView)

        companion object {
            fun from(parent: ViewGroup): GuidePageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.treeo_guide_object_layout, parent, false)
                return GuidePageViewHolder(view)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuidePageViewHolder {
        return GuidePageViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: GuidePageViewHolder, position: Int) {
        holder.apply {
            list[position].apply {
                itemView.setOnClickListener {
                    listener.onClick(list[position])
                }

                Glide.with(itemView.context)
                    .load(getActivityGuideDrawableId(template.activityType.toString()))
                    .into(imageView)

                detailsTextView.text = description[selectedLanguage].toString()

                when {
                    inProgress -> {
                        todayFlag.visibility = View.INVISIBLE
                        unfinishedLayout.visibility = View.VISIBLE
                    }
                    isCompleted -> {
                        todayFlag.visibility = View.INVISIBLE
                        guideCompleteFlag.visibility = View.VISIBLE
                    }
                    else -> {
                        todayFlag.visibility = View.VISIBLE
                        unfinishedLayout.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun submitList(activities: List<Activity>) {
        list = activities
        notifyDataSetChanged()
    }
}

interface OnGuideClickListener {
    fun onClick(activity: Activity)
}
