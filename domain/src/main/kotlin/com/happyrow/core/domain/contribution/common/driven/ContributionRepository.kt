package com.happyrow.core.domain.contribution.common.driven

import arrow.core.Either
import com.happyrow.core.domain.contribution.add.model.AddContributionRequest
import com.happyrow.core.domain.contribution.common.error.ContributionRepositoryException
import com.happyrow.core.domain.contribution.common.model.Contribution
import java.util.UUID

interface ContributionRepository {
  fun addOrUpdate(request: AddContributionRequest): Either<ContributionRepositoryException, Contribution>
  fun delete(userId: UUID, eventId: UUID, resourceId: UUID): Either<ContributionRepositoryException, Unit>
  fun findByResource(resourceId: UUID): Either<ContributionRepositoryException, List<Contribution>>
}
