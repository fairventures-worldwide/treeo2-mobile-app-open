package org.treeo.treeo.ui.treemeasurement

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import org.treeo.treeo.R
import org.treeo.treeo.util.BIG_AND_SMALL_TREES
import org.treeo.treeo.util.INVENTORY_ID
import org.treeo.treeo.util.IS_FROM_WHOLE_FIELD_BOTTOM_NAV_TAP
import org.treeo.treeo.util.TREE_TYPE

@AndroidEntryPoint
class TreeMeasurementActivity : AppCompatActivity() {
    private val viewModel: TMViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tree_measurement)

        val isFromBottomNavTap = intent.getBooleanExtra(IS_FROM_WHOLE_FIELD_BOTTOM_NAV_TAP, false)
        if (isFromBottomNavTap) {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navHostFragment.navController.apply {
                popBackStack()
                navigate(
                    R.id.selectWholeFieldFragment,
                )
            }
        } else {
            viewModel.treeType = intent.getStringExtra(TREE_TYPE) ?: BIG_AND_SMALL_TREES
            val forestInventoryId = intent.getLongExtra(INVENTORY_ID, -1L)
            if (forestInventoryId != -1L) {
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                navHostFragment.navController.apply {
                    popBackStack()
                    navigate(
                        R.id.preparationFragment,
                        bundleOf(INVENTORY_ID to forestInventoryId)
                    )
                }
            }
        }
    }
}
