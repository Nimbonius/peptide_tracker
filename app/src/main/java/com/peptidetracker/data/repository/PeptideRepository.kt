package com.peptidetracker.data.repository

import androidx.room.withTransaction
import com.peptidetracker.data.local.ActiveVialDao
import com.peptidetracker.data.local.ActiveVialEntity
import com.peptidetracker.data.local.ActiveVialWithStockName
import com.peptidetracker.data.local.DoseLogEntity
import com.peptidetracker.data.local.DoseLogWithStockName
import com.peptidetracker.data.local.PeptideDatabase
import com.peptidetracker.data.local.PeptideStockDao
import com.peptidetracker.data.local.DoseLogDao
import com.peptidetracker.data.local.PeptideStockEntity
import com.peptidetracker.domain.model.PeptideMath
import com.peptidetracker.domain.model.SyringeType
import kotlinx.coroutines.flow.Flow

class PeptideRepository(
    private val database: PeptideDatabase,
    private val stockDao: PeptideStockDao,
    private val vialDao: ActiveVialDao,
    private val doseLogDao: DoseLogDao
) {
    val stocks: Flow<List<PeptideStockEntity>> = stockDao.observeAll()
    val activeVials: Flow<List<ActiveVialWithStockName>> = vialDao.observeActiveWithStockName()

    suspend fun saveStock(stock: PeptideStockEntity) {
        require(stock.name.isNotBlank()) { "Name is required" }
        require(stock.vialMg.isFinite() && stock.vialMg > 0.0) { "Vial mass must be positive" }
        require(stock.unopenedCount >= 0) { "Vial count cannot be negative" }
        require(stock.purchasePriceUsd.isFinite() && stock.purchasePriceUsd >= 0.0) {
            "Price must be finite and non-negative"
        }
        if (stock.id == 0L) stockDao.insert(stock) else stockDao.update(stock)
    }

    suspend fun deleteStock(id: Long) {
        val deleted = vialDao.deleteStockWhenUnused(id)
        check(deleted) { "Archive or remove this stock's active vials before deleting it" }
    }

    suspend fun reconstitute(
        stockId: Long,
        diluentType: String,
        volumeMl: Double,
        shelfLifeDays: Int = 28,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): Long = database.withTransaction {
        require(diluentType.isNotBlank()) { "Diluent type is required" }
        require(volumeMl.isFinite() && volumeMl > 0.0) { "Diluent volume must be positive" }
        require(shelfLifeDays > 0) { "Shelf life must be positive" }
        val consumedStock = stockDao.consumeOneForReconstitution(stockId)
            ?: throw IllegalStateException("No unopened vials remain")
        val concentration = PeptideMath.calculateConcentration(consumedStock.vialMg, volumeMl)
        vialDao.insert(
            ActiveVialEntity(
                stockId = consumedStock.id,
                reconstitutionDateEpoch = nowEpochMillis,
                diluentType = diluentType.trim(),
                bacVolumeMl = volumeMl,
                concentrationMgPerMl = concentration,
                remainingVolumeMl = volumeMl,
                shelfLifeDays = shelfLifeDays
            )
        )
    }

    suspend fun archiveVial(vialId: Long) {
        vialDao.archiveAndDeleteWhenUnlogged(vialId)
    }

    fun observeRecentDoses(): Flow<List<DoseLogWithStockName>> = doseLogDao.observeRecent()

    suspend fun logDose(
        vialId: Long,
        doseMcg: Double,
        syringeUnitsPerMl: Double,
        injectionSite: String,
        notes: String?,
        nowEpochMillis: Long = System.currentTimeMillis()
    ) {
        require(doseMcg.isFinite() && doseMcg > 0.0) { "Dose must be positive" }
        require(syringeUnitsPerMl == 100.0 || syringeUnitsPerMl == 40.0) { "Unsupported syringe calibration" }
        require(injectionSite.isNotBlank()) { "Injection site is required" }
        val vial = vialDao.getById(vialId) ?: throw IllegalArgumentException("Vial not found")
        require(!vial.isArchived) { "Vial is archived" }
        val volumeMl = doseMcg / 1_000.0 / vial.concentrationMgPerMl
        require(volumeMl.isFinite() && volumeMl > 0.0) { "Calculated volume is invalid" }
        val units = PeptideMath.calculateUnitsRequired(
            doseMcg,
            vial.concentrationMgPerMl,
            if (syringeUnitsPerMl == 100.0) SyringeType.U100
            else SyringeType.U40
        )
        val log = DoseLogEntity(
            activeVialId = vialId,
            timestampEpoch = nowEpochMillis,
            doseMcg = doseMcg,
            calculatedVolumeMl = volumeMl,
            syringeUnits = units,
            injectionSite = injectionSite.trim(),
            notes = notes?.trim()?.ifBlank { null }
        )
        val saved = vialDao.recordDoseAndDecrement(vialId, log)
        check(saved) { "Insufficient vial volume for this entry" }
        val updated = vialDao.getById(vialId) ?: return
        if (updated.remainingVolumeMl <= 1e-9) vialDao.archive(updated.id)
    }

    suspend fun suggestedInjectionSite(sites: List<String>): String {
        require(sites.isNotEmpty()) { "At least one injection site is required" }
        val last = doseLogDao.getLastInjectionSite()
        val index = sites.indexOf(last)
        return sites[(index + 1).mod(sites.size)]
    }
}
