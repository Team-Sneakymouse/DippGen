package com.danidipp.dippgen

import com.danidipp.dippgen.Commands.InfoCommand
import com.danidipp.dippgen.Commands.KoboldCommand
import com.danidipp.dippgen.Events.BreedAnimalsFlag
import com.danidipp.dippgen.Events.Chat
import com.danidipp.dippgen.Modules.PlotManagement.Plot
import com.danidipp.dippgen.Modules.PlotManagement.PlotClaimGUI
import com.danidipp.dippgen.Modules.PlotManagement.PlotDeed
import com.danidipp.dippgen.Modules.PlotManagement.PlotManagementGUI
import com.sk89q.worldguard.WorldGuard
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

    var replacements: MutableList<Replacement> = mutableListOf()
    var replacementRegistrationEnabled: MutableMap<Player, String> = mutableMapOf()
    var recentBooks: MutableMap<String, BookMeta> = mutableMapOf()
    var autoPickup: MutableMap<Player, ScheduledTask> = mutableMapOf()

    override fun onLoad() {
        logger.info("Plugin is Loading!")
        val flagRegistry = WorldGuard.getInstance().flagRegistry
        runCatching {
            flagRegistry.register(Plot.plotUnlockedFlag)
            flagRegistry.register(Plot.teleportLocationFlag)
            flagRegistry.register(BreedAnimalsFlag.breedAnimalsFlag)
            flagRegistry.register(Chat.chatFlag)
        }.onFailure {
            logger.warning("Failed to register one or more WorldGuard flags: ${it.message}")
        }
    }

    override fun onEnable() {
        plugin = this
        replacementRegistrationEnabled = mutableMapOf()
        recentBooks = mutableMapOf()
        autoPickup = mutableMapOf()

        logger.info("Plugin is Starting!")
        parseConfig()
        logger.info("Config Loaded!")

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
        server.pluginManager.registerEvents(PlotDeed.listener, this)
        server.pluginManager.registerEvents(PlotClaimGUI.listener, this)
        server.pluginManager.registerEvents(PlotManagementGUI.listener, this)
        server.pluginManager.registerEvents(KoboldCommand.listener, this)
        count += 4
        logger.info("Registered $count events")

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            DippExpansion(this).register()
        }
        logger.info("Registered placeholders")

        Bukkit.getScheduler().runTaskLater(this, Runnable {
            replacements.forEach { replacement ->
                Bukkit.getScheduler().runTaskLater(this, Runnable {
                    replacement.locations.forEach { replacement.placeBlock(it.block) }
                    if (replacement.regions.isNotEmpty()) {
                        replacement.locations.clear()
                        config.set("replacements.${replacement.name}", replacement.toMap())
                        saveConfig()
                    }
                }, 1L)
            }
        }, 1L)
    }

    override fun onDisable() {
        logger.info("Plugin is Disabling!")
    }

    fun parseConfig() {
        replacements = ReplacementConfigParser.parse(this)
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
