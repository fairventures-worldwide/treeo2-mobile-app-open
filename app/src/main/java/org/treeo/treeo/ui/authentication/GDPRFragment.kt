package org.treeo.treeo.ui.authentication

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.fragment_gdpr.*
import kotlinx.android.synthetic.main.gdpr_inflate_error.*
import org.treeo.treeo.R

class GDPRFragment : DialogFragment() {

    private var errorInflatingWebView = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            inflater.inflate(R.layout.fragment_gdpr, container, false)
        } catch (e: Exception) {
            errorInflatingWebView = true
            inflater.inflate(R.layout.gdpr_inflate_error, container, false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (errorInflatingWebView) {
            btnGDPRConditions.setOnClickListener {
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://fairventures.org/ueber-uns-2/datenschutz/")
                    startActivity(this)
                }
            }
        } else {
            try {
                gdpr_conditions_button.setOnClickListener {
                    dismiss()
                }

                val webView = view.findViewById<WebView>(R.id.webview)
                webView.loadUrl("https://fairventures.org/ueber-uns-2/datenschutz/")
            } catch (e: Exception) {
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
        }
    }

    companion object {
        @JvmStatic
        fun display(fragmentManager: FragmentManager?): GDPRFragment {
            val gdprFragment = GDPRFragment()
            val tag = "GRPR_dialog"
            if (fragmentManager != null) {
                gdprFragment.show(fragmentManager, tag)
            }
            return gdprFragment
        }
    }
}
