# 1. PRÉSENTATION DU CONTEXTE ET DE L'ENTREPRISE

## 1.1 Contexte du projet

### 1.1.1 Cadre de réalisation

Ce projet a été réalisé dans le cadre de l'obtention du titre professionnel **Concepteur Développeur d'Applications (CDA)**, niveau 6 (équivalent Bac+3/4), enregistré au RNCP sous le code RNCP37873.

**Type de projet** : Projet de formation professionnelle  
**Durée** : 6 mois (Août 2025 - Janvier 2026)  
**Statut** : Projet personnel en autonomie complète  
**Environnement** : Télétravail avec outils professionnels

---

### 1.1.2 Origine du projet

**Problématique identifiée** :

L'organisation d'événements festifs (anniversaires, soirées, dîners entre amis) implique plusieurs défis récurrents :

1. **Coordination difficile** : Multiples échanges par SMS, email, WhatsApp
2. **Gestion des participants** : Difficile de suivre qui vient, qui apporte quoi
3. **Organisation des ressources** : Nourriture, boissons, matériel à coordonner
4. **Répartition des coûts** : Calculs manuels, oublis, conflits
5. **Manque de centralisation** : Informations éparpillées, perte de messages

**Besoin identifié** :

Une solution centralisée permettant de :
- Créer et gérer des événements festifs
- Inviter et suivre les participants
- Organiser les contributions (qui apporte quoi)
- Faciliter la communication entre participants
- Simplifier l'organisation collective

---

### 1.1.3 Public cible

**Profil des utilisateurs** :

- **Organisateurs** : Personnes souhaitant organiser un événement festif
- **Participants** : Invités aux événements
- **Âge** : 18-50 ans (population active)
- **Compétences techniques** : Utilisation courante d'applications mobiles/web
- **Contexte d'utilisation** : Personnel, loisirs, vie sociale

**Cas d'usage typiques** :

1. Marie organise son anniversaire (30 ans) avec 25 invités
2. Un groupe d'amis planifie un apéro mensuel
3. Une famille organise un dîner de Noël avec contributions
4. Des collègues préparent une soirée de départ

---

## 1.2 Environnement de développement

### 1.2.1 Organisation du travail

**Statut** : Développeur indépendant (projet personnel)

**Modalités de travail** :
- Télétravail à 100%
- Autonomie complète dans les décisions techniques
- Gestion de projet en méthodologie Agile (Scrum adapté)
- Itérations de 2 semaines (sprints)

**Outils de collaboration** :
- **GitHub** : Gestion du code source, issues, pull requests
- **Trello** : Gestion des tâches et du backlog
- **Notion** : Documentation technique et décisions architecturales
- **Discord** : Participation aux communautés Kotlin/Ktor

---

### 1.2.2 Infrastructure technique

**Environnement de développement** :

| Composant | Outil/Version |
|-----------|---------------|
| **OS** | macOS Sonoma 14.5 |
| **IDE** | IntelliJ IDEA Ultimate 2024.3 |
| **JDK** | OpenJDK 21 (Temurin) |
| **Build tool** | Gradle 8.5 |
| **VCS** | Git 2.42 |
| **Terminal** | iTerm2 + Zsh |
| **API Testing** | Postman, Insomnia |
| **Database client** | DBeaver 23.3.0 |

**Environnement d'hébergement** :

| Composant | Service |
|-----------|---------|
| **Application** | Render (Platform as a Service) |
| **Base de données** | Render PostgreSQL (managed) |
| **CI/CD** | GitHub Actions |
| **Frontend** | Vercel (futur) |
| **Domaine** | happyrow-core.onrender.com |

---

### 1.2.3 Budget et ressources

**Budget alloué** : 0 € (services gratuits)

**Services utilisés** :

| Service | Plan | Coût mensuel | Usage |
|---------|------|--------------|-------|
| **Render Web Service** | Free | 0 € | Hébergement backend |
| **Render PostgreSQL** | Free | 0 € | Base de données (1 GB) |
| **GitHub** | Free | 0 € | Code source + CI/CD |
| **Vercel** | Free | 0 € | Futur frontend |

**Contraintes liées au plan gratuit** :
- Render : Mise en veille après 15 min d'inactivité (démarrage ~30s)
- PostgreSQL : Limite de 1 GB de stockage
- Pas de backup automatique
- Pas de support technique premium

**Stratégie d'optimisation** :
- Utilisation de plan gratuit pour le développement et les tests
- Migration vers plan payant si passage en production réelle
- Architecture prévue pour scalabilité future

---

## 1.3 Veille et préparation du projet

### 1.3.1 Étude de l'existant

**Solutions concurrentes analysées** :

#### 1. Doodle
- **Avantages** : Planification de dates, sondages
- **Limites** : Pas de gestion de ressources, pas de contributions
- **Positionnement** : Complémentaire (focus sur les dates)

#### 2. WhatsApp / Messenger
- **Avantages** : Omniprésent, discussions de groupe
- **Limites** : Pas structuré, perte d'informations, pas de suivi
- **Positionnement** : Outil de communication adjacent

#### 3. Tricount
- **Avantages** : Excellent pour les dépenses partagées
- **Limites** : Pas de gestion d'événements, focus uniquement financier
- **Positionnement** : Complémentaire (focus sur les coûts)

#### 4. Facebook Events
- **Avantages** : Large base d'utilisateurs, visibilité
- **Limites** : Dépendance à Facebook, pas de gestion fine des contributions
- **Positionnement** : Alternatif (événements publics)

**Différenciation de HappyRow Core** :
- ✅ Spécialisé dans les événements festifs privés
- ✅ Gestion intégrée : événements + participants + ressources + contributions
- ✅ API REST moderne et évolutive
- ✅ Architecture découplée (backend/frontend indépendants)
- ✅ Open source et hébergement contrôlé

---

### 1.3.2 Choix technologiques préliminaires

**Critères de sélection** :

1. **Performance** : Temps de réponse < 200ms
2. **Maintenabilité** : Code propre, architecture claire
3. **Sécurité** : Standards OWASP, protection des données
4. **Scalabilité** : Capacité à gérer la croissance
5. **Productivité** : Frameworks matures, documentation
6. **Coût** : Solutions open source privilégiées

**Technologies retenues** :

| Domaine | Technologie | Justification |
|---------|-------------|---------------|
| **Langage** | Kotlin 2.2 | Type-safe, moderne, interopérable Java |
| **Framework web** | Ktor 3.2 | Léger, performant, Kotlin-first |
| **Base de données** | PostgreSQL 15 | Robuste, ACID, open source |
| **ORM** | Exposed 0.61 | Type-safe, intégration Kotlin |
| **Architecture** | Hexagonale | Découplage, testabilité, maintenabilité |

Voir section 6 pour les justifications détaillées.

---

### 1.3.3 Compétences mobilisées

**Compétences techniques** :

- ✅ Programmation orientée objet (Kotlin)
- ✅ Programmation fonctionnelle (Arrow)
- ✅ Architecture logicielle (Clean Architecture, DDD)
- ✅ API REST (conception, implémentation, documentation)
- ✅ Base de données relationnelles (SQL, modélisation)
- ✅ Tests automatisés (unitaires, intégration)
- ✅ DevOps (Docker, CI/CD, déploiement cloud)
- ✅ Sécurité applicative (OWASP, RGPD)

**Compétences transversales** :

- ✅ Gestion de projet Agile (Scrum)
- ✅ Documentation technique
- ✅ Veille technologique
- ✅ Résolution de problèmes complexes
- ✅ Autonomie et prise d'initiative
- ✅ Communication écrite (README, ADR, documentation API)

---

## 1.4 Cadre légal et conformité

### 1.4.1 Propriété intellectuelle

**Statut du projet** : Open source (prévu)

**Licence envisagée** : MIT License
- Utilisation libre (commerciale ou non)
- Modification et redistribution autorisées
- Pas de garantie

**Code source** : https://github.com/jimni6/happyrow-core

---

### 1.4.2 Protection des données (RGPD)

**Données personnelles collectées** :
- Emails (identifiant utilisateur)
- Noms des événements et descriptions
- Localisation des événements

**Bases légales** :
- **Consentement** : Création de compte volontaire
- **Intérêt légitime** : Organisation d'événements

**Principes appliqués** :
- ✅ Minimisation : Seules les données nécessaires
- ✅ Transparence : Information claire sur l'utilisation
- ✅ Limitation de la durée : Suppression après inactivité (prévu)
- ✅ Sécurité : Chiffrement en transit (SSL/TLS)

**Droits des utilisateurs** (prévus) :
- Droit d'accès aux données
- Droit de rectification
- Droit à l'effacement ("droit à l'oubli")
- Droit à la portabilité

Voir section 8 pour les détails de mise en œuvre.

---

### 1.4.3 Accessibilité et inclusivité

**RGAA (Référentiel Général d'Amélioration de l'Accessibilité)** :

Bien que l'API backend ne soit pas directement concernée par le RGAA (frontend uniquement), les principes suivants sont anticipés :

- **Messages d'erreur explicites** : Codes et descriptions clairs
- **Structure JSON cohérente** : Facilite l'intégration frontend accessible
- **Documentation complète** : API facilement compréhensible

**Objectif futur** : Frontend conforme RGAA niveau AA.

---

## 1.5 Objectifs professionnels

### 1.5.1 Objectifs pédagogiques

Ce projet vise à démontrer la maîtrise des compétences du référentiel CDA :

**Activité Type 1** : Concevoir et développer des composants
- Développer des composants d'accès aux données (API REST)
- Développer la persistance des données (PostgreSQL, ORM)
- Développer des composants métier (Use Cases)

**Activité Type 2** : Concevoir et développer la persistance
- Concevoir une base de données relationnelle
- Développer des composants d'accès aux données (Repositories)
- Mettre en place une architecture en couches

**Activité Type 3** : Concevoir et développer une application sécurisée
- Préparer le déploiement (Docker, CI/CD)
- Sécuriser les composants (injection SQL, validation)
- Sécuriser les échanges (SSL/TLS, CORS)

**Compétence transversale** : Effectuer une veille technologique

---

### 1.5.2 Objectifs techniques

**Court terme (projet CDA)** :
- ✅ API REST fonctionnelle et sécurisée
- ✅ Architecture Clean (hexagonale)
- ✅ Déploiement automatisé (CI/CD)
- ✅ Tests et qualité de code
- ✅ Documentation complète

**Moyen terme (après CDA)** :
- Frontend React + TypeScript
- Authentification JWT/OAuth2
- Tests automatisés (couverture ≥ 80%)
- Monitoring et observabilité

**Long terme (évolution du projet)** :
- Application mobile (Kotlin Multiplatform)
- Notifications en temps réel (WebSocket)
- Intégration calendrier (Google, iCal)
- Intelligence artificielle (recommandations)

---

### 1.5.3 Objectifs de carrière

**Compétences visées pour le marché** :

- ✅ Maîtrise de Kotlin et de son écosystème
- ✅ Expertise en architecture logicielle moderne
- ✅ Pratique DevOps (CI/CD, containerisation)
- ✅ Sécurité applicative (OWASP, RGPD)
- ✅ Méthodologie Agile en autonomie

**Débouchés professionnels** :
- Développeur Backend Kotlin/Java
- Concepteur Développeur d'Applications
- Ingénieur Logiciel Full Stack
- DevOps Engineer (avec montée en compétences)

**Valeur ajoutée du projet** :
- Projet complet de A à Z en production
- Code source public (portfolio GitHub)
- Démonstration de compétences variées
- Capacité à travailler en autonomie

---

## 1.6 Présentation synthétique

### 1.6.1 Fiche d'identité du projet

| Élément | Valeur |
|---------|--------|
| **Nom du projet** | HappyRow Core |
| **Type** | API REST Backend |
| **Domaine** | Gestion d'événements festifs |
| **Durée** | 6 mois (Août 2025 - Janvier 2026) |
| **Statut** | Projet de formation CDA |
| **Environnement** | Autonomie complète, télétravail |
| **Budget** | 0 € (services gratuits) |
| **Langage principal** | Kotlin 2.2 |
| **Framework** | Ktor 3.2 |
| **Base de données** | PostgreSQL 15 |
| **Hébergement** | Render (PaaS) |
| **Code source** | https://github.com/jimni6/happyrow-core |
| **URL production** | https://happyrow-core.onrender.com |

---

### 1.6.2 Valeur du projet

**Pour les utilisateurs** :
- Simplification de l'organisation d'événements
- Centralisation des informations
- Meilleure coordination entre participants

**Pour le développeur (candidat CDA)** :
- Démonstration de compétences complètes
- Projet portfolio de qualité professionnelle
- Expérience de bout en bout (conception → déploiement)

**Pour la communauté** :
- Projet open source réutilisable
- Documentation technique complète
- Exemple d'architecture moderne en Kotlin

---

## Conclusion de la section 1

Ce projet **HappyRow Core** s'inscrit dans le cadre de l'obtention du titre professionnel CDA. Il répond à un **besoin réel** d'organisation d'événements festifs tout en démontrant la maîtrise de **compétences techniques et professionnelles** variées.

Le contexte de **développement en autonomie complète** a permis de prendre des **décisions architecturales** justifiées, d'appliquer les **bonnes pratiques** de l'industrie (OWASP, RGPD, DevOps) et de mener un projet de **A à Z** jusqu'à la mise en production.

Les sections suivantes détailleront :
- **Section 2** : Présentation détaillée du projet
- **Section 3** : Cahier des charges et spécifications
- **Section 4** : Gestion de projet et méthodologie
- **Sections 5-12** : Aspects techniques, sécurité, tests, veille
