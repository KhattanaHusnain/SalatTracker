package com.salattracker.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetVariantTest {
    @Test fun `widget picker exposes every size style and information layout`() {
        assertEquals(16, WidgetVariant.entries.size)
        assertEquals(4, WidgetVariant.entries.count { it.size == WidgetSize.SMALL })
        assertEquals(4, WidgetVariant.entries.count { it.size == WidgetSize.MEDIUM })
        assertEquals(4, WidgetVariant.entries.count { it.size == WidgetSize.LARGE })
        assertEquals(4, WidgetVariant.entries.count { it.size == WidgetSize.LOCK_SCREEN })
        assertEquals(8, WidgetVariant.entries.count { it.style == WidgetStyle.MONOCHROME })
        assertEquals(8, WidgetVariant.entries.count { it.style == WidgetStyle.THEMED })
        assertEquals(8, WidgetVariant.entries.count { it.content == WidgetContent.ALL_FIVE })
        assertEquals(8, WidgetVariant.entries.count { it.content == WidgetContent.CURRENT_NEXT })
    }

    @Test fun `each variant has an independent provider class`() {
        assertEquals(16, WidgetVariant.entries.map(::providerClass).toSet().size)
        assertTrue(WidgetVariant.entries.all { providerClass(it).simpleName.endsWith("WidgetProvider") })
    }
}
