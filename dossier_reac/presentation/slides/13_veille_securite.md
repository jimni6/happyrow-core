<!-- Slides 47-48 — Timing: 2 min -->

# Veille sur les vulnérabilités de sécurité

## Slide 47 — Démarche et chronologie (1 min)

| Période | Action | Résultat |
|---------|--------|----------|
| **Phase 1** — Fondations | Étude OWASP Top 10 (2021) | 6 catégories pertinentes identifiées |
| **Phase 2** — Authentification | Docs Ktor Security + Supabase Auth, vérification CVE auth0 java-jwt | Aucune CVE critique, HMAC256 validé (RFC 7518) |
| **Phase 3** — Participants | Activation Dependabot, vérification advisories Exposed ORM | Alertes automatiques configurées |
| **Phase 4** — Contributions | Recherche vulnérabilités concurrence (TOCTOU), docs PostgreSQL isolation | Verrou optimiste avec double vérification |
| **Phase 5** — Qualité | Audit npm, workflow Security Audit quotidien (cron GitHub Actions) | Pipeline sécurité front-end automatisé |
| **En continu** | Surveillance Dependabot, mises à jour Ktor/Supabase | Mises à jour mineures appliquées |

### Sources consultées

OWASP Top 10 | CVE Database | GitHub Dependabot | Docs Ktor/Supabase | ANSSI | PostgreSQL Security

---

## Slide 48 — OWASP Top 10 : couverture (1 min)

### Catégories traitées

| Catégorie | Protection appliquée |
|-----------|---------------------|
| **A01 — Broken Access Control** | JWT obligatoire, autorisation créateur, email du JWT serveur |
| **A02 — Cryptographic Failures** | HMAC256, HTTPS, SSL PostgreSQL, secrets en env vars |
| **A03 — Injection** | Exposed ORM, requêtes paramétrées, pas de SQL brut |
| **A05 — Security Misconfiguration** | CORS whitelist, Docker non-root, erreurs génériques |
| **A07 — XSS** | API JSON only, échappement React |
| **A08 — Data Integrity** | Dependabot, versions fixées, verrou optimiste, FK/CHECK |

### Catégories non traitées (justification)

| Catégorie | Raison |
|-----------|--------|
| **A04 — Insecure Design** | Couvert implicitement : architecture hexagonale, validation multi-couche |
| **A06 — Vulnerable Components** | Couvert par Dependabot + versionnage strict |
| **A09 — Logging Failures** | Monitoring identifié comme perspective d'évolution |
| **A10 — SSRF** | Non applicable : aucun appel HTTP sortant basé sur des données utilisateur |

**Aucune CVE critique identifiée** sur les dépendances pendant la durée du projet.
