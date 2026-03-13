<!-- Slides 21-24 — Timing: 4 min -->

# Réalisations — Interfaces utilisateur

## Slide 21 — Captures d'écran : Dashboard / HomePage (45s)

> *[Insérer capture d'écran : HomePage avec les EventCards]*

- Le dashboard affiche les événements sous forme de cartes (`EventCard`)
- Chaque carte montre : date, nom, nombre de participants, localisation
- Barre de navigation : profil, créer un événement (+), accueil
- **3 dépendances de production** seulement (React, React Router, Supabase)
- PWA installable avec mode offline (Service Worker Workbox)

---

## Slide 22 — Captures d'écran : EventDetailsView (45s)

> *[Insérer capture d'écran : EventDetailsView avec les ressources et contrôles +/−]*

- Vue détail d'un événement avec ressources organisées par catégorie (Food, Drinks…)
- Chaque ressource affiche : quantité courante / suggérée
- Contrôles de contribution : boutons **+/−** pour sélectionner un delta
- Bouton **Validate** visible uniquement quand un delta est sélectionné
- Design Tokens CSS : teal `#5FBDB4`, navy `#3D5A6C`, coral `#E6A19A`

---

## Slide 23 — Code React : ResourceItem (1 min 30s)

```typescript
// src/features/resources/components/ResourceItem.tsx
export const ResourceItem: React.FC<ResourceItemProps> = ({
  resource, currentUserId,
  onAddContribution, onUpdateContribution, onDeleteContribution,
}) => {
  const [selectedQuantity, setSelectedQuantity] = useState(0);
  const [isSaving, setIsSaving] = useState(false);

  const userContribution = resource.contributors.find(
    c => c.userId === currentUserId
  );
  const userQuantity = userContribution?.quantity || 0;

  const handleValidate = async () => {
    try {
      setIsSaving(true);
      const newQuantity = userQuantity + selectedQuantity;
      if (newQuantity <= 0) await onDeleteContribution(resource.id);
      else if (userQuantity === 0) await onAddContribution(resource.id, selectedQuantity);
      else await onUpdateContribution(resource.id, newQuantity);
      setSelectedQuantity(0);
    } catch (error) {
      console.error('Error updating contribution:', error);
    } finally {
      setIsSaving(false);
    }
  };
  // ... rendu JSX avec boutons +/- et Validate
};
```

- Logique **add/update/delete** déterminée par l'état de la contribution existante
- `selectedQuantity` = delta par rapport à la contribution actuelle
- Bouton Validate visible uniquement quand un delta est sélectionné

---

## Slide 24 — Architecture front-end Clean Architecture (1 min)

### Inversion de dépendance côté front-end

```
[Composants React] → [Context/Provider] → [Use Cases] → [Repository (interface)] → [HttpRepository (fetch)]
```

### Organisation par features

```
src/features/
├── auth/           → AuthProvider, AuthRepository, LoginModal
├── events/         → EventProvider, EventCard, CreateEventForm
├── participants/   → ParticipantProvider, ParticipantList
├── resources/      → ResourceProvider, ResourceItem
└── contributions/  → ContributionProvider, HttpContributionRepository
```

### Injection du token JWT

```typescript
// Le token est injecté via callback getToken dans les providers
async createContribution(data: ContributionCreationRequest): Promise<Contribution> {
  const token = this.getToken();
  if (!token) throw new Error('Authentication required');
  const response = await fetch(`${this.baseUrl}/...`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ quantity: data.quantity }),
  });
  // ...
}
```

- Même principe d'architecture que le back-end : cohérence architecturale
- Chaque module isolé dans `src/features/` avec composants, hooks, services, types
- API native `fetch` — pas d'Axios, pas de dépendance tierce
