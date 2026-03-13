<!-- Slides 49-50 — Timing: 2 min -->

# Synthèse et conclusion

## Slide 49 — Bilan et satisfactions (1 min)

### Chiffres clés

| | Back-end | Front-end |
|---|----------|-----------|
| **Endpoints / Écrans** | 15 endpoints REST | 8 écrans + 6 modales |
| **Code** | ~5 000 lignes Kotlin | React 19 / TypeScript |
| **Tests** | Kotest + MockK + Testcontainers | 46 tests Vitest + 6 E2E Playwright |
| **CI/CD** | Detekt → Tests → Render | ESLint → Tests → Docker → Vercel |
| **Dépendances** | Kotlin, Ktor, Exposed, Arrow | 3 deps prod (React, Router, Supabase) |

### Satisfactions

- **Architecture hexagonale** : testabilité, évolutivité, lisibilité
- **Arrow Either** : gestion fonctionnelle des erreurs, composabilité, traçabilité
- **Verrou optimiste** : gestion d'un problème de concurrence réel
- **Clean Architecture front-end** : cohérence architecturale back/front
- **CI/CD complet** : filet de sécurité à chaque push

---

## Slide 50 — Difficultés et perspectives (1 min)

### Difficultés rencontrées

- **Concurrence** : propager la `version` à travers toutes les couches (Repository → UseCase → Endpoint → 409)
- **Suppression en cascade** : ordre imposé par les FK (contributions → ressources → participants → événement)
- **SSL Raspberry Pi** : tunnel Cloudflare + certificats PostgreSQL → centralisation des secrets en env vars

### Perspectives d'évolution

| Évolution | Description |
|-----------|-------------|
| Application mobile | Client natif ou cross-platform |
| Notifications temps réel | WebSocket ou SSE |
| NoSQL | Logs d'activité, historique contributions |
| Tests intégration en CI | Testcontainers dans GitHub Actions |
| Monitoring | Micrometer + Grafana |

---

**Merci pour votre attention.**

*Je suis disponible pour vos questions.*
