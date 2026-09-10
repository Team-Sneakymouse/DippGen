package com.danidipp.dippgen.Commands

import com.danidipp.dippgen.Plugin
import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.event.player.AsyncChatEvent
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.concurrent.TimeUnit

class KoboldCommand : ICommandImpl {
    companion object {
        private val koboldPlayers: MutableMap<Player, ScheduledTask> = mutableMapOf()

        @JvmField
        val listener: Listener = object : Listener {
            @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
            fun onPlayerChat(event: AsyncChatEvent) {
                val player = event.player
                val task = koboldPlayers.remove(player) ?: return
                task.cancel()

                val plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message())
                val koboldMessage = plainMessage
                    .replace(Regex("[\\w']+"), "kobold")
                    .replace(". k", ". K")
                    .replaceFirst("k", "K")

                val originalRenderer = event.renderer()
                event.renderer(ChatRenderer { source, displayName, message, viewer ->
                    var renderedMessage = message
                    val viewerPlayer = viewer as? Player
                    if (viewerPlayer != null) {
                        val tags = PlaceholderAPI.setPlaceholders(viewerPlayer, "%sneakycharacters_character_tags%")
                            .split(",")
                            .map { it.trim() }
                        val isSpy = viewerPlayer.hasPermission("dipp.koboldspy")
                        val isKobold = tags.any { it.equals("kobold", ignoreCase = true) }
                        if (!isSpy && !isKobold) {
                            renderedMessage = Component.text(koboldMessage)
                        }
                    }

                    originalRenderer.render(source, displayName, renderedMessage, viewer)
                })
            }
        }
    }

    override val name: String = "kobold"

    override val executor: CommandExecutor = CommandExecutor { sender, _, _, args ->
        if (args.isEmpty()) {
            return@CommandExecutor false
        }

        val player = sender as? Player
        if (player == null) {
            sender.sendMessage(Component.text("This command can only be used by a player."))
            return@CommandExecutor true
        }

        koboldPlayers.remove(player)?.cancel()
        val task = Bukkit.getAsyncScheduler().runDelayed(Plugin.plugin, {
            koboldPlayers.remove(player)
        }, 1, TimeUnit.SECONDS)
        koboldPlayers[player] = task
        player.chat(args.joinToString(" "))
        true
    }

    override val tabCompleter: TabCompleter? = null
}
