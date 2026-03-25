package com.happyrow.core.domain.participant.create

import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.error.CreateParticipantRepositoryException
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import com.happyrow.core.domain.participant.create.error.CreateParticipantException
import com.happyrow.core.domain.participant.create.model.CreateParticipantRequest
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
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
    val userId = UUID.fromString("33333333-3333-3333-3333-333333333333")
    val eventId = UUID.fromString("44444444-4444-4444-4444-444444444444")
    val request = CreateParticipantRequest(
      userId = userId,
      eventId = eventId,
      status = ParticipantStatus.CONFIRMED,
    )
    val aParticipant = Participant(
      identifier = UUID.fromString("55555555-5555-5555-5555-555555555555"),
      userId = userId,
      eventId = eventId,
      status = ParticipantStatus.CONFIRMED,
      joinedAt = java.time.Instant.now(),
      createdAt = java.time.Instant.now(),
      updatedAt = java.time.Instant.now(),
    )

    every { participantRepository.create(request) } returns aParticipant.right()

    val result = useCase.execute(request)

    result shouldBeRight aParticipant
    verify(exactly = 1) { participantRepository.create(request) }
  }

  @Test
  fun `should transfer error from participant repository`() {
    val request = CreateParticipantRequest(
      userId = UUID.fromString("33333333-3333-3333-3333-333333333333"),
      eventId = UUID.fromString("44444444-4444-4444-4444-444444444444"),
    )
    val repoError = CreateParticipantRepositoryException(request, RuntimeException("db error"))

    every { participantRepository.create(request) } returns repoError.left()

    val result = useCase.execute(request)

    result shouldBeLeft CreateParticipantException(request, repoError)
  }
}
