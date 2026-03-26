# BACK-001 — API de génération d'un lien d'invitation

## Type
`feature` `backend` `API`

## Priorité
🔴 Haute

## Contexte
L'organisateur d'un événement doit pouvoir générer un lien d'invitation unique pour partager son événement sans connaître l'email des invités. Ce lien contient un token sécurisé avec une durée de validité.

## User Story
> En tant qu'organisateur, je veux générer un lien d'invitation unique pour mon événement afin de le partager facilement.

## Endpoint

```
POST /api/event/configuration/api/v1/events/{eventId}/invites
```

### Headers
| Header | Valeur |
|--------|--------|
| `Authorization` | `Bearer <token>` |
| `Content-Type` | `application/json` |

### Request Body (optionnel)
```json
{
  "expires_in_days": 7,
  "max_uses": null
}
```

| Champ | Type | Default | Description |
|-------|------|---------|-------------|
| `expires_in_days` | `integer` | `7` | Durée de validité en jours (min: 1, max: 30) |
| `max_uses` | `integer \| null` | `null` | Nombre max d'utilisations (`null` = illimité) |

### Response `201 Created`
```json
{
  "token": "abc123def456ghi789",
  "event_id": "uuid-event-id",
  "invite_url": "https://happyrow.app/invite/abc123def456ghi789",
  "created_at": 1711382400000,
  "expires_at": 1711987200000,
  "max_uses": null,
  "current_uses": 0,
  "status": "ACTIVE",
  "created_by": "uuid-organizer-id"
}
```

### Erreurs

| Status | Code | Description |
|--------|------|-------------|
| `401` | `UNAUTHORIZED` | Token d'auth manquant ou invalide |
| `403` | `FORBIDDEN` | L'utilisateur n'est pas l'organisateur de l'événement |
| `404` | `NOT_FOUND` | L'événement n'existe pas |
| `409` | `CONFLICT` | Un lien actif existe déjà (voir règle ci-dessous) |

## Règles métier
1. **Seul l'organisateur** de l'événement peut générer un lien
2. **Un seul lien actif** par événement à la fois (retourner `409` si un lien actif existe, ou le retourner directement — à décider)
3. Le **token** doit être :
   - Unique (UUID v4 ou chaîne aléatoire cryptographiquement sûre, 20+ caractères)
   - Non prédictible (pas de séquence, pas basé sur l'eventId)
4. Par défaut, le lien expire après **7 jours**
5. L'URL de base pour le lien (`https://happyrow.app`) doit être configurable via variable d'environnement

## Modèle de données

### Table `event_invites`

| Colonne | Type | Contraintes |
|---------|------|-------------|
| `id` | `UUID` | PK, auto-generated |
| `token` | `VARCHAR(64)` | UNIQUE, NOT NULL, INDEX |
| `event_id` | `UUID` | FK → events, NOT NULL |
| `created_by` | `UUID` | FK → users (organizer), NOT NULL |
| `status` | `ENUM('ACTIVE', 'EXPIRED', 'REVOKED')` | NOT NULL, DEFAULT 'ACTIVE' |
| `max_uses` | `INTEGER` | NULLABLE |
| `current_uses` | `INTEGER` | NOT NULL, DEFAULT 0 |
| `created_at` | `TIMESTAMP` | NOT NULL |
| `expires_at` | `TIMESTAMP` | NOT NULL |

### Index recommandés
- `idx_event_invites_token` sur `token` (lookup par token)
- `idx_event_invites_event_status` sur `(event_id, status)` (vérification lien actif)

## Tests à écrire
- [ ] Génération réussie par l'organisateur → 201
- [ ] Refus si non organisateur → 403
- [ ] Refus si non authentifié → 401
- [ ] Refus si événement inexistant → 404
- [ ] Refus si lien actif déjà existant → 409
- [ ] Le token est unique et non prédictible
- [ ] `expires_at` est correctement calculé
- [ ] `max_uses` est optionnel et nullable

## Dépendances
Aucune (premier endpoint de la feature)

## Estimation
**M** (2-3 jours)
