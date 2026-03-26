# BACK-002 — API de validation d'un token d'invitation (détails publics)

## Type
`feature` `backend` `API`

## Priorité
🔴 Haute

## Contexte
Quand un utilisateur clique sur un lien d'invitation, le frontend doit pouvoir récupérer les informations de l'événement associé **sans nécessiter d'authentification**. Cet endpoint est public mais ne retourne que des informations limitées.

## User Story
> En tant qu'invité recevant un lien, je veux voir les détails de l'événement avant de décider de rejoindre, même si je n'ai pas encore de compte.

## Endpoint

```
GET /api/event/configuration/api/v1/invites/{token}
```

### Headers
Aucun header d'authentification requis (endpoint public).

### Response `200 OK`
```json
{
  "token": "abc123def456ghi789",
  "status": "VALID",
  "event": {
    "identifier": "uuid-event-id",
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

### Réponses d'erreur

#### Token expiré — `200 OK` (avec status EXPIRED)
```json
{
  "token": "abc123def456ghi789",
  "status": "EXPIRED",
  "event": null,
  "expires_at": 1711987200000
}
```

#### Token révoqué — `200 OK` (avec status REVOKED)
```json
{
  "token": "abc123def456ghi789",
  "status": "REVOKED",
  "event": null,
  "expires_at": null
}
```

#### Token max utilisations atteint — `200 OK` (avec status EXHAUSTED)
```json
{
  "token": "abc123def456ghi789",
  "status": "EXHAUSTED",
  "event": null,
  "expires_at": 1711987200000
}
```

#### Token inconnu — `404 Not Found`
```json
{
  "error": "INVITE_NOT_FOUND",
  "message": "This invitation link does not exist."
}
```

### Enum `InviteStatus`
| Valeur | Description |
|--------|-------------|
| `VALID` | Lien actif, utilisable |
| `EXPIRED` | Date d'expiration dépassée |
| `REVOKED` | Révoqué manuellement par l'organisateur |
| `EXHAUSTED` | Nombre max d'utilisations atteint |

## Règles métier
1. **Endpoint public** — Pas de Bearer token requis
2. Ne retourner les **détails de l'événement** que si le lien est `VALID`
3. Le `status` doit être **calculé dynamiquement** :
   - Si `status` en BDD = `REVOKED` → `REVOKED`
   - Si `expires_at < now()` → `EXPIRED`
   - Si `max_uses != null && current_uses >= max_uses` → `EXHAUSTED`
   - Sinon → `VALID`
4. **Informations limitées** : ne pas exposer l'email de l'organisateur, seulement son prénom
5. Le `participant_count` ne compte que les participants `CONFIRMED`
6. **Rate limiting** recommandé : 30 requêtes/minute par IP pour éviter le brute-force de tokens

## Sécurité
- ⚠️ Cet endpoint est **public** : ne pas exposer de données sensibles
- Le token doit être suffisamment long et aléatoire (≥20 caractères) pour résister au brute-force
- Envisager un rate limiting par IP
- Ne pas retourner d'information sur l'organisateur au-delà du prénom

## Tests à écrire
- [ ] Token valide → 200 avec détails de l'événement et status `VALID`
- [ ] Token expiré → 200 avec status `EXPIRED`, `event: null`
- [ ] Token révoqué → 200 avec status `REVOKED`, `event: null`
- [ ] Token épuisé → 200 avec status `EXHAUSTED`, `event: null`
- [ ] Token inexistant → 404
- [ ] Le `participant_count` ne compte que les `CONFIRMED`
- [ ] L'email de l'organisateur n'est pas exposé
- [ ] Le status est calculé dynamiquement (modifier `expires_at` en BDD et revérifier)

## Dépendances
- **BACK-001** — Modèle de données `event_invites` et génération de token

## Estimation
**M** (2-3 jours)
