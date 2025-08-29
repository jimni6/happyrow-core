package com.happyrow.core.infrastructure.technical.config

import javax.sql.DataSource
import org.jetbrains.exposed.sql.Database

class ExposedDatabase(dataSource: DataSource) {
    val database: Database = Database.connect(dataSource)
}
