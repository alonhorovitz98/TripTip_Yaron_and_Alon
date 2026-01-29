package com.example.triptip_yaron_and_alon.util

/**
 * Application-wide constants
 */
object Constants {
    // Firebase Collections
    const val COLLECTION_POSTS = "posts"
    const val COLLECTION_TRIPS = "trips"
    const val COLLECTION_USERS = "users"
    
    // Storage Paths (for local file storage)
    const val STORAGE_POST_IMAGES = "post_images"
    const val STORAGE_PROFILE_IMAGES = "profile_images"
    
    // API Keys (store in local.properties or BuildConfig)
    // These will be set when APIs are configured in Step 12
    // For now, use placeholder - will be replaced with BuildConfig values later
    const val OPENWEATHER_API_KEY = "" // TODO: Add to BuildConfig in Step 12
    const val OPENTRIPMAP_API_KEY = "" // TODO: Add to BuildConfig in Step 12
    
    // API Base URLs
    const val OPENWEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/"
    const val OPENTRIPMAP_BASE_URL = "https://api.opentripmap.io/0.1/en/"
    
    // Default Values
    const val NEARBY_PLACES_RADIUS = 5000 // meters
    const val DEFAULT_PAGINATION_SIZE = 20
    
    // Date/Time Formats
    const val DATE_FORMAT = "yyyy-MM-dd"
    const val TIME_FORMAT = "HH:mm"
    const val DATETIME_FORMAT = "yyyy-MM-dd HH:mm"
}

