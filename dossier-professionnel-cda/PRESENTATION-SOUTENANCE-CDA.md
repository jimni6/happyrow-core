---
title: "HappyRow Core - API Backend de Gestion d'Événements Festifs"
subtitle: "Soutenance CDA - Concepteur Développeur d'Applications"
author: "[Votre Nom]"
date: "[Date de soutenance]"
---

# HappyRow Core

## API Backend de Gestion d'Événements Festifs

### Soutenance CDA
**Concepteur Développeur d'Applications**

---

# Contexte et Problématique

## 🎯 La Problématique

### Comment simplifier l'organisation d'événements entre amis ou en famille ?

---

## Situation actuelle (dispersée)

| Outil | Usage | Problème |
|-------|-------|----------|
| 📱 **WhatsApp/SMS** | Communication | Informations perdues |
| 📊 **Excel/Google Sheets** | Suivi contributions | Pas collaboratif |
| 📝 **Notes papier** | Liste des besoins | Risque de perte |
| 🧠 **Mémoire** | Rappels | Charge mentale |

---

## Conséquences

- ❌ Oublis fréquents
- ❌ Doublons (2 personnes apportent la même chose)
- ❌ Charge mentale pour l'organisateur
- ❌ Manque de visibilité pour les participants

---

## Ma Solution : HappyRow Core

### API centralisée pour :

- ✅ Créer et gérer des événements
- ✅ Inviter des participants
- ✅ Définir les ressources nécessaires
- ✅ Suivre les contributions de chacun
- ✅ Réduire la charge mentale

### 🎓 Objectif : Démontrer les **9 compétences CDA**

---

# Architecture Hexagonale

## 🏗️ Architecture Ports et Adaptateurs

### Les 3 Couches

:::::::::::::: {.columns}
::: {.column width="30%"}
**ADAPTATEURS ENTRANTS**

- Points d'entrée
- Endpoints REST
- Ktor
- Transformation DTO
:::

::: {.column width="40%"}
**DOMAIN (Centre)**

- Logique métier pure
- Entities
- Use Cases
- Ports (Interfaces)
- Indépendant des technos
:::

::: {.column width="30%"}
**ADAPTATEURS SORTANTS**

- Ressources externes
- PostgreSQL
- Exposed ORM
- Repositories
:::
::::::::::::::

---

## Avantages de l'Architecture Hexagonale

| Avantage | Bénéfice |
|----------|----------|
| **🎯 Isolation du métier** | Code métier protégé des détails techniques |
| **🧪 Testabilité** | Tests unitaires sans base de données |
| **🔄 Évolutivité** | Changement de framework sans impact métier |
| **📚 Maintenabilité** | Responsabilités clairement séparées |

**🎓 Compétence CDA-2.1** : Concevoir une application organisée en couches

---

# Stack Technique

## 🛠️ Technologies Choisies

| Catégorie | Technologie | Version |
|-----------|-------------|---------|
| **Langage** | Kotlin | 1.9+ |
| **Framework** | Ktor | 2.3+ |
| **Base de données** | PostgreSQL | 15+ |
| **ORM** | Exposed | 0.44+ |
| **Validation** | Arrow (Either) | 1.2+ |
| **Tests** | JUnit 5, Kotest | - |
| **Qualité** | Detekt, Spotless | - |
| **Déploiement** | Docker, Render | - |

---

## Justifications des Choix

**Kotlin** : Typage fort, null-safety, fonctionnel, interopérabilité Java

**Ktor** : Framework léger, asynchrone (coroutines), modulaire

**PostgreSQL** : SGBD relationnel robuste, open-source, transactions ACID

**Exposed** : ORM Kotlin natif, type-safe, support Arrow

**Arrow** : Programmation fonctionnelle, gestion erreurs avec Either

---

# Modèle de Données

## 📊 Entités Principales

:::::::::::::: {.columns}
::: {.column width="50%"}
**EVENTS**
- id (UUID, PK)
- title
- description
- event_date
- location
- distance_km
- max_participants

**PARTICIPANTS**
- id (UUID, PK)
- user_id (FK)
- event_id (FK)
- status (ENUM)
- registered_at
:::

::: {.column width="50%"}
**RESULTS**
- id (UUID, PK)
- event_id (FK)
- participant_id (FK)
- finish_time (INTERVAL)
- ranking

**TEAMS**
- id (UUID, PK)
- event_id (FK)
- team_name
- max_members
:::
::::::::::::::

---

## Relations

- **Event ↔ Participants** : 1-N (un événement a plusieurs participants)
- **Participant ↔ Results** : 1-N (un participant peut avoir plusieurs résultats)
- **Event ↔ Teams** : 1-N (un événement peut avoir plusieurs équipes)

**🎓 Compétence CDA-1.3** : Développer la persistance des données

---

# Endpoints REST API

## 🌐 15 Endpoints Disponibles

### Events (9 endpoints)
```
GET    /api/events              # Liste tous les événements
POST   /api/events              # Créer un événement
GET    /api/events/{id}         # Détails d'un événement
PUT    /api/events/{id}         # Modifier un événement
DELETE /api/events/{id}         # Supprimer un événement
GET    /api/events/{id}/participants
POST   /api/events/{id}/participants
GET    /api/events/{id}/results
POST   /api/events/{id}/results
```

---

## Endpoints REST (suite)

### Participants (3 endpoints)
```
GET    /api/participants/{id}
PUT    /api/participants/{id}
DELETE /api/participants/{id}
```

### Results (3 endpoints)
```
GET    /api/results/{id}
PUT    /api/results/{id}
DELETE /api/results/{id}
```

---

## Conventions REST

| Méthode | Usage | Code Succès |
|---------|-------|-------------|
| **GET** | Récupération | 200 OK |
| **POST** | Création | 201 Created |
| **PUT** | Modification complète | 200 OK |
| **DELETE** | Suppression | 204 No Content |

**Codes d'erreur** : 400 (Bad Request), 404 (Not Found), 500 (Server Error)

**🎓 Compétence CDA-1.1** : Développer la partie back-end d'une application web

---

# Use Cases Métier

## 🎯 Architecture des Use Cases

```kotlin
interface CreateEventUseCase {
  fun execute(request: CreateEventRequest): 
    Either<DomainError, EventResponse>
}
```

**Principe** : Un Use Case = Une action métier

---

## Exemple : CreateEventUseCase

```kotlin
class CreateEventUseCaseImpl(
  private val eventRepository: EventRepository,
  private val clock: Clock
) : CreateEventUseCase {

  override fun execute(
    request: CreateEventRequest
  ): Either<DomainError, EventResponse> {
    return request.validate()
      .flatMap { validRequest ->
        val event = Event.create(
          title = validRequest.title,
          eventDate = validRequest.eventDate,
          location = validRequest.location,
          maxParticipants = validRequest.maxParticipants,
          clock = clock
        )
        eventRepository.save(event)
      }
      .map { EventResponse.from(it) }
  }
}
```

---

## Gestion des Erreurs avec Arrow Either

```kotlin
sealed interface DomainError {
  data class ValidationError(val message: String) : DomainError
  data class NotFoundError(val id: String) : DomainError
  data class ConflictError(val message: String) : DomainError
  data class DatabaseError(val cause: Throwable) : DomainError
}
```

**Avantage** : Erreurs typées, pas d'exceptions, composition fonctionnelle

**🎓 Compétence CDA-1.2** : Développer les composants métier

---

# Repositories et Persistance

## 💾 Pattern Repository

### Interface Domain (Port)
```kotlin
interface EventRepository {
  fun findAll(): Either<DomainError, List<Event>>
  fun findById(id: UUID): Either<DomainError, Event>
  fun save(event: Event): Either<DomainError, Event>
  fun update(event: Event): Either<DomainError, Event>
  fun delete(id: UUID): Either<DomainError, Unit>
}
```

---

## Implémentation Infrastructure

```kotlin
class EventRepositoryImpl(
  private val database: Database
) : EventRepository {

  override fun findById(id: UUID): Either<DomainError, Event> {
    return Either.catch {
      database.transaction {
        EventTable.select { EventTable.id eq id }
          .singleOrNull()
          ?.toEvent()
          ?: throw NotFoundException("Event not found")
      }
    }.mapLeft { DomainError.DatabaseError(it) }
  }
}
```

---

## Gestion des Transactions

```kotlin
fun <T> transactional(block: () -> T): Either<DomainError, T> {
  return Either.catch {
    transaction {
      block()
    }
  }.mapLeft { DomainError.DatabaseError(it) }
}
```

**Points clés** :
- Transactions ACID garanties
- Connection pooling (HikariCP)
- Mapping Domain ↔ Table

**🎓 Compétences CDA-1.3 + CDA-2.3** : Persistance et composants d'accès aux données

---

# Authentification JWT

## 🔐 Architecture JWT avec Supabase

:::::::::::::: {.columns}
::: {.column width="50%"}
**Flow d'authentification**

1. Client → Supabase Auth
2. Supabase → JWT Token
3. Client → API avec Header `Authorization: Bearer <token>`
4. API → Validation du token
5. API → Extraction userId
6. API → Traitement sécurisé
:::

::: {.column width="50%"}
**Sécurité**

✅ Signature HMAC256  
✅ Vérification issuer  
✅ Vérification audience  
✅ Extraction claims sécurisée  
✅ Gestion des erreurs
:::
::::::::::::::

---

## Service de Validation JWT

```kotlin
class SupabaseJwtService(
  private val config: SupabaseJwtConfig
) {
  private val algorithm = Algorithm.HMAC256(config.jwtSecret)

  fun validateToken(token: String): 
    Either<Throwable, AuthenticatedUser> {
    return Either.catch {
      val verifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()
      
      val verifiedJwt = verifier.verify(token)
      extractUser(verifiedJwt)
    }
  }
}
```

---

## Plugin Ktor Personnalisé

```kotlin
class JwtAuthenticationPlugin(
  private val jwtService: SupabaseJwtService
) {
  fun intercept(call: ApplicationCall) {
    val authHeader = call.request.header("Authorization")
    if (authHeader?.startsWith("Bearer ") == true) {
      val token = authHeader.removePrefix("Bearer ")
      jwtService.validateToken(token)
        .map { user -> 
          call.attributes.put(authenticatedUserKey, user) 
        }
    }
  }
}
```

**🎓 Compétences CDA-3.2 + CDA-3.3** : Sécurité et développement sécurisé

---

# Démonstration API

## 🚀 Test avec Postman

### Créer un événement (POST /api/events)

```json
{
  "title": "Course des Lumières 2025",
  "description": "Course nocturne de 10km",
  "eventDate": "2025-06-15T20:00:00Z",
  "location": "Lyon",
  "distanceKm": 10.0,
  "maxParticipants": 500
}
```

**Réponse 201 Created** avec l'événement créé

---

## Test GET /api/events

**Réponse 200 OK** :
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Course des Lumières 2025",
    "eventDate": "2025-06-15T20:00:00Z",
    "location": "Lyon",
    "distanceKm": 10.0,
    "maxParticipants": 500,
    "currentParticipants": 0
  }
]
```

---

## Test avec Authentification JWT

```
GET /api/events/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**✅ Token valide** → Accès autorisé  
**❌ Token invalide** → 401 Unauthorized

---

# Sécurité OWASP Top 10

## 🔒 Mesures de Sécurité Implémentées

| Vulnérabilité OWASP | Mesure Implémentée | Statut |
|---------------------|-------------------|--------|
| A01 - Broken Access Control | JWT + validation tokens | ✅ |
| A02 - Cryptographic Failures | Variables d'environnement | ✅ |
| A03 - Injection | Exposed ORM (requêtes paramétrées) | ✅ |
| A04 - Insecure Design | Architecture hexagonale | ✅ |
| A05 - Security Misconfiguration | Configuration externalisée | ✅ |
| A07 - ID & Auth Failures | JWT avec signature HMAC256 | ✅ |
| A08 - Data Integrity Failures | Validation avec Arrow | ✅ |
| A09 - Logging Failures | Logs structurés (SLF4J) | ✅ |

**Score : 8/10 vulnérabilités traitées**

---

# Qualité du Code

## 🧪 Outils de Qualité

### Detekt (Analyse statique)
- 0 issues majeures
- 0 code smells
- Respect des conventions Kotlin

### Spotless (Formatage)
- Code formaté automatiquement
- Style cohérent dans tout le projet

### Tests
- Tests unitaires des Use Cases
- Tests d'intégration des Repositories
- Tests des endpoints REST

**Objectif : Couverture ≥ 80%** (en cours)

**🎓 Compétence CDA-2.2** : Concevoir une application sécurisée et respecter les bonnes pratiques

---

# DevOps et Déploiement

## 🚀 Pipeline CI/CD

### GitHub Actions
```yaml
name: Deploy to Render
on:
  push:
    branches: [ main ]

jobs:
  deploy:
    - Checkout code
    - Build JAR
    - Build Docker image
    - Push to Docker Hub
    - Deploy on Render
```

---

## Dockerfile Multi-Stage

```dockerfile
# Stage 1: Build
FROM gradle:8-jdk17 AS builder
COPY . /app
RUN gradle clean build --no-daemon

# Stage 2: Runtime
FROM openjdk:17-slim
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

## Déploiement Render

**URL Production** : `https://happyrow-core.onrender.com`

**Configuration** :
- PostgreSQL managé
- Variables d'environnement sécurisées
- HTTPS automatique
- Redémarrage automatique

**🎓 Compétence CDA-3.1** : Préparer et exécuter le déploiement

---

# Bilan Personnel

## 🎓 Difficultés Rencontrées

- **Architecture hexagonale** : Comprendre la séparation stricte des responsabilités
- **Arrow Either** : Nouvelle approche de gestion d'erreurs
- **JWT avec Supabase** : Intégration d'un service externe

## 💪 Compétences Acquises

- Conception d'API REST robuste
- Programmation fonctionnelle avec Kotlin
- Sécurité des applications web
- DevOps et conteneurisation

---

## Ce que j'ai Appris

**Technique** :
- Architecture propre et maintenable
- Gestion des erreurs typées
- Authentification sécurisée
- Tests automatisés

**Méthodologie** :
- Approche itérative (MVP puis améliorations)
- Documentation continue
- Code review et qualité

**🎓 9/9 Compétences CDA démontrées**

---

# Perspectives d'Évolution

## 🔮 Roadmap Future

### Court terme (Q1-Q2 2025)
- 🔄 Gestion des rôles (ADMIN, ORGANIZER, PARTICIPANT)
- 🔄 Refresh tokens et révocation
- ✅ Tests automatisés (≥ 80% couverture)

### Moyen terme (2025)
- Frontend React + TypeScript
- Application mobile (React Native)
- Notifications push
- Monitoring et observabilité

---

## Long terme (2026+)

- Intelligence artificielle (suggestions d'événements)
- Intégration calendrier (Google, Outlook)
- Système de recommandations
- Analyse de données et statistiques avancées

**Vision** : Plateforme complète de gestion d'événements festifs

---

# Merci pour votre Attention

## 🎓 Questions du Jury ?

---

**Contact**  
📧 [votre.email@example.com]  
💻 [github.com/jimni6/happyrow-core]  
🌐 [happyrow-core.onrender.com]

---

## Points à Approfondir

- Architecture hexagonale
- JWT et sécurité
- Gestion des erreurs avec Arrow
- DevOps et déploiement
- Modèle de données SQL
- Tests et qualité
