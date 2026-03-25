package com.happyrow.core.domain.resource.get

import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.contribution.common.model.Contribution
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import com.happyrow.core.domain.resource.common.driven.ResourceRepository
import com.happyrow.core.domain.resource.common.error.GetResourceRepositoryException
import com.happyrow.core.domain.resource.common.model.ContributorInfo
import com.happyrow.core.domain.resource.common.model.Resource
import com.happyrow.core.domain.resource.common.model.ResourceCategory
import com.happyrow.core.domain.resource.common.model.ResourceWithContributors
import com.happyrow.core.domain.resource.get.error.GetResourcesException
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

class GetResourcesByEventUseCaseTestUT {
  private val resourceRepositoryMock = mockk<ResourceRepository>()
  private val contributionRepositoryMock = mockk<ContributionRepository>()
  private val participantRepositoryMock = mockk<ParticipantRepository>()
  private val useCase =
    GetResourcesByEventUseCase(resourceRepositoryMock, contributionRepositoryMock, participantRepositoryMock)

  private val eventId = Persona.Event.Properties.identifier
  private val now: Instant = Persona.Time.now

  private val aResource = Resource(
    identifier = UUID.fromString("22222222-2222-2222-2222-222222222222"),
    name = "Chips",
    category = ResourceCategory.FOOD,
    suggestedQuantity = 5,
    currentQuantity = 3,
    eventId = Persona.Event.Properties.identifier,
    version = 1,
    createdAt = now,
    updatedAt = now,
  )

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
  fun `should return resources with contributors`() {
    val participant = Participant(
      identifier = aContribution.participantId,
      userEmail = "user@test.com",
      eventId = eventId,
      status = ParticipantStatus.CONFIRMED,
      joinedAt = Persona.Time.now,
      createdAt = Persona.Time.now,
      updatedAt = Persona.Time.now,
    )
    val expected = listOf(
      ResourceWithContributors(
        resource = aResource,
        contributors = listOf(
          ContributorInfo(
            userId = "user@test.com",
            quantity = 3,
            contributedAt = aContribution.createdAt.toEpochMilli(),
          ),
        ),
      ),
    )

    every { resourceRepositoryMock.findByEvent(eventId) } returns listOf(aResource).right()
    every { contributionRepositoryMock.findByResource(aResource.identifier) } returns listOf(aContribution).right()
    every { participantRepositoryMock.findById(aContribution.participantId) } returns participant.right()

    val result = useCase.execute(eventId)

    result shouldBeRight expected
  }

  @Test
  fun `should return empty list when no resources`() {
    every { resourceRepositoryMock.findByEvent(eventId) } returns emptyList<Resource>().right()

    val result = useCase.execute(eventId)

    result shouldBeRight emptyList()
  }

  @Test
  fun `should transfer error when resource loading fails`() {
    val repositoryError = GetResourceRepositoryException(eventId, Exception("db"))
    every { resourceRepositoryMock.findByEvent(eventId) } returns repositoryError.left()

    val result = useCase.execute(eventId)
    val ex: GetResourcesException = result.shouldBeLeft()

    ex.eventId shouldBe eventId
    ex.cause shouldBe repositoryError
  }
}
