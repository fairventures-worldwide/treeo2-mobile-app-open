package org.treeo.treeo.ui.landsurvey.screens

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.android.synthetic.main.fragment_request_camera_use.*
import kotlinx.android.synthetic.main.fragment_request_camera_use.view.*
import org.treeo.treeo.R
import org.treeo.treeo.models.ActivitySummaryItem
import org.treeo.treeo.models.LandSurveySummaryItem
import org.treeo.treeo.util.ADHOC_ACTIVITY_ID
import org.treeo.treeo.util.FOREST_INVENTORY
import org.treeo.treeo.util.INVENTORY_ID
import org.treeo.treeo.util.MEASUREMENT
import kotlin.properties.Delegates

/**
 *  Calls for special permissions to run activities on the app
 */
class RequestCameraFragment : Fragment(), EasyPermissions.PermissionCallbacks {
    private lateinit var indexMap: Map<String, Int>
    private var bundleItem: ActivitySummaryItem? = null
    private lateinit var summaryItem: LandSurveySummaryItem
    private lateinit var navigateTo: String
    private var inventoryId by Delegates.notNull<Long>()
    private var adhocActivityId by Delegates.notNull<Long>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        navigateTo = arguments?.getString("navigateTo").toString()
        inventoryId = arguments?.getLong(INVENTORY_ID)!!
        adhocActivityId = arguments?.getLong(ADHOC_ACTIVITY_ID)!!
        if (navigateTo == "landSurveyCamera") {
            bundleItem = arguments?.getParcelable("summaryItem")
            summaryItem = bundleItem as LandSurveySummaryItem
            indexMap = arguments?.get("indexMap") as Map<String, Int>
        }
        return inflater.inflate(R.layout.fragment_request_camera_use, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.toolbar.inflateMenu(R.menu.main_menu)
        view.toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        view.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        requestPermissionsOrNavigateAway()
        initializeButton()

        if(navigateTo == "forest inventory"){
            view.toolbar.title = "Tree Measurement"
        }

        if(navigateTo == "treeMeasurementCamera"){
            view.toolbar.title = "1 Tree Measurement"
        }

        setUpViewDetails(
            PageDetails(
                R.drawable.ic_location,
                "ALLOW SHARING YOUR LOCATION FOR USE IN TREEO APP",
                "TURN ON LOCATION"
            )
        )

        view.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.exit_btn -> {
                    view.findNavController()
                        .navigate(R.id.action_requestCameraFragment_to_homeFragment)
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun setUpViewDetails(pageDetails: PageDetails) {
        camera_icon.setImageResource(pageDetails.image);
        request_camera_text.text = pageDetails.title
        btn_turn_on_camera.text = pageDetails.buttonText
    }

    private fun initializeButton() {
        btn_turn_on_camera.setOnClickListener {
            requestForPermissions()
        }
    }

    private fun requestPermissionsOrNavigateAway() {
        if (hasCameraPermissions()) {
            if (hasLocationPermissions()) {
                goToNextFragment()
            }
        }
    }

    private fun hasCameraPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return checkHasPermissions(REQUIRED_CAMERA_PERMISSIONS_ABOVE29)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return checkHasPermissions(REQUIRED_CAMERA_PERMISSIONS_BELOW29)
        }
        return false
    }

    private fun hasLocationPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return checkHasPermissions(REQUIRED_LOCATION_PERMISSIONS_ABOVE29)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return checkHasPermissions(REQUIRED_LOCATION_PERMISSIONS_BELOW29)
        }
        return false
    }

    private fun checkHasPermissions(permissions: Array<String>): Boolean {
        return EasyPermissions.hasPermissions(requireContext(), perms = permissions)
    }

    private fun requestForPermissions() {
        if (!hasLocationPermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissions(REQUIRED_LOCATION_PERMISSIONS_ABOVE29)
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                requestPermissions(REQUIRED_LOCATION_PERMISSIONS_BELOW29)
            }
        } else {
            if (!hasCameraPermissions()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestPermissions(REQUIRED_CAMERA_PERMISSIONS_ABOVE29)
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    requestPermissions(REQUIRED_CAMERA_PERMISSIONS_BELOW29)
                }
            }
        }
    }

    private fun requestPermissions(permissions: Array<String>) {
        EasyPermissions.requestPermissions(
            this,
            if (!hasCameraPermissions()) CAMERA_PERMISSION_RATIONALE else LOCATION_PERMISSION_RATIONALE,
            REQUEST_CODE_PERMISSIONS,
            perms = permissions
        )
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {

        if (!hasCameraPermissions()) {

            setUpViewDetails(
                PageDetails(
                    R.drawable.ic_camera,
                    "ALLOW TREEO APP TO USE YOUR CAMERA",
                    "TURN ON CAMERA"
                )
            )

        } else if (!hasLocationPermissions()) {
            setUpViewDetails(
                PageDetails(
                    R.drawable.ic_location,
                    "ALLOW SHARING YOUR LOCATION FOR USE IN TREEO APP",
                    "TURN ON LOCATION"
                )
            )
        } else {
            goToNextFragment()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(
                this,
                deniedPerms = perms.toTypedArray()
            )
        ) {
            SettingsDialog.Builder(requireContext()).build().show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun goToNextFragment() {
        if (navigateTo == "landSurveyCamera") {
            findNavController().navigate(
                R.id.landSurveyPrepFragment,
                bundleOf(
                    "summaryItem" to summaryItem,
                    "indexMap" to indexMap
                )
            )
        }
        if (navigateTo == "treeMeasurementCamera") {
            findNavController()
                .navigate(R.id.treeMeasurementCameraFragment,    bundleOf(
                    ADHOC_ACTIVITY_ID to adhocActivityId,
                ))
        }
        if (navigateTo == "forestInventory") {
            findNavController().
                        navigate(
                R.id.treeMeasurementCameraFragment,
                bundleOf(
                    MEASUREMENT to FOREST_INVENTORY,
                    INVENTORY_ID to inventoryId
                )
            )
        }
    }


    companion object {
        @JvmStatic
        fun newInstance() = RequestCameraFragment()

        private const val REQUEST_CODE_PERMISSIONS = 10

        private const val CAMERA_PERMISSION_RATIONALE =
            "Allow TREEO app to use your camera"

        private const val LOCATION_PERMISSION_RATIONALE =
            "Allow sharing your location for using the TREEO app."

        @RequiresApi(Build.VERSION_CODES.Q)
        private var REQUIRED_CAMERA_PERMISSIONS_ABOVE29 = arrayOf(
            Manifest.permission.CAMERA,
        )

        @RequiresApi(Build.VERSION_CODES.Q)
        private var REQUIRED_LOCATION_PERMISSIONS_ABOVE29 = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        private var REQUIRED_CAMERA_PERMISSIONS_BELOW29 = arrayOf(
            Manifest.permission.CAMERA
        )
        private var REQUIRED_LOCATION_PERMISSIONS_BELOW29 = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

}


data class PageDetails(val image: Int, val title: String, val buttonText: String)
