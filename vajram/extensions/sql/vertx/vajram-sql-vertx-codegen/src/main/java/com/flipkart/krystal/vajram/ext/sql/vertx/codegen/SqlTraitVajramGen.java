package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlModelParser;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryBuilder;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.JoinRelation;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.JoinSqlResult;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.OrderByClause;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.ProjectionInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.ScalarColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.TraitResultType;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereInput;
import com.flipkart.krystal.vajram.ext.sql.statement.SELECT;
import com.flipkart.krystal.vajram.ext.sql.statement.SQL;
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
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

/**
 * Generates a {@code @Vajram} ComputeVajram for each {@code @SQL @SELECT @Trait} interface.
 *
 * <p>SQL model parsing and query building are delegated to the framework-agnostic {@code
 * vajram-sql-codegen} module ({@link SqlModelParser}, {@link SqlQueryBuilder}). This class handles
 * only the Vert.x-specific JavaPoet code generation.
 */
public class SqlTraitVajramGen {

  private static final String SQL_VAJRAM_SUFFIX = "_VertxSql";
  private static final String IMMUT_POJO_SUFFIX = "_ImmutPojo";
  private static final String SQL_RESULT_FACET = "sqlResult";
  static final String VERTX_SQL_POOL_FACET = "vertxSql_pool";

  private static final ClassName INJECT = ClassName.get("jakarta.inject", "Inject");
  private static final ClassName NAMED = ClassName.get("jakarta.inject", "Named");
  private static final ClassName NULLABLE =
      ClassName.get("org.checkerframework.checker.nullness.qual", "Nullable");
  private static final ClassName EXECUTE_VERTX_SQL = ClassName.get(ExecuteVertxSql.class);
  private static final ClassName EXECUTE_VERTX_SQL_REQ =
      ClassName.get("com.flipkart.krystal.vajram.ext.sql.vertx", "ExecuteVertxSql_Req");
  private static final ParameterizedTypeName ROW_SET_OF_ROW =
      ParameterizedTypeName.get(ClassName.get(RowSet.class), ClassName.get(Row.class));

  private final CodeGenUtility util;
  private final SqlModelParser parser;

  public SqlTraitVajramGen(CodeGenUtility util) {
    this.util = util;
    this.parser = new SqlModelParser(util.processingEnv());
  }

  // ─── Entry point ─────────────────────────────────────────────────────────────

  public void generate(RoundEnvironment roundEnv) {
    parser.validateTableAndWhereElements(roundEnv);
    Set<? extends Element> sqlElements = roundEnv.getElementsAnnotatedWith(SQL.class);
    for (Element element : sqlElements) {
      if (element.getAnnotation(Trait.class) == null) {
        continue;
      }
      if (element.getAnnotation(SELECT.class) == null) {
        continue;
      }
      if (!(element instanceof TypeElement typeElement)) {
        continue;
      }
      try {
        generateSqlVajram(typeElement);
      } catch (Exception e) {
        util.error(
            "[SqlTraitVajramGen] Error generating SQL Vajram for %s: %s"
                .formatted(element, stackTrace(e)),
            element);
      }
    }
  }

  // ─── Main generation ─────────────────────────────────────────────────────────

  private void generateSqlVajram(TypeElement traitElement) throws Exception {
    String pkg =
        util.processingEnv()
            .getElementUtils()
            .getPackageOf(traitElement)
            .getQualifiedName()
            .toString();
    String traitName = traitElement.getSimpleName().toString();
    String vajramName = traitName + SQL_VAJRAM_SUFFIX;

    TraitResultType resultType = parser.parseTraitResultType(traitElement);
    if (resultType == null) {
      util.error(
          "[SqlTraitVajramGen] Cannot resolve TraitDef<T> return type for " + traitName,
          traitElement);
      return;
    }

    // Invariant: TraitDef<T> result type must be a @Projection, never a @Table model.
    if (parser.isTableModel(resultType.projectionElement())) {
      util.error(
          "[SqlTraitVajramGen] "
              + traitName
              + ": TraitDef<T> result type '"
              + resultType.projectionElement().getSimpleName()
              + "' is annotated with @Table. SELECT traits must return a @Projection, never a"
              + " @Table model. Create a dedicated @Projection interface for this query's result"
              + " shape.",
          traitElement);
      return;
    }

    ProjectionInfo proj = parser.parseProjectionInfo(resultType.projectionElement());
    if (proj == null) {
      util.error(
          "[SqlTraitVajramGen] @Projection(over=...) not found on "
              + resultType.projectionElement().getSimpleName()
              + " (required for "
              + traitName
              + ")",
          traitElement);
      return;
    }

    // Invariant: every projected column must exist in the underlying table.
    List<String> invalidCols =
        parser.findInvalidProjectionColumns(resultType.projectionElement(), proj.tableElement());
    if (!invalidCols.isEmpty()) {
      util.error(
          "[SqlTraitVajramGen] "
              + traitName
              + ": projection '"
              + resultType.projectionElement().getSimpleName()
              + "' references columns not present in table '"
              + proj.tableName()
              + "': "
              + invalidCols
              + ". Check column names and @Column overrides.",
          traitElement);
      return;
    }

    String resultPkg =
        util.processingEnv()
            .getElementUtils()
            .getPackageOf(resultType.projectionElement())
            .getQualifiedName()
            .toString();
    String resultName = resultType.projectionElement().getSimpleName().toString();

    List<ExecutableElement> traitInputMethods = findInputMethods(traitElement);
    List<WhereInput> whereInputs = parser.collectWhereInputs(traitInputMethods);

    boolean hasJoins = !proj.joins().isEmpty();

    // Parse ORDER BY / LIMIT from the TraitDef<@LIMIT @ORDER_BY T> type argument
    List<OrderByClause> traitOrderBys = parser.parseOrderBysFromTraitTypeArg(traitElement);
    int traitLimit = parser.parseLimitFromTraitTypeArg(traitElement);

    // Invariant: List-result traits must declare an explicit @LIMIT on the type argument.
    // Fetching an unbounded list can return excessive data. Use @LIMIT(LIMIT.NO_LIMIT) to
    // explicitly opt out. @LIMIT(1) is not valid on a list trait — use TraitDef<T> instead.
    if (resultType.isList()) {
      if (!parser.hasLimitAnnotationOnTraitTypeArg(traitElement)) {
        util.error(
            "[SqlTraitVajramGen] "
                + traitName
                + ": returns a List but has no @LIMIT on the type argument. "
                + "Use @LIMIT(N) to cap the number of returned rows or "
                + "@LIMIT(LIMIT.NO_LIMIT) to explicitly fetch all rows.",
            traitElement);
        return;
      }
      if (traitLimit == 1) {
        util.error(
            "[SqlTraitVajramGen] "
                + traitName
                + ": @LIMIT(1) is not valid on a list-result trait (TraitDef<List<"
                + resultType.projectionElement().getSimpleName()
                + ">>). For a single-row result use TraitDef<@LIMIT(1) "
                + resultType.projectionElement().getSimpleName()
                + "> instead.",
            traitElement);
        return;
      }
    }

    // Build SQL.
    // For single-result JOIN queries, trait-level ORDER BY/LIMIT are intentionally omitted: a
    // LIMIT 1 at the outer query level would truncate child rows, breaking the grouping in
    // mapResult.
    // For list-result JOIN queries, trait-level ORDER BY/LIMIT are applied to the parent table.
    final String sql;
    final String parentPkAlias;
    if (hasJoins) {
      JoinSqlResult joinResult =
          SqlQueryBuilder.buildJoinSql(
              proj, whereInputs, traitOrderBys, traitLimit, resultType.isList());
      sql = joinResult.sql();
      parentPkAlias = joinResult.parentPkAlias();
    } else {
      sql = SqlQueryBuilder.buildSimpleSql(proj, whereInputs, traitOrderBys, traitLimit);
      parentPkAlias = null;
    }

    ClassName traitClass = ClassName.get(pkg, traitName);
    ClassName facClass = ClassName.get(pkg, vajramName + "_Fac");

    TypeName vajramResultTypeName;
    if (resultType.isList()) {
      vajramResultTypeName =
          ParameterizedTypeName.get(
              ClassName.get(List.class), ClassName.get(resultPkg, resultName));
    } else {
      vajramResultTypeName = ClassName.get(resultPkg, resultName);
    }

    TypeSpec vajramSpec =
        util.classBuilder(vajramName, traitClass.canonicalName())
            .addModifiers(PUBLIC, ABSTRACT)
            .addAnnotation(Vajram.class)
            .superclass(
                ParameterizedTypeName.get(
                    ClassName.get(ComputeVajramDef.class), vajramResultTypeName))
            .addSuperinterface(traitClass)
            .addType(buildInputsInterface(traitInputMethods))
            .addType(buildInternalFacetsInterface())
            .addMethod(buildResolveSqlMethod(facClass, sql))
            .addMethod(buildResolveParamsMethod(facClass, whereInputs, traitInputMethods))
            .addMethod(buildResolvePoolMethod(facClass))
            .addMethod(
                buildMapResultMethod(
                    resultType,
                    proj,
                    resultPkg,
                    resultName,
                    vajramResultTypeName,
                    vajramName,
                    parentPkAlias))
            .build();
    ClassName vajramClassName = ClassName.get(pkg, vajramName);

    JavaFile javaFile = JavaFile.builder(pkg, vajramSpec).build();
    util.generateSourceFile(vajramClassName.canonicalName(), javaFile, traitElement);
    util.note("Generated " + pkg + "." + vajramName);
  }

  // ─── _Inputs / _InternalFacets ────────────────────────────────────────────────

  private List<ExecutableElement> findInputMethods(TypeElement traitElement) {
    for (TypeElement nested : ElementFilter.typesIn(traitElement.getEnclosedElements())) {
      if (nested.getSimpleName().contentEquals("_Inputs")) {
        return ElementFilter.methodsIn(nested.getEnclosedElements());
      }
    }
    return List.of();
  }

  private TypeSpec buildInputsInterface(List<ExecutableElement> traitInputMethods) {
    TypeSpec.Builder inputs = TypeSpec.interfaceBuilder("_Inputs").addModifiers(STATIC);
    AnnotationSpec ifAbsentFail =
        AnnotationSpec.builder(IfAbsent.class)
            .addMember("value", "$T.$L", FAIL.getDeclaringClass(), "FAIL")
            .build();
    for (ExecutableElement method : traitInputMethods) {
      inputs.addMethod(
          MethodSpec.methodBuilder(method.getSimpleName().toString())
              .addModifiers(PUBLIC, ABSTRACT)
              .addAnnotation(ifAbsentFail)
              .returns(TypeName.get(method.getReturnType()))
              .build());
    }
    return inputs.build();
  }

  private TypeSpec buildInternalFacetsInterface() {
    AnnotationSpec ifAbsentFail =
        AnnotationSpec.builder(IfAbsent.class)
            .addMember("value", "$T.$L", FAIL.getDeclaringClass(), "FAIL")
            .build();
    MethodSpec poolField =
        MethodSpec.methodBuilder(VERTX_SQL_POOL_FACET)
            .returns(ClassName.get(Pool.class))
            .addModifiers(PUBLIC, ABSTRACT)
            .addAnnotation(ifAbsentFail)
            .addAnnotation(INJECT)
            .addAnnotation(
                AnnotationSpec.builder(NAMED)
                    .addMember("value", "$S", VERTX_SQL_POOL_FACET)
                    .build())
            .build();
    MethodSpec sqlResultField =
        MethodSpec.methodBuilder(SQL_RESULT_FACET)
            .returns(ROW_SET_OF_ROW)
            .addModifiers(PUBLIC, ABSTRACT)
            .addAnnotation(ifAbsentFail)
            .addAnnotation(
                AnnotationSpec.builder(Dependency.class)
                    .addMember("onVajram", "$T.class", EXECUTE_VERTX_SQL)
                    .build())
            .build();
    return TypeSpec.interfaceBuilder("_InternalFacets")
        .addModifiers(STATIC)
        .addMethod(poolField)
        .addMethod(sqlResultField)
        .build();
  }

  // ─── @Resolve methods ────────────────────────────────────────────────────────

  private MethodSpec buildResolveSqlMethod(ClassName facClass, String sql) {
    return MethodSpec.methodBuilder("resolveSql")
        .addModifiers(STATIC)
        .addAnnotation(
            AnnotationSpec.builder(Resolve.class)
                .addMember("dep", "$T.$L", facClass, SQL_RESULT_FACET + "_n")
                .addMember("depInputs", "$T.$L", EXECUTE_VERTX_SQL_REQ, "sql_n")
                .build())
        .returns(String.class)
        .addStatement("return $S", sql)
        .build();
  }

  private MethodSpec buildResolveParamsMethod(
      ClassName facClass, List<WhereInput> whereInputs, List<ExecutableElement> traitInputMethods) {
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("resolveParams")
            .addModifiers(STATIC)
            .addAnnotation(
                AnnotationSpec.builder(Resolve.class)
                    .addMember("dep", "$T.$L", facClass, SQL_RESULT_FACET + "_n")
                    .addMember("depInputs", "$T.$L", EXECUTE_VERTX_SQL_REQ, "params_n")
                    .build())
            .returns(ClassName.get(Tuple.class));
    for (ExecutableElement input : traitInputMethods) {
      method.addParameter(TypeName.get(input.getReturnType()), input.getSimpleName().toString());
    }
    List<String> args = new ArrayList<>();
    for (WhereInput wi : whereInputs) {
      for (String field : wi.fields()) {
        args.add(wi.paramName() + "." + field + "()");
      }
    }
    if (args.isEmpty()) {
      method.addStatement("return $T.tuple()", ClassName.get(Tuple.class));
    } else {
      method.addStatement(
          "return $T.from($T.of($L))",
          ClassName.get(Tuple.class),
          List.class,
          String.join(", ", args));
    }
    return method.build();
  }

  private MethodSpec buildResolvePoolMethod(ClassName facClass) {
    return MethodSpec.methodBuilder("resolvePool")
        .addModifiers(STATIC)
        .addAnnotation(
            AnnotationSpec.builder(Resolve.class)
                .addMember("dep", "$T.$L", facClass, SQL_RESULT_FACET + "_n")
                .addMember("depInputs", "$T.$L", EXECUTE_VERTX_SQL_REQ, "pool_n")
                .build())
        .addParameter(ClassName.get(Pool.class), VERTX_SQL_POOL_FACET)
        .returns(ClassName.get(Pool.class))
        .addStatement("return $L", VERTX_SQL_POOL_FACET)
        .build();
  }

  // ─── @Output mapResult ────────────────────────────────────────────────────────

  private MethodSpec buildMapResultMethod(
      TraitResultType resultType,
      ProjectionInfo proj,
      String resultPkg,
      String resultName,
      TypeName outputReturnTypeName,
      String vajramName,
      String parentPkAlias) {

    if (!proj.joins().isEmpty() && resultType.isList()) {
      return buildListJoinMapResultMethod(
          proj, resultPkg, resultName, outputReturnTypeName, parentPkAlias);
    } else if (!proj.joins().isEmpty()) {
      return buildJoinMapResultMethod(proj, resultPkg, resultName, vajramName, parentPkAlias);
    } else if (resultType.isList()) {
      return buildListMapResultMethod(proj, resultPkg, resultName, outputReturnTypeName);
    } else {
      return buildSingleMapResultMethod(proj, resultPkg, resultName);
    }
  }

  /** Maps the first row to the result type; returns {@code null} when the result set is empty. */
  private MethodSpec buildSingleMapResultMethod(
      ProjectionInfo proj, String resultPkg, String resultName) {
    ClassName resultImmutPojo = ClassName.get(resultPkg, resultName + IMMUT_POJO_SUFFIX);

    MethodSpec.Builder method =
        MethodSpec.methodBuilder("mapResult")
            .addModifiers(STATIC)
            .addAnnotation(Output.class)
            .addAnnotation(NULLABLE)
            .returns(ClassName.get(resultPkg, resultName))
            .addParameter(ROW_SET_OF_ROW, SQL_RESULT_FACET);

    method.addStatement(
        "$T<$T> _it = $L.iterator()", java.util.Iterator.class, Row.class, SQL_RESULT_FACET);
    method.beginControlFlow("if (!_it.hasNext())");
    method.addStatement("return null");
    method.endControlFlow();
    method.addStatement("$T _row = _it.next()", Row.class);

    CodeBlock.Builder chain = CodeBlock.builder().add("return $T._builder()", resultImmutPojo);
    for (ScalarColumn col : proj.scalars()) {
      chain.add(
          "\n    .$L($L)", col.methodName(), rowGetter("_row", col.methodName(), col.javaType()));
    }
    chain.add("\n    ._build()");
    method.addStatement(chain.build());

    return method.build();
  }

  /** Maps all rows to a {@code List<result>}. */
  private MethodSpec buildListMapResultMethod(
      ProjectionInfo proj, String resultPkg, String resultName, TypeName outputReturnTypeName) {
    ClassName resultImmutPojo = ClassName.get(resultPkg, resultName + IMMUT_POJO_SUFFIX);
    ClassName resultClass = ClassName.get(resultPkg, resultName);

    MethodSpec.Builder method =
        MethodSpec.methodBuilder("mapResult")
            .addModifiers(STATIC)
            .addAnnotation(Output.class)
            .returns(outputReturnTypeName)
            .addParameter(ROW_SET_OF_ROW, SQL_RESULT_FACET);

    method.addStatement("$T<$T> _result = new $T<>()", List.class, resultClass, ArrayList.class);
    method.beginControlFlow("for ($T _row : $L)", Row.class, SQL_RESULT_FACET);

    CodeBlock.Builder chain = CodeBlock.builder().add("_result.add($T._builder()", resultImmutPojo);
    for (ScalarColumn col : proj.scalars()) {
      chain.add(
          "\n    .$L($L)", col.methodName(), rowGetter("_row", col.methodName(), col.javaType()));
    }
    chain.add("\n    ._build())");
    method.addStatement(chain.build());

    method.endControlFlow();
    method.addStatement("return _result");
    return method.build();
  }

  /**
   * Maps a LEFT JOIN result set to a {@code List<T>}: multiple parent records, each with their
   * grouped child lists.
   *
   * <p>The parent PK column (via {@code parentPkAlias}) is used as the grouping key. For simple
   * joins (no nested joins on the child projection), each child row is accumulated into a {@link
   * List} keyed by the parent PK. For nested joins (child has its own {@code List<Projection>}
   * methods), a {@link LinkedHashMap} is used to deduplicate level-1 child rows, with separate
   * accumulators per parent for each level-2 join.
   */
  private MethodSpec buildListJoinMapResultMethod(
      ProjectionInfo proj,
      String resultPkg,
      String resultName,
      TypeName outputReturnTypeName,
      String parentPkAlias) {
    ClassName resultImmutPojo = ClassName.get(resultPkg, resultName + IMMUT_POJO_SUFFIX);
    ClassName resultClass = ClassName.get(resultPkg, resultName);
    ClassName builderClass = ClassName.get(resultPkg, resultName + IMMUT_POJO_SUFFIX, "Builder");

    MethodSpec.Builder method =
        MethodSpec.methodBuilder("mapResult")
            .addModifiers(STATIC)
            .addAnnotation(Output.class)
            .returns(outputReturnTypeName)
            .addParameter(ROW_SET_OF_ROW, SQL_RESULT_FACET);

    // Pre-loop: LinkedHashMap<Object, Builder> for parent rows, keyed by parent PK
    method.addStatement(
        "$T<$T, $T> _parentBuilders = new $T<>()",
        LinkedHashMap.class,
        Object.class,
        builderClass,
        LinkedHashMap.class);

    // Pre-loop: one accumulator map per join
    for (JoinRelation join : proj.joins()) {
      String joinProjPkg = projPkg(join);
      String joinProjName = join.projectionElement().getSimpleName().toString();
      ClassName joinClass = ClassName.get(joinProjPkg, joinProjName);
      ClassName joinBuilderClass =
          ClassName.get(joinProjPkg, joinProjName + IMMUT_POJO_SUFFIX, "Builder");

      if (join.nestedJoins().isEmpty()) {
        // Simple: LinkedHashMap<Object, List<JoinType>> — outer key = parent PK
        method.addStatement(
            "$T<$T, $T<$T>> _$L = new $T<>()",
            LinkedHashMap.class,
            Object.class,
            List.class,
            joinClass,
            join.methodName(),
            LinkedHashMap.class);
      } else {
        // Nested: outer key = parent PK, inner key = join PK
        method.addStatement(
            "$T<$T, $T<$T, $T>> _$LBuilders = new $T<>()",
            LinkedHashMap.class,
            Object.class,
            LinkedHashMap.class,
            Object.class,
            joinBuilderClass,
            join.methodName(),
            LinkedHashMap.class);
        for (JoinRelation nested : join.nestedJoins()) {
          String nestedProjPkg = projPkg(nested);
          String nestedProjName = nested.projectionElement().getSimpleName().toString();
          ClassName nestedClass = ClassName.get(nestedProjPkg, nestedProjName);
          // LinkedHashMap<Object, LinkedHashMap<Object, List<NestedType>>>
          method.addStatement(
              "$T<$T, $T<$T, $T<$T>>> _$L_$L = new $T<>()",
              LinkedHashMap.class,
              Object.class,
              LinkedHashMap.class,
              Object.class,
              List.class,
              nestedClass,
              join.methodName(),
              nested.methodName(),
              LinkedHashMap.class);
        }
      }
    }

    method.beginControlFlow("for ($T _row : $L)", Row.class, SQL_RESULT_FACET);

    // Parent key
    method.addStatement("$T _parentKey = _row.getValue($S)", Object.class, parentPkAlias);

    // Init parent entry on first encounter
    method.beginControlFlow("if (_parentKey != null && !_parentBuilders.containsKey(_parentKey))");
    CodeBlock.Builder parentChain =
        CodeBlock.builder().add("_parentBuilders.put(_parentKey, $T._builder()", resultImmutPojo);
    for (ScalarColumn col : proj.scalars()) {
      String alias = proj.tableName() + "_" + col.methodName();
      parentChain.add("\n    .$L($L)", col.methodName(), rowGetter("_row", alias, col.javaType()));
    }
    parentChain.add(")");
    method.addStatement(parentChain.build());
    // Initialise child accumulators for this new parent
    for (JoinRelation join : proj.joins()) {
      if (join.nestedJoins().isEmpty()) {
        method.addStatement("_$L.put(_parentKey, new $T<>())", join.methodName(), ArrayList.class);
      } else {
        method.addStatement(
            "_$LBuilders.put(_parentKey, new $T<>())", join.methodName(), LinkedHashMap.class);
        for (JoinRelation nested : join.nestedJoins()) {
          method.addStatement(
              "_$L_$L.put(_parentKey, new $T<>())",
              join.methodName(),
              nested.methodName(),
              LinkedHashMap.class);
        }
      }
    }
    method.endControlFlow();

    // Accumulate child rows
    for (JoinRelation join : proj.joins()) {
      String joinProjPkg = projPkg(join);
      String joinProjName = join.projectionElement().getSimpleName().toString();
      ClassName joinImmutPojo = ClassName.get(joinProjPkg, joinProjName + IMMUT_POJO_SUFFIX);

      if (join.nestedJoins().isEmpty()) {
        String sentinelAlias = join.tableName() + "_" + join.columns().get(0).methodName();
        method.beginControlFlow(
            "if (_parentKey != null && _row.getValue($S) != null)", sentinelAlias);
        CodeBlock.Builder childChain =
            CodeBlock.builder()
                .add("_$L.get(_parentKey).add($T._builder()", join.methodName(), joinImmutPojo);
        for (ScalarColumn col : join.columns()) {
          String alias = join.tableName() + "_" + col.methodName();
          childChain.add(
              "\n    .$L($L)", col.methodName(), rowGetter("_row", alias, col.javaType()));
        }
        childChain.add("\n    ._build())");
        method.addStatement(childChain.build());
        method.endControlFlow();
      } else {
        // Nested: key on level-1 join PK, accumulate level-2 rows
        String childPkCol =
            join.childPkColumn() != null
                ? join.childPkColumn()
                : join.columns().get(0).methodName();
        String childPkAlias = join.tableName() + "_" + childPkCol;
        method.addStatement(
            "$T _$LKey = _row.getValue($S)", Object.class, join.methodName(), childPkAlias);
        method.beginControlFlow(
            "if (_parentKey != null && _$LKey != null"
                + " && !_$LBuilders.get(_parentKey).containsKey(_$LKey))",
            join.methodName(),
            join.methodName(),
            join.methodName());
        CodeBlock.Builder l1Chain =
            CodeBlock.builder()
                .add(
                    "_$LBuilders.get(_parentKey).put(_$LKey, $T._builder()",
                    join.methodName(),
                    join.methodName(),
                    joinImmutPojo);
        for (ScalarColumn col : join.columns()) {
          String alias = join.tableName() + "_" + col.methodName();
          l1Chain.add("\n    .$L($L)", col.methodName(), rowGetter("_row", alias, col.javaType()));
        }
        l1Chain.add(")");
        method.addStatement(l1Chain.build());
        for (JoinRelation nested : join.nestedJoins()) {
          method.addStatement(
              "_$L_$L.get(_parentKey).put(_$LKey, new $T<>())",
              join.methodName(),
              nested.methodName(),
              join.methodName(),
              ArrayList.class);
        }
        method.endControlFlow();
        // Level-2 accumulation
        for (JoinRelation nested : join.nestedJoins()) {
          String nestedProjPkg = projPkg(nested);
          String nestedProjName = nested.projectionElement().getSimpleName().toString();
          ClassName nestedImmutPojo =
              ClassName.get(nestedProjPkg, nestedProjName + IMMUT_POJO_SUFFIX);
          String nestedSentinelAlias =
              nested.tableName() + "_" + nested.columns().get(0).methodName();
          method.beginControlFlow(
              "if (_parentKey != null && _$LKey != null && _row.getValue($S) != null)",
              join.methodName(),
              nestedSentinelAlias);
          CodeBlock.Builder l2Chain =
              CodeBlock.builder()
                  .add(
                      "_$L_$L.get(_parentKey).get(_$LKey).add($T._builder()",
                      join.methodName(),
                      nested.methodName(),
                      join.methodName(),
                      nestedImmutPojo);
          for (ScalarColumn col : nested.columns()) {
            String alias = nested.tableName() + "_" + col.methodName();
            l2Chain.add(
                "\n    .$L($L)", col.methodName(), rowGetter("_row", alias, col.javaType()));
          }
          l2Chain.add("\n    ._build())");
          method.addStatement(l2Chain.build());
          method.endControlFlow();
        }
      }
    }

    method.endControlFlow(); // for loop

    // Post-loop: assemble List<T> from the parent builder map
    method.addStatement("$T<$T> _result = new $T<>()", List.class, resultClass, ArrayList.class);
    method.beginControlFlow("for ($T _key : _parentBuilders.keySet())", Object.class);

    // For nested joins, build the intermediate List<JoinType> first
    for (JoinRelation join : proj.joins()) {
      if (!join.nestedJoins().isEmpty()) {
        String joinProjPkg = projPkg(join);
        String joinProjName = join.projectionElement().getSimpleName().toString();
        ClassName joinClass = ClassName.get(joinProjPkg, joinProjName);
        method.addStatement(
            "$T<$T> _$L = new $T<>()", List.class, joinClass, join.methodName(), ArrayList.class);
        method.beginControlFlow(
            "for ($T _joinKey : _$LBuilders.get(_key).keySet())", Object.class, join.methodName());
        CodeBlock.Builder assembleJoinChain =
            CodeBlock.builder()
                .add(
                    "_$L.add(_$LBuilders.get(_key).get(_joinKey)",
                    join.methodName(),
                    join.methodName());
        for (JoinRelation nested : join.nestedJoins()) {
          assembleJoinChain.add(
              "\n    .$L(_$L_$L.get(_key).get(_joinKey))",
              nested.methodName(),
              join.methodName(),
              nested.methodName());
        }
        assembleJoinChain.add("\n    ._build())");
        method.addStatement(assembleJoinChain.build());
        method.endControlFlow();
      }
    }

    // Build and add the parent
    CodeBlock.Builder finalChain = CodeBlock.builder().add("_result.add(_parentBuilders.get(_key)");
    for (JoinRelation join : proj.joins()) {
      if (join.nestedJoins().isEmpty()) {
        finalChain.add("\n    .$L(_$L.get(_key))", join.methodName(), join.methodName());
      } else {
        finalChain.add("\n    .$L(_$L)", join.methodName(), join.methodName());
      }
    }
    finalChain.add("\n    ._build())");
    method.addStatement(finalChain.build());

    method.endControlFlow(); // for _key loop
    method.addStatement("return _result");

    return method.build();
  }

  /**
   * Maps a LEFT JOIN result set: one parent record with grouped child lists.
   *
   * <p>For level-1 joins with no nested joins, child rows are accumulated into a {@link List}. For
   * level-1 joins that themselves contain nested joins (multi-level), a {@link LinkedHashMap} is
   * used to deduplicate level-1 rows by their PK, with separate accumulators for each level-2 join.
   *
   * <p>Validation: if any row carries a different parent PK value than the first row, an {@link
   * IllegalStateException} is thrown. This catches cases where a buggy WHERE clause returns rows
   * for multiple parent entities.
   */
  private MethodSpec buildJoinMapResultMethod(
      ProjectionInfo proj,
      String resultPkg,
      String resultName,
      String vajramName,
      String parentPkAlias) {
    ClassName resultImmutPojo = ClassName.get(resultPkg, resultName + IMMUT_POJO_SUFFIX);
    ClassName builderClass = ClassName.get(resultPkg, resultName + IMMUT_POJO_SUFFIX, "Builder");

    MethodSpec.Builder method =
        MethodSpec.methodBuilder("mapResult")
            .addModifiers(STATIC)
            .addAnnotation(Output.class)
            .addAnnotation(NULLABLE)
            .returns(ClassName.get(resultPkg, resultName))
            .addParameter(ROW_SET_OF_ROW, SQL_RESULT_FACET);

    method.addStatement("$T _parent = null", builderClass);

    boolean hasValidation = parentPkAlias != null;
    if (hasValidation) {
      method.addStatement("$T _parentKey = null", Object.class);
    }

    // Pre-loop declarations for each JOIN
    for (JoinRelation join : proj.joins()) {
      String joinProjPkg = projPkg(join);
      String joinProjName = join.projectionElement().getSimpleName().toString();
      ClassName joinClass = ClassName.get(joinProjPkg, joinProjName);
      ClassName joinBuilderClass =
          ClassName.get(joinProjPkg, joinProjName + IMMUT_POJO_SUFFIX, "Builder");

      if (join.nestedJoins().isEmpty()) {
        // Simple: accumulate into a List
        method.addStatement(
            "$T<$T> _$L = new $T<>()", List.class, joinClass, join.methodName(), ArrayList.class);
      } else {
        // Nested: deduplicate level-1 rows by PK key into a LinkedHashMap<Object, Builder>
        method.addStatement(
            "$T<$T, $T> _$LBuilders = new $T<>()",
            LinkedHashMap.class,
            Object.class,
            joinBuilderClass,
            join.methodName(),
            LinkedHashMap.class);
        // One List accumulator per level-2 nested join, keyed by the level-1 PK
        for (JoinRelation nested : join.nestedJoins()) {
          String nestedProjPkg = projPkg(nested);
          String nestedProjName = nested.projectionElement().getSimpleName().toString();
          ClassName nestedClass = ClassName.get(nestedProjPkg, nestedProjName);
          method.addStatement(
              "$T<$T, $T<$T>> _$L_$L = new $T<>()",
              LinkedHashMap.class,
              Object.class,
              List.class,
              nestedClass,
              join.methodName(),
              nested.methodName(),
              LinkedHashMap.class);
        }
      }
    }

    method.beginControlFlow("for ($T _row : $L)", Row.class, SQL_RESULT_FACET);

    // Parent initialisation / identity validation
    if (hasValidation) {
      method.addStatement("$T _rowKey = _row.getValue($S)", Object.class, parentPkAlias);
      method.beginControlFlow("if (_parent == null)");
      method.addStatement("_parentKey = _rowKey");
    } else {
      method.beginControlFlow("if (_parent == null)");
    }

    CodeBlock.Builder parentChain =
        CodeBlock.builder().add("_parent = $T._builder()", resultImmutPojo);
    for (ScalarColumn col : proj.scalars()) {
      String alias = proj.tableName() + "_" + col.methodName();
      parentChain.add("\n    .$L($L)", col.methodName(), rowGetter("_row", alias, col.javaType()));
    }
    method.addStatement(parentChain.build());

    if (hasValidation) {
      method.nextControlFlow("else if (!$T.equals(_parentKey, _rowKey))", Objects.class);
      method.addStatement(
          "throw new $T($S + _parentKey + $S + _rowKey + $S)",
          IllegalStateException.class,
          vajramName
              + ".mapResult: WHERE clause returned multiple parent rows (table='"
              + proj.tableName()
              + "'). Expected single entity with "
              + proj.tableName()
              + "."
              + proj.parentPkColumn()
              + "=",
          " but encountered " + proj.tableName() + "." + proj.parentPkColumn() + "=",
          ". Ensure your WHERE clause inputs uniquely identify one parent row.");
    }

    method.endControlFlow(); // if (_parent == null)

    // Append child rows for each JOIN
    for (JoinRelation join : proj.joins()) {
      String joinProjPkg = projPkg(join);
      String joinProjName = join.projectionElement().getSimpleName().toString();
      ClassName joinImmutPojo = ClassName.get(joinProjPkg, joinProjName + IMMUT_POJO_SUFFIX);

      if (join.nestedJoins().isEmpty()) {
        // Simple: sentinel-null-check then append to list
        String sentinelAlias = join.tableName() + "_" + join.columns().get(0).methodName();
        method.beginControlFlow("if (_row.getValue($S) != null)", sentinelAlias);
        CodeBlock.Builder childChain =
            CodeBlock.builder().add("_$L.add($T._builder()", join.methodName(), joinImmutPojo);
        for (ScalarColumn col : join.columns()) {
          String alias = join.tableName() + "_" + col.methodName();
          childChain.add(
              "\n    .$L($L)", col.methodName(), rowGetter("_row", alias, col.javaType()));
        }
        childChain.add("\n    ._build())");
        method.addStatement(childChain.build());
        method.endControlFlow();
      } else {
        // Nested: deduplicate level-1 by PK, accumulate level-2 into separate lists
        String childPkCol =
            join.childPkColumn() != null
                ? join.childPkColumn()
                : join.columns().get(0).methodName();
        String childPkAlias = join.tableName() + "_" + childPkCol;
        method.addStatement(
            "$T _$LKey = _row.getValue($S)", Object.class, join.methodName(), childPkAlias);
        method.beginControlFlow(
            "if (_$LKey != null && !_$LBuilders.containsKey(_$LKey))",
            join.methodName(),
            join.methodName(),
            join.methodName());
        // Build and store level-1 entry
        CodeBlock.Builder l1Chain =
            CodeBlock.builder()
                .add(
                    "_$LBuilders.put(_$LKey, $T._builder()",
                    join.methodName(),
                    join.methodName(),
                    joinImmutPojo);
        for (ScalarColumn col : join.columns()) {
          String alias = join.tableName() + "_" + col.methodName();
          l1Chain.add("\n    .$L($L)", col.methodName(), rowGetter("_row", alias, col.javaType()));
        }
        l1Chain.add(")");
        method.addStatement(l1Chain.build());
        // Initialise empty accumulators for each nested join
        for (JoinRelation nested : join.nestedJoins()) {
          method.addStatement(
              "_$L_$L.put(_$LKey, new $T<>())",
              join.methodName(),
              nested.methodName(),
              join.methodName(),
              ArrayList.class);
        }
        method.endControlFlow();

        // Level-2: accumulate grandchild rows
        for (JoinRelation nested : join.nestedJoins()) {
          String nestedProjPkg = projPkg(nested);
          String nestedProjName = nested.projectionElement().getSimpleName().toString();
          ClassName nestedImmutPojo =
              ClassName.get(nestedProjPkg, nestedProjName + IMMUT_POJO_SUFFIX);
          String nestedSentinelAlias =
              nested.tableName() + "_" + nested.columns().get(0).methodName();
          method.beginControlFlow(
              "if (_$LKey != null && _row.getValue($S) != null)",
              join.methodName(),
              nestedSentinelAlias);
          CodeBlock.Builder l2Chain =
              CodeBlock.builder()
                  .add(
                      "_$L_$L.get(_$LKey).add($T._builder()",
                      join.methodName(),
                      nested.methodName(),
                      join.methodName(),
                      nestedImmutPojo);
          for (ScalarColumn col : nested.columns()) {
            String alias = nested.tableName() + "_" + col.methodName();
            l2Chain.add(
                "\n    .$L($L)", col.methodName(), rowGetter("_row", alias, col.javaType()));
          }
          l2Chain.add("\n    ._build())");
          method.addStatement(l2Chain.build());
          method.endControlFlow();
        }
      }
    }

    method.endControlFlow(); // for loop

    method.beginControlFlow("if (_parent == null)");
    method.addStatement("return null");
    method.endControlFlow();

    // Post-loop: assemble LinkedHashMap-based joins into lists, then build and return parent
    for (JoinRelation join : proj.joins()) {
      if (!join.nestedJoins().isEmpty()) {
        String joinProjPkg = projPkg(join);
        String joinProjName = join.projectionElement().getSimpleName().toString();
        ClassName joinClass = ClassName.get(joinProjPkg, joinProjName);
        // Build List<JoinType> from the dedup map
        method.addStatement(
            "$T<$T> _$L = new $T<>()", List.class, joinClass, join.methodName(), ArrayList.class);
        method.beginControlFlow(
            "for ($T _key$L : _$LBuilders.keySet())",
            Object.class,
            join.methodName(),
            join.methodName());
        CodeBlock.Builder assembleChain =
            CodeBlock.builder()
                .add(
                    "_$L.add(_$LBuilders.get(_key$L)",
                    join.methodName(),
                    join.methodName(),
                    join.methodName());
        for (JoinRelation nested : join.nestedJoins()) {
          assembleChain.add(
              "\n    .$L(_$L_$L.get(_key$L))",
              nested.methodName(),
              join.methodName(),
              nested.methodName(),
              join.methodName());
        }
        assembleChain.add("\n    ._build())");
        method.addStatement(assembleChain.build());
        method.endControlFlow();
      }
    }

    CodeBlock.Builder finalChain = CodeBlock.builder().add("return _parent");
    for (JoinRelation join : proj.joins()) {
      finalChain.add(".$L(_$L)", join.methodName(), join.methodName());
    }
    finalChain.add("._build()");
    method.addStatement(finalChain.build());

    return method.build();
  }

  /** Returns the package name of a join's projection element. */
  private String projPkg(JoinRelation join) {
    return util.processingEnv()
        .getElementUtils()
        .getPackageOf(join.projectionElement())
        .getQualifiedName()
        .toString();
  }

  // ─── Row getter ──────────────────────────────────────────────────────────────

  private String rowGetter(String rowVar, String alias, TypeMirror type) {
    String q = "\"" + alias + "\"";
    if (type.getKind() == TypeKind.LONG) return rowVar + ".getLong(" + q + ")";
    if (type.getKind() == TypeKind.INT) return rowVar + ".getInteger(" + q + ")";
    if (type.getKind() == TypeKind.BOOLEAN) return rowVar + ".getBoolean(" + q + ")";
    if (type.getKind() == TypeKind.DOUBLE) return rowVar + ".getDouble(" + q + ")";
    if (type.getKind() == TypeKind.FLOAT) return rowVar + ".getFloat(" + q + ")";
    if (type.getKind() == TypeKind.SHORT) return rowVar + ".getShort(" + q + ")";
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

  private static String stackTrace(Exception e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }
}
