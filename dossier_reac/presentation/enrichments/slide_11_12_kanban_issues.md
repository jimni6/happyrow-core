# Slides 11 et 12 — Kanban / GitHub Issues + Projects (screenshots)

> **Type** : CREATION — Il faut creer les issues et le project board sur GitHub avant de prendre les screenshots.

## Etat actuel

- Le repo `happyrow-core` a 10 issues (Phase 1 fondations), toutes fermees
- Aucun GitHub Project n'existe
- Il faut creer des issues supplementaires couvrant les 5 phases, puis un Project Board Kanban

## Etape 1 : Creer les issues supplementaires

Executer ces commandes `gh` depuis le repertoire du projet `happyrow-core` :

```bash
# ===== PHASE 2 : Evenements =====
gh issue create --title "Modele domaine Event (data class + Creator)" --body "Creer les entites Event, EventType et Creator dans le module domain." --label "enhancement"
gh issue create --title "EventRepository : interface port du domaine" --body "Definir le port EventRepository avec les operations CRUD." --label "enhancement"
gh issue create --title "SqlEventRepository : implementation Exposed" --body "Implementer le repository SQL avec Exposed et PostgreSQL." --label "enhancement"
gh issue create --title "CreateEventUseCase + auto-ajout createur" --body "Use case de creation d'evenement avec ajout automatique du createur comme participant confirme." --label "enhancement"
gh issue create --title "Endpoints REST Evenements (CRUD complet)" --body "Creer les 4 endpoints REST pour les evenements : POST, GET, PUT, DELETE." --label "enhancement"
gh issue create --title "Authentification JWT Supabase (plugin Ktor)" --body "Implementer le plugin d'authentification JWT avec verification HMAC256." --label "enhancement"

# ===== PHASE 3 : Participants =====
gh issue create --title "Modele domaine Participant + ParticipantStatus" --body "Data class Participant avec enum de statuts (PENDING, CONFIRMED, DECLINED)." --label "enhancement"
gh issue create --title "ParticipantRepository et SqlParticipantRepository" --body "Port + implementation SQL pour la gestion des participants." --label "enhancement"
gh issue create --title "Endpoints REST Participants (ajout, liste, mise a jour)" --body "Endpoints pour ajouter, lister et modifier le statut des participants." --label "enhancement"

# ===== PHASE 4 : Ressources et Contributions =====
gh issue create --title "Modele domaine Resource + ResourceCategory" --body "Data class Resource avec champ version pour le verrou optimiste." --label "enhancement"
gh issue create --title "Modele domaine Contribution" --body "Data class Contribution reliant un participant a une ressource." --label "enhancement"
gh issue create --title "ResourceRepository + verrou optimiste (updateQuantity)" --body "Interface et implementation SQL avec UPDATE conditionnel sur la version." --label "enhancement"
gh issue create --title "ContributionRepository : addOrUpdate, reduce, delete" --body "Repository complet avec gestion du delta de quantite et mise a jour atomique." --label "enhancement"
gh issue create --title "AddContributionUseCase" --body "Use case d'ajout de contribution avec orchestration participant + contribution." --label "enhancement"
gh issue create --title "ReduceContributionUseCase + DeleteContributionUseCase" --body "Use cases de reduction et suppression de contributions." --label "enhancement"
gh issue create --title "Endpoints REST Contributions (add, reduce, delete)" --body "3 endpoints pour les operations de contribution avec gestion du 409 Conflict." --label "enhancement"
gh issue create --title "Gestion du 409 Conflict (OptimisticLockException)" --body "Remonter la cause racine pour distinguer conflit de concurrence et erreur technique." --label "bug"

# ===== PHASE 5 : Qualite et Deploiement =====
gh issue create --title "Tests unitaires use cases (Kotest + MockK)" --body "Tests BDD pour CreateEventUseCase, AddContributionUseCase et les autres." --label "enhancement"
gh issue create --title "Tests d'integration (Testcontainers + PostgreSQL)" --body "Tests des repositories avec une vraie base PostgreSQL en conteneur." --label "enhancement"
gh issue create --title "Deploiement Docker multi-stage sur Render" --body "Dockerfile multi-stage avec image JRE minimale et utilisateur non-root." --label "enhancement"
gh issue create --title "Pipeline CI/CD back-end (Detekt + Tests + Deploy)" --body "GitHub Actions avec analyse statique, tests et deploiement automatique." --label "enhancement"

# ===== Perspectives (To Do) =====
gh issue create --title "Notifications temps reel (WebSocket)" --body "Ajouter des notifications push quand un participant contribue a un evenement." --label "enhancement"
gh issue create --title "Application mobile (React Native ou Kotlin Multiplatform)" --body "Porter l'application sur mobile pour une meilleure accessibilite." --label "enhancement"
gh issue create --title "Monitoring applicatif (Micrometer + Grafana)" --body "Ajouter des metriques et un dashboard de monitoring en production." --label "enhancement"
```

## Etape 2 : Fermer les issues des phases 2-5

```bash
# Fermer les issues de Phase 2 (numeros a adapter selon la creation)
# Les numeros commencent apres les 11 existantes, donc a partir de #12
for i in $(seq 12 32); do
  gh issue close $i --comment "Done - Implemente dans le cadre du projet HappyRow." 2>/dev/null
done
```

> **Note** : Garder les 3 dernieres issues (Perspectives) ouvertes pour avoir du contenu dans "To Do".

## Etape 3 : Creer le GitHub Project Board

```bash
# Creer un project (type board) au niveau utilisateur
gh project create --title "HappyRow - Kanban" --owner "@me"

# Lister pour recuperer le numero du project
gh project list --owner "@me"
```

> Apres creation, noter le numero du project (ex: 1).

## Etape 4 : Ajouter les issues au project

```bash
PROJECT_NUMBER=1  # Adapter selon le numero obtenu

# Ajouter toutes les issues au project
for i in $(seq 1 35); do
  gh project item-add $PROJECT_NUMBER --owner "@me" --url "https://github.com/jimni6/happyrow-core/issues/$i" 2>/dev/null
done
```

## Etape 5 : Organiser les colonnes

Dans l'interface GitHub Projects (https://github.com/users/jimni6/projects/) :

1. Ouvrir le project "HappyRow - Kanban"
2. Passer en vue **Board**
3. Les colonnes par defaut sont "Todo", "In Progress", "Done"
4. Deplacer manuellement :
   - **Done** : toutes les issues fermees (Phases 1 a 5)
   - **In Progress** : 1-2 issues pour le rendu visuel (ex: "Tests d'integration", "Monitoring")
   - **To Do** : les 3 issues de perspectives (notifications, mobile, monitoring)

## Etape 6 : Prendre les screenshots

1. **Screenshot 1 (slide 11)** : Vue Board complete montrant les 3 colonnes avec des issues reparties
2. **Screenshot 2 (slide 12)** : Vue Board filtree ou zoomee sur les colonnes pour montrer les phases

## Ce qu'il faut dire (notes orales)

**Slide 11** : "Voici mon tableau Kanban GitHub Projects. On voit les trois colonnes classiques : To Do pour les taches planifiees, In Progress pour le travail en cours, et Done pour les taches terminees. Chaque ticket correspond a une fonctionnalite, un correctif ou une tache technique. L'approche Kanban me permet de limiter le travail en cours et de livrer de maniere incrementale."

**Slide 12** : "Le projet a ete decoupe en 5 phases. La premiere pose les fondations techniques. Les phases 2 et 3 couvrent les evenements et les participants. La phase 4 introduit les ressources et les contributions avec le verrou optimiste. La phase 5 consolide l'ensemble avec les tests et le deploiement. On voit ici que la grande majorite des taches sont terminees, et il reste quelques perspectives d'evolution."
