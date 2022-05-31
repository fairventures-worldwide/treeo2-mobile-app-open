package org.treeo.treeo.ui.forestinventory

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_select_whole_field.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.treeo.treeo.R
import org.treeo.treeo.adapters.HomeGuideListener
import org.treeo.treeo.adapters.HomeGuideRecyclerAdapter
import org.treeo.treeo.databinding.DialogUnfinishedForestInventoryBinding
import org.treeo.treeo.models.Activity
import org.treeo.treeo.ui.home.HomeViewModel
import org.treeo.treeo.ui.treemeasurement.BottomSheetDialogViewModel
import org.treeo.treeo.ui.treemeasurement.TMViewModel
import org.treeo.treeo.ui.treemeasurement.TMViewModel.Companion.currentActivityId
import org.treeo.treeo.ui.treemeasurement.TMViewModel.Companion.currentActivityType
import org.treeo.treeo.ui.treemeasurement.TMViewModel.Companion.currentActivityUUID
import org.treeo.treeo.ui.treemeasurement.TreeMeasurementActivity
import org.treeo.treeo.util.ADHOC_ACTIVITY_ID
import org.treeo.treeo.util.INVENTORY_ID
import org.treeo.treeo.util.createNewAdhocActivityFromExistingActivity
import org.treeo.treeo.util.showView

@AndroidEntryPoint
class SelectWholeFieldFragment : Fragment(), HomeGuideListener {

    private lateinit var adhocOnPlotRecyclerAdapter: HomeGuideRecyclerAdapter
    private lateinit var adhocNewPlotRecyclerAdapter: HomeGuideRecyclerAdapter
    private var selectedLanguage = "en"

    private val tmViewModel: TMViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val bottomSheetViewModel: BottomSheetDialogViewModel by viewModels()

    private var theActivity: Activity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_select_whole_field, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
        setUpUI()
        setUpHomeGuideRecycler()
    }

    private fun setObservers() {

        tmViewModel.adhocActivities.observe(viewLifecycleOwner) { list ->

            val adhocPlotActivityList: List<Activity> = list.filter { it.plot != null  && it.template.activityType != "one_tree"}
            if (adhocPlotActivityList.isEmpty()) {
                showView(outOfAdhocPlotActivitiesBanner)
            }
            val adhocNewPlotActivityList = list.filter { it.plot == null && it.template.activityType != "one_tree" }
            if (adhocNewPlotActivityList.isNotEmpty()) {
                val subList = adhocNewPlotActivityList.subList(0, 1)
                adhocNewPlotRecyclerAdapter.submitList(subList)
            } else {
                showView(outOfActivitiesNewPlotBanner)
            }
            adhocOnPlotRecyclerAdapter.submitList(adhocPlotActivityList)
        }
        bottomSheetViewModel.adhocActivity.observe(viewLifecycleOwner) {
            startTheActivity(it)
        }
        bottomSheetViewModel.incompleteAdhocActivity.observe(viewLifecycleOwner) {
            if (it == null) {
                val activityEntity = theActivity?.let { it1 ->
                    createNewAdhocActivityFromExistingActivity(
                        it1
                    )
                }
                if (activityEntity != null) {
                    bottomSheetViewModel.createActivityFromEntity(activityEntity)
                }
            } else {
                startTheActivity(it)
            }
        }
    }

    private fun setUpHomeGuideRecycler() {
        adhocOnPlotRecyclerAdapter = HomeGuideRecyclerAdapter(selectedLanguage, this)
        adhocOnPlotRecycler.adapter = adhocOnPlotRecyclerAdapter
        adhocOnPlotRecycler.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )

        adhocNewPlotRecyclerAdapter = HomeGuideRecyclerAdapter(selectedLanguage, this)
        adhocNewPlotRecycler.adapter = adhocNewPlotRecyclerAdapter
        adhocNewPlotRecycler.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )

        tmViewModel.getNextAdhocActivities()
    }

    private fun setUpUI() {
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setOnMenuItemClickListener {
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

    override fun onHomeGuideClick(activity: Activity) {
        theActivity = activity
        bottomSheetViewModel.getIncompleteAdhocActivities(activity.plot?.area)
    }

    private fun startTheActivity(activity: Activity) {
        homeViewModel.setActivityStartDate(activity.id)
        currentActivityType = activity.type
        currentActivityId = activity.id
        currentActivityUUID = activity.uuid
        tmViewModel.currentRetryTimes = activity.configuration?.retryTimes

        if (activity.template.activityType == "tree_survey") {
            GlobalScope.launch {

                val inventoryId: Long

                val incompleteInventory =
                    homeViewModel.checkIncompleteInventory(activityId = activity.id)
                if (incompleteInventory == null) {
                    inventoryId = homeViewModel.startNewForestInventory(activity.id)
                    startActivity(
                        Intent(
                            requireActivity(),
                            TreeMeasurementActivity::class.java
                        ).apply {
                            putExtra(ADHOC_ACTIVITY_ID, activity.id)
                            putExtra(INVENTORY_ID, inventoryId)
                        }
                    )
                } else {
                    promptUserWithIncompleteInventory(
                        activityId = activity.id,
                        incompleteInventory.forestInventoryId,
                        activityType = activity.type,
                    )
                }
            }
        }
    }

    private fun promptUserWithIncompleteInventory(
        activityId: Long,
        forestInventoryId: Long,
        activityType: String?
    ) {
        activity?.runOnUiThread {
            Dialog(requireContext()).apply {
                val binding = DialogUnfinishedForestInventoryBinding.inflate(layoutInflater)
                setContentView(binding.root)

                binding.btnStartNew.setOnClickListener {
                    dismiss()
                    if (activityType != null) {
                        askUserToConfirmDelete(activityId, forestInventoryId)
                    }
                }

                binding.btnContinue.setOnClickListener {
                    bottomSheetViewModel.initForestInventory(forestInventoryId, false)
                    findNavController().apply {
                        popBackStack()
                        navigate(
                            R.id.preparationFragment,
                            bundleOf(INVENTORY_ID to forestInventoryId)
                        )
                    }
                    dismiss()
                }

                show()
            }
        }
    }

    private fun askUserToConfirmDelete(
        activityId: Long,
        forestInventoryId: Long,
    ) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.title_confirm_inventory_delete)
            setMessage(R.string.prompt_confirm_inventory_delete)
            setPositiveButton(R.string.option_delete) { _, _ ->
                bottomSheetViewModel.clearPreviousInventoryAndStartNewOneBtyActivityId(
                    forestInventoryId,
                    activityId
                )
                findNavController().apply {
                    popBackStack()
                    navigate(
                        R.id.preparationFragment,
                        bundleOf(INVENTORY_ID to forestInventoryId)
                    )
                }
            }
            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            setCancelable(false)
            show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SelectWholeFieldFragment()
    }
}
