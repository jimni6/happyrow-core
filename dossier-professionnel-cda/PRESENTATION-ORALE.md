# PRÉSENTATION ORALE - SOUTENANCE CDA
## HappyRow Core - API Backend de gestion d'événements

**Durée totale** : 20 minutes  
**Candidat** : [Votre Nom]  
**Date** : [Date de soutenance]

---

## 🎯 PARTIE 1 : INTRODUCTION (2 minutes)

### Slide 1 : Page de garde

**[Pause - regarder le jury]**

Bonjour Mesdames et Messieurs les membres du jury.

Je m'appelle [Votre Nom], et je suis candidat au titre de Concepteur Développeur d'Applications.

Aujourd'hui, je vais vous présenter mon projet HappyRow Core, une API REST backend pour la gestion d'événements festifs.

**[Transition naturelle]**

### Slide 2 : Contexte et problématique

Ce projet répond à une problématique concrète : comment simplifier l'organisation d'événements entre amis ou en famille ?

Actuellement, les organisateurs utilisent des solutions dispersées : des groupes WhatsApp pour communiquer, des tableurs Excel pour suivre les contributions, et des rappels manuels pour chaque participant.

Cette dispersion entraîne des oublis, des doublons, et une charge mentale importante pour l'organisateur.

**Ma solution** : une API centralisée qui permet de créer des événements, d'inviter des participants, de définir des ressources nécessaires, et de suivre les contributions de chacun.

**[Montrer schéma du contexte]**

Le projet s'inscrit dans le cadre de ma validation du titre CDA, et démontre les compétences des trois activités types exigées.

---

## 🏗️ PARTIE 2 : ARCHITECTURE ET CONCEPTION (5 minutes)

### Slide 3 : Architecture hexagonale

**[Pointer le diagramme]**

J'ai choisi d'implémenter une **architecture hexagonale**, aussi appelée ports et adaptateurs.

Cette architecture présente trois couches distinctes :

**Le Domain** au centre : il contient la logique métier pure, indépendante de toute technologie. Par exemple, mes entités Event, Participant, Resource, et mes Use Cases comme CreateEventUseCase.

**L'Infrastructure** autour : elle contient les adaptateurs techniques. À gauche, les adaptateurs entrants comme mes endpoints REST Ktor. À droite, les adaptateurs sortants comme mes repositories PostgreSQL.

**Les ports** qui définissent les contrats : ce sont des interfaces Kotlin qui permettent l'inversion de dépendances.

**[Geste circulaire sur le schéma]**

Cette architecture garantit :
- L'isolation du métier
- La testabilité : je peux tester le domain sans base de données
- L'évolutivité : je peux changer de framework ou de base de données sans modifier le métier

**Ceci couvre la compétence CDA-2.1 : concevoir une application organisée en couches.**

### Slide 4 : Stack technique

**[Assurance dans la voix]**

Pour implémenter cette architecture, j'ai sélectionné un stack technique moderne et performant :

**Côté langage** : Kotlin 2.2.0, pour sa concision, sa null-safety, et son interopérabilité avec l'écosystème Java.

**Framework web** : Ktor 3.2.2, un framework asynchrone et léger développé par JetBrains. J'ai préféré Ktor à Spring Boot pour sa simplicité et ses performances supérieures dans mon cas d'usage.

**ORM** : Exposed, également de JetBrains, qui offre un DSL Kotlin type-safe pour les requêtes SQL.

**Base de données** : PostgreSQL 15, avec HikariCP pour le connection pooling.

**Programmation fonctionnelle** : Arrow pour la gestion des erreurs avec Either, évitant les exceptions et rendant le code plus prévisible.

**Injection de dépendances** : Koin, léger et idiomatique en Kotlin.

**[Transition]**

Tous ces choix sont justifiés dans mon dossier, section 6 : Spécifications techniques.

### Slide 5 : Modèle de données

**[Pointer le diagramme]**

Mon modèle de données s'articule autour de quatre entités principales :

**Event** : représente l'événement avec son nom, sa description, sa date, son lieu, et son type.

**Participant** : représente une personne invitée, avec son nom, email, et son statut de participation.

**Resource** : représente les besoins de l'événement, comme "bouteilles de vin" ou "desserts". Chaque ressource a une quantité attendue et une unité.

**Contribution** : fait le lien entre un participant et une ressource. Elle indique qu'un participant s'engage à apporter une certaine quantité d'une ressource.

**[Tracer les relations avec la main]**

Les relations sont :
- Un événement a plusieurs participants et plusieurs ressources
- Un participant peut avoir plusieurs contributions
- Une contribution lie un participant à une ressource spécifique

Ce modèle couvre les besoins fonctionnels du MVP tout en permettant des évolutions futures.

---

## 💻 PARTIE 3 : RÉALISATIONS TECHNIQUES (8 minutes)

### Slide 6 : Endpoints REST

**[Ton plus technique]**

J'ai développé une API REST complète avec 15 endpoints organisés en 4 groupes de ressources :

**Events** :
- POST /events : créer un événement
- GET /events : lister par organisateur
- PUT /events/{id} : mettre à jour
- DELETE /events/{id} : supprimer

**Participants**, **Resources**, et **Contributions** suivent la même logique CRUD.

**[Montrer exemple]**

Tous les endpoints respectent les conventions REST :
- Utilisation correcte des verbes HTTP
- Codes de statut appropriés : 200, 201, 400, 404, 500
- Validation des entrées avec messages d'erreur explicites

**Ceci illustre la compétence CDA-1.1 : développer des composants d'accès aux données.**

### Slide 7 : Use Cases métier

**[Important - bien articuler]**

La logique métier est encapsulée dans des Use Cases, suivant le principe de responsabilité unique.

**Par exemple, CreateEventUseCase** :

Il reçoit une requête de création, effectue les validations métier comme :
- Le nom de l'événement ne doit pas dépasser 256 caractères
- La date doit être dans le futur
- Le type doit être valide : PARTY, BIRTHDAY, DINER, ou SNACK

Puis il délègue la persistance au repository via le port EventRepository.

**[Geste de séparation]**

Cette séparation garantit que ma logique métier est testable indépendamment de l'infrastructure.

J'utilise Arrow Either pour gérer les erreurs de manière fonctionnelle, sans exception. Un Either contient soit un succès (Right), soit une erreur (Left).

**Ceci démontre CDA-1.2 : développer des composants métier.**

### Slide 8 : Repositories et persistance

**[Pointer le code]**

Pour la persistance, j'ai implémenté le pattern Repository avec deux parties :

**L'interface** dans le domain : EventRepository définit les opérations métier comme save, findById, findByOrganizer, delete.

**L'implémentation** dans l'infrastructure : SqlEventRepository utilise Exposed ORM pour traduire ces opérations en requêtes SQL.

**[Exemple concret]**

Exposed me permet d'écrire des requêtes type-safe en Kotlin. Par exemple :

```kotlin
EventTable.select { EventTable.id eq eventId }
  .map { it.toEvent() }
  .firstOrNull()
```

Le compilateur vérifie que les colonnes existent, évitant les erreurs à l'exécution.

J'utilise des transactions pour garantir la cohérence des données, et HikariCP pour optimiser les performances avec un pool de connexions.

**Ceci couvre CDA-1.3 et CDA-2.3 : développer la persistance des données.**

### Slide 9 : Authentification JWT avec Supabase

**[Nouveauté - mettre en avant]**

Un point crucial de mon projet : l'authentification JWT que j'ai récemment implémentée.

J'ai intégré Supabase Auth, un service d'authentification professionnel, pour gérer les utilisateurs et les tokens.

**Mon implémentation comprend** :

Un service SupabaseJwtService qui valide les tokens JWT reçus dans le header Authorization.

La validation vérifie :
- La signature cryptographique avec l'algorithme HMAC256
- L'issuer : l'URL de mon instance Supabase
- L'audience : "authenticated"

**[Montrer le flow]**

Quand une requête arrive :
1. Mon plugin Ktor intercepte la requête
2. Il extrait le token du header "Bearer {token}"
3. Le service valide le token
4. Si valide, il extrait l'userId et l'email du payload
5. Ces informations sont disponibles dans tous mes endpoints

Les secrets comme le JWT secret sont stockés dans des variables d'environnement, jamais dans le code.

**Cette authentification renforce significativement la sécurité de mon application et démontre CDA-3.2 : sécuriser l'accès aux données.**

### Slide 10 : Démonstration API

**[Si démo en direct]**

Permettez-moi de vous montrer rapidement l'API en action avec Postman.

**[Ouvrir Postman]**

Je vais créer un événement :
- Je fournis un token JWT valide dans le header Authorization
- Je POST un JSON avec les informations de l'événement
- **[Exécuter]** L'API me retourne un 201 Created avec l'événement créé et son ID

Maintenant je récupère cet événement :
- **[Exécuter GET]** Je reçois tous les détails de l'événement

Et si j'essaie sans token ?
- **[Retirer le token et exécuter]** Je reçois une erreur 401 Unauthorized

**[Revenir aux slides]**

Cette démo montre que l'API est fonctionnelle et sécurisée.

---

## 🔒 PARTIE 4 : SÉCURITÉ ET QUALITÉ (3 minutes)

### Slide 11 : Sécurité - OWASP Top 10

**[Sérieux]**

La sécurité est au cœur de mon projet. J'ai analysé les 10 vulnérabilités les plus critiques selon l'OWASP 2021.

**Voici les mesures implémentées** :

**A01 - Contrôle d'accès défaillant** :
- ✅ Authentification JWT obligatoire
- ✅ Validation de l'organisateur sur chaque ressource

**A02 - Défaillances cryptographiques** :
- ✅ SSL/TLS obligatoire en production
- ✅ Secrets en variables d'environnement
- ✅ Pas de données sensibles en base

**A03 - Injection** :
- ✅ Protection à 100% grâce à Exposed ORM
- ✅ Requêtes paramétrées automatiquement
- ✅ Aucune concaténation SQL manuelle

**A05 - Mauvaise configuration** :
- ✅ CORS configuré avec liste blanche
- ✅ En-têtes de sécurité appropriés
- ✅ Pas d'informations sensibles dans les erreurs

**A07 - Authentification défaillante** :
- ✅ JWT avec signature cryptographique
- ✅ Validation issuer/audience
- ✅ Tokens expirables

**[Pause]**

Sur les 10 vulnérabilités OWASP, j'en traite 8. Les 2 restantes (A04, A06) concernent des aspects non applicables à mon MVP.

**Ceci démontre CDA-3.3 : sécuriser les données lors des échanges.**

### Slide 12 : Qualité du code

**[Montrer les métriques]**

Pour garantir la qualité du code, j'ai mis en place plusieurs outils :

**Detekt** : analyseur statique de code Kotlin
- Résultat : **0 issue**
- Vérifie la complexité, les conventions de nommage, les code smells

**Spotless** avec KtLint : formatage automatique
- Code uniformément formaté
- Intégré au build Gradle

**Tests** : J'ai implémenté 7 scénarios de test documentés dans mon dossier :
- Cas nominal : création d'événement réussie
- 6 cas d'erreur : validation, contraintes, sécurité
- **Taux de réussite** : 85% (6/7)
- **Performances** : 100% des requêtes sous 200ms

**Couverture de code** : Objectif ≥ 80% avec JaCoCo

**[Geste d'assurance]**

Ces métriques prouvent que mon code est maintenable, lisible, et robuste.

### Slide 13 : DevOps et déploiement

**[Montrer le pipeline]**

J'ai automatisé le déploiement avec un pipeline CI/CD sur GitHub Actions.

**Le workflow** :

1. **Detekt** : Analyse statique du code
2. **Tests** : Exécution des tests unitaires et d'intégration
3. **Build** : Compilation avec Gradle
4. **Docker** : Construction de l'image Docker
5. **Deploy** : Déploiement automatique sur Render

**[Précision importante]**

À chaque push sur la branche main, le pipeline se déclenche automatiquement. En 5 minutes environ, mon application est déployée en production.

L'infrastructure est définie en code avec :
- Un Dockerfile pour la conteneurisation
- Un render.yaml pour la configuration Render
- Des variables d'environnement pour les secrets

L'application est accessible publiquement à l'adresse happyrow-core.onrender.com

**Ceci illustre CDA-3.1 : préparer le déploiement d'une application sécurisée.**

---

## 🎓 PARTIE 5 : CONCLUSION ET PERSPECTIVES (2 minutes)

### Slide 14 : Bilan personnel

**[Ton réflexif]**

Ce projet m'a permis de développer de nombreuses compétences techniques et méthodologiques.

**Les points forts** que je retiens :

**Techniquement** :
- Maîtrise de Kotlin et de l'écosystème moderne
- Compréhension approfondie de l'architecture hexagonale
- Implémentation complète de la sécurité avec JWT

**Méthodologiquement** :
- Gestion de projet en autonomie
- Documentation rigoureuse
- Démarche qualité avec outils automatisés

**Les difficultés rencontrées** :

Le principal défi a été l'intégration de l'authentification JWT. J'ai dû comprendre le fonctionnement des tokens, de la validation cryptographique, et créer un plugin Ktor personnalisé.

J'ai également travaillé sur la configuration Detekt/Spotless pour éviter les conflits entre les règles de formatage.

**[Sourire]**

Ces difficultés m'ont permis d'approfondir mes connaissances et de produire une solution robuste.

### Slide 15 : Perspectives d'évolution

**[Regarder vers l'avenir]**

Le projet ne s'arrête pas là. Voici mes perspectives d'évolution à court et moyen terme :

**Phase 2 - Court terme** :
- Gestion des rôles : ORGANIZER, PARTICIPANT, ADMIN
- Refresh tokens pour améliorer l'expérience utilisateur
- Tests automatisés avec couverture ≥ 80%
- Monitoring avec OpenTelemetry

**Phase 3 - Moyen terme** :
- Frontend React + TypeScript pour l'interface utilisateur
- Notifications par email lors des changements d'événements
- Application mobile en Kotlin Multiplatform

**Vision long terme** :
- Intégration calendrier (Google Calendar, iCal)
- Gestion financière (type Tricount)
- Intelligence artificielle pour recommander des ressources

**[Pause]**

Ce projet constitue une base solide pour construire une solution complète d'organisation d'événements.

### Slide 16 : Remerciements et questions

**[Ton formel et respectueux]**

Je tiens à remercier :
- Mon organisme de formation pour l'accompagnement
- Les mainteneurs des projets open source utilisés
- Et vous, membres du jury, pour votre attention et votre écoute

**[Regarder le jury]**

Toute la documentation technique, le code source, et l'application déployée sont accessibles :
- GitHub : github.com/jimni6/happyrow-core
- Application : happyrow-core.onrender.com
- Dossier professionnel complet : 87 pages + annexes

**[Pause - respiration]**

Je suis maintenant à votre disposition pour répondre à vos questions.

**[S'asseoir et attendre]**

---

## 📝 CONSEILS POUR L'APPRENTISSAGE

### Technique de mémorisation

1. **Apprendre par sections** : Maîtriser une partie à la fois (introduction, puis architecture, etc.)

2. **Répéter à voix haute** : Pratiquer devant un miroir ou enregistrer

3. **Adapter, ne pas réciter** : Comprendre le sens, pas seulement les mots

4. **Points d'ancrage** : Mémoriser les transitions et mots-clés, le reste viendra naturellement

5. **Timing** : S'entraîner avec un chronomètre pour respecter les 20 minutes

### Gestion du stress

- **Respiration** : Inspirer profondément avant de commencer
- **Débit** : Parler lentement et articuler
- **Pauses** : Ne pas avoir peur du silence, cela donne du poids
- **Regard** : Balayer le jury, ne pas fixer un point
- **Posture** : Se tenir droit, mains visibles, gestes mesurés

### Questions probables du jury

**Sur les choix techniques** :
- "Pourquoi Ktor plutôt que Spring Boot ?"
- "Comment gérez-vous les transactions ?"

**Sur la sécurité** :
- "Que se passe-t-il si un token est volé ?"
- "Comment gérez-vous l'expiration des tokens ?"

**Sur le code** :
- "Pouvez-vous m'expliquer ce Use Case ?"
- "Comment testez-vous votre code ?"

**Sur le projet** :
- "Quelles difficultés avez-vous rencontrées ?"
- "Comment évolueriez-vous ce projet ?"

**Préparer des réponses concises (30-60 secondes) pour chaque question probable.**

---

## 🎯 CHECKLIST JOUR J

**La veille** :
- [ ] Relire la présentation 2-3 fois
- [ ] Vérifier que l'API est déployée et fonctionne
- [ ] Préparer Postman avec les requêtes de démo
- [ ] Dormir suffisamment

**Le jour J** :
- [ ] Arriver 15 minutes en avance
- [ ] Tester le matériel (vidéoprojecteur, son)
- [ ] Avoir le code source accessible sur laptop
- [ ] Eau à portée de main
- [ ] Sourire et confiance !

---

**Bonne chance pour votre soutenance ! 🎓**
