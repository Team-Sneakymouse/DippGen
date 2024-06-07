package com.danidipp.dippgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.danidipp.dippgen.Commands.ICommandImpl;
import com.danidipp.dippgen.Events.BreedAnimalsFlag;
import com.danidipp.dippgen.Events.Chat;
import com.danidipp.dippgen.Modules.PlotManagement.Plot;
import com.danidipp.dippgen.Modules.PlotManagement.PlotClaimGUI;
import com.danidipp.dippgen.Modules.PlotManagement.PlotDeed;
import com.danidipp.dippgen.Modules.PlotManagement.PlotManagementGUI;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import pl.mjaron.tinyloki.ILogStream;
import pl.mjaron.tinyloki.LogController;
import pl.mjaron.tinyloki.TinyLoki;

public class Plugin extends JavaPlugin {
    public static Plugin plugin;
    private LogController lokiLogger;
    public ILogStream lokiChatStream;
    public List<Replacement> replacements;
    public Map<Player, String> replacementRegistrationEnabled;
    public Map<String, BookMeta> recentBooks;

    public static Component LOG_PREFIX = Component.text("[", NamedTextColor.GRAY).append(Component.text("DIPP", NamedTextColor.AQUA))
            .append(Component.text("] ", NamedTextColor.GRAY));

    @Override
    public void onLoad() {
        getLogger().info("Plugin is Loading!");
        WorldGuard worldGuard = WorldGuard.getInstance();
        FlagRegistry flagRegistry = worldGuard.getFlagRegistry();

        flagRegistry.register(Plot.maxMembersFlag);
        flagRegistry.register(Plot.plotUnlockedFlag);
        flagRegistry.register(Plot.teleportLocationFlag);
        flagRegistry.register(BreedAnimalsFlag.breedAnimalsFlag);
        flagRegistry.register(Chat.chatFlag);
    }

    @Override
    public void onEnable() {
        Plugin.plugin = this;
        this.replacementRegistrationEnabled = new HashMap<>();
        this.recentBooks = new HashMap<>();
        getLogger().info("Plugin is Starting!");

        this.parseConfig();
        getLogger().info("Config Loaded!");

        this.lokiLogger = TinyLoki.withUrl("http://grafana-loki:3100/loki/api/v1/push").start();
        this.lokiChatStream = this.lokiLogger.createStream(
                TinyLoki.l("type", "chat")
                        .l("server", this.getConfig().getString("server_name")));

        var count = this.registerCommands();
        getLogger().info("Registered " + count + "commands!");

        count = this.registerEvents();
        this.getServer().getPluginManager().registerEvents(PlotDeed.listener, this);
        this.getServer().getPluginManager().registerEvents(PlotClaimGUI.listener, this);
        this.getServer().getPluginManager().registerEvents(PlotManagementGUI.listener, this);
        count += 3;
        getLogger().info("Registered " + count + " events!");

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DippExpansion(this).register();
        }
        getLogger().info("Registered placeholders!");

        // Refresh all replacements
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (var replacement : plugin.replacements) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            for (var location : replacement.locations()) {
                                location.getBlock().setType(replacement.getRandomMaterial());

                            }
                            if (replacement.regions().size() > 0) {
                                // region-based replacements shouldn't have persistent locations
                                replacement.locations().clear();
                                Plugin.plugin.getConfig().set("replacements." + replacement.name(), replacement.toMap());
                                Plugin.plugin.saveConfig();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin is Disabling!");
        lokiLogger.softStop().hardStop();
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
                try {
                    int x = Integer.parseInt(stringCoordinations[0]);
                    int y = Integer.parseInt(stringCoordinations[1]);
                    int z = Integer.parseInt(stringCoordinations[2]);
                    Location location = new Location(Bukkit.getServer().getWorlds().get(0), x, y, z);
                    locations.add(location);
                } catch (NumberFormatException e) {
                    getLogger().warning("Failed to parse location for replacement " + replacementName + ": " + configLocation);
                }

            }
            List<String> configRegions = (List<String>) configReplacement.getStringList("regions");
            List<ProtectedRegion> regions = new ArrayList<ProtectedRegion>();
            var wgWorld = BukkitAdapter.adapt(Bukkit.getWorld("world"));
            getLogger().warning("Regions: " + configRegions.toString());
            for (String configRegion : configRegions) {
                var region = WorldGuard.getInstance().getPlatform().getRegionContainer().get(wgWorld).getRegion(configRegion);
                if (region == null) {
                    getLogger().warning("Unknown region for replacement " + replacementName + ": " + configRegion);
                    continue;
                }
                regions.add(region);
            }

            var minDelay = configReplacement.contains("minDelay") ? configReplacement.getLong("minDelay") : 120;
            var maxDelay = configReplacement.contains("maxDelay") ? configReplacement.getLong("maxDelay") : 120;

            this.replacements.add(new Replacement(replacementName, blocks, minDelay * 20, maxDelay * 20, locations, regions));
        }
        getLogger().warning(this.replacements.toString());

        this.saveConfig();
    }

    int registerCommands() {
        var count = 0;
        try {
            ClassPath cp = ClassPath.from(Plugin.class.getClassLoader());
            for (ClassInfo classInfo : cp.getTopLevelClasses("com.danidipp.dippgen.Commands")) {
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
                        if (commandImpl instanceof Listener) {
                            Listener event = (Listener) commandImpl;
                            this.getServer().getPluginManager().registerEvents(event, Plugin.plugin);
                            count++;
                        }
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
            for (ClassInfo classInfo : cp.getTopLevelClasses("com.danidipp.dippgen.Events")) {
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
