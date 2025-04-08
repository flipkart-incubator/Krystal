package com.flipkart.krystal.vajram.codegen.processor;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.flipkart.krystal.vajram.codegen.processor.Constants.DEFAULT_VAJRAM_CODEGEN_PROVIDER;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.Utils;
import com.flipkart.krystal.vajram.codegen.common.models.VajramValidationException;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenContext;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGeneratorProvider;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

abstract sealed class AbstractVajramCodegenProcessor extends AbstractProcessor
    permits VajramModelGenProcessor, VajramWrapperGenProcessor {

  private final CodegenPhase codegenPhase;

  public AbstractVajramCodegenProcessor(CodegenPhase codegenPhase) {
    this.codegenPhase = codegenPhase;
  }

  @Override
  public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Utils util = new Utils(processingEnv, this.getClass());
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
                  CODEGEN_PHASE_KEY),
          null);
      return false;
    }
    List<TypeElement> vajramDefinitions = util.getDefinitionClasses(roundEnv);
    util.note(
        "Vajrams and Traits received by %s: %s"
            .formatted(
                getClass().getSimpleName(),
                vajramDefinitions.stream()
                    .map(Objects::toString)
                    .collect(
                        joining(lineSeparator(), '[' + lineSeparator(), lineSeparator() + ']'))));

    Iterable<VajramCodeGeneratorProvider> codeGeneratorProviders =
        Iterables.concat(
            // Start with the default code generator
            List.of(DEFAULT_VAJRAM_CODEGEN_PROVIDER),
            // Load custom vajram code generator providers
            ServiceLoader.load(
                VajramCodeGeneratorProvider.class, this.getClass().getClassLoader()));

    for (TypeElement vajramClass : vajramDefinitions) {
      VajramCodeGenContext creationContext =
          new VajramCodeGenContext(util.computeVajramInfo(vajramClass), util, codegenPhase);
      for (VajramCodeGeneratorProvider customCodeGeneratorProvider : codeGeneratorProviders) {
        try {
          customCodeGeneratorProvider.create(creationContext).generate();
        } catch (VajramValidationException e) {
          continue;
        }
      }
    }
    return false;
  }
}
