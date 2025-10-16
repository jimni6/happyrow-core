package com.happyrow.core.infrastructure.contribution.common.driven

import com.happyrow.core.infrastructure.participant.common.driven.ParticipantTable
import com.happyrow.core.infrastructure.resource.common.driven.ResourceTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object ContributionTable : UUIDTable("configuration.contribution", "id") {
  val participantId = uuid("participant_id").references(ParticipantTable.id)
  val resourceId = uuid("resource_id").references(ResourceTable.id)
  val quantity = integer("quantity")
  val createdAt = timestamp("created_at")
  val updatedAt = timestamp("updated_at")

  init {
    uniqueIndex("uq_contribution_participant_resource", participantId, resourceId)
    index("idx_contribution_participant", false, participantId)
    index("idx_contribution_resource", false, resourceId)
    check("chk_quantity_positive") { quantity greater 0 }
  }
}
