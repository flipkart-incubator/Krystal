package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.ext.sql.vertx.codegen.VertxSqlUtil.SQL_RESULT_FACET;
import static com.flipkart.krystal.vajram.ext.sql.vertx.codegen.VertxSqlUtil.loadProtocolConfig;
import static com.flipkart.krystal.vajram.ext.sql.vertx.codegen.VertxSqlUtil.varArgsToList;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.ContainerType;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.data.Failure;
import com.flipkart.krystal.except.SkippedExecutionException;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.InsertModelParser;
import com.flipkart.krystal.vajram.ext.sql.codegen.InsertQueryBuilder;
import com.flipkart.krystal.vajram.ext.sql.codegen.InsertQueryModel;
import com.flipkart.krystal.vajram.ext.sql.codegen.InsertQueryModel.TableColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlDriverConfig;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlModelParser;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.SerdeColumnInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.syntax.SqlSyntax;
import com.flipkart.krystal.vajram.ext.sql.lang.ReturnOnInsert;
import com.flipkart.krystal.vajram.ext.sql.lang.SqlDialect;
import com.flipkart.krystal.vajram.ext.sql.model.DefaultValue;
import com.flipkart.krystal.vajram.ext.sql.model.DefaultValueStrategy;
import com.flipkart.krystal.vajram.ext.sql.model.DefaultValueStrategy.ValueComputation;
import com.flipkart.krystal.vajram.ext.sql.model.IncomingForeignKey;
import com.flipkart.krystal.vajram.ext.sql.vertx.ExecuteVertxSql;
import com.flipkart.krystal.vajram.ext.sql.vertx.codegen.InsertResultType.ReturningColumn;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.nullness.qual.Nullable;

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
  private final SqlModelParser sqlParser;
  private final VajramCodeGenUtility vajramUtil;
  private final VajramInfo executeSqlVajram;
  private final SqlSyntax syntax;
  private final InsertQueryBuilder insertQueryBuilder;
  private final DialectCodeGenerator dialectGen;

  public SqlInsertVajramGen(
      VajramCodeGenUtility vajramUtil,
      VajramInfo vajramInfo,
      InsertModelParser parser,
      SqlModelParser sqlParser,
      SqlDialect sqlDialect) {
    this.util = vajramUtil.codegenUtil();
    this.vajramInfo = vajramInfo;
    this.parser = parser;
    this.sqlParser = sqlParser;
    this.vajramUtil = vajramUtil;
    this.executeSqlVajram = vajramUtil.computeVajramInfo(ExecuteVertxSql.class);
    this.syntax = SqlSyntax.forDialect(sqlDialect);
    this.insertQueryBuilder = new InsertQueryBuilder(vajramUtil);
    this.dialectGen = DialectCodeGenerator.forDialect(sqlDialect, new VertxSqlUtil(util, syntax));
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
      throw util.errorAndThrow(
          "No Inputs found in %s. Insert trait must have one input representing the row to insert",
          sqlTraitElement);
    }

    // Parse the trait's response type to determine if RETURNING clause is needed.
    InsertResultType resultType = parseInsertResultType(vajramInfo, insertModel.tableElement());
    boolean hasReturning = resultType != null;

    ClassName traitClass = ClassName.get(pkg, traitName);
    ClassName facClass = ClassName.get(pkg, vajramName + "_Fac");

    // Determine the vajram's response type.
    TypeName vajramResponseTypeName;
    if (hasReturning) {
      String resultPkg =
          util.processingEnv()
              .getElementUtils()
              .getPackageOf(resultType.selectionElement())
              .getQualifiedName()
              .toString();
      String resultName = resultType.selectionElement().getSimpleName().toString();
      ClassName selectionClass = ClassName.get(resultPkg, resultName);
      vajramResponseTypeName =
          resultType.isListResult()
              ? ParameterizedTypeName.get(ClassName.get(List.class), selectionClass)
              : selectionClass;
    } else {
      vajramResponseTypeName = TypeName.INT.box();
    }

    TypeSpec vajramSpec =
        util.classBuilder(vajramName, traitClass.canonicalName())
            .addModifiers(PUBLIC, ABSTRACT)
            .addAnnotation(Vajram.class)
            .superclass(
                ParameterizedTypeName.get(
                    ClassName.get(ComputeVajramDef.class), vajramResponseTypeName))
            .addSuperinterface(traitClass)
            .addType(buildInputsInterface(inputMethod))
            .addType(buildInternalFacetsInterface())
            .addMethod(
                buildResolveSqlMethod(
                    facClass,
                    insertModel,
                    inputMethod,
                    hasReturning && syntax.supportsReturning() ? syntax : null,
                    hasReturning ? resultType.returningColumns() : List.of()))
            .addMethod(buildResolveParamsMethod(facClass, insertModel, inputMethod))
            .addMethod(buildResolvePoolMethod(facClass))
            .addMethod(
                hasReturning
                    ? dialectGen.mapResultsForInsertReturn(resultType)
                    : buildOutputMethod())
            .build();
    ClassName vajramClassName = ClassName.get(pkg, vajramName);

    JavaFile javaFile = JavaFile.builder(pkg, vajramSpec).build();
    util.generateSourceFile(vajramClassName.canonicalName(), javaFile, sqlTraitElement);
    util.note("Generated " + pkg + "." + vajramName);
  }

  /**
   * Parses the INSERT trait's response type. Returns {@code null} if the response is {@code
   * Integer} (row-count mode). Otherwise, validates the response type is annotated with
   * {@code @ReturnOnInsert} and parses its declared columns.
   */
  private @Nullable InsertResultType parseInsertResultType(
      VajramInfo vajramInfo, TypeElement tableElement) {
    TypeMirror rawResponseType = vajramInfo.lite().responseType().typeMirror(util.processingEnv());

    // Primitive int → row-count mode, no RETURNING
    if (rawResponseType.getKind() == TypeKind.INT) {
      return null;
    }

    TypeMirror responseType = rawResponseType;

    TypeElement responseTypeElem =
        requireNonNull((TypeElement) util.processingEnv().getTypeUtils().asElement(responseType));

    // Boxed Integer → row-count mode, no RETURNING
    if (responseTypeElem.getQualifiedName().contentEquals(Integer.class.getCanonicalName())) {
      return null;
    }

    boolean isListResult = false;
    if (ContainerType.LIST.equals(util.getContainerType(responseType))) {
      isListResult = true;
      responseType = util.getContentType(responseType);
      responseTypeElem =
          requireNonNull((TypeElement) util.processingEnv().getTypeUtils().asElement(responseType));
    }

    // Validate the response type has @ReturnOnInsert
    ReturnOnInsert returnOnInsertAnno = responseTypeElem.getAnnotation(ReturnOnInsert.class);
    if (returnOnInsertAnno == null) {
      util.error(
          "[SqlInsertVajramGen] Response type '"
              + responseTypeElem.getQualifiedName()
              + "' must be annotated with @ReturnOnInsert",
          vajramInfo.definitionElement());
      return null;
    }

    // Parse returning columns from the @ReturnOnInsert interface
    List<ReturningColumn> returningColumns = parseReturningColumns(responseTypeElem, tableElement);

    return new InsertResultType(responseTypeElem, isListResult, returningColumns);
  }

  /**
   * Parses the methods of a {@code @ReturnOnInsert} interface to build a list of returning columns.
   * For each method, resolves the DB column name, Java type, serde info, and whether the
   * corresponding table column has {@code @DefaultValueStrategy(AUTO_ASSIGN_ID)}.
   */
  private List<ReturningColumn> parseReturningColumns(
      TypeElement returnOnInsertElement, TypeElement tableElement) {
    List<ReturningColumn> returningColumns = new ArrayList<>();
    List<? extends ExecutableElement> methods = util.getModelFields(returnOnInsertElement);

    for (ExecutableElement method : methods) {
      String methodName = method.getSimpleName().toString();
      String dbColumnName = sqlParser.resolveColumnName(method);

      TypeMirror returnType = method.getReturnType();
      boolean isOptional = vajramUtil.codegenUtil().isOptional(returnType);
      TypeMirror actualType =
          isOptional ? vajramUtil.codegenUtil().getOptionalInnerType(returnType) : returnType;

      // Resolve serde info from the corresponding table column
      @Nullable SerdeColumnInfo serdeInfo =
          sqlParser.resolveSerdeInfoFromTable(dbColumnName, tableElement);

      // Check if the corresponding table column has @DefaultValueStrategy(AUTO_ASSIGN_ID)
      boolean isAutoAssignId = isAutoAssignIdInTable(dbColumnName, tableElement);

      returningColumns.add(
          new ReturningColumn(
              dbColumnName, methodName, actualType, isOptional, serdeInfo, isAutoAssignId));
    }

    if (returningColumns.isEmpty()) {
      util.error(
          "[SqlInsertVajramGen] @ReturnOnInsert interface '"
              + returnOnInsertElement.getQualifiedName()
              + "' has no methods. At least one column must be declared.",
          returnOnInsertElement);
    }

    return returningColumns;
  }

  /** Checks if the given column in the table has {@code @DefaultValueStrategy(AUTO_ASSIGN_ID)}. */
  private boolean isAutoAssignIdInTable(String dbColumnName, TypeElement tableElement) {
    for (ExecutableElement tableMethod : util.getModelFields(tableElement)) {
      if (tableMethod.getAnnotation(IncomingForeignKey.class) != null) {
        continue;
      }
      if (sqlParser.resolveColumnName(tableMethod).equals(dbColumnName)) {
        DefaultValueStrategy defaultValueStrategy =
            tableMethod.getAnnotation(DefaultValueStrategy.class);
        return defaultValueStrategy != null
            && defaultValueStrategy.value() == ValueComputation.AUTO_ASSIGN_ID;
      }
    }
    return false;
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
    Builder inputs = TypeSpec.interfaceBuilder("_Inputs").addModifiers(STATIC);
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

  private TypeSpec buildInternalFacetsInterface() {
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
        MethodSpec.methodBuilder(SQL_RESULT_FACET)
            .returns(VertxSqlUtil.ROW_SET_OF_ROW)
            .addModifiers(PUBLIC, ABSTRACT)
            .addAnnotation(
                AnnotationSpec.builder(Dependency.class)
                    .addMember("onVajram", "$T.class", ExecuteVertxSql.class)
                    .build())
            .build();
    Builder builder =
        TypeSpec.interfaceBuilder("_InternalFacets")
            .addModifiers(STATIC)
            .addMethod(poolField)
            .addMethod(sqlResultField);

    return builder.build();
  }

  // ─── @Resolve resolveSql ────────────────────────────────────────────────────

  private MethodSpec buildResolveSqlMethod(
      ClassName facClass,
      InsertQueryModel model,
      ExecutableElement inputMethod,
      @Nullable SqlSyntax syntax,
      List<ReturningColumn> returningColumns) {
    List<String> returningColNames =
        returningColumns.stream().map(ReturningColumn::columnName).toList();
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("resolveSql")
            .addModifiers(STATIC)
            .addAnnotation(
                AnnotationSpec.builder(Resolve.class)
                    .addMember("dep", "$T.$L", facClass, SQL_RESULT_FACET + "_n")
                    .addMember("depInputs", "$T.$L", getExecuteVertxSqlReq(), "sql_n")
                    .build())
            .returns(String.class)
            .addParameter(
                TypeName.get(inputMethod.getReturnType()), inputMethod.getSimpleName().toString());

    if (model.isList()) {
      buildDynamicInsertSql(method, model, syntax, returningColNames);
    } else {
      method.addStatement(
          "return $S",
          insertQueryBuilder.buildInsertSql(model, VERTX_SQL_CONFIG, syntax, returningColNames));
    }

    return method.build();
  }

  private void buildDynamicInsertSql(
      MethodSpec.Builder method,
      InsertQueryModel model,
      @Nullable SqlSyntax syntax,
      List<String> returningColNames) {
    List<TableColumn> columns = model.columns();

    // Build: "INSERT INTO table (col1, col2, ...) VALUES "
    StringBuilder prefix = new StringBuilder("INSERT INTO ");
    prefix.append(model.tableName()).append(" (");
    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) prefix.append(", ");
      prefix.append(columns.get(i).columnName());
    }
    prefix.append(") VALUES ");
    method.addCode(
"""
    if($L.isEmpty()){
      throw new $T($S);
    }
""",
        model.inputParamName(),
        SkippedExecutionException.class,
        "%s: skipping dependency ExecuteVertxSql since '%s' input list is empty"
            .formatted(vajramInfo.definitionElement().getSimpleName(), model.inputParamName()));
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
    if (syntax != null && !returningColNames.isEmpty() && syntax.supportsReturning()) {
      try {
        method.addStatement("_sb.append($S)", syntax.returningClause(returningColNames));
      } catch (Exception e) {
        util.error(e, model.tableElement());
      }
    }
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
                    .addMember("dep", "$T.$L", facClass, SQL_RESULT_FACET + "_n")
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
    for (TableColumn col : model.columns()) {
      args.add(columnAccessor(model.inputParamName(), col));
    }
    method.addStatement("return $T.from($L)", ClassName.get(Tuple.class), varArgsToList(args));
  }

  private void buildDynamicParams(MethodSpec.Builder method, InsertQueryModel model) {
    method.addStatement("$T<$T> _params = new $T<>()", List.class, Object.class, ArrayList.class);
    method.beginControlFlow(
        "for ($T _item : $L)", TypeName.get(model.tableElement().asType()), model.inputParamName());

    for (TableColumn col : model.columns()) {
      method.addStatement("_params.add($L)", columnAccessor("_item", col));
    }

    method.endControlFlow();
    method.addStatement("return $T.from(_params)", ClassName.get(Tuple.class));
  }

  /** Generates the code to access a column value from a table model variable. */
  private CodeBlock columnAccessor(String varName, TableColumn col) {
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

    return codeBlock;
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

  // ─── @Resolve resolvePool ───────────────────────────────────────────────────

  private MethodSpec buildResolvePoolMethod(ClassName facClass) {
    return MethodSpec.methodBuilder("resolvePool")
        .addModifiers(STATIC)
        .addAnnotation(
            AnnotationSpec.builder(Resolve.class)
                .addMember("dep", "$T.$L", facClass, SQL_RESULT_FACET + "_n")
                .addMember("depInputs", "$T.$L", getExecuteVertxSqlReq(), "pool_n")
                .build())
        .addParameter(ClassName.get(Pool.class), VertxSqlUtil.VERTX_SQL_POOL_FACET)
        .returns(ClassName.get(Pool.class))
        .addStatement("return $L", VertxSqlUtil.VERTX_SQL_POOL_FACET)
        .build();
  }

  // ─── @Output mapResult ──────────────────────────────────────────────────────

  private MethodSpec buildOutputMethod() {
    return MethodSpec.methodBuilder("mapResult")
        .addModifiers(STATIC)
        .addAnnotation(Output.class)
        .returns(TypeName.INT)
        .addException(Throwable.class)
        .addParameter(
            ParameterSpec.builder(VertxSqlUtil.ERRABLE_OF_ROW_SET_OF_ROW, SQL_RESULT_FACET).build())
        .addCode(
"""
    if($L instanceof $T<?> failure){
      throw failure.error();
    }
    if($L.value() == null){
      return 0;
    }
    return $L.value().rowCount();
""",
            SQL_RESULT_FACET,
            Failure.class,
            SQL_RESULT_FACET,
            SQL_RESULT_FACET)
        .build();
  }

  // ─── Helpers ────────────────────────────────────────────────────────────────

  private TypeName getExecuteVertxSqlReq() {
    return executeSqlVajram.lite().requestInterfaceTypeName();
  }
}
