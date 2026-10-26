package com.peptidetracker.domain.model

import kotlin.math.ceil
import kotlin.math.floor

/** Supported syringe calibrations. These are unit conversions, not dosing recommendations. */
enum class SyringeType(val unitsPerMl: Double) {
    U100(100.0),
    U40(40.0)
}

object PeptideMath {
    private const val MILLIS_PER_DAY = 86_400_000.0

    fun calculateConcentration(mg: Double, ml: Double): Double {
        requirePositiveFinite(mg, "mg")
        requirePositiveFinite(ml, "ml")
        val concentration = mg / ml
        require(concentration.isFinite() && concentration > 0.0) { "Calculated concentration is out of range" }
        return concentration
    }

    fun calculateUnitsRequired(
        targetMcg: Double,
        concentrationMgPerMl: Double,
        syringeType: SyringeType
    ): Double {
        requireNonNegativeFinite(targetMcg, "targetMcg")
        requirePositiveFinite(concentrationMgPerMl, "concentrationMgPerMl")
        val units = (targetMcg / 1_000.0 / concentrationMgPerMl) * syringeType.unitsPerMl
        require(units.isFinite()) { "Calculated syringe units are not finite" }
        return units
    }

    fun calculateCostPerDose(costPerVial: Double, totalVialMg: Double, doseMg: Double): Double {
        requireNonNegativeFinite(costPerVial, "costPerVial")
        requirePositiveFinite(totalVialMg, "totalVialMg")
        requireNonNegativeFinite(doseMg, "doseMg")
        val cost = costPerVial * doseMg / totalVialMg
        require(cost.isFinite()) { "Calculated dose cost is not finite" }
        return cost
    }

    fun calculateRemainingDoses(remainingVolumeMl: Double, doseVolumeMl: Double): Int {
        requireNonNegativeFinite(remainingVolumeMl, "remainingVolumeMl")
        requirePositiveFinite(doseVolumeMl, "doseVolumeMl")
        val count = floor(remainingVolumeMl / doseVolumeMl)
        require(count <= Int.MAX_VALUE) { "Dose count exceeds supported range" }
        return count.toInt()
    }

    /** Epoch values are milliseconds. Pass nowEpochMillis explicitly for a deterministic calculation. */
    fun calculateDaysRemainingBeforeExpiry(
        reconstitutionDateEpoch: Long,
        shelfLifeDays: Int,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): Int {
        require(shelfLifeDays >= 0) { "shelfLifeDays must not be negative" }
        require(reconstitutionDateEpoch >= 0L) { "reconstitutionDateEpoch must not be negative" }
        val expiryEpoch = reconstitutionDateEpoch.toDouble() + shelfLifeDays.toDouble() * MILLIS_PER_DAY
        require(expiryEpoch.isFinite()) { "Expiry timestamp is out of range" }
        val days = ceil((expiryEpoch - nowEpochMillis.toDouble()) / MILLIS_PER_DAY)
        return days.coerceIn(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()).toInt()
    }

    private fun requirePositiveFinite(value: Double, name: String) {
        require(value.isFinite() && value > 0.0) { "$name must be finite and greater than zero" }
    }

    private fun requireNonNegativeFinite(value: Double, name: String) {
        require(value.isFinite() && value >= 0.0) { "$name must be finite and non-negative" }
    }
}
