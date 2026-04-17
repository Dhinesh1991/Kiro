package com.pinkauto.app.data.remote

import com.pinkauto.app.domain.KycStatus
import com.pinkauto.app.domain.PaymentMethod
import com.pinkauto.app.domain.RideStatus
import com.pinkauto.app.domain.UserRole

data class SendOtpRequest(val phone: String)
data class VerifyOtpRequest(val phone: String, val otp: String, val role: UserRole)
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val name: String,
    val phone: String,
    val role: UserRole,
    val kycStatus: KycStatus
)

data class CreateRideRequestDto(
    val pickup: String,
    val destination: String,
    val paymentMethod: PaymentMethod
)

data class FareDto(
    val baseFare: Double,
    val distanceCharge: Double,
    val waitingCharge: Double,
    val surgeMultiplier: Double,
    val platformFee: Double,
    val discount: Double
)

data class RideDto(
    val id: String,
    val riderId: String,
    val riderName: String,
    val driverId: String?,
    val driverName: String?,
    val pickup: String,
    val destination: String,
    val status: RideStatus,
    val fare: FareDto?
)
