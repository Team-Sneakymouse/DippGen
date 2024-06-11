package com.danidipp.dippgen.Events;

import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import com.danidipp.dippgen.Plugin;
import com.danidipp.dippgen.Modules.PlotManagement.Plot;

import java.util.Random;
import java.util.TimerTask;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;

public class RequeueOnBlockBreak implements Listener {

    // @EventHandler
    // public void onBlockPlaceEvent(BlockPlaceEvent event) {
    //     Bukkit.broadcastMessage("BlockPlaceEvent " + event.getBlock().getType().name());
    // }

    @EventHandler(priority = EventPriority.HIGH)
    void onBlockBreakEvent(BlockBreakEvent event) {
        // Bukkit.broadcastMessage("BlockBreakEvent " + event.getBlock().getType().name());
        if (event.isCancelled()) return;
        var isTopHalf = event.getBlock().getBlockData() instanceof Bisected
                && ((Bisected) event.getBlock().getBlockData()).getHalf() == Bisected.Half.TOP;
        var eventLocation = isTopHalf ? event.getBlock().getRelative(BlockFace.DOWN).getLocation() : event.getBlock().getLocation();

        for (var replacement : Plugin.plugin.replacements) {
            var matchesLocation = replacement.locations().stream().anyMatch(l -> l.equals(eventLocation));
            var matchesRegion = replacement.regions().stream()
                    .anyMatch(r -> r.contains(eventLocation.getBlockX(), eventLocation.getBlockY(),
                            eventLocation.getBlockZ()))
                    && replacement.blocks().stream().anyMatch(b -> b.material().equals(eventLocation.getBlock().getType()))
                    && Plot.getPlot(eventLocation) == null;
            if (matchesLocation || matchesRegion) {
                // Bukkit.broadcastMessage("Location match: " + eventLocation);
                var min = replacement.minDelay();
                var max = replacement.maxDelay();
                var delay = min == max ? min : new Random().nextLong(max - min) + min;

                if (!matchesLocation) {
                    // add location to config so it persists through restarts
                    replacement.locations().add(eventLocation);
                    Plugin.plugin.getConfig().set("replacements." + replacement.name(), replacement.toMap());
                    Plugin.plugin.saveConfig();
                }

                TimerTask task = new TimerTask() {
                    public void run() {
                        Plugin.plugin.getLogger().log(Level.FINE, "Ran task");
                        replacement.placeBlock(eventLocation.getBlock());
                        if (!matchesLocation) {
                            replacement.locations().remove(eventLocation);
                            Plugin.plugin.getConfig().set("replacements." + replacement.name(), replacement.toMap());
                            Plugin.plugin.saveConfig();
                        }
                    }
                };

                Bukkit.getScheduler().runTaskLater(Plugin.plugin, task, delay);
            }
        }
    }
}
