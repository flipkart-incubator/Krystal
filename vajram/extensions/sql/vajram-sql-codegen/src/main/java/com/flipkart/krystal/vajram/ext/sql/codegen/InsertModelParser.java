package com.flipkart.krystal.vajram.ext.sql.codegen;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.vajram.codegen.common.models.DefaultFacetModel;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.InsertQueryModel.InsertColumn;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlQueryModel.SerdeColumnInfo;
import com.flipkart.krystal.vajram.ext.sql.model.IncomingForeignKey;
import com.flipkart.krystal.vajram.ext.sql.model.Table;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Parses {@code @SQL @INSERT @Trait} interfaces and builds {@link InsertQueryModel} records
 * suitable for INSERT SQL generation.
 *
 * <p>This class is framework-agnostic — it does not reference Vert.x or any specific SQL driver.
 */
public final class InsertModelParser {

  private final CodeGenUtility util;
  private final SqlModelParser sqlParser;

  public InsertModelParser(VajramCodeGenUtility vajramUtil, SqlModelParser sqlParser) {
    this.util = vajramUtil.codegenUtil();
    this.sqlParser = sqlParser;
  }

  /**
   * Parses the INSERT trait's single input, discovering the table type and column information.
   *
   * <p>Validates:
   *
   * <ul>
   *   <li>The trait must have exactly one input
   *   <li>That input must be a {@code @Table}-annotated type or {@code List<@Table>}
   * </ul>
   */
  public @Nullable InsertQueryModel parseInsertInputs(VajramInfo vajramInfo) {
    List<DefaultFacetModel> inputs =
        vajramInfo.givenFacets().stream().filter(fd -> fd.facetType() == FacetType.INPUT).toList();

    if (inputs.size() != 1) {
      util.error(
          "[vajram-sql] INSERT trait must have exactly one input whose type is "
              + "a @Table-annotated model or List<@Table>. Found "
              + inputs.size()
              + " inputs.",
          vajramInfo.definitionElement());
      return null;
    }

    DefaultFacetModel input = inputs.get(0);
    TypeMirror inputType = input.dataType().typeMirror(util.processingEnv());
    String paramName = input.name();

    // Try List<@Table> first, then single @Table
    TypeElement tableElement = getListTableElement(inputType);
    boolean isList = tableElement != null;
    if (tableElement == null) {
      tableElement = getTableElement(inputType);
    }

    if (tableElement == null) {
      util.error(
          "[vajram-sql] INSERT trait input '"
              + paramName
              + "' must be a @Table-annotated type or List<@Table>. Found: "
              + inputType,
          vajramInfo.definitionElement());
      return null;
    }

    String tableName = sqlParser.getTableName(tableElement);

    // Collect column names from the table (excluding @IncomingForeignKey)
    List<InsertColumn> columns = new ArrayList<>();
    for (ExecutableElement method : util.extractAndValidateModelMethods(tableElement)) {
      if (method.getAnnotation(IncomingForeignKey.class) != null) {
        continue; // not a real column
      }
      String dbColumnName = sqlParser.resolveColumnName(method);
      String accessorName = method.getSimpleName().toString();

      TypeMirror returnType = method.getReturnType();
      boolean isOptional = util.isOptional(returnType);
      TypeMirror actualType = isOptional ? util.getOptionalInnerType(returnType) : returnType;

      SerdeColumnInfo serdeInfo = sqlParser.resolveSerdeInfo(method, actualType);

      columns.add(new InsertColumn(dbColumnName, actualType, accessorName, isOptional, serdeInfo));
    }

    if (columns.isEmpty()) {
      util.error("Table '%s' has no columns. To INSERT in a table, at least one column is needed");
    }

    return new InsertQueryModel(tableElement, tableName, columns, paramName, isList);
  }

  /** Returns the table TypeElement if the given type is a {@code @Table}-annotated type. */
  private @Nullable TypeElement getTableElement(TypeMirror type) {
    if (!(type instanceof DeclaredType dt)) {
      return null;
    }
    if (!(dt.asElement() instanceof TypeElement te)) {
      return null;
    }
    if (te.getAnnotation(Table.class) != null) {
      return te;
    }
    return null;
  }

  /** Returns the table TypeElement if the given type is {@code List<@Table>}. */
  private @Nullable TypeElement getListTableElement(TypeMirror type) {
    if (!(type instanceof DeclaredType dt)) {
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
    return getTableElement(inner);
  }
}
