# 11. Veille sur les vulnérabilités de sécurité

Dans ce chapitre, je décris la veille sécurité que j'ai menée tout au long du développement de HappyRow, les vulnérabilités que j'ai identifiées et les mesures correctives que j'ai appliquées.

## 11.1 Démarche de veille

### Chronologie de la veille

J'ai intégré la veille sécurité à chaque phase du projet :

| Période | Ce que j'ai fait | Résultat |
|---------|--------|----------|
| **Début de projet** (phase 1) | J'ai étudié le OWASP Top 10 (2021) pour établir ma stratégie de sécurité | J'ai identifié 6 catégories pertinentes (A01, A02, A03, A05, A07, A08) |
| **Phase 2 — Authentification** | J'ai consulté la doc Ktor Security et Supabase Auth. J'ai vérifié les CVE sur `com.auth0:java-jwt` (v4.4.0) | Aucune CVE critique. Choix HMAC256 validé par la RFC 7518 |
| **Phase 3 — Participants** | J'ai activé Dependabot sur GitHub et vérifié les advisories sur Exposed ORM (v0.61.0) | Alertes automatiques configurées. Aucun advisory critique |
| **Phase 4 — Contributions** | J'ai recherché les vulnérabilités liées à la concurrence (race conditions, TOCTOU). J'ai consulté la doc PostgreSQL sur les niveaux d'isolation | J'ai implémenté le verrou optimiste avec double vérification |
| **Phase 5 — Qualité** | J'ai audité les dépendances front-end (`npm audit`) et configuré un workflow Security Audit quotidien (cron GitHub Actions) | Pipeline de sécurité automatisé : audit npm, lockfile-lint, détection de scripts suspects |
| **En continu** | J'ai surveillé les alertes Dependabot et les mises à jour Ktor/Supabase | Mises à jour mineures appliquées régulièrement |

### Sources que j'ai consultées

| Source | Type | Fréquence |
|--------|------|-----------|
| **OWASP Top 10** (2021) | Référentiel de vulnérabilités web | Consulté en début de projet pour ma stratégie de sécurité |
| **CVE Database** (cve.org) | Base de données de vulnérabilités connues | Vérification ponctuelle sur les dépendances critiques (auth0 java-jwt, Ktor, Exposed, HikariCP) |
| **Advisories GitHub** (Dependabot) | Alertes automatiques sur les dépendances | En continu (notifications automatiques sur les 2 dépôts) |
| **Documentation Ktor Security** | Bonnes pratiques framework | Lors de l'implémentation de l'authentification |
| **Documentation Supabase Auth** | Sécurité JWT, bonnes pratiques | Lors de l'intégration de l'authentification |
| **ANSSI — Recommandations** | Guides de sécurité des systèmes d'information | Consulté pour les principes généraux (chiffrement, gestion des secrets) |
| **PostgreSQL Security** | Doc officielle sur les niveaux d'isolation et le SSL | Lors de l'implémentation du verrou optimiste et de la configuration SSL |

## 11.2 Vulnérabilités identifiées et mesures appliquées

### A03:2021 — Injection

| Risque | Ce que j'ai mis en place |
|--------|-----------------|
| **Injection SQL** | J'utilise exclusivement Exposed ORM avec des requêtes paramétrées. Il n'y a aucune requête SQL brute dans mon projet. Chaque valeur est passée comme paramètre, jamais concaténée dans une chaîne SQL. |
| **Injection dans les logs** | Les entrées utilisateur ne sont pas directement injectées dans les messages de log. J'utilise les identifiants (UUID) comme référence. |

### A01:2021 — Broken Access Control

| Risque | Ce que j'ai mis en place |
|--------|-----------------|
| **Accès non autorisé aux événements** | Chaque requête passe par mon `JwtAuthenticationPlugin`. L'identité de l'utilisateur est extraite du token, jamais transmise par le client. |
| **Suppression par un non-créateur** | Dans `SqlEventRepository.delete`, je vérifie que l'utilisateur authentifié est bien le créateur de l'événement. Sinon : `UnauthorizedDeleteException` → 403. |
| **Manipulation des contributions** | L'email du contributeur est extrait du JWT côté serveur via `authenticatedUser().email`, ce qui empêche un utilisateur de contribuer au nom d'un autre. |

### A07:2021 — Cross-Site Scripting (XSS)

| Risque | Ce que j'ai mis en place |
|--------|-----------------|
| **XSS via les entrées** | Mon API retourne uniquement du JSON (pas de HTML), ce qui élimine le vecteur XSS côté serveur. Côté front-end, React assure l'échappement automatique. |

### A02:2021 — Cryptographic Failures

| Risque | Ce que j'ai mis en place |
|--------|-----------------|
| **Exposition de secrets** | Tous les secrets (JWT secret, credentials DB) sont stockés en variables d'environnement. Detekt vérifie l'absence de secrets codés en dur. Le fichier `.env` est dans `.gitignore`. |
| **Communication en clair** | HTTPS forcé en production (Render). SSL activé pour la connexion PostgreSQL (`DB_SSL_MODE=require`). |
| **Algorithme JWT** | HMAC256 — algorithme symétrique recommandé par la RFC 7518 pour les tokens de courte durée. |

### A05:2021 — Security Misconfiguration

| Risque | Ce que j'ai mis en place |
|--------|-----------------|
| **CORS permissif** | J'ai configuré une liste blanche explicite des origines autorisées. Pas de wildcard `*`. |
| **Headers de sécurité** | Ktor configuré avec les headers essentiels. Render ajoute les headers de sécurité au niveau du reverse proxy. |
| **Exposition d'informations** | Les erreurs 500 retournent un message générique. Aucune stack trace, aucun nom de table, aucune requête SQL dans les réponses. |
| **Utilisateur Docker non-root** | Le conteneur s'exécute avec `USER 1000:1000`, ce qui réduit la surface d'attaque. |

### A08:2021 — Software and Data Integrity Failures

| Risque | Ce que j'ai mis en place |
|--------|-----------------|
| **Dépendances compromises** | J'ai fixé les versions dans `gradle/libs.versions.toml` et activé les alertes Dependabot. |
| **Intégrité des données** | Verrou optimiste sur les ressources, contraintes d'intégrité en base (FK, CHECK, UNIQUE), transactions pour chaque opération. |

## 11.3 Catégories OWASP non traitées — Justification

Certaines catégories du OWASP Top 10 n'ont pas nécessité de mesures spécifiques dans mon projet. Voici pourquoi :

| Catégorie | Ma justification |
|-----------|-------------------------------|
| **A04:2021 — Insecure Design** | Mon projet suit une architecture hexagonale avec séparation stricte des responsabilités, validation à chaque couche (endpoint, domaine, base), et utilisation de types forts (Either, value class). Ces choix de conception constituent en eux-mêmes une réponse à cette catégorie. Je n'ai identifié aucun défaut de conception lors de ma veille. |
| **A06:2021 — Vulnerable and Outdated Components** | C'est couvert indirectement par Dependabot (alertes automatiques) et le versionnage strict de mes dépendances dans `gradle/libs.versions.toml` et `package-lock.json`. Mon pipeline Security Audit front-end vérifie quotidiennement la fraîcheur des dépendances. Aucune CVE critique n'a été détectée pendant la durée du projet. |
| **A09:2021 — Security Logging and Monitoring Failures** | J'ai un logging serveur pour les erreurs (stack trace côté serveur, messages génériques côté client), mais je n'ai pas mis en place de système de monitoring dédié (type Grafana/Prometheus). C'est une limitation que j'identifie dans mes perspectives d'évolution (chapitre 12). Pour un projet de cette taille, le logging existant me semble suffisant. |
| **A10:2021 — Server-Side Request Forgery (SSRF)** | Non applicable à mon projet : l'API HappyRow ne fait aucun appel HTTP sortant basé sur des données utilisateur. Toutes les URL sont codées en dur (connexion PostgreSQL, configuration Supabase). Il n'y a aucun mécanisme de fetch d'URL fournie par le client. |

## 11.4 Bilan de la veille

La veille que j'ai menée pendant le projet m'a permis de :

1. **Identifier les risques** avant de coder (le OWASP Top 10 m'a servi de guide de conception)
2. **Appliquer les mesures préventives** dès la phase de développement (requêtes paramétrées, validation des entrées, gestion des secrets)
3. **Vérifier l'absence de CVE critiques** sur mes dépendances principales (Ktor, Exposed, Auth0 JWT)
4. **Documenter mes choix de sécurité** pour la traçabilité et la maintenance future

Aucune vulnérabilité critique n'a été identifiée dans les dépendances que j'utilise pendant la durée du projet. J'ai appliqué les mises à jour mineures régulièrement.
