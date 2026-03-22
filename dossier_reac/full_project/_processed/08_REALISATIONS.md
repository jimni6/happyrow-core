# 8. Réalisations — Extraits de code significatifs

Ce chapitre présente les extraits de code les plus représentatifs du projet, organisés selon les couches de l'architecture hexagonale. Pour chaque extrait, les choix techniques sont argumentés.

## 8.1 Interfaces utilisateur et code correspondant

### Endpoint REST — Création d'un événement (Driving Adapter)

L'endpoint est le point d'entrée HTTP. Il reçoit la requête, la valide, appelle le use case du domaine, et formate la réponse.

```kotlin
// infrastructure/event/create/driving/CreateEventEndpoint.kt

fun Route.createEventEndpoint(createEventUseCase: CreateEventUseCase) = route("") {
  post {
    Either.catch {
      val user = call.authenticatedUser()          // Extraction de l'utilisateur JWT
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

**Choix techniques :**

- `Either.catch` capture les exceptions de désérialisation et les transforme en erreur métier typée (`BadRequestException`)
- `flatMap` chaîne l'appel au use case de manière fonctionnelle : si la désérialisation échoue, le use case n'est pas appelé
- `fold` sépare clairement le chemin d'erreur du chemin de succès
- La conversion `toDomain()` / `toDto()` assure l'isolation entre les objets de l'API et ceux du domaine

### Gestion des erreurs HTTP — Contribution (avec verrou optimiste)

```kotlin
// infrastructure/contribution/add/driving/AddContributionEndpoint.kt

private suspend fun AddContributionException.handleAddContributionFailure(
  call: ApplicationCall
) {
  val rootCause = generateSequence<Throwable>(this.cause) { it.cause }
    .lastOrNull()

  when (rootCause) {
    is OptimisticLockException -> call.logAndRespond(
      status = HttpStatusCode.Conflict,
      responseMessage = ClientErrorMessage.of(
        type = "OPTIMISTIC_LOCK_FAILURE",
        detail = "Resource was modified by another user. Please refresh and try again.",
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

**Choix techniques :**

- La remontée de la cause racine via `generateSequence` permet de distinguer un conflit de concurrence d'une erreur technique
- Le code HTTP 409 (Conflict) informe le client qu'il doit rafraîchir et réessayer
- Le message d'erreur est explicite pour le client, sans révéler de détails techniques internes

### Composant front-end — Contribution inline sur une ressource

Le composant `ResourceItem` gère l'interaction de contribution directement sur chaque ressource, avec une logique intelligente : ajout, mise à jour ou suppression selon la quantité résultante.

```typescript
// src/features/resources/components/ResourceItem.tsx (extrait)

export const ResourceItem: React.FC<ResourceItemProps> = ({
  resource, currentUserId,
  onAddContribution, onUpdateContribution, onDeleteContribution,
}) => {
  const [selectedQuantity, setSelectedQuantity] = useState(0);
  const [isSaving, setIsSaving] = useState(false);

  const userContribution = resource.contributors.find(
    c => c.userId === currentUserId
  );
  const userQuantity = userContribution?.quantity || 0;

  const handleValidate = async () => {
    try {
      setIsSaving(true);
      const newQuantity = userQuantity + selectedQuantity;
      if (newQuantity <= 0) {
        await onDeleteContribution(resource.id);
      } else if (userQuantity === 0) {
        await onAddContribution(resource.id, selectedQuantity);
      } else {
        await onUpdateContribution(resource.id, newQuantity);
      }
      setSelectedQuantity(0);
    } catch (error) {
      console.error('Error updating contribution:', error);
    } finally {
      setIsSaving(false);
    }
  };
  // ... rendu JSX avec boutons +/- et Validate
};
```

**Choix techniques :**

- La logique add/update/delete est déterminée par le composant selon l'état de la contribution existante
- Le state local `selectedQuantity` est un delta par rapport à la contribution actuelle, permettant des incréments/décréments intuitifs
- Le bouton Validate n'apparaît que quand une sélection est en cours (`selectedQuantity !== 0`)

### Service HTTP front-end — Appel API avec JWT

```typescript
// src/features/contributions/services/HttpContributionRepository.ts (extrait)

async createContribution(data: ContributionCreationRequest): Promise<Contribution> {
  const token = this.getToken();
  if (!token) throw new Error('Authentication required');

  const response = await fetch(
    `${this.baseUrl}/events/${data.eventId}/resources/${data.resourceId}/contributions`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ quantity: data.quantity }),
    }
  );

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
  }

  const contributionResponse = await response.json();
  return this.mapApiResponseToContribution(contributionResponse, data.eventId);
}
```

**Choix techniques :**

- API native `fetch` sans dépendance tierce — approche minimaliste (seulement 3 dépendances de production)
- Le token est obtenu via une callback `getToken` injectée par le provider parent, assurant le découplage
- Le mapping `snake_case` (API) → `camelCase` (domaine front) est centralisé dans chaque repository

## 8.2 Composants métier

### Use Case — Création d'événement avec auto-ajout du créateur

Ce use case illustre la composition fonctionnelle avec Arrow `Either` : deux opérations enchaînées (création de l'événement + ajout du participant), où l'échec de l'une empêche l'exécution de l'autre.

```kotlin
// domain/event/create/CreateEventUseCase.kt

class CreateEventUseCase(
  private val eventRepository: EventRepository,
  private val participantRepository: ParticipantRepository,
) {
  fun create(request: CreateEventRequest): Either<CreateEventException, Event> =
    eventRepository.create(request)
      .mapLeft { CreateEventException(request, it) }
      .flatMap { event ->
        participantRepository.create(
          CreateParticipantRequest(
            userEmail = request.creator.toString(),
            eventId = event.identifier,
            status = ParticipantStatus.CONFIRMED,
          ),
        )
          .map { event }
          .mapLeft { CreateEventException(request, it) }
      }
}
```

**Choix techniques :**

- Le use case ne dépend que des interfaces `EventRepository` et `ParticipantRepository` (ports), jamais des implémentations SQL
- `flatMap` assure que le participant n'est créé que si l'événement a été créé avec succès
- `mapLeft` enveloppe les erreurs de repository dans une exception domaine (`CreateEventException`) pour préserver le contexte
- La règle métier "le créateur est automatiquement participant confirmé" est encapsulée dans le domaine, pas dans l'infrastructure

### Modèle du domaine — Entités

```kotlin
// domain/event/common/model/event/Event.kt
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
  val members: List<Creator> = listOf(),
)

// domain/resource/common/model/Resource.kt
data class Resource(
  val identifier: UUID,
  val name: String,
  val category: ResourceCategory,
  val suggestedQuantity: Int,
  val currentQuantity: Int,
  val eventId: UUID,
  val version: Int,        // Utilisé pour le verrou optimiste
  val createdAt: Instant,
  val updatedAt: Instant,
)

// domain/contribution/common/model/Contribution.kt
data class Contribution(
  val identifier: UUID,
  val participantId: UUID,
  val resourceId: UUID,
  val quantity: Int,
  val createdAt: Instant,
  val updatedAt: Instant,
)
```

**Choix techniques :**

- Les `data class` Kotlin garantissent l'immutabilité, l'implémentation automatique de `equals`, `hashCode`, `copy`
- Aucune annotation framework (pas de `@Entity`, `@Column`) : le domaine est pur
- `Creator` est un `@JvmInline value class` : typage fort sans coût mémoire à l'exécution
- Le champ `version` dans `Resource` porte la sémantique du verrou optimiste au niveau du modèle

## 8.3 Composants d'accès aux données

### Repository SQL — Verrou optimiste (Driven Adapter)

L'extrait le plus significatif du projet : la mise à jour de la quantité d'une ressource avec verrou optimiste.

```kotlin
// infrastructure/resource/common/driven/SqlResourceRepository.kt

override fun updateQuantity(
  resourceId: UUID,
  quantityDelta: Int,
  expectedVersion: Int,
): Either<GetResourceRepositoryException, Resource> {
  return Either.catch {
    transaction(exposedDatabase.database) {
      // Lecture de la ressource avec vérification de la version
      val currentResource = ResourceTable
        .selectAll().where {
          (ResourceTable.id eq resourceId) and
            (ResourceTable.version eq expectedVersion)
        }
        .singleOrNull()

      if (currentResource == null) {
        val existsWithDifferentVersion = ResourceTable
          .selectAll().where { ResourceTable.id eq resourceId }
          .singleOrNull()

        if (existsWithDifferentVersion == null) {
          throw ResourceNotFoundException(resourceId)
        } else {
          throw OptimisticLockException(resourceId, expectedVersion)
        }
      }

      val currentQuantity = currentResource[ResourceTable.currentQuantity]
      val newQuantity = currentQuantity + quantityDelta

      // UPDATE conditionnel : uniquement si la version n'a pas changé
      val updatedRows = ResourceTable.update({
        (ResourceTable.id eq resourceId) and
          (ResourceTable.version eq expectedVersion)
      }) {
        it[ResourceTable.currentQuantity] = newQuantity
        it[ResourceTable.version] = expectedVersion + 1
        it[ResourceTable.updatedAt] = clock.instant()
      }

      if (updatedRows == 0) {
        throw OptimisticLockException(resourceId, expectedVersion)
      }
      resourceId
    }
  }
    .flatMap { id -> find(id).flatMap { /* ... */ } }
    .mapLeft { /* mapping des erreurs */ }
}
```

**Choix techniques :**

- Le verrou optimiste évite les verrous SQL (pas de `SELECT FOR UPDATE`) : meilleure performance en contexte de concurrence faible à modérée
- Double vérification : la version est vérifiée à la lecture ET à l'écriture, protégeant contre les modifications entre les deux
- L'`OptimisticLockException` est une exception domaine, permettant au endpoint de retourner un 409 Conflict
- Le delta de quantité (et non la quantité absolue) permet des mises à jour concurrentes valides si elles ne concernent pas la même version

### Table Exposed — Contribution (avec contraintes)

```kotlin
// infrastructure/contribution/common/driven/ContributionTable.kt

object ContributionTable : UUIDTable("configuration.contribution", "id") {
  val participantId = uuid("participant_id").references(ParticipantTable.id)
  val resourceId = uuid("resource_id").references(ResourceTable.id)
  val quantity = integer("quantity")
  val createdAt = timestamp("created_at")
  val updatedAt = timestamp("updated_at")

  init {
    uniqueIndex("uq_contribution_participant_resource", participantId, resourceId)
    index("idx_contribution_participant", false, participantId)
    index("idx_contribution_resource", false, resourceId)
    check("chk_quantity_positive") { quantity greater 0 }
  }
}
```

**Choix techniques :**

- `uniqueIndex` composite empêche qu'un participant contribue deux fois à la même ressource au niveau base de données (défense en profondeur)
- `check` constraint garantit que les quantités sont toujours positives
- Les index sur `participantId` et `resourceId` optimisent les requêtes de recherche
- Les clés étrangères vers `ParticipantTable` et `ResourceTable` assurent l'intégrité référentielle

## 8.4 Autres composants (contrôleurs, utilitaires)

### Plugin d'authentification JWT Ktor

```kotlin
// infrastructure/technical/auth/JwtAuthenticationPlugin.kt

val JwtAuthenticationPlugin = createApplicationPlugin(
  name = "JwtAuthenticationPlugin",
  createConfiguration = ::JwtAuthConfig,
) {
  val jwtService = pluginConfig.jwtService

  onCall { call ->
    if (call.request.local.method == HttpMethod.Options ||
        call.request.local.uri in PUBLIC_PATHS) {
      return@onCall
    }

    val token = extractBearerToken(call)

    if (token == null) {
      call.respond(HttpStatusCode.Unauthorized,
        mapOf("type" to "MISSING_TOKEN",
              "message" to "Authorization header with Bearer token is required"))
      return@onCall
    }

    jwtService.validateToken(token).fold(
      ifLeft = { exception ->
        call.respond(HttpStatusCode.Unauthorized,
          mapOf("type" to "INVALID_TOKEN", "message" to exception.message))
      },
      ifRight = { user ->
        call.attributes.put(SupabaseAuthKey, user)
      },
    )
  }
}
```

**Choix techniques :**

- Plugin Ktor personnalisé (pas d'utilisation du module auth Ktor standard) pour un contrôle total sur le flux d'authentification
- Les routes OPTIONS (pré-vol CORS) et les routes publiques (`/`, `/info`) sont exclues
- Le token validé est stocké dans les `attributes` du call, accessible via `call.authenticatedUser()`
- Arrow `Either` est utilisé même dans l'infrastructure pour la validation du token

### Service de validation JWT Supabase

```kotlin
// infrastructure/technical/auth/SupabaseJwtService.kt

class SupabaseJwtService(private val config: SupabaseJwtConfig) {
  private val algorithm = Algorithm.HMAC256(config.jwtSecret)

  fun validateToken(token: String): Either<Throwable, AuthenticatedUser> {
    return Either.catch {
      val verifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

      val verifiedJwt = verifier.verify(token)
      extractUser(verifiedJwt)
    }
  }

  private fun extractUser(jwt: DecodedJWT): AuthenticatedUser {
    val userId = jwt.subject
      ?: throw JWTVerificationException("Token missing 'sub' claim")
    val email = jwt.getClaim("email").asString()
      ?: throw JWTVerificationException("Token missing 'email' claim")
    return AuthenticatedUser(userId = userId, email = email)
  }
}
```

**Choix techniques :**

- Algorithme HMAC256 : vérification symétrique avec le secret partagé Supabase
- Triple validation : issuer (`supabase_url/auth/v1`), audience (`authenticated`), expiration (automatique par la librairie Auth0)
- Extraction du `sub` (userId) et de l'`email` depuis les claims JWT
- Le secret JWT est chargé depuis une variable d'environnement, jamais codé en dur
