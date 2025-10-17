package com.happyrow.core.infrastructure.resource.common.driven

import arrow.core.Either
import com.happyrow.core.domain.resource.common.model.Resource
import com.happyrow.core.domain.resource.common.model.ResourceCategory
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toResource(): Either<Throwable, Resource> = Either.catch {
  Resource(
    identifier = this[ResourceTable.id].value,
    name = this[ResourceTable.name],
    category = ResourceCategory.valueOf(this[ResourceTable.category]),
    suggestedQuantity = this[ResourceTable.suggestedQuantity],
    currentQuantity = this[ResourceTable.currentQuantity],
    eventId = this[ResourceTable.eventId],
    version = this[ResourceTable.version],
    createdAt = this[ResourceTable.createdAt],
    updatedAt = this[ResourceTable.updatedAt],
  )
}
