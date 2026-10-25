package com.salattracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationMethodSuggestionTest {
    @Test fun `Pakistan India and Bangladesh suggest Karachi`() {
        listOf("PK", "IN", "BD").forEach { country ->
            assertEquals(CalculationMethod.KARACHI, CalculationMethod.suggestedFor(country))
        }
    }

    @Test fun `United States Saudi Arabia and Turkey get their regional methods`() {
        assertEquals(CalculationMethod.ISNA, CalculationMethod.suggestedFor("US"))
        assertEquals(CalculationMethod.UMM_AL_QURA, CalculationMethod.suggestedFor("SA"))
        assertEquals(CalculationMethod.TURKEY, CalculationMethod.suggestedFor("TR"))
    }

    @Test fun `unknown or absent countries fall back to MWL`() {
        assertEquals(CalculationMethod.MWL, CalculationMethod.suggestedFor("XX"))
        assertEquals(CalculationMethod.MWL, CalculationMethod.suggestedFor(null))
    }
}
