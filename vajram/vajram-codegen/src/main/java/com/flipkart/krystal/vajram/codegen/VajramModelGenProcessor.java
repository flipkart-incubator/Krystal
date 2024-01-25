package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.codegen.Constants.COGENGEN_PHASE_KEY;
import static com.flipkart.krystal.vajram.codegen.Utils.getInputUtilClassName;
import static com.flipkart.krystal.vajram.codegen.models.CodegenPhase.MODELS;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.vajram.codegen.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.models.VajramInfo;
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

@SupportedAnnotationTypes("com.flipkart.krystal.vajram.VajramDef")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions(COGENGEN_PHASE_KEY)
public class VajramModelGenProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Utils util = new Utils(processingEnv, this.getClass());
    String phaseString = processingEnv.getOptions().get(COGENGEN_PHASE_KEY);
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
                  COGENGEN_PHASE_KEY),
          null);
    }
    List<TypeElement> vajramDefinitions = util.getVajramClasses(roundEnv);
    util.note(
        "Vajram Defs received by VajramModelGenProcessor: %s"
            .formatted(
                vajramDefinitions.stream()
                    .map(Objects::toString)
                    .collect(
                        joining(lineSeparator(), '[' + lineSeparator(), lineSeparator() + ']'))));
    for (TypeElement vajramClass : vajramDefinitions) {
      VajramInfo vajramInfo = util.computeVajramInfo(vajramClass);

      VajramCodeGenerator vajramCodeGenerator = util.createCodeGenerator(vajramInfo);

      util.generateSourceFile(
          vajramCodeGenerator.getPackageName() + '.' + vajramCodeGenerator.getRequestClassName(),
          vajramCodeGenerator.codeGenVajramRequest(),
          vajramClass);
      util.generateSourceFile(
          vajramCodeGenerator.getPackageName()
              + '.'
              + getInputUtilClassName(vajramCodeGenerator.getVajramName()),
          vajramCodeGenerator.codeGenInputUtil(),
          vajramClass);
    }
    return true;
  }
}
