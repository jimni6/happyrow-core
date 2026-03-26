package com.happyrow.core.modules

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
import com.happyrow.core.domain.user.findorcreate.FindOrCreateUserUseCase
import org.koin.dsl.module

val domainModule = module {
  single<FindOrCreateUserUseCase> { FindOrCreateUserUseCase(get()) }
  single<EventAccessControl> { EventAccessControl(get(), get()) }
  single<CreateEventUseCase> { CreateEventUseCase(get(), get()) }
  single<GetEventsByUserUseCase> { GetEventsByUserUseCase(get()) }
  single<UpdateEventUseCase> { UpdateEventUseCase(get()) }
  single<DeleteEventUseCase> { DeleteEventUseCase(get()) }
  single<CreateParticipantUseCase> { CreateParticipantUseCase(get()) }
  single<GetParticipantsByEventUseCase> { GetParticipantsByEventUseCase(get()) }
  single<UpdateParticipantUseCase> { UpdateParticipantUseCase(get(), get()) }
  single<DeleteParticipantUseCase> { DeleteParticipantUseCase(get(), get()) }
  single<CreateResourceUseCase> { CreateResourceUseCase(get(), get()) }
  single<GetResourcesByEventUseCase> { GetResourcesByEventUseCase(get(), get(), get()) }
  single<AddContributionUseCase> { AddContributionUseCase(get()) }
  single<DeleteContributionUseCase> { DeleteContributionUseCase(get()) }
  single<ReduceContributionUseCase> { ReduceContributionUseCase(get()) }
  single<CreateInviteLinkUseCase> { CreateInviteLinkUseCase(get(), get()) }
  single<ValidateInviteTokenUseCase> { ValidateInviteTokenUseCase(get(), get(), get(), get()) }
  single<AcceptInviteUseCase> { AcceptInviteUseCase(get(), get(), get()) }
  single<GetActiveInviteLinkUseCase> { GetActiveInviteLinkUseCase(get(), get()) }
  single<RevokeInviteLinkUseCase> { RevokeInviteLinkUseCase(get(), get()) }
}
