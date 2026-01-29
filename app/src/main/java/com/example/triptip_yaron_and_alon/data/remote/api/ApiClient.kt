package com.example.triptip_yaron_and_alon.data.remote.api

import com.example.triptip_yaron_and_alon.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val weatherRetrofit = Retrofit.Builder()
        .baseUrl(Constants.OPEN_METEO_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val openTripMapRetrofit = Retrofit.Builder()
        .baseUrl(Constants.OPENTRIPMAP_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val weatherApiService: WeatherApiService = weatherRetrofit.create(WeatherApiService::class.java)
    val openTripMapApiService: OpenTripMapApiService = openTripMapRetrofit.create(OpenTripMapApiService::class.java)
}

