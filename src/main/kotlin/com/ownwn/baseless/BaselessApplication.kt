package com.ownwn.baseless

import com.ownwn.baseless.Env2.loadEnv
import com.ownwn.server.Server
import java.nio.file.Files
import java.nio.file.Path

class BaselessApplication {
    companion object {
        fun getEnv(key: String): String? {
            return Env2.envs[key]
        }
    }
}

fun main() {
    loadEnv()
    val port = BaselessApplication.getEnv("PORT")?.toShort() ?: throw RuntimeException("Missing port .env")
    Server.create("/api", port)
}

private object Env2 {
    private val envPath = Path.of(".env")
    val envs = mutableMapOf<String, String>()

    fun loadEnv() {
        if (!Files.exists(envPath)) {
            throw RuntimeException("Missing .env at ${envPath.toAbsolutePath()}")
        }

        val lines = Files.readAllLines(envPath)
        for (line in lines) {
            if (!line.matches(Regex("^[^=]+=[^=]+$"))) { // foo=bar
                throw RuntimeException("Bad env line $line")
            }

            val pair = line.split("=")
            if (envs.contains(pair[0])) {
                throw RuntimeException("Duplicate key ${pair[0]}")
            }
            val hasQuotes: Boolean = pair[1].startsWith("\"") && pair[1].endsWith("\"")
            envs[pair[0]] = if (hasQuotes) pair[1].substring(1, pair[1].length-1) else pair[1]
        }
    }
}
