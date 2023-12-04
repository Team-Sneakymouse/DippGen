package com.danidipp.lom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.MemorySection;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.danidipp.lom.Commands.ICommandImpl;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

public class Plugin extends JavaPlugin {
    public static Plugin plugin;
    public List<Replacement> replacements;
    public Map<String, BookMeta> recentBooks;

    @Override
    public void onEnable() {
        Plugin.plugin = this;
        this.recentBooks = new HashMap<>();
        getLogger().info("Plugin is Starting!");

        this.parseConfig();
        getLogger().info("Config Loaded!");

        var count = this.registerCommands();
        getLogger().info("Registered " + count + "commands!");

        count = this.registerEvents();
        getLogger().info("Registered " + count + " events!");

    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin is Disabling!");
    }

    public void parseConfig() {
        this.saveDefaultConfig();
        this.replacements = new ArrayList<Replacement>();

        // replacements
        if (!this.getConfig().getKeys(false).contains("replacements")) {
            this.getConfig().set("replacements", this.getConfig().getConfigurationSection("replacements").getDefaultSection());
        }

        var configReplacements = this.getConfig().getConfigurationSection("replacements").getValues(false);
        for (var entry : configReplacements.entrySet()) {
            var replacementName = entry.getKey();
            var configReplacement = (MemorySection) entry.getValue();
            getLogger().warning(replacementName + " " + configReplacement.getClass().toGenericString());

            var configBlocks = configReplacement.getMapList("blocks");
            if (configBlocks == null || configBlocks.isEmpty()) {
                getLogger().warning("Replacement defintion at \"replacements." + replacementName + "\" invalid: \"blocks\" is missing");
                continue;
            }

            Set<ReplacementBlock> blocks = new HashSet<ReplacementBlock>(configBlocks.size());
            for (var configBlock : configBlocks) {
                Material material = Material.getMaterial((String) configBlock.get("material"));
                int weight = configBlock.get("weight") != null ? (Integer) configBlock.get("weight") : 1;
                blocks.add(new ReplacementBlock(material, weight));
            }

            List<String> configLocations = (List<String>) configReplacement.getStringList("coordinates");
            List<Location> locations = new ArrayList<Location>(configLocations.size());
            for (String configLocation : configLocations) {
                var stringCoordinations = configLocation.split(" ");
                int x = Integer.parseInt(stringCoordinations[0]);
                int y = Integer.parseInt(stringCoordinations[1]);
                int z = Integer.parseInt(stringCoordinations[2]);
                Location location = new Location(Bukkit.getServer().getWorlds().get(0), x, y, z);
                locations.add(location);
            }

            var minDelay = configReplacement.contains("minDelay") ? configReplacement.getLong("minDelay") : 120;
            var maxDelay = configReplacement.contains("maxDelay") ? configReplacement.getLong("maxDelay") : 120;

            this.replacements.add(new Replacement(replacementName, blocks, minDelay * 20, maxDelay * 20, locations));
        }
        getLogger().warning(this.replacements.toString());

        this.saveConfig();
    }

    int registerCommands() {
        var count = 0;
        try {
            ClassPath cp = ClassPath.from(Plugin.class.getClassLoader());
            for (ClassInfo classInfo : cp.getTopLevelClasses("com.danidipp.lom.Commands")) {
                Class<?> commandClass = Class.forName(classInfo.getName());
                // getLogger().warning("className: " + classInfo.getName());
                try {
                    if (ICommandImpl.class.isAssignableFrom(commandClass) && !commandClass.isInterface()) {
                        ICommandImpl commandImpl = (ICommandImpl) commandClass.getDeclaredConstructor().newInstance();
                        PluginCommand command = this.getCommand(commandImpl.getName());
                        if (command == null) {
                            this.getLogger().warning("Command \"" + commandImpl.getName() + "\" is not defined in plugin.yml");
                            continue;
                        }
                        this.getLogger().info("Registering command /" + commandImpl.getName());
                        command.setExecutor(commandImpl.getExecutor());
                        command.setTabCompleter(commandImpl.getTabCompleter());
                        count++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    int registerEvents() {
        var count = 0;
        try {
            ClassPath cp = ClassPath.from(Plugin.class.getClassLoader());
            for (ClassInfo classInfo : cp.getTopLevelClasses("com.danidipp.lom.Events")) {
                Class<?> eventClass = Class.forName(classInfo.getName());
                try {
                    if (Listener.class.isAssignableFrom(eventClass) && !eventClass.isInterface()) {
                        Listener event = (Listener) eventClass.getDeclaredConstructor().newInstance();
                        this.getServer().getPluginManager().registerEvents(event, Plugin.plugin);
                        count++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }
}
