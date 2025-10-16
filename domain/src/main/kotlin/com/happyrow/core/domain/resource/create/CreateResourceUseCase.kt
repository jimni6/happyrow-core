package com.happyrow.core.domain.resource.create

import arrow.core.Either
import com.happyrow.core.domain.resource.common.driven.ResourceRepository
import com.happyrow.core.domain.resource.common.model.Resource
import com.happyrow.core.domain.resource.create.error.CreateResourceException
import com.happyrow.core.domain.resource.create.model.CreateResourceRequest

class CreateResourceUseCase(
  private val resourceRepository: ResourceRepository,
) {
  fun execute(request: CreateResourceRequest): Either<CreateResourceException, Resource> =
    resourceRepository.create(request)
      .mapLeft { CreateResourceException(request, it) }
}
