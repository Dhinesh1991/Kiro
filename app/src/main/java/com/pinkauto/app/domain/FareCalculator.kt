package com.pinkauto.app.domain

object FareCalculator {
    fun estimate(
        baseFare: Double,
        perKmRate: Double,
        distanceKm: Double,
        perMinuteRate: Double,
        waitingMinutes: Double,
        surgeMultiplier: Double,
        platformFee: Double,
        discount: Double
    ): FareBreakdown {
        val breakdown = FareBreakdown(
            baseFare = baseFare,
            distanceCharge = perKmRate * distanceKm,
            waitingCharge = perMinuteRate * waitingMinutes,
            surgeMultiplier = surgeMultiplier,
            platformFee = platformFee,
            discount = discount
        )
        return breakdown
    }
}
