package com.danidipp.dippgen;

import java.util.Collection;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.danidipp.dippgen.Modules.PlotManagement.District;
import com.danidipp.dippgen.Modules.PlotManagement.Plot;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

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
            return String.join(", ", plot.region().getOwners().getPlayers());
        }

        if (params.equalsIgnoreCase("owns_plot_in_distrct")) {
            var district = District.fromLocation(player.getLocation());
            if (district == null)
                return "false";

            var plots = Plot.getPlots(player);
            if (plots.stream().anyMatch(p -> p.district() == district))
                return "true";

            return "false";
        }
        return onRequest(player, params);
    }
}
