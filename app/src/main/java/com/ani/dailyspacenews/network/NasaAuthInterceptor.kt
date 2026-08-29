package com.ani.dailyspacenews.network

import com.ani.dailyspacenews.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class NasaAuthInterceptor : Interceptor {

    private val apiKeys = listOf(
        BuildConfig.NASA_API_KEY_1,
        BuildConfig.NASA_API_KEY_2,
        BuildConfig.NASA_API_KEY_3
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Only apply API key rotation to NASA requests
        if (!originalRequest.url.host.contains("nasa.gov")) {
            return chain.proceed(originalRequest)
        }

        val newUrl = originalRequest.url.newBuilder()
            .addQueryParameter("api_key", apiKeys.random())
            .build()
        
        val newRequest = originalRequest.newBuilder().url(newUrl).build()
        return chain.proceed(newRequest)
    }
}