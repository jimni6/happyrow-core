# Ordre d'execution des issues d'audit

> Source : `AUDIT_HAPPYROW_CORE.md` — 22 mars 2026

## Phase 0 — Quick wins (1 jour)

| Ordre | Issue | Temps | Statut |
|---|---|---|---|
| 1 | #45 — Endpoint `/health` manquant | 10 min | A faire |
| 2 | #50 — Credentials loguees en clair | 5 min | A faire |
| 3 | #52 — JWT verifier cache | 10 min | A faire |
| 4 | #48 — Version default incoherent | 5 min | A faire |
| 5 | #59 — /info disclosure | 5 min | A faire |

## Phase 1 — Securite critique (3-5 jours)

| Ordre | Issue | Temps | Depend de |
|---|---|---|---|
| 6 | #49 — CreateParticipant auth ignoree | 0.5j | — |
| 7 | #51 — IDOR sur 4 endpoints | 1j | #49 |
| 8 | #53 — Bypass verrou optimiste | 0.5j | — |
| 9 | #54 — Quantite negative | 0.5j | — |
| 10 | #61 — Transactions atomiques | 1j | #53, #54 |
| 11 | #56 — CORS production | 0.5j | — |

## Phase 2 — Outillage et nettoyage (2-3 jours)

| Ordre | Issue | Temps | Depend de |
|---|---|---|---|
| 12 | #46 — Code mort | 0.5j | — |
| 13 | #60 — Migration tool (Flyway/Liquibase) | 1.5j | — |
| 14 | #47 — Schema init (resolu par #60) | 0 | #60 |

## Phase 3 — User Management (5-7 jours)

| Ordre | Issue | Temps | Depend de |
|---|---|---|---|
| 15 | #66 — Table `app_user` | 1j | #60 |
| 16 | #67 — Creator -> UUID (resout #44) | 1j | #66 |
| 17 | #68 — Participant -> userId | 1.5j | #67 |
| 18 | #69 — Contribution -> userId | 1j | #68 |
| 19 | #70 — Endpoints userId | 1.5j | #69, #51 |

## Phase 4 — Backlog

| Ordre | Issue | Temps |
|---|---|---|
| 20 | #63 — Validation domaine | 1j |
| 21 | #57 — Date future | 0.5j |
| 22 | #58 — Limite quantites | 0.5j |
| 23 | #55 — Rate limiting | 1j |
| 24 | #65 — Format erreurs RFC7807 | 1j |
| 25 | #62 — Pagination | 1.5j |
| 26 | #64 — Tests unitaires (12 use cases) | 3-4j |

## Total : ~12-17 jours pour 27 issues
