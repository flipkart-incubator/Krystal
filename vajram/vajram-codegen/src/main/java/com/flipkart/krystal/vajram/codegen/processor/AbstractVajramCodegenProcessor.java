package com.flipkart.krystal.vajram.codegen.processor;

import static com.flipkart.krystal.vajram.codegen.processor.Constants.DEFAULT_VAJRAM_CODEGEN_PROVIDER;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.codegen.common.models.AbstractKrystalAnnoProcessor;
import com.flipkart.krystal.codegen.common.models.CodeGenShortCircuitException;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.common.spi.AllVajramCodeGenContext;
import com.flipkart.krystal.vajram.codegen.common.spi.AllVajramsCodeGeneratorProvider;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenContext;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGeneratorProvider;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
abstract sealed class AbstractVajramCodegenProcessor extends AbstractKrystalAnnoProcessor
    permits VajramModelGenProcessor, VajramWrapperGenProcessor {

  private final List<TypeElement> vajramDefinitions = new LinkedList<>();

  public AbstractVajramCodegenProcessor() {}

  @Override
  public final boolean processImpl(
      Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    VajramCodeGenUtility util = new VajramCodeGenUtility(codeGenUtil());
    this.vajramDefinitions.addAll(util.getDefinitionClasses(roundEnv));
    CharSequence message =
        "Vajrams and Traits received by %s: %s"
            .formatted(
                getClass().getSimpleName(),
                vajramDefinitions.stream()
                    .map(Objects::toString)
                    .collect(
                        joining(lineSeparator(), '[' + lineSeparator(), lineSeparator() + ']')));
    util.codegenUtil().note(message);

    Iterable<VajramCodeGeneratorProvider> vajramCodeGeneratorProviders =
        Iterables.concat(
            // Start with the default code generator
            List.of(DEFAULT_VAJRAM_CODEGEN_PROVIDER),
            // Load custom vajram code generator providers
            ServiceLoader.load(
                VajramCodeGeneratorProvider.class, this.getClass().getClassLoader()));

    List<Failure> failures = new ArrayList<>();
    List<VajramInfo> vajramInfos = new ArrayList<>();
    Iterator<TypeElement> iterator = vajramDefinitions.iterator();
    while (iterator.hasNext()) {
      TypeElement vajramDefinition = iterator.next();
      try {
        VajramInfo vajramInfo = util.computeVajramInfo(vajramDefinition);
        vajramInfos.add(vajramInfo);
        VajramCodeGenContext creationContext =
            new VajramCodeGenContext(vajramInfo, util, codegenPhase());
        for (VajramCodeGeneratorProvider customCodeGeneratorProvider :
            vajramCodeGeneratorProviders) {
          try {
            customCodeGeneratorProvider.create(creationContext).generate();
          } catch (Exception e) {
            failures.add(new Failure(vajramDefinition, e));
          }
        }
        iterator.remove();
      } catch (Exception e) {
        failures.add(new Failure(vajramDefinition, e));
      }
    }

    Iterable<AllVajramsCodeGeneratorProvider> allVajramCodeGeneratorProviders =
        ServiceLoader.load(AllVajramsCodeGeneratorProvider.class, this.getClass().getClassLoader());
    for (AllVajramsCodeGeneratorProvider allVajramCodeGen : allVajramCodeGeneratorProviders) {
      try {
        allVajramCodeGen
            .create(new AllVajramCodeGenContext(vajramInfos, util, codegenPhase()))
            .generate();
      } catch (Exception e) {
        failures.add(new Failure(null, e));
      }
    }
    for (Failure failure : failures) {
      Throwable throwable = failure.throwable();
      if (throwable instanceof CodeGenShortCircuitException) {
        util.codegenUtil()
            .note("[Vajram Codegen Exception]" + throwable.getMessage(), failure.element());
      } else {
        util.codegenUtil()
            .error(
                "[Vajram Codegen Exception] " + getStackTraceAsString(throwable),
                failure.element());
      }
    }
    return false;
  }

  private record Failure(@Nullable TypeElement element, Throwable throwable) {}
}
