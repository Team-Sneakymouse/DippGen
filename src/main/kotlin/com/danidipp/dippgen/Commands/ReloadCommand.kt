package com.danidipp.dippgen.Commands

import com.danidipp.dippgen.Plugin
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter

class ReloadCommand : ICommandImpl {
    override val name: String = "reload"

    override val executor: CommandExecutor = CommandExecutor { _, _, _, _ ->
        Plugin.plugin.reloadConfig()
        Plugin.plugin.parseConfig()
        true
    }

    override val tabCompleter: TabCompleter? = null
}
