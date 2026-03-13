<!-- Slides 4-6 — Timing: 3 min -->

# Contexte de formation

## Slide 4 — L'organisme et la promotion (45s)

| | |
|---|---|
| **Organisme** | *[Nom du centre de formation]* |
| **Formation** | Titre Professionnel CDA — Niveau 6 |
| **Promotion** | *[Promotion CDA 2025-2026]* |
| **Effectif** | *[X apprenants]* |
| **Encadrement** | *[X formateurs techniques]* |

---

## Slide 5 — Mon rôle et le projet (45s)

**Projet personnel** réalisé en autonomie pendant la formation

- **Conception** : architecture, modèle de données, choix techniques
- **Développement** : back-end (Kotlin/Ktor) + front-end (React/TypeScript)
- **Tests** : unitaires, intégration, E2E, analyse statique
- **Déploiement** : Docker, CI/CD GitHub Actions, Render + Vercel
- **Encadrement** : formateurs en rôle de conseil et validation

> Objectif : couvrir les 11 compétences professionnelles du référentiel CDA

---

## Slide 6 — Environnement de travail (1 min)

### Outils de développement

| Outil | Usage |
|-------|-------|
| **IntelliJ IDEA** | IDE principal (Kotlin, Gradle, Docker) |
| **VS Code** | Développement front-end (React, TypeScript) |
| **JDK 21** (Eclipse Temurin) | Runtime JVM pour le back-end |
| **Node.js 22** | Runtime front-end, npm, Vite |
| **Docker Desktop** | Conteneurisation, PostgreSQL local |
| **Git + GitHub** | Gestion de versions, collaboration, CI/CD |

### Configuration locale

```
docker-compose.yml → PostgreSQL local (port 5432)
application.conf   → Configuration Ktor (ports, DB, JWT)
.env               → Variables d'environnement (secrets locaux)
gradle/libs.versions.toml → Catalogue centralisé des dépendances
```

### Structure Gradle multi-modules

```
happyrow-core/
├── domain/          → Logique métier pure (0 dépendance technique)
├── infrastructure/  → Ktor, Exposed, JWT, SQL adapters
├── build.gradle.kts → Configuration projet
└── docker-compose.yml
```
