package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.vajram.ext.sql.lang.ORDER;
import com.squareup.javapoet.CodeBlock;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Data model types shared between {@link SqlModelParser} and {@link SqlQueryBuilder} — and by
 * downstream codegen modules (e.g. {@code vajram-sql-vertx-codegen}) that need to produce code from
 * parsed SQL selection models.
 */
public final class SqlQueryModel {

  private SqlQueryModel() {}

  /**
   * A scalar (non-join) column in a selection.
   *
   * @param methodName the method name in the selection interface (used as the SQL alias)
   * @param dbColumnName the actual DB column name ({@code methodName} unless {@code @Column} is
   *     present)
   * @param javaType the Java type of the column value
   * @param isOptional {@code true} when the selection method returns {@code Optional<T>}
   */
  public record ScalarColumn(
      String methodName,
      String dbColumnName,
      TypeMirror javaType,
      boolean isOptional,
      @Nullable SerdeColumnInfo serdeInfo) {}

  /** A single {@code ORDER BY col [ASC|DESC]} term. */
  public record OrderByClause(String columnName, ORDER.Direction direction) {}

  /**
   * A LEFT JOIN relation discovered from a {@code List<AnotherSelection>} method in a parent
   * selection.
   *
   * @param methodName method name in the parent selection (used for grouping child rows)
   * @param childSelectionElement the child {@code @Selection} interface
   * @param tableElement the child {@code @Table} model interface
   * @param tableName SQL table name of the child table
   * @param parentJoinColumn PK column name in the parent table
   * @param childJoinColumn FK column name in the child table that references the parent
   * @param childPkColumn PK column name in the child table; used for deduplication when this join
   *     itself contains nested joins; {@code null} if the child table has no {@code @PrimaryKey}
   * @param columns scalar columns of the child selection (excludes {@code List<Selection>} methods)
   * @param orderBys ORDER BY clauses from {@code @ORDER_BY} annotations on the method
   * @param limit LIMIT value from {@code @LIMIT} annotation; {@code -1} if absent
   * @param nestedJoins LEFT JOIN relations discovered from {@code List<Selection>} methods in the
   *     child selection; supports multi-level parent → child → grandchild JOINs
   */
  public record JoinRelation(
      String methodName,
      TypeElement childSelectionElement,
      TypeElement tableElement,
      String tableName,
      String parentJoinColumn,
      String childJoinColumn,
      String childPkColumn,
      List<ScalarColumn> columns,
      List<ORDER> orderBys,
      int limit,
      List<JoinRelation> nestedJoins) {}

  /**
   * Parsed representation of a {@code @Selection(over = TableClass.class)} interface.
   *
   * @param selectionElement the selection interface
   * @param tableElement the referenced {@code @Table} interface
   * @param tableName SQL name of the parent table
   * @param parentPkColumn PK column name of the parent table ({@code null} if none found)
   * @param scalars scalar columns in this selection (excludes {@code List<Selection>} methods)
   * @param joins LEFT JOIN relations discovered from {@code List<Selection>} methods
   */
  public record SelectionInfo(
      TypeElement selectionElement,
      TypeElement tableElement,
      String tableName,
      String parentPkColumn,
      List<ScalarColumn> scalars,
      List<JoinRelation> joins) {}

  /**
   * Parsed result-type of a {@code TraitDef<T>} or {@code TraitDef<List<T>>} declaration.
   *
   * @param selectionElement the {@code @Selection}-annotated interface T
   * @param isList {@code true} when the trait returns {@code List<T>}
   */
  public record TraitResultType(TypeElement selectionElement, boolean isList) {}

  /**
   * A single column comparison within a WHERE predicate.
   *
   * @param accessorMethod Java method name used to get the value (e.g., {@code "idIs"})
   * @param dbColumnName actual DB column name (from {@code @Column} or the method name)
   * @param operator SQL comparison operator (e.g., {@code "="} from {@code @IsEqualTo})
   */
  public record WhereColumn(String accessorMethod, String dbColumnName, WhereOperator operator) {}

  /**
   * A leaf WHERE predicate: an AND group of column comparisons from a single {@link
   * com.flipkart.krystal.vajram.ext.sql.lang.SelectionPredicate}.
   *
   * @param javaAccessorPrefix full Java accessor prefix for parameter binding (e.g., {@code
   *     "where"} or {@code "where.orWithUserId()"})
   * @param inTableName SQL table name from {@code @WHERE(inTable = ...)}
   * @param columns columns in this predicate, AND-ed together
   */
  public record WhereLeaf(
      String javaAccessorPrefix, String inTableName, List<WhereColumn> columns) {}

  /**
   * Complete WHERE specification for one {@code _Inputs} method.
   *
   * <p>For a simple {@link com.flipkart.krystal.vajram.ext.sql.lang.SelectionPredicate} input,
   * {@code isOr} is {@code false} and {@code leaves} has a single entry. For an {@link
   * com.flipkart.krystal.vajram.ext.sql.lang.operators.logical.SqlOrPredicate} input, {@code isOr}
   * is {@code true} and {@code leaves} contains one entry per OR branch.
   *
   * @param paramName method name in the trait's {@code _Inputs}
   * @param isOr {@code true} when the input type extends {@code SqlOrPredicate}
   * @param leaves leaf predicates — OR-joined when {@code isOr}, otherwise a single leaf
   */
  public record WhereInput(String paramName, boolean isOr, List<WhereLeaf> leaves) {}

  /**
   * Result of {@link SqlQueryBuilder#buildJoinSql}.
   *
   * @param sql the generated SQL string
   * @param parentPkAlias the alias of the parent PK column in the SELECT (e.g. {@code "users_id"});
   *     used by the code generator to emit per-row parent identity validation
   */
  public record JoinSqlResult(CodeBlock sql, String parentPkAlias) {}

  /**
   * Serialization/deserialization info for a column annotated with {@code @SerdeWith}.
   *
   * @param protocolTypeElement the {@code SerdeProtocol} implementation (e.g. {@code Json})
   * @param columnType
   */
  public record SerdeColumnInfo(TypeElement protocolTypeElement, TypeMirror columnType) {}
}
