# 11. Veille sur les vulnérabilités de sécurité

Ce chapitre décrit la veille sécurité effectuée pendant le développement du projet HappyRow, les vulnérabilités identifiées et les mesures correctives appliquées.

## 11.1 Démarche de veille

### Sources consultées

| Source | Type | Fréquence |
|--------|------|-----------|
| **OWASP Top 10** (2021) | Référentiel de vulnérabilités web | Consulté en début de projet pour établir la stratégie de sécurité |
| **CVE Database** (cve.org) | Base de données de vulnérabilités connues | Vérification ponctuelle sur les dépendances critiques |
| **Advisories GitHub** (Dependabot) | Alertes automatiques sur les dépendances | En continu (notifications automatiques) |
| **Documentation Ktor Security** | Bonnes pratiques framework | Lors de l'implémentation de l'authentification |
| **Documentation Supabase Auth** | Sécurité JWT, bonnes pratiques | Lors de l'intégration de l'authentification |
| **ANSSI — Recommandations** | Guides de sécurité des systèmes d'information | Consulté pour les principes généraux |

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

## 11.3 Bilan de la veille

La veille effectuée pendant le projet a permis de :

1. **Identifier les risques** avant l'implémentation (OWASP Top 10 comme guide de conception)
2. **Appliquer les mesures préventives** dès la phase de développement (requêtes paramétrées, validation des entrées, gestion des secrets)
3. **Vérifier l'absence de CVE critiques** sur les dépendances principales (Ktor, Exposed, Auth0 JWT)
4. **Documenter les choix de sécurité** pour la traçabilité et la maintenance

Aucune vulnérabilité critique n'a été identifiée dans les dépendances utilisées pendant la durée du projet. Les mises à jour mineures ont été appliquées régulièrement.
