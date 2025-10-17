package com.happyrow.core.infrastructure.contribution

import com.happyrow.core.domain.contribution.add.AddContributionUseCase
import com.happyrow.core.domain.contribution.delete.DeleteContributionUseCase
import com.happyrow.core.infrastructure.contribution.add.driving.addContributionEndpoint
import com.happyrow.core.infrastructure.contribution.delete.driving.deleteContributionEndpoint
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.contributionEndpoints(
  addContributionUseCase: AddContributionUseCase,
  deleteContributionUseCase: DeleteContributionUseCase,
) = route("/events/{eventId}/resources/{resourceId}/contributions") {
  addContributionEndpoint(addContributionUseCase)
  deleteContributionEndpoint(deleteContributionUseCase)
}
