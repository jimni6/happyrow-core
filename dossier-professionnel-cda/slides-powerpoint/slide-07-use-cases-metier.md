# SLIDE 7 : USE CASES MÉTIER

---

## 🎯 Architecture Use Case

```kotlin
// Interface du Use Case (Domain)
interface CreateEventUseCase {
  fun execute(request: CreateEventRequest): Either<Throwable, Event>
}

// Implémentation (Domain)
class CreateEventUseCaseImpl(
  private val eventRepository: EventRepository,  // Port
  private val clock: Clock
) : CreateEventUseCase {
  
  override fun execute(request: CreateEventRequest): Either<Throwable, Event> {
    return Either.catch {
      // 1. Validations métier
      validateBusinessRules(request)
      
      // 2. Création de l'entité
      val event = Event(
        id = UUID.randomUUID(),
        name = request.name,
        description = request.description,
        date = request.date,
        location = request.location,
        type = request.type,
        organizerId = request.organizerId
      )
      
      // 3. Persistance via le port
      eventRepository.save(event)
      
      // 4. Retour de l'entité créée
      event
    }
  }
  
  private fun validateBusinessRules(request: CreateEventRequest) {
    // Validation : nom max 256 caractères
    if (request.name.length > 256) {
      throw BadRequestException("Name too long")
    }
    
    // Validation : date dans le futur
    if (request.date.isBefore(clock.now())) {
      throw BadRequestException("Date must be in the future")
    }
    
    // Validation : type valide
    if (request.type !in EventType.values()) {
      throw BadRequestException("Invalid event type")
    }
  }
}
```

---

## 📦 Les 9 Use Cases Principaux

### 📅 Events
1. **CreateEventUseCase** : Créer un événement
2. **GetEventsUseCase** : Récupérer les événements d'un organisateur
3. **GetEventByIdUseCase** : Récupérer un événement par ID
4. **UpdateEventUseCase** : Mettre à jour un événement
5. **DeleteEventUseCase** : Supprimer un événement

### 👥 Participants
6. **AddParticipantUseCase** : Ajouter un participant

### 📦 Resources
7. **AddResourceUseCase** : Ajouter une ressource

### 🤝 Contributions
8. **CreateContributionUseCase** : Créer une contribution
9. **GetContributionsByEventUseCase** : Lister les contributions

---

## 🎯 Principe de Responsabilité Unique

Chaque Use Case a **une seule raison de changer** :

| Use Case | Responsabilité | Couplage |
|----------|----------------|----------|
| **CreateEventUseCase** | Logique de création d'événement | ❌ Aucun avec infrastructure |
| **UpdateEventUseCase** | Logique de mise à jour | ❌ Indépendant de Ktor/PostgreSQL |
| **DeleteEventUseCase** | Logique de suppression | ❌ Pas de dépendance technique |

---

## 🔄 Gestion Fonctionnelle des Erreurs

### Arrow Either<L, R>

```kotlin
// Either<Erreur, Succès>
val result: Either<Throwable, Event> = createEventUseCase.execute(request)

// Pattern matching
result.fold(
  ifLeft = { error -> 
    // Gérer l'erreur : log, mapper en HTTP error
    logger.error("Failed to create event", error)
    call.respond(HttpStatusCode.BadRequest, error.message)
  },
  ifRight = { event -> 
    // Succès : retourner l'événement créé
    call.respond(HttpStatusCode.Created, event.toDto())
  }
)
```

### Avantages
✅ **Pas d'exceptions** : Les erreurs sont des valeurs  
✅ **Type-safe** : Le compilateur force la gestion des erreurs  
✅ **Composable** : Chaînage avec `flatMap`, `map`  
✅ **Testable** : Facile à mocker et tester

---

## 🧪 Testabilité

Les Use Cases sont **100% testables** sans infrastructure :

```kotlin
class CreateEventUseCaseTest : StringSpec({
  val mockRepository = mockk<EventRepository>()
  val fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"))
  val useCase = CreateEventUseCaseImpl(mockRepository, fixedClock)
  
  "should create event with valid input" {
    // Given
    val request = CreateEventRequest(...)
    every { mockRepository.save(any()) } returns mockEvent
    
    // When
    val result = useCase.execute(request)
    
    // Then
    result.shouldBeRight()
    verify(exactly = 1) { mockRepository.save(any()) }
  }
  
  "should fail when name is too long" {
    // Given
    val request = CreateEventRequest(name = "a".repeat(257), ...)
    
    // When
    val result = useCase.execute(request)
    
    // Then
    result.shouldBeLeft()
  }
})
```

---

## ✅ Avantages Architecture Use Case

| Avantage | Bénéfice |
|----------|----------|
| **🎯 Responsabilité unique** | Code focalisé et maintenable |
| **🧪 Testabilité** | Tests sans base de données |
| **🔄 Réutilisabilité** | Utilisable depuis API, CLI, etc. |
| **📚 Lisibilité** | Logique métier claire et explicite |

---

## 🎓 Compétence CDA

**CDA-1.2** : Développer des composants métier
