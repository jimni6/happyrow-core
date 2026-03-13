# 12. Synthèse et conclusion

## 12.1 Bilan du projet

Le projet HappyRow a abouti à une application full-stack fonctionnelle et déployée, composée de :

**Back-end (happyrow-core)** :
- **15 endpoints REST** couvrant 4 domaines métier (événements, participants, ressources, contributions)
- **12 use cases** dans une architecture hexagonale stricte
- **4 tables PostgreSQL** avec contraintes d'intégrité, index et verrou optimiste
- **Pipeline CI/CD** GitHub Actions (Detekt → Tests → Déploiement Render)
- **~5 000 lignes de code** Kotlin, organisées en 2 modules Gradle

**Front-end (happyrow-front)** :
- **SPA React 19 / TypeScript** avec architecture Clean Architecture (features modules)
- **8 écrans/vues** et **6 modales** couvrant l'ensemble des fonctionnalités
- **PWA** installable avec mode offline (Service Worker Workbox)
- **Pipeline CI/CD** GitHub Actions (Lint → Tests → Docker → Approval → Déploiement Vercel)
- **46 tests unitaires** (Vitest) + **6 tests E2E** (Playwright)
- **Seulement 3 dépendances de production** (React, React Router, Supabase)

L'application est déployée en production : back-end sur Render, front-end sur Vercel.

## 12.2 Satisfactions

### Architecture hexagonale

Le choix de l'architecture hexagonale s'est révélé pertinent pour un projet de cette taille. La séparation stricte entre le domaine et l'infrastructure offre :

- **Testabilité** : les use cases sont testables unitairement avec des mocks, sans base de données
- **Évolutivité** : un changement d'ORM ou de framework web n'impacte pas la logique métier
- **Lisibilité** : la structure en packages reflète les concepts métier (bounded contexts)

### Gestion fonctionnelle des erreurs (Arrow Either)

L'adoption d'Arrow `Either` en remplacement des exceptions a apporté :

- **Explicité** : le type de retour `Either<Error, Success>` force à gérer chaque cas d'erreur
- **Composabilité** : `flatMap` et `mapLeft` permettent de chaîner les opérations proprement
- **Traçabilité** : chaque couche enveloppe les erreurs de la couche inférieure, préservant le contexte

### Verrou optimiste

L'implémentation du verrou optimiste pour les contributions est la fonctionnalité technique la plus aboutie du projet. Elle démontre la gestion d'un problème de concurrence réel dans une application multi-utilisateurs.

### Clean Architecture front-end

L'application du même principe d'architecture en couches côté front-end (composants → providers → use cases → repositories) a permis une cohérence architecturale entre back-end et front-end, et une testabilité accrue grâce à l'inversion de dépendance (mocking des repositories dans les tests).

### CI/CD

Les pipelines GitHub Actions (back-end et front-end) apportent un filet de sécurité à chaque push : analyse statique, tests automatisés, audit de sécurité, déploiement automatique. Le pipeline front-end inclut en plus un workflow de sécurité quotidien (audit npm, vérification lockfile, détection supply chain).

## 12.3 Difficultés rencontrées

### Concurrence et verrou optimiste

La gestion des accès concurrents a été le défi technique principal. Le verrou optimiste nécessite de propager la `version` de la ressource depuis la lecture initiale jusqu'à l'écriture finale, en passant par plusieurs couches. La remontée de l'`OptimisticLockException` à travers les couches `Repository → UseCase → Endpoint` jusqu'au client HTTP 409 a nécessité une gestion d'erreurs précise.

### Suppression en cascade

La suppression d'un événement implique la suppression ordonnée de toutes les contributions, puis des ressources, puis des participants, et enfin de l'événement. L'ordre est imposé par les clés étrangères. Cette cascade a été implémentée manuellement dans le `SqlEventRepository` pour garder le contrôle sur la logique et les logs.

### Configuration SSL (Raspberry Pi)

Le déploiement alternatif sur Raspberry Pi a nécessité la mise en place d'un tunnel Cloudflare pour le HTTPS et la gestion des certificats SSL pour la connexion PostgreSQL. Cette difficulté a conduit à centraliser toute la configuration sensible dans des variables d'environnement.

## 12.4 Perspectives

| Évolution | Description |
|-----------|-------------|
| **Application mobile** | Développer un client mobile natif ou cross-platform (CP13 du REAC — non couverte dans ce projet) |
| **Notifications** | Ajouter des notifications temps réel (WebSocket ou SSE) quand un participant contribue |
| **NoSQL** | Explorer le stockage de données non structurées (logs d'activité, historique des contributions) dans une base NoSQL |
| **Tests d'intégration en CI** | Intégrer les tests Testcontainers dans le pipeline GitHub Actions |
| **Monitoring** | Ajouter des métriques applicatives (Micrometer) et du monitoring (Grafana) |

## 12.5 Conclusion

Le projet HappyRow démontre la capacité à concevoir, développer et déployer une application web moderne et sécurisée dans un contexte professionnel. L'ensemble des compétences du référentiel CDA sont couvertes par ce projet, depuis l'analyse des besoins jusqu'au déploiement en production, en passant par la conception, le développement, les tests et la sécurité.

Les choix techniques (Kotlin, architecture hexagonale, Arrow Either, verrou optimiste) témoignent d'une approche professionnelle qui va au-delà du fonctionnel pour adresser les problématiques de maintenabilité, de fiabilité et de sécurité.
