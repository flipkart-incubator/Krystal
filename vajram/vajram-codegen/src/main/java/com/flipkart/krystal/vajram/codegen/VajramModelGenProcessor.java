package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.codegen.CodegenPhase.MODELS;
import static com.flipkart.krystal.vajram.codegen.common.Constants.CODEGEN_PHASE_KEY;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.codegen.common.Utils;
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

@SupportedAnnotationTypes({
  "com.flipkart.krystal.vajram.Vajram",
  "com.flipkart.krystal.vajram.VajramTrait"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions(CODEGEN_PHASE_KEY)
public class VajramModelGenProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Utils util = new Utils(processingEnv, this.getClass());
    String phaseString = processingEnv.getOptions().get(CODEGEN_PHASE_KEY);
    try {
      if (phaseString == null || !MODELS.equals(CodegenPhase.valueOf(phaseString))) {
        util.note(
            "Skipping VajramModelGenProcessor since codegen phase is %s"
                .formatted(String.valueOf(phaseString)));
        return false;
      }
    } catch (IllegalArgumentException e) {
      util.error(
          ("VajramModelGenProcessor could not parse phase string '%s'. "
                  + "Exactly one of %s must be passed as value to java compiler "
                  + "via the annotation processor argument '-A%s='")
              .formatted(
                  String.valueOf(phaseString),
                  Arrays.toString(CodegenPhase.values()),
                  CODEGEN_PHASE_KEY),
          null);
    }
    List<TypeElement> vajramRootDefinitions = util.getDefinitionClasses(roundEnv);
    util.note(
        "Vajrams and Traits received by VajramModelGenProcessor: %s"
            .formatted(
                vajramRootDefinitions.stream()
                    .map(Objects::toString)
                    .collect(
                        joining(lineSeparator(), '[' + lineSeparator(), lineSeparator() + ']'))));
    for (TypeElement vajramClass : vajramRootDefinitions) {
      VajramCodeGenerator vajramCodeGenerator =
          new VajramCodeGenerator(util.computeVajramInfo(vajramClass), util);

      vajramCodeGenerator.vajramRequest();

      if (vajramClass.getAnnotation(Vajram.class) != null) {
        vajramCodeGenerator.vajramFacets();
      }
    }
    return false;
  }
}
