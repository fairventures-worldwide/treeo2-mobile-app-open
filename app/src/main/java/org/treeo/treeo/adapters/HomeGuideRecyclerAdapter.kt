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
import org.treeo.treeo.util.getRemaining


class HomeGuideRecyclerAdapter(
    var selectedLanguage: String,
    private val listener: HomeGuideListener
) : RecyclerView.Adapter<HomeGuideRecyclerAdapter.GuideViewHolder>() {

    var list: List<Activity> = listOf()

    class GuideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView =
            itemView.findViewById(R.id.guideObjectImageView)
        val todayFlag: TextView = itemView.findViewById(R.id.guideTodayFlag)
        val plotNameFlag: TextView = itemView.findViewById(R.id.guidePlotNameFlag)
        val continueFlag: TextView =
            itemView.findViewById(R.id.guideContinueFlag)
        val unfinishedLayout: LinearLayout =
            itemView.findViewById(R.id.unfinishedLayout)
        val titleTextView: TextView =
            itemView.findViewById(R.id.guideObjectTitleTextView)
        val detailsTextView: TextView =
            itemView.findViewById(R.id.guideObjectDetailTextView)

        companion object {
            fun from(parent: ViewGroup): GuideViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(
                    R.layout.treeo_guide_object_layout,
                    parent,
                    false
                )
                return GuideViewHolder(view)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GuideViewHolder {
        return GuideViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: GuideViewHolder, position: Int) {
        holder.apply {
            list[position].apply {
                Glide.with(itemView.context)
                    .load(getActivityGuideDrawableId(template.activityType.toString()))
                    .into(imageView)

                if (plot != null) {
                    plotNameFlag.visibility = View.VISIBLE
                    plotNameFlag.text = "${plot.plotName}"
                }

                titleTextView.text = title[selectedLanguage].toString()
                detailsTextView.text = description[selectedLanguage].toString()

                if (inProgress) {
                    todayFlag.visibility = View.INVISIBLE
                    unfinishedLayout.visibility = View.VISIBLE
                } else {
                    if (type != "adhoc") {
                        todayFlag.text = dueDate?.let { getRemaining(it) }
                        todayFlag.visibility = View.VISIBLE
                    } else {
                        todayFlag.visibility = View.INVISIBLE
                    }
                    unfinishedLayout.visibility = View.INVISIBLE
                }
            }

            itemView.setOnClickListener {
                listener.onHomeGuideClick(list[position], )
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun submitList(newList: List<Activity>) {
        list = newList
        notifyDataSetChanged()
    }
}

interface HomeGuideListener {
    fun onHomeGuideClick(activity: Activity)
}
