# 7. Spécifications techniques

## 7.1 Stack technique

### Back-end (happyrow-core)

Voici les technologies que j'ai choisies pour le back-end et les raisons de ces choix :

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

**Architecture Clean Architecture** : j'ai appliqué côté front-end le même principe d'inversion de dépendance que côté back-end :

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

J'ai suivi strictement le pattern **Ports & Adapters** :

- **Ports driving** (entrants) : les endpoints REST appellent les use cases du domaine
- **Ports driven** (sortants) : les use cases définissent des interfaces de repository, implémentées par les adapters SQL
- **Domaine isolé** : le module `domain` n'a aucune dépendance vers Ktor, Exposed ou toute bibliothèque technique

### Patterns que j'ai appliqués

| Pattern | Comment je l'ai utilisé |
|---------|------------|
| **Use Case** | J'ai créé une classe dédiée par cas d'utilisation métier, chacune avec une seule méthode publique (`create`, `execute`, `delete`…) |
| **Repository** | J'ai défini l'interface dans le domaine et implémenté la version SQL dans l'infrastructure |
| **DTO (Data Transfer Object)** | J'ai séparé strictement les objets du domaine et ceux de l'API, avec des fonctions `toDomain()` et `toDto()` pour la conversion |
| **Either monad (Arrow)** | J'ai adopté une gestion des erreurs sans exceptions : chaque opération retourne `Either<Error, Success>`, composable via `flatMap` et `mapLeft` |
| **Verrou optimiste** | J'ai ajouté un champ `version` à la table `resource`, vérifié et incrémenté atomiquement lors des mises à jour de quantité |
| **Value class** | J'ai encapsulé `Creator` dans un `@JvmInline value class` pour éviter les erreurs de type primitif |

### Principes SOLID

| Principe | Comment je l'ai appliqué |
|----------|------------|
| **S** — Single Responsibility | Un use case par fichier, un endpoint par fichier, un repository par entité |
| **O** — Open/Closed | Je peux ajouter de nouveaux bounded contexts sans modifier l'existant |
| **L** — Liskov Substitution | Mes repositories respectent le contrat de l'interface domaine |
| **I** — Interface Segregation | J'ai défini des interfaces de repository spécifiques à chaque entité |
| **D** — Dependency Inversion | Le domaine définit les interfaces, l'infrastructure les implémente |

## 7.3 API REST

### Convention de nommage

J'ai suivi les conventions REST classiques :

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

J'ai défini une correspondance claire entre les situations d'erreur et les codes HTTP :

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

## 7.4 Éco-conception

J'ai intégré des principes d'éco-conception numérique dans mes choix de développement pour réduire l'empreinte environnementale du service :

| Principe | Ce que j'ai fait dans HappyRow |
|----------|--------------------------|
| **Sobriété fonctionnelle** | J'ai limité le périmètre fonctionnel aux besoins réels (gestion d'événements, participants, ressources, contributions). Pas de fonctionnalités superflues. |
| **Optimisation des requêtes** | L'ORM Exposed génère des requêtes SQL ciblées avec indexes. Le verrou optimiste évite les verrous pessimistes qui bloqueraient inutilement des lignes. |
| **Minimalisme des dépendances** | Le front-end n'utilise que 3 dépendances de production (React, React Router, Supabase SDK). Pas de framework CSS lourd. API native `fetch` sans Axios. |
| **Images Docker optimisées** | Mon build multi-stage fait que l'image de production ne contient que le JRE 21 et le JAR, pas le SDK Gradle ni les sources. |
| **PWA et mode offline** | Le service worker (Workbox) met en cache les assets statiques, ce qui réduit les requêtes réseau lors des visites répétées. |
| **Pas de sur-provisionnement** | J'ai tuné la JVM (`-Xmx512m -Xms256m`) pour consommer uniquement la mémoire nécessaire. L'hébergement Render est dimensionné au besoin réel. |
| **Pool de connexions** | HikariCP mutualise les connexions PostgreSQL au lieu d'en ouvrir/fermer à chaque requête. |

Ces choix sont en ligne avec les recommandations du référentiel GR491 (Guide de Référence de l'éco-conception de services numériques).

## 7.5 Sécurité — Spécifications techniques

Voici les mesures de sécurité que j'ai mises en place côté technique :

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

## 7.6 Justification de l'absence de NoSQL

Le référentiel CDA (CP8) mentionne les composants d'accès aux données « SQL et NoSQL ». J'ai fait le choix d'utiliser exclusivement **PostgreSQL** (SQL relationnel) pour les raisons suivantes :

| Critère | Ma justification |
|---------|---------------|
| **Nature des données** | Les données du projet sont fortement relationnelles (événements → participants → ressources → contributions) avec des contraintes d'intégrité référentielle (FK, UNIQUE, CHECK). Un modèle relationnel est clairement le plus adapté à ce cas. |
| **Intégrité transactionnelle** | Le verrou optimiste et les opérations atomiques (création événement + ajout participant) nécessitent des transactions ACID, qui sont le point fort des SGBD relationnels. |
| **Complexité du projet** | Ajouter une base NoSQL (Redis, MongoDB…) aurait introduit une complexité d'infrastructure qui n'est pas justifiée par les besoins fonctionnels actuels. |
| **PostgreSQL et JSON** | PostgreSQL supporte nativement les types `JSONB` et les arrays (`UUID[]` que j'utilise pour le champ `members`), ce qui couvre les cas de stockage semi-structuré sans nécessiter un SGBD NoSQL dédié. |

Si le projet devait évoluer (par exemple pour des notifications temps réel, de l'analytics ou du cache de sessions), l'ajout d'un composant NoSQL comme Redis serait envisageable dans une version future, comme je le mentionne dans les perspectives du chapitre 12.
