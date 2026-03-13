# 5. Gestion de projet

## 5.1 Méthodologie

Le projet HappyRow a été conduit en suivant une approche **Kanban**, adaptée au contexte d'un développeur en autonomie sur un projet de taille moyenne. Cette méthode permet de :

- Visualiser le flux de travail (To Do → In Progress → Done)
- Limiter le travail en cours (WIP) pour rester focalisé
- Livrer de façon continue et incrémentale

Les tâches sont organisées sous forme de **tickets** dans un tableau Kanban sur GitHub (Issues + Projects), chaque ticket correspondant à une fonctionnalité, un correctif ou une tâche technique.

## 5.2 Planification et découpage

Le projet a été découpé en itérations fonctionnelles :

| Phase | Contenu | Livrables |
|-------|---------|-----------|
| **Phase 1 — Fondations** | Architecture hexagonale, configuration Gradle multi-modules, setup Docker, connexion PostgreSQL, pipeline CI/CD | Projet compilable, base de données opérationnelle, pipeline fonctionnel |
| **Phase 2 — Événements** | CRUD complet des événements, authentification JWT Supabase | Endpoints events, auth fonctionnelle |
| **Phase 3 — Participants** | Gestion des participants, auto-ajout du créateur | Endpoints participants |
| **Phase 4 — Ressources & Contributions** | Gestion des ressources, système de contributions avec verrou optimiste | Endpoints ressources et contributions |
| **Phase 5 — Qualité & Déploiement** | Tests unitaires, intégration, analyse statique, déploiement Render | Application déployée et testée |

## 5.3 Suivi du projet

### Outils de suivi

| Outil | Usage |
|-------|-------|
| **GitHub Issues** | Suivi des tâches, bugs, améliorations |
| **GitHub Projects** | Tableau Kanban pour visualiser l'avancement |
| **Git** | Gestion des versions, branches par fonctionnalité |
| **GitHub Actions** | Feedback automatisé sur chaque push (qualité, tests) |

### Gestion des versions

Le projet suit un workflow Git structuré :

- Branche `main` : code stable, déployé automatiquement
- Branches de fonctionnalité : développement isolé, merge via pull request
- Chaque push sur `main` déclenche les pipelines CI/CD (back-end et front-end)

### Pipeline CI/CD front-end

Le front-end dispose de son propre pipeline GitHub Actions en 4 jobs :

1. **Test** : lockfile-lint, `npm ci --ignore-scripts`, ESLint, Vitest, build
2. **Build & Push Docker** : image multi-stage vers `ghcr.io`
3. **Approval** : approbation manuelle (GitHub Environment "production")
4. **Deploy** : déploiement sur Vercel via `vercel --prod`

Un workflow **Security Audit** s'exécute quotidiennement (cron) : audit npm, vérification du lockfile, détection de scripts d'installation suspects, contrôle de fraîcheur des dépendances.

## 5.4 Objectifs de qualité

| Objectif | Outil / Mesure |
|----------|----------------|
| Analyse statique du code | **Detekt** — détection de code smells, complexité, conventions Kotlin |
| Formatage homogène | **Spotless** avec ktlint — style de code uniforme |
| Tests automatisés | **Kotest** + **MockK** (unitaires), **Testcontainers** (intégration) |
| Zéro régression au déploiement | Pipelines CI/CD bloquants : back (Detekt → Tests → Deploy) et front (Lint → Tests → Build → Deploy) |
| Documentation du code | Auto-documentation par nommage explicite (classes, fonctions, variables) |

## 5.5 Environnement technique

### Back-end (happyrow-core)

| Composant | Technologie | Version |
|-----------|------------|---------|
| Langage | Kotlin | 2.2.0 |
| Runtime | JVM (Eclipse Temurin) | 21 |
| Framework web | Ktor | 3.2.2 |
| ORM | JetBrains Exposed | 0.61.0 |
| Base de données | PostgreSQL | 42.7.7 (driver) |
| Pool de connexions | HikariCP | 6.3.1 |
| Injection de dépendances | Koin | 4.1.0 |
| Gestion fonctionnelle des erreurs | Arrow | 2.1.2 |
| Sérialisation | Jackson | — |
| Authentification | Auth0 JWT + Supabase | 4.4.0 |
| Tests back-end | Kotest, MockK, Testcontainers | 5.9.1 / 1.14.5 / 1.21.3 |
| Analyse statique | Detekt | 1.23.7 |
| Build | Gradle | 8.14.3 |

### Front-end (happyrow-front)

| Composant | Technologie | Version |
|-----------|------------|---------|
| Framework UI | React | 19.1.1 |
| Langage | TypeScript (strict) | 5.8.3 |
| Build | Vite | 7.1.2 |
| Routing | React Router DOM | 7.13.0 |
| Authentification | Supabase JS SDK | 2.39.3 |
| PWA | vite-plugin-pwa + Workbox | 1.2.0 / 7.4.0 |
| Tests unitaires | Vitest + Testing Library | 3.2.4 |
| Tests E2E | Playwright | 1.59.0 |
| Linting | ESLint | 9.33 |
| Formatage | Prettier | 3.6.2 |
| Git hooks | Husky + lint-staged | 9.1.7 / 16.1.6 |

### Infrastructure commune

| Composant | Technologie |
|-----------|------------|
| Conteneurisation | Docker (multi-stage) — back-end et front-end |
| CI/CD | GitHub Actions — 2 pipelines (back + front) |
| Hébergement back-end | Render (Francfort) |
| Hébergement front-end | Vercel |
| IDE | IntelliJ IDEA / VS Code |

## 5.6 Communication

- **Documentation technique** rédigée en anglais dans le code source (nommage, commentaires Exposed, Ktor)
- **Lecture de documentation** technique en anglais : Ktor, Exposed, Arrow, Supabase, OWASP
- **Commits et pull requests** en anglais
- **Documentation projet** (docs/) en anglais et français
