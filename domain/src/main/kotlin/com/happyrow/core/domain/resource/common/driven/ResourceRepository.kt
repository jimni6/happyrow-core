package com.happyrow.core.domain.resource.common.driven

import arrow.core.Either
import com.happyrow.core.domain.resource.common.error.CreateResourceRepositoryException
import com.happyrow.core.domain.resource.common.error.GetResourceRepositoryException
import com.happyrow.core.domain.resource.common.model.Resource
import com.happyrow.core.domain.resource.create.model.CreateResourceRequest
import java.util.UUID

interface ResourceRepository {
  fun create(request: CreateResourceRequest): Either<CreateResourceRepositoryException, Resource>
  fun find(resourceId: UUID): Either<GetResourceRepositoryException, Resource?>
  fun findByEvent(eventId: UUID): Either<GetResourceRepositoryException, List<Resource>>
  fun updateQuantity(
    resourceId: UUID,
    quantityDelta: Int,
    expectedVersion: Int,
  ): Either<GetResourceRepositoryException, Resource>
}
