# Annexes — Code complet de la fonctionnalité représentative

La fonctionnalité représentative est **l'ajout d'une contribution avec verrou optimiste**. Elle traverse toutes les couches de l'architecture hexagonale et illustre les choix techniques majeurs du projet.

---

## Annexe 0 — Maquettes Figma de la fonctionnalité contribution

Les maquettes ci-dessous ont été réalisées sur Figma en amont du développement. Elles utilisent la charte graphique HappyRow (Design Tokens CSS : teal `#5FBDB4`, navy `#3D5A6C`, coral `#E6A19A`, police Comic Neue).

### Maquette 1 — Vue détail d'un événement (EventDetailsView)

> *[Insérer ici la maquette Figma de l'écran EventDetailsView — affichant les ressources par catégorie avec les contrôles de contribution +/−]*

Cette maquette présente l'organisation par catégorie des ressources (Food, Drinks) avec pour chaque ressource : le nom, la quantité courante / suggérée, les boutons +/− pour sélectionner un delta, et le bouton « Validate » qui apparaît lorsqu'un delta est sélectionné.

### Maquette 2 — Page d'accueil (HomePage / Dashboard)

> *[Insérer ici la maquette Figma de l'écran HomePage — affichant les EventCards et la barre de navigation]*

Le dashboard affiche la liste des événements de l'utilisateur sous forme de cartes. Chaque carte présente la date, le nom de l'événement, les participants et la localisation.

### Maquette 3 — Formulaire de création d'événement (CreateEventForm)

> *[Insérer ici la maquette Figma du formulaire de création d'événement]*

Le formulaire permet de saisir le nom (min 3 caractères), la description, la date/heure (futur uniquement), le lieu et le type d'événement (Party, Birthday, Diner, Snack).

### Maquette 4 — Authentification (WelcomeView + LoginModal)

> *[Insérer ici la maquette Figma de la page d'accueil non authentifié et du modal de connexion]*

---

## Annexe A — Interfaces utilisateur de la fonctionnalité contribution

### Captures d'écran des interfaces réelles

> *[Insérer ici une capture d'écran réelle de l'écran EventDetailsView en fonctionnement, montrant les ressources avec les contrôles de contribution]*

> *[Insérer ici une capture d'écran réelle du HomePage avec les EventCards]*

### A.1 — Composant ResourceCategorySection (organisation par catégorie)

```typescript
// src/features/events/components/ResourceCategorySection.tsx

export const ResourceCategorySection: React.FC<ResourceCategorySectionProps> = ({
  title, category, resources, currentUserId,
  onAddContribution, onUpdateContribution, onDeleteContribution, onAddResource,
}) => {
  return (
    <div className="category-section">
      <h2 className="category-title">{title}</h2>
      <div className="resources-list">
        {resources.map(resource => (
          <ResourceItem
            key={resource.id}
            resource={resource}
            currentUserId={currentUserId}
            onAddContribution={onAddContribution}
            onUpdateContribution={onUpdateContribution}
            onDeleteContribution={onDeleteContribution}
          />
        ))}
      </div>
      <InlineAddResourceForm category={category} onSubmit={onAddResource} />
    </div>
  );
};
```

### A.2 — Composant ResourceItem (contribution inline)

```typescript
// src/features/resources/components/ResourceItem.tsx

export const ResourceItem: React.FC<ResourceItemProps> = ({
  resource, currentUserId,
  onAddContribution, onUpdateContribution, onDeleteContribution,
}) => {
  const [selectedQuantity, setSelectedQuantity] = useState(0);
  const [isSaving, setIsSaving] = useState(false);

  const userContribution = resource.contributors.find(c => c.userId === currentUserId);
  const userQuantity = userContribution?.quantity || 0;
  const hasSelection = selectedQuantity !== 0;

  const handleValidate = async () => {
    try {
      setIsSaving(true);
      const newQuantity = userQuantity + selectedQuantity;
      if (newQuantity <= 0) await onDeleteContribution(resource.id);
      else if (userQuantity === 0) await onAddContribution(resource.id, selectedQuantity);
      else await onUpdateContribution(resource.id, newQuantity);
      setSelectedQuantity(0);
    } catch (error) {
      console.error('Error updating contribution:', error);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="resource-item-card">
      <div className="resource-item-content">
        <div className="resource-item-header">
          <span className="resource-item-name">{resource.name}</span>
          {userQuantity > 0 && (
            <span className="resource-user-contribution">
              Your contribution: {userQuantity}
            </span>
          )}
        </div>
        <div className="resource-item-controls">
          <span className="resource-item-quantity">
            {resource.currentQuantity}
            {resource.suggestedQuantity && `/${resource.suggestedQuantity}`}
          </span>
          <div className="resource-item-buttons">
            <button className="resource-btn resource-btn-minus"
              onClick={() => setSelectedQuantity(prev => prev - 1)}
              disabled={(selectedQuantity === 0 && userQuantity === 0) || isSaving}>−</button>
            {hasSelection && (
              <span className={`resource-selected-quantity ${selectedQuantity < 0 ? 'negative' : ''}`}>
                {selectedQuantity > 0 ? '+' : ''}{selectedQuantity}
              </span>
            )}
            <button className="resource-btn resource-btn-plus"
              onClick={() => setSelectedQuantity(prev => prev + 1)}
              disabled={isSaving}>+</button>
          </div>
        </div>
      </div>
      {hasSelection && (
        <div className="resource-item-actions">
          <button className="resource-validate-btn"
            onClick={handleValidate} disabled={isSaving}>
            {isSaving ? 'Saving...' : 'Validate'}
          </button>
        </div>
      )}
    </div>
  );
};
```

---

## Annexe B — Code front-end de la fonctionnalité contribution (services et state)

### B.1 — HttpContributionRepository (appels API)

```typescript
// src/features/contributions/services/HttpContributionRepository.ts

export class HttpContributionRepository implements ContributionRepository {
  private baseUrl: string;
  private getToken: () => string | null;

  constructor(getToken: () => string | null, baseUrl: string = /* ... */) {
    this.baseUrl = baseUrl;
    this.getToken = getToken;
  }

  async createContribution(data: ContributionCreationRequest): Promise<Contribution> {
    const token = this.getToken();
    if (!token) throw new Error('Authentication required');

    const response = await fetch(
      `${this.baseUrl}/events/${data.eventId}/resources/${data.resourceId}/contributions`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ quantity: data.quantity }),
      }
    );

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
    }

    const contributionResponse = await response.json();
    return this.mapApiResponseToContribution(contributionResponse, data.eventId);
  }

  async deleteContribution(params: { eventId: string; resourceId: string }): Promise<void> {
    const token = this.getToken();
    if (!token) throw new Error('Authentication required');

    const response = await fetch(
      `${this.baseUrl}/events/${params.eventId}/resources/${params.resourceId}/contributions`,
      { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } }
    );
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
  }

  private mapApiResponseToContribution(response: ContributionApiResponse, eventId: string): Contribution {
    return {
      id: response.identifier,
      eventId, resourceId: response.resource_id,
      userId: response.participant_id, quantity: response.quantity,
      createdAt: new Date(response.created_at),
    };
  }
}
```

### B.2 — Use Case AddContribution (validation métier)

```typescript
// src/features/contributions/use-cases/AddContribution.ts

export class AddContribution {
  constructor(private contributionRepository: ContributionRepository) {}

  async execute(input: AddContributionInput): Promise<Contribution> {
    this.validateInput(input);
    try {
      return await this.contributionRepository.createContribution({
        eventId: input.eventId, resourceId: input.resourceId,
        userId: input.userId, quantity: input.quantity,
      });
    } catch (error) {
      throw new Error(`Failed to add contribution: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }

  private validateInput(input: AddContributionInput): void {
    if (!input.quantity || input.quantity < 1) throw new Error('Quantity must be at least 1');
    if (!input.userId?.trim()) throw new Error('Valid user ID is required');
    if (!input.eventId?.trim()) throw new Error('Valid event ID is required');
    if (!input.resourceId?.trim()) throw new Error('Valid resource ID is required');
  }
}
```

### B.3 — useContributionOperations (mise à jour optimiste + rollback)

```typescript
// src/features/resources/hooks/useContributionOperations.ts (extrait — addContribution)

const addContribution = useCallback(
  async (resourceId: string, userId: string, quantity: number): Promise<void> => {
    setError(null);
    const previousResources = [...resources]; // Sauvegarde pour rollback

    try {
      // Mise à jour optimiste AVANT l'appel API
      setResources(prev =>
        prev.map(r =>
          r.id === resourceId
            ? {
                ...r,
                currentQuantity: r.currentQuantity + quantity,
                contributors: [...r.contributors, { userId, quantity, contributedAt: new Date() }],
              }
            : r
        )
      );

      if (!currentEventId) throw new Error('No event context available');

      await addContributionUseCase.execute({ eventId: currentEventId, resourceId, userId, quantity });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add contribution');
      setResources(previousResources); // ROLLBACK
      throw err;
    }
  },
  [addContributionUseCase, currentEventId, resources, setResources, setError]
);
```

---

## Annexe C — Code complet des composants métier

### C.1 — Modèle du domaine : AddContributionRequest

```kotlin
// domain/contribution/add/model/AddContributionRequest.kt

package com.happyrow.core.domain.contribution.add.model

import java.util.UUID

data class AddContributionRequest(
  val userEmail: String,
  val eventId: UUID,
  val resourceId: UUID,
  val quantity: Int,
)
```

### C.2 — Modèle du domaine : Contribution

```kotlin
// domain/contribution/common/model/Contribution.kt

package com.happyrow.core.domain.contribution.common.model

import java.time.Instant
import java.util.UUID

data class Contribution(
  val identifier: UUID,
  val participantId: UUID,
  val resourceId: UUID,
  val quantity: Int,
  val createdAt: Instant,
  val updatedAt: Instant,
)
```

### C.3 — Port driven : ContributionRepository (interface)

```kotlin
// domain/contribution/common/driven/ContributionRepository.kt

package com.happyrow.core.domain.contribution.common.driven

import arrow.core.Either
import com.happyrow.core.domain.contribution.add.model.AddContributionRequest
import com.happyrow.core.domain.contribution.common.error.ContributionRepositoryException
import com.happyrow.core.domain.contribution.common.model.Contribution
import com.happyrow.core.domain.contribution.reduce.model.ReduceContributionRequest
import java.util.UUID

interface ContributionRepository {
  fun addOrUpdate(request: AddContributionRequest):
    Either<ContributionRepositoryException, Contribution>
  fun reduce(request: ReduceContributionRequest):
    Either<ContributionRepositoryException, Contribution?>
  fun delete(userEmail: String, eventId: UUID, resourceId: UUID):
    Either<ContributionRepositoryException, Unit>
  fun findByResource(resourceId: UUID):
    Either<ContributionRepositoryException, List<Contribution>>
}
```

### C.4 — Port driven : ResourceRepository (interface)

```kotlin
// domain/resource/common/driven/ResourceRepository.kt

package com.happyrow.core.domain.resource.common.driven

import arrow.core.Either
import com.happyrow.core.domain.resource.common.error.*
import com.happyrow.core.domain.resource.common.model.Resource
import com.happyrow.core.domain.resource.create.model.CreateResourceRequest
import java.util.UUID

interface ResourceRepository {
  fun create(request: CreateResourceRequest):
    Either<CreateResourceRepositoryException, Resource>
  fun find(resourceId: UUID):
    Either<GetResourceRepositoryException, Resource?>
  fun findByEvent(eventId: UUID):
    Either<GetResourceRepositoryException, List<Resource>>
  fun updateQuantity(
    resourceId: UUID,
    quantityDelta: Int,
    expectedVersion: Int,
  ): Either<GetResourceRepositoryException, Resource>
}
```

### C.5 — Use Case : AddContributionUseCase

```kotlin
// domain/contribution/add/AddContributionUseCase.kt

package com.happyrow.core.domain.contribution.add

import arrow.core.Either
import com.happyrow.core.domain.contribution.add.error.AddContributionException
import com.happyrow.core.domain.contribution.add.model.AddContributionRequest
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.contribution.common.model.Contribution

class AddContributionUseCase(
  private val contributionRepository: ContributionRepository,
) {
  fun execute(request: AddContributionRequest):
    Either<AddContributionException, Contribution> =
    contributionRepository.addOrUpdate(request)
      .mapLeft { AddContributionException(request, it) }
}
```

---

## Annexe D — Code complet des composants d'accès aux données

### D.1 — Table Exposed : ContributionTable

```kotlin
// infrastructure/contribution/common/driven/ContributionTable.kt

package com.happyrow.core.infrastructure.contribution.common.driven

import com.happyrow.core.infrastructure.participant.common.driven.ParticipantTable
import com.happyrow.core.infrastructure.resource.common.driven.ResourceTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

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

### D.2 — Mapper : ContributionMapper

```kotlin
// infrastructure/contribution/common/driven/ContributionMapper.kt

package com.happyrow.core.infrastructure.contribution.common.driven

import arrow.core.Either
import com.happyrow.core.domain.contribution.common.model.Contribution
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toContribution(): Either<Throwable, Contribution> = Either.catch {
  Contribution(
    identifier = this[ContributionTable.id].value,
    participantId = this[ContributionTable.participantId],
    resourceId = this[ContributionTable.resourceId],
    quantity = this[ContributionTable.quantity],
    createdAt = this[ContributionTable.createdAt],
    updatedAt = this[ContributionTable.updatedAt],
  )
}
```

### D.3 — Repository SQL : SqlContributionRepository (complet)

```kotlin
// infrastructure/contribution/common/driven/SqlContributionRepository.kt

package com.happyrow.core.infrastructure.contribution.common.driven

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.contribution.add.model.AddContributionRequest
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.contribution.common.error.ContributionRepositoryException
import com.happyrow.core.domain.contribution.common.model.Contribution
import com.happyrow.core.domain.contribution.reduce.error.InsufficientContributionException
import com.happyrow.core.domain.contribution.reduce.model.ReduceContributionRequest
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.resource.common.driven.ResourceRepository
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.util.UUID

class SqlContributionRepository(
  private val clock: Clock,
  private val exposedDatabase: ExposedDatabase,
  private val participantRepository: ParticipantRepository,
  private val resourceRepository: ResourceRepository,
) : ContributionRepository {

  override fun addOrUpdate(
    request: AddContributionRequest
  ): Either<ContributionRepositoryException, Contribution> {
    return participantRepository.findOrCreate(request.userEmail, request.eventId)
      .mapLeft { ContributionRepositoryException(request.resourceId, it) }
      .flatMap { participant ->
        val existingContribution = Either.catch {
          transaction(exposedDatabase.database) {
            ContributionTable
              .selectAll().where {
                (ContributionTable.participantId eq participant.identifier) and
                  (ContributionTable.resourceId eq request.resourceId)
              }
              .singleOrNull()
          }
        }.mapLeft { ContributionRepositoryException(request.resourceId, it) }

        existingContribution.flatMap { existing ->
          if (existing != null) {
            val oldQuantity = existing[ContributionTable.quantity]
            val delta = request.quantity - oldQuantity
            updateContribution(
              participant.identifier, request.resourceId, request.quantity
            ).flatMap { contribution ->
              resourceRepository.find(request.resourceId)
                .mapLeft { ContributionRepositoryException(request.resourceId, it) }
                .flatMap { resource ->
                  resource?.let {
                    resourceRepository.updateQuantity(
                      request.resourceId, delta, it.version
                    )
                      .mapLeft { ContributionRepositoryException(request.resourceId, it) }
                      .map { contribution }
                  } ?: Either.Left(
                    ContributionRepositoryException(
                      request.resourceId, Exception("Resource not found")
                    )
                  )
                }
            }
          } else {
            createContribution(
              participant.identifier, request.resourceId, request.quantity
            ).flatMap { contribution ->
              resourceRepository.find(request.resourceId)
                .mapLeft { ContributionRepositoryException(request.resourceId, it) }
                .flatMap { resource ->
                  resource?.let {
                    resourceRepository.updateQuantity(
                      request.resourceId, request.quantity, it.version
                    )
                      .mapLeft { ContributionRepositoryException(request.resourceId, it) }
                      .map { contribution }
                  } ?: Either.Left(
                    ContributionRepositoryException(
                      request.resourceId, Exception("Resource not found")
                    )
                  )
                }
            }
          }
        }
      }
  }

  override fun reduce(
    request: ReduceContributionRequest
  ): Either<ContributionRepositoryException, Contribution?> {
    return participantRepository.find(request.userEmail, request.eventId)
      .mapLeft { ContributionRepositoryException(request.resourceId, it) }
      .flatMap { participant ->
        participant?.let {
          Either.catch {
            transaction(exposedDatabase.database) {
              ContributionTable
                .selectAll().where {
                  (ContributionTable.participantId eq participant.identifier) and
                    (ContributionTable.resourceId eq request.resourceId)
                }
                .singleOrNull()
            }
          }.mapLeft { ContributionRepositoryException(request.resourceId, it) }
            .flatMap { contribution ->
              contribution?.let {
                processReduction(
                  participant.identifier, request, it[ContributionTable.quantity]
                )
              } ?: Either.Left(
                ContributionRepositoryException(
                  request.resourceId, Exception("Contribution not found")
                ),
              )
            }
        } ?: Either.Left(
          ContributionRepositoryException(
            request.resourceId, Exception("Participant not found")
          ),
        )
      }
  }

  private fun processReduction(
    participantId: UUID,
    request: ReduceContributionRequest,
    currentQuantity: Int,
  ): Either<ContributionRepositoryException, Contribution?> {
    if (request.quantity > currentQuantity) {
      return Either.Left(
        ContributionRepositoryException(
          request.resourceId,
          InsufficientContributionException(currentQuantity, request.quantity),
        ),
      )
    }
    val newQuantity = currentQuantity - request.quantity
    return if (newQuantity == 0) {
      deleteContributionAndUpdateResource(participantId, request.resourceId, request.quantity)
    } else {
      reduceContributionAndUpdateResource(
        participantId, request.resourceId, newQuantity, request.quantity
      )
    }
  }

  private fun deleteContributionAndUpdateResource(
    participantId: UUID, resourceId: UUID, quantityToReduce: Int,
  ): Either<ContributionRepositoryException, Contribution?> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        ContributionTable.deleteWhere {
          (ContributionTable.participantId eq participantId) and
            (ContributionTable.resourceId eq resourceId)
        }
      }
    }
      .mapLeft { ContributionRepositoryException(resourceId, it) }
      .flatMap { updateResourceQuantity(resourceId, -quantityToReduce).map { null } }
  }

  private fun reduceContributionAndUpdateResource(
    participantId: UUID, resourceId: UUID, newQuantity: Int, quantityToReduce: Int,
  ): Either<ContributionRepositoryException, Contribution?> {
    return updateContribution(participantId, resourceId, newQuantity)
      .flatMap { updatedContribution ->
        updateResourceQuantity(resourceId, -quantityToReduce).map { updatedContribution }
      }
  }

  private fun updateResourceQuantity(
    resourceId: UUID, quantityDelta: Int,
  ): Either<ContributionRepositoryException, Unit> {
    return resourceRepository.find(resourceId)
      .mapLeft { ContributionRepositoryException(resourceId, it) }
      .flatMap { resource ->
        resource?.let {
          resourceRepository.updateQuantity(resourceId, quantityDelta, it.version)
            .mapLeft { ContributionRepositoryException(resourceId, it) }
            .map { }
        } ?: Either.Left(
          ContributionRepositoryException(resourceId, Exception("Resource not found"))
        )
      }
  }

  override fun delete(
    userEmail: String, eventId: UUID, resourceId: UUID,
  ): Either<ContributionRepositoryException, Unit> {
    return participantRepository.find(userEmail, eventId)
      .mapLeft { ContributionRepositoryException(resourceId, it) }
      .flatMap { participant ->
        participant?.let {
          val contributionQuantity = Either.catch {
            transaction(exposedDatabase.database) {
              ContributionTable
                .selectAll().where {
                  (ContributionTable.participantId eq participant.identifier) and
                    (ContributionTable.resourceId eq resourceId)
                }
                .singleOrNull()
                ?.get(ContributionTable.quantity)
            }
          }.mapLeft { ContributionRepositoryException(resourceId, it) }

          contributionQuantity.flatMap { quantity ->
            quantity?.let {
              Either.catch {
                transaction(exposedDatabase.database) {
                  ContributionTable.deleteWhere {
                    (ContributionTable.participantId eq participant.identifier) and
                      (ContributionTable.resourceId eq resourceId)
                  }
                }
              }
                .mapLeft { ContributionRepositoryException(resourceId, it) }
                .flatMap {
                  resourceRepository.find(resourceId)
                    .mapLeft { ContributionRepositoryException(resourceId, it) }
                    .flatMap { resource ->
                      resource?.let { res ->
                        resourceRepository.updateQuantity(resourceId, -quantity, res.version)
                          .mapLeft { ContributionRepositoryException(resourceId, it) }
                          .map { }
                      } ?: Either.Right(Unit)
                    }
                }
            } ?: Either.Right(Unit)
          }
        } ?: Either.Right(Unit)
      }
  }

  override fun findByResource(
    resourceId: UUID
  ): Either<ContributionRepositoryException, List<Contribution>> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        ContributionTable
          .selectAll().where { ContributionTable.resourceId eq resourceId }
          .map { row ->
            row.toContribution().fold(
              { error -> throw error },
              { it },
            )
          }
      }
    }.mapLeft { ContributionRepositoryException(resourceId, it) }
  }

  private fun createContribution(
    participantId: UUID, resourceId: UUID, quantity: Int
  ): Either<ContributionRepositoryException, Contribution> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        val contributionId = ContributionTable.insert {
          it[ContributionTable.participantId] = participantId
          it[ContributionTable.resourceId] = resourceId
          it[ContributionTable.quantity] = quantity
          it[createdAt] = clock.instant()
          it[updatedAt] = clock.instant()
        }[ContributionTable.id].value

        ContributionTable
          .selectAll().where { ContributionTable.id eq contributionId }
          .single()
          .toContribution()
          .getOrNull()!!
      }
    }.mapLeft { ContributionRepositoryException(resourceId, it) }
  }

  private fun updateContribution(
    participantId: UUID, resourceId: UUID, quantity: Int
  ): Either<ContributionRepositoryException, Contribution> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        ContributionTable.update({
          (ContributionTable.participantId eq participantId) and
            (ContributionTable.resourceId eq resourceId)
        }) {
          it[ContributionTable.quantity] = quantity
          it[updatedAt] = clock.instant()
        }

        ContributionTable
          .selectAll().where {
            (ContributionTable.participantId eq participantId) and
              (ContributionTable.resourceId eq resourceId)
          }
          .single()
          .toContribution()
          .getOrNull()!!
      }
    }.mapLeft { ContributionRepositoryException(resourceId, it) }
  }
}
```

### D.4 — Verrou optimiste : SqlResourceRepository.updateQuantity

```kotlin
// infrastructure/resource/common/driven/SqlResourceRepository.kt (extrait)

override fun updateQuantity(
  resourceId: UUID,
  quantityDelta: Int,
  expectedVersion: Int,
): Either<GetResourceRepositoryException, Resource> {
  return Either.catch {
    transaction(exposedDatabase.database) {
      val currentResource = ResourceTable
        .selectAll().where {
          (ResourceTable.id eq resourceId) and
            (ResourceTable.version eq expectedVersion)
        }
        .singleOrNull()

      if (currentResource == null) {
        val existsWithDifferentVersion = ResourceTable
          .selectAll().where { ResourceTable.id eq resourceId }
          .singleOrNull()

        if (existsWithDifferentVersion == null) {
          throw ResourceNotFoundException(resourceId)
        } else {
          throw OptimisticLockException(resourceId, expectedVersion)
        }
      }

      val currentQuantity = currentResource[ResourceTable.currentQuantity]
      val newQuantity = currentQuantity + quantityDelta

      val updatedRows = ResourceTable.update({
        (ResourceTable.id eq resourceId) and
          (ResourceTable.version eq expectedVersion)
      }) {
        it[ResourceTable.currentQuantity] = newQuantity
        it[ResourceTable.version] = expectedVersion + 1
        it[ResourceTable.updatedAt] = clock.instant()
      }

      if (updatedRows == 0) {
        throw OptimisticLockException(resourceId, expectedVersion)
      }
      resourceId
    }
  }
    .flatMap { id ->
      find(id).flatMap {
        it?.let { resource -> Either.Right(resource) }
          ?: Either.Left(
            GetResourceRepositoryException(null, ResourceNotFoundException(id))
          )
      }
    }
    .mapLeft {
      when (it) {
        is OptimisticLockException -> GetResourceRepositoryException(null, it)
        is ResourceNotFoundException -> GetResourceRepositoryException(null, it)
        else -> GetResourceRepositoryException(null, it)
      }
    }
}
```

---

## Annexe E — Code des autres composants (contrôleurs, utilitaires)

### E.1 — Endpoint : AddContributionEndpoint (complet)

```kotlin
// infrastructure/contribution/add/driving/AddContributionEndpoint.kt

package com.happyrow.core.infrastructure.contribution.add.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.contribution.add.AddContributionUseCase
import com.happyrow.core.domain.contribution.add.error.AddContributionException
import com.happyrow.core.domain.resource.common.error.OptimisticLockException
import com.happyrow.core.infrastructure.common.error.BadRequestException
import com.happyrow.core.infrastructure.contribution.add.driving.dto.AddContributionRequestDto
import com.happyrow.core.infrastructure.contribution.common.dto.toDto
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage.Companion.technicalErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.util.UUID

private const val OPTIMISTIC_LOCK_ERROR_TYPE = "OPTIMISTIC_LOCK_FAILURE"

fun Route.addContributionEndpoint(
  addContributionUseCase: AddContributionUseCase
) {
  post {
    val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
      ?: return@post call.respond(
        HttpStatusCode.BadRequest, "Missing eventId"
      )

    val resourceId = call.parameters["resourceId"]?.let { UUID.fromString(it) }
      ?: return@post call.respond(
        HttpStatusCode.BadRequest, "Missing resourceId"
      )

    Either.catch {
      val user = call.authenticatedUser()
      val requestDto = call.receive<AddContributionRequestDto>()
      requestDto.toDomain(user.email, eventId, resourceId)
    }
      .mapLeft { BadRequestException.InvalidBodyException(it) }
      .flatMap { request -> addContributionUseCase.execute(request) }
      .map { it.toDto() }
      .fold(
        { it.handleFailure(call) },
        { call.respond(HttpStatusCode.OK, it) },
      )
  }
}

private suspend fun Exception.handleFailure(call: ApplicationCall) =
  when (this) {
    is BadRequestException -> call.logAndRespond(
      status = HttpStatusCode.BadRequest,
      responseMessage = ClientErrorMessage.of(
        type = type, detail = message
      ),
      failure = this,
    )
    is AddContributionException ->
      this.handleAddContributionFailure(call)
    else -> call.logAndRespond(
      status = HttpStatusCode.InternalServerError,
      responseMessage = technicalErrorMessage(),
      failure = this,
    )
  }

private suspend fun AddContributionException
  .handleAddContributionFailure(call: ApplicationCall) {
  val rootCause = generateSequence<Throwable>(this.cause) { it.cause }
    .lastOrNull()

  when (rootCause) {
    is OptimisticLockException -> call.logAndRespond(
      status = HttpStatusCode.Conflict,
      responseMessage = ClientErrorMessage.of(
        type = OPTIMISTIC_LOCK_ERROR_TYPE,
        detail = "Resource was modified by another user. " +
          "Please refresh and try again.",
      ),
      failure = this,
    )
    else -> call.logAndRespond(
      status = HttpStatusCode.InternalServerError,
      responseMessage = technicalErrorMessage(),
      failure = this,
    )
  }
}
```

### E.2 — DTO : AddContributionRequestDto

```kotlin
// infrastructure/contribution/add/driving/dto/AddContributionRequestDto.kt

package com.happyrow.core.infrastructure.contribution.add.driving.dto

import com.happyrow.core.domain.contribution.add.model.AddContributionRequest
import java.util.UUID

class AddContributionRequestDto(
  val quantity: Int,
) {
  fun toDomain(
    userEmail: String, eventId: UUID, resourceId: UUID
  ): AddContributionRequest {
    require(quantity > 0) { "Quantity must be greater than 0" }
    return AddContributionRequest(
      userEmail = userEmail,
      eventId = eventId,
      resourceId = resourceId,
      quantity = this.quantity,
    )
  }
}
```

### E.3 — DTO réponse : ContributionDto

```kotlin
// infrastructure/contribution/common/dto/ContributionDto.kt

package com.happyrow.core.infrastructure.contribution.common.dto

import com.happyrow.core.domain.contribution.common.model.Contribution

data class ContributionDto(
  val identifier: String,
  val participantId: String,
  val resourceId: String,
  val quantity: Int,
  val createdAt: Long,
  val updatedAt: Long,
)

fun Contribution.toDto(): ContributionDto = ContributionDto(
  identifier = this.identifier.toString(),
  participantId = this.participantId.toString(),
  resourceId = this.resourceId.toString(),
  quantity = this.quantity,
  createdAt = this.createdAt.toEpochMilli(),
  updatedAt = this.updatedAt.toEpochMilli(),
)
```

### E.4 — Test unitaire : CreateEventUseCaseTestUT

```kotlin
// domain/test/event/create/CreateEventUseCaseTestUT.kt

class CreateEventUseCaseTestUT {
  private val eventRepositoryMock = mockk<EventRepository>()
  private val participantRepositoryMock = mockk<ParticipantRepository>()
  private val useCase = CreateEventUseCase(
    eventRepositoryMock, participantRepositoryMock
  )

  @BeforeEach
  fun beforeEach() { clearAllMocks() }

  @Test
  fun `should create event and auto-add creator as participant`() {
    givenACreateRequest()
      .andAWorkingCreation()
      .whenCreating()
      .then { (result) ->
        result shouldBeRight Persona.Event.anEvent
      }
  }

  @Test
  fun `should transfer error from event repository`() {
    val error = CreateEventRepositoryException(
      Persona.Event.aCreateEventRequest
    )
    givenACreateRequest()
      .andAFailingCreation(error)
      .whenCreating()
      .then { (result, request) ->
        result shouldBeLeft CreateEventException(request, error)
      }
  }

  private fun CreateEventRequest.andAWorkingCreation() = also {
    every {
      eventRepositoryMock.create(Persona.Event.aCreateEventRequest)
    } returns Persona.Event.anEvent.right()
    every {
      participantRepositoryMock.create(any())
    } returns mockk<Participant>(relaxed = true).right()
  }

  private fun CreateEventRequest.andAFailingCreation(
    error: CreateEventRepositoryException
  ) = also {
    every {
      eventRepositoryMock.create(Persona.Event.aCreateEventRequest)
    } returns error.left()
  }

  private fun CreateEventRequest.whenCreating() =
    useCase.create(this) to this

  private fun givenACreateRequest() =
    Persona.Event.aCreateEventRequest
}
```
