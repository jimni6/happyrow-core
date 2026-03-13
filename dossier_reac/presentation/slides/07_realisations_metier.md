<!-- Slides 25-28 — Timing: 4 min -->

# Réalisations — Composants métier

## Slide 25 — Use Case : CreateEventUseCase (1 min 15s)

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

- Dépendance sur les **ports** (interfaces), jamais sur les implémentations SQL
- `flatMap` : le participant n'est créé que si l'événement a réussi
- **Règle métier encapsulée** : "le créateur est automatiquement participant confirmé"

---

## Slide 26 — Use Case : AddContributionUseCase (1 min)

### Chaîne fonctionnelle complète

```kotlin
// domain/contribution/add/AddContributionUseCase.kt

class AddContributionUseCase(
  private val contributionRepository: ContributionRepository,
  private val participantRepository: ParticipantRepository,
) {
  fun execute(request: AddContributionRequest): Either<AddContributionException, Contribution> =
    participantRepository.findOrCreate(request.userEmail, request.eventId)
      .mapLeft { AddContributionException(request, it) }
      .flatMap { participant ->
        contributionRepository.addOrUpdate(
          participant = participant,
          request = request,
        )
      }
      .mapLeft { AddContributionException(request, it) }
}
```

- **findOrCreate** : si le participant n'existe pas, il est créé automatiquement
- Le use case orchestre 2 repositories sans connaître leur implémentation
- L'`addOrUpdate` inclut le verrou optimiste (dans le repository SQL)

---

## Slide 27 — Modèles domaine (1 min)

```kotlin
// domain/event/common/model/event/Event.kt
data class Event(
  val identifier: UUID, val name: String, val description: String,
  val eventDate: Instant, val creator: Creator, val location: String,
  val type: EventType, val members: List<Creator> = listOf(),
)

// domain/resource/common/model/Resource.kt
data class Resource(
  val identifier: UUID, val name: String, val category: ResourceCategory,
  val suggestedQuantity: Int, val currentQuantity: Int,
  val eventId: UUID, val version: Int,  // verrou optimiste
)

// domain/event/common/model/creator/Creator.kt
@JvmInline value class Creator(val value: String)
```

- **`data class`** : immutabilité, `equals`/`hashCode` automatiques, `copy`
- **Aucune annotation framework** : pas de `@Entity`, `@Column` — domaine pur
- **`Creator`** = `@JvmInline value class` : typage fort, zéro coût mémoire à l'exécution
- **`version`** : sémantique du verrou optimiste au niveau du modèle

---

## Slide 28 — Gestion d'erreurs : Arrow Either (45s)

### Flux fonctionnel sans exceptions

```kotlin
// Chaque opération retourne Either<Error, Success>
fun create(request: CreateEventRequest): Either<CreateEventException, Event>

// Composition via flatMap / mapLeft
eventRepository.create(request)          // Either<RepoError, Event>
  .mapLeft { CreateEventException(it) }  // Enveloppe l'erreur avec contexte
  .flatMap { event ->                    // Chaîne uniquement si Right (succès)
    participantRepository.create(...)
      .map { event }                     // Garde l'événement comme résultat
  }

// Résolution finale via fold
result.fold(
  ifLeft  = { error -> call.respond(error.toHttpStatus(), error.toDto()) },
  ifRight = { event -> call.respond(HttpStatusCode.Created, event.toDto()) },
)
```

### Avantages vs exceptions

| Either | Exceptions |
|--------|-----------|
| Type de retour explicite | Invisible dans la signature |
| Composable (flatMap, map) | try/catch imbriqués |
| Pas de coût de stack trace | Stack trace coûteuse |
| Traçabilité (mapLeft enveloppe) | Perte de contexte |
