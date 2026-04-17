package com.pinkauto.app.data.local.db

import androidx.room.TypeConverter
import com.pinkauto.app.domain.RideStatus

class Converters {
    @TypeConverter
    fun fromStatus(value: RideStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): RideStatus = RideStatus.valueOf(value)
}
