package org.treeo.treeo.ui.landsurvey.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.fragment_land_photos.*
import kotlinx.android.synthetic.main.fragment_request_camera_use.view.*
import org.treeo.treeo.R
import org.treeo.treeo.models.ActivitySummaryItem
import org.treeo.treeo.models.LandSurveySummaryItem

class LandPhotosFragment : Fragment() {

    private lateinit var indexMap: Map<String, Int>
    private var bundleItem: ActivitySummaryItem? = null
    private lateinit var summaryItem: LandSurveySummaryItem


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bundleItem = arguments?.getParcelable("summaryItem")
        summaryItem = bundleItem as LandSurveySummaryItem
        indexMap = arguments?.get("indexMap") as Map<String, Int>
        return inflater.inflate(R.layout.fragment_land_photos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        view.toolbar.inflateMenu(R.menu.main_menu)
        view.toolbar.setNavigationOnClickListener {
            view.findNavController().popBackStack()
        }
        initializeButton()

        view.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.exit_btn -> {
                    view.findNavController()
                        .navigate(R.id.action_landPhotosFragment_to_homeFragment)
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun initializeButton() {
        btn_continue.setOnClickListener {
            view?.findNavController()
                ?.navigate(
                    R.id.landCornersFragment,
                    bundleOf(
                        "summaryItem" to summaryItem,
                        "indexMap" to indexMap
                    )
                )
        }
    }
}
