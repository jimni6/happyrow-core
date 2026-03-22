# 6. Spécifications fonctionnelles

## 6.1 Contraintes du projet et livrables attendus

### Contraintes

Voici les contraintes techniques dans lesquelles j'ai travaillé :

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

J'ai produit les livrables suivants :

1. API REST avec 15 endpoints couvrant les 4 domaines (Event, Participant, Resource, Contribution)
2. Base de données PostgreSQL avec schéma, contraintes et indexes
3. Pipeline CI/CD GitHub Actions (Detekt → Tests → Deploy)
4. Dockerfile multi-stage pour le déploiement
5. Configuration Render pour l'hébergement cloud
6. Documentation technique

## 6.2 Architecture logicielle du projet

### Architecture hexagonale (Ports & Adapters)

J'ai structuré l'application selon une architecture hexagonale, répartie en deux modules Gradle indépendants :

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

Le diagramme suivant montre comment les différentes couches communiquent entre elles :

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

Pour résumer, voici le chemin que parcourt une requête dans l'application :

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

Ce diagramme montre plus précisément quels endpoints appellent quels use cases, et quels repositories sont sollicités :

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

Le point important, c'est que le module `domain` ne dépend d'aucune bibliothèque technique. Il définit des **ports** (interfaces de repository) que le module `infrastructure` implémente. Grâce à cette inversion de dépendance, la logique métier reste testable de manière isolée et ne serait pas impactée si je décidais de remplacer PostgreSQL par un autre SGBD.

## 6.3 Maquettes et enchaînement des écrans

L'application HappyRow est une **SPA (Single Page Application)** React. J'ai construit la navigation avec React Router DOM et un rendu conditionnel basé sur l'état d'authentification. Les maquettes Figma originales se trouvent dans `design-figma/` du repo `happyrow-front`, avec une charte graphique basée sur des Design Tokens CSS (teal `#5FBDB4`, navy `#3D5A6C`, coral `#E6A19A`, police Comic Neue).

### Enchaînement des écrans

Voici comment les écrans s'enchaînent dans l'application :

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

J'ai conçu le MCD autour de quatre entités principales — **Event**, **Participant**, **Resource** et **Contribution**. Un événement héberge plusieurs participants et nécessite plusieurs ressources. La contribution fait le lien entre un participant et une ressource : c'est elle qui matérialise le fait qu'un participant apporte quelque chose.

![Modèle Conceptuel de Données](merise/mcd.png)

### Modèle Logique de Données (MLD)

J'ai ensuite traduit ce modèle conceptuel en schéma relationnel. Les associations sont matérialisées par des clés étrangères : `event_id` dans les tables **Participant** et **Resource**, et `participant_id` / `resource_id` dans la table **Contribution**. J'ai mis en place des contraintes d'intégrité référentielle et des index uniques composites pour garantir la cohérence des données (un participant unique par événement, une contribution unique par couple participant-ressource).

![Modèle Logique de Données](merise/mld.png)

### Modèle Physique de Données (MPD)

Le MPD correspond à l'implémentation réelle dans PostgreSQL, dans le schéma `configuration`. J'y ai défini les types de colonnes (UUID, VARCHAR, TIMESTAMP, INTEGER), les contraintes (PRIMARY KEY, NOT NULL, CHECK, DEFAULT), un type énuméré `EVENT_TYPE` (PARTY, BIRTHDAY, DINER, SNACK), ainsi que les index pour la performance. J'ai aussi ajouté un champ `version` à la table **Resource** pour implémenter un verrou optimiste qui gère les accès concurrents lors des contributions.

![Modèle Physique de Données](merise/mpd.png)

### Script de création de la base de données

Voici le script SQL qui correspond au schéma que j'ai mis en place :

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

J'ai identifié 12 cas d'utilisation, répartis en 4 domaines fonctionnels. Voici le diagramme UML des cas d'utilisation :

```plantuml
@startuml
left to right direction
skinparam packageStyle rectangle

actor "Utilisateur authentifié" as User

rectangle "HappyRow — Gestion d'événements" {

  package "Événements" {
    usecase "Créer un événement" as UC1
    usecase "Consulter ses événements" as UC2
    usecase "Modifier un événement" as UC3
    usecase "Supprimer un événement" as UC4
  }

  package "Participants" {
    usecase "Ajouter un participant" as UC5
    usecase "Consulter les participants" as UC6
    usecase "Modifier le statut d'un participant" as UC7
  }

  package "Ressources" {
    usecase "Créer une ressource" as UC8
    usecase "Consulter les ressources" as UC9
  }

  package "Contributions" {
    usecase "Ajouter une contribution" as UC10
    usecase "Réduire une contribution" as UC11
    usecase "Supprimer une contribution" as UC12
  }
}

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

UC1 ..> UC5 : <<include>>
UC4 ..> UC12 : <<include>>
UC10 ..> UC9 : <<include>>
@enduml
```

**Quelques règles métier importantes à noter :**

- **UC1 → UC5 (include)** : quand je crée un événement, le créateur est automatiquement ajouté comme participant confirmé. C'est une décision que j'ai prise pour simplifier le parcours utilisateur.
- **UC4 → UC12 (include)** : la suppression d'un événement entraîne la suppression en cascade de tous les participants, ressources et contributions associés.
- **UC10 → UC9 (include)** : l'ajout d'une contribution nécessite de consulter la ressource pour vérifier sa version (verrou optimiste) avant de mettre à jour la quantité courante.

## 6.6 Diagrammes de séquence

### Séquence 1 : Création d'un événement (avec auto-ajout du créateur)

Ce diagramme montre le flux complet d'une création d'événement, depuis la requête HTTP jusqu'à l'insertion en base :

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

Voici le diagramme le plus complexe du projet. Il illustre le mécanisme de verrou optimiste que j'ai mis en place pour gérer les cas où deux utilisateurs contribuent en même temps :

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

L'idée centrale, c'est que la mise à jour de la quantité d'une ressource n'est acceptée que si la version en base n'a pas changé depuis la lecture. Si deux utilisateurs contribuent en même temps, le second reçoit une erreur 409 et doit simplement rafraîchir et réessayer.
