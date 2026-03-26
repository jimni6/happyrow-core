# Invite Link API — Frontend Integration Guide

Documentation technique pour l'integration frontend de la feature **Invite Link Sharing**.

**Base URL** : `https://happyrow-core.onrender.com/event/configuration/api/v1`
**JSON** : snake_case (`event_id`, `created_at`, `invite_url`, etc.)
**Auth** : `Authorization: Bearer <supabase_token>` sauf mention contraire
**Dates** : timestamps en millisecondes (epoch ms)

---

## Table des endpoints

| # | Methode | Route | Auth | Description |
|---|---------|-------|------|-------------|
| 1 | `POST` | `/events/{eventId}/invites` | Bearer | Generer un lien d'invitation |
| 2 | `GET` | `/invites/{token}` | **Aucune** | Valider un token (page d'invitation) |
| 3 | `POST` | `/invites/{token}/accept` | Bearer | Accepter une invitation |
| 4 | `GET` | `/events/{eventId}/invites/active` | Bearer | Recuperer le lien actif |
| 5 | `DELETE` | `/events/{eventId}/invites/{token}` | Bearer | Revoquer un lien |

---

## 1. Generer un lien d'invitation

**Utilise par** : FEAT-001 (ShareInviteModal)
**Qui** : organisateur uniquement

```
POST /events/{eventId}/invites
Authorization: Bearer <token>
Content-Type: application/json
```

### Request body (optionnel)

```json
{
  "expires_in_days": 7,
  "max_uses": null
}
```

| Champ | Type | Default | Description |
|-------|------|---------|-------------|
| `expires_in_days` | `integer` | `7` | Duree de validite (1-30 jours) |
| `max_uses` | `integer \| null` | `null` | Nombre max d'utilisations (`null` = illimite) |

### Response `201 Created`

```json
{
  "token": "aB3kL9mNpQrStUvWxYz1234567890AB",
  "event_id": "550e8400-e29b-41d4-a716-446655440000",
  "invite_url": "https://happyrow.app/invite/aB3kL9mNpQrStUvWxYz1234567890AB",
  "created_at": 1711382400000,
  "expires_at": 1711987200000,
  "max_uses": null,
  "current_uses": 0,
  "status": "ACTIVE",
  "created_by": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
}
```

### Erreurs

| Status | `type` | Quand |
|--------|--------|-------|
| `401` | `MISSING_TOKEN` | Pas de header Authorization |
| `403` | `FORBIDDEN` | L'utilisateur n'est pas l'organisateur |
| `404` | `NOT_FOUND` | L'evenement n'existe pas |
| `409` | `CONFLICT` | Un lien actif existe deja pour cet evenement |

### Exemple d'usage frontend

```typescript
async function createInviteLink(eventId: string): Promise<InviteLink> {
  const response = await fetch(
    `${API_BASE}/events/${eventId}/invites`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ expires_in_days: 7 }),
    }
  );

  if (response.status === 409) {
    // Un lien actif existe deja -> recuperer le lien actif
    return getActiveInviteLink(eventId);
  }

  if (!response.ok) throw new Error('Failed to create invite link');
  return response.json();
}
```

---

## 2. Valider un token d'invitation (endpoint public)

**Utilise par** : FEAT-003 (InviteLandingPage `/invite/:token`)
**Qui** : tout le monde (pas d'auth requise)

```
GET /invites/{token}
```

> **Aucun header Authorization requis.** C'est le seul endpoint public de l'API invite.

### Response `200 OK` — Token valide

```json
{
  "token": "aB3kL9mNpQrStUvWxYz1234567890AB",
  "status": "VALID",
  "event": {
    "identifier": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Anniversaire de Marie",
    "event_date": 1711382400000,
    "location": "12 rue de la Paix, Paris",
    "type": "BIRTHDAY",
    "organizer_name": "Marie",
    "participant_count": 8
  },
  "expires_at": 1711987200000
}
```

### Response `200 OK` — Token expire

```json
{
  "token": "aB3kL9mNpQrStUvWxYz1234567890AB",
  "status": "EXPIRED",
  "event": null,
  "expires_at": 1711987200000
}
```

### Response `200 OK` — Token revoque

```json
{
  "token": "aB3kL9mNpQrStUvWxYz1234567890AB",
  "status": "REVOKED",
  "event": null,
  "expires_at": 1711987200000
}
```

### Response `200 OK` — Utilisations epuisees

```json
{
  "token": "aB3kL9mNpQrStUvWxYz1234567890AB",
  "status": "EXHAUSTED",
  "event": null,
  "expires_at": 1711987200000
}
```

### Response `404 Not Found` — Token inconnu

```json
{
  "type": "INVITE_NOT_FOUND",
  "title": "Not Found",
  "status": 404,
  "detail": "This invitation link does not exist."
}
```

### Enum `status`

| Valeur | Description | `event` present ? |
|--------|-------------|-------------------|
| `VALID` | Lien actif et utilisable | Oui |
| `EXPIRED` | Date d'expiration depassee | Non |
| `REVOKED` | Revoque par l'organisateur | Non |
| `EXHAUSTED` | Nombre max d'utilisations atteint | Non |

### Exemple d'usage frontend

```typescript
// src/features/invite/services/HttpInviteRepository.ts

interface InviteValidation {
  token: string;
  status: 'VALID' | 'EXPIRED' | 'REVOKED' | 'EXHAUSTED';
  event: InviteEventSummary | null;
  expires_at: number | null;
}

interface InviteEventSummary {
  identifier: string;
  name: string;
  event_date: number;
  location: string;
  type: string;
  organizer_name: string;
  participant_count: number;
}

async function getInviteDetails(token: string): Promise<InviteValidation | null> {
  const response = await fetch(`${API_BASE}/invites/${token}`);

  if (response.status === 404) return null;
  if (!response.ok) throw new Error('Failed to validate invite');

  return response.json();
}
```

### Flow recommande pour InviteLandingPage

```
1. Extraire le token depuis l'URL : /invite/:token
2. Appeler GET /invites/{token}
3. Si 404 → afficher "Lien invalide"
4. Si status !== "VALID" → afficher message d'erreur selon status
5. Si status === "VALID" → afficher les details de l'evenement
6. Si utilisateur connecte → bouton "Rejoindre" → POST /invites/{token}/accept
7. Si non connecte → stocker le token dans localStorage, rediriger vers login/register
8. Apres login/register → recuperer le token du localStorage → POST /invites/{token}/accept
```

---

## 3. Accepter une invitation

**Utilise par** : FEAT-003 (InviteLandingPage — bouton "Rejoindre")
**Qui** : utilisateur authentifie

```
POST /invites/{token}/accept
Authorization: Bearer <token>
```

> **Pas de body requis.** L'utilisateur est identifie via le Bearer token.

### Response `200 OK`

```json
{
  "event_id": "550e8400-e29b-41d4-a716-446655440000",
  "user_id": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
  "user_name": "other@happyrow.com",
  "status": "CONFIRMED",
  "joined_at": 1711382400000
}
```

### Erreurs

| Status | `type` / `error` | Quand | Action frontend |
|--------|-------------------|-------|-----------------|
| `401` | `MISSING_TOKEN` | Non authentifie | Rediriger vers login |
| `404` | `INVITE_NOT_FOUND` | Token inexistant | Afficher "Lien invalide" |
| `409` | `ALREADY_PARTICIPANT` | Deja participant | Rediriger vers `/events/{event_id}` |
| `410` | `INVITE_EXPIRED` | Lien expire | Afficher "Lien expire" |
| `410` | `INVITE_REVOKED` | Lien revoque | Afficher "Lien revoque" |
| `410` | `INVITE_EXHAUSTED` | Max utilisations atteint | Afficher "Lien epuise" |

### Response `409 Conflict` (cas special)

```json
{
  "error": "ALREADY_PARTICIPANT",
  "message": "You are already a participant of this event.",
  "event_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

> Le champ `event_id` permet de rediriger directement vers l'evenement.

### Exemple d'usage frontend

```typescript
// src/features/invite/use-cases/AcceptInvite.ts

async function acceptInvite(token: string): Promise<AcceptInviteResult> {
  const response = await fetch(`${API_BASE}/invites/${token}/accept`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${authToken}` },
  });

  if (response.status === 409) {
    const body = await response.json();
    // Deja participant → rediriger
    window.location.href = `/events/${body.event_id}`;
    return;
  }

  if (response.status === 410) {
    const body = await response.json();
    throw new InviteExpiredError(body.type);
  }

  if (!response.ok) throw new Error('Failed to accept invite');
  return response.json();
}
```

---

## 4. Recuperer le lien actif d'un evenement

**Utilise par** : FEAT-005 (ShareInviteModal — afficher le lien existant)
**Qui** : organisateur uniquement

```
GET /events/{eventId}/invites/active
Authorization: Bearer <token>
```

### Response `200 OK` — Lien actif existe

```json
{
  "token": "aB3kL9mNpQrStUvWxYz1234567890AB",
  "event_id": "550e8400-e29b-41d4-a716-446655440000",
  "invite_url": "https://happyrow.app/invite/aB3kL9mNpQrStUvWxYz1234567890AB",
  "created_at": 1711382400000,
  "expires_at": 1711987200000,
  "max_uses": null,
  "current_uses": 3,
  "status": "ACTIVE",
  "created_by": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
}
```

### Response `204 No Content`

Aucun lien actif pour cet evenement. Le body est vide.

### Erreurs

| Status | `type` | Quand |
|--------|--------|-------|
| `401` | `MISSING_TOKEN` | Non authentifie |
| `403` | `FORBIDDEN` | L'utilisateur n'est pas l'organisateur |

### Exemple d'usage frontend

```typescript
async function getActiveInviteLink(eventId: string): Promise<InviteLink | null> {
  const response = await fetch(`${API_BASE}/events/${eventId}/invites/active`, {
    headers: { 'Authorization': `Bearer ${authToken}` },
  });

  if (response.status === 204) return null;
  if (response.status === 403) return null; // Non organisateur
  if (!response.ok) throw new Error('Failed to get active invite');

  return response.json();
}
```

---

## 5. Revoquer un lien d'invitation

**Utilise par** : FEAT-005 (ShareInviteModal — bouton "Revoquer")
**Qui** : organisateur uniquement

```
DELETE /events/{eventId}/invites/{token}
Authorization: Bearer <token>
```

### Response `204 No Content`

Lien revoque avec succes. Body vide.

### Erreurs

| Status | `type` | Quand |
|--------|--------|-------|
| `401` | `MISSING_TOKEN` | Non authentifie |
| `403` | `FORBIDDEN` | L'utilisateur n'est pas l'organisateur |
| `404` | `NOT_FOUND` | Lien ou evenement inexistant |
| `409` | `ALREADY_REVOKED` | Lien deja revoque |

### Exemple d'usage frontend

```typescript
async function revokeInviteLink(eventId: string, token: string): Promise<void> {
  const response = await fetch(
    `${API_BASE}/events/${eventId}/invites/${token}`,
    {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${authToken}` },
    }
  );

  if (!response.ok && response.status !== 409) {
    throw new Error('Failed to revoke invite');
  }
}
```

---

## Types TypeScript recommandes

```typescript
// src/features/events/types/InviteLink.ts

export interface InviteLink {
  token: string;
  event_id: string;
  invite_url: string;
  created_at: number;
  expires_at: number;
  max_uses: number | null;
  current_uses: number;
  status: 'ACTIVE';
  created_by: string;
}

export interface InviteLinkCreationRequest {
  expires_in_days?: number;  // default: 7, min: 1, max: 30
  max_uses?: number | null;  // default: null (illimite)
}

// src/features/invite/types/Invite.ts

export type InviteValidationStatus = 'VALID' | 'EXPIRED' | 'REVOKED' | 'EXHAUSTED';

export interface InviteEventSummary {
  identifier: string;
  name: string;
  event_date: number;
  location: string;
  type: string;
  organizer_name: string;
  participant_count: number;
}

export interface InviteValidation {
  token: string;
  status: InviteValidationStatus;
  event: InviteEventSummary | null;
  expires_at: number | null;
}

export interface AcceptInviteResult {
  event_id: string;
  user_id: string;
  user_name: string | null;
  status: 'CONFIRMED';
  joined_at: number;
}
```

---

## Format des erreurs

Toutes les erreurs suivent le format `ProblemDetail` :

```json
{
  "type": "ERROR_CODE",
  "title": "HTTP Status Description",
  "status": 403,
  "detail": "Human-readable error message"
}
```

Exception : la reponse `409 ALREADY_PARTICIPANT` utilise un format specifique avec `event_id` pour permettre la redirection.

---

## Rate limiting

| Route | Limite |
|-------|--------|
| `POST /events/{eventId}/invites` | 30 req/min |
| `POST /invites/{token}/accept` | 30 req/min |
| `DELETE /events/{eventId}/invites/{token}` | 30 req/min |
| `GET /invites/{token}` | 100 req/min (global) |
| `GET /events/{eventId}/invites/active` | 100 req/min (global) |

En cas de depassement : `429 Too Many Requests` avec header `Retry-After`.

---

## Flow complet : invite link lifecycle

```
Organisateur                          Backend                          Invite
    │                                    │                               │
    ├─ POST /events/{id}/invites ───────>│                               │
    │<── 201 { token, invite_url } ──────│                               │
    │                                    │                               │
    ├─ Partage le lien (WhatsApp, SMS) ─────────────────────────────────>│
    │                                    │                               │
    │                                    │     GET /invites/{token} <────┤
    │                                    │──── 200 { status: VALID } ───>│
    │                                    │                               │
    │                                    │  POST /invites/{token}/accept │
    │                                    │<─────── (apres login) ────────┤
    │                                    │──── 200 { CONFIRMED } ───────>│
    │                                    │                               │
    ├─ GET /events/{id}/invites/active ─>│                               │
    │<── 200 { current_uses: 1 } ────────│                               │
    │                                    │                               │
    ├─ DELETE /events/{id}/invites/{t} ─>│                               │
    │<── 204 No Content ─────────────────│                               │
    │                                    │                               │
    ├─ POST /events/{id}/invites ───────>│  (nouveau lien)               │
    │<── 201 { new_token } ──────────────│                               │
```

---

## Correspondance issues frontend / endpoints

| Issue frontend | Endpoints utilises |
|----------------|--------------------|
| FEAT-001 (ShareInviteModal) | `POST /events/{id}/invites`, `GET /events/{id}/invites/active` |
| FEAT-002 (QR Code) | Aucun (genere cote client a partir de `invite_url`) |
| FEAT-003 (InviteLandingPage) | `GET /invites/{token}`, `POST /invites/{token}/accept` |
| FEAT-004 (Web Share API) | Aucun (utilise `invite_url` du endpoint 1 ou 4) |
| FEAT-005 (Gestion des liens) | `GET /events/{id}/invites/active`, `DELETE /events/{id}/invites/{token}`, `POST /events/{id}/invites` |

---

## Token localStorage (flow login/register)

Quand un utilisateur non connecte arrive sur `/invite/:token` :

1. Stocker le token dans `localStorage` :
   ```typescript
   localStorage.setItem('pending_invite_token', token);
   ```

2. Rediriger vers login ou register

3. Apres authentification, verifier et consommer le token :
   ```typescript
   const pendingToken = localStorage.getItem('pending_invite_token');
   if (pendingToken) {
     localStorage.removeItem('pending_invite_token');
     await acceptInvite(pendingToken);
   }
   ```
