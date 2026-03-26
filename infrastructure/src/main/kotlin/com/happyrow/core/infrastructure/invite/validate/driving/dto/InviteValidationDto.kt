package com.happyrow.core.infrastructure.invite.validate.driving.dto

import com.happyrow.core.domain.invite.common.model.InviteEventSummary
import com.happyrow.core.domain.invite.common.model.InviteValidation

data class InviteValidationDto(
  val token: String,
  val status: String,
  val event: InviteEventSummaryDto?,
  val expiresAt: Long?,
)

data class InviteEventSummaryDto(
  val identifier: String,
  val name: String,
  val eventDate: Long,
  val location: String,
  val type: String,
  val organizerName: String,
  val participantCount: Long,
)

fun InviteValidation.toDto(): InviteValidationDto = InviteValidationDto(
  token = token,
  status = status.name,
  event = event?.toDto(),
  expiresAt = expiresAt?.toEpochMilli(),
)

fun InviteEventSummary.toDto(): InviteEventSummaryDto = InviteEventSummaryDto(
  identifier = identifier.toString(),
  name = name,
  eventDate = eventDate.toEpochMilli(),
  location = location,
  type = type.name,
  organizerName = organizerName,
  participantCount = participantCount,
)
