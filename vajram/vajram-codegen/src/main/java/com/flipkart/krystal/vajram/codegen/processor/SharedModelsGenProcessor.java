package com.flipkart.krystal.vajram.codegen.processor;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.flipkart.krystal.codegen.common.models.Constants.MODULE_ROOT_PATH_KEY;
import static com.flipkart.krystal.codegen.common.models.Constants.SHARED_MODELS_SUB_PACKAGE;
import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.models.AbstractKrystalAnnoProcessor;
import com.flipkart.krystal.codegen.common.models.DeclaredTypeVisitor;
import com.flipkart.krystal.codegen.common.models.RunOnlyWhenCodegenPhaseIs;
import com.flipkart.krystal.codegen.common.models.TypeNameVisitor;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.InputsForVajram;
import com.flipkart.krystal.facets.VajramInputs;
import com.flipkart.krystal.vajram.codegen.common.models.DefaultFacetModel;
import com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInputsInfo;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Annotation processor that generates request interfaces for Vajrams whose inputs are defined
 * externally via {@link InputsForVajram} annotated interfaces that extend {@link VajramInputs}.
 *
 * <p>This processor runs during the MODELS codegen phase and generates the request interface in the
 * {@code parentPackage.shared_models} package.
 */
@SupportedAnnotationTypes("com.flipkart.krystal.facets.InputsForVajram")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions({CODEGEN_PHASE_KEY, MODULE_ROOT_PATH_KEY})
@RunOnlyWhenCodegenPhaseIs(MODELS)
public final class SharedModelsGenProcessor extends AbstractKrystalAnnoProcessor {

  @Override
  protected void processImpl(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Set<? extends Element> annotatedElements =
        roundEnv.getElementsAnnotatedWith(InputsForVajram.class);

    VajramCodeGenUtility vajramUtil = new VajramCodeGenUtility(codeGenUtil());

    CharSequence message =
        "SharedModelsGenProcessor found @InputsForVajram elements: %s"
            .formatted(
                annotatedElements.stream()
                    .map(Objects::toString)
                    .collect(
                        joining(lineSeparator(), '[' + lineSeparator(), lineSeparator() + ']')));
    codeGenUtil().note(message);

    List<Failure> failures = new ArrayList<>();
    for (Element element : annotatedElements) {
      if (!(element instanceof TypeElement inputsInterface)) {
        codeGenUtil().error("@InputsForVajram can only be applied to interfaces", element);
        continue;
      }
      InputsForVajram[] inputsForVajrams =
          inputsInterface.getAnnotationsByType(InputsForVajram.class);
      for (InputsForVajram inputsForVajram : inputsForVajrams) {
        try {
          processInputsForVajram(vajramUtil, inputsInterface, inputsForVajram);
        } catch (Exception e) {
          failures.add(new Failure(inputsInterface, e));
        }
      }
    }

    for (Failure failure : failures) {
      codeGenUtil()
          .error(
              "[SharedModels Codegen Exception] " + getStackTraceAsString(failure.throwable()),
              failure.element());
    }
  }

  private void processInputsForVajram(
      @UnknownNullability VajramCodeGenUtility vajramUtil,
      TypeElement inputsInterface,
      InputsForVajram inputsForVajram) {
    String parentPackage = inputsForVajram.parentPackage();
    String requestPackage = parentPackage + "." + SHARED_MODELS_SUB_PACKAGE;

    // Extract response type from VajramInputs<T>
    CodeGenType responseType = extractResponseType(inputsInterface);

    VajramID vajramId = vajramID(inputsForVajram.vajramId());
    List<DefaultFacetModel> inputs = buildInputFacets(inputsInterface, vajramId);
    VajramInputsInfo inputsInfo =
        new VajramInputsInfo(
            vajramId, responseType, requestPackage, inputs, Collections.emptyList(), false);
    // Build input facet models from the interface methods

    // Compute request super types (extends Request<ResponseType>)
    TypeName responseTypeName =
        new TypeNameVisitor(true).visit(responseType.typeMirror(codeGenUtil().processingEnv()));
    Iterable<TypeName> requestSuperTypes =
        List.of(ParameterizedTypeName.get(ClassName.get(Request.class), responseTypeName.box()));

    VajramCodeGenerator generator = new VajramCodeGenerator(inputsInfo, vajramUtil);
    generator.generateVajramRequest(
        inputsInfo,
        inputs,
        requestSuperTypes,
        inputsInterface.getQualifiedName().toString(),
        inputsInterface,
        inputsInterface,
        inputsInfo);
  }

  private CodeGenType extractResponseType(TypeElement inputsInterface) {
    // Find VajramInputs<T> in the interface's superinterfaces
    for (TypeMirror superInterface : inputsInterface.getInterfaces()) {
      if (superInterface instanceof DeclaredType declaredType) {
        Element superElement = declaredType.asElement();
        if (superElement instanceof TypeElement typeElement
            && typeElement
                .getQualifiedName()
                .contentEquals(VajramInputs.class.getCanonicalName())) {
          List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
          if (typeArgs.size() == 1) {
            return typeArgs
                .get(0)
                .accept(new DeclaredTypeVisitor(codeGenUtil(), inputsInterface), null);
          }
        }
      }
    }
    throw codeGenUtil()
        .errorAndThrow(
            "@InputsForVajram interface must extend VajramInputs<T> where T is the response type",
            inputsInterface);
  }

  private List<DefaultFacetModel> buildInputFacets(TypeElement inputsInterface, VajramID vajramID) {
    List<DefaultFacetModel> facets = new ArrayList<>();

    for (ExecutableElement method :
        ElementFilter.methodsIn(inputsInterface.getEnclosedElements())) {
      String facetName = method.getSimpleName().toString();
      TypeMirror returnType = method.getReturnType();
      CodeGenType dataType =
          returnType.accept(new DeclaredTypeVisitor(codeGenUtil(), method), null);

      DefaultFacetModel facet =
          DefaultFacetModel.builder()
              .name(facetName)
              .vajramId(vajramID)
              .dataType(dataType)
              .documentation(codeGenUtil().processingEnv().getElementUtils().getDocComment(method))
              .facetType(INPUT)
              .facetElement(method)
              .build();
      facets.add(facet);
    }
    return facets;
  }

  private record Failure(TypeElement element, Throwable throwable) {}
}
