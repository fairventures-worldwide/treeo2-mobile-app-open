package org.treeo.treeo.ui.landsurvey.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import id.zelory.compressor.Compressor
import kotlinx.android.synthetic.main.display_photo_layout.*
import kotlinx.android.synthetic.main.fragment_request_camera_use.view.*
import kotlinx.android.synthetic.main.fragment_take_land_photos.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.treeo.treeo.R
import org.treeo.treeo.models.*
import org.treeo.treeo.ui.landsurvey.LandSurveyViewModel
import org.treeo.treeo.util.*
import java.io.File
import java.sql.Timestamp
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class TakeLandPhotos : Fragment(), SensorEventListener {
    private lateinit var indexMap: Map<String, Int>
    private val surveyViewModel: LandSurveyViewModel by activityViewModels()

    private var landSurvey: LandSurvey? = null
    private var photoList = mutableListOf<Photo>()
    private var landSurveyId: Long? = 0

    private var bundleItem: ActivitySummaryItem? = null
    private lateinit var summaryItem: LandSurveySummaryItem

    @Inject
    lateinit var sharedPref: SharedPreferences

    private val locationManager by lazy {
        requireContext().getSystemService(LOCATION_SERVICE) as LocationManager
    }

    private var imageCapture: ImageCapture? = null
    private val cameraExecutor by lazy {
        Executors.newSingleThreadExecutor()
    }

    private var sensorManager: SensorManager? = null

    private var currentSteps: Float = 0f

    private var numberOfCorners = 0
    private var imagePath = ""

    private var locationProvider: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var userLocation: UserLocation? = null
    private var gpsAccuracy: Float = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requireContext().isNetworkAvailable()) {
            initializeLocationProvider()
        } else {
            getLocationWhileOffLine()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bundleItem = arguments?.getParcelable("summaryItem")
        summaryItem = bundleItem as LandSurveySummaryItem
        numberOfCorners = arguments?.getInt("numberOfCorners") ?: 0
        indexMap = arguments?.get("indexMap") as Map<String, Int>
        return inflater.inflate(
            R.layout.fragment_take_land_photos,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        view.toolbar.setNavigationOnClickListener {
            if (photoList.size == numberOfCorners) {
                view.findNavController()
                    .popBackStack(R.id.activitySummaryFragment, false)
            } else {
                view.findNavController()
                    .popBackStack(R.id.landCornersFragment, false)
            }
        }

        startCamera()

        sensorManager =
            requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager

        setObservers()
        initializeViews()
    }

    private fun initializeViews() {
        if (numberOfCorners > 6) {
            displayPhotoContinueButton.text =
                getString(R.string.continue_to_soil_photo)
            showView(displayPhotoContinueButton)
        }
        takePictureButton.setOnClickListener {
            if (photoList.size == numberOfCorners) {
                if (summaryItem.activity?.template?.configuration?.soilPhotoRequired!!) {
                    findNavController().navigate(
                        R.id.soilPhotoFragment,
                        bundleOf(
                            "userLocation" to userLocation,
                            "gpsAccuracy" to gpsAccuracy,
                            "landSurveyId" to landSurveyId,
                            "summaryItem" to summaryItem,
                            "indexMap" to indexMap
                        )
                    )
                } else {
                    findNavController().navigate(
                        R.id.landSpecificationFragment,
                        bundleOf(
                            "landSurveyId" to landSurveyId,
                            "summaryItem" to summaryItem,
                            "indexMap" to indexMap
                        )
                    )
                }
            } else {
                takeLocationAndPhoto()
            }
        }

        displayPhotoNextButton.setOnClickListener {
            if (photoList.size == numberOfCorners) {
                if (summaryItem.activity?.template?.configuration?.soilPhotoRequired!!) {
                    hideView(displayPhotoNextButton)
                    displayPhotoContinueButton.text =
                        getString(R.string.continue_to_soil_photo)
                    showView(displayPhotoContinueButton)
                } else {
                    hideView(displayPhotoNextButton)
                    showView(displayPhotoContinueButton)
                }
            } else {
                hidePhotoDisplayLayout()
                savePhotoToDB()
            }
        }

        displayPhotoContinueButton.setOnClickListener {
            savePhotoToDB()
            if (summaryItem.activity?.template?.configuration?.soilPhotoRequired!!) {
                findNavController().navigate(
                    R.id.soilPhotoFragment,
                    bundleOf(
                        "userLocation" to userLocation,
                        "gpsAccuracy" to gpsAccuracy,
                        "landSurvey" to landSurvey,
                        "summaryItem" to summaryItem,
                        "indexMap" to indexMap
                    )
                )
            } else {
                findNavController().navigate(
                    R.id.landSpecificationFragment,
                    bundleOf(
                        "landSurveyId" to landSurveyId,
                        "summaryItem" to summaryItem,
                        "indexMap" to indexMap
                    )
                )
            }
        }

        displayPhotoRetakeButton.setOnClickListener {
            hidePhotoDisplayLayout()
            removePhotoFromList()
        }
    }

    private fun setTitle() {
        val title = if (numberOfCorners > 6) {
            getString(R.string.land_photos).plus(" ").plus(photoList.size)
        } else {
            getString(R.string.land_photos).plus(" ").plus(photoList.size)
                .plus("/${numberOfCorners}")
        }
        photoTitleTextView.text = title
    }

    private fun setObservers() {
        surveyViewModel.landSurveyWithPhotos.observe(
            viewLifecycleOwner,
            {
                if (it != null && it.isNotEmpty()) {
                    landSurvey = it["landSurvey"] as LandSurvey
                    numberOfCorners = landSurvey?.corners ?: 0
                    val imageList = it["photoList"] as MutableList<Photo>
                    photoList.clear()
                    imageList.forEach { photo ->
                        if (photo.imageType == LAND_PHOTO) {
                            photoList.add(photo)
                        }
                    }
                }
                setTitle()
            })
    }


    private fun savePhotoToDB() {
        if (photoList.isNotEmpty()) {
            GlobalScope.launch {
                try {
                    val file = File(photoList.last().imagePath)
                    val compressedImage = withContext(Dispatchers.IO) {
                        Compressor.compress(
                            requireContext(),
                            file
                        )
                    }
                    photoList.last().imagePath = compressedImage.absolutePath
                    surveyViewModel.insertLandSurveyPhoto(photoList.last())
                } catch (e: Exception) {
                    requireContext().showToast("File not found, skipping...")
                }
            }
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

    private fun initializeLocationProvider() {
        locationProvider =
            LocationServices.getFusedLocationProviderClient(requireContext())
        locationRequest = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(10)
            fastestInterval = TimeUnit.SECONDS.toMillis(5)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

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

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationProvider?.requestLocationUpdates(
            locationRequest!!,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        locationProvider?.removeLocationUpdates(locationCallback)
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (locationRequest != null) {
                checkSettingsAndStartLocationUpdates()
            }
        }
    }

    private fun checkSettingsAndStartLocationUpdates() {
        val settingsRequest = LocationSettingsRequest
            .Builder()
            .addLocationRequest(locationRequest!!)
            .build()

        val settingsClient =
            LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(settingsRequest)
        locationSettingsResponseTask.addOnSuccessListener {
            startLocationUpdates()
        }

        locationSettingsResponseTask.addOnFailureListener {
            if (it is ResolvableApiException) {
                try {
                    // Request the user to update location settings.
                    it.startResolutionForResult(
                        requireActivity(),
                        REQUEST_CHANGE_LOCATION_SETTINGS
                    )
                } catch (e: Exception) {
                    // Ignore the exception.
                }
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == REQUEST_CHANGE_LOCATION_SETTINGS) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                startLocationUpdates()
            } else {
                showLocationRequiredDialog(R.string.message_cannot_access_location)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showLocationRequiredDialog(messageResourceId: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(messageResourceId)
            .setPositiveButton(R.string.label_exit) { _, _ ->
                findNavController().popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()

        val stepsSensor =
            sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepsSensor != null) {
            sensorManager?.registerListener(
                this,
                stepsSensor,
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH
            )
        }

        surveyViewModel.getLandSurveyWithPhotosByActivity(summaryItem.activity?.id!!)
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(
            {
                // Used to bind the lifecycle of cameras to the lifecycle owner
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(landPhotoCameraPreview.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder().build()

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture
                    )

                } catch (exc: Exception) {
                }

            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    @SuppressLint("MissingPermission")
    private fun takeLocationAndPhoto() {
        if (userLocation?.lat == null || userLocation?.lng == null) {
            takePictureButton.isEnabled = false
            requireActivity().showToast(getString(R.string.message_getting_location))
            LocationServices.getFusedLocationProviderClient(requireActivity())
                .lastLocation
                .addOnCompleteListener {
                    takePictureButton.isEnabled = true
                    val location = it.result
                    if (it.isSuccessful && location != null) {
                        gpsAccuracy = location.accuracy
                        takePhoto(
                            UserLocation(
                                location.latitude.toString(),
                                location.longitude.toString()
                            )
                        )
                    } else {
                        showLocationRequiredDialog(R.string.message_check_settings)
                    }
                }

        } else {
            takePhoto(userLocation!!)
        }
    }

    private fun takePhoto(location: UserLocation) {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            getCameraOutputDirectory(),
            UUID.randomUUID().toString() + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    imagePath = photoFile.absolutePath
                    showPhotoDisplayLayout()
                    addPhotoToList(photoFile.absolutePath, location)
                }
            })
    }

    private fun addPhotoToList(imagePath: String, location: UserLocation) {
        val photo = Photo(
            landSurvey?.surveyId!!,
            null,
            UUID.randomUUID(),
            landSurvey?.surveyUUID!!,
            imagePath,
            LAND_PHOTO,
            PhotoMetaData(
                Timestamp(System.currentTimeMillis()).toString(),
                "0",
                location,
                gpsAccuracy,
                "0",
                "0",
                "0",
                false
            )
        )

        photoList.add(photo)
        setTitle()
        setImageInImageView()
    }

    private fun removePhotoFromList() {
        photoList.removeLast()
        setTitle()
    }

    private fun showPhotoDisplayLayout() {
        showView(displayPhotoLayout)
    }

    private fun hidePhotoDisplayLayout() {
        hideView(displayPhotoLayout)
    }

    private fun setImageInImageView() {
        Glide.with(requireContext()).load(imagePath).into(displayPhotoImageView)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        currentSteps = event?.values?.get(0)!!
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    companion object {
        @JvmStatic
        fun newInstance() = TakeLandPhotos()

        private const val REQUEST_CHANGE_LOCATION_SETTINGS = 44
    }
}
