# Audit Complet — happyrow-core

> Date : 22 mars 2026
> Scope : backend Kotlin/Ktor (domain, infrastructure, app)

---

## Table des matieres

1. [Incoherences](#1-incoherences)
2. [Failles de securite](#2-failles-de-securite)
3. [Ameliorations suggerees](#3-ameliorations-suggerees)
4. [Gestion des utilisateurs : email vs ID](#4-gestion-des-utilisateurs--email-vs-id)
5. [Resume et priorisation](#5-resume-et-priorisation)

---

## 1. Incoherences

### 1.1 Creator = email en runtime, UUID en test (CRITIQUE)

| Fichier | Comportement |
|---|---|
| `CreateEventEndpoint.kt` | Passe `user.email` comme creator : `requestDto.toDomain(user.email)` |
| `EventTable.kt` | `members` est de type `UUID[]` via `array("members", UUIDColumnType())` |
| `SqlEventRepository.create()` | `request.members.map { UUID.fromString(it.toString()) }` — plante si Creator contient un email |
| `UserPersona.kt` | `aUser = Creator("ab70634a-...")` — utilise un UUID, pas un email |

**Consequence** : la fonctionnalite `members` est cassee en production. `UUID.fromString("test@happyrow.com")` lance une `IllegalArgumentException`.

**Fichiers concernes** :
- `infrastructure/src/main/kotlin/.../event/create/driving/CreateEventEndpoint.kt`
- `infrastructure/src/main/kotlin/.../event/common/driven/event/EventTable.kt`
- `infrastructure/src/main/kotlin/.../common/driven/event/SqlEventRepository.kt`
- `domain/src/testFixtures/kotlin/.../persona/UserPersona.kt`

---

### 1.2 Endpoint `/health` manquant

`render.yaml` declare :

```yaml
healthCheckPath: /health
```

Mais `Routing.kt` ne definit que `/` et `/info`. La route `/health` n'existe pas.

**Consequence** : Render ne peut pas valider la sante de l'application. Le deploiement peut etre marque comme echoue.

**Fichiers concernes** :
- `render.yaml` (ligne 16)
- `src/main/kotlin/.../Routing.kt`

---

### 1.3 Code mort et duplication

| Code mort | Localisation | Raison |
|---|---|---|
| `DatabaseConfig.kt` + `DatabaseFactory` | `infrastructure/src/main/kotlin/.../DatabaseConfig.kt` | Le vrai chemin utilise `DataSource.kt` + `SqlDatabaseConfig` |
| `GetEventsByOrganizerUseCase` | `domain/src/main/kotlin/.../event/get/GetEventsByOrganizerUseCase.kt` | Jamais injecte dans `UseCaseModule.kt`, jamais route |
| `ConfigurationManager.kt` | `infrastructure/src/main/kotlin/.../ConfigurationManager.kt` | Inutilise |
| Bloc `database {}` dans `application.conf` | `src/main/resources/application.conf` | Le code utilise le bloc `application.sql {}` |

---

### 1.4 Double strategie de schema init

| Source | Comportement |
|---|---|
| `init-db.sql` | Cree `RESOURCE_CATEGORY` comme enum PostgreSQL |
| `DatabaseInitializer.kt` | Cree aussi l'enum, puis le migre immediatement en `VARCHAR(50)` via `migrateResourceCategory()` |

Deux strategies coexistent sans outil de migration. Les migrations sont du DDL brut dans du code Kotlin.

**Fichiers concernes** :
- `init-db.sql`
- `infrastructure/src/main/kotlin/.../technical/config/DatabaseInitializer.kt`

---

### 1.5 Version initiale du Resource incoherente

| Fichier | Valeur |
|---|---|
| `ResourceTable.kt` | `val version = integer("version").default(1)` |
| `SqlResourceRepository.create()` | `it[version] = 0` |

Le defaut de la colonne dit 1, mais l'insertion force a 0. Ces deux valeurs se contredisent.

**Fichiers concernes** :
- `infrastructure/src/main/kotlin/.../resource/common/driven/ResourceTable.kt` (ligne 16)
- `infrastructure/src/main/kotlin/.../resource/common/driven/SqlResourceRepository.kt` (ligne 37)

---

### 1.6 Aucune verification d'autorisation sur la creation de participant

`CreateParticipantEndpoint.kt` appelle `call.authenticatedUser()` mais **ignore le resultat** :

```kotlin
Either.catch {
  call.authenticatedUser()            // resultat ignore
  call.receive<CreateParticipantRequestDto>()
}
```

N'importe quel utilisateur authentifie peut ajouter un participant a n'importe quel evenement.

**Fichier concerne** :
- `infrastructure/src/main/kotlin/.../participant/create/driving/CreateParticipantEndpoint.kt` (lignes 27-29)

---

## 2. Failles de securite

### 2.1 Credentials loguees en clair (CRITIQUE)

Dans `DataSource.kt` :

```kotlin
logger.info("  JDBC URL: ${config.jdbcUrl}")
logger.info("  Username: ${config.username}")
```

Si l'URL contenait des credentials embeddes (format Render `postgresql://user:password@host/db`), elles seraient loguees en clair. Meme sans credentials dans l'URL, le username est expose.

De plus, dans le bloc `catch` :

```kotlin
logger.error("Connection details - URL: ${config.jdbcUrl}, Username: ${config.username}")
```

**Fichier concerne** :
- `infrastructure/src/main/kotlin/.../technical/config/DataSource.kt` (lignes 46-51, 57)

**Remediation** : supprimer ces logs ou masquer les informations sensibles.

---

### 2.2 IDOR — Pas de controle d'acces sur les ressources (CRITIQUE)

Les endpoints suivants ne verifient pas que l'utilisateur authentifie est membre ou createur de l'evenement :

| Endpoint | Fichier | Risque |
|---|---|---|
| `GET /events/{id}/resources` | `GetResourcesByEventEndpoint.kt` | Lecture des ressources de n'importe quel evenement |
| `GET /events/{id}/participants` | `GetParticipantsByEventEndpoint.kt` | Lecture des participants (emails) de n'importe quel evenement |
| `POST /events/{id}/resources` | `CreateResourceEndpoint.kt` | Ajout de ressources a n'importe quel evenement |
| `POST /events/{id}/participants` | `CreateParticipantEndpoint.kt` | Ajout de participants a n'importe quel evenement |

**Consequence** : n'importe quel utilisateur authentifie peut interagir avec n'importe quel evenement en devinant ou enumerant les UUIDs. Les UUIDs ne sont pas des secrets.

**Remediation** : avant chaque operation, verifier que l'utilisateur est participant ou createur de l'evenement.

---

### 2.3 JWT verifier recree a chaque requete

Dans `SupabaseJwtService.validateToken()` :

```kotlin
fun validateToken(token: String): Either<Throwable, AuthenticatedUser> {
  return Either.catch {
    val verifier = JWT.require(algorithm)   // reconstruit a chaque appel
      .withIssuer(config.issuer)
      .withAudience(config.audience)
      .build()
    val verifiedJwt = verifier.verify(token)
    extractUser(verifiedJwt)
  }
}
```

Le `JWTVerifier` est thread-safe et immutable. Il devrait etre construit une seule fois dans le constructeur.

**Fichier concerne** :
- `infrastructure/src/main/kotlin/.../technical/auth/SupabaseJwtService.kt` (lignes 14-24)

**Remediation** :

```kotlin
class SupabaseJwtService(private val config: SupabaseJwtConfig) {
  private val algorithm = Algorithm.HMAC256(config.jwtSecret)
  private val verifier = JWT.require(algorithm)
    .withIssuer(config.issuer)
    .withAudience(config.audience)
    .build()

  fun validateToken(token: String): Either<Throwable, AuthenticatedUser> =
    Either.catch { extractUser(verifier.verify(token)) }
}
```

---

### 2.4 Bypass du verrou optimiste (HAUTE)

`SqlParticipantRepository.delete()` met a jour les quantites des resources **sans verifier la version** :

```kotlin
ResourceTable.update({ ResourceTable.id eq resourceId }) {
  it[currentQuantity] = currentQty - contributedQty
  it[version] = currentVersion + 1       // increment sans WHERE version = ...
  it[updatedAt] = clock.instant()
}
```

Ceci bypasse completement le mecanisme d'optimistic locking mis en place dans `SqlResourceRepository.updateQuantity()`.

**Fichier concerne** :
- `infrastructure/src/main/kotlin/.../participant/common/driven/SqlParticipantRepository.kt` (lignes 110-116)

**Remediation** : utiliser `resourceRepository.updateQuantity()` avec le pattern retry, ou ajouter `and (ResourceTable.version eq currentVersion)` dans la clause WHERE.

---

### 2.5 Quantite de resource peut devenir negative

Toujours dans `SqlParticipantRepository.delete()` :

```kotlin
it[currentQuantity] = currentQty - contributedQty
```

Aucun check que `currentQty >= contributedQty`. En cas de bug ou de concurrence, la quantite peut passer en negatif.

**Remediation** :
- Ajouter un check cote code : `require(currentQty >= contributedQty)`
- Ajouter une contrainte DB : `CHECK (current_quantity >= 0)` sur `ResourceTable`

---

### 2.6 Pas de rate limiting

Aucun mecanisme de rate limiting n'est en place. Tous les endpoints sont accessibles sans restriction de debit.

**Risques** : brute force sur les UUIDs, spam de creation d'evenements, DDoS applicatif.

**Remediation** : ajouter un plugin Ktor de rate limiting (par IP ou par user).

---

### 2.7 CORS trop permissif

Dans `Application.kt`, 12+ origines localhost sont codees en dur, et une variable d'env `ALLOWED_ORIGINS` permet d'ajouter dynamiquement des origines. Combine avec `allowCredentials = true`, c'est risque.

```kotlin
allowHost("localhost:3000")
allowHost("localhost:3001")
// ... 10 autres localhost
allowCredentials = true
```

**Remediation** : en production, ne garder que les origines Vercel connues. Supprimer les localhost ou les conditionner a `ENVIRONMENT != production`.

---

### 2.8 Pas de validation de date future pour les evenements

`CreateEventRequestDto.toDomain()` parse la date mais ne verifie pas qu'elle est dans le futur :

```kotlin
fun toDomain(creator: String): CreateEventRequest {
  require(name.isNotBlank()) { "Event name must not be blank" }
  require(location.isNotBlank()) { "Event location must not be blank" }
  return CreateEventRequest(
    // ...
    Instant.parse(eventDate),  // aucune validation eventDate > now
    // ...
  )
}
```

**Remediation** : `require(Instant.parse(eventDate).isAfter(Instant.now())) { "Event date must be in the future" }`

---

### 2.9 Pas de limite haute sur les quantites

Un utilisateur peut contribuer `Int.MAX_VALUE` (2 147 483 647) a une ressource. Aucune borne superieure.

**Remediation** : ajouter `require(quantity in 1..10000) { "Quantity must be between 1 and 10000" }` (ou une limite metier adaptee).

---

### 2.10 Infos applicatives exposees sans auth

Les routes `/` et `/info` sont publiques et exposent :

```kotlin
get("/info") {
  call.respond(mapOf(
    "name" to "happyrow-core",
    "version" to "1.0.0",
    "environment" to (System.getenv("ENVIRONMENT") ?: "unknown"),
    "timestamp" to System.currentTimeMillis(),
  ))
}
```

**Risque** : information disclosure (version, environnement).

---

## 3. Ameliorations suggerees

### 3.1 Outil de migration de base de donnees

Remplacer `DatabaseInitializer.kt` (DDL brut execute au demarrage) par **Flyway** ou **Liquibase** (deja dans les deps de test). Cela permet de versionner les migrations, de les rejouer, et d'eviter les `DO $$ BEGIN ... END $$` ad hoc.

---

### 3.2 Transactions atomiques pour les contributions

`SqlContributionRepository.addOrUpdate()` execute plusieurs operations dans des transactions separees :

1. `participantRepository.findOrCreate()` — transaction 1
2. `ContributionTable.selectAll()` — transaction 2
3. `updateContribution()` — transaction 3
4. `resourceRepository.find()` — transaction 4
5. `resourceRepository.updateQuantity()` — transaction 5

Si l'etape 5 echoue, la contribution est enregistree mais la quantite de resource n'est pas mise a jour, laissant le systeme dans un etat incoherent.

**Remediation** : wrapper le tout dans une seule `transaction(exposedDatabase.database) { ... }`.

---

### 3.3 Pagination des endpoints de liste

Les endpoints `GET /events`, `GET /events/{id}/participants` et `GET /events/{id}/resources` retournent toutes les lignes sans pagination.

**Remediation** : ajouter les query params `?page=0&size=20` et utiliser `LIMIT/OFFSET` dans les requetes.

---

### 3.4 Validation metier dans le domaine

Actuellement, les validations (`name.isNotBlank()`, `quantity > 0`) sont uniquement dans les DTOs de l'infrastructure. Si un autre adaptateur (CLI, message queue) utilise les use cases, les validations ne s'appliqueront pas.

**Remediation** : dupliquer les validations critiques dans les use cases du domaine pour respecter l'architecture hexagonale.

---

### 3.5 Tests unitaires du domaine incomplets

Sur 13 use cases, seul `CreateEventUseCaseTestUT` a des tests unitaires. Les 12 autres n'en ont pas :

- `UpdateEventUseCase`
- `DeleteEventUseCase`
- `GetEventsByUserUseCase`
- `CreateParticipantUseCase`
- `UpdateParticipantUseCase`
- `DeleteParticipantUseCase`
- `GetParticipantsByEventUseCase`
- `CreateResourceUseCase`
- `GetResourcesByEventUseCase`
- `AddContributionUseCase`
- `ReduceContributionUseCase`
- `DeleteContributionUseCase`

---

### 3.6 Nettoyage du code mort

Supprimer :
- `infrastructure/src/main/kotlin/.../DatabaseConfig.kt` (+ `DatabaseFactory`)
- `domain/src/main/kotlin/.../event/get/GetEventsByOrganizerUseCase.kt`
- `infrastructure/src/main/kotlin/.../ConfigurationManager.kt`
- Le bloc `database {}` dans `src/main/resources/application.conf`

---

### 3.7 Cacher le JWT verifier

Voir la remediation proposee en section 2.3.

---

### 3.8 Gestion d'erreurs plus coherente

Certains endpoints retournent des structures d'erreur differentes :
- `AddContributionEndpoint` retourne `{ type, detail }`
- `CreateParticipantEndpoint` retourne un 500 generique pour toute erreur metier
- Le `StatusPages` global retourne `{ type, detail }` pour `IllegalArgumentException` mais un message different pour `Throwable`

**Remediation** : standardiser un format d'erreur unique (ex: RFC 7807 Problem Details).

---

## 4. Gestion des utilisateurs : email vs ID

### Etat actuel

L'utilisateur est identifie par son **email** dans tout le systeme :

```
Participant.userEmail
Creator(email)
ContributionRepository.delete(userEmail, ...)
EventRepository.findByUser(userEmail)
AddContributionRequest.userEmail
```

Le JWT Supabase contient un `sub` (UUID) et un `email`. Le `sub` est extrait dans `AuthenticatedUser.userId` mais **jamais utilise** pour les operations metier — seul l'email est utilise.

### Problemes

| Probleme | Impact |
|---|---|
| Changement d'email | Perte de toutes les associations (evenements, participations, contributions) |
| PII leak | L'email est dans chaque table, chaque log d'erreur, chaque reponse API |
| RGPD | Difficile a anonymiser car l'email est duplique dans 4+ tables |
| Performance | Comparaisons de strings (`VARCHAR(255)`) vs `UUID` (16 bytes fixes) |
| Incoherence | `Creator` wraps tantot un email, tantot un UUID selon le contexte |

### Recommandation : utiliser le UUID Supabase

Le JWT Supabase fournit deja un UUID stable via le claim `sub`. C'est l'identifiant naturel.

**Architecture cible** :

```
configuration.app_user
  id          UUID PRIMARY KEY   -- = sub du JWT Supabase
  email       VARCHAR(255)
  name        VARCHAR(255)
  created_at  TIMESTAMP
  updated_at  TIMESTAMP

configuration.event
  creator     UUID REFERENCES app_user(id)   -- au lieu de VARCHAR(email)

configuration.participant
  user_id     UUID REFERENCES app_user(id)   -- au lieu de user_email VARCHAR

configuration.contribution
  (inchange — reference deja participant_id UUID)
```

**Pourquoi UUID plutot que Int** :
- Supabase fournit deja des UUIDs — pas besoin de table de mapping supplementaire
- Pas de lookup supplementaire a chaque requete (UUID du JWT = PK directe)
- Generable cote client (pas de sequence auto-increment a synchroniser)
- Compatible avec une architecture distribuee

**Si Int est absolument souhaite** :
Il faudrait une table `app_user(id SERIAL PK, supabase_id UUID UNIQUE, email, ...)` et un lookup a chaque requete authentifiee pour resoudre le UUID Supabase en Int interne. C'est faisable mais ajoute une indirection et une requete supplementaire.

### Changements necessaires pour la migration vers UUID

| Couche | Fichiers a modifier |
|---|---|
| Domain models | `Creator.kt` → wrap `UUID` au lieu de `String` |
| Domain models | `Participant.kt` → `userId: UUID` au lieu de `userEmail: String` |
| Domain models | `AddContributionRequest.kt`, `ReduceContributionRequest.kt` → `userId: UUID` |
| Domain repos | `ParticipantRepository.kt`, `ContributionRepository.kt` → signatures avec `UUID` |
| Domain use cases | Tous les use cases qui passent `userEmail` → passer `userId` |
| Infra tables | `EventTable.creator` → `uuid().references(AppUserTable.id)` |
| Infra tables | `ParticipantTable.userEmail` → `ParticipantTable.userId` (UUID) |
| Infra repos | Tous les `SqlXxxRepository` → adapter les requetes |
| Infra endpoints | Extraire `user.userId` (UUID) au lieu de `user.email` |
| Infra new | Creer `AppUserTable`, `SqlAppUserRepository`, `AppUserService` |
| Tests | Adapter tous les tests d'integration et les personas |

---

## 5. Resume et priorisation

### Critique (a corriger immediatement)

| # | Probleme | Type |
|---|---|---|
| 1.1 | Creator email/UUID — members casse en production | Incoherence |
| 2.1 | Credentials loguees en clair | Securite |
| 2.2 | IDOR — pas de controle d'acces sur les ressources | Securite |
| 1.2 | Endpoint `/health` manquant | Incoherence |

### Haute (a planifier rapidement)

| # | Probleme | Type |
|---|---|---|
| 2.4 | Bypass du verrou optimiste | Securite |
| 2.5 | Quantite negative possible | Securite |
| 3.2 | Transactions non atomiques | Amelioration |
| 4 | Migration email → UUID | Refactoring |

### Moyenne (sprint suivant)

| # | Probleme | Type |
|---|---|---|
| 2.3 | JWT verifier recree a chaque requete | Securite |
| 2.6 | Pas de rate limiting | Securite |
| 2.7 | CORS trop permissif | Securite |
| 3.1 | Migration tool (Flyway/Liquibase) | Amelioration |
| 3.4 | Validation dans le domaine | Amelioration |
| 1.3 | Code mort | Incoherence |

### Basse (backlog)

| # | Probleme | Type |
|---|---|---|
| 2.8 | Validation date future | Securite |
| 2.9 | Limite haute quantites | Securite |
| 3.3 | Pagination | Amelioration |
| 3.5 | Tests unitaires manquants | Amelioration |
| 3.8 | Format d'erreur standardise | Amelioration |
| 1.5 | Version default incoherent | Incoherence |
