<!-- Slides 1-3 — Timing: 2 min -->

# Introduction

## Slide 1 — Page titre (30s)

# HappyRow

## Plateforme collaborative de gestion d'événements

---

**Titre Professionnel visé** : Concepteur Développeur d'Applications (CDA) — Niveau 6

**Candidat** : *[Nom Prénom]*

**Date de session** : *[À compléter]*

**Centre de formation** : *[À compléter]*

---

| Dépôt | Stack |
|-------|-------|
| `happyrow-core` | Back-end — Kotlin / Ktor / PostgreSQL |
| `happyrow-front` | Front-end — React 19 / TypeScript |

---

## Slide 2 — Sommaire (30s)

# Plan de la présentation

1. **Contexte** — Formation, rôle, environnement de travail
2. **Cahier des charges** — Besoins, contraintes, livrables
3. **Gestion de projet** — Kanban, suivi, qualité
4. **Spécifications fonctionnelles** — Architecture, maquettes, MCD, séquences
5. **Réalisations — UI** — Captures d'écran, code React
6. **Réalisations — Métier** — Use cases, modèles domaine, Arrow Either
7. **Réalisations — Données** — Verrou optimiste, repositories SQL
8. **Réalisations — Autres composants** — JWT, endpoints REST, erreurs
9. **Sécurité** — Auth, applicative, infrastructure, éco-conception
10. **Tests et jeu d'essai** — Stratégie, Kotest, scénarios
11. **Déploiement et DevOps** — Docker, CI/CD, mise en production
12. **Veille sécurité** — OWASP Top 10, CVE
13. **Synthèse et conclusion** — Bilan, perspectives

---

## Slide 3 — Compétences CDA mises en œuvre (1 min)

# Compétences du référentiel CDA

### CCP 1 — Développer une application sécurisée

| Compétence | Mise en œuvre |
|---|---|
| **C1 — Installer et configurer l'environnement** | Kotlin 2.2 / JDK 21, Gradle multi-modules, Docker, IntelliJ IDEA, docker-compose |
| **C2 — Développer des interfaces utilisateur** | Endpoints REST Ktor + SPA React 19 / TypeScript (8 vues, 6 modales) |
| **C3 — Développer des composants métier** | 12 use cases (module `domain`), Arrow Either, verrou optimiste |
| **C4 — Contribuer à la gestion de projet** | Kanban GitHub, CI/CD GitHub Actions, Detekt + Spotless |

### CCP 2 — Concevoir et développer une application sécurisée organisée en couches

| Compétence | Mise en œuvre |
|---|---|
| **C5 — Analyser les besoins et maquetter** | Expression des besoins, maquettes Figma, Design Tokens CSS |
| **C6 — Définir l'architecture logicielle** | Architecture hexagonale (Ports & Adapters), 2 modules Gradle, Koin DI |
| **C7 — Concevoir et mettre en place une BDD** | MCD 4 entités, PostgreSQL, FK, CHECK, UNIQUE, verrou optimiste |
| **C8 — Développer des composants d'accès aux données** | 4 repositories SQL (Exposed ORM), requêtes paramétrées, transactions |

### CCP 3 — Préparer le déploiement d'une application sécurisée

| Compétence | Mise en œuvre |
|---|---|
| **C9 — Préparer et exécuter les plans de tests** | Kotest + MockK, Testcontainers, Vitest, Playwright, Detekt |
| **C10 — Préparer et documenter le déploiement** | Dockerfile multi-stage, render.yaml, docker-compose, procédure documentée |
| **C11 — Contribuer à la mise en production DevOps** | 2 pipelines CI/CD GitHub Actions, audit sécurité quotidien, Dependabot |
