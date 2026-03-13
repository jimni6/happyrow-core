# 2. Liste des compétences mises en œuvre

Le tableau ci-dessous recense les 11 compétences professionnelles du référentiel CDA (arrêté du 26/04/2023) et indique, pour chacune, comment le projet HappyRow permet de les démontrer.

## CCP 1 — Développer une application sécurisée

| Compétence | Mise en œuvre dans HappyRow |
|---|---|
| **Installer et configurer son environnement de travail en fonction du projet** | Mise en place de l'environnement Kotlin 2.2 / JDK 21, Gradle multi-modules, Docker, IntelliJ IDEA, PostgreSQL local via `docker-compose.yml`, configuration Ktor via `application.conf` et variables d'environnement. |
| **Développer des interfaces utilisateur** | Back-end : endpoints REST (driving adapters Ktor), DTOs de requête/réponse, sérialisation Jackson. Front-end : SPA React 19 / TypeScript avec 8 vues et 6 modales, Design Tokens CSS, composants fonctionnels (`ResourceItem`, `EventDetailsView`, `LoginModal`), gestion d'état via Context/Provider. |
| **Développer des composants métier** | 12 Use Cases dans le module `domain` : création d'événement avec auto-ajout du créateur en tant que participant, gestion des contributions avec verrou optimiste, suppression en cascade. Gestion fonctionnelle des erreurs avec Arrow `Either`. |
| **Contribuer à la gestion d'un projet informatique** | Gestion Git avec branches, CI/CD GitHub Actions back-end (Detekt → Tests → Deploy Render) et front-end (Lint → Tests → Docker → Approval → Deploy Vercel), méthode Kanban, analyse statique Detekt + formatage Spotless/Prettier. |

## CCP 2 — Concevoir et développer une application sécurisée organisée en couches

| Compétence | Mise en œuvre dans HappyRow |
|---|---|
| **Analyser les besoins et maquetter une application** | Expression des besoins formalisée à partir du contexte métier (gestion collaborative d'événements). Maquettes Figma (`design-figma/`), diagramme d'enchaînement des écrans, Design Tokens CSS (charte graphique teal/navy/coral). |
| **Définir l'architecture logicielle d'une application** | Architecture hexagonale (ports & adapters) en 2 modules Gradle : `domain` (logique métier pure, aucune dépendance framework) et `infrastructure` (adapters REST, SQL, JWT). Injection de dépendances via Koin. 4 bounded contexts : Event, Resource, Participant, Contribution. |
| **Concevoir et mettre en place une base de données relationnelle** | Modèle Conceptuel de Données (MCD) à 4 entités. Modèle physique PostgreSQL avec contraintes d'intégrité, index, clés étrangères CASCADE, contraintes CHECK, index uniques composites. Script `init-db.sql` et tables Exposed. |
| **Développer des composants d'accès aux données SQL et NoSQL** | 4 repositories SQL implémentant les ports du domaine via Exposed ORM. Requêtes paramétrées (protection injection SQL), transactions, verrou optimiste avec versioning sur la table Resource. |

## CCP 3 — Préparer le déploiement d'une application sécurisée

| Compétence | Mise en œuvre dans HappyRow |
|---|---|
| **Préparer et exécuter les plans de tests d'une application** | Back-end : tests unitaires Kotest + MockK + assertions Arrow, personas réutilisables, tests d'intégration Testcontainers, analyse statique Detekt. Front-end : 46 tests unitaires Vitest + Testing Library, 6 tests E2E Playwright (PWA). Jeu d'essai sur la fonctionnalité de contribution. |
| **Préparer et documenter le déploiement d'une application** | Back-end : Dockerfile multi-stage (Gradle + JRE 21), `render.yaml` Render. Front-end : Dockerfile multi-stage (Node + Nginx), déploiement Vercel. `docker-compose.yml` pour le développement local des deux services. |
| **Contribuer à la mise en production dans une démarche DevOps** | Pipelines CI/CD GitHub Actions : back-end (Detekt → Tests → Deploy Render) et front-end (Lint → Tests → Docker → Approval manuelle → Deploy Vercel). Audit de sécurité quotidien (npm audit, lockfile-lint). Conteneurisation Docker des deux services. |
