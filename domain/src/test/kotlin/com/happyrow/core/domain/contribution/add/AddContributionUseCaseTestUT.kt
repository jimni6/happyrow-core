package com.happyrow.core.domain.contribution.add

import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.contribution.add.error.AddContributionException
import com.happyrow.core.domain.contribution.add.model.AddContributionRequest
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.contribution.common.error.ContributionRepositoryException
import com.happyrow.core.domain.contribution.common.model.Contribution
import com.happyrow.core.persona.Persona
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AddContributionUseCaseTestUT {
  private val contributionRepositoryMock = mockk<ContributionRepository>()
  private val useCase = AddContributionUseCase(contributionRepositoryMock)

  private val eventId = Persona.Event.Properties.identifier
  private val now: Instant = Persona.Time.now
  private val resourceId = UUID.fromString("22222222-2222-2222-2222-222222222222")

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
  fun `should add contribution successfully`() {
    val request = AddContributionRequest(
      userEmail = "user@test.com",
      eventId = eventId,
      resourceId = resourceId,
      quantity = 3,
    )

    every { contributionRepositoryMock.addOrUpdate(request) } returns aContribution.right()

    val result = useCase.execute(request)

    result shouldBeRight aContribution
  }

  @Test
  fun `should transfer error from repository`() {
    val request = AddContributionRequest(
      userEmail = "user@test.com",
      eventId = eventId,
      resourceId = resourceId,
      quantity = 3,
    )
    val repositoryError = ContributionRepositoryException(resourceId, Exception("failed"))

    every { contributionRepositoryMock.addOrUpdate(request) } returns repositoryError.left()

    val result = useCase.execute(request)
    val ex: AddContributionException = result.shouldBeLeft()

    ex.request shouldBe request
    ex.cause shouldBe repositoryError
  }
}
