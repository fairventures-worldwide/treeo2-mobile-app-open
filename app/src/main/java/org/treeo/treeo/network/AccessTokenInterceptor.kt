package org.treeo.treeo.network

import android.content.Context
import android.content.SharedPreferences
import com.akexorcist.localizationactivity.core.LocalizationUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import org.treeo.treeo.R
import org.treeo.treeo.util.errors
import org.treeo.treeo.util.getAccessToken
import org.treeo.treeo.util.getErrorMessageFromJson
import org.treeo.treeo.util.getUserRefreshToken
import javax.inject.Inject


class AccessTokenInterceptor @Inject constructor(
    val context: Context,
    private val  apiService: ApiService,
    private val sharedPreferences: SharedPreferences
) : Interceptor, RequestManager(apiService) {

    var prefs: SharedPreferences? = null


    override fun intercept(chain: Interceptor.Chain): Response {
        leNewToken =  sharedPreferences. getAccessToken(context)
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("Authorization", "Bearer $leNewToken")
        val request = requestBuilder.build()
        val response =  chain.proceed(request)

        if (response.code == 401) {
            val refreshToken: String = sharedPreferences.getUserRefreshToken()

            if(refreshToken != ""){

                GlobalScope.launch (Dispatchers.Main) {
                    val leNewToken: String =  requestRefreshToken(refreshToken)
                    setUserToken(leNewToken)
                }

                val newRequest =  chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $leNewToken")
                    .build()
                return chain.proceed(newRequest)

            }
        }

        return response
    }


    private fun setUserToken(token: String) {
        with(sharedPreferences.edit()){
            putString(LocalizationUtility.getResources(context).getString(R.string.user_token), token)
            apply()
        }}


    private suspend fun requestRefreshToken(refreshToken: String): String {
        var newToken: String = ""
        try {
            val response = apiService.requestRefreshToken("Bearer $refreshToken")
            if (response.isSuccessful) {
                newToken = response.body()?.AccessToken ?: ""
            } else {
                val jsonResponse = response.errorBody()!!.charStream().readText()
                errors.postValue(getErrorMessageFromJson(jsonResponse))
            }
        } catch (e: Exception) {
            errors.postValue(e.message)
        }
        return newToken
    }


    companion object {
        private var leNewToken: String = ""
    }
}
