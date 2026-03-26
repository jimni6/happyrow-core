# BACK-004 — API de gestion des liens d'invitation (get actif, révoquer)

## Type
`feature` `backend` `API`

## Priorité
🟡 Moyenne

## Contexte
L'organisateur doit pouvoir consulter le lien d'invitation actif de son événement et le révoquer si nécessaire (ex : lien partagé par erreur, événement complet).

## User Story
> En tant qu'organisateur, je veux pouvoir voir le lien actif de mon événement et le révoquer pour en générer un nouveau si besoin.

---

## Endpoint 1 — Récupérer le lien actif

```
GET /api/event/configuration/api/v1/events/{eventId}/invites/active
```

### Headers
| Header | Valeur |
|--------|--------|
| `Authorization` | `Bearer <token>` |

### Response `200 OK` (lien actif trouvé)
```json
{
  "token": "abc123def456ghi789",
  "event_id": "uuid-event-id",
  "invite_url": "https://happyrow.app/invite/abc123def456ghi789",
  "created_at": 1711382400000,
  "expires_at": 1711987200000,
  "max_uses": null,
  "current_uses": 3,
  "status": "ACTIVE",
  "created_by": "uuid-organizer-id"
}
```

### Response `204 No Content`
Aucun lien actif pour cet événement.

### Erreurs

| Status | Code | Description |
|--------|------|-------------|
| `401` | `UNAUTHORIZED` | Non authentifié |
| `403` | `FORBIDDEN` | L'utilisateur n'est pas l'organisateur |
| `404` | `NOT_FOUND` | L'événement n'existe pas |

---

## Endpoint 2 — Révoquer un lien d'invitation

```
DELETE /api/event/configuration/api/v1/events/{eventId}/invites/{token}
```

### Headers
| Header | Valeur |
|--------|--------|
| `Authorization` | `Bearer <token>` |

### Response `204 No Content`
Le lien a été révoqué avec succès.

### Erreurs

| Status | Code | Description |
|--------|------|-------------|
| `401` | `UNAUTHORIZED` | Non authentifié |
| `403` | `FORBIDDEN` | L'utilisateur n'est pas l'organisateur |
| `404` | `NOT_FOUND` | L'événement ou le lien n'existe pas |
| `409` | `ALREADY_REVOKED` | Le lien est déjà révoqué |

## Règles métier

### GET active
1. Seul l'**organisateur** peut consulter le lien actif
2. Un lien est considéré "actif" si `status = ACTIVE` ET `expires_at > now()` ET (`max_uses IS NULL` OU `current_uses < max_uses`)
3. Si le lien en BDD a `status = ACTIVE` mais est expiré, le marquer automatiquement comme `EXPIRED` (lazy expiration) et retourner `204`

### DELETE (révocation)
1. Seul l'**organisateur** peut révoquer un lien
2. Passer le `status` à `REVOKED` en BDD (soft delete, ne pas supprimer la ligne)
3. Les participants déjà ajoutés via ce lien **ne sont pas affectés**
4. Après révocation, toute tentative d'utilisation du token retourne `410 Gone` (BACK-003)

## Tests à écrire

### GET active
- [ ] Organisateur avec lien actif → 200 avec détails du lien
- [ ] Organisateur sans lien actif → 204
- [ ] Lien expiré en BDD mais status ACTIVE → lazy expiration → 204
- [ ] Non organisateur → 403
- [ ] Non authentifié → 401
- [ ] Événement inexistant → 404

### DELETE
- [ ] Révocation réussie → 204
- [ ] Lien déjà révoqué → 409
- [ ] Non organisateur → 403
- [ ] Non authentifié → 401
- [ ] Token/événement inexistant → 404
- [ ] Après révocation, `GET /invites/{token}` retourne `REVOKED`
- [ ] Après révocation, `POST /invites/{token}/accept` retourne `410`
- [ ] Les participants existants ne sont pas supprimés

## Dépendances
- **BACK-001** — Table `event_invites`

## Estimation
**M** (2-3 jours)
