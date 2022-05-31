package org.treeo.treeo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import org.treeo.treeo.R
import org.treeo.treeo.models.Country

class CountrySpinnerAdapter(
    context: Context,
    resource: Int,
    countries: MutableList<Country>
) :
    ArrayAdapter<Country>(context, 0, countries) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    private fun initView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.country_spinner_item,
            parent,
            false
        )
        val country: Country? = getItem(position)

        val imageView = view.findViewById<ImageView>(R.id.flagImageView)
        Glide.with(context)
            .load(country?.flag)
            .into(imageView)

        val countryTV = view.findViewById<TextView>(R.id.countryTextView)
        countryTV.text = country?.country

        return view
    }
}
