package com.happyrow.core.domain.event.get

import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.common.model.Page
import com.happyrow.core.domain.common.model.PageRequest
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.get.error.GetEventException
import com.happyrow.core.persona.Persona
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetEventsByUserUseCaseTestUT {
  private val eventRepositoryMock = mockk<EventRepository>()
  private val useCase = GetEventsByUserUseCase(eventRepositoryMock)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
  }

  @Test
  fun `should return events for user`() {
    val userId = Persona.User.aUser.toString()
    val userEmail = Persona.User.aUserEmail
    val pageRequest = PageRequest()
    val event = Persona.Event.anEvent
    val expectedPage = Page.of(listOf(event), pageRequest, 1L)
    every { eventRepositoryMock.findByUser(userId, userEmail, pageRequest) } returns expectedPage.right()

    val result = useCase.execute(userId, userEmail, pageRequest)

    result shouldBeRight expectedPage
    verify { eventRepositoryMock.findByUser(userId, userEmail, pageRequest) }
  }

  @Test
  fun `should transfer error from repository`() {
    val userId = Persona.User.aUser.toString()
    val userEmail = Persona.User.aUserEmail
    val pageRequest = PageRequest()
    val cause = RuntimeException("repository failure")
    val repositoryError = GetEventException(null, cause)
    every { eventRepositoryMock.findByUser(userId, userEmail, pageRequest) } returns repositoryError.left()

    val result = useCase.execute(userId, userEmail, pageRequest)

    result shouldBeLeft repositoryError
    verify { eventRepositoryMock.findByUser(userId, userEmail, pageRequest) }
  }
}
