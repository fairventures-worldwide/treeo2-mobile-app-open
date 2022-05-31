package org.treeo.treeo.ui.treemeasurement.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.*
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.annotations.AfterPermissionGranted
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import id.zelory.compressor.Compressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.treeo.treeo.BuildConfig
import org.treeo.treeo.R
import org.treeo.treeo.databinding.FragmentTreeMeasurementCameraBinding
import org.treeo.treeo.models.UserLocation
import org.treeo.treeo.ui.treemeasurement.TMViewModel
import org.treeo.treeo.ui.treemeasurement.screens.dialogs.ProcessingImageDialog
import org.treeo.treeo.util.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class TreeMeasurementCameraFragment : Fragment(),
    EasyPermissions.PermissionCallbacks, ProcessingImageDialog.OnRetakeListener {

    @Inject
    lateinit var sharedPref: SharedPreferences

    private val viewModel: TMViewModel by activityViewModels()
    private val binding by viewBinding(FragmentTreeMeasurementCameraBinding::bind)
    private val processingDialog by lazy {
        ProcessingImageDialog.newInstance(
            measurement,
            inventoryId
        )
    }

    private val locationManager by lazy {
        requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private var cameraFlashOn = false
    private val imageCapture by lazy {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    private var imagePath: String = ""
    private lateinit var savedUri: Uri

    private val cameraProviderFuture by lazy {
        ProcessCameraProvider.getInstance(requireContext())
    }

    private lateinit var cameraProvider: ProcessCameraProvider

    private var userLocation: UserLocation? = null
    private var gpsAccuracy = 0f

    private var locationProvider: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private lateinit var measurement: String
    private var inventoryId: Long? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result.locations.forEach {
                gpsAccuracy = it.accuracy
                userLocation = UserLocation(
                    it.latitude.toString(),
                    it.longitude.toString()
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireContext().isNetworkAvailable()) {
            initializeLocationProvider()
        } else {
            getLocationWhileOffLine()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        measurement = arguments?.getString(MEASUREMENT).toString()
        if (measurement == FOREST_INVENTORY) {
            inventoryId = arguments?.getLong(INVENTORY_ID)!!
        }

        return inflater.inflate(
            R.layout.fragment_tree_measurement_camera,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Intercept back press events
        requireActivity().onBackPressedDispatcher.addCallback {
            handleBackNav()
        }

        binding.toolbar.setUpToolbar()
        initializeLocationProvider()
        checkForPermissions()
    }

    private fun handleBackNav() {
        if (measurement == FOREST_INVENTORY) {
            findNavController().popBackStack(R.id.firstGuideFragment, false)
        } else {
            findNavController().popBackStack(R.id.treeMeasureIntroFragment, false)
        }
    }

    private fun Toolbar.setUpToolbar() {
        if (measurement == FOREST_INVENTORY) {
            binding.photoTitleTextView.visibility = View.GONE
            binding.toolbar.title = getString(R.string.tree_measurement)
        }
        setNavigationIcon(R.drawable.ic_back_arrow)
        setNavigationOnClickListener {
            handleBackNav()
        }

        inflateMenu(R.menu.tree_measurement_menu)
        setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.flashOn -> {
                    cameraFlashOn = false
                    menu.toggleFlashIcon(R.id.flashOff, it)
                    requireContext().showToast(getString(R.string.message_flash_turned_off))
                    true
                }
                R.id.flashOff -> {
                    cameraFlashOn = true
                    menu.toggleFlashIcon(R.id.flashOn, it)
                    requireContext().showToast(getString(R.string.message_flash_turned_on))
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun Menu.toggleFlashIcon(newIconId: Int, currentItem: MenuItem) {
        findItem(newIconId).isVisible = true
        currentItem.isVisible = false
    }

    private fun initializeLocationProvider() {
        locationProvider =
            LocationServices.getFusedLocationProviderClient(requireContext())
        locationRequest = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(10)
            fastestInterval = TimeUnit.SECONDS.toMillis(5)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationWhileOffLine() {
        val hasNetwork =
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (hasNetwork) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000,
                0.0f
            ) { location ->
                gpsAccuracy = location.accuracy
                userLocation = UserLocation(
                    location.latitude.toString(),
                    location.longitude.toString()
                )
            }
        }
    }

    @AfterPermissionGranted(REQUEST_CODE_PERMISSIONS)
    private fun checkForPermissions() {
        if (hasPermissions()) {
            startLocationUpdates()
            startCamera()
        } else {
            requestPermissions()
        }
    }

    private fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return checkHasPermissions(REQUIRED_PERMISSIONS_ABOVE29)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return checkHasPermissions(REQUIRED_PERMISSIONS_BELOW29)
        }
        return false
    }

    private fun checkHasPermissions(permissions: Array<String>): Boolean {
        return EasyPermissions.hasPermissions(
            requireContext(),
            perms = permissions
        )
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationProvider?.requestLocationUpdates(
            locationRequest!!,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(REQUIRED_PERMISSIONS_ABOVE29)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requestPermissions(REQUIRED_PERMISSIONS_BELOW29)
        }
    }

    private fun requestPermissions(permissions: Array<String>) {
        EasyPermissions.requestPermissions(
            this,
            getString(R.string.message_permissions_required_to_measure_tree),
            REQUEST_CODE_PERMISSIONS,
            perms = permissions
        )
    }

    private fun startCamera() {
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()

                // Preview use case
                val preview = Preview.Builder()
                    .build()
                    .apply {
                        if (view != null) {
                            setSurfaceProvider(binding.treeMeasurementCameraPreview.surfaceProvider)
                        }
                    }

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (exc: Exception) {
                    // Do nothing
                }

            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    override fun onResume() {
        super.onResume()
        setUpViews()
        setObservers()
    }

    override fun restartCamera() {
        startCamera()
    }

    private fun setUpViews() {
        binding.takePictureButton.setOnClickListener {
            if (userLocation?.lat == null || userLocation?.lng == null) {
                binding.takePictureButton.isEnabled = false
                requireActivity().showToast(getString(R.string.message_getting_location))
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@setOnClickListener
                }
                LocationServices.getFusedLocationProviderClient(requireActivity())
                    .lastLocation
                    .addOnCompleteListener {
                        binding.takePictureButton.isEnabled = true
                        val location = it.result
                        if (it.isSuccessful && location != null) {
                            gpsAccuracy = location.accuracy
                            userLocation = UserLocation(
                                location.latitude.toString(),
                                location.longitude.toString()
                            )
                            takePhoto()
                        }
                    }

            } else {
                takePhoto()
            }
        }

        binding.tipsBtn.setOnClickListener {
            findNavController().navigate(R.id.bottomSheetTipsDialog)
        }

        setUpUIBasedOnTreeType()
    }

    private fun stopCamera() {
        try {
            cameraProvider.unbindAll()
        } catch (exc: Exception) {
            // Do nothing
        }
    }

    private fun setUpUIBasedOnTreeType() {
        when (viewModel.treeType) {
            BIG_TREES -> {
                viewModel.recordingSmallTree = false
                hideView(binding.tmSwitcher)
            }
            SMALL_TREES -> {
                viewModel.recordingSmallTree = true
                hideView(binding.tmSwitcher)
            }
            BIG_AND_SMALL_TREES -> {
                showView(binding.tmSwitcher)
                binding.tmSwitcher.setOnBigTreeSelected {
                    viewModel.recordingSmallTree = false
                    updateUIForSmallOrBigTree()
                }

                binding.tmSwitcher.setOnSmallTreeSelected {
                    viewModel.recordingSmallTree = true
                    updateUIForSmallOrBigTree()
                }
            }
        }
        binding.tmSwitcher.setSmallTreeSelected(viewModel.recordingSmallTree)
        updateUIForSmallOrBigTree()
    }

    private fun updateUIForSmallOrBigTree() {
        if (viewModel.recordingSmallTree) {
            binding.cameraOverlay.shouldShowCameraGuide = false
            hideView(binding.tipsBtn)
        } else {
            binding.cameraOverlay.shouldShowCameraGuide = true
            showView(binding.tipsBtn)
        }
    }

    private fun compressPhoto(file: File) {
        GlobalScope.launch {
            val compressedImage = withContext(Dispatchers.IO) {
                Compressor.compress(requireContext(), file)
            }
            imagePath = compressedImage.absolutePath
        }
    }

    private fun setObservers() {
        viewModel.measurementDone = false

        viewModel.treeDiameter.observe(viewLifecycleOwner) {
            if (viewModel.measurementDone) {
                if ("${viewModel.cardPolygon.value}" == " " || viewModel.treeLines.value == " ") {
                    if (viewModel.roiStages == "stage1") {
                        viewModel.roiStages = "stage2"
                        viewModel.measureTreeDiameter(savedUri)
                    } else {
                        viewModel.createAdHocTreeMeasurement(
                            imagePath,
                            userLocation,
                            gpsAccuracy,
                            it,
                            cameraFlashOn,
                            viewModel.treeLines.value,
                            viewModel.cardPolygon.value,
                            viewModel.roiStages,
                            inventoryId,
                        )
                        viewModel.roiStages = "stage1"
                        processingDialog.showMeasurementError(
                            {},
                            retakesCount = viewModel.numOfAttempts,
                            onSkipNavigation = {
                                processingDialog.navigateToNextScreen(isSkippedMeasurement = true)
                            })
                    }
                } else {
                    viewModel.createAdHocTreeMeasurement(
                        imagePath,
                        userLocation,
                        gpsAccuracy,
                        it,
                        cameraFlashOn,
                        viewModel.treeLines.value,
                        viewModel.cardPolygon.value,
                        viewModel.roiStages,
                        inventoryId
                    )
                    viewModel.roiStages = "stage1"

                    if (it > 0.0) {
                        processingDialog.navigateToNextScreen()
                    } else {
                        processingDialog.showMeasurementError(
                            {},
                            retakesCount = viewModel.numOfAttempts,
                            onSkipNavigation = {
                                processingDialog.navigateToNextScreen(isSkippedMeasurement = true)
                            })
                    }
                }
            }
        }

        viewModel.smallTreeIsClear.observe(viewLifecycleOwner) {
            if (viewModel.measurementDone) {
                if (it) {
                    viewModel.createAdHocTreeMeasurement(
                        imagePath,
                        userLocation,
                        gpsAccuracy,
                        0.0,
                        cameraFlashOn,
                        null,
                        null,
                        null,
                        inventoryId,
                    )
                    processingDialog.navigateToNextScreen()
                } else {
                    processingDialog.showMeasurementError(
                        {},
                        retakesCount = viewModel.numOfAttempts,
                        onSkipNavigation = {
                            processingDialog.navigateToNextScreen(isSkippedMeasurement = true)
                        })
                }
            }
        }
    }

    private fun takePhoto() {
        try {
            // Set flash mode
            imageCapture.flashMode =
                if (cameraFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF

            val photoFile = File(
                getCameraOutputDirectory(),
                UUID.randomUUID().toString() + ".jpg"
            )

            // Output file options
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            // Capture image
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        stopCamera()
                        processingDialog.show(
                            requireActivity().supportFragmentManager,
                            "Image Processing"
                        )
                        processingDialog.setOnRetakeListener(this@TreeMeasurementCameraFragment)
                        imagePath = photoFile.absolutePath
                        compressPhoto(photoFile)
                        savedUri = Uri.fromFile(photoFile)

                        viewModel.run {
                            if (recordingSmallTree) {
                                checkIfSmallTreeIsClear(savedUri)
                            } else {
                                measureTreeDiameter(savedUri)
                            }
                            incrementAttempts()
                            measurementDone = true
                        }
                    }
                })
        } catch (exc: Exception) {
            // Do nothing
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        AlertDialog.Builder(requireContext()).run {
            setTitle(R.string.label_device_access)
            setMessage(R.string.message_permissions_required_to_measure_tree)
            setPositiveButton(R.string.label_go_to_settings) { _, _ ->
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                    startActivityForResult(this, REQUEST_CODE_SETTINGS)
                }
            }
            setNegativeButton(R.string.label_exit) { _, _ ->
                findNavController().popBackStack()
            }
            setCancelable(false)
            show()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            this
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SETTINGS) {
            checkForPermissions()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TreeMeasurementCameraFragment()

        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val REQUEST_CODE_SETTINGS = 655

        @RequiresApi(Build.VERSION_CODES.Q)
        private var REQUIRED_PERMISSIONS_ABOVE29 = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION
        )

        private var REQUIRED_PERMISSIONS_BELOW29 = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}
