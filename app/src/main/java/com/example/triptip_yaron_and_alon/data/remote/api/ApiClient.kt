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

    // Shorter timeouts for location APIs so offline users get feedback quickly.
    private val locationOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
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
    
    // Geocoding API client with custom User-Agent (required by Nominatim)
    private val geocodingOkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "TripTip/1.0") // Required by Nominatim
                .build()
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val geocodingRetrofit = Retrofit.Builder()
        .baseUrl(Constants.NOMINATIM_BASE_URL)
        .client(geocodingOkHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // Google Places API client
    private val googlePlacesRetrofit = Retrofit.Builder()
        .baseUrl(Constants.GOOGLE_PLACES_BASE_URL)
        .client(locationOkHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val weatherApiService: WeatherApiService = weatherRetrofit.create(WeatherApiService::class.java)
    val openTripMapApiService: OpenTripMapApiService = openTripMapRetrofit.create(OpenTripMapApiService::class.java)
    val geocodingApiService: GeocodingApiService = geocodingRetrofit.create(GeocodingApiService::class.java)
    val googlePlacesApiService: GooglePlacesApiService = googlePlacesRetrofit.create(GooglePlacesApiService::class.java)
}

