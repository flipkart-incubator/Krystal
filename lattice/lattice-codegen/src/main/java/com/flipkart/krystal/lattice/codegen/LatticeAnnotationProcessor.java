package com.flipkart.krystal.lattice.codegen;

import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static java.util.Objects.requireNonNull;

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

  @Override
  public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    String phaseString = processingEnv.getOptions().get(CODEGEN_PHASE_KEY);
    VajramCodeGenUtility util =
        new VajramCodeGenUtility(processingEnv, this.getClass(), phaseString);
    CodegenPhase codegenPhase;
    try {
      if (phaseString == null) {
        util.codegenUtil().note("Skipping %s since codegen phase is null");
        return false;
      } else {
        codegenPhase = CodegenPhase.valueOf(phaseString);
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
    TypeElement latticeAppTypeElement = latticeApps.get(0);

    CharSequence message =
        "Lattice detected by %s: %s"
            .formatted(getClass().getSimpleName(), latticeAppTypeElement.getQualifiedName());
    util.codegenUtil().note(message);

    LatticeCodegenContext codegenContext =
        new LatticeCodegenContext(
            latticeAppTypeElement,
            requireNonNull(latticeAppTypeElement.getAnnotation(LatticeApp.class)),
            codegenPhase,
            util,
            roundEnv);
    for (LatticeCodeGeneratorProvider customCodeGeneratorProvider :
        ServiceLoader.load(LatticeCodeGeneratorProvider.class, this.getClass().getClassLoader())) {
      try {
        customCodeGeneratorProvider.create(codegenContext).generate();
      } catch (Exception e) {
        util.error(e.toString(), latticeAppTypeElement);
        continue;
      }
    }

    return false;
  }
}
