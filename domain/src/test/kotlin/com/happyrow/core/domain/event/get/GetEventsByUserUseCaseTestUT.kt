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
  fun `should return events page from repository`() {
    val userId = Persona.User.aUser.toString()
    val pageRequest = PageRequest(0, 20)
    val expectedPage = Page.of(emptyList<com.happyrow.core.domain.event.common.model.event.Event>(), pageRequest, 0L)

    every { eventRepositoryMock.findByUser(userId, pageRequest) } returns expectedPage.right()

    val result = useCase.execute(userId, pageRequest)

    result shouldBeRight expectedPage
    verify { eventRepositoryMock.findByUser(userId, pageRequest) }
  }

  @Test
  fun `should propagate repository errors`() {
    val userId = Persona.User.aUser.toString()
    val pageRequest = PageRequest(0, 20)
    val repositoryError = GetEventException(null, RuntimeException("db error"))

    every { eventRepositoryMock.findByUser(userId, pageRequest) } returns repositoryError.left()

    val result = useCase.execute(userId, pageRequest)

    result shouldBeLeft repositoryError
    verify { eventRepositoryMock.findByUser(userId, pageRequest) }
  }
}
