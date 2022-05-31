package org.treeo.treeo.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConnectivityMonitor(private val context: Context) : LiveData<Boolean>() {

    var connectivityManager = context
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onActive() {
        super.onActive()
        val requestBuilder = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)

        CoroutineScope(Dispatchers.IO).launch {
            networkCallback = networkCallback()
            connectivityManager.registerNetworkCallback(
                requestBuilder.build(),
                networkCallback
            )
        }
    }

    override fun onInactive() {
        super.onInactive()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun networkCallback() =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                val hasNetwork = NetworkMonitor.execute()
                val result = if (hasNetwork) {
                    context.isNetworkAvailable()
                } else {
                    false
                }
                postValue(result)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                postValue(false)
            }
        }
}
