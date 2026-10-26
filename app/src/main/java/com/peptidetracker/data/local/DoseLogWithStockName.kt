package com.peptidetracker.data.local

import androidx.room.ColumnInfo

/** Joined history view; LEFT JOIN keeps immutable dose history visible if a vial is archived. */
data class DoseLogWithStockName(
    val id: Long,
    val activeVialId: Long,
    val timestampEpoch: Long,
    val doseMcg: Double,
    val calculatedVolumeMl: Double,
    val syringeUnits: Double,
    val injectionSite: String,
    val notes: String?,
    @ColumnInfo(name = "stockName") val stockName: String?
)
