# 3. CAHIER DES CHARGES

## 3.1 Expression des besoins

### 3.1.1 Besoins fonctionnels

#### BF-01 : Gestion des événements

**Description** : Le système doit permettre la gestion complète des événements festifs.

**Règles métier** :
- Un événement a un nom unique
- La date de l'événement doit être future
- Un événement a un créateur (organisateur)
- Le créateur est automatiquement participant confirmé

**Fonctionnalités** :
- **BF-01.1** : Créer un événement
  - Champs obligatoires : name, description, eventDate, location, type
  - Champs optionnels : members (liste d'emails)
  - Validation : nom unique, date future
  
- **BF-01.2** : Consulter les événements d'un organisateur
  - Filtre par email de l'organisateur
  - Retour de la liste complète
  
- **BF-01.3** : Modifier un événement
  - Modification de tous les champs sauf identifier et creator
  - Mise à jour automatique de updateDate
  
- **BF-01.4** : Supprimer un événement
  - Suppression en cascade des participants, ressources, contributions

**Priorité** : Haute (MVP)

---

#### BF-02 : Gestion des participants

**Description** : Le système doit permettre de gérer les participants à un événement.

**Règles métier** :
- Un participant est identifié par email (userId)
- Un participant a un statut (CONFIRMED, PENDING, DECLINED)
- Le créateur de l'événement est automatiquement participant CONFIRMED

**Fonctionnalités** :
- **BF-02.1** : Ajouter un participant
  - Champs obligatoires : userId, eventId, status
  - Validation : userId valide, eventId existant
  
- **BF-02.2** : Consulter les participants d'un événement
  - Filtre par eventId
  - Retour de la liste complète avec statuts
  
- **BF-02.3** : Modifier le statut d'un participant
  - Changement de PENDING → CONFIRMED ou DECLINED
  - Mise à jour possible par le participant lui-même

**Priorité** : Haute (MVP)

---

#### BF-03 : Gestion des ressources

**Description** : Le système doit permettre de définir les ressources nécessaires pour un événement.

**Règles métier** :
- Une ressource appartient à un événement
- Une ressource a un type (FOOD, DRINK, EQUIPMENT)
- Une ressource a une quantité et une unité

**Fonctionnalités** :
- **BF-03.1** : Créer une ressource
  - Champs obligatoires : eventId, name, quantity, unit, type
  - Validation : eventId existant, quantity > 0
  
- **BF-03.2** : Consulter les ressources d'un événement
  - Filtre par eventId
  - Retour de la liste avec quantités

**Priorité** : Haute (MVP)

---

#### BF-04 : Gestion des contributions

**Description** : Le système doit permettre d'associer des participants à des ressources (qui apporte quoi).

**Règles métier** :
- Une contribution lie un participant à une ressource
- Une contribution a une quantité promise

**Fonctionnalités** :
- **BF-04.1** : Ajouter une contribution
  - Champs obligatoires : participantId, resourceId, quantity
  - Validation : participant et ressource existants, quantity > 0
  
- **BF-04.2** : Supprimer une contribution
  - Suppression par identifiant de contribution

**Priorité** : Moyenne (MVP)

---

### 3.1.2 Besoins non fonctionnels

#### BNF-01 : Performance

**Exigences** :
- Temps de réponse < 200ms pour 95% des requêtes (p95)
- Temps de démarrage < 5 secondes
- Capacité : 100 requêtes/seconde (cible MVP)
- Temps de déploiement < 10 minutes

**Mesure** :
- Tests de performance avec K6 ou Gatling
- Monitoring avec logs Render
- Métriques de temps de réponse

---

#### BNF-02 : Sécurité

**Exigences** :
- Conformité OWASP Top 10
- Protection injection SQL : 100%
- SSL/TLS obligatoire en production
- CORS avec liste blanche stricte
- Validation de toutes les entrées
- Gestion sécurisée des erreurs (pas de stack trace exposée)
- Secrets en variables d'environnement

**Mesure** :
- Tests d'injection SQL
- Scan de vulnérabilités (Snyk)
- Audit de sécurité

---

#### BNF-03 : Fiabilité

**Exigences** :
- Uptime ≥ 95% (hors maintenance)
- Transactions ACID (base de données)
- Gestion des erreurs robuste
- Logs structurés pour debugging
- Rollback automatique en cas d'erreur

**Mesure** :
- Monitoring uptime (Render)
- Tests d'intégration
- Revue des logs d'erreur

---

#### BNF-04 : Maintenabilité

**Exigences** :
- Architecture Clean (hexagonale)
- Code quality : 0 issue Detekt
- Couverture de code ≥ 80%
- Documentation complète
- Tests automatisés

**Mesure** :
- Rapports Detekt
- Rapports JaCoCo
- Revue de code

---

#### BNF-05 : Portabilité

**Exigences** :
- Containerisation Docker
- Configuration externalisée (variables d'environnement)
- Compatibilité JDK 21+
- Compatibilité PostgreSQL 12+

**Mesure** :
- Tests sur différents environnements
- Validation Docker

---

#### BNF-06 : Conformité

**Exigences** :
- RGPD : Protection des données personnelles
- Licences open source compatibles (MIT)
- Standards REST (HTTP methods, status codes)
- Format JSON pour les échanges

**Mesure** :
- Audit RGPD
- Validation des licenses
- Tests API

---

## 3.2 Spécifications fonctionnelles détaillées

### 3.2.1 API REST Endpoints

#### Événements

**POST /event/configuration/api/v1/events**
- **Description** : Créer un événement
- **Headers** : `x-user-id` (email de l'organisateur)
- **Body** :
  ```json
  {
    "name": "string",
    "description": "string",
    "event_date": "ISO-8601",
    "location": "string",
    "type": "PARTY|BIRTHDAY|DINER|SNACK"
  }
  ```
- **Réponse 201** : Événement créé avec identifier
- **Erreurs** : 400 (validation), 409 (nom existant)

**GET /event/configuration/api/v1/events?organizerId={email}**
- **Description** : Récupérer les événements d'un organisateur
- **Query params** : `organizerId` (email)
- **Réponse 200** : Liste des événements
- **Erreurs** : 400 (organizerId manquant)

**PUT /event/configuration/api/v1/events/{identifier}**
- **Description** : Modifier un événement
- **Path param** : `identifier` (UUID)
- **Body** : Champs modifiables
- **Réponse 200** : Événement modifié
- **Erreurs** : 404 (non trouvé), 400 (validation)

**DELETE /event/configuration/api/v1/events/{identifier}**
- **Description** : Supprimer un événement
- **Path param** : `identifier` (UUID)
- **Réponse 204** : Suppression réussie
- **Erreurs** : 404 (non trouvé)

---

#### Participants

**POST /event/configuration/api/v1/participants**
- **Description** : Ajouter un participant
- **Body** :
  ```json
  {
    "user_id": "UUID",
    "event_id": "UUID",
    "status": "CONFIRMED|PENDING|DECLINED"
  }
  ```
- **Réponse 201** : Participant ajouté
- **Erreurs** : 400 (validation), 404 (événement non trouvé)

**GET /event/configuration/api/v1/participants?eventId={uuid}**
- **Description** : Récupérer les participants d'un événement
- **Query params** : `eventId` (UUID)
- **Réponse 200** : Liste des participants
- **Erreurs** : 400 (eventId manquant)

**PUT /event/configuration/api/v1/participants/{identifier}**
- **Description** : Modifier le statut d'un participant
- **Path param** : `identifier` (UUID)
- **Body** :
  ```json
  {
    "status": "CONFIRMED|PENDING|DECLINED"
  }
  ```
- **Réponse 200** : Statut modifié
- **Erreurs** : 404 (non trouvé)

---

#### Ressources

**POST /event/configuration/api/v1/resources**
- **Description** : Créer une ressource
- **Body** :
  ```json
  {
    "event_id": "UUID",
    "name": "string",
    "quantity": "number",
    "unit": "string",
    "type": "FOOD|DRINK|EQUIPMENT"
  }
  ```
- **Réponse 201** : Ressource créée
- **Erreurs** : 400 (validation), 404 (événement non trouvé)

**GET /event/configuration/api/v1/resources?eventId={uuid}**
- **Description** : Récupérer les ressources d'un événement
- **Query params** : `eventId` (UUID)
- **Réponse 200** : Liste des ressources
- **Erreurs** : 400 (eventId manquant)

---

#### Contributions

**POST /event/configuration/api/v1/contributions**
- **Description** : Ajouter une contribution
- **Body** :
  ```json
  {
    "participant_id": "UUID",
    "resource_id": "UUID",
    "quantity": "number"
  }
  ```
- **Réponse 201** : Contribution ajoutée
- **Erreurs** : 400 (validation), 404 (participant ou ressource non trouvé)

**DELETE /event/configuration/api/v1/contributions/{participantId}/{resourceId}**
- **Description** : Supprimer une contribution
- **Path params** : `participantId`, `resourceId`
- **Réponse 204** : Suppression réussie
- **Erreurs** : 404 (non trouvé)

---

## 3.3 Contraintes techniques

### 3.3.1 Contraintes d'architecture

**Architecture hexagonale (Ports & Adapters)** :
- Couche Domain : Entités, Use Cases, Interfaces (Ports)
- Couche Infrastructure : Adaptateurs (Endpoints, Repositories, Configuration)
- Règle : Le Domain ne dépend jamais de l'Infrastructure

**Séparation des responsabilités** :
- Endpoints : Validation HTTP, mapping DTO ↔ Domain
- Use Cases : Logique métier, orchestration
- Repositories : Persistance, requêtes SQL

**Injection de dépendances** :
- Koin pour la gestion des dépendances
- Configuration centralisée dans des modules
- Pas de `new` dans le code métier

---

### 3.3.2 Contraintes de base de données

**PostgreSQL** :
- Version minimum : 12
- Schéma dédié : `configuration`
- Enums PostgreSQL : `EVENT_TYPE`, `PARTICIPANT_STATUS`, `RESOURCE_TYPE`

**Contraintes d'intégrité** :
- Clés primaires : UUID générés automatiquement
- Contraintes UNIQUE : `event.name`
- Contraintes de clés étrangères avec CASCADE
- Index sur colonnes fréquemment recherchées

**Transactions** :
- ACID garanti
- Isolation level : READ COMMITTED (défaut PostgreSQL)
- Rollback automatique en cas d'erreur

---

### 3.3.3 Contraintes de sécurité

**Validation des entrées** :
- Validation format (DTO)
- Validation métier (Use Case)
- Validation données (Repository)

**Protection injection SQL** :
- Utilisation ORM Exposed (requêtes paramétrées)
- Pas de concaténation SQL
- Échappement automatique

**CORS** :
- Liste blanche stricte
- Pas de wildcard (`*`)
- Headers autorisés : `x-user-id`, `Content-Type`, `Authorization`

**SSL/TLS** :
- Obligatoire en production
- Base de données : `sslmode=require`
- HTTPS pour l'API

---

## 3.4 Exigences de qualité

### 3.4.1 Tests

**Tests unitaires** :
- Cible : 70% de la base de code
- Focus : Use Cases, logique métier
- Framework : Kotest + MockK

**Tests d'intégration** :
- Cible : 20% de la base de code
- Focus : Endpoints + Base de données
- Framework : Kotest + Testcontainers

**Tests de sécurité** :
- Tests d'injection SQL
- Tests CORS
- Tests validation

**Couverture globale** :
- Objectif : ≥ 80%
- Outil : JaCoCo
- Rapports dans CI/CD

---

### 3.4.2 Qualité du code

**Analyse statique** :
- Outil : Detekt
- Objectif : 0 issue
- Intégration CI/CD

**Formatage** :
- Outil : Spotless + KtLint
- Style : Kotlin officiel
- Automatique (pre-commit)

**Conventions de nommage** :
- Classes : PascalCase
- Fonctions : camelCase
- Constantes : SCREAMING_SNAKE_CASE
- Packages : lowercase

---

### 3.4.3 Documentation

**Code** :
- Commentaires KDoc pour API publiques
- Commentaires inline pour logique complexe
- README.md à la racine

**API** :
- Documentation inline (Ktor)
- Collection Postman
- Exemples de requêtes cURL

**Architecture** :
- ADR (Architecture Decision Records)
- Diagrammes UML
- Schémas de base de données

---

## 3.5 Environnements

### 3.5.1 Environnement de développement

**Configuration** :
- Docker Compose pour PostgreSQL local
- Variables d'environnement : `.env` (ignoré par Git)
- Port : 8080 (backend), 5432 (PostgreSQL)

**Base de données** :
- PostgreSQL 15 en container Docker
- Script d'initialisation : `init-db.sql`
- Données de test (seed) optionnelles

---

### 3.5.2 Environnement de CI/CD

**GitHub Actions** :
- Déclenchement : Push sur `main`, Pull Requests
- Jobs : Detekt → Tests → Build → Deploy
- Temps d'exécution : < 10 minutes

**Secrets requis** :
- `RENDER_API_KEY` : Déploiement sur Render
- `RENDER_SERVICE_ID` : Identification du service

---

### 3.5.3 Environnement de production

**Render Platform** :
- Region : EU (Francfort)
- Plan : Free (MVP), Standard (production future)
- Auto-scaling : Non (Free Tier)

**Base de données** :
- Render PostgreSQL Managed
- Plan : Free (1 GB)
- Backups : Manuels (Free Tier)

**Monitoring** :
- Logs : Render Dashboard
- Uptime : Render monitoring
- Alertes : Email (Render)

---

## 3.6 Planning et jalons

### 3.6.1 Phases du projet

**Phase 1 : Conception (2 semaines - Août 2025)**
- Analyse des besoins
- Choix technologiques
- Architecture détaillée
- Modélisation base de données

**Phase 2 : Développement Core (6 semaines - Sept-Oct 2025)**
- Setup projet Gradle
- Module Domain (entités, Use Cases)
- Module Infrastructure (Repositories)
- Tests unitaires

**Phase 3 : API REST (4 semaines - Oct-Nov 2025)**
- Endpoints Ktor
- Validation et gestion d'erreurs
- Tests d'intégration
- Documentation API

**Phase 4 : Sécurité et Qualité (3 semaines - Nov-Déc 2025)**
- OWASP Top 10
- Detekt + Spotless
- Tests de sécurité
- Revue de code

**Phase 5 : DevOps (2 semaines - Déc 2025)**
- Dockerfile
- GitHub Actions CI/CD
- Déploiement Render
- Monitoring

**Phase 6 : Documentation (3 semaines - Déc 2025-Jan 2026)**
- Dossier professionnel CDA
- Préparation soutenance
- Tests finaux

---

### 3.6.2 Jalons clés

| Jalon | Date cible | Statut |
|-------|------------|--------|
| Architecture validée | 15 août 2025 | ✅ |
| Domain Layer terminé | 30 sept 2025 | ✅ |
| API REST fonctionnelle | 30 oct 2025 | ✅ |
| Tests et qualité OK | 20 nov 2025 | ✅ |
| Déploiement production | 15 déc 2025 | ✅ |
| Dossier CDA finalisé | 5 jan 2026 | ✅ |
| Soutenance CDA | 25 jan 2026 | 🔄 |

---

## 3.7 Risques et mitigation

### 3.7.1 Risques techniques

**Risque 1 : Complexité de l'architecture hexagonale**
- Probabilité : Moyenne
- Impact : Moyen
- Mitigation : Formation, documentation, exemples
- Statut : ✅ Maîtrisé

**Risque 2 : Performance insuffisante**
- Probabilité : Faible
- Impact : Moyen
- Mitigation : Tests de performance, optimisations
- Statut : ✅ Validé (temps < 200ms)

**Risque 3 : Vulnérabilités de sécurité**
- Probabilité : Moyenne
- Impact : Élevé
- Mitigation : OWASP, tests, scan automatique
- Statut : ✅ Maîtrisé (0 CVE critique)

---

### 3.7.2 Risques organisationnels

**Risque 4 : Dépassement du planning**
- Probabilité : Moyenne
- Impact : Élevé (deadline CDA)
- Mitigation : Priorisation MVP, itérations courtes
- Statut : ✅ Planning respecté

**Risque 5 : Manque de compétences**
- Probabilité : Faible
- Impact : Moyen
- Mitigation : Veille, formation, communautés
- Statut : ✅ Maîtrisé

---

### 3.7.3 Risques infrastructure

**Risque 6 : Indisponibilité Render**
- Probabilité : Faible
- Impact : Moyen
- Mitigation : Backup code + DB, documentation déploiement
- Statut : ⚠️ Acceptable (plan gratuit)

**Risque 7 : Limite Free Tier dépassée**
- Probabilité : Faible
- Impact : Faible
- Mitigation : Monitoring usage, plan de migration
- Statut : ✅ Sous les limites

---

## Conclusion de la section 3

Ce cahier des charges définit les **besoins fonctionnels** (gestion événements, participants, ressources, contributions) et **non fonctionnels** (performance, sécurité, fiabilité) du projet HappyRow Core.

Les **spécifications détaillées** (API REST, contraintes techniques, qualité) fournissent un cadre clair pour le développement. Le **planning** en 6 phases et les **jalons** permettent de suivre l'avancement.

Les **risques identifiés** sont maîtrisés grâce à des stratégies de mitigation adaptées. Le projet est conforme aux exigences du **titre CDA** et aux standards de l'industrie.

**Section suivante** : Gestion de projet et méthodologie Agile.
