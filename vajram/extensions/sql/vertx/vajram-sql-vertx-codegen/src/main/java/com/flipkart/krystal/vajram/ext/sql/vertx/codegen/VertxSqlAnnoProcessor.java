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
import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("com.flipkart.krystal.vajram.ext.sql.lang.SQL")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions({CODEGEN_PHASE_KEY, MODULE_ROOT_PATH_KEY})
@RunOnlyWhenCodegenPhaseIs(MODELS)
public class VertxSqlAnnoProcessor extends AbstractKrystalAnnoProcessor {

  @Override
  protected void processImpl(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      VajramCodeGenUtility vajramUtil = new VajramCodeGenUtility(codeGenUtil());
      SqlModelParser parser = new SqlModelParser(vajramUtil, paramIndex -> "$" + paramIndex);
      parser.validateTableAndWhereElements(roundEnv);
      InsertModelParser insertParser = new InsertModelParser(vajramUtil, parser);
      List<TypeElement> sqlTraits = getSqlTraits(roundEnv);
      Iterator<TypeElement> iterator = sqlTraits.iterator();
      while (iterator.hasNext()) {
        TypeElement vajramElement = iterator.next();
        try {
          VajramInfo vajramInfo = vajramUtil.computeVajramInfo(vajramElement);
          if (vajramElement.getAnnotation(INSERT.class) != null) {
            new SqlInsertVajramGen(vajramUtil, vajramInfo, insertParser).generate();
          } else {
            new SqlSelectVajramGen(vajramUtil, vajramInfo, parser).generate();
          }
          iterator.remove();
        } catch (Exception e) {
          if (e instanceof CodeGenShortCircuitException) {
            vajramUtil
                .codegenUtil()
                .note("[Vajram Codegen Exception]" + e.getMessage(), vajramElement);
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
