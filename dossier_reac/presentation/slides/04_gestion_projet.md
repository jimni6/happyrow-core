<!-- Slides 9-11 — Timing: 3 min -->

# Gestion de projet

## Slide 9 — Méthodologie et planning (1 min)

### Kanban — GitHub Issues + Projects

- **To Do** → **In Progress** → **Done**
- WIP limité pour rester focalisé
- Livraison continue et incrémentale

### Planning en 5 phases

| Phase | Contenu | Livrables |
|-------|---------|-----------|
| **1. Fondations** | Architecture hexagonale, Gradle, Docker, PostgreSQL, CI/CD | Projet compilable, pipeline fonctionnel |
| **2. Événements** | CRUD événements, authentification JWT Supabase | Endpoints events, auth fonctionnelle |
| **3. Participants** | Gestion participants, auto-ajout du créateur | Endpoints participants |
| **4. Ressources & Contributions** | Contributions avec verrou optimiste | Endpoints ressources + contributions |
| **5. Qualité & Déploiement** | Tests, analyse statique, déploiement Render/Vercel | Application déployée et testée |

---

## Slide 10 — Suivi des tâches (1 min)

### Outils de suivi

| Outil | Usage |
|-------|-------|
| **GitHub Issues** | Tickets par fonctionnalité, bug, amélioration |
| **GitHub Projects** | Tableau Kanban — visualisation de l'avancement |
| **Git** | Branches par fonctionnalité, merge via PR |
| **GitHub Actions** | Feedback automatisé à chaque push |

> *[Insérer capture d'écran du tableau GitHub Projects]*

### Gestion des versions Git

- Branche `main` : code stable, déployé automatiquement
- Branches de fonctionnalité : développement isolé
- Chaque push sur `main` déclenche les pipelines CI/CD

---

## Slide 11 — Objectifs de qualité (1 min)

| Objectif | Outil | Résultat |
|----------|-------|----------|
| Analyse statique | **Detekt** | Code smells, complexité, conventions |
| Formatage homogène | **Spotless** + ktlint (back) / Prettier (front) | Style uniforme |
| Tests automatisés | **Kotest** + MockK / **Vitest** + Playwright | Couverture fonctionnelle |
| Sécurité supply chain | npm audit quotidien, lockfile-lint, Dependabot | Alertes automatiques |
| Zéro régression | Pipelines CI/CD bloquants | Aucun déploiement si échec |

### Pipeline résumé

```
Back-end :  Detekt → Tests unitaires → Déploiement Render
Front-end : ESLint → Vitest → Build Docker → Approval → Déploiement Vercel
```
