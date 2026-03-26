# FEAT-004 — Intégration Web Share API & Copy to Clipboard

## Type
`feature` `frontend`

## Priorité
🟡 Moyenne

## Contexte
Pour maximiser la viralité, il faut exploiter les canaux de partage natifs du téléphone (WhatsApp, SMS, Telegram, etc.) via la Web Share API, et assurer un fallback robuste avec le copier-coller.

## User Story
> En tant qu'organisateur, je veux partager le lien d'invitation via WhatsApp, SMS ou tout autre canal disponible sur mon téléphone, directement depuis l'app.

## Spécifications

### Web Share API (`navigator.share`)
- Disponible sur la majorité des mobiles (iOS Safari, Chrome Android)
- Non disponible sur desktop (Firefox, certains navigateurs)
- Partager un objet :
  ```js
  navigator.share({
    title: `Rejoins "${eventName}" sur HappyRow !`,
    text: `${organizerName} t'invite à "${eventName}" le ${eventDate}. Clique pour rejoindre :`,
    url: inviteLink,
  });
  ```

### Clipboard API (`navigator.clipboard`)
- Copier le lien avec feedback visuel
- Texte du bouton : "Copier le lien" → "Copié ✓" pendant 2 secondes
- Fallback pour les navigateurs anciens : `document.execCommand('copy')`

### Composants à créer
- `src/shared/components/ShareButton.tsx` — Bouton de partage natif réutilisable
- `src/shared/components/CopyButton.tsx` — Bouton copier avec feedback réutilisable

### API des composants

```tsx
// ShareButton — masqué automatiquement si Web Share API non supportée
interface ShareButtonProps {
  title: string;
  text: string;
  url: string;
  className?: string;
  children?: React.ReactNode;
}

// CopyButton — toujours visible
interface CopyButtonProps {
  textToCopy: string;
  label?: string;          // default: "Copier le lien"
  copiedLabel?: string;    // default: "Copié ✓"
  copiedDurationMs?: number; // default: 2000
  className?: string;
}
```

### Intégration
- Intégrés dans `ShareInviteModal` (FEAT-001)
- Réutilisables pour d'autres features futures (partage de profil, partage de liste de courses, etc.)

## Critères d'acceptation
- [ ] Le bouton "Partager" utilise `navigator.share()` sur mobile
- [ ] Le bouton "Partager" est masqué sur les navigateurs non compatibles
- [ ] Le bouton "Copier" copie le lien et affiche un feedback "Copié ✓"
- [ ] Le fallback clipboard fonctionne sur les navigateurs anciens
- [ ] Les composants sont génériques et réutilisables
- [ ] Le message partagé contient le nom de l'événement, la date et le lien

## Dépendances
- **FEAT-001** — Modal de partage (intégration)

## Estimation
**S** (1-2 jours)
