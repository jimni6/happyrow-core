package com.happyrow.core.modules.internal

import org.koin.dsl.module
import java.time.Clock
import java.time.ZoneId

val clockModule = module {
    single<Clock> { Clock.system(ZoneId.systemDefault()) }
}
