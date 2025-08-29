package com.happyrow.core.modules.internal

import com.happyrow.core.ConfigLoader
import org.koin.core.module.Module
import org.koin.dsl.module

val configurationModule = module {
    single {
        ConfigLoader.getConfig()
    }
}
