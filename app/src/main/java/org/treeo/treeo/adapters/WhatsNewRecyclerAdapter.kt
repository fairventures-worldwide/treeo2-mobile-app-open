package org.treeo.treeo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.treeo.treeo.R
import org.treeo.treeo.models.WhatsNew


class WhatsNewRecyclerAdapter(val context: Context, val items: List<WhatsNew>, val onClick: () -> Unit) :
    RecyclerView.Adapter<WhatsNewRecyclerAdapter.WhatsNewViewHolder>() {
    class WhatsNewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.whatsNewImageView)
        val titleTextView: TextView = itemView.findViewById(R.id.whatsNewTitleTextView)
        val detailsTextView: TextView = itemView.findViewById(R.id.whatsNewDetailsTextView)

        companion object {
            fun from(parent: ViewGroup): WhatsNewViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.whats_new_recycler_item, parent, false)
                return WhatsNewViewHolder(view)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WhatsNewViewHolder {
        return WhatsNewViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: WhatsNewViewHolder, position: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            holder.apply {
                Glide.with(context)
                    .load(items[position].image)
                    .into(imageView)
                titleTextView.text = items[position].title
                detailsTextView.text = items[position].details
                itemView.setOnClickListener {
                    onClick()
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
