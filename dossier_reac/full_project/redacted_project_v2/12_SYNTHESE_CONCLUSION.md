# 12. Synthèse et conclusion

## 12.1 Bilan du projet

Le projet HappyRow a abouti à une application full-stack fonctionnelle et déployée en production. Voici ce que j'ai livré concrètement :

**Back-end (happyrow-core)** :
- **15 endpoints REST** couvrant 4 domaines métier (événements, participants, ressources, contributions)
- **12 use cases** dans une architecture hexagonale stricte
- **4 tables PostgreSQL** avec contraintes d'intégrité, index et verrou optimiste
- **Pipeline CI/CD** GitHub Actions (Detekt, Tests, Déploiement Render)
- **~5 000 lignes de code** Kotlin, organisées en 2 modules Gradle

**Front-end (happyrow-front)** :
- **SPA React 19 / TypeScript** avec architecture Clean Architecture (features modules)
- **8 écrans/vues** et **6 modales** couvrant l'ensemble des fonctionnalités
- **PWA** installable avec mode offline (Service Worker Workbox)
- **Pipeline CI/CD** GitHub Actions (Lint, Tests, Docker, Approval, Déploiement Vercel)
- **46 tests unitaires** (Vitest) + **6 tests E2E** (Playwright)
- **Seulement 3 dépendances de production** (React, React Router, Supabase)

L'application est déployée en production : back-end sur Render, front-end sur Vercel.

## 12.2 Satisfactions

### Architecture hexagonale

Le choix de l'architecture hexagonale s'est révélé vraiment pertinent pour ce projet. La séparation stricte entre le domaine et l'infrastructure m'a apporté :

- **Testabilité** : j'ai pu tester mes use cases unitairement avec des mocks, sans base de données
- **Évolutivité** : si demain je décidais de changer d'ORM ou de framework web, la logique métier ne serait pas impactée
- **Lisibilité** : la structure en packages reflète les concepts métier (bounded contexts), ce qui rend le code facile à naviguer

### Gestion fonctionnelle des erreurs (Arrow Either)

L'adoption d'Arrow `Either` en remplacement des exceptions classiques a été une vraie découverte pour moi. Ça m'a apporté :

- **Explicité** : le type de retour `Either<Error, Success>` me force à gérer chaque cas d'erreur — impossible d'oublier un cas
- **Composabilité** : `flatMap` et `mapLeft` me permettent de chaîner les opérations proprement sans blocs try/catch imbriqués
- **Traçabilité** : chaque couche enveloppe les erreurs de la couche inférieure, ce qui préserve le contexte complet

### Verrou optimiste

L'implémentation du verrou optimiste pour les contributions est la fonctionnalité technique dont je suis le plus fier. Elle démontre la gestion d'un problème de concurrence réel dans une application multi-utilisateurs, avec une remontée d'erreur propre jusqu'au client HTTP 409.

### Clean Architecture front-end

J'ai appliqué le même principe d'architecture en couches côté front-end (composants, providers, use cases, repositories). Ça m'a permis d'avoir une cohérence architecturale entre back-end et front-end, et une testabilité accrue grâce à l'inversion de dépendance (mocking des repositories dans les tests).

### CI/CD

Les pipelines GitHub Actions que j'ai mis en place (back-end et front-end) apportent un vrai filet de sécurité à chaque push : analyse statique, tests automatisés, audit de sécurité, déploiement automatique. Le pipeline front-end inclut en plus un workflow de sécurité quotidien (audit npm, vérification lockfile, détection supply chain).

## 12.3 Difficultés rencontrées

### Concurrence et verrou optimiste

La gestion des accès concurrents a été mon principal défi technique. Le verrou optimiste nécessite de propager la `version` de la ressource depuis la lecture initiale jusqu'à l'écriture finale, en traversant plusieurs couches. Faire remonter l'`OptimisticLockException` à travers les couches Repository, UseCase, Endpoint jusqu'au client HTTP 409 m'a demandé une gestion d'erreurs très précise.

### Suppression en cascade

La suppression d'un événement implique la suppression ordonnée de toutes les contributions, puis des ressources, puis des participants, et enfin de l'événement. L'ordre est imposé par les clés étrangères. J'ai implémenté cette cascade manuellement dans le `SqlEventRepository` pour garder le contrôle sur la logique et les logs, plutôt que de m'appuyer sur un `ON DELETE CASCADE` automatique en base.

## 12.4 Perspectives

| Évolution | Description |
|-----------|-------------|
| **Application mobile** | Développer un client mobile natif ou cross-platform (CP13 du REAC, non couverte dans ce projet) |
| **Notifications** | Ajouter des notifications temps réel (WebSocket ou SSE) quand un participant contribue |
| **NoSQL** | Explorer le stockage de données non structurées (logs d'activité, historique des contributions) dans une base NoSQL |
| **Tests d'intégration en CI** | Intégrer les tests Testcontainers dans le pipeline GitHub Actions |
| **Monitoring** | Ajouter des métriques applicatives (Micrometer) et du monitoring (Grafana) |

## 12.5 Conclusion

Ce projet m'a permis de démontrer ma capacité à concevoir, développer et déployer une application web moderne et sécurisée. J'ai couvert l'ensemble des compétences du référentiel CDA, depuis l'analyse des besoins jusqu'au déploiement en production, en passant par la conception, le développement, les tests et la sécurité.

Les choix techniques que j'ai faits (Kotlin, architecture hexagonale, Arrow Either, verrou optimiste) témoignent d'une approche qui va au-delà du simple fonctionnel pour adresser les problématiques de maintenabilité, de fiabilité et de sécurité. C'est un projet dont je suis satisfait et qui reflète bien les compétences que j'ai acquises pendant cette formation.
