# Prompt pour l'agent backend (happyrow-core) — Vérification endpoint PUT update participant status

## Contexte

Côté front-end (happyrow-front), on vient d'implémenter une feature qui permet à un participant invité à un événement de **modifier son propre statut** (INVITED → CONFIRMED ou DECLINED).

Le front appelle l'endpoint suivant :

```
PUT /event/configuration/api/v1/events/{eventId}/participants/{userEmail}

Headers:
  Content-Type: application/json
  Authorization: Bearer <JWT Supabase>

Body:
{
  "status": "CONFIRMED"   // ou "DECLINED"
}

Response attendue (200 OK):
{
  "user_email": "user@example.com",
  "event_id": "uuid",
  "status": "CONFIRMED",
  "joined_at": 1234567890,
  "updated_at": 1234567890
}
```

## Ce qu'il faut vérifier / corriger côté back-end

### 1. L'endpoint PUT existe-t-il ?

Vérifie qu'il existe une route `PUT /event/configuration/api/v1/events/{eventId}/participants/{userEmail}` dans le routing Ktor.

Si elle n'existe pas, crée-la avec le comportement décrit ci-dessous.

### 2. Autorisation : un participant peut modifier SON PROPRE statut

L'endpoint doit autoriser **deux cas** :
- **L'organisateur** de l'événement (`event.creator == authenticatedUserEmail`) peut modifier le statut de n'importe quel participant
- **Le participant lui-même** (`userEmail == authenticatedUserEmail`) peut modifier **son propre** statut

Vérifie que la logique d'autorisation ne bloque pas un participant qui veut changer son propre statut. Si seul l'organisateur est autorisé actuellement, il faut ajouter la condition `isSelf`.

Pseudo-code attendu :
```kotlin
val authenticatedEmail = // extraire l'email du JWT Supabase
val event = eventRepository.findById(eventId)
val isOrganizer = event.creator == authenticatedEmail
val isSelf = userEmail == authenticatedEmail

if (!isOrganizer && !isSelf) {
    call.respond(HttpStatusCode.Forbidden, "Not authorized to update this participant's status")
    return@put
}
```

### 3. Valeurs de statut acceptées

Le back-end doit accepter les valeurs suivantes pour le champ `status` :
- `INVITED`
- `CONFIRMED`
- `MAYBE`
- `DECLINED`

Vérifie que l'enum ou la validation côté back accepte bien ces 4 valeurs et renvoie une erreur 400 si une valeur invalide est envoyée.

### 4. Mise à jour en base de données

La requête SQL doit mettre à jour le statut ET le timestamp `updated_at` :
```sql
UPDATE participant
SET status = :status, updated_at = NOW()
WHERE event_id = :eventId AND user_email = :userEmail
```

Vérifie que :
- La colonne `updated_at` existe dans la table `participant`
- Elle est bien mise à jour lors du changement de statut
- Si le participant n'existe pas, l'endpoint renvoie une erreur 404

### 5. Format de la réponse

La réponse doit respecter ce format (snake_case, timestamps en epoch milliseconds) :
```json
{
  "user_email": "user@example.com",
  "event_id": "uuid-de-levenement",
  "status": "CONFIRMED",
  "joined_at": 1710000000000,
  "updated_at": 1710000001000
}
```

### 6. Cas limites à gérer

- **Participant inexistant** : renvoyer 404 si le couple (eventId, userEmail) n'existe pas dans la table participant
- **Événement inexistant** : renvoyer 404 si l'eventId n'existe pas
- **Statut invalide** : renvoyer 400 si la valeur du statut n'est pas dans l'enum
- **JWT manquant/invalide** : renvoyer 401 (normalement déjà géré par le middleware d'auth)

## Résumé des actions

1. Vérifie que la route PUT existe
2. Vérifie/corrige l'autorisation (organisateur OU participant lui-même)
3. Vérifie la validation du statut
4. Vérifie la mise à jour en BDD (status + updated_at)
5. Vérifie le format de réponse (snake_case, epoch ms)
6. Vérifie la gestion des erreurs (404, 400, 401, 403)
7. Si des tests existent, ajoute un test pour le cas "participant met à jour son propre statut"
