package org.treeo.treeo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.treeo.treeo.R
import org.treeo.treeo.models.TMSummaryListItem

class TMSummaryAdapter(
    private val listItems: List<TMSummaryListItem>
) : RecyclerView.Adapter<TMSummaryAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var itemTitle: TextView = itemView.findViewById(R.id.item_title)
        var itemValue: TextView = itemView.findViewById(R.id.item_value)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView: View = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.single_tree_info_row_item,
                parent,
                false
            )
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val summaryListItem: TMSummaryListItem = listItems[position]
        holder.itemTitle.text = summaryListItem.key
        holder.itemValue.text = summaryListItem.value
    }

    override fun getItemCount(): Int {
        return listItems.size
    }
}
