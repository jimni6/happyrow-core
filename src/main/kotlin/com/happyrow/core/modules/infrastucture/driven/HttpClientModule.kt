package com.happyrow.core.modules.infrastucture.driven

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import org.koin.dsl.module

val httpClientModule = module {
  single<HttpClientEngine> { CIO.create() }
}
