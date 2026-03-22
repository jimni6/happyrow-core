<!-- Slides 33-35 — Timing: 3 min -->

# Réalisations — Autres composants

## Slide 33 — Authentification JWT : Plugin Ktor (1 min 15s)

```kotlin
// infrastructure/technical/auth/JwtAuthenticationPlugin.kt

val JwtAuthenticationPlugin = createApplicationPlugin(
  name = "JwtAuthenticationPlugin",
  createConfiguration = ::JwtAuthConfig,
) {
  val jwtService = pluginConfig.jwtService
  onCall { call ->
    if (call.request.local.method == HttpMethod.Options ||
        call.request.local.uri in PUBLIC_PATHS) return@onCall

    val token = extractBearerToken(call)
    if (token == null) {
      call.respond(HttpStatusCode.Unauthorized,
        mapOf("type" to "MISSING_TOKEN", "message" to "..."))
      return@onCall
    }
    jwtService.validateToken(token).fold(
      ifLeft  = { call.respond(HttpStatusCode.Unauthorized, ...) },
      ifRight = { user -> call.attributes.put(SupabaseAuthKey, user) },
    )
  }
}
```

```kotlin
// infrastructure/technical/auth/SupabaseJwtService.kt
class SupabaseJwtService(private val config: SupabaseJwtConfig) {
  private val algorithm = Algorithm.HMAC256(config.jwtSecret)
  fun validateToken(token: String): Either<Throwable, AuthenticatedUser> =
    Either.catch {
      val verifier = JWT.require(algorithm)
        .withIssuer(config.issuer).withAudience(config.audience).build()
      val jwt = verifier.verify(token)
      AuthenticatedUser(userId = jwt.subject, email = jwt.getClaim("email").asString())
    }
}
```

- **Plugin Ktor custom** : contrôle total sur le flux d'authentification
- **Triple validation** : issuer + audience + expiration
- **Secret** en variable d'environnement, jamais dans le code

---

## Slide 34 — Endpoint REST : CreateEventEndpoint (1 min)

```kotlin
// infrastructure/event/create/driving/CreateEventEndpoint.kt

fun Route.createEventEndpoint(createEventUseCase: CreateEventUseCase) = route("") {
  post {
    Either.catch {
      val user = call.authenticatedUser()          // Extraction utilisateur JWT
      val requestDto = call.receive<CreateEventRequestDto>()
      requestDto.toDomain(user.email)              // Conversion DTO → domaine
    }
      .mapLeft { BadRequestException.InvalidBodyException(it) }
      .flatMap { request -> createEventUseCase.create(request) }
      .map { it.toDto() }                          // Conversion domaine → DTO réponse
      .fold(
        { it.handleFailure(call) },                // Gestion d'erreur
        { call.respond(HttpStatusCode.Created, it) },
      )
  }
}
```

- `Either.catch` capture les exceptions de désérialisation → erreur typée
- `flatMap` chaîne le use case : si désérialisation échoue, use case non appelé
- `toDomain()` / `toDto()` : isolation objets API ↔ objets domaine
- `fold` sépare le chemin d'erreur du chemin de succès

---

## Slide 35 — Gestion des erreurs HTTP (45s)

```kotlin
// infrastructure/contribution/add/driving/AddContributionEndpoint.kt

private suspend fun AddContributionException.handleAddContributionFailure(
  call: ApplicationCall
) {
  val rootCause = generateSequence<Throwable>(this.cause) { it.cause }.lastOrNull()
  when (rootCause) {
    is OptimisticLockException -> call.logAndRespond(
      status = HttpStatusCode.Conflict,
      responseMessage = ClientErrorMessage.of(
        type = "OPTIMISTIC_LOCK_FAILURE",
        detail = "Resource was modified. Please refresh and try again.",
      ),
      failure = this,
    )
    else -> call.logAndRespond(
      status = HttpStatusCode.InternalServerError,
      responseMessage = technicalErrorMessage(),
      failure = this,
    )
  }
}
```

### Codes HTTP utilisés

| Code | Situation |
|------|-----------|
| `201` | Création réussie |
| `200` | Opération réussie |
| `400` | Requête invalide (body mal formé) |
| `401` | Token JWT manquant ou invalide |
| `403` | Action non autorisée |
| `409` | Conflit (verrou optimiste, nom dupliqué) |
| `500` | Erreur technique (message générique) |
