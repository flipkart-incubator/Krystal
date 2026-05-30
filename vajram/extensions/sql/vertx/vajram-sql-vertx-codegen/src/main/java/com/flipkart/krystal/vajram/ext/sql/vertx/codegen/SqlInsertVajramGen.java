package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.ext.sql.codegen.InsertQueryBuilder.buildInsertSql;
import static com.flipkart.krystal.vajram.ext.sql.vertx.codegen.VertxSqlUtil.loadProtocolConfig;
import static com.flipkart.krystal.vajram.ext.sql.vertx.codegen.VertxSqlUtil.varArgsToList;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.InsertModelParser;
import com.flipkart.krystal.vajram.ext.sql.codegen.InsertQueryModel;
import com.flipkart.krystal.vajram.ext.sql.codegen.InsertQueryModel.InsertColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlDriverConfig;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.SerdeColumnInfo;
import com.flipkart.krystal.vajram.ext.sql.vertx.ExecuteVertxSql;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

/**
 * Generates a {@code @Vajram} ComputeVajram for each {@code @SQL @INSERT @Trait} interface.
 *
 * <p>The generated vajram:
 *
 * <ul>
 *   <li>{@code resolveSql()} — returns the INSERT SQL string (static for single-row, dynamic for
 *       list inputs)
 *   <li>{@code resolveParams()} — extracts column values from the Table input into a {@link Tuple}
 *   <li>{@code resolvePool()} — passes through the injected connection pool
 *   <li>{@code mapResult()} — returns the number of rows inserted as {@code int}
 * </ul>
 */
public class SqlInsertVajramGen implements CodeGenerator {

  private static final SqlDriverConfig VERTX_SQL_CONFIG = paramIndex -> "$" + paramIndex;

  private final CodeGenUtility util;
  private final VajramInfo vajramInfo;
  private final InsertModelParser parser;
  private final VajramCodeGenUtility vajramUtil;
  private final VajramInfo executeSqlVajram;

  public SqlInsertVajramGen(
      VajramCodeGenUtility vajramUtil, VajramInfo vajramInfo, InsertModelParser parser) {
    this.util = vajramUtil.codegenUtil();
    this.vajramInfo = vajramInfo;
    this.parser = parser;
    this.vajramUtil = vajramUtil;
    this.executeSqlVajram = vajramUtil.computeVajramInfo(ExecuteVertxSql.class);
  }

  // ─── Entry point ─────────────────────────────────────────────────────────────

  public void generate() {
    TypeElement sqlTraitElement = vajramInfo.definitionElement();
    String pkg =
        util.processingEnv()
            .getElementUtils()
            .getPackageOf(sqlTraitElement)
            .getQualifiedName()
            .toString();
    String traitName = sqlTraitElement.getSimpleName().toString();
    String vajramName = traitName + VertxSqlUtil.SQL_VAJRAM_SUFFIX;

    VajramInfo vajramInfo = vajramUtil.computeVajramInfo(sqlTraitElement);
    InsertQueryModel insertModel = parser.parseInsertInputs(vajramInfo);
    if (insertModel == null) {
      return; // errors already reported by the parser
    }

    ExecutableElement inputMethod = findInputMethod(sqlTraitElement);
    if (inputMethod == null) {
      return;
    }

    ClassName traitClass = ClassName.get(pkg, traitName);
    ClassName facClass = ClassName.get(pkg, vajramName + "_Fac");

    TypeSpec vajramSpec =
        util.classBuilder(vajramName, traitClass.canonicalName())
            .addModifiers(PUBLIC, ABSTRACT)
            .addAnnotation(Vajram.class)
            .superclass(
                ParameterizedTypeName.get(
                    ClassName.get(ComputeVajramDef.class), TypeName.INT.box()))
            .addSuperinterface(traitClass)
            .addType(buildInputsInterface(inputMethod))
            .addType(buildInternalFacetsInterface(insertModel))
            .addMethod(buildResolveSqlMethod(facClass, insertModel, inputMethod))
            .addMethod(buildResolveParamsMethod(facClass, insertModel, inputMethod))
            .addMethod(buildResolvePoolMethod(facClass))
            .addMethod(buildMapResultMethod())
            .build();
    ClassName vajramClassName = ClassName.get(pkg, vajramName);

    JavaFile javaFile = JavaFile.builder(pkg, vajramSpec).build();
    util.generateSourceFile(vajramClassName.canonicalName(), javaFile, sqlTraitElement);
    util.note("Generated " + pkg + "." + vajramName);
  }

  private ExecutableElement findInputMethod(TypeElement traitElement) {
    for (TypeElement nested : ElementFilter.typesIn(traitElement.getEnclosedElements())) {
      if (nested.getSimpleName().contentEquals("_Inputs")) {
        List<ExecutableElement> methods = ElementFilter.methodsIn(nested.getEnclosedElements());
        if (methods.size() == 1) {
          return methods.get(0);
        }
      }
    }
    return null;
  }

  // ─── _Inputs / _InternalFacets ────────────────────────────────────────────────

  private TypeSpec buildInputsInterface(ExecutableElement inputMethod) {
    TypeSpec.Builder inputs = TypeSpec.interfaceBuilder("_Inputs").addModifiers(STATIC);
    AnnotationSpec ifAbsentFail =
        AnnotationSpec.builder(IfAbsent.class)
            .addMember("value", "$T.$L", FAIL.getDeclaringClass(), "FAIL")
            .build();
    inputs.addMethod(
        MethodSpec.methodBuilder(inputMethod.getSimpleName().toString())
            .addModifiers(PUBLIC, ABSTRACT)
            .addAnnotation(ifAbsentFail)
            .returns(TypeName.get(inputMethod.getReturnType()))
            .build());
    return inputs.build();
  }

  private TypeSpec buildInternalFacetsInterface(InsertQueryModel insertModel) {
    AnnotationSpec ifAbsentFail =
        AnnotationSpec.builder(IfAbsent.class)
            .addMember("value", "$T.$L", FAIL.getDeclaringClass(), "FAIL")
            .build();
    MethodSpec poolField =
        MethodSpec.methodBuilder(VertxSqlUtil.VERTX_SQL_POOL_FACET)
            .returns(ClassName.get(Pool.class))
            .addModifiers(PUBLIC, ABSTRACT)
            .addAnnotation(ifAbsentFail)
            .addAnnotation(Inject.class)
            .addAnnotation(
                AnnotationSpec.builder(Named.class)
                    .addMember("value", "$S", VertxSqlUtil.VERTX_SQL_POOL_FACET)
                    .build())
            .build();
    MethodSpec sqlResultField =
        MethodSpec.methodBuilder(VertxSqlUtil.SQL_RESULT_FACET)
            .returns(VertxSqlUtil.ROW_SET_OF_ROW)
            .addModifiers(PUBLIC, ABSTRACT)
            .addAnnotation(ifAbsentFail)
            .addAnnotation(
                AnnotationSpec.builder(Dependency.class)
                    .addMember("onVajram", "$T.class", ExecuteVertxSql.class)
                    .build())
            .build();
    TypeSpec.Builder builder =
        TypeSpec.interfaceBuilder("_InternalFacets")
            .addModifiers(STATIC)
            .addMethod(poolField)
            .addMethod(sqlResultField);

    return builder.build();
  }

  // ─── @Resolve resolveSql ────────────────────────────────────────────────────

  private MethodSpec buildResolveSqlMethod(
      ClassName facClass, InsertQueryModel model, ExecutableElement inputMethod) {
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("resolveSql")
            .addModifiers(STATIC)
            .addAnnotation(
                AnnotationSpec.builder(Resolve.class)
                    .addMember("dep", "$T.$L", facClass, VertxSqlUtil.SQL_RESULT_FACET + "_n")
                    .addMember("depInputs", "$T.$L", getExecuteVertxSqlReq(), "sql_n")
                    .build())
            .returns(String.class)
            .addParameter(
                TypeName.get(inputMethod.getReturnType()), inputMethod.getSimpleName().toString());

    if (model.isList()) {
      buildDynamicInsertSql(method, model);
    } else {
      String sql = buildInsertSql(model, VERTX_SQL_CONFIG);
      method.addStatement("return $S", sql);
    }

    return method.build();
  }

  private void buildDynamicInsertSql(MethodSpec.Builder method, InsertQueryModel model) {
    List<InsertColumn> columns = model.columns();

    // Build: "INSERT INTO table (col1, col2, ...) VALUES "
    StringBuilder prefix = new StringBuilder("INSERT INTO ");
    prefix.append(model.tableName()).append(" (");
    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) prefix.append(", ");
      prefix.append(columns.get(i).columnName());
    }
    prefix.append(") VALUES ");

    method.addStatement(
        "$T _sb = new $T($S)", StringBuilder.class, StringBuilder.class, prefix.toString());
    method.addStatement("int _paramIdx = 1");
    method.beginControlFlow("for (int _i = 0; _i < $L.size(); _i++)", model.inputParamName());
    method.beginControlFlow("if (_i > 0)");
    method.addStatement("_sb.append($S)", ", ");
    method.endControlFlow();
    method.addStatement("_sb.append($S)", "(");

    for (int j = 0; j < columns.size(); j++) {
      if (j > 0) {
        method.addStatement("_sb.append($S)", ", ");
      }
      method.addStatement("_sb.append($S).append(_paramIdx++)", "$");
    }
    method.addStatement("_sb.append($S)", ")");
    method.endControlFlow();
    method.addStatement("return _sb.toString()");
  }

  // ─── @Resolve resolveParams ─────────────────────────────────────────────────

  private MethodSpec buildResolveParamsMethod(
      ClassName facClass, InsertQueryModel model, ExecutableElement inputMethod) {
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("resolveParams")
            .addModifiers(STATIC)
            .addAnnotation(
                AnnotationSpec.builder(Resolve.class)
                    .addMember("dep", "$T.$L", facClass, VertxSqlUtil.SQL_RESULT_FACET + "_n")
                    .addMember("depInputs", "$T.$L", getExecuteVertxSqlReq(), "params_n")
                    .build())
            .returns(ClassName.get(Tuple.class))
            .addParameter(
                TypeName.get(inputMethod.getReturnType()), inputMethod.getSimpleName().toString());

    if (model.isList()) {
      buildDynamicParams(method, model);
    } else {
      buildStaticParams(method, model);
    }

    return method.build();
  }

  private void buildStaticParams(MethodSpec.Builder method, InsertQueryModel model) {
    List<CodeBlock> args = new ArrayList<>();
    for (InsertColumn col : model.columns()) {
      args.add(columnAccessor(model.inputParamName(), col));
    }
    method.addStatement("return $T.from($L)", ClassName.get(Tuple.class), varArgsToList(args));
  }

  private void buildDynamicParams(MethodSpec.Builder method, InsertQueryModel model) {
    method.addStatement("$T<$T> _params = new $T<>()", List.class, Object.class, ArrayList.class);
    method.beginControlFlow(
        "for ($T _item : $L)", TypeName.get(model.tableElement().asType()), model.inputParamName());

    for (InsertColumn col : model.columns()) {
      method.addStatement("_params.add($L)", columnAccessor("_item", col));
    }

    method.endControlFlow();
    method.addStatement("return $T.from(_params)", ClassName.get(Tuple.class));
  }

  /** Generates the code to access a column value from a table model variable. */
  private CodeBlock columnAccessor(String varName, InsertColumn col) {
    SerdeColumnInfo serde = col.serdeInfo();
    if (col.isOptional()) {
      if (serde != null) {
        // Optional serde — e.g. user.address().map(a -> Json.JSON.serialize(a,
        // config)).orElse(null)
        return CodeBlock.of(
            "$L.$L().map(_v -> $L).orElse(null)",
            varName,
            col.accessorMethodName(),
            serializeExpression("_v", serde));
      }
      // Optional scalar — e.g. user.phoneNumber().orElse(null)
      return CodeBlock.of("$L.$L().orElse(null)", varName, col.accessorMethodName());
    }
    if (serde != null) {
      // Mandatory serde — e.g. Json.JSON.serialize(user.address(), config)
      return serializeExpression(
          CodeBlock.of("$L.$L()", varName, col.accessorMethodName()).toString(), serde);
    }
    return CodeBlock.of("$L.$L()", varName, col.accessorMethodName());
  }

  /**
   * Generates a {@code facetVar.serialize(value, configOrNull)} expression wrapped in {@code
   * SqlSerdeUtil.toSqlValue(...)}.
   *
   * <p>If the protocol's config annotation type is not {@code NoAnnotation}, the config is read
   * from the column's type via {@code Type.class.getAnnotation(ConfigAnno.class)}. Otherwise,
   * {@code null} is passed.
   */
  private CodeBlock serializeExpression(String valueExpr, SerdeColumnInfo serde) {
    return loadProtocolConfig(serde.protocolTypeElement())
        .createSerializationExpression(CodeBlock.of("$L", valueExpr), serde.columnType(), util);
  }

  // ─── @Resolve resolvePool ───────────────────────────────────────────────────

  private MethodSpec buildResolvePoolMethod(ClassName facClass) {
    return MethodSpec.methodBuilder("resolvePool")
        .addModifiers(STATIC)
        .addAnnotation(
            AnnotationSpec.builder(Resolve.class)
                .addMember("dep", "$T.$L", facClass, VertxSqlUtil.SQL_RESULT_FACET + "_n")
                .addMember("depInputs", "$T.$L", getExecuteVertxSqlReq(), "pool_n")
                .build())
        .addParameter(ClassName.get(Pool.class), VertxSqlUtil.VERTX_SQL_POOL_FACET)
        .returns(ClassName.get(Pool.class))
        .addStatement("return $L", VertxSqlUtil.VERTX_SQL_POOL_FACET)
        .build();
  }

  // ─── @Output mapResult ──────────────────────────────────────────────────────

  private MethodSpec buildMapResultMethod() {
    return MethodSpec.methodBuilder("mapResult")
        .addModifiers(STATIC)
        .addAnnotation(Output.class)
        .returns(TypeName.INT.box())
        .addParameter(VertxSqlUtil.ROW_SET_OF_ROW, VertxSqlUtil.SQL_RESULT_FACET)
        .addStatement("return $L.rowCount()", VertxSqlUtil.SQL_RESULT_FACET)
        .build();
  }

  // ─── Helpers ────────────────────────────────────────────────────────────────

  private TypeName getExecuteVertxSqlReq() {
    return executeSqlVajram.lite().requestInterfaceTypeName();
  }
}
