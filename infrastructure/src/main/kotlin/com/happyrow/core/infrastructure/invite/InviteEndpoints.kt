package com.happyrow.core.infrastructure.invite

import com.happyrow.core.domain.invite.accept.AcceptInviteUseCase
import com.happyrow.core.domain.invite.create.CreateInviteLinkUseCase
import com.happyrow.core.domain.invite.getactive.GetActiveInviteLinkUseCase
import com.happyrow.core.domain.invite.revoke.RevokeInviteLinkUseCase
import com.happyrow.core.domain.invite.validate.ValidateInviteTokenUseCase
import com.happyrow.core.infrastructure.invite.accept.driving.acceptInviteEndpoint
import com.happyrow.core.infrastructure.invite.create.driving.createInviteLinkEndpoint
import com.happyrow.core.infrastructure.invite.getactive.driving.getActiveInviteLinkEndpoint
import com.happyrow.core.infrastructure.invite.revoke.driving.revokeInviteLinkEndpoint
import com.happyrow.core.infrastructure.invite.validate.driving.validateInviteTokenEndpoint
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.inviteEndpoints(
  createInviteLinkUseCase: CreateInviteLinkUseCase,
  validateInviteTokenUseCase: ValidateInviteTokenUseCase,
  acceptInviteUseCase: AcceptInviteUseCase,
  getActiveInviteLinkUseCase: GetActiveInviteLinkUseCase,
  revokeInviteLinkUseCase: RevokeInviteLinkUseCase,
) {
  route("/events/{eventId}/invites") {
    rateLimit(RateLimitName("mutation")) {
      createInviteLinkEndpoint(createInviteLinkUseCase)
    }
    route("/active") {
      getActiveInviteLinkEndpoint(getActiveInviteLinkUseCase)
    }
    route("/{token}") {
      rateLimit(RateLimitName("mutation")) {
        revokeInviteLinkEndpoint(revokeInviteLinkUseCase)
      }
    }
  }

  route("/invites/{token}") {
    validateInviteTokenEndpoint(validateInviteTokenUseCase)
    route("/accept") {
      rateLimit(RateLimitName("mutation")) {
        acceptInviteEndpoint(acceptInviteUseCase)
      }
    }
  }
}
