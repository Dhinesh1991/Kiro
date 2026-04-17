package com.pinkauto.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pinkauto.app.domain.KycStatus
import com.pinkauto.app.domain.PaymentMethod
import com.pinkauto.app.domain.Ride
import com.pinkauto.app.domain.RideStatus
import com.pinkauto.app.domain.UserRole

private enum class HomeTab { RIDE, PROFILE, SAFETY, NOTIFICATIONS, ADMIN }
private enum class LoginTab { OTP, EMAIL, GOOGLE }

@Composable
fun PinkAutoApp(vm: PinkAutoViewModel) {
    val state by vm.state.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.session == null) {
                LoginScreen(vm)
                return@Column
            }

            Text(
                text = "Welcome ${state.session?.user?.name} (${state.session?.user?.role})",
                style = MaterialTheme.typography.titleLarge
            )
            Text("KYC: ${state.session?.user?.kycStatus}")

            if (state.loading) CircularProgressIndicator()
            if (state.message.isNotBlank()) Text(state.message, color = MaterialTheme.colorScheme.primary)

            when (state.session?.user?.role) {
                UserRole.RIDER -> RiderHome(vm, state)
                UserRole.DRIVER -> DriverHome(vm, state.rides, state.session?.user?.kycStatus)
                null -> {}
            }

            NotificationFeed(state.notifications)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = vm::logout) { Text("Logout") }
        }
    }
}

@Composable
private fun LoginScreen(vm: PinkAutoViewModel) {
    var tab by remember { mutableStateOf(LoginTab.OTP) }
    var phone by rememberSaveable { mutableStateOf("9000000001") }
    var otp by rememberSaveable { mutableStateOf("123456") }
    var roleDriver by remember { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("rider@pinkauto.app") }
    var password by rememberSaveable { mutableStateOf("secret123") }
    var googleToken by rememberSaveable { mutableStateOf("google-token") }

    Text("Pink Auto", style = MaterialTheme.typography.headlineMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { tab = LoginTab.OTP }) { Text("OTP") }
        Button(onClick = { tab = LoginTab.EMAIL }) { Text("Email") }
        Button(onClick = { tab = LoginTab.GOOGLE }) { Text("Google") }
    }

    when (tab) {
        LoginTab.OTP -> {
            OutlinedTextField(phone, { phone = it }, label = { Text("Phone") })
            OutlinedTextField(otp, { otp = it }, label = { Text("OTP") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { roleDriver = false }) { Text("Rider") }
                Button(onClick = { roleDriver = true }) { Text("Driver") }
            }
            Button(onClick = {
                vm.loginOtp(phone, otp, if (roleDriver) UserRole.DRIVER else UserRole.RIDER)
            }) { Text("Login with OTP") }
            Text("Demo OTP: 123456")
        }

        LoginTab.EMAIL -> {
            OutlinedTextField(email, { email = it }, label = { Text("Email") })
            OutlinedTextField(password, { password = it }, label = { Text("Password") })
            Button(onClick = { vm.loginEmail(email, password) }) { Text("Login with Email") }
        }

        LoginTab.GOOGLE -> {
            OutlinedTextField(googleToken, { googleToken = it }, label = { Text("Google Token") })
            Button(onClick = { vm.loginGoogle(googleToken) }) { Text("Login with Google") }
        }
    }
}

@Composable
private fun RiderHome(vm: PinkAutoViewModel, state: UiState) {
    var tab by remember { mutableStateOf(HomeTab.RIDE) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { tab = HomeTab.RIDE }) { Text("Ride") }
        Button(onClick = { tab = HomeTab.PROFILE }) { Text("Profile") }
        Button(onClick = { tab = HomeTab.SAFETY }) { Text("Safety") }
        Button(onClick = { tab = HomeTab.ADMIN }) { Text("Admin") }
    }
    when (tab) {
        HomeTab.RIDE -> RiderRideTab(vm, state.rides)
        HomeTab.PROFILE -> ProfileTab(vm)
        HomeTab.SAFETY -> SafetyTab(vm, state)
        HomeTab.NOTIFICATIONS -> {}
        HomeTab.ADMIN -> AdminTab(vm)
    }
}

@Composable
private fun DriverHome(vm: PinkAutoViewModel, rides: List<Ride>, kycStatus: KycStatus?) {
    if (kycStatus != KycStatus.ACTIVE) {
        Text("Complete KYC to go online and accept rides.")
        Button(onClick = vm::submitKyc) { Text("Submit KYC") }
        return
    }
    var tab by remember { mutableStateOf(HomeTab.RIDE) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { vm.setDriverOnline(true) }) { Text("Go Online") }
        Button(onClick = { vm.setDriverOnline(false) }) { Text("Go Offline") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { tab = HomeTab.RIDE }) { Text("Ride") }
        Button(onClick = { tab = HomeTab.PROFILE }) { Text("Profile") }
    }
    when (tab) {
        HomeTab.RIDE -> DriverRideTab(vm, rides)
        HomeTab.PROFILE -> DriverProfileTab(vm)
        else -> Unit
    }
}

@Composable
private fun RiderRideTab(vm: PinkAutoViewModel, rides: List<Ride>) {
    var pickup by rememberSaveable { mutableStateOf("Tambaram") }
    var destination by rememberSaveable { mutableStateOf("Guindy") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var rating by remember { mutableIntStateOf(5) }
    var ratingComment by rememberSaveable { mutableStateOf("") }

    Text("Book Ride", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(pickup, { pickup = it }, label = { Text("Pickup") })
    OutlinedTextField(destination, { destination = it }, label = { Text("Destination") })
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { paymentMethod = PaymentMethod.UPI }) { Text("UPI") }
        Button(onClick = { paymentMethod = PaymentMethod.CARD }) { Text("Card") }
        Button(onClick = { paymentMethod = PaymentMethod.CASH }) { Text("Cash") }
    }
    Button(onClick = { vm.requestRide(pickup, destination, paymentMethod) }) { Text("Confirm Booking") }

    val riderRides = rides
    RideList(
        rides = riderRides,
        onStart = {},
        onEnd = {},
        onCancel = vm::cancelRide
    )
    val completed = riderRides.firstOrNull { it.status == RideStatus.COMPLETED }
    if (completed != null) {
        Text("Rate your completed ride")
        OutlinedTextField(value = rating.toString(), onValueChange = { rating = it.toIntOrNull() ?: 5 }, label = { Text("Stars 1-5") })
        OutlinedTextField(ratingComment, { ratingComment = it }, label = { Text("Comment") })
        Button(onClick = { vm.submitRating(completed.id, rating, ratingComment) }) { Text("Submit Rating") }
    }
}

@Composable
private fun DriverRideTab(vm: PinkAutoViewModel, rides: List<Ride>) {
    val active = rides.filter { it.status != RideStatus.CANCELLED && it.status != RideStatus.COMPLETED }
    Text("Driver Ride Queue", style = MaterialTheme.typography.titleMedium)
    RideList(
        rides = active,
        onStart = vm::startRide,
        onEnd = vm::endRide,
        onCancel = {}
    )
    Button(onClick = vm::loadDriverEarnings) { Text("Refresh Earnings") }
    val earningsState by vm.state.collectAsState()
    Text("Today: Rs ${"%.2f".format(earningsState.earnings.today)}")
    Text("Week: Rs ${"%.2f".format(earningsState.earnings.week)}")
}

@Composable
private fun ProfileTab(vm: PinkAutoViewModel) {
    var name by rememberSaveable { mutableStateOf("Updated Rider") }
    var email by rememberSaveable { mutableStateOf("rider.updated@pinkauto.app") }
    Text("Profile", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(name, { name = it }, label = { Text("Name") })
    OutlinedTextField(email, { email = it }, label = { Text("Email") })
    Button(onClick = { vm.updateProfile(name, email) }) { Text("Save Profile") }
}

@Composable
private fun DriverProfileTab(vm: PinkAutoViewModel) {
    var name by rememberSaveable { mutableStateOf("Driver Updated") }
    Text("Driver Profile", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(name, { name = it }, label = { Text("Name") })
    Button(onClick = { vm.updateProfile(name, null) }) { Text("Save") }
}

@Composable
private fun SafetyTab(vm: PinkAutoViewModel, state: UiState) {
    val active = state.rides.firstOrNull { it.status == RideStatus.IN_PROGRESS } ?: state.rides.firstOrNull()
    Text("Safety", style = MaterialTheme.typography.titleMedium)
    if (active == null) {
        Text("No active ride for SOS/tracking share.")
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { vm.triggerSos(active.id) }) { Text("SOS") }
        Button(onClick = { vm.shareTracking(active.id) }) { Text("Share Tracking") }
    }
    if (state.trackingLat != null && state.trackingLng != null) {
        Text("Driver tracking: ${state.trackingLat}, ${state.trackingLng}")
        Text("ETA: ${(state.trackingEtaSeconds ?: 0) / 60} min")
    }
}

@Composable
private fun AdminTab(vm: PinkAutoViewModel) {
    var driverId by rememberSaveable { mutableStateOf("d1") }
    var reason by rememberSaveable { mutableStateOf("Unsafe behavior") }
    Button(onClick = vm::loadAdminAnalytics) { Text("Load Admin Analytics") }
    val state by vm.state.collectAsState()
    state.analytics?.let {
        Text("Completed rides: ${it.totalRides}")
        Text("Revenue: Rs ${"%.2f".format(it.totalRevenue)}")
        Text("Active drivers: ${it.activeDrivers}")
        Text("Active riders: ${it.activeRiders}")
    }
    OutlinedTextField(driverId, { driverId = it }, label = { Text("Driver Id") })
    OutlinedTextField(reason, { reason = it }, label = { Text("Blacklist reason") })
    Button(onClick = { vm.blacklistDriver(driverId, reason) }) { Text("Blacklist Driver") }
}

@Composable
private fun NotificationFeed(items: List<com.pinkauto.app.domain.NotificationItem>) {
    Text("Notifications", style = MaterialTheme.typography.titleSmall)
    LazyColumn(modifier = Modifier.height(140.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(items.take(8)) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(item.title, style = MaterialTheme.typography.labelLarge)
                    Text(item.body)
                    Text("Channel: ${item.channel}")
                }
            }
        }
    }
}

@Composable
private fun RideList(
    rides: List<Ride>,
    onStart: (String) -> Unit,
    onEnd: (String) -> Unit,
    onCancel: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(220.dp)) {
        items(rides) { ride ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${ride.pickup} -> ${ride.destination}", style = MaterialTheme.typography.titleSmall)
                    Text("Status: ${ride.status}")
                    Text("Driver: ${ride.driverName ?: "Pending"}")
                    ride.fare?.let { Text("Fare: Rs ${"%.2f".format(it.totalFare)}") }
                    if (ride.etaSeconds > 0) Text("ETA: ${ride.etaSeconds / 60} min")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (ride.status == RideStatus.DRIVER_ASSIGNED || ride.status == RideStatus.DRIVER_EN_ROUTE) {
                            Button(onClick = { onStart(ride.id) }) { Text("Start") }
                        }
                        if (ride.status == RideStatus.IN_PROGRESS) {
                            Button(onClick = { onEnd(ride.id) }) { Text("End") }
                        }
                        if (ride.status == RideStatus.REQUESTED || ride.status == RideStatus.DRIVER_ASSIGNED) {
                            Button(onClick = { onCancel(ride.id) }) { Text("Cancel") }
                        }
                    }
                }
            }
        }
    }
}
