package com.danidipp.dippgen

import com.danidipp.dippgen.Commands.ICommandImpl
import com.google.common.reflect.ClassPath
import org.bukkit.command.PluginCommand
import org.bukkit.event.Listener

object AutoRegistrar {
    fun registerCommands(plugin: Plugin): Int {
        var count = 0
        runCatching {
            val cp = ClassPath.from(Plugin::class.java.classLoader)
            cp.getTopLevelClasses("com.danidipp.dippgen.Commands").forEach { classInfo ->
                runCatching {
                    val commandClass = Class.forName(classInfo.name)
                    if (!ICommandImpl::class.java.isAssignableFrom(commandClass) || commandClass.isInterface) {
                        return@runCatching
                    }

                    val commandImpl = commandClass.getDeclaredConstructor().newInstance() as ICommandImpl
                    val command: PluginCommand? = plugin.getCommand(commandImpl.name)
                    if (command == null) {
                        plugin.logger.warning("Command ${commandImpl.name} is not defined in plugin.yml")
                        return@runCatching
                    }

                    plugin.logger.info("Registering command /${commandImpl.name}")
                    command.setExecutor(commandImpl.executor)
                    command.tabCompleter = commandImpl.tabCompleter
                    count++

                    if (commandImpl is Listener) {
                        plugin.server.pluginManager.registerEvents(commandImpl, plugin)
                        count++
                    }
                }.onFailure { it.printStackTrace() }
            }
        }.onFailure { it.printStackTrace() }

        return count
    }

    fun registerEvents(plugin: Plugin): Int {
        var count = 0
        runCatching {
            val cp = ClassPath.from(Plugin::class.java.classLoader)
            cp.getTopLevelClasses("com.danidipp.dippgen.Events").forEach { classInfo ->
                runCatching {
                    val eventClass = Class.forName(classInfo.name)
                    if (!Listener::class.java.isAssignableFrom(eventClass) || eventClass.isInterface) {
                        return@runCatching
                    }

                    val event = eventClass.getDeclaredConstructor().newInstance() as Listener
                    plugin.server.pluginManager.registerEvents(event, plugin)
                    count++
                }.onFailure { it.printStackTrace() }
            }
        }.onFailure { it.printStackTrace() }

        return count
    }
}
