package com.danidipp.dippgen.Modules.PlotManagement

import org.bukkit.scheduler.BukkitTask

data class PlayerAddMemberInfo(
    val plot: Plot,
    val task: BukkitTask,
)
