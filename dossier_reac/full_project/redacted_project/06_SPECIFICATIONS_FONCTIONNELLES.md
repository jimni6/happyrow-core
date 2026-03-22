# 6. Spécifications fonctionnelles

## 6.1 Contraintes du projet et livrables attendus

### Contraintes

| Contrainte | Description |
|---|---|
| **Langage** | Kotlin sur JVM 21 — typage fort, null-safety, interopérabilité Java |
| **Framework** | Ktor 3.2.2 — framework web asynchrone non-bloquant |
| **Base de données** | PostgreSQL — SGBD relationnel avec support des enums, UUID, arrays |
| **Authentification** | Déléguée à Supabase (JWT HMAC256) — pas de gestion de mots de passe côté serveur |
| **Conteneurisation** | Docker obligatoire pour le déploiement |
| **Architecture** | Hexagonale imposée — séparation stricte domaine / infrastructure |
| **Qualité** | Analyse statique Detekt obligatoire dans le pipeline CI/CD |

### Livrables

1. API REST avec 15 endpoints couvrant les 4 domaines (Event, Participant, Resource, Contribution)
2. Base de données PostgreSQL avec schéma, contraintes et indexes
3. Pipeline CI/CD GitHub Actions (Detekt → Tests → Deploy)
4. Dockerfile multi-stage pour le déploiement
5. Configuration Render pour l'hébergement cloud
6. Documentation technique

## 6.2 Architecture logicielle du projet

### Architecture hexagonale (Ports & Adapters)

L'application suit une architecture hexagonale, organisée en deux modules Gradle indépendants :

```
happyrow-core/
├── domain/          ← Logique métier pure (aucune dépendance framework)
│   └── src/main/kotlin/
│       └── com/happyrow/core/domain/
│           ├── event/          ← Bounded context Événement
│           ├── participant/    ← Bounded context Participant
│           ├── resource/       ← Bounded context Ressource
│           └── contribution/   ← Bounded context Contribution
│
├── infrastructure/  ← Adapters techniques
│   └── src/main/kotlin/
│       └── com/happyrow/core/infrastructure/
│           ├── event/          ← Driving (endpoints) + Driven (SQL)
│           ├── participant/
│           ├── resource/
│           ├── contribution/
│           ├── technical/      ← Auth JWT, config DB, Jackson, Ktor
│           └── common/         ← Erreurs partagées
│
└── src/             ← Point d'entrée (Application.kt, Routing, Koin modules)
```

### Diagramme de composants

```mermaid
graph TB
    subgraph client [Client]
        Frontend["Front-end web<br/>(happyrow-front)"]
    end

    subgraph api [API REST - Ktor]
        Auth["JwtAuthenticationPlugin"]
        Routing["Routing"]
    end

    subgraph driving [Adapters Driving - Endpoints]
        EventEP["EventEndpoints"]
        ParticipantEP["ParticipantEndpoints"]
        ResourceEP["ResourceEndpoints"]
        ContributionEP["ContributionEndpoints"]
    end

    subgraph domainLayer [Domain - Use Cases]
        CreateEvent["CreateEventUseCase"]
        GetEvents["GetEventsByOrganizerUseCase"]
        UpdateEvent["UpdateEventUseCase"]
        DeleteEvent["DeleteEventUseCase"]
        CreateParticipant["CreateParticipantUseCase"]
        GetParticipants["GetParticipantsByEventUseCase"]
        UpdateParticipant["UpdateParticipantUseCase"]
        CreateResource["CreateResourceUseCase"]
        GetResources["GetResourcesByEventUseCase"]
        AddContribution["AddContributionUseCase"]
        ReduceContribution["ReduceContributionUseCase"]
        DeleteContribution["DeleteContributionUseCase"]
    end

    subgraph driven [Adapters Driven - Repositories SQL]
        SqlEvent["SqlEventRepository"]
        SqlParticipant["SqlParticipantRepository"]
        SqlResource["SqlResourceRepository"]
        SqlContribution["SqlContributionRepository"]
    end

    subgraph db [PostgreSQL]
        EventTbl["event"]
        ParticipantTbl["participant"]
        ResourceTbl["resource"]
        ContributionTbl["contribution"]
    end

    Frontend -->|"HTTP + JWT"| Auth
    Auth --> Routing
    Routing --> driving
    driving --> domainLayer
    domainLayer -->|"Ports (interfaces)"| driven
    driven --> db
```

### Flux global simplifié

```mermaid
graph LR
    Client["🖥️ Client<br/>(happyrow-front)"]
    API["🔐 API REST<br/>Ktor + JWT"]
    Driving["📡 Driving<br/>Endpoints"]
    Domain["⚙️ Domain<br/>Use Cases"]
    Driven["💾 Driven<br/>Repositories"]
    DB["🐘 PostgreSQL"]

    Client -->|"HTTP + JWT"| API
    API --> Driving
    Driving --> Domain
    Domain -->|"Ports<br/>(interfaces)"| Driven
    Driven --> DB
```

### Détail par couche

```mermaid
graph LR
    subgraph Driving ["Adapters Driving — Endpoints"]
        E1["EventEndpoints"]
        E2["ParticipantEndpoints"]
        E3["ResourceEndpoints"]
        E4["ContributionEndpoints"]
    end

    subgraph Domain ["Domain — Use Cases"]
        direction TB
        D1["CreateEvent"]
        D2["GetEventsByOrganizer"]
        D3["UpdateEvent"]
        D4["DeleteEvent"]
        D5["CreateParticipant"]
        D6["GetParticipantsByEvent"]
        D7["UpdateParticipant"]
        D8["CreateResource"]
        D9["GetResourcesByEvent"]
        D10["AddContribution"]
        D11["ReduceContribution"]
        D12["DeleteContribution"]
    end

    subgraph Driven ["Adapters Driven — Repositories SQL"]
        R1["SqlEventRepository"]
        R2["SqlParticipantRepository"]
        R3["SqlResourceRepository"]
        R4["SqlContributionRepository"]
    end

    E1 --> D1 & D2 & D3 & D4
    E2 --> D5 & D6 & D7
    E3 --> D8 & D9
    E4 --> D10 & D11 & D12

    D1 & D2 & D3 & D4 --> R1
    D5 & D6 & D7 --> R2
    D8 & D9 --> R3
    D10 & D11 & D12 --> R4
```

### Principe de dépendance

Le module `domain` ne dépend d'aucune bibliothèque technique. Il définit des **ports** (interfaces de repository) que le module `infrastructure` implémente. Cette inversion de dépendance garantit que la logique métier est testable indépendamment et ne change pas si l'on remplace PostgreSQL par un autre SGBD.

## 6.3 Maquettes et enchaînement des écrans

L'application HappyRow est une **SPA (Single Page Application)** React. La navigation repose sur React Router DOM et un rendu conditionnel basé sur l'état d'authentification. Les maquettes Figma originales sont dans `design-figma/` du repo `happyrow-front`, avec une charte basée sur des Design Tokens CSS (teal `#5FBDB4`, navy `#3D5A6C`, coral `#E6A19A`, police Comic Neue).

### Enchaînement des écrans

```mermaid
flowchart TB
    Start([Lancement App]) --> AuthCheck{Utilisateur authentifié ?}

    AuthCheck -->|Non| Welcome[WelcomeView]
    AuthCheck -->|Oui| Home[HomePage - Dashboard]

    Welcome -->|Clic Create Account| RegisterModal[RegisterModal]
    Welcome -->|Clic Login| LoginModal[LoginModal]

    RegisterModal -->|Inscription réussie| LoginModal
    LoginModal -->|Connexion réussie| Home

    Home -->|Clic EventCard| EventDetails[EventDetailsView]
    Home -->|Bouton +| CreateEvent[CreateEventForm Modal]
    Home -->|Icône participant| AddParticipant[AddParticipantModal]
    Home -->|Navbar Profile| Profile[UserProfilePage]

    CreateEvent -->|Événement créé| Home
    EventDetails -->|Bouton retour| Home
    EventDetails -->|Bouton Edit| EditEvent[UpdateEventForm Modal]
    EventDetails -->|Bouton Delete| DeleteConfirm[ConfirmDeleteModal]
    EventDetails -->|+/- Validate| ContributionAPI["Appel API Contribution"]
    ContributionAPI --> EventDetails

    Profile -->|Sign Out| Welcome
```

### Écrans principaux

| Écran | Rôle | Composants clés |
|-------|------|-----------------|
| **WelcomeView** | Page d'accueil non authentifié | Logo, tagline, boutons Login / Create Account |
| **LoginModal** | Connexion email/mot de passe (Supabase) | Champs email/password, toggle visibilité, validation, animation fermeture 400ms |
| **RegisterModal** | Inscription nouvel utilisateur | Champs prénom, nom, email, mot de passe, confirmation |
| **HomePage** | Dashboard — liste des événements | `EventCard` (date, nom, participants, localisation), `AppNavbar` (Home, Profile, "+") |
| **EventDetailsView** | Détail complet d'un événement | Header avec retour/edit, `ResourceCategorySection` (Food/Drinks), `ResourceItem` (+/- et Validate), ajout ressource inline, bouton Delete Event |
| **CreateEventForm** | Formulaire de création d'événement | Nom (min 3 car.), description, date/heure (futur), lieu, type (Party, Birthday, Diner, Snack) |
| **UserProfilePage** | Profil et déconnexion | Avatar, nom, email, bouton Sign Out |

## 6.4 Modèle entités-associations et modèle physique

### Modèle Conceptuel de Données (MCD)

Le MCD ci-dessous représente les entités métier de l'application HappyRow et les associations qui les relient. On y distingue quatre entités principales — **Event**, **Participant**, **Resource** et **Contribution** — ainsi que leurs cardinalités : un événement héberge plusieurs participants et nécessite plusieurs ressources, tandis qu'une contribution fait le lien entre un participant et une ressource.

![Modèle Conceptuel de Données](../merise/mcd.png)

### Modèle Logique de Données (MLD)

Le MLD traduit le modèle conceptuel en un schéma relationnel. Les associations sont matérialisées par des clés étrangères : `event_id` dans les tables **Participant** et **Resource**, et `participant_id` / `resource_id` dans la table **Contribution**. Ce modèle met en évidence les contraintes d'intégrité référentielle et les index uniques composites qui garantissent la cohérence des données (un participant unique par événement, une contribution unique par couple participant-ressource).

![Modèle Logique de Données](../merise/mld.png)

### Modèle Physique de Données (MPD)

Le MPD détaille l'implémentation physique dans PostgreSQL, au sein du schéma `configuration`. Il précise les types de colonnes (UUID, VARCHAR, TIMESTAMP, INTEGER), les contraintes (PRIMARY KEY, NOT NULL, CHECK, DEFAULT), le type énuméré `EVENT_TYPE` (PARTY, BIRTHDAY, DINER, SNACK), ainsi que les index de performance. Le champ `version` de la table **Resource** implémente un mécanisme de verrou optimiste pour gérer les accès concurrents lors des mises à jour de contributions.

![Modèle Physique de Données](../merise/mpd.png)

### Script de création de la base de données

```sql
-- init-db.sql — Initialisation du schéma et des types
CREATE SCHEMA IF NOT EXISTS configuration;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TYPE EVENT_TYPE AS ENUM ('PARTY', 'BIRTHDAY', 'DINER', 'SNACK');
GRANT ALL PRIVILEGES ON SCHEMA configuration TO happyrow_user;

-- Table EVENT
CREATE TABLE configuration.event (
  identifier  UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
  name        VARCHAR(256) NOT NULL,
  description TEXT         NOT NULL,
  event_date  TIMESTAMP    NOT NULL,
  creator     VARCHAR(256) NOT NULL,
  location    VARCHAR(256) NOT NULL,
  type        EVENT_TYPE   NOT NULL,
  creation_date TIMESTAMP  NOT NULL,
  update_date   TIMESTAMP  NOT NULL,
  members     UUID[]
);

-- Table PARTICIPANT
CREATE TABLE configuration.participant (
  id          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_email  VARCHAR(255) NOT NULL,
  event_id    UUID         NOT NULL REFERENCES configuration.event(identifier),
  status      VARCHAR(50)  NOT NULL DEFAULT 'CONFIRMED',
  joined_at   TIMESTAMP    NOT NULL,
  created_at  TIMESTAMP    NOT NULL,
  updated_at  TIMESTAMP    NOT NULL
);
CREATE UNIQUE INDEX uq_participant_user_event ON configuration.participant(user_email, event_id);
CREATE INDEX idx_participant_user ON configuration.participant(user_email);
CREATE INDEX idx_participant_event ON configuration.participant(event_id);

-- Table RESOURCE
CREATE TABLE configuration.resource (
  id                 UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
  name               VARCHAR(255) NOT NULL,
  category           VARCHAR(50)  NOT NULL,
  suggested_quantity INTEGER      NOT NULL DEFAULT 0,
  current_quantity   INTEGER      NOT NULL DEFAULT 0,
  event_id           UUID         NOT NULL REFERENCES configuration.event(identifier),
  version            INTEGER      NOT NULL DEFAULT 1,
  created_at         TIMESTAMP    NOT NULL,
  updated_at         TIMESTAMP    NOT NULL
);
CREATE INDEX idx_resource_event ON configuration.resource(event_id);

-- Table CONTRIBUTION
CREATE TABLE configuration.contribution (
  id              UUID      PRIMARY KEY DEFAULT uuid_generate_v4(),
  participant_id  UUID      NOT NULL REFERENCES configuration.participant(id),
  resource_id     UUID      NOT NULL REFERENCES configuration.resource(id),
  quantity        INTEGER   NOT NULL CHECK (quantity > 0),
  created_at      TIMESTAMP NOT NULL,
  updated_at      TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX uq_contribution_participant_resource
  ON configuration.contribution(participant_id, resource_id);
CREATE INDEX idx_contribution_participant ON configuration.contribution(participant_id);
CREATE INDEX idx_contribution_resource ON configuration.contribution(resource_id);
```

En pratique, les tables sont créées automatiquement par Exposed ORM au démarrage de l'application via `SchemaUtils.createMissingTablesAndColumns`. Le script ci-dessus représente le SQL équivalent généré.

## 6.5 Diagramme des cas d'utilisation

```mermaid
graph TB
    subgraph actors [Acteurs]
        User["Utilisateur authentifié"]
    end

    subgraph ucEvents [Événements]
        UC1["Créer un événement"]
        UC2["Consulter ses événements"]
        UC3["Modifier un événement"]
        UC4["Supprimer un événement"]
    end

    subgraph ucParticipants [Participants]
        UC5["Ajouter un participant"]
        UC6["Consulter les participants"]
        UC7["Modifier le statut"]
    end

    subgraph ucResources [Ressources]
        UC8["Créer une ressource"]
        UC9["Consulter les ressources"]
    end

    subgraph ucContributions [Contributions]
        UC10["Ajouter une contribution"]
        UC11["Réduire une contribution"]
        UC12["Supprimer une contribution"]
    end

    User --> UC1
    User --> UC2
    User --> UC3
    User --> UC4
    User --> UC5
    User --> UC6
    User --> UC7
    User --> UC8
    User --> UC9
    User --> UC10
    User --> UC11
    User --> UC12

    UC1 -.->|"inclut"| UC5
    UC4 -.->|"cascade"| UC12
```

### Version compacte (orientation horizontale)

```mermaid
graph LR
    User(["👤 Utilisateur authentifié"])

    subgraph Événements
        direction TB
        UC1["Créer"]
        UC2["Consulter"]
        UC3["Modifier"]
        UC4["Supprimer"]
    end

    subgraph Participants
        direction TB
        UC5["Ajouter"]
        UC6["Consulter"]
        UC7["Modifier statut"]
    end

    subgraph Ressources
        direction TB
        UC8["Créer"]
        UC9["Consulter"]
    end

    subgraph Contributions
        direction TB
        UC10["Ajouter"]
        UC11["Réduire"]
        UC12["Supprimer"]
    end

    User --> Événements
    User --> Participants
    User --> Ressources
    User --> Contributions

    UC1 -.->|"inclut"| UC5
    UC4 -.->|"cascade"| UC12
```

**Règles métier notables :**

- **UC1 → UC5** : La création d'un événement inclut automatiquement l'ajout du créateur comme participant confirmé.
- **UC4 → cascade** : La suppression d'un événement supprime en cascade tous les participants, ressources et contributions.
- **UC10** : L'ajout d'une contribution met à jour la quantité courante de la ressource via un verrou optimiste (protection contre les accès concurrents).

## 6.6 Diagrammes de séquence

### Séquence 1 : Création d'un événement (avec auto-ajout du créateur)

```mermaid
sequenceDiagram
    participant Client as Front-end
    participant Auth as JwtAuthPlugin
    participant EP as CreateEventEndpoint
    participant UC as CreateEventUseCase
    participant ER as EventRepository
    participant PR as ParticipantRepository
    participant DB as PostgreSQL

    Client->>Auth: POST /events (JWT + body)
    Auth->>Auth: Valider le token JWT
    Auth->>EP: Requête authentifiée
    EP->>EP: Désérialiser CreateEventRequestDto
    EP->>EP: toDomain(user.email)
    EP->>UC: create(CreateEventRequest)
    UC->>ER: create(request)
    ER->>DB: INSERT INTO event
    DB-->>ER: Event créé
    ER-->>UC: Either.Right(Event)
    UC->>PR: create(CreateParticipantRequest)
    Note over UC,PR: Auto-ajout du créateur<br/>status = CONFIRMED
    PR->>DB: INSERT INTO participant
    DB-->>PR: Participant créé
    PR-->>UC: Either.Right(Participant)
    UC-->>EP: Either.Right(Event)
    EP->>EP: toDto()
    EP-->>Client: 201 Created + EventDto
```

### Séquence 2 : Ajout d'une contribution (avec verrou optimiste)

```mermaid
sequenceDiagram
    participant Client as Front-end
    participant EP as AddContributionEndpoint
    participant UC as AddContributionUseCase
    participant CR as SqlContributionRepository
    participant PR as ParticipantRepository
    participant RR as ResourceRepository
    participant DB as PostgreSQL

    Client->>EP: POST /events/{id}/resources/{rid}/contributions
    EP->>EP: authenticatedUser() + receive body
    EP->>UC: execute(AddContributionRequest)
    UC->>CR: addOrUpdate(request)
    CR->>PR: findOrCreate(userEmail, eventId)
    PR->>DB: SELECT participant / INSERT si absent
    DB-->>PR: Participant
    PR-->>CR: Either.Right(Participant)
    CR->>DB: SELECT contribution WHERE participant_id AND resource_id
    alt Contribution existante
        CR->>DB: UPDATE contribution SET quantity
        CR->>RR: find(resourceId)
        RR->>DB: SELECT resource
        DB-->>RR: Resource (version = N)
        CR->>RR: updateQuantity(resourceId, delta, version=N)
        RR->>DB: UPDATE resource SET current_quantity, version=N+1 WHERE version=N
        alt Version OK
            DB-->>RR: 1 row updated
            RR-->>CR: Either.Right(Resource)
        else Version changée (accès concurrent)
            DB-->>RR: 0 rows updated
            RR-->>CR: Either.Left(OptimisticLockException)
            CR-->>UC: Either.Left(error)
            UC-->>EP: Either.Left(AddContributionException)
            EP-->>Client: 409 Conflict OPTIMISTIC_LOCK_FAILURE
        end
    else Nouvelle contribution
        CR->>DB: INSERT INTO contribution
        CR->>RR: updateQuantity(resourceId, quantity, version)
        RR->>DB: UPDATE resource WHERE version=N
        DB-->>RR: Resource updated
    end
    CR-->>UC: Either.Right(Contribution)
    UC-->>EP: Either.Right(Contribution)
    EP-->>Client: 200 OK + ContributionDto
```

Ce diagramme illustre le mécanisme central du verrou optimiste : la mise à jour de la quantité d'une ressource n'est acceptée que si la version en base n'a pas changé depuis la lecture. En cas de conflit (deux utilisateurs contribuant simultanément), le second reçoit une erreur 409 et doit réessayer.
