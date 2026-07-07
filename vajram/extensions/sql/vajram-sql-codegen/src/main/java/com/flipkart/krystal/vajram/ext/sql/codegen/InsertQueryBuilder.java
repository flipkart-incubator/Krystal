package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.ext.sql.codegen.InsertQueryModel.TableColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.syntax.SqlSyntax;
import java.util.List;

/**
 * Builds SQL INSERT statements from parsed {@link InsertQueryModel} records.
 *
 * <p>The generated INSERT uses positional placeholders ({@code $1}, {@code $2}, …) for parameter
 * binding, matching the convention used by the Vert.x SQL client.
 */
public final class InsertQueryBuilder {

  private final CodeGenUtility util;

  public InsertQueryBuilder(VajramCodeGenUtility vajramUtil) {
    this.util = vajramUtil.codegenUtil();
  }

  /**
   * Builds a parameterized INSERT statement, optionally appending a {@code RETURNING} clause.
   *
   * @param model the parsed INSERT model containing table name and column definitions
   * @param config driver-specific config for placeholder format
   * @param syntax the SQL syntax provider for the target database dialect; if non-null and {@code
   *     returningColumnNames} is non-empty, a RETURNING clause is appended
   * @param returningColumnNames the column names to include in the RETURNING clause; may be empty
   * @return the SQL INSERT string with positional placeholders and optional RETURNING clause
   */
  public String buildInsertSql(
      InsertQueryModel model,
      SqlDriverConfig config,
      SqlSyntax syntax,
      List<String> returningColumnNames) {
    List<TableColumn> columns = model.columns();

    StringBuilder sb = new StringBuilder();
    sb.append("INSERT INTO ").append(model.tableName()).append(" (");

    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(columns.get(i).columnName());
    }

    sb.append(") VALUES (");

    for (int i = 0; i < columns.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(config.getParamPlaceholder(i + 1));
    }

    sb.append(")");

    if (syntax != null && !returningColumnNames.isEmpty()) {
      try {
        sb.append(syntax.returningClause(returningColumnNames));
      } catch (Exception e) {
        util.error(e, model.tableElement());
      }
    }

    return sb.toString();
  }
}
