# 7. Spécifications techniques

## 7.1 Stack technique

### Back-end (happyrow-core)

| Couche | Technologie | Rôle |
|--------|------------|------|
| **Langage** | Kotlin 2.2.0 / JVM 21 | Typage fort, null-safety, data classes, extension functions |
| **Framework web** | Ktor 3.2.2 | Serveur HTTP asynchrone, plugins, routing type-safe |
| **ORM** | JetBrains Exposed 0.61.0 | DSL SQL type-safe en Kotlin, gestion des transactions |
| **Base de données** | PostgreSQL | SGBD relationnel, enums, UUID natifs, arrays |
| **Pool connexions** | HikariCP 6.3.1 | Pool de connexions JDBC performant |
| **Injection de dépendances** | Koin 4.1.0 | DI légère sans génération de code |
| **Erreurs fonctionnelles** | Arrow 2.1.2 | `Either<Error, Success>` pour la gestion d'erreurs sans exceptions |
| **Sérialisation** | Jackson (Kotlin + JavaTime) | JSON ↔ objets Kotlin, snake_case, null-safety stricte |
| **Authentification** | Auth0 JWT 4.4.0 + Supabase | Vérification HMAC256, extraction claims (sub, email) |

### Front-end (happyrow-front)

| Couche | Technologie | Rôle |
|--------|------------|------|
| **Framework** | React 19.1.1 | Bibliothèque UI (dernières fonctionnalités React 19) |
| **Langage** | TypeScript 5.8.3 (strict) | Typage statique, détection d'erreurs à la compilation |
| **Build** | Vite 7.1.2 | Bundler rapide, hot reload, proxy dev |
| **Routing** | React Router DOM 7.13.0 | Navigation SPA client-side |
| **Auth** | Supabase JS SDK 2.39.3 | Login/register, gestion JWT, refresh automatique |
| **CSS** | CSS natif + Design Tokens | Variables CSS custom (couleurs, typo, spacing), pas de framework CSS |
| **PWA** | vite-plugin-pwa 1.2.0 + Workbox | Service Worker, mode offline, installabilité |
| **HTTP** | API native `fetch` | Appels REST sans dépendance tierce (pas d'Axios) |

**Architecture Clean Architecture** : le front-end suit le même principe d'inversion de dépendance que le back-end :

```
[Composants React] → [Context/Provider] → [Use Cases] → [Repository (interface)] → [HttpRepository (fetch)]
```

Chaque domaine fonctionnel (auth, events, resources, contributions, participants) est un module isolé dans `src/features/` avec ses propres composants, hooks, services, use-cases et types. Le token JWT est injecté via une callback `getToken` dans les providers, et chaque repository HTTP ajoute le header `Authorization: Bearer <token>`.

### Infrastructure

| Composant | Technologie |
|-----------|------------|
| **Conteneurisation** | Docker multi-stage (build Gradle 8 + runtime JRE 21) |
| **CI/CD** | GitHub Actions (3 jobs : Detekt → Tests → Deploy) |
| **Hébergement** | Render (Francfort, starter plan) |
| **Qualité** | Detekt 1.23.7 (analyse statique), Spotless + ktlint (formatage) |
| **Tests** | Kotest 5.9.1, MockK 1.14.5, Testcontainers 1.21.3 |

## 7.2 Patterns et principes de conception

### Architecture hexagonale

Le code suit strictement le pattern **Ports & Adapters** :

- **Ports driving** (entrants) : les endpoints REST appellent les use cases du domaine
- **Ports driven** (sortants) : les use cases définissent des interfaces de repository, implémentées par les adapters SQL
- **Domaine isolé** : le module `domain` n'a aucune dépendance vers Ktor, Exposed ou toute bibliothèque technique

### Patterns appliqués

| Pattern | Application |
|---------|------------|
| **Use Case** | Chaque cas d'utilisation métier est une classe dédiée avec une seule méthode publique (`create`, `execute`, `delete`…) |
| **Repository** | Interface définie dans le domaine, implémentation SQL dans l'infrastructure |
| **DTO (Data Transfer Object)** | Séparation stricte entre les objets du domaine et les objets de l'API. Fonctions `toDomain()` et `toDto()` pour la conversion. |
| **Either monad (Arrow)** | Gestion des erreurs sans exceptions. Chaque opération retourne `Either<Error, Success>`, composable via `flatMap` et `mapLeft`. |
| **Verrou optimiste** | Le champ `version` de la table `resource` est vérifié et incrémenté atomiquement lors des mises à jour de quantité. |
| **Value class** | `Creator` est un `@JvmInline value class` encapsulant un String, évitant les erreurs de type primitif. |

### Principes SOLID

| Principe | Application |
|----------|------------|
| **S** — Single Responsibility | Un use case par fichier, un endpoint par fichier, un repository par entité |
| **O** — Open/Closed | Ajout de nouveaux bounded contexts sans modifier l'existant |
| **L** — Liskov Substitution | Les repositories respectent le contrat de l'interface domaine |
| **I** — Interface Segregation | Interfaces de repository spécifiques à chaque entité |
| **D** — Dependency Inversion | Le domaine définit les interfaces, l'infrastructure les implémente |

## 7.3 API REST

### Convention de nommage

- Base path : `/event/configuration/api/v1`
- Ressources au pluriel, identifiants en path parameters
- Méthodes HTTP standards (GET, POST, PUT, DELETE)
- Réponses en JSON, snake_case

### Tableau des endpoints

| Méthode | Chemin | Description | Auth |
|---------|--------|-------------|------|
| `GET` | `/` | Hello (sanity check) | Non |
| `GET` | `/info` | Informations serveur | Non |
| `GET` | `/health` | Health check (Render) | Non |
| `POST` | `/events` | Créer un événement | Oui |
| `GET` | `/events` | Lister ses événements | Oui |
| `PUT` | `/events/{eventId}` | Modifier un événement | Oui |
| `DELETE` | `/events/{eventId}` | Supprimer un événement | Oui |
| `POST` | `/events/{eventId}/participants` | Ajouter un participant | Oui |
| `GET` | `/events/{eventId}/participants` | Lister les participants | Oui |
| `PUT` | `/events/{eventId}/participants/{userEmail}` | Modifier le statut | Oui |
| `POST` | `/events/{eventId}/resources` | Créer une ressource | Oui |
| `GET` | `/events/{eventId}/resources` | Lister les ressources | Oui |
| `POST` | `/events/{eid}/resources/{rid}/contributions` | Ajouter contribution | Oui |
| `POST` | `/events/{eid}/resources/{rid}/contributions/reduce` | Réduire contribution | Oui |
| `DELETE` | `/events/{eid}/resources/{rid}/contributions` | Supprimer contribution | Oui |

### Gestion des erreurs HTTP

| Code | Situation |
|------|-----------|
| `201` | Création réussie |
| `200` | Opération réussie |
| `204` | Suppression réussie |
| `400` | Requête invalide (body mal formé, paramètre manquant) |
| `401` | Token JWT manquant ou invalide |
| `403` | Action non autorisée (ex : supprimer un événement dont on n'est pas le créateur) |
| `404` | Ressource non trouvée |
| `409` | Conflit (nom d'événement dupliqué, verrou optimiste échoué) |
| `500` | Erreur technique (aucune stack trace exposée au client) |

## 7.4 Sécurité — Spécifications techniques

| Mesure | Implémentation |
|--------|----------------|
| **Authentification** | JWT Supabase vérifié à chaque requête via `JwtAuthenticationPlugin` (HMAC256, issuer, audience, expiration) |
| **Autorisation** | Suppression d'événement restreinte au créateur. Extraction de l'identité depuis le token. |
| **Injection SQL** | Requêtes paramétrées systématiques (Exposed ORM, pas de SQL brut) |
| **Validation des entrées** | Vérification côté serveur de tous les paramètres et corps de requête |
| **CORS** | Liste blanche d'origines autorisées, méthodes et headers restreints |
| **Secrets** | Variables d'environnement exclusivement (SUPABASE_JWT_SECRET, DATABASE_URL), jamais dans le code |
| **HTTPS** | Forcé en production (Render) |
| **SSL base de données** | `DB_SSL_MODE=require` en production |
| **Gestion d'erreurs** | Messages génériques côté client, logs détaillés côté serveur. Aucune stack trace exposée. |
| **Analyse statique** | Detekt vérifie l'absence de secrets codés en dur, la complexité cyclomatique, les patterns à risque |
