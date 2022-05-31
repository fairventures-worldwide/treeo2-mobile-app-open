package org.treeo.treeo.ui.treemeasurement.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import org.treeo.treeo.R
import org.treeo.treeo.databinding.BottomSheetTipsLayoutBinding

class BottomSheetTipsDialog : BottomSheetDialogFragment() {

    private val binding by viewBinding(BottomSheetTipsLayoutBinding::bind)
    private lateinit var audioGuideUtils: AudioGuideUtils

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.bottom_sheet_tips_layout,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.closeBtn.setOnClickListener {
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        audioGuideUtils = AudioGuideUtils(
            requireContext(),
            lifecycleScope,
            binding.audioComponent
        ).apply { setupAudioGuide() }
    }

    override fun onPause() {
        super.onPause()
        audioGuideUtils.pauseAudioGuide()
    }
}
