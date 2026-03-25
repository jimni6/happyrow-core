package com.happyrow.core.domain.event.delete

import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.common.error.DeleteEventRepositoryException
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

class DeleteEventUseCaseTestUT {
  private val eventRepositoryMock = mockk<EventRepository>()
  private val useCase = DeleteEventUseCase(eventRepositoryMock)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
  }

  @Test
  fun `should delete event successfully`() {
    val identifier = Persona.Event.Properties.identifier
    val userId = Persona.User.aUser.toString()
    every { eventRepositoryMock.delete(identifier, userId) } returns Unit.right()

    val result = useCase.delete(identifier, userId)

    result shouldBeRight Unit
    verify { eventRepositoryMock.delete(identifier, userId) }
  }

  @Test
  fun `should transfer error from repository`() {
    val identifier = Persona.Event.Properties.identifier
    val userId = Persona.User.aUser.toString()
    val repositoryError = DeleteEventRepositoryException(identifier, null)
    every { eventRepositoryMock.delete(identifier, userId) } returns repositoryError.left()

    val result = useCase.delete(identifier, userId)

    val exception = result.shouldBeLeft()
    exception.identifier shouldBe identifier
    exception.cause shouldBe repositoryError
    verify { eventRepositoryMock.delete(identifier, userId) }
  }
}
