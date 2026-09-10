package com.danidipp.dippgen.Commands

import com.danidipp.dippgen.Modules.PlotManagement.Plot
import com.danidipp.dippgen.Plugin
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.LocalPlayer
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.UUID

class PlotshowCommand : ICommandImpl {
    override val name: String = "plotshow"

    override val executor: CommandExecutor = CommandExecutor { sender, _, _, args ->
        val target = if (args.isEmpty()) {
            val sourcePlayer = sender as? Player ?: run {
                sender.sendMessage(Component.text("error: You must be on the server to use this command without specifying a player", NamedTextColor.RED))
                return@CommandExecutor true
            }

            val plot = Plot.getPlot(sourcePlayer.location)
            if (plot == null) {
                sender.sendMessage(Component.text("error: You are not in a plot", NamedTextColor.RED))
                return@CommandExecutor true
            }

            val uuid = plot.region.owners.uniqueIds.firstOrNull()
            if (uuid == null) {
                sender.sendMessage(Component.text("error: Plot has no owner", NamedTextColor.RED))
                return@CommandExecutor true
            }

            Bukkit.getOfflinePlayer(uuid)
        } else {
            val playerName = args[0]
            val byUuid = runCatching { UUID.fromString(playerName) }.getOrNull()?.let(Bukkit::getOfflinePlayer)
            val byName = Bukkit.getOfflinePlayers().firstOrNull { it.name.equals(playerName, ignoreCase = true) }
            val offline = byUuid ?: byName
            if (offline == null) {
                sender.sendMessage(Component.text("error: Player $playerName does not exist", NamedTextColor.RED))
                return@CommandExecutor true
            }
            offline
        }

        val world = (sender as? Player)?.world ?: Bukkit.getWorlds().firstOrNull()
        if (world == null) {
            sender.sendMessage(Component.text("error: No world available", NamedTextColor.RED))
            return@CommandExecutor true
        }

        val wgWorld = BukkitAdapter.adapt(world)
        val regionManager = WorldGuard.getInstance().platform.regionContainer.get(wgWorld)
        val regions = regionManager?.regions?.values?.toList() ?: emptyList()
        val wgPlayer: LocalPlayer = WorldGuardPlugin.inst().wrapOfflinePlayer(target)

        val ownerRegions = regions.filter { it.owners.contains(wgPlayer) }
        val memberRegions = regions.filter { it.members.contains(wgPlayer) }

        val infoComponent = Component.text("[info]", NamedTextColor.YELLOW)
            .hoverEvent(HoverEvent.showText(Component.text("Show player info")))
            .clickEvent(ClickEvent.runCommand("/cmi info ${target.name}"))

        val altsComponent = Component.text("[alts]", NamedTextColor.YELLOW)
            .hoverEvent(HoverEvent.showText(Component.text("Show player alts")))
            .clickEvent(ClickEvent.runCommand("/cmi checkaccount ${target.name}"))

        val isOnline = target.isOnline
        val teleportComponent = Component.text("[tp]", if (isOnline) NamedTextColor.YELLOW else NamedTextColor.GRAY)
            .hoverEvent(HoverEvent.showText(Component.text(if (isOnline) "Teleport to player" else "Player is offline")))
            .clickEvent(if (isOnline) ClickEvent.runCommand("/minecraft:tp @s ${target.name}") else null)

        sender.sendMessage(
            Component.textOfChildren(
                Plugin.LOG_PREFIX,
                Component.text("Plot info for ", NamedTextColor.GRAY),
                Component.text(target.name ?: "unknown", NamedTextColor.WHITE),
                Component.space(),
                infoComponent,
                Component.space(),
                altsComponent,
                Component.space(),
                teleportComponent,
            ),
        )

        sender.sendMessage(Component.text("${target.name} owns ${ownerRegions.size} plots:", NamedTextColor.GRAY))
        ownerRegions.forEach { region ->
            val indirect = !region.owners.playerDomain.contains(wgPlayer)
            sender.sendMessage(formatRegion(region, world, indirect = indirect))
        }

        if (memberRegions.isNotEmpty()) {
            sender.sendMessage(Component.text("${target.name} is a member of ${memberRegions.size} plots:", NamedTextColor.GRAY))
            memberRegions.forEach { region ->
                val indirect = !region.members.contains(wgPlayer)
                sender.sendMessage(formatRegion(region, world, indirect = indirect, showOwner = true))
            }
        }

        true
    }

    private fun formatRegion(
        region: ProtectedRegion,
        world: World,
        indirect: Boolean = false,
        showOwner: Boolean = false,
    ): Component {
        var regionCenter = region.minimumPoint.add(region.maximumPoint).divide(2)
        var centerY = region.maximumPoint.blockY

        while (world.getBlockAt(regionCenter.blockX, centerY, regionCenter.blockZ).isPassable) {
            centerY--
        }
        regionCenter = regionCenter.withY(centerY + 1)

        val infoComponent = Component.text("[info]", NamedTextColor.YELLOW)
            .hoverEvent(HoverEvent.showText(Component.text("Show plot info")))
            .clickEvent(ClickEvent.runCommand("/rg info -w ${world.name} ${region.id}"))

        val tpCommand = "/minecraft:tp @s ${regionCenter.blockX} ${regionCenter.blockY} ${regionCenter.blockZ}"
        val teleportComponent = Component.text("[tp]", NamedTextColor.YELLOW)
            .hoverEvent(HoverEvent.showText(Component.text("Teleport to plot")))
            .clickEvent(ClickEvent.runCommand(tpCommand))

        val hasContainers = PlotChestsCommand.getContainers(region, world).isNotEmpty()
        val chestsComponent = Component.text("[chests]", if (hasContainers) NamedTextColor.YELLOW else NamedTextColor.GRAY)
            .hoverEvent(HoverEvent.showText(Component.text(if (hasContainers) "Show plot chests" else "No chests found")))
            .clickEvent(if (hasContainers) ClickEvent.runCommand("/dippgen:plotchests ${world.name}:${region.id}") else null)

        val ownerName = region.owners.uniqueIds
            .map(Bukkit::getOfflinePlayer)
            .mapNotNull { it.name }
            .firstOrNull() ?: "Unknown"

        var regionName: Component = if (showOwner) {
            Component.text("$ownerName's plot", NamedTextColor.WHITE)
                .hoverEvent(HoverEvent.showText(Component.text(region.id)))
        } else {
            Component.text(region.id, NamedTextColor.WHITE)
        }

        if (indirect) {
            regionName = regionName.color(NamedTextColor.GRAY)
                .hoverEvent(HoverEvent.showText(Component.text("via group")))
        }

        return Component.textOfChildren(
            Component.text("- "),
            regionName,
            Component.space(),
            infoComponent,
            Component.space(),
            teleportComponent,
            Component.space(),
            chestsComponent,
        )
    }

    override val tabCompleter: TabCompleter = TabCompleter { _, _, _, args ->
        val prefix = args.firstOrNull().orEmpty().lowercase()
        Bukkit.getOfflinePlayers()
            .mapNotNull { it.name }
            .filter { it.lowercase().startsWith(prefix) }
    }
}
