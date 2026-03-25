# Plan de verification front-end — Post-audit back-end

> **Date** : 25/03/2026
> **Contexte** : Le back-end `happyrow-core` a subi une serie de correctifs d'audit (issues #44 a #63).
> Ce document liste les changements impactants et les verifications a effectuer cote front-end.

---

## 1. Changements cassants (BREAKING CHANGES)

### 1.1 Le champ `creator` est maintenant un UUID (et non plus un email)

| | Avant | Apres |
|---|---|---|
| `event.creator` | `"jean@example.com"` | `"ab70634a-345e-415e-8417-60841b6bcb20"` |
| `event.members[]` | `["ab70634a-..."]` (UUID) | `["ab70634a-..."]` (UUID) — inchange |

**Commits** : `d575d45` (issue #44)

**Impact front-end :**

- [ ] **Affichage du createur** : Si le front affiche `event.creator` comme nom/email, il affichera un UUID.
  - **Fix** : Utiliser `event.creator` uniquement comme identifiant technique. Pour afficher le nom du createur, croiser avec la liste des participants (`GET /events/{id}/participants`) ou le champ `email` du JWT decode.
- [ ] **Comparaison "suis-je le createur ?"** : Si le front fait `event.creator === currentUser.email`, la condition ne sera plus jamais vraie.
  - **Fix** : Comparer avec le `sub` (userId) du JWT : `event.creator === jwtPayload.sub`.
- [ ] **Membres** : `event.members` etait deja en UUID — pas de changement.

### 1.2 Nouvelles reponses 403 Forbidden (controle d'acces IDOR)

4 endpoints retournent desormais **403** si l'utilisateur n'est ni le createur ni un participant de l'evenement :

| Endpoint | Methode |
|---|---|
| `/events/{eventId}/participants` | `GET` |
| `/events/{eventId}/participants` | `POST` |
| `/events/{eventId}/resources` | `GET` |
| `/events/{eventId}/resources` | `POST` |

**Format de la reponse 403 :**
```json
{
  "type": "FORBIDDEN",
  "detail": "You do not have access to this event"
}
```

**Commits** : `b28bd98` (issues #49, #51)

**Impact front-end :**

- [ ] **Gestion d'erreur** : Ajouter un handler pour le status 403 sur ces 4 endpoints.
  - Suggestion : afficher un message "Vous n'avez pas acces a cet evenement" et rediriger vers la liste des evenements.
- [ ] **Navigation** : S'assurer que le front ne propose pas de liens vers des evenements auxquels l'utilisateur n'a pas acces (normalement deja le cas si `GET /events` ne retourne que les evenements de l'utilisateur).

---

## 2. Nouvelles validations serveur (400 Bad Request)

### 2.1 Date d'evenement obligatoirement dans le futur

**Commit** : `dfeec93` (issue #57)

La creation d'un evenement avec une date passee retourne desormais :
```json
{ "type": "BAD_REQUEST", "detail": "Event date must be in the future" }
```

- [ ] **Formulaire de creation** : Ajouter une validation front (date picker avec `min=today+1`).
- [ ] **Formulaire d'edition** : Idem — verifier que la date selectionnee est dans le futur.
- [ ] **Affichage d'erreur** : Gerer le 400 et afficher un message utilisateur.

### 2.2 Limite haute sur les quantites (max 10 000)

**Commit** : `dfeec93` (issue #58)

| Champ | Plage valide |
|---|---|
| Quantite de contribution (ajout) | `1` a `10 000` |
| Quantite de contribution (reduction) | `1` a `10 000` |
| Quantite initiale de resource | `1` a `10 000` |
| Quantite suggeree de resource | `0` a `10 000` |

Reponse en cas de depassement :
```json
{ "type": "BAD_REQUEST", "detail": "Contribution quantity must be between 1 and 10000" }
```

- [ ] **Champs de saisie** : Ajouter `max="10000"` sur tous les inputs de quantite.
- [ ] **Validation front** : Bloquer la soumission si quantite > 10 000.

### 2.3 Validation des noms et localisations

**Commit** : `8469588` (issue #63)

Le back-end valide desormais dans la couche domaine (en plus des DTOs) :
- `event.name` ne peut pas etre vide
- `event.location` ne peut pas etre vide
- `resource.name` ne peut pas etre vide

- [ ] Verifier que les formulaires front ont deja ces validations (probablement oui).

---

## 3. Changements comportementaux (non cassants)

### 3.1 Transactions atomiques pour les contributions

**Commit** : `60833d6` (issue #61)

Avant, un echec lors de la mise a jour de la quantite de resource pouvait laisser une contribution enregistree sans la quantite mise a jour. Ce n'est plus le cas.

- [ ] **Rien a changer** cote front, mais les 409 Conflict (optimistic lock) sont maintenant plus fiables. En cas de 409, le front devrait recharger les donnees et reessayer.

### 3.2 Quantite de resource ne peut plus etre negative

**Commit** : `60833d6` (issue #54)

- [ ] **Rien a changer** — c'est une protection serveur. Verifier que le front ne gere pas un cas ou `currentQuantity < 0` (qui ne devrait plus arriver).

### 3.3 CORS restreint en production

**Commit** : `8469588` (issue #56)

Les origines `localhost:*` ne sont plus autorisees quand `ENVIRONMENT=production`.

- [ ] **Dev local** : S'assurer de ne PAS pointer vers l'API de production depuis localhost.
- [ ] **Vercel** : Les domaines `happyrow-front.vercel.app` et variantes restent autorises (HTTPS uniquement).
- [ ] **Nouveau domaine** : Si un nouveau domaine de deploy est ajoute, il faut le configurer via la variable d'environnement `ALLOWED_ORIGINS` cote back-end.

---

## 4. Checklist de test par fonctionnalite

### Evenements

| Test | Endpoint | Verification |
|---|---|---|
| Creer un evenement | `POST /events` | Le `creator` dans la reponse est un UUID (pas un email) |
| Creer avec date passee | `POST /events` | Retourne 400 |
| Creer avec nom vide | `POST /events` | Retourne 400 |
| Lister ses evenements | `GET /events` | Retourne uniquement les evenements ou je suis createur/participant |
| Modifier un evenement | `PUT /events/{id}` | Fonctionne normalement |
| Supprimer un evenement | `DELETE /events/{id}` | Seul le createur peut supprimer (403 sinon) |
| Verifier "suis-je le createur ?" | — | Comparer `event.creator` avec `jwt.sub` et non l'email |

### Participants

| Test | Endpoint | Verification |
|---|---|---|
| Lister les participants | `GET /events/{id}/participants` | 403 si non-membre de l'evenement |
| Ajouter un participant | `POST /events/{id}/participants` | 403 si non-membre de l'evenement |
| Modifier un participant | `PUT /events/{id}/participants/{email}` | Fonctionne (verif organizer deja en place) |
| Supprimer un participant | `DELETE /events/{id}/participants/{email}` | Fonctionne (seul l'organisateur) |

### Resources

| Test | Endpoint | Verification |
|---|---|---|
| Lister les resources | `GET /events/{id}/resources` | 403 si non-membre de l'evenement |
| Creer une resource | `POST /events/{id}/resources` | 403 si non-membre ; nom vide → 400 |
| Creer avec quantite > 10000 | `POST /events/{id}/resources` | Retourne 400 |

### Contributions

| Test | Endpoint | Verification |
|---|---|---|
| Ajouter une contribution | `POST /events/{id}/resources/{rid}/contributions` | Quantite 1-10000 seulement |
| Reduire une contribution | `POST /events/{id}/resources/{rid}/contributions/reduce` | Quantite 1-10000 ; pas plus que l'existant |
| Supprimer une contribution | `DELETE /events/{id}/resources/{rid}/contributions` | Fonctionne normalement |
| Conflit optimiste (409) | — | Recharger les donnees et reessayer |

---

## 5. Resume des actions front-end

| Priorite | Action | Issue liee |
|---|---|---|
| **CRITIQUE** | Remplacer `event.creator === user.email` par `event.creator === jwt.sub` | #44 |
| **CRITIQUE** | Ne plus afficher `event.creator` comme email (utiliser la liste participants) | #44 |
| **HAUTE** | Gerer les reponses 403 sur GET/POST participants et resources | #49, #51 |
| **MOYENNE** | Ajouter validation date future dans le formulaire de creation d'evenement | #57 |
| **MOYENNE** | Ajouter `max=10000` sur les champs de quantite | #58 |
| **BASSE** | Verifier la gestion du 409 Conflict (retry apres reload) | #53 |
| **BASSE** | Verifier que les origines CORS sont correctes en production | #56 |

---

## 6. Contrat API — Reponse Event (rappel)

```json
{
  "identifier": "781daeac-1726-4c84-9ebf-6422e2d61e69",
  "name": "Birthday Party",
  "description": "A fun birthday party",
  "eventDate": "2026-04-01T18:00:00Z",
  "creationDate": "2026-03-25T10:00:00Z",
  "updateDate": "2026-03-25T10:00:00Z",
  "creator": "ab70634a-345e-415e-8417-60841b6bcb20",
  "location": "Paris",
  "type": "BIRTHDAY",
  "members": ["ab70634a-345e-415e-8417-60841b6bcb20"]
}
```

> **Attention** : `creator` est desormais un **UUID** (= `sub` du JWT Supabase), pas un email.
