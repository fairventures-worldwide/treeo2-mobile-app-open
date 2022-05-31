package org.treeo.treeo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import org.treeo.treeo.R
import org.treeo.treeo.models.TreeoProject
import org.treeo.treeo.ui.authentication.AuthViewModel

class ProjectsListAdapter (private val treeoProjects:  List<TreeoProject>, viewModel: AuthViewModel) : RecyclerView.Adapter<ProjectsListAdapter.ViewHolder>()
{
    private var checkedRadioButton: CompoundButton? = null
    private  val leViewModel: AuthViewModel = viewModel


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val projectOption: RadioButton = itemView.findViewById(R.id.rbProjectCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val contactView = inflater.inflate(R.layout.project_card, parent, false)
        return ViewHolder(contactView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val project: TreeoProject = treeoProjects[position]
        val projectOption = viewHolder.projectOption
        projectOption.text = project.name

        projectOption.setOnClickListener{
            leViewModel.setProjectId(treeoProjects[position].id)
        }

        projectOption.setOnCheckedChangeListener(checkedChangeListener)

        if (projectOption.isChecked) {
            checkedRadioButton = projectOption
        }
    }

    private val checkedChangeListener = CompoundButton.OnCheckedChangeListener {
            compoundButton, isChecked ->
        checkedRadioButton?.apply { setChecked(!isChecked) }
        checkedRadioButton = compoundButton.apply {
            setChecked(isChecked)
        }
    }

    override fun getItemCount(): Int {
        return treeoProjects.size
    }
}
