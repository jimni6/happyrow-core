package com.happyrow.core.domain.participant.update

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.get.error.GetEventException
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import com.happyrow.core.domain.participant.update.error.ForbiddenParticipantUpdateException
import com.happyrow.core.domain.participant.update.error.ParticipantNotFoundException
import com.happyrow.core.domain.participant.update.model.UpdateParticipantRequest
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

class UpdateParticipantUseCaseTestUT {
  private val participantRepository = mockk<ParticipantRepository>()
  private val eventRepository = mockk<EventRepository>()
  private val useCase = UpdateParticipantUseCase(participantRepository, eventRepository)

  private val participantUserId = UUID.fromString("77777777-7777-7777-7777-777777777777")

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
  }

  @Test
  fun `should update when user is organizer`() {
    val aParticipant = Participant(
      identifier = UUID.fromString("11111111-1111-1111-1111-111111111111"),
      userId = participantUserId,
      eventId = Persona.Event.Properties.identifier,
      status = ParticipantStatus.CONFIRMED,
      joinedAt = Persona.Time.now,
      createdAt = Persona.Time.now,
      updatedAt = Persona.Time.now,
    )
    val request = UpdateParticipantRequest(
      userId = participantUserId,
      eventId = Persona.Event.Properties.identifier,
      status = ParticipantStatus.DECLINED,
    )
    val updated = aParticipant.copy(status = ParticipantStatus.DECLINED)
    every { eventRepository.find(Persona.Event.Properties.identifier) } returns Persona.Event.anEvent.right()
    every {
      participantRepository.find(participantUserId, Persona.Event.Properties.identifier)
    } returns Either.Right(aParticipant)
    every { participantRepository.update(any()) } returns updated.right()

    val result: Either<Exception, Participant> = useCase.execute(
      request,
      Persona.User.aUser.toString(),
    )

    result shouldBeRight updated
    verify(exactly = 1) { participantRepository.update(updated) }
  }

  @Test
  fun `should update when user is self`() {
    val aParticipant = Participant(
      identifier = UUID.fromString("11111111-1111-1111-1111-111111111111"),
      userId = participantUserId,
      eventId = Persona.Event.Properties.identifier,
      status = ParticipantStatus.CONFIRMED,
      joinedAt = Persona.Time.now,
      createdAt = Persona.Time.now,
      updatedAt = Persona.Time.now,
    )
    val request = UpdateParticipantRequest(
      userId = participantUserId,
      eventId = Persona.Event.Properties.identifier,
      status = ParticipantStatus.DECLINED,
    )
    val updated = aParticipant.copy(status = ParticipantStatus.DECLINED)
    every { eventRepository.find(Persona.Event.Properties.identifier) } returns Persona.Event.anEvent.right()
    every {
      participantRepository.find(participantUserId, Persona.Event.Properties.identifier)
    } returns Either.Right(aParticipant)
    every { participantRepository.update(any()) } returns updated.right()

    val result = useCase.execute(
      request,
      participantUserId.toString(),
    )

    result shouldBeRight updated
  }

  @Test
  fun `should return ForbiddenParticipantUpdateException when user is neither organizer nor self`() {
    val request = UpdateParticipantRequest(
      userId = participantUserId,
      eventId = Persona.Event.Properties.identifier,
      status = ParticipantStatus.DECLINED,
    )
    every { eventRepository.find(Persona.Event.Properties.identifier) } returns Persona.Event.anEvent.right()

    val result = useCase.execute(
      request,
      Persona.User.aRequesterUserId,
    )

    val error = result.shouldBeLeft()
    error.shouldBeInstanceOf<ForbiddenParticipantUpdateException>()
    verify(exactly = 0) { participantRepository.find(any(), any()) }
  }

  @Test
  fun `should return ParticipantNotFoundException when event not found`() {
    val request = UpdateParticipantRequest(
      userId = participantUserId,
      eventId = Persona.Event.Properties.identifier,
      status = ParticipantStatus.DECLINED,
    )
    every { eventRepository.find(Persona.Event.Properties.identifier) } returns GetEventException(
      Persona.Event.Properties.identifier,
      RuntimeException("missing"),
    ).left()

    val result = useCase.execute(
      request,
      Persona.User.aUser.toString(),
    )

    val error = result.shouldBeLeft()
    error.shouldBeInstanceOf<ParticipantNotFoundException>()
    (error as ParticipantNotFoundException).userId shouldBe request.userId
    error.eventId shouldBe request.eventId
  }

  @Test
  fun `should return ParticipantNotFoundException when participant not found`() {
    val request = UpdateParticipantRequest(
      userId = participantUserId,
      eventId = Persona.Event.Properties.identifier,
      status = ParticipantStatus.DECLINED,
    )
    every { eventRepository.find(Persona.Event.Properties.identifier) } returns Persona.Event.anEvent.right()
    every {
      participantRepository.find(participantUserId, Persona.Event.Properties.identifier)
    } returns Either.Right(null)

    val result = useCase.execute(
      request,
      participantUserId.toString(),
    )

    val error = result.shouldBeLeft()
    error.shouldBeInstanceOf<ParticipantNotFoundException>()
    (error as ParticipantNotFoundException).userId shouldBe request.userId
    error.eventId shouldBe request.eventId
  }
}
