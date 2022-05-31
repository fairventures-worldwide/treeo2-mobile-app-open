package org.treeo.treeo.ui.home.screens

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Context.CAMERA_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.home_page_header.*
import kotlinx.android.synthetic.main.offline_sync_status_indicator.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.treeo.treeo.R
import org.treeo.treeo.adapters.HomeGuideListener
import org.treeo.treeo.adapters.HomeGuideRecyclerAdapter
import org.treeo.treeo.adapters.WhatsNewRecyclerAdapter
import org.treeo.treeo.models.Activity
import org.treeo.treeo.models.WhatsNew
import org.treeo.treeo.ui.MainActivity
import org.treeo.treeo.ui.authentication.AuthViewModel
import org.treeo.treeo.ui.home.HomeViewModel
import org.treeo.treeo.ui.treemeasurement.TMViewModel
import org.treeo.treeo.ui.treemeasurement.TMViewModel.Companion.currentActivityId
import org.treeo.treeo.ui.treemeasurement.TMViewModel.Companion.currentActivityType
import org.treeo.treeo.ui.treemeasurement.TMViewModel.Companion.currentActivityUUID
import org.treeo.treeo.ui.treemeasurement.TreeMeasurementActivity
import org.treeo.treeo.util.*
import javax.inject.Inject

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class HomeFragment : Fragment(), HomeGuideListener {

    @Inject
    lateinit var sharedPref: SharedPreferences

    @Inject
    lateinit var deviceInfoUtils: DeviceInfoUtils

    @Inject
    lateinit var dispatcher: IDispatcherProvider

    private val loginLogoutViewModel: AuthViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val tmViewModel: TMViewModel by viewModels()

    private lateinit var homeGuideRecyclerAdapter: HomeGuideRecyclerAdapter

    private var selectedLanguage = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseCrashlytics.getInstance().setUserId(getUserId().toString())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedLanguage = getSelectedLanguage()
        setUpViews()
        setObservers()
        homeViewModel.getBasicUserInfo()
    }

    override fun onStart() {
        super.onStart()
        getDeviceInformation()
    }

    override fun onResume() {
        super.onResume()
        autoSyncQueue()
        homeViewModel.syncTreeSpecies()
        homeViewModel.getRemotePlannedActivities()
    }

    private fun autoSyncQueue() {
        homeViewModel.apply {
            uploadQueueContent()
            getOfflineSyncStatus()
        }
    }

    private fun setUpViews() {
        setUpWhatsNewRecycler()
        setUpHomeGuideRecycler()
        settingsButton.setOnClickListener {
            navigateToPage(R.id.profileFragment, getString(R.string.message_cannot_got_to_settings))
        }
        labelSeeAll.setOnClickListener {
            navigateToPage(R.id.guideFragment, getString(R.string.message_not_yet_implemented))
        }
    }

    private fun navigateToPage(pageId: Int, errorMessage: String) {
        try {
            (requireActivity() as MainActivity).navigateToPage(pageId)
        } catch (e: Exception) {
            requireContext().showToast(errorMessage)
        }
    }

    private fun setUpHomeGuideRecycler() {
        homeGuideRecyclerAdapter = HomeGuideRecyclerAdapter(selectedLanguage, this)
        homeGuideRecycler.adapter = homeGuideRecyclerAdapter
        homeGuideRecycler.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )
    }

    private fun setUpWhatsNewRecycler() {
        whatsNewRecycler.adapter =
            WhatsNewRecyclerAdapter(requireContext(), getWhatsNewList()) {
                navigateToPage(R.id.learnFragment, getString(R.string.message_not_yet_implemented))
            }
        whatsNewRecycler.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
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

    private fun setObservers() {
        homeViewModel.nextTwoActivities.observe(
            viewLifecycleOwner
        ) { activities ->
            if (activities.isNotEmpty()) {
                if (activities.size > 2) {
                    homeGuideRecyclerAdapter.submitList(activities.subList(0, 2))
                } else {
                    homeGuideRecyclerAdapter.submitList(activities)
                }
                hideView(outOfActivitiesBanner)
            } else {
                showView(outOfActivitiesBanner)
            }
        }

        homeViewModel.offlineSyncStatus.observe(viewLifecycleOwner) {
            setUIBasedOnOfflineSyncStatus(it)
        }

        homeViewModel.basicUserInfo.observe(viewLifecycleOwner) {
            tvUserGreeting.text =
                String.format(getString(R.string.user_greeting), it.firstName)
        }
    }

    private fun setUIBasedOnOfflineSyncStatus(offlineSyncStatus: Int) {
        hideAllOfflineSyncStatusIndicators()

        when (offlineSyncStatus) {
            OFFLINE_WITH_DATA_TO_SYNC -> {
                cLOfflineWithData.visibility = View.VISIBLE
                btnGoToSettings.setOnClickListener {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
            OFFLINE_WITHOUT_DATA_TO_SYNC -> {
                val offlineInfoDismissed = sharedPref.getBoolean(
                    getString(R.string.offline_info_dismissed),
                    false
                )
                if (offlineInfoDismissed) {
                    cLOfflineMode.visibility = View.VISIBLE
                } else {
                    cLOfflineInfo.visibility = View.VISIBLE
                    btnContinueOffline.setOnClickListener {
                        with(sharedPref.edit()) {
                            putBoolean(
                                getString(R.string.offline_info_dismissed),
                                true
                            )
                            apply()
                            hideAllOfflineSyncStatusIndicators()
                            cLOfflineMode.visibility = View.VISIBLE
                        }
                    }
                }
            }
            ONLINE_WITH_SYNC_IN_PROGRESS -> {
                setUIForOnlineWithSyncInProgress()
            }
            ONLINE_WITHOUT_DATA_TO_SYNC -> {
                offlineSyncIndicator.visibility = View.GONE
            }
            ONLINE_SYNC_SUCCESSFUL -> {
                cLOnlineSyncedData.visibility = View.VISIBLE
                btnContinueAfterUpload.setOnClickListener {
                    homeViewModel.acknowledgeSuccessfulSync()
                    setUIBasedOnOfflineSyncStatus(ONLINE_WITHOUT_DATA_TO_SYNC)
                }
            }
        }
    }

    private fun hideAllOfflineSyncStatusIndicators() {
        cLOfflineInfo.visibility = View.GONE
        cLOfflineMode.visibility = View.GONE
        cLOfflineWithData.visibility = View.GONE
        cLUnfinishedActivity.visibility = View.GONE
        cLOnlineSyncingData.visibility = View.GONE
        cLOnlineSyncedData.visibility = View.GONE
        offlineSyncIndicator.visibility = View.VISIBLE
    }

    private fun setUIForOnlineWithSyncInProgress() {
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

    private fun getDeviceInformation() {
        val saveDeviceInfoStatus: Boolean =
            sharedPref.getDeviceInfoNeedStatus(requireContext())
        if (!saveDeviceInfoStatus) {
            GlobalScope.launch(dispatcher.main()) {
                loginLogoutViewModel.postDeviceData(
                    deviceInfoUtils.getDeviceInformation(
                        requireActivity().getSystemService(ACTIVITY_SERVICE) as ActivityManager,
                        requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager,
                        requireActivity().packageManager,
                        requireActivity().getSystemService(CAMERA_SERVICE) as CameraManager
                    ),
                    getUserToken()
                )
                setDeviceInfoNeedStatus(sharedPref, requireContext())
            }
        }
    }

    private fun getUserToken(): String {
        with(sharedPref.edit()) {
            val token =
                sharedPref.getString(getString(R.string.user_token), null)
            if (!token.isNullOrEmpty()) {
                return "Bearer $token"
            }
            apply()
        }
        return ""
    }

    private fun getUserId(): Int {
        with(sharedPref.edit()) {
            val id =
                sharedPref.getInt(resources.getString(R.string.user_id), 0)
            if (id != 0) {
                return id
            }
            apply()
        }
        return 0
    }

    override fun onHomeGuideClick(activity: Activity) {
        homeViewModel.setActivityStartDate(activity.id)
        if (activity.template.activityType == "tree_survey" && !activity.isCompleted) {
            lifecycleScope.launch {

                try {
                    val inventoryId: Long
                    // check if any inventory exists under the given id
                    val incompleteInventory =
                        homeViewModel.checkIncompleteInventory(activityId = activity.id)

                    inventoryId = incompleteInventory?.forestInventoryId
                        ?: homeViewModel.startNewForestInventory(activity.id)
                    currentActivityUUID = activity.uuid
                    currentActivityId = activity.id
                    currentActivityType = activity.type
                    tmViewModel.currentRetryTimes = activity.configuration?.retryTimes
                    startActivity(
                        Intent(
                            requireActivity(),
                            TreeMeasurementActivity::class.java
                        ).apply {
                            putExtra(ADHOC_ACTIVITY_ID, activity.id)
                            putExtra(INVENTORY_ID, inventoryId)
                            putExtra(ACTIVITY_TYPE, activity.type)
                        }
                    )
                } catch (e: Exception) {
                    requireContext().showToast(e.toString())
                }
            }
        } else {
            findNavController().navigate(
                R.id.activitySummaryFragment,
                bundleOf("activity" to activity)
            )
        }
    }

    private fun getSelectedLanguage() = sharedPref.getString(SELECTED_LANGUAGE, "en")!!

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}
