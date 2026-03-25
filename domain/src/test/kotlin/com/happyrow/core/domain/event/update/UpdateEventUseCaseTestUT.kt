package com.happyrow.core.domain.event.update

import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.common.error.UpdateEventRepositoryException
import com.happyrow.core.domain.event.common.model.event.EventType
import com.happyrow.core.domain.event.update.model.UpdateEventRequest
import com.happyrow.core.persona.Persona
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class UpdateEventUseCaseTestUT {
  private val eventRepositoryMock = mockk<EventRepository>()
  private val useCase = UpdateEventUseCase(eventRepositoryMock)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
  }

  @Test
  fun `should update event successfully`() {
    val request = UpdateEventRequest(
      identifier = Persona.Event.Properties.identifier,
      name = "Updated Name",
      description = "Updated desc",
      eventDate = Persona.Time.now.plus(10, ChronoUnit.DAYS),
      location = "New Location",
      type = EventType.DINER,
      updater = Persona.User.aUser,
    )
    every { eventRepositoryMock.update(request) } returns Persona.Event.anEvent.right()

    val result = useCase.update(request)

    result shouldBeRight Persona.Event.anEvent
    verify { eventRepositoryMock.update(request) }
  }

  @Test
  fun `should transfer error from repository`() {
    val request = UpdateEventRequest(
      identifier = Persona.Event.Properties.identifier,
      name = "Updated Name",
      description = "Updated desc",
      eventDate = Persona.Time.now.plus(10, ChronoUnit.DAYS),
      location = "New Location",
      type = EventType.DINER,
      updater = Persona.User.aUser,
    )
    val repositoryError = UpdateEventRepositoryException(request, null)
    every { eventRepositoryMock.update(request) } returns repositoryError.left()

    val result = useCase.update(request)

    val exception = result.shouldBeLeft()
    exception.request shouldBe request
    exception.cause shouldBe repositoryError
    verify { eventRepositoryMock.update(request) }
  }
}
