# SLIDE 10 : DÉMONSTRATION API

---

## 🎬 Scénario de Démonstration

### Préparation
- ✅ API déployée sur Render : `https://happyrow-core.onrender.com`
- ✅ Postman ouvert avec collection préparée
- ✅ Token JWT valide disponible

---

## 📝 Test 1 : Créer un Événement

### Request
```http
POST https://happyrow-core.onrender.com/events
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "Anniversaire de Marie",
  "description": "Fête d'anniversaire surprise pour les 30 ans de Marie",
  "date": "2026-02-15T19:00:00Z",
  "location": "Restaurant Le Gourmet, Paris",
  "type": "BIRTHDAY"
}
```

### Response ✅ 201 Created
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Anniversaire de Marie",
  "description": "Fête d'anniversaire surprise pour les 30 ans de Marie",
  "date": "2026-02-15T19:00:00Z",
  "location": "Restaurant Le Gourmet, Paris",
  "type": "BIRTHDAY",
  "organizerId": "123e4567-e89b-12d3-a456-426614174000"
}
```

**✅ Succès** : Événement créé avec un ID généré

---

## 🔍 Test 2 : Récupérer l'Événement

### Request
```http
GET https://happyrow-core.onrender.com/events/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Response ✅ 200 OK
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Anniversaire de Marie",
  "description": "Fête d'anniversaire surprise pour les 30 ans de Marie",
  "date": "2026-02-15T19:00:00Z",
  "location": "Restaurant Le Gourmet, Paris",
  "type": "BIRTHDAY",
  "organizerId": "123e4567-e89b-12d3-a456-426614174000"
}
```

**✅ Succès** : Données persistées et récupérées correctement

---

## ❌ Test 3 : Sans Authentification

### Request (sans token)
```http
POST https://happyrow-core.onrender.com/events
Content-Type: application/json

{
  "name": "Test sans auth",
  "date": "2026-03-01T10:00:00Z",
  "type": "PARTY"
}
```

### Response ❌ 401 Unauthorized
```json
{
  "error": "Authentication required",
  "message": "Missing or invalid Authorization header"
}
```

**✅ Sécurité** : Requête non authentifiée rejetée

---

## 🚫 Test 4 : Validation Métier

### Request (date dans le passé)
```http
POST https://happyrow-core.onrender.com/events
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "Événement passé",
  "date": "2020-01-01T10:00:00Z",
  "type": "PARTY"
}
```

### Response ❌ 400 Bad Request
```json
{
  "error": "ValidationError",
  "message": "Event date must be in the future",
  "field": "date"
}
```

**✅ Validation** : Règles métier appliquées

---

## 👥 Test 5 : Ajouter un Participant

### Request
```http
POST https://happyrow-core.onrender.com/events/550e8400-e29b-41d4-a716-446655440000/participants
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "Jean Dupont",
  "email": "jean.dupont@example.com"
}
```

### Response ✅ 201 Created
```json
{
  "id": "789e4567-e89b-12d3-a456-426614174111",
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Jean Dupont",
  "email": "jean.dupont@example.com",
  "status": "PENDING"
}
```

---

## 📦 Test 6 : Ajouter une Ressource

### Request
```http
POST https://happyrow-core.onrender.com/events/550e8400-e29b-41d4-a716-446655440000/resources
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "Bouteilles de vin",
  "quantityNeeded": 5,
  "unit": "unité"
}
```

### Response ✅ 201 Created
```json
{
  "id": "999e4567-e89b-12d3-a456-426614174222",
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Bouteilles de vin",
  "quantityNeeded": 5,
  "unit": "unité"
}
```

---

## 🤝 Test 7 : Créer une Contribution

### Request
```http
POST https://happyrow-core.onrender.com/contributions
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "participantId": "789e4567-e89b-12d3-a456-426614174111",
  "resourceId": "999e4567-e89b-12d3-a456-426614174222",
  "quantityPromised": 2
}
```

### Response ✅ 201 Created
```json
{
  "id": "111e4567-e89b-12d3-a456-426614174333",
  "participantId": "789e4567-e89b-12d3-a456-426614174111",
  "resourceId": "999e4567-e89b-12d3-a456-426614174222",
  "quantityPromised": 2
}
```

**✅ Résultat** : Jean s'engage à apporter 2 bouteilles de vin

---

## 📊 Récapitulatif des Tests

| Test | Endpoint | Résultat | Temps | Validation |
|------|----------|----------|-------|------------|
| 1. Créer événement | POST /events | ✅ 201 | ~150ms | ✅ |
| 2. Récupérer événement | GET /events/{id} | ✅ 200 | ~100ms | ✅ |
| 3. Sans auth | POST /events | ❌ 401 | ~50ms | ✅ Sécurité |
| 4. Date invalide | POST /events | ❌ 400 | ~80ms | ✅ Validation |
| 5. Ajouter participant | POST /participants | ✅ 201 | ~130ms | ✅ |
| 6. Ajouter ressource | POST /resources | ✅ 201 | ~120ms | ✅ |
| 7. Créer contribution | POST /contributions | ✅ 201 | ~140ms | ✅ |

---

## ✅ Points Démontrés

✅ **API fonctionnelle** : Tous les endpoints répondent correctement  
✅ **Authentification** : JWT validé, requêtes non auth rejetées  
✅ **Validation métier** : Règles appliquées (date future, longueurs, etc.)  
✅ **Persistance** : Données sauvegardées et récupérables  
✅ **Performance** : Tous les appels < 200ms  
✅ **Déploiement** : Application accessible publiquement sur Render

---

## 🔗 Accès

**API** : https://happyrow-core.onrender.com  
**GitHub** : https://github.com/jimni6/happyrow-core  
**Collection Postman** : Disponible dans le repo
