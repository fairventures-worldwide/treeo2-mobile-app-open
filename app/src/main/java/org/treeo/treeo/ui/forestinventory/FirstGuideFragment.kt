package org.treeo.treeo.ui.forestinventory

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
import kotlinx.android.synthetic.main.fragment_first_guide.*
import org.treeo.treeo.R
import org.treeo.treeo.databinding.FragmentFirstGuideBinding
import org.treeo.treeo.ui.treemeasurement.TMViewModel
import org.treeo.treeo.ui.treemeasurement.screens.AudioGuideUtils
import org.treeo.treeo.util.ADHOC_ACTIVITY_ID
import org.treeo.treeo.util.INVENTORY_ID

class FirstGuideFragment : Fragment() {

    private val viewModel: TMViewModel by activityViewModels()
    private val binding by viewBinding(FragmentFirstGuideBinding::bind)
    private lateinit var audioGuideUtils: AudioGuideUtils
    private var lm: LocationManager? = null
    private var gpsEnabled = false


    private fun checkIfGpsEnabled() {
        if (!gpsEnabled) {
            binding.continueBtn.isEnabled = false
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(getString(R.string.turn_on_location))
            builder.setMessage(getString(R.string.turn_on_location_desc))
            builder.setPositiveButton(getString(R.string.enable)) { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }

            builder.setNegativeButton(android.R.string.no) { _, _ ->
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first_guide, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.continueBtn.isEnabled = false
        binding.loading.isVisible = true;
        setObservers()
        viewModel.initializeMeasurementObject();
        setUpViews(view)
    }

    private fun setUpViews(view: View) {
        setUpToolbar(view)
        gpsEnabled = getGpsStatus()
        checkIfGpsEnabled()

        continue_btn.setOnClickListener {
            findNavController()
                .navigate(
                    R.id.requestCameraFragment,
                    bundleOf(
                        "navigateTo" to "forestInventory",
                        INVENTORY_ID to arguments?.getLong(INVENTORY_ID),
                        ADHOC_ACTIVITY_ID to arguments?.getLong(ADHOC_ACTIVITY_ID)
                    )
                )
        }
    }

    private fun setUpToolbar(view: View) {
        toolbar_guide.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar_guide.setNavigationOnClickListener {
            requireActivity().finish()
        }

        toolbar_guide.inflateMenu(R.menu.main_menu)
        toolbar_guide.setOnMenuItemClickListener {
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

    private fun getGpsStatus(): Boolean {
        try {
            return lm!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
            Log.d("error", ex.message.toString())
        }
        return false
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
            binding.continueBtn.isEnabled = true
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
