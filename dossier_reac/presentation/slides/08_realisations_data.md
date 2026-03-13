<!-- Slides 29-32 — Timing: 4 min -->

# Réalisations — Accès aux données

## Slide 29 — Verrou optimiste : SqlResourceRepository (1 min 30s)

```kotlin
// infrastructure/resource/common/driven/SqlResourceRepository.kt

override fun updateQuantity(
  resourceId: UUID, quantityDelta: Int, expectedVersion: Int,
): Either<GetResourceRepositoryException, Resource> {
  return Either.catch {
    transaction(exposedDatabase.database) {
      val currentResource = ResourceTable
        .selectAll().where {
          (ResourceTable.id eq resourceId) and
            (ResourceTable.version eq expectedVersion)
        }.singleOrNull()

      if (currentResource == null) {
        val exists = ResourceTable.selectAll()
          .where { ResourceTable.id eq resourceId }.singleOrNull()
        if (exists == null) throw ResourceNotFoundException(resourceId)
        else throw OptimisticLockException(resourceId, expectedVersion)
      }

      val updatedRows = ResourceTable.update({
        (ResourceTable.id eq resourceId) and
          (ResourceTable.version eq expectedVersion)
      }) {
        it[currentQuantity] = currentResource[currentQuantity] + quantityDelta
        it[version] = expectedVersion + 1
      }
      if (updatedRows == 0) throw OptimisticLockException(resourceId, expectedVersion)
    }
  }
}
```

- **Double vérification** : version check à la lecture ET à l'écriture
- **Pas de verrou SQL** (`SELECT FOR UPDATE`) : meilleure performance
- **`OptimisticLockException`** → 409 Conflict côté client

---

## Slide 30 — Table Exposed : ContributionTable (45s)

```kotlin
// infrastructure/contribution/common/driven/ContributionTable.kt

object ContributionTable : UUIDTable("configuration.contribution", "id") {
  val participantId = uuid("participant_id").references(ParticipantTable.id)
  val resourceId = uuid("resource_id").references(ResourceTable.id)
  val quantity = integer("quantity")
  val createdAt = timestamp("created_at")
  val updatedAt = timestamp("updated_at")

  init {
    uniqueIndex("uq_contribution_participant_resource", participantId, resourceId)
    index("idx_contribution_participant", false, participantId)
    index("idx_contribution_resource", false, resourceId)
    check("chk_quantity_positive") { quantity greater 0 }
  }
}
```

- **`uniqueIndex` composite** : un participant ne contribue qu'une fois par ressource
- **`check` constraint** : quantités strictement positives
- **FK + index** : intégrité référentielle + performance des requêtes

---

## Slide 31 — Repository : SqlContributionRepository (1 min)

```kotlin
// infrastructure/contribution/common/driven/SqlContributionRepository.kt

override fun addOrUpdate(
  participant: Participant, request: AddContributionRequest,
): Either<AddContributionException, Contribution> {
  return Either.catch {
    transaction(exposedDatabase.database) {
      // Rechercher une contribution existante
      val existing = ContributionTable.selectAll().where {
        (ContributionTable.participantId eq participant.identifier) and
          (ContributionTable.resourceId eq request.resourceId)
      }.singleOrNull()

      if (existing != null) {
        // Mise à jour de la quantité
        ContributionTable.update({
          ContributionTable.id eq existing[ContributionTable.id]
        }) {
          it[quantity] = request.quantity
          it[updatedAt] = clock.instant()
        }
      } else {
        // Nouvelle contribution
        ContributionTable.insert {
          it[participantId] = participant.identifier
          it[resourceId] = request.resourceId
          it[quantity] = request.quantity
          it[createdAt] = clock.instant()
          it[updatedAt] = clock.instant()
        }
      }
      // Puis updateQuantity sur la Resource (verrou optimiste)
    }
  }
}
```

- Pattern **Repository** : interface dans le domaine, implémentation SQL ici
- Chaque opération dans une **transaction** Exposed
- Requêtes **paramétrées** automatiquement (protection injection SQL)

---

## Slide 32 — Transactions et intégrité des données (45s)

### Défense en profondeur

| Mécanisme | Protection |
|-----------|-----------|
| **Verrou optimiste** | Empêche les mises à jour concurrentes silencieuses |
| **FK CASCADE** | Suppression événement → cascade contributions → ressources → participants |
| **UNIQUE composite** | Un participant ne contribue qu'une fois par ressource |
| **CHECK constraint** | Quantités strictement positives (`quantity > 0`) |
| **Transactions** | Chaque opération encapsulée dans `transaction { }` |

### Suppression en cascade (ordre imposé par les FK)

```
1. Supprimer les contributions (FK → participant + resource)
2. Supprimer les ressources (FK → event)
3. Supprimer les participants (FK → event)
4. Supprimer l'événement
```

L'ordre est géré manuellement dans `SqlEventRepository.delete` pour garder le contrôle sur la logique et les logs.
