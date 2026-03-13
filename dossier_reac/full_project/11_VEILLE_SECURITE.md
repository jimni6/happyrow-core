# 11. Veille sur les vulnérabilités de sécurité

Ce chapitre décrit la veille sécurité effectuée pendant le développement du projet HappyRow, les vulnérabilités identifiées et les mesures correctives appliquées.

## 11.1 Démarche de veille

### Chronologie de la veille

| Période | Action | Résultat |
|---------|--------|----------|
| **Début de projet** (phase 1 — Fondations) | Étude du OWASP Top 10 (2021) pour établir la stratégie de sécurité globale | Identification des 6 catégories pertinentes au projet (A01, A02, A03, A05, A07, A08) |
| **Phase 2 — Authentification** | Consultation de la documentation Ktor Security et Supabase Auth. Vérification des CVE sur `com.auth0:java-jwt` (v4.4.0) | Aucune CVE critique identifiée. Choix de HMAC256 validé par la RFC 7518 |
| **Phase 3 — Participants** | Activation de Dependabot sur le dépôt GitHub. Vérification des advisories sur Exposed ORM (v0.61.0) | Alertes automatiques configurées. Aucun advisory critique sur Exposed |
| **Phase 4 — Contributions** | Recherche sur les vulnérabilités liées à la concurrence (race conditions, TOCTOU). Consultation de la documentation PostgreSQL sur les niveaux d'isolation | Implémentation du verrou optimiste avec double vérification (lecture + écriture) |
| **Phase 5 — Qualité** | Audit des dépendances front-end (`npm audit`). Configuration du workflow Security Audit quotidien (cron GitHub Actions) | Pipeline de sécurité automatisé : audit npm, lockfile-lint, détection de scripts suspects |
| **En continu** | Surveillance des alertes Dependabot et des mises à jour Ktor/Supabase | Mises à jour mineures appliquées régulièrement |

### Sources consultées

| Source | Type | Fréquence |
|--------|------|-----------|
| **OWASP Top 10** (2021) | Référentiel de vulnérabilités web | Consulté en début de projet pour établir la stratégie de sécurité |
| **CVE Database** (cve.org) | Base de données de vulnérabilités connues | Vérification ponctuelle sur les dépendances critiques (auth0 java-jwt, Ktor, Exposed, HikariCP) |
| **Advisories GitHub** (Dependabot) | Alertes automatiques sur les dépendances | En continu (notifications automatiques sur les 2 dépôts) |
| **Documentation Ktor Security** | Bonnes pratiques framework | Lors de l'implémentation de l'authentification |
| **Documentation Supabase Auth** | Sécurité JWT, bonnes pratiques | Lors de l'intégration de l'authentification |
| **ANSSI — Recommandations** | Guides de sécurité des systèmes d'information | Consulté pour les principes généraux (chiffrement, gestion des secrets) |
| **PostgreSQL Security** | Documentation officielle sur les niveaux d'isolation et le SSL | Lors de l'implémentation du verrou optimiste et de la configuration SSL |

## 11.2 Vulnérabilités identifiées et mesures appliquées

### A03:2021 — Injection

| Risque | Mesure appliquée |
|--------|-----------------|
| **Injection SQL** | Utilisation exclusive d'Exposed ORM avec requêtes paramétrées. Aucune requête SQL brute dans le projet. Chaque valeur est passée comme paramètre, jamais concaténée dans une chaîne SQL. |
| **Injection dans les logs** | Les entrées utilisateur ne sont pas directement injectées dans les messages de log. Les identifiants (UUID) sont utilisés comme référence. |

### A01:2021 — Broken Access Control

| Risque | Mesure appliquée |
|--------|-----------------|
| **Accès non autorisé aux événements** | Chaque requête passe par `JwtAuthenticationPlugin`. L'identité de l'utilisateur est extraite du token, jamais transmise par le client. |
| **Suppression par un non-créateur** | Le `SqlEventRepository.delete` vérifie que l'utilisateur authentifié est le créateur de l'événement. En cas de non-correspondance : `UnauthorizedDeleteException` → 403. |
| **Manipulation des contributions** | L'email du contributeur est extrait du JWT côté serveur via `authenticatedUser().email`, empêchant un utilisateur de contribuer au nom d'un autre. |

### A07:2021 — Cross-Site Scripting (XSS)

| Risque | Mesure appliquée |
|--------|-----------------|
| **XSS via les entrées** | L'API retourne uniquement du JSON (pas de HTML), éliminant le vecteur XSS côté serveur. La protection XSS est assurée côté front-end par le framework (échappement automatique). |

### A02:2021 — Cryptographic Failures

| Risque | Mesure appliquée |
|--------|-----------------|
| **Exposition de secrets** | Tous les secrets (JWT secret, credentials DB) sont stockés en variables d'environnement. Detekt vérifie l'absence de secrets codés en dur. Le fichier `.env` est dans `.gitignore`. |
| **Communication en clair** | HTTPS forcé en production (Render). SSL activé pour la connexion PostgreSQL (`DB_SSL_MODE=require`). |
| **Algorithme JWT** | HMAC256 — algorithme symétrique recommandé par la RFC 7518 pour les tokens de courte durée. |

### A05:2021 — Security Misconfiguration

| Risque | Mesure appliquée |
|--------|-----------------|
| **CORS permissif** | Liste blanche explicite des origines autorisées. Pas de wildcard `*`. |
| **Headers de sécurité** | Ktor configuré avec les headers essentiels. Render ajoute les headers de sécurité au niveau du reverse proxy. |
| **Exposition d'informations** | Les erreurs 500 retournent un message générique. Aucune stack trace, aucun nom de table, aucune requête SQL dans les réponses d'erreur. |
| **Utilisateur Docker non-root** | Le conteneur s'exécute avec `USER 1000:1000`, réduisant la surface d'attaque. |

### A08:2021 — Software and Data Integrity Failures

| Risque | Mesure appliquée |
|--------|-----------------|
| **Dépendances compromises** | Versions des dépendances fixées dans `gradle/libs.versions.toml`. Alertes GitHub Dependabot activées. |
| **Intégrité des données** | Verrou optimiste sur les ressources, contraintes d'intégrité en base (FK, CHECK, UNIQUE), transactions pour chaque opération. |

## 11.3 Catégories OWASP non traitées — Justification

Les catégories suivantes du OWASP Top 10 (2021) n'ont pas fait l'objet de mesures spécifiques dans le projet. Voici la justification pour chacune :

| Catégorie | Justification de non-traitement |
|-----------|-------------------------------|
| **A04:2021 — Insecure Design** | Le projet suit une architecture hexagonale avec séparation stricte des responsabilités, validation à chaque couche (endpoint → domaine → base), et utilisation de types forts (Either, value class). Ces choix de conception constituent en eux-mêmes une réponse à cette catégorie. Aucun défaut de conception n'a été identifié lors de la veille. |
| **A06:2021 — Vulnerable and Outdated Components** | Couvert indirectement par Dependabot (alertes automatiques) et le versionnage strict des dépendances dans `gradle/libs.versions.toml` et `package-lock.json`. Le pipeline Security Audit front-end vérifie quotidiennement la fraîcheur des dépendances. Aucune CVE critique n'a été détectée pendant la durée du projet. |
| **A09:2021 — Security Logging and Monitoring Failures** | Le projet dispose d'un logging serveur pour les erreurs (stack trace côté serveur, messages génériques côté client), mais ne dispose pas d'un système de monitoring dédié (type Grafana/Prometheus). Cette limitation est identifiée dans les perspectives d'évolution (chapitre 12). Pour un projet de cette taille, le logging existant est jugé suffisant. |
| **A10:2021 — Server-Side Request Forgery (SSRF)** | Non applicable au projet : l'API HappyRow ne fait aucun appel HTTP sortant basé sur des données utilisateur. Toutes les URL sont codées en dur (connexion PostgreSQL, configuration Supabase). Il n'y a aucun mécanisme de fetch d'URL fournie par le client. |

## 11.4 Bilan de la veille

La veille effectuée pendant le projet a permis de :

1. **Identifier les risques** avant l'implémentation (OWASP Top 10 comme guide de conception)
2. **Appliquer les mesures préventives** dès la phase de développement (requêtes paramétrées, validation des entrées, gestion des secrets)
3. **Vérifier l'absence de CVE critiques** sur les dépendances principales (Ktor, Exposed, Auth0 JWT)
4. **Documenter les choix de sécurité** pour la traçabilité et la maintenance

Aucune vulnérabilité critique n'a été identifiée dans les dépendances utilisées pendant la durée du projet. Les mises à jour mineures ont été appliquées régulièrement.
