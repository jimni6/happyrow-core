package com.happyrow.core.domain.invite.revoke.error

class RevokeInviteLinkException(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause)
