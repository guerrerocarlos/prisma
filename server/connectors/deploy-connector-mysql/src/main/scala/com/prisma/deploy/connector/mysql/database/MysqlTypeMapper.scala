package com.prisma.deploy.connector.mysql.database

import com.prisma.deploy.connector.jdbc.database.TypeMapper
import com.prisma.gc_values.GCValue
import com.prisma.shared.models.TypeIdentifier
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import org.jooq.DSLContext

case class MySqlTypeMapper() extends TypeMapper {
  override def rawSQLFromParts(
      name: String,
      isRequired: Boolean,
      isList: Boolean,
      typeIdentifier: TypeIdentifier,
      isAutoGenerated: Boolean = false,
      defaultValue: Option[GCValue] = None
  )(implicit dsl: DSLContext): String = {
    val n         = esc(name)
    val nullable  = if (isRequired) "NOT NULL" else "NULL"
    val generated = if (isAutoGenerated) "AUTO_INCREMENT" else ""
    val ty        = rawSqlTypeForScalarTypeIdentifier(isList, typeIdentifier)
    val default   = defaultValue.map(d => s"DEFAULT ${d.value}").getOrElse("")

    s"$n $ty $nullable $default $generated"
  }

  // note: utf8mb4 requires up to 4 bytes per character and includes full utf8 support, including emoticons
  // utf8 requires up to 3 bytes per character and does not have full utf8 support.
  // mysql indexes have a max size of 767 bytes or 191 utf8mb4 characters.
  // We limit enums to 191, and create text indexes over the first 191 characters of the string, but
  // allow the actual content to be much larger.
  // Key columns are utf8_general_ci as this collation is ~10% faster when sorting and requires less memory
  override def rawSqlTypeForScalarTypeIdentifier(isList: Boolean, t: TypeIdentifier.TypeIdentifier): String = t match {
    case _ if isList             => "mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    case TypeIdentifier.String   => "mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    case TypeIdentifier.Boolean  => "boolean"
    case TypeIdentifier.Int      => "int"
    case TypeIdentifier.Float    => "Decimal(65,30)"
    case TypeIdentifier.Cuid     => "char(25) CHARACTER SET utf8 COLLATE utf8_general_ci"
    case TypeIdentifier.UUID     => "char(36) CHARACTER SET utf8 COLLATE utf8_general_ci" // TODO: verify that this is the right thing to do
    case TypeIdentifier.Enum     => "varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    case TypeIdentifier.Json     => "mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    case TypeIdentifier.DateTime => "datetime(3)"
    case _                       => ???
  }
}
