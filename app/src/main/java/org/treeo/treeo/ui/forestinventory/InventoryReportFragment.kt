package org.treeo.treeo.ui.forestinventory

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_inventory_report.*
import org.treeo.treeo.R
import org.treeo.treeo.adapters.PredictionsListAdapter
import org.treeo.treeo.adapters.WhatsNewRecyclerAdapter
import org.treeo.treeo.models.PredictionItem
import org.treeo.treeo.models.WhatsNew
import org.treeo.treeo.ui.MainActivity
import org.treeo.treeo.ui.treemeasurement.TMViewModel
import org.treeo.treeo.util.showToast
import javax.inject.Inject

class InventoryReportFragment : Fragment() {

    @Inject
    lateinit var sharedPref: SharedPreferences

    private val sharedPrefFile = "showDialogsPrefFile"
    private val bestHarvestTime: String = "Best harvest time"

    private val viewModel: TMViewModel by activityViewModels()
    private var predictionsList = ArrayList<PredictionItem>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPref = requireContext().getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)
        setUpViews(view)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inventory_report, container, false)
    }

    private fun setUpViews(view: View) {

        val seenBestHarvestDialog: Boolean = sharedPref.getBoolean(
            getString(R.string.seen_harvest_dialog),
            false
        )
        val potentialValueDialog = sharedPref.getBoolean(
            getString(R.string.seen_potential_value_dialog),
            false
        )
        val seenCarbonCreditDialog = sharedPref.getBoolean(
            getString(R.string.seen_carbon_credits_dialog),
            false
        )
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            findNavController()
                .popBackStack()
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

        goToTheDashboard.setOnClickListener {
            requireActivity().finish()
        }
        seeProfileBtn.setOnClickListener {

            // TODO: Return activity result
            requireActivity().finish()
        }


        if (seenBestHarvestDialog) {
            showHarvestTimeDialog()
        }
        if (potentialValueDialog) {
            showPotentialValueDialog()
        }
        if (seenCarbonCreditDialog) {
            showCarbonCreditsDialog()
        }

        viewModel.tmSummaryInfoMap.apply {
            item_numberOfTrees_value.text = get("Total Trees")
            item_avg_diameter_value.text = get("Avg. Diameter")
            item_estimated_price_value.text = "N/A"
        }

        predictionsList.add(
            PredictionItem(
                bestHarvestTime,
                "N/A",
                "Estimated best time to harvest to reach the highest potential and price"
            )
        )
        predictionsList.add(
            PredictionItem(
                "Potential value",
                "N/A",
                "Price may vary according to market value"
            )
        )
        predictionsList.add(
            PredictionItem(
                "Carbon credits",
                "N/A",
                "By keeping your trees growing more than 10 years, you are eligible to receive finance from carbon credits"
            )
        )

        val adapter = PredictionsListAdapter(predictionsList)
        predictions_recyclerview.adapter = adapter
        predictions_recyclerview.layoutManager = LinearLayoutManager(requireContext())

        leWhatsNewRecycler.adapter =
            WhatsNewRecyclerAdapter(requireContext(), getWhatsNewList()) {
                navigateToPage(R.id.learnFragment, getString(R.string.message_not_yet_implemented))
            }
        leWhatsNewRecycler.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
    }

    private fun navigateToPage(pageId: Int, errorMessage: String) {
        try {
            (requireActivity() as MainActivity).navigateToPage(pageId)
        } catch (e: Exception) {
            requireContext().showToast(errorMessage)
        }
    }

    private fun showHarvestTimeDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(bestHarvestTime)
        builder.setMessage("The best predicted time for you to harvest the trees. To get the highest potential and best market price. This time may vary according to climate and soil conditions.")

        builder.setPositiveButton(getText(R.string.i_understand)) { _, _ ->
            with(sharedPref.edit()) {
                putBoolean(getString(R.string.seen_harvest_dialog), true)
                apply()
            }
        }

        builder.setNegativeButton(getText(R.string.close)) { _, _ ->
            findNavController().navigate(R.id.homeFragment)
        }

        builder.show()
    }


    private fun showCarbonCreditsDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Carbon credits")
        builder.setMessage("By keeping your trees growing more than 10 years you are eligigible to receive finance from carbon credits.")

        builder.setPositiveButton(getText(R.string.i_understand)) { _, _ ->
            with(sharedPref.edit()) {
                putBoolean(getString(R.string.seen_carbon_credits_dialog), true)
                apply()
            }
        }

        builder.setNegativeButton(getText(R.string.close)) { _, _ ->
            findNavController().navigate(R.id.homeFragment)
        }

        builder.show()
    }

    private fun showPotentialValueDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Potential value")
        builder.setMessage("The best predicted time for you to harvest the trees. To get the highest potential and best market price. This time may vary according to climate and soil conditions.")

        builder.setPositiveButton(getText(R.string.i_understand)) { _, _ ->
            with(sharedPref.edit()) {
                putBoolean(getString(R.string.seen_potential_value_dialog), true)
                apply()
            }
        }

        builder.setNegativeButton(getText(R.string.close)) { _, _ ->
            findNavController().navigate(R.id.homeFragment)
        }

        builder.show()
    }

    private fun getWhatsNewList(): List<WhatsNew> {
        return listOf(
            WhatsNew(
                R.drawable.trees_1,
                "What the fern?",
                resources.getString(R.string.lorem_ipsum)
            ),
            WhatsNew(
                R.drawable.trees_2,
                "Get started with Baobab",
                resources.getString(R.string.lorem_ipsum)
            ),
            WhatsNew(
                R.drawable.trees_3,
                "Understanding Eucalyptus",
                resources.getString(R.string.lorem_ipsum)
            )
        )
    }

    companion object {
        @JvmStatic
        fun newInstance() = InventoryReportFragment()
    }
}
