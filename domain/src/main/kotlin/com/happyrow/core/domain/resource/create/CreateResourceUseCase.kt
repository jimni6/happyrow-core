package com.happyrow.core.domain.resource.create

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.contribution.add.model.AddContributionRequest
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.resource.common.driven.ResourceRepository
import com.happyrow.core.domain.resource.common.model.Resource
import com.happyrow.core.domain.resource.create.error.CreateResourceException
import com.happyrow.core.domain.resource.create.model.CreateResourceRequest

class CreateResourceUseCase(
  private val resourceRepository: ResourceRepository,
  private val contributionRepository: ContributionRepository,
) {
  fun execute(request: CreateResourceRequest): Either<CreateResourceException, Resource> =
    resourceRepository.create(request)
      .mapLeft { CreateResourceException(request, it) }
      .flatMap { resource ->
        // Automatically add the creator's contribution
        contributionRepository.addOrUpdate(
          AddContributionRequest(
            userId = request.userId,
            eventId = request.eventId,
            resourceId = resource.identifier,
            quantity = request.initialQuantity,
          ),
        )
          .map { resource } // Return the resource, not the contribution
          .mapLeft { CreateResourceException(request, it) }
      }
}
