package com.danidipp.dippgen.Events

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent

class CommandOverrides : Listener {
    private val commands = arrayListOf("info")

    @EventHandler
    fun onCommandPreprocessEvent(event: PlayerCommandPreprocessEvent) {
        val command = event.message.split(" ")[0].removePrefix("/").lowercase()
        if (commands.contains(command)) {
            event.message = "/dippgen:${event.message.removePrefix("/")}"
        }
    }
}
