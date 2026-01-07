# TODO - AMÉLIORATIONS ET TÂCHES RESTANTES

**Projet** : HappyRow Core  
**Date** : 5 janvier 2026  
**Statut** : Dossier CDA finalisé, améliorations post-CDA

---

## 🎯 Priorité HAUTE (Avant soutenance CDA)

### ✅ Dossier professionnel
- [x] Créer les 12 sections du dossier
- [x] Table des matières complète
- [x] README et guide de finalisation
- [ ] **Personnaliser avec vos informations**
  - [ ] Remplacer `[Votre Nom]` dans tous les fichiers
  - [ ] Ajouter vos coordonnées (email, LinkedIn, GitHub)
  - [ ] Personnaliser les remerciements (section 12)
  - [ ] Créer une page de garde avec photo

### ✅ Préparation soutenance
- [ ] **Créer les slides de présentation**
  - [ ] 15-20 slides maximum
  - [ ] Introduction (2 min)
  - [ ] Architecture (5 min)
  - [ ] Réalisations techniques (8 min)
  - [ ] Sécurité et qualité (3 min)
  - [ ] Conclusion (2 min)
  
- [ ] **Préparer la démonstration**
  - [ ] Collection Postman complète et testée
  - [ ] Scénarios de démonstration prêts
  - [ ] Tests négatifs (erreurs) préparés
  - [ ] Vérifier que l'application est accessible
  
- [ ] **Anticiper les questions**
  - [ ] Pourquoi Kotlin vs Java/Spring ?
  - [ ] Comment garantir la sécurité ?
  - [ ] Difficultés rencontrées ?
  - [ ] Évolutions prévues ?
  - [ ] Choix d'architecture (hexagonale) ?

### 📄 Documentation
- [ ] **Relire et corriger le dossier**
  - [ ] Vérifier la cohérence entre sections
  - [ ] Corriger orthographe/grammaire
  - [ ] Vérifier les numéros de page
  - [ ] S'assurer que tous les liens fonctionnent
  
- [ ] **Préparer les annexes**
  - [ ] Sélectionner le code source à inclure
  - [ ] Générer rapports JaCoCo (`./gradlew test jacocoTestReport`)
  - [ ] Exporter collection Postman en JSON
  - [ ] Créer diagrammes haute résolution (PNG/SVG)
  - [ ] Compiler le glossaire
  
- [ ] **Export final**
  - [ ] Convertir en PDF
  - [ ] Ajouter en-têtes/pieds de page
  - [ ] Créer table des matières automatique
  - [ ] Vérifier la qualité des images
  - [ ] Nommer : `NOM_Prenom_CDA_2026.pdf`

---

## 🔴 Priorité HAUTE (Post-CDA, Court terme Q1-Q2 2025)

### 🧪 Tests automatisés
**Statut** : Partiellement implémenté (objectif 80% non atteint)

- [ ] **Tests unitaires (Use Cases)**
  - [ ] `CreateEventUseCaseTest` (exemple en section 9)
  - [ ] `UpdateEventUseCaseTest`
  - [ ] `DeleteEventUseCaseTest`
  - [ ] `GetEventsByOrganizerUseCaseTest`
  - [ ] Idem pour Participants, Resources, Contributions
  - [ ] Objectif : 70% de couverture Domain
  
- [ ] **Tests d'intégration (Endpoints + DB)**
  - [ ] `CreateEventEndpointIntegrationTest` (exemple en section 9)
  - [ ] Tests pour tous les endpoints (12 endpoints)
  - [ ] Utiliser Testcontainers PostgreSQL
  - [ ] Objectif : 20% de couverture Infrastructure
  
- [ ] **Tests de sécurité**
  - [ ] Tests d'injection SQL automatisés
  - [ ] Tests CORS (origines autorisées/refusées)
  - [ ] Tests validation (données invalides)
  - [ ] Tests rate limiting (futur)
  
- [ ] **Configuration JaCoCo**
  - [ ] Vérifier configuration dans `build.gradle.kts`
  - [ ] Générer rapports HTML + XML
  - [ ] Intégrer dans CI/CD (GitHub Actions)
  - [ ] Ajouter badge de couverture dans README

**Fichiers à créer** :
```
domain/src/test/kotlin/
  com/happyrow/core/domain/event/
    create/CreateEventUseCaseTest.kt
    get/GetEventsByOrganizerUseCaseTest.kt
    update/UpdateEventUseCaseTest.kt
    delete/DeleteEventUseCaseTest.kt

infrastructure/src/test/kotlin/
  com/happyrow/core/infrastructure/event/
    create/CreateEventEndpointIntegrationTest.kt
    get/GetEventsEndpointIntegrationTest.kt
    ...
```

---

### 🔐 Authentification robuste
**Statut** : ✅ JWT implémenté avec Supabase

- [x] **Implémenter JWT**
  - [x] Ajout dépendance JWT (auth0/java-jwt)
  - [x] Service `SupabaseJwtService` pour validation tokens
  - [x] Validation avec signature HMAC256
  - [x] Extraction userId et email depuis token
  - [x] Plugin Ktor personnalisé `JwtAuthenticationPlugin`
  - [ ] Endpoints `POST /auth/login` et `/auth/register` → Géré par Supabase
  
- [x] **Intégration Supabase Auth**
  - [x] Configuration depuis variables d'environnement
  - [x] Validation issuer et audience
  - [x] Gestion des erreurs de validation
  - [x] Extraction sécurisée des claims
  
- [ ] **Gestion des rôles** (Phase 2)
  - [ ] Enum `UserRole` (ORGANIZER, PARTICIPANT, ADMIN)
  - [ ] Table `user_roles`
  - [ ] Middleware pour vérifier les rôles
  - [ ] Permissions granulaires
  
- [ ] **Tokens de rafraîchissement** (Phase 2)
  - [ ] Refresh tokens stockés en DB
  - [ ] Endpoint `POST /auth/refresh`
  - [ ] Expiration automatique
  - [ ] Révocation des tokens (blacklist)

**Estimation phase 2** : 1-2 semaines

---

### 📊 Monitoring et observabilité
**Statut** : Logs basiques uniquement

- [ ] **OpenTelemetry**
  - [ ] Ajouter dépendance OpenTelemetry Kotlin
  - [ ] Configurer instrumentation automatique
  - [ ] Exporter vers backend (Jaeger local ou cloud)
  
- [ ] **Métriques**
  - [ ] Micrometer pour Ktor
  - [ ] Métriques custom (temps de réponse, erreurs)
  - [ ] Endpoint `/metrics` (Prometheus format)
  
- [ ] **Logs structurés**
  - [ ] Migration vers logs JSON (Logstash format)
  - [ ] Ajout de trace IDs
  - [ ] Niveaux de logs appropriés
  
- [ ] **Dashboards**
  - [ ] Grafana (self-hosted ou cloud)
  - [ ] Dashboard temps de réponse
  - [ ] Dashboard erreurs
  - [ ] Dashboard base de données
  
- [ ] **Alertes**
  - [ ] Alertes sur erreurs 5xx (email/Slack)
  - [ ] Alertes sur temps de réponse > 500ms
  - [ ] Alertes sur uptime < 95%

**Estimation** : 1-2 semaines

---

## 🟠 Priorité MOYENNE (Q3-Q4 2025)

### 🌐 Frontend React
**Statut** : Non implémenté

- [ ] **Setup projet**
  - [ ] Create React App avec TypeScript
  - [ ] Configuration TailwindCSS
  - [ ] Configuration ESLint + Prettier
  - [ ] Routing (React Router)
  
- [ ] **Pages principales**
  - [ ] Page d'accueil
  - [ ] Page de connexion/inscription
  - [ ] Dashboard (liste des événements)
  - [ ] Création d'événement (formulaire)
  - [ ] Détail d'événement
  - [ ] Gestion des participants
  - [ ] Gestion des ressources/contributions
  
- [ ] **Composants**
  - [ ] EventCard
  - [ ] EventForm
  - [ ] ParticipantList
  - [ ] ResourceList
  - [ ] Navigation
  - [ ] Modals (confirmations)
  
- [ ] **API Client**
  - [ ] Axios ou Fetch configuré
  - [ ] Gestion des tokens JWT
  - [ ] Gestion des erreurs
  - [ ] Types TypeScript générés depuis API
  
- [ ] **Déploiement**
  - [ ] Vercel (gratuit)
  - [ ] CI/CD GitHub Actions
  - [ ] Variables d'environnement

**Estimation** : 4-6 semaines

---

### 📧 Notifications et emails
**Statut** : Non implémenté

- [ ] **Système d'envoi d'emails**
  - [ ] Service (SendGrid, Mailgun, ou AWS SES)
  - [ ] Templates HTML d'emails
  - [ ] Email d'invitation à un événement
  - [ ] Email de confirmation de participation
  - [ ] Email de rappel (J-3, J-1)
  
- [ ] **Notifications push (optionnel)**
  - [ ] Firebase Cloud Messaging
  - [ ] Notifications navigateur (Web Push API)
  
- [ ] **Préférences utilisateur**
  - [ ] Opt-in/opt-out notifications
  - [ ] Fréquence des notifications

**Estimation** : 1-2 semaines

---

### 🚀 Optimisations performance
**Statut** : Performance OK mais améliorable

- [ ] **Cache Redis**
  - [ ] Setup Redis (local + production)
  - [ ] Cache des événements par organisateur
  - [ ] Cache des participants par événement
  - [ ] Invalidation cache (write-through)
  
- [ ] **Optimisations base de données**
  - [ ] Index sur colonnes fréquemment recherchées
    - [ ] `event.creator`
    - [ ] `participant.event_id`
    - [ ] `resource.event_id`
  - [ ] Requêtes N+1 : utiliser joins
  - [ ] Connection pooling (déjà fait avec HikariCP)
  
- [ ] **Pagination**
  - [ ] Pagination des événements (limit/offset)
  - [ ] Pagination des participants
  - [ ] Cursor-based pagination (futur)
  
- [ ] **Compression**
  - [ ] Gzip pour les réponses JSON
  - [ ] Configuration Ktor Compression plugin

**Estimation** : 1 semaine

---

### 🔒 Sécurité avancée
**Statut** : Sécurité de base OK, améliorations possibles

- [ ] **Rate limiting**
  - [ ] Ktor Rate Limit plugin
  - [ ] Limite par IP : 100 req/min
  - [ ] Limite par utilisateur : 1000 req/heure
  
- [ ] **CSRF Protection**
  - [ ] Tokens CSRF pour forms
  - [ ] Double submit cookies
  
- [ ] **Content Security Policy**
  - [ ] Header CSP configuré
  - [ ] Uniquement les origines de confiance
  
- [ ] **Audit logs**
  - [ ] Journalisation des actions sensibles
  - [ ] Qui a créé/modifié/supprimé quoi et quand
  
- [ ] **Chiffrement au repos**
  - [ ] Chiffrement des emails en base (AES-256)
  - [ ] Gestion des clés (AWS KMS ou HashiCorp Vault)

**Estimation** : 1-2 semaines

---

## 🟢 Priorité BASSE (2026+)

### 📱 Application mobile
**Statut** : Non prévu à court terme

- [ ] **Kotlin Multiplatform Mobile (KMP)**
  - [ ] Setup projet KMP
  - [ ] Partage du code Domain
  - [ ] Partage des DTOs et validation
  - [ ] UI native iOS (SwiftUI) + Android (Compose)
  
- [ ] **Fonctionnalités mobile**
  - [ ] Notifications push natives
  - [ ] Géolocalisation (événements à proximité)
  - [ ] Mode offline
  - [ ] Appareil photo (QR codes)

**Estimation** : 2-3 mois

---

### 🔗 Intégrations tierces
**Statut** : Non implémenté

- [ ] **Calendrier**
  - [ ] Export iCal (.ics)
  - [ ] Intégration Google Calendar API
  - [ ] Intégration Outlook Calendar
  
- [ ] **Réseaux sociaux**
  - [ ] Partage événement sur Facebook
  - [ ] Partage événement sur Twitter/X
  - [ ] Partage événement sur WhatsApp
  
- [ ] **Paiements (si gestion financière)**
  - [ ] Stripe pour les paiements
  - [ ] PayPal
  - [ ] Remboursements automatiques

**Estimation** : 3-4 semaines

---

### 🤖 Intelligence artificielle
**Statut** : Idée prospective

- [ ] **Recommandations**
  - [ ] Suggérer des participants basés sur l'historique
  - [ ] Suggérer des ressources basées sur le type d'événement
  - [ ] Suggérer des lieux populaires
  
- [ ] **Analyse prédictive**
  - [ ] Prédire le taux de participation
  - [ ] Estimer les besoins en ressources
  
- [ ] **Assistant conversationnel**
  - [ ] Chatbot pour créer des événements
  - [ ] Intégration GPT API

**Estimation** : 1-2 mois

---

## 📦 Infrastructure et DevOps

### 🐳 Améliorations Docker
- [ ] **Multi-stage build optimisé**
  - [x] Déjà implémenté (180 MB)
  - [ ] Réduire davantage avec distroless image
  
- [ ] **Docker Compose pour production**
  - [ ] Service backend
  - [ ] Service PostgreSQL
  - [ ] Service Redis (futur)
  - [ ] Service Nginx (reverse proxy)

---

### ☸️ Kubernetes (si scalabilité nécessaire)
- [ ] **Manifests Kubernetes**
  - [ ] Deployment backend (replicas: 3)
  - [ ] Service backend (LoadBalancer)
  - [ ] StatefulSet PostgreSQL
  - [ ] ConfigMaps et Secrets
  - [ ] Ingress (HTTPS)
  
- [ ] **Helm Charts**
  - [ ] Chart HappyRow Core
  - [ ] Values pour dev/staging/prod
  
- [ ] **Auto-scaling**
  - [ ] Horizontal Pod Autoscaler (HPA)
  - [ ] Metrics Server

**Estimation** : 2-3 semaines

---

### 🔄 CI/CD amélioré
- [ ] **Tests de charge automatisés**
  - [ ] Scripts K6 ou Gatling
  - [ ] Exécution dans CI/CD
  - [ ] Rapports de performance
  
- [ ] **Scan de sécurité automatique**
  - [ ] Snyk dans GitHub Actions
  - [ ] Trivy pour scan images Docker
  - [ ] Dependabot pour mises à jour dépendances
  
- [ ] **Déploiement multi-environnements**
  - [ ] Environnement staging (test)
  - [ ] Environnement production
  - [ ] Blue-green deployment

---

## 📚 Documentation

### 📖 Documentation API
- [ ] **OpenAPI/Swagger**
  - [ ] Générer spec OpenAPI 3.0
  - [ ] Plugin Ktor OpenAPI
  - [ ] Interface Swagger UI
  - [ ] Héberger sur `/docs`
  
- [ ] **Postman/Insomnia**
  - [x] Collection Postman basique
  - [ ] Collection complète (12 endpoints)
  - [ ] Tests automatisés dans Postman
  - [ ] Publication sur Postman Public API Network

---

### 📝 Documentation développeur
- [ ] **CONTRIBUTING.md**
  - [ ] Guide pour contribuer
  - [ ] Conventions de code
  - [ ] Processus de pull request
  
- [ ] **ARCHITECTURE.md**
  - [ ] Explication détaillée de l'architecture
  - [ ] Diagrammes actualisés
  - [ ] ADR (déjà en Notion, migrer vers repo)
  
- [ ] **Guides**
  - [ ] Guide de déploiement local
  - [ ] Guide de déploiement production
  - [ ] Guide de troubleshooting

---

## 🎨 Améliorations UX/UI (Frontend)

### 🌈 Design System
- [ ] **Composants UI**
  - [ ] Bibliothèque de composants (Storybook)
  - [ ] shadcn/ui ou Material-UI
  - [ ] Thème personnalisé HappyRow
  
- [ ] **Responsive design**
  - [ ] Mobile-first
  - [ ] Tablette
  - [ ] Desktop
  
- [ ] **Accessibilité (RGAA)**
  - [ ] Navigation au clavier
  - [ ] Support lecteurs d'écran
  - [ ] Contraste suffisant
  - [ ] Labels appropriés

---

## 🌍 Internationalisation
- [ ] **i18n**
  - [ ] Backend : messages d'erreur multilingues
  - [ ] Frontend : React i18next
  - [ ] Langues : FR, EN (minimum)
  - [ ] Détection automatique de la langue

---

## 📊 Analytics
- [ ] **Suivi utilisateur**
  - [ ] Google Analytics ou Plausible
  - [ ] Événements (création, participation, etc.)
  - [ ] Funnels de conversion
  
- [ ] **Métriques métier**
  - [ ] Nombre d'événements créés
  - [ ] Taux de participation
  - [ ] Ressources les plus populaires
  - [ ] Dashboard admin

---

## 🧹 Refactoring et dette technique

### 🔧 Code improvements
- [ ] **Réduire duplication**
  - [ ] Extraire code commun Repositories
  - [ ] Extraire code commun Endpoints
  - [ ] Utiliser génériques où possible
  
- [ ] **Améliorer gestion erreurs**
  - [ ] Hiérarchie d'exceptions plus claire
  - [ ] Messages d'erreur plus explicites
  - [ ] Codes d'erreur standardisés
  
- [ ] **Améliorer validation**
  - [ ] Bibliothèque de validation (Konform)
  - [ ] Validation centralisée
  - [ ] Messages de validation clairs

---

## ✅ Checklist pré-production

Avant de passer en production réelle (au-delà du MVP) :

- [ ] ✅ Tests : Couverture ≥ 80%
- [ ] ✅ Sécurité : 0 vulnérabilité critique
- [ ] ✅ Performance : p95 < 200ms
- [ ] ✅ Documentation : API complète + guides
- [ ] ✅ Monitoring : Métriques + alertes
- [ ] ✅ Backups : Automatiques quotidiens
- [ ] ✅ SSL/TLS : Certificats valides
- [ ] ✅ RGPD : Conformité complète
- [ ] ✅ Authentification : JWT implémenté
- [ ] ✅ Rate limiting : Protection DoS
- [ ] ✅ Logs : Structurés et centralisés
- [ ] ✅ CI/CD : Pipeline stable
- [ ] ✅ Rollback : Procédure documentée
- [ ] ✅ Support : Contact/FAQ
- [ ] ✅ Legal : CGU + Politique de confidentialité

---

## 💰 Budget prévisionnel (post-MVP)

| Service | Plan | Coût mensuel |
|---------|------|--------------|
| **Render Web Service** | Starter | 7 $ |
| **Render PostgreSQL** | Standard | 7 $ |
| **Redis** (Upstash) | Free/Paid | 0-10 $ |
| **SendGrid** (emails) | Free (100/jour) | 0 $ |
| **Vercel** (frontend) | Free | 0 $ |
| **Sentry** (monitoring) | Free | 0 $ |
| **Domaine** (.com) | Annuel | ~1 $/mois |
| **Total** | | **15-25 $/mois** |

Pour production à grande échelle :
- Render Professional : 25 $/service
- PostgreSQL Production : 50 $
- Redis : 10-50 $
- CDN : 5-20 $
- Total : **100-150 $/mois**

---

## 📅 Roadmap suggérée

### Version 1.0 (Actuelle - MVP CDA)
- ✅ API REST CRUD complète
- ✅ Architecture hexagonale
- ✅ Sécurité de base (OWASP)
- ✅ CI/CD automatisé
- ✅ Déploiement Render

### Version 1.5 (Q2 2025 - Post-CDA)
- ✅ Tests automatisés (80% couverture)
- ✅ Authentification JWT
- ✅ Monitoring (OpenTelemetry + Grafana)
- ✅ Documentation API (OpenAPI)

### Version 2.0 (Q3-Q4 2025)
- ✅ Frontend React + TypeScript
- ✅ Notifications emails
- ✅ Optimisations (Redis, indexes)
- ✅ Rate limiting et sécurité avancée

### Version 3.0 (2026)
- ✅ Application mobile (KMP)
- ✅ Intégrations (Calendar, réseaux sociaux)
- ✅ Intelligence artificielle (recommandations)
- ✅ Kubernetes pour scalabilité

---

## 🎯 Conclusion

**Statut actuel** : ✅ MVP fonctionnel et dossier CDA complet

**Priorités immédiates** :
1. Finaliser le dossier (personnalisation, relecture, export PDF)
2. Préparer la soutenance (slides, démo, questions)
3. Implémenter les tests automatisés (post-CDA)
4. Ajouter l'authentification JWT (post-CDA)

**Vision long terme** : Application complète (backend + frontend + mobile) avec fonctionnalités avancées et scalabilité.

Le projet HappyRow Core a une base solide et peut évoluer progressivement vers une application de production complète ! 🚀
