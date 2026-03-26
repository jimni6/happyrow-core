package com.happyrow.core.domain.invite.common.error

class InviteLinkRepositoryException(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause)
