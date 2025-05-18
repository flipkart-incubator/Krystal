package com.flipkart.krystal.lattice.codegen;

import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;

import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.lattice.core.LatticeApp;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.google.auto.service.AutoService;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("com.flipkart.krystal.lattice.core.LatticeApp")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions(CODEGEN_PHASE_KEY)
public class LatticeAnnotationProcessor extends AbstractProcessor {

  private final CodegenPhase codegenPhase = CodegenPhase.MODELS;

  @Override
  public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    VajramCodeGenUtility util = new VajramCodeGenUtility(processingEnv, this.getClass());
    String phaseString = processingEnv.getOptions().get(CODEGEN_PHASE_KEY);
    try {
      if (phaseString == null || !codegenPhase.equals(CodegenPhase.valueOf(phaseString))) {
        util.note(
            "Skipping %s since codegen phase is '%s'. This class only supports '%s'"
                .formatted(getClass().getSimpleName(), String.valueOf(phaseString), codegenPhase));
        return false;
      }
    } catch (IllegalArgumentException e) {
      util.error(
          ("%s could not parse phase string '%s'. "
                  + "Exactly one of %s must be passed as value to the java compiler "
                  + "via the annotation processor argument '-A%s='")
              .formatted(
                  getClass().getSimpleName(),
                  String.valueOf(phaseString),
                  Arrays.toString(CodegenPhase.values()),
                  CODEGEN_PHASE_KEY));
      return false;
    }
    List<TypeElement> latticeApps =
        roundEnv.getElementsAnnotatedWith(LatticeApp.class).stream()
            .filter(element -> element.getKind() == ElementKind.CLASS)
            .map(executableElement -> (TypeElement) executableElement)
            .toList();
    if (latticeApps.isEmpty()) {
      return false;
    } else if (latticeApps.size() > 1) {
      throw util.errorAndThrow("More than one lattice apps cannot be present in the same module");
    }
    TypeElement latticeApp = latticeApps.get(0);

    util.note(
        "Lattice detected by %s: %s"
            .formatted(getClass().getSimpleName(), latticeApp.getQualifiedName()));

    LatticeCodegenContext codegenContext =
        new LatticeCodegenContext(codegenPhase, latticeApp, util);
    for (LatticeCodeGeneratorProvider customCodeGeneratorProvider :
        ServiceLoader.load(LatticeCodeGeneratorProvider.class, this.getClass().getClassLoader())) {
      try {
        customCodeGeneratorProvider.create(codegenContext).generate();
      } catch (Exception e) {
        util.error(e.toString(), latticeApp);
        continue;
      }
    }

    return false;
  }
}
