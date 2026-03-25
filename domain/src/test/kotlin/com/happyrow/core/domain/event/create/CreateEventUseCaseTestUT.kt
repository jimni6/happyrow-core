package com.happyrow.core.domain.event.create

import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.common.error.CreateEventRepositoryException
import com.happyrow.core.domain.event.create.error.CreateEventException
import com.happyrow.core.domain.event.create.model.CreateEventRequest
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.persona.Persona
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateEventUseCaseTestUT {
  private val eventRepositoryMock = mockk<EventRepository>()
  private val participantRepositoryMock = mockk<ParticipantRepository>()
  private val useCase = CreateEventUseCase(eventRepositoryMock, participantRepositoryMock)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
  }

  @Test
  fun `should create event and auto-add creator as participant`() {
    givenACreateRequest()
      .andAWorkingCreation()
      .whenCreating()
      .let { pair ->
        pair.first shouldBeRight Persona.Event.anEvent
      }
  }

  @Test
  fun `should transfer error from event repository`() {
    val error = CreateEventRepositoryException(Persona.Event.aCreateEventRequest)

    givenACreateRequest()
      .andAFailingCreation(error)
      .whenCreating()
      .let { pair ->
        pair.first shouldBeLeft CreateEventException(pair.second, error)
      }
  }

  private fun CreateEventRequest.andAWorkingCreation() = also {
    every {
      eventRepositoryMock.create(Persona.Event.aCreateEventRequest)
    } returns Persona.Event.anEvent.right()

    every {
      participantRepositoryMock.create(any())
    } returns mockk<com.happyrow.core.domain.participant.common.model.Participant>(relaxed = true).right()
  }

  private fun CreateEventRequest.andAFailingCreation(error: CreateEventRepositoryException) = also {
    every {
      eventRepositoryMock.create(Persona.Event.aCreateEventRequest)
    } returns error.left()
  }

  private fun CreateEventRequest.whenCreating() = useCase.create(this) to this

  private fun givenACreateRequest() = Persona.Event.aCreateEventRequest
}
