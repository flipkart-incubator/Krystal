package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import static com.flipkart.krystal.vajram.ext.sql.codegen.InsertQueryBuilder.loadProtocolConfig;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlCodegenUtil;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.ColumnModel;
import com.flipkart.krystal.vajram.ext.sql.codegen.syntax.SqlSyntax;
import com.flipkart.krystal.vajram.ext.sql.data.DataUtils;
import com.flipkart.krystal.vajram.ext.sql.lang.SqlDialect;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.lang.model.element.QualifiedNameable;

record VertxSqlUtil(SqlCodegenUtil sqlUtil, SqlSyntax syntax, SqlDialect dialect) {

  static final String SQL_VAJRAM_SUFFIX = "_VertxSql";
  static final String VERTX_SQL_POOL_FACET = "vertxSql_pool";
  static final String SQL_RESULT_FACET = "sqlResult";
  static final ParameterizedTypeName ROW_SET_OF_ROW =
      ParameterizedTypeName.get(RowSet.class, Row.class);
  static final ParameterizedTypeName ERRABLE_OF_ROW_SET_OF_ROW =
      ParameterizedTypeName.get(
          ClassName.get(Errable.class), ParameterizedTypeName.get(RowSet.class, Row.class));

  static CodeBlock varArgsToList(List<CodeBlock> args) {
    return CodeBlock.of(
        "$T.asList($L)",
        Arrays.class,
        args.stream()
            .collect(CodeBlock.joining(", "))) // Arrays supports null values, which is needed
    ;
  }

  public CodeBlock readColumnAndSetValue(ColumnModel col, String alias) {
    CodeBlock columnExpression = vertxColumnGetter(col, alias);
    if (col.serdeInfo() != null) {
      columnExpression =
          loadProtocolConfig(requireNonNull(col.serdeInfo()).protocolTypeElement())
              .createDeserializationExpression(columnExpression, col.javaType(), sqlUtil.util());
    }
    return CodeBlock.of("\n    .$L($L)", col.methodName(), columnExpression);
  }

  private static final Set<String> CUSTOM_GETTERS =
      Set.of(
          Boolean.class.getCanonicalName(),
          Byte.class.getCanonicalName(),
          Short.class.getCanonicalName(),
          Integer.class.getCanonicalName(),
          Long.class.getCanonicalName(),
          Double.class.getCanonicalName(),
          Float.class.getCanonicalName(),
          String.class.getCanonicalName(),
          UUID.class.getCanonicalName(),
          LocalDate.class.getCanonicalName(),
          LocalDateTime.class.getCanonicalName(),
          OffsetDateTime.class.getCanonicalName());

  private CodeBlock vertxColumnGetter(ColumnModel column, String alias) {
    if (sqlUtil.isFloatArray(column.javaType()) && column.serdeInfo() == null) {
      requirePostgresFloatArray(column.methodName());
      return CodeBlock.of(
          "$T.sqlValToFloatArray(_row.getArrayOfFloats($S))",
          DataUtils.class,
          syntax.columnNameInResult(alias));
    }
    String computedGetterName =
        column.serdeInfo() != null
            ?
            // If serdeInfo is present, read the raw value which can be used for deserialized
            "getValue"
            : switch (column.javaType().getKind()) {
              case BOOLEAN -> "getBoolean";
              case BYTE -> "getByte";
              case SHORT -> "getShort";
              case INT -> "getInteger";
              case LONG -> "getLong";
              case DOUBLE -> "getDouble";
              case FLOAT -> "getFloat";
              default -> {
                String getterName = "getValue";
                if (sqlUtil.util().processingEnv().getTypeUtils().asElement(column.javaType())
                    instanceof QualifiedNameable qualifiedNameable) {
                  if (CUSTOM_GETTERS.contains(qualifiedNameable.getQualifiedName().toString())) {
                    getterName = "get" + qualifiedNameable.getSimpleName();
                  }
                }
                yield getterName;
              }
            };
    return CodeBlock.of("$L.$L($S)", "_row", computedGetterName, syntax.columnNameInResult(alias));
  }

  private void requirePostgresFloatArray(String columnName) {
    if (dialect != SqlDialect.POSTGRESQL_18) {
      throw sqlUtil
          .util()
          .errorAndThrow(
              "[vajram-sql] FloatArray column '"
                  + columnName
                  + "' requires PostgreSQL REAL[] support. Dialect "
                  + dialect
                  + " has no native FloatArray mapping.");
    }
  }
}
