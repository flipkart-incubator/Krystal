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
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
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
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("com.flipkart.krystal.model.ModelRoot")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions({CODEGEN_PHASE_KEY, MODULE_ROOT_PATH_KEY})
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
        // Load custom model code generator providers
        ServiceLoader.load(ModelsCodeGeneratorProvider.class, this.getClass().getClassLoader());

    for (TypeElement modelRoot : modelRoots) {
      Set<Class<? extends ModelProtocol>> supportedModelProtocols =
          getSupportedModelProtocols(modelRoot, util);
      ModelsCodeGenContext creationContext =
          new ModelsCodeGenContext(modelRoot, util, codegenPhase());
      for (Class<? extends ModelProtocol> modelProtocol : supportedModelProtocols) {
        if (stream(codeGeneratorProviders.spliterator(), false)
            .noneMatch(
                customCodeGeneratorProvider ->
                    customCodeGeneratorProvider
                        .getSupportedModelProtocols()
                        .contains(modelProtocol))) {
          util.error(
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
    return false;
  }

  private Set<Class<? extends ModelProtocol>> getSupportedModelProtocols(
      TypeElement modelRootType, CodeGenUtility util) {
    SupportedModelProtocols supportedModelProtocols =
        modelRootType.getAnnotation(SupportedModelProtocols.class);
    if (supportedModelProtocols == null) {
      return Set.of(PlainJavaObject.class);
    }
    // Check if Json is mentioned in the annotation value
    return util.getTypesFromAnnotationMember(supportedModelProtocols::value).stream()
        .map(typeMirror -> util.processingEnv().getTypeUtils().asElement(typeMirror))
        .filter(elem -> elem instanceof QualifiedNameable)
        .map(element -> requireNonNull((QualifiedNameable) element).getQualifiedName().toString())
        .map(
            s -> {
              try {
                //noinspection unchecked
                return (Class<? extends ModelProtocol>)
                    Class.forName(s, false, this.getClass().getClassLoader());
              } catch (ClassNotFoundException e) {
                util.error(Throwables.getStackTraceAsString(e), modelRootType);
                return null;
              }
            })
        .filter(Objects::nonNull)
        .collect(toSet());
  }
}
