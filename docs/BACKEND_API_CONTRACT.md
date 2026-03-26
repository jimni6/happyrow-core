# Backend API Contract — Guide de coherence Frontend/Backend

Ce document decrit l'etat actuel de l'API backend `happyrow-core` apres les refactorings recents. Il est destine a l'agent IA frontend pour verifier la coherence entre les deux services.

---

## Changements majeurs a verifier

### 1. Identification par UUID (et non plus email)

Tous les modeles utilisent desormais `userId: UUID` (extrait du claim `sub` du JWT Supabase) au lieu de `userEmail`.

| Avant | Apres |
|-------|-------|
| `userEmail: "jimmy@example.com"` | `userId: "ab70634a-345e-415e-8417-60841b6bcb20"` |
| `creator: "jimmy@example.com"` | `creator: "ab70634a-345e-415e-8417-60841b6bcb20"` |
| Route `/{userEmail}` | Route `/{userId}` |
| `members: ["email@ex.com"]` | `members: ["uuid-string"]` |

**Points de verification frontend :**
- Les payloads envoyant un `userEmail` doivent envoyer un `userId` (UUID string)
- Les reponses retournent `userId` au lieu de `userEmail` dans `ParticipantDto`
- Le champ `creator` dans `EventDto` est un UUID string
- Le champ `members` dans les requetes create/update event est une liste de UUID strings
- Les routes participant utilisent `/{userId}` (UUID) au lieu de `/{userEmail}`

### 2. Pagination — reponse en tableau (pas d'enveloppe)

Les endpoints de liste retournent un **tableau JSON** directement (compatible avec l'ancien format). Les metadonnees de pagination sont dans les **headers de reponse**.

**Corps de reponse :** `[{...}, {...}]` (tableau de DTOs)

**Headers de pagination :**

| Header | Description | Exemple |
|--------|-------------|---------|
| `X-Page` | Index de la page courante (0-based) | `0` |
| `X-Size` | Taille de la page | `20` |
| `X-Total-Elements` | Nombre total d'elements | `42` |
| `X-Total-Pages` | Nombre total de pages | `3` |

**Parametres de requete optionnels :**

| Parametre | Defaut | Contraintes |
|-----------|--------|-------------|
| `page` | `0` | >= 0 |
| `size` | `20` | 1 a 100 |

**Points de verification frontend :**
- `GET /events` retourne un tableau `[]`, pas `{ content: [], page: ... }`
- Idem pour `GET /events/{eventId}/participants` et `GET /events/{eventId}/resources`
- Si le frontend a besoin des infos de pagination, lire les headers `X-*`

### 3. Format d'erreur RFC 7807 (ProblemDetail)

Toutes les erreurs metier sont retournees au format :

```json
{
  "type": "EVENT_NOT_FOUND",
  "title": "Not Found",
  "status": 404,
  "detail": "Event abc-123 not found"
}
```

**Points de verification frontend :**
- Les reponses d'erreur ne sont plus `{ message: "..." }` mais `{ type, title, status, detail }`
- Le champ `type` est un code machine (ex: `FORBIDDEN`, `BAD_REQUEST`)
- Le champ `detail` est un message lisible

### 4. Rate Limiting

| Scope | Limite | Fenetre |
|-------|--------|---------|
| Global (toutes les requetes) | 100 requetes | 1 minute |
| Mutation (POST/PUT/DELETE) | 30 requetes | 1 minute |

**Reponse 429 :**
```json
{
  "type": "RATE_LIMIT_EXCEEDED",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Too many requests. Retry after ... seconds."
}
```

**Points de verification frontend :**
- Gerer le code HTTP `429` et afficher un message adapte
- Utiliser le header `Retry-After` si present

---

## Contrat d'API complet

### Base URL

```
/event/configuration/api/v1
```

Routes publiques (sans JWT) : `GET /`, `GET /health`, `GET /info`

### Authentification

Toutes les routes sous `/event/configuration/api/v1` necessitent :
```
Authorization: Bearer <jwt_supabase>
```

Claims JWT utilises :

| Claim | Usage |
|-------|-------|
| `sub` | `userId` (UUID string) — identifiant principal |
| `email` | `email` — utilise pour certaines verifications internes |

Reponse 401 (token manquant/invalide) :
```json
{ "type": "MISSING_TOKEN", "message": "..." }
```

---

### Endpoints Events

#### `GET /events`

Retourne les evenements de l'utilisateur authentifie.

**Query params :** `page`, `size` (optionnels, voir section pagination)

**Reponse 200 :** Tableau de `EventDto`
```json
[
  {
    "identifier": "uuid",
    "name": "Fete de Noel",
    "description": "...",
    "eventDate": "2026-12-25T18:00:00Z",
    "creationDate": "2026-03-25T10:00:00Z",
    "updateDate": "2026-03-25T10:00:00Z",
    "creator": "uuid-du-createur",
    "location": "Paris",
    "type": "PARTY",
    "members": ["uuid-1", "uuid-2"]
  }
]
```

#### `POST /events`

**Body :**
```json
{
  "name": "string (obligatoire, non vide)",
  "description": "string",
  "eventDate": "ISO-8601 instant (doit etre dans le futur)",
  "location": "string (obligatoire, non vide)",
  "type": "PARTY | BIRTHDAY | DINER | SNACK",
  "members": ["uuid-string", "..."]
}
```

**Reponse 201 :** `EventDto`

#### `PUT /events/{id}`

Meme body que POST. `{id}` = UUID de l'evenement.

**Reponse 200 :** `EventDto` mis a jour

#### `DELETE /events/{id}`

**Reponse 204 :** Pas de body. Seul le createur peut supprimer.

---

### Endpoints Participants

Base : `/events/{eventId}/participants`

#### `GET /events/{eventId}/participants`

**Query params :** `page`, `size` (optionnels)

**Reponse 200 :** Tableau de `ParticipantDto`
```json
[
  {
    "identifier": "uuid",
    "userId": "uuid-du-participant",
    "userName": "Jean" | null,
    "eventId": "uuid-event",
    "status": "CONFIRMED",
    "joinedAt": 1711360000000,
    "createdAt": 1711360000000,
    "updatedAt": 1711360000000
  }
]
```

#### `POST /events/{eventId}/participants`

**Body :**
```json
{
  "userId": "uuid-string (obligatoire)",
  "userName": "string (optionnel)"
}
```

**Reponse 201 :** `ParticipantDto`

#### `PUT /events/{eventId}/participants/{userId}`

`{userId}` = UUID du participant (pas son email).

**Body :**
```json
{
  "status": "INVITED | CONFIRMED | DECLINED | MAYBE"
}
```

**Reponse 200 :** `ParticipantDto` mis a jour

#### `DELETE /events/{eventId}/participants/{userId}`

**Reponse 204 :** Suppression du participant.

---

### Endpoints Resources

Base : `/events/{eventId}/resources`

#### `GET /events/{eventId}/resources`

**Query params :** `page`, `size` (optionnels)

**Reponse 200 :** Tableau de `ResourceDto` (avec contributeurs)
```json
[
  {
    "identifier": "uuid",
    "name": "Chips",
    "category": "FOOD",
    "suggestedQuantity": 5,
    "currentQuantity": 3,
    "eventId": "uuid-event",
    "contributors": [
      {
        "userId": "uuid-du-contributeur",
        "quantity": 3,
        "contributedAt": 1711360000000
      }
    ],
    "version": 1,
    "createdAt": 1711360000000,
    "updatedAt": 1711360000000
  }
]
```

#### `POST /events/{eventId}/resources`

**Body :**
```json
{
  "name": "string (obligatoire, non vide)",
  "category": "FOOD | DRINK | UTENSIL | DECORATION | OTHER",
  "quantity": 5,
  "suggestedQuantity": 10
}
```

**Reponse 201 :** `ResourceDto`

---

### Endpoints Contributions

Base : `/events/{eventId}/resources/{resourceId}/contributions`

#### `POST .../contributions` (ajouter)

**Body :**
```json
{
  "quantity": 3
}
```

**Reponse 200 :** `ContributionDto`
```json
{
  "identifier": "uuid",
  "participantId": "uuid",
  "resourceId": "uuid",
  "quantity": 3,
  "createdAt": 1711360000000,
  "updatedAt": 1711360000000
}
```

#### `POST .../contributions/reduce` (reduire)

**Body :**
```json
{
  "quantity": 1
}
```

**Reponse 200 :** `ContributionDto` (si contribution restante) ou **204** (si entierement supprimee)

#### `DELETE .../contributions` (supprimer entierement)

**Reponse 204**

---

## Enumerations

### EventType
`PARTY`, `BIRTHDAY`, `DINER`, `SNACK`

### ResourceCategory
`FOOD`, `DRINK`, `UTENSIL`, `DECORATION`, `OTHER`

### ParticipantStatus
`INVITED`, `CONFIRMED`, `DECLINED`, `MAYBE`

---

## Types d'erreur connus (`ProblemDetail.type`)

| Type | HTTP | Contexte |
|------|------|----------|
| `BAD_REQUEST` | 400 | Validation de parametres |
| `INVALID_BODY` | 400 | Body JSON invalide |
| `INVALID_PARAMETER` | 400 | Parametre de requete invalide |
| `INVALID_UUID` | 400 | UUID mal forme |
| `INVALID_ORGANIZER_ID` | 400 | Probleme d'authentification |
| `MISSING_TOKEN` | 401 | JWT absent |
| `INVALID_TOKEN` | 401 | JWT invalide |
| `FORBIDDEN` | 403 | Pas d'acces a l'evenement |
| `UNAUTHORIZED_DELETE` | 403 | Non-createur tente de supprimer |
| `NOT_FOUND` | 404 | Ressource introuvable |
| `EVENT_NOT_FOUND` | 404 | Evenement introuvable |
| `NAME_ALREADY_EXISTS` | 409 | Nom de ressource en doublon |
| `OPTIMISTIC_LOCK_FAILURE` | 409 | Conflit de concurrence |
| `INSUFFICIENT_CONTRIBUTION` | 409 | Quantite insuffisante a reduire |
| `RATE_LIMIT_EXCEEDED` | 429 | Trop de requetes |
| `TECHNICAL_ERROR` | 500 | Erreur serveur inattendue |

---

## CORS

**Origins autorises :**
- Developpement : `localhost:3000`, `3001`, `4200`, `5173`, `8080`, `8081` (+ `127.0.0.1`)
- Production : `jimni6.github.io`, `happyrow-front.vercel.app`, `happyrow-front-*-jimni6s-projects.vercel.app`
- Configurable via env `ALLOWED_ORIGINS` (comma-separated)

**Headers exposes :** `X-Page`, `X-Size`, `X-Total-Elements`, `X-Total-Pages`

**Credentials :** autorisees

---

## Checklist de verification frontend

- [ ] Les appels utilisent `userId` (UUID) au lieu de `userEmail` dans les payloads et routes
- [ ] Le champ `creator` dans les reponses event est un UUID (pas un email)
- [ ] Le champ `members` dans les requetes event est une liste de UUID strings
- [ ] Les routes participant utilisent `/{userId}` (UUID) au lieu de `/{userEmail}`
- [ ] Les reponses des listes sont des tableaux JSON `[]` (pas d'enveloppe `{content: []}`)
- [ ] Les headers de pagination `X-Page`, `X-Size`, `X-Total-Elements`, `X-Total-Pages` sont lus si necessaire
- [ ] Les erreurs sont parsees au format `{ type, title, status, detail }` (RFC 7807)
- [ ] Le code 429 est gere (rate limiting) avec un message adapte
- [ ] Les `ParticipantDto` contiennent `userId` (UUID) et non `userEmail`
- [ ] Les `ContributorDto` dans les resources contiennent `userId` (UUID)
- [ ] Les enums sont envoyes en majuscules : `PARTY`, `FOOD`, `CONFIRMED`, etc.
