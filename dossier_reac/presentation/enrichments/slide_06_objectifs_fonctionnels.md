# Slide 6 — Les objectifs fonctionnels (schema)

> **Type** : CREATION — Ce diagramme n'existait pas, il a ete cree pour visualiser les objectifs fonctionnels.

## Diagramme a inserer dans la slide

```mermaid
flowchart TB
    subgraph HappyRow ["HappyRow — Perimetre fonctionnel"]
        direction TB

        subgraph Events ["Evenements"]
            E1["Creer un evenement"]
            E2["Consulter ses evenements"]
            E3["Modifier un evenement"]
            E4["Supprimer un evenement"]
        end

        subgraph Participants ["Participants"]
            P1["Inviter un participant"]
            P2["Suivre le statut"]
            P3["Auto-ajout du createur"]
        end

        subgraph Resources ["Ressources"]
            R1["Definir les ressources necessaires"]
            R2["Categorie + quantite suggeree"]
        end

        subgraph Contributions ["Contributions"]
            C1["Declarer sa contribution"]
            C2["Reduire ou supprimer"]
            C3["Verrou optimiste"]
        end
    end

    User(["Utilisateur authentifie"]) --> Events
    User --> Participants
    User --> Resources
    User --> Contributions

    E1 -.->|"inclut"| P3
    C1 & C2 -.->|"protege par"| C3
```

## Ce qu'il faut dire (notes orales)

Le perimetre fonctionnel de HappyRow s'organise autour de 4 domaines. L'utilisateur authentifie peut :

1. **Evenements** : gerer le cycle de vie complet (CRUD)
2. **Participants** : inviter des personnes et suivre leur statut (le createur est auto-ajoute comme participant confirme)
3. **Ressources** : definir ce qui est necessaire a l'evenement, avec une categorie et une quantite suggeree
4. **Contributions** : declarer ce que chacun apporte, avec un mecanisme de verrou optimiste pour gerer les acces concurrents

Ce schema montre deux regles metier importantes :
- La creation d'un evenement **inclut** automatiquement l'ajout du createur comme participant
- Les operations de contribution sont **protegees** par le verrou optimiste
