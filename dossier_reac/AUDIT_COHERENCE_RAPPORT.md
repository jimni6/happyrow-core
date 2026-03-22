# Audit de cohérence : Rapport PDF vs Code Source happyrow-core

**Date** : 17/03/2026
**Rapport audité** : `dossier_reac/DP_jimmy_ni_final.pdf`
**Projet audité** : `happyrow-core` (branche main)

---

## Méthodologie

Vérification systématique de chaque affirmation technique du rapport contre le code source réel du projet : fichiers Gradle (`libs.versions.toml`, `build.gradle.kts`, `settings.gradle.kts`), modules `domain/` et `infrastructure/`, workflows CI/CD (`.github/workflows/`), `Dockerfile`, tables Exposed, routes Ktor, tests, `render.yaml`, `docker-compose.yml`, `init-db.sql`.

---

## 1. Éléments confirmés (corrects)

Les affirmations suivantes ont été vérifiées et sont **toutes correctes** :

### Versions des dépendances

| Composant | Version rapport | Version `libs.versions.toml` | Statut |
|-----------|----------------|------------------------------|--------|
| Kotlin | 2.2.0 | 2.2.0 | ✅ |
| JDK / JRE | 21 | `jvmToolchain(21)` | ✅ |
| Ktor | 3.2.2 | 3.2.2 | ✅ |
| Exposed | 0.61.0 | 0.61.0 | ✅ |
| HikariCP | 6.3.1 | 6.3.1 | ✅ |
| Koin | 4.1.0 | 4.1.0 | ✅ |
| Arrow | 2.1.2 | 2.1.2 | ✅ |
| Auth0 JWT | 4.4.0 | 4.4.0 | ✅ |
| PostgreSQL driver | 42.7.7 | 42.7.7 | ✅ |
| Kotest | 5.9.1 | 5.9.1 | ✅ |
| MockK | 1.14.5 | 1.14.5 | ✅ |
| Testcontainers | 1.21.3 | 1.21.3 | ✅ |
| Detekt | 1.23.7 | 1.23.7 | ✅ |
| Gradle | 8.14.3 | `gradle-wrapper.properties` : 8.14.3 | ✅ |

### Architecture et structure

| Affirmation | Vérification | Statut |
|-------------|-------------|--------|
| Architecture hexagonale, 2 modules Gradle (domain + infrastructure) | `settings.gradle.kts` : `include("domain")`, `include("infrastructure")` | ✅ |
| Module domain sans dépendance framework | `domain/build.gradle.kts` : `dependencies {}` vide, hérite uniquement stdlib + Arrow | ✅ |
| 4 bounded contexts : Event, Participant, Resource, Contribution | 4 packages dans `domain/src/main/kotlin/.../domain/` | ✅ |
| 4 repository interfaces (ports) dans le domaine | `EventRepository`, `ParticipantRepository`, `ResourceRepository`, `ContributionRepository` | ✅ |
| 4 implémentations SQL dans l'infrastructure | `SqlEventRepository`, `SqlParticipantRepository`, `SqlResourceRepository`, `SqlContributionRepository` | ✅ |
| Arrow Either pour la gestion d'erreurs | Utilisé dans tous les use cases et repositories | ✅ |
| Creator est un `@JvmInline value class` | `domain/.../event/creator/model/Creator.kt` | ✅ |
| Koin pour l'injection de dépendances | `koin-ktor` dans les dépendances, `inject()` dans `Routing.kt` | ✅ |
| Jackson sérialisation (snake_case) | `jackson-module-kotlin`, `jackson-datatype-jsr310` dans les dépendances | ✅ |
| Base path `/event/configuration/api/v1` | `Routing.kt` : `const val BASE_PATH = "/event/configuration"` + `route("/api/v1")` | ✅ |

### Logique métier

| Affirmation | Vérification | Statut |
|-------------|-------------|--------|
| Création d'événement avec auto-ajout du créateur comme participant CONFIRMED | `CreateEventUseCase.kt` : `flatMap` → `participantRepository.create(status = CONFIRMED)` | ✅ |
| Verrou optimiste sur Resource (champ version) | `ResourceTable.kt` : `val version = integer("version")` ; `SqlResourceRepository.updateQuantity()` : `WHERE version = expectedVersion` | ✅ |
| Suppression en cascade manuelle | `SqlEventRepository.delete()` : supprime contributions → ressources → participants → événement | ✅ |
| DTO pattern avec conversions toDomain/toDto | 12 fichiers DTO (request + response) dans l'infrastructure | ✅ |

### Base de données (tables Exposed)

| Affirmation | Vérification | Statut |
|-------------|-------------|--------|
| 4 tables dans le schéma `configuration` | `configuration.event`, `configuration.participant`, `configuration.resource`, `configuration.contribution` | ✅ |
| EVENT_TYPE enum (PARTY, BIRTHDAY, DINER, SNACK) | `EventTable.kt` : `customEnumeration("type", "EVENT_TYPE", ...)` + `init-db.sql` | ✅ |
| uniqueIndex sur Participant(userEmail, eventId) | `ParticipantTable.kt` : `uniqueIndex("uq_participant_user_event", userEmail, eventId)` | ✅ |
| uniqueIndex sur Contribution(participantId, resourceId) | `ContributionTable.kt` : `uniqueIndex("uq_contribution_participant_resource", ...)` | ✅ |
| CHECK constraint contribution quantity > 0 | `ContributionTable.kt` : `check("chk_quantity_positive") { quantity greater 0 }` | ✅ |
| Status default CONFIRMED sur Participant | `ParticipantTable.kt` : `.default("CONFIRMED")` | ✅ |
| Index de performance (idx_participant_user, idx_resource_event, etc.) | Définis dans les blocs `init {}` des tables | ✅ |
| Champ members (UUID array) sur EventTable | `EventTable.kt` : `val members = array("members", UUIDColumnType(), ...)` | ✅ |

### Sécurité

| Affirmation | Vérification | Statut |
|-------------|-------------|--------|
| Plugin JWT custom (pas le module auth Ktor standard) | `JwtAuthenticationPlugin.kt` : `createApplicationPlugin("JwtAuthenticationPlugin")` | ✅ |
| HMAC256, validation issuer + audience + expiration | `SupabaseJwtService.kt` : issuer `$supabaseUrl/auth/v1`, audience `authenticated` | ✅ |
| CORS whitelist (localhost:3000/3001/4200/5173/8080/8081 + Vercel) | `Application.kt` : `allowHost(...)` explicites | ✅ |
| Méthodes CORS : GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD | `Application.kt` : 7 `allowMethod(...)` | ✅ |
| Headers CORS : Authorization, Content-Type, Accept, Origin | `Application.kt` : 4 `allowHeader(...)` | ✅ |
| Credentials activés | `Application.kt` : `allowCredentials = true` | ✅ |
| Erreurs 401 avec types MISSING_TOKEN / INVALID_TOKEN | `JwtAuthenticationPlugin.kt` : réponses structurées | ✅ |
| authenticatedUser() via call.attributes | `JwtAuthenticationPlugin.kt` : `SupabaseAuthKey` + extension function | ✅ |

### Infrastructure et CI/CD

| Affirmation | Vérification | Statut |
|-------------|-------------|--------|
| Docker multi-stage (Gradle 8 + JDK 21 → JRE 21) | `Dockerfile` : `FROM gradle:8-jdk21` → `FROM eclipse-temurin:21-jre-jammy` | ✅ |
| USER 1000:1000 (non-root) | `Dockerfile` : `USER 1000:1000` | ✅ |
| JVM -Xmx512m -Xms256m | `Dockerfile` CMD | ✅ |
| Render starter plan, Frankfurt, Docker runtime | `render.yaml` : `plan: starter`, `region: frankfurt`, `runtime: docker` | ✅ |
| Pipeline CI/CD : Detekt → Tests → Deploy Render | `deploy-render.yml` : 4 jobs (detekt → test → deploy → notify) | ✅ |
| `-PWithoutIntegrationTests` en CI | `deploy-render.yml` : `./gradlew test -PWithoutIntegrationTests` | ✅ |
| Spotless + ktlint pour le formatage | `build.gradle.kts` : plugin `com.diffplug.spotless`, config `ktlint()` | ✅ |
| application.conf avec variables d'environnement | `src/main/resources/application.conf` : `${?DATABASE_URL}`, `${?PORT}`, etc. | ✅ |
| init-db.sql (schéma, enum, permissions) | `init-db.sql` : `CREATE SCHEMA configuration`, `CREATE TYPE EVENT_TYPE`, `GRANT` | ✅ |

### Tests

| Affirmation | Vérification | Statut |
|-------------|-------------|--------|
| Tests unitaires BDD (Given/When/Then) | `CreateEventUseCaseTestUT.kt` dans domain | ✅ |
| Personas / fixtures réutilisables | `domain/src/testFixtures/.../persona/` : EventPersona, UserPersona, etc. | ✅ |
| Tests d'intégration Testcontainers | 8 fichiers dans `src/test/.../integration/` | ✅ |
| Test d'intégration verrou optimiste | `OptimisticLockIntegrationTest.kt` | ✅ |

---

## 2. Erreurs factuelles (à corriger)

### ❌ Erreur 1 — "12 Use Cases dans le module domain"

**Sections concernées** : 1 (compétences), 5.5 (cas d'utilisation), 11.1 (bilan)

**Ce que dit le rapport** : "J'ai implémenté 12 Use Cases dans le module domain"

**Réalité** : Il y a **14 classes UseCase** dans `domain/src/main/kotlin/` :

| # | Use Case | Bounded Context |
|---|----------|-----------------|
| 1 | CreateEventUseCase | Event |
| 2 | GetEventsByOrganizerUseCase | Event |
| 3 | GetEventsByUserUseCase | Event |
| 4 | UpdateEventUseCase | Event |
| 5 | DeleteEventUseCase | Event |
| 6 | CreateParticipantUseCase | Participant |
| 7 | GetParticipantsByEventUseCase | Participant |
| 8 | UpdateParticipantUseCase | Participant |
| 9 | DeleteParticipantUseCase | Participant |
| 10 | CreateResourceUseCase | Resource |
| 11 | GetResourcesByEventUseCase | Resource |
| 12 | AddContributionUseCase | Contribution |
| 13 | ReduceContributionUseCase | Contribution |
| 14 | DeleteContributionUseCase | Contribution |

**Correction** : Remplacer "12" par "14" dans les 3 sections.

---

### ❌ Erreur 2 — Endpoint `/health` inexistant

**Sections concernées** : 6.3 (tableau des endpoints), 8.1 (routes publiques)

**Ce que dit le rapport** :
- Tableau 6.3 : `GET /health — Health check (Render) — Non authentifié`
- Section 8.1 : "Seules /, /info et /health sont accessibles sans token"

**Réalité** :
- `Routing.kt` ne définit que `get("/")` et `get("/info")` — aucune route `/health`
- `JwtAuthenticationPlugin.kt` : `PUBLIC_PATHS = setOf("/", "/info")` — pas de `/health`
- `render.yaml` référence `healthCheckPath: /health` mais l'endpoint n'existe pas dans le code

**Conséquence** : Le health check Render pointe vers un endpoint inexistant (réponse 401 ou erreur).

**Correction** :
- Option A : Ajouter `get("/health") { call.respond(HttpStatusCode.OK) }` dans `Routing.kt` et ajouter `"/health"` dans `PUBLIC_PATHS`
- Option B : Retirer `/health` du rapport et du `render.yaml`, remplacer par `/`

---

### ❌ Erreur 3 — Endpoint DELETE participant absent du tableau

**Section concernée** : 6.3 (tableau des endpoints)

**Ce que dit le rapport** : Le tableau liste 15 endpoints mais **ne mentionne pas** `DELETE /events/{eventId}/participants/{userEmail}`.

**Réalité** : Cet endpoint existe dans le code :
- `DeleteParticipantEndpoint.kt` dans infrastructure
- `DeleteParticipantUseCase.kt` dans domain
- Câblé dans `participantEndpoints()` via `Routing.kt`

**Correction** : Ajouter une ligne dans le tableau :

| Méthode | Chemin | Description | Auth |
|---------|--------|-------------|------|
| DELETE | /events/{eventId}/participants/{userEmail} | Supprimer un participant | Oui |

> Note : Avec l'ajout de cet endpoint et le retrait de `/health`, le total reste 15 endpoints (ou 16 si `/health` est ajouté au code).

---

### ❌ Erreur 4 — Contrainte UNIQUE EVENT (name, creator) absente du code

**Section concernée** : 6.4 (contraintes d'intégrité)

**Ce que dit le rapport** : "UNIQUE EVENT (name, creator) — Empêche les doublons de nom par organisateur"

**Réalité** : `EventTable.kt` ne définit **aucun uniqueIndex** :

```kotlin
// EventTable.kt — pas de bloc init{}, pas de uniqueIndex
object EventTable : UUIDTable("configuration.event", "identifier") {
  val name = varchar("name", 256)
  val creator = varchar("creator", 256)
  // ... autres colonnes, mais AUCUN uniqueIndex
}
```

Par comparaison, `ParticipantTable` et `ContributionTable` ont bien des `uniqueIndex` dans leur bloc `init {}`.

Le code `SqlEventRepository` gère `isUnicityConflictException()` (SQL state 23505), mais sans la contrainte en base, cette exception ne sera jamais levée.

**Correction** :
- Option A : Ajouter dans `EventTable` : `init { uniqueIndex("uq_event_name_creator", name, creator) }`
- Option B : Retirer cette contrainte du tableau dans le rapport

---

### ❌ Erreur 5 — "~5 000 lignes de code Kotlin"

**Section concernée** : 11.1 (bilan)

**Ce que dit le rapport** : "~5 000 lignes de code Kotlin, organisées en 2 modules Gradle"

**Réalité** : **~6 048 lignes** de code Kotlin (hors répertoires `.gradle`, `.kotlin`, `build`).

**Correction** : Remplacer par "~6 000 lignes de code Kotlin".

---

### ❌ Erreur 6 — Routes publiques incluent `/health`

**Section concernée** : 8.1 (sécurité côté serveur)

Même cause que l'erreur #2. Le rapport dit "Seules /, /info et /health sont accessibles sans token" alors que le code ne définit que `{"/", "/info"}`.

**Correction** : Aligner avec la correction de l'erreur #2.

---

## 3. Inexactitudes mineures

### ⚠️ Minor 1 — DEFAULT RESOURCE version = 1 vs création à version 0

**Section** : 6.4 (contraintes)

**Rapport** : "DEFAULT RESOURCE version = 1"
**Code** :
- `ResourceTable.kt` : `val version = integer("version").default(1)` → default SQL = 1
- `SqlResourceRepository.create()` : `it[version] = 0` → valeur effectivement insérée = 0

Les ressources démarrent à version **0** en pratique, pas 1. Le scénario de test (section 9.3) utilise correctement `expectedVersion=0`, mais le tableau des contraintes est trompeur.

---

### ⚠️ Minor 2 — Nom de la variable d'environnement CORS

**Section** : 4.3 (variables d'environnement Render)

**Rapport** : `CORS_ALLOWED_ORIGINS -> Origines autorisées (URL front-end Vercel)`
**Code** : `System.getenv("ALLOWED_ORIGINS")` — la variable s'appelle `ALLOWED_ORIGINS`, pas `CORS_ALLOWED_ORIGINS`.

---

### ⚠️ Minor 3 — "Clés étrangères CASCADE" (section 8.8)

**Rapport** : "Clés étrangères CASCADE — La suppression d'un événement supprime proprement toutes les données liées"
**Code** : Les FK Exposed utilisent `.references()` **sans** `onDelete = ReferenceOption.CASCADE`. La cascade est **manuelle** dans `SqlEventRepository.delete()`.

Le rapport reconnaît la cascade manuelle en section 11.3, mais la formulation de la section 8.8 laisse penser que c'est une fonctionnalité SQL native, ce qui est inexact.

---

### ⚠️ Minor 4 — GetEventsByOrganizerUseCase référencé mais non câblé

**Section** : 8.2 (autorisation)

**Rapport** : "GetEventsByOrganizerUseCase filtre par organizer = authenticatedUser"
**Code** : Ce use case **existe** dans le domaine mais n'est **pas câblé** dans `Routing.kt`. C'est `GetEventsByUserUseCase` qui est injecté et utilisé — il retourne les événements où l'utilisateur est créateur **ou** participant.

---

### ⚠️ Minor 5 — docker-compose.yml ne contient pas PostgreSQL local

**Section** : 1 (compétences)

**Rapport** : "PostgreSQL en local via docker-compose.yml"
**Code** : Le `docker-compose.yml` ne contient qu'un service `happyrow-core` qui se connecte à une base **distante** via `DATABASE_URL`. Il n'y a **pas** de service PostgreSQL dans le fichier compose.

---

## 4. Résumé

| Catégorie | Nombre |
|-----------|--------|
| Affirmations vérifiées correctes | ~45+ |
| **Erreurs factuelles** à corriger | **6** |
| Inexactitudes mineures | 5 |

### Verdict

Le rapport est **globalement fidèle** au code source. Les versions des dépendances, l'architecture hexagonale, les patterns (Either, verrou optimiste, DTO, value class), la sécurité JWT, le pipeline CI/CD et la majorité des détails techniques sont **corrects et vérifiables** dans le code.

Les erreurs principales portent sur :
1. Un **comptage inexact** des use cases (14 réels vs 12 annoncés)
2. Un **endpoint `/health` fantôme** (listé dans le rapport et render.yaml mais absent du code)
3. Un **endpoint DELETE participant** existant mais non documenté
4. Une **contrainte UNIQUE** sur EventTable annoncée mais non implémentée

Aucune fabrication majeure n'a été détectée. Les erreurs sont des imprécisions ou des oublis de mise à jour du rapport par rapport à l'état actuel du code.
