package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import static com.flipkart.krystal.vajram.ext.sql.vertx.codegen.VertxSqlUtil.SQL_RESULT_FACET;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.Constants;
import com.flipkart.krystal.vajram.ext.sql.vertx.VertxSqlInsertResultUtil;
import com.flipkart.krystal.vajram.ext.sql.vertx.codegen.InsertResultType.ReturningColumn;
import com.flipkart.krystal.vajram.facets.Output;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class MySqlCodeGenerator implements DialectCodeGenerator {

  private final CodeGenUtility util;

  MySqlCodeGenerator(VertxSqlUtil vertxSqlUtil) {
    this.util = vertxSqlUtil.util();
  }

  @Override
  public MethodSpec mapResultsForInsertReturn(InsertResultType resultType) {
    List<ReturningColumn> returningColumns = resultType.returningColumns();
    if (returningColumns.isEmpty()) {
      throw new IllegalArgumentException("At least one returning column expected");
    }
    String resultPkg =
        util.processingEnv()
            .getElementUtils()
            .getPackageOf(resultType.selectionElement())
            .getQualifiedName()
            .toString();
    String resultName = resultType.selectionElement().getSimpleName().toString();
    ClassName resultClass = ClassName.get(resultPkg, resultName);
    ClassName resultImmutPojo = ClassName.get(resultPkg, resultName + Constants.IMMUT_POJO_SUFFIX);
    if (returningColumns.size() > 1) {
      throw new UnsupportedOperationException(
          "Mysql doesn't support returning more than one column from an INSERT");
    }
    ReturningColumn returningColumn = returningColumns.get(0);
    if (!returningColumn.isAutoAssignId()) {
      throw new IllegalArgumentException(
          "Vertx MySql only supports returning one auto-assigned column on insert but "
              + returningColumn.methodName()
              + " does not have @DefaultValueStrategy(AUTO_ASSIGN_ID) on the corresponding table column");
    }
    if (resultType.isListResult()) {
      return buildMySqlListReturningMapResult(resultClass, resultImmutPojo, returningColumn);
    } else {
      return buildMySqlSingleReturningMapResult(resultClass, resultImmutPojo, returningColumn);
    }
  }

  /**
   * MySQL batch INSERT: extracts sequential auto-assigned IDs via {@link
   * VertxSqlInsertResultUtil#extractMySqlAutoIds}.
   */
  private MethodSpec buildMySqlListReturningMapResult(
      ClassName resultClass, ClassName resultImmutPojo, ReturningColumn autoIdCol) {
    TypeName outputReturnTypeName =
        ParameterizedTypeName.get(ClassName.get(List.class), resultClass);

    MethodSpec.Builder method =
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
    method.addStatement(
        "$T<$T> _autoIds = $T.extractMySqlAutoIds($L, $L.rowCount())",
        List.class,
        Long.class,
        VertxSqlInsertResultUtil.class,
        SQL_RESULT_FACET,
        SQL_RESULT_FACET);
    method.addStatement("$T<$T> _result = new $T<>()", List.class, resultClass, ArrayList.class);
    method.beginControlFlow("for (long _autoId : _autoIds)");

    CodeBlock.Builder chain = CodeBlock.builder().add("_result.add($T._builder()", resultImmutPojo);
    chain.add("\n    .$L($L)", autoIdCol.methodName(), mySqlAutoIdCast(autoIdCol));
    chain.add("\n    ._build())");
    method.addStatement(chain.build());

    method.endControlFlow();
    method.addStatement("return _result");
    return method.build();
  }

  /**
   * MySQL single-row INSERT: extracts auto-assigned ID via {@link
   * VertxSqlInsertResultUtil#extractMySqlAutoId}.
   */
  private MethodSpec buildMySqlSingleReturningMapResult(
      ClassName resultClass, ClassName resultImmutPojo, ReturningColumn autoIdCol) {
    MethodSpec.Builder method =
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
    method.addStatement(
        "long _autoId = $T.extractMySqlAutoId($L)",
        VertxSqlInsertResultUtil.class,
        SQL_RESULT_FACET);

    CodeBlock.Builder chain = CodeBlock.builder().add("return $T._builder()", resultImmutPojo);
    chain.add("\n    .$L($L)", autoIdCol.methodName(), mySqlAutoIdCast(autoIdCol));
    chain.add("\n    ._build()");
    method.addStatement(chain.build());

    return method.build();
  }

  /**
   * Returns a cast expression if the auto-ID column's type is narrower than {@code long} (e.g.
   * {@code int}). Otherwise, returns the variable name directly.
   */
  private String mySqlAutoIdCast(ReturningColumn col) {
    String varName = "_autoId";
    if (col.javaType().getKind() == TypeKind.INT) {
      return "(int) " + varName;
    }
    if (col.javaType().getKind() == TypeKind.SHORT) {
      return "(short) " + varName;
    }
    return varName;
  }
}
