package com.pinkauto.app.di

import android.content.Context
import com.pinkauto.app.data.PinkAutoRepository
import com.pinkauto.app.data.local.db.AppDatabase
import com.pinkauto.app.data.local.session.SessionStore
import com.pinkauto.app.data.remote.FallbackLocationSocketClient
import com.pinkauto.app.data.remote.MockLocationSocketClient
import com.pinkauto.app.data.remote.NetworkModule
import com.pinkauto.app.data.remote.WsLocationSocketClient

class AppContainer(context: Context) {
    private val db = AppDatabase.get(context)
    private val sessionStore = SessionStore(context)
    private val api = NetworkModule.createApi { kotlinx.coroutines.runBlocking { sessionStore.currentAccessToken() } }
    private val locationClient = FallbackLocationSocketClient(
        primary = WsLocationSocketClient(),
        fallback = MockLocationSocketClient()
    )

    val repository = PinkAutoRepository(
        api = api,
        rideDao = db.rideDao(),
        sessionStore = sessionStore,
        locationSocketClient = locationClient
    )
}
