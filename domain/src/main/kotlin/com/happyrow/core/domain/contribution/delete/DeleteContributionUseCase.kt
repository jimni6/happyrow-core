package com.happyrow.core.domain.contribution.delete

import arrow.core.Either
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.contribution.delete.error.DeleteContributionException
import java.util.UUID

class DeleteContributionUseCase(
  private val contributionRepository: ContributionRepository,
) {
  fun execute(userId: UUID, eventId: UUID, resourceId: UUID): Either<DeleteContributionException, Unit> =
    contributionRepository.delete(userId, eventId, resourceId)
      .mapLeft { DeleteContributionException(userId, resourceId, it) }
}
