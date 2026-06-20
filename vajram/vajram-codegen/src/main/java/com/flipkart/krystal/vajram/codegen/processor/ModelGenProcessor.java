package com.flipkart.krystal.vajram.codegen.processor;

import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.flipkart.krystal.codegen.common.models.Constants.MODULE_ROOT_PATH_KEY;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;

import com.flipkart.krystal.codegen.common.models.AbstractKrystalAnnoProcessor;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.model.ImportSharedModels;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.model.SupportedModelProtocolName;
import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

@SupportedAnnotationTypes({
  "com.flipkart.krystal.model.ModelRoot",
  "com.flipkart.krystal.model.ImportModels"
})
@AutoService(Processor.class)
@SupportedOptions({CODEGEN_PHASE_KEY, MODULE_ROOT_PATH_KEY})
public final class ModelGenProcessor extends AbstractKrystalAnnoProcessor {

  @Override
  protected void processImpl(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    CodeGenUtility util = codeGenUtil();
    List<TypeElement> modelRoots =
        new ArrayList<>(
            roundEnv.getElementsAnnotatedWith(ModelRoot.class).stream()
                .filter(
                    element ->
                        element.getKind() == ElementKind.INTERFACE
                            || element.getKind() == ElementKind.ENUM)
                .map(executableElement -> (TypeElement) executableElement)
                .toList());
    modelRoots.addAll(
        roundEnv.getElementsAnnotatedWith(ImportSharedModels.class).stream()
            .map(executableElement -> (TypeElement) executableElement)
            .map(executableElement -> getImportedModelElements(executableElement, util))
            .flatMap(List::stream)
            .peek(e -> validateImportedModels(e, util))
            .toList());
    util.note(
        "Model Roots received by %s: %s"
            .formatted(
                getClass().getSimpleName(),
                modelRoots.stream()
                    .map(Objects::toString)
                    .collect(
                        joining(lineSeparator(), '[' + lineSeparator(), lineSeparator() + ']'))));

    Iterable<ModelsCodeGeneratorProvider> codeGeneratorProviders =
        // Load custom model code generator providers
        ServiceLoader.load(ModelsCodeGeneratorProvider.class, this.getClass().getClassLoader());

    for (TypeElement modelRoot : modelRoots) {
      Set<Class<? extends ModelProtocol>> supportedModelProtocols =
          getSupportedModelProtocols(modelRoot, util);
      ModelsCodeGenContext creationContext =
          new ModelsCodeGenContext(modelRoot, codegenPhase(), util);
      for (Class<? extends ModelProtocol> modelProtocol : supportedModelProtocols) {
        if (stream(codeGeneratorProviders.spliterator(), false)
            .noneMatch(
                customCodeGeneratorProvider ->
                    customCodeGeneratorProvider
                        .getSupportedModelProtocols()
                        .contains(modelProtocol))) {
          util.note(
              "No model code generator found supporting model protocol %s for model root %s"
                  .formatted(modelProtocol, modelRoot),
              modelRoot);
        }
      }
      for (ModelsCodeGeneratorProvider customCodeGeneratorProvider : codeGeneratorProviders) {
        try {
          customCodeGeneratorProvider.create(creationContext).generate();
        } catch (Exception e) {
          util.error(Throwables.getStackTraceAsString(e), creationContext.modelRootType());
        }
      }
    }
    return;
  }

  private static List<TypeElement> getImportedModelElements(
      TypeElement executableElement, CodeGenUtility util) {
    ImportSharedModels importSharedModels =
        requireNonNull(executableElement.getAnnotation(ImportSharedModels.class));
    List<TypeElement> allSharedModels =
        new ArrayList<>(util.getTypeElemsFromAnnotationMember(importSharedModels::value));
    String[] packageNames = importSharedModels.fromPackages();
    for (String importedPackage : packageNames) {
      PackageElement packageElement =
          util.processingEnv().getElementUtils().getPackageElement(importedPackage);
      if (packageElement == null) {
        throw util.errorAndThrow(
            "Package %s not found".formatted(importedPackage), executableElement);
      }
      ElementFilter.typesIn(packageElement.getEnclosedElements()).stream()
          .filter(e -> e.getAnnotation(ModelRoot.class) != null)
          .forEach(allSharedModels::add);
    }
    return allSharedModels;
  }

  private void validateImportedModels(TypeElement modelRootElem, CodeGenUtility util) {
    ModelRoot modelRootAnno = modelRootElem.getAnnotation(ModelRoot.class);
    if (modelRootAnno == null || !modelRootAnno.isShared()) {
      throw util.errorAndThrow(
          "Imported model %s must be annotated with @ModelRoot(isShared=true)"
              .formatted(modelRootElem),
          modelRootElem);
    }
  }

  private Set<Class<? extends ModelProtocol>> getSupportedModelProtocols(
      TypeElement modelRootType, CodeGenUtility util) {
    SupportedModelProtocol[] protocols =
        modelRootType.getAnnotationsByType(SupportedModelProtocol.class);
    SupportedModelProtocolName[] protocolNames =
        modelRootType.getAnnotationsByType(SupportedModelProtocolName.class);
    Set<Class<? extends ModelProtocol>> modelProtocolClasses =
        Arrays.stream(protocols)
            .map(p -> util.getTypeElemFromAnnotationMember(p::value))
            .map(element -> element.getQualifiedName().toString())
            .map(
                protocolName -> {
                  try {
                    //noinspection unchecked
                    return (Class<? extends ModelProtocol>)
                        Class.forName(protocolName, false, this.getClass().getClassLoader());
                  } catch (ClassNotFoundException e) {
                    util.error(Throwables.getStackTraceAsString(e), modelRootType);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    modelProtocolClasses.addAll(
        Arrays.stream(protocolNames)
            .map(SupportedModelProtocolName::value)
            .map(
                protocolName -> {
                  try {
                    //noinspection unchecked
                    return (Class<? extends ModelProtocol>)
                        Class.forName(protocolName, false, this.getClass().getClassLoader());
                  } catch (ClassNotFoundException e) {
                    util.note(
                        "Skipping processing of model protocol %s since it is not part of the classpath."
                            .formatted(protocolName));
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(toSet()));

    if (modelProtocolClasses.isEmpty()) {
      return Set.of(PlainJavaObject.class);
    }
    return modelProtocolClasses;
  }
}
