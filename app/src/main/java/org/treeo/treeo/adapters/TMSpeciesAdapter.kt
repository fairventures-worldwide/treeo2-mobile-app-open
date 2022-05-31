package org.treeo.treeo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.treeo.treeo.R
import org.treeo.treeo.databinding.TmSpecieItemBinding
import org.treeo.treeo.db.models.TreeSpecieEntity
import org.treeo.treeo.util.showView

class TMSpeciesAdapter(
    private val species: List<TreeSpecieEntity>,
    private val selectedSpecie: String?,
    private val onSpecieSelected: (code: String, name: String) -> Unit,
    private val userLanguage: String,
) : RecyclerView.Adapter<TMSpeciesAdapter.ViewHolder>() {

    private lateinit var binding: TmSpecieItemBinding

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.tm_specie_item,
            parent,
            false,
        )
        binding = TmSpecieItemBinding.bind(view)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        binding.apply {
            species[position].apply {
                val langCode =
                    if (trivialName.containsKey(userLanguage)) userLanguage else "en"
                val trivialName = trivialName[langCode].toString()
                treeSpecieName.text = trivialName

                root.setOnClickListener {
                    onSpecieSelected(code, trivialName)
                }

                if (code == selectedSpecie) {
                    cardRoot.setBackgroundColor(root.resources.getColor(R.color.app_green))
                    showView(icCheck)
                    treeSpecieName.setTextColor(root.resources.getColor(R.color.white))
                }
            }
        }
    }

    override fun getItemCount(): Int = species.size
}
