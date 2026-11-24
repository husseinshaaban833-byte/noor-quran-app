package com.example.islamic

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object QiblaCalculator {
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LON = 39.826206

    /**
     * Returns Qibla bearing in degrees clockwise from True North [0, 360).
     */
    fun calculateBearingDegrees(lat: Double, lon: Double): Double {
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(KAABA_LAT)
        val deltaLambda = Math.toRadians(KAABA_LON - lon)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360.0) % 360.0
    }

    fun directionLabelAr(bearing: Double): String {
        return when (((bearing + 22.5) / 45.0).toInt() % 8) {
            0 -> "شمال"
            1 -> "شمال شرق"
            2 -> "شرق"
            3 -> "جنوب شرق"
            4 -> "جنوب"
            5 -> "جنوب غرب"
            6 -> "غرب"
            else -> "شمال غرب"
        }
    }
}
