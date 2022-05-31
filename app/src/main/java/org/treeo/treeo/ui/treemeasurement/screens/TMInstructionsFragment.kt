package org.treeo.treeo.ui.treemeasurement.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_tm_instructions.*
import org.treeo.treeo.R

class TMInstructionsFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_tm_instructions, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        btn_continue.setOnClickListener {
            findNavController().navigate(R.id.treeMeasureIntroFragment)
        }

        toolbar.apply {
            setNavigationIcon(R.drawable.ic_back_arrow)
            setNavigationOnClickListener {
                requireActivity().finish()
            }

            inflateMenu(R.menu.main_menu)
            setOnMenuItemClickListener {
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
        }
    }
}
