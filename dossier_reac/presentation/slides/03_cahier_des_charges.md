<!-- Slides 7-8 — Timing: 2 min -->

# Cahier des charges

## Slide 7 — Contexte et objectifs (1 min)

**HappyRow** — Application web collaborative de gestion d'événements

**Problème** : organiser un événement entre plusieurs personnes repose sur des échanges informels (messages, tableurs) — manque de structure et de visibilité

**Solution** : centraliser la coordination dans une application dédiée

### Objectifs fonctionnels

- Créer et gérer des événements (fête, anniversaire, dîner…)
- Inviter des participants et suivre leur statut
- Définir les ressources nécessaires (nourriture, boissons, ustensiles…)
- Permettre aux participants de déclarer leurs contributions
- Gérer les accès concurrents sur les ressources (verrou optimiste)

---

## Slide 8 — Contraintes techniques et livrables (1 min)

### Contraintes techniques

| Contrainte | Choix |
|------------|-------|
| Langage | Kotlin 2.2 / JVM 21 |
| Framework web | Ktor 3.2.2 |
| Base de données | PostgreSQL |
| Authentification | JWT via Supabase (HMAC256) |
| Architecture | Hexagonale (Ports & Adapters) |
| Front-end | React 19 / TypeScript — SPA + PWA |
| Déploiement | Docker + Render (back) / Vercel (front) |
| Qualité | Detekt + Spotless obligatoires en CI |

### Livrables

1. API REST — 15 endpoints, 4 domaines métier
2. Base PostgreSQL — 4 tables, contraintes, index
3. Front-end React 19 — PWA, 8 écrans, 6 modales
4. Pipelines CI/CD — back-end + front-end
5. Tests — unitaires, intégration, E2E
