package com.danidipp.lom;

import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.danidipp.lom.Commands.ICommandImpl;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.gson.Gson;

public class Plugin extends JavaPlugin {
    public static JavaPlugin plugin;

    @Override
    public void onEnable() {
        Plugin.plugin = this;
        getLogger().info("Plugin is Starting!");

        this.parseConfig();
        getLogger().info("Config Loaded!");

        this.registerCommands();
        getLogger().info("Registered commands!");

        this.registerEvents();
        getLogger().info("Registered Events!");

    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin is Disabling!");
    }

    public void parseConfig() {
        this.saveDefaultConfig();
        var configKeys = this.getConfig().getKeys(true);
        getLogger().warning(configKeys.toString());

        // regions
        if (!configKeys.contains("regions")) {
            this.getConfig().set("regions", this.getConfig().getConfigurationSection("regions").getDefaultSection());
            configKeys = this.getConfig().getKeys(true);
        }
        var regionPaths = configKeys.stream().filter(key -> key.matches("regions.\\w+")).collect(Collectors.toList());
        for (var regionPath : regionPaths) {
            getLogger().warning(regionPath);
            var regionBlockPath = regionPath + ".blocks";
            // blocks
            if (!configKeys.contains(regionBlockPath)) {
                var defaultBlocks = this.getConfig().getConfigurationSection(regionBlockPath).getDefaultSection();
                if (defaultBlocks != null)
                    this.getConfig().set(regionBlockPath, defaultBlocks);
                configKeys.add(regionBlockPath);
            }

            // read region
        }

        this.saveConfig();
    }

    void registerCommands() {
        try {
            ClassPath cp = ClassPath.from(Plugin.class.getClassLoader());
            for (ClassInfo classInfo : cp.getTopLevelClasses("com.danidipp.lom.Commands")) {
                Class<?> commandClass = Class.forName(classInfo.getName());
                getLogger().warning("className: " + classInfo.getName());
                try {
                    if (ICommandImpl.class.isAssignableFrom(commandClass) && !commandClass.isInterface()) {
                        ICommandImpl commandImpl = (ICommandImpl) commandClass.getDeclaredConstructor().newInstance();
                        PluginCommand command = this.getCommand(commandImpl.getName());
                        if (command == null) {
                            getLogger().warning("Command \"" + commandImpl.getName() + "\" is not defined in plugin");
                            continue;
                        }
                        getLogger().info("Registering command /" + commandImpl.getName());
                        command.setExecutor(commandImpl.getExecutor());
                        command.setTabCompleter(commandImpl.getTabCompleter());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void registerEvents() {
        try {
            ClassPath cp = ClassPath.from(Plugin.class.getClassLoader());
            for (ClassInfo classInfo : cp.getTopLevelClasses("com.danidipp.lom.Events")) {
                Class<?> eventClass = Class.forName(classInfo.getName());
                try {
                    if (Listener.class.isAssignableFrom(eventClass) && !eventClass.isInterface()) {
                        Listener event = (Listener) eventClass.getDeclaredConstructor().newInstance();
                        this.getServer().getPluginManager().registerEvents(event, Plugin.plugin);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
