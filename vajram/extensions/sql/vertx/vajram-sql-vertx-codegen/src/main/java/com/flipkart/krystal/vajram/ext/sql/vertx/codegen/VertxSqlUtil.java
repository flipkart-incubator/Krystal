package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider.ModelProtocolConfig;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.vajram.ext.sql.codegen.syntax.SqlSyntax;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

final class VertxSqlUtil {
  static final String SQL_VAJRAM_SUFFIX = "_VertxSql";
  static final String VERTX_SQL_POOL_FACET = "vertxSql_pool";
  static final String SQL_RESULT_FACET = "sqlResult";
  static final ParameterizedTypeName ROW_SET_OF_ROW =
      ParameterizedTypeName.get(RowSet.class, Row.class);
  static final ParameterizedTypeName ERRABLE_OF_ROW_SET_OF_ROW =
      ParameterizedTypeName.get(
          ClassName.get(Errable.class), ParameterizedTypeName.get(RowSet.class, Row.class));
  @Getter private final CodeGenUtility util;
  @Getter private final SqlSyntax syntax;

  public VertxSqlUtil(CodeGenUtility util, SqlSyntax syntax) {
    this.util = util;
    this.syntax = syntax;
  }

  static CodeBlock varArgsToList(List<CodeBlock> args) {
    return CodeBlock.of(
        "$T.asList($L)",
        Arrays.class,
        args.stream()
            .collect(CodeBlock.joining(", "))) // Arrays supports null values, which is needed
    ;
  }

  private static @MonotonicNonNull Map<String, ModelProtocolConfig> protocolConfigCache;

  static ModelProtocolConfig loadProtocolConfig(TypeElement protocolTypeElement) {
    if (protocolConfigCache == null) {
      protocolConfigCache =
          ServiceLoader.load(ModelProtocolConfigProvider.class, VertxSqlUtil.class.getClassLoader())
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

  public String vertxColumnGetter(String rowVar, String columnName, TypeMirror type) {
    String q = "\"" + syntax.columnNameInResult(columnName) + "\"";
    if (type.getKind() == TypeKind.LONG) {
      return rowVar + ".getLong(" + q + ")";
    }
    if (type.getKind() == TypeKind.INT) {
      return rowVar + ".getInteger(" + q + ")";
    }
    if (type.getKind() == TypeKind.BOOLEAN) {
      return rowVar + ".getBoolean(" + q + ")";
    }
    if (type.getKind() == TypeKind.DOUBLE) {
      return rowVar + ".getDouble(" + q + ")";
    }
    if (type.getKind() == TypeKind.FLOAT) {
      return rowVar + ".getFloat(" + q + ")";
    }
    if (type.getKind() == TypeKind.SHORT) {
      return rowVar + ".getShort(" + q + ")";
    }
    return switch (type.toString()) {
      case "java.lang.String" -> rowVar + ".getString(" + q + ")";
      case "java.lang.Long" -> rowVar + ".getLong(" + q + ")";
      case "java.lang.Integer" -> rowVar + ".getInteger(" + q + ")";
      case "java.lang.Boolean" -> rowVar + ".getBoolean(" + q + ")";
      case "java.lang.Double" -> rowVar + ".getDouble(" + q + ")";
      case "java.lang.Float" -> rowVar + ".getFloat(" + q + ")";
      case "java.lang.Short" -> rowVar + ".getShort(" + q + ")";
      case "java.time.LocalDate" -> rowVar + ".getLocalDate(" + q + ")";
      case "java.time.LocalDateTime" -> rowVar + ".getLocalDateTime(" + q + ")";
      case "java.time.OffsetDateTime" -> rowVar + ".getOffsetDateTime(" + q + ")";
      case "java.util.UUID" -> rowVar + ".getUUID(" + q + ")";
      default -> rowVar + ".getValue(" + q + ")";
    };
  }
}
