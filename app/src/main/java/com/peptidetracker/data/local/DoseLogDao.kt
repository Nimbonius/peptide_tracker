package com.peptidetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs WHERE activeVialId = :activeVialId ORDER BY timestampEpoch DESC")
    fun observeForVial(activeVialId: Long): Flow<List<DoseLogEntity>>

    @Query("SELECT d.id, d.activeVialId, d.timestampEpoch, d.doseMcg, d.calculatedVolumeMl, d.syringeUnits, d.injectionSite, d.notes, s.name AS stockName FROM dose_logs AS d LEFT JOIN active_vials AS v ON v.id = d.activeVialId LEFT JOIN peptide_stock AS s ON s.id = v.stockId ORDER BY d.timestampEpoch DESC LIMIT 100")
    fun observeRecent(): Flow<List<DoseLogWithStockName>>

    @Query("SELECT injectionSite FROM dose_logs WHERE injectionSite != '' ORDER BY timestampEpoch DESC LIMIT 1")
    suspend fun getLastInjectionSite(): String?

    @Insert
    suspend fun insert(log: DoseLogEntity): Long
}
