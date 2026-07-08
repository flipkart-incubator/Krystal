package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import static com.flipkart.krystal.vajram.ext.sql.vertx.codegen.VertxSqlUtil.SQL_RESULT_FACET;
import static com.flipkart.krystal.vajram.ext.sql.vertx.codegen.VertxSqlUtil.loadProtocolConfig;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.codegen.common.models.Constants;
import com.flipkart.krystal.vajram.ext.sql.vertx.codegen.InsertResultType.ReturningColumn;
import com.flipkart.krystal.vajram.facets.Output;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import io.vertx.sqlclient.Row;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class SqlLiteCodeGenerator implements DialectCodeGenerator {
  private final VertxSqlUtil vertxSqlUtil;

  SqlLiteCodeGenerator(VertxSqlUtil vertxSqlUtil) {
    this.vertxSqlUtil = vertxSqlUtil;
  }

  @Override
  public MethodSpec mapResultsForInsertReturn(InsertResultType resultType) {
    String resultPkg =
        vertxSqlUtil
            .util()
            .processingEnv()
            .getElementUtils()
            .getPackageOf(resultType.selectionElement())
            .getQualifiedName()
            .toString();
    String resultName = resultType.selectionElement().getSimpleName().toString();
    ClassName resultClass = ClassName.get(resultPkg, resultName);
    ClassName resultImmutPojo = ClassName.get(resultPkg, resultName + Constants.IMMUT_POJO_SUFFIX);
    if (resultType.isListResult()) {
      return buildListReturningMapResult(
          resultType.returningColumns(), resultClass, resultImmutPojo);
    } else {
      return buildSingleReturningMapResult(
          resultType.returningColumns(), resultClass, resultImmutPojo);
    }
  }

  /** Maps the first row of the RETURNING result to a single Selection object. */
  private MethodSpec buildSingleReturningMapResult(
      List<ReturningColumn> returningColumns, ClassName resultClass, ClassName resultImmutPojo) {
    Builder method =
        MethodSpec.methodBuilder("mapResult")
            .addModifiers(STATIC)
            .addAnnotation(Output.class)
            .addAnnotation(Nullable.class)
            .returns(resultClass)
            .addParameter(
                ParameterSpec.builder(VertxSqlUtil.ROW_SET_OF_ROW, SQL_RESULT_FACET)
                    .addAnnotation(Nullable.class)
                    .build());

    method.addCode(
        """
            if($L == null){
              return null;
            }
        """,
        SQL_RESULT_FACET);
    method.addStatement("$T<$T> _it = $L.iterator()", Iterator.class, Row.class, SQL_RESULT_FACET);
    method.beginControlFlow("if (!_it.hasNext())");
    method.addStatement("return null");
    method.endControlFlow();
    method.addStatement("$T _row = _it.next()", Row.class);

    CodeBlock.Builder chain = CodeBlock.builder().add("return $T._builder()", resultImmutPojo);
    for (ReturningColumn col : returningColumns) {
      chain.add("\n    .$L($L)", col.methodName(), returningColumnExpression("_row", col));
    }
    chain.add("\n    ._build()");
    method.addStatement(chain.build());

    return method.build();
  }

  /** Maps all rows of the RETURNING result to a {@code List<Selection>}. */
  private MethodSpec buildListReturningMapResult(
      List<ReturningColumn> returningColumns, ClassName resultClass, ClassName resultImmutPojo) {
    TypeName outputReturnTypeName =
        ParameterizedTypeName.get(ClassName.get(List.class), resultClass);

    Builder method =
        MethodSpec.methodBuilder("mapResult")
            .addModifiers(STATIC)
            .addAnnotation(Output.class)
            .returns(outputReturnTypeName)
            .addParameter(
                ParameterSpec.builder(VertxSqlUtil.ROW_SET_OF_ROW, SQL_RESULT_FACET)
                    .addAnnotation(Nullable.class)
                    .build());

    method.addCode(
        """
            if($L == null){
              return $T.of();
            }
        """,
        SQL_RESULT_FACET,
        List.class);
    method.addStatement("$T<$T> _result = new $T<>()", List.class, resultClass, ArrayList.class);
    method.beginControlFlow("for ($T _row : $L)", Row.class, SQL_RESULT_FACET);

    CodeBlock.Builder chain = CodeBlock.builder().add("_result.add($T._builder()", resultImmutPojo);
    for (ReturningColumn col : returningColumns) {
      chain.add("\n    .$L($L)", col.methodName(), returningColumnExpression("_row", col));
    }
    chain.add("\n    ._build())");
    method.addStatement(chain.build());

    method.endControlFlow();
    method.addStatement("return _result");
    return method.build();
  }

  /**
   * Returns a {@link CodeBlock} for extracting a column value from a RETURNING row. For columns
   * with serde info, generates a deserialization call; otherwise delegates to {@link
   * VertxSqlUtil#vertxColumnGetter}.
   */
  public CodeBlock returningColumnExpression(String rowVar, ReturningColumn col) {
    if (col.serdeInfo() != null) {
      return loadProtocolConfig(requireNonNull(col.serdeInfo()).protocolTypeElement())
          .createDeserializationExpression(
              CodeBlock.of(
                  "$L.getValue($S)",
                  rowVar,
                  vertxSqlUtil.syntax().columnNameInResult(col.columnName())),
              col.javaType(),
              vertxSqlUtil.util());
    }
    if (col.isOptional()) {
      // Wrap nullable DB value in Optional
      return CodeBlock.of(
          "$T.ofNullable($L)",
          Optional.class,
          vertxSqlUtil.vertxColumnGetter(rowVar, col.columnName(), col.javaType()));
    }
    return CodeBlock.of(
        "$L", vertxSqlUtil.vertxColumnGetter(rowVar, col.columnName(), col.javaType()));
  }
}
