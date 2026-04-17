package com.pinkauto.app.data.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

data class DriverLocationUpdate(
    val rideId: String,
    val lat: Double,
    val lng: Double,
    val etaSeconds: Int
)

interface LocationSocketClient {
    fun trackRide(rideId: String): Flow<DriverLocationUpdate>
}

class MockLocationSocketClient : LocationSocketClient {
    override fun trackRide(rideId: String): Flow<DriverLocationUpdate> = flow {
        emit(DriverLocationUpdate(rideId, 12.9716, 77.5946, 480))
    }
}

class FallbackLocationSocketClient(
    private val primary: LocationSocketClient,
    private val fallback: LocationSocketClient = MockLocationSocketClient()
) : LocationSocketClient {
    override fun trackRide(rideId: String): Flow<DriverLocationUpdate> =
        primary.trackRide(rideId).catch {
            emitAll(fallback.trackRide(rideId))
        }
}
