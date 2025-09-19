package com.happyrow.core.infrastructure.technical.exposed

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.jdbc.JdbcConnectionImpl
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.postgresql.jdbc.PgArray
import java.util.UUID
import kotlin.reflect.KClass

fun <T : Any> Table.array(
  name: String,
  elementType: ColumnType<T>,
  clazz: KClass<T>,
  elementFromDb: (Any?) -> T,
): Column<List<T>> = registerColumn(name, ArrayColumnType(elementType, elementFromDb, clazz))

class ArrayColumnType<T : Any>(
  private val elementType: ColumnType<T>,
  private val elementFromDb: (Any?) -> T,
  private val clazz: KClass<T>,
) : ColumnType<List<T>>() {
  override fun sqlType(): String = "${elementType.sqlType()}[]"

  override fun valueToDB(value: List<T>?): Any? = value?.let {
    val jdbcConnection = (TransactionManager.current().connection as JdbcConnectionImpl).connection
    jdbcConnection.createArrayOf(elementType.sqlType(), value.toArray())
  } ?: super.valueToDB(value)

  override fun valueFromDB(value: Any): List<T> = when (value) {
    is PgArray -> (value.array as Array<*>).asList().map { elementFromDb(it) }
    else -> error("Array does not support for this database")
  }

  @Suppress("ReturnCount")
  override fun notNullValueToDB(value: List<T>): Any {
    if (value.isEmpty()) {
      return "'{}'"
    }

    val jdbcConnection = (TransactionManager.current().connection as JdbcConnectionImpl).connection
    return jdbcConnection.createArrayOf(elementType.sqlType(), value.toArray())
      ?: error("Can't create non null array for $value")
  }

  private fun List<T>.toArray() = when (clazz) {
    UUID::class -> (this as List<*>).toTypedArray()
    else -> error("PgArray of $clazz are not supported. Please add it to supported types")
  }
}
