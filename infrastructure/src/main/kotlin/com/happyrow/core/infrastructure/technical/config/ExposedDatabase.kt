package com.happyrow.core.infrastructure.technical.config

import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

class ExposedDatabase(dataSource: DataSource) {
  val database: Database = Database.connect(dataSource)
}
