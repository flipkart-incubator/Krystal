package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.JoinRelation;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.OrderByClause;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.ProjectionInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.ScalarColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.TraitResultType;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereInput;
import com.flipkart.krystal.vajram.ext.sql.model.ForeignKey;
import com.flipkart.krystal.vajram.ext.sql.model.IncomingForeignKey;
import com.flipkart.krystal.vajram.ext.sql.model.PrimaryKey;
import com.flipkart.krystal.vajram.ext.sql.model.Table;
import com.flipkart.krystal.vajram.ext.sql.model.UniqueKey;
import com.flipkart.krystal.vajram.ext.sql.statement.Column;
import com.flipkart.krystal.vajram.ext.sql.statement.ORDER;
import com.flipkart.krystal.vajram.ext.sql.statement.Projection;
import com.flipkart.krystal.vajram.ext.sql.statement.WHERE;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Reads {@code @Table}, {@code @Projection}, {@code @WHERE}, {@code @ForeignKey}, and related
 * annotations from source elements and builds {@link SqlQueryModel} records suitable for SQL query
 * generation.
 *
 * <p>This class is framework-agnostic — it does not reference Vert.x or any specific SQL driver.
 */
public final class SqlModelParser {

  private static final String TRAIT_DEF_CLASS = "com.flipkart.krystal.vajram.TraitDef";

  private static final String MODEL_ROOT_ANNO = "com.flipkart.krystal.model.ModelRoot";
  private static final String TABLE_MODEL_FQN =
      "com.flipkart.krystal.vajram.ext.sql.model.TableModel";
  private static final String WHERE_CLAUSE_FQN =
      "com.flipkart.krystal.vajram.ext.sql.statement.WhereClause";

  private static final String TABLE_ANNO = Table.class.getName();
  private static final String PROJECTION_ANNO = Projection.class.getName();
  private static final String COLUMN_ANNO = Column.class.getName();
  private static final String WHERE_ANNO = WHERE.class.getName();
  private static final String FOREIGN_KEY_ANNO = ForeignKey.class.getName();
  private static final String INCOMING_FK_ANNO = IncomingForeignKey.class.getName();
  private static final String PRIMARY_KEY_ANNO = PrimaryKey.class.getName();
  private static final String UNIQUE_KEY_ANNO = UniqueKey.class.getName();
  private static final String ORDER_BY_ANNO = ORDER.class.getName();
  private static final String ORDER_BYS_ANNO = ORDER.class.getName() + ".ORDER_BYs";
  private static final String LIMIT_ANNO = "com.flipkart.krystal.vajram.ext.sql.statement.LIMIT";

  private final CodeGenUtility util;

  public SqlModelParser(CodeGenUtility util) {
    this.util = util;
  }

  // ─── Trait result type ────────────────────────────────────────────────────────

  /**
   * Parses the {@code T} from {@code TraitDef<T>} or {@code TraitDef<List<T>>} on the given trait
   * element. Returns {@code null} if the trait does not extend {@code TraitDef}.
   */
  public TraitResultType parseTraitResultType(TypeElement traitElement) {
    for (TypeMirror iface : traitElement.getInterfaces()) {
      if (!(iface instanceof DeclaredType dt)) {
        continue;
      }
      if (!(dt.asElement() instanceof TypeElement te)) {
        continue;
      }
      if (!te.getQualifiedName().contentEquals(TRAIT_DEF_CLASS)) {
        continue;
      }
      if (dt.getTypeArguments().isEmpty()) {
        continue;
      }
      TypeMirror arg = dt.getTypeArguments().get(0);
      if (!(arg instanceof DeclaredType argDt)) {
        continue;
      }
      if (!(argDt.asElement() instanceof TypeElement argElem)) {
        continue;
      }
      if (argElem.getQualifiedName().contentEquals("java.util.List")) {
        if (!argDt.getTypeArguments().isEmpty()) {
          TypeMirror inner = argDt.getTypeArguments().get(0);
          TypeElement innerElem =
              (TypeElement) util.processingEnv().getTypeUtils().asElement(inner);
          if (innerElem != null) {
            return new TraitResultType(innerElem, true);
          }
        }
      } else {
        return new TraitResultType(argElem, false);
      }
    }
    return null;
  }

  // ─── Projection info ─────────────────────────────────────────────────────────

  /**
   * Parses a {@code @Projection(over = TableClass.class)} interface into a {@link ProjectionInfo}.
   * Returns {@code null} if the interface does not have {@code @Projection}.
   */
  public ProjectionInfo parseProjectionInfo(TypeElement projectionElement) {
    TypeElement tableElement = getAnnotationClassValue(projectionElement, PROJECTION_ANNO, "over");
    if (tableElement == null) {
      return null;
    }

    String tableName = getTableName(tableElement);
    String parentPkColumn = findPkColumn(tableElement);

    List<ScalarColumn> scalars = new ArrayList<>();
    List<JoinRelation> joins = new ArrayList<>();

    for (ExecutableElement method :
        ElementFilter.methodsIn(projectionElement.getEnclosedElements())) {
      TypeMirror returnType = method.getReturnType();
      TypeElement joinProjElem = getListElementProjection(returnType);
      if (joinProjElem != null) {
        JoinRelation join = parseJoinRelation(method, joinProjElem, tableElement);
        if (join != null) {
          joins.add(join);
        }
      } else {
        boolean isOpt = isOptionalType(returnType);
        TypeMirror actualType = isOpt ? getOptionalInnerType(returnType) : returnType;
        scalars.add(
            new ScalarColumn(
                method.getSimpleName().toString(), resolveColumnName(method), actualType, isOpt));
      }
    }

    return new ProjectionInfo(
        projectionElement, tableElement, tableName, parentPkColumn, scalars, joins);
  }

  // ─── WHERE inputs ─────────────────────────────────────────────────────────────

  /**
   * Collects {@link WhereInput} records from the given list of trait {@code _Inputs} methods. Only
   * methods whose return type is annotated with {@code @WHERE} are included.
   */
  public List<WhereInput> collectWhereInputs(List<ExecutableElement> inputMethods) {
    List<WhereInput> result = new ArrayList<>();
    for (ExecutableElement method : inputMethods) {
      TypeMirror rt = method.getReturnType();
      if (!(rt instanceof DeclaredType dt)) {
        continue;
      }
      if (!(dt.asElement() instanceof TypeElement typeElem)) {
        continue;
      }
      if (!hasAnnotation(typeElem, WHERE_ANNO)) {
        continue;
      }

      TypeElement inTable = getAnnotationClassValue(typeElem, WHERE_ANNO, "inTable");
      String inTableName = inTable != null ? getTableName(inTable) : "";

      List<String> fields = new ArrayList<>();
      for (ExecutableElement wm : ElementFilter.methodsIn(typeElem.getEnclosedElements())) {
        fields.add(wm.getSimpleName().toString());
      }
      result.add(
          new WhereInput(
              method.getSimpleName().toString(), typeElem, inTable, inTableName, fields));
    }
    return result;
  }

  // ─── Internal helpers ────────────────────────────────────────────────────────

  /**
   * Parses a single {@code List<ChildProjection>} join method into a {@link JoinRelation}.
   *
   * <p><b>Invariant:</b> a nested {@code List<@Projection>} join is only valid when the two
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
      ExecutableElement method, TypeElement childProjElem, TypeElement parentTableElem) {
    TypeElement childTableElem = getAnnotationClassValue(childProjElem, PROJECTION_ANNO, "over");
    if (childTableElem == null) {
      return null;
    }

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

    // Invariant: every List<Projection> join method must declare @LIMIT.
    // Use @LIMIT(LIMIT.NO_LIMIT) for an unbounded join.
    if (!hasAnnotationOnElement(method, LIMIT_ANNO)) {
      util.error(
          "[vajram-sql] @LIMIT is required on '"
              + method.getSimpleName()
              + "()' in '"
              + method.getEnclosingElement().getSimpleName()
              + "'. Fetching an unbounded join can return excessive data. "
              + "Use @LIMIT(N) for a bounded result or @LIMIT(LIMIT.NO_LIMIT) to"
              + " explicitly opt out of a limit.",
          method);
    }

    List<ScalarColumn> columns = parseScalarColumns(childProjElem);
    List<OrderByClause> orderBys = parseOrderBys(method);
    int limit = parseLimit(method);

    // Recursively parse nested joins from the child projection's List<Projection> methods
    List<JoinRelation> nestedJoins = new ArrayList<>();
    for (ExecutableElement childMethod :
        ElementFilter.methodsIn(childProjElem.getEnclosedElements())) {
      TypeElement grandChildProjElem = getListElementProjection(childMethod.getReturnType());
      if (grandChildProjElem != null) {
        JoinRelation nested = parseJoinRelation(childMethod, grandChildProjElem, childTableElem);
        if (nested != null) {
          nestedJoins.add(nested);
        }
      }
    }

    return new JoinRelation(
        method.getSimpleName().toString(),
        childProjElem,
        childTableElem,
        childTableName,
        parentPkCol,
        childFkCol,
        childPkCol,
        columns,
        orderBys,
        limit,
        nestedJoins);
  }

  /** Parses all scalar (non-join) columns from a projection interface. */
  public List<ScalarColumn> parseScalarColumns(TypeElement projectionElement) {
    List<ScalarColumn> result = new ArrayList<>();
    for (ExecutableElement m : ElementFilter.methodsIn(projectionElement.getEnclosedElements())) {
      TypeMirror rt = m.getReturnType();
      if (getListElementProjection(rt) != null) {
        continue; // skip List<@Projection> join methods
      }
      boolean isOpt = isOptionalType(rt);
      TypeMirror actualType = isOpt ? getOptionalInnerType(rt) : rt;
      result.add(
          new ScalarColumn(m.getSimpleName().toString(), resolveColumnName(m), actualType, isOpt));
    }
    return result;
  }

  // ─── Annotation reading ───────────────────────────────────────────────────────

  /** Resolves the DB column name from {@code @Column("name")} or falls back to the method name. */
  public String resolveColumnName(ExecutableElement method) {
    for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
      if (!mirror.getAnnotationType().toString().equals(COLUMN_ANNO)) {
        continue;
      }
      for (AnnotationValue value : mirror.getElementValues().values()) {
        String col = (String) value.getValue();
        if (!col.isEmpty()) {
          return col;
        }
      }
    }
    return method.getSimpleName().toString();
  }

  /** Returns the SQL table name from {@code @Table(name = "...")} on a table model element. */
  public String getTableName(TypeElement tableElement) {
    for (AnnotationMirror mirror : tableElement.getAnnotationMirrors()) {
      if (!mirror.getAnnotationType().toString().equals(TABLE_ANNO)) {
        continue;
      }
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e :
          mirror.getElementValues().entrySet()) {
        if (e.getKey().getSimpleName().contentEquals("name")) {
          String name = (String) e.getValue().getValue();
          if (!name.isEmpty()) {
            return name;
          }
        }
      }
      return tableElement.getSimpleName().toString().toLowerCase();
    }
    return tableElement.getSimpleName().toString().toLowerCase();
  }

  /**
   * Reads a {@code Class}-typed annotation attribute and returns the corresponding element. Returns
   * {@code null} if the annotation or attribute is absent.
   */
  public TypeElement getAnnotationClassValue(TypeElement element, String annoFqn, String attrName) {
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (!mirror.getAnnotationType().toString().equals(annoFqn)) {
        continue;
      }
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e :
          mirror.getElementValues().entrySet()) {
        if (!e.getKey().getSimpleName().contentEquals(attrName)) {
          continue;
        }
        TypeMirror tm = (TypeMirror) e.getValue().getValue();
        return (TypeElement) util.processingEnv().getTypeUtils().asElement(tm);
      }
    }
    return null;
  }

  /** Returns {@code true} if the element carries an annotation with the given FQN. */
  public boolean hasAnnotation(TypeElement element, String annoFqn) {
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (mirror.getAnnotationType().toString().equals(annoFqn)) {
        return true;
      }
    }
    return false;
  }

  /** Returns {@code true} if the method element carries an annotation with the given FQN. */
  public boolean hasAnnotationOnElement(ExecutableElement element, String annoFqn) {
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (mirror.getAnnotationType().toString().equals(annoFqn)) {
        return true;
      }
    }
    return false;
  }

  public boolean isOptionalType(TypeMirror type) {
    if (!(type instanceof DeclaredType dt)) {
      return false;
    }
    if (!(dt.asElement() instanceof TypeElement te)) {
      return false;
    }
    return te.getQualifiedName().contentEquals("java.util.Optional");
  }

  public TypeMirror getOptionalInnerType(TypeMirror type) {
    if (type instanceof DeclaredType dt && !dt.getTypeArguments().isEmpty()) {
      return dt.getTypeArguments().get(0);
    }
    return type;
  }

  /**
   * Returns the projection element T when {@code returnType} is {@code List<T>} and T has
   * {@code @Projection}. Returns {@code null} otherwise.
   */
  public @Nullable TypeElement getListElementProjection(TypeMirror returnType) {
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
    if (innerElem != null && hasAnnotation(innerElem, PROJECTION_ANNO)) {
      return innerElem;
    }
    return null;
  }

  /** Finds the FK column name in {@code childTable} whose type references {@code parentTable}. */
  public String findFkColumnInChildForParent(TypeElement childTable, TypeElement parentTable) {
    for (ExecutableElement method : ElementFilter.methodsIn(childTable.getEnclosedElements())) {
      if (!hasAnnotationOnElement(method, FOREIGN_KEY_ANNO)) {
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
    for (ExecutableElement method : ElementFilter.methodsIn(tableElement.getEnclosedElements())) {
      if (hasAnnotationOnElement(method, PRIMARY_KEY_ANNO)) {
        return method.getSimpleName().toString();
      }
    }
    return null;
  }

  /**
   * Returns {@code true} when {@code parentTable} has at least one {@code @IncomingForeignKey}
   * method whose return type is {@code ChildTable} or {@code List<ChildTable>}.
   */
  public boolean hasIncomingFkForChild(TypeElement parentTable, TypeElement childTable) {
    for (ExecutableElement method : ElementFilter.methodsIn(parentTable.getEnclosedElements())) {
      if (!hasAnnotationOnElement(method, INCOMING_FK_ANNO)) {
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
    if (!(rt instanceof DeclaredType dt)) {
      return null;
    }
    TypeElement te = (TypeElement) util.processingEnv().getTypeUtils().asElement(dt);
    if (te == null) {
      return null;
    }
    if (te.getQualifiedName().contentEquals("java.util.List") && !dt.getTypeArguments().isEmpty()) {
      TypeMirror inner = dt.getTypeArguments().get(0);
      return (TypeElement) util.processingEnv().getTypeUtils().asElement(inner);
    }
    return te;
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
        util.processingEnv().getElementUtils().getTypeElement(TABLE_MODEL_FQN);
    TypeElement whereClauseType =
        util.processingEnv().getElementUtils().getTypeElement(WHERE_CLAUSE_FQN);

    for (Element element : roundEnv.getElementsAnnotatedWith(Table.class)) {
      if (!(element instanceof TypeElement te)) {
        continue;
      }
      if (!hasAnnotation(te, MODEL_ROOT_ANNO)) {
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
      if (!hasAnnotation(te, MODEL_ROOT_ANNO)) {
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

  /** Returns {@code true} if the element is annotated with {@code @Table}. */
  public boolean isTableModel(TypeElement element) {
    return hasAnnotation(element, TABLE_ANNO);
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

    for (ExecutableElement method : ElementFilter.methodsIn(tableElement.getEnclosedElements())) {
      if (hasAnnotationOnElement(method, UNIQUE_KEY_ANNO)) {
        if (whereFields.contains(method.getSimpleName().toString())) {
          return true;
        }
      }
    }

    for (AnnotationMirror mirror : tableElement.getAnnotationMirrors()) {
      if (!mirror.getAnnotationType().toString().equals(UNIQUE_KEY_ANNO)) {
        continue;
      }
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e :
          mirror.getElementValues().entrySet()) {
        if (!e.getKey().getSimpleName().contentEquals("columns")) {
          continue;
        }
        List<?> colValues = (List<?>) e.getValue().getValue();
        Set<String> keyColumns = new HashSet<>();
        for (Object cv : colValues) {
          if (cv instanceof AnnotationValue av) {
            keyColumns.add((String) av.getValue());
          }
        }
        if (!keyColumns.isEmpty() && whereFields.containsAll(keyColumns)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns DB column names referenced in the projection that do not exist as methods on the
   * underlying table. An empty list means every projected column is present in the table schema.
   *
   * <p>{@code @IncomingForeignKey} methods on the table are excluded — they are not real columns.
   * {@code List<Projection>} methods (JOIN signals) in the projection are skipped.
   */
  public List<String> findInvalidProjectionColumns(
      TypeElement projectionElement, TypeElement tableElement) {
    Set<String> tableColumns =
        ElementFilter.methodsIn(tableElement.getEnclosedElements()).stream()
            .filter(m -> !hasAnnotationOnElement(m, INCOMING_FK_ANNO))
            .map(m -> m.getSimpleName().toString())
            .collect(Collectors.toSet());

    List<String> invalid = new ArrayList<>();
    for (ExecutableElement m : ElementFilter.methodsIn(projectionElement.getEnclosedElements())) {
      if (getListElementProjection(m.getReturnType()) != null) {
        continue;
      }
      String dbCol = resolveColumnName(m);
      if (!tableColumns.contains(dbCol)) {
        invalid.add("'" + dbCol + "' (method: " + m.getSimpleName() + ")");
      }
    }
    return invalid;
  }

  /**
   * Parses {@code @ORDER_BY} annotations from the type argument of {@code TraitDef<T>}.
   *
   * <p>For example, {@code extends TraitDef<@ORDER(by="name", direction=ASC) UserInfo>} will return
   * a single {@link OrderByClause}. Multiple {@code @ORDER} annotations are supported via the
   * repeatable container.
   */
  public List<OrderByClause> parseOrderBysFromTraitTypeArg(TypeElement traitElement) {
    TypeMirror typeArg = findTraitDefTypeArg(traitElement);
    if (typeArg == null) {
      return List.of();
    }
    List<OrderByClause> result = new ArrayList<>();
    for (AnnotationMirror mirror : typeArg.getAnnotationMirrors()) {
      String type = mirror.getAnnotationType().toString();
      if (type.equals(ORDER_BY_ANNO)) {
        result.add(parseOrderBy(mirror));
      } else if (type.equals(ORDER_BYS_ANNO)) {
        for (AnnotationValue containerVal : mirror.getElementValues().values()) {
          if (!(containerVal.getValue() instanceof List<?> list)) {
            continue;
          }
          for (Object item : list) {
            if (item instanceof AnnotationValue av
                && av.getValue() instanceof AnnotationMirror am) {
              result.add(parseOrderBy(am));
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Parses the {@code @LIMIT} value from the type argument of {@code TraitDef<T>}; returns {@code
   * -1} if absent.
   *
   * <p>For example, {@code extends TraitDef<@LIMIT(1) UserInfo>} returns {@code 1}.
   */
  public int parseLimitFromTraitTypeArg(TypeElement traitElement) {
    TypeMirror typeArg = findTraitDefTypeArg(traitElement);
    if (typeArg == null) {
      return -1;
    }
    for (AnnotationMirror mirror : typeArg.getAnnotationMirrors()) {
      if (!mirror.getAnnotationType().toString().equals(LIMIT_ANNO)) {
        continue;
      }
      for (AnnotationValue val : mirror.getElementValues().values()) {
        return ((Number) val.getValue()).intValue();
      }
    }
    return -1;
  }

  /**
   * Returns {@code true} when the {@code TraitDef<T>} type argument carries an explicit
   * {@code @LIMIT} annotation (including {@code @LIMIT(LIMIT.NO_LIMIT)}). Returns {@code false}
   * when no {@code @LIMIT} annotation is present on the type argument at all.
   */
  public boolean hasLimitAnnotationOnTraitTypeArg(TypeElement traitElement) {
    TypeMirror typeArg = findTraitDefTypeArg(traitElement);
    if (typeArg == null) {
      return false;
    }
    for (AnnotationMirror mirror : typeArg.getAnnotationMirrors()) {
      if (mirror.getAnnotationType().toString().equals(LIMIT_ANNO)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the first type argument {@code T} of {@code TraitDef<T>} or {@code TraitDef<List<T>>}
   * as a raw {@link TypeMirror}, preserving any type-use annotations. Returns {@code null} if the
   * element does not extend {@code TraitDef}.
   */
  private TypeMirror findTraitDefTypeArg(TypeElement traitElement) {
    for (TypeMirror iface : traitElement.getInterfaces()) {
      if (!(iface instanceof DeclaredType dt)) {
        continue;
      }
      if (!(dt.asElement() instanceof TypeElement te)) {
        continue;
      }
      if (!te.getQualifiedName().contentEquals(TRAIT_DEF_CLASS)) {
        continue;
      }
      if (!dt.getTypeArguments().isEmpty()) {
        return dt.getTypeArguments().get(0);
      }
    }
    return null;
  }

  private List<OrderByClause> parseOrderBys(ExecutableElement method) {
    List<OrderByClause> result = new ArrayList<>();
    for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
      String type = mirror.getAnnotationType().toString();
      if (type.equals(ORDER_BY_ANNO)) {
        result.add(parseOrderBy(mirror));
      } else if (type.equals(ORDER_BYS_ANNO)) {
        for (AnnotationValue containerVal : mirror.getElementValues().values()) {
          if (!(containerVal.getValue() instanceof List<?> list)) {
            continue;
          }
          for (Object item : list) {
            if (item instanceof AnnotationValue av
                && av.getValue() instanceof AnnotationMirror am) {
              result.add(parseOrderBy(am));
            }
          }
        }
      }
    }
    return result;
  }

  private OrderByClause parseOrderBy(AnnotationMirror mirror) {
    String column = "";
    String order = "ASC";
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e :
        mirror.getElementValues().entrySet()) {
      String attr = e.getKey().getSimpleName().toString();
      if (attr.equals("by")) {
        column = (String) e.getValue().getValue();
      } else if (attr.equals("direction")) {
        VariableElement ve = (VariableElement) e.getValue().getValue();
        order = ve.getSimpleName().toString();
      }
    }
    return new OrderByClause(column, order);
  }

  private int parseLimit(ExecutableElement method) {
    for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
      if (!mirror.getAnnotationType().toString().equals(LIMIT_ANNO)) {
        continue;
      }
      for (AnnotationValue val : mirror.getElementValues().values()) {
        return ((Number) val.getValue()).intValue();
      }
    }
    return -1;
  }
}
