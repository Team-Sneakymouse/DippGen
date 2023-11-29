package com.danidipp.lom.Events;

import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.EventHandler;

import com.danidipp.lom.Plugin;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.bukkit.Bukkit;

public class HandleBlockBreak implements Listener {

    // @EventHandler
    // public void onBlockPlaceEvent(BlockPlaceEvent event) {
    //     Bukkit.broadcastMessage("BlockPlaceEvent " + event.getBlock().getType().name());
    // }

    @EventHandler
    void onBlockBreakEvent(BlockBreakEvent event) {
        // Bukkit.broadcastMessage("BlockBreakEvent " + event.getBlock().getType().name());

        for (var replacement : ((Plugin) Plugin.plugin).replacements) {
            if (replacement.locations().stream().anyMatch(l -> l.equals(event.getBlock().getLocation()))) {
                // Bukkit.broadcastMessage("Location match: " + event.getBlock().getLocation());
                var min = replacement.minDelay();
                var max = replacement.maxDelay();
                var delay = min == max ? min : new Random().nextLong(max - min) + min;
                var material = replacement.getRandomMaterial();

                TimerTask task = new TimerTask() {
                    public void run() {
                        Plugin.plugin.getLogger().warning("Ran task");
                        event.getBlock().setType(material);
                    }
                };

                var bukkitTask = Bukkit.getScheduler().runTaskLater(Plugin.plugin, task, delay);
                // event.getPlayer().sendMessage("Scheduling task " + bukkitTask.getTaskId() + " for block " + material.name() + " in " + delay + " ticks");

            }
        }
    }
}
