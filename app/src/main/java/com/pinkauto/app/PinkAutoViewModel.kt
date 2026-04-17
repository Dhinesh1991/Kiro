package com.pinkauto.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pinkauto.app.data.PinkAutoRepository
import com.pinkauto.app.domain.AdminAnalytics
import com.pinkauto.app.domain.AuthSession
import com.pinkauto.app.domain.Earnings
import com.pinkauto.app.domain.NotificationItem
import com.pinkauto.app.domain.PaymentMethod
import com.pinkauto.app.domain.Ride
import com.pinkauto.app.domain.RideRequest
import com.pinkauto.app.domain.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = false,
    val message: String = "",
    val session: AuthSession? = null,
    val rides: List<Ride> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val earnings: Earnings = Earnings(),
    val analytics: AdminAnalytics? = null,
    val trackingLat: Double? = null,
    val trackingLng: Double? = null,
    val trackingEtaSeconds: Int? = null
)

class PinkAutoViewModel(
    private val repository: PinkAutoRepository
) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.session.collect { session ->
                _state.update { it.copy(session = session) }
            }
        }
        viewModelScope.launch {
            repository.rides.collect { rides ->
                _state.update { it.copy(rides = rides) }
            }
        }
        viewModelScope.launch {
            repository.notifications.collect { items ->
                _state.update { it.copy(notifications = items) }
            }
        }
        viewModelScope.launch { repository.refreshHistory() }
    }

    fun loginOtp(phone: String, otp: String, role: UserRole) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = "") }
            repository.sendOtp(phone)
            val result = repository.verifyOtp(phone, otp, role)
            _state.update {
                if (result.isSuccess) it.copy(loading = false, message = "Login success")
                else it.copy(loading = false, message = result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun loginEmail(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = "") }
            val result = repository.loginWithEmail(email, password)
            _state.update {
                if (result.isSuccess) it.copy(loading = false, message = "Email login success")
                else it.copy(loading = false, message = result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun loginGoogle(token: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = "") }
            val result = repository.loginWithGoogle(token)
            _state.update {
                if (result.isSuccess) it.copy(loading = false, message = "Google login success")
                else it.copy(loading = false, message = result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun submitKyc() {
        val current = _state.value.session ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            repository.submitKyc(current)
            _state.update { it.copy(loading = false, message = "KYC submitted") }
        }
    }

    fun setDriverOnline(online: Boolean) {
        viewModelScope.launch {
            val result = repository.setDriverOnline(online)
            _state.update {
                if (result.isSuccess) it.copy(message = if (online) "You are online" else "You are offline")
                else it.copy(message = result.exceptionOrNull()?.message ?: "Unable to set status")
            }
        }
    }

    fun updateProfile(name: String, email: String?) {
        viewModelScope.launch {
            repository.updateProfile(name, email)
            _state.update { it.copy(message = "Profile updated") }
        }
    }

    fun requestRide(pickup: String, destination: String, paymentMethod: PaymentMethod) {
        val rider = _state.value.session?.user ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            repository.createRide(RideRequest(pickup, destination, paymentMethod), rider)
            _state.update { it.copy(loading = false, message = "Ride requested") }
        }
    }

    fun cancelRide(rideId: String) {
        viewModelScope.launch { repository.cancelRide(rideId) }
    }

    fun startRide(rideId: String) {
        viewModelScope.launch { repository.startRide(rideId) }
    }

    fun endRide(rideId: String) {
        viewModelScope.launch { repository.endRide(rideId) }
    }

    fun submitRating(rideId: String, stars: Int, comment: String?) {
        viewModelScope.launch { repository.rateRide(rideId, stars, comment) }
    }

    fun triggerSos(rideId: String) {
        viewModelScope.launch { repository.triggerSos(rideId) }
    }

    fun shareTracking(rideId: String) {
        viewModelScope.launch {
            repository.shareLiveTracking(rideId)
            repository.observeDriverTracking(rideId).collect { update ->
                _state.update {
                    it.copy(
                        trackingLat = update.lat,
                        trackingLng = update.lng,
                        trackingEtaSeconds = update.etaSeconds
                    )
                }
            }
        }
    }

    fun loadDriverEarnings() {
        val session = _state.value.session ?: return
        viewModelScope.launch {
            val earnings = repository.driverEarnings(session.user.id)
            _state.update { it.copy(earnings = earnings) }
        }
    }

    fun loadAdminAnalytics() {
        viewModelScope.launch {
            val analytics = repository.adminAnalytics()
            _state.update { it.copy(analytics = analytics) }
        }
    }

    fun blacklistDriver(driverId: String, reason: String) {
        viewModelScope.launch {
            repository.blacklistDriver(driverId, reason)
            _state.update { it.copy(message = "Driver blacklisted") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _state.update { it.copy(message = "Logged out", analytics = null, earnings = Earnings()) }
        }
    }
}

class PinkAutoViewModelFactory(
    private val repository: PinkAutoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PinkAutoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PinkAutoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
