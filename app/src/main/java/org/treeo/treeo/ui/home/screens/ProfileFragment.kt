package org.treeo.treeo.ui.home.screens

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.offline_profile_sync_status.*
import kotlinx.android.synthetic.main.policy_buttons.*
import kotlinx.android.synthetic.main.profile_page_header.*
import org.treeo.treeo.R
import org.treeo.treeo.ui.authentication.AuthViewModel
import org.treeo.treeo.ui.home.HomeViewModel
import org.treeo.treeo.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    @Inject
    lateinit var sharedPref: SharedPreferences

    private val loginLogoutViewModel: AuthViewModel by viewModels()

    private val homeViewModel: HomeViewModel by viewModels()
    private var dataSize = " "


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpUI()
        hideAllProfileOfflineSyncStatus()
        setObservers()
        initializeButton()
    }

    override fun onResume() {
        homeViewModel.getOfflineSyncStatus()
        getDataToSync()
        super.onResume()
    }

    private fun setObservers() {
        loginLogoutViewModel.logoutResponse.observe(
            viewLifecycleOwner
        ) { logoutResponse ->
            if (logoutResponse != null) {
                deleteUserDetailsFromSharePref()
            } else {
                Toast.makeText(context, errors.value, Toast.LENGTH_LONG)
                    .show()
            }
        }

        homeViewModel.dataInBytes.observe(viewLifecycleOwner) {
            if (it > 0) {
                val bracket = "("
                dataSize = bracket.plus(it.toString()).plus("MBs)")
            }
        }

        homeViewModel.dataToSync.observe(viewLifecycleOwner) {
            if (it == 1) {
                data_to_sync.text =
                    it.toString().plus(resources.getString(R.string.to_sync_))
                        .plus(dataSize)
                data_sync_status2.text =
                    it.toString().plus(resources.getString(R.string.to_sync_))
                        .plus(dataSize)
                data_sync_status4.text =
                    it.toString().plus(resources.getString(R.string.to_sync_))
                        .plus(dataSize)
            } else {
                data_to_sync.text =
                    it.toString().plus(resources.getString(R.string.to_sync))
                        .plus(dataSize)
                data_sync_status2.text =
                    it.toString().plus(resources.getString(R.string.to_sync))
                        .plus(dataSize)
                data_sync_status4.text =
                    it.toString().plus(resources.getString(R.string.to_sync))
                        .plus(dataSize)
            }
        }

        homeViewModel.offlineSyncStatus.observe(viewLifecycleOwner) {
            setUIBasedOnOfflineSyncStatus(it)
        }

        homeViewModel.basicUserInfo.observe(viewLifecycleOwner) {
            tvUserProfileGreeting.text =
                String.format(getString(R.string.user_greeting), it.firstName)
            tvPhoneNumber.text = it.phoneNumber
        }
    }

    private fun updateDateOfLastSync() {
        val dateOfLastSync = homeViewModel.getDateOfLastSync()
        val lastSyncMessage = if (dateOfLastSync != null)
            String.format(
                getString(R.string.last_data_sync_was_done),
                dateOfLastSync
            )
        else
            getString(R.string.last_sync_date_not_application)
        date_of_sync.text = lastSyncMessage
        date_of_sync1.text = lastSyncMessage
        date_of_sync2.text = lastSyncMessage
        date_of_sync3.text = lastSyncMessage
        date_of_sync4.text = lastSyncMessage
    }

    private fun initializeButton() {
        profileLogoutButton.setOnClickListener {
            logoutUser()
        }
        sync_btn4.setOnClickListener {
            homeViewModel.apply {
                uploadQueueContent()
            }
        }
    }

    private fun setUpUI() {
        btn_privacy_policy.setOnClickListener{
            view?.findNavController()
                ?.navigate(
                    R.id.action_profileFragment_to_termsFragment2,
                    bundleOf(
                        "is_privacy_policy" to true,
                    )
                )
        }

        btn_terms_of_use.setOnClickListener{
            view?.findNavController()
                ?.navigate(
                    R.id.action_profileFragment_to_termsFragment2,
                    bundleOf(
                        "is_privacy_policy" to false,
                    )
                )
        }
    }

    private fun setUIBasedOnOfflineSyncStatus(offlineSyncStatus: Int) {
        hideAllProfileOfflineSyncStatus()
        when (offlineSyncStatus) {
            OFFLINE_WITH_DATA_TO_SYNC -> {
                offline_data_to_sync.visibility = View.VISIBLE
                updateDateOfLastSync()
            }
            OFFLINE_WITHOUT_DATA_TO_SYNC -> {
                offline_no_data_to_sync.visibility = View.VISIBLE
                updateDateOfLastSync()
            }
            ONLINE_WITH_DATA_TO_SYNC -> {
                setUpUIForOnlineWithData()
            }
            ONLINE_WITH_SYNC_IN_PROGRESS -> {
                online_with_sync_in_progress.visibility = View.VISIBLE
            }
            ONLINE_WITHOUT_DATA_TO_SYNC -> {
                online_with_no_data_to_sync.visibility = View.VISIBLE
                updateDateOfLastSync()
            }
            ONLINE_SYNC_SUCCESSFUL -> {
                online_with_no_data_to_sync.visibility = View.VISIBLE
                updateDateOfLastSync()
            }
        }
    }

    private fun setUpUIForOnlineWithData() {
        hideAllProfileOfflineSyncStatus()
        online_data_to_sync.visibility = View.VISIBLE
    }

    private fun hideAllProfileOfflineSyncStatus() {
        offline_data_to_sync.visibility = View.GONE
        offline_no_data_to_sync.visibility = View.GONE
        online_data_to_sync.visibility = View.GONE
        online_with_sync_in_progress.visibility = View.GONE
        online_with_no_data_to_sync.visibility = View.GONE
    }

    private fun getDataToSync() {
        homeViewModel.apply {
            getDataToSync()
            getBasicUserInfo()
        }
    }

    private fun logoutUser() {
        logoutFromBackend()
    }

    private fun logoutFromBackend() {
        val token = sharedPref.getString(
            getString(R.string.user_token),
            null
        )
        if (token != null) {
            loginLogoutViewModel.logout(token)
        } else {
            deleteUserDetailsFromSharePref()
        }
    }

    private fun deleteUserDetailsFromSharePref() {
        with(sharedPref.edit()) {
            remove(getString(R.string.user_token))
            remove(getString(R.string.user_name))
            remove(getString(R.string.user_phone_number))
            apply()
        }
        closeApp()
    }

    private fun closeApp() {
        findNavController().popBackStack()
        requireActivity().finish()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ProfileFragment()
    }
}
