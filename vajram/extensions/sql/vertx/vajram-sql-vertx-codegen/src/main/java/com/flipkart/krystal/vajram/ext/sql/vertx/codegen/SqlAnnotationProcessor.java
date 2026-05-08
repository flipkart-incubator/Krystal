package com.flipkart.krystal.vajram.ext.sql.vertx.codegen;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.flipkart.krystal.codegen.common.models.Constants.MODULE_ROOT_PATH_KEY;
import static com.google.common.base.Throwables.getStackTraceAsString;

import com.flipkart.krystal.codegen.common.models.AbstractKrystalAnnoProcessor;
import com.flipkart.krystal.codegen.common.models.RunOnlyWhenCodegenPhaseIs;
import com.google.auto.service.AutoService;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("com.flipkart.krystal.vajram.ext.sql.statement.SQL")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions({CODEGEN_PHASE_KEY, MODULE_ROOT_PATH_KEY})
@RunOnlyWhenCodegenPhaseIs(MODELS)
public class SqlAnnotationProcessor extends AbstractKrystalAnnoProcessor {

  private boolean generated;

  @Override
  protected boolean processImpl(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (generated) {
      return false;
    }
    try {
      new SqlTraitVajramGen(codeGenUtil()).generate(roundEnv);
    } catch (Exception e) {
      codeGenUtil().error("[SQL TraitVajramGen] " + getStackTraceAsString(e));
    }
    generated = true;
    return false;
  }
}
