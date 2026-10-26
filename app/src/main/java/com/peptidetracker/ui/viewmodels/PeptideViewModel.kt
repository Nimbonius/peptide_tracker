package com.peptidetracker.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.peptidetracker.data.local.ActiveVialWithStockName
import com.peptidetracker.data.local.DoseLogWithStockName
import com.peptidetracker.data.local.PeptideDatabaseProvider
import com.peptidetracker.data.local.PeptideStockEntity
import com.peptidetracker.data.repository.PeptideRepository
import com.peptidetracker.domain.model.PeptideMath
import com.peptidetracker.domain.model.SyringeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PeptideViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PeptideDatabaseProvider.get(application)
    private val repository = PeptideRepository(
        database,
        database.peptideStockDao(),
        database.activeVialDao(),
        database.doseLogDao()
    )

    val stocks: StateFlow<List<PeptideStockEntity>> = repository.stocks.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val activeVials: StateFlow<List<ActiveVialWithStockName>> = repository.activeVials.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val recentDoses: StateFlow<List<DoseLogWithStockName>> = repository.observeRecentDoses().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _scratchpad = MutableStateFlow(ScratchpadResult())
    val scratchpad: StateFlow<ScratchpadResult> = _scratchpad.asStateFlow()

    private val _suggestedInjectionSite = MutableStateFlow(INJECTION_SITES.first())
    val suggestedInjectionSite: StateFlow<String> = _suggestedInjectionSite.asStateFlow()

    fun addStock(
        name: String,
        vendor: String,
        lot: String,
        vialMg: Double,
        unopenedCount: Int,
        purchasePriceUsd: Double,
        storageLocation: String,
        expirationDateEpoch: Long?
    ) = launchOperation {
        repository.saveStock(
            PeptideStockEntity(
                name = name.trim(),
                sourceVendor = vendor.trim(),
                lotNumber = lot.trim().ifBlank { null },
                vialMg = vialMg,
                unopenedCount = unopenedCount,
                purchasePriceUsd = purchasePriceUsd,
                storageLocation = storageLocation.trim(),
                expirationDateEpoch = expirationDateEpoch
            )
        )
        _message.value = "Inventory saved"
    }

    fun deleteStock(id: Long) = launchOperation {
        repository.deleteStock(id)
        _message.value = "Inventory item removed"
    }

    fun reconstitute(stockId: Long, diluent: String, volumeMl: Double) = launchOperation {
        repository.reconstitute(stockId, diluent, volumeMl)
        _message.value = "Vial added to active stock"
    }

    fun archiveVial(vialId: Long) = launchOperation {
        repository.archiveVial(vialId)
        _message.value = "Vial archived"
    }

    fun refreshSuggestedInjectionSite() {
        viewModelScope.launch {
            runCatching { repository.suggestedInjectionSite(INJECTION_SITES) }
                .onSuccess { _suggestedInjectionSite.value = it }
                .onFailure { Log.e("PeptideTracker", "Could not load suggested injection site", it) }
        }
    }

    fun logDose(
        vialId: Long,
        doseMcg: Double,
        syringeType: SyringeType,
        site: String,
        notes: String
    ) = launchOperation {
        repository.logDose(vialId, doseMcg, syringeType.unitsPerMl, site, notes)
        _suggestedInjectionSite.value = repository.suggestedInjectionSite(INJECTION_SITES)
        _message.value = "Dose saved and vial volume updated"
    }

    fun calculateScratchpad(doseMcg: Double, concentrationMgPerMl: Double, syringeType: SyringeType) {
        try {
            val units = PeptideMath.calculateUnitsRequired(doseMcg, concentrationMgPerMl, syringeType)
            val volumeMl = units / syringeType.unitsPerMl
            _scratchpad.value = ScratchpadResult(units = units, volumeMl = volumeMl, error = null)
        } catch (exception: IllegalArgumentException) {
            _scratchpad.value = ScratchpadResult(error = exception.message ?: "Check the entered values")
        }
    }

    fun clearMessage() { _message.value = null }

    private fun launchOperation(operation: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                operation()
            } catch (exception: Exception) {
                Log.e("PeptideTracker", "Local tracking operation failed", exception)
                _message.value = exception.message ?: "Operation could not be completed"
            }
        }
    }
}

private val INJECTION_SITES = listOf(
    "Abdomen - Right", "Abdomen - Left", "Thigh - Right", "Thigh - Left", "Deltoid - Right", "Deltoid - Left"
)

data class ScratchpadResult(
    val units: Double? = null,
    val volumeMl: Double? = null,
    val error: String? = null
)
