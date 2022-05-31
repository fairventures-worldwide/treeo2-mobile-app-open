package org.treeo.treeo.util

import android.util.Log
import java.net.InetSocketAddress
import java.net.Socket

object NetworkMonitor {
    fun execute(): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress("https://www.google.com/", 53), 2000)
            socket.close()
            true
        } catch (e: NoNetworkException) {
            Log.e("NoNetwork: ", e.stackTraceToString())
            false
        }
    }
}
