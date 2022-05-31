package org.treeo.treeo.ui.treemeasurement.widget

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import org.treeo.treeo.R

class CameraOverlayGuide : View {

    var shouldShowCameraGuide = true
        set(value) {
            invalidate()
            field = value
        }

    private var cardAreaX: Float = 0.0F
    private var cardAreaY: Float = 0.0F
    private var cardAreaX2: Float = 0.0F
    private var cardAreaY2: Float = 0.0F

    private lateinit var label: String
    private var labelTextSize = 36f
    private var dottedRectWidthPercentage = 0.6f
    private var dottedRectHeightPercentage = 0.4f

    private lateinit var translucentPaint: Paint
    private lateinit var cornerLinesPaint: Paint
    private lateinit var verticalLinePaint: Paint
    private lateinit var dottedRectPaint: Paint
    private lateinit var textGuidePaint: Paint

    private val translucentRegionPath = Path()
    private val cornerLinePath1 = Path()
    private val cornerLinePath2 = Path()
    private val cornerLinePath3 = Path()
    private val cornerLinePath4 = Path()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defaultStyle: Int
    ) : super(context, attrs, defaultStyle) {
        init(attrs)
    }


    private fun init(attrs: AttributeSet) {
        initFromXMLAttributes(attrs)

        translucentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#99000000")
        }
        cornerLinesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 12f
            color = Color.WHITE
        }
        verticalLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            strokeWidth = 3f
            color = Color.WHITE
        }
        dottedRectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            strokeWidth = 3f
            color = Color.WHITE
        }
        textGuidePaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = labelTextSize
        }
    }

    private fun getOverlaySize(
        dottedRectStartX: Float,
        dottedRectStartY: Float,
        dottedRectWidth: Float,
        dottedRectHeight: Float
    ) {
        val sharedPreference =
            context.getSharedPreferences("CAMERA_OVERLAY_SIZE", Context.MODE_PRIVATE)
        if (!sharedPreference.contains("cardAreaX")) {
            val editor = sharedPreference.edit()
            cardAreaX = (dottedRectStartX / width)
            cardAreaY = (dottedRectStartY / height)
            cardAreaX2 = ((dottedRectStartX + dottedRectWidth) / width)
            cardAreaY2 = ((dottedRectStartY + dottedRectHeight) / height)
            editor.putFloat("cardAreaX", cardAreaX)
            editor.putFloat("cardAreaY", cardAreaY)
            editor.putFloat("cardAreaX2", cardAreaX2)
            editor.putFloat("cardAreaY2", cardAreaY2)
            editor.apply()
        }
    }

    private fun initFromXMLAttributes(attrs: AttributeSet) {

        context.obtainStyledAttributes(
            attrs,
            R.styleable.CameraOverlayGuide
        ).apply {
            label = getString(R.styleable.CameraOverlayGuide_label)
                .toString()
            dottedRectWidthPercentage = getFloat(
                R.styleable.CameraOverlayGuide_guidelineWidthPercentage,
                dottedRectWidthPercentage
            )
            dottedRectHeightPercentage = getFloat(
                R.styleable.CameraOverlayGuide_guidelineWidthPercentage,
                dottedRectHeightPercentage
            )
            labelTextSize = getFloat(
                R.styleable.CameraOverlayGuide_labelSize,
                labelTextSize
            )
            recycle()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.run {

            // Translucent region
            val innerRectWidth = width - InnerRect.PADDING
            val innerRectHeight = height * 0.7f
            translucentRegionPath.apply {
                moveTo(0f, 0f)
                lineTo(0f, height.toFloat())
                lineTo(width / 2f, height.toFloat())
                moveTo(width / 2f, innerRectHeight)
                lineTo(InnerRect.PADDING, innerRectHeight)
                lineTo(InnerRect.PADDING, height * InnerRect.TOP_MARGIN_PERCENTAGE)
                lineTo(innerRectWidth, height * InnerRect.TOP_MARGIN_PERCENTAGE)
                lineTo(innerRectWidth, innerRectHeight)
                lineTo(width / 2f, innerRectHeight)
                moveTo(width / 2f, height.toFloat())
                lineTo(width.toFloat(), height.toFloat())
                lineTo(width.toFloat(), 0f)
                lineTo(0f, 0f)
                drawPath(this, translucentPaint)
            }

            drawCornerLines(innerRectWidth, innerRectHeight)

            if (shouldShowCameraGuide) {
                val half = 0.5f
                drawLine(
                    width * half,
                    height * InnerRect.TOP_MARGIN_PERCENTAGE,
                    width * half,
                    innerRectHeight,
                    verticalLinePaint
                )

                val dottedRectWidth = dottedRectWidthPercentage * innerRectWidth
                val dottedRectStartX = (width - dottedRectWidth) / 2
                val dottedRectHeight = dottedRectHeightPercentage * innerRectHeight
                val dottedRectStartY =
                    (height * InnerRect.TOP_MARGIN_PERCENTAGE) + (innerRectHeight - dottedRectHeight) / 2

                getOverlaySize(
                    dottedRectStartX,
                    dottedRectStartY,
                    dottedRectWidth,
                    dottedRectHeight
                )

                drawRect(
                    dottedRectStartX,
                    dottedRectStartY,
                    dottedRectStartX + dottedRectWidth,
                    dottedRectStartY + dottedRectHeight,
                    dottedRectPaint
                )

                val textWidth = textGuidePaint.measureText(label)
                val textHeight =
                    textGuidePaint.fontMetrics.ascent // A negative value
                val textStartX = (width - textWidth) / 2f
                val textStartY = dottedRectStartY - InnerRect.PADDING
                val textPadding = 16f
                drawRect(
                    textStartX - textPadding,
                    textStartY + textHeight - (textPadding / 2),
                    textStartX + textWidth + textPadding,
                    textStartY + textPadding,
                    translucentPaint
                )
                drawText(
                    label,
                    textStartX,
                    textStartY,
                    textGuidePaint
                )
            }
        }
    }

    private fun Canvas.drawCornerLines(rectWidth: Float, rectHeight: Float) {
        cornerLinePath1.setCornerLinePoints(
            arrayListOf(
                InnerRect.PADDING,
                (height * InnerRect.TOP_MARGIN_PERCENTAGE) + InnerRect.PADDING,
                InnerRect.PADDING,
                height * InnerRect.TOP_MARGIN_PERCENTAGE,
                64f,
                height * InnerRect.TOP_MARGIN_PERCENTAGE
            )
        )
        cornerLinePath2.setCornerLinePoints(
            arrayListOf(
                rectWidth - InnerRect.PADDING,
                height * InnerRect.TOP_MARGIN_PERCENTAGE,
                rectWidth,
                height * InnerRect.TOP_MARGIN_PERCENTAGE,
                rectWidth,
                (height * InnerRect.TOP_MARGIN_PERCENTAGE) + InnerRect.PADDING
            )
        )
        cornerLinePath3.setCornerLinePoints(
            arrayListOf(
                rectWidth,
                rectHeight - InnerRect.PADDING,
                rectWidth,
                rectHeight,
                rectWidth - InnerRect.PADDING,
                rectHeight
            )
        )
        cornerLinePath4.setCornerLinePoints(
            arrayListOf(
                InnerRect.PADDING,
                rectHeight - InnerRect.PADDING,
                InnerRect.PADDING,
                rectHeight,
                64f,
                rectHeight
            )
        )

        drawPath(cornerLinePath1, cornerLinesPaint)
        drawPath(cornerLinePath2, cornerLinesPaint)
        drawPath(cornerLinePath3, cornerLinesPaint)
        drawPath(cornerLinePath4, cornerLinesPaint)
    }

    /**
     * Draws a path through three points defined by consecutive pairs
     * of float coordinates.
     *
     * @param points A list of six floats.
     */
    private fun Path.setCornerLinePoints(points: List<Float>) {
        moveTo(points[0], points[1])
        lineTo(points[2], points[3])
        lineTo(points[4], points[5])
    }

    private object InnerRect {
        const val PADDING = 32f
        const val TOP_MARGIN_PERCENTAGE = 0.1f
    }
}
