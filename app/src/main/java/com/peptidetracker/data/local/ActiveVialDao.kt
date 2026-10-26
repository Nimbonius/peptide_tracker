package com.peptidetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ActiveVialDao {
    @Query("SELECT COUNT(*) FROM dose_logs WHERE activeVialId = :vialId")
    protected abstract suspend fun doseLogCount(vialId: Long): Int

    @Query("SELECT COUNT(*) FROM active_vials WHERE stockId = :stockId")
    protected abstract suspend fun vialCountForStock(stockId: Long): Int

    @Query("SELECT * FROM active_vials WHERE isArchived = 0 ORDER BY reconstitutionDateEpoch DESC")
    abstract fun observeActive(): Flow<List<ActiveVialEntity>>

    @Query("SELECT * FROM active_vials WHERE id = :id LIMIT 1")
    abstract suspend fun getById(id: Long): ActiveVialEntity?

    @Query("SELECT v.*, s.name AS stockName FROM active_vials AS v INNER JOIN peptide_stock AS s ON s.id = v.stockId WHERE v.isArchived = 0 ORDER BY v.reconstitutionDateEpoch DESC")
    abstract fun observeActiveWithStockName(): Flow<List<ActiveVialWithStockName>>

    @Insert
    abstract suspend fun insert(vial: ActiveVialEntity): Long

    @Update
    abstract suspend fun update(vial: ActiveVialEntity)

    @Query("UPDATE active_vials SET isArchived = 1 WHERE id = :id")
    abstract suspend fun archive(id: Long)

    @Transaction
    open suspend fun deleteStockWhenUnused(stockId: Long): Boolean {
        if (vialCountForStock(stockId) != 0) return false
        deleteStockRow(stockId)
        return true
    }

    @Query("DELETE FROM peptide_stock WHERE id = :stockId")
    protected abstract suspend fun deleteStockRow(stockId: Long)

    @Transaction
    open suspend fun archiveAndDeleteWhenUnlogged(id: Long) {
        if (doseLogCount(id) == 0) deleteUnloggedVial(id) else archive(id)
    }

    @Query("DELETE FROM active_vials WHERE id = :id")
    protected abstract suspend fun deleteUnloggedVial(id: Long)

    @Query("UPDATE active_vials SET remainingVolumeMl = remainingVolumeMl - :volumeMl WHERE id = :id AND isArchived = 0 AND remainingVolumeMl + 0.000000001 >= :volumeMl")
    protected abstract suspend fun decrementVolumeIfAvailable(id: Long, volumeMl: Double): Int

    @Transaction
    open suspend fun recordDoseAndDecrement(
        vialId: Long,
        log: DoseLogEntity
    ): Boolean {
        require(log.activeVialId == vialId) { "Dose log must refer to the vial being updated" }
        require(log.calculatedVolumeMl.isFinite() && log.calculatedVolumeMl > 0.0) {
            "Drawn volume must be finite and greater than zero"
        }
        if (decrementVolumeIfAvailable(vialId, log.calculatedVolumeMl) != 1) return false
        insertDoseLog(log)
        return true
    }

    @Insert
    protected abstract suspend fun insertDoseLog(log: DoseLogEntity): Long
}
