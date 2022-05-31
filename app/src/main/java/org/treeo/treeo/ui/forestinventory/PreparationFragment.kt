package org.treeo.treeo.ui.forestinventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_preparation.*
import org.treeo.treeo.R
import org.treeo.treeo.util.ADHOC_ACTIVITY_ID
import org.treeo.treeo.util.INVENTORY_ID

class PreparationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_preparation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpUI(view)
    }

    private fun setUpUI(view: View) {
        preparation_toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        preparation_toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        preparation_toolbar.inflateMenu(R.menu.main_menu)
        preparation_toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.exit_btn -> {
                    requireActivity().finish()
                    true
                }
                else -> {
                    false
                }
            }
        }


        btn_measure_trees.setOnClickListener {
            findNavController().navigate(
                R.id.firstGuideFragment,
                bundleOf(
                    INVENTORY_ID to arguments?.getLong(INVENTORY_ID),
                    ADHOC_ACTIVITY_ID to arguments?.getLong(
                        ADHOC_ACTIVITY_ID
                    )
                )
            )
        }
    }
}
