# FEAT-001 — Bouton "Partager" et génération du lien d'invitation

## Type
`feature` `frontend`

## Priorité
🔴 Haute

## Contexte
Actuellement, l'organisateur doit connaître l'email exact de chaque participant pour l'inviter. Un lien de partage permettrait d'inviter n'importe qui sans friction.

## User Story
> En tant qu'organisateur d'un événement, je veux pouvoir générer un lien d'invitation unique afin de le partager facilement avec mes invités.

## Spécifications

### UI / Comportement
1. Ajouter un bouton **"Partager"** dans le header de `EventDetailsView`, visible uniquement par l'organisateur (à côté du bouton "Edit")
2. Au clic, ouvrir un **modal de partage** contenant :
   - Le lien d'invitation (champ texte read-only)
   - Un bouton **"Copier le lien"** (avec feedback visuel "Copié !")
   - Un bouton **"Partager"** utilisant l'API native `navigator.share()` si disponible (fallback: masqué)
   - Le **QR code** correspondant au lien (voir FEAT-002)
3. Le lien est généré via un appel API `POST /events/:eventId/invites` (voir issue backend BACK-001)
4. Le lien a le format : `https://happyrow.app/invite/:token`

### Composants à créer
- `ShareInviteModal.tsx` — Modal de partage avec lien + actions
- `ShareInviteModal.css` — Styles du modal

### Composants à modifier
- `EventDetailsView.tsx` — Ajouter le bouton "Partager" et intégrer le modal

### Logique métier
- `src/features/events/types/InviteLink.ts` — Types `InviteLink`, `InviteLinkCreationRequest`
- `src/features/events/use-cases/CreateInviteLink.ts` — Use case de création
- `HttpEventRepository.ts` — Ajouter la méthode `createInviteLink(eventId)`

### Maquette (texte)
```
┌──────────────────────────────────────┐
│         Partager l'événement         │
│                                      │
│  🔗 https://happyrow.app/invite/abc │
│                                      │
│  [ 📋 Copier le lien ]  [ 📤 Partager ] │
│                                      │
│         ┌─────────────┐              │
│         │  QR CODE    │              │
│         │  (FEAT-002) │              │
│         └─────────────┘              │
│                                      │
│  ⏳ Ce lien expire dans 7 jours      │
└──────────────────────────────────────┘
```

## Critères d'acceptation
- [ ] Le bouton "Partager" est visible uniquement pour l'organisateur
- [ ] Le modal affiche le lien d'invitation
- [ ] Le bouton "Copier" copie le lien dans le presse-papier avec feedback visuel
- [ ] Le bouton "Partager" natif fonctionne sur mobile (ou est masqué si non supporté)
- [ ] Un état de chargement est affiché pendant la génération du lien
- [ ] Les erreurs API sont gérées et affichées dans le modal
- [ ] La date d'expiration du lien est affichée

## Dépendances
- **BACK-001** —  
- **FEAT-002** — Composant QR Code (peut être développé en parallèle)

## Estimation
**M** (3-5 jours)
