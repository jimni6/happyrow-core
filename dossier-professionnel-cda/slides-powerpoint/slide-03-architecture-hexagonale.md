# SLIDE 3 : ARCHITECTURE HEXAGONALE

---

## 🏗️ Architecture Ports et Adaptateurs

```
┌─────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE                            │
│                                                               │
│  ┌─────────────────┐              ┌─────────────────┐       │
│  │  ADAPTATEURS    │              │  ADAPTATEURS    │       │
│  │   ENTRANTS      │              │   SORTANTS      │       │
│  │                 │              │                 │       │
│  │  • REST API     │              │ • PostgreSQL    │       │
│  │  • Endpoints    │              │ • Repositories  │       │
│  │  • Ktor         │              │ • Exposed ORM   │       │
│  └────────┬────────┘              └────────▲────────┘       │
│           │                                 │                │
│           │         ┌───────────────────┐  │                │
│           │         │                   │  │                │
│           └────────►│     DOMAIN        │──┘                │
│                     │   (Logique Métier)│                   │
│                     │                   │                   │
│                     │  • Entities       │                   │
│                     │  • Use Cases      │                   │
│                     │  • Ports          │                   │
│                     │    (Interfaces)   │                   │
│                     │                   │                   │
│                     └───────────────────┘                   │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Les 3 Couches

### 🎯 **DOMAIN** (Centre)
- Logique métier pure
- Indépendant de toute technologie
- **Entities** : Event, Participant, Resource
- **Use Cases** : CreateEvent, UpdateEvent...
- **Ports** : Interfaces (EventRepository...)

### 🔌 **ADAPTATEURS ENTRANTS** (Gauche)
- Points d'entrée de l'application
- Endpoints REST avec Ktor
- Transformation DTO ↔ Domain

### 💾 **ADAPTATEURS SORTANTS** (Droite)
- Accès aux ressources externes
- Repositories PostgreSQL
- Exposed ORM

---

## ✅ Avantages

| Avantage | Bénéfice |
|----------|----------|
| **🎯 Isolation du métier** | Code métier protégé des détails techniques |
| **🧪 Testabilité** | Tests unitaires sans base de données |
| **🔄 Évolutivité** | Changement de framework sans impact métier |
| **📚 Maintenabilité** | Responsabilités clairement séparées |

---

## 🎓 Compétence CDA

**CDA-2.1** : Concevoir une application organisée en couches
