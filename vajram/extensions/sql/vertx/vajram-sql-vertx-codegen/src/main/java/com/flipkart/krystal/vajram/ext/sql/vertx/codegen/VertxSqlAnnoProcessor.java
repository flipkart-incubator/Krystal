package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.flipkart.krystal.codegen.common.models.Constants.MODULE_ROOT_PATH_KEY;
import static com.google.common.base.Throwables.getStackTraceAsString;

import com.flipkart.krystal.codegen.common.models.AbstractKrystalAnnoProcessor;
import com.flipkart.krystal.codegen.common.models.CodeGenShortCircuitException;
import com.flipkart.krystal.codegen.common.models.RunOnlyWhenCodegenPhaseIs;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.ext.sql.codegen.InsertModelParser;
import com.flipkart.krystal.vajram.ext.sql.codegen.SqlModelParser;
import com.flipkart.krystal.vajram.ext.sql.lang.INSERT;
import com.flipkart.krystal.vajram.ext.sql.lang.SQL;
import com.flipkart.krystal.vajram.ext.sql.lang.SqlDialect;
import com.flipkart.krystal.vajram.ext.sql.model.Table;
import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes({
  "com.flipkart.krystal.vajram.ext.sql.lang.SQL",
  "com.flipkart.krystal.vajram.ext.sql.model.Table"
})
@AutoService(Processor.class)
@SupportedOptions({CODEGEN_PHASE_KEY, MODULE_ROOT_PATH_KEY})
@RunOnlyWhenCodegenPhaseIs(MODELS)
public class VertxSqlAnnoProcessor extends AbstractKrystalAnnoProcessor {

  /** Qualified names of SQL traits deferred because their response type was not yet available. */
  private final Set<String> deferredTraitNames = new LinkedHashSet<>();

  @Override
  protected void processImpl(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      VajramCodeGenUtility vajramUtil = new VajramCodeGenUtility(codeGenUtil());
      SqlModelParser parser = new SqlModelParser(vajramUtil, paramIndex -> "$" + paramIndex);
      parser.validateTableAndWhereElements(roundEnv);

      for (Element element : roundEnv.getElementsAnnotatedWith(Table.class)) {
        if (element instanceof TypeElement tableElement) {
          new SqlTableModelGen(new SqlTableGenContext(codeGenUtil(), tableElement)).generate();
        }
      }

      InsertModelParser insertParser = new InsertModelParser(vajramUtil, parser);

      // Collect new SQL traits from this round + previously deferred traits.
      List<TypeElement> sqlTraits = getSqlTraits(roundEnv);
      for (String fqn : new ArrayList<>(deferredTraitNames)) {
        TypeElement te = processingEnv.getElementUtils().getTypeElement(fqn);
        if (te != null) {
          sqlTraits.add(te);
        }
      }
      deferredTraitNames.clear();

      for (TypeElement vajramElement : sqlTraits) {
        try {
          VajramInfo vajramInfo = vajramUtil.computeVajramInfo(vajramElement);
          // Check if the response type is still an error type (not yet generated).
          if (vajramElement.getAnnotation(INSERT.class) != null) {
            SQL sqlAnno = vajramElement.getAnnotation(SQL.class);
            SqlDialect dialect = sqlAnno != null ? sqlAnno.dialect() : SqlDialect.SQL_2023;
            new SqlInsertVajramGen(vajramUtil, vajramInfo, insertParser, parser, dialect)
                .generate();
          } else {
            SQL sqlAnnoSelect = vajramElement.getAnnotation(SQL.class);
            SqlDialect selectDialect =
                sqlAnnoSelect != null ? sqlAnnoSelect.dialect() : SqlDialect.SQL_2023;
            new SqlSelectVajramGen(vajramUtil, vajramInfo, parser, selectDialect).generate();
          }
        } catch (Exception e) {
          if (e instanceof CodeGenShortCircuitException) {
            // Response type not yet available — defer to the next AP round.
            deferredTraitNames.add(vajramElement.getQualifiedName().toString());
            vajramUtil
                .codegenUtil()
                .note(
                    "[Vajram Codegen] Deferring "
                        + vajramElement.getSimpleName()
                        + ": "
                        + e.getMessage(),
                    vajramElement);
          } else {
            vajramUtil
                .codegenUtil()
                .error("[Vajram Codegen Exception] " + getStackTraceAsString(e), vajramElement);
          }
        }
      }
    } catch (Exception e) {
      codeGenUtil().error("[SQL TraitVajramGen] " + getStackTraceAsString(e));
    }
  }

  private List<TypeElement> getSqlTraits(RoundEnvironment roundEnv) {
    List<TypeElement> sqlTraits = new ArrayList<>();
    for (Element element : roundEnv.getElementsAnnotatedWith(SQL.class)) {
      if (element.getAnnotation(Trait.class) == null) {
        continue;
      }
      if (!(element instanceof TypeElement typeElement)) {
        continue;
      }
      sqlTraits.add(typeElement);
    }
    return sqlTraits;
  }
}
