package org.treeo.treeo.ui.treemeasurement.screens

import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import kotlinx.android.synthetic.main.fragment_tree_measurement_camera.*
import org.treeo.treeo.R
import org.treeo.treeo.databinding.FragmentTMResultsBinding
import org.treeo.treeo.models.Point
import org.treeo.treeo.ui.treemeasurement.TMViewModel
import org.treeo.treeo.util.*
import kotlin.properties.Delegates

class TMResultsFragment : Fragment() {

    private val viewModel: TMViewModel by activityViewModels()
    private val binding by viewBinding(FragmentTMResultsBinding::bind)
    private lateinit var measurement: String
    private var isSkippedMeasurement by Delegates.notNull<Boolean>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        measurement = arguments?.getString(MEASUREMENT).toString()
        isSkippedMeasurement = arguments?.getBoolean(SKIP_MEASUREMENT) == true
        return inflater.inflate(R.layout.fragment_t_m_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUIForSmallTree()


        if (viewModel.recordingSmallTree) {
            viewModel.setMeasurementType(SMALL_TREE_ACCEPTED)
        } else {
            viewModel.setMeasurementType(TREE_MEASUREMENT_SUCCESSFUL)
        }


        if (isSkippedMeasurement){
            Glide.with(requireContext())
                .load(viewModel.getImagePath())
                .into(binding.displayPhotoImageView)
            binding.displayPhotoNextButton.visibility = View.GONE
            binding.diameterTextView.visibility = View.GONE
            binding.skipMeasurementButton.visibility = View.VISIBLE
        }

        if(viewModel.numOfAttempts > viewModel.currentRetryTimes!!){
            binding.skipMeasurementButton.visibility = View.VISIBLE
        }

        binding.displayPhotoNextButton.setOnClickListener {
            findNavController().navigate(
                R.id.TMSelectSpeciesFragment, bundleOf(MEASUREMENT to measurement, INVENTORY_ID to arguments?.getLong(INVENTORY_ID)!!, SKIP_MEASUREMENT to isSkippedMeasurement),
            )
        }

        binding.skipMeasurementButton.setOnClickListener {
            viewModel.run {
                viewModel.setMeasurementType(MEASUREMENT_SKIPPED)
                resetDiameter()
            }
            findNavController().navigate(
                R.id.TMSelectSpeciesFragment, bundleOf(MEASUREMENT to measurement, INVENTORY_ID to arguments?.getLong(INVENTORY_ID)!!, SKIP_MEASUREMENT to isSkippedMeasurement),
            )
        }

        binding.displayPhotoRetakeButton.setOnClickListener {
            viewModel.run {
                if (recordingSmallTree) {
                    setMeasurementType(SMALL_TREE_REJECTED)
                } else {
                    setMeasurementType(TREE_MEASUREMENT_REJECTED)
                }
                saveTreeMeasurement()
                resetDiameter()
            }
            findNavController().popBackStack()
        }

        setUpToolbar()
    }

    private fun updateUIForSmallTree() {
        if (viewModel.recordingSmallTree) {
            hideView(binding.diameterTextView)
            binding.tvPhotoQuality.text = getString(R.string.qtn_use_this_photo)
            binding.tvInstructions.text = getString(R.string.instruction_retake_photo)
            binding.displayPhotoNextButton.text = getString(R.string.text_continue_with_photo)
            binding.displayPhotoImageView.setImageURI(Uri.parse(viewModel.getImagePath()))
        }
    }

    private fun setUpToolbar() {
        if (measurement == FOREST_INVENTORY) {
            binding.photoTitleTextView.visibility = View.GONE
            toolbar.title = getString(R.string.tree_measurement)
        }

        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
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
    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.recordingSmallTree) {
            displayTreeAndCardPolygons()
        }
    }

    private fun displayTreeAndCardPolygons() {
        try {
            val original: Bitmap = BitmapFactory.decodeFile(viewModel.getImagePath())
            val result: Bitmap =
                Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
            val mCanvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            mCanvas.drawBitmap(original, 0f, 0f, null)
            paint.xfermode = null

            val paint2 = Paint(Paint.ANTI_ALIAS_FLAG)
            paint2.color = Color.RED

            val cardPointsList =
                listOf(*viewModel.cardPolygon.value.toString().split("_").toTypedArray())
            val treePointsList =
                listOf(*viewModel.treeLines.value.toString().split("_").toTypedArray())

            val firstPoint: Point?
            val secondPoint: Point?
            val thirdPoint: Point?
            val fourthPoint: Point?

            val first: List<Float> = getXY(cardPointsList[1])
            firstPoint = Point(first[0], first[1])

            val second: List<Float> = getXY(cardPointsList[2])
            secondPoint = Point(second[0], second[1])

            val third: List<Float> = getXY(cardPointsList[3])
            thirdPoint = Point(third[0], third[1])

            val fourth: List<Float> = getXY(cardPointsList[4])
            fourthPoint = Point(fourth[0], fourth[1])

            mCanvas.drawLine(firstPoint.x, firstPoint.y, secondPoint.x, secondPoint.y, paint2)
            mCanvas.drawLine(secondPoint.x, secondPoint.y, thirdPoint.x, thirdPoint.y, paint2)
            mCanvas.drawLine(thirdPoint.x, thirdPoint.y, fourthPoint.x, fourthPoint.y, paint2)
            mCanvas.drawLine(fourthPoint.x, fourthPoint.y, firstPoint.x, firstPoint.y, paint2)

            val firstPoint2: Point?
            val secondPoint2: Point?
            val thirdPoint2: Point?
            val fourthPoint2: Point?

            val first2: List<Float> = getXY(treePointsList[1])
            firstPoint2 = Point(first2[0], first2[1])

            val second2: List<Float> = getXY(treePointsList[2])
            secondPoint2 = Point(second2[0], second2[1])

            val third2: List<Float> = getXY(treePointsList[3])
            thirdPoint2 = Point(third2[0], third2[1])

            val fourth2: List<Float> = getXY(treePointsList[4])
            fourthPoint2 = Point(fourth2[0], fourth2[1])

            mCanvas.drawLine(firstPoint2.x, firstPoint2.y, secondPoint2.x, secondPoint2.y, paint2)
            mCanvas.drawLine(secondPoint2.x, secondPoint2.y, fourthPoint2.x, fourthPoint2.y, paint2)
            mCanvas.drawLine(thirdPoint2.x, thirdPoint2.y, fourthPoint2.x, fourthPoint2.y, paint2)
            mCanvas.drawLine(thirdPoint2.x, thirdPoint2.y, firstPoint2.x, firstPoint2.y, paint2)

            binding.displayPhotoImageView.setImageBitmap(result)
        } catch (e: Exception) {
            print(e.message)
        }

        val diameter = String.format("%.2f", viewModel.treeDiameter.value)
        val diameterText = "$diameter mm"
        binding.diameterTextView.text = diameterText
    }

    private fun getXY(strPoint: String): List<Float> {
        val items = listOf(*strPoint.split(",").toTypedArray())
        return listOf(
            (items[0].toDouble() / 4.0).toFloat(),
            (items[1].toFloat() / 4.0).toFloat()
        )
    }
}
