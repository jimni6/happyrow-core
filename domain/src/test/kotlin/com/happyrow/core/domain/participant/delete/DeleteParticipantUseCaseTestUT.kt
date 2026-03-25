package com.happyrow.core.domain.participant.delete

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.get.error.GetEventException
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import com.happyrow.core.domain.participant.delete.error.ForbiddenParticipantDeleteException
import com.happyrow.core.domain.participant.update.error.ParticipantNotFoundException
import com.happyrow.core.persona.Persona
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DeleteParticipantUseCaseTestUT {
  private val participantRepository = mockk<ParticipantRepository>()
  private val eventRepository = mockk<EventRepository>()
  private val useCase = DeleteParticipantUseCase(participantRepository, eventRepository)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
  }

  @Test
  fun `should delete when user is organizer`() {
    val userEmail = "participant@example.com"
    val eventId = Persona.Event.Properties.identifier
    val aParticipant = Participant(
      identifier = UUID.fromString("11111111-1111-1111-1111-111111111111"),
      userEmail = userEmail,
      eventId = Persona.Event.Properties.identifier,
      status = ParticipantStatus.CONFIRMED,
      joinedAt = Persona.Time.now,
      createdAt = Persona.Time.now,
      updatedAt = Persona.Time.now,
    )
    every { eventRepository.find(eventId) } returns Persona.Event.anEvent.right()
    every { participantRepository.find(userEmail, eventId) } returns Either.Right(aParticipant)
    every { participantRepository.delete(userEmail, eventId) } returns Unit.right()

    val result: Either<Exception, Unit> =
      useCase.execute(userEmail, eventId, Persona.User.aUser.toString())

    result shouldBeRight Unit
    verify(exactly = 1) { participantRepository.delete(userEmail, eventId) }
  }

  @Test
  fun `should return ForbiddenParticipantDeleteException when user is not organizer`() {
    val userEmail = "participant@example.com"
    val eventId = Persona.Event.Properties.identifier
    every { eventRepository.find(eventId) } returns Persona.Event.anEvent.right()

    val result = useCase.execute(userEmail, eventId, Persona.User.aRequesterUserId)

    val error = result.shouldBeLeft()
    error.shouldBeInstanceOf<ForbiddenParticipantDeleteException>()
    verify(exactly = 0) { participantRepository.find(any(), any()) }
    verify(exactly = 0) { participantRepository.delete(any(), any()) }
  }

  @Test
  fun `should return ParticipantNotFoundException when event not found`() {
    val userEmail = "participant@example.com"
    val eventId = Persona.Event.Properties.identifier
    every { eventRepository.find(eventId) } returns GetEventException(
      eventId,
      RuntimeException("missing"),
    ).left()

    val result = useCase.execute(userEmail, eventId, Persona.User.aUser.toString())

    val error = result.shouldBeLeft()
    error.shouldBeInstanceOf<ParticipantNotFoundException>()
    (error as ParticipantNotFoundException).userEmail shouldBe userEmail
    error.eventId shouldBe eventId
    verify(exactly = 0) { participantRepository.delete(any(), any()) }
  }
}
