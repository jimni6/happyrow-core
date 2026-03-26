# EPIC-001 — Lien de partage d'événement & QR Code

## Description
Permettre aux organisateurs de générer un lien d'invitation unique (+ QR code) pour partager un événement. Les invités peuvent rejoindre l'événement en cliquant sur le lien, qu'ils aient un compte ou non.

## Objectif produit
- **Réduire la friction d'invitation** : plus besoin de connaître l'email de chaque invité
- **Augmenter la viralité** : partage facile via WhatsApp, SMS, réseaux sociaux
- **Améliorer l'acquisition** : les invités sans compte découvrent l'app via le lien

## Impact estimé
🔴 **Très élevé** — C'est la feature #1 identifiée dans le classement produit

---

## Issues Frontend

| Issue | Titre | Priorité | Estimation | Dépendances |
|-------|-------|----------|------------|-------------|
| [FEAT-001](../issues/frontend/FEAT-001-share-invite-link-button.md) | Bouton "Partager" et génération du lien | 🔴 Haute | M (3-5j) | BACK-001 |
| [FEAT-002](../issues/frontend/FEAT-002-qr-code-component.md) | Composant QR Code | 🟡 Moyenne | S (1-2j) | FEAT-001 |
| [FEAT-003](../issues/frontend/FEAT-003-invite-landing-page.md) | Landing page d'invitation (`/invite/:token`) | 🔴 Haute | L (5-8j) | BACK-002, BACK-003 |
| [FEAT-004](../issues/frontend/FEAT-004-native-share-integration.md) | Web Share API & Copy to Clipboard | 🟡 Moyenne | S (1-2j) | FEAT-001 |
| [FEAT-005](../issues/frontend/FEAT-005-invite-link-management.md) | Gestion des liens (révocation, régénération) | 🟢 Basse | M (2-3j) | BACK-004 |

## Issues Backend

| Issue | Titre | Priorité | Estimation | Dépendances |
|-------|-------|----------|------------|-------------|
| [BACK-001](../issues/backend/BACK-001-generate-invite-link-api.md) | API de génération du lien d'invitation | 🔴 Haute | M (2-3j) | — |
| [BACK-002](../issues/backend/BACK-002-validate-invite-token-api.md) | API de validation du token (endpoint public) | 🔴 Haute | M (2-3j) | BACK-001 |
| [BACK-003](../issues/backend/BACK-003-accept-invite-api.md) | API d'acceptation d'une invitation | 🔴 Haute | M (3-4j) | BACK-001, BACK-002 |
| [BACK-004](../issues/backend/BACK-004-manage-invite-links-api.md) | API de gestion (get actif, révoquer) | 🟡 Moyenne | M (2-3j) | BACK-001 |

---

## Ordre d'implémentation recommandé

```
Phase 1 — Backend fondations
  BACK-001  Génération du lien        ──┐
                                        ├──▶ Phase 2
  BACK-002  Validation du token       ──┘
  
Phase 2 — Backend + Frontend en parallèle
  BACK-003  Acceptation invitation    ──┐
  FEAT-001  Modal de partage          ──┤
  FEAT-002  QR Code                   ──┤──▶ Phase 3
  FEAT-004  Share API & Clipboard     ──┘
  
Phase 3 — Frontend flow complet
  FEAT-003  Landing page /invite      ──┐
                                        ├──▶ Phase 4
Phase 4 — Gestion avancée              │
  BACK-004  API gestion des liens     ──┘
  FEAT-005  UI gestion des liens
```

## Estimation totale
- **Backend** : ~10-13 jours
- **Frontend** : ~12-20 jours
- **Total (en parallèle)** : ~3-4 sprints (en comptant les reviews et tests)

## Métriques de succès
- % d'événements avec au moins un lien généré
- Nombre de participants ajoutés via lien vs via email
- Taux de conversion : clic sur lien → inscription → participation
- Nombre de partages natifs (Web Share API)

## Notes techniques
- **API base URL** : `https://happyrow-core.onrender.com/event/configuration/api/v1`
- **Auth** : Supabase Bearer token
- **Frontend URL** : configurable, actuellement `https://happyrow.app`
- Le token d'invitation doit être stocké dans `localStorage` pour survivre au flow login/register
