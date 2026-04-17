package com.pinkauto.app.data

import com.pinkauto.app.data.local.db.RideDao
import com.pinkauto.app.data.local.db.RideEntity
import com.pinkauto.app.data.local.session.SessionStore
import com.pinkauto.app.data.remote.AuthResponse
import com.pinkauto.app.data.remote.CreateRideRequestDto
import com.pinkauto.app.data.remote.FareDto
import com.pinkauto.app.data.remote.DriverLocationUpdate
import com.pinkauto.app.data.remote.LocationSocketClient
import com.pinkauto.app.data.remote.PinkAutoApi
import com.pinkauto.app.data.remote.RideDto
import com.pinkauto.app.data.remote.SendOtpRequest
import com.pinkauto.app.data.remote.VerifyOtpRequest
import com.pinkauto.app.domain.AdminAnalytics
import com.pinkauto.app.domain.AuthSession
import com.pinkauto.app.domain.Earnings
import com.pinkauto.app.domain.FareBreakdown
import com.pinkauto.app.domain.KycStatus
import com.pinkauto.app.domain.NotificationItem
import com.pinkauto.app.domain.PaymentMethod
import com.pinkauto.app.domain.RatingItem
import com.pinkauto.app.domain.Ride
import com.pinkauto.app.domain.RideRequest
import com.pinkauto.app.domain.RideStatus
import com.pinkauto.app.domain.User
import com.pinkauto.app.domain.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlin.random.Random

class PinkAutoRepository(
    private val api: PinkAutoApi,
    private val rideDao: RideDao,
    private val sessionStore: SessionStore,
    private val locationSocketClient: LocationSocketClient
) {
    private val notificationsFlow = MutableStateFlow<List<NotificationItem>>(emptyList())
    private val ratingsFlow = MutableStateFlow<List<RatingItem>>(emptyList())
    private val emergencyContactsFlow = MutableStateFlow(listOf("+919999000111", "+919999000222"))
    private val blacklistedDrivers = MutableStateFlow<Map<String, String>>(emptyMap())

    val session: Flow<AuthSession?> = sessionStore.sessionFlow
    val rides: Flow<List<Ride>> = rideDao.observeAll().map { list -> list.map { it.toDomain() } }
    val notifications: Flow<List<NotificationItem>> = notificationsFlow
    val driverTracking: MutableStateFlow<DriverLocationUpdate?> = MutableStateFlow(null)

    suspend fun sendOtp(phone: String): Boolean = runCatching {
        api.sendOtp(SendOtpRequest(phone))
        createNotification("OTP Sent", "OTP delivered to $phone", "sms")
        true
    }.getOrElse {
        createNotification("OTP Sent", "Offline OTP generated for $phone", "sms")
        phone.length >= 10
    }

    suspend fun verifyOtp(phone: String, otp: String, role: UserRole): Result<AuthSession> {
        if (otp != "123456") return Result.failure(IllegalArgumentException("OTP_INVALID"))
        val session = runCatching {
            api.verifyOtp(VerifyOtpRequest(phone, otp, role)).toDomain()
        }.getOrElse {
            AuthSession(
                accessToken = "offline-access-${Random.nextInt(1000, 9999)}",
                refreshToken = "offline-refresh-${Random.nextInt(1000, 9999)}",
                user = User(
                    id = "${role.name.lowercase()}-${Random.nextInt(1000, 9999)}",
                    name = if (role == UserRole.RIDER) "Rider" else "Driver",
                    phone = phone,
                    role = role,
                    email = if (role == UserRole.RIDER) "rider@pinkauto.app" else "driver@pinkauto.app",
                    kycStatus = if (role == UserRole.DRIVER) KycStatus.PENDING else KycStatus.ACTIVE
                )
            )
        }
        sessionStore.saveSession(session)
        createNotification("Login Success", "Authenticated as ${session.user.role}", "push")
        return Result.success(session)
    }

    suspend fun loginWithEmail(email: String, password: String): Result<AuthSession> {
        if (password.length < 6) return Result.failure(IllegalArgumentException("AUTH_FAILED"))
        val session = AuthSession(
            accessToken = "email-access-${Random.nextInt(1000, 9999)}",
            refreshToken = "email-refresh-${Random.nextInt(1000, 9999)}",
            user = User(
                id = "rider-${Random.nextInt(1000, 9999)}",
                name = "Email Rider",
                phone = "9000000001",
                role = UserRole.RIDER,
                email = email,
                kycStatus = KycStatus.ACTIVE
            )
        )
        sessionStore.saveSession(session)
        createNotification("Login Success", "Email login successful", "push")
        return Result.success(session)
    }

    suspend fun loginWithGoogle(token: String): Result<AuthSession> {
        if (token.isBlank()) return Result.failure(IllegalArgumentException("GOOGLE_TOKEN_INVALID"))
        val session = AuthSession(
            accessToken = "google-access-${Random.nextInt(1000, 9999)}",
            refreshToken = "google-refresh-${Random.nextInt(1000, 9999)}",
            user = User(
                id = "rider-${Random.nextInt(1000, 9999)}",
                name = "Google Rider",
                phone = "9000000003",
                role = UserRole.RIDER,
                email = "google.rider@pinkauto.app",
                kycStatus = KycStatus.ACTIVE
            )
        )
        sessionStore.saveSession(session)
        createNotification("Login Success", "Google sign-in successful", "push")
        return Result.success(session)
    }

    suspend fun submitKyc(current: AuthSession): AuthSession {
        val updated = current.copy(user = current.user.copy(kycStatus = KycStatus.UNDER_REVIEW))
        sessionStore.saveSession(updated)
        createNotification("KYC Submitted", "Your documents are under review", "push")
        return updated
    }

    suspend fun updateProfile(name: String, email: String?): AuthSession? {
        val current = session.first() ?: return null
        val updated = current.copy(user = current.user.copy(name = name, email = email))
        sessionStore.saveSession(updated)
        createNotification("Profile Updated", "Your profile changes were saved", "push")
        return updated
    }

    suspend fun setDriverOnline(online: Boolean): Result<AuthSession> {
        val current = session.first() ?: return Result.failure(IllegalStateException("NO_SESSION"))
        if (current.user.role != UserRole.DRIVER) return Result.failure(IllegalStateException("NOT_DRIVER"))
        if (online && current.user.kycStatus != KycStatus.ACTIVE) {
            return Result.failure(IllegalStateException("KYC_NOT_ACTIVE"))
        }
        if (online && blacklistedDrivers.value.containsKey(current.user.id)) {
            return Result.failure(IllegalStateException("DRIVER_BLACKLISTED"))
        }
        val updated = current.copy(user = current.user.copy(isOnline = online))
        sessionStore.saveSession(updated)
        createNotification("Driver Status", if (online) "You are online" else "You are offline", "push")
        return Result.success(updated)
    }

    suspend fun createRide(request: RideRequest, rider: User): Ride {
        val remoteRide = runCatching {
            api.createRide(CreateRideRequestDto(request.pickup, request.destination, request.paymentMethod))
        }.getOrElse {
            RideDto(
                id = "ride-${Random.nextInt(1000, 9999)}",
                riderId = rider.id,
                riderName = rider.name,
                driverId = "d1",
                driverName = "Driver One",
                pickup = request.pickup,
                destination = request.destination,
                status = RideStatus.DRIVER_ASSIGNED,
                fare = FareDto(30.0, 60.0, 5.0, 1.1, 8.0, if (request.paymentMethod == PaymentMethod.WALLET) 10.0 else 0.0)
            )
        }
        val ride = remoteRide.toDomainRide()
        rideDao.upsert(ride.toEntity())
        createNotification("Ride Assigned", "Driver ${ride.driverName ?: "will be assigned"}", "push")
        return ride
    }

    suspend fun cancelRide(id: String) {
        val ride = rides.first().firstOrNull { it.id == id } ?: return
        rideDao.upsert(ride.copy(status = RideStatus.CANCELLED).toEntity())
        createNotification("Ride Cancelled", "Ride $id has been cancelled", "push")
    }

    suspend fun startRide(id: String) = updateRide(id, RideStatus.IN_PROGRESS)
    suspend fun endRide(id: String) = updateRide(id, RideStatus.COMPLETED)

    private suspend fun updateRide(id: String, forcedStatus: RideStatus) {
        val remote = when (forcedStatus) {
            RideStatus.IN_PROGRESS -> runCatching { api.startRide(id) }.getOrNull()
            RideStatus.COMPLETED -> runCatching { api.endRide(id) }.getOrNull()
            else -> null
        }
        if (remote != null) {
            rideDao.upsert(remote.toDomainRide().toEntity())
        } else {
            val current = rides.first().firstOrNull { it.id == id } ?: return
            rideDao.upsert(current.copy(status = forcedStatus).toEntity())
        }
        if (forcedStatus == RideStatus.COMPLETED) {
            createNotification("Ride Completed", "Payment initiated for ride $id", "push")
        }
    }

    suspend fun rateRide(rideId: String, stars: Int, comment: String?) {
        val safeStars = stars.coerceIn(1, 5)
        ratingsFlow.update { it + RatingItem(rideId = rideId, stars = safeStars, comment = comment) }
        createNotification("Rating Submitted", "You rated this ride $safeStars stars", "push")
    }

    suspend fun triggerSos(activeRideId: String) {
        val contacts = emergencyContactsFlow.value.joinToString(", ")
        createNotification(
            "SOS Triggered",
            "Emergency alert shared for ride $activeRideId to contacts: $contacts and support",
            "sms"
        )
    }

    suspend fun shareLiveTracking(activeRideId: String) {
        createNotification(
            "Live Tracking Shared",
            "Tracking link generated for ride $activeRideId",
            "push"
        )
    }

    fun observeDriverTracking(rideId: String): Flow<DriverLocationUpdate> =
        locationSocketClient.trackRide(rideId).catch {
            emitAll(flowOf(DriverLocationUpdate(rideId, 12.9716, 77.5946, 300)))
        }

    suspend fun driverEarnings(driverId: String): Earnings {
        val completed = rides.first().filter { it.driverId == driverId && it.status == RideStatus.COMPLETED }
        val total = completed.sumOf { it.fare?.totalFare ?: 0.0 }
        return Earnings(today = total * 0.2, week = total)
    }

    suspend fun adminAnalytics(): AdminAnalytics {
        val allRides = rides.first()
        val completed = allRides.filter { it.status == RideStatus.COMPLETED }
        val active = allRides.filter { it.status != RideStatus.CANCELLED && it.status != RideStatus.COMPLETED }
        return AdminAnalytics(
            totalRides = completed.size,
            totalRevenue = completed.sumOf { it.fare?.totalFare ?: 0.0 },
            activeDrivers = active.mapNotNull { it.driverId }.distinct().size,
            activeRiders = active.map { it.riderId }.distinct().size
        )
    }

    suspend fun blacklistDriver(driverId: String, reason: String) {
        blacklistedDrivers.update { it + (driverId to reason) }
        createNotification("Driver Blacklisted", "Driver $driverId blocked: $reason", "push")
    }

    suspend fun refreshHistory() {
        val remote = runCatching { api.ridesHistory() }.getOrElse { emptyList() }
        if (remote.isNotEmpty()) rideDao.upsertAll(remote.map { it.toDomainRide().toEntity() })
    }

    suspend fun logout() {
        sessionStore.clear()
    }

    private fun createNotification(title: String, body: String, channel: String) {
        notificationsFlow.update { current ->
            listOf(
                NotificationItem(
                    id = "ntf-${Random.nextInt(1000, 9999)}",
                    title = title,
                    body = body,
                    channel = channel,
                    createdAtEpochMs = System.currentTimeMillis()
                )
            ) + current
        }
    }
}

private fun RideEntity.toDomain(): Ride = Ride(
    id = id,
    riderId = riderId,
    riderName = riderName,
    driverId = driverId,
    driverName = driverName,
    pickup = pickup,
    destination = destination,
    status = status,
    fare = totalFare?.let {
        FareBreakdown(
            baseFare = it,
            distanceCharge = 0.0,
            waitingCharge = 0.0,
            surgeMultiplier = 1.0,
            platformFee = 0.0,
            discount = 0.0
        )
    }
)

private fun Ride.toEntity(): RideEntity = RideEntity(
    id = id,
    riderId = riderId,
    riderName = riderName,
    driverId = driverId,
    driverName = driverName,
    pickup = pickup,
    destination = destination,
    status = status,
    totalFare = fare?.totalFare
)

private fun AuthResponse.toDomain(): AuthSession = AuthSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    user = User(
        id = user.id,
        name = user.name,
        phone = user.phone,
        role = user.role,
        kycStatus = user.kycStatus
    )
)

private fun RideDto.toDomainRide(): Ride = Ride(
    id = id,
    riderId = riderId,
    riderName = riderName,
    driverId = driverId,
    driverName = driverName,
    pickup = pickup,
    destination = destination,
    status = status,
    fare = fare?.let {
        FareBreakdown(
            baseFare = it.baseFare,
            distanceCharge = it.distanceCharge,
            waitingCharge = it.waitingCharge,
            surgeMultiplier = it.surgeMultiplier,
            platformFee = it.platformFee,
            discount = it.discount
        )
    },
    etaSeconds = 420
)
