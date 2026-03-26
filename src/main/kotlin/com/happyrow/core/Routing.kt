package com.happyrow.core

import com.happyrow.core.domain.contribution.add.AddContributionUseCase
import com.happyrow.core.domain.contribution.delete.DeleteContributionUseCase
import com.happyrow.core.domain.contribution.reduce.ReduceContributionUseCase
import com.happyrow.core.domain.event.common.EventAccessControl
import com.happyrow.core.domain.event.create.CreateEventUseCase
import com.happyrow.core.domain.event.delete.DeleteEventUseCase
import com.happyrow.core.domain.event.get.GetEventsByUserUseCase
import com.happyrow.core.domain.event.update.UpdateEventUseCase
import com.happyrow.core.domain.invite.accept.AcceptInviteUseCase
import com.happyrow.core.domain.invite.create.CreateInviteLinkUseCase
import com.happyrow.core.domain.invite.getactive.GetActiveInviteLinkUseCase
import com.happyrow.core.domain.invite.revoke.RevokeInviteLinkUseCase
import com.happyrow.core.domain.invite.validate.ValidateInviteTokenUseCase
import com.happyrow.core.domain.participant.create.CreateParticipantUseCase
import com.happyrow.core.domain.participant.delete.DeleteParticipantUseCase
import com.happyrow.core.domain.participant.get.GetParticipantsByEventUseCase
import com.happyrow.core.domain.participant.update.UpdateParticipantUseCase
import com.happyrow.core.domain.resource.create.CreateResourceUseCase
import com.happyrow.core.domain.resource.get.GetResourcesByEventUseCase
import com.happyrow.core.infrastructure.contribution.contributionEndpoints
import com.happyrow.core.infrastructure.event.eventEndpoints
import com.happyrow.core.infrastructure.invite.inviteEndpoints
import com.happyrow.core.infrastructure.participant.participantEndpoints
import com.happyrow.core.infrastructure.resource.resourceEndpoints
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

const val BASE_PATH = "/event/configuration"

fun Application.configureRouting() {
  val createEventUseCase: CreateEventUseCase by inject()
  val getEventsByUserUseCase: GetEventsByUserUseCase by inject()
  val updateEventUseCase: UpdateEventUseCase by inject()
  val deleteEventUseCase: DeleteEventUseCase by inject()
  val createParticipantUseCase: CreateParticipantUseCase by inject()
  val getParticipantsByEventUseCase: GetParticipantsByEventUseCase by inject()
  val updateParticipantUseCase: UpdateParticipantUseCase by inject()
  val deleteParticipantUseCase: DeleteParticipantUseCase by inject()
  val createResourceUseCase: CreateResourceUseCase by inject()
  val getResourcesByEventUseCase: GetResourcesByEventUseCase by inject()
  val addContributionUseCase: AddContributionUseCase by inject()
  val deleteContributionUseCase: DeleteContributionUseCase by inject()
  val reduceContributionUseCase: ReduceContributionUseCase by inject()
  val eventAccessControl: EventAccessControl by inject()
  val createInviteLinkUseCase: CreateInviteLinkUseCase by inject()
  val validateInviteTokenUseCase: ValidateInviteTokenUseCase by inject()
  val acceptInviteUseCase: AcceptInviteUseCase by inject()
  val getActiveInviteLinkUseCase: GetActiveInviteLinkUseCase by inject()
  val revokeInviteLinkUseCase: RevokeInviteLinkUseCase by inject()

  routing {
    route(BASE_PATH) {
      route("/api/v1") {
        eventEndpoints(createEventUseCase, getEventsByUserUseCase, updateEventUseCase, deleteEventUseCase)
        participantEndpoints(
          createParticipantUseCase,
          getParticipantsByEventUseCase,
          updateParticipantUseCase,
          deleteParticipantUseCase,
          eventAccessControl,
        )
        resourceEndpoints(createResourceUseCase, getResourcesByEventUseCase, eventAccessControl)
        contributionEndpoints(addContributionUseCase, deleteContributionUseCase, reduceContributionUseCase)
        inviteEndpoints(
          createInviteLinkUseCase,
          validateInviteTokenUseCase,
          acceptInviteUseCase,
          getActiveInviteLinkUseCase,
          revokeInviteLinkUseCase,
        )
      }
    }

    get("/") {
      call.respondText("Hello from happyrow-core!", ContentType.Text.Plain)
    }

    get("/health") {
      call.respond(mapOf("status" to "UP"))
    }

    get("/info") {
      call.respond(
        mapOf(
          "name" to "happyrow-core",
          "status" to "running",
        ),
      )
    }
  }
}
