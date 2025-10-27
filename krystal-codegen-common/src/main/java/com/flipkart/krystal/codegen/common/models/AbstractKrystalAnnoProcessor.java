package com.flipkart.krystal.codegen.common.models;

import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static lombok.AccessLevel.PROTECTED;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public abstract class AbstractKrystalAnnoProcessor extends AbstractProcessor {

  private final @Nullable CodegenPhase expectedPhase;

  @Getter(PROTECTED)
  private CodeGenUtility codeGenUtil;

  @Getter(PROTECTED)
  private @Nullable CodegenPhase codegenPhase;

  public AbstractKrystalAnnoProcessor() {
    RunOnlyWhenCodegenPhaseIs runOnlyWhenCodegenPhaseIs =
        this.getClass().getAnnotation(RunOnlyWhenCodegenPhaseIs.class);
    if (runOnlyWhenCodegenPhaseIs == null) {
      this.expectedPhase = null;
    } else {
      this.expectedPhase = runOnlyWhenCodegenPhaseIs.value();
    }
    log.info(
        "Initializing {} with codegen phase {}", this.getClass().getSimpleName(), expectedPhase);
  }

  @Override
  public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    validateAndInit();
    if (expectedPhase == null || Objects.equals(expectedPhase, codegenPhase)) {
      return processImpl(annotations, roundEnv);
    }
    codeGenUtil.note(
        "Skipping %s since codegen phase is '%s'. This class only supports '%s'"
            .formatted(getClass().getSimpleName(), codegenPhase, expectedPhase));
    return false;
  }

  private void validateAndInit() {
    validateCodegenPhase();
  }

  private void validateCodegenPhase() {
    String currentPhaseString = processingEnv.getOptions().get(CODEGEN_PHASE_KEY);
    if (currentPhaseString == null) {
      return;
    }
    try {
      this.codegenPhase = CodegenPhase.valueOf(currentPhaseString);
    } catch (IllegalArgumentException e) {
      processingEnv
          .getMessager()
          .printMessage(
              Kind.ERROR,
              ("%s could not parse phase string '%s'. "
                      + "Exactly one of %s must be passed as value to the java compiler "
                      + "via the annotation processor argument '-A%s='")
                  .formatted(
                      getClass().getSimpleName(),
                      String.valueOf(currentPhaseString),
                      Arrays.toString(CodegenPhase.values()),
                      CODEGEN_PHASE_KEY));
    }
    this.codeGenUtil = new CodeGenUtility(processingEnv, this.getClass(), codegenPhase);
  }

  protected boolean processImpl(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    return false;
  }
}
