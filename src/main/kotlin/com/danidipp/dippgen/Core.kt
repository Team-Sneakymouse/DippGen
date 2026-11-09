package com.danidipp.dippgen

import com.danidipp.dippgen.Commands.InfoCommand
import com.danidipp.dippgen.Events.BreedAnimalsFlag
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.flags.BooleanFlag
import com.sk89q.worldguard.protection.flags.LocationFlag
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.plugin.java.JavaPlugin

class Plugin : JavaPlugin() {
    companion object {
        lateinit var plugin: Plugin
            private set

        val LOG_PREFIX: Component = Component.text("[", NamedTextColor.GRAY)
            .append(Component.text("DIPP", NamedTextColor.AQUA))
            .append(Component.text("] ", NamedTextColor.GRAY))
    }

    var recentBooks: MutableMap<String, BookMeta> = mutableMapOf()
    var autoPickup: MutableMap<Player, ScheduledTask> = mutableMapOf()

    override fun onLoad() {
        logger.info("Plugin is Loading!")
        val flagRegistry = WorldGuard.getInstance().flagRegistry
        runCatching {
            // Plot flags from the old plot management, kept so WorldGuard preserves this data in regions.yml
            // until SneakyPlots has imported it (/plots import worldguard). Remove after the migration.
            flagRegistry.register(BooleanFlag("plot-unlocked"))
            flagRegistry.register(LocationFlag("teleport-location"))
            flagRegistry.register(BreedAnimalsFlag.breedAnimalsFlag)
        }.onFailure {
            logger.warning("Failed to register one or more WorldGuard flags: ${it.message}")
        }
    }

    override fun onEnable() {
        plugin = this
        recentBooks = mutableMapOf()
        autoPickup = mutableMapOf()

        logger.info("Plugin is Starting!")

        var count = AutoRegistrar.registerCommands(this)
        logger.info("Registered $count commands/listeners")

        Bukkit.getScheduler().runTaskLater(this, Runnable {
            val infoCommand = InfoCommand()
            getCommand("info")?.apply {
                setExecutor(infoCommand.executor)
                tabCompleter = infoCommand.tabCompleter
            }
        }, 1L)

        count = AutoRegistrar.registerEvents(this)
        logger.info("Registered $count events")

    }

    override fun onDisable() {
        logger.info("Plugin is Disabling!")
    }

    fun sendDiscord(message: String) {
        val webhook = config.getString("discord_webhook")
        if (webhook.isNullOrBlank()) {
            logger.warning("No Discord webhook configured")
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
            runCatching {
                DiscordWebhook.sendWebhook(webhook, message)
            }.onFailure {
                logger.warning("Failed to send Discord message: ${it.message}")
            }
        })
    }
}
