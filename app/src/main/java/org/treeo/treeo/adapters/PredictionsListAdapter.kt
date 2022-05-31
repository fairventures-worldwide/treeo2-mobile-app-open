package org.treeo.treeo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.treeo.treeo.R
import org.treeo.treeo.models.PredictionItem

class PredictionsListAdapter(
    private val listData: ArrayList<PredictionItem>
): RecyclerView.Adapter<PredictionsListAdapter.ViewHolder>() {
    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var itemTitle: TextView = itemView.findViewById(R.id.le_title)
        var itemValue: TextView = itemView.findViewById(R.id.le_value)
        var itemDetail: TextView = itemView.findViewById(R.id.le_detail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictionsListAdapter.ViewHolder {
        val itemView: View = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.tm_prediction_item,
                parent,
                false
            )
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PredictionsListAdapter.ViewHolder, position: Int) {
        val summaryList: PredictionItem = listData[position]
        holder.itemTitle.text = summaryList.title
        holder.itemValue.text = summaryList.value
        holder.itemDetail.text = summaryList.detail
    }

    override fun getItemCount(): Int {
        return listData.size
    }
}