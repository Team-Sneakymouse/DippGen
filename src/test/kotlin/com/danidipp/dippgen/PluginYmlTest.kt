package com.danidipp.dippgen

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** plugin.yml must match the code: AutoRegistrar only logs a warning for undeclared commands. */
class PluginYmlTest {
    private val pluginYml: YamlConfiguration =
        javaClass.classLoader.getResourceAsStream("plugin.yml")!!.reader().use {
            YamlConfiguration.loadConfiguration(it)
        }

    @Test
    fun `main class is the JavaPlugin`() {
        val main = Class.forName(pluginYml.getString("main"))
        assertTrue(JavaPlugin::class.java.isAssignableFrom(main), "$main is not a JavaPlugin")
    }

    @Test
    fun `every auto-registered command is declared`() {
        val declared =
            checkNotNull(pluginYml.getConfigurationSection("commands")) { "plugin.yml has no commands" }.getKeys(false)

        // Some commands need a running server to construct, so read each ICommandImpl's name from source.
        // Gradle runs tests from the project directory.
        val commandsDir = File("src/main/kotlin/com/danidipp/dippgen/Commands")
        val namePattern = Regex("""override val name: String = "([^"]+)"""")
        val names =
            commandsDir.listFiles { f -> f.extension == "kt" }!!
                .filter { Regex("""class \w+\s*:[^{]*\bICommandImpl\b""").containsMatchIn(it.readText()) }
                .associate { it.name to namePattern.find(it.readText())?.groupValues?.get(1) }
        assertTrue(names.isNotEmpty(), "no ICommandImpl classes found in $commandsDir")
        assertEquals(emptyMap<String, String?>(), names.filterValues { it == null }, "commands without a literal name")

        val missing = names.values.filterNotNull().filter { it !in declared }
        assertEquals(emptyList<String>(), missing, "commands missing from plugin.yml")
    }
}
