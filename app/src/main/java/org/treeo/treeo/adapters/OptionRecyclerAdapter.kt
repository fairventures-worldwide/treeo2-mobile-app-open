package org.treeo.treeo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import org.treeo.treeo.R
import org.treeo.treeo.models.Option

private const val ITEM_VIEW_TYPE_CHECKBOX = 1
private const val ITEM_VIEW_TYPE_RADIO = 2

class OptionRecyclerAdapter(
    var selectedLanguage: String,
    private val checkListener: OptionCheckedListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var list = mutableListOf<Option>()
    private var questionType = ""
    private var lastRadioSelection: Option? = null
    private var lastCheckedRadio: RadioButton? = null

    class CheckBoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: MaterialCheckBox = itemView.findViewById(R.id.questionnaireCheckBox)

        companion object {
            fun from(parent: ViewGroup): CheckBoxViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.question_checkbox_item, parent, false)
                return CheckBoxViewHolder(view)
            }
        }
    }

    class RadioButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val radioButton: RadioButton = itemView.findViewById(R.id.questionnaireOptionTextView)

        companion object {
            fun from(parent: ViewGroup): RadioButtonViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.question_radio_item, parent, false)
                return RadioButtonViewHolder(view)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_CHECKBOX -> CheckBoxViewHolder.from(parent)
            ITEM_VIEW_TYPE_RADIO -> RadioButtonViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CheckBoxViewHolder -> {
                holder.setIsRecyclable(false)
                holder.apply {
                    checkBox.text = list[position].title[selectedLanguage].toString()
                    if (list[position].isSelected) {
                        checkBox.isChecked = true
                        checkListener.incrementSelectionSum(null)
                    }

                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (buttonView.isChecked) {
                            checkListener.onOptionCheck(list[position].optionId, true)
                            checkListener.incrementSelectionSum(null)
                        } else {
                            checkListener.onOptionCheck(list[position].optionId, false)
                            checkListener.decrementSelectionSum()
                        }
                    }
                }
            }
            is RadioButtonViewHolder -> {
                holder.setIsRecyclable(false)
                holder.apply {
                    radioButton.text = list[position].title[selectedLanguage].toString()
                    if (list[position].isSelected) {
                        radioButton.isChecked = true
                        checkListener.incrementSelectionSum("radio")
                        lastRadioSelection = list[position]
                        lastCheckedRadio = radioButton
                    }

                    radioButton.setOnClickListener {
                        checkListener.incrementSelectionSum("radio")

                        if (lastCheckedRadio != null && lastCheckedRadio != radioButton) {
                            lastCheckedRadio!!.isChecked = false
                        }
                        lastCheckedRadio = radioButton

                        checkListener.onOptionCheck(list[position].optionId, true)
                        if (lastRadioSelection != null) {
                            checkListener.onOptionCheck(lastRadioSelection!!.optionId, false)
                        }

                        lastRadioSelection = list[position]
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (questionType) {
            "checkbox" -> {
                ITEM_VIEW_TYPE_CHECKBOX
            }
            "radio" -> {
                ITEM_VIEW_TYPE_RADIO
            }
            else -> 0
        }
    }

    fun submitList(options: List<Option>, flag: String) {
        list.clear()
        list = options.toMutableList()
        questionType = flag
        notifyDataSetChanged()
    }
}

interface OptionCheckedListener {
    fun onOptionCheck(id: Long, isSelected: Boolean)
    fun incrementSelectionSum(flag: String?)
    fun decrementSelectionSum()
}

