# 4. GESTION DE PROJET ET MÉTHODOLOGIE

## 4.1 Méthodologie Agile appliquée

### 4.1.1 Choix de la méthodologie

**Méthode retenue** : Scrum adapté pour développeur solo

**Justification du choix** :
- ✅ Itérations courtes (sprints de 2 semaines)
- ✅ Livraisons régulières et incrémentales
- ✅ Adaptation facile aux imprévus
- ✅ Focus sur la valeur métier (MVP)
- ✅ Amélioration continue (rétrospectives)

**Adaptations pour un développeur solo** :
- Pas de Daily Standup (suivi personnel quotidien)
- Sprint Planning simplifié (définition des objectifs)
- Sprint Review auto-évaluation (démo fonctionnelle)
- Rétrospective individuelle (documentation des apprentissages)

---

### 4.1.2 Organisation des sprints

**Durée** : 2 semaines par sprint

**Rituels maintenus** :

| Rituel | Fréquence | Durée | Objectif |
|--------|-----------|-------|----------|
| **Sprint Planning** | Début de sprint | 2h | Définir les objectifs et tâches |
| **Daily Review** | Quotidien | 15min | Faire le point sur l'avancement |
| **Sprint Review** | Fin de sprint | 1h | Démo et validation fonctionnelle |
| **Sprint Retrospective** | Fin de sprint | 1h | Identifier améliorations |

**Artefacts utilisés** :
- **Product Backlog** : Trello (liste des fonctionnalités)
- **Sprint Backlog** : Trello (tâches du sprint en cours)
- **Increment** : Code déployé sur Render
- **Definition of Done** : Checklist de validation

---

### 4.1.3 Product Backlog initial

**Epic 1 : Gestion des événements**
- US-01 : En tant qu'organisateur, je veux créer un événement
- US-02 : En tant qu'organisateur, je veux consulter mes événements
- US-03 : En tant qu'organisateur, je veux modifier un événement
- US-04 : En tant qu'organisateur, je veux supprimer un événement

**Epic 2 : Gestion des participants**
- US-05 : En tant qu'organisateur, je veux ajouter des participants
- US-06 : En tant qu'organisateur, je veux voir les participants
- US-07 : En tant que participant, je veux confirmer ma présence
- US-08 : En tant que participant, je veux décliner l'invitation

**Epic 3 : Gestion des ressources**
- US-09 : En tant qu'organisateur, je veux définir les ressources nécessaires
- US-10 : En tant qu'organisateur, je veux consulter les ressources

**Epic 4 : Gestion des contributions**
- US-11 : En tant que participant, je veux indiquer ce que j'apporte
- US-12 : En tant qu'organisateur, je veux voir qui apporte quoi

**Epic 5 : Infrastructure et qualité**
- US-13 : En tant que développeur, je veux un pipeline CI/CD
- US-14 : En tant que développeur, je veux des tests automatisés
- US-15 : En tant que développeur, je veux une application sécurisée

---

## 4.2 Planification détaillée

### 4.2.1 Sprint 0 : Setup et conception (2 semaines)

**Objectifs** :
- Définir l'architecture du projet
- Créer le projet Gradle multi-modules
- Configurer les outils de développement
- Modéliser la base de données

**Tâches** :
- [x] Analyse des besoins et rédaction du cahier des charges
- [x] Choix technologiques (Kotlin, Ktor, Exposed)
- [x] Création du repository GitHub
- [x] Setup projet Gradle avec multi-modules
- [x] Configuration Detekt + Spotless
- [x] Modélisation UML (classes, séquence)
- [x] Schéma de base de données PostgreSQL

**Livrables** :
- ✅ Architecture définie (hexagonale)
- ✅ Projet Gradle fonctionnel
- ✅ Diagrammes UML
- ✅ Schéma de base de données

**Rétrospective** :
- ✅ Points positifs : Architecture claire, outils configurés
- ⚠️ Difficultés : Apprentissage Ktor et Exposed
- 🔄 Améliorations : Documentation plus détaillée

---

### 4.2.2 Sprint 1 : Domain Layer - Événements (2 semaines)

**Objectifs** :
- Implémenter les entités Domain
- Créer les Use Cases pour les événements
- Définir les interfaces (Ports)

**User Stories** :
- US-01 : Créer un événement
- US-02 : Consulter les événements

**Tâches** :
- [x] Entité `Event` avec validation
- [x] Value Object `Creator`
- [x] Enum `EventType`
- [x] Interface `EventRepository`
- [x] `CreateEventUseCase` avec logique métier
- [x] `GetEventsByOrganizerUseCase`
- [x] Tests unitaires des Use Cases (Kotest + MockK)

**Livrables** :
- ✅ Domain Layer fonctionnel (événements)
- ✅ Tests unitaires (15 tests)
- ✅ Couverture ≥ 90%

**Rétrospective** :
- ✅ Points positifs : Architecture hexagonale bien respectée
- ⚠️ Difficultés : Gestion des erreurs avec Arrow Either
- 🔄 Améliorations : Mieux typer les exceptions

---

### 4.2.3 Sprint 2 : Infrastructure Layer - Événements (2 semaines)

**Objectifs** :
- Implémenter les adaptateurs Infrastructure
- Créer les Repositories SQL
- Créer les Endpoints REST

**User Stories** :
- US-01 : POST /events (créer)
- US-02 : GET /events (consulter)

**Tâches** :
- [x] Configuration Ktor + plugins
- [x] Table `EventTable` avec Exposed
- [x] `SqlEventRepository` avec CRUD
- [x] `CreateEventEndpoint` avec validation
- [x] `GetEventsEndpoint` avec query params
- [x] DTOs et mapping
- [x] Configuration Koin (DI)
- [x] Tests d'intégration avec Testcontainers

**Livrables** :
- ✅ API REST fonctionnelle (2 endpoints)
- ✅ Base de données PostgreSQL
- ✅ Tests d'intégration (8 tests)

**Rétrospective** :
- ✅ Points positifs : Exposed ORM type-safe excellent
- ⚠️ Difficultés : Initialisation base de données
- 🔄 Améliorations : Créer un DatabaseInitializer

---

### 4.2.4 Sprint 3 : Update/Delete Événements + Participants (2 semaines)

**Objectifs** :
- Compléter CRUD événements
- Implémenter gestion des participants

**User Stories** :
- US-03 : Modifier un événement
- US-04 : Supprimer un événement
- US-05 : Ajouter des participants
- US-06 : Consulter les participants

**Tâches** :
- [x] `UpdateEventUseCase` + endpoint
- [x] `DeleteEventUseCase` + endpoint
- [x] Entité `Participant` (Domain)
- [x] `CreateParticipantUseCase`
- [x] `GetParticipantsByEventUseCase`
- [x] Table `ParticipantTable`
- [x] `SqlParticipantRepository`
- [x] Endpoints participants
- [x] Tests unitaires + intégration

**Livrables** :
- ✅ CRUD événements complet (4 endpoints)
- ✅ Gestion participants (3 endpoints)
- ✅ Tests (25 tests cumulés)

**Rétrospective** :
- ✅ Points positifs : Rythme de développement stable
- ⚠️ Difficultés : Gestion des suppressions en cascade
- 🔄 Améliorations : Mieux documenter les contraintes FK

---

### 4.2.5 Sprint 4 : Ressources et Contributions (2 semaines)

**Objectifs** :
- Implémenter gestion des ressources
- Implémenter gestion des contributions

**User Stories** :
- US-09 : Définir les ressources
- US-10 : Consulter les ressources
- US-11 : Indiquer ses contributions
- US-12 : Voir qui apporte quoi

**Tâches** :
- [x] Entité `Resource` (Domain)
- [x] `CreateResourceUseCase`
- [x] `GetResourcesByEventUseCase`
- [x] Table `ResourceTable`
- [x] `SqlResourceRepository`
- [x] Endpoints ressources
- [x] Entité `Contribution` (Domain)
- [x] `AddContributionUseCase`
- [x] `DeleteContributionUseCase`
- [x] Table `ContributionTable`
- [x] `SqlContributionRepository`
- [x] Endpoints contributions
- [x] Tests complets

**Livrables** :
- ✅ API complète (12 endpoints)
- ✅ 4 modules fonctionnels
- ✅ Tests (40 tests cumulés)

**Rétrospective** :
- ✅ Points positifs : MVP fonctionnel !
- ⚠️ Difficultés : Gestion des relations many-to-many
- 🔄 Améliorations : Refactoring pour réduire duplication

---

### 4.2.6 Sprint 5 : Sécurité et Qualité (2 semaines)

**Objectifs** :
- Implémenter les mesures de sécurité OWASP
- Atteindre 0 issue Detekt
- Améliorer la couverture de tests

**User Story** :
- US-15 : Application sécurisée

**Tâches** :
- [x] Audit OWASP Top 10
- [x] Configuration CORS stricte
- [x] Validation multicouche
- [x] Tests d'injection SQL
- [x] Configuration SSL/TLS PostgreSQL
- [x] Gestion sécurisée des erreurs
- [x] Secrets en variables d'environnement
- [x] Correction issues Detekt (596 → 0)
- [x] Configuration Spotless
- [x] Ajout de tests manquants

**Livrables** :
- ✅ 0 issue Detekt
- ✅ 0 vulnérabilité critique
- ✅ Tests de sécurité (7 scénarios)

**Rétrospective** :
- ✅ Points positifs : Qualité de code excellente
- ⚠️ Difficultés : Conflits Detekt/Spotless résolus
- 🔄 Améliorations : Automatiser scan de vulnérabilités

---

### 4.2.7 Sprint 6 : DevOps et Déploiement (2 semaines)

**Objectifs** :
- Containeriser l'application
- Mettre en place le CI/CD
- Déployer en production sur Render

**User Story** :
- US-13 : Pipeline CI/CD automatisé

**Tâches** :
- [x] Création Dockerfile multi-stage
- [x] Configuration docker-compose (dev)
- [x] Workflow GitHub Actions
- [x] Jobs : Detekt → Tests → Build → Deploy
- [x] Configuration Render (render.yaml)
- [x] Variables d'environnement production
- [x] Tests de déploiement
- [x] Documentation déploiement

**Livrables** :
- ✅ Docker image optimisée (180 MB)
- ✅ Pipeline CI/CD fonctionnel
- ✅ Application déployée sur Render
- ✅ HTTPS + SSL/TLS activés

**Rétrospective** :
- ✅ Points positifs : Déploiement automatique réussi
- ⚠️ Difficultés : Temps de démarrage Render (Free Tier)
- 🔄 Améliorations : Ajouter monitoring et alertes

---

### 4.2.8 Sprint 7 : Documentation et Tests finaux (3 semaines)

**Objectifs** :
- Rédiger le dossier professionnel CDA
- Créer le jeu d'essai détaillé
- Préparer la soutenance

**Tâches** :
- [x] Section 1 : Présentation du contexte
- [x] Section 2 : Présentation du projet
- [x] Section 3 : Cahier des charges
- [x] Section 4 : Gestion de projet
- [x] Section 5 : Spécifications fonctionnelles
- [x] Section 6 : Spécifications techniques
- [x] Section 7 : Réalisations et extraits de code
- [x] Section 8 : Éléments de sécurité
- [x] Section 9 : Plan de tests
- [x] Section 10 : Jeu d'essai et analyse
- [x] Section 11 : Veille technologique
- [x] Section 12 : Conclusion générale
- [x] Table des matières
- [x] README et guide de finalisation

**Livrables** :
- ✅ Dossier CDA complet (100 pages)
- ✅ Diagrammes UML
- ✅ Jeu d'essai (7 scénarios)
- ✅ Collection Postman

**Rétrospective** :
- ✅ Points positifs : Documentation complète et professionnelle
- ✅ Objectifs CDA atteints
- 🔄 Prochaine étape : Préparation soutenance orale

---

## 4.3 Outils de gestion de projet

### 4.3.1 Gestion des tâches

**Outil** : Trello

**Organisation des colonnes** :
- **Backlog** : Fonctionnalités à venir
- **To Do (Sprint)** : Tâches du sprint en cours
- **In Progress** : Tâches en cours de réalisation
- **Testing** : En phase de tests
- **Done** : Terminées et validées

**Utilisation des labels** :
- 🔴 Priorité haute
- 🟠 Priorité moyenne
- 🟢 Priorité basse
- 🔵 Documentation
- ⚫ Technique
- 🟣 Sécurité

---

### 4.3.2 Gestion du code source

**Outil** : Git + GitHub

**Stratégie de branches** :
```
main (production)
  ├── develop (intégration)
  │   ├── feature/create-event
  │   ├── feature/participants
  │   ├── feature/resources
  │   └── ...
  └── hotfix/security-patch (si urgent)
```

**Convention de commits** :
```
<type>(<scope>): <description>

Types:
- feat: Nouvelle fonctionnalité
- fix: Correction de bug
- refactor: Refactoring sans changement fonctionnel
- test: Ajout de tests
- docs: Documentation
- chore: Tâches de maintenance
- ci: CI/CD

Exemples:
feat(event): add create event endpoint
fix(security): prevent SQL injection in event repository
test(participant): add integration tests for participant endpoints
docs(readme): update deployment instructions
```

**Pull Requests** :
- Description détaillée
- Screenshots si pertinent
- Checklist (tests, doc, etc.)
- Auto-merge après validation CI

---

### 4.3.3 Documentation technique

**Outil** : Notion

**Organisation** :
- **Architecture** : Décisions architecturales (ADR)
- **API** : Exemples de requêtes, réponses
- **Base de données** : Schémas, migrations
- **Déploiement** : Procédures, troubleshooting
- **Apprentissages** : Notes de veille, tutoriels

**ADR (Architecture Decision Records)** :
Format standardisé pour documenter les décisions importantes :
```markdown
# ADR-001 : Utilisation de Ktor au lieu de Spring Boot

## Statut
Accepté

## Contexte
Besoin d'un framework web pour le backend Kotlin.

## Décision
Utilisation de Ktor pour sa légèreté et son orientation Kotlin.

## Conséquences
Positives:
- Performance supérieure
- API plus simple et idiomatique Kotlin
- Coroutines natives

Négatives:
- Communauté plus petite que Spring
- Moins de ressources/tutoriels
```

---

## 4.4 Suivi et métriques

### 4.4.1 Métriques de développement

**Vélocité** (story points par sprint) :

| Sprint | Story Points planifiés | Story Points réalisés | Vélocité |
|--------|------------------------|------------------------|----------|
| Sprint 1 | 13 | 13 | 100% |
| Sprint 2 | 13 | 11 | 85% |
| Sprint 3 | 21 | 21 | 100% |
| Sprint 4 | 21 | 21 | 100% |
| Sprint 5 | 13 | 13 | 100% |
| Sprint 6 | 8 | 8 | 100% |
| **Moyenne** | **15** | **14.5** | **97%** |

**Observations** :
- Vélocité stable après Sprint 2
- Sprint 2 : sous-estimation de la complexité Exposed ORM
- Ajustement réussi pour les sprints suivants

---

### 4.4.2 Métriques de qualité

**Évolution de la qualité du code** :

| Sprint | Issues Detekt | Couverture tests | Vulnérabilités |
|--------|---------------|------------------|----------------|
| Sprint 1 | - | 90% | - |
| Sprint 2 | - | 75% | - |
| Sprint 3 | - | 70% | - |
| Sprint 4 | - | 68% | - |
| Sprint 5 | 596 → 0 | 75% | 0 |
| Sprint 6 | 0 | 75% | 0 |
| Sprint 7 | 0 | 80% (objectif) | 0 |

**Actions correctives** :
- Sprint 5 : Désactivation detekt-formatting (conflit Spotless)
- Sprints 5-7 : Ajout de tests manquants
- Objectif final : 80% de couverture atteint

---

### 4.4.3 Métriques de déploiement

**Fréquence de déploiement** :

| Période | Déploiements | Succès | Taux de succès |
|---------|--------------|--------|----------------|
| Sprint 6 | 8 | 7 | 87% |
| Sprint 7 | 12 | 12 | 100% |
| **Total** | **20** | **19** | **95%** |

**Temps de déploiement moyen** : 5 minutes 30s

**Incidents en production** : 0

---

## 4.5 Communication et collaboration

### 4.5.1 Communication interne (solo)

**Journal de développement** :
- Notes quotidiennes (apprentissages, blocages, décisions)
- Format Markdown dans Notion
- Revue hebdomadaire

**Rétrospectives individuelles** :
- Fin de chaque sprint
- Format : What went well? / What can be improved? / Action items
- Documentation des apprentissages

---

### 4.5.2 Communautés et support

**Participation aux communautés** :

| Communauté | Plateforme | Utilisation |
|------------|------------|-------------|
| **Kotlin** | Slack | Questions techniques |
| **Ktor** | Slack | Aide sur le framework |
| **Reddit r/Kotlin** | Reddit | Discussions, veille |
| **Stack Overflow** | Web | Recherche de solutions |

**Contributions** :
- Rapports de bugs (Ktor GitHub)
- Réponses à des questions (Stack Overflow)
- Partage d'expérience (Reddit)

---

### 4.5.3 Documentation pour le jury CDA

**Préparation de la soutenance** :

**Support de présentation** :
- Slides concis (15-20 slides max)
- Focus sur l'architecture et les réalisations
- Démonstration live de l'API
- Extraits de code significatifs

**Démonstration** :
- Collection Postman prête
- Scénarios préparés (création événement complet)
- Gestion des erreurs (tests négatifs)

**Anticipation des questions** :
- Pourquoi Kotlin plutôt que Java/Spring ?
- Comment garantir la sécurité ?
- Quelles évolutions prévues ?
- Difficultés rencontrées et solutions ?

---

## 4.6 Gestion des changements

### 4.6.1 Changements de périmètre

**Changement 1 : Simplification de l'authentification**

**Contexte** : Authentification JWT/OAuth2 initialement prévue

**Décision** : Report à la version 2.0, header `x-user-id` simple pour MVP

**Justification** :
- Complexité élevée pour le MVP
- Temps limité (deadline CDA)
- Fonctionnalité non critique pour la démo

**Impact** :
- Gain de temps : 2 semaines
- Risque sécurité : Acceptable (environnement de test)
- Migration future : Prévue et documentée

---

**Changement 2 : Pas de frontend dans le MVP**

**Contexte** : Frontend React initialement prévu

**Décision** : API uniquement, frontend après CDA

**Justification** :
- Focus sur le backend (compétences CDA)
- API utilisable indépendamment
- Démonstration possible via Postman

**Impact** :
- Gain de temps : 4 semaines
- Facilite les tests (API directe)
- Frontend prévu post-CDA

---

### 4.6.2 Gestion des imprévus

**Imprévu 1 : Conflits Detekt/Spotless (Sprint 5)**

**Problème** : 596 issues Detekt, conflits de formatage

**Solution** : Désactivation detekt-formatting, Spotless seul pour le formatage

**Résultat** : 0 issue Detekt, formatage uniforme

**Apprentissage** : Ne pas utiliser 2 outils pour le même objectif

---

**Imprévu 2 : Initialisation base de données (Sprint 2)**

**Problème** : Tables non créées au démarrage, erreurs runtime

**Solution** : Création de `DatabaseInitializer` exécuté au startup

**Résultat** : Initialisation automatique, compatible Render

**Apprentissage** : Anticiper la gestion du schéma DB en production

---

## 4.7 Bilan de la gestion de projet

### 4.7.1 Points forts

✅ **Méthodologie Agile efficace** : Itérations courtes, adaptation rapide  
✅ **Planning respecté** : Deadline CDA tenue  
✅ **Qualité maintenue** : 0 issue Detekt, tests automatisés  
✅ **Documentation rigoureuse** : ADR, dossier CDA complet  
✅ **Gestion des risques** : Imprévus maîtrisés  

---

### 4.7.2 Axes d'amélioration

⚠️ **Estimation initiale** : Sous-estimation de la complexité Exposed (Sprint 2)  
⚠️ **Tests** : Couverture de code à améliorer (objectif 80%)  
⚠️ **Monitoring** : Pas de métriques de performance en production  
⚠️ **Automatisation** : Tests de sécurité à automatiser (CI/CD)  

---

### 4.7.3 Compétences développées

**Gestion de projet** :
- Planification Agile (Scrum)
- Gestion des priorités (MVP)
- Gestion des risques et imprévus
- Documentation technique

**Compétences techniques** :
- Architecture logicielle (hexagonale)
- Kotlin + écosystème (Ktor, Exposed, Arrow)
- DevOps (Docker, CI/CD, Render)
- Sécurité (OWASP, RGPD)
- Tests automatisés (Kotest, Testcontainers)

**Compétences transversales** :
- Autonomie et prise d'initiative
- Résolution de problèmes complexes
- Veille technologique continue
- Communication écrite (documentation)

---

## Conclusion de la section 4

La gestion du projet HappyRow Core a été réalisée selon une **méthodologie Agile Scrum adaptée** à un développeur solo. Les **7 sprints** de 2 semaines ont permis de livrer régulièrement et d'adapter le périmètre selon les contraintes.

Le **planning a été respecté** malgré quelques imprévus, grâce à une **priorisation claire** (MVP) et une **gestion proactive des risques**. Les **outils** (Trello, Git, Notion) ont facilité le suivi et la documentation.

Le projet démontre la capacité à **gérer un projet complet** de manière autonome, en appliquant les **bonnes pratiques** de l'industrie et en maintenant un **haut niveau de qualité**.

**Sections suivantes** : Spécifications fonctionnelles et techniques détaillées.
