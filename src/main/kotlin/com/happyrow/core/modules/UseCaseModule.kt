package com.happyrow.core.modules

import com.happyrow.core.domain.contribution.add.AddContributionUseCase
import com.happyrow.core.domain.contribution.delete.DeleteContributionUseCase
import com.happyrow.core.domain.contribution.reduce.ReduceContributionUseCase
import com.happyrow.core.domain.event.create.CreateEventUseCase
import com.happyrow.core.domain.event.delete.DeleteEventUseCase
import com.happyrow.core.domain.event.get.GetEventsByOrganizerUseCase
import com.happyrow.core.domain.event.update.UpdateEventUseCase
import com.happyrow.core.domain.participant.create.CreateParticipantUseCase
import com.happyrow.core.domain.participant.get.GetParticipantsByEventUseCase
import com.happyrow.core.domain.participant.update.UpdateParticipantUseCase
import com.happyrow.core.domain.resource.create.CreateResourceUseCase
import com.happyrow.core.domain.resource.get.GetResourcesByEventUseCase
import org.koin.dsl.module

val domainModule = module {
  single<CreateEventUseCase> { CreateEventUseCase(get(), get()) }
  single<GetEventsByOrganizerUseCase> { GetEventsByOrganizerUseCase(get()) }
  single<UpdateEventUseCase> { UpdateEventUseCase(get()) }
  single<DeleteEventUseCase> { DeleteEventUseCase(get()) }
  single<CreateParticipantUseCase> { CreateParticipantUseCase(get()) }
  single<GetParticipantsByEventUseCase> { GetParticipantsByEventUseCase(get()) }
  single<UpdateParticipantUseCase> { UpdateParticipantUseCase(get()) }
  single<CreateResourceUseCase> { CreateResourceUseCase(get(), get()) }
  single<GetResourcesByEventUseCase> { GetResourcesByEventUseCase(get()) }
  single<AddContributionUseCase> { AddContributionUseCase(get()) }
  single<DeleteContributionUseCase> { DeleteContributionUseCase(get()) }
  single<ReduceContributionUseCase> { ReduceContributionUseCase(get()) }
}
