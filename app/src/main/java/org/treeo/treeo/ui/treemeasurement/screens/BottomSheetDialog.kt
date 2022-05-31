package org.treeo.treeo.ui.treemeasurement.screens

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.bottom_sheet_tm_layout.*
import org.treeo.treeo.R
import org.treeo.treeo.databinding.DialogUnfinishedForestInventoryBinding
import org.treeo.treeo.db.models.ForestInventoryEntity
import org.treeo.treeo.ui.treemeasurement.BottomSheetDialogViewModel
import org.treeo.treeo.ui.treemeasurement.BottomSheetDialogViewModel.Companion.activityId
import org.treeo.treeo.ui.treemeasurement.TreeMeasurementActivity
import org.treeo.treeo.util.*

@AndroidEntryPoint
class BottomSheetDialog : BottomSheetDialogFragment() {
    private val viewModel by lazy {
        ViewModelProvider(this)[BottomSheetDialogViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_tm_layout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.setObservers()

        one_tree_button.setOnClickListener {
            viewModel.getOneTreeActivity(requireContext(), requireActivity())
        }
        whole_field_button.setOnClickListener {
            dismiss()
            startActivity(
                Intent(
                    requireActivity(),
                    TreeMeasurementActivity::class.java
                ).apply {
                    putExtra(IS_FROM_WHOLE_FIELD_BOTTOM_NAV_TAP, true)
                }
            )
        }
    }

    private fun BottomSheetDialogViewModel.setObservers() {

        incompleteInventory.observe(viewLifecycleOwner) {
            if (it == null) {
                startNewForestInventory()
            } else {
                it.promptUserWithIncompleteInventory()
            }
        }

        newInventoryId.observe(viewLifecycleOwner) {
            startActivityForResult(
                Intent(
                    requireActivity(),
                    TreeMeasurementActivity::class.java
                ).apply {
                    putExtra(ADHOC_ACTIVITY_ID, activityId)
                    putExtra(TREE_TYPE, treeType)
                    putExtra(INVENTORY_ID, it)
                }, REQUEST_CODE_MEASURE_TREE
            )
            dismiss()
        }

        adhocActivityId.observe(viewLifecycleOwner) {
            startActivityForResult(
                Intent(
                    requireActivity(),
                    TreeMeasurementActivity::class.java
                ).apply {
                    putExtra(ADHOC_ACTIVITY_ID, it)
                    putExtra(TREE_TYPE, treeType)
                }, REQUEST_CODE_MEASURE_TREE
            )
            dismiss()
        }
    }

    private fun ForestInventoryEntity.promptUserWithIncompleteInventory() {
        Dialog(requireContext()).apply {
            val binding = DialogUnfinishedForestInventoryBinding.inflate(layoutInflater)
            setContentView(binding.root)
            binding.btnStartNew.setOnClickListener {
                dismiss()
                askUserToConfirmDelete()
            }

            binding.btnContinue.setOnClickListener {
                viewModel.initForestInventory(forestInventoryId, false)

                startActivityForResult(
                    Intent(
                        requireActivity(),
                        TreeMeasurementActivity::class.java
                    ).apply {
                        putExtra(ADHOC_ACTIVITY_ID, activityId)
                        putExtra(TREE_TYPE, viewModel.treeType)
                        putExtra(INVENTORY_ID, forestInventoryId)
                    }, REQUEST_CODE_MEASURE_TREE
                )

                dismiss()
                this@BottomSheetDialog.dismiss()
            }

            show()
        }
    }

    private fun ForestInventoryEntity.askUserToConfirmDelete() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.title_confirm_inventory_delete)
            setMessage(R.string.prompt_confirm_inventory_delete)
            setPositiveButton(R.string.option_delete) { _, _ ->
                viewModel.clearPreviousInventoryAndStartNewOne(forestInventoryId)
            }
            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            setCancelable(false)
            show()
        }
    }
}
