# 5. SPÉCIFICATIONS FONCTIONNELLES

## 5.1 Architecture logicielle

### 5.1.1 Vue d'ensemble de l'architecture

HappyRow Core est une application backend développée selon les principes de la **Clean Architecture** et du **Domain-Driven Design (DDD)**. L'architecture multicouche répartie sécurisée adoptée garantit une séparation claire des responsabilités et facilite la maintenabilité et l'évolutivité du système.

**Architecture en 3 couches principales :**

```
┌─────────────────────────────────────────────────────────────┐
│                    COUCHE PRÉSENTATION                       │
│                 (Infrastructure - Driving)                   │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │  API REST  │  │   CORS     │  │  Routing   │            │
│  │ Endpoints  │  │  Security  │  │  Handler   │            │
│  └────────────┘  └────────────┘  └────────────┘            │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                      COUCHE MÉTIER                           │
│                    (Domain - Core)                           │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │  Use Cases │  │   Models   │  │  Business  │            │
│  │            │  │            │  │   Rules    │            │
│  └────────────┘  └────────────┘  └────────────┘            │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                  COUCHE ACCÈS DONNÉES                        │
│                (Infrastructure - Driven)                     │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │ Repository │  │  Database  │  │   Exposed  │            │
│  │    Impl    │  │   Tables   │  │    ORM     │            │
│  └────────────┘  └────────────┘  └────────────┘            │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
               ┌───────────────┐
               │  PostgreSQL   │
               │   Database    │
               └───────────────┘
```

### 5.1.2 Choix technologiques

#### Stack technique

| Composant | Technologie | Version | Justification |
|-----------|-------------|---------|---------------|
| **Framework Backend** | Ktor | 3.x | Framework Kotlin moderne, léger et performant pour les API REST |
| **Langage** | Kotlin | 1.9+ | Langage moderne, sûr (null-safety), interopérable avec Java |
| **Base de données** | PostgreSQL | 15+ | SGBD relationnel robuste, performant, open-source |
| **ORM** | Exposed | 0.50+ | ORM Kotlin natif, type-safe, DSL intuitif |
| **Injection de dépendances** | Koin | 3.5+ | Framework DI léger et facile à utiliser pour Kotlin |
| **Conteneurisation** | Docker | 24+ | Isolation des environnements, portabilité |
| **CI/CD** | GitHub Actions | - | Intégration continue native à GitHub |
| **Gestion de versions** | Git | 2.x | Contrôle de version distribué standard |

#### Justification de l'architecture sécurisée

L'architecture multicouche répartie choisie offre plusieurs avantages en termes de sécurité :

1. **Isolation des couches** : Chaque couche a des responsabilités bien définies, limitant la surface d'attaque
2. **Validation en cascade** : 
   - Couche API : Validation des données entrantes (DTO)
   - Couche Domain : Validation des règles métier
   - Couche Data : Validation SQL et gestion des transactions
3. **Ports & Adapters** : Abstraction des dépendances techniques via des interfaces (ports)
4. **Principes SOLID** : Application rigoureuse pour un code maintenable et testable

### 5.1.3 Organisation des modules

Le projet est structuré en **3 modules Gradle** distincts :

```
happyrow-core/
├── src/                        # Module application (point d'entrée)
│   └── main/kotlin/com/happyrow/core/
│       ├── Application.kt      # Configuration Ktor
│       ├── Routing.kt          # Configuration des routes
│       └── modules/            # Configuration Koin
│
├── domain/                     # Module Domain (Logique métier)
│   └── src/main/kotlin/com/happyrow/core/domain/
│       ├── event/              # Agrégat Event
│       │   ├── common/         # Modèles et ports
│       │   ├── create/         # Use Case création
│       │   ├── update/         # Use Case modification
│       │   ├── delete/         # Use Case suppression
│       │   └── get/            # Use Case consultation
│       ├── participant/        # Agrégat Participant
│       ├── resource/           # Agrégat Resource
│       └── contribution/       # Agrégat Contribution
│
└── infrastructure/             # Module Infrastructure
    └── src/main/kotlin/com/happyrow/core/infrastructure/
        ├── event/              # Adaptateurs Event
        │   ├── driving/        # API REST endpoints
        │   └── driven/         # Implémentation Repository
        ├── participant/
        ├── resource/
        ├── contribution/
        └── technical/          # Utilitaires techniques
            ├── config/         # Configuration DB
            ├── exposed/        # Extensions Exposed
            └── jackson/        # Sérialisation JSON
```

### 5.1.4 Stratégie de sécurité par couche

#### Couche Présentation (API REST)
- **CORS** : Configuration stricte des origines autorisées
- **Validation des entrées** : DTOs avec validation stricte
- **Gestion des erreurs** : Pas de fuite d'informations sensibles
- **Headers sécurisés** : Contrôle des headers HTTP autorisés

#### Couche Métier (Domain)
- **Encapsulation** : Les modèles du domain sont immuables (data class Kotlin)
- **Validation métier** : Règles métier centralisées dans les Use Cases
- **Gestion des erreurs** : Types Either<Error, Success> pour le flow fonctionnel
- **Isolation** : Aucune dépendance vers l'infrastructure

#### Couche Données (Repository)
- **Protection SQL Injection** : Utilisation exclusive de l'ORM Exposed (requêtes paramétrées)
- **Transactions ACID** : Gestion des transactions pour l'intégrité des données
- **Gestion des connexions** : Pool de connexions sécurisé (HikariCP)
- **Credentials** : Variables d'environnement pour les secrets

---

## 5.2 Maquettes et enchaînement des écrans (API REST)

### 5.2.1 Vue d'ensemble de l'API

HappyRow Core expose une **API REST** permettant la gestion d'événements festifs et de leurs participants. L'API suit les conventions REST et utilise le format JSON pour les échanges de données.

**Base URL de l'API :**
```
http://localhost:8080/event/configuration/api/v1
```

**En production (Render) :**
```
https://happyrow-core.onrender.com/event/configuration/api/v1
```

### 5.2.2 Endpoints disponibles

#### A. Gestion des événements

| Méthode | Endpoint | Description | Authentification |
|---------|----------|-------------|------------------|
| `POST` | `/events` | Créer un nouvel événement | ❌ Non (prévu) |
| `GET` | `/events?organizerId={id}` | Récupérer les événements d'un organisateur | ❌ Non (prévu) |
| `PUT` | `/events/{eventId}` | Mettre à jour un événement | ❌ Non (prévu) |
| `DELETE` | `/events/{eventId}` | Supprimer un événement | ❌ Non (prévu) |

#### B. Gestion des participants

| Méthode | Endpoint | Description | Authentification |
|---------|----------|-------------|------------------|
| `POST` | `/participants` | Ajouter un participant à un événement | ❌ Non (prévu) |
| `GET` | `/participants?eventId={id}` | Récupérer les participants d'un événement | ❌ Non (prévu) |
| `PUT` | `/participants/{participantId}` | Mettre à jour un participant | ❌ Non (prévu) |

#### C. Gestion des ressources

| Méthode | Endpoint | Description | Authentification |
|---------|----------|-------------|------------------|
| `POST` | `/resources` | Ajouter une ressource à un événement | ❌ Non (prévu) |
| `GET` | `/resources?eventId={id}` | Récupérer les ressources d'un événement | ❌ Non (prévu) |

#### D. Gestion des contributions

| Méthode | Endpoint | Description | Authentification |
|---------|----------|-------------|------------------|
| `POST` | `/contributions` | Ajouter une contribution | ❌ Non (prévu) |
| `DELETE` | `/contributions/{contributionId}` | Supprimer une contribution | ❌ Non (prévu) |

#### E. Endpoints utilitaires

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/` | Message de bienvenue |
| `GET` | `/info` | Informations sur l'application |

### 5.2.3 Diagramme de flux des opérations principales

```
┌──────────┐                                          ┌──────────┐
│  Client  │                                          │   API    │
│ Frontend │                                          │  Backend │
└────┬─────┘                                          └────┬─────┘
     │                                                      │
     │  1. POST /events                                    │
     │  {name, description, date, location, type}          │
     ├────────────────────────────────────────────────────>│
     │                                                      │
     │  2. Response: 201 Created                           │
     │  {eventId, ...}                                     │
     │<────────────────────────────────────────────────────┤
     │                                                      │
     │  3. POST /participants                              │
     │  {eventId, name, email}                             │
     ├────────────────────────────────────────────────────>│
     │                                                      │
     │  4. Response: 201 Created                           │
     │  {participantId, ...}                               │
     │<────────────────────────────────────────────────────┤
     │                                                      │
     │  5. POST /resources                                 │
     │  {eventId, name, quantity}                          │
     ├────────────────────────────────────────────────────>│
     │                                                      │
     │  6. Response: 201 Created                           │
     │  {resourceId, ...}                                  │
     │<────────────────────────────────────────────────────┤
     │                                                      │
     │  7. GET /events?organizerId=xxx                     │
     ├────────────────────────────────────────────────────>│
     │                                                      │
     │  8. Response: 200 OK                                │
     │  [{event1}, {event2}, ...]                          │
     │<────────────────────────────────────────────────────┤
     │                                                      │
```

### 5.2.4 Spécifications des principaux endpoints

#### Création d'un événement

**Requête :**
```http
POST /event/configuration/api/v1/events
Content-Type: application/json

{
  "name": "Anniversaire de Marie",
  "description": "Fête d'anniversaire pour les 30 ans de Marie",
  "eventDate": "2024-12-25T18:00:00Z",
  "creator": "jean.dupont@email.com",
  "location": "15 rue de la Paix, Paris",
  "type": "BIRTHDAY"
}
```

**Réponse succès (201 Created) :**
```json
{
  "identifier": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Anniversaire de Marie",
  "description": "Fête d'anniversaire pour les 30 ans de Marie",
  "eventDate": "2024-12-25T18:00:00Z",
  "creationDate": "2024-01-05T10:30:00Z",
  "updateDate": "2024-01-05T10:30:00Z",
  "creator": {
    "identifier": "jean.dupont@email.com"
  },
  "location": "15 rue de la Paix, Paris",
  "type": "BIRTHDAY",
  "members": []
}
```

**Réponse erreur (400 Bad Request) :**
```json
{
  "error": "Validation failed",
  "field": "eventDate",
  "message": "Event date cannot be in the past"
}
```

#### Récupération des événements d'un organisateur

**Requête :**
```http
GET /event/configuration/api/v1/events?organizerId=jean.dupont@email.com
```

**Réponse succès (200 OK) :**
```json
[
  {
    "identifier": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Anniversaire de Marie",
    "description": "Fête d'anniversaire pour les 30 ans de Marie",
    "eventDate": "2024-12-25T18:00:00Z",
    "creationDate": "2024-01-05T10:30:00Z",
    "updateDate": "2024-01-05T10:30:00Z",
    "creator": {
      "identifier": "jean.dupont@email.com"
    },
    "location": "15 rue de la Paix, Paris",
    "type": "BIRTHDAY",
    "members": [
      {"identifier": "marie.martin@email.com"},
      {"identifier": "paul.durand@email.com"}
    ]
  }
]
```

### 5.2.5 Gestion des erreurs

L'API utilise les codes HTTP standards et renvoie des messages d'erreur structurés :

| Code HTTP | Signification | Exemple d'utilisation |
|-----------|---------------|----------------------|
| 200 | OK | Récupération réussie |
| 201 | Created | Création réussie |
| 400 | Bad Request | Données invalides |
| 404 | Not Found | Ressource non trouvée |
| 409 | Conflict | Conflit (unicité violée) |
| 500 | Internal Server Error | Erreur serveur |

---

## 5.3 Modèle de données

### 5.3.1 Modèle Entité-Association (MCD)

```
┌─────────────────────┐
│       EVENT         │
├─────────────────────┤
│ identifier (PK)     │ UUID
│ name                │ VARCHAR(256)
│ description         │ VARCHAR(256)
│ event_date          │ TIMESTAMP
│ creator             │ VARCHAR(256)
│ location            │ VARCHAR(256)
│ type                │ ENUM(EVENT_TYPE)
│ creation_date       │ TIMESTAMP
│ update_date         │ TIMESTAMP
│ members             │ UUID[]
└─────────────────────┘
          │
          │ 1:N
          │
          ▼
┌─────────────────────┐
│    PARTICIPANT      │
├─────────────────────┤
│ identifier (PK)     │ UUID
│ event_id (FK)       │ UUID
│ name                │ VARCHAR(256)
│ email               │ VARCHAR(256)
│ status              │ VARCHAR(50)
│ creation_date       │ TIMESTAMP
│ update_date         │ TIMESTAMP
└─────────────────────┘

          ┌─────────────────────┐
          │       EVENT         │
          └─────────────────────┘
                    │
                    │ 1:N
                    │
                    ▼
          ┌─────────────────────┐
          │     RESOURCE        │
          ├─────────────────────┤
          │ identifier (PK)     │ UUID
          │ event_id (FK)       │ UUID
          │ name                │ VARCHAR(256)
          │ quantity            │ INTEGER
          │ assigned_to         │ UUID
          │ creation_date       │ TIMESTAMP
          │ update_date         │ TIMESTAMP
          └─────────────────────┘

          ┌─────────────────────┐
          │       EVENT         │
          └─────────────────────┘
                    │
                    │ 1:N
                    │
                    ▼
          ┌─────────────────────┐
          │   CONTRIBUTION      │
          ├─────────────────────┤
          │ identifier (PK)     │ UUID
          │ event_id (FK)       │ UUID
          │ participant_id (FK) │ UUID
          │ resource_id (FK)    │ UUID
          │ amount              │ DECIMAL
          │ creation_date       │ TIMESTAMP
          └─────────────────────┘
```

### 5.3.2 Modèle Physique de Données (MPD)

#### Table EVENT

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| `identifier` | UUID | PRIMARY KEY | Identifiant unique de l'événement |
| `name` | VARCHAR(256) | NOT NULL | Nom de l'événement |
| `description` | VARCHAR(256) | NOT NULL | Description détaillée |
| `event_date` | TIMESTAMP | NOT NULL | Date et heure de l'événement |
| `creator` | VARCHAR(256) | NOT NULL | Email de l'organisateur |
| `location` | VARCHAR(256) | NOT NULL | Lieu de l'événement |
| `type` | EVENT_TYPE | NOT NULL | Type d'événement (PARTY, BIRTHDAY, DINER, SNACK) |
| `creation_date` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Date de création de l'enregistrement |
| `update_date` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Date de dernière modification |
| `members` | UUID[] | | Liste des identifiants des membres |

**Index :**
- PRIMARY KEY sur `identifier`
- INDEX sur `creator` (pour les recherches par organisateur)
- INDEX sur `event_date` (pour les recherches par date)

#### Table PARTICIPANT

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| `identifier` | UUID | PRIMARY KEY | Identifiant unique du participant |
| `event_id` | UUID | FOREIGN KEY → EVENT(identifier), NOT NULL | Référence vers l'événement |
| `name` | VARCHAR(256) | NOT NULL | Nom du participant |
| `email` | VARCHAR(256) | NOT NULL | Email du participant |
| `status` | VARCHAR(50) | NOT NULL | Statut (INVITED, CONFIRMED, DECLINED) |
| `creation_date` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Date de création |
| `update_date` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Date de modification |

**Index :**
- PRIMARY KEY sur `identifier`
- FOREIGN KEY sur `event_id` → `EVENT(identifier)` ON DELETE CASCADE
- INDEX sur `event_id` (pour les jointures)
- UNIQUE sur `(event_id, email)` (un participant unique par email et événement)

#### Table RESOURCE

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| `identifier` | UUID | PRIMARY KEY | Identifiant unique de la ressource |
| `event_id` | UUID | FOREIGN KEY → EVENT(identifier), NOT NULL | Référence vers l'événement |
| `name` | VARCHAR(256) | NOT NULL | Nom de la ressource |
| `quantity` | INTEGER | NOT NULL, CHECK > 0 | Quantité requise |
| `assigned_to` | UUID | | Participant assigné (peut être NULL) |
| `creation_date` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Date de création |
| `update_date` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Date de modification |

**Index :**
- PRIMARY KEY sur `identifier`
- FOREIGN KEY sur `event_id` → `EVENT(identifier)` ON DELETE CASCADE
- INDEX sur `event_id`

#### Table CONTRIBUTION

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| `identifier` | UUID | PRIMARY KEY | Identifiant unique de la contribution |
| `event_id` | UUID | FOREIGN KEY → EVENT(identifier), NOT NULL | Référence vers l'événement |
| `participant_id` | UUID | FOREIGN KEY → PARTICIPANT(identifier), NOT NULL | Participant contributeur |
| `resource_id` | UUID | FOREIGN KEY → RESOURCE(identifier), NOT NULL | Ressource concernée |
| `amount` | DECIMAL(10,2) | NOT NULL, CHECK > 0 | Montant de la contribution |
| `creation_date` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Date de création |

**Index :**
- PRIMARY KEY sur `identifier`
- FOREIGN KEY sur `event_id`, `participant_id`, `resource_id`
- INDEX composé sur `(event_id, participant_id, resource_id)`

### 5.3.3 Enum EVENT_TYPE

Type énuméré PostgreSQL pour les types d'événements :

```sql
CREATE TYPE EVENT_TYPE AS ENUM (
  'PARTY',      -- Soirée/Fête
  'BIRTHDAY',   -- Anniversaire
  'DINER',      -- Dîner
  'SNACK'       -- Apéro/Goûter
);
```

### 5.3.4 Règles de gestion

#### Intégrité référentielle
1. La suppression d'un événement supprime en cascade tous ses participants, ressources et contributions
2. Un participant ne peut être créé que si l'événement existe
3. Une ressource ne peut être assignée qu'à un participant existant de l'événement
4. Une contribution doit référencer un événement, un participant et une ressource valides

#### Contraintes métier
1. Un événement doit avoir une date future lors de la création
2. Un participant est unique par email pour un événement donné
3. Le montant d'une contribution doit être strictement positif
4. La quantité d'une ressource doit être strictement positive

#### Règles de nommage
- Toutes les tables sont dans le schéma `configuration`
- Noms de tables en minuscules, au singulier
- Noms de colonnes en snake_case
- Identifiants nommés `identifier` (pas `id`) pour éviter les conflits
- Clés étrangères suffixées par `_id`

---

## 5.4 Script de base de données

### 5.4.1 Script d'initialisation (init-db.sql)

Ce script est exécuté automatiquement au démarrage du conteneur PostgreSQL en développement local.

```sql
-- Database initialization script for HappyRow Core local development
-- This script runs automatically when the PostgreSQL container starts

-- Create the configuration schema
CREATE SCHEMA IF NOT EXISTS configuration;

-- Create the EVENT_TYPE enum
DO $$ BEGIN
  CREATE TYPE EVENT_TYPE AS ENUM ('PARTY', 'BIRTHDAY', 'DINER', 'SNACK');
EXCEPTION
  WHEN duplicate_object THEN null;
END $$;

-- Grant necessary permissions to the user
GRANT ALL PRIVILEGES ON SCHEMA configuration TO happyrow_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA configuration TO happyrow_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA configuration TO happyrow_user;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA configuration GRANT ALL ON TABLES TO happyrow_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA configuration GRANT ALL ON SEQUENCES TO happyrow_user;

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Log completion
SELECT 'Database initialization completed successfully' AS status;
```

### 5.4.2 Initialisation automatique via Kotlin (DatabaseInitializer.kt)

Pour l'environnement de production (Render), l'initialisation de la base de données est effectuée automatiquement au démarrage de l'application via la classe `DatabaseInitializer`.

**Principe :**
- Création du schéma `configuration` si inexistant
- Création de l'enum `EVENT_TYPE` si inexistant
- Création automatique des tables via Exposed `SchemaUtils.create()`

**Extrait de code Kotlin :**

```kotlin
class DatabaseInitializer(private val dataSource: DataSource) {
  
  fun initializeDatabase() {
    logger.info("Starting database initialization...")
    
    transaction(Database.connect(dataSource)) {
      // Create schema
      exec("CREATE SCHEMA IF NOT EXISTS configuration")
      
      // Create enum type
      exec("""
        DO $$ BEGIN
          CREATE TYPE EVENT_TYPE AS ENUM ('PARTY', 'BIRTHDAY', 'DINER', 'SNACK');
        EXCEPTION
          WHEN duplicate_object THEN null;
        END $$;
      """)
      
      // Create tables using Exposed SchemaUtils
      SchemaUtils.create(
        EventTable,
        ParticipantTable,
        ResourceTable,
        ContributionTable
      )
      
      logger.info("Database initialization completed successfully")
    }
  }
}
```

### 5.4.3 Définition des tables avec Exposed ORM

#### EventTable

```kotlin
object EventTable : UUIDTable("configuration.event", "identifier") {
  val name: Column<String> = varchar("name", 256)
  val description: Column<String> = varchar("description", 256)
  val eventDate = timestamp("event_date")
  val creator = varchar("creator", 256)
  val location: Column<String> = varchar("location", 256)
  val type: Column<EventType> = customEnumeration(
    name = "type",
    sql = "EVENT_TYPE",
    fromDb = { value -> (value as String).toEventType().getOrElse { throw it } },
    toDb = { PGEnum("EVENT_TYPE", it) }
  )
  val creationDate = timestamp("creation_date")
  val updateDate = timestamp("update_date")
  val members = array("members", UUIDColumnType(), UUID::class) { 
    UUID.fromString(it.toString()) 
  }
}
```

**Points clés :**
- Utilisation de `UUIDTable` pour un identifiant UUID auto-généré
- Mapping de l'enum PostgreSQL vers l'enum Kotlin `EventType`
- Type array pour la colonne `members` (liste d'UUIDs)
- Tous les champs requis définis avec des types Kotlin appropriés

#### ParticipantTable

```kotlin
object ParticipantTable : UUIDTable("configuration.participant", "identifier") {
  val eventId = uuid("event_id").references(EventTable.id, onDelete = ReferenceOption.CASCADE)
  val name = varchar("name", 256)
  val email = varchar("email", 256)
  val status = varchar("status", 50)
  val creationDate = timestamp("creation_date")
  val updateDate = timestamp("update_date")
  
  init {
    uniqueIndex("uk_participant_event_email", eventId, email)
  }
}
```

**Points clés :**
- Clé étrangère vers `EventTable` avec suppression en cascade
- Contrainte d'unicité composite sur `(eventId, email)`
- Gestion automatique des timestamps

### 5.4.4 Gestion des migrations

**Stratégie actuelle :**
- Utilisation de `SchemaUtils.create()` pour la création initiale des tables
- En développement : reconstruction complète de la DB via Docker
- En production : Initialisation automatique au premier déploiement

**Évolutions prévues :**
- Intégration de **Flyway** ou **Liquibase** pour les migrations versionnées
- Scripts de migration incrémentaux pour les mises à jour de schéma
- Historique des migrations dans une table dédiée

---

## 5.5 Diagrammes de comportement

### 5.5.1 Diagramme de cas d'utilisation

```
                    ┌─────────────────┐
                    │  Organisateur   │
                    └────────┬────────┘
                             │
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         │                   │                   │
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ Créer un        │ │ Gérer les       │ │ Gérer les       │
│ événement       │ │ participants    │ │ ressources      │
└─────────────────┘ └─────────────────┘ └─────────────────┘
         │                   │                   │
         │                   │                   │
         │  ┌────────────────┼────────────────┐  │
         │  │                │                │  │
         ▼  ▼                ▼                ▼  ▼
┌─────────────────────────────────────────────────────┐
│                                                     │
│         Système HappyRow Core (API REST)            │
│                                                     │
│  - Valider les données                              │
│  - Persister en base de données                     │
│  - Notifier les participants (futur)                │
│  - Gérer les contributions                          │
│                                                     │
└─────────────────────────────────────────────────────┘


                    ┌─────────────────┐
                    │   Participant   │
                    └────────┬────────┘
                             │
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         │                   │                   │
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ Consulter       │ │ Confirmer sa    │ │ Ajouter une     │
│ l'événement     │ │ participation   │ │ contribution    │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

### 5.5.2 Diagramme de séquence - Création d'un événement

```
Organisateur    API REST     CreateEventUseCase    EventRepository    Database
    │              │                 │                   │               │
    │              │                 │                   │               │
    │  POST /events│                 │                   │               │
    ├─────────────>│                 │                   │               │
    │              │                 │                   │               │
    │              │ validate DTO    │                   │               │
    │              ├─────────┐       │                   │               │
    │              │         │       │                   │               │
    │              │<────────┘       │                   │               │
    │              │                 │                   │               │
    │              │ toRequest()     │                   │               │
    │              ├─────────┐       │                   │               │
    │              │         │       │                   │               │
    │              │<────────┘       │                   │               │
    │              │                 │                   │               │
    │              │   execute()     │                   │               │
    │              ├────────────────>│                   │               │
    │              │                 │                   │               │
    │              │                 │ validate business │               │
    │              │                 │ rules             │               │
    │              │                 ├──────────┐        │               │
    │              │                 │          │        │               │
    │              │                 │<─────────┘        │               │
    │              │                 │                   │               │
    │              │                 │   create()        │               │
    │              │                 ├──────────────────>│               │
    │              │                 │                   │               │
    │              │                 │                   │ INSERT INTO   │
    │              │                 │                   ├──────────────>│
    │              │                 │                   │               │
    │              │                 │                   │   Event       │
    │              │                 │                   │<──────────────┤
    │              │                 │                   │               │
    │              │                 │  Event            │               │
    │              │                 │<──────────────────┤               │
    │              │                 │                   │               │
    │              │   Event         │                   │               │
    │              │<────────────────┤                   │               │
    │              │                 │                   │               │
    │  201 Created │                 │                   │               │
    │  + EventDto  │                 │                   │               │
    │<─────────────┤                 │                   │               │
    │              │                 │                   │               │
```

**Description détaillée du flux :**

1. **Réception de la requête** : L'organisateur envoie une requête HTTP POST avec les données de l'événement
2. **Validation DTO** : La couche API valide le format et les types de données (DTO)
3. **Conversion** : Le DTO est converti en objet du domain (`CreateEventRequest`)
4. **Exécution du Use Case** : Le `CreateEventUseCase` est invoqué
5. **Validation métier** : Vérification des règles métier (ex: date dans le futur)
6. **Persistance** : L'événement est sauvegardé en base via le `EventRepository`
7. **Retour** : L'événement créé est renvoyé au client avec un code 201

### 5.5.3 Diagramme de séquence - Récupération des événements d'un organisateur

```
Organisateur    API REST    GetEventsByOrganizerUseCase    EventRepository    Database
    │              │                    │                        │               │
    │              │                    │                        │               │
    │ GET /events? │                    │                        │               │
    │ organizerId= │                    │                        │               │
    │    xxx       │                    │                        │               │
    ├─────────────>│                    │                        │               │
    │              │                    │                        │               │
    │              │ validate params    │                        │               │
    │              ├─────────┐          │                        │               │
    │              │         │          │                        │               │
    │              │<────────┘          │                        │               │
    │              │                    │                        │               │
    │              │    execute()       │                        │               │
    │              ├───────────────────>│                        │               │
    │              │                    │                        │               │
    │              │                    │  findByOrganizer()     │               │
    │              │                    ├───────────────────────>│               │
    │              │                    │                        │               │
    │              │                    │                        │ SELECT * FROM │
    │              │                    │                        │ event WHERE   │
    │              │                    │                        │ creator = ?   │
    │              │                    │                        ├──────────────>│
    │              │                    │                        │               │
    │              │                    │                        │ List<EventRow>│
    │              │                    │                        │<──────────────┤
    │              │                    │                        │               │
    │              │                    │   List<Event>          │               │
    │              │                    │<───────────────────────┤               │
    │              │                    │                        │               │
    │              │   List<Event>      │                        │               │
    │              │<───────────────────┤                        │               │
    │              │                    │                        │               │
    │  200 OK      │                    │                        │               │
    │ + List<Dto>  │                    │                        │               │
    │<─────────────┤                    │                        │               │
    │              │                    │                        │               │
```

### 5.5.4 Diagramme d'activité - Mise à jour d'un événement

```
             ┌──────────────────┐
             │      Début       │
             └────────┬─────────┘
                      │
                      ▼
         ┌────────────────────────┐
         │ Recevoir PUT request   │
         │ avec eventId et data   │
         └────────┬───────────────┘
                  │
                  ▼
         ┌────────────────────────┐
         │  Valider les données   │
         │      d'entrée          │
         └────┬──────────┬────────┘
              │          │
       Valide │          │ Invalide
              │          │
              ▼          ▼
    ┌─────────────┐  ┌────────────┐
    │             │  │  Retourner │
    │             │  │  400 Bad   │
    │             │  │  Request   │
    │             │  └──────┬─────┘
    │             │         │
    │             │         ▼
    │             │    ┌────────┐
    │             │    │  Fin   │
    │             │    └────────┘
    │             │
    ▼             │
┌──────────────────┐
│ Rechercher       │
│ l'événement par  │
│ eventId          │
└────┬──────┬──────┘
     │      │
 Trouvé    Pas trouvé
     │      │
     │      ▼
     │   ┌────────────┐
     │   │  Retourner │
     │   │  404 Not   │
     │   │  Found     │
     │   └──────┬─────┘
     │          │
     │          ▼
     │     ┌────────┐
     │     │  Fin   │
     │     └────────┘
     │
     ▼
┌──────────────────┐
│ Vérifier les     │
│ règles métier    │
└────┬──────┬──────┘
     │      │
Valide     Invalide
     │      │
     │      ▼
     │   ┌────────────┐
     │   │  Retourner │
     │   │  400 Bad   │
     │   │  Request   │
     │   └──────┬─────┘
     │          │
     │          ▼
     │     ┌────────┐
     │     │  Fin   │
     │     └────────┘
     │
     ▼
┌──────────────────┐
│ Mettre à jour    │
│ l'événement      │
│ en base          │
└────┬─────────────┘
     │
     ▼
┌──────────────────┐
│ Mettre à jour    │
│ le timestamp     │
│ update_date      │
└────┬─────────────┘
     │
     ▼
┌──────────────────┐
│ Retourner 200 OK │
│ avec événement   │
│ mis à jour       │
└────┬─────────────┘
     │
     ▼
┌──────────────────┐
│       Fin        │
└──────────────────┘
```

### 5.5.5 Diagramme de classes (Domain)

```
┌──────────────────────────────────────┐
│           <<Entity>>                 │
│            Event                     │
├──────────────────────────────────────┤
│ - identifier: UUID                   │
│ - name: String                       │
│ - description: String                │
│ - eventDate: Instant                 │
│ - creationDate: Instant              │
│ - updateDate: Instant                │
│ - creator: Creator                   │
│ - location: String                   │
│ - type: EventType                    │
│ - members: List<Creator>             │
└──────────────────────────────────────┘
                │
                │ composition
                │
                ▼
┌──────────────────────────────────────┐
│        <<Value Object>>              │
│           Creator                    │
├──────────────────────────────────────┤
│ - identifier: String                 │
└──────────────────────────────────────┘


┌──────────────────────────────────────┐
│           <<Enum>>                   │
│          EventType                   │
├──────────────────────────────────────┤
│ PARTY                                │
│ BIRTHDAY                             │
│ DINER                                │
│ SNACK                                │
└──────────────────────────────────────┘


┌──────────────────────────────────────┐
│        <<Use Case>>                  │
│      CreateEventUseCase              │
├──────────────────────────────────────┤
│ - eventRepository: EventRepository   │
├──────────────────────────────────────┤
│ + execute(request): Event            │
└──────────────────────────────────────┘
                │
                │ depends on
                │
                ▼
┌──────────────────────────────────────┐
│         <<Interface>>                │
│       EventRepository                │
├──────────────────────────────────────┤
│ + create(event): Event               │
│ + findById(id): Event?               │
│ + findByOrganizer(id): List<Event>   │
│ + update(event): Event               │
│ + delete(id): Unit                   │
└──────────────────────────────────────┘
```

---

## Conclusion de la section 5

Cette section présente les spécifications fonctionnelles complètes du projet HappyRow Core :

✅ **Architecture multicouche sécurisée** avec séparation Domain/Infrastructure  
✅ **API REST** bien définie avec 15+ endpoints  
✅ **Modèle de données relationnel** normalisé avec 4 tables principales  
✅ **Scripts d'initialisation** automatiques pour le déploiement  
✅ **Diagrammes de comportement** détaillant les cas d'utilisation principaux  

Les choix techniques (Ktor, Kotlin, PostgreSQL, Exposed ORM) sont justifiés et alignés avec les bonnes pratiques de développement moderne et les recommandations de sécurité de l'ANSSI.

L'architecture mise en place permet :
- Une évolutivité facilitée grâce à la séparation des couches
- Une maintenabilité optimale avec des responsabilités claires
- Une sécurité renforcée à chaque niveau de l'application
- Une testabilité complète de tous les composants
