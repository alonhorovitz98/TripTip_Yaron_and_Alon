package com.example.triptip_yaron_and_alon.util

/**
 * Application-wide constants
 */
object Constants {
    // Firebase Collections
    const val COLLECTION_POSTS = "posts"
    const val COLLECTION_TRIPS = "trips"
    const val COLLECTION_USERS = "users"
    
    // Firebase Storage paths
    const val STORAGE_POST_IMAGES = "post_images"
    const val STORAGE_PROFILE_IMAGES = "profile_images"
    const val STORAGE_COMMENT_IMAGES = "comment_images"

    // Fallback API key constants — real keys are injected via BuildConfig from local.properties
    const val OPENTRIPMAP_API_KEY = ""
    const val GOOGLE_PLACES_API_KEY = ""
    
    // API Base URLs
    const val OPEN_METEO_BASE_URL = "https://api.open-meteo.com/v1/"
    const val OPENTRIPMAP_BASE_URL = "https://api.opentripmap.io/0.1/en/"
    const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/" // Free geocoding API (fallback)
    const val GOOGLE_PLACES_BASE_URL = "https://maps.googleapis.com/maps/api/place/"
    
    // Default Values
    const val NEARBY_PLACES_RADIUS = 5000 // meters
    const val DEFAULT_PAGINATION_SIZE = 20
    
    // Date/Time Formats
    const val DATE_FORMAT = "yyyy-MM-dd"
    const val TIME_FORMAT = "HH:mm"
    const val DATETIME_FORMAT = "yyyy-MM-dd HH:mm"
}

