# 2. Liste des compétences mises en œuvre

Dans ce chapitre, je présente les 11 compétences professionnelles du référentiel CDA (arrêté du 26/04/2023) et la manière dont je les ai mobilisées concrètement à travers le projet HappyRow.

## CCP 1 — Développer une application sécurisée

| Compétence | Ce que j'ai réalisé dans HappyRow |
|---|---|
| **Installer et configurer son environnement de travail en fonction du projet** | J'ai mis en place mon environnement avec Kotlin 2.2 / JDK 21, un build Gradle multi-modules, Docker pour la conteneurisation, IntelliJ IDEA comme IDE, et PostgreSQL en local via `docker-compose.yml`. J'ai aussi configuré Ktor via `application.conf` et les variables d'environnement. |
| **Développer des interfaces utilisateur** | Côté back-end, j'ai développé les endpoints REST (driving adapters Ktor) avec les DTOs de requête/réponse et la sérialisation Jackson. Côté front-end, j'ai construit une SPA React 19 / TypeScript avec 8 vues et 6 modales, des Design Tokens CSS, des composants fonctionnels comme `ResourceItem`, `EventDetailsView` ou `LoginModal`, et une gestion d'état via Context/Provider. |
| **Développer des composants métier** | J'ai implémenté 12 Use Cases dans le module `domain` : la création d'événement avec auto-ajout du créateur en tant que participant, la gestion des contributions avec verrou optimiste, la suppression en cascade… J'ai utilisé Arrow `Either` pour gérer les erreurs de manière fonctionnelle. |
| **Contribuer à la gestion d'un projet informatique** | J'ai géré le projet avec Git et des branches, mis en place des pipelines CI/CD GitHub Actions pour le back-end (Detekt → Tests → Deploy Render) et le front-end (Lint → Tests → Docker → Approval → Deploy Vercel). J'ai suivi une méthode Kanban et intégré l'analyse statique Detekt ainsi que le formatage Spotless/Prettier. |

## CCP 2 — Concevoir et développer une application sécurisée organisée en couches

| Compétence | Ce que j'ai réalisé dans HappyRow |
|---|---|
| **Analyser les besoins et maquetter une application** | J'ai formalisé l'expression des besoins à partir du contexte métier (gestion collaborative d'événements). J'ai réalisé les maquettes sur Figma (`design-figma/`), le diagramme d'enchaînement des écrans, et défini les Design Tokens CSS pour la charte graphique (teal/navy/coral). |
| **Définir l'architecture logicielle d'une application** | J'ai conçu une architecture hexagonale (ports & adapters) organisée en 2 modules Gradle : `domain` pour la logique métier pure (sans aucune dépendance framework) et `infrastructure` pour les adapters REST, SQL et JWT. L'injection de dépendances est gérée par Koin, et j'ai découpé le domaine en 4 bounded contexts : Event, Resource, Participant, Contribution. |
| **Concevoir et mettre en place une base de données relationnelle** | J'ai conçu un Modèle Conceptuel de Données (MCD) à 4 entités, puis j'ai implémenté le modèle physique sous PostgreSQL avec des contraintes d'intégrité, des index, des clés étrangères CASCADE, des contraintes CHECK et des index uniques composites. J'ai aussi écrit le script `init-db.sql` et défini les tables via Exposed. |
| **Développer des composants d'accès aux données SQL et NoSQL** | J'ai développé 4 repositories SQL qui implémentent les ports définis dans le domaine, en utilisant Exposed ORM. Toutes les requêtes sont paramétrées (protection contre l'injection SQL), les opérations sont transactionnelles, et j'ai mis en place un verrou optimiste avec versioning sur la table Resource. |

## CCP 3 — Préparer le déploiement d'une application sécurisée

| Compétence | Ce que j'ai réalisé dans HappyRow |
|---|---|
| **Préparer et exécuter les plans de tests d'une application** | Pour le back-end, j'ai écrit des tests unitaires avec Kotest + MockK et des assertions Arrow, des personas réutilisables, et des tests d'intégration avec Testcontainers. J'ai aussi intégré Detekt pour l'analyse statique. Côté front-end, j'ai mis en place 46 tests unitaires (Vitest + Testing Library) et 6 tests E2E Playwright pour la PWA. J'ai réalisé un jeu d'essai complet sur la fonctionnalité de contribution. |
| **Préparer et documenter le déploiement d'une application** | J'ai créé un Dockerfile multi-stage pour le back-end (Gradle + JRE 21) et configuré `render.yaml` pour le déploiement sur Render. Pour le front-end, j'ai fait de même avec un Dockerfile multi-stage (Node + Nginx) et un déploiement sur Vercel. J'ai aussi mis en place un `docker-compose.yml` pour le développement local. |
| **Contribuer à la mise en production dans une démarche DevOps** | J'ai configuré les pipelines CI/CD GitHub Actions pour les deux services : back-end (Detekt → Tests → Deploy Render) et front-end (Lint → Tests → Docker → Approval manuelle → Deploy Vercel). J'ai aussi mis en place un audit de sécurité quotidien (npm audit, lockfile-lint) et conteneurisé les deux services avec Docker. |
