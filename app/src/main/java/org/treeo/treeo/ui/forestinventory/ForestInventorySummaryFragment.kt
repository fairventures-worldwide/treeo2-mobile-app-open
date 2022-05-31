package org.treeo.treeo.ui.forestinventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import org.treeo.treeo.R
import org.treeo.treeo.adapters.TMSummaryAdapter
import org.treeo.treeo.databinding.FragmentForestInventorySummaryBinding
import org.treeo.treeo.models.TMSummaryListItem
import org.treeo.treeo.ui.treemeasurement.TMViewModel

class ForestInventorySummaryFragment : Fragment() {
    private val viewModel: TMViewModel by activityViewModels()
    private val binding by viewBinding(FragmentForestInventorySummaryBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_forest_inventory_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setUpViews()
    }

    private fun FragmentForestInventorySummaryBinding.setUpViews() {
        toolbar.setUpToolbar()

        toDashboardBtn.setOnClickListener {
            viewModel.cleanup()
            requireActivity().finish()
        }

        seeReportBtn.setOnClickListener {
            findNavController().navigate(R.id.inventoryReportFragment)
        }

        setUpSummaryRecyclerView()
    }

    private fun Toolbar.setUpToolbar() {
        setNavigationIcon(R.drawable.ic_back_arrow)
        setNavigationOnClickListener {
            findNavController().popBackStack()
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

    private fun FragmentForestInventorySummaryBinding.setUpSummaryRecyclerView() {
        val summaryList = viewModel.tmSummaryInfoMap.entries.map {
            TMSummaryListItem(it.key, it.value)
        }.toMutableList()
        rvInventorySummary.run {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = TMSummaryAdapter(summaryList)
        }
    }
}
