package de.nogaemer.springhomepage.utils

import io.github.cdimascio.dotenv.Dotenv

object EnvUtils {
    private val dotenv = try {
        Dotenv.load()
    } catch (e: Exception) {
        null
    }

    fun getEnvVariable(key: String): String? {
        return dotenv?.get(key) ?: System.getenv(key)
    }
}