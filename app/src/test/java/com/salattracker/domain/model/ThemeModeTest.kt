package com.salattracker.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `LIGHT is never dark, regardless of the system`() {
        assertFalse(ThemeMode.LIGHT.isDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.LIGHT.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `DARK is always dark, regardless of the system`() {
        assertTrue(ThemeMode.DARK.isDark(systemInDarkTheme = true))
        assertTrue(ThemeMode.DARK.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `SYSTEM mirrors whatever the system is currently set to`() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemInDarkTheme = false))
    }
}
