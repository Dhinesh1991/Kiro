package com.pinkauto.app.domain

enum class UserRole { RIDER, DRIVER }
enum class KycStatus { PENDING, UNDER_REVIEW, ACTIVE, REJECTED, BLACKLISTED }
enum class RideStatus { REQUESTED, DRIVER_ASSIGNED, DRIVER_EN_ROUTE, IN_PROGRESS, COMPLETED, CANCELLED }
enum class LoginMethod { OTP, EMAIL, GOOGLE }
enum class PaymentMethod { UPI, CARD, NET_BANKING, WALLET, CASH }

data class User(
    val id: String,
    val name: String,
    val phone: String,
    val role: UserRole = UserRole.RIDER,
    val email: String? = null,
    val photoUrl: String? = null,
    val kycStatus: KycStatus = KycStatus.PENDING,
    val isOnline: Boolean = false,
    val rating: Double = 5.0
)

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val user: User
)

data class RideRequest(
    val pickup: String,
    val destination: String,
    val paymentMethod: PaymentMethod
)

data class FareBreakdown(
    val baseFare: Double,
    val distanceCharge: Double,
    val waitingCharge: Double,
    val surgeMultiplier: Double,
    val platformFee: Double,
    val discount: Double
) {
    val totalFare: Double
        get() = ((baseFare + distanceCharge + waitingCharge) * surgeMultiplier) + platformFee - discount
}

data class Ride(
    val id: String,
    val riderId: String,
    val riderName: String,
    val driverId: String?,
    val driverName: String?,
    val pickup: String,
    val destination: String,
    val status: RideStatus,
    val fare: FareBreakdown? = null,
    val etaSeconds: Int = 0
)

data class Earnings(
    val today: Double = 0.0,
    val week: Double = 0.0
)

data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val channel: String,
    val createdAtEpochMs: Long
)

data class RatingItem(
    val rideId: String,
    val stars: Int,
    val comment: String? = null
)

data class AdminAnalytics(
    val totalRides: Int,
    val totalRevenue: Double,
    val activeDrivers: Int,
    val activeRiders: Int
)
