package org.treeo.treeo.network

import androidx.annotation.Nullable

class APIServiceHolder {
    private var apiService: ApiService? = null
    @Nullable
    fun apiService(): ApiService? {
        return apiService
    }

    fun setAPIService(apiService: ApiService?) {
        this.apiService = apiService
    }
}
