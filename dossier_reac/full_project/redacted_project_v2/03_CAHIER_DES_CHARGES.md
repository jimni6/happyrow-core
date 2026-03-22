# 3. Cahier des charges — Expression des besoins

## 3.1 Contexte du projet

HappyRow est une application web collaborative que j'ai conçue pour faciliter l'organisation d'événements entre amis, collègues ou en famille. L'idée est simple : quand on prépare une fête, un anniversaire ou un dîner à plusieurs, on se retrouve souvent à jongler entre les messages, les tableurs partagés et les listes improvisées. Ça manque de structure et de visibilité.

J'ai voulu créer un outil dédié qui centralise toute cette coordination : qui vient, qui apporte quoi, combien il en faut. HappyRow répond à ce besoin en proposant une plateforme où chaque participant peut voir les ressources nécessaires et déclarer sa contribution.

## 3.2 Objectifs

Voici les objectifs que je me suis fixés pour ce projet :

- Permettre à un utilisateur authentifié de créer et gérer ses événements
- Permettre d'inviter des participants et de suivre leur statut (confirmé, en attente, décliné)
- Permettre de définir les ressources nécessaires à un événement (avec catégorie et quantité suggérée)
- Permettre aux participants de déclarer leurs contributions sur les ressources
- Assurer la cohérence des données quand plusieurs participants contribuent en même temps (accès concurrents)
- Sécuriser l'accès via une authentification JWT

## 3.3 Périmètre fonctionnel

### Gestion des événements

| Fonctionnalité | Description |
|---|---|
| Créer un événement | Nom, description, date, lieu, type (PARTY, BIRTHDAY, DINER, SNACK), membres invités. Le créateur est automatiquement ajouté comme participant confirmé. |
| Consulter ses événements | Liste des événements dont l'utilisateur est organisateur. |
| Modifier un événement | Mise à jour des informations (nom, date, lieu, type, membres). |
| Supprimer un événement | Réservé au créateur. Suppression en cascade de tous les participants, ressources et contributions associés. |

### Gestion des participants

| Fonctionnalité | Description |
|---|---|
| Ajouter un participant | Ajout par email avec un statut initial (INVITED, CONFIRMED). |
| Consulter les participants | Liste des participants d'un événement avec leur statut. |
| Modifier le statut | Passage entre INVITED, CONFIRMED, DECLINED, MAYBE. |

### Gestion des ressources

| Fonctionnalité | Description |
|---|---|
| Créer une ressource | Nom, catégorie (FOOD, DRINK, UTENSIL, DECORATION, OTHER), quantité suggérée. |
| Consulter les ressources | Liste des ressources d'un événement avec la quantité courante et les contributeurs. |

### Gestion des contributions

| Fonctionnalité | Description |
|---|---|
| Ajouter / modifier une contribution | Un participant déclare une quantité pour une ressource. Si une contribution existe déjà, elle est mise à jour. La quantité courante de la ressource est ajustée via un **verrou optimiste**. |
| Réduire une contribution | Diminution de la quantité. Si elle atteint 0, la contribution est supprimée. |
| Supprimer une contribution | Retrait complet d'une contribution. |

## 3.4 Contraintes techniques

| Contrainte | Détail |
|---|---|
| Langage | Kotlin (JVM) — choix imposé par l'entreprise |
| Framework | Ktor — framework web asynchrone léger |
| Base de données | PostgreSQL — SGBD relationnel |
| Authentification | JWT via Supabase (service d'authentification externe) |
| Déploiement | Docker + Render (PaaS) |
| Architecture | Hexagonale (ports & adapters) |
| Qualité | Analyse statique obligatoire (Detekt), formatage (Spotless) |

## 3.5 Livrables attendus

1. **API REST** fonctionnelle avec 15 endpoints couvrant les 4 domaines métier
2. **Base de données** PostgreSQL avec schéma complet et contraintes d'intégrité
3. **Pipeline CI/CD** automatisé (analyse, tests, déploiement)
4. **Documentation technique** (architecture, API, déploiement)
5. **Tests** unitaires et d'intégration
6. **Application front-end** consommant l'API (repo `happyrow-front`)
