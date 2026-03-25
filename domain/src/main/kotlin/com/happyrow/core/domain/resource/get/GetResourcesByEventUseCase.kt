package com.happyrow.core.domain.resource.get

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.common.model.Page
import com.happyrow.core.domain.common.model.PageRequest
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.resource.common.driven.ResourceRepository
import com.happyrow.core.domain.resource.common.model.ContributorInfo
import com.happyrow.core.domain.resource.common.model.ResourceWithContributors
import com.happyrow.core.domain.resource.get.error.GetResourcesException
import java.util.UUID

class GetResourcesByEventUseCase(
  private val resourceRepository: ResourceRepository,
  private val contributionRepository: ContributionRepository,
  private val participantRepository: ParticipantRepository,
) {
  fun execute(eventId: UUID, pageRequest: PageRequest): Either<GetResourcesException, Page<ResourceWithContributors>> {
    return resourceRepository.findByEvent(eventId, pageRequest)
      .mapLeft { GetResourcesException(eventId, it) }
      .flatMap { resourcePage ->
        val participantCache = mutableMapOf<UUID, String>()
        val result = mutableListOf<ResourceWithContributors>()

        for (resource in resourcePage.content) {
          val contributionsResult = contributionRepository.findByResource(resource.identifier)
            .mapLeft {
              GetResourcesException(
                eventId,
                Exception("Failed to load contributions for resource ${resource.identifier}", it),
              )
            }

          when (contributionsResult) {
            is Either.Left -> return@execute contributionsResult
            is Either.Right -> {
              val contributions = contributionsResult.value
              val contributorInfos = contributions.mapNotNull { contribution ->
                val userId = participantCache.getOrPut(contribution.participantId) {
                  participantRepository.findById(contribution.participantId)
                    .fold(
                      { null },
                      { it?.userEmail },
                    ) ?: return@mapNotNull null
                }

                ContributorInfo(
                  userId = userId,
                  quantity = contribution.quantity,
                  contributedAt = contribution.createdAt.toEpochMilli(),
                )
              }

              result.add(
                ResourceWithContributors(
                  resource = resource,
                  contributors = contributorInfos,
                ),
              )
            }
          }
        }

        Either.Right(
          Page(
            content = result,
            page = resourcePage.page,
            size = resourcePage.size,
            totalElements = resourcePage.totalElements,
            totalPages = resourcePage.totalPages,
          ),
        )
      }
  }
}
