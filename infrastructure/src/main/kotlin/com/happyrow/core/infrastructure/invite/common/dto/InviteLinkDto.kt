package com.happyrow.core.infrastructure.invite.common.dto

import com.happyrow.core.domain.invite.common.model.InviteLink

data class InviteLinkDto(
  val token: String,
  val eventId: String,
  val inviteUrl: String,
  val createdAt: Long,
  val expiresAt: Long,
  val maxUses: Int?,
  val currentUses: Int,
  val status: String,
  val createdBy: String,
)

fun InviteLink.toDto(): InviteLinkDto {
  val baseUrl = System.getenv("INVITE_URL_BASE") ?: "https://happyrow.app"
  return InviteLinkDto(
    token = token,
    eventId = eventId.toString(),
    inviteUrl = "$baseUrl/invite/$token",
    createdAt = createdAt.toEpochMilli(),
    expiresAt = expiresAt.toEpochMilli(),
    maxUses = maxUses,
    currentUses = currentUses,
    status = status.name,
    createdBy = createdBy.toString(),
  )
}
