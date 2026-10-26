package com.peptidetracker.data.local

import androidx.room.TypeConverter
import java.time.Instant

class EpochConverters {
    @TypeConverter
    fun nullableEpochToStorage(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun storageToNullableEpoch(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
