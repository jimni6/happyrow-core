package com.happyrow.core.integration

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.happyrow.core.configureRouting
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.resource.common.driven.ResourceRepository
import com.happyrow.core.infrastructure.common.driven.event.SqlEventRepository
import com.happyrow.core.infrastructure.contribution.common.driven.ContributionTable
import com.happyrow.core.infrastructure.contribution.common.driven.SqlContributionRepository
import com.happyrow.core.infrastructure.event.common.driven.event.EventTable
import com.happyrow.core.infrastructure.participant.common.driven.ParticipantTable
import com.happyrow.core.infrastructure.participant.common.driven.SqlParticipantRepository
import com.happyrow.core.infrastructure.resource.common.driven.ResourceTable
import com.happyrow.core.infrastructure.resource.common.driven.SqlResourceRepository
import com.happyrow.core.infrastructure.technical.auth.JwtAuthenticationPlugin
import com.happyrow.core.infrastructure.technical.auth.SupabaseJwtConfig
import com.happyrow.core.infrastructure.technical.auth.SupabaseJwtService
import com.happyrow.core.infrastructure.technical.config.DatabaseInitializer
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import com.happyrow.core.infrastructure.technical.jackson.JsonObjectMapper
import com.happyrow.core.modules.domainModule
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.resources.Resources
import io.ktor.server.response.respond
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import javax.sql.DataSource
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

abstract class IntegrationTestBase {

  companion object {
    const val TEST_JWT_SECRET = "test-secret-key-for-integration-tests-minimum-256-bits-long"
    const val TEST_ISSUER = "http://localhost/auth/v1"
    const val TEST_AUDIENCE = "authenticated"
    const val TEST_USER_ID = "test-user-id-123"
    const val TEST_USER_EMAIL = "test@happyrow.com"
    const val SECOND_USER_EMAIL = "other@happyrow.com"

    private val algorithm = Algorithm.HMAC256(TEST_JWT_SECRET)

    private val sharedDataSource: DataSource by lazy {
      HikariDataSource(
        HikariConfig().apply {
          jdbcUrl = System.getenv("TEST_DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/happyrow_test"
          username = System.getenv("TEST_DB_USERNAME") ?: "postgres"
          password = System.getenv("TEST_DB_PASSWORD") ?: "postgres"
          driverClassName = "org.postgresql.Driver"
          maximumPoolSize = 5
          isAutoCommit = false
          addDataSourceProperty("sslmode", "disable")
          validate()
        },
      )
    }

    private val sharedDatabase: Database by lazy { Database.connect(sharedDataSource) }

    private val dbInitialized: Boolean by lazy {
      val initializer = DatabaseInitializer(ExposedDatabase(sharedDataSource))
      initializer.initializeDatabase()
      true
    }

    fun generateToken(userId: String = TEST_USER_ID, email: String = TEST_USER_EMAIL): String = JWT.create()
      .withIssuer(TEST_ISSUER)
      .withAudience(TEST_AUDIENCE)
      .withSubject(userId)
      .withClaim("email", email)
      .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
      .sign(algorithm)

    fun generateExpiredToken(userId: String = TEST_USER_ID, email: String = TEST_USER_EMAIL): String = JWT.create()
      .withIssuer(TEST_ISSUER)
      .withAudience(TEST_AUDIENCE)
      .withSubject(userId)
      .withClaim("email", email)
      .withExpiresAt(Date.from(Instant.now().minusSeconds(3600)))
      .sign(algorithm)
  }

  @BeforeEach
  fun setUp() {
    try {
      stopKoin()
    } catch (_: Exception) { }

    dbInitialized

    transaction(sharedDatabase) {
      ContributionTable.deleteAll()
      ResourceTable.deleteAll()
      ParticipantTable.deleteAll()
      EventTable.deleteAll()
    }
  }

  fun integrationTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
    application {
      val testJwtService = SupabaseJwtService(
        SupabaseJwtConfig(
          jwtSecret = TEST_JWT_SECRET,
          issuer = TEST_ISSUER,
          audience = TEST_AUDIENCE,
        ),
      )

      val testModule = module {
        single<Clock> { Clock.system(ZoneId.systemDefault()) }
        single<DataSource> { sharedDataSource }
        single { ExposedDatabase(get()) }
        single { DatabaseInitializer(get()) }
        singleOf(::SqlEventRepository) bind EventRepository::class
        singleOf(::SqlParticipantRepository) bind ParticipantRepository::class
        singleOf(::SqlResourceRepository) bind ResourceRepository::class
        singleOf(::SqlContributionRepository) bind ContributionRepository::class
      }

      install(Koin) {
        modules(testModule, domainModule)
      }

      install(DoubleReceive)
      install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(JsonObjectMapper.defaultMapper))
      }
      install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
          call.respond(
            HttpStatusCode.BadRequest,
            mapOf("type" to "BAD_REQUEST", "detail" to (cause.message ?: "Invalid request")),
          )
        }
        exception<Throwable> { call, cause ->
          call.respond(
            HttpStatusCode.InternalServerError,
            mapOf("type" to "TECHNICAL_ERROR", "detail" to "An unexpected error occurred"),
          )
        }
      }
      install(JwtAuthenticationPlugin) {
        this.jwtService = testJwtService
      }
      install(Resources)
      configureRouting()
    }

    block()
  }

  fun ApplicationTestBuilder.authenticatedClient(): HttpClient = createClient {
    install(ClientContentNegotiation) {
      register(ContentType.Application.Json, JacksonConverter(JsonObjectMapper.defaultMapper))
    }
  }

  fun ApplicationTestBuilder.rawClient(): HttpClient = createClient {}

  fun authHeader(token: String = generateToken()) = "Bearer $token"
}
