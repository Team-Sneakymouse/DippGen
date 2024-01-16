package com.danidipp.dippgen;

import java.util.Collection;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.danidipp.dippgen.Modules.PlotManagement.District;
import com.danidipp.dippgen.Modules.PlotManagement.Plot;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.md_5.bungee.api.ChatColor;

public class DippExpansion extends PlaceholderExpansion {
    private final Plugin plugin;

    @Override
    public String getAuthor() {
        return "DaniDipp";
    }

    @Override
    public String getIdentifier() {
        return "dipp";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    public DippExpansion(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params.equalsIgnoreCase("test")) {

            return "test123";
        }

        if (params.equalsIgnoreCase("district")) {
            var district = District.fromLocation(player.getLocation());
            if (district == null)
                return "none";
            return district.id();
        }

        if (params.equalsIgnoreCase("district_name")) {
            var district = District.fromLocation(player.getLocation());
            if (district == null)
                return "none";
            return district.name();
        }

        if (params.equalsIgnoreCase("plot")) {
            var plot = Plot.getPlot(player.getLocation());
            if (plot == null)
                return "none";
            return plot.getId().split("-")[1];
        }

        if (params.equalsIgnoreCase("plot_owner")) {
            var plot = Plot.getPlot(player.getLocation());
            if (plot == null)
                return "n.a.";
            var owners = plot.region().getOwners().getUniqueIds();
            if (owners.isEmpty())
                return "none";
            return owners.stream().map(u -> Plugin.plugin.getServer().getOfflinePlayer(u).getName()).collect(Collectors.joining(", "));
        }

        if (params.equalsIgnoreCase("tablist_footer")) {
            var plot = Plot.getPlot(player.getLocation());
            if (plot == null) {
                var district = District.fromLocation(player.getLocation());
                var districtName = district == null ? "n.a." : district.name();

                return districtName;
            } else {
                var districtName = plot.district().name();
                var plotName = plot.getId().split("-")[1];
                var plotOwners = plot.region().getOwners().getUniqueIds();
                var plotOwnersNames = plotOwners.isEmpty() ? null
                        : plotOwners.stream().map(u -> Plugin.plugin.getServer().getOfflinePlayer(u).getName())
                                .collect(Collectors.joining(ChatColor.GRAY + ", " + ChatColor.WHITE));
                var plotOwnerInfo = ChatColor.GRAY + (plotOwnersNames == null ? "unowned" : "owned by " + ChatColor.WHITE + plotOwnersNames);

                return ChatColor.WHITE + plotName + ChatColor.GRAY + " (" + ChatColor.WHITE + districtName + ChatColor.GRAY + ") " + plotOwnerInfo;
            }
        }

        if (params.equalsIgnoreCase("owns_plot_in_distrct")) {
            var district = District.fromLocation(player.getLocation());
            if (district == null)
                return "false";

            var plots = Plot.getOwnedPlots(player);
            if (plots.stream().anyMatch(p -> p.district() == district))
                return "true";

            return "false";
        }
        return onRequest(player, params);
    }
}
