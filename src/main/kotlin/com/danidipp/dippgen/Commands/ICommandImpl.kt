package com.danidipp.dippgen.Commands

import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter

interface ICommandImpl {
    val name: String
    val executor: CommandExecutor
    val tabCompleter: TabCompleter?
}
