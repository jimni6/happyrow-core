package com.happyrow.core.domain.resource.get

import arrow.core.Either
import com.happyrow.core.domain.resource.common.driven.ResourceRepository
import com.happyrow.core.domain.resource.common.model.Resource
import com.happyrow.core.domain.resource.get.error.GetResourcesException
import java.util.UUID

class GetResourcesByEventUseCase(
  private val resourceRepository: ResourceRepository,
) {
  fun execute(eventId: UUID): Either<GetResourcesException, List<Resource>> = resourceRepository.findByEvent(eventId)
    .mapLeft { GetResourcesException(eventId, it) }
}
