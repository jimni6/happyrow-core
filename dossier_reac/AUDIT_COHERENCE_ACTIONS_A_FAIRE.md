# Actions a faire plus tard

Ce document regroupe les ecarts releves pendant l'audit de coherence entre le rapport d'alternance et les depots `happyrow-core` / `happyrow-front`.

Objectif: garder une liste claire des corrections a traiter plus tard, soit dans le rapport, soit dans le code, soit dans les deux.

## Priorite haute

- [ ] Aligner le healthcheck backend.
  Action:
  - soit ajouter un endpoint public `GET /health` dans `happyrow-core`
  - soit corriger le rapport et les configurations qui supposent son existence
  Fichiers concernes:
  - `src/main/kotlin/com/happyrow/core/Routing.kt`
  - `render.yaml`
  - `.raspberry/docker-compose.yml`
  - `dossier_reac/full_project/redacted_project_v2/07_SPECIFICATIONS_TECHNIQUES.md`

- [ ] Corriger dans le rapport l'affirmation "aucune requete SQL brute".
  Action:
  - remplacer par une formulation plus precise: Exposed est utilise pour les operations metier, mais du SQL brut existe pour l'initialisation et certaines migrations
  Fichiers/preuves:
  - `infrastructure/src/main/kotlin/com/happyrow/core/infrastructure/technical/config/DatabaseInitializer.kt`
  - `init-db.sql`

- [ ] Corriger la partie base de donnees qui parle de cles etrangeres `CASCADE`.
  Action:
  - remplacer par une formulation qui explique que la suppression en cascade est geree applicativement dans `SqlEventRepository`
  - verifier si tu veux vraiment implementer du `ON DELETE CASCADE` en base plus tard
  Fichiers/preuves:
  - `infrastructure/src/main/kotlin/com/happyrow/core/infrastructure/common/driven/event/SqlEventRepository.kt`
  - `infrastructure/src/main/kotlin/com/happyrow/core/infrastructure/event/common/driven/event/EventTable.kt`
  - `infrastructure/src/main/kotlin/com/happyrow/core/infrastructure/participant/common/driven/ParticipantTable.kt`
  - `infrastructure/src/main/kotlin/com/happyrow/core/infrastructure/resource/common/driven/ResourceTable.kt`
  - `infrastructure/src/main/kotlin/com/happyrow/core/infrastructure/contribution/common/driven/ContributionTable.kt`

- [ ] Corriger le nombre d'endpoints annonces dans le rapport.
  Action:
  - remplacer "15 endpoints couvrant les 4 domaines" par une formulation exacte
  - version conseillee: "13 endpoints metier couvrant les 4 domaines, plus 2 routes utilitaires (`/` et `/info`)"
  Fichiers/preuves:
  - `src/main/kotlin/com/happyrow/core/Routing.kt`
  - `infrastructure/src/main/kotlin/com/happyrow/core/infrastructure/**/*Endpoint.kt`

- [ ] Corriger le nombre de use cases annonces.
  Action:
  - remplacer "12 use cases" par un chiffre aligne avec l'etat actuel du code
  - verifier si tu veux compter seulement les use cases cables, ou aussi toutes les classes `*UseCase.kt`
  Fichiers/preuves:
  - `src/main/kotlin/com/happyrow/core/modules/UseCaseModule.kt`
  - `domain/src/main/kotlin/com/happyrow/core/domain/**/*UseCase.kt`

## Priorite moyenne

- [ ] Corriger l'affirmation "PostgreSQL en local via `docker-compose.yml`".
  Action:
  - si tu parles du fichier racine, c'est faux aujourd'hui
  - si tu parles du setup Raspberry, cite plutot `.raspberry/docker-compose.yml`
  Fichiers/preuves:
  - `docker-compose.yml`
  - `.raspberry/docker-compose.yml`

- [ ] Corriger la variable d'environnement CORS citee dans le rapport.
  Action:
  - remplacer `CORS_ALLOWED_ORIGINS` par `ALLOWED_ORIGINS`
  Fichiers/preuves:
  - `src/main/kotlin/com/happyrow/core/Application.kt`
  - `dossier_reac/full_project/redacted_project_v2/05_GESTION_DE_PROJET.md`

- [ ] Corriger la phrase sur les headers de securite Ktor.
  Action:
  - soit retirer la phrase
  - soit ajouter une vraie configuration de headers de securite dans le backend plus tard
  Fichiers/preuves:
  - `src/main/kotlin/com/happyrow/core/Application.kt`

- [ ] Corriger le nombre de tests unitaires front annonces.
  Action:
  - remplacer `46` par le nombre actuellement present dans `happyrow-front`
  - dernier comptage realise pendant l'audit: `54`
  Fichiers/preuves:
  - `/Users/j.ni/IdeaProjects/happyrow-front/tests/features/auth/services/SupabaseAuthRepository.test.ts`
  - `/Users/j.ni/IdeaProjects/happyrow-front/tests/features/auth/views/AuthView.test.tsx`
  - `/Users/j.ni/IdeaProjects/happyrow-front/tests/features/auth/components/ForgotPasswordForm.test.tsx`
  - `/Users/j.ni/IdeaProjects/happyrow-front/tests/features/auth/hooks/AuthProvider.test.tsx`

- [ ] Corriger le nombre de dependances de production front.
  Action:
  - remplacer "3 dependances de production" par `4 packages npm`
  - alternative: reformuler en termes de "3 briques fonctionnelles" si tu veux garder l'idee de sobriete
  Fichiers/preuves:
  - `/Users/j.ni/IdeaProjects/happyrow-front/package.json`

## Priorite basse

- [ ] Revoir la formulation sur les "8 vues/ecrans".
  Action:
  - verifier si tu veux compter les vues existantes dans le code, les vues routees, ou les ecrans UX au sens large
  - la formulation actuelle parait plausible, mais pas strictement demontree par le routage actif
  Fichiers/preuves:
  - `/Users/j.ni/IdeaProjects/happyrow-front/src/App.tsx`
  - `/Users/j.ni/IdeaProjects/happyrow-front/src/features/**/*View.tsx`
  - `/Users/j.ni/IdeaProjects/happyrow-front/src/features/**/*Page.tsx`

- [ ] Revoir la formulation sur les "6 modales".
  Action:
  - ce chiffre est defendable, mais il vaut mieux lister explicitement les modales si tu veux eviter toute ambiguite
  Fichiers/preuves:
  - `/Users/j.ni/IdeaProjects/happyrow-front/src/features/auth/components/LoginModal.tsx`
  - `/Users/j.ni/IdeaProjects/happyrow-front/src/features/auth/components/RegisterModal.tsx`
  - `/Users/j.ni/IdeaProjects/happyrow-front/src/features/participants/components/AddParticipantModal.tsx`
  - `/Users/j.ni/IdeaProjects/happyrow-front/src/features/events/components/ConfirmDeleteModal.tsx`
  - `/Users/j.ni/IdeaProjects/happyrow-front/src/layouts/AppLayout/AppLayout.tsx`
  - `/Users/j.ni/IdeaProjects/happyrow-front/src/features/events/views/EventDetailsView.tsx`

- [ ] Revoir les affirmations non directement verifiables depuis le code seul.
  Action:
  - Dependabot
  - GitHub Projects / Kanban
  - 1Password CLI / secrets d'environnement
  - certains details de process DevOps
  Note:
  - ces points ne sont pas necessairement faux, mais ils ne sont pas tous prouvables depuis les depots seuls

## Points confirmes a ne pas corriger inutilement

- [ ] Conserver les affirmations justes sur la stack backend:
  - Kotlin / JDK 21 / Gradle multi-modules
  - Ktor / Koin / Exposed / JWT Supabase
  - Docker / Render / Detekt / Spotless

- [ ] Conserver les affirmations justes sur l'architecture backend:
  - separation `domain` / `infrastructure`
  - repositories SQL
  - use cases metier
  - auto-ajout du createur comme participant confirme
  - verrou optimiste sur `Resource.version`

- [ ] Conserver les affirmations justes sur le front:
  - React 19 / TypeScript / Vite
  - Supabase auth
  - `fetch` natif
  - PWA avec Workbox
  - tests Vitest + Playwright
  - deploiement Vercel

## Decisions a prendre plus tard

- [ ] Choisir entre correction documentaire ou correction code pour `/health`.

- [ ] Decider si tu veux garder la suppression en cascade applicative, ou ajouter de vraies contraintes `CASCADE` en base.

- [ ] Decider si tu veux figer les chiffres dans le rapport ou adopter des formulations plus robustes:
  - "plusieurs use cases"
  - "plus de 50 tests unitaires front"
  - "une dizaine d'endpoints metier"

- [ ] Si tu veux garder des chiffres exacts, refaire un dernier comptage juste avant la version finale du rapport.

## Resume ultra court

Les points les plus importants a traiter plus tard sont:

- [ ] `/health` incoherent entre le rapport, `render.yaml` et le code
- [ ] "aucune requete SQL brute" faux
- [ ] "cles etrangeres CASCADE" faux ou au minimum trompeur
- [ ] chiffres obsoletes sur endpoints, use cases, tests front et dependances front
