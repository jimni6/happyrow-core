package com.happyrow.core.domain.resource.create

import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.contribution.add.model.AddContributionRequest
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.contribution.common.model.Contribution
import com.happyrow.core.domain.resource.common.driven.ResourceRepository
import com.happyrow.core.domain.resource.common.error.CreateResourceRepositoryException
import com.happyrow.core.domain.resource.common.model.Resource
import com.happyrow.core.domain.resource.common.model.ResourceCategory
import com.happyrow.core.domain.resource.create.error.CreateResourceException
import com.happyrow.core.domain.resource.create.model.CreateResourceRequest
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

class CreateResourceUseCaseTestUT {
  private val resourceRepositoryMock = mockk<ResourceRepository>()
  private val contributionRepositoryMock = mockk<ContributionRepository>()
  private val useCase = CreateResourceUseCase(resourceRepositoryMock, contributionRepositoryMock)

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
  fun `should create resource and add initial contribution`() {
    val request = CreateResourceRequest(
      name = "Chips",
      category = ResourceCategory.FOOD,
      initialQuantity = 3,
      eventId = eventId,
      userEmail = "user@example.com",
    )
    val addRequest = AddContributionRequest(
      userEmail = request.userEmail,
      eventId = request.eventId,
      resourceId = aResource.identifier,
      quantity = request.initialQuantity,
    )

    every { resourceRepositoryMock.create(request) } returns aResource.right()
    every { contributionRepositoryMock.addOrUpdate(addRequest) } returns aContribution.right()
    every { resourceRepositoryMock.find(aResource.identifier) } returns aResource.right()

    val result = useCase.execute(request)

    result shouldBeRight aResource
  }

  @Test
  fun `should transfer error when resource creation fails`() {
    val request = CreateResourceRequest(
      name = "Chips",
      category = ResourceCategory.FOOD,
      initialQuantity = 3,
      eventId = eventId,
      userEmail = "user@example.com",
    )
    val repositoryError = CreateResourceRepositoryException(request, Exception("db"))

    every { resourceRepositoryMock.create(request) } returns repositoryError.left()

    val result = useCase.execute(request)
    val ex: CreateResourceException = result.shouldBeLeft()

    ex.request shouldBe request
    ex.cause shouldBe repositoryError
  }
}
