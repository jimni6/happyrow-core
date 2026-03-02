package com.happyrow.core.modules.infrastructure.driven

import com.happyrow.core.AppConfig
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.resource.common.driven.ResourceRepository
import com.happyrow.core.infrastructure.common.driven.event.SqlEventRepository
import com.happyrow.core.infrastructure.contribution.common.driven.SqlContributionRepository
import com.happyrow.core.infrastructure.participant.common.driven.SqlParticipantRepository
import com.happyrow.core.infrastructure.resource.common.driven.SqlResourceRepository
import com.happyrow.core.infrastructure.technical.config.DatabaseInitializer
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import com.happyrow.core.infrastructure.technical.config.dataSource
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val postgresqlModule = module {
  single {
    get<AppConfig>().sql
  }

  single {
    dataSource(get<com.happyrow.core.infrastructure.technical.config.SqlDatabaseConfig>())
  }

  singleOf(::ExposedDatabase)
  singleOf(::DatabaseInitializer)
  singleOf(::SqlEventRepository) bind EventRepository::class
  singleOf(::SqlParticipantRepository) bind ParticipantRepository::class
  singleOf(::SqlResourceRepository) bind ResourceRepository::class
  singleOf(::SqlContributionRepository) bind ContributionRepository::class
}
