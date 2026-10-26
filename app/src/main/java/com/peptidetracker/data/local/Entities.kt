package com.peptidetracker.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "peptide_stock")
data class PeptideStockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceVendor: String,
    val lotNumber: String?,
    val vialMg: Double,
    val unopenedCount: Int,
    val purchasePriceUsd: Double,
    val storageLocation: String,
    val expirationDateEpoch: Long?
)

@Entity(
    tableName = "active_vials",
    foreignKeys = [
        ForeignKey(
            entity = PeptideStockEntity::class,
            parentColumns = ["id"],
            childColumns = ["stockId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("stockId")]
)
data class ActiveVialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stockId: Long,
    val reconstitutionDateEpoch: Long,
    val diluentType: String,
    val bacVolumeMl: Double,
    val concentrationMgPerMl: Double,
    val remainingVolumeMl: Double,
    val shelfLifeDays: Int = 28,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = ActiveVialEntity::class,
            parentColumns = ["id"],
            childColumns = ["activeVialId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("activeVialId"), Index("timestampEpoch")]
)
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activeVialId: Long,
    val timestampEpoch: Long,
    val doseMcg: Double,
    val calculatedVolumeMl: Double,
    val syringeUnits: Double,
    val injectionSite: String,
    val notes: String?
)
