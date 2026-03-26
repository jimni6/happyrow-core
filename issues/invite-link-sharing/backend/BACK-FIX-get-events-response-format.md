# BACK-FIX: Format de réponse GET /events

## Contexte

Le frontend appelle `GET /api/events` avec un `Authorization: Bearer <token>` et s'attend à recevoir un **tableau JSON** d'événements.

## Problème rencontré

La réponse actuelle du backend n'est **pas un tableau** mais un **objet wrappé** (probablement `{ "events": [...] }`).

Côté frontend, le code faisait :

```ts
const eventsResponse = await response.json();
eventsResponse.map(event => ({ ... }));
```

Ce qui provoquait l'erreur :
```
Error: Failed to get events: (await s.json()).map is not a function
```

## Fix appliqué côté frontend

Le frontend gère maintenant les deux formats (tableau ou objet wrappé) :

```ts
const json = await response.json();
const eventsResponse = Array.isArray(json)
  ? json
  : Array.isArray(json.events)
    ? json.events
    : [];
```

**Fichier modifié :** `src/features/events/services/HttpEventRepository.ts` — méthode `getEventsByOrganizer()`

## Action souhaitée côté backend

Merci de **confirmer le format exact** de la réponse `GET /api/events` :

| Option | Format | Status frontend |
|--------|--------|-----------------|
| A | `[ { identifier, name, ... }, ... ]` | ✅ Supporté |
| B | `{ "events": [ { identifier, name, ... }, ... ] }` | ✅ Supporté |
| C | Autre structure ? | ⚠️ À adapter |

### Format attendu par événement

```json
{
  "identifier": "uuid",
  "name": "string",
  "description": "string",
  "event_date": 1234567890,
  "location": "string",
  "type": "PARTY",
  "creator": "uuid",
  "creation_date": 1234567890,
  "update_date": 1234567890,
  "members": ["uuid"]
}
```

## Note additionnelle

Des erreurs **HTTP 429 (Too Many Requests)** ont également été observées sur `POST /api/events`. Si un rate limiter est en place, merci de confirmer les limites configurées.
