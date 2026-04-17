package com.pinkauto.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PinkAutoApi {
    @POST("auth/send-otp")
    suspend fun sendOtp(@Body req: SendOtpRequest)

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body req: VerifyOtpRequest): AuthResponse

    @POST("rides/request")
    suspend fun createRide(@Body req: CreateRideRequestDto): RideDto

    @POST("rides/{id}/start")
    suspend fun startRide(@Path("id") id: String): RideDto

    @POST("rides/{id}/end")
    suspend fun endRide(@Path("id") id: String): RideDto

    @GET("rides/history")
    suspend fun ridesHistory(): List<RideDto>
}
