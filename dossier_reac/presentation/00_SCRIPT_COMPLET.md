# Script de présentation orale — HappyRow (40 min / 50 slides)

> **Consignes** : Ce script est un guide. Ne le lisez pas mot à mot. Appropriez-vous les idées et reformulez avec vos propres mots. Les timings sont indicatifs — adaptez le rythme en fonction de votre aisance.

---

## PARTIE 0 — INTRODUCTION (slides 1-3, ~2 min)

### Slide 1 — Page titre (30s)

> *Afficher la slide titre*

Bonjour, je m'appelle *[Prénom Nom]* et je vous présente aujourd'hui mon projet **HappyRow**, réalisé dans le cadre du titre professionnel Concepteur Développeur d'Applications.

HappyRow est une plateforme collaborative de gestion d'événements, développée en Kotlin avec Ktor pour le back-end, et React TypeScript pour le front-end.

---

### Slide 2 — Sommaire (30s)

> *Afficher le plan*

Ma présentation est organisée en 13 parties. Je commencerai par le contexte et le cahier des charges, puis la gestion de projet. Ensuite, les spécifications fonctionnelles — architecture, maquettes, modèle de données. Puis les réalisations concrètes, organisées par couche : interfaces utilisateur, composants métier, accès aux données, et autres composants. Je poursuivrai avec la sécurité, les tests et le jeu d'essai, le déploiement et DevOps, la veille sécurité. Et je terminerai par la synthèse et la conclusion.

---

### Slide 3 — Compétences CDA mises en œuvre (1 min)

> *Afficher le tableau des compétences*

Avant d'entrer dans le projet, je vais vous montrer comment HappyRow couvre les 11 compétences du référentiel CDA, réparties dans les 3 CCP.

Pour le CCP 1 — Développer une application sécurisée : j'ai installé et configuré mon environnement avec Kotlin, JDK 21, Gradle multi-modules et Docker. J'ai développé les interfaces utilisateur côté front-end avec React et côté back-end avec les endpoints REST Ktor. Les composants métier sont les 12 use cases du module domaine. Et la gestion de projet s'appuie sur Kanban, GitHub Projects et les pipelines CI/CD.

Pour le CCP 2 — Concevoir et développer une application organisée en couches : l'analyse des besoins et les maquettes Figma, l'architecture hexagonale Ports et Adapters, le modèle de données PostgreSQL avec 4 tables et contraintes, et les 4 repositories SQL avec Exposed ORM.

Pour le CCP 3 — Préparer le déploiement : les tests unitaires et d'intégration avec Kotest, MockK, Testcontainers, Vitest et Playwright. Le déploiement documenté avec Docker multi-stage et Render. Et la mise en production DevOps avec les pipelines GitHub Actions.

> *Transition : "Passons au contexte du projet."*

---

## PARTIE 1 — CONTEXTE ET EXPRESSION DES BESOINS (slides 4-8, ~4 min)

### Slide 4 — L'organisme et la promotion (45s)

> *Afficher la slide contexte formation*

Ce projet a été réalisé pendant ma formation CDA au sein de *[nom du centre de formation]*. Notre promotion comptait *[X]* apprenants, encadrés par *[X]* formateurs techniques.

La formation est organisée autour de projets individuels et collectifs. Chaque apprenant développe un projet personnel full-stack qui doit couvrir l'ensemble des compétences du référentiel CDA.

---

### Slide 5 — Mon rôle et le projet (45s)

HappyRow est un projet personnel que j'ai conçu, développé et déployé en totale autonomie, de l'analyse des besoins jusqu'à la mise en production.

J'ai été responsable de l'ensemble de la chaîne : la conception de l'architecture, le développement back-end et front-end, la mise en place de la base de données, l'écriture des tests, la configuration du CI/CD, et le déploiement en production.

Les formateurs ont joué un rôle de conseil et de validation, sans intervenir directement dans le code.

---

### Slide 6 — Environnement de travail (1 min)

> *Afficher la slide environnement*

Pour le développement, j'utilise IntelliJ IDEA comme IDE principal pour le back-end Kotlin et Gradle, et VS Code pour le front-end React et TypeScript.

Le runtime est JDK 21 Eclipse Temurin pour le back-end et Node.js 22 pour le front-end. Docker Desktop permet de lancer un conteneur PostgreSQL en local via docker-compose.

La configuration du projet est centralisée : `application.conf` pour Ktor, un fichier `.env` pour les secrets locaux, et un catalogue de dépendances dans `gradle/libs.versions.toml`.

Point important sur la structure : le projet est organisé en deux modules Gradle séparés — `domain` pour la logique métier pure, sans aucune dépendance technique, et `infrastructure` pour les adapters REST, SQL et l'authentification JWT. Cette séparation est au cœur de l'architecture hexagonale.

> *Transition : "Voyons maintenant le cahier des charges."*

---

### Slide 7 — Contexte et objectifs (45s)

Le projet HappyRow est né d'un constat simple : quand on organise un événement entre amis ou collègues — une fête, un dîner — la coordination repose souvent sur des messages informels ou des tableurs partagés, qui manquent de structure. Qui apporte quoi ? Est-ce qu'on a assez de boissons ?

L'objectif est de centraliser cette coordination dans une application dédiée. Un utilisateur authentifié peut créer un événement, inviter des participants, définir les ressources nécessaires, et chaque participant peut déclarer sa contribution.

Un point clé : quand plusieurs personnes contribuent en même temps sur la même ressource, il faut gérer les accès concurrents. C'est ce qui m'a amené à implémenter un verrou optimiste.

---

### Slide 8 — Contraintes techniques et livrables (45s)

Au niveau des contraintes techniques : le back-end est en Kotlin sur JVM 21 avec Ktor. La base de données est PostgreSQL. L'authentification est déléguée à Supabase avec un JWT signé en HMAC256. L'architecture est hexagonale. Le front-end est une SPA React 19 en TypeScript, déployée comme PWA. Et l'analyse statique avec Detekt et le formatage avec Spotless sont obligatoires dans le pipeline CI/CD.

Les livrables sont : une API REST avec 15 endpoints, une base PostgreSQL avec 4 tables, un front-end React avec 8 écrans, des pipelines CI/CD, et des tests unitaires, d'intégration et E2E.

> *Transition : "Passons à la gestion de projet."*

---

## PARTIE 2 — GESTION DE PROJET (slides 9-11, ~3 min)

### Slide 9 — Méthodologie et planning (1 min)

> *Afficher la slide planning*

J'ai adopté une approche Kanban, adaptée au contexte d'un développeur en autonomie. Les tâches sont organisées en tickets dans GitHub Issues, visualisées dans un tableau GitHub Projects avec trois colonnes : To Do, In Progress, Done.

Le projet a été découpé en 5 phases itératives. La phase 1 pose les fondations : architecture hexagonale, Gradle multi-modules, Docker, PostgreSQL et le pipeline CI/CD. Les phases 2 et 3 couvrent les événements et les participants. La phase 4, la plus complexe, implémente les ressources et les contributions avec le verrou optimiste. Et la phase 5 se concentre sur la qualité et le déploiement en production.

---

### Slide 10 — Suivi des tâches (1 min)

Le suivi du projet s'appuie sur 4 outils complémentaires. GitHub Issues pour le suivi des tâches — chaque ticket correspond à une fonctionnalité ou un bug. GitHub Projects pour la visualisation du tableau Kanban. Git avec des branches par fonctionnalité et merge via pull request. Et GitHub Actions pour le feedback automatisé à chaque push.

Le workflow Git est structuré : la branche `main` contient le code stable et est déployée automatiquement. Chaque fonctionnalité est développée dans une branche isolée, et chaque push sur `main` déclenche les pipelines CI/CD des deux dépôts — back-end et front-end.

---

### Slide 11 — Objectifs de qualité (1 min)

En termes de qualité, le projet repose sur plusieurs outils complémentaires. L'analyse statique avec Detekt détecte les code smells, les problèmes de complexité et les violations de conventions. Le formatage est assuré par Spotless avec ktlint côté back-end et Prettier côté front-end.

Les tests automatisés couvrent les use cases avec Kotest et MockK, et les fonctionnalités front-end avec Vitest et Playwright. La sécurité de la chaîne d'approvisionnement est vérifiée quotidiennement côté front-end avec npm audit et lockfile-lint.

L'objectif est clair : zéro régression en production. Si Detekt ou les tests échouent, le pipeline est bloqué et le déploiement n'a pas lieu.

> *Transition : "Je vais maintenant détailler les spécifications fonctionnelles."*

---

## PARTIE 3 — SPÉCIFICATIONS FONCTIONNELLES (slides 12-20, ~8 min)

### Slide 12 — Architecture hexagonale : principes (1 min)

> *Afficher le schéma d'architecture*

L'architecture suit strictement le pattern Ports et Adapters. Au centre, le domaine contient toute la logique métier : les use cases, les modèles et les interfaces de repository — qu'on appelle les ports.

Autour du domaine, deux types d'adapters. Les adapters driving, côté entrant, ce sont les endpoints REST qui reçoivent les requêtes HTTP et appellent les use cases. Les adapters driven, côté sortant, ce sont les repositories SQL qui implémentent les interfaces définies dans le domaine.

Le principe fondamental, c'est l'inversion de dépendance : le domaine ne dépend d'aucune bibliothèque technique — ni Ktor, ni Exposed, ni Jackson. C'est l'infrastructure qui dépend du domaine.

---

### Slide 13 — Architecture : modules Gradle (45s)

Concrètement, le code est organisé en deux modules Gradle séparés. Le module `domain` ne contient que la logique métier pure — les use cases, les modèles, les interfaces de repository. Il a zéro dépendance technique.

Le module `infrastructure` contient les adapters : les endpoints REST dans des dossiers `driving`, les repositories SQL dans des dossiers `driven`, et les composants techniques comme l'authentification JWT, la configuration de la base de données et l'injection de dépendances avec Koin.

Chaque bounded context — événements, participants, ressources, contributions — a sa propre arborescence dans les deux modules.

---

### Slide 14 — Maquettes et enchaînement des écrans (45s)

> *Afficher le diagramme d'enchaînement + captures*

L'application est une SPA avec React Router. L'enchaînement repose sur l'état d'authentification : si l'utilisateur n'est pas connecté, il voit la page d'accueil avec les boutons Login et Register. Une fois authentifié, il accède au dashboard avec la liste de ses événements.

En cliquant sur un événement, il accède à la vue détail avec les ressources organisées par catégorie. C'est ici que la fonctionnalité de contribution est accessible : les boutons plus/moins pour sélectionner un delta, et le bouton Validate pour confirmer.

La charte graphique utilise des Design Tokens CSS avec les couleurs teal, navy et coral, et la police Comic Neue pour un ton convivial.

---

### Slide 15 — Modèle Conceptuel de Données (1 min)

> *Afficher le MCD*

Le modèle conceptuel comprend 4 entités. Un événement héberge des participants et nécessite des ressources. Un participant apporte des contributions, et une ressource reçoit des contributions.

Les points importants : la table `resource` a un champ `version` pour le verrou optimiste, incrémenté à chaque modification de quantité. Les relations sont toutes en 1-N. Et les contraintes d'intégrité — clés étrangères, index uniques, contraintes CHECK — sont définies au niveau physique.

---

### Slide 16 — Modèle Physique et script SQL (45s)

> *Afficher le code Exposed + SQL*

Au niveau physique, les tables sont définies avec Exposed ORM en Kotlin. Ici, la table `resource` dans le schéma `configuration` avec ses champs, l'enum pour la catégorie, la version initialisée à 0, et la clé étrangère vers la table `event`.

Le script SQL correspondant crée le schéma, la table avec les contraintes — `NOT NULL`, `CHECK` sur les quantités positives, `DEFAULT` pour la version. Exposed génère les requêtes SQL paramétrées à partir de ces définitions.

---

### Slide 17 — Cas d'utilisation (45s)

> *Afficher le diagramme UC*

Le projet couvre 12 cas d'utilisation répartis dans les 4 bounded contexts. Événements : créer, consulter, modifier, supprimer. Participants : ajouter, consulter, modifier le statut. Ressources : créer, consulter. Contributions : ajouter, réduire, supprimer.

Deux règles métier notables : la création d'un événement inclut automatiquement l'ajout du créateur comme participant confirmé — c'est le lien « inclut ». Et la suppression d'un événement déclenche la cascade de toutes les données liées.

---

### Slide 18 — Séquence : création d'événement (1 min)

> *Afficher le diagramme de séquence*

Ce diagramme montre le flux complet de la création d'un événement. Le front-end envoie un POST avec le JWT et le body. Le plugin d'authentification valide le token HMAC256 et extrait l'utilisateur. Le endpoint désérialise la requête et appelle le use case.

Le use case crée l'événement dans le repository, puis crée automatiquement le participant avec le statut CONFIRMED — c'est la règle métier encapsulée dans le domaine. Enfin, le endpoint convertit le résultat en DTO et retourne un 201 Created.

Le point important, c'est l'utilisation d'Arrow Either. Si la création de l'événement échoue, le `flatMap` empêche la création du participant. L'erreur remonte proprement.

---

### Slide 19 — Séquence : contribution avec verrou optimiste (1 min 15s)

> *Afficher le diagramme de séquence*

C'est la fonctionnalité la plus complexe du projet. Quand un utilisateur contribue à une ressource, le système doit mettre à jour la quantité de façon atomique.

Le repository cherche d'abord si une contribution existe déjà. S'il s'agit d'une nouvelle contribution, il l'insère. Ensuite, il appelle `updateQuantity` sur la ressource avec le delta et la version attendue. Le SQL exécute un UPDATE conditionnel : `WHERE version = expectedVersion`. Si une autre personne a modifié la ressource entre-temps, la version en base ne correspond plus, zéro lignes sont mises à jour, et une `OptimisticLockException` est levée.

Cette exception remonte et le endpoint retourne un 409 Conflict. Le client doit rafraîchir et réessayer. L'avantage par rapport au verrou pessimiste, c'est que ça ne bloque pas les autres transactions.

---

### Slide 20 — Stack technique complète (45s)

> *Afficher les tableaux de stack*

Pour résumer la stack technique : côté back-end, Kotlin 2.2 sur JVM 21, Ktor 3.2 pour le web, Exposed pour l'ORM, HikariCP pour le pool de connexions, Koin pour l'injection de dépendances, Arrow pour la gestion fonctionnelle des erreurs, Jackson pour la sérialisation JSON, et Auth0 JWT pour l'authentification.

Côté front-end : React 19 en TypeScript strict, Vite pour le build, React Router pour la navigation, Supabase SDK pour l'authentification, CSS natif avec Design Tokens, et PWA avec Workbox.

Point notable : le front-end n'a que 3 dépendances de production. Pas d'Axios, pas de framework CSS lourd. L'API native `fetch` est utilisée directement.

> *Transition : "Passons aux réalisations concrètes."*

---

## PARTIE 4 — RÉALISATIONS : INTERFACES UTILISATEUR (slides 21-24, ~4 min)

### Slide 21 — Captures d'écran : Dashboard (45s)

> *Afficher la capture d'écran du dashboard*

Voici le dashboard de l'application. Il affiche les événements de l'utilisateur sous forme de cartes — les EventCards — avec la date, le nom, le nombre de participants et la localisation.

La barre de navigation permet d'accéder au profil, de créer un nouvel événement avec le bouton plus, et de revenir à l'accueil.

L'application est une PWA installable, avec un mode offline grâce au Service Worker Workbox. Et je le rappelle : seulement 3 dépendances de production — React, React Router et Supabase.

---

### Slide 22 — Captures d'écran : EventDetailsView (45s)

> *Afficher la capture d'écran EventDetails*

La vue détail d'un événement montre les ressources organisées par catégorie — Food, Drinks, et cetera. Chaque ressource affiche la quantité courante par rapport à la quantité suggérée.

Les contrôles de contribution sont intégrés directement : les boutons plus et moins pour sélectionner un delta de quantité. Quand un delta est sélectionné, le bouton Validate apparaît pour confirmer la contribution.

Le design utilise les Design Tokens CSS — les couleurs teal, navy et coral définies comme variables CSS — ce qui garantit la cohérence graphique et facilite la maintenance.

---

### Slide 23 — Code React : ResourceItem (1 min 30s)

> *Afficher le code*

Le composant `ResourceItem` gère l'interaction de contribution. La logique est intéressante : selon l'état actuel, le composant décide s'il faut ajouter, mettre à jour ou supprimer la contribution.

Le `selectedQuantity` est un delta par rapport à la contribution existante. Si la quantité résultante tombe à zéro ou moins, c'est une suppression. Si l'utilisateur n'avait pas de contribution, c'est un ajout. Sinon, c'est une mise à jour.

Le bouton Validate n'apparaît que quand un delta est sélectionné — on ne peut pas soumettre une contribution vide. Et le state `isSaving` empêche les doubles soumissions pendant l'appel API.

---

### Slide 24 — Architecture front-end Clean Architecture (1 min)

Le front-end suit le même principe d'inversion de dépendance que le back-end. Le flux est : Composants React → Context/Provider → Use Cases → Repository interface → HttpRepository qui utilise l'API native `fetch`.

Chaque domaine fonctionnel — auth, events, participants, resources, contributions — est un module isolé dans `src/features/` avec ses propres composants, hooks, services, use cases et types.

Le token JWT est injecté via une callback `getToken` dans les providers. Chaque repository HTTP ajoute le header Authorization Bearer automatiquement. Cette architecture permet de mocker les repositories dans les tests — exactement comme côté back-end.

> *Transition : "Voyons maintenant les composants métier."*

---

## PARTIE 5 — RÉALISATIONS : COMPOSANTS MÉTIER (slides 25-28, ~4 min)

### Slide 25 — Use Case : CreateEventUseCase (1 min 15s)

> *Afficher le code*

Ce use case illustre la composition fonctionnelle avec Arrow Either. La méthode `create` enchaîne deux opérations : la création de l'événement et l'ajout du participant.

Le `flatMap` assure que le participant n'est créé que si l'événement a réussi. Le `mapLeft` enveloppe les erreurs de repository dans une exception domaine pour préserver le contexte.

Le point fondamental, c'est que ce use case ne dépend que des interfaces — les ports — jamais des implémentations SQL. Il est testable avec des mocks, sans base de données. Et la règle métier — le créateur est automatiquement participant confirmé — est encapsulée dans le domaine, pas dans l'infrastructure.

---

### Slide 26 — Use Case : AddContributionUseCase (1 min)

Ce deuxième use case montre la chaîne fonctionnelle complète pour l'ajout d'une contribution. Il orchestre deux repositories : le repository de participants et celui des contributions.

D'abord, il cherche ou crée le participant avec `findOrCreate` — si le participant n'existe pas encore pour cet événement, il est créé automatiquement. Ensuite, il appelle `addOrUpdate` sur le repository de contributions, qui inclut le verrou optimiste dans son implémentation SQL.

Le use case ne connaît pas les détails de l'implémentation. Il ne sait pas qu'il y a un verrou optimiste ou une transaction SQL. Il sait seulement qu'il orchestre deux opérations qui peuvent réussir ou échouer, et il compose les résultats avec `flatMap`.

---

### Slide 27 — Modèles domaine (1 min)

> *Afficher le code des data classes*

Les modèles du domaine sont des `data class` Kotlin pures. Aucune annotation framework — pas de `@Entity`, pas de `@Column`. Le domaine est complètement isolé de l'infrastructure.

Le type `Creator` est un `@JvmInline value class` : c'est un typage fort qui encapsule un String, mais sans coût mémoire à l'exécution — le compilateur l'inline. Ça empêche de confondre un email avec un nom ou un identifiant.

Le champ `version` dans `Resource` porte la sémantique du verrou optimiste directement au niveau du modèle. Ce n'est pas un détail technique caché dans l'infrastructure — c'est un concept métier explicite.

---

### Slide 28 — Gestion d'erreurs Arrow Either (45s)

Pour résumer le pattern de gestion d'erreurs : chaque opération retourne un `Either<Error, Success>`. Le type de retour est explicite — on voit dans la signature que l'opération peut échouer.

La composition se fait via `flatMap` pour chaîner les opérations qui dépendent du résultat précédent, et `mapLeft` pour envelopper les erreurs avec du contexte supplémentaire. Et `fold` résout le résultat final en séparant le chemin d'erreur du chemin de succès.

Les avantages par rapport aux exceptions classiques : c'est composable, il n'y a pas de coût de stack trace, et la traçabilité est meilleure car chaque couche enveloppe les erreurs de la couche inférieure.

> *Transition : "Voyons la couche d'accès aux données."*

---

## PARTIE 6 — RÉALISATIONS : ACCÈS AUX DONNÉES (slides 29-32, ~4 min)

### Slide 29 — Verrou optimiste : SqlResourceRepository (1 min 30s)

> *Afficher le code*

C'est l'extrait de code le plus significatif du projet. La méthode `updateQuantity` implémente le verrou optimiste en deux temps.

D'abord, elle lit la ressource avec une clause `WHERE version = expectedVersion`. Si aucune ligne n'est retournée, elle distingue deux cas : la ressource n'existe pas — erreur 404 — ou la version a changé — conflit de concurrence.

Ensuite, elle exécute un UPDATE conditionnel avec la même clause sur la version. Si entre la lecture et l'écriture un autre thread a modifié la ressource, l'UPDATE retourne zéro lignes, et une `OptimisticLockException` est levée.

La double vérification — à la lecture et à l'écriture — est une défense en profondeur contre les race conditions. Le delta de quantité, plutôt qu'une valeur absolue, permet des mises à jour concurrentes valides si elles ne concernent pas la même version.

---

### Slide 30 — ContributionTable Exposed (45s)

La table Exposed pour les contributions montre l'approche de défense en profondeur au niveau de la base de données.

L'index unique composite sur `participantId` et `resourceId` empêche qu'un participant contribue deux fois à la même ressource — même si un bug applicatif tentait de le faire. La contrainte CHECK garantit que les quantités sont positives. Les index sur les clés étrangères optimisent les requêtes de recherche.

C'est le principe : les contraintes ne sont pas seulement dans le code applicatif, elles sont aussi dans la base.

---

### Slide 31 — SqlContributionRepository (1 min)

> *Afficher le code*

Ce repository illustre l'implémentation concrète d'un port du domaine. La méthode `addOrUpdate` recherche d'abord une contribution existante pour le participant et la ressource. Si elle existe, elle met à jour la quantité. Sinon, elle insère une nouvelle contribution.

Tout est encapsulé dans une transaction Exposed, ce qui garantit l'atomicité. Et les requêtes sont automatiquement paramétrées par Exposed — pas de risque d'injection SQL.

Après l'opération sur la contribution, le use case appelle `updateQuantity` sur la ressource pour mettre à jour la quantité courante avec le verrou optimiste.

---

### Slide 32 — Transactions et intégrité des données (45s)

En résumé, l'intégrité des données repose sur cinq mécanismes complémentaires. Le verrou optimiste empêche les mises à jour concurrentes silencieuses. Les clés étrangères en cascade assurent la suppression propre des données liées. Les index uniques composites empêchent les doublons. Les contraintes CHECK garantissent la validité des données. Et chaque opération est encapsulée dans une transaction.

Pour la suppression en cascade, l'ordre est imposé par les clés étrangères : d'abord les contributions, puis les ressources, puis les participants, et enfin l'événement. Cet ordre est géré manuellement dans le repository pour garder le contrôle.

> *Transition : "Passons aux autres composants."*

---

## PARTIE 7 — RÉALISATIONS : AUTRES COMPOSANTS (slides 33-35, ~3 min)

### Slide 33 — Authentification JWT (1 min 15s)

> *Afficher le code*

L'authentification utilise un plugin Ktor personnalisé plutôt que le module auth standard. Ça donne un contrôle total sur le flux.

Le plugin intercepte chaque requête. Les routes OPTIONS — pour le pré-vol CORS — et les routes publiques sont exclues. Si le token est absent, c'est un 401 avec le type `MISSING_TOKEN`. Sinon, le `SupabaseJwtService` vérifie la signature HMAC256, l'issuer, l'audience et l'expiration. Si tout est valide, l'utilisateur authentifié est stocké dans les attributs du call.

Le secret JWT est chargé depuis une variable d'environnement, jamais codé en dur. Detekt vérifie l'absence de secrets dans le code.

---

### Slide 34 — Endpoint REST : CreateEventEndpoint (1 min)

> *Afficher le code*

Cet endpoint est un example typique de driving adapter. Il reçoit la requête HTTP, la désérialise, appelle le use case du domaine, et formate la réponse.

`Either.catch` capture les exceptions de désérialisation et les transforme en erreur typée. `flatMap` chaîne l'appel au use case — si la désérialisation échoue, le use case n'est pas appelé. Les fonctions `toDomain()` et `toDto()` assurent l'isolation entre les objets de l'API et ceux du domaine. Et `fold` sépare clairement le chemin d'erreur du chemin de succès.

Ce pattern est appliqué à tous les 15 endpoints de l'application.

---

### Slide 35 — Gestion des erreurs HTTP (45s)

> *Afficher le code de handleFailure*

La gestion des erreurs HTTP est typée et centralisée. Pour la contribution, le `handleFailure` remonte la cause racine via `generateSequence` pour distinguer un conflit de concurrence d'une erreur technique.

Si c'est une `OptimisticLockException`, on retourne un 409 Conflict avec un message explicite. Sinon, c'est un 500 avec un message générique — aucune stack trace exposée au client.

Les codes HTTP utilisés dans l'application sont standards : 201 pour la création, 200 pour le succès, 400 pour les requêtes invalides, 401 pour l'authentification manquante, 403 pour l'autorisation refusée, 409 pour les conflits, et 500 pour les erreurs techniques.

> *Transition : "Parlons maintenant de la sécurité."*

---

## PARTIE 8 — SÉCURITÉ (slides 36-39, ~3 min)

### Slide 36 — Authentification et autorisation (45s)

> *Afficher le diagramme de séquence*

Le flux d'authentification complet passe par Supabase. L'utilisateur saisit son email et mot de passe côté front-end. Le SDK Supabase obtient un JWT signé en HMAC256. Ce token est envoyé dans le header Authorization à chaque requête API.

Côté autorisation, la suppression d'un événement est réservée au créateur. L'identité est toujours extraite du JWT côté serveur — jamais transmise par le client. C'est ce qui empêche un utilisateur de contribuer au nom d'un autre.

---

### Slide 37 — Sécurité applicative (45s)

Voici les mesures de sécurité applicatives. Contre l'injection SQL : Exposed ORM avec des requêtes paramétrées, aucune requête SQL brute. Contre le XSS : l'API ne retourne que du JSON, et React assure l'échappement côté front-end.

Le CORS est configuré avec une liste blanche d'origines — pas de wildcard. Les secrets ne sont jamais dans le code, et Detekt le vérifie. L'usurpation d'identité est impossible car l'email est extrait du JWT serveur, jamais du body client.

La validation des entrées se fait à 3 niveaux : endpoint, DTO Jackson, et contraintes en base de données.

---

### Slide 38 — Sécurité infrastructure (45s)

Au niveau infrastructure : HTTPS est forcé par Render avec un certificat TLS automatique. La connexion PostgreSQL utilise SSL en mode `require`. Le conteneur Docker tourne en utilisateur non-root. L'utilisateur de base de données a des droits restreints au schéma `configuration`. Les secrets CI/CD sont dans GitHub Secrets. Et Dependabot surveille les vulnérabilités des dépendances.

Pour l'intégrité des données, on combine le verrou optimiste, les clés étrangères en cascade, les index uniques et les transactions.

---

### Slide 39 — Éco-conception (45s)

La conception de HappyRow intègre des principes d'éco-conception numérique. La sobriété fonctionnelle limite le périmètre aux besoins réels. Le minimalisme des dépendances : seulement 3 dépendances de production côté front-end, API fetch native. Les images Docker sont optimisées avec le build multi-stage — l'image de production ne contient que le JRE et le JAR.

La PWA avec le Service Worker Workbox réduit les requêtes réseau grâce au cache. Le pool de connexions HikariCP mutualise les connexions PostgreSQL. Et la JVM est tunée avec des limites mémoire adaptées au besoin réel.

> *Transition : "Passons aux tests."*

---

## PARTIE 9 — TESTS ET JEU D'ESSAI (slides 40-43, ~3 min 30s)

### Slide 40 — Stratégie de tests (45s)

> *Afficher la pyramide et le pipeline*

La stratégie suit une pyramide de tests. Les tests unitaires avec Kotest et MockK testent les use cases et la logique métier. Les tests d'intégration avec Testcontainers testent les repositories avec une vraie base PostgreSQL dans un conteneur Docker jetable.

L'analyse statique avec Detekt tourne à chaque push. Côté front-end, Vitest couvre l'authentification avec 46 tests unitaires, et Playwright vérifie les capacités PWA avec 6 tests E2E.

Les tests d'intégration Testcontainers sont exclus du pipeline CI pour la performance, mais sont exécutés localement avant chaque mise en production.

---

### Slide 41 — Tests unitaires : exemple Kotest BDD (45s)

> *Afficher le code de test*

Les tests suivent une structure BDD — Given, When, Then — avec des fonctions d'aide pour la lisibilité. Le premier test vérifie le scénario nominal : créer un événement et vérifier que le créateur est automatiquement ajouté comme participant.

Le deuxième test vérifie la propagation des erreurs : si le repository échoue, l'erreur est correctement enveloppée dans une exception domaine.

Les données de test sont centralisées dans des objets Persona réutilisables. Les assertions Arrow — `shouldBeRight` et `shouldBeLeft` — vérifient le contenu de l'Either.

---

### Slide 42 — Jeu d'essai : scénarios (1 min)

La fonctionnalité la plus représentative est l'ajout d'une contribution avec verrou optimiste, car elle traverse toutes les couches.

Le scénario nominal montre les 6 étapes : la requête HTTP, l'extraction du JWT, la recherche du participant, la création de la contribution, la mise à jour de la ressource avec le verrou optimiste, et la réponse 200 OK. Pour chaque étape, j'ai vérifié la donnée en entrée, le résultat attendu et le résultat obtenu. Aucun écart constaté.

Le scénario de conflit montre ce qui se passe quand deux utilisateurs contribuent en même temps. L'utilisateur A réussit et fait passer la version de 0 à 1. L'utilisateur B, qui avait lu la version 0, échoue avec une `OptimisticLockException` et reçoit un 409 Conflict.

---

### Slide 43 — Tests de sécurité (45s)

J'ai élaboré 7 tests de sécurité spécifiques à la fonctionnalité contribution. Sans JWT : 401. JWT expiré : 401. Mauvaise signature : 401. Quantité négative : 400. Quantité non numérique : 400. Injection SQL via le body : 400 — double protection par Jackson et Exposed. Et tentative d'usurpation d'identité : la contribution est créée avec l'email du JWT, pas celui du body.

Tous les tests sont conformes. La stratégie de défense en profondeur fonctionne à chaque niveau.

> *Transition : "Voyons le déploiement."*

---

## PARTIE 10 — DÉPLOIEMENT ET DEVOPS (slides 44-46, ~2 min 30s)

### Slide 44 — Architecture de déploiement (45s)

> *Afficher le schéma d'architecture*

L'architecture de déploiement a trois environnements. En local, docker-compose lance PostgreSQL dans un conteneur, et les deux applications — back-end et front-end — tournent en mode développement.

En CI, GitHub Actions exécute les pipelines à chaque push sur `main`. Les secrets sont dans GitHub Secrets, jamais dans le code.

En production, le back-end est déployé sur Render à Francfort dans un conteneur Docker. Le front-end est déployé sur Vercel. La base de données PostgreSQL est hébergée par Supabase. Vercel communique avec Render via les appels API REST.

---

### Slide 45 — Pipeline CI/CD détaillé (1 min)

Le pipeline back-end s'exécute en 3 étapes séquentielles : d'abord Detekt pour l'analyse statique, puis les tests unitaires avec Kotest et MockK — sans les tests d'intégration pour la performance — et enfin le déploiement sur Render via son API.

Le pipeline front-end a 4 étapes : d'abord les tests avec lockfile-lint, npm ci en mode sécurisé, ESLint et Vitest. Ensuite le build Docker avec push sur le registry GitHub. Puis une approbation manuelle — un gate de sécurité humain. Et enfin le déploiement sur Vercel.

En plus, un workflow de sécurité s'exécute quotidiennement en cron : audit npm pour les vulnérabilités connues, vérification de l'intégrité du lockfile, et détection de scripts d'installation suspects.

---

### Slide 46 — Mise en production (45s)

> *Afficher le Dockerfile*

Le déploiement back-end utilise un Dockerfile multi-stage. Le premier stage utilise l'image Gradle complète pour le build. Le second stage ne conserve que le JRE 21 et le JAR — le SDK et les sources ne sont pas dans l'image de production. Le conteneur tourne en utilisateur non-root et la JVM est configurée avec des limites mémoire.

Les variables d'environnement sur Render configurent la connexion à la base de données, le secret JWT Supabase, le mode SSL, et les origines CORS autorisées. La procédure est automatisée : push sur main, pipeline, déploiement zero-downtime.

> *Transition : "Pour finir, la veille sécurité."*

---

## PARTIE 11 — VEILLE SÉCURITÉ (slides 47-48, ~2 min)

### Slide 47 — Démarche de veille (1 min)

La veille sécurité a été menée tout au long du projet, pas seulement à la fin. En début de projet, j'ai étudié le OWASP Top 10 pour établir la stratégie de sécurité. Pendant la phase d'authentification, j'ai consulté les documentations Ktor Security et Supabase Auth, et vérifié les CVE sur la librairie auth0 java-jwt — aucune CVE critique, choix validé par la RFC 7518.

J'ai activé Dependabot sur le dépôt GitHub dès la phase 3. Pendant la phase contributions, j'ai recherché les vulnérabilités liées à la concurrence — les problèmes de type TOCTOU, Time-Of-Check-Time-Of-Use — ce qui a renforcé ma décision d'implémenter le verrou optimiste avec double vérification.

En phase 5, le workflow Security Audit a été mis en place pour une surveillance quotidienne automatisée côté front-end.

Les sources consultées incluent le OWASP, la base CVE, GitHub Dependabot, les documentations officielles de Ktor, Supabase, PostgreSQL, et les recommandations de l'ANSSI.

---

### Slide 48 — OWASP Top 10 : couverture (1 min)

> *Afficher les tableaux*

Sur les 10 catégories du OWASP 2021, j'en ai traité 6 explicitement. Broken Access Control avec le JWT obligatoire et l'autorisation par créateur. Cryptographic Failures avec HMAC256, HTTPS et SSL. Injection avec Exposed ORM et les requêtes paramétrées. Security Misconfiguration avec CORS whitelist et Docker non-root. XSS avec l'API JSON-only et l'échappement React. Et Data Integrity avec Dependabot, le verrou optimiste et les contraintes en base.

Pour les 4 restantes : Insecure Design est couvert implicitement par l'architecture hexagonale. Vulnerable Components est couvert par Dependabot. Security Logging est identifié comme perspective d'évolution. Et SSRF n'est pas applicable car l'API ne fait aucun appel HTTP sortant basé sur des données utilisateur.

Aucune CVE critique n'a été identifiée pendant la durée du projet.

> *Transition : "Pour conclure."*

---

## PARTIE 12 — SYNTHÈSE ET CONCLUSION (slides 49-50, ~2 min)

### Slide 49 — Bilan et satisfactions (1 min)

En résumé, le projet HappyRow a abouti à une application full-stack fonctionnelle et déployée. Le back-end compte 15 endpoints REST, 12 use cases, environ 5 000 lignes de Kotlin. Le front-end est une PWA React 19 avec 8 écrans, 46 tests unitaires et 6 tests E2E. Seulement 3 dépendances de production.

Mes satisfactions principales : l'architecture hexagonale a tenu ses promesses en termes de testabilité et d'évolutivité. Arrow Either a transformé la gestion d'erreurs en la rendant explicite et composable. Le verrou optimiste m'a permis de résoudre un vrai problème de concurrence. La Clean Architecture côté front-end assure la cohérence architecturale. Et les pipelines CI/CD apportent une confiance réelle à chaque déploiement.

---

### Slide 50 — Difficultés et perspectives (1 min)

Les difficultés principales ont été : la propagation de la version du verrou optimiste à travers toutes les couches, de la base de données jusqu'au 409 côté client. La suppression en cascade imposée par les clés étrangères, qui nécessite un ordre précis. Et la configuration SSL lors du déploiement alternatif sur Raspberry Pi, qui a conduit à centraliser tous les secrets en variables d'environnement.

En termes de perspectives : une application mobile native ou cross-platform, des notifications temps réel via WebSocket, l'exploration du NoSQL pour les logs d'activité, l'intégration des tests Testcontainers dans le pipeline CI, et l'ajout de monitoring avec Micrometer et Grafana.

---

Merci pour votre attention. Je suis disponible pour vos questions.

---

## Récapitulatif des timings

| Slides | Section | Timing |
|--------|---------|--------|
| 1-3 | Introduction (titre, sommaire, compétences) | 2:00 |
| 4-6 | Contexte de formation | 2:30 |
| 7-8 | Cahier des charges | 1:45 |
| 9-11 | Gestion de projet | 3:00 |
| 12-14 | Specs — Architecture + Maquettes | 2:30 |
| 15-16 | Specs — MCD + MPD | 1:45 |
| 17 | Specs — Cas d'utilisation | 0:45 |
| 18-19 | Specs — Diagrammes de séquence | 2:15 |
| 20 | Specs — Stack technique | 0:45 |
| 21-22 | Réalisations UI — Captures | 1:30 |
| 23-24 | Réalisations UI — Code React + Archi front | 2:30 |
| 25-26 | Réalisations métier — Use cases | 2:15 |
| 27-28 | Réalisations métier — Modèles + Arrow | 1:45 |
| 29-30 | Réalisations données — Verrou + Table | 2:15 |
| 31-32 | Réalisations données — Repo + Transactions | 1:45 |
| 33-35 | Autres composants — JWT + Endpoint + Erreurs | 3:00 |
| 36-39 | Sécurité | 3:00 |
| 40-43 | Tests et jeu d'essai | 3:15 |
| 44-46 | Déploiement et DevOps | 2:30 |
| 47-48 | Veille sécurité | 2:00 |
| 49-50 | Synthèse et conclusion | 2:00 |
| | **TOTAL** | **~41 min** |

> **Marge** : ~1 min de marge sur 40 min. Si vous êtes en avance, développez les slides 19 (verrou optimiste) et 29 (code SQL) qui sont les plus impressionnantes techniquement. Si vous êtes en retard, condensez les slides 20 (stack technique), 32 (transactions) et 39 (éco-conception) qui reprennent du contenu déjà couvert dans d'autres slides.
