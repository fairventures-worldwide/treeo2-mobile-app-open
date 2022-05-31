package org.treeo.treeo.adapters

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import org.treeo.treeo.R
import org.treeo.treeo.models.Option
import org.treeo.treeo.models.Page
import org.treeo.treeo.util.PAGE_TYPE_LONG_TEXT
import org.treeo.treeo.util.PAGE_TYPE_SHORT_TEXT

private const val ITEM_VIEW_TYPE_CHECKBOX = 1
private const val ITEM_VIEW_TYPE_RADIO = 2
private const val ITEM_VIEW_TYPE_TEXT = 3
private const val ITEM_VIEW_TYPE_MULTILINE_TEXT = 4

class ResponseRecyclerAdapter(
    private var selectedLanguage: String,
    private val responseListener: ResponseListener,
    private val page: Page
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

    class SingleTextViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val textInput: EditText = itemView.findViewById(R.id.questionTextInput)

        companion object {
            fun from(parent: ViewGroup): SingleTextViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(
                    R.layout.question_text_item,
                    parent,
                    false
                )
                return SingleTextViewHolder(view)
            }
        }
    }

    class MultilineTextViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val multilineTextInput: EditText =
            itemView.findViewById(R.id.multilineQuestionTextInput)

        companion object {
            fun from(parent: ViewGroup): MultilineTextViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(
                    R.layout.question_multiline_text_item,
                    parent,
                    false
                )
                return MultilineTextViewHolder(view)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_CHECKBOX -> CheckBoxViewHolder.from(parent)
            ITEM_VIEW_TYPE_RADIO -> RadioButtonViewHolder.from(parent)
            ITEM_VIEW_TYPE_TEXT -> SingleTextViewHolder.from(parent)
            ITEM_VIEW_TYPE_MULTILINE_TEXT -> MultilineTextViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        when (holder) {
            is CheckBoxViewHolder -> {
                holder.apply {
                    checkBox.text = page.options!![position].title[selectedLanguage].toString()
                    if (page.options!![position].isSelected) {
                        checkBox.isChecked = true
                        responseListener.incrementSelectionSum(null)
                    }

                    checkBox.setOnCheckedChangeListener { buttonView, _ ->
                        if (buttonView.isChecked) {
                            responseListener.onOptionCheck(page.options!![position].optionId, true)
                            responseListener.incrementSelectionSum(null)
                        } else {
                            responseListener.onOptionCheck(page.options!![position].optionId, false)
                            responseListener.decrementSelectionSum()
                        }
                    }
                }
            }
            is RadioButtonViewHolder -> {
                holder.apply {
                    radioButton.text = page.options!![position].title[selectedLanguage].toString()
                    if (page.options!![position].isSelected) {
                        radioButton.isChecked = true
                        responseListener.incrementSelectionSum("radio")
                        lastRadioSelection = page.options!![position]
                        lastCheckedRadio = radioButton
                    }

                    radioButton.setOnClickListener {
                        responseListener.incrementSelectionSum("radio")

                        if (lastCheckedRadio != null && lastCheckedRadio != radioButton) {
                            lastCheckedRadio!!.isChecked = false
                        }
                        lastCheckedRadio = radioButton

                        responseListener.onOptionCheck(page.options!![position].optionId, true)
                        if (lastRadioSelection != null) {
                            responseListener.onOptionCheck(lastRadioSelection!!.optionId, false)
                        }

                        lastRadioSelection = page.options?.get(position)
                    }
                }
            }
            is SingleTextViewHolder -> {
                holder.apply {
                    if (!page.mandatory) {
                        responseListener.incrementSelectionSum(null)
                    }

                    textInput.hint = page.description[selectedLanguage].toString()
                    if (page.pageType == PAGE_TYPE_LONG_TEXT) {
                        textInput.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    }

                    textInput.addTextChangedListener {
                        responseListener.onTextInput(page.pageId, textInput.text.toString())
                    }
                }
            }
            is MultilineTextViewHolder -> {
                holder.apply {
                    if (!page.mandatory) {
                        responseListener.incrementSelectionSum(null)
                    }

                    multilineTextInput.hint = page.description[selectedLanguage].toString()
                    multilineTextInput.addTextChangedListener {
                        responseListener.onTextInput(
                            page.pageId,
                            multilineTextInput.text.toString()
                        )
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        // Set a size of at least one
        return if (page.options!!.isEmpty()) 1 else page.options!!.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (page.pageType) {
            "checkbox" -> {
                ITEM_VIEW_TYPE_CHECKBOX
            }
            "radio" -> {
                ITEM_VIEW_TYPE_RADIO
            }
            PAGE_TYPE_SHORT_TEXT -> {
                ITEM_VIEW_TYPE_TEXT
            }
            PAGE_TYPE_LONG_TEXT -> {
                ITEM_VIEW_TYPE_MULTILINE_TEXT
            }
            else -> 0
        }
    }
}

interface ResponseListener {
    fun onOptionCheck(id: Long, isSelected: Boolean)
    fun onTextInput(pageId: Long, response: String)
    fun incrementSelectionSum(flag: String?)
    fun decrementSelectionSum()
}
