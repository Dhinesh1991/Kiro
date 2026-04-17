package com.pinkauto.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FareCalculatorTest {
    @Test
    fun estimate_applies_formula_with_surge_platform_and_discount() {
        val fare = FareCalculator.estimate(
            baseFare = 30.0,
            perKmRate = 12.0,
            distanceKm = 5.0,
            perMinuteRate = 1.0,
            waitingMinutes = 10.0,
            surgeMultiplier = 1.2,
            platformFee = 8.0,
            discount = 5.0
        )
        assertEquals(123.0, fare.totalFare, 0.0001)
    }
}
