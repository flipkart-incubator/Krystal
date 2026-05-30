package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider.ModelProtocolConfig;
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
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

@UtilityClass
class VertxSqlUtil {
  static final String SQL_VAJRAM_SUFFIX = "_VertxSql";
  static final String VERTX_SQL_POOL_FACET = "vertxSql_pool";
  static final String SQL_RESULT_FACET = "sqlResult";
  static final ParameterizedTypeName ROW_SET_OF_ROW =
      ParameterizedTypeName.get(RowSet.class, Row.class);

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
}
