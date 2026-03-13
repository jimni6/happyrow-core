# Prompts pour l'agent du repo happyrow-front

Ce fichier contient les prompts a donner a l'agent Cursor ouvert dans le repo `happyrow-front` pour collecter les elements manquants au dossier professionnel CDA.

Les resultats de ces prompts doivent etre integres dans les fichiers du `dossier_reac/` (back-end repo) aux emplacements indiques.

---

## Prompt 1 — Stack technique et architecture front-end

```
Je prépare mon dossier professionnel CDA (Concepteur Développeur d'Applications).
Le back-end du projet HappyRow est dans le repo happyrow-core (Kotlin/Ktor).
Ce repo (happyrow-front) est le front-end.

J'ai besoin que tu analyses le projet et que tu me fournisses :

1. La stack technique complète : framework (React, Vue, Angular, Svelte...), version, langage (JS/TS), gestionnaire de paquets, CSS framework, outils de build
2. La structure des dossiers du projet (arborescence)
3. Les dépendances principales du package.json
4. Comment l'authentification Supabase est gérée côté front (login, stockage du JWT, envoi dans les headers)
5. Comment les appels API vers le back-end sont faits (fetch, axios, service dédié...)
6. S'il y a des tests front-end et avec quels outils

Formate ta réponse en Markdown, prête à être insérée dans un dossier technique.
```

**Destination** : compléter `07_SPECIFICATIONS_TECHNIQUES.md` section front-end, et `05_GESTION_DE_PROJET.md` tableau environnement technique.

---

## Prompt 2 — Maquettes et enchaînement des écrans

```
Pour mon dossier professionnel CDA, j'ai besoin des maquettes / wireframes de l'application.

1. Liste-moi tous les écrans/pages de l'application (routes)
2. Pour chaque écran, décris :
   - Son rôle fonctionnel
   - Les composants principaux qu'il contient
   - Les interactions utilisateur possibles
3. Dessine un diagramme d'enchaînement des écrans en Mermaid (graph LR ou graph TB)
4. S'il existe des fichiers de maquettes (Figma export, wireframes, etc.), indique-moi leur emplacement

Le diagramme doit montrer la navigation : login → dashboard → détail événement → modals (création, contribution, etc.)

Formate ta réponse en Markdown.
```

**Destination** : compléter `06_SPECIFICATIONS_FONCTIONNELLES.md` section 6.3 (Maquettes et enchaînement).

---

## Prompt 3 — Captures d'écran et code des interfaces utilisateur

```
Pour mon dossier professionnel CDA, j'ai besoin de captures d'écran de l'application et du code correspondant.

Pour chacun des écrans suivants, fournis-moi :
1. Une description textuelle de ce qui est affiché (je ferai les captures moi-même)
2. Le code source complet du composant principal de la page
3. Le code du composant qui appelle l'API back-end (service/fetch)

Écrans demandés :
- Page de connexion (login via Supabase)
- Dashboard / liste des événements
- Page de détail d'un événement (avec participants, ressources, contributions)
- Modal ou formulaire de création d'événement
- Modal ou formulaire d'ajout de contribution à une ressource

Formate le code en blocs Kotlin/TypeScript/JavaScript avec le chemin du fichier en commentaire.
```

**Destination** : compléter `08_REALISATIONS.md` section 8.1 (captures d'écran + code interfaces), et `ANNEXES.md` Annexes A et B.

---

## Prompt 4 — Code d'appel API et gestion du JWT côté client

```
Pour mon dossier professionnel CDA, j'ai besoin du code qui gère :

1. L'authentification côté front :
   - Comment le login Supabase est déclenché
   - Comment le JWT est stocké (localStorage, cookie, contexte React/Vue...)
   - Comment le JWT est envoyé dans les headers des requêtes API
   - Comment la déconnexion est gérée
   - Comment les routes protégées sont implémentées (guard, middleware, redirect...)

2. Les appels API vers le back-end :
   - Le fichier/module de service API (baseURL, headers, interceptors...)
   - Un exemple d'appel POST (création d'événement)
   - Un exemple d'appel GET (liste des événements)
   - La gestion des erreurs côté client (401, 409 optimistic lock, 500...)

Fournis le code complet des fichiers concernés avec le chemin.
```

**Destination** : compléter `08_REALISATIONS.md` section 8.1, `09_SECURITE.md` (auth côté client), et `ANNEXES.md` Annexe E.

---

## Prompt 5 — Tests front-end

```
Pour mon dossier professionnel CDA, j'ai besoin de savoir si des tests front-end existent.

1. Y a-t-il des tests unitaires ? Si oui, avec quel framework (Jest, Vitest, Testing Library...) ?
2. Y a-t-il des tests end-to-end (Cypress, Playwright...) ?
3. Fournis un exemple de test existant (le plus significatif)
4. S'il n'y a pas de tests, indique-le clairement

Formate ta réponse en Markdown.
```

**Destination** : compléter `10_PLAN_DE_TESTS.md` (tests front-end).

---

## Prompt 6 — CI/CD et déploiement front-end

```
Pour mon dossier professionnel CDA :

1. Comment le front-end est-il déployé ? (Vercel, Netlify, GitHub Pages, autre...)
2. Y a-t-il un pipeline CI/CD (GitHub Actions, autre) ? Si oui, fournis le fichier de workflow complet.
3. Comment les variables d'environnement sont gérées (SUPABASE_URL, API_URL...) ?
4. Y a-t-il un Dockerfile pour le front ?

Formate ta réponse en Markdown.
```

**Destination** : compléter `05_GESTION_DE_PROJET.md` et `08_REALISATIONS.md`.

---

## Prompt 7 — Composant le plus significatif (fonctionnalité contribution)

```
Pour mon dossier professionnel CDA, la fonctionnalité la plus représentative du projet est la gestion des contributions (un participant contribue une quantité de ressource à un événement).

J'ai besoin du code complet côté front-end pour cette fonctionnalité :

1. Le composant qui affiche les ressources d'un événement avec les contributions existantes
2. Le composant/modal qui permet d'ajouter ou modifier une contribution
3. Le service/fonction qui appelle l'endpoint POST /events/{eventId}/resources/{resourceId}/contributions
4. La gestion de l'erreur 409 (OPTIMISTIC_LOCK_FAILURE) côté client — comment le front réagit quand le verrou optimiste échoue
5. La gestion de l'actualisation des données après une contribution réussie

Fournis le code complet avec les chemins de fichiers.
```

**Destination** : `ANNEXES.md` Annexes A, B et E — la fonctionnalité représentative complète (front + back).

---

## Prompt 8 — Correction bug : affichage des événements pour les participants (pas seulement l'organisateur)

```
Un bug vient d'être corrigé côté back-end. Voici le contexte :

### Problème
Quand un utilisateur était ajouté comme participant à un événement (ex: pierregarric1@gmail.com), 
cet événement n'apparaissait PAS dans sa liste d'événements lorsqu'il se connectait avec son propre compte.
Seul le créateur (organisateur) de l'événement voyait ses événements.

### Correction back-end effectuée
L'endpoint GET /event/configuration/api/v1/events retourne désormais :
- Les événements dont l'utilisateur est le **créateur** (comme avant)
- **ET** les événements auxquels l'utilisateur est **participant** (table `participant`, colonne `user_email`)

La réponse API reste identique (même format EventDto) :
{
  "identifier": "uuid",
  "name": "string",
  "description": "string", 
  "eventDate": "instant",
  "creationDate": "instant",
  "updateDate": "instant",
  "creator": "email@example.com",
  "location": "string",
  "type": "PARTY | BIRTHDAY | DINER | SNACK",
  "members": ["uuid", ...]
}

### Ce qu'il faut vérifier / adapter côté front-end

1. **Liste des événements (dashboard)** : 
   - Vérifier que le composant qui affiche la liste des événements fonctionne correctement avec les événements où l'utilisateur n'est PAS le créateur
   - Si le front filtre les événements côté client (ex: `events.filter(e => e.creator === currentUser.email)`), **supprimer ce filtre** car le back-end retourne déjà les bons événements

2. **Distinction visuelle créateur vs participant** :
   - Ajouter un indicateur visuel pour différencier les événements créés par l'utilisateur et ceux auxquels il participe
   - Exemple : un badge "Organisateur" ou "Participant" sur la carte de l'événement
   - Comparer `event.creator` avec l'email de l'utilisateur connecté pour déterminer le rôle

3. **Droits d'édition/suppression** :
   - Vérifier que les boutons "Modifier" et "Supprimer" ne sont affichés QUE si l'utilisateur est le créateur de l'événement (`event.creator === currentUser.email`)
   - Un participant ne doit pas pouvoir modifier ni supprimer un événement dont il n'est pas le créateur
   - Le back-end bloque déjà la suppression côté serveur (vérification du creator), mais le front doit aussi masquer les boutons

4. **Page de détail d'un événement** :
   - Vérifier que la page de détail fonctionne correctement quand l'utilisateur est participant (pas créateur)
   - Les fonctionnalités de contribution doivent rester accessibles pour les participants

Pas de changement d'endpoint ni de contrat API. Seul le comportement de GET /events a changé (retourne plus de résultats).
```

**Destination** : mise à jour du code front-end (dashboard, détail événement, gestion des droits).

---

## Instructions d'intégration

Après avoir collecté les réponses de l'agent front, intégrer les éléments dans les fichiers suivants :

| Réponse | Fichier cible | Section |
|---------|--------------|---------|
| Prompt 1 | `07_SPECIFICATIONS_TECHNIQUES.md` | Nouvelle section "Front-end" |
| Prompt 2 | `06_SPECIFICATIONS_FONCTIONNELLES.md` | Remplacer les placeholders de la section 6.3 |
| Prompt 3 | `08_REALISATIONS.md` + `ANNEXES.md` | Section 8.1 + Annexes A et B |
| Prompt 4 | `08_REALISATIONS.md` + `09_SECURITE.md` | Section 8.1 + section auth côté client |
| Prompt 5 | `10_PLAN_DE_TESTS.md` | Nouvelle sous-section "Tests front-end" |
| Prompt 6 | `05_GESTION_DE_PROJET.md` | Compléter CI/CD et déploiement |
| Prompt 7 | `ANNEXES.md` | Annexes A, B, E — code front fonctionnalité représentative |
| Prompt 8 | Code front-end | Dashboard, détail événement, gestion droits créateur/participant |
