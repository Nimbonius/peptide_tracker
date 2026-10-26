package com.peptidetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PeptideMathTest {
    @Test fun concentrationIsMilligramsPerMilliliter() {
        assertEquals(5.0, PeptideMath.calculateConcentration(10.0, 2.0), 1e-9)
    }

    @Test fun concentrationRejectsZeroVolumeAndNonFiniteInputs() {
        assertThrows(IllegalArgumentException::class.java) { PeptideMath.calculateConcentration(10.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { PeptideMath.calculateConcentration(Double.NaN, 2.0) }
    }

    @Test fun syringeUnitsHonorU100AndU40Calibration() {
        assertEquals(10.0, PeptideMath.calculateUnitsRequired(500.0, 5.0, SyringeType.U100), 1e-9)
        assertEquals(4.0, PeptideMath.calculateUnitsRequired(500.0, 5.0, SyringeType.U40), 1e-9)
    }

    @Test fun syringeCalculationRejectsInvalidInputs() {
        assertThrows(IllegalArgumentException::class.java) {
            PeptideMath.calculateUnitsRequired(1.0, 0.0, SyringeType.U100)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PeptideMath.calculateUnitsRequired(Double.POSITIVE_INFINITY, 2.0, SyringeType.U100)
        }
    }

    @Test fun costPerDoseIsProportionalToDoseMass() {
        assertEquals(2.5, PeptideMath.calculateCostPerDose(50.0, 10.0, 0.5), 1e-9)
        assertEquals(0.0, PeptideMath.calculateCostPerDose(50.0, 10.0, 0.0), 1e-9)
    }

    @Test fun remainingDosesRoundDownAndCheckBounds() {
        assertEquals(3, PeptideMath.calculateRemainingDoses(1.0, 0.3))
        assertEquals(0, PeptideMath.calculateRemainingDoses(0.2, 0.3))
        assertThrows(IllegalArgumentException::class.java) {
            PeptideMath.calculateRemainingDoses(1.0, 0.0)
        }
    }

    @Test fun expiryDaysUseCeilingAndAllowExpiredValues() {
        val day = 86_400_000L
        assertEquals(3, PeptideMath.calculateDaysRemainingBeforeExpiry(0L, 28, 25 * day))
        assertEquals(0, PeptideMath.calculateDaysRemainingBeforeExpiry(0L, 28, 28 * day))
        assertEquals(-1, PeptideMath.calculateDaysRemainingBeforeExpiry(0L, 28, 29 * day))
        assertThrows(IllegalArgumentException::class.java) {
            PeptideMath.calculateDaysRemainingBeforeExpiry(0L, -1, 0L)
        }
    }
}
