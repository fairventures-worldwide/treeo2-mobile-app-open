package org.treeo.treeo.ui.treemeasurement.screens.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_processing_image_dialog.*
import org.treeo.treeo.R
import org.treeo.treeo.ui.treemeasurement.TMViewModel
import org.treeo.treeo.ui.treemeasurement.screens.TreeMeasurementCameraFragment
import org.treeo.treeo.util.*
import kotlin.properties.Delegates

class ProcessingImageDialog : DialogFragment() {

    private lateinit var listener: TreeMeasurementCameraFragment
    private val viewModel: TMViewModel by activityViewModels()
    private lateinit var measurement: String
    private var inventoryId by Delegates.notNull<Long>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        measurement = arguments?.getString(MEASUREMENT).toString()
        inventoryId = arguments?.getLong(INVENTORY_ID)!!

        return inflater.inflate(
            R.layout.fragment_processing_image_dialog,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeDialogState()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    private fun initializeDialogState() {
        showView(processingImageLinearLayout)
        hideView(processingImageErrorLinearLayout)
    }

    fun setOnRetakeListener(fragment: TreeMeasurementCameraFragment) {
        listener = fragment
    }

    fun navigateToNextScreen(isSkippedMeasurement: Boolean = false) {
        findNavController().navigate(
            R.id.TMResultsFragment,
            bundleOf(
                MEASUREMENT to measurement,
                INVENTORY_ID to inventoryId,
                SKIP_MEASUREMENT to isSkippedMeasurement
            )
        )
        dismiss()
    }

    fun showMeasurementError(
        onRetake: () -> Unit,
        onSkipNavigation: () -> Unit,
        retakesCount: Int
    ) {
        try {
            if (viewModel.recordingSmallTree) {
                retakePhotoDescription.text = getString(R.string.description_retake_small_tree)
            } else {
                retakePhotoDescription.text = getString(R.string.text_bad_photo_hint)
            }

            hideView(processingImageLinearLayout)
            showView(processingImageErrorLinearLayout)


            if (retakesCount > viewModel.currentRetryTimes!!) {
                skipMeasurementDialogButton.visibility = View.VISIBLE
            }

            skipMeasurementDialogButton.setOnClickListener {
                onSkipNavigation()
                dismiss()
            }


            retakePhotoDialogButton.setOnClickListener {
                viewModel.run {
                    if (!recordingSmallTree) {
                        setMeasurementType(TREE_MEASUREMENT_UNSUCCESSFUL)
                        saveTreeMeasurement()
                    }
                }
                onRetake()
                listener.restartCamera()
                dismiss()
            }
        } catch (e: Exception) {
            // Do nothing
        }
    }

    interface OnRetakeListener {
        fun restartCamera()
    }

    companion object {
        @JvmStatic
        fun newInstance(navigateTo: String, inventoryId: Long?): ProcessingImageDialog {
            val dialog = ProcessingImageDialog()
            val args = Bundle()
            args.putString(MEASUREMENT, navigateTo)
            if (inventoryId != null) {
                args.putLong(INVENTORY_ID, inventoryId)
            }
            dialog.arguments = args
            return dialog
        }
    }
}
