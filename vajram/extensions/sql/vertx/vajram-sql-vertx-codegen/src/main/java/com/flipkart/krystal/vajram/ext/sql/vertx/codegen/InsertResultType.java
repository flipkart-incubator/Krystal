package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.SerdeColumnInfo;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Captures the INSERT trait's response type and the columns to be returned from the database.
 *
 * @param selectionElement the {@code @ReturnOnInsert}-annotated model interface
 * @param isListResult {@code true} if the response type is {@code List<ReturnOnInsert>}
 * @param returningColumns columns declared in the {@code @ReturnOnInsert} interface
 */
public record InsertResultType(
    TypeElement selectionElement, boolean isListResult, List<ReturningColumn> returningColumns) {

  /**
   * A column declared in a {@code @ReturnOnInsert} interface.
   *
   * @param columnName the DB column name (from {@code @Column} or the method name)
   * @param methodName the Java method name on the {@code @ReturnOnInsert} interface
   * @param javaType the Java return type (unwrapped if Optional)
   * @param isOptional {@code true} if the return type is {@code Optional<T>}
   * @param serdeInfo serde info from the corresponding table column; {@code null} if none
   * @param isAutoAssignId {@code true} if the corresponding table column has
   *     {@code @DefaultValueStrategy(AUTO_ASSIGN_ID)}
   */
  public record ReturningColumn(
      String columnName,
      String methodName,
      TypeMirror javaType,
      boolean isOptional,
      @Nullable SerdeColumnInfo serdeInfo,
      boolean isAutoAssignId) {}
}
