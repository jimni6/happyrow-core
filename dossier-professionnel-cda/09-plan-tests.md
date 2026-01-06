# 9. PLAN DE TESTS

Le plan de tests définit la stratégie globale pour garantir la qualité, la fiabilité et la sécurité de l'application HappyRow Core. Cette section détaille les types de tests, les outils utilisés, la couverture de code et l'intégration dans le pipeline CI/CD.

## 9.1 Stratégie de tests

### 9.1.1 Pyramide des tests

La stratégie de tests suit la **pyramide des tests** recommandée par Martin Fowler :

```
              ┌─────────────┐
              │  Tests E2E  │  (10%)
              │  (Manuels)  │
              └─────────────┘
           ┌─────────────────────┐
           │  Tests d'intégration │  (20%)
           │    (API + DB)        │
           └─────────────────────┘
      ┌──────────────────────────────┐
      │      Tests unitaires          │  (70%)
      │  (Use Cases, Repositories)    │
      └──────────────────────────────┘
```

**Justification de la répartition :**

1. **70% Tests unitaires**
   - Rapides à exécuter (< 1 seconde)
   - Isolés (pas de dépendances externes)
   - Couvrent la logique métier (Use Cases)
   - Testent les composants individuellement

2. **20% Tests d'intégration**
   - Vérifient l'interaction entre composants
   - Testent les endpoints REST + base de données
   - Utilisent Testcontainers (PostgreSQL)
   - Plus lents mais plus réalistes

3. **10% Tests end-to-end**
   - Tests manuels ou automatisés (Postman)
   - Vérifient les scénarios complets
   - Exécutés avant chaque release

### 9.1.2 Niveaux de tests

#### Tests unitaires (Unit Tests)

**Objectif** : Tester une unité de code isolément (fonction, classe, méthode).

**Scope** :
- Use Cases du domain
- Fonctions utilitaires
- Mappers DTO ↔ Domain
- Validations métier

**Caractéristiques** :
- ✅ Exécution rapide (< 100ms par test)
- ✅ Pas de dépendances externes (mocks)
- ✅ Déterministes (même résultat à chaque exécution)
- ✅ Isolation complète

**Exemple de test unitaire - CreateEventUseCase** :

```kotlin
package com.happyrow.core.domain.event.create

import arrow.core.Either
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.common.model.event.Event
import com.happyrow.core.domain.event.common.model.event.EventType
import com.happyrow.core.domain.event.create.model.CreateEventRequest
import com.happyrow.core.domain.event.creator.model.Creator
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.UUID

class CreateEventUseCaseTest : BehaviorSpec({
  
  val eventRepository = mockk<EventRepository>()
  val participantRepository = mockk<ParticipantRepository>()
  val useCase = CreateEventUseCase(eventRepository, participantRepository)
  
  Given("une requête de création d'événement valide") {
    val request = CreateEventRequest(
      name = "Anniversaire de Marie",
      description = "Fête d'anniversaire",
      eventDate = Instant.parse("2025-12-25T18:00:00Z"),
      creator = Creator("jean.dupont@email.com"),
      location = "Paris",
      type = EventType.BIRTHDAY,
      members = emptyList()
    )
    
    val expectedEvent = Event(
      identifier = UUID.randomUUID(),
      name = request.name,
      description = request.description,
      eventDate = request.eventDate,
      creationDate = Instant.now(),
      updateDate = Instant.now(),
      creator = request.creator,
      location = request.location,
      type = request.type,
      members = emptyList()
    )
    
    every { eventRepository.create(request) } returns Either.Right(expectedEvent)
    every { participantRepository.create(any()) } returns Either.Right(mockk())
    
    When("le Use Case est exécuté") {
      val result = useCase.create(request)
      
      Then("l'événement est créé avec succès") {
        result.shouldBeInstanceOf<Either.Right<Event>>()
        result as Either.Right
        result.value.name shouldBe "Anniversaire de Marie"
        result.value.type shouldBe EventType.BIRTHDAY
      }
      
      Then("le repository est appelé une fois") {
        verify(exactly = 1) { eventRepository.create(request) }
        verify(exactly = 1) { participantRepository.create(any()) }
      }
    }
  }
  
  Given("une requête avec un événement déjà existant") {
    val request = CreateEventRequest(
      name = "Anniversaire existant",
      description = "Test",
      eventDate = Instant.parse("2025-12-25T18:00:00Z"),
      creator = Creator("test@email.com"),
      location = "Paris",
      type = EventType.BIRTHDAY,
      members = emptyList()
    )
    
    every { eventRepository.create(request) } returns Either.Left(
      mockk(relaxed = true)
    )
    
    When("le Use Case est exécuté") {
      val result = useCase.create(request)
      
      Then("une erreur est retournée") {
        result.shouldBeInstanceOf<Either.Left<*>>()
      }
    }
  }
})
```

**Avantages de Kotest BehaviorSpec** :
- Syntaxe BDD (Given/When/Then)
- Lisible par les non-développeurs
- Structure les tests de manière claire

---

#### Tests d'intégration (Integration Tests)

**Objectif** : Tester l'intégration entre plusieurs composants (API + Repository + Database).

**Scope** :
- Endpoints REST complets
- Repositories avec vraie base de données (Testcontainers)
- Sérialisation JSON
- Transactions SQL

**Caractéristiques** :
- ⚠️ Plus lents (1-5 secondes par test)
- ✅ Base de données PostgreSQL réelle (Docker)
- ✅ Tests des requêtes SQL
- ✅ Validation de l'intégration complète

**Exemple de test d'intégration - CreateEventEndpoint** :

```kotlin
package com.happyrow.core.infrastructure.event.create

import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class CreateEventEndpointIntegrationTest : FunSpec({
  
  // Configuration Testcontainers PostgreSQL
  val postgres = PostgreSQLContainer(
    DockerImageName.parse("postgres:15-alpine")
  ).apply {
    withDatabaseName("happyrow_test")
    withUsername("test_user")
    withPassword("test_password")
    start()
  }
  
  afterSpec {
    postgres.stop()
  }
  
  test("POST /events - création d'un événement valide") {
    testApplication {
      application {
        // Configuration de l'application de test
        configureTestApplication(postgres)
      }
      
      val response = client.post("/event/configuration/api/v1/events") {
        header("x-user-id", "test@example.com")
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody("""
          {
            "name": "Test Event",
            "description": "Test Description",
            "event_date": "2025-12-25T18:00:00Z",
            "location": "Paris",
            "type": "PARTY"
          }
        """.trimIndent())
      }
      
      // Vérifications
      response.status shouldBe HttpStatusCode.Created
      
      val body = response.bodyAsText()
      body.contains("Test Event") shouldBe true
      body.contains("identifier") shouldBe true
    }
  }
  
  test("POST /events - nom déjà existant retourne 409 Conflict") {
    testApplication {
      application {
        configureTestApplication(postgres)
      }
      
      // Création du premier événement
      client.post("/event/configuration/api/v1/events") {
        header("x-user-id", "test@example.com")
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody("""
          {
            "name": "Duplicate Event",
            "description": "Test",
            "event_date": "2025-12-25T18:00:00Z",
            "location": "Paris",
            "type": "PARTY"
          }
        """.trimIndent())
      }
      
      // Tentative de création d'un doublon
      val response = client.post("/event/configuration/api/v1/events") {
        header("x-user-id", "test@example.com")
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody("""
          {
            "name": "Duplicate Event",
            "description": "Test",
            "event_date": "2025-12-25T18:00:00Z",
            "location": "Paris",
            "type": "PARTY"
          }
        """.trimIndent())
      }
      
      response.status shouldBe HttpStatusCode.Conflict
    }
  }
  
  test("POST /events - données invalides retourne 400 Bad Request") {
    testApplication {
      application {
        configureTestApplication(postgres)
      }
      
      val response = client.post("/event/configuration/api/v1/events") {
        header("x-user-id", "test@example.com")
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody("""
          {
            "name": "Test Event",
            "event_date": "invalid-date",
            "type": "INVALID_TYPE"
          }
        """.trimIndent())
      }
      
      response.status shouldBe HttpStatusCode.BadRequest
    }
  }
})
```

**Avantages de Testcontainers** :
- Base de données PostgreSQL réelle dans Docker
- Isolation complète entre les tests
- Configuration proche de la production
- Nettoyage automatique après les tests

---

#### Tests de sécurité

**Objectif** : Vérifier que les mesures de sécurité sont bien implémentées.

**Scope** :
- Protection injection SQL
- CORS
- Validation des entrées
- Gestion des erreurs

**Exemple de test de sécurité - Injection SQL** :

```kotlin
test("POST /events - tentative d'injection SQL est bloquée") {
  testApplication {
    application {
      configureTestApplication(postgres)
    }
    
    val response = client.post("/event/configuration/api/v1/events") {
      header("x-user-id", "test@example.com")
      header(HttpHeaders.ContentType, ContentType.Application.Json)
      setBody("""
        {
          "name": "'; DROP TABLE event; --",
          "description": "Test injection",
          "event_date": "2025-12-25T18:00:00Z",
          "location": "Paris",
          "type": "PARTY"
        }
      """.trimIndent())
    }
    
    // Le nom doit être inséré comme chaîne littérale
    response.status shouldBe HttpStatusCode.Created
    
    // Vérification que la table existe toujours
    val getResponse = client.get("/event/configuration/api/v1/events?organizerId=test@example.com")
    getResponse.status shouldBe HttpStatusCode.OK
  }
}
```

---

#### Tests de charge (Performance Tests)

**Objectif** : Vérifier les performances sous charge.

**Outils** : Gatling, K6, Apache JMeter

**Métriques** :
- Temps de réponse (< 200ms pour 95% des requêtes)
- Throughput (requêtes/seconde)
- Taux d'erreur (< 1%)

**Exemple de scénario K6** :

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 50 },  // Montée en charge : 50 users
    { duration: '3m', target: 50 },  // Maintien de la charge
    { duration: '1m', target: 0 },   // Descente
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'],  // 95% des requêtes < 200ms
    http_req_failed: ['rate<0.01'],    // Taux d'erreur < 1%
  },
};

export default function () {
  const payload = JSON.stringify({
    name: `Event ${Date.now()}`,
    description: 'Load test event',
    event_date: '2025-12-25T18:00:00Z',
    location: 'Paris',
    type: 'PARTY',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'x-user-id': 'loadtest@example.com',
    },
  };

  const res = http.post(
    'https://happyrow-core.onrender.com/event/configuration/api/v1/events',
    payload,
    params
  );

  check(res, {
    'status is 201': (r) => r.status === 201,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });

  sleep(1);
}
```

---

### 9.1.3 Critères d'acceptation

**Tests unitaires** :
- ✅ Couverture de code : **≥ 80%** pour le domain
- ✅ Tous les Use Cases testés
- ✅ Tous les cas d'erreur couverts
- ✅ Temps d'exécution : < 1 seconde pour tous les tests unitaires

**Tests d'intégration** :
- ✅ Tous les endpoints REST testés
- ✅ Cas nominaux et cas d'erreur
- ✅ Tests des contraintes de base de données
- ✅ Temps d'exécution : < 30 secondes pour tous les tests d'intégration

**Tests de sécurité** :
- ✅ Injection SQL : 100% bloquée
- ✅ CORS : Configuration vérifiée
- ✅ Validation : Données invalides rejetées

**Tests de charge** :
- ✅ 95% des requêtes < 200ms
- ✅ Taux d'erreur < 1%
- ✅ Throughput : ≥ 100 req/sec

---

## 9.2 Frameworks et outils de tests

### 9.2.1 Kotest - Framework de tests Kotlin

**Version** : 5.9.1

**Pourquoi Kotest ?**
- Framework natif Kotlin (pas d'adaptation Java)
- Syntaxe expressive et lisible
- Multiples styles de specs (BehaviorSpec, FunSpec, StringSpec)
- Matchers riches et extensibles
- Support coroutines Kotlin

**Styles de specs disponibles** :

```kotlin
// BehaviorSpec (Given/When/Then - BDD)
class CreateEventBehaviorSpec : BehaviorSpec({
  Given("une requête valide") {
    When("le Use Case est exécuté") {
      Then("l'événement est créé") {
        // assertions
      }
    }
  }
})

// FunSpec (tests fonctionnels)
class CreateEventFunSpec : FunSpec({
  test("création d'un événement valide") {
    // test logic
  }
})

// StringSpec (tests simples)
class CreateEventStringSpec : StringSpec({
  "création d'un événement valide" {
    // test logic
  }
})
```

**Matchers Kotest** :

```kotlin
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.string.shouldContain

// Exemples d'assertions
result shouldBe expectedValue
list.shouldContain(item)
list.shouldBeEmpty()
result.shouldBeInstanceOf<Either.Right<Event>>()
errorMessage shouldContain "Invalid date"
```

**Configuration dans build.gradle.kts** :

```kotlin
dependencies {
  testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
  testImplementation("io.kotest:kotest-assertions-core:5.9.1")
  testImplementation("io.kotest:kotest-assertions-json:5.9.1")
  testImplementation("io.kotest.extensions:kotest-assertions-arrow:2.0.0")
}

tasks.withType<Test> {
  useJUnitPlatform()
}
```

---

### 9.2.2 MockK - Mocking framework pour Kotlin

**Version** : 1.14.5

**Pourquoi MockK ?**
- Framework de mocking natif Kotlin
- Support des coroutines
- Syntaxe DSL intuitive
- Mocke les classes finales (contrairement à Mockito)

**Exemples d'utilisation** :

```kotlin
import io.mockk.*

// Création d'un mock
val repository = mockk<EventRepository>()

// Configuration du comportement (stubbing)
every { repository.create(any()) } returns Either.Right(event)
every { repository.find(eventId) } returns Either.Right(event)

// Vérification des appels
verify(exactly = 1) { repository.create(any()) }
verify(atLeast = 1) { repository.find(eventId) }
verify { repository wasNot Called }

// Mock relaxé (retourne des valeurs par défaut)
val relaxedMock = mockk<EventRepository>(relaxed = true)

// Capture d'arguments
val slot = slot<CreateEventRequest>()
every { repository.create(capture(slot)) } returns Either.Right(event)
// Vérifier l'argument capturé
slot.captured.name shouldBe "Test Event"

// Coévery pour les fonctions suspend
coEvery { suspendFunction() } returns result
coVerify { suspendFunction() }
```

---

### 9.2.3 Testcontainers - Containers Docker pour les tests

**Version** : 1.21.3

**Pourquoi Testcontainers ?**
- Base de données PostgreSQL réelle
- Isolation complète entre les tests
- Configuration proche de la production
- Nettoyage automatique

**Configuration PostgreSQL** :

```kotlin
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class PostgresContainer : PostgreSQLContainer<PostgresContainer>(
  DockerImageName.parse("postgres:15-alpine")
)

// Utilisation dans les tests
val postgres = PostgresContainer().apply {
  withDatabaseName("happyrow_test")
  withUsername("test_user")
  withPassword("test_password")
  withInitScript("init-db.sql")  // Script d'initialisation
  start()
}

// Configuration de la connexion
val dataSource = HikariDataSource(HikariConfig().apply {
  jdbcUrl = postgres.jdbcUrl
  username = postgres.username
  password = postgres.password
  driverClassName = "org.postgresql.Driver"
})
```

**Avantages** :
- ✅ Tests avec une vraie base de données PostgreSQL
- ✅ Pas de mock de la couche base de données
- ✅ Détection des problèmes SQL à l'exécution
- ✅ Tests des migrations et du schéma

---

### 9.2.4 Ktor Test - Framework de tests pour Ktor

**Intégré dans Ktor 3.2.2**

**Fonctionnalités** :
- Test des endpoints sans démarrer un serveur
- Client HTTP intégré
- Assertions sur les réponses HTTP

**Exemple** :

```kotlin
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*

testApplication {
  application {
    configureRouting()
    configureSerialization()
  }
  
  val response = client.get("/event/configuration/api/v1/events") {
    parameter("organizerId", "test@example.com")
  }
  
  response.status shouldBe HttpStatusCode.OK
  response.bodyAsText().shouldContain("Test Event")
}
```

---

### 9.2.5 Awaitility - Tests asynchrones

**Version** : 4.3.0

**Utilisation** : Attendre une condition asynchrone.

```kotlin
import org.awaitility.kotlin.*

await.atMost(5.seconds).until {
  repository.find(eventId).isRight()
}
```

---

### 9.2.6 JaCoCo - Couverture de code

**Version** : 0.8.13

**Configuration** :

```kotlin
plugins {
  jacoco
}

jacoco {
  toolVersion = "0.8.13"
}

tasks.jacocoTestReport {
  reports {
    xml.required.set(true)
    html.required.set(true)
  }
  
  classDirectories.setFrom(
    files(classDirectories.files.map {
      fileTree(it) {
        exclude(
          "**/dto/**",
          "**/config/**",
          "**/Application*"
        )
      }
    })
  )
}

tasks.jacocoTestCoverageVerification {
  violationRules {
    rule {
      limit {
        minimum = 0.80.toBigDecimal()  // 80% minimum
      }
    }
  }
}
```

**Rapport généré** :
- HTML : `build/reports/jacoco/test/html/index.html`
- XML : `build/reports/jacoco/test/jacocoTestReport.xml`

---

## 9.3 Environnements de tests

### 9.3.1 Environnement local

**Configuration** :
- Base de données : PostgreSQL via Testcontainers
- Variables d'environnement : `.env.test`
- Exécution : `./gradlew test`

**Fichier .env.test** :

```bash
DB_USERNAME=test_user
DB_PASSWORD=test_password
DB_SSL_MODE=disable
ENVIRONMENT=test
```

---

### 9.3.2 Environnement CI/CD (GitHub Actions)

**Workflow** : `.github/workflows/deploy-render.yml`

```yaml
test:
  name: Run Tests
  runs-on: ubuntu-latest
  needs: detekt
  
  steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        
    - name: Cache Gradle packages
      uses: actions/cache@v4
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
        
    - name: Run tests
      run: ./gradlew test -PWithoutIntegrationTests
      
    - name: Upload test results
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: test-results
        path: |
          **/build/reports/tests/
          **/build/test-results/
```

**Exclusion des tests d'intégration en CI** :
- Les tests d'intégration nécessitent Docker (Testcontainers)
- GitHub Actions gratuit limite l'usage de Docker
- Option : `-PWithoutIntegrationTests` pour exclure les tests d'intégration

---

### 9.3.3 Environnement de staging (Render)

**Configuration** :
- Base de données : PostgreSQL Render (instance dédiée)
- Tests manuels via Postman/Insomnia
- Tests de charge avec K6

---

## 9.4 Couverture de code et intégration CI/CD

### 9.4.1 Objectifs de couverture

| Couche | Objectif | Justification |
|--------|----------|---------------|
| **Domain (Use Cases)** | ≥ 90% | Logique métier critique |
| **Infrastructure (Repositories)** | ≥ 80% | Accès aux données |
| **Infrastructure (Endpoints)** | ≥ 70% | Orchestration HTTP |
| **Configuration** | ≥ 50% | Code de configuration |
| **Global** | ≥ 80% | Objectif général |

**Exclusions de la couverture** :
- DTOs (pas de logique)
- Fichiers de configuration
- Point d'entrée `Application.kt`

---

### 9.4.2 Rapports de couverture

**Génération du rapport** :

```bash
./gradlew test jacocoTestReport
```

**Visualisation** :
- HTML : `build/reports/jacoco/test/html/index.html`
- Affiche la couverture par package, classe, méthode

**Exemple de rapport** :

```
Package: com.happyrow.core.domain.event.create
  CreateEventUseCase.kt       95%  (19/20 lines)
  CreateEventRequest.kt      100%  (5/5 lines)
  CreateEventException.kt    100%  (3/3 lines)

Package: com.happyrow.core.infrastructure.event.create
  CreateEventEndpoint.kt      85%  (34/40 lines)
  CreateEventRequestDto.kt   100%  (8/8 lines)
```

---

### 9.4.3 Intégration CI/CD

#### Pipeline GitHub Actions

```
┌──────────────┐
│   Push/PR    │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│   Detekt     │  ← Analyse statique de code
│  (Quality)   │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│    Tests     │  ← Exécution des tests unitaires
│  (Unit + ?)  │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│    Build     │  ← Compilation du JAR
│              │
└──────┬───────┘
       │
       ▼ (main only)
┌──────────────┐
│   Deploy     │  ← Déploiement sur Render
│   (Render)   │
└──────────────┘
```

**Étapes du pipeline** :

1. **Detekt** : Analyse statique (code quality)
2. **Tests** : Exécution des tests unitaires (et intégration si Docker disponible)
3. **Build** : Compilation du JAR (sans les tests pour gagner du temps)
4. **Deploy** : Déploiement automatique sur Render (branche `main` uniquement)

**Conditions de déploiement** :
- ✅ Tous les tests passent
- ✅ Detekt ne remonte pas d'erreurs critiques
- ✅ Build réussi
- ✅ Branche `main`

---

### 9.4.4 Badges de qualité (recommandé)

**GitHub README.md** :

```markdown
![Tests](https://github.com/jimni6/happyrow-core/workflows/Deploy%20to%20Render/badge.svg)
![Code Coverage](https://codecov.io/gh/jimni6/happyrow-core/branch/main/graph/badge.svg)
![Quality](https://sonarcloud.io/api/project_badges/measure?project=jimni6_happyrow-core&metric=alert_status)
```

**Services d'intégration recommandés** :
- **Codecov** : Suivi de la couverture de code
- **SonarCloud** : Analyse de qualité de code
- **Dependabot** : Alertes sur les vulnérabilités

---

## 9.5 Tests par type de composant

### 9.5.1 Tests des Use Cases (Domain)

**Exemple : CreateEventUseCase**

```kotlin
class CreateEventUseCaseTest : BehaviorSpec({
  
  val eventRepository = mockk<EventRepository>()
  val participantRepository = mockk<ParticipantRepository>()
  val useCase = CreateEventUseCase(eventRepository, participantRepository)
  
  Given("un événement valide") {
    val request = createValidRequest()
    val event = createValidEvent()
    
    every { eventRepository.create(request) } returns Either.Right(event)
    every { participantRepository.create(any()) } returns Either.Right(mockk())
    
    When("le Use Case est exécuté") {
      val result = useCase.create(request)
      
      Then("l'événement est créé") {
        result.shouldBeRight()
        result.getOrNull()?.name shouldBe "Test Event"
      }
      
      Then("le créateur est ajouté comme participant") {
        verify { participantRepository.create(any()) }
      }
    }
  }
  
  Given("le repository retourne une erreur") {
    val request = createValidRequest()
    val error = CreateEventRepositoryException(request, Exception("DB error"))
    
    every { eventRepository.create(request) } returns Either.Left(error)
    
    When("le Use Case est exécuté") {
      val result = useCase.create(request)
      
      Then("une erreur métier est retournée") {
        result.shouldBeLeft()
        result.leftOrNull().shouldBeInstanceOf<CreateEventException>()
      }
    }
  }
})
```

**Couverture** :
- ✅ Cas nominal
- ✅ Erreur repository
- ✅ Erreur participant
- ✅ Vérification des appels

---

### 9.5.2 Tests des Repositories (Infrastructure)

**Exemple : SqlEventRepository**

```kotlin
class SqlEventRepositoryIntegrationTest : FunSpec({
  
  lateinit var postgres: PostgreSQLContainer<*>
  lateinit var repository: SqlEventRepository
  lateinit var database: Database
  
  beforeSpec {
    postgres = PostgreSQLContainer("postgres:15-alpine").apply {
      withDatabaseName("test")
      start()
    }
    
    val dataSource = HikariDataSource(HikariConfig().apply {
      jdbcUrl = postgres.jdbcUrl
      username = postgres.username
      password = postgres.password
    })
    
    database = Database.connect(dataSource)
    
    transaction(database) {
      SchemaUtils.create(EventTable)
    }
    
    repository = SqlEventRepository(Clock.systemUTC(), ExposedDatabase(database))
  }
  
  afterSpec {
    postgres.stop()
  }
  
  test("create - insère un événement en base") {
    val request = CreateEventRequest(
      name = "Test Event",
      description = "Test",
      eventDate = Instant.now(),
      creator = Creator("test@example.com"),
      location = "Paris",
      type = EventType.PARTY,
      members = emptyList()
    )
    
    val result = repository.create(request)
    
    result.shouldBeRight()
    val event = result.getOrNull()!!
    event.name shouldBe "Test Event"
    
    // Vérification en base
    transaction(database) {
      val count = EventTable.selectAll().count()
      count shouldBe 1
    }
  }
  
  test("create - contrainte d'unicité sur le nom") {
    val request = CreateEventRequest(
      name = "Unique Event",
      description = "Test",
      eventDate = Instant.now(),
      creator = Creator("test@example.com"),
      location = "Paris",
      type = EventType.PARTY,
      members = emptyList()
    )
    
    repository.create(request) // Première insertion
    val result = repository.create(request) // Doublon
    
    result.shouldBeLeft()
    result.leftOrNull()?.cause.shouldBeInstanceOf<UnicityConflictException>()
  }
})
```

---

### 9.5.3 Tests des Endpoints (Infrastructure)

**Exemple : CreateEventEndpoint**

```kotlin
class CreateEventEndpointIntegrationTest : FunSpec({
  
  lateinit var postgres: PostgreSQLContainer<*>
  
  beforeSpec {
    postgres = PostgreSQLContainer("postgres:15-alpine").apply {
      start()
    }
  }
  
  afterSpec {
    postgres.stop()
  }
  
  test("POST /events - retourne 201 Created avec un événement valide") {
    testApplication {
      environment {
        config = ApplicationConfig("application-test.conf")
      }
      
      application {
        configureTestApplication(postgres)
      }
      
      val response = client.post("/event/configuration/api/v1/events") {
        header("x-user-id", "test@example.com")
        contentType(ContentType.Application.Json)
        setBody("""
          {
            "name": "Test Event",
            "description": "Test Description",
            "event_date": "2025-12-25T18:00:00Z",
            "location": "Paris",
            "type": "PARTY"
          }
        """)
      }
      
      response.status shouldBe HttpStatusCode.Created
      response.headers[HttpHeaders.ContentType] shouldContain "application/json"
      
      val body = response.bodyAsText()
      body shouldContain "identifier"
      body shouldContain "Test Event"
    }
  }
  
  test("POST /events - retourne 400 Bad Request avec un type invalide") {
    testApplication {
      application {
        configureTestApplication(postgres)
      }
      
      val response = client.post("/event/configuration/api/v1/events") {
        header("x-user-id", "test@example.com")
        contentType(ContentType.Application.Json)
        setBody("""
          {
            "name": "Test Event",
            "description": "Test",
            "event_date": "2025-12-25T18:00:00Z",
            "location": "Paris",
            "type": "INVALID_TYPE"
          }
        """)
      }
      
      response.status shouldBe HttpStatusCode.BadRequest
      response.bodyAsText() shouldContain "Invalid event type"
    }
  }
})
```

---

## 9.6 Bonnes pratiques de tests

### 9.6.1 Principes FIRST

Les tests doivent être **FIRST** :

- **F**ast : Rapides à exécuter
- **I**ndependent : Indépendants les uns des autres
- **R**epeatable : Reproductibles (même résultat)
- **S**elf-validating : Validation automatique (pas d'inspection manuelle)
- **T**imely : Écrits en même temps que le code

### 9.6.2 Convention de nommage

**Pattern** : `methodName_stateUnderTest_expectedBehavior`

```kotlin
test("create_withValidRequest_returnsCreatedEvent")
test("create_withDuplicateName_returnsConflictError")
test("create_withInvalidDate_returnsBadRequestError")
```

**Ou BDD** (Given/When/Then) :

```kotlin
Given("une requête valide") {
  When("le Use Case est exécuté") {
    Then("l'événement est créé")
  }
}
```

### 9.6.3 Arrange-Act-Assert (AAA)

```kotlin
test("create event with valid request") {
  // Arrange : Préparation
  val request = CreateEventRequest(...)
  val repository = mockk<EventRepository>()
  every { repository.create(request) } returns Either.Right(event)
  val useCase = CreateEventUseCase(repository)
  
  // Act : Exécution
  val result = useCase.create(request)
  
  // Assert : Vérification
  result.shouldBeRight()
  result.getOrNull()?.name shouldBe "Test Event"
}
```

### 9.6.4 Test isolation

**Chaque test doit être isolé** :
- Pas de dépendances entre tests
- Nettoyage après chaque test (beforeEach/afterEach)
- Utilisation de mocks pour les dépendances externes

```kotlin
class CreateEventUseCaseTest : BehaviorSpec({
  
  lateinit var repository: EventRepository
  lateinit var useCase: CreateEventUseCase
  
  beforeEach {
    repository = mockk()
    useCase = CreateEventUseCase(repository)
  }
  
  afterEach {
    clearAllMocks()
  }
  
  // Tests...
})
```

---

## Conclusion de la section 9

Cette section présente un **plan de tests complet et professionnel** :

✅ **Stratégie multicouche** : Unitaires (70%), Intégration (20%), E2E (10%)  
✅ **Frameworks modernes** : Kotest, MockK, Testcontainers  
✅ **Couverture ciblée** : ≥ 80% global, ≥ 90% pour le domain  
✅ **Intégration CI/CD** : GitHub Actions avec pipeline automatisé  
✅ **Bonnes pratiques** : FIRST, AAA, BDD, isolation  
✅ **Tests de sécurité** : Injection SQL, CORS, validation  

**Points forts** :
- Tests avec PostgreSQL réel (Testcontainers)
- Pipeline CI/CD complet (Detekt → Tests → Build → Deploy)
- Exemples de tests concrets et réutilisables
- Objectifs de couverture clairs et justifiés

**Axes d'amélioration identifiés** :
- Implémenter les tests unitaires (actuellement 0 tests)
- Ajouter les tests d'intégration avec Testcontainers
- Configurer JaCoCo pour le suivi de la couverture
- Intégrer Codecov pour les rapports publics

**Compétences démontrées** :
- **CDA-1.4** : Documenter le déploiement (stratégie de tests)
- **CDA-2.2** : Développer une application en couches (tests par couche)
- **CDA-3.1** : Préparer le déploiement sécurisé (tests de sécurité)
