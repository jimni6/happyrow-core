package com.happyrow.core

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import javax.sql.DataSource

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
                    val isValid = connection.isValid(5) // 5 second timeout
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
            } catch (e: Exception) {
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
