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

public class RequeueOnBlockBreak implements Listener {

    // @EventHandler
    // public void onBlockPlaceEvent(BlockPlaceEvent event) {
    //     Bukkit.broadcastMessage("BlockPlaceEvent " + event.getBlock().getType().name());
    // }

    @EventHandler(priority = EventPriority.HIGH)
    void onBlockBreakEvent(BlockBreakEvent event) {
        // Bukkit.broadcastMessage("BlockBreakEvent " + event.getBlock().getType().name());
        if (event.isCancelled()) return;
        for (var replacement : Plugin.plugin.replacements) {
            var matchesLocation = replacement.locations().stream().anyMatch(l -> l.equals(event.getBlock().getLocation()));
            var matchesRegion = replacement.regions().stream()
                    .anyMatch(r -> r.contains(event.getBlock().getLocation().getBlockX(), event.getBlock().getLocation().getBlockY(),
                            event.getBlock().getLocation().getBlockZ()))
                    && replacement.blocks().stream().anyMatch(b -> b.material().equals(event.getBlock().getType()))
                    && Plot.getPlot(event.getBlock().getLocation()) == null;
            if (matchesLocation || matchesRegion) {
                // Bukkit.broadcastMessage("Location match: " + event.getBlock().getLocation());
                var min = replacement.minDelay();
                var max = replacement.maxDelay();
                var delay = min == max ? min : new Random().nextLong(max - min) + min;
                var newMaterial = replacement.getRandomMaterial();

                if (!matchesLocation) {
                    // add location to config so it persists through restarts
                    replacement.locations().add(event.getBlock().getLocation());
                    Plugin.plugin.getConfig().set("replacements." + replacement.name(), replacement.toMap());
                    Plugin.plugin.saveConfig();
                }

                TimerTask task = new TimerTask() {
                    public void run() {
                        Plugin.plugin.getLogger().log(Level.FINE, "Ran task");
                        event.getBlock().setType(newMaterial);
                        if (!matchesLocation) {
                            replacement.locations().remove(event.getBlock().getLocation());
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
