package com.danidipp.dippgen.Commands

import com.danidipp.dippgen.Modules.PlotManagement.Plot
import com.danidipp.dippgen.Plugin
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector2
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Polygonal2DRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.managers.RegionManager
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import com.sk89q.worldguard.protection.regions.RegionContainer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter

class PlotChestsCommand : ICommandImpl {
    companion object {
        fun getContainers(region: ProtectedRegion, world: World): Set<Block> {
            val weRegion: Region = when (region) {
                is ProtectedCuboidRegion -> {
                    val min = region.minimumPoint
                    val max = region.maximumPoint
                    CuboidRegion(BukkitAdapter.adapt(world), min, max)
                }
                is ProtectedPolygonalRegion -> {
                    val points: List<BlockVector2> = region.points
                    val minY = region.minimumPoint.blockY
                    val maxY = region.maximumPoint.blockY
                    Polygonal2DRegion(BukkitAdapter.adapt(world), points, minY, maxY)
                }
                else -> throw RuntimeException("Unsupported region type: ${region.type.name}")
            }

            val containers = linkedSetOf<Block>()
            for (vec3: BlockVector3 in weRegion) {
                val block = world.getBlockAt(vec3.blockX, vec3.blockY, vec3.blockZ)
                if (block.state is Container) {
                    containers += block
                }
            }
            return containers
        }

        fun formatBlock(inventoryBlock: Block): Component {
            val container = inventoryBlock.state as Container
            val name = container.customName() ?: Component.text(inventoryBlock.type.name)
            val pos = inventoryBlock.location
            val slotsUsed = container.inventory.contents.count { it != null }
            val maxSlots = container.inventory.size
            val command = "/dippgen:opencontainer ${pos.world.name} ${pos.blockX} ${pos.blockY} ${pos.blockZ}"

            return Component.textOfChildren(
                name,
                Component.space(),
                Component.text("[", NamedTextColor.DARK_GRAY),
                Component.text(pos.blockX, NamedTextColor.GRAY),
                Component.text(", ", NamedTextColor.DARK_GRAY),
                Component.text(pos.blockY, NamedTextColor.GRAY),
                Component.text(", ", NamedTextColor.DARK_GRAY),
                Component.text(pos.blockZ, NamedTextColor.GRAY),
                Component.text("]: ", NamedTextColor.DARK_GRAY),
                Component.text(slotsUsed, NamedTextColor.YELLOW),
                Component.text("/", NamedTextColor.WHITE),
                Component.text(maxSlots, NamedTextColor.YELLOW),
            )
                .hoverEvent(HoverEvent.showText(Component.text("Click to open")))
                .clickEvent(ClickEvent.runCommand(command))
        }
    }

    override val name: String = "plotchests"

    override val executor: CommandExecutor = CommandExecutor { sender, _, _, args ->
        if (args.size != 1) {
            return@CommandExecutor false
        }

        val plotId = args[0]
        val plot = Plot.getPlot(plotId)
        if (plot == null) {
            sender.sendMessage(Component.text("error: Plot $plotId does not exist", NamedTextColor.RED))
            return@CommandExecutor true
        }

        val inventoryBlocks = getContainers(plot.region, plot.world)
        sender.sendMessage(Plugin.LOG_PREFIX.append(Component.text("Found ${inventoryBlocks.size} inventories in plot $plotId:")))
        inventoryBlocks.forEach { sender.sendMessage(formatBlock(it)) }
        true
    }

    override val tabCompleter: TabCompleter = TabCompleter { _, _, _, args ->
        if (args.size != 1) {
            return@TabCompleter emptyList()
        }

        var worlds = Bukkit.getWorlds()
        if (args[0].contains(':')) {
            val worldName = args[0].substringBefore(':')
            val world = worlds.firstOrNull { it.name == worldName }
            if (world != null) {
                worlds = mutableListOf(world)
            }
        }

        val regionNames = mutableListOf<String>()
        worlds.forEach { world ->
            val wgWorld = BukkitAdapter.adapt(world)
            val container: RegionContainer = WorldGuard.getInstance().platform.regionContainer
            val regionManager: RegionManager = container.get(wgWorld) ?: return@forEach
            regionManager.regions.keys.forEach { regionName ->
                regionNames += "${world.name}:$regionName"
            }
        }

        regionNames.filter { it.startsWith(args[0], ignoreCase = true) }
    }
}
