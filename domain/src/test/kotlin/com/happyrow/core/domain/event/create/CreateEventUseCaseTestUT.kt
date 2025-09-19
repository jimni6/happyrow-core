package com.happyrow.core.domain.event.create

import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.common.error.CreateEventRepositoryException
import com.happyrow.core.domain.event.create.error.CreateEventException
import com.happyrow.core.domain.event.create.model.CreateEventRequest
import com.happyrow.core.extension.then
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
  private val useCase = CreateEventUseCase(eventRepositoryMock)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
  }

  @Test
  fun `should create and get audience`() {
    givenACreateRequest()
      .andAWorkingCreation()
      .whenCreating()
      .then { (result) ->
        result shouldBeRight Persona.Event.anEvent
      }
  }

  @Test
  fun `should transfer error from audience repository`() {
    val error = CreateEventRepositoryException(Persona.Event.aCreateEventRequest)

    givenACreateRequest()
      .andAFailingCreation(error)
      .whenCreating()
      .then { (result, request) ->
        result shouldBeLeft CreateEventException(request, error)
      }
  }

  private fun CreateEventRequest.andAWorkingCreation() = also {
    every {
      eventRepositoryMock.create(Persona.Event.aCreateEventRequest)
    } returns Persona.Event.anEvent.right()
  }

  private fun CreateEventRequest.andAFailingCreation(error: CreateEventRepositoryException) = also {
    every {
      eventRepositoryMock.create(Persona.Event.aCreateEventRequest)
    } returns error.left()
  }

  private fun CreateEventRequest.whenCreating() = useCase.create(this) to this

  private fun givenACreateRequest() = Persona.Event.aCreateEventRequest
}
