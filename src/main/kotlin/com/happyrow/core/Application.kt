package com.happyrow.core

import com.happyrow.core.modules.infrastucture.infrastructureModule
import com.happyrow.core.infrastructure.technical.config.shutdownDataSource
import com.happyrow.core.infrastructure.technical.jackson.JsonObjectMapper
import com.happyrow.core.modules.internal.clockModule
import com.happyrow.core.modules.internal.configurationModule
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.resources.Resources
import kotlinx.coroutines.runBlocking
import org.koin.core.logger.Level
import org.koin.core.logger.PrintLogger
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  val logger = LoggerFactory.getLogger(Application::class.java)
  @Suppress("TooGenericExceptionCaught", "MagicNumber")
  try {
    EngineMain.main(args)
  } catch (throwable: Throwable) {
    logger.error("Unexpected error", throwable)
    exitProcess(42)
  }
}

fun Application.module() {
  install(Koin) {
    logger(PrintLogger(Level.DEBUG))
    modules(clockModule, configurationModule, infrastructureModule)
  }
  application()
  addShutdownHook()
}

fun Application.application() {
  install(DoubleReceive)
  install(ContentNegotiation) {
    register(
      ContentType.Application.Json,
      JacksonConverter(JsonObjectMapper.defaultMapper)
    )
  }
  install(Resources)
  install(PartialContent)
  install(AutoHeadResponse)
  configureRouting()
}

fun Application.addShutdownHook() {
  val dataSource by inject<DataSource>()

  monitor.subscribe(ApplicationStopPreparing) {
    runBlocking {
      shutdownDataSource(dataSource)
    }
  }
}
