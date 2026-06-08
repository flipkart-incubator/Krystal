package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.SerdeColumnInfo;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
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
    List<TableColumn> columns,
    String inputParamName,
    boolean isList) {

  @Override
  public List<TableColumn> columns() {
    return columns.stream().filter(TableColumn::isEligibleForInsertion).toList();
  }

  /**
   * A column in the target table.
   *
   * @param columnName the DB column name (from {@code @Column} or the method name)
   * @param javaType the Java type of the column (unwrapped if Optional)
   * @param isOptional {@code true} if the column's return type on the Table model is {@code
   *     Optional<T>}
   * @param serdeInfo if the column's type has {@code @SupportedModelProtocol}, the serialization
   *     info; otherwise {@code null}
   * @param isEligibleForInsertion {@code true} if the column is eligible for insertion from
   *     application (Ex: columns whose values are auto-assigned, like auto-incremented IDs and
   *     current timestamp, are not)
   */
  public record TableColumn(
      ExecutableElement declaringMethod,
      String columnName,
      TypeMirror javaType,
      boolean isOptional,
      @Nullable SerdeColumnInfo serdeInfo,
      boolean isEligibleForInsertion) {

    public String methodName() {
      return declaringMethod.getSimpleName().toString();
    }
  }
}
