package com.danidipp.dippgen

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class DebugModeTest {
    @Test
    fun `off by default and toggles on and off`() {
        val player = UUID.randomUUID()
        assertFalse(DebugMode.isEnabled(player))

        assertTrue(DebugMode.toggle(player))
        assertTrue(DebugMode.isEnabled(player))

        assertFalse(DebugMode.toggle(player))
        assertFalse(DebugMode.isEnabled(player))
    }

    @Test
    fun `disable clears the toggle and only affects that player`() {
        val player = UUID.randomUUID()
        val other = UUID.randomUUID()
        DebugMode.toggle(player)
        DebugMode.toggle(other)

        DebugMode.disable(player)

        assertFalse(DebugMode.isEnabled(player))
        assertTrue(DebugMode.isEnabled(other))
        DebugMode.disable(other)
    }
}
