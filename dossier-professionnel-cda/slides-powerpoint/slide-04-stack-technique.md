# SLIDE 4 : STACK TECHNIQUE

---

## 💻 Technologies Utilisées

### 🔵 Langage & Framework

| Techno | Version | Justification |
|--------|---------|---------------|
| **Kotlin** | 2.2.0 | • Concision et null-safety<br>• Interopérabilité Java<br>• Support officiel JetBrains |
| **Ktor** | 3.2.2 | • Framework asynchrone léger<br>• Performances supérieures<br>• DSL Kotlin idiomatique |

**Pourquoi Ktor plutôt que Spring Boot ?**
- ⚡ Plus léger (~10x moins de dépendances)
- 🚀 Démarrage plus rapide (~2 secondes vs ~20 secondes)
- 🎯 Adapté aux microservices et APIs simples

---

### 💾 Base de Données

| Techno | Version | Usage |
|--------|---------|-------|
| **PostgreSQL** | 15 | Base de données production |
| **Exposed** | 0.61.0 | ORM avec DSL Kotlin type-safe |
| **HikariCP** | 6.3.1 | Connection pooling performant |

---

### 🧰 Outils Qualité & DevOps

| Catégorie | Outil | Version | Rôle |
|-----------|-------|---------|------|
| **Tests** | Kotest | 5.9.1 | Framework de tests |
| | MockK | 1.14.5 | Mocking |
| | Testcontainers | 1.21.3 | Tests d'intégration |
| | JaCoCo | 0.8.13 | Couverture de code |
| **Qualité** | Detekt | 1.23.7 | Analyse statique |
| | Spotless | 6.25.0 | Formatage auto |
| **DevOps** | GitHub Actions | - | CI/CD |
| | Docker | - | Conteneurisation |
| | Render | - | Hébergement cloud |

---

### 📦 Bibliothèques

| Librairie | Version | Usage |
|-----------|---------|-------|
| **Arrow** | 2.1.2 | Programmation fonctionnelle (Either) |
| **Koin** | 4.1.0 | Injection de dépendances |
| **Auth0 JWT** | 4.4.0 | Validation des tokens JWT |

---

## 🎯 Choix Techniques Clés

✅ **Kotlin** : Langage moderne, concis, type-safe  
✅ **Ktor** : Léger et performant pour API REST  
✅ **Exposed** : ORM type-safe évitant l'injection SQL  
✅ **Arrow Either** : Gestion fonctionnelle des erreurs  
✅ **PostgreSQL** : Base robuste et éprouvée

---

## 📚 Documentation

Tous les choix sont justifiés dans le dossier  
**Section 6** : Spécifications techniques
