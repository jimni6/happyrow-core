package com.happyrow.core.domain.contribution.add

import arrow.core.Either
import com.happyrow.core.domain.contribution.add.error.AddContributionException
import com.happyrow.core.domain.contribution.add.model.AddContributionRequest
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.contribution.common.model.Contribution

class AddContributionUseCase(
  private val contributionRepository: ContributionRepository,
) {
  fun execute(request: AddContributionRequest): Either<AddContributionException, Contribution> =
    contributionRepository.addOrUpdate(request)
      .mapLeft { AddContributionException(request, it) }
}
