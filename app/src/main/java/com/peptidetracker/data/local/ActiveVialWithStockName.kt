package com.peptidetracker.data.local

data class ActiveVialWithStockName(
    val id: Long,
    val stockId: Long,
    val reconstitutionDateEpoch: Long,
    val diluentType: String,
    val bacVolumeMl: Double,
    val concentrationMgPerMl: Double,
    val remainingVolumeMl: Double,
    val shelfLifeDays: Int,
    val isArchived: Boolean,
    val stockName: String
)
