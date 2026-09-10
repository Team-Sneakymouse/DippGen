package com.danidipp.dippgen

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI

object DiscordWebhook {
    fun sendWebhook(webhookUrl: String, message: String) {
        val connection = (URI(webhookUrl).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }

        OutputStreamWriter(connection.outputStream).use { out ->
            out.write("{\"content\":\"${message.replace("\"", "\\\"")}\"}")
        }

        connection.inputStream.use { }
    }
}
