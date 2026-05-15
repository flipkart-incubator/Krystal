package com.flipkart.krystal.vajram.ext.sql.codegen;

import static com.flipkart.krystal.vajram.ext.sql.statement.LIMIT.Creator.create;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.AnnotationInfo;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.vajram.codegen.common.models.DefaultFacetModel;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.JoinRelation;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.ScalarColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.SelectionInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereInput;
import com.flipkart.krystal.vajram.ext.sql.model.ForeignKey;
import com.flipkart.krystal.vajram.ext.sql.model.IncomingForeignKey;
import com.flipkart.krystal.vajram.ext.sql.model.PrimaryKey;
import com.flipkart.krystal.vajram.ext.sql.model.Table;
import com.flipkart.krystal.vajram.ext.sql.model.TableModel;
import com.flipkart.krystal.vajram.ext.sql.model.UniqueKey;
import com.flipkart.krystal.vajram.ext.sql.statement.Column;
import com.flipkart.krystal.vajram.ext.sql.statement.LIMIT;
import com.flipkart.krystal.vajram.ext.sql.statement.ORDER;
import com.flipkart.krystal.vajram.ext.sql.statement.ORDER.Direction;
import com.flipkart.krystal.vajram.ext.sql.statement.Selection;
import com.flipkart.krystal.vajram.ext.sql.statement.WHERE;
import com.flipkart.krystal.vajram.ext.sql.statement.WhereClause;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
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

  public SqlModelParser(VajramCodeGenUtility vajramUtil) {
    this.util = vajramUtil.codegenUtil();
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
        scalars.add(
            new ScalarColumn(
                method.getSimpleName().toString(), resolveColumnName(method), actualType, isOpt));
      }
    }

    return new SelectionInfo(
        selectionElement, tableElement, tableName, parentPkColumn, scalars, joins);
  }

  // ─── WHERE inputs ─────────────────────────────────────────────────────────────

  /**
   * Collects {@link WhereInput} records from the given list of trait {@code _Inputs} methods. Only
   * methods whose return type is annotated with {@code @WHERE} are included.
   */
  public List<WhereInput> collectWhereInputs(@MonotonicNonNull VajramInfo vajramInfo) {
    List<DefaultFacetModel> inputs =
        vajramInfo.givenFacets().stream().filter(fd -> fd.facetType() == FacetType.INPUT).toList();
    util.note("Input facets on " + vajramInfo.lite().vajramId() + " : " + vajramInfo.givenFacets());

    List<WhereInput> result = new ArrayList<>();
    for (DefaultFacetModel input : inputs) {
      TypeMirror inputType = input.dataType().typeMirror(util.processingEnv());

      if (!(inputType instanceof DeclaredType dt)) {
        continue;
      }
      if (!(dt.asElement() instanceof TypeElement typeElem)) {
        continue;
      }
      WHERE whereAnno = typeElem.getAnnotation(WHERE.class);
      if (whereAnno == null) {
        continue;
      }

      TypeElement inTable = util.getTypeElemFromAnnotationMember(whereAnno::inTable);
      String inTableName = inTable != null ? getTableName(inTable) : "";

      List<String> fields = new ArrayList<>();
      for (ExecutableElement wm : util.extractAndValidateModelMethods(typeElem)) {
        fields.add(wm.getSimpleName().toString());
      }
      result.add(new WhereInput(input.name(), typeElem, inTable, inTableName, fields));
    }
    return result;
  }

  // ─── Internal helpers ────────────────────────────────────────────────────────

  /**
   * Parses a single {@code List<ChildSelection>} join method into a {@link JoinRelation}.
   *
   * <p><b>Invariant:</b> a nested {@code List<@Selection>} join is only valid when the two
   * underlying tables have a <em>bidirectional</em> FK relationship:
   *
   * <ul>
   *   <li>The child table must have a {@code @ForeignKey}-annotated method whose return type is the
   *       parent table model type — this is the actual FK column in the DB schema.
   *   <li>The parent table must have an {@code @IncomingForeignKey}-annotated method whose return
   *       type is {@code List<ChildTable>} (or {@code ChildTable}) — this models the reverse side
   *       of the relationship and is not a real DB column.
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

    List<ScalarColumn> columns = parseScalarColumns(childSelectionElem);
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
  public List<ScalarColumn> parseScalarColumns(TypeElement selectionElement) {
    List<ScalarColumn> result = new ArrayList<>();
    for (ExecutableElement m : util.extractAndValidateModelMethods(selectionElement)) {
      TypeMirror rt = m.getReturnType();
      if (getListElementSelection(rt) != null) {
        continue; // skip List<@Selection> join methods
      }
      boolean isOpt = util.isOptional(rt);
      TypeMirror actualType = util.getOptionalInnerType(rt);
      result.add(
          new ScalarColumn(m.getSimpleName().toString(), resolveColumnName(m), actualType, isOpt));
    }
    return result;
  }

  // ─── Annotation reading ───────────────────────────────────────────────────────

  /** Resolves the DB column name from {@code @Column("name")} or falls back to the method name. */
  public String resolveColumnName(ExecutableElement method) {
    Column columnAnno = method.getAnnotation(Column.class);
    if (columnAnno == null) {
      return method.getSimpleName().toString();
    } else {
      return columnAnno.value();
    }
  }

  /** Returns the SQL table name from {@code @Table(name = "...")} on a table model element. */
  public String getTableName(TypeElement tableElement) {
    Table tableAnno = tableElement.getAnnotation(Table.class);
    if (tableAnno == null) {
      return tableElement.getSimpleName().toString().toLowerCase();
    }
    return tableAnno.name();
  }

  /**
   * Returns the selection element T when {@code returnType} is {@code List<T>} and T has
   * {@code @Selection}. Returns {@code null} otherwise.
   */
  public @Nullable TypeElement getListElementSelection(TypeMirror returnType) {
    if (!(returnType instanceof DeclaredType dt)) {
      return null;
    }
    if (!(dt.asElement() instanceof TypeElement te)) {
      return null;
    }
    if (!te.getQualifiedName().contentEquals("java.util.List")) {
      return null;
    }
    if (dt.getTypeArguments().isEmpty()) {
      return null;
    }
    TypeMirror inner = dt.getTypeArguments().get(0);
    TypeElement innerElem = (TypeElement) util.processingEnv().getTypeUtils().asElement(inner);
    if (innerElem != null && innerElem.getAnnotation(Selection.class) != null) {
      return innerElem;
    }
    return null;
  }

  /** Finds the FK column name in {@code childTable} whose type references {@code parentTable}. */
  public String findFkColumnInChildForParent(TypeElement childTable, TypeElement parentTable) {
    for (ExecutableElement method : util.extractAndValidateModelMethods(childTable)) {
      if (method.getAnnotation(ForeignKey.class) == null) {
        continue;
      }
      if (!(method.getReturnType() instanceof DeclaredType dt)) {
        continue;
      }
      if (!(dt.asElement() instanceof TypeElement refElem)) {
        continue;
      }
      if (refElem.getQualifiedName().equals(parentTable.getQualifiedName())) {
        return method.getSimpleName().toString();
      }
    }
    return null;
  }

  /** Returns the name of the {@code @PrimaryKey}-annotated column in the given table. */
  public String findPkColumn(TypeElement tableElement) {
    for (ExecutableElement method : util.extractAndValidateModelMethods(tableElement)) {
      if (method.getAnnotation(PrimaryKey.class) != null) {
        return method.getSimpleName().toString();
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
   * method whose return type is {@code ChildTable} or {@code List<ChildTable>}.
   */
  public boolean hasIncomingFkForChild(TypeElement parentTable, TypeElement childTable) {
    for (ExecutableElement method : util.extractAndValidateModelMethods(parentTable)) {
      if (method.getAnnotation(IncomingForeignKey.class) == null) {
        continue;
      }
      TypeElement refType = extractSingleOrListType(method.getReturnType());
      if (refType != null && refType.getQualifiedName().equals(childTable.getQualifiedName())) {
        return true;
      }
    }
    return false;
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
      case MAP -> null;
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
        util.processingEnv().getElementUtils().getTypeElement(WhereClause.class.getCanonicalName());

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
    Set<String> whereFields =
        whereInputs.stream().flatMap(wi -> wi.fields().stream()).collect(Collectors.toSet());

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
            .map(m -> m.getSimpleName().toString())
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
}
