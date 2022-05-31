package org.treeo.treeo.ui.landsurvey.screens

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
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
import dagger.hilt.android.AndroidEntryPoint
import id.zelory.compressor.Compressor
import kotlinx.android.synthetic.main.display_soil_photo_layout.*
import kotlinx.android.synthetic.main.fragment_soil_photo.*
import kotlinx.android.synthetic.main.fragment_soil_photo.view.*
import kotlinx.android.synthetic.main.soil_photo_prep_layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.treeo.treeo.R
import org.treeo.treeo.models.*
import org.treeo.treeo.ui.landsurvey.LandSurveyViewModel
import org.treeo.treeo.util.SOIL_PHOTO
import org.treeo.treeo.util.hideView
import org.treeo.treeo.util.showView
import java.io.File
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class SoilPhotoFragment : Fragment() {
    private lateinit var indexMap: Map<String, Int>

    @Inject
    lateinit var sharedPref: SharedPreferences

    private val surveyViewModel: LandSurveyViewModel by activityViewModels()

    private var landSurvey: LandSurvey? = null
    private var photoList = mutableListOf<Photo>()

    private var bundleItem: ActivitySummaryItem? = null
    private lateinit var summaryItem: LandSurveySummaryItem

    private var imageCapture: ImageCapture? = null
    private val cameraExecutor by lazy {
        Executors.newSingleThreadExecutor()
    }

    private lateinit var outputDirectory: File
    private var imagePath = ""

    private var userLocation: UserLocation? = null
    private var gpsAccuracy: Float = 0.0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bundleItem = arguments?.getParcelable("summaryItem")
        summaryItem = bundleItem as LandSurveySummaryItem
        userLocation = arguments?.getParcelable("userLocation")
        gpsAccuracy = arguments?.getFloat("gpsAccuracy") ?: 0.0f
        landSurvey = arguments?.getParcelable("landSurvey")
        indexMap = arguments?.get("indexMap") as Map<String, Int>
        return inflater.inflate(R.layout.fragment_soil_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.soilPhotoToolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        view.soilPhotoToolbar.inflateMenu(R.menu.main_menu)
        view.soilPhotoToolbar.setNavigationOnClickListener {
            view.findNavController().popBackStack()
        }

        startCamera()

        outputDirectory = getOutputDirectory()

        initializeViews()
        setObservers()
    }

    override fun onResume() {
        super.onResume()
        surveyViewModel.getLandSurveyWithPhotosByActivity(summaryItem.activity?.id!!)
    }

    private fun initializeViews() {
        setTitle()
        showSoilPhotoPrepLayout()

        takeSoilPhotoButton.setOnClickListener {
            if (photoList.size == 1) {
                findNavController().navigate(
                    R.id.landSpecificationFragment, bundleOf(
                        "landSurvey" to landSurvey,
                        "summaryItem" to summaryItem,
                        "indexMap" to indexMap
                    )
                )
            } else {
                takePhoto()
            }
        }

        soilPhotoPrepContinueButton.setOnClickListener {
            hideSoilPhotoPrepLayout()
        }

        displaySoilPhotoFinishButton.setOnClickListener {
            savePhotoToDB()
            findNavController().navigate(
                R.id.landSpecificationFragment, bundleOf(
                    "landSurvey" to landSurvey,
                    "summaryItem" to summaryItem,
                    "indexMap" to indexMap
                )
            )
        }

        displaySoilPhotoRetakeButton.setOnClickListener {
            hideSoilPhotoDisplayLayout()
            removePhotoFromList()
        }
    }

    private fun setObservers() {
        surveyViewModel.landSurveyWithPhotos.observe(viewLifecycleOwner) {
            if (it != null && it.isNotEmpty()) {
                landSurvey = it["landSurvey"] as LandSurvey
                val imageList = it["photoList"] as MutableList<Photo>
                photoList.clear()
                imageList.forEach { photo ->
                    if (photo.imageType == SOIL_PHOTO) {
                        photoList.add(photo)
                    }
                }
            }
            setTitle()
        }
    }

    private fun savePhotoToDB() {
        GlobalScope.launch {
            val compressedImage = withContext(Dispatchers.IO) {
                Compressor.compress(requireContext(), File(photoList.last().imagePath))
            }
            photoList.last().imagePath = compressedImage.absolutePath
            surveyViewModel.insertLandSurveyPhoto(photoList.last())
        }
    }

    private fun setTitle() {
        val title = getString(R.string.soil_photos).plus("${photoList.size}/1")
        soilPhotoTitleTextView.text = title
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
                        it.setSurfaceProvider(soilPhotoCameraPreview.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder()
                    .build()

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
                    Log.e("Soil Photo", "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            UUID.randomUUID().toString() + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    imagePath = photoFile.absolutePath
                    showSoilPhotoDisplayLayout()
                    addPhotoToList(photoFile.absolutePath)
                }
            })
    }

    private fun addPhotoToList(imagePath: String) {
        val photo = Photo(
            landSurvey?.surveyId!!,
            null,
            UUID.randomUUID(),
            landSurvey?.surveyUUID!!,
            imagePath,
            SOIL_PHOTO,
            PhotoMetaData(
                Timestamp(System.currentTimeMillis()).toString(),
                "",
                userLocation,
                gpsAccuracy,
                "",
                "",
                "",
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

    private fun setImageInImageView() {
        Glide.with(requireContext()).load(imagePath).into(displaySoilPhotoImageView)
    }

    private fun getOutputDirectory(): File {
        val contextWrapper = ContextWrapper(requireActivity().applicationContext)
        val directory = contextWrapper.getDir("imageDir", Context.MODE_PRIVATE)

        return File(
            directory,
            SimpleDateFormat(
                FILENAME_FORMAT,
                Locale.US
            ).format(System.currentTimeMillis())
        ).apply { mkdir() }
    }

    private fun showSoilPhotoPrepLayout() {
        showView(soilPhotoPrepLayout)
    }

    private fun hideSoilPhotoPrepLayout() {
        hideView(soilPhotoPrepLayout)
    }

    private fun showSoilPhotoDisplayLayout() {
        showView(displaySoilPhotoLayout)
    }

    private fun hideSoilPhotoDisplayLayout() {
        hideView(displaySoilPhotoLayout)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        @JvmStatic
        fun newInstance() = SoilPhotoFragment()

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}
