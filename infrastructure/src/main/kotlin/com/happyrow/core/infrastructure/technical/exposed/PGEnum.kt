package com.happyrow.core.infrastructure.technical.exposed

import org.postgresql.util.PGobject

class PGEnum<T : Enum<T>>(enumTypeName: String, enumValue: T?) : PGobject() {
  init {
    value = enumValue?.name
    type = enumTypeName
  }
}
