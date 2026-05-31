package com.flipkart.krystal.vajram.ext.sql.codegen;

import static com.flipkart.krystal.codegen.common.models.Constants.EMPTY_CODE_BLOCK;

import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.JoinRelation;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.JoinSqlResult;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.ScalarColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.SelectionInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereInput;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.WhereLeaf;
import com.flipkart.krystal.vajram.ext.sql.lang.ORDER;
import com.squareup.javapoet.CodeBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
 *       #buildSimpleSql(SelectionInfo, List, List, int)})
 *   <li><b>JOIN</b> — projection with at least one {@code List<AnotherProjection>} field ({@link
 *       #buildJoinSql(SelectionInfo, List, List, int, boolean)})
 * </ol>
 */
public final class SqlQueryBuilder {

  private SqlQueryBuilder() {}

  // ─── Simple SELECT (no JOIN) ──────────────────────────────────────────────────
  /**
   * Builds a simple {@code SELECT ... FROM ... WHERE ... ORDER BY ... LIMIT ...} statement,
   * appending trait-level {@code ORDER BY} and {@code LIMIT} clauses when provided.
   *
   * @param orderBys trait-level ORDER BY terms; empty list means no ORDER BY is appended
   * @param limit trait-level LIMIT value; {@code -1} means no LIMIT is appended
   */
  public static CodeBlock buildSimpleSql(
      SelectionInfo proj, List<WhereInput> whereInputs, List<ORDER> orderBys, int limit) {
    List<String> cols = new ArrayList<>();
    for (ScalarColumn col : proj.scalars()) {
      cols.add(colExpr(col.dbColumnName(), col.methodName()));
    }
    CodeBlock.Builder sql = CodeBlock.builder();
    sql.add(
        "$S + $L",
        CodeBlock.of("SELECT $L FROM $L", String.join(", ", cols), proj.tableName()),
        getWhere(whereInputs, false));
    if (!orderBys.isEmpty()) {
      List<String> parts = new ArrayList<>();
      for (ORDER ob : orderBys) {
        parts.add(ob.by() + " " + ob.direction());
      }
      sql.add("+ $S", CodeBlock.of(" ORDER BY $L", String.join(", ", parts)));
    }
    if (limit > 0) {
      sql.add("+ $S", CodeBlock.of(" LIMIT $L", limit));
    }
    return sql.build();
  }

  // ─── JOIN SELECT ─────────────────────────────────────────────────────────────

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
      SelectionInfo selection,
      List<WhereInput> whereInputs,
      List<ORDER> traitOrderBys,
      int traitLimit,
      boolean isListTrait) {
    String parentPkCol = selection.parentPkColumn();
    String parentPkAlias = parentPkCol != null ? selection.tableName() + "_" + parentPkCol : null;

    List<String> cols = new ArrayList<>();

    // Add parent PK sentinel only if it is not already projected as a scalar.
    boolean pkAlreadyProjected =
        parentPkCol != null
            && selection.scalars().stream().anyMatch(c -> c.dbColumnName().equals(parentPkCol));

    if (parentPkAlias != null && !pkAlreadyProjected) {
      cols.add(selection.tableName() + "." + parentPkCol + " AS " + parentPkAlias);
    }

    for (ScalarColumn col : selection.scalars()) {
      cols.add(
          selection.tableName()
              + "."
              + col.dbColumnName()
              + " AS "
              + selection.tableName()
              + "_"
              + col.methodName());
    }

    List<JoinRelation> level1Joins = selection.joins();
    for (JoinRelation join : level1Joins) {
      // Add child PK sentinel if this join has nested joins and the PK is not already projected
      if (!join.nestedJoins().isEmpty() && join.childPkColumn() != null) {
        boolean childPkSelected =
            join.columns().stream().anyMatch(c -> c.dbColumnName().equals(join.childPkColumn()));
        if (!childPkSelected) {
          cols.add(
              join.methodName()
                  + "."
                  + join.childPkColumn()
                  + " AS "
                  + join.methodName()
                  + "_"
                  + join.childPkColumn());
        }
      }
      for (ScalarColumn col : join.columns()) {
        cols.add(
            join.methodName()
                + "."
                + col.dbColumnName()
                + " AS "
                + join.methodName()
                + "_"
                + col.methodName());
      }
      // Nested join columns (level-2)
      for (JoinRelation nested : join.nestedJoins()) {
        for (ScalarColumn col : nested.columns()) {
          cols.add(
              nested.methodName()
                  + "."
                  + col.dbColumnName()
                  + " AS "
                  + nested.methodName()
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
      CodeBlock.Builder inner =
          CodeBlock.builder()
              .add(
                  "$S + $L",
                  CodeBlock.of("SELECT * FROM $L", selection.tableName()),
                  getWhere(whereInputs, false));
      if (!traitOrderBys.isEmpty()) {
        List<String> parts = new ArrayList<>();
        for (ORDER ob : traitOrderBys) {
          parts.add(ob.by() + " " + ob.direction());
        }
        inner.add(CodeBlock.of(" + $S", CodeBlock.of(" ORDER BY $L", String.join(", ", parts))));
      }
      inner.add(CodeBlock.of(" + $S", CodeBlock.of(" LIMIT $L", traitLimit)));

      CodeBlock.Builder sql =
          CodeBlock.builder()
              .add(CodeBlock.of("$S", CodeBlock.of("SELECT $L FROM ", selectCols)))
              .add(" + $S + $L + $S ", "(", inner.build(), ") ")
              .add(" + $S", selection.tableName());

      // In the subquery path the parent is always multi-row, so every top-level join uses
      // ROW_NUMBER. Nested joins always use ROW_NUMBER regardless.
      for (JoinRelation join : level1Joins) {
        sql.add(" + $L", buildJoinClause(join, selection.tableName(), /* useRowNumber= */ true));
        for (JoinRelation nested : join.nestedJoins()) {
          sql.add(" + $L", buildJoinClause(nested, join.methodName(), /* useRowNumber= */ true));
        }
      }

      // Outer ORDER BY: parent ordering columns (to preserve subquery order in the final result)
      // followed by join-level ordering columns (for consistent child-row ordering).
      List<String> outerOrderBys = new ArrayList<>();
      for (ORDER ob : traitOrderBys) {
        outerOrderBys.add(selection.tableName() + "." + ob.by() + " " + ob.direction());
      }
      for (JoinRelation join : level1Joins) {
        for (ORDER ob : join.orderBys()) {
          outerOrderBys.add(join.methodName() + "." + ob.by() + " " + ob.direction());
        }
        for (JoinRelation nested : join.nestedJoins()) {
          for (ORDER ob : nested.orderBys()) {
            outerOrderBys.add(nested.methodName() + "." + ob.by() + " " + ob.direction());
          }
        }
      }
      if (!outerOrderBys.isEmpty()) {
        sql.add(" + $S", CodeBlock.of(" ORDER BY $L", String.join(", ", outerOrderBys)));
      }

      return new JoinSqlResult(sql.build(), parentPkAlias);
    }

    // ── Standard approach when traitLimit <= 0 (NO_LIMIT or absent) ─────────────
    // WHERE and ORDER BY (parent + child) go on the outer query.
    //
    // Top-level join limit strategy:
    //   - isListTrait=true  → always ROW_NUMBER (multiple parents, must partition per parent)
    //   - isListTrait=false, join has nested joins → ROW_NUMBER (a plain LIMIT N would truncate
    //     total result rows, not the number of level-1 children; each level-1 row expands into
    //     multiple rows due to level-2 joins, so outer LIMIT N would cut off mid-entity)
    //   - isListTrait=false, join has NO nested joins → plain LIMIT N at the outer query
    //     (Valid because there is exactly one parent, so no per-parent partitioning is needed,
    //     and there is only one level of child rows so LIMIT N == first N children).
    //
    // Nested (level-2) joins always use ROW_NUMBER because their immediate parent is multi-row.
    CodeBlock.Builder sql =
        CodeBlock.builder()
            .add("$S", CodeBlock.of("SELECT $L FROM $L", selectCols, selection.tableName()));

    for (JoinRelation join : level1Joins) {
      // ROW_NUMBER is required whenever there is more than one parent (list trait) OR there are
      // multiple level1 Joins (outer LIMIT would truncate sibling cross product rows) OR whenever
      // this join has nested joins (outer LIMIT would truncate grandchild rows).
      boolean useRowNumber = isListTrait || !join.nestedJoins().isEmpty() || level1Joins.size() > 1;
      sql.add(" + $L", buildJoinClause(join, selection.tableName(), useRowNumber));
      for (JoinRelation nested : join.nestedJoins()) {
        sql.add(" + $L", buildJoinClause(nested, join.methodName(), /* useRowNumber= */ true));
      }
    }

    sql.add(" + $L", getWhere(whereInputs, true));

    // ORDER BY: trait-level (parent table) first, then join-level (child tables).
    List<String> orderByParts = new ArrayList<>();
    for (ORDER ob : traitOrderBys) {
      orderByParts.add(selection.tableName() + "." + ob.by() + " " + ob.direction());
    }
    for (JoinRelation join : level1Joins) {
      for (ORDER ob : join.orderBys()) {
        orderByParts.add(join.methodName() + "." + ob.by() + " " + ob.direction());
      }
      for (JoinRelation nested : join.nestedJoins()) {
        for (ORDER ob : nested.orderBys()) {
          orderByParts.add(nested.methodName() + "." + ob.by() + " " + ob.direction());
        }
      }
    }
    if (!orderByParts.isEmpty()) {
      sql.add(" + $S", CodeBlock.of(" ORDER BY $L", String.join(", ", orderByParts)));
    }

    // For a single-parent trait whose has one level-1 join with NO nested joins, express the join's
    // @LIMIT(N) as a plain LIMIT N on the outer query. This is equivalent to ROW_NUMBER but
    // simpler, because there is exactly one parent with one join and no grandchild rows or sibling
    // joins to disturb the count.
    // When the level-1 join has nested joins, ROW_NUMBER is used instead (see above), so no
    // outer LIMIT is emitted here.
    if (!isListTrait) {
      if (level1Joins.size() == 1) {
        JoinRelation join = level1Joins.get(0);
        if (join.limit() > 0 && join.nestedJoins().isEmpty()) {
          sql.add(" + $S", CodeBlock.of(" LIMIT $L", join.limit()));
        }
      }
    }

    return new JoinSqlResult(sql.build(), parentPkAlias);
  }

  // ─── Shared helpers ──────────────────────────────────────────────────────────

  /**
   * Appends {@code WHERE ...} clauses. Supports simple AND predicates and OR-grouped predicates.
   * When {@code qualified} is {@code true}, column names are prefixed with the table name from
   * {@link WhereLeaf#inTableName()}.
   */
  private static CodeBlock getWhere(List<WhereInput> whereInputs, boolean qualified) {
    List<CodeBlock> topLevelClauses = new ArrayList<>();
    AtomicInteger idx = new AtomicInteger();
    for (WhereInput wi : whereInputs) {
      if (wi.isOr()) {
        List<CodeBlock> orBranches = new ArrayList<>();
        for (WhereLeaf leaf : wi.leaves()) {
          List<CodeBlock> andClauses = new ArrayList<>();
          for (WhereColumn col : leaf.columns()) {
            String colRef =
                qualified ? leaf.inTableName() + "." + col.dbColumnName() : col.dbColumnName();
            andClauses.add(col.operator().toSql(colRef, leaf, col, idx));
          }
          orBranches.add(
              CodeBlock.of("($L)", joinSqlCodeBlocks(andClauses, CodeBlock.of("$S", " AND "))));
        }
        topLevelClauses.add(
            CodeBlock.of("($L)", joinSqlCodeBlocks(orBranches, CodeBlock.of("$S", " OR "))));
      } else {
        for (WhereLeaf leaf : wi.leaves()) {
          for (WhereColumn col : leaf.columns()) {
            String colRef =
                qualified ? leaf.inTableName() + "." + col.dbColumnName() : col.dbColumnName();
            topLevelClauses.add(col.operator().toSql(colRef, leaf, col, idx));
          }
        }
      }
    }
    if (!topLevelClauses.isEmpty()) {
      return CodeBlock.of(
          "$S + $L", " WHERE ", joinSqlCodeBlocks(topLevelClauses, CodeBlock.of("$S", " AND ")));
    } else {
      return EMPTY_CODE_BLOCK;
    }
  }

  private static CodeBlock joinSqlCodeBlocks(List<CodeBlock> codeBlocks, CodeBlock separator) {
    List<CodeBlock> newList = new ArrayList<>();
    for (int i = 0; i < codeBlocks.size() - 1; i++) {
      newList.add(codeBlocks.get(i));
      newList.add(separator);
    }
    if (!codeBlocks.isEmpty()) {
      newList.add(codeBlocks.get(codeBlocks.size() - 1));
    }
    return CodeBlock.join(newList, " + ");
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
   * <p>When {@code useRowNumber} is {@code false} (single-parent, no nested joins), the limit is
   * deferred to the outer query as a plain {@code LIMIT N}; this method emits only the plain LEFT
   * JOIN. Callers must not pass {@code false} when the join has nested joins — outer LIMIT would
   * then truncate grandchild rows instead of bounding level-1 children.
   *
   * <p>Without a positive limit a plain {@code LEFT JOIN <child> ON …} is always emitted.
   */
  private static CodeBlock buildJoinClause(
      JoinRelation join, String parentTableName, boolean useRowNumber) {
    String joinClause;
    if (join.limit() > 0 && useRowNumber) {
      StringBuilder overClause = new StringBuilder("PARTITION BY ").append(join.childJoinColumn());
      if (!join.orderBys().isEmpty()) {
        List<String> parts = new ArrayList<>();
        for (ORDER ob : join.orderBys()) {
          parts.add(ob.by() + " " + ob.direction());
        }
        overClause.append(" ORDER BY ").append(String.join(", ", parts));
      }
      joinClause =
          " LEFT JOIN (SELECT *, ROW_NUMBER() OVER ("
              + overClause
              + ") AS _rn FROM "
              + join.tableName()
              + ") "
              + join.methodName()
              + " ON "
              + parentTableName
              + "."
              + join.parentJoinColumn()
              + " = "
              + join.methodName()
              + "."
              + join.childJoinColumn()
              + " AND "
              + join.methodName()
              + "._rn <= "
              + join.limit();
    } else {
      joinClause =
          " LEFT JOIN "
              + join.tableName()
              + " "
              + join.methodName()
              + " ON "
              + parentTableName
              + "."
              + join.parentJoinColumn()
              + " = "
              + join.methodName()
              + "."
              + join.childJoinColumn();
    }
    return CodeBlock.of("$S", joinClause);
  }
}
