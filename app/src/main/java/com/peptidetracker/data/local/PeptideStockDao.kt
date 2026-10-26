package com.peptidetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PeptideStockDao {
    @Query("SELECT * FROM peptide_stock ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<PeptideStockEntity>>

    @Query("SELECT * FROM peptide_stock WHERE id = :id LIMIT 1")
    abstract suspend fun getById(id: Long): PeptideStockEntity?

    @Insert
    abstract suspend fun insert(stock: PeptideStockEntity): Long

    @Update
    abstract suspend fun update(stock: PeptideStockEntity)

    @Query("DELETE FROM peptide_stock WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("UPDATE peptide_stock SET unopenedCount = unopenedCount - 1 WHERE id = :id AND unopenedCount > 0")
    protected abstract suspend fun decrementIfAvailable(id: Long): Int

    @Transaction
    open suspend fun consumeOneForReconstitution(id: Long): PeptideStockEntity? {
        if (decrementIfAvailable(id) != 1) return null
        return getById(id)
    }
}
