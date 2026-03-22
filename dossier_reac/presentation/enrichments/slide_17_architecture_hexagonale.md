# Slide 17 — Architecture hexagonale (schema enrichi)

> **Type** : EXISTANT (diagram_1.mmd) + AMELIORATION — Schema enrichi pour mieux montrer les modules Gradle et le principe d'inversion de dependance.

## Diagramme enrichi

```mermaid
graph LR
    subgraph Infrastructure ["Module Gradle : infrastructure/"]
        direction TB

        subgraph DrivingAdapters ["Adapters Driving (entrants)"]
            EP1["CreateEventEndpoint"]
            EP2["GetEventsEndpoint"]
            EP3["AddContributionEndpoint"]
            EP4["ReduceContributionEndpoint"]
            JWT["JwtAuthenticationPlugin"]
        end

        subgraph DrivenAdapters ["Adapters Driven (sortants)"]
            SR1["SqlEventRepository"]
            SR2["SqlParticipantRepository"]
            SR3["SqlResourceRepository"]
            SR4["SqlContributionRepository"]
        end

        subgraph Tech ["Composants techniques"]
            Koin["Koin DI"]
            DB["DataSource + HikariCP"]
            Jackson["Jackson JSON"]
        end
    end

    subgraph Domain ["Module Gradle : domain/"]
        direction TB

        subgraph UseCases ["Use Cases"]
            UC1["CreateEventUseCase"]
            UC2["GetEventsByUserUseCase"]
            UC3["AddContributionUseCase"]
            UC4["ReduceContributionUseCase"]
        end

        subgraph Ports ["Ports (interfaces)"]
            PR1["EventRepository"]
            PR2["ParticipantRepository"]
            PR3["ResourceRepository"]
            PR4["ContributionRepository"]
        end

        subgraph Models ["Modeles domaine"]
            M1["Event + Creator"]
            M2["Participant"]
            M3["Resource"]
            M4["Contribution"]
        end
    end

    EP1 & EP2 --> UC1 & UC2
    EP3 & EP4 --> UC3 & UC4
    UC1 & UC2 --> PR1 & PR2
    UC3 & UC4 --> PR3 & PR4
    PR1 -.->|"implemente"| SR1
    PR2 -.->|"implemente"| SR2
    PR3 -.->|"implemente"| SR3
    PR4 -.->|"implemente"| SR4
```

## Schema simplifie (alternative pour la slide)

```mermaid
graph LR
    Client(["Client HTTP"]) --> DrivingAdapters

    subgraph Infra ["infrastructure/"]
        DrivingAdapters["Endpoints REST + JWT"]
        DrivenAdapters["Repositories SQL"]
    end

    subgraph Dom ["domain/"]
        UseCases["Use Cases"]
        PortsIn["Ports entrants"]
        PortsOut["Ports sortants"]
        Models["Modeles metier"]
    end

    DrivingAdapters -->|"appelle"| UseCases
    UseCases --> PortsOut
    PortsOut -.->|"implemente par"| DrivenAdapters
    DrivenAdapters --> DB[("PostgreSQL")]
```

## Points cles pour la slide

- **Deux modules Gradle independants** : `domain/` n'a aucune dependance vers `infrastructure/`
- **Inversion de dependance** : le domaine definit les interfaces (ports), l'infrastructure les implemente
- **Driving adapters** (entrants) : les endpoints REST qui recoivent les requetes
- **Driven adapters** (sortants) : les repositories SQL qui persistent les donnees
- **Koin** assemble le tout au demarrage (injection de dependances)

## Ce qu'il faut dire (notes orales)

L'architecture hexagonale est le fil conducteur du projet. J'ai materialise cette architecture avec deux modules Gradle physiquement separes.

Le module `domain` contient tout le coeur metier : les use cases, les modeles et les interfaces de repository — qu'on appelle les ports. Ce module n'a **aucune dependance technique** — ni Ktor, ni Exposed, ni Jackson. Il ne connait que Kotlin stdlib et Arrow.

Le module `infrastructure` contient les adapters. Cote entrant, les endpoints REST recoivent les requetes HTTP et appellent les use cases. Cote sortant, les repositories SQL implementent les interfaces definies dans le domaine.

Le principe fondamental, c'est l'**inversion de dependance** : la fleche de dependance va toujours de l'infrastructure vers le domaine, jamais l'inverse. Si demain je change d'ORM ou de framework web, seul le module infrastructure est impacte — le domaine reste intact.
