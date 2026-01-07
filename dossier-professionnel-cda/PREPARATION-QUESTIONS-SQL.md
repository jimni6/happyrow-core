# PRÉPARATION QUESTIONS SQL - SOUTENANCE CDA

## 📊 Vue d'ensemble

**Modèle de données HappyRow** :
- `events` : Événements sportifs
- `participants` : Participants aux événements
- `results` : Résultats/performances
- `teams` : Équipes (futurs sprints)

---

## 🎯 Questions SQL Fréquentes du Jury

### ❓ "Pouvez-vous expliquer votre modèle de données ?"

**Réponse préparée** :
```
Mon application gère des événements sportifs avec 4 entités principales :
- Events : stocke les infos de l'événement (nom, date, lieu, distance)
- Participants : relation N-N entre users et events avec statut
- Results : performances avec temps et rang
- Teams : pour les courses en équipes (prévu phase 2)

Relations :
- Un Event a plusieurs Participants (1-N)
- Un Participant peut avoir plusieurs Results (1-N)
- Un Event peut avoir plusieurs Teams (1-N)
```

---

## 📝 NIVEAU 1 : Requêtes SELECT Simples

### Question 1 : Lister tous les événements à venir

```sql
-- Afficher tous les événements futurs triés par date
SELECT 
  id,
  title,
  event_date,
  location,
  distance_km,
  max_participants
FROM events
WHERE event_date > CURRENT_DATE
ORDER BY event_date ASC;
```

**Points à mentionner** :
- Utilisation de `CURRENT_DATE` pour la date du jour
- Tri chronologique avec `ORDER BY`
- Sélection des colonnes pertinentes uniquement

---

### Question 2 : Compter les participants par événement

```sql
-- Nombre de participants pour chaque événement
SELECT 
  e.title AS "Nom de l'événement",
  COUNT(p.id) AS "Nombre de participants"
FROM events e
LEFT JOIN participants p ON e.id = p.event_id
GROUP BY e.id, e.title
ORDER BY COUNT(p.id) DESC;
```

**Points à mentionner** :
- `LEFT JOIN` pour inclure les événements sans participants
- `GROUP BY` avec toutes les colonnes non agrégées
- Alias avec `AS` pour clarté

---

## 📊 NIVEAU 2 : Jointures et Agrégations

### Question 3 : Liste des participants avec leur statut

```sql
-- Détails des participants pour un événement spécifique
SELECT 
  e.title AS "Événement",
  p.user_id AS "ID Utilisateur",
  p.status AS "Statut",
  p.registered_at AS "Date d'inscription"
FROM participants p
INNER JOIN events e ON p.event_id = e.id
WHERE e.id = 'EVENT_UUID_HERE'
  AND p.status = 'REGISTERED'
ORDER BY p.registered_at ASC;
```

**Points à mentionner** :
- `INNER JOIN` car on veut seulement les participants liés à un événement
- Filtrage multiple avec `WHERE ... AND`
- Format UUID pour les IDs

---

### Question 4 : Événements avec places disponibles

```sql
-- Trouver les événements où il reste de la place
SELECT 
  e.title,
  e.event_date,
  e.max_participants AS "Capacité max",
  COUNT(p.id) AS "Inscrits",
  (e.max_participants - COUNT(p.id)) AS "Places restantes"
FROM events e
LEFT JOIN participants p 
  ON e.id = p.event_id 
  AND p.status = 'REGISTERED'
WHERE e.event_date > CURRENT_DATE
GROUP BY e.id, e.title, e.event_date, e.max_participants
HAVING COUNT(p.id) < e.max_participants
ORDER BY e.event_date ASC;
```

**Points à mentionner** :
- Calcul de places restantes : `max - COUNT()`
- `HAVING` pour filtrer après agrégation (≠ `WHERE`)
- Condition dans le `JOIN` pour ne compter que les REGISTERED

---

## 🔥 NIVEAU 3 : Requêtes Avancées

### Question 5 : Top 3 des meilleurs temps par événement

```sql
-- Podium des meilleurs temps pour chaque événement
WITH ranked_results AS (
  SELECT 
    r.event_id,
    r.participant_id,
    r.finish_time,
    r.ranking,
    e.title AS event_title,
    ROW_NUMBER() OVER (
      PARTITION BY r.event_id 
      ORDER BY r.finish_time ASC
    ) AS position
  FROM results r
  INNER JOIN events e ON r.event_id = e.id
)
SELECT 
  event_title AS "Événement",
  participant_id AS "Participant",
  finish_time AS "Temps",
  ranking AS "Rang",
  position AS "Position"
FROM ranked_results
WHERE position <= 3
ORDER BY event_title, position;
```

**Points à mentionner** :
- CTE (Common Table Expression) avec `WITH`
- Fonction window `ROW_NUMBER()` avec `PARTITION BY`
- Permet de calculer des rangs par groupe
- Alternative : sous-requête corrélée

---

### Question 6 : Moyenne des temps par événement

```sql
-- Statistiques des performances par événement
SELECT 
  e.title AS "Événement",
  e.distance_km AS "Distance (km)",
  COUNT(r.id) AS "Nb résultats",
  MIN(r.finish_time) AS "Meilleur temps",
  MAX(r.finish_time) AS "Temps le plus long",
  AVG(r.finish_time) AS "Temps moyen",
  ROUND(AVG(EXTRACT(EPOCH FROM r.finish_time)) / 60, 2) AS "Moyenne (minutes)"
FROM events e
INNER JOIN results r ON e.id = r.event_id
GROUP BY e.id, e.title, e.distance_km
HAVING COUNT(r.id) >= 3
ORDER BY e.distance_km DESC;
```

**Points à mentionner** :
- Fonctions d'agrégation : `MIN`, `MAX`, `AVG`, `COUNT`
- `EXTRACT(EPOCH FROM interval)` pour convertir en secondes
- `ROUND()` pour arrondir
- `HAVING` pour filtrer les événements avec au moins 3 résultats

---

### Question 7 : Participants n'ayant pas encore de résultats

```sql
-- Trouver les participants qui se sont inscrits mais n'ont pas de résultat
SELECT 
  p.id AS "ID Participant",
  p.user_id AS "ID Utilisateur",
  e.title AS "Événement",
  e.event_date AS "Date",
  p.status AS "Statut"
FROM participants p
INNER JOIN events e ON p.event_id = e.id
LEFT JOIN results r ON p.id = r.participant_id
WHERE r.id IS NULL
  AND e.event_date < CURRENT_DATE
  AND p.status = 'REGISTERED'
ORDER BY e.event_date DESC;
```

**Points à mentionner** :
- `LEFT JOIN` + `WHERE ... IS NULL` pour trouver les absences
- Technique du "anti-join"
- Alternative avec `NOT EXISTS`

---

## 💡 NIVEAU 4 : Requêtes Complexes et Sous-requêtes

### Question 8 : Utilisateurs les plus actifs

```sql
-- Top 5 des utilisateurs avec le plus de participations
SELECT 
  p.user_id AS "ID Utilisateur",
  COUNT(DISTINCT p.event_id) AS "Nb événements",
  COUNT(r.id) AS "Nb résultats",
  ROUND(
    100.0 * COUNT(r.id) / NULLIF(COUNT(DISTINCT p.event_id), 0), 
    2
  ) AS "Taux complétion (%)"
FROM participants p
LEFT JOIN results r ON p.id = r.participant_id
GROUP BY p.user_id
HAVING COUNT(DISTINCT p.event_id) > 0
ORDER BY COUNT(DISTINCT p.event_id) DESC
LIMIT 5;
```

**Points à mentionner** :
- `COUNT(DISTINCT ...)` pour éviter les doublons
- `NULLIF()` pour éviter division par zéro
- Calcul de pourcentage avec `100.0 *` (cast en float)
- `LIMIT` pour pagination

---

### Question 9 : Événements populaires (taux de remplissage > 80%)

```sql
-- Trouver les événements presque complets
SELECT 
  e.title AS "Événement",
  e.event_date AS "Date",
  e.max_participants AS "Capacité",
  COUNT(p.id) AS "Inscrits",
  ROUND(
    100.0 * COUNT(p.id) / e.max_participants, 
    2
  ) AS "Taux remplissage (%)"
FROM events e
LEFT JOIN participants p 
  ON e.id = p.event_id 
  AND p.status = 'REGISTERED'
WHERE e.event_date > CURRENT_DATE
GROUP BY e.id, e.title, e.event_date, e.max_participants
HAVING COUNT(p.id) >= (e.max_participants * 0.8)
ORDER BY (COUNT(p.id)::FLOAT / e.max_participants) DESC;
```

**Points à mentionner** :
- Calcul de pourcentage de remplissage
- Cast avec `::FLOAT` pour division décimale
- Tri par taux de remplissage décroissant

---

### Question 10 : Comparaison de performances entre événements

```sql
-- Comparer les temps d'un utilisateur sur différents événements
WITH user_performances AS (
  SELECT 
    p.user_id,
    e.title AS event_title,
    e.distance_km,
    r.finish_time,
    EXTRACT(EPOCH FROM r.finish_time) / e.distance_km AS pace_per_km
  FROM results r
  INNER JOIN participants p ON r.participant_id = p.id
  INNER JOIN events e ON r.event_id = e.id
  WHERE p.user_id = 'USER_UUID_HERE'
)
SELECT 
  event_title AS "Événement",
  distance_km AS "Distance (km)",
  finish_time AS "Temps",
  ROUND(pace_per_km::numeric, 2) AS "Allure (s/km)"
FROM user_performances
ORDER BY distance_km ASC;
```

**Points à mentionner** :
- CTE pour simplifier la lecture
- Calcul de l'allure (pace) : secondes par km
- Cast `::numeric` pour arrondir proprement

---

## 🔍 NIVEAU 5 : Requêtes Métier Complexes

### Question 11 : Détecter les événements à risque (surréservation)

```sql
-- Alertes : événements avec plus d'inscrits que de places
SELECT 
  e.id,
  e.title AS "Événement à risque",
  e.event_date AS "Date",
  e.max_participants AS "Capacité",
  COUNT(p.id) AS "Inscrits",
  (COUNT(p.id) - e.max_participants) AS "Dépassement"
FROM events e
LEFT JOIN participants p 
  ON e.id = p.event_id 
  AND p.status IN ('REGISTERED', 'CONFIRMED')
WHERE e.event_date > CURRENT_DATE
GROUP BY e.id, e.title, e.event_date, e.max_participants
HAVING COUNT(p.id) > e.max_participants
ORDER BY (COUNT(p.id) - e.max_participants) DESC;
```

**Points à mentionner** :
- Requête business logic pour validation métier
- Plusieurs statuts acceptés avec `IN ()`
- Tri par gravité du problème

---

### Question 12 : Progression d'un participant (évolution des temps)

```sql
-- Voir l'évolution des performances d'un utilisateur
SELECT 
  e.event_date AS "Date",
  e.title AS "Événement",
  e.distance_km AS "Distance",
  r.finish_time AS "Temps",
  LAG(r.finish_time) OVER (
    PARTITION BY p.user_id 
    ORDER BY e.event_date
  ) AS "Temps précédent",
  r.finish_time - LAG(r.finish_time) OVER (
    PARTITION BY p.user_id 
    ORDER BY e.event_date
  ) AS "Différence"
FROM results r
INNER JOIN participants p ON r.participant_id = p.id
INNER JOIN events e ON r.event_id = e.id
WHERE p.user_id = 'USER_UUID_HERE'
ORDER BY e.event_date ASC;
```

**Points à mentionner** :
- Fonction window `LAG()` pour accéder aux valeurs précédentes
- Calcul de progression/régression
- Analyse temporelle des performances

---

## 🎓 Questions Théoriques Fréquentes

### Q1 : "Quelle est la différence entre INNER JOIN et LEFT JOIN ?"

**Réponse** :
- **INNER JOIN** : retourne uniquement les lignes qui ont une correspondance dans les deux tables
- **LEFT JOIN** : retourne toutes les lignes de la table de gauche, même sans correspondance (NULL à droite)

**Exemple concret** :
```sql
-- INNER JOIN : seulement les événements avec participants
SELECT e.title, p.user_id
FROM events e
INNER JOIN participants p ON e.id = p.event_id;
-- Ne retourne QUE les événements qui ont au moins 1 participant

-- LEFT JOIN : tous les événements, avec ou sans participants
SELECT e.title, p.user_id
FROM events e
LEFT JOIN participants p ON e.id = p.event_id;
-- Retourne TOUS les événements (NULL pour user_id si aucun participant)
```

---

### Q2 : "Différence entre WHERE et HAVING ?"

**Réponse** :
- **WHERE** : filtre AVANT l'agrégation (sur les lignes individuelles)
- **HAVING** : filtre APRÈS l'agrégation (sur les résultats de GROUP BY)

**Exemple** :
```sql
-- Mauvais : WHERE COUNT(*) > 5  ❌ ERREUR
-- Bon : HAVING COUNT(*) > 5  ✅

SELECT event_id, COUNT(*) as nb_participants
FROM participants
WHERE status = 'REGISTERED'    -- Filtre avant agrégation
GROUP BY event_id
HAVING COUNT(*) > 10;           -- Filtre après agrégation
```

---

### Q3 : "Qu'est-ce qu'une transaction ? Pourquoi les utiliser ?"

**Réponse** :
Une transaction garantit que plusieurs opérations SQL sont exécutées de manière atomique (tout ou rien).

**Propriétés ACID** :
- **A**tomicity : tout ou rien
- **C**onsistency : respect des contraintes
- **I**solation : transactions indépendantes
- **D**urability : persistance des données

**Exemple dans HappyRow** :
```sql
BEGIN;

-- 1. Créer un participant
INSERT INTO participants (id, user_id, event_id, status)
VALUES (gen_random_uuid(), 'user123', 'event456', 'REGISTERED');

-- 2. Vérifier la capacité
SELECT COUNT(*) FROM participants WHERE event_id = 'event456';

-- Si dépassement, annuler tout
ROLLBACK;
-- Sinon, valider
COMMIT;
```

---

### Q4 : "Pourquoi utiliser des index ? Sur quelles colonnes ?"

**Réponse** :
Les index accélèrent les recherches en créant une structure de données optimisée.

**Dans HappyRow, j'ai créé des index sur** :
```sql
-- Index sur les clés étrangères (JOIN fréquents)
CREATE INDEX idx_participants_event_id ON participants(event_id);
CREATE INDEX idx_participants_user_id ON participants(user_id);
CREATE INDEX idx_results_participant_id ON results(participant_id);

-- Index sur les colonnes de recherche fréquente
CREATE INDEX idx_events_date ON events(event_date);
CREATE INDEX idx_participants_status ON participants(status);
```

**Règle** : créer des index sur les colonnes utilisées dans :
- `WHERE`
- `JOIN ON`
- `ORDER BY`
- `GROUP BY`

---

### Q5 : "Qu'est-ce qu'une clé primaire ? Une clé étrangère ?"

**Réponse** :
- **Clé primaire (PRIMARY KEY)** : identifiant unique de chaque ligne
  - Unique
  - Non NULL
  - Une seule par table
  
- **Clé étrangère (FOREIGN KEY)** : référence vers une clé primaire d'une autre table
  - Assure l'intégrité référentielle
  - Peut être NULL

**Dans mon modèle** :
```sql
CREATE TABLE events (
  id UUID PRIMARY KEY,  -- Clé primaire
  title VARCHAR(255) NOT NULL
);

CREATE TABLE participants (
  id UUID PRIMARY KEY,
  event_id UUID NOT NULL,
  user_id VARCHAR(255) NOT NULL,
  FOREIGN KEY (event_id) REFERENCES events(id)  -- Clé étrangère
);
```

---

## 🎯 Stratégie de Réponse au Jury

### Si on vous demande d'écrire une requête SQL

1. **Comprendre la question** : reformuler à voix haute
2. **Identifier les tables** : quelles entités sont concernées ?
3. **Déterminer les jointures** : quelles relations ?
4. **Préciser les filtres** : WHERE, HAVING ?
5. **Écrire la requête** progressivement (SELECT → FROM → JOIN → WHERE → GROUP BY → HAVING → ORDER BY)
6. **Tester mentalement** avec des données d'exemple

### Structure de réponse idéale

```
"Pour répondre à cette question, j'aurais besoin des tables X et Y.
Je vais faire un [type] JOIN sur [colonne] car [raison].
Ensuite, je filtre avec WHERE pour [condition].
Enfin, je trie/agrège avec [ORDER BY/GROUP BY]."

[Écrire la requête au tableau]
```

---

## 📚 Ressources SQL Utiles

### Ordre d'exécution SQL (important !)
```
1. FROM       -- Sélection des tables
2. JOIN       -- Jointures
3. WHERE      -- Filtres sur lignes
4. GROUP BY   -- Agrégation
5. HAVING     -- Filtres sur agrégats
6. SELECT     -- Sélection des colonnes
7. ORDER BY   -- Tri
8. LIMIT      -- Pagination
```

### Fonctions d'agrégation courantes
```sql
COUNT(*)           -- Compte toutes les lignes
COUNT(colonne)     -- Compte les valeurs non-NULL
SUM(colonne)       -- Somme
AVG(colonne)       -- Moyenne
MIN(colonne)       -- Minimum
MAX(colonne)       -- Maximum
```

### Opérateurs utiles
```sql
=, !=, <, >, <=, >=    -- Comparaison
BETWEEN x AND y        -- Entre deux valeurs
IN (liste)             -- Dans une liste
LIKE 'pattern%'        -- Recherche textuelle
IS NULL / IS NOT NULL  -- Test NULL
AND, OR, NOT           -- Logique
```

---

## ✅ Checklist Préparation SQL

- [ ] Relire le schéma de base de données
- [ ] Comprendre toutes les relations (1-N, N-N)
- [ ] Mémoriser les noms de tables et colonnes principales
- [ ] Pratiquer 3-4 requêtes complexes
- [ ] Savoir expliquer les index créés
- [ ] Connaître la différence WHERE/HAVING
- [ ] Maîtriser INNER JOIN vs LEFT JOIN
- [ ] Comprendre les transactions ACID
- [ ] Savoir calculer des agrégations (COUNT, AVG, SUM)
- [ ] Être capable d'écrire une requête au tableau

---

**Bon courage pour votre soutenance ! 💪**
