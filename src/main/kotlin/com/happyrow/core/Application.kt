package com.happyrow.core

import com.happyrow.core.infrastructure.technical.auth.JwtAuthenticationPlugin
import com.happyrow.core.infrastructure.technical.auth.SupabaseJwtService
import com.happyrow.core.infrastructure.technical.config.DatabaseInitializer
import com.happyrow.core.infrastructure.technical.config.shutdownDataSource
import com.happyrow.core.infrastructure.technical.jackson.JsonObjectMapper
import com.happyrow.core.modules.domainModule
import com.happyrow.core.modules.infrastructure.authModule
import com.happyrow.core.modules.infrastructure.infrastructureModule
import com.happyrow.core.modules.internal.clockModule
import com.happyrow.core.modules.internal.configurationModule
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
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
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.resources.Resources
import io.ktor.server.response.respond
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
    modules(clockModule, configurationModule, authModule, infrastructureModule, domainModule)
  }

  // Initialize database schema for Render PostgreSQL
  val databaseInitializer by inject<DatabaseInitializer>()
  databaseInitializer.initializeDatabase()

  application()
  addShutdownHook()
}

fun Application.application() {
  configureCors()
  install(DoubleReceive)
  install(ContentNegotiation) {
    register(
      ContentType.Application.Json,
      JacksonConverter(JsonObjectMapper.defaultMapper),
    )
  }

  install(StatusPages) {
    exception<IllegalArgumentException> { call, cause ->
      call.respond(
        HttpStatusCode.BadRequest,
        mapOf("type" to "BAD_REQUEST", "detail" to (cause.message ?: "Invalid request")),
      )
    }
    exception<Throwable> { call, cause ->
      LoggerFactory.getLogger("StatusPages").error("Unhandled exception", cause)
      call.respond(
        HttpStatusCode.InternalServerError,
        mapOf("type" to "TECHNICAL_ERROR", "detail" to "An unexpected error occurred"),
      )
    }
  }

  val jwtService by inject<SupabaseJwtService>()
  install(JwtAuthenticationPlugin) {
    this.jwtService = jwtService
  }

  install(Resources)
  install(PartialContent)
  install(AutoHeadResponse)
  configureRouting()
}

private fun Application.configureCors() {
  install(CORS) {
    allowHost("localhost:3000")
    allowHost("localhost:3001")
    allowHost("localhost:4200")
    allowHost("localhost:5173")
    allowHost("localhost:8080")
    allowHost("localhost:8081")
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

    val allowedOrigins = System.getenv("ALLOWED_ORIGINS") ?: ""
    if (allowedOrigins.isNotEmpty()) {
      allowedOrigins.split(",").forEach { origin ->
        val cleanOrigin = origin.trim()
        if (cleanOrigin.isNotEmpty()) {
          val host = cleanOrigin.removePrefix("https://").removePrefix("http://")
          allowHost(host, schemes = listOf("http", "https"))
        }
      }
    }

    allowMethod(HttpMethod.Get)
    allowMethod(HttpMethod.Post)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Head)

    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.ContentType)
    allowHeader(HttpHeaders.Accept)
    allowHeader(HttpHeaders.Origin)

    allowCredentials = true
    allowNonSimpleContentTypes = true
  }
}

fun Application.addShutdownHook() {
  val dataSource by inject<DataSource>()

  monitor.subscribe(ApplicationStopPreparing) {
    runBlocking {
      shutdownDataSource(dataSource)
    }
  }
}
