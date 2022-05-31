package org.treeo.treeo.ui.landsurvey.screens

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_finish_land_specification.*
import kotlinx.android.synthetic.main.fragment_request_camera_use.view.*
import kotlinx.android.synthetic.main.offline_sync_status_indicator.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.treeo.treeo.R
import org.treeo.treeo.models.ActivitySummaryItem
import org.treeo.treeo.models.LandSurveySummaryItem
import org.treeo.treeo.ui.home.HomeViewModel
import org.treeo.treeo.ui.landsurvey.LandSurveyViewModel
import org.treeo.treeo.util.*
import javax.inject.Inject

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class FinishLandSpecificationFragment : Fragment() {

    @Inject
    lateinit var sharedPref: SharedPreferences
    private val landSurveyViewModel: LandSurveyViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    private var bundleItem: ActivitySummaryItem? = null
    private lateinit var indexMap: Map<String, Int>
    private lateinit var summaryItem: LandSurveySummaryItem
    private var isAlreadyComplete = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bundleItem = arguments?.getParcelable("summaryItem")
        summaryItem = bundleItem as LandSurveySummaryItem
        indexMap = arguments?.get("indexMap") as Map<String, Int>
        isAlreadyComplete = arguments?.getBoolean("isAlreadyComplete") ?: false
        return inflater.inflate(R.layout.fragment_finish_land_specification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        view.toolbar.inflateMenu(R.menu.main_menu)
        view.toolbar.setNavigationOnClickListener {
            view.findNavController().popBackStack()
        }

        if (!isAlreadyComplete) {
            landSurveyViewModel.markLandSurveyAsCompleted(summaryItem.landSurvey.surveyId!!)

            if (indexMap["itemPosition"] == indexMap["listSize"]) {
                summaryItem.activity?.id?.let { landSurveyViewModel.markActivityAsCompleted(it) }
            }
        }

        initializeButton()
        setObservers()
    }

    private fun initializeButton() {
        continueToDashboardButton.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }

        to_profile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        autoSyncQueueData()
    }

    private fun autoSyncQueueData() {
        homeViewModel.apply {
            uploadQueueContent()
            getOfflineSyncStatus()
        }
    }

    private fun setObservers() {
        homeViewModel.offlineSyncStatus.observe(viewLifecycleOwner) {
            onOfflineSyncStatusUI(it)
        }
    }

    private fun onOfflineSyncStatusUI(offlineSyncStatus: Int) {
        hideAllOfflineViews()

        when (offlineSyncStatus) {
            OFFLINE_WITH_DATA_TO_SYNC -> {
                cLOfflineWithData.visibility = View.VISIBLE
                btnGoToSettings.setOnClickListener {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
            ONLINE_WITH_SYNC_IN_PROGRESS -> {
                onlineWithSyncInProgressUI()
            }
            ONLINE_WITHOUT_DATA_TO_SYNC -> {
                offlineSyncIndicator.visibility = View.GONE
            }
            ONLINE_SYNC_SUCCESSFUL -> {
                cLOnlineSyncedData.visibility = View.VISIBLE
                btnContinueAfterUpload.setOnClickListener {
                    homeViewModel.acknowledgeSuccessfulSync()
                    onOfflineSyncStatusUI(ONLINE_WITHOUT_DATA_TO_SYNC)
                }
            }
        }
    }

    private fun hideAllOfflineViews() {
        cLOfflineInfo.visibility = View.GONE
        cLOfflineMode.visibility = View.GONE
        cLOfflineWithData.visibility = View.GONE
        cLUnfinishedActivity.visibility = View.GONE
        cLOnlineSyncingData.visibility = View.GONE
        cLOnlineSyncedData.visibility = View.GONE
        offlineSyncIndicator.visibility = View.VISIBLE
    }

    private fun onlineWithSyncInProgressUI() {
        cLOnlineSyncingData.visibility = View.VISIBLE
        lifecycleScope.launch {
            val sizeLeft = homeViewModel.getBytesLeftToUploadInQueue()
            val initialTotal = homeViewModel.getTotalBytesAtStartOfSync()
            val bytesSynced = initialTotal - sizeLeft
            var syncPercentage =
                ((bytesSynced.toDouble() / initialTotal.toDouble()) * 100.0).toInt()
            if (syncPercentage < 0) {
                syncPercentage = 0
            }
            uploadProgressBar.progress = syncPercentage
            tvUploadProgress.text = String.format(
                getString(R.string.upload_percentage_placeholder),
                syncPercentage.toString(),
                humanReadableByteCountSI(initialTotal)
            )
        }
    }
}
