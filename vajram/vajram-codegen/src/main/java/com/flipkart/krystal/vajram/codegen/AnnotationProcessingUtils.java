package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.codegen.FacetFieldTypeVisitor.isOptional;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.REQUEST_SUFFIX;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.toMap;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.codegen.models.DependencyModel;
import com.flipkart.krystal.vajram.codegen.models.DependencyModel.DependencyModelBuilder;
import com.flipkart.krystal.vajram.codegen.models.InputModel;
import com.flipkart.krystal.vajram.codegen.models.InputModel.InputModelBuilder;
import com.flipkart.krystal.vajram.codegen.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.models.VajramInfoLite;
import com.flipkart.krystal.vajram.codegen.utils.CodegenUtils;
import com.squareup.javapoet.TypeName;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

public class AnnotationProcessingUtils {

  private final RoundEnvironment roundEnv;
  private final ProcessingEnvironment processingEnv;
  private final CodegenUtils codegenUtils;

  public AnnotationProcessingUtils(RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
    this.roundEnv = roundEnv;
    this.processingEnv = processingEnv;
    this.codegenUtils =
        new CodegenUtils(processingEnv.getTypeUtils(), processingEnv.getElementUtils());
  }

  List<TypeElement> getVajramClasses() {
    return roundEnv.getElementsAnnotatedWith(VajramDef.class).stream()
        .filter(element -> element.getKind() == ElementKind.CLASS)
        .map(executableElement -> (TypeElement) executableElement)
        .toList();
  }

  VajramCodeGenerator createCodeGenerator(VajramInfo vajramInfo) {
    return new VajramCodeGenerator(
        vajramInfo,
        vajramInfo.dependencies().stream()
            .map(DependencyModel::depVajramId)
            .collect(
                toMap(
                    VajramID::vajramId,
                    vajramId ->
                        new VajramInfoLite(
                            vajramId.vajramId(),
                            TypeName.get(vajramId.responseType().javaModelType(processingEnv))))),
        processingEnv);
  }

  void generateSourceFile(String className, String code, TypeElement vajramDefinition) {
    try {
      JavaFileObject requestFile =
          processingEnv.getFiler().createSourceFile(className, vajramDefinition);
      note("Successfully Create source file %s".formatted(className));
      try (PrintWriter out = new PrintWriter(requestFile.openWriter())) {
        out.println(code);
      }
    } catch (Exception e) {
      log(
          Kind.WARNING,
          "Error creating java file for className: %s. Error: %s".formatted(className, e),
          vajramDefinition);
    }
  }

  public VajramInfo computeVajramInfo(TypeElement vajramClass) {
    note("Did not find .vajram.yaml file. Will use annotated fields to generate models");
    VajramID vajramId = getVajramId(vajramClass);
    List<? extends Element> enclosedElements = vajramClass.getEnclosedElements();
    List<VariableElement> fields = ElementFilter.fieldsIn(enclosedElements);
    List<VariableElement> inputFields =
        fields.stream()
            .filter(variableElement -> variableElement.getAnnotation(Input.class) != null)
            .toList();
    List<VariableElement> dependencyFields =
        fields.stream()
            .filter(variableElement -> variableElement.getAnnotation(Dependency.class) != null)
            .toList();
    PackageElement enclosingElement = (PackageElement) vajramClass.getEnclosingElement();
    String packageName = enclosingElement.getQualifiedName().toString();
    VajramInfo vajramInfo =
        new VajramInfo(
            vajramId,
            packageName,
            inputFields.stream()
                .map(
                    inputField -> {
                      InputModelBuilder<Object> inputBuilder = InputModel.builder();
                      inputBuilder.name(inputField.getSimpleName().toString());
                      inputBuilder.isMandatory(!isOptional(inputField.asType(), processingEnv));
                      DataType<?> dataType =
                          inputField
                              .asType()
                              .accept(
                                  new FacetFieldTypeVisitor(processingEnv, true, inputField), null);
                      inputBuilder.type(dataType);
                      inputBuilder.needsModulation(
                          inputField.getAnnotation(Input.class).modulated());
                      return inputBuilder.build();
                    })
                .collect(toImmutableList()),
            dependencyFields.stream()
                .map(
                    depField -> {
                      DependencyModelBuilder depBuilder = DependencyModel.builder();
                      depBuilder.name(depField.getSimpleName().toString());
                      depBuilder.isMandatory(!isOptional(depField.asType(), processingEnv));
                      Dependency dependency = depField.getAnnotation(Dependency.class);
                      VajramID depVajramId =
                          getDepGenModel(vajramId.vajramId(), depField).depVajramId();
                      Optional<TypeMirror> vajramReqType =
                          getTypeFromAnnotationMember(dependency::withVajramReq);
                      Optional<TypeMirror> vajramType =
                          getTypeFromAnnotationMember(dependency::onVajram);
                      TypeMirror vajramOrReqType =
                          vajramReqType
                              .or(() -> vajramType)
                              .orElseThrow(
                                  () -> {
                                    log(
                                        Kind.ERROR,
                                        "At least one of `onVajram` or `withVajramReq` is needed in dependency declaration '%s' of vajram '%s'"
                                            .formatted(depField.getSimpleName(), vajramId),
                                        depField);
                                    return new RuntimeException("Invalid Dependency specification");
                                  });
                      if (vajramReqType.isPresent() && vajramType.isPresent()) {
                        log(
                            Kind.ERROR,
                            ("Both `onVajram` or `withVajramReq` cannot be set."
                                    + " Please set only one of them for dependency '%s' of vajram '%s'")
                                .formatted(depField.getSimpleName(), vajramId),
                            depField);
                      } else {
                        depBuilder
                            .depVajramId(depVajramId)
                            .depReqClassName(
                                getVajramReqClassName(
                                    (TypeElement)
                                        processingEnv.getTypeUtils().asElement(vajramOrReqType)))
                            .canFanout(dependency.canFanout());
                        return depBuilder.build();
                      }
                      throw new RuntimeException("Invalid Dependency specification");
                    })
                .collect(toImmutableList()),
            getResponseType(vajramClass),
            vajramClass);
    note("VajramInfo: %s".formatted(vajramInfo));
    return vajramInfo;
  }

  private DependencyModel getDepGenModel(String vajramId, VariableElement depField) {
    Dependency dependency = depField.getAnnotation(Dependency.class);
    DependencyModelBuilder depBuilder = DependencyModel.builder();
    depBuilder.name(depField.getSimpleName().toString());
    depBuilder.isMandatory(!isOptional(depField.asType(), processingEnv));
    Optional<TypeMirror> vajramReqType = getTypeFromAnnotationMember(dependency::withVajramReq);
    Optional<TypeMirror> vajramType = getTypeFromAnnotationMember(dependency::onVajram);
    TypeMirror vajramOrReqType =
        vajramReqType
            .or(() -> vajramType)
            .orElseThrow(
                () -> {
                  log(
                      Kind.ERROR,
                      "At least one of `onVajram` or `withVajramReq` is needed in dependency declaration '%s' of vajram '%s'"
                          .formatted(depField.getSimpleName(), vajramId),
                      depField);
                  return new RuntimeException("Invalid Dependency specification");
                });
    if (vajramReqType.isPresent() && vajramType.isPresent()) {
      log(
          Kind.ERROR,
          ("Both `onVajram` or `withVajramReq` cannot be set."
                  + " Please set only one of them for dependency '%s' of vajram '%s'")
              .formatted(depField.getSimpleName(), vajramId),
          depField);
    } else {
      TypeElement vajramOrReqElement =
          (TypeElement) processingEnv.getTypeUtils().asElement(vajramOrReqType);
      depBuilder
          .depVajramId(getVajramId(vajramOrReqElement))
          .depReqClassName(getVajramReqClassName(vajramOrReqElement))
          .canFanout(dependency.canFanout());
      return depBuilder.build();
    }
    throw new RuntimeException("Invalid Dependency specification");
  }

  private VajramID getVajramId(TypeElement vajramClass) {
    String vajramClassName = vajramClass.getSimpleName().toString();
    if (codegenUtils.isRawAssignable(vajramClass.asType(), Vajram.class)) {
      VajramDef vajramDef = vajramClass.getAnnotation(VajramDef.class);
      String vajramId = vajramDef.value();
      if (vajramId.isEmpty()) {
        vajramId = vajramClassName;
      }
      return VajramID.vajramID(vajramId);
    } else if (codegenUtils.isRawAssignable(vajramClass.asType(), VajramRequest.class)) {
      return VajramID.vajramID(
          vajramClassName.substring(0, vajramClassName.length() - REQUEST_SUFFIX.length()));
    } else {
      throw new IllegalArgumentException(
          "Unknown class hierarchy of vajram class %s. Expected %s or %s"
              .formatted(vajramClass, Vajram.class, VajramRequest.class));
    }
  }

  private String getVajramReqClassName(TypeElement vajramClass) {
    if (codegenUtils.isRawAssignable(vajramClass.asType(), Vajram.class)) {
      return vajramClass.getQualifiedName().toString() + "Request";
    } else if (codegenUtils.isRawAssignable(vajramClass.asType(), VajramRequest.class)) {
      return vajramClass.getQualifiedName().toString();
    } else {
      throw new AssertionError("This should not happen!");
    }
  }

  private Optional<TypeMirror> getTypeFromAnnotationMember(Supplier<Class<?>> runnable) {
    try {
      runnable.get();
      throw new AssertionError();
    } catch (MirroredTypeException mte) {
      return Optional.ofNullable(mte.getTypeMirror());
    }
  }

  private TypeName getResponseType(TypeElement vajramDef) {
    List<TypeMirror> currentTypes = List.of(vajramDef.asType());
    note("VajramDef: %s".formatted(vajramDef));

    Types typeUtils = processingEnv.getTypeUtils();
    DeclaredType vajramInterface = null;
    do {
      List<TypeMirror> newSuperTypes = new ArrayList<>();
      for (TypeMirror currentType : currentTypes) {
        List<DeclaredType> superTypes =
            processingEnv.getTypeUtils().directSupertypes(currentType).stream()
                .filter(t -> (t instanceof DeclaredType))
                .map(t -> (DeclaredType) t)
                .toList();
        newSuperTypes.addAll(superTypes);
        for (DeclaredType superType : superTypes) {
          note("SuperType: %s [%s]".formatted(superType, superType.getClass()));
          Element element = typeUtils.asElement(superType);
          if (element instanceof TypeElement typeElement) {
            note("Element qualified name: %s".formatted(typeElement.getQualifiedName()));
            if (typeElement.getQualifiedName().contentEquals(Vajram.class.getName())) {
              vajramInterface = superType;
              break;
            }
          }
        }
        note("CurrentElement: %s".formatted(currentType));
      }
      if (vajramInterface == null) {
        currentTypes = newSuperTypes;
      }
    } while (!currentTypes.isEmpty() && vajramInterface == null);
    if (vajramInterface != null) {
      List<? extends TypeMirror> typeParameters = vajramInterface.getTypeArguments();
      if (typeParameters.size() == 1) {
        return TypeName.get(typeParameters.get(0));
      } else {
        log(
            Kind.ERROR,
            "Incorrect number of parameter types on Vajram interface. Expected 1, Found %s"
                .formatted(typeParameters),
            vajramDef);
      }
    }
    log(Kind.ERROR, "Unable to infer response type for Vajram", vajramDef);
    throw new RuntimeException();
  }

  void note(CharSequence message) {
    processingEnv.getMessager().printMessage(Kind.NOTE, message);
  }

  void log(Kind kind, String message) {
    processingEnv.getMessager().printMessage(kind, message);
  }

  void log(Kind kind, String message, Element element) {
    processingEnv.getMessager().printMessage(kind, message, element);
  }
}
