package com.happyrow.core.infrastructure.invite.accept.driving.dto

import com.happyrow.core.domain.invite.common.model.AcceptInviteResult

data class AcceptInviteResponseDto(
  val eventId: String,
  val userId: String,
  val userName: String?,
  val status: String,
  val joinedAt: Long,
)

fun AcceptInviteResult.toDto(): AcceptInviteResponseDto = AcceptInviteResponseDto(
  eventId = eventId.toString(),
  userId = userId.toString(),
  userName = userName,
  status = status,
  joinedAt = joinedAt.toEpochMilli(),
)
