package com.danidipp.dippgen

import com.danidipp.dippgen.Modules.PlotManagement.District
import com.danidipp.dippgen.Modules.PlotManagement.Plot
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player

class DippExpansion(private val plugin: Plugin) : PlaceholderExpansion() {
    private val legacySerializer = LegacyComponentSerializer.legacySection()

    override fun getAuthor(): String = "DaniDipp"

    override fun getIdentifier(): String = "dipp"

    override fun getVersion(): String = "1.0.0"

    override fun persist(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, params: String): String {
        if (player == null) {
            return ""
        }

        return when {
            params.equals("test", ignoreCase = true) -> "test123"
            params.equals("district", ignoreCase = true) -> District.fromLocation(player.location)?.id ?: "none"
            params.equals("district_name", ignoreCase = true) -> District.fromLocation(player.location)?.name ?: "none"
            params.equals("plot", ignoreCase = true) -> Plot.getPlot(player.location)?.id?.split("-")?.getOrNull(1) ?: "none"
            params.equals("plot_owner", ignoreCase = true) -> {
                val plot = Plot.getPlot(player.location) ?: return "n.a."
                val owners = plot.region.owners.uniqueIds
                if (owners.isEmpty()) {
                    "none"
                } else {
                    owners.joinToString(", ") { uuid ->
                        plugin.server.getOfflinePlayer(uuid).name ?: "unknown"
                    }
                }
            }
            params.equals("owns_plot_in_distrct", ignoreCase = true) -> {
                val district = District.fromLocation(player.location) ?: return "false"
                val ownsInDistrict = Plot.getOwnedPlots(player).any { it.district == district }
                if (ownsInDistrict) "true" else "false"
            }
            params.equals("tablist_footer", ignoreCase = true) -> {
                val footer = tablistFooter(player)
                legacySerializer.serialize(footer)
            }
            else -> onRequest(player, params) ?: ""
        }
    }

    private fun tablistFooter(player: Player): Component {
        val plot = Plot.getPlot(player.location)
        if (plot == null) {
            return Component.text(District.fromLocation(player.location)?.name ?: "n.a.", NamedTextColor.GRAY)
        }

        val districtName = plot.district?.name ?: "-"
        val plotName = plot.id.split("-").getOrNull(1) ?: plot.id
        val plotOwners = plot.region.owners.uniqueIds
        val plotGroups = plot.region.owners.groups

        val ownerInfo = when {
            plotOwners.isEmpty() && plotGroups.isEmpty() -> Component.text("unowned", NamedTextColor.GRAY)
            plotOwners.isNotEmpty() -> {
                val ownerNames = plotOwners.joinToString(", ") { uuid ->
                    plugin.server.getOfflinePlayer(uuid).name ?: "unknown"
                }
                Component.text("owned by ", NamedTextColor.GRAY)
                    .append(Component.text(ownerNames, NamedTextColor.WHITE))
            }
            else -> {
                val groupNames = plotGroups.joinToString(", ") { it.uppercase() }
                Component.text("maintained by ", NamedTextColor.GRAY)
                    .append(Component.text(groupNames, NamedTextColor.WHITE))
            }
        }

        return Component.text(plotName, NamedTextColor.WHITE)
            .append(Component.text(" (", NamedTextColor.GRAY))
            .append(Component.text(districtName, NamedTextColor.WHITE))
            .append(Component.text(") ", NamedTextColor.GRAY))
            .append(ownerInfo)
    }
}
