package com.happyrow.core.domain.participant.get

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.error.GetParticipantRepositoryException
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import com.happyrow.core.domain.participant.get.error.GetParticipantsException
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

class GetParticipantsByEventUseCaseTestUT {
  private val participantRepository = mockk<ParticipantRepository>()
  private val useCase = GetParticipantsByEventUseCase(participantRepository)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
  }

  @Test
  fun `should return participants for event`() {
    val eventId = Persona.Event.Properties.identifier
    val aParticipant = Participant(
      identifier = UUID.fromString("11111111-1111-1111-1111-111111111111"),
      userEmail = "participant@example.com",
      eventId = Persona.Event.Properties.identifier,
      status = ParticipantStatus.CONFIRMED,
      joinedAt = Persona.Time.now,
      createdAt = Persona.Time.now,
      updatedAt = Persona.Time.now,
    )
    every { participantRepository.findByEvent(eventId) } returns listOf(aParticipant).right()

    val result: Either<GetParticipantsException, List<Participant>> = useCase.execute(eventId)

    result shouldBeRight listOf(aParticipant)
    verify(exactly = 1) { participantRepository.findByEvent(eventId) }
  }

  @Test
  fun `should transfer error from participant repository`() {
    val eventId = Persona.Event.Properties.identifier
    val cause = RuntimeException("db error")
    val repoError = GetParticipantRepositoryException(eventId, cause)
    every { participantRepository.findByEvent(eventId) } returns repoError.left()

    val result = useCase.execute(eventId)

    val error = result.shouldBeLeft()
    error.shouldBeInstanceOf<GetParticipantsException>()
    (error as GetParticipantsException).eventId shouldBe eventId
    error.cause shouldBe repoError
  }
}
