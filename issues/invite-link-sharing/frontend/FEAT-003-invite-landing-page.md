# FEAT-003 — Landing page d'invitation (route `/invite/:token`)

## Type
`feature` `frontend`

## Priorité
🔴 Haute

## Contexte
Quand un utilisateur clique sur un lien d'invitation, il arrive sur une page dédiée qui affiche les détails de l'événement et lui permet de rejoindre. Cette page doit fonctionner pour les utilisateurs connectés ET non connectés.

## User Story
> En tant qu'invité recevant un lien de partage, je veux voir les détails de l'événement et pouvoir le rejoindre facilement, que j'aie déjà un compte ou non.

## Spécifications

### Route
- `GET /invite/:token` — Nouvelle route publique dans `App.tsx`

### Flow utilisateur

```
Utilisateur clique sur le lien
        │
        ▼
   Page d'invitation
   (infos événement)
        │
        ├── Connecté ?
        │      │
        │      ├── OUI → Bouton "Rejoindre l'événement"
        │      │          → POST /invites/:token/accept
        │      │          → Redirect vers /events/:eventId
        │      │
        │      └── NON → Deux boutons :
        │                 • "Se connecter et rejoindre"
        │                 │  → Login modal → auto-join → redirect
        │                 • "Créer un compte et rejoindre"
        │                    → Register modal → auto-join → redirect
        │
        └── Token invalide/expiré ?
               → Message d'erreur + bouton retour accueil
```

### UI — Page d'invitation
La page affiche (données retournées par `GET /invites/:token`) :
- **Nom de l'événement**
- **Date et heure**
- **Lieu**
- **Type** (icône + label)
- **Organisateur** (prénom)
- **Nombre de participants confirmés**
- Bouton(s) d'action selon l'état d'authentification

### Composants à créer
- `src/features/invite/views/InviteLandingPage.tsx` — Page principale
- `src/features/invite/views/InviteLandingPage.css` — Styles
- `src/features/invite/types/Invite.ts` — Types `InviteDetails`, `InviteStatus`
- `src/features/invite/services/HttpInviteRepository.ts` — API calls
- `src/features/invite/use-cases/GetInviteDetails.ts` — Récupérer les détails via token
- `src/features/invite/use-cases/AcceptInvite.ts` — Accepter l'invitation
- `src/features/invite/index.ts` — Barrel export

### Composants à modifier
- `App.tsx` — Ajouter la route `/invite/:token` (accessible sans auth)

### Types

```typescript
// src/features/invite/types/Invite.ts

export enum InviteStatus {
  VALID = 'VALID',
  EXPIRED = 'EXPIRED',
  REVOKED = 'REVOKED',
  ALREADY_MEMBER = 'ALREADY_MEMBER',
}

export interface InviteDetails {
  token: string;
  eventId: string;
  eventName: string;
  eventDate: Date;
  eventLocation: string;
  eventType: string;
  organizerName: string;
  participantCount: number;
  status: InviteStatus;
  expiresAt: Date;
}
```

### Maquette (texte)

```
┌──────────────────────────────────────┐
│          🎉 Tu es invité(e) !        │
│                                      │
│    ┌────────────────────────────┐    │
│    │  Anniversaire de Marie     │    │
│    │                            │    │
│    │  📅 Samedi 15 mars, 19h00  │    │
│    │  📍 12 rue de la Paix      │    │
│    │  🎂 Anniversaire           │    │
│    │  👤 Organisé par Marie     │    │
│    │  👥 8 participants         │    │
│    └────────────────────────────┘    │
│                                      │
│  ┌──────────────────────────────┐    │
│  │    Rejoindre l'événement     │    │
│  └──────────────────────────────┘    │
│                                      │
│    ou                                │
│                                      │
│  Se connecter  •  Créer un compte    │
└──────────────────────────────────────┘
```

### États d'erreur
- **Token expiré** : "Ce lien d'invitation a expiré. Demandez un nouveau lien à l'organisateur."
- **Token invalide** : "Ce lien d'invitation n'est pas valide."
- **Déjà membre** : "Tu participes déjà à cet événement !" + lien vers l'événement
- **Erreur réseau** : Message d'erreur générique + bouton "Réessayer"

## Critères d'acceptation
- [ ] La page s'affiche correctement avec les détails de l'événement
- [ ] Un utilisateur connecté peut rejoindre en un clic
- [ ] Un utilisateur non connecté peut se connecter/s'inscrire puis est automatiquement ajouté
- [ ] Les tokens expirés/invalides affichent un message d'erreur clair
- [ ] Un utilisateur déjà participant est redirigé vers l'événement
- [ ] La page est responsive et fonctionne sur mobile
- [ ] La page a un design attractif (c'est le premier contact pour de nouveaux utilisateurs)
- [ ] Le token est stocké temporairement (localStorage) pour survivre au flow login/register

## Dépendances
- **BACK-002** — API de validation du token d'invitation
- **BACK-003** — API d'acceptation de l'invitation

## Estimation
**L** (5-8 jours)
