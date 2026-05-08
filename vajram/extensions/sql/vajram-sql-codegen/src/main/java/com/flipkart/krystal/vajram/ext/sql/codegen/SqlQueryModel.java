package com.flipkart.krystal.vajram.ext.sql.codegen;

import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Data model types shared between {@link SqlModelParser} and {@link SqlQueryBuilder} — and by
 * downstream codegen modules (e.g. {@code vajram-sql-vertx-codegen}) that need to produce code from
 * parsed SQL projection models.
 */
public final class SqlQueryModel {

  private SqlQueryModel() {}

  /**
   * A scalar (non-join) column in a projection.
   *
   * @param methodName the method name in the projection interface (used as the SQL alias)
   * @param dbColumnName the actual DB column name ({@code methodName} unless {@code @Column} is
   *     present)
   * @param javaType the Java type of the column value
   * @param isOptional {@code true} when the projection method returns {@code Optional<T>}
   */
  public record ScalarColumn(
      String methodName, String dbColumnName, TypeMirror javaType, boolean isOptional) {}

  /** A single {@code ORDER BY col [ASC|DESC]} term. */
  public record OrderByClause(String columnName, String order) {}

  /**
   * A LEFT JOIN relation discovered from a {@code List<AnotherProjection>} method in a parent
   * projection.
   *
   * @param methodName method name in the parent projection (used for grouping child rows)
   * @param projectionElement the child {@code @Projection} interface
   * @param tableElement the child {@code @Table} model interface
   * @param tableName SQL table name of the child table
   * @param parentJoinColumn PK column name in the parent table
   * @param childJoinColumn FK column name in the child table that references the parent
   * @param childPkColumn PK column name in the child table; used for deduplication when this join
   *     itself contains nested joins; {@code null} if the child table has no {@code @PrimaryKey}
   * @param columns scalar columns of the child projection (excludes {@code List<Projection>}
   *     methods)
   * @param orderBys ORDER BY clauses from {@code @ORDER_BY} annotations on the method
   * @param limit LIMIT value from {@code @LIMIT} annotation; {@code -1} if absent
   * @param nestedJoins LEFT JOIN relations discovered from {@code List<Projection>} methods in the
   *     child projection; supports multi-level parent → child → grandchild JOINs
   */
  public record JoinRelation(
      String methodName,
      TypeElement projectionElement,
      TypeElement tableElement,
      String tableName,
      String parentJoinColumn,
      String childJoinColumn,
      String childPkColumn,
      List<ScalarColumn> columns,
      List<OrderByClause> orderBys,
      int limit,
      List<JoinRelation> nestedJoins) {}

  /**
   * Parsed representation of a {@code @Projection(over = TableClass.class)} interface.
   *
   * @param projectionElement the projection interface
   * @param tableElement the referenced {@code @Table} interface
   * @param tableName SQL name of the parent table
   * @param parentPkColumn PK column name of the parent table ({@code null} if none found)
   * @param scalars scalar columns in this projection (excludes {@code List<Projection>} methods)
   * @param joins LEFT JOIN relations discovered from {@code List<Projection>} methods
   */
  public record ProjectionInfo(
      TypeElement projectionElement,
      TypeElement tableElement,
      String tableName,
      String parentPkColumn,
      List<ScalarColumn> scalars,
      List<JoinRelation> joins) {}

  /**
   * Parsed result type of a {@code TraitDef<T>} or {@code TraitDef<List<T>>} declaration.
   *
   * @param projectionElement the {@code @Projection}-annotated interface T
   * @param isList {@code true} when the trait returns {@code List<T>}
   */
  public record TraitResultType(TypeElement projectionElement, boolean isList) {}

  /**
   * A single {@code @WHERE}-annotated input parameter from a trait's {@code _Inputs} interface.
   *
   * @param paramName method name in the trait's {@code _Inputs}
   * @param typeElement the {@code @WHERE} interface type
   * @param inTable the table referenced by {@code @WHERE(inTable = ...)}
   * @param inTableName SQL name of {@code inTable}
   * @param fields method names of the {@code @WHERE} interface (one per WHERE predicate)
   */
  public record WhereInput(
      String paramName,
      TypeElement typeElement,
      TypeElement inTable,
      String inTableName,
      List<String> fields) {}

  /**
   * Result of {@link SqlQueryBuilder#buildJoinSql}.
   *
   * @param sql the generated SQL string
   * @param parentPkAlias the alias of the parent PK column in the SELECT (e.g. {@code "users_id"});
   *     used by the code generator to emit per-row parent identity validation
   */
  public record JoinSqlResult(String sql, String parentPkAlias) {}
}
