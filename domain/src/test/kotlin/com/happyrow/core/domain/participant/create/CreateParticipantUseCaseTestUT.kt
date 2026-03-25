package com.happyrow.core.domain.participant.create

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.error.CreateParticipantRepositoryException
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import com.happyrow.core.domain.participant.create.error.CreateParticipantException
import com.happyrow.core.domain.participant.create.model.CreateParticipantRequest
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

class CreateParticipantUseCaseTestUT {
  private val participantRepository = mockk<ParticipantRepository>()
  private val useCase = CreateParticipantUseCase(participantRepository)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
  }

  @Test
  fun `should create participant`() {
    val request = CreateParticipantRequest(
      userEmail = "p@example.com",
      eventId = Persona.Event.Properties.identifier,
    )
    val aParticipant = Participant(
      identifier = UUID.fromString("11111111-1111-1111-1111-111111111111"),
      userEmail = "p@example.com",
      eventId = Persona.Event.Properties.identifier,
      status = ParticipantStatus.CONFIRMED,
      joinedAt = Persona.Time.now,
      createdAt = Persona.Time.now,
      updatedAt = Persona.Time.now,
    )
    every { participantRepository.create(request) } returns aParticipant.right()

    val result: Either<CreateParticipantException, Participant> = useCase.execute(request)

    result shouldBeRight aParticipant
    verify(exactly = 1) { participantRepository.create(request) }
  }

  @Test
  fun `should transfer error from participant repository`() {
    val request = CreateParticipantRequest(
      userEmail = "p@example.com",
      eventId = Persona.Event.Properties.identifier,
    )
    val cause = RuntimeException("db error")
    val repoError = CreateParticipantRepositoryException(request, cause)
    every { participantRepository.create(request) } returns repoError.left()

    val result = useCase.execute(request)

    val error = result.shouldBeLeft()
    error.shouldBeInstanceOf<CreateParticipantException>()
    (error as CreateParticipantException).request shouldBe request
    error.cause shouldBe repoError
  }
}
