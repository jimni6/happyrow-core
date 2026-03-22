# 8. Réalisations — Extraits de code significatifs

Dans ce chapitre, je présente les extraits de code les plus représentatifs de mon travail, organisés selon les couches de l'architecture hexagonale. Pour chaque extrait, j'explique les choix techniques que j'ai faits et pourquoi.

## 8.1 Interfaces utilisateur et code correspondant

### Captures d'écran — Interface de contribution sur une ressource

> *[Insérer ici une capture d'écran de l'écran EventDetailsView montrant la liste des ressources avec les boutons +/- et le bouton Validate]*

L'écran ci-dessus montre la vue détail d'un événement avec ses ressources organisées par catégorie (Food, Drinks). Chaque ressource affiche la quantité courante / suggérée et les contrôles de contribution (+/−). Quand l'utilisateur sélectionne une quantité via les boutons +/−, un compteur de delta apparaît et le bouton « Validate » s'affiche pour confirmer la contribution.

> *[Insérer ici une capture d'écran de l'écran HomePage montrant la liste des événements (EventCards)]*

Le dashboard affiche les événements de l'utilisateur sous forme de cartes (`EventCard`) avec la date, le nom, le nombre de participants et la localisation. La barre de navigation permet d'accéder au profil, de créer un événement (+) et de revenir à l'accueil.

### Endpoint REST — Création d'un événement (Driving Adapter)

Voici le endpoint que j'ai écrit pour la création d'événement. C'est le point d'entrée HTTP : il reçoit la requête, la valide, appelle le use case du domaine, et formate la réponse.

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

**Pourquoi j'ai fait ces choix :**

- `Either.catch` capture les exceptions de désérialisation et les transforme en erreur métier typée (`BadRequestException`)
- `flatMap` chaîne l'appel au use case de manière fonctionnelle : si la désérialisation échoue, le use case n'est pas appelé
- `fold` sépare clairement le chemin d'erreur du chemin de succès
- Les conversions `toDomain()` / `toDto()` assurent l'isolation entre les objets de l'API et ceux du domaine

### Gestion des erreurs HTTP — Contribution (avec verrou optimiste)

J'ai implémenté une gestion d'erreur spécifique pour les contributions, notamment pour détecter les conflits de concurrence :

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

**Pourquoi j'ai fait ces choix :**

- La remontée de la cause racine via `generateSequence` me permet de distinguer un conflit de concurrence d'une erreur technique
- Le code HTTP 409 (Conflict) informe le client qu'il doit rafraîchir et réessayer
- Le message d'erreur est explicite pour le client, sans révéler de détails techniques internes

### Composant front-end — Contribution inline sur une ressource

J'ai développé le composant `ResourceItem` pour gérer l'interaction de contribution directement sur chaque ressource. La logique est intelligente : selon la quantité résultante, il décide s'il faut ajouter, mettre à jour ou supprimer la contribution.

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

**Pourquoi j'ai fait ces choix :**

- La logique add/update/delete est déterminée par le composant selon l'état de la contribution existante
- Le state local `selectedQuantity` est un delta par rapport à la contribution actuelle, ce qui rend les incréments/décréments intuitifs
- Le bouton Validate n'apparaît que quand une sélection est en cours (`selectedQuantity !== 0`)

### Service HTTP front-end — Appel API avec JWT

Voici comment j'ai implémenté les appels API côté front-end :

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

**Pourquoi j'ai fait ces choix :**

- J'ai utilisé l'API native `fetch` sans dépendance tierce — c'est une approche minimaliste (le front n'a que 3 dépendances de production)
- Le token est obtenu via une callback `getToken` injectée par le provider parent, ce qui assure le découplage
- Le mapping `snake_case` (API) → `camelCase` (domaine front) est centralisé dans chaque repository

## 8.2 Composants métier

### Use Case — Création d'événement avec auto-ajout du créateur

Ce use case est un bon exemple de la composition fonctionnelle avec Arrow `Either`. Il enchaîne deux opérations (création de l'événement + ajout du participant), où l'échec de l'une empêche l'exécution de l'autre.

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

**Ce qui me plaît dans cette implémentation :**

- Le use case ne dépend que des interfaces `EventRepository` et `ParticipantRepository` (ports), jamais des implémentations SQL
- `flatMap` fait en sorte que le participant n'est créé que si l'événement a été créé avec succès
- `mapLeft` enveloppe les erreurs de repository dans une exception domaine (`CreateEventException`) pour préserver le contexte
- La règle métier "le créateur est automatiquement participant confirmé" est encapsulée dans le domaine, pas dans l'infrastructure

### Modèle du domaine — Entités

Voici les principales entités de mon domaine :

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

**Mes choix sur le modèle :**

- Les `data class` Kotlin garantissent l'immutabilité et l'implémentation automatique de `equals`, `hashCode`, `copy`
- Il n'y a aucune annotation framework (pas de `@Entity`, `@Column`) : le domaine reste pur
- `Creator` est un `@JvmInline value class` : j'ai un typage fort sans coût mémoire à l'exécution
- Le champ `version` dans `Resource` porte la sémantique du verrou optimiste directement dans le modèle

## 8.3 Composants d'accès aux données

### Repository SQL — Verrou optimiste (Driven Adapter)

C'est l'extrait dont je suis le plus fier : la mise à jour de la quantité d'une ressource avec verrou optimiste.

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

**Pourquoi cette approche :**

- Le verrou optimiste évite les verrous SQL (pas de `SELECT FOR UPDATE`) : c'est plus performant dans un contexte de concurrence faible à modérée
- J'ai mis en place une double vérification : la version est vérifiée à la lecture ET à l'écriture, ce qui protège contre les modifications entre les deux
- L'`OptimisticLockException` est une exception domaine, ce qui permet au endpoint de retourner un 409 Conflict
- Le delta de quantité (et non la quantité absolue) permet des mises à jour concurrentes valides quand elles ne concernent pas la même version

### Table Exposed — Contribution (avec contraintes)

Voici comment j'ai défini la table de contribution avec toutes ses contraintes :

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

**L'idée derrière ces contraintes :**

- Le `uniqueIndex` composite empêche qu'un participant contribue deux fois à la même ressource — c'est de la défense en profondeur au niveau base de données
- La contrainte `check` garantit que les quantités sont toujours positives
- Les index sur `participantId` et `resourceId` optimisent les requêtes de recherche
- Les clés étrangères vers `ParticipantTable` et `ResourceTable` assurent l'intégrité référentielle

## 8.4 Autres composants (contrôleurs, utilitaires)

### Plugin d'authentification JWT Ktor

J'ai développé mon propre plugin d'authentification JWT plutôt que d'utiliser le module auth standard de Ktor, pour avoir un contrôle total sur le flux :

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

**Mes choix :**

- J'ai écrit un plugin Ktor personnalisé pour avoir un contrôle total sur le flux d'authentification
- Les routes OPTIONS (pré-vol CORS) et les routes publiques (`/`, `/info`) sont exclues
- Le token validé est stocké dans les `attributes` du call, accessible ensuite via `call.authenticatedUser()`
- J'utilise Arrow `Either` même dans l'infrastructure pour la validation du token

### Service de validation JWT Supabase

Voici le service qui valide les tokens JWT émis par Supabase :

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

**Ce que j'ai mis en place :**

- Algorithme HMAC256 : vérification symétrique avec le secret partagé Supabase
- Triple validation : issuer (`supabase_url/auth/v1`), audience (`authenticated`), expiration (automatique par la librairie Auth0)
- Extraction du `sub` (userId) et de l'`email` depuis les claims JWT
- Le secret JWT est chargé depuis une variable d'environnement, jamais codé en dur
