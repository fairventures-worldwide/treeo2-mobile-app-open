package org.treeo.treeo.ui.treemeasurement.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.tree_measurement_switcher.view.*
import org.treeo.treeo.R
import org.treeo.treeo.databinding.TreeMeasurementSwitcherBinding

class TreeMeasurementSwitcher : ConstraintLayout {

    private val view: View = inflate(context, R.layout.tree_measurement_switcher, this)
    private val binding by lazy {
        TreeMeasurementSwitcherBinding.bind(view)
    }

    private var onBigTreeSelected: (() -> Unit)? = null
    private var onSmallTreeSelected: (() -> Unit)? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defaultStyleAttr: Int
    ) : super(context, attrs, defaultStyleAttr) {
        init()
    }

    private fun init() {
        setSmallTreeSelected(false)
        binding.tmSwitcherCardView.cardViewLayout.apply {
            tvBigTree.setOnClickListener {
                setSmallTreeSelected(false)
                onBigTreeSelected?.invoke()
            }

            tvSmallTree.setOnClickListener {
                setSmallTreeSelected(true)
                onSmallTreeSelected?.invoke()
            }
        }
    }

    fun setSmallTreeSelected(selected: Boolean = true) {
        if (selected) {
            binding.tmSwitcherCardView.cardViewLayout.apply {
                tvSmallTree.setBackgroundResource(R.drawable.tm_switcher_selected_background)
                tvSmallTree.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.green
                    )
                )
                tvBigTree.setBackgroundResource(R.drawable.tm_switcher_unselected_background)
                tvBigTree.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.white
                    )
                )
            }
        } else {
            binding.tmSwitcherCardView.cardViewLayout.apply {
                tvBigTree.setBackgroundResource(R.drawable.tm_switcher_selected_background)
                tvBigTree.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.green
                    )
                )
                tvSmallTree.setBackgroundResource(R.drawable.tm_switcher_unselected_background)
                tvSmallTree.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.white
                    )
                )
            }
        }
    }

    fun setOnBigTreeSelected(onBigTreeSelected: () -> Unit) {
        this.onBigTreeSelected = onBigTreeSelected
    }

    fun setOnSmallTreeSelected(onSmallTreeSelected: () -> Unit) {
        this.onSmallTreeSelected = onSmallTreeSelected
    }
}
