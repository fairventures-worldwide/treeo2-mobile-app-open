package org.treeo.treeo.ui.forestinventory.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_finish_measurement_dialog.*
import org.treeo.treeo.R
import org.treeo.treeo.ui.treemeasurement.TMViewModel
import org.treeo.treeo.util.INVENTORY_ID
import kotlin.properties.Delegates

class FinishMeasurementDialog : DialogFragment() {
    private var inventoryId by Delegates.notNull<Long>()
    private val viewModel: TMViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        inventoryId = arguments?.getLong(INVENTORY_ID)!!

        return inflater.inflate(
            R.layout.fragment_finish_measurement_dialog,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cancel.setOnClickListener {
            dismiss()
        }

        pause.setOnClickListener {
            // save and exit
            viewModel.cleanup()
            requireActivity().finish()
            dismiss()
        }

        finish.setOnClickListener {
            viewModel.processForestInventorySummary(inventoryId)
            viewModel.cleanup()
            viewModel.completeForestInventoryActivity(inventoryId)
            findNavController().navigate(R.id.inventorySummaryFragment)
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    companion object {
        @JvmStatic
        fun newInstance(inventoryId: Long): FinishMeasurementDialog {
            val dialog = FinishMeasurementDialog()
            val args = Bundle()
            args.putLong("inventoryId", inventoryId)
            dialog.arguments = args
            return dialog
        }
    }
}
