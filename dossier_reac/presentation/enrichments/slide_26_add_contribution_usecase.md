# Slide 26 — AddContributionUseCase (extraits de code)

> **Type** : EXISTANT — Code reel du projet

## Extrait 1 : Le use case (`AddContributionUseCase.kt`)

```kotlin
class AddContributionUseCase(
  private val contributionRepository: ContributionRepository,
) {
  fun execute(request: AddContributionRequest): Either<AddContributionException, Contribution> =
    contributionRepository.addOrUpdate(request)
      .mapLeft { AddContributionException(request, it) }
}
```

**Source** : `domain/src/main/kotlin/.../contribution/add/AddContributionUseCase.kt`

## Extrait 2 : Le port (interface repository)

```kotlin
interface ContributionRepository {
  fun addOrUpdate(request: AddContributionRequest): Either<ContributionRepositoryException, Contribution>
  fun reduce(request: ReduceContributionRequest): Either<ContributionRepositoryException, Contribution?>
  fun delete(userEmail: String, eventId: UUID, resourceId: UUID): Either<ContributionRepositoryException, Unit>
  fun findByResource(resourceId: UUID): Either<ContributionRepositoryException, List<Contribution>>
}
```

**Source** : `domain/src/main/kotlin/.../contribution/common/driven/ContributionRepository.kt`

## Extrait 3 : Le modele de requete

```kotlin
data class AddContributionRequest(
  val userEmail: String,
  val eventId: UUID,
  val resourceId: UUID,
  val quantity: Int,
)
```

**Source** : `domain/src/main/kotlin/.../contribution/add/model/AddContributionRequest.kt`

## Extrait 4 : L'implementation SQL (logique cle de `addOrUpdate`)

```kotlin
override fun addOrUpdate(request: AddContributionRequest): Either<ContributionRepositoryException, Contribution> {
    return participantRepository.findOrCreate(request.userEmail, request.eventId)
      .mapLeft { ContributionRepositoryException(request.resourceId, it) }
      .flatMap { participant ->
        // Verifier si une contribution existe deja
        existingContribution.flatMap { existing ->
          if (existing != null) {
            // Mise a jour : delta = nouvelle quantite - ancienne quantite
            val delta = request.quantity - oldQuantity
            updateContribution(participant.identifier, request.resourceId, request.quantity)
              .flatMap { contribution ->
                resourceRepository.updateQuantity(request.resourceId, delta, resource.version)
                  .map { contribution }
              }
          } else {
            // Creation : delta = quantite demandee
            createContribution(participant.identifier, request.resourceId, request.quantity)
              .flatMap { contribution ->
                resourceRepository.updateQuantity(request.resourceId, request.quantity, resource.version)
                  .map { contribution }
              }
          }
        }
      }
  }
```

**Source** : `infrastructure/src/main/kotlin/.../contribution/common/driven/SqlContributionRepository.kt` (simplifie)

## Comparaison Use Case vs Implementation

| Aspect | AddContributionUseCase (domaine) | SqlContributionRepository (infra) |
|--------|----------------------------------|-----------------------------------|
| **Responsabilite** | Orchestrer et wrapper les erreurs | Trouver/creer participant, gerer contribution, verrou optimiste |
| **Dependances** | ContributionRepository (port) | ParticipantRepository, ResourceRepository, ExposedDatabase |
| **Complexite** | Simple delegation (`mapLeft`) | Logique riche (findOrCreate, delta, updateQuantity) |
| **Connaissance technique** | Zero (pas de SQL, pas de transaction) | Transactions Exposed, UPDATE conditionnel |

## Ce qu'il faut dire (notes orales)

Ce use case illustre bien la separation des responsabilites dans l'architecture hexagonale.

Le `AddContributionUseCase` est **volontairement simple** : il delegue au repository et enveloppe l'erreur avec `mapLeft` pour ajouter du contexte metier. Il ne sait rien de la base de donnees, des transactions ou du verrou optimiste.

Toute la complexite technique est dans le `SqlContributionRepository` :
1. D'abord, il cherche ou cree le participant avec `findOrCreate`
2. Ensuite, il verifie si une contribution existe deja
3. Si oui, il calcule le **delta** (nouvelle quantite - ancienne quantite) et met a jour
4. Si non, il cree une nouvelle contribution
5. Dans les deux cas, il appelle `updateQuantity` sur la ressource avec le **verrou optimiste**

Ce decoupage montre que le use case orchestre a un niveau metier, tandis que l'infrastructure gere la complexite technique. Le use case reste testable avec un simple mock, sans base de donnees.
