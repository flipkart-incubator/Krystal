package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.JoinRelation;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.JoinSqlResult;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.OrderByClause;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.ProjectionInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.ScalarColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereInput;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds SQL query strings from parsed {@link SqlQueryModel} records.
 *
 * <p>All methods are stateless and can be called as static utilities. The class cannot be
 * instantiated.
 *
 * <p>Three query patterns are supported:
 *
 * <ol>
 *   <li><b>Simple</b> — scalar-only projection, single or multiple rows ({@link
 *       #buildSimpleSql(ProjectionInfo, List)})
 *   <li><b>JOIN</b> — projection with at least one {@code List<AnotherProjection>} field ({@link
 *       #buildJoinSql(ProjectionInfo, List)})
 * </ol>
 */
public final class SqlQueryBuilder {

  private SqlQueryBuilder() {}

  // ─── Simple SELECT (no JOIN) ──────────────────────────────────────────────────

  /**
   * Builds a simple {@code SELECT ... FROM ... WHERE ...} statement for projections that contain
   * only scalar columns (no nested {@code List<Projection>} methods).
   *
   * <p>The SQL uses positional placeholders ({@code $1}, {@code $2}, …) compatible with PostgreSQL
   * and the Vert.x SQL client.
   */
  public static String buildSimpleSql(ProjectionInfo proj, List<WhereInput> whereInputs) {
    return buildSimpleSql(proj, whereInputs, List.of(), -1);
  }

  /**
   * Builds a simple {@code SELECT ... FROM ... WHERE ... ORDER BY ... LIMIT ...} statement,
   * appending trait-level {@code ORDER BY} and {@code LIMIT} clauses when provided.
   *
   * @param orderBys trait-level ORDER BY terms; empty list means no ORDER BY is appended
   * @param limit trait-level LIMIT value; {@code -1} means no LIMIT is appended
   */
  public static String buildSimpleSql(
      ProjectionInfo proj, List<WhereInput> whereInputs, List<OrderByClause> orderBys, int limit) {
    List<String> cols = new ArrayList<>();
    for (ScalarColumn col : proj.scalars()) {
      cols.add(colExpr(col.dbColumnName(), col.methodName()));
    }
    StringBuilder sql =
        new StringBuilder("SELECT ")
            .append(String.join(", ", cols))
            .append(" FROM ")
            .append(proj.tableName());
    appendWhere(sql, whereInputs, /* qualified= */ false);
    if (!orderBys.isEmpty()) {
      List<String> parts = new ArrayList<>();
      for (OrderByClause ob : orderBys) {
        parts.add(ob.columnName() + " " + ob.order());
      }
      sql.append(" ORDER BY ").append(String.join(", ", parts));
    }
    if (limit > 0) {
      sql.append(" LIMIT ").append(limit);
    }
    return sql.toString();
  }

  // ─── JOIN SELECT ─────────────────────────────────────────────────────────────

  /**
   * Builds a LEFT JOIN {@code SELECT} statement for projections that contain at least one {@code
   * List<AnotherProjection>} method.
   *
   * <p>All column aliases are prefixed with {@code tableName_} to prevent clashes across tables
   * (e.g. {@code users.name AS users_name}, {@code orders.orderId AS orders_orderId}).
   *
   * <p>The parent table's PK column is always included in the {@code SELECT} — either because it is
   * already a projected scalar, or as an extra sentinel column. This alias is returned in {@link
   * JoinSqlResult#parentPkAlias()} and is used by the code generator to emit per-row identity
   * validation in {@code mapResult}.
   */
  public static JoinSqlResult buildJoinSql(ProjectionInfo proj, List<WhereInput> whereInputs) {
    return buildJoinSql(proj, whereInputs, List.of(), -1, false);
  }

  /**
   * Builds a LEFT JOIN {@code SELECT} statement, optionally appending trait-level {@code ORDER BY}
   * and {@code LIMIT} for the parent table.
   *
   * <p>When {@code isListTrait} is {@code true} (the trait returns {@code List<T>}), every join
   * that carries a positive {@code @LIMIT} uses a {@code ROW_NUMBER() OVER (PARTITION BY fk …)}
   * subquery so the limit is enforced <em>per parent row</em>.
   *
   * <p>When {@code isListTrait} is {@code false} (single-row parent), top-level join limits are
   * expressed as a plain {@code LIMIT N} on the outer query (simpler SQL, same semantics because
   * there is only one parent). Nested joins always use {@code ROW_NUMBER} because their immediate
   * parent (the level-1 join table) can produce multiple rows.
   *
   * @param traitOrderBys trait-level ORDER BY terms applied to the parent table; empty = none
   * @param traitLimit trait-level LIMIT; {@code -1} = no trait-level limit
   * @param isListTrait {@code true} when the trait return type is {@code List<T>}
   */
  public static JoinSqlResult buildJoinSql(
      ProjectionInfo proj,
      List<WhereInput> whereInputs,
      List<OrderByClause> traitOrderBys,
      int traitLimit,
      boolean isListTrait) {
    String parentPkCol = proj.parentPkColumn();
    String parentPkAlias = parentPkCol != null ? proj.tableName() + "_" + parentPkCol : null;

    List<String> cols = new ArrayList<>();

    // Add parent PK sentinel only if it is not already projected as a scalar.
    boolean pkAlreadyProjected =
        parentPkCol != null
            && proj.scalars().stream().anyMatch(c -> c.methodName().equals(parentPkCol));

    if (parentPkAlias != null && !pkAlreadyProjected) {
      cols.add(proj.tableName() + "." + parentPkCol + " AS " + parentPkAlias);
    }

    for (ScalarColumn col : proj.scalars()) {
      cols.add(
          proj.tableName()
              + "."
              + col.dbColumnName()
              + " AS "
              + proj.tableName()
              + "_"
              + col.methodName());
    }

    for (JoinRelation join : proj.joins()) {
      // Add child PK sentinel if this join has nested joins and the PK is not already projected
      if (!join.nestedJoins().isEmpty() && join.childPkColumn() != null) {
        boolean childPkProjected =
            join.columns().stream().anyMatch(c -> c.methodName().equals(join.childPkColumn()));
        if (!childPkProjected) {
          cols.add(
              join.tableName()
                  + "."
                  + join.childPkColumn()
                  + " AS "
                  + join.tableName()
                  + "_"
                  + join.childPkColumn());
        }
      }
      for (ScalarColumn col : join.columns()) {
        cols.add(
            join.tableName()
                + "."
                + col.dbColumnName()
                + " AS "
                + join.tableName()
                + "_"
                + col.methodName());
      }
      // Nested join columns (level-2)
      for (JoinRelation nested : join.nestedJoins()) {
        for (ScalarColumn col : nested.columns()) {
          cols.add(
              nested.tableName()
                  + "."
                  + col.dbColumnName()
                  + " AS "
                  + nested.tableName()
                  + "_"
                  + col.methodName());
        }
      }
    }

    String selectCols = String.join(", ", cols);

    // ── Subquery approach when traitLimit > 0 ───────────────────────────────────
    // A positive LIMIT on the root list type means "return at most N parent rows". Appending
    // LIMIT N after a LEFT JOIN would limit total result rows (including child rows), not parent
    // rows. Instead, wrap the parent table in a subquery so the LIMIT and ORDER BY apply only to
    // parent rows before the join expands them.
    if (traitLimit > 0) {
      StringBuilder inner = new StringBuilder("SELECT * FROM ").append(proj.tableName());
      // Unqualified WHERE inside the subquery — only one table in scope.
      appendWhere(inner, whereInputs, /* qualified= */ false);
      if (!traitOrderBys.isEmpty()) {
        List<String> parts = new ArrayList<>();
        for (OrderByClause ob : traitOrderBys) {
          parts.add(ob.columnName() + " " + ob.order());
        }
        inner.append(" ORDER BY ").append(String.join(", ", parts));
      }
      inner.append(" LIMIT ").append(traitLimit);

      StringBuilder sql =
          new StringBuilder("SELECT ")
              .append(selectCols)
              .append(" FROM (")
              .append(inner)
              .append(") ")
              .append(proj.tableName());

      // In the subquery path the parent is always multi-row, so every top-level join uses
      // ROW_NUMBER. Nested joins always use ROW_NUMBER regardless.
      for (JoinRelation join : proj.joins()) {
        sql.append(" ").append(buildJoinClause(join, proj.tableName(), /* useRowNumber= */ true));
        for (JoinRelation nested : join.nestedJoins()) {
          sql.append(" ")
              .append(buildJoinClause(nested, join.tableName(), /* useRowNumber= */ true));
        }
      }

      // Outer ORDER BY: parent ordering columns (to preserve subquery order in the final result)
      // followed by join-level ordering columns (for consistent child-row ordering).
      List<String> outerOrderBys = new ArrayList<>();
      for (OrderByClause ob : traitOrderBys) {
        outerOrderBys.add(proj.tableName() + "." + ob.columnName() + " " + ob.order());
      }
      for (JoinRelation join : proj.joins()) {
        for (OrderByClause ob : join.orderBys()) {
          outerOrderBys.add(join.tableName() + "." + ob.columnName() + " " + ob.order());
        }
        for (JoinRelation nested : join.nestedJoins()) {
          for (OrderByClause ob : nested.orderBys()) {
            outerOrderBys.add(nested.tableName() + "." + ob.columnName() + " " + ob.order());
          }
        }
      }
      if (!outerOrderBys.isEmpty()) {
        sql.append(" ORDER BY ").append(String.join(", ", outerOrderBys));
      }

      return new JoinSqlResult(sql.toString(), parentPkAlias);
    }

    // ── Standard approach when traitLimit <= 0 (NO_LIMIT or absent) ─────────────
    // WHERE and ORDER BY (parent + child) go on the outer query.
    // Top-level join limits: use ROW_NUMBER when the parent is multi-row (list trait), otherwise
    // use a plain LIMIT N at the end of the outer query (single-parent equivalent).
    // Nested joins always use ROW_NUMBER because their immediate parent (level-1) is multi-row.
    StringBuilder sql =
        new StringBuilder("SELECT ").append(selectCols).append(" FROM ").append(proj.tableName());

    for (JoinRelation join : proj.joins()) {
      sql.append(" ")
          .append(buildJoinClause(join, proj.tableName(), /* useRowNumber= */ isListTrait));
      for (JoinRelation nested : join.nestedJoins()) {
        sql.append(" ").append(buildJoinClause(nested, join.tableName(), /* useRowNumber= */ true));
      }
    }

    appendWhere(sql, whereInputs, /* qualified= */ true);

    // ORDER BY: trait-level (parent table) first, then join-level (child tables).
    List<String> orderByParts = new ArrayList<>();
    for (OrderByClause ob : traitOrderBys) {
      orderByParts.add(proj.tableName() + "." + ob.columnName() + " " + ob.order());
    }
    for (JoinRelation join : proj.joins()) {
      for (OrderByClause ob : join.orderBys()) {
        orderByParts.add(join.tableName() + "." + ob.columnName() + " " + ob.order());
      }
      for (JoinRelation nested : join.nestedJoins()) {
        for (OrderByClause ob : nested.orderBys()) {
          orderByParts.add(nested.tableName() + "." + ob.columnName() + " " + ob.order());
        }
      }
    }
    if (!orderByParts.isEmpty()) {
      sql.append(" ORDER BY ").append(String.join(", ", orderByParts));
    }

    // For single-parent traits, a top-level join's @LIMIT(N) is expressed as LIMIT N on the outer
    // query — equivalent to ROW_NUMBER, but simpler because there is exactly one parent.
    if (!isListTrait) {
      for (JoinRelation join : proj.joins()) {
        if (join.limit() > 0) {
          sql.append(" LIMIT ").append(join.limit());
          break;
        }
      }
    }

    return new JoinSqlResult(sql.toString(), parentPkAlias);
  }

  // ─── Shared helpers ──────────────────────────────────────────────────────────

  /**
   * Appends {@code WHERE col = $N AND ...} clauses. When {@code qualified} is {@code true}, column
   * names are prefixed with the table name from {@link WhereInput#inTableName()}.
   */
  private static void appendWhere(
      StringBuilder sql, List<WhereInput> whereInputs, boolean qualified) {
    List<String> clauses = new ArrayList<>();
    int idx = 1;
    for (WhereInput wi : whereInputs) {
      for (String field : wi.fields()) {
        String colRef = qualified ? wi.inTableName() + "." + field : field;
        clauses.add(colRef + " = $" + idx++);
      }
    }
    if (!clauses.isEmpty()) {
      sql.append(" WHERE ").append(String.join(" AND ", clauses));
    }
  }

  /** Returns {@code "colName"} or {@code "colName AS alias"} as appropriate. */
  private static String colExpr(String dbCol, String alias) {
    return dbCol.equals(alias) ? dbCol : dbCol + " AS " + alias;
  }

  /**
   * Builds a JOIN clause for the given relation.
   *
   * <p>When {@code useRowNumber} is {@code true} and the join has a positive {@code LIMIT}, a
   * {@code ROW_NUMBER() OVER (PARTITION BY fk ORDER BY …)} subquery enforces the per-parent cap:
   *
   * <pre>{@code
   * LEFT JOIN (
   *   SELECT *, ROW_NUMBER() OVER (PARTITION BY <fk> ORDER BY <col> <dir>) AS _rn
   *   FROM <child>
   * ) <child> ON <parent>.<pk> = <child>.<fk> AND <child>._rn <= N
   * }</pre>
   *
   * <p>When {@code useRowNumber} is {@code false} (single-parent context), the limit is deferred to
   * the outer query as a plain {@code LIMIT N}; this method emits only the plain LEFT JOIN.
   *
   * <p>Without a positive limit a plain {@code LEFT JOIN <child> ON …} is always emitted.
   */
  private static String buildJoinClause(
      JoinRelation join, String parentTableName, boolean useRowNumber) {
    if (join.limit() > 0 && useRowNumber) {
      StringBuilder overClause = new StringBuilder("PARTITION BY ").append(join.childJoinColumn());
      if (!join.orderBys().isEmpty()) {
        List<String> parts = new ArrayList<>();
        for (OrderByClause ob : join.orderBys()) {
          parts.add(ob.columnName() + " " + ob.order());
        }
        overClause.append(" ORDER BY ").append(String.join(", ", parts));
      }
      return "LEFT JOIN (SELECT *, ROW_NUMBER() OVER ("
          + overClause
          + ") AS _rn FROM "
          + join.tableName()
          + ") "
          + join.tableName()
          + " ON "
          + parentTableName
          + "."
          + join.parentJoinColumn()
          + " = "
          + join.tableName()
          + "."
          + join.childJoinColumn()
          + " AND "
          + join.tableName()
          + "._rn <= "
          + join.limit();
    }
    return "LEFT JOIN "
        + join.tableName()
        + " ON "
        + parentTableName
        + "."
        + join.parentJoinColumn()
        + " = "
        + join.tableName()
        + "."
        + join.childJoinColumn();
  }
}
