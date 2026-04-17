package com.pinkauto.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pinkauto.app.domain.RideStatus

@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey val id: String,
    val riderId: String,
    val riderName: String,
    val driverId: String?,
    val driverName: String?,
    val pickup: String,
    val destination: String,
    val status: RideStatus,
    val totalFare: Double?
)
