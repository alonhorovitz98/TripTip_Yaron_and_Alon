package com.example.triptip_yaron_and_alon.util

import kotlin.math.abs

object LocationUtils {

    data class LatLng(val latitude: Double, val longitude: Double)

    /** Standard Android emulator mock location (Googleplex / Mountain View). */
    private const val EMULATOR_DEFAULT_LAT = 37.4219983
    private const val EMULATOR_DEFAULT_LNG = -122.084
    private const val EMULATOR_LOCATION_TOLERANCE = 0.05

    fun defaultNearbySearchLocation(): LatLng =
        LatLng(Constants.ASHDOD_MARINA_LAT, Constants.ASHDOD_MARINA_LNG)

    /**
     * Maps the emulator's default GPS to Ashdod Marina so nearby results match the app's region.
     */
    fun resolveNearbySearchLocation(latitude: Double, longitude: Double): LatLng {
        if (isAndroidEmulatorDefaultLocation(latitude, longitude)) {
            return defaultNearbySearchLocation()
        }
        return LatLng(latitude, longitude)
    }

    private fun isAndroidEmulatorDefaultLocation(latitude: Double, longitude: Double): Boolean =
        abs(latitude - EMULATOR_DEFAULT_LAT) < EMULATOR_LOCATION_TOLERANCE &&
            abs(longitude - EMULATOR_DEFAULT_LNG) < EMULATOR_LOCATION_TOLERANCE
}
