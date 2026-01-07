# SLIDE 5 : MODÈLE DE DONNÉES

---

## 📊 Schéma Entité-Association

```
┌─────────────────────┐
│      EVENT          │
├─────────────────────┤
│ • id (UUID)         │
│ • name              │
│ • description       │
│ • date              │◄──────────┐
│ • location          │           │
│ • type              │           │ 1
│ • organizer_id      │           │
└──────┬──────────────┘           │
       │                          │
       │ 1                        │
       │                          │
       │ *                        │
┌──────▼──────────────┐           │
│   PARTICIPANT       │           │
├─────────────────────┤           │
│ • id (UUID)         │           │
│ • event_id (FK)     ├───────────┘
│ • name              │
│ • email             │
│ • status            │
└──────┬──────────────┘
       │
       │ 1
       │
       │ *
┌──────▼──────────────┐     *     ┌─────────────────────┐
│   CONTRIBUTION      ├───────────►│     RESOURCE        │
├─────────────────────┤      1     ├─────────────────────┤
│ • id (UUID)         │            │ • id (UUID)         │
│ • participant_id(FK)│            │ • event_id (FK)     │
│ • resource_id (FK)  │            │ • name              │
│ • quantity_promised │            │ • quantity_needed   │
└─────────────────────┘            │ • unit              │
                                   └──────▲──────────────┘
                                          │
                                          │ *
                                          │
                                          │ 1
                                    ┌─────┴────────────┐
                                    │    EVENT         │
                                    └──────────────────┘
```

---

## 🎯 Les 4 Entités Principales

### 📅 **EVENT** (Événement)
- Informations de base : nom, description, date, lieu
- Type d'événement : PARTY, BIRTHDAY, DINER, SNACK
- Identifiant de l'organisateur

### 👤 **PARTICIPANT** 
- Personne invitée à un événement
- Nom, email
- Statut : PENDING, ACCEPTED, DECLINED

### 📦 **RESOURCE** (Ressource nécessaire)
- Ce qu'il faut apporter (ex: "Bouteilles de vin")
- Quantité attendue et unité (ex: "3 unités")
- Liée à un événement spécifique

### 🤝 **CONTRIBUTION**
- Lien entre un participant et une ressource
- Engagement : "Je m'engage à apporter X de Y"
- Quantité promise par le participant

---

## 🔗 Relations

| Relation | Cardinalité | Description |
|----------|-------------|-------------|
| Event → Participant | **1..*** | Un événement a plusieurs participants |
| Event → Resource | **1..*** | Un événement a plusieurs ressources |
| Participant → Contribution | **1..*** | Un participant peut faire plusieurs contributions |
| Resource → Contribution | **1..*** | Une ressource peut être promise par plusieurs participants |
| Contribution → Participant | ***..1** | Une contribution appartient à un participant |
| Contribution → Resource | ***..1** | Une contribution concerne une ressource |

---

## ✅ Contraintes d'Intégrité

✅ **Clés primaires** : UUID v4 pour tous les identifiants  
✅ **Clés étrangères** : Relations garanties par la base  
✅ **NOT NULL** : Champs obligatoires définis  
✅ **CASCADE DELETE** : Suppression en cascade des entités liées

---

## 💡 Exemple Concret

**Événement** : "Anniversaire de Marie - 15 février"  
**Ressources** : 
- "Bouteilles de vin" (3 unités)
- "Desserts" (2 unités)

**Participants** :
- "Jean" → Contribution : 2 bouteilles de vin
- "Sophie" → Contribution : 1 dessert

---

## 🎓 Compétence CDA

**CDA-1.3** : Développer la persistance des données
