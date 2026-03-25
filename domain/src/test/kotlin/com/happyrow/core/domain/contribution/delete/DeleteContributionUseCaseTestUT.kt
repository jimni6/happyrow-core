package com.happyrow.core.domain.contribution.delete

import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.contribution.common.error.ContributionRepositoryException
import com.happyrow.core.domain.contribution.delete.error.DeleteContributionException
import com.happyrow.core.persona.Persona
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DeleteContributionUseCaseTestUT {
  private val contributionRepositoryMock = mockk<ContributionRepository>()
  private val useCase = DeleteContributionUseCase(contributionRepositoryMock)

  private val eventId = Persona.Event.Properties.identifier
  private val resourceId = UUID.fromString("22222222-2222-2222-2222-222222222222")
  private val userId = UUID.fromString("88888888-8888-8888-8888-888888888888")

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
  }

  @Test
  fun `should delete contribution successfully`() {
    every { contributionRepositoryMock.delete(userId, eventId, resourceId) } returns Unit.right()

    val result = useCase.execute(userId, eventId, resourceId)

    result shouldBeRight Unit
  }

  @Test
  fun `should transfer error from repository`() {
    val repositoryError = ContributionRepositoryException(resourceId, Exception("failed"))

    every { contributionRepositoryMock.delete(userId, eventId, resourceId) } returns repositoryError.left()

    val result = useCase.execute(userId, eventId, resourceId)
    val ex: DeleteContributionException = result.shouldBeLeft()

    ex.userId shouldBe userId
    ex.resourceId shouldBe resourceId
    ex.cause shouldBe repositoryError
  }
}
