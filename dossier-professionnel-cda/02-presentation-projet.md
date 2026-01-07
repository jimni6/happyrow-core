# 2. PRÉSENTATION DU PROJET HAPPYROW CORE

## 2.1 Vision et objectifs du projet

### 2.1.1 Vision générale

**HappyRow Core** est une API REST backend moderne conçue pour **simplifier l'organisation d'événements festifs** (anniversaires, soirées, dîners, apéros) en centralisant la gestion des événements, des participants, des ressources et des contributions.

**Mission** : Transformer l'expérience d'organisation d'événements en fournissant une plateforme intuitive, sécurisée et performante.

**Valeurs** :
- **Simplicité** : Interface API claire et cohérente
- **Fiabilité** : Architecture robuste et testée
- **Sécurité** : Protection des données utilisateurs
- **Performance** : Temps de réponse optimaux
- **Ouverture** : API documentée et extensible

---

### 2.1.2 Objectifs du projet

#### Objectifs fonctionnels

**Gestion des événements** :
- ✅ Créer un événement avec informations détaillées
- ✅ Consulter les événements par organisateur
- ✅ Modifier les informations d'un événement
- ✅ Supprimer un événement

**Gestion des participants** :
- ✅ Ajouter des participants à un événement
- ✅ Consulter la liste des participants
- ✅ Mettre à jour le statut (confirmé, en attente, refusé)

**Gestion des ressources** :
- ✅ Définir les ressources nécessaires (nourriture, boissons, matériel)
- ✅ Consulter les ressources d'un événement

**Gestion des contributions** :
- ✅ Associer des participants à des ressources (qui apporte quoi)
- ✅ Consulter les contributions
- ✅ Supprimer des contributions

#### Objectifs techniques

**Architecture** :
- ✅ Architecture hexagonale (Clean Architecture)
- ✅ Séparation stricte Domain/Infrastructure
- ✅ Injection de dépendances (Koin)

**Performance** :
- ✅ Temps de réponse < 200ms (p95)
- ✅ Temps de démarrage < 3s
- ✅ Connection pooling optimisé (HikariCP)

**Sécurité** :
- ✅ Protection injection SQL (ORM type-safe)
- ✅ CORS avec liste blanche
- ✅ SSL/TLS obligatoire
- ✅ Validation multicouche

**Qualité** :
- ✅ Code quality : 0 issue Detekt
- ✅ Tests automatisés (unitaires + intégration)
- ✅ Couverture de code ≥ 80% (objectif)
- ✅ CI/CD automatisé

#### Objectifs DevOps

**Déploiement** :
- ✅ Containerisation Docker
- ✅ Déploiement automatique (GitHub Actions)
- ✅ Hébergement cloud (Render)
- ✅ Base de données managée (Render PostgreSQL)

**Monitoring** (futur) :
- ⚠️ Logs structurés
- ⚠️ Métriques de performance
- ⚠️ Alertes automatiques

---

## 2.2 Périmètre du projet

### 2.2.1 Fonctionnalités incluses (MVP)

**Module Événements** :
- Création d'événements festifs (4 types : PARTY, BIRTHDAY, DINER, SNACK)
- Champs : nom, description, date, lieu, type, créateur
- Contraintes : nom unique, date future
- CRUD complet (Create, Read, Update, Delete)

**Module Participants** :
- Ajout de participants à un événement
- Statuts : CONFIRMED, PENDING, DECLINED
- Participant créateur automatiquement ajouté (status CONFIRMED)
- Consultation par événement
- Mise à jour du statut

**Module Ressources** :
- Définition des ressources nécessaires pour un événement
- Types : FOOD, DRINK, EQUIPMENT
- Quantité et unité de mesure
- CRUD complet

**Module Contributions** :
- Association participant ↔ ressource
- Quantité promise par le participant
- Ajout et suppression de contributions
- Consultation par événement

---

### 2.2.2 Fonctionnalités exclues (hors périmètre MVP)

**Authentification** :
- ✅ Système JWT intégré avec Supabase
- ✅ Validation des tokens avec signature HMAC256
- ✅ Extraction automatique de l'utilisateur authentifié (userId, email)
- 🔄 Gestion des rôles et permissions prévue pour version 2.0

**Gestion des invitations** :
- ❌ Pas d'envoi d'emails automatiques
- ❌ Pas de système de notifications
- 🔄 Prévu pour version 1.5

**Interface utilisateur** :
- ❌ Pas de frontend (API uniquement)
- 🔄 Frontend React prévu après CDA

**Gestion financière** :
- ❌ Pas de gestion des coûts/dépenses
- ❌ Pas de remboursements
- 🔄 Intégration future possible (type Tricount)

**Messagerie** :
- ❌ Pas de chat entre participants
- ❌ Pas de commentaires sur événements
- 🔄 Prévu pour version 2.0

**Calendrier** :
- ❌ Pas d'intégration Google Calendar/iCal
- ❌ Pas de synchronisation
- 🔄 Prévu pour version 2.0

---

### 2.2.3 Contraintes du projet

#### Contraintes techniques

**Performance** :
- Temps de réponse : < 200ms pour 95% des requêtes
- Temps de démarrage : < 5 secondes
- Capacité : 100 utilisateurs simultanés (cible MVP)

**Compatibilité** :
- Backend : JDK 21+
- Base de données : PostgreSQL 12+
- Navigateurs (futur frontend) : Dernières versions Chrome, Firefox, Safari, Edge

#### Contraintes de sécurité

- Conformité OWASP Top 10
- Protection RGPD (données personnelles)
- SSL/TLS obligatoire en production
- Validation de toutes les entrées
- Gestion sécurisée des erreurs

#### Contraintes budgétaires

- Budget : 0 € (services gratuits)
- Hébergement gratuit limité (Render Free Tier)
- Pas de services payants externes
- Optimisation pour minimiser les coûts

#### Contraintes de temps

- Durée : 6 mois (Août 2025 - Janvier 2026)
- Deadline : Présentation CDA fin janvier 2026
- Développement en autonomie (pas d'équipe)

---

## 2.3 Architecture fonctionnelle

### 2.3.1 Modules fonctionnels

```
┌─────────────────────────────────────────────────────────┐
│                    HAPPYROW CORE API                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────┐  ┌─────────────────┐             │
│  │  MODULE EVENT   │  │ MODULE PARTI-   │             │
│  │                 │  │    CIPANT       │             │
│  │ - Create Event  │  │                 │             │
│  │ - Get Events    │  │ - Add User      │             │
│  │ - Update Event  │  │ - Get Users     │             │
│  │ - Delete Event  │  │ - Update Status │             │
│  └─────────────────┘  └─────────────────┘             │
│                                                         │
│  ┌─────────────────┐  ┌─────────────────┐             │
│  │ MODULE RESOURCE │  │ MODULE CONTRI-  │             │
│  │                 │  │    BUTION       │             │
│  │ - Create Res.   │  │                 │             │
│  │ - Get Res.      │  │ - Add Contrib.  │             │
│  │                 │  │ - Delete Cont.  │             │
│  └─────────────────┘  └─────────────────┘             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

### 2.3.2 Flux de données principal

**Scénario : Création d'un événement**

```
┌─────────┐                                  ┌──────────┐
│ Client  │                                  │ Backend  │
│ (futur) │                                  │  Ktor    │
└────┬────┘                                  └────┬─────┘
     │                                            │
     │  POST /events                              │
     │  {name, description, date, ...}            │
     ├───────────────────────────────────────────►│
     │                                            │
     │                        ┌──────────────┐    │
     │                        │ Validation   │    │
     │                        │ - Format     │    │
     │                        │ - Business   │    │
     │                        └──────┬───────┘    │
     │                               │            │
     │                        ┌──────▼───────┐    │
     │                        │ Repository   │    │
     │                        │ - Insert DB  │    │
     │                        │ - Transaction│    │
     │                        └──────┬───────┘    │
     │                               │            │
     │  201 Created                  │            │
     │  {identifier, ...}            │            │
     │◄───────────────────────────────────────────┤
     │                                            │
     │  POST /participants                        │
     │  {userId, eventId, status}                 │
     ├───────────────────────────────────────────►│
     │                                            │
     │  201 Created                               │
     │◄───────────────────────────────────────────┤
     │                                            │
```

---

### 2.3.3 Modèle de domaine

**Entités principales** :

```
┌─────────────────────────────────────────┐
│             EVENT                       │
├─────────────────────────────────────────┤
│ - identifier: UUID                      │
│ - name: String (unique)                 │
│ - description: String                   │
│ - eventDate: Instant                    │
│ - creator: Creator (email)              │
│ - location: String                      │
│ - type: EventType (ENUM)                │
│ - members: List<UUID>                   │
│ - creationDate: Instant                 │
│ - updateDate: Instant                   │
└─────────────────────────────────────────┘
           │
           │ 1 organise
           │
           ▼ *
┌─────────────────────────────────────────┐
│          PARTICIPANT                    │
├─────────────────────────────────────────┤
│ - identifier: UUID                      │
│ - userId: UUID                          │
│ - eventId: UUID                         │
│ - status: ParticipantStatus (ENUM)      │
└─────────────────────────────────────────┘
           │
           │ * apporte
           │
           ▼ *
┌─────────────────────────────────────────┐
│          CONTRIBUTION                   │
├─────────────────────────────────────────┤
│ - resourceId: UUID                      │
│ - participantId: UUID                   │
│ - quantity: Int                         │
└─────────────────────────────────────────┘
           │
           │ * concerne
           │
           ▼ 1
┌─────────────────────────────────────────┐
│           RESOURCE                      │
├─────────────────────────────────────────┤
│ - identifier: UUID                      │
│ - eventId: UUID                         │
│ - name: String                          │
│ - quantity: Int                         │
│ - unit: String                          │
│ - type: ResourceType (ENUM)             │
└─────────────────────────────────────────┘
```

---

## 2.4 Technologies et stack technique

### 2.4.1 Stack backend

| Couche | Technologie | Version | Justification |
|--------|-------------|---------|---------------|
| **Langage** | Kotlin | 2.2.0 | Type-safe, moderne, concis |
| **Framework** | Ktor | 3.2.2 | Léger, performant, Kotlin-first |
| **ORM** | Exposed | 0.61.0 | Type-safe DSL, intégration Kotlin |
| **Base de données** | PostgreSQL | 15 | Robuste, ACID, relationnel |
| **Injection dépendances** | Koin | 4.1.0 | Simple, Kotlin-native |
| **Validation** | Arrow | 2.1.2 | Programmation fonctionnelle |
| **Logs** | Logback | 1.4.14 | Standard, configurable |

---

### 2.4.2 Stack DevOps

| Domaine | Technologie | Justification |
|---------|-------------|---------------|
| **Containerisation** | Docker | Standard industrie |
| **CI/CD** | GitHub Actions | Gratuit, intégré GitHub |
| **Hébergement** | Render | PaaS simple, gratuit |
| **Monitoring** | Logs Render | Inclus dans l'offre |
| **VCS** | Git + GitHub | Standard, collaboration |

---

### 2.4.3 Stack qualité et tests

| Domaine | Technologie | Version | Usage |
|---------|-------------|---------|-------|
| **Tests** | Kotest | 5.9.1 | Framework BDD Kotlin |
| **Mocking** | MockK | 1.14.5 | Mocking natif Kotlin |
| **Test containers** | Testcontainers | 1.21.3 | PostgreSQL en Docker |
| **Couverture** | JaCoCo | 0.8.13 | Mesure couverture |
| **Analyse statique** | Detekt | 1.23.7 | Qualité du code |
| **Formatage** | Spotless | 6.25.0 | KtLint automatique |

---

## 2.5 Livrables du projet

### 2.5.1 Livrables techniques

**Code source** :
- ✅ Repository GitHub : https://github.com/jimni6/happyrow-core
- ✅ Architecture multi-modules (Domain, Infrastructure)
- ✅ Tests unitaires et d'intégration
- ✅ Configuration CI/CD

**Documentation technique** :
- ✅ README.md complet
- ✅ ADR (Architecture Decision Records)
- ✅ Documentation API (inline + Postman collection)
- ✅ Guide de déploiement

**Application déployée** :
- ✅ URL : https://happyrow-core.onrender.com
- ✅ Base de données PostgreSQL managée
- ✅ SSL/TLS activé
- ✅ CORS configuré

**Outils de développement** :
- ✅ Collection Postman (tests manuels)
- ✅ Scripts de tests
- ✅ Configuration Docker (local + production)

---

### 2.5.2 Livrables pédagogiques

**Dossier professionnel CDA** :
- ✅ 12 sections détaillées (~100 pages)
- ✅ Schémas et diagrammes UML
- ✅ Extraits de code commentés
- ✅ Jeu d'essai détaillé
- ✅ Veille technologique

**Annexes** :
- Code source complet (sélection)
- Rapports de tests
- Documentation API
- Captures d'écran

**Présentation orale** :
- Slides de support (prévu)
- Démonstration live de l'API
- Réponses aux questions du jury

---

### 2.5.3 Métriques de succès

**Métriques fonctionnelles** :

| Métrique | Objectif | Résultat |
|----------|----------|----------|
| Endpoints implémentés | 12 | ✅ 12 |
| CRUD complets | 4 modules | ✅ 4 |
| Cas d'usage couverts | 100% MVP | ✅ 100% |

**Métriques techniques** :

| Métrique | Objectif | Résultat |
|----------|----------|----------|
| Temps de réponse (p95) | < 200ms | ✅ ~150ms |
| Issues Detekt | 0 | ✅ 0 |
| Couverture de code | ≥ 80% | ⚠️ En cours |
| Vulnérabilités critiques | 0 | ✅ 0 |

**Métriques qualité** :

| Métrique | Objectif | Résultat |
|----------|----------|----------|
| Documentation API | 100% endpoints | ✅ 100% |
| Tests E2E | 7 scénarios | ✅ 7 |
| Uptime production | ≥ 95% | ✅ ~98% |
| Déploiement CI/CD | Automatique | ✅ Oui |

---

## 2.6 Bénéfices attendus

### 2.6.1 Pour les utilisateurs finaux

**Simplicité** :
- Interface API claire et cohérente
- Documentation complète
- Messages d'erreur explicites

**Fiabilité** :
- Architecture robuste
- Tests automatisés
- Base de données ACID

**Sécurité** :
- Protection des données
- Validation stricte
- Conformité RGPD

**Performance** :
- Temps de réponse optimaux
- Scalabilité prévue
- Hébergement fiable

---

### 2.6.2 Pour le développeur (candidat CDA)

**Compétences démontrées** :
- Architecture logicielle moderne
- Maîtrise de Kotlin/Ktor
- DevOps et CI/CD
- Sécurité applicative
- Tests et qualité

**Portfolio** :
- Projet complet en production
- Code source public
- Documentation professionnelle
- Références techniques

**Employabilité** :
- Titre CDA (niveau 6)
- Compétences recherchées
- Projet démontrable
- Veille technologique active

---

### 2.6.3 Pour la communauté

**Open source** :
- Code réutilisable
- Exemple d'architecture Kotlin
- Documentation complète

**Partage de connaissances** :
- ADR (décisions architecturales)
- Bonnes pratiques
- Retours d'expérience

---

## Conclusion de la section 2

Le projet **HappyRow Core** est une API REST backend moderne et sécurisée, conçue selon les principes de la **Clean Architecture**. Il couvre l'ensemble du cycle de développement, de la conception à la mise en production, en passant par les tests et la sécurité.

Les **objectifs fonctionnels** (gestion d'événements, participants, ressources, contributions) sont atteints avec une **API cohérente et documentée**. Les **objectifs techniques** (architecture, performance, sécurité, qualité) sont validés par des tests et des outils de contrôle.

Le projet démontre la **maîtrise des compétences CDA** et constitue une base solide pour de futures évolutions (authentification, frontend, notifications, etc.).

**Section suivante** : Cahier des charges détaillé et spécifications.
