package com.happyrow.core.domain.contribution.reduce

import arrow.core.Either
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.contribution.common.model.Contribution
import com.happyrow.core.domain.contribution.reduce.error.ReduceContributionException
import com.happyrow.core.domain.contribution.reduce.model.ReduceContributionRequest

class ReduceContributionUseCase(
  private val contributionRepository: ContributionRepository,
) {
  fun execute(request: ReduceContributionRequest): Either<ReduceContributionException, Contribution?> =
    contributionRepository.reduce(request)
      .mapLeft { ReduceContributionException(request, it) }
}
