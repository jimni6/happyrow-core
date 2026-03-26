# BACK-003 — API d'acceptation d'une invitation via token

## Type
`feature` `backend` `API`

## Priorité
🔴 Haute

## Contexte
Une fois que l'utilisateur a vu les détails de l'événement via le lien d'invitation (BACK-002), il doit pouvoir accepter l'invitation pour être ajouté comme participant. Cet endpoint est **authentifié** car l'utilisateur doit avoir un compte pour rejoindre.

## User Story
> En tant qu'utilisateur authentifié ayant reçu un lien d'invitation, je veux accepter l'invitation pour être automatiquement ajouté comme participant à l'événement.

## Endpoint

```
POST /api/event/configuration/api/v1/invites/{token}/accept
```

### Headers
| Header | Valeur |
|--------|--------|
| `Authorization` | `Bearer <token>` |

### Request Body
Aucun body requis. L'utilisateur est identifié via le Bearer token.

### Response `200 OK`
```json
{
  "event_id": "uuid-event-id",
  "user_email": "user@example.com",
  "user_name": "Jean Dupont",
  "status": "CONFIRMED",
  "joined_at": 1711382400000
}
```

> Le format de réponse est identique à celui de `POST /events/{eventId}/participants` pour la cohérence.

### Erreurs

| Status | Code | Description |
|--------|------|-------------|
| `401` | `UNAUTHORIZED` | Token d'auth manquant ou invalide |
| `404` | `INVITE_NOT_FOUND` | Token d'invitation inexistant |
| `410` | `INVITE_EXPIRED` | Le lien d'invitation a expiré |
| `410` | `INVITE_REVOKED` | Le lien a été révoqué par l'organisateur |
| `410` | `INVITE_EXHAUSTED` | Le nombre max d'utilisations est atteint |
| `409` | `ALREADY_PARTICIPANT` | L'utilisateur est déjà participant de cet événement |

#### Exemple erreur `410 Gone`
```json
{
  "error": "INVITE_EXPIRED",
  "message": "This invitation link has expired. Please ask the organizer for a new one."
}
```

#### Exemple erreur `409 Conflict`
```json
{
  "error": "ALREADY_PARTICIPANT",
  "message": "You are already a participant of this event.",
  "event_id": "uuid-event-id"
}
```

## Règles métier

### Flow d'acceptation
1. Vérifier que le token d'invitation existe → sinon `404`
2. Vérifier que le token est valide (non expiré, non révoqué, non épuisé) → sinon `410`
3. Identifier l'utilisateur via le Bearer token
4. Vérifier que l'utilisateur n'est pas déjà participant → sinon `409` (avec `event_id` pour permettre la redirection)
5. **Ajouter l'utilisateur comme participant** avec le status `CONFIRMED`
6. **Incrémenter** `current_uses` dans la table `event_invites`
7. Retourner le participant créé

### Règles complémentaires
- L'utilisateur est ajouté avec le status **`CONFIRMED`** (pas `INVITED`, car il a volontairement rejoint)
- Si l'organisateur utilise son propre lien, il reçoit `409 ALREADY_PARTICIPANT`
- La transaction doit être **atomique** : l'ajout du participant et l'incrémentation de `current_uses` doivent être dans la même transaction
- Après acceptation, l'utilisateur a les mêmes droits qu'un participant ajouté manuellement

## Tests à écrire
- [ ] Acceptation réussie → 200, participant créé avec status `CONFIRMED`
- [ ] `current_uses` est incrémenté après acceptation
- [ ] Token expiré → 410 `INVITE_EXPIRED`
- [ ] Token révoqué → 410 `INVITE_REVOKED`
- [ ] Token épuisé (max_uses atteint) → 410 `INVITE_EXHAUSTED`
- [ ] Token inexistant → 404
- [ ] Utilisateur déjà participant → 409 avec `event_id`
- [ ] Non authentifié → 401
- [ ] Vérifier l'atomicité : si l'ajout du participant échoue, `current_uses` n'est pas incrémenté
- [ ] Acceptation avec `max_uses = 1` : le premier accepte, le deuxième reçoit `410`
- [ ] Le participant créé est visible dans `GET /events/{eventId}/participants`

## Dépendances
- **BACK-001** — Table `event_invites` et token
- **BACK-002** — Logique de validation du status du token (réutilisable)

## Estimation
**M** (3-4 jours)
