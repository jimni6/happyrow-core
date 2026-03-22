# Backend : Ajouter `user_name` dans la réponse participant

## Contexte

Le front-end affiche désormais les participants par leur nom plutôt que par leur email.
Le front attend un champ optionnel `user_name` dans la réponse JSON de chaque participant.

**Comportement actuel du front :**
- Si `user_name` est présent → affiche le nom
- Si `user_name` est absent/null → affiche la partie avant `@` de l'email (fallback)

## Ce qu'il faut faire

### 1. Modifier le DTO `ParticipantResponse`

Ajouter un champ `user_name` (nullable) dans la data class de réponse participant :

```kotlin
@Serializable
data class ParticipantResponse(
    val user_email: String,
    val user_name: String? = null,  // <-- AJOUTER
    val event_id: String,
    val status: String,
    val joined_at: Long,
    val updated_at: Long? = null
)
```

### 2. Récupérer le nom de l'utilisateur

Quand on construit la réponse participant, il faut récupérer le nom de l'utilisateur.

**Option A (recommandée) — Jointure SQL :**

Dans le `SqlParticipantRepository` (ou équivalent), modifier la requête `SELECT` pour joindre avec la table `users` (ou `auth.users` si Supabase) :

```sql
SELECT p.user_email, p.event_id, p.status, p.joined_at, p.updated_at,
       u.firstname, u.lastname
FROM participant p
LEFT JOIN users u ON p.user_email = u.email
WHERE p.event_id = :eventId
```

Puis construire `user_name` comme `"${firstname} ${lastname}"`.trim() ou `null` si les deux sont vides.

**Option B — Enrichissement dans le use case :**

Si la jointure n'est pas possible, injecter un `UserRepository` dans le use case et enrichir chaque participant avec le nom de l'utilisateur après la requête.

### 3. Mapper vers la réponse

Dans l'endpoint ou le mapper, s'assurer que `user_name` est inclus :

```kotlin
fun Participant.toResponse(): ParticipantResponse = ParticipantResponse(
    user_email = this.userEmail,
    user_name = this.userName,  // <-- AJOUTER
    event_id = this.eventId,
    status = this.status.name,
    joined_at = this.joinedAt.toEpochMilliseconds(),
    updated_at = this.updatedAt?.toEpochMilliseconds()
)
```

### 4. Mettre à jour le modèle domaine si nécessaire

Si le domain model `Participant` n'a pas de champ `userName`, l'ajouter :

```kotlin
data class Participant(
    val userEmail: String,
    val userName: String? = null,  // <-- AJOUTER
    val eventId: String,
    val status: ParticipantStatus,
    val joinedAt: Instant,
    val updatedAt: Instant? = null
)
```

### 5. Vérifications

- `./gradlew compileKotlin` → 0 erreur
- `./gradlew spotlessApply`
- `./gradlew detekt`
- Tester : `GET /events/{eventId}/participants` doit retourner `user_name` pour chaque participant

### 6. Format de réponse attendu

```json
[
  {
    "user_email": "juju@f.co",
    "user_name": "Julien Dupont",
    "event_id": "abc-123",
    "status": "CONFIRMED",
    "joined_at": 1710000000000,
    "updated_at": 1710000060000
  },
  {
    "user_email": "pierre@gmail.com",
    "user_name": null,
    "event_id": "abc-123",
    "status": "INVITED",
    "joined_at": 1710000000000
  }
]
```

**Important :** `user_name` peut être `null` si l'utilisateur n'a pas de nom enregistré. Le front gère ce cas avec un fallback.
