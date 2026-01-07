# SLIDE 8 : REPOSITORIES ET PERSISTANCE

---

## 📦 Pattern Repository

### Architecture en 2 parties

```
DOMAIN (Interface)              INFRASTRUCTURE (Implémentation)
┌──────────────────────┐       ┌────────────────────────────┐
│  EventRepository     │       │  SqlEventRepository        │
├──────────────────────┤       ├────────────────────────────┤
│ • save(Event)        │◄──────┤ + save(Event)              │
│ • findById(UUID)     │       │ + findById(UUID)           │
│ • findByOrganizer()  │       │ + findByOrganizer()        │
│ • delete(UUID)       │       │ + delete(UUID)             │
│ • exists(UUID)       │       │ + exists(UUID)             │
└──────────────────────┘       │                            │
                               │ Uses: Exposed ORM          │
                               │       HikariCP             │
                               │       PostgreSQL           │
                               └────────────────────────────┘
```

---

## 🎯 Interface Repository (Domain)

```kotlin
// Port dans le Domain - Abstraction pure
interface EventRepository {
  fun save(event: Event): Event
  fun findById(id: UUID): Event?
  fun findByOrganizer(organizerId: String): List<Event>
  fun delete(id: UUID): Boolean
  fun exists(id: UUID): Boolean
}
```

✅ **Avantages** :
- Indépendant de la technologie
- Testable avec des mocks
- Contrat clair et simple

---

## 💾 Implémentation SQL (Infrastructure)

```kotlin
class SqlEventRepository(
  private val database: Database
) : EventRepository {
  
  override fun save(event: Event): Event {
    return transaction(database) {
      EventTable.insert {
        it[id] = event.id
        it[name] = event.name
        it[description] = event.description
        it[date] = event.date
        it[location] = event.location
        it[type] = event.type
        it[organizerId] = event.organizerId
      }
      event
    }
  }
  
  override fun findById(id: UUID): Event? {
    return transaction(database) {
      EventTable
        .select { EventTable.id eq id }
        .map { it.toEvent() }
        .firstOrNull()
    }
  }
  
  override fun findByOrganizer(organizerId: String): List<Event> {
    return transaction(database) {
      EventTable
        .select { EventTable.organizerId eq organizerId }
        .map { it.toEvent() }
    }
  }
}
```

---

## 🗄️ Table Exposed (DSL Type-Safe)

```kotlin
object EventTable : UUIDTable("events") {
  val name = varchar("name", 256)
  val description = text("description")
  val date = timestamp("event_date")
  val location = varchar("location", 512)
  val type = customEnumeration(
    "event_type",
    "event_type",
    { value -> EventType.valueOf(value as String) },
    { it.name }
  )
  val organizerId = varchar("organizer_id", 256)
}
```

### Avantages Exposed
✅ **Type-safe** : Le compilateur vérifie les colonnes  
✅ **DSL Kotlin** : Syntaxe naturelle et lisible  
✅ **Protection SQL injection** : Requêtes paramétrées automatiquement  
✅ **Support PostgreSQL** : Types natifs (UUID, timestamp, enum)

---

## 🔄 Transactions ACID

```kotlin
transaction(database) {
  // Toutes les opérations sont atomiques
  val event = eventRepository.save(newEvent)
  resourceRepository.save(resource1.copy(eventId = event.id))
  resourceRepository.save(resource2.copy(eventId = event.id))
  // Si une opération échoue, tout est rollback
}
```

### Propriétés ACID garanties
- **Atomicité** : Tout ou rien
- **Cohérence** : Contraintes respectées
- **Isolation** : Transactions concurrentes isolées
- **Durabilité** : Données persistées définitivement

---

## 🏊 Connection Pooling avec HikariCP

```kotlin
val hikariConfig = HikariConfig().apply {
  jdbcUrl = "jdbc:postgresql://localhost:5432/happyrow"
  username = "postgres"
  password = System.getenv("DB_PASSWORD")
  driverClassName = "org.postgresql.Driver"
  
  // Configuration du pool
  maximumPoolSize = 10
  minimumIdle = 2
  connectionTimeout = 30000
  idleTimeout = 600000
  maxLifetime = 1800000
}

val dataSource = HikariDataSource(hikariConfig)
val database = Database.connect(dataSource)
```

### Avantages
⚡ **Performances** : Réutilisation des connexions  
🔒 **Stabilité** : Gestion automatique du pool  
📊 **Monitoring** : Métriques disponibles

---

## 🧪 Tests avec Testcontainers

```kotlin
class SqlEventRepositoryIntegrationTest : FunSpec({
  // PostgreSQL en container Docker pour les tests
  val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine")
  
  beforeSpec {
    postgres.start()
    // Setup database avec vrai PostgreSQL
  }
  
  test("should save and retrieve event") {
    val repository = SqlEventRepository(database)
    val event = Event(...)
    
    repository.save(event)
    val retrieved = repository.findById(event.id)
    
    retrieved shouldNotBe null
    retrieved?.name shouldBe event.name
  }
  
  afterSpec {
    postgres.stop()
  }
})
```

---

## ✅ Bénéfices de l'Architecture

| Aspect | Bénéfice |
|--------|----------|
| **🎯 Inversion de dépendances** | Domain ne dépend pas de l'infrastructure |
| **🧪 Testabilité** | Mocks pour tests unitaires, Testcontainers pour intégration |
| **🔄 Évolutivité** | Changement de BD sans impact sur le métier |
| **🔒 Sécurité** | Protection SQL injection à 100% |
| **⚡ Performance** | Connection pooling et transactions optimisées |

---

## 🎓 Compétences CDA

**CDA-1.3** : Développer la persistance des données  
**CDA-2.3** : Développer des composants d'accès aux données
