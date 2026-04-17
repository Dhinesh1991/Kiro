package com.pinkauto.app.data.remote

import com.pinkauto.app.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WsLocationSocketClient : LocationSocketClient {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    override fun trackRide(rideId: String): Flow<DriverLocationUpdate> = callbackFlow {
        val request = Request.Builder()
            .url("${BuildConfig.WS_BASE_URL}ws/rider/track/$rideId")
            .build()

        val socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val json = JSONObject(text)
                    trySend(
                        DriverLocationUpdate(
                            rideId = rideId,
                            lat = json.optDouble("driver_lat", 0.0),
                            lng = json.optDouble("driver_lng", 0.0),
                            etaSeconds = json.optInt("eta_seconds", 0)
                        )
                    )
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(t)
            }
        })

        awaitClose {
            socket.close(1000, "closed")
        }
    }
}
