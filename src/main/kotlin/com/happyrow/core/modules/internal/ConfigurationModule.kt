package com.happyrow.core.modules.internal

import com.happyrow.core.ConfigLoader
import com.typesafe.config.ConfigException
import org.koin.dsl.module

val configurationModule = module {
    single {
        println("DEBUG: Creating AppConfig")
        try {
            val config = ConfigLoader.getConfig()
            println("DEBUG: AppConfig created successfully: $config")
            config
        } catch (e: ConfigException) {
            println("DEBUG: Failed to create AppConfig: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
