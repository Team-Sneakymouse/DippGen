package com.danidipp.dippgen

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Players who opted into permission debug messages. In memory only; cleared on logout. */
object DebugMode {
    private val enabled: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    /** Flips debug mode for [player] and returns the new state. */
    fun toggle(player: UUID): Boolean = if (enabled.remove(player)) false else enabled.add(player)

    fun isEnabled(player: UUID): Boolean = player in enabled

    fun disable(player: UUID) {
        enabled.remove(player)
    }
}
