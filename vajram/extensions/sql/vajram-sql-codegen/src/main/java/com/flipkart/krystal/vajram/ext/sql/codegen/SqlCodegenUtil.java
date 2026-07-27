package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.model.array.FloatArray;
import com.flipkart.krystal.vajram.ext.sql.lang.SqlDialect;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.type.TypeMirror;
import lombok.Getter;

public class SqlCodegenUtil {

  @Getter private final CodeGenUtility util;
  private final SqlDialect sqlDialect;

  public SqlCodegenUtil(CodeGenUtility util, SqlDialect sqlDialect) {
    this.util = util;
    this.sqlDialect = sqlDialect;
  }

  public boolean isFloatArray(TypeMirror type) {
    if (!(util.processingEnv().getTypeUtils().asElement(type)
        instanceof QualifiedNameable qualifiedNameable)) {
      return false;
    }
    return qualifiedNameable.getQualifiedName().contentEquals(FloatArray.class.getCanonicalName());
  }

  public void requirePostgresFloatArray(String columnName) {
    if (sqlDialect != SqlDialect.POSTGRESQL_18) {
      throw util.errorAndThrow(
          "[vajram-sql] FloatArray column '"
              + columnName
              + "' requires PostgreSQL REAL[] support. Dialect "
              + sqlDialect
              + " has no native FloatArray mapping.");
    }
  }
}
