package com.danidipp.dippgen.Commands

import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import java.util.Arrays

class CMIGiveCommand : ICommandImpl {
    override val name: String = "cmi_give"

    override val executor: CommandExecutor = CommandExecutor { sender, _, _, args ->
        if (args.size < 2) {
            return@CommandExecutor false
        }

        val playerName = args[0]
        val argument = Arrays.copyOfRange(args, 1, args.size).joinToString(" ").replace(" ", "_")
        sender.server.dispatchCommand(sender, "cmi give $playerName $argument")
        true
    }

    override val tabCompleter: TabCompleter? = null
}
