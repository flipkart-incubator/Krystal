package com.flipkart.krystal.vajram.codegen.processor;

import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.flipkart.krystal.vajram.codegen.processor.Constants.DEFAULT_MODELS_CODEGEN_PROVIDER;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.codegen.common.models.AbstractKrystalAnnoProcessor;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.model.ModelRoot;
import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("com.flipkart.krystal.model.ModelRoot")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions(CODEGEN_PHASE_KEY)
public final class ModelGenProcessor extends AbstractKrystalAnnoProcessor {

  @Override
  protected boolean processImpl(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    CodeGenUtility util = codeGenUtil();
    List<TypeElement> modelRoots =
        roundEnv.getElementsAnnotatedWith(ModelRoot.class).stream()
            .filter(element -> element.getKind() == ElementKind.INTERFACE)
            .map(executableElement -> (TypeElement) executableElement)
            .toList();
    util.note(
        "Model Roots received by %s: %s"
            .formatted(
                getClass().getSimpleName(),
                modelRoots.stream()
                    .map(Objects::toString)
                    .collect(
                        joining(lineSeparator(), '[' + lineSeparator(), lineSeparator() + ']'))));

    Iterable<ModelsCodeGeneratorProvider> codeGeneratorProviders =
        Iterables.concat(
            List.of(DEFAULT_MODELS_CODEGEN_PROVIDER),
            // Load custom model code generator providers
            ServiceLoader.load(
                ModelsCodeGeneratorProvider.class, this.getClass().getClassLoader()));

    for (TypeElement modelRoot : modelRoots) {
      ModelsCodeGenContext creationContext =
          new ModelsCodeGenContext(modelRoot, util, codegenPhase());
      for (ModelsCodeGeneratorProvider customCodeGeneratorProvider : codeGeneratorProviders) {
        try {
          customCodeGeneratorProvider.create(creationContext).generate();
        } catch (Exception e) {
          util.error(Throwables.getStackTraceAsString(e), creationContext.modelRootType());
        }
      }
    }
    return false;
  }
}
