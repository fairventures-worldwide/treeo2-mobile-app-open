package org.treeo.treeo.ui.treemeasurement.screens

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import org.treeo.treeo.R
import org.treeo.treeo.databinding.FragmentTreeMeasureIntroBinding
import org.treeo.treeo.ui.treemeasurement.TMViewModel

class TreeMeasureIntroFragment : Fragment() {

    private val binding by viewBinding(FragmentTreeMeasureIntroBinding::bind)
    private lateinit var audioGuideUtils: AudioGuideUtils
    private var lm: LocationManager? = null
    private var gpsEnabled = false
    private val viewModel: TMViewModel by activityViewModels()

    private fun getGpsStatus(): Boolean {
        try {
            return lm!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
            Log.d("error", ex.message.toString())
        }
        return false
    }


    private fun checkIfGpsEnabled() {
        if (!gpsEnabled) {
            binding.continueBtn.isEnabled = false
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(getString(R.string.turn_on_location))
            builder.setMessage(getString(R.string.turn_on_location_desc))
            builder.setPositiveButton(getString(R.string.enable)) { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }

            builder.setNegativeButton(getString(R.string.cancel)) { _, _ ->
                view?.findNavController()?.navigate(R.id.homeFragment)
            }

            builder.setNeutralButton("") { _, _ ->
            }
            builder.show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        lm =
            context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return inflater.inflate(
            R.layout.fragment_tree_measure_intro,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.continueBtn.isEnabled = false
        binding.loading.isVisible = true;
        setObservers()
        viewModel.initializeMeasurementObject();
        binding.continueBtn.setOnClickListener {
            findNavController()
                .navigate(
                    R.id.requestCameraFragment,
                    bundleOf(
                        "navigateTo" to "treeMeasurementCamera",
                    )
                )
        }

        gpsEnabled = getGpsStatus()

        checkIfGpsEnabled()

        binding.toolbar.apply {
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
    }


    private fun setObservers() {
        viewModel.hasInitializedMeasurementObject.observe(viewLifecycleOwner, Observer {
            if(it){
                binding.continueBtn.isEnabled = true
                binding.loading.isVisible = false;
            }
        })
    }



    override fun onResume() {
        super.onResume()
        if (getGpsStatus()) {
            binding.continueBtn.isEnabled = true;
        }
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
