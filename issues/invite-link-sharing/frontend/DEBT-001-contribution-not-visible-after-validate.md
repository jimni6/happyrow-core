# DEBT-001 — La contribution n'est pas visible immédiatement après validation

## Type
`bug` `tech-debt` `frontend`

## Priorité
🔴 Haute

## Description du problème
Quand un utilisateur ajoute une contribution à une ressource et clique sur "Validate", le texte **"Your contribution: X"** n'apparaît pas immédiatement. L'utilisateur doit **rafraîchir manuellement la page** pour voir sa contribution.

### Comportement actuel
1. L'utilisateur clique `+` pour sélectionner une quantité (ex : 6)
2. L'utilisateur clique "Validate"
3. Le spinner "Saving..." apparaît puis disparaît
4. ❌ "Your contribution: 6" **n'est pas affiché**
5. L'utilisateur rafraîchit la page (F5)
6. ✅ "Your contribution: 6" **apparaît correctement**

### Comportement attendu
Après l'étape 3, "Your contribution: 6" doit apparaître **immédiatement** sans nécessiter de rafraîchissement.

---

## Analyse technique

### Flow actuel
```
ResourceItem.handleValidate()
  → onAddContribution(resourceId, quantity)
    → useContributionOperations.addContribution()
      → POST /events/{id}/resources/{id}/contributions   ✅ (succès)
      → GET  /events/{id}/resources                      ⚠️ (données potentiellement stale)
      → setResources(eventResources)                     → re-render avec données stale
  → setSelectedQuantity(0)
```

### Causes racines identifiées

#### Cause 1 — Read-after-write inconsistency (backend)
Le `GET /resources` est appelé **immédiatement** après le `POST /contributions`. Si le backend utilise de l'event sourcing, un cache, ou une réplication asynchrone, le GET peut retourner les données **avant** que la contribution ne soit persistée/projetée. Au rafraîchissement de la page, la donnée a eu le temps de se propager.

**Fichier** : `src/features/resources/hooks/useContributionOperations.ts` (lignes 42-53)
```typescript
// POST contribution
await addContributionUseCase.execute({ eventId, resourceId, userId, quantity });

// GET resources immédiatement après → peut retourner des données stale
const eventResources = await getResourcesUseCase.execute({ eventId });
setResources(eventResources);
```

#### Cause 2 — Mismatch `currentUserId` (email vs UUID)
Dans `EventDetailsView.tsx`, le `currentUserId` passé aux `ResourceCategorySection` est `user?.email` :
```typescript
// EventDetailsView.tsx lignes 239, 249
currentUserId={user?.email || ''}
```

Mais dans `ResourceItem.tsx`, ce `currentUserId` est comparé à `resource.contributors[].userId` :
```typescript
// ResourceItem.tsx ligne 24
const userContribution = resource.contributors.find(
  c => c.userId === currentUserId
);
```

Si le backend retourne des **UUIDs** dans `contributors[].userId` (cf. migration récente), alors `c.userId` (UUID) ≠ `currentUserId` (email) → la contribution n'est **jamais** trouvée côté frontend.

> **Note** : le même problème existe pour `myContributions` dans `EventDetailsView.tsx` (lignes 176-186) qui utilise aussi `user?.email`.

---

## Solutions proposées

### Fix 1 — Corriger le `currentUserId` (quick fix, prioritaire)
Remplacer `user?.email` par `user?.id` dans `EventDetailsView.tsx` :

```typescript
// Avant
currentUserId={user?.email || ''}

// Après  
currentUserId={user?.id || ''}
```

**Fichiers à modifier** :
- `src/features/events/views/EventDetailsView.tsx`
  - Ligne 239 : `currentUserId={user?.id || ''}`
  - Ligne 249 : `currentUserId={user?.id || ''}`
  - Ligne 177 : `const userId = user?.id || '';` (pour `myContributions`)
  - Ligne 276 : vérifier `currentUserEmail` si similaire

### Fix 2 — Optimistic UI update (meilleure UX)
Au lieu de re-fetch **toutes** les ressources après un POST, mettre à jour l'état local **immédiatement** avec les données attendues :

```typescript
// Dans useContributionOperations.addContribution
await addContributionUseCase.execute({ eventId, resourceId, userId, quantity });

// Optimistic update: mettre à jour le state local immédiatement
setResources(prev =>
  prev.map(r =>
    r.id === resourceId
      ? {
          ...r,
          currentQuantity: r.currentQuantity + quantity,
          contributors: [
            ...r.contributors,
            { userId, quantity, contributedAt: new Date() },
          ],
        }
      : r
  )
);

// Puis re-fetch en arrière-plan pour synchroniser (sans bloquer l'UI)
getResourcesUseCase.execute({ eventId }).then(setResources).catch(console.error);
```

### Fix 3 — Délai avant re-fetch (workaround temporaire)
Si le problème est confirmé côté backend (eventual consistency) :

```typescript
await addContributionUseCase.execute({ eventId, resourceId, userId, quantity });
// Laisser le temps au backend de propager
await new Promise(resolve => setTimeout(resolve, 300));
const eventResources = await getResourcesUseCase.execute({ eventId });
setResources(eventResources);
```

> ⚠️ Hack — à utiliser uniquement en dernier recours.

---

## Recommandation

**Appliquer Fix 1 + Fix 2 ensemble** :
1. **Fix 1** corrige le mismatch d'identifiant (quick win, 15 min)
2. **Fix 2** assure une UX instantanée même en cas de latence backend (1-2h)

---

## Fichiers impactés

| Fichier | Modification |
|---------|-------------|
| `src/features/events/views/EventDetailsView.tsx` | Remplacer `user?.email` par `user?.id` pour `currentUserId` et `myContributions` |
| `src/features/resources/hooks/useContributionOperations.ts` | Ajouter optimistic update dans `addContribution`, `updateContribution`, `deleteContribution` |
| `src/features/resources/components/ResourceItem.tsx` | Aucune modification nécessaire (le composant est correct) |

## Critères d'acceptation
- [ ] Après validation d'une contribution, "Your contribution: X" apparaît **instantanément**
- [ ] La quantité totale de la ressource se met à jour **instantanément**
- [ ] Le comportement est identique pour l'ajout, la modification et la suppression de contributions
- [ ] En cas d'erreur backend, l'UI revient à l'état précédent (rollback de l'optimistic update)
- [ ] Les données restent cohérentes après un rafraîchissement de page

## Estimation
**S** (1-2 jours)
