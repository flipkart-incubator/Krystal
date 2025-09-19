package com.flipkart.krystal.vajram.codegen.processor;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.flipkart.krystal.vajram.codegen.processor.Constants.DEFAULT_VAJRAM_CODEGEN_PROVIDER;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.common.spi.AllVajramCodeGenContext;
import com.flipkart.krystal.vajram.codegen.common.spi.AllVajramsCodeGeneratorProvider;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenContext;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGeneratorProvider;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract sealed class AbstractVajramCodegenProcessor extends AbstractProcessor
    permits VajramModelGenProcessor, VajramWrapperGenProcessor {

  private final CodegenPhase codegenPhase;

  public AbstractVajramCodegenProcessor(CodegenPhase codegenPhase) {
    this.codegenPhase = codegenPhase;
    log.info(
        "Initializing {} with codegen phase {}", this.getClass().getSimpleName(), codegenPhase);
  }

  @Override
  public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    CodeGenUtility util = new CodeGenUtility(processingEnv, getClass());
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
    List<TypeElement> vajramDefinitions = util.getDefinitionClasses(roundEnv);
    util.note(
        "Vajrams and Traits received by %s: %s"
            .formatted(
                getClass().getSimpleName(),
                vajramDefinitions.stream()
                    .map(Objects::toString)
                    .collect(
                        joining(lineSeparator(), '[' + lineSeparator(), lineSeparator() + ']'))));

    Iterable<VajramCodeGeneratorProvider> vajramCodeGeneratorProviders =
        Iterables.concat(
            // Start with the default code generator
            List.of(DEFAULT_VAJRAM_CODEGEN_PROVIDER),
            // Load custom vajram code generator providers
            ServiceLoader.load(
                VajramCodeGeneratorProvider.class, this.getClass().getClassLoader()));
    record Failure(TypeElement element, Throwable throwable) {}
    List<Failure> failures = new ArrayList<>();

    List<VajramInfo> vajramInfos = new ArrayList<>();
    for (TypeElement vajramDefinition : vajramDefinitions) {
      try {
        VajramInfo vajramInfo = util.computeVajramInfo(vajramDefinition);
        vajramInfos.add(vajramInfo);
        VajramCodeGenContext creationContext =
            new VajramCodeGenContext(vajramInfo, util, codegenPhase);
        for (VajramCodeGeneratorProvider customCodeGeneratorProvider :
            vajramCodeGeneratorProviders) {
          try {
            customCodeGeneratorProvider.create(creationContext).generate();
          } catch (Exception e) {
            failures.add(new Failure(vajramDefinition, e));
            continue;
          }
        }
      } catch (Exception e) {
        failures.add(new Failure(vajramDefinition, e));
        continue;
      }
    }

    Iterable<AllVajramsCodeGeneratorProvider> allVajramCodeGeneratorProviders =
        ServiceLoader.load(AllVajramsCodeGeneratorProvider.class, this.getClass().getClassLoader());
    for (AllVajramsCodeGeneratorProvider allVajramCodeGen : allVajramCodeGeneratorProviders) {
      allVajramCodeGen
          .create(new AllVajramCodeGenContext(vajramInfos, util, codegenPhase))
          .generate();
    }
    for (Failure failure : failures) {
      util.note(
          "[Vajram Codegen Exception] "
              + failure.element().getQualifiedName()
              + "\n"
              + getStackTraceAsString(failure.throwable()));
    }

    return false;
  }
}
