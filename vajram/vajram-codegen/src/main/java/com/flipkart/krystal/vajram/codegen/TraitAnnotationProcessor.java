package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.codegen.CodegenPhase.MODELS;
import static com.flipkart.krystal.vajram.codegen.Constants.COGENGEN_PHASE_KEY;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import com.google.auto.service.AutoService;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("com.flipkart.krystal.vajram.VajramTrait")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions(COGENGEN_PHASE_KEY)
public class TraitAnnotationProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Utils util = new Utils(processingEnv, this.getClass());
    String phaseString = processingEnv.getOptions().get(COGENGEN_PHASE_KEY);
    try {
      if (phaseString == null || !MODELS.equals(CodegenPhase.valueOf(phaseString))) {
        util.note(
            "Skipping VajramTraitAnnotationProcessor since codegen phase is %s"
                .formatted(String.valueOf(phaseString)));
        return false;
      }
    } catch (IllegalArgumentException e) {
      util.error(
          ("VajramTraitAnnotationProcessor could not parse phase string '%s'. "
                  + "Exactly one of %s must be passed as value to java compiler "
                  + "via the annotation processor argument '-A%s='")
              .formatted(
                  String.valueOf(phaseString),
                  Arrays.toString(CodegenPhase.values()),
                  COGENGEN_PHASE_KEY),
          null);
    }
    List<TypeElement> vajramTraits = util.getVajramTraitClasses(roundEnv);
    util.note(
        "Vajram Traits received by VajramTraitAnnotationProcessor: %s"
            .formatted(
                vajramTraits.stream()
                    .map(Objects::toString)
                    .collect(
                        joining(lineSeparator(), '[' + lineSeparator(), lineSeparator() + ']'))));
    for (TypeElement vajramClass : vajramTraits) {
      VajramInfo vajramInfo = util.computeVajramInfo(vajramClass);

      VajramCodeGenerator vajramCodeGenerator = util.createCodeGenerator(vajramInfo);

      vajramCodeGenerator.vajramRequest();
    }
    return false;
  }
}
