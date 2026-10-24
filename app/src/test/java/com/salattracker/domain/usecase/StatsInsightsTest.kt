package com.salattracker.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsInsightsTest {
    @Test fun `insight requires a meaningful ten point change`() {
        assertNull(statsInsight(9))
        assertEquals(StatsInsight.CompletionChanged(10), statsInsight(10))
        assertEquals(StatsInsight.CompletionChanged(-12), statsInsight(-12))
    }
}
