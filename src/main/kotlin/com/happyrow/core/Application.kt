package com.happyrow.core

import com.happyrow.core.infrastructure.technical.config.DatabaseInitializer
import com.happyrow.core.infrastructure.technical.config.shutdownDataSource
import com.happyrow.core.infrastructure.technical.jackson.JsonObjectMapper
import com.happyrow.core.modules.domainModule
import com.happyrow.core.modules.infrastucture.infrastructureModule
import com.happyrow.core.modules.internal.clockModule
import com.happyrow.core.modules.internal.configurationModule
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
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
    modules(clockModule, configurationModule, infrastructureModule, domainModule)
  }

  // Initialize database schema for Render PostgreSQL
  val databaseInitializer by inject<DatabaseInitializer>()
  databaseInitializer.initializeDatabase()

  application()
  addShutdownHook()
}

fun Application.application() {
  install(CORS) {
    // Allow requests from common frontend development ports
    allowHost("localhost:3000") // React default
    allowHost("localhost:3001") // Alternative React port
    allowHost("localhost:4200") // Angular default
    allowHost("localhost:5173") // Vite default
    allowHost("localhost:8080") // Vue default
    allowHost("localhost:8081") // Alternative Vue port
    allowHost("127.0.0.1:3000")
    allowHost("127.0.0.1:3001")
    allowHost("127.0.0.1:4200")
    allowHost("127.0.0.1:5173")
    allowHost("127.0.0.1:8080")
    allowHost("127.0.0.1:8081")
    allowHost("jimni6.github.io")
    allowHost("happyrow-front-lyayzeci9-jimni6s-projects.vercel.app")
    allowHost("happyrow-front-git-main-jimni6s-projects.vercel.app")
    allowHost("happyrow-front.vercel.app")
    allowHost("happyrow-front-jimni6s-projects.vercel.app")

    // Allow common HTTP methods
    allowMethod(HttpMethod.Get)
    allowMethod(HttpMethod.Post)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Head)

    // Allow common headers
    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.ContentType)
    allowHeader(HttpHeaders.Accept)
    allowHeader(HttpHeaders.Origin)
    allowHeader("x-user-id")

    // Allow credentials (cookies, authorization headers)
    allowCredentials = true

    // Allow any header (more permissive, can be restricted later)
    allowNonSimpleContentTypes = true
  }
  install(DoubleReceive)
  install(ContentNegotiation) {
    register(
      ContentType.Application.Json,
      JacksonConverter(JsonObjectMapper.defaultMapper),
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
