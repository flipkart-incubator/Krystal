package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider.ModelProtocolConfig;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.ext.sql.codegen.InsertQueryModel.TableColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.SerdeColumnInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.syntax.SqlSyntax;
import com.flipkart.krystal.vajram.ext.sql.data.DataUtils;
import com.flipkart.krystal.vajram.ext.sql.lang.SqlDialect;
import com.flipkart.krystal.vajram.ext.sql.model.DefaultValue;
import com.squareup.javapoet.CodeBlock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Builds SQL INSERT statements from parsed {@link InsertQueryModel} records.
 *
 * <p>The generated INSERT uses positional placeholders ({@code $1}, {@code $2}, …) for parameter
 * binding, matching the convention used by the Vert.x SQL client.
 */
public final class InsertQueryBuilder {

  private final CodeGenUtility util;
  private final SqlDialect sqlDialect;
  private final SqlCodegenUtil sqlCodegenUtil;

  public InsertQueryBuilder(VajramCodeGenUtility vajramUtil, SqlDialect sqlDialect) {
    this.util = vajramUtil.codegenUtil();
    this.sqlCodegenUtil = new SqlCodegenUtil(util, sqlDialect);
    this.sqlDialect = sqlDialect;
  }

  /**
   * Builds a parameterized INSERT statement, optionally appending a {@code RETURNING} clause.
   *
   * @param model the parsed INSERT model containing table name and column definitions
   * @param config driver-specific config for placeholder format
   * @param syntax the SQL syntax provider for the target database dialect; if non-null and {@code
   *     returningColumnNames} is non-empty, a RETURNING clause is appended
   * @param returningColumnNames the column names to include in the RETURNING clause; may be empty
   * @return the SQL INSERT string with positional placeholders and optional RETURNING clause
   */
  public String buildInsertSql(
      InsertQueryModel model,
      SqlDriverConfig config,
      SqlSyntax syntax,
      List<String> returningColumnNames) {
    List<TableColumn> columns = model.columns();

    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO ").append(model.tableName()).append(" (");

    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(columns.get(i).columnName());
    }

    sb.append(") VALUES (");

    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(config.getParamPlaceholder(i + 1));
    }

    sb.append(")");

    if (syntax != null && !returningColumnNames.isEmpty()) {
      try {
        sb.append(syntax.returningClause(returningColumnNames));
      } catch (Exception e) {
        util.error(e, model.tableElement());
      }
    }

    return sb.toString();
  }

  /** Generates the code to access a column value from a table model variable. */
  public CodeBlock columnAccessor(String varName, TableColumn col) {
    CodeBlock codeBlock =
        CodeBlock.of(
            "$L.$L()$L", varName, col.methodName(), col.isOptional() ? ".orElse(null)" : "");
    SerdeColumnInfo serde = col.serdeInfo();
    if (serde != null) {
      // Serde — e.g. Json.JSON.serialize(user.address(), config)
      // Serde is supposed to handle null values
      codeBlock = serializeExpression(codeBlock, serde);
    }

    DefaultValue defaultValue = col.declaringMethod().getAnnotation(DefaultValue.class);
    if (defaultValue != null) {
      codeBlock =
          CodeBlock.of(
              "$T.requireNonNullElse($L, $L)",
              Objects.class,
              codeBlock,
              convertDefaultValue(defaultValue, col));
    }

    return sqlParameterValues(codeBlock, col.javaType(), col.serdeInfo(), col.methodName());
  }

  public CodeBlock sqlParameterValues(
      CodeBlock valueExpression,
      TypeMirror type,
      @Nullable SerdeColumnInfo serdeInfo,
      String columnName) {
    if (serdeInfo == null) {
      if (sqlCodegenUtil.isFloatArray(type)) {
        sqlCodegenUtil.requirePostgresFloatArray(columnName);
        return CodeBlock.of("$T.floatArrayToSqlVal($L)", DataUtils.class, valueExpression);
      }
    }
    return valueExpression;
  }

  /**
   * Generates a {@code facetVar.serialize(value, configOrNull)} expression wrapped in {@code
   * SqlSerdeUtil.toSqlValue(...)}.
   *
   * <p>If the protocol's config annotation type is not {@code NoAnnotation}, the config is read
   * from the column's type via {@code Type.class.getAnnotation(ConfigAnno.class)}. Otherwise,
   * {@code null} is passed.
   */
  private CodeBlock serializeExpression(CodeBlock valueExpr, SerdeColumnInfo serde) {
    return loadProtocolConfig(serde.protocolTypeElement())
        .createSerializationExpression(CodeBlock.of("$L", valueExpr), serde.columnType(), util);
  }

  private CodeBlock convertDefaultValue(DefaultValue defaultValue, TableColumn column) {
    TypeMirror typeMirror = column.javaType();
    final String defaultValueString = defaultValue.value();
    try {
      return CodeBlock.of(
          "$L",
          switch (typeMirror.getKind()) {
            case BOOLEAN -> Boolean.valueOf(defaultValueString);
            case BYTE -> Byte.valueOf(defaultValueString);
            case SHORT -> Short.valueOf(defaultValueString);
            case INT -> Integer.valueOf(defaultValueString);
            case LONG -> Long.valueOf(defaultValueString);
            case CHAR -> {
              if (defaultValueString.length() != 1) {
                throw new IllegalArgumentException(
                    "@DefaultValue(value) must be exactly one character long for type "
                        + typeMirror);
              }
              yield defaultValueString.charAt(0);
            }
            case FLOAT -> Float.valueOf(defaultValueString);
            case DOUBLE -> Double.valueOf(defaultValueString);
            default -> CodeBlock.of("$S", defaultValueString);
          });
    } catch (Exception e) {
      util.error(e, column.declaringMethod());
      throw e;
    }
  }

  private static @MonotonicNonNull Map<String, ModelProtocolConfig> protocolConfigCache;

  public static ModelProtocolConfig loadProtocolConfig(TypeElement protocolTypeElement) {
    if (protocolConfigCache == null) {
      protocolConfigCache =
          ServiceLoader.load(
                  ModelProtocolConfigProvider.class, InsertQueryBuilder.class.getClassLoader())
              .stream()
              .map(Provider::get)
              .map(ModelProtocolConfigProvider::getConfig)
              .collect(
                  Collectors.toMap(
                      c -> c.modelProtocol().getClass().getCanonicalName(), Function.identity()));
    }
    String key = protocolTypeElement.getQualifiedName().toString();
    ModelProtocolConfig config = protocolConfigCache.get(key);
    if (config == null) {
      throw new IllegalStateException(
          "[vajram-sql] No ModelProtocolConfigProvider found for protocol: " + key);
    }
    return config;
  }
}
