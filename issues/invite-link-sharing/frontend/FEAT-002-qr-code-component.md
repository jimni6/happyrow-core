# FEAT-002 — Composant QR Code pour lien d'invitation

## Type
`feature` `frontend`

## Priorité
🟡 Moyenne

## Contexte
Le QR code permet un partage ultra-rapide en face à face (montrer son écran) ou sur une affiche/message. C'est un complément essentiel au lien texte.

## User Story
> En tant qu'organisateur, je veux afficher un QR code de mon lien d'invitation afin que mes invités puissent le scanner directement avec leur téléphone.

## Spécifications

### UI / Comportement
1. Afficher un QR code dans le `ShareInviteModal` (FEAT-001)
2. Le QR code encode l'URL complète : `https://happyrow.app/invite/:token`
3. Bouton **"Télécharger le QR code"** pour sauvegarder l'image en PNG
4. Le QR code doit être lisible et de taille suffisante (~200x200px minimum)
5. Optionnel : ajouter le logo HappyRow au centre du QR code

### Composants à créer
- `QRCodeDisplay.tsx` — Composant réutilisable qui prend une URL et affiche le QR code
- `QRCodeDisplay.css` — Styles

### Dépendance npm
- Utiliser la librairie **`qrcode.react`** (légère, React-native, bien maintenue)
  - Alternative : `react-qr-code`

### API du composant
```tsx
interface QRCodeDisplayProps {
  url: string;
  size?: number;        // default: 200
  showDownload?: boolean; // default: true
  logoUrl?: string;     // optional center logo
}
```

## Critères d'acceptation
- [ ] Le QR code s'affiche correctement dans le modal de partage
- [ ] Le QR code est scannable avec un téléphone standard
- [ ] Le bouton "Télécharger" sauvegarde un PNG de bonne qualité
- [ ] Le composant est réutilisable (pas couplé au contexte d'invitation)
- [ ] Le QR code est responsive (s'adapte aux petits écrans)

## Dépendances
- **FEAT-001** — Modal de partage (intégration)

## Estimation
**S** (1-2 jours)
