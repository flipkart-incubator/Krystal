package com.flipkart.krystal.vajram.ext.sql.codegen;

import static com.flipkart.krystal.vajram.ext.sql.lang.LIMIT.Creator.create;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.AnnotationInfo;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.ModelRootInfo;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.vajram.codegen.common.models.DefaultFacetModel;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.JoinRelation;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.ScalarColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.SelectionInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.SerdeColumnInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereInput;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereLeaf;
import com.flipkart.krystal.vajram.ext.sql.lang.LIMIT;
import com.flipkart.krystal.vajram.ext.sql.lang.ORDER;
import com.flipkart.krystal.vajram.ext.sql.lang.ORDER.Direction;
import com.flipkart.krystal.vajram.ext.sql.lang.SqlWherePredicate;
import com.flipkart.krystal.vajram.ext.sql.lang.WHERE;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsEqualTo;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsGreaterThan;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsGreaterThanOrEqual;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsInRange;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsLessThan;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsLessThanOrEqual;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.logical.SqlOrPredicate;
import com.flipkart.krystal.vajram.ext.sql.model.Column;
import com.flipkart.krystal.vajram.ext.sql.model.ForeignKey;
import com.flipkart.krystal.vajram.ext.sql.model.IncomingForeignKey;
import com.flipkart.krystal.vajram.ext.sql.model.PrimaryKey;
import com.flipkart.krystal.vajram.ext.sql.model.Selection;
import com.flipkart.krystal.vajram.ext.sql.model.SerdeWith;
import com.flipkart.krystal.vajram.ext.sql.model.Table;
import com.flipkart.krystal.vajram.ext.sql.model.TableModel;
import com.flipkart.krystal.vajram.ext.sql.model.UniqueKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Reads {@code @Table}, {@code @Selection}, {@code @WHERE}, {@code @ForeignKey}, and related
 * annotations from source elements and builds {@link SqlQueryModel} records suitable for SQL query
 * generation.
 *
 * <p>This class is framework-agnostic — it does not reference Vert.x or any specific SQL driver.
 */
public final class SqlModelParser {

  private final CodeGenUtility util;
  private final SqlDriverConfig sqlParamPrinter;

  public SqlModelParser(VajramCodeGenUtility vajramUtil, SqlDriverConfig sqlParamPrinter) {
    this.util = vajramUtil.codegenUtil();
    this.sqlParamPrinter = sqlParamPrinter;
  }

  // ─── Trait result type ────────────────────────────────────────────────────────

  // ─── Selection info ─────────────────────────────────────────────────────────

  /**
   * Parses a {@code @Selection(from = TableClass.class)} interface into a {@link SelectionInfo}.
   * Returns {@code null} if the interface does not have {@code @Selection}.
   */
  public @Nullable SelectionInfo parseSelectionInfo(TypeElement selectionElement) {
    Selection selectionAnno = selectionElement.getAnnotation(Selection.class);
    if (selectionAnno == null) {
      return null;
    }
    TypeElement tableElement = util.getTypeElemFromAnnotationMember(selectionAnno::from);
    if (tableElement == null) {
      return null;
    }

    String tableName = getTableName(tableElement);
    String parentPkColumn = findPkColumn(tableElement);

    List<ScalarColumn> scalars = new ArrayList<>();
    List<JoinRelation> joins = new ArrayList<>();

    for (ExecutableElement method : util.extractAndValidateModelMethods(selectionElement)) {
      TypeMirror returnType = method.getReturnType();
      TypeElement joinSelectionElem = getListElementSelection(returnType);
      if (joinSelectionElem != null) {
        JoinRelation join = parseJoinRelation(method, joinSelectionElem, tableElement);
        if (join != null) {
          joins.add(join);
        }
      } else {
        boolean isOpt = util.isOptional(returnType);
        TypeMirror actualType = util.getOptionalInnerType(returnType);
        String dbColumnName = resolveColumnName(method);
        SerdeColumnInfo serdeInfo = resolveSerdeInfoFromTable(dbColumnName, tableElement);
        scalars.add(
            new ScalarColumn(
                method.getSimpleName().toString(), dbColumnName, actualType, isOpt, serdeInfo));
      }
    }

    return new SelectionInfo(
        selectionElement, tableElement, tableName, parentPkColumn, scalars, joins);
  }

  // ─── WHERE inputs ─────────────────────────────────────────────────────────────

  /**
   * Collects {@link WhereInput} records from the given list of trait {@code _Inputs} methods.
   *
   * <p>Supports two patterns:
   *
   * <ul>
   *   <li><b>Simple predicate</b> — the input type is annotated with {@code @WHERE} and extends
   *       {@code SelectionPredicate}. A single {@link WhereLeaf} is produced.
   *   <li><b>OR predicate</b> — the input type extends {@code SqlOrPredicate}. Each method of the
   *       OR interface returns a {@code SelectionPredicate} subtype annotated with {@code @WHERE};
   *       these are collected as multiple {@link WhereLeaf}s joined by {@code OR}.
   * </ul>
   *
   * <p>{@code @Column} and {@code @IsEqualTo} annotations on predicate methods are respected for
   * column name resolution and comparison operator selection.
   */
  public List<WhereInput> collectWhereInputs(@MonotonicNonNull VajramInfo vajramInfo) {
    List<DefaultFacetModel> inputs =
        vajramInfo.givenFacets().stream().filter(fd -> fd.facetType() == FacetType.INPUT).toList();
    util.note("Input facets on " + vajramInfo.lite().vajramId() + " : " + vajramInfo.givenFacets());

    TypeElement sqlOrPredicateType =
        util.processingEnv()
            .getElementUtils()
            .getTypeElement(SqlOrPredicate.class.getCanonicalName());

    List<WhereInput> result = new ArrayList<>();
    for (DefaultFacetModel input : inputs) {
      TypeMirror inputType = input.dataType().typeMirror(util.processingEnv());

      if (!(inputType instanceof DeclaredType dt)) {
        continue;
      }
      if (!(dt.asElement() instanceof TypeElement typeElem)) {
        continue;
      }

      String paramName = input.name();

      // ── SqlOrPredicate path ────────────────────────────────────────────────────
      if (sqlOrPredicateType != null
          && util.processingEnv()
              .getTypeUtils()
              .isAssignable(typeElem.asType(), sqlOrPredicateType.asType())) {
        List<WhereLeaf> leaves = new ArrayList<>();
        for (ExecutableElement orMethod : util.extractAndValidateModelMethods(typeElem)) {
          TypeMirror childType = orMethod.getReturnType();
          if (!(childType instanceof DeclaredType childDt)) {
            continue;
          }
          if (!(childDt.asElement() instanceof TypeElement childTypeElem)) {
            continue;
          }
          WHERE childWhere = childTypeElem.getAnnotation(WHERE.class);
          if (childWhere == null) {
            util.error("SqlPredicate model must have a @WHERE annotation.", childTypeElem);
            continue;
          }
          String accessorPrefix = paramName + "." + orMethod.getSimpleName() + "()";
          leaves.add(parseWhereLeaf(childTypeElem, childWhere, accessorPrefix));
        }
        if (!leaves.isEmpty()) {
          result.add(new WhereInput(paramName, /* isOr= */ true, leaves));
        }
        continue;
      }

      // ── Simple SelectionPredicate path ─────────────────────────────────────────
      WHERE whereAnno = typeElem.getAnnotation(WHERE.class);
      if (whereAnno == null) {
        continue;
      }
      WhereLeaf leaf = parseWhereLeaf(typeElem, whereAnno, paramName);
      result.add(new WhereInput(paramName, /* isOr= */ false, List.of(leaf)));
    }
    return result;
  }

  /**
   * Parses a single {@code @WHERE}-annotated {@code SelectionPredicate} into a {@link WhereLeaf}.
   * Each method in the predicate interface becomes a {@link WhereColumn} with resolved DB column
   * name (via {@code @Column}) and comparison operator (example: {@code @IsEqualTo}, defaulting to
   * {@code "="}).
   */
  private WhereLeaf parseWhereLeaf(
      TypeElement predicateElem, WHERE whereAnno, String accessorPrefix) {
    TypeElement inTable = util.getTypeElemFromAnnotationMember(whereAnno::inTable);
    String inTableName = inTable != null ? getTableName(inTable) : "";

    List<WhereColumn> columns = new ArrayList<>();
    for (ExecutableElement method : util.extractAndValidateModelMethods(predicateElem)) {
      String fieldName = method.getSimpleName().toString();
      String dbCol = resolveColumnName(method, false);
      WhereOperator operator = resolveComparisonOperator(method);
      columns.add(new WhereColumn(fieldName, dbCol, operator));
    }
    return new WhereLeaf(accessorPrefix, inTableName, columns);
  }

  /**
   * Returns the SQL comparison operator for a predicate method based on its annotations. Supports
   * {@code @IsEqualTo} ({@code "="}), {@code @IsGreaterThan} ({@code ">"}),
   * {@code @IsGreaterThanOrEqual} ({@code ">="}), {@code @IsLessThan} ({@code "<"}), and
   * {@code @IsLessThanOrEqual} ({@code "<="}).
   *
   * <p>Ordering operators ({@code >}, {@code >=}, {@code <}, {@code <=}) are only valid on
   * comparable types: numeric primitives and their boxed equivalents, and temporal types.
   */
  public WhereOperator resolveComparisonOperator(ExecutableElement method) {
    if (method.getAnnotation(IsEqualTo.class) != null) {
      return new SimpleWhereOperator("=", sqlParamPrinter);
    }
    if (method.getAnnotation(IsGreaterThan.class) != null) {
      validateComparableType(method, "@IsGreaterThan");
      return new SimpleWhereOperator(">", sqlParamPrinter);
    }
    if (method.getAnnotation(IsGreaterThanOrEqual.class) != null) {
      validateComparableType(method, "@IsGreaterThanOrEqual");
      return new SimpleWhereOperator(">=", sqlParamPrinter);
    }
    if (method.getAnnotation(IsLessThan.class) != null) {
      validateComparableType(method, "@IsLessThan");
      return new SimpleWhereOperator("<", sqlParamPrinter);
    }
    if (method.getAnnotation(IsLessThanOrEqual.class) != null) {
      validateComparableType(method, "@IsLessThanOrEqual");
      return new SimpleWhereOperator("<=", sqlParamPrinter);
    }
    if (method.getAnnotation(IsInRange.class) != null) {
      validateRangeType(method);
      return new RangeWhereOperator(sqlParamPrinter);
    }
    util.error(
        "No comparison operator annotation such as @IsEqualTo, @IsGreaterThan, @IsLessThan, @IsInRange, etc. has been found",
        method);
    return new SimpleWhereOperator("<UNKNOWN_OPERATOR>", sqlParamPrinter);
  }

  private static final Set<TypeKind> COMPARABLE_PRIMITIVES =
      Set.of(TypeKind.INT, TypeKind.LONG, TypeKind.SHORT, TypeKind.FLOAT, TypeKind.DOUBLE);

  private static final Set<String> COMPARABLE_DECLARED_TYPES =
      Set.of(
          "java.lang.Integer",
          "java.lang.Long",
          "java.lang.Short",
          "java.lang.Float",
          "java.lang.Double",
          "java.time.LocalDate",
          "java.time.LocalDateTime",
          "java.time.OffsetDateTime");

  /**
   * Validates that the return-type of a predicate method annotated with {@code @IsInRange} is
   * {@code Range<T>} where {@code T} is a comparable type (numeric boxed types or temporal types).
   */
  private void validateRangeType(ExecutableElement method) {
    TypeMirror returnType = method.getReturnType();
    if (!(returnType instanceof DeclaredType dt)) {
      util.error("@IsInRange requires a return type of Range<T>, but found: " + returnType, method);
      return;
    }
    if (!(dt.asElement() instanceof TypeElement te)
        || !te.getQualifiedName().contentEquals("com.google.common.collect.Range")) {
      util.error("@IsInRange requires a return type of Range<T>, but found: " + returnType, method);
      return;
    }
    if (dt.getTypeArguments().isEmpty()) {
      util.error("@IsInRange requires a parameterized Range<T>, but found raw Range.", method);
      return;
    }
    TypeMirror typeArg = dt.getTypeArguments().get(0);
    if (typeArg instanceof DeclaredType argDt
        && argDt.asElement() instanceof TypeElement argTe
        && COMPARABLE_DECLARED_TYPES.contains(argTe.getQualifiedName().toString())) {
      return;
    }
    util.error(
        "@IsInRange requires Range<T> where T is a comparable type (boxed numerals or temporal"
            + " types like Long, Integer, LocalDate, etc.). Found Range<"
            + typeArg
            + ">.",
        method);
  }

  /**
   * Validates that the return-type of a predicate method is a comparable type suitable for ordering
   * operators ({@code >}, {@code >=}, {@code <}, {@code <=}). Reports a compile-time error if the
   * type is not numeric or temporal.
   */
  private void validateComparableType(ExecutableElement method, String annotationName) {
    TypeMirror returnType = method.getReturnType();
    if (COMPARABLE_PRIMITIVES.contains(returnType.getKind())) {
      return;
    }
    if (returnType instanceof DeclaredType dt
        && dt.asElement() instanceof TypeElement te
        && COMPARABLE_DECLARED_TYPES.contains(te.getQualifiedName().toString())) {
      return;
    }
    util.error(
        annotationName
            + " is only supported on comparable types (numeric primitives, boxed numerals,"
            + " LocalDate, LocalDateTime, OffsetDateTime). Found: "
            + returnType,
        method);
  }

  // ─── Internal helpers ────────────────────────────────────────────────────────

  /**
   * Parses a single {@code List<ChildSelection>} join method into a {@link JoinRelation}.
   *
   * <p><b>Invariant:</b> a nested {@code List<@Selection>} join is only valid when the two
   * underlying tables have a <em>bidirectional</em> FK relationship:
   *
   * <ul>
   *   <li>The child table must have a {@code @ForeignKey(toTable = ParentTable.class)}-annotated
   *       method — this is the actual FK column in the DB schema.
   *   <li>The parent table must have an {@code @IncomingForeignKey}-annotated method whose return
   *       type is {@code List<ChildTable>} or {@code ChildTable} — this models the reverse side of
   *       the relationship and is not a real DB column.
   * </ul>
   *
   * <p>A compile-time error is reported if either annotation is absent, making the constraint
   * visible to developers at build time rather than at runtime.
   */
  private JoinRelation parseJoinRelation(
      ExecutableElement method, TypeElement childSelectionElem, TypeElement parentTableElem) {
    Selection selectionAnno = childSelectionElem.getAnnotation(Selection.class);
    if (selectionAnno == null) {
      return null;
    }
    TypeElement childTableElem = util.getTypeElemFromAnnotationMember(selectionAnno::from);

    String childTableName = getTableName(childTableElem);
    String childFkCol = findFkColumnInChildForParent(childTableElem, parentTableElem);

    // Invariant: child table must have @ForeignKey pointing to the parent table.
    if (childFkCol == null) {
      util.error(
          "[vajram-sql] Join '"
              + method.getSimpleName()
              + "()' in '"
              + method.getEnclosingElement().getSimpleName()
              + "' is invalid: table '"
              + childTableName
              + "' has no @ForeignKey method whose type is '"
              + parentTableElem.getSimpleName()
              + "'. Add @ForeignKey on the FK column in '"
              + childTableName
              + "' and @IncomingForeignKey on the reverse side in '"
              + parentTableElem.getSimpleName()
              + "'.",
          method);
      return null;
    }

    // Invariant: parent table must have @IncomingForeignKey pointing to the child table.
    if (!hasIncomingFkForChild(parentTableElem, childTableElem)) {
      util.error(
          "[vajram-sql] Join '"
              + method.getSimpleName()
              + "()' in '"
              + method.getEnclosingElement().getSimpleName()
              + "' is invalid: table '"
              + parentTableElem.getSimpleName()
              + "' has no @IncomingForeignKey method whose type is '"
              + childTableName
              + "' (or List<"
              + childTableName
              + ">). Add @IncomingForeignKey on the reverse side in '"
              + parentTableElem.getSimpleName()
              + "'.",
          method);
      return null;
    }

    String parentPkCol = findPkColumn(parentTableElem);
    String childPkCol = findPkColumn(childTableElem);
    if (parentPkCol == null) {
      return null;
    }
    LIMIT limit = parseLimit(method.getReturnType());

    // Invariant: every List<Selection> join method must declare @LIMIT.
    // Use @LIMIT(LIMIT.NO_LIMIT) for an unbounded join.
    if (limit == null) {
      util.error(
          "[vajram-sql] @LIMIT is required on return type of '"
              + method.getSimpleName()
              + "()' in '"
              + method.getEnclosingElement().getSimpleName()
              + "'. Fetching an unbounded join can return excessive data. "
              + "Use @LIMIT(N) for a bounded result or @LIMIT(LIMIT.NO_LIMIT) to"
              + " explicitly opt out of a limit.",
          method);
      limit = LIMIT.Creator.create(1);
    }

    List<ScalarColumn> columns = parseScalarColumns(childSelectionElem, childTableElem);
    List<ORDER> orderBys = parseOrderBys(method.getReturnType());

    // Recursively parse nested joins from the child selection's List<Selection> methods
    List<JoinRelation> nestedJoins = new ArrayList<>();
    for (ExecutableElement childMethod : util.extractAndValidateModelMethods(childSelectionElem)) {
      TypeElement grandChildProjElem = getListElementSelection(childMethod.getReturnType());
      if (grandChildProjElem != null) {
        JoinRelation nested = parseJoinRelation(childMethod, grandChildProjElem, childTableElem);
        if (nested != null) {
          nestedJoins.add(nested);
        }
      }
    }

    return new JoinRelation(
        method.getSimpleName().toString(),
        childSelectionElem,
        childTableElem,
        childTableName,
        parentPkCol,
        childFkCol,
        childPkCol,
        columns,
        orderBys,
        limit.value(),
        nestedJoins);
  }

  /** Parses all scalar (non-join) columns from a selection interface. */
  public List<ScalarColumn> parseScalarColumns(
      TypeElement selectionElement, TypeElement tableElement) {
    List<ScalarColumn> result = new ArrayList<>();
    for (ExecutableElement m : util.extractAndValidateModelMethods(selectionElement)) {
      TypeMirror rt = m.getReturnType();
      if (getListElementSelection(rt) != null) {
        continue; // skip List<@Selection> join methods
      }
      boolean isOpt = util.isOptional(rt);
      TypeMirror actualType = util.getOptionalInnerType(rt);
      String dbColumnName = resolveColumnName(m);
      SerdeColumnInfo serdeInfo = resolveSerdeInfoFromTable(dbColumnName, tableElement);
      result.add(
          new ScalarColumn(
              m.getSimpleName().toString(), dbColumnName, actualType, isOpt, serdeInfo));
    }
    return result;
  }

  // ─── Annotation reading ───────────────────────────────────────────────────────

  /** Resolves the DB column name from {@code @Column("name")} or falls back to the method name. */
  public String resolveColumnName(ExecutableElement method) {
    return resolveColumnName(method, true);
  }

  public String resolveColumnName(ExecutableElement method, boolean defaultToMethodName) {
    Column columnAnno = method.getAnnotation(Column.class);
    String name;
    if (columnAnno == null) {
      if (defaultToMethodName) {
        name = method.getSimpleName().toString();
      } else {
        util.error("Could not find @Column annotation on method", method);
        return "<UNKNOWN_COLUMN>";
      }
    } else {
      name = columnAnno.value();
    }
    validateNotReservedKeyword(name, "Column name", method);
    return name;
  }

  /**
   * Returns the SQL table name from {@code @Table(name = "...")} on a table model element.
   *
   * <p>If the {@code name} attribute is blank, the interface's simple name is used.
   *
   * <p>A compile-time error is raised if the resolved name is a SQL reserved keyword.
   */
  public String getTableName(TypeElement tableElement) {
    Table tableAnno = tableElement.getAnnotation(Table.class);
    if (tableAnno == null) {
      throw util.errorAndThrow(tableElement + " does not have @Table annotation");
    }
    String name;
    if (tableAnno.name().isBlank()) {
      name = tableElement.getSimpleName().toString();
    } else {
      name = tableAnno.name();
    }
    validateNotReservedKeyword(name, "Table name", tableElement);
    return name;
  }

  /**
   * Returns the selection element T when {@code returnType} is {@code List<T>} and T has
   * {@code @Selection}. Returns {@code null} otherwise.
   */
  public @Nullable TypeElement getListElementSelection(TypeMirror returnType) {
    if (!util.isListType(returnType)) {
      return null;
    }
    TypeMirror inner = util.getContentType(returnType);
    TypeElement innerElem = (TypeElement) util.processingEnv().getTypeUtils().asElement(inner);
    if (innerElem != null && innerElem.getAnnotation(Selection.class) != null) {
      return innerElem;
    }
    return null;
  }

  /**
   * Finds the FK column name in {@code childTable} whose {@code @ForeignKey(toTable = ...)} points
   * to {@code parentTable}.
   */
  public String findFkColumnInChildForParent(TypeElement childTable, TypeElement parentTable) {
    for (ExecutableElement method : util.extractAndValidateModelMethods(childTable)) {
      ForeignKey fkAnno = method.getAnnotation(ForeignKey.class);
      if (fkAnno == null) {
        continue;
      }
      TypeElement referencedTable = util.getTypeElemFromAnnotationMember(fkAnno::toTable);
      if (referencedTable != null
          && referencedTable.getQualifiedName().equals(parentTable.getQualifiedName())) {
        return resolveColumnName(method);
      }
    }
    return null;
  }

  /** Returns the name of the {@code @PrimaryKey}-annotated column in the given table. */
  public String findPkColumn(TypeElement tableElement) {
    ExecutableElement pkMethod = findPkMethod(tableElement);
    return resolveColumnName(pkMethod, true);
  }

  /**
   * Returns the {@code @PrimaryKey}-annotated method in the given table, or throws a compile-time
   * error if none is found.
   */
  public ExecutableElement findPkMethod(TypeElement tableElement) {
    for (ExecutableElement method : util.extractAndValidateModelMethods(tableElement)) {
      if (method.getAnnotation(PrimaryKey.class) != null) {
        return method;
      }
    }
    throw util.errorAndThrow(
        "@PrimaryKey not found in table "
            + getTableName(tableElement)
            + ". Primary Key is mandatory for tables modelled using vajram-sql",
        tableElement);
  }

  /**
   * Returns {@code true} when {@code parentTable} has at least one {@code @IncomingForeignKey}
   * method whose return type (unwrapped from {@code List<>} if present) is the {@code childTable}.
   */
  public boolean hasIncomingFkForChild(TypeElement parentTable, TypeElement childTable) {
    for (ExecutableElement method : util.extractAndValidateModelMethods(parentTable)) {
      IncomingForeignKey ifkAnno = method.getAnnotation(IncomingForeignKey.class);
      if (ifkAnno == null) {
        continue;
      }
      TypeElement inferredChild = extractSingleOrListType(method.getReturnType());
      if (inferredChild != null
          && inferredChild.getQualifiedName().equals(childTable.getQualifiedName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Validates that every {@code @ForeignKey}-annotated method in a table has a return type matching
   * the {@code @PrimaryKey} return type of the referenced table.
   */
  private void validateForeignKeyTypes(TypeElement tableElement) {
    for (ExecutableElement method : util.extractAndValidateModelMethods(tableElement)) {
      ForeignKey fkAnno = method.getAnnotation(ForeignKey.class);
      if (fkAnno == null) {
        continue;
      }
      TypeElement targetTable = util.getTypeElemFromAnnotationMember(fkAnno::toTable);
      if (targetTable == null) {
        continue;
      }
      ExecutableElement pkMethod;
      try {
        pkMethod = findPkMethod(targetTable);
      } catch (RuntimeException e) {
        // PK not found — already reported by findPkMethod
        continue;
      }
      TypeMirror fkType = method.getReturnType();
      TypeMirror pkType = pkMethod.getReturnType();
      if (!util.processingEnv().getTypeUtils().isSameType(fkType, pkType)) {
        util.error(
            "[vajram-sql] @ForeignKey method '"
                + method.getSimpleName()
                + "()' in '"
                + tableElement.getSimpleName()
                + "' has return type '"
                + fkType
                + "', but the @PrimaryKey '"
                + pkMethod.getSimpleName()
                + "()' in target table '"
                + targetTable.getSimpleName()
                + "' has type '"
                + pkType
                + "'. The FK column type must match the target table's PK type.",
            method);
      }
    }
  }

  /**
   * Extracts the element type from a return type that is either {@code T} or {@code List<T>}.
   * Returns {@code null} if the type cannot be resolved.
   */
  private TypeElement extractSingleOrListType(TypeMirror rt) {
    return switch (util.getContainerType(rt)) {
      case NO_CONTAINER -> (TypeElement) util.processingEnv().getTypeUtils().asElement(rt);
      case LIST ->
          (TypeElement) util.processingEnv().getTypeUtils().asElement(util.getContentType(rt));
      case MAP, RANGE -> null;
    };
  }

  // ─── Structural validations ───────────────────────────────────────────────────

  /**
   * Validates every {@code @Table}-annotated and {@code @WHERE}-annotated element in the round:
   *
   * <ul>
   *   <li>{@code @Table} interface must have {@code @ModelRoot} and must extend {@code TableModel}.
   *   <li>{@code @WHERE} interface must have {@code @ModelRoot} and must extend {@code
   *       WhereClause}.
   * </ul>
   *
   * <p>Errors are reported via {@link CodeGenUtility#error} so that the compiler surfaces them as
   * build failures.
   */
  public void validateTableAndWhereElements(RoundEnvironment roundEnv) {
    TypeElement tableModelType =
        util.processingEnv().getElementUtils().getTypeElement(TableModel.class.getCanonicalName());
    TypeElement whereClauseType =
        util.processingEnv()
            .getElementUtils()
            .getTypeElement(SqlWherePredicate.class.getCanonicalName());

    for (Element element : roundEnv.getElementsAnnotatedWith(Table.class)) {
      if (!(element instanceof TypeElement te)) {
        continue;
      }
      if (te.getAnnotation(ModelRoot.class) == null) {
        util.error(
            "[vajram-sql] @Table interface '"
                + te.getQualifiedName()
                + "' must also be annotated with @ModelRoot.",
            te);
      }
      if (tableModelType != null
          && !util.processingEnv()
              .getTypeUtils()
              .isAssignable(te.asType(), tableModelType.asType())) {
        util.error(
            "[vajram-sql] @Table interface '" + te.getQualifiedName() + "' must extend TableModel.",
            te);
      }

      // Validate FK column types match the target table's PK type
      validateForeignKeyTypes(te);
    }

    for (Element element : roundEnv.getElementsAnnotatedWith(WHERE.class)) {
      if (!(element instanceof TypeElement te)) {
        continue;
      }
      if (te.getAnnotation(ModelRoot.class) == null) {
        util.error(
            "[vajram-sql] @WHERE interface '"
                + te.getQualifiedName()
                + "' must also be annotated with @ModelRoot.",
            te);
      }
      if (whereClauseType != null
          && !util.processingEnv()
              .getTypeUtils()
              .isAssignable(te.asType(), whereClauseType.asType())) {
        util.error(
            "[vajram-sql] @WHERE interface '"
                + te.getQualifiedName()
                + "' must extend WhereClause.",
            te);
      }
    }
  }

  /**
   * Returns {@code true} when the WHERE inputs collectively cover at least one complete unique key
   * of the table (the {@code @PrimaryKey}, a single-column {@code @UniqueKey}, or all columns of a
   * composite {@code @UniqueKey}).
   *
   * <p>Used to enforce the invariant that a single-row SELECT trait must uniquely identify its row.
   */
  public boolean whereClauseCoversSingleRow(
      TypeElement tableElement, List<WhereInput> whereInputs) {
    // OR predicates widen the result set, so they cannot guarantee a single row.
    Set<String> whereFields =
        whereInputs.stream()
            .filter(wi -> !wi.isOr())
            .flatMap(wi -> wi.leaves().stream())
            .flatMap(leaf -> leaf.columns().stream())
            .map(WhereColumn::dbColumnName)
            .collect(Collectors.toSet());

    String pkCol = findPkColumn(tableElement);
    if (pkCol != null && whereFields.contains(pkCol)) {
      return true;
    }

    for (ExecutableElement method : util.extractAndValidateModelMethods(tableElement)) {
      if (method.getAnnotation(UniqueKey.class) != null) {
        if (whereFields.contains(method.getSimpleName().toString())) {
          return true;
        }
      }
    }

    for (UniqueKey uniqueKeyAnno : tableElement.getAnnotationsByType(UniqueKey.class)) {
      String[] columns = uniqueKeyAnno.columns();
      if (whereFields.containsAll(Arrays.asList(columns))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns DB column names referenced in the selection that do not exist as methods on the
   * underlying table. An empty list means every projected column is present in the table schema.
   *
   * <p>{@code @IncomingForeignKey} methods on the table are excluded — they are not real columns.
   * {@code List<Selection>} methods (JOIN signals) in the selection are skipped.
   */
  public List<String> findInvalidSelectionColumns(
      TypeElement selectionElement, TypeElement tableElement) {
    Set<String> tableColumns =
        util.extractAndValidateModelMethods(tableElement).stream()
            .filter(m -> m.getAnnotation(IncomingForeignKey.class) == null)
            .map(this::resolveColumnName)
            .collect(Collectors.toSet());

    List<String> invalid = new ArrayList<>();
    for (ExecutableElement m : util.extractAndValidateModelMethods(selectionElement)) {
      if (getListElementSelection(m.getReturnType()) != null) {
        continue;
      }
      String dbCol = resolveColumnName(m);
      if (!tableColumns.contains(dbCol)) {
        invalid.add("'" + dbCol + "' (method: " + m.getSimpleName() + ")");
      }
    }
    return invalid;
  }

  public List<ORDER> parseOrderBys(TypeMirror type) {
    return util
        .getAnnotationInfos(
            type,
            ORDER.class,
            annoValues ->
                ORDER.Creator.create(
                    String.valueOf(requireNonNull(annoValues.get("by")).getValue()),
                    Direction.valueOf(
                        ((Element) requireNonNull(annoValues.get("direction")).getValue())
                            .getSimpleName()
                            .toString())))
        .stream()
        .map(AnnotationInfo::annotation)
        .toList();
  }

  public @Nullable LIMIT parseLimit(TypeMirror type) {
    List<AnnotationInfo<LIMIT>> annotationInfos =
        util.getAnnotationInfos(
            type,
            LIMIT.class,
            annoParamValues ->
                create((Integer) requireNonNull(annoParamValues.get("value")).getValue()));
    return annotationInfos.isEmpty() ? null : annotationInfos.get(0).annotation();
  }

  // ─── Reserved keyword validation ─────────────────────────────────────────────

  /**
   * SQL reserved keywords (SQL:2016 core + common vendor extensions). Checked at compile time to
   * prevent table or column names from clashing with SQL syntax.
   */
  private static final Set<String> SQL_RESERVED_KEYWORDS =
      Set.of(
          "ADD",
          "ALL",
          "ALTER",
          "AND",
          "ANY",
          "AS",
          "ASC",
          "BETWEEN",
          "BY",
          "CASE",
          "CAST",
          "CHECK",
          "COLUMN",
          "CONSTRAINT",
          "CREATE",
          "CROSS",
          "CURRENT",
          "CURRENT_DATE",
          "CURRENT_TIME",
          "CURRENT_TIMESTAMP",
          "CURRENT_USER",
          "DATABASE",
          "DEFAULT",
          "DELETE",
          "DESC",
          "DISTINCT",
          "DROP",
          "ELSE",
          "END",
          "ESCAPE",
          "EXCEPT",
          "EXISTS",
          "FALSE",
          "FETCH",
          "FOR",
          "FOREIGN",
          "FROM",
          "FULL",
          "GRANT",
          "GROUP",
          "HAVING",
          "IF",
          "IN",
          "INDEX",
          "INNER",
          "INSERT",
          "INTERSECT",
          "INTO",
          "IS",
          "JOIN",
          "KEY",
          "LEFT",
          "LIKE",
          "LIMIT",
          "NATURAL",
          "NOT",
          "NULL",
          "OFFSET",
          "ON",
          "OR",
          "ORDER",
          "OUTER",
          "PRIMARY",
          "REFERENCES",
          "REVOKE",
          "RIGHT",
          "ROLLBACK",
          "SELECT",
          "SET",
          "TABLE",
          "THEN",
          "TO",
          "TRUE",
          "TRUNCATE",
          "UNION",
          "UNIQUE",
          "UPDATE",
          "USER",
          "USING",
          "VALUES",
          "VIEW",
          "WHEN",
          "WHERE",
          "WITH");

  // ─── Serde resolution ──────────────────────────────────────────────────────

  /**
   * Looks up the table method corresponding to {@code dbColumnName} and resolves {@code @SerdeWith}
   * on it. Returns {@code null} if the column has no serde annotation.
   */
  public @Nullable SerdeColumnInfo resolveSerdeInfoFromTable(
      String dbColumnName, TypeElement tableElement) {
    for (ExecutableElement tableMethod : util.extractAndValidateModelMethods(tableElement)) {
      if (tableMethod.getAnnotation(IncomingForeignKey.class) != null) {
        continue;
      }
      if (resolveColumnName(tableMethod).equals(dbColumnName)) {
        TypeMirror actualType = util.getOptionalInnerType(tableMethod.getReturnType());
        return resolveSerdeInfo(tableMethod, actualType);
      }
    }
    return null;
  }

  /**
   * Checks whether the method's return type has a {@code @SerdeWith} annotation. If so, validates
   * that the specified protocol is a {@link SerdeProtocol} and that the model type (the column's
   * element type) has a matching {@code @SupportedModelProtocol}.
   */
  public @Nullable SerdeColumnInfo resolveSerdeInfo(
      ExecutableElement method, TypeMirror actualType) {
    @Nullable AnnotationMirror serdeWith =
        util.getAnnotationMirror(method.getReturnType(), SerdeWith.class);

    if (serdeWith == null) {
      return null;
    }

    TypeMirror protocolType = util.getAnnotationElement(serdeWith, "value", TypeMirror.class);
    TypeElement protocolTypeElement =
        (TypeElement) util.processingEnv().getTypeUtils().asElement(protocolType);

    // Determine the model type element for validation.
    // For List<T>/Optional<T>, we validate on the element type T.
    TypeElement modelTypeElement =
        util.asModelRoot(actualType).map(ModelRootInfo::element).orElse(null);
    if (modelTypeElement != null) {
      List<? extends Element> supportedProtocolTypeElements =
          util.getSupportedProtocolTypeElements(modelTypeElement);
      // Validate that the model type has @SupportedModelProtocol for this protocol
      if (!supportedProtocolTypeElements.contains(protocolTypeElement)) {
        util.error(
            "[vajram-sql] @SerdeWith("
                + protocolTypeElement.getSimpleName()
                + ".class) on column '"
                + method.getSimpleName()
                + "()' but the model type '"
                + modelTypeElement.getQualifiedName()
                + "' does not have a matching @SupportedModelProtocol("
                + protocolTypeElement.getSimpleName()
                + ".class) annotation.",
            method);
        return null;
      }
    }

    return new SerdeColumnInfo(protocolTypeElement, method.getReturnType());
  }

  /**
   * Raises a compile-time error when {@code name} (case-insensitive) matches a SQL reserved
   * keyword.
   */
  private void validateNotReservedKeyword(String name, String kind, Element element) {
    if (SQL_RESERVED_KEYWORDS.contains(name.toUpperCase())) {
      util.error(
          kind
              + " '"
              + name
              + "' is a SQL reserved keyword. Use an annotation like @Table(name = \"...\") or"
              + " @Column(\"...\") to specify a non-reserved name.",
          element);
    }
  }
}
