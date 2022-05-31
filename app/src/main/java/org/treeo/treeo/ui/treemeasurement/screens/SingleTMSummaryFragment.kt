package org.treeo.treeo.ui.treemeasurement.screens

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import org.treeo.treeo.R
import org.treeo.treeo.adapters.TMSummaryAdapter
import org.treeo.treeo.databinding.FragmentSingleTMSummaryBinding
import org.treeo.treeo.models.TMSummaryListItem
import org.treeo.treeo.ui.treemeasurement.TMViewModel
import javax.inject.Inject

@AndroidEntryPoint
class SingleTMSummaryFragment : Fragment() {
    private val viewModel: TMViewModel by activityViewModels()
    private val binding by viewBinding(FragmentSingleTMSummaryBinding::bind)

    @Inject
    lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_single_t_m_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setUpViews()
    }

    private fun FragmentSingleTMSummaryBinding.setUpViews() {
        toolbar.setUpToolbar()

        toDashboardBtn.setOnClickListener {
            viewModel.cleanup()
            requireActivity().finish()
        }

        Glide.with(requireContext())
            .load(viewModel.getImagePath())
            .into(image)

        treeSpecie.text = viewModel.tmSummaryInfoMap["specie"] ?: "N/A"

        setUpSummaryRecyclerView()
    }

    private fun Toolbar.setUpToolbar() {
        setNavigationIcon(R.drawable.ic_back_arrow)
        inflateMenu(R.menu.main_menu)
        setNavigationOnClickListener {
            findNavController().popBackStack()
        }
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

    private fun FragmentSingleTMSummaryBinding.setUpSummaryRecyclerView() {
        val summaryList = viewModel.tmSummaryInfoMap.entries.map {
            TMSummaryListItem(it.key, it.value)
        }.toMutableList()
        rvTMSummaryInfo.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = TMSummaryAdapter(summaryList)
        }
    }
}
