package org.treeo.treeo.ui.treemeasurement.screens

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexboxLayoutManager
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import org.treeo.treeo.R
import org.treeo.treeo.adapters.TMSpeciesAdapter
import org.treeo.treeo.databinding.FragmentTmSelectSpeciesBinding
import org.treeo.treeo.ui.forestinventory.dialogs.FinishMeasurementDialog
import org.treeo.treeo.ui.treemeasurement.TMViewModel
import org.treeo.treeo.util.*
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
class TMSelectSpeciesFragment : Fragment() {
    private val viewModel: TMViewModel by activityViewModels()
    private val binding by viewBinding(FragmentTmSelectSpeciesBinding::bind)

    private var selectedSpecieCode = ""
    private var selectedSpecieName = ""
    private var selectedTreeHealth = "N/A"

    @Inject
    lateinit var sharedPref: SharedPreferences

    private lateinit var measurement: String
    private var co2InTonnes: String? = null
    private var manualDiameter: String = "N/A"
    private var isSkippedMeasurement by Delegates.notNull<Boolean>()

    private val finishMeasurementDialog by lazy {
        FinishMeasurementDialog.newInstance(arguments?.getLong(INVENTORY_ID)!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        measurement = arguments?.getString(MEASUREMENT).toString()
        isSkippedMeasurement = arguments?.getBoolean(SKIP_MEASUREMENT) == true
        return inflater.inflate(R.layout.fragment_tm_select_species, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpViews()
        viewModel.setUpObservers()

        if(isSkippedMeasurement){
            binding.diameterTextView.visibility = View.GONE
        }

        if (measurement == FOREST_INVENTORY) {
            binding.photoTitleTextView.visibility = View.GONE
            binding.toolbar.title = getString(R.string.tree_measurement)
        }
    }

    private fun setUpViews() {
        if (measurement == FOREST_INVENTORY) {
            customizeForestInventoryButtons()
        }

        handleFinishBtnClick()

        Glide.with(requireContext())
            .load(viewModel.getImagePath())
            .into(binding.displayPhoto)

        if (!viewModel.recordingSmallTree) {
            val diameter = String.format("%.2f", viewModel.treeDiameter.value)
            val diameterText = "$diameter mm"
            binding.diameterTextView.text = diameterText
        } else {
            hideView(binding.diameterTextView)
            hideView(binding.grpManualDiameter)
        }

        binding.toolbar.setUpToolbar()
        binding.handleTreeHealthSelection()
    }

    private fun customizeForestInventoryButtons() {
        binding.nextTree.visibility = View.VISIBLE
        binding.nextTree.setOnClickListener {
            if (viewModel.specieNotSpecified()) {
                requireContext().showToast(getString(R.string.message_select_specie))
            } else {
                viewModel.calculateCarbonAndSaveTreeMeasurement()
                findNavController().popBackStack(R.id.treeMeasurementCameraFragment, false)
            }
        }

        binding.finishBtn.text = getString(R.string.finish_or_pause)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.finishBtn.setTextColor(
                resources.getColor(
                    R.color.app_green,
                    context?.theme
                )
            )
        } else {
            binding.finishBtn.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.app_green
                )
            )
        }

        binding.finishBtn.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.green_outline_background)
    }

    private fun TMViewModel.calculateCarbonAndSaveTreeMeasurement() {
        if (!viewModel.recordingSmallTree) {
            co2InTonnes = "${calculateTreeCarbon()} Tonnes"
            setCO2(co2InTonnes)
            val manualDiameter =
                if (binding.manualDiameterInput.text.isNotBlank()) "${binding.manualDiameterInput.text} mm" else ""
            treeMeasurement?.manualDiameter = manualDiameter
        }

        // Additional data
        treeMeasurement?.run {
            treeHealth = selectedTreeHealth
            treeHeightMm = if (binding.manualHeightInput.text.isNullOrBlank()) {
                0
            } else {
                val treeHeightInMeters = binding.manualHeightInput.text.toString().toDouble()
                (treeHeightInMeters * 10).toInt()
            }

            comment = binding.commentInput.text.toString()
        }

        saveTreeMeasurement(measurement == FOREST_INVENTORY)

        // Mark activity as complete only for single tree measurements.
        if (measurement != FOREST_INVENTORY) {
            markActivityAsCompleted(getActivityId())
        }
    }

    private fun handleFinishBtnClick() {
        binding.finishBtn.setOnClickListener {
            if (viewModel.specieNotSpecified()) {
                requireContext().showToast(getString(R.string.message_select_specie))
            } else {
                viewModel.calculateCarbonAndSaveTreeMeasurement()
                if (measurement == FOREST_INVENTORY) {
                    finishMeasurementDialog.show(
                        requireActivity().supportFragmentManager,
                        "finish measurement"
                    )
                } else {
                    viewModel.tmSummaryInfoMap.run {
                        set("specie", selectedSpecieName)
                        set("carbon", co2InTonnes ?: "N/A")
                    }
                    findNavController().navigate(R.id.tmSummaryInfoFragment)
                }
            }
        }
    }

    private fun Toolbar.setUpToolbar() {
        if (measurement == FOREST_INVENTORY) {
            binding.photoTitleTextView.visibility = View.GONE
            title = getString(R.string.tree_measurement)
        }

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

    private fun FragmentTmSelectSpeciesBinding.handleTreeHealthSelection() {
        cardGoodHealth.setOnClickListener {
            updateTreeHealthState(GOOD_HEALTH)
        }

        cardBadHealth.setOnClickListener {
            updateTreeHealthState(BAD_HEALTH)
        }

        cardDeadHealth.setOnClickListener {
            updateTreeHealthState(DEAD_HEALTH)
        }
    }

    private fun FragmentTmSelectSpeciesBinding.updateTreeHealthState(health: String) {
        resetSelectedTreeHealthUI()

        selectedTreeHealth = health
        when (selectedTreeHealth) {
            GOOD_HEALTH -> {
                cLGoodHealth.updateColor(R.color.app_green)
                icGoodHealth.updateColor(R.color.white)
                labelGoodHealth.updateColor(R.color.white)
            }
            BAD_HEALTH -> {
                cLBadHealth.updateColor(R.color.app_green)
                icBadHealth.updateColor(R.color.white)
                labelBadHealth.updateColor(R.color.white)
            }
            DEAD_HEALTH -> {
                cLDeadHealth.updateColor(R.color.app_green)
                icDeadHealth.updateColor(R.color.white)
                labelDeadHealth.updateColor(R.color.white)
            }
        }

        validateRequiredFieldsAndEnableActionButtons()
    }

    private fun FragmentTmSelectSpeciesBinding.resetSelectedTreeHealthUI() {
        when (selectedTreeHealth) {
            GOOD_HEALTH -> {
                cLGoodHealth.updateColor(R.color.white)
                icGoodHealth.updateColor(Color.parseColor("#00601E"), false)
                labelGoodHealth.updateColor(R.color.black)
            }
            BAD_HEALTH -> {
                cLBadHealth.updateColor(R.color.white)
                icBadHealth.updateColor(Color.parseColor("#A9001E"), false)
                labelBadHealth.updateColor(R.color.black)
            }
            DEAD_HEALTH -> {
                cLDeadHealth.updateColor(R.color.white)
                icDeadHealth.updateColor(Color.parseColor("#3C3932"), false)
                labelDeadHealth.updateColor(R.color.black)
            }
        }
    }

    private fun ConstraintLayout.updateColor(color: Int) {
        setBackgroundColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun ImageView.updateColor(color: Int, isResolvedColor: Boolean = true) {
        if (isResolvedColor) {
            setColorFilter(ContextCompat.getColor(requireContext(), color))
        } else {
            setColorFilter(color)
        }
    }

    private fun TextView.updateColor(color: Int) {
        setTextColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun TMViewModel.setUpObservers() {
        treeSpecies.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                updateFragmentWithSpecieDetails("N/A", "Unknown Specie")
                hideView(binding.grpSpeciesHeading)
            } else {
                binding.rvTreeSpecies.apply {
                    layoutManager = FlexboxLayoutManager(requireContext())
                    setHasFixedSize(true)
                    adapter = TMSpeciesAdapter(
                        it,
                        selectedSpecieCode,
                        this@TMSelectSpeciesFragment::onSpecieSelected,
                        getUserLanguage(),
                    )
                }
            }
        }

        additionalDataConfigLoaded.observe(viewLifecycleOwner) {
            if (it) {
                hideHiddenAdditionalDataInputs()
                labelOptionalAdditionalDataInputs()
                labelAndValidateRequiredAdditionalDataInputs()
            }
        }

        getTreeSpecies()
        getAdditionalDataConfig()
    }

    private fun updateFragmentWithSpecieDetails(specieCode: String, specieName: String) {
        selectedSpecieCode = specieCode
        selectedSpecieName = specieName
        viewModel.setSpecie(selectedSpecieCode)
        validateRequiredFieldsAndEnableActionButtons()
    }

    private fun validateRequiredFieldsAndEnableActionButtons() {
        if (requiredInputFieldsAreValid()) {
            enableView(binding.nextTree)
            enableView(binding.finishBtn)
        } else {
            disableView(binding.nextTree)
            disableView(binding.finishBtn)
        }
    }

    private fun requiredInputFieldsAreValid(): Boolean {
        val requiredFields = viewModel.getRequiredAdditionalDataInputFields()
        val validFields = arrayListOf<String>()
        for (field in requiredFields) {
            when (field) {
                TREE_HEALTH -> {
                    if (selectedTreeHealth != "N/A") {
                        validFields.add(TREE_HEALTH)
                    }
                }
                MANUAL_DBH -> {
                    if (!binding.manualDiameterInput.text.isNullOrBlank()) {
                        validFields.add(MANUAL_DBH)
                    }
                }
                MANUAL_HEIGHT -> {
                    if (!binding.manualHeightInput.text.isNullOrBlank()) {
                        validFields.add(MANUAL_HEIGHT)
                    }
                }
                TREE_COMMENT -> {
                    if (!binding.commentInput.text.isNullOrBlank()) {
                        validFields.add(TREE_COMMENT)
                    }
                }
            }
        }
        return validFields.size == requiredFields.size
    }

    private fun hideHiddenAdditionalDataInputs() {
        for (field in viewModel.getHiddenAdditionalDataInputFields()) {
            when (field) {
                TREE_HEALTH -> {
                    hideView(binding.grpTreeHealth)
                }
                MANUAL_DBH -> {
                    hideView(binding.grpManualDiameter)
                }
                MANUAL_HEIGHT -> {
                    hideView(binding.grpManualHeight)
                }
                TREE_COMMENT -> {
                    hideView(binding.grpComment)
                }
            }
        }
    }

    private fun labelOptionalAdditionalDataInputs() {
        for (field in viewModel.getOptionalAdditionalDataInputFields()) {
            when (field) {
                TREE_HEALTH -> {
                    binding.tvRequiredOrOptional1.text = getString(R.string.label_optional)
                }
                MANUAL_DBH -> {
                    binding.tvRequiredOrOptional2.text = getString(R.string.label_optional)
                }
                MANUAL_HEIGHT -> {
                    binding.tvRequiredOrOptional3.text = getString(R.string.label_optional)
                }
                TREE_COMMENT -> {
                    binding.tvRequiredOrOptional4.text = getString(R.string.label_optional)
                }
            }
        }
    }

    private fun labelAndValidateRequiredAdditionalDataInputs() {
        for (field in viewModel.getRequiredAdditionalDataInputFields()) {
            when (field) {
                TREE_HEALTH -> {
                    binding.tvRequiredOrOptional1.text = getString(R.string.label_required)
                }
                MANUAL_DBH -> {
                    binding.manualDiameterInput.addTextChangedListener {
                        validateRequiredFieldsAndEnableActionButtons()
                    }
                    binding.tvRequiredOrOptional2.text = getString(R.string.label_required)
                }
                MANUAL_HEIGHT -> {
                    binding.manualHeightInput.addTextChangedListener {
                        validateRequiredFieldsAndEnableActionButtons()
                    }
                    binding.tvRequiredOrOptional3.text = getString(R.string.label_required)
                }
                TREE_COMMENT -> {
                    binding.commentInput.addTextChangedListener {
                        validateRequiredFieldsAndEnableActionButtons()
                    }
                    binding.tvRequiredOrOptional4.text = getString(R.string.label_required)
                }
            }
        }

        validateRequiredFieldsAndEnableActionButtons()
    }

    private fun onSpecieSelected(specieCode: String, specieName: String) {
        updateFragmentWithSpecieDetails(specieCode, specieName)

        // Require recycler view to redraw its items
        binding.rvTreeSpecies.adapter =
            TMSpeciesAdapter(
                viewModel.treeSpecies.value!!,
                specieCode,
                this::onSpecieSelected,
                viewModel.getUserLanguage(),
            )
    }

    companion object {
        private const val GOOD_HEALTH = "good"
        private const val BAD_HEALTH = "bad"
        private const val DEAD_HEALTH = "dead"
    }
}
