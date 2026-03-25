package com.happyrow.core.infrastructure.participant.common.dto

import com.happyrow.core.domain.participant.common.model.Participant

data class ParticipantDto(
  val identifier: String,
  val userId: String,
  val userName: String? = null,
  val eventId: String,
  val status: String,
  val joinedAt: Long,
  val createdAt: Long,
  val updatedAt: Long,
)

fun Participant.toDto(): ParticipantDto = ParticipantDto(
  identifier = this.identifier.toString(),
  userId = this.userId.toString(),
  userName = this.userName,
  eventId = this.eventId.toString(),
  status = this.status.name,
  joinedAt = this.joinedAt.toEpochMilli(),
  createdAt = this.createdAt.toEpochMilli(),
  updatedAt = this.updatedAt.toEpochMilli(),
)
