package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.SerdeColumnInfo;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Data model for a parsed {@code @SQL @INSERT @Trait} interface, containing the target table, its
 * columns, and the single input parameter.
 *
 * @param tableElement the {@code @Table}-annotated type element
 * @param tableName the SQL table name from {@code @Table(name = ...)}
 * @param columns the table's insertable columns (excluding {@code @IncomingForeignKey})
 * @param inputParamName the name of the single input in {@code _Inputs}
 * @param isList {@code true} if the input type is {@code List<@Table>}, {@code false} for a single
 *     {@code @Table}
 */
public record InsertQueryModel(
    TypeElement tableElement,
    String tableName,
    List<InsertColumn> columns,
    String inputParamName,
    boolean isList) {

  /**
   * A column in the target table.
   *
   * @param columnName the DB column name (from {@code @Column} or the method name)
   * @param javaType the Java type of the column (unwrapped if Optional)
   * @param accessorMethodName the method name on the Table model to call
   * @param isOptional {@code true} if the column's return type on the Table model is {@code
   *     Optional<T>}
   * @param serdeInfo if the column's type has {@code @SupportedModelProtocol}, the serialization
   *     info; otherwise {@code null}
   */
  public record InsertColumn(
      String columnName,
      TypeMirror javaType,
      String accessorMethodName,
      boolean isOptional,
      @Nullable SerdeColumnInfo serdeInfo) {}
}
