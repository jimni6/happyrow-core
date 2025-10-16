package com.happyrow.core.infrastructure.contribution.common.driven

import arrow.core.Either
import com.happyrow.core.domain.contribution.common.model.Contribution
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toContribution(): Either<Throwable, Contribution> = Either.catch {
  Contribution(
    identifier = this[ContributionTable.id].value,
    participantId = this[ContributionTable.participantId],
    resourceId = this[ContributionTable.resourceId],
    quantity = this[ContributionTable.quantity],
    createdAt = this[ContributionTable.createdAt],
    updatedAt = this[ContributionTable.updatedAt],
  )
}
