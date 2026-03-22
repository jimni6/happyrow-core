# Slide 19 — Flux d'une requete dans l'application (schema)

> **Type** : CREATION — Ce diagramme de sequence a ete cree pour visualiser le parcours complet d'une requete a travers les couches.

## Diagramme de sequence

```mermaid
sequenceDiagram
    participant Client as Client HTTP
    participant JWT as JwtAuthPlugin
    participant EP as Endpoint (driving adapter)
    participant DTO as DTO (in/out)
    participant UC as UseCase (domaine)
    participant Port as Port (interface)
    participant Repo as Repository SQL (driven adapter)
    participant DB as PostgreSQL

    Client->>JWT: Requete HTTP + Bearer token
    JWT->>JWT: Verifier signature HMAC256
    JWT->>EP: Call authentifie (user dans attributes)

    rect rgb(240, 248, 255)
        Note over EP,DTO: Couche infrastructure (driving)
        EP->>DTO: receive() + deserialisation
        DTO->>DTO: toDomain() — DTO vers modele domaine
    end

    rect rgb(240, 255, 240)
        Note over UC,Port: Couche domaine
        EP->>UC: Appel du use case
        UC->>Port: Appel via interface (port)
    end

    rect rgb(255, 248, 240)
        Note over Repo,DB: Couche infrastructure (driven)
        Port->>Repo: Implementation concrete
        Repo->>DB: SQL parametre (Exposed)
        DB-->>Repo: ResultRow
        Repo-->>Port: Either Right(Entity)
    end

    Port-->>UC: Either Right(Entity)
    UC-->>EP: Either Right(Entity)

    rect rgb(240, 248, 255)
        Note over EP,DTO: Couche infrastructure (driving)
        EP->>DTO: toDto() — domaine vers DTO reponse
    end

    EP-->>Client: HTTP 200/201 + JSON
```

## Schema simplifie (alternatif, pour tenir sur une slide)

```mermaid
flowchart LR
    Client(["Client"]) -->|"HTTP + JWT"| Endpoint
    subgraph Infra1 ["infrastructure/ (driving)"]
        Endpoint["Endpoint REST"]
        DTOin["DTO in : toDomain()"]
    end
    Endpoint --> DTOin
    DTOin --> UseCase
    subgraph Dom ["domain/"]
        UseCase["UseCase"]
        Port["Port (interface)"]
    end
    UseCase --> Port
    Port --> Repo
    subgraph Infra2 ["infrastructure/ (driven)"]
        Repo["Repository SQL"]
        DTOout["DTO out : toDto()"]
    end
    Repo --> DB[("PostgreSQL")]
    DB --> Repo
    Repo --> UseCase
    UseCase --> DTOout
    DTOout -->|"JSON"| Client
```

## Ce qu'il faut dire (notes orales)

Ce schema montre le parcours complet d'une requete a travers les differentes couches de l'application.

1. **Entree** : Le client envoie une requete HTTP avec un token JWT. Le `JwtAuthenticationPlugin` verifie la signature HMAC256 et extrait l'identite de l'utilisateur.

2. **Couche driving (infrastructure)** : L'endpoint Ktor recoit la requete, la deserialise avec Jackson, et la convertit en objet domaine via `toDomain()`. C'est la frontiere entre le monde HTTP et le monde metier.

3. **Couche domaine** : Le use case applique la regle metier et dialogue avec les repositories via des interfaces abstraites (les ports). Le domaine ne sait pas comment les donnees sont stockees.

4. **Couche driven (infrastructure)** : Le repository SQL implemente le port et execute les requetes parametrees via Exposed. Les donnees sont lues depuis PostgreSQL et reconstruites en entites domaine.

5. **Retour** : L'entite domaine remonte a travers les couches, est convertie en DTO de reponse via `toDto()`, puis envoyee au client en JSON.

Le point cle : a chaque frontiere de couche, les objets sont **convertis** (DTO in/out vs entites domaine), ce qui maintient une separation nette et permet de faire evoluer chaque couche independamment.
