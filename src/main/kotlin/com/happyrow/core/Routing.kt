package com.happyrow.core

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import java.sql.SQLException
import javax.sql.DataSource

private const val DB_CONNECTION_TIMEOUT_SECONDS = 5

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello from happyrow-core! 🎉", ContentType.Text.Plain)
        }

        get("/health") {
            val dataSource by inject<DataSource>()
            try {
                // Test database connection
                dataSource.connection.use { connection ->
                    val isValid = connection.isValid(DB_CONNECTION_TIMEOUT_SECONDS)
                    if (isValid) {
                        call.respond(
                            HttpStatusCode.OK,
                            mapOf(
                                "status" to "healthy",
                                "database" to "connected",
                                "timestamp" to System.currentTimeMillis()
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            mapOf(
                                "status" to "unhealthy",
                                "database" to "connection_invalid",
                                "timestamp" to System.currentTimeMillis()
                            )
                        )
                    }
                }
            } catch (e: SQLException) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf(
                        "status" to "unhealthy",
                        "database" to "connection_failed",
                        "error" to e.message,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
        }

        get("/info") {
            call.respond(
                mapOf(
                    "name" to "happyrow-core",
                    "version" to "1.0.0",
                    "environment" to (System.getenv("ENVIRONMENT") ?: "unknown"),
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
}
