# SLIDE 6 : ENDPOINTS REST

---

## 🌐 API REST Complète - 15 Endpoints

### 📅 **EVENTS** (5 endpoints)

| Méthode | Endpoint | Description | Code retour |
|---------|----------|-------------|-------------|
| **POST** | `/events` | Créer un événement | 201 Created |
| **GET** | `/events?organizerId={id}` | Lister par organisateur | 200 OK |
| **GET** | `/events/{id}` | Récupérer un événement | 200 OK / 404 |
| **PUT** | `/events/{id}` | Mettre à jour | 200 OK / 404 |
| **DELETE** | `/events/{id}` | Supprimer | 204 No Content |

---

### 👥 **PARTICIPANTS** (4 endpoints)

| Méthode | Endpoint | Description | Code retour |
|---------|----------|-------------|-------------|
| **POST** | `/events/{eventId}/participants` | Ajouter participant | 201 Created |
| **GET** | `/events/{eventId}/participants` | Lister participants | 200 OK |
| **PUT** | `/participants/{id}` | Modifier statut | 200 OK / 404 |
| **DELETE** | `/participants/{id}` | Retirer participant | 204 No Content |

---

### 📦 **RESOURCES** (3 endpoints)

| Méthode | Endpoint | Description | Code retour |
|---------|----------|-------------|-------------|
| **POST** | `/events/{eventId}/resources` | Ajouter ressource | 201 Created |
| **GET** | `/events/{eventId}/resources` | Lister ressources | 200 OK |
| **DELETE** | `/resources/{id}` | Supprimer ressource | 204 No Content |

---

### 🤝 **CONTRIBUTIONS** (3 endpoints)

| Méthode | Endpoint | Description | Code retour |
|---------|----------|-------------|-------------|
| **POST** | `/contributions` | Créer contribution | 201 Created |
| **GET** | `/events/{eventId}/contributions` | Lister par événement | 200 OK |
| **DELETE** | `/contributions/{id}` | Supprimer contribution | 204 No Content |

---

## ✅ Conventions REST Respectées

### 📋 Verbes HTTP
- **POST** : Création de ressource
- **GET** : Lecture (idempotent)
- **PUT** : Mise à jour complète
- **DELETE** : Suppression

### 🎯 Codes de statut appropriés
- **200 OK** : Succès avec contenu
- **201 Created** : Ressource créée
- **204 No Content** : Succès sans contenu
- **400 Bad Request** : Erreur de validation
- **404 Not Found** : Ressource inexistante
- **500 Internal Server Error** : Erreur serveur

### 📝 Format JSON
```json
// Request
{
  "name": "Anniversaire Marie",
  "description": "Fête d'anniversaire",
  "date": "2026-02-15T19:00:00Z",
  "location": "Restaurant Le Gourmet",
  "type": "BIRTHDAY"
}

// Response
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Anniversaire Marie",
  "description": "Fête d'anniversaire",
  "date": "2026-02-15T19:00:00Z",
  "location": "Restaurant Le Gourmet",
  "type": "BIRTHDAY",
  "organizerId": "123e4567-e89b-12d3-a456-426614174000"
}
```

---

## 🔒 Sécurité

✅ **Authentification JWT** obligatoire (header `Authorization: Bearer {token}`)  
✅ **Validation des entrées** (DTO avec règles métier)  
✅ **Gestion des erreurs** (messages explicites)

---

## 🎓 Compétence CDA

**CDA-1.1** : Développer des composants d'accès aux données
