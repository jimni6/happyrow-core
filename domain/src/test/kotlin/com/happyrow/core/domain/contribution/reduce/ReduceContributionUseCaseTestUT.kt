package com.happyrow.core.domain.contribution.reduce

import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.contribution.common.error.ContributionRepositoryException
import com.happyrow.core.domain.contribution.common.model.Contribution
import com.happyrow.core.domain.contribution.reduce.error.ReduceContributionException
import com.happyrow.core.domain.contribution.reduce.model.ReduceContributionRequest
import com.happyrow.core.persona.Persona
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ReduceContributionUseCaseTestUT {
  private val contributionRepositoryMock = mockk<ContributionRepository>()
  private val useCase = ReduceContributionUseCase(contributionRepositoryMock)

  private val eventId = Persona.Event.Properties.identifier
  private val now: Instant = Persona.Time.now
  private val resourceId = UUID.fromString("22222222-2222-2222-2222-222222222222")
  private val userId = UUID.fromString("88888888-8888-8888-8888-888888888888")

  private val aContribution = Contribution(
    identifier = UUID.fromString("33333333-3333-3333-3333-333333333333"),
    participantId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
    resourceId = UUID.fromString("22222222-2222-2222-2222-222222222222"),
    quantity = 3,
    createdAt = now,
    updatedAt = now,
  )

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
  }

  @Test
  fun `should reduce contribution successfully`() {
    val request = ReduceContributionRequest(
      userId = userId,
      eventId = eventId,
      resourceId = resourceId,
      quantity = 1,
    )

    every { contributionRepositoryMock.reduce(request) } returns aContribution.right()

    val result = useCase.execute(request)

    result shouldBeRight aContribution
  }

  @Test
  fun `should return null when contribution fully reduced`() {
    val request = ReduceContributionRequest(
      userId = userId,
      eventId = eventId,
      resourceId = resourceId,
      quantity = 1,
    )

    every { contributionRepositoryMock.reduce(request) } returns null.right()

    val result = useCase.execute(request)

    result shouldBeRight null
  }

  @Test
  fun `should transfer error from repository`() {
    val request = ReduceContributionRequest(
      userId = userId,
      eventId = eventId,
      resourceId = resourceId,
      quantity = 1,
    )
    val repositoryError = ContributionRepositoryException(resourceId, Exception("failed"))

    every { contributionRepositoryMock.reduce(request) } returns repositoryError.left()

    val result = useCase.execute(request)

    result shouldBeLeft ReduceContributionException(request, repositoryError)
  }
}
