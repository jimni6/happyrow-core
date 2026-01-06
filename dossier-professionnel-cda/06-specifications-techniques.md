# 6. SPÉCIFICATIONS TECHNIQUES

## 6.1 Environnement de développement

### 6.1.1 Configuration du poste de développement

#### Outils et versions

| Outil | Version | Rôle |
|-------|---------|------|
| **Système d'exploitation** | macOS / Linux / Windows | Environnement de développement |
| **JDK** | Eclipse Temurin 21 | Runtime Java/Kotlin |
| **Kotlin** | 2.2.0 | Langage de programmation principal |
| **Gradle** | 8.x (wrapper) | Gestionnaire de build et dépendances |
| **IntelliJ IDEA** | 2024.x Ultimate | IDE de développement |
| **Git** | 2.x | Contrôle de version |
| **Docker** | 24+ | Conteneurisation |
| **Docker Compose** | 2.x | Orchestration locale |
| **PostgreSQL** | 15+ | Base de données de développement |

#### IDE et extensions

**IntelliJ IDEA Ultimate** avec les plugins suivants :
- **Kotlin** : Support natif du langage
- **Ktor** : Assistance pour le framework
- **Database Tools** : Gestion de la base PostgreSQL
- **Docker** : Intégration Docker
- **GitToolBox** : Outils Git avancés
- **Rainbow Brackets** : Lisibilité du code
- **Detekt** : Analyse statique de code Kotlin

### 6.1.2 Structure du projet Gradle

Le projet utilise **Gradle 8** avec le Kotlin DSL et une architecture multi-modules :

```
happyrow-core/
├── build.gradle.kts           # Configuration racine
├── settings.gradle.kts         # Paramètres multi-modules
├── gradle.properties           # Propriétés globales
├── gradle/
│   ├── libs.versions.toml     # Catalogue de versions centralisé
│   └── wrapper/               # Gradle wrapper
├── src/                       # Module application
├── domain/                    # Module métier
│   └── build.gradle.kts
└── infrastructure/            # Module infrastructure
    └── build.gradle.kts
```

#### Catalogue de versions centralisé (libs.versions.toml)

Toutes les dépendances et leurs versions sont centralisées dans un fichier TOML :

```toml
[versions]
kotlin = "2.2.0"
ktor = "3.2.2"
exposed = "0.61.0"
postgres = "42.7.7"
koin = "4.1.0"
kotest = "5.9.1"
arrow = "2.1.2"
detekt = "1.23.7"
hikari = "6.3.1"
jackson = "2.19.2"
logback = "1.5.18"

[libraries]
# Framework Ktor
ktor-server-core = { group = "io.ktor", name = "ktor-server-core", version.ref = "ktor" }
ktor-server-netty = { group = "io.ktor", name = "ktor-server-netty", version.ref = "ktor" }
ktor-server-cors = { group = "io.ktor", name = "ktor-server-cors", version.ref = "ktor" }
ktor-serialization-jackson = { group = "io.ktor", name = "ktor-serialization-jackson", version.ref = "ktor" }

# ORM Exposed
exposed-core = { group = "org.jetbrains.exposed", name = "exposed-core", version.ref = "exposed" }
exposed-jdbc = { group = "org.jetbrains.exposed", name = "exposed-jdbc", version.ref = "exposed" }
exposed-java-time = { group = "org.jetbrains.exposed", name = "exposed-java-time", version.ref = "exposed" }

# Database
postgresql = { group = "org.postgresql", name = "postgresql", version.ref = "postgres" }
hikariCP = { group = "com.zaxxer", name = "HikariCP", version.ref = "hikari" }

# Injection de dépendances
koin-ktor = { group = "io.insert-koin", name = "koin-ktor", version.ref = "koin" }

# Programmation fonctionnelle
arrow-core = { group = "io.arrow-kt", name = "arrow-core", version.ref = "arrow" }

# Tests
kotest-runner-junit5 = { group = "io.kotest", name = "kotest-runner-junit5", version.ref = "kotest" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }

[bundles]
ktor-server = ["ktor-server-core", "ktor-server-netty", "ktor-server-cors"]
exposed = ["exposed-core", "exposed-jdbc", "exposed-java-time"]
```

**Avantages :**
- Versions centralisées et cohérentes sur tous les modules
- Mise à jour simplifiée des dépendances
- Évite les conflits de versions
- Facilite la maintenance

### 6.1.3 Configuration Docker pour le développement local

#### docker-compose.yml

Environnement de développement complet avec PostgreSQL :

```yaml
version: '3.8'

services:
  happyrow-core:
    build: .
    container_name: happyrow-app
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=${DATABASE_URL}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - DB_SSL_MODE=${DB_SSL_MODE:-require}
      - DB_MAX_POOL_SIZE=${DB_MAX_POOL_SIZE:-10}
      - PORT=8080
      - ENVIRONMENT=${ENVIRONMENT:-development}
    env_file:
      - .env
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    restart: unless-stopped
    networks:
      - happyrow-network

networks:
  happyrow-network:
    driver: bridge
```

#### Dockerfile multi-stage

Build optimisé en deux étapes (build + runtime) :

```dockerfile
# Stage 1: Build
FROM gradle:8-jdk21 AS build

WORKDIR /app
COPY . .

# Build avec toutes les optimisations
RUN ./gradlew clean build --no-daemon --stacktrace

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

USER 1000:1000
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/build/libs/*.jar happyrow-core.jar
COPY --from=build /app/src/main/resources/application.conf /app/application.conf

EXPOSE 8080

# JVM optimisations
ENTRYPOINT ["java"]
CMD ["-Xmx512m", "-Xms256m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", 
     "-Djava.security.egd=file:/dev/./urandom", "-jar", "happyrow-core.jar"]
```

**Caractéristiques :**
- **Multi-stage build** : Sépare la compilation du runtime pour réduire la taille de l'image
- **Image JRE légère** : Eclipse Temurin JRE 21 (sans JDK, plus petit)
- **Utilisateur non-root** : Sécurité renforcée (UID 1000)
- **Optimisations JVM** : Heap limitée, G1GC pour faible latence
- **Image finale** : ~200 MB (vs ~500 MB avec le JDK complet)

### 6.1.4 Configuration de la base de données

#### Application.conf (Typesafe Config)

Configuration centralisée pour tous les environnements :

```hocon
application {
  sql {
    url = ${?DATABASE_URL}
    url = "jdbc:postgresql://localhost:5432/happyrow_db" # Default local
    
    username = ${?DB_USERNAME}
    username = "happyrow_user" # Default
    
    password = ${?DB_PASSWORD}
    password = "secret" # Default
    
    driver = "org.postgresql.Driver"
    
    maxPoolSize = ${?DB_MAX_POOL_SIZE}
    maxPoolSize = 10
    
    connectionTimeout = ${?DB_CONNECTION_TIMEOUT}
    connectionTimeout = 30000
    
    idleTimeout = ${?DB_IDLE_TIMEOUT}
    idleTimeout = 600000
    
    maxLifetime = ${?DB_MAX_LIFETIME}
    maxLifetime = 1800000
    
    sslMode = ${?DB_SSL_MODE}
    sslMode = "require"
  }
}
```

**Hiérarchie de configuration :**
1. Variables d'environnement (priorité haute)
2. Fichier `.env` (local)
3. Valeurs par défaut (fallback)

#### HikariCP Connection Pool

Configuration du pool de connexions pour des performances optimales :

```kotlin
data class SqlDatabaseConfig(
  val url: String,
  val username: String,
  val password: String,
  val driver: String = "org.postgresql.Driver",
  val maxPoolSize: Int = 10,
  val connectionTimeout: Long = 30000,
  val idleTimeout: Long = 600000,
  val maxLifetime: Long = 1800000,
  val sslMode: String = "require"
)

fun createDataSource(config: SqlDatabaseConfig): DataSource {
  val hikariConfig = HikariConfig().apply {
    jdbcUrl = config.url
    username = config.username
    password = config.password
    driverClassName = config.driver
    maximumPoolSize = config.maxPoolSize
    connectionTimeout = config.connectionTimeout
    idleTimeout = config.idleTimeout
    maxLifetime = config.maxLifetime
    
    // SSL pour Render PostgreSQL
    addDataSourceProperty("sslmode", config.sslMode)
    
    // Validation des connexions
    isAutoCommit = false
    validate()
  }
  
  return HikariDataSource(hikariConfig)
}
```

**Paramètres clés :**
- **maximumPoolSize = 10** : Limite les connexions (adapter selon la charge)
- **connectionTimeout = 30s** : Timeout d'acquisition d'une connexion
- **idleTimeout = 10min** : Fermeture des connexions inactives
- **maxLifetime = 30min** : Durée de vie maximale d'une connexion
- **autoCommit = false** : Gestion manuelle des transactions

### 6.1.5 Outils de qualité de code

#### Detekt - Analyse statique

Configuration `.detekt.yml` :

```yaml
build:
  maxIssues: 0
  excludeCorrectable: false

config:
  validation: true
  checkExhaustiveness: true

complexity:
  active: true
  CyclomaticComplexMethod:
    threshold: 15
  LongMethod:
    threshold: 60
  TooManyFunctions:
    threshold: 20

naming:
  active: true
  VariableNaming:
    variablePattern: '[a-z][A-Za-z0-9]*'
  
style:
  active: true
  MaxLineLength:
    maxLineLength: 120
```

**Rapports générés :**
- HTML (navigable)
- XML (CI/CD)
- TXT (console)
- SARIF (GitHub Security)
- Markdown (documentation)

**Commandes Gradle :**
```bash
./gradlew detekt              # Analyse
./gradlew detektBaseline      # Créer baseline
```

#### Spotless - Formatage automatique

Configuration dans `build.gradle.kts` :

```kotlin
spotless {
  kotlin {
    ktlint().editorConfigOverride(mapOf(
      "ktlint_standard_property-naming" to "disabled",
      "ktlint_standard_value-argument-comment" to "disabled"
    ))
    target("src/**/*.kt", "domain/src/**/*.kt", "infrastructure/src/**/*.kt")
    trimTrailingWhitespace()
    endWithNewline()
  }
}
```

**Commandes Gradle :**
```bash
./gradlew spotlessCheck       # Vérifier formatage
./gradlew spotlessApply       # Appliquer formatage
```

### 6.1.6 Gestion des versions avec Git

#### Stratégie de branches

```
main (production)
  ↑
  └── Pull Requests (reviews obligatoires)
        ↑
        ├── feature/xxx (nouvelles fonctionnalités)
        ├── fix/xxx (corrections de bugs)
        └── refactor/xxx (refactoring)
```

**Convention de nommage des branches :**
- `feature/add-event-validation` : Nouvelles fonctionnalités
- `fix/cors-headers-issue` : Corrections de bugs
- `refactor/repository-pattern` : Refactoring
- `chore/update-dependencies` : Maintenance

#### Convention de commits (Conventional Commits)

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Exemples :**
```
feat(event): add event creation endpoint
fix(cors): allow localhost:3000 origin
refactor(repository): use Either for error handling
docs(readme): update deployment instructions
test(event): add integration tests for event creation
```

**Types standards :**
- `feat` : Nouvelle fonctionnalité
- `fix` : Correction de bug
- `refactor` : Refactoring (sans changement fonctionnel)
- `docs` : Documentation
- `test` : Ajout/modification de tests
- `chore` : Tâches de maintenance

#### Fichier .gitignore

```gitignore
# Build outputs
build/
out/
*.jar
*.war

# IDE
.idea/
*.iml
.vscode/

# Gradle
.gradle/
gradle-app.setting

# Environment variables
.env
*.env.local

# OS
.DS_Store
Thumbs.db

# Logs
*.log
logs/
```

---

## 6.2 Composants développés

### 6.2.1 Architecture des packages

```
com.happyrow.core/
│
├── Application.kt                    # Point d'entrée Ktor
├── Routing.kt                        # Configuration des routes
├── AppConfig.kt                      # Configuration application
│
├── modules/                          # Modules Koin
│   ├── UseCaseModule.kt             # Use Cases du domain
│   ├── internal/
│   │   ├── ClockModule.kt           # Gestion du temps
│   │   └── ConfigurationModule.kt   # Configuration
│   └── infrastucture/
│       ├── InfrastructureModule.kt  # Module principal infrastructure
│       └── driven/
│           ├── PostgreSQLModule.kt  # Configuration BDD
│           └── HttpClientModule.kt  # Client HTTP
│
└── domain/                           # Logique métier (module séparé)
    ├── event/
    │   ├── common/
    │   │   ├── model/               # Entités Event, EventType
    │   │   ├── driven/              # Port EventRepository
    │   │   └── error/               # Exceptions métier
    │   ├── create/                  # Use Case création
    │   ├── update/                  # Use Case modification
    │   ├── delete/                  # Use Case suppression
    │   └── get/                     # Use Case consultation
    ├── participant/
    ├── resource/
    └── contribution/
│
└── infrastructure/                   # Adaptateurs (module séparé)
    ├── event/
    │   ├── EventEndpoints.kt        # Agrégation endpoints
    │   ├── common/
    │   │   ├── dto/                 # DTOs
    │   │   ├── driven/              # Implémentation Repository
    │   │   └── error/               # Exceptions techniques
    │   ├── create/
    │   │   └── driving/             # Endpoint POST /events
    │   ├── update/
    │   │   └── driving/             # Endpoint PUT /events/{id}
    │   ├── delete/
    │   │   └── driving/             # Endpoint DELETE /events/{id}
    │   └── get/
    │       └── driving/             # Endpoint GET /events
    ├── participant/
    ├── resource/
    ├── contribution/
    └── technical/
        ├── config/                  # Configuration DB, initialisation
        ├── exposed/                 # Extensions Exposed
        └── jackson/                 # Sérialisation JSON
```

**Organisation par feature :**
Chaque agrégat (Event, Participant, Resource, Contribution) contient toute sa logique du domain et ses adaptateurs.

### 6.2.2 Couche Domain - Logique métier

#### Entité Event

```kotlin
package com.happyrow.core.domain.event.common.model.event

data class Event(
  val identifier: UUID,
  val name: String,
  val description: String,
  val eventDate: Instant,
  val creationDate: Instant,
  val updateDate: Instant,
  val creator: Creator,
  val location: String,
  val type: EventType,
  val members: List<Creator> = listOf()
)
```

**Caractéristiques :**
- **Immutable** : `data class` Kotlin sans mutabilité
- **Type-safe** : UUID pour les identifiants, Instant pour les dates
- **Encapsulation** : Validation dans les Use Cases
- **Sans dépendances** : Pur domaine, aucune référence à l'infrastructure

#### Enum EventType

```kotlin
enum class EventType {
  PARTY,
  BIRTHDAY,
  DINER,
  SNACK
}
```

#### Value Object Creator

```kotlin
data class Creator(
  val identifier: String  // Email de l'utilisateur
)
```

#### Port EventRepository

```kotlin
package com.happyrow.core.domain.event.common.driven.event

interface EventRepository {
  suspend fun create(event: Event): Either<CreateEventRepositoryException, Event>
  suspend fun findById(id: UUID): Either<EventNotFoundException, Event>
  suspend fun findByOrganizer(organizerId: String): Either<GetEventException, List<Event>>
  suspend fun update(event: Event): Either<UpdateEventRepositoryException, Event>
  suspend fun delete(id: UUID): Either<DeleteEventRepositoryException, Unit>
}
```

**Caractéristiques :**
- **Interface pure** : Définit le contrat sans implémentation
- **Programmation fonctionnelle** : `Either<Error, Success>` pour la gestion d'erreurs
- **Coroutines Kotlin** : `suspend` pour l'asynchrone
- **Indépendance** : Aucune dépendance vers Exposed, PostgreSQL ou autre technologie

#### Use Case - CreateEventUseCase

```kotlin
package com.happyrow.core.domain.event.create

class CreateEventUseCase(
  private val eventRepository: EventRepository,
  private val clock: Clock
) {
  suspend fun execute(request: CreateEventRequest): Either<CreateEventException, Event> {
    // Validation métier
    if (request.eventDate.isBefore(clock.instant())) {
      return Either.Left(CreateEventException("Event date must be in the future"))
    }
    
    // Création de l'entité
    val event = Event(
      identifier = UUID.randomUUID(),
      name = request.name,
      description = request.description,
      eventDate = request.eventDate,
      creationDate = clock.instant(),
      updateDate = clock.instant(),
      creator = Creator(request.creator),
      location = request.location,
      type = request.type,
      members = emptyList()
    )
    
    // Persistance via le port
    return eventRepository.create(event)
      .mapLeft { CreateEventException("Failed to create event: ${it.message}") }
  }
}
```

**Points clés :**
1. **Validation métier centralisée** : Date future obligatoire
2. **Injection de dépendances** : Repository et Clock injectés
3. **Gestion d'erreurs fonctionnelle** : `Either<Error, Success>`
4. **Pas de logique d'infrastructure** : Uniquement la logique métier
5. **Testabilité** : Facile à tester avec des mocks

### 6.2.3 Couche Infrastructure - Adaptateurs

#### Adaptateur driving - CreateEventEndpoint

```kotlin
package com.happyrow.core.infrastructure.event.create.driving

fun Route.createEventEndpoint(createEventUseCase: CreateEventUseCase) {
  post("/events") {
    // 1. Récupération et validation du DTO
    val dto = call.receive<CreateEventRequestDto>()
    
    // 2. Conversion DTO → Request Domain
    val request = dto.toCreateEventRequest()
    
    // 3. Exécution du Use Case
    when (val result = createEventUseCase.execute(request)) {
      is Either.Right -> {
        // Succès : conversion Event → DTO et réponse 201
        call.respond(HttpStatusCode.Created, result.value.toDto())
      }
      is Either.Left -> {
        // Erreur métier : réponse 400 ou 409
        when (result.value) {
          is UnicityConflictException -> 
            call.respond(HttpStatusCode.Conflict, mapOf("error" to result.value.message))
          else -> 
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.value.message))
        }
      }
    }
  }
}
```

**Responsabilités :**
1. **Réception HTTP** : Parse le JSON en DTO
2. **Validation DTO** : Validation des types et formats
3. **Conversion** : DTO → Objet Domain
4. **Orchestration** : Appel du Use Case
5. **Gestion des erreurs** : Mapping erreurs métier → codes HTTP
6. **Réponse HTTP** : Sérialisation du résultat en JSON

#### DTO - CreateEventRequestDto

```kotlin
data class CreateEventRequestDto(
  val name: String,
  val description: String,
  val eventDate: String,        // ISO-8601 format
  val creator: String,
  val location: String,
  val type: String
) {
  fun toCreateEventRequest(): CreateEventRequest {
    return CreateEventRequest(
      name = name,
      description = description,
      eventDate = Instant.parse(eventDate),
      creator = creator,
      location = location,
      type = type.toEventType().getOrElse { throw BadRequestException("Invalid event type") }
    )
  }
}
```

**Points clés :**
- **Séparation DTO/Domain** : Le DTO reste dans l'infrastructure
- **Validation et conversion** : Parsing de la date ISO-8601, conversion enum
- **Gestion d'erreurs** : Exceptions techniques si format invalide

#### Adaptateur driven - SqlEventRepository

```kotlin
package com.happyrow.core.infrastructure.common.driven.event

class SqlEventRepository(
  private val exposedDatabase: ExposedDatabase
) : EventRepository {
  
  override suspend fun create(event: Event): Either<CreateEventRepositoryException, Event> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        EventTable.insert {
          it[id] = event.identifier
          it[name] = event.name
          it[description] = event.description
          it[eventDate] = event.eventDate
          it[creator] = event.creator.identifier
          it[location] = event.location
          it[type] = event.type
          it[creationDate] = event.creationDate
          it[updateDate] = event.updateDate
          it[members] = event.members.map { member -> UUID.fromString(member.identifier) }
        }
        event
      }
    }.mapLeft { CreateEventRepositoryException("Database error: ${it.message}", it) }
  }
  
  override suspend fun findByOrganizer(organizerId: String): Either<GetEventException, List<Event>> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        EventTable
          .select { EventTable.creator eq organizerId }
          .map { it.toEvent().getOrElse { error -> throw error } }
      }
    }.mapLeft { GetEventException("Failed to fetch events: ${it.message}", it) }
  }
}
```

**Caractéristiques :**
1. **Implémentation du port** : Respecte l'interface `EventRepository`
2. **ORM Exposed** : DSL type-safe pour SQL
3. **Transactions** : Gestion automatique ACID
4. **Gestion d'erreurs** : `Either.catch` pour capturer les exceptions SQL
5. **Mapping** : ResultRow → Event via fonction d'extension

#### Table Exposed - EventTable

```kotlin
object EventTable : UUIDTable("configuration.event", "identifier") {
  val name: Column<String> = varchar("name", 256)
  val description: Column<String> = varchar("description", 256)
  val eventDate = timestamp("event_date")
  val creator = varchar("creator", 256)
  val location: Column<String> = varchar("location", 256)
  val type: Column<EventType> = customEnumeration(
    name = "type",
    sql = "EVENT_TYPE",
    fromDb = { value -> (value as String).toEventType().getOrElse { throw it } },
    toDb = { PGEnum("EVENT_TYPE", it) }
  )
  val creationDate = timestamp("creation_date")
  val updateDate = timestamp("update_date")
  val members = array("members", UUIDColumnType(), UUID::class) { 
    UUID.fromString(it.toString()) 
  }
}
```

**Points clés :**
- **Type-safe DSL** : Définition de la table en Kotlin
- **Mapping enum PostgreSQL** : Custom enumeration pour EVENT_TYPE
- **Type array** : Support natif des arrays PostgreSQL
- **Schema qualifié** : `configuration.event`

### 6.2.4 Injection de dépendances avec Koin

#### Module Domain

```kotlin
val domainModule = module {
  // Use Cases Event
  single { CreateEventUseCase(get(), get()) }
  single { GetEventsByOrganizerUseCase(get()) }
  single { UpdateEventUseCase(get(), get()) }
  single { DeleteEventUseCase(get()) }
  
  // Use Cases Participant
  single { CreateParticipantUseCase(get(), get()) }
  single { GetParticipantsByEventUseCase(get()) }
  single { UpdateParticipantUseCase(get(), get()) }
  
  // Use Cases Resource
  single { CreateResourceUseCase(get(), get()) }
  single { GetResourcesByEventUseCase(get()) }
  
  // Use Cases Contribution
  single { AddContributionUseCase(get(), get()) }
  single { DeleteContributionUseCase(get()) }
}
```

#### Module Infrastructure

```kotlin
val infrastructureModule = module {
  include(postgresqlModule)
  include(httpClientModule)
}

val postgresqlModule = module {
  // Configuration
  single {
    val appConfig = get<AppConfig>()
    appConfig.sql
  }
  
  // DataSource HikariCP
  single {
    val sqlConfig = get<SqlDatabaseConfig>()
    dataSource(sqlConfig)
  }
  
  // Exposed Database
  singleOf(::ExposedDatabase)
  
  // Initialisation DB
  singleOf(::DatabaseInitializer)
  
  // Repositories (binding interface → implementation)
  singleOf(::SqlEventRepository) bind EventRepository::class
  singleOf(::SqlParticipantRepository) bind ParticipantRepository::class
  singleOf(::SqlResourceRepository) bind ResourceRepository::class
  singleOf(::SqlContributionRepository) bind ContributionRepository::class
}
```

**Avantages :**
- **Légèreté** : Koin est un framework DI léger, sans génération de code
- **DSL Kotlin** : Configuration intuitive
- **Binding** : Association interface → implémentation
- **Scopes** : Singleton, Factory, Scoped
- **Testabilité** : Facilité de remplacement par des mocks

### 6.2.5 Configuration de l'application Ktor

#### Application.kt - Point d'entrée

```kotlin
fun Application.module() {
  // 1. Installation de Koin
  install(Koin) {
    logger(PrintLogger(Level.DEBUG))
    modules(clockModule, configurationModule, infrastructureModule, domainModule)
  }
  
  // 2. Initialisation de la base de données
  val databaseInitializer by inject<DatabaseInitializer>()
  databaseInitializer.initializeDatabase()
  
  // 3. Configuration de l'application
  application()
  
  // 4. Hook de fermeture propre
  addShutdownHook()
}

fun Application.application() {
  // Installation CORS
  install(CORS) {
    allowHost("localhost:3000")
    allowHost("happyrow-front.vercel.app")
    
    // Origines dynamiques depuis variable d'environnement
    val allowedOrigins = System.getenv("ALLOWED_ORIGINS") ?: ""
    if (allowedOrigins.isNotEmpty()) {
      allowedOrigins.split(",").forEach { origin ->
        val host = origin.trim().removePrefix("https://").removePrefix("http://")
        allowHost(host, schemes = listOf("http", "https"))
      }
    }
    
    allowMethod(HttpMethod.Get)
    allowMethod(HttpMethod.Post)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)
    
    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.ContentType)
    allowHeader("x-user-id")
    
    allowCredentials = true
    allowNonSimpleContentTypes = true
  }
  
  // Content Negotiation (JSON)
  install(ContentNegotiation) {
    register(ContentType.Application.Json, JacksonConverter(JsonObjectMapper.defaultMapper))
  }
  
  // Autres plugins
  install(DoubleReceive)
  install(Resources)
  install(PartialContent)
  install(AutoHeadResponse)
  
  // Configuration des routes
  configureRouting()
}
```

**Plugins installés :**
- **Koin** : Injection de dépendances
- **CORS** : Gestion des origines cross-domain
- **ContentNegotiation** : Sérialisation JSON (Jackson)
- **DoubleReceive** : Lecture multiple du body
- **Resources** : Type-safe routing
- **PartialContent** : Support HTTP 206 (Range)
- **AutoHeadResponse** : Génération auto des HEAD

#### Routing.kt - Configuration des routes

```kotlin
fun Application.configureRouting() {
  // Injection des Use Cases
  val createEventUseCase: CreateEventUseCase by inject()
  val getEventsByOrganizerUseCase: GetEventsByOrganizerUseCase by inject()
  val updateEventUseCase: UpdateEventUseCase by inject()
  val deleteEventUseCase: DeleteEventUseCase by inject()
  // ... autres Use Cases
  
  routing {
    route("/event/configuration/api/v1") {
      // Endpoints Event
      eventEndpoints(createEventUseCase, getEventsByOrganizerUseCase, 
                     updateEventUseCase, deleteEventUseCase)
      
      // Endpoints Participant
      participantEndpoints(createParticipantUseCase, getParticipantsByEventUseCase, 
                           updateParticipantUseCase)
      
      // Endpoints Resource
      resourceEndpoints(createResourceUseCase, getResourcesByEventUseCase)
      
      // Endpoints Contribution
      contributionEndpoints(addContributionUseCase, deleteContributionUseCase)
    }
    
    // Endpoints utilitaires
    get("/") {
      call.respondText("Hello from happyrow-core! 🎉", ContentType.Text.Plain)
    }
    
    get("/info") {
      call.respond(mapOf(
        "name" to "happyrow-core",
        "version" to "1.0.0",
        "environment" to (System.getenv("ENVIRONMENT") ?: "unknown"),
        "timestamp" to System.currentTimeMillis()
      ))
    }
  }
}
```

---

## 6.3 Stratégie de sécurité

### 6.3.1 Sécurité de la couche Présentation

#### Configuration CORS stricte

**Problématique :**
Les navigateurs bloquent les requêtes cross-origin par défaut (Same-Origin Policy). Il faut configurer CORS de manière sécurisée.

**Solution implémentée :**

```kotlin
install(CORS) {
  // 1. Liste blanche d'hôtes autorisés (pas de wildcard *)
  allowHost("localhost:3000")        // Dev React
  allowHost("localhost:5173")        // Dev Vite
  allowHost("happyrow-front.vercel.app")  // Production
  
  // 2. Origines dynamiques depuis variable d'environnement
  val allowedOrigins = System.getenv("ALLOWED_ORIGINS") ?: ""
  if (allowedOrigins.isNotEmpty()) {
    allowedOrigins.split(",").forEach { origin ->
      val host = origin.trim().removePrefix("https://").removePrefix("http://")
      allowHost(host, schemes = listOf("http", "https"))
    }
  }
  
  // 3. Méthodes HTTP autorisées uniquement
  allowMethod(HttpMethod.Get)
  allowMethod(HttpMethod.Post)
  allowMethod(HttpMethod.Put)
  allowMethod(HttpMethod.Delete)
  // PAS de TRACE, CONNECT, etc.
  
  // 4. Headers autorisés explicitement
  allowHeader(HttpHeaders.Authorization)
  allowHeader(HttpHeaders.ContentType)
  allowHeader(HttpHeaders.Accept)
  allowHeader("x-user-id")
  
  // 5. Credentials autorisés (cookies, auth headers)
  allowCredentials = true
}
```

**Bonnes pratiques appliquées :**
- ❌ **Pas de wildcard `*`** : Liste blanche explicite
- ✅ **Configuration dynamique** : Variable d'environnement pour ajouter des origines sans redéployer
- ✅ **HTTPS en production** : Schemes HTTP/HTTPS selon l'environnement
- ✅ **Headers limités** : Seulement ceux nécessaires
- ✅ **Credentials sécurisés** : Permet l'authentification mais avec origines restreintes

#### Validation des entrées utilisateur

**Principe :** Ne jamais faire confiance aux données entrantes.

**Couches de validation :**

1. **Validation DTO (format)** :
```kotlin
data class CreateEventRequestDto(
  val name: String,
  val description: String,
  val eventDate: String,  // Doit être ISO-8601
  val creator: String,    // Doit être un email valide
  val location: String,
  val type: String        // Doit être un EventType valide
) {
  fun toCreateEventRequest(): CreateEventRequest {
    // Validation format date
    val parsedDate = try {
      Instant.parse(eventDate)
    } catch (e: Exception) {
      throw BadRequestException("Invalid date format, expected ISO-8601")
    }
    
    // Validation enum
    val parsedType = type.toEventType()
      .getOrElse { throw BadRequestException("Invalid event type: $type") }
    
    // Validation email (basique)
    if (!creator.contains("@")) {
      throw BadRequestException("Invalid email format")
    }
    
    return CreateEventRequest(
      name = name.trim(),
      description = description.trim(),
      eventDate = parsedDate,
      creator = creator.trim(),
      location = location.trim(),
      type = parsedType
    )
  }
}
```

2. **Validation métier (Use Case)** :
```kotlin
class CreateEventUseCase(
  private val eventRepository: EventRepository,
  private val clock: Clock
) {
  suspend fun execute(request: CreateEventRequest): Either<CreateEventException, Event> {
    // Validation métier : date dans le futur
    if (request.eventDate.isBefore(clock.instant())) {
      return Either.Left(CreateEventException("Event date must be in the future"))
    }
    
    // Validation : nom non vide
    if (request.name.isBlank()) {
      return Either.Left(CreateEventException("Event name cannot be empty"))
    }
    
    // Validation : description non vide
    if (request.description.isBlank()) {
      return Either.Left(CreateEventException("Event description cannot be empty"))
    }
    
    // ... création de l'événement
  }
}
```

**Avantages :**
- **Défense en profondeur** : 2 couches de validation
- **Séparation des responsabilités** : Format (infra) vs Règles métier (domain)
- **Pas de données polluées** : Trim des espaces, validation stricte

#### Gestion sécurisée des erreurs

**Problématique :** Les exceptions peuvent révéler des informations sensibles (chemins de fichiers, structure DB, stack traces).

**Solution :**

```kotlin
fun Route.createEventEndpoint(createEventUseCase: CreateEventUseCase) {
  post("/events") {
    try {
      val dto = call.receive<CreateEventRequestDto>()
      val request = dto.toCreateEventRequest()
      
      when (val result = createEventUseCase.execute(request)) {
        is Either.Right -> {
          call.respond(HttpStatusCode.Created, result.value.toDto())
        }
        is Either.Left -> {
          // Erreurs métier uniquement (pas de détails techniques)
          when (result.value) {
            is UnicityConflictException -> 
              call.respond(HttpStatusCode.Conflict, ErrorResponse(
                error = "Conflict",
                message = "An event with this name already exists"
              ))
            else -> 
              call.respond(HttpStatusCode.BadRequest, ErrorResponse(
                error = "Bad Request",
                message = result.value.message ?: "Invalid request"
              ))
          }
        }
      }
    } catch (e: BadRequestException) {
      // Erreurs de validation DTO
      call.respond(HttpStatusCode.BadRequest, ErrorResponse(
        error = "Validation Error",
        message = e.message ?: "Invalid input"
      ))
    } catch (e: Exception) {
      // Erreurs inattendues : log serveur, message générique client
      logger.error("Unexpected error", e)
      call.respond(HttpStatusCode.InternalServerError, ErrorResponse(
        error = "Internal Server Error",
        message = "An unexpected error occurred"
      ))
    }
  }
}
```

**Principes :**
- ❌ **Pas de stack traces** dans les réponses HTTP
- ✅ **Messages génériques** pour les erreurs inattendues
- ✅ **Logging serveur** pour le debugging
- ✅ **Codes HTTP appropriés** : 400, 404, 409, 500

### 6.3.2 Sécurité de la base de données

#### Protection contre les injections SQL

**Problématique :** Les injections SQL sont une des vulnérabilités les plus critiques (OWASP Top 10 #1).

**Solution : ORM Exposed avec requêtes paramétrées**

```kotlin
// ❌ DANGEREUX (concaténation de chaînes)
val unsafeQuery = "SELECT * FROM event WHERE creator = '$organizerId'"

// ✅ SÛR (ORM Exposed avec paramètres)
EventTable.select { EventTable.creator eq organizerId }
```

**Avantages de Exposed :**
1. **DSL type-safe** : Impossible d'écrire du SQL brut dangereux
2. **Requêtes paramétrées** : Tous les paramètres sont échappés automatiquement
3. **Compilation** : Erreurs détectées à la compilation, pas à l'exécution

**Exemples de requêtes sécurisées :**

```kotlin
// SELECT avec WHERE
EventTable.select { EventTable.creator eq organizerId }
// SQL généré : SELECT * FROM event WHERE creator = ?

// INSERT avec valeurs
EventTable.insert {
  it[name] = event.name
  it[creator] = event.creator.identifier
}
// SQL généré : INSERT INTO event (name, creator) VALUES (?, ?)

// UPDATE avec WHERE
EventTable.update({ EventTable.id eq eventId }) {
  it[name] = updatedName
  it[updateDate] = Instant.now()
}
// SQL généré : UPDATE event SET name = ?, update_date = ? WHERE id = ?
```

#### Gestion des credentials

**Problématique :** Ne jamais hard-coder les credentials en clair dans le code source.

**Solution : Variables d'environnement + fichier .env (local)**

1. **Fichier .env (local, gitignored)** :
```env
DATABASE_URL=jdbc:postgresql://localhost:5432/happyrow_db
DB_USERNAME=happyrow_user
DB_PASSWORD=super_secret_password_123
DB_SSL_MODE=disable
```

2. **Variables d'environnement (Render)** :
```bash
DATABASE_URL=jdbc:postgresql://dpg-xxxx.oregon-postgres.render.com:5432/happyrow_db
DB_USERNAME=happyrow_user
DB_PASSWORD=<auto-generated-by-render>
DB_SSL_MODE=require
```

3. **Chargement sécurisé** :
```kotlin
data class SqlDatabaseConfig(
  val url: String,
  val username: String,
  val password: String,
  val sslMode: String = "require"
)

// Chargement depuis variables d'environnement
val config = SqlDatabaseConfig(
  url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/happyrow_db",
  username = System.getenv("DB_USERNAME") ?: "happyrow_user",
  password = System.getenv("DB_PASSWORD") ?: throw IllegalStateException("DB_PASSWORD required"),
  sslMode = System.getenv("DB_SSL_MODE") ?: "require"
)
```

**Bonnes pratiques :**
- ✅ **Fichier .env dans .gitignore** : Jamais commité
- ✅ **Variables d'environnement en production** : Render gère les secrets
- ✅ **SSL obligatoire en production** : `sslMode=require`
- ✅ **Rotation des passwords** : Changement régulier recommandé

#### SSL/TLS pour la connexion PostgreSQL

**Configuration HikariCP avec SSL :**

```kotlin
val hikariConfig = HikariConfig().apply {
  jdbcUrl = config.url
  username = config.username
  password = config.password
  
  // SSL/TLS activé
  addDataSourceProperty("sslmode", config.sslMode)  // "require" en production
  
  // Certificats (optionnel selon Render)
  addDataSourceProperty("sslrootcert", "server-ca.pem")
  addDataSourceProperty("sslcert", "client-cert.pem")
  addDataSourceProperty("sslkey", "client-key.pem")
  
  // Validation
  validate()
}
```

**Modes SSL :**
- `disable` : Pas de SSL (dev local uniquement)
- `allow` : SSL si disponible
- `prefer` : SSL préféré
- **`require`** : SSL obligatoire (production)
- `verify-ca` : Vérification du certificat CA
- `verify-full` : Vérification complète (hostname + CA)

#### Gestion des transactions ACID

**Principe :** Garantir l'intégrité des données en cas d'erreur.

```kotlin
override suspend fun create(event: Event): Either<CreateEventRepositoryException, Event> {
  return Either.catch {
    transaction(exposedDatabase.database) {
      // 1. Insertion de l'événement
      val eventId = EventTable.insert {
        it[id] = event.identifier
        it[name] = event.name
        // ...
      } get EventTable.id
      
      // 2. Insertion des membres (si erreur, rollback automatique)
      event.members.forEach { member ->
        MemberTable.insert {
          it[eventId] = eventId.value
          it[userId] = UUID.fromString(member.identifier)
        }
      }
      
      event
    }
    // Si exception : rollback automatique
  }.mapLeft { CreateEventRepositoryException("Database error: ${it.message}", it) }
}
```

**Garanties ACID :**
- **Atomicity** : Tout ou rien (rollback automatique en cas d'erreur)
- **Consistency** : Contraintes d'intégrité respectées
- **Isolation** : Transactions isolées les unes des autres
- **Durability** : Données persistées après commit

### 6.3.3 Sécurité du déploiement sur Render

#### Configuration sécurisée

**Secrets gérés par Render :**
```
DATABASE_URL (auto-generated)
DB_USERNAME (auto-generated)
DB_PASSWORD (auto-generated)
ALLOWED_ORIGINS (custom)
```

**Variables d'environnement dans Render Dashboard :**
- ✅ **Pas de commit des secrets** : Configurés via l'interface Render
- ✅ **Rotation automatique** : Render peut régénérer les credentials
- ✅ **Chiffrement au repos** : Secrets chiffrés dans Render

#### HTTPS obligatoire

**Render fournit automatiquement :**
- Certificat SSL/TLS gratuit (Let's Encrypt)
- HTTPS forcé (redirection HTTP → HTTPS)
- Renouvellement automatique des certificats

**Dans l'application :**
```kotlin
// CORS accepte HTTPS en production
allowHost("happyrow-front.vercel.app", schemes = listOf("https"))
```

#### Healthcheck et monitoring

**Configuration Render :**
```yaml
services:
  - type: web
    name: happyrow-core
    runtime: docker
    healthCheckPath: /
    envVars:
      - key: DATABASE_URL
        fromDatabase:
          name: happyrow-db
          property: connectionString
```

**Endpoint de health check :**
```kotlin
get("/") {
  call.respondText("Hello from happyrow-core! 🎉", ContentType.Text.Plain)
}
```

Render vérifie régulièrement cet endpoint (HTTP 200 = healthy).

#### Isolation réseau

**Sécurité Render PostgreSQL :**
- Base de données dans un réseau privé
- Accès uniquement depuis l'application Render
- Pas d'exposition publique (sauf si activée explicitement)
- Connexions chiffrées SSL/TLS

---

## Conclusion de la section 6

Cette section présente les spécifications techniques complètes du projet HappyRow Core :

✅ **Environnement de développement moderne** : JDK 21, Kotlin 2.2, Gradle 8, Docker  
✅ **Architecture multi-modules** : Séparation claire Domain/Infrastructure  
✅ **Qualité de code** : Detekt + Spotless pour une qualité constante  
✅ **Injection de dépendances** : Koin pour une architecture découplée  
✅ **Sécurité multicouche** : CORS, validation, ORM paramétré, SSL/TLS, secrets gérés  
✅ **Déploiement Render** : Configuration cloud sécurisée et automatisée  

Les choix techniques privilégient la **sécurité**, la **maintenabilité** et les **bonnes pratiques** recommandées par l'ANSSI et l'OWASP.
