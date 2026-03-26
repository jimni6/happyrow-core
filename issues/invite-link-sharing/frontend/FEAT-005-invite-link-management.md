# FEAT-005 — Gestion des liens d'invitation (révocation, régénération)

## Type
`feature` `frontend`

## Priorité
🟢 Basse

## Contexte
L'organisateur doit pouvoir contrôler les liens d'invitation actifs : les révoquer si nécessaire, en générer de nouveaux, et voir leur état (actif, expiré).

## User Story
> En tant qu'organisateur, je veux pouvoir révoquer un lien d'invitation existant et en générer un nouveau, afin de garder le contrôle sur qui peut rejoindre mon événement.

## Spécifications

### UI / Comportement
1. Dans le `ShareInviteModal`, afficher l'état du lien actuel :
   - **Actif** : lien affiché + date d'expiration + bouton "Révoquer"
   - **Expiré** : message "Lien expiré" + bouton "Générer un nouveau lien"
   - **Aucun lien** : bouton "Générer un lien d'invitation"
2. Au clic sur **"Révoquer"**, afficher une confirmation puis appeler `DELETE /events/:eventId/invites/:token`
3. Au clic sur **"Générer un nouveau lien"**, appeler `POST /events/:eventId/invites`

### Composants à modifier
- `ShareInviteModal.tsx` — Ajouter la logique de gestion (états, révocation, régénération)

### Use cases à créer
- `RevokeInviteLink.ts` — Révoquer un lien existant
- `GetActiveInviteLink.ts` — Récupérer le lien actif d'un événement (s'il existe)

### Méthodes à ajouter dans le repository
- `HttpEventRepository.ts` :
  - `getActiveInviteLink(eventId: string): Promise<InviteLink | null>`
  - `revokeInviteLink(eventId: string, token: string): Promise<void>`

## Critères d'acceptation
- [ ] L'organisateur voit l'état du lien actuel (actif / expiré / aucun)
- [ ] L'organisateur peut révoquer un lien actif avec confirmation
- [ ] L'organisateur peut générer un nouveau lien
- [ ] Après révocation, l'ancien lien ne fonctionne plus
- [ ] L'interface se met à jour en temps réel après chaque action

## Dépendances
- **FEAT-001** — Modal de partage
- **BACK-004** — API de gestion des liens

## Estimation
**M** (2-3 jours)
