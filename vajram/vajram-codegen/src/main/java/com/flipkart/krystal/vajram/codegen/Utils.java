package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.codegen.Constants.BATCHABLE_FACETS;
import static com.flipkart.krystal.vajram.codegen.Constants.COMMON_FACETS;
import static com.flipkart.krystal.vajram.codegen.Constants.FACETS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.DeclaredTypeVisitor.isOptional;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Generated;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.batching.Batch;
import com.flipkart.krystal.vajram.codegen.models.DependencyModel;
import com.flipkart.krystal.vajram.codegen.models.DependencyModel.DependencyModelBuilder;
import com.flipkart.krystal.vajram.codegen.models.InputModel;
import com.flipkart.krystal.vajram.codegen.models.InputModel.InputModelBuilder;
import com.flipkart.krystal.vajram.codegen.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.models.VajramInfoLite;
import com.flipkart.krystal.vajram.facets.InputSource;
import com.flipkart.krystal.vajram.facets.Using;
import com.google.common.primitives.Primitives;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import jakarta.inject.Inject;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import lombok.Getter;

public class Utils {

  private static final boolean DEBUG = false;

  public static final String DOT = ".";
  public static final String COMMA = ",";
  public static final String REQUEST_SUFFIX = "Request";
  public static final String IMPL = "Impl";
  public static final String FACET_UTIL = "FacetUtil";
  public static final String CONVERTER = "BATCH_CONVERTER";

  @Getter private final ProcessingEnvironment processingEnv;
  private final Types typeUtils;
  private final Elements elementUtils;
  private final Class<?> generator;

  public Utils(ProcessingEnvironment processingEnv, Class<?> generator) {
    this.processingEnv = processingEnv;
    this.typeUtils = processingEnv.getTypeUtils();
    this.elementUtils = processingEnv.getElementUtils();
    this.generator = generator;
  }

  List<TypeElement> getVajramClasses(RoundEnvironment roundEnv) {
    return roundEnv.getElementsAnnotatedWith(VajramDef.class).stream()
        .filter(element -> element.getKind() == ElementKind.CLASS)
        .map(executableElement -> (TypeElement) executableElement)
        .toList();
  }

  VajramCodeGenerator createCodeGenerator(VajramInfo vajramInfo) {
    Map<VajramID, VajramInfoLite> vajramDefs = new HashMap<>();
    for (DependencyModel depModel : vajramInfo.dependencies()) {
      vajramDefs.put(
          depModel.depVajramId(),
          new VajramInfoLite(depModel.depVajramId().vajramId(), depModel.responseType()));
    }
    return new VajramCodeGenerator(vajramInfo, vajramDefs, processingEnv, this);
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
      error(
          "Error creating java file for className: %s. Error: %s".formatted(className, e),
          vajramDefinition);
    }
  }

  public VajramInfo computeVajramInfo(TypeElement vajramClass) {
    VajramInfoLite vajramInfoLite = getVajramInfoLite(vajramClass);
    Optional<Element> facetsClass =
        vajramClass.getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.CLASS)
            .filter(element -> element.getSimpleName().contentEquals("_Facets"))
            .findFirst()
            .map(element -> typeUtils.asElement(element.asType()));

    List<VariableElement> fields =
        ElementFilter.fieldsIn(facetsClass.map(Element::getEnclosedElements).orElse(List.of()));
    List<VariableElement> inputFields =
        fields.stream()
            .filter(
                variableElement ->
                    variableElement.getAnnotation(Input.class) != null
                        || variableElement.getAnnotation(Inject.class) != null)
            .toList();
    List<VariableElement> dependencyFields =
        fields.stream()
            .filter(variableElement -> variableElement.getAnnotation(Dependency.class) != null)
            .toList();
    PackageElement enclosingElement = (PackageElement) vajramClass.getEnclosingElement();
    String packageName = enclosingElement.getQualifiedName().toString();
    VajramInfo vajramInfo =
        new VajramInfo(
            vajramID(vajramInfoLite.vajramId()),
            vajramInfoLite.responseType(),
            packageName,
            inputFields.stream()
                .map(
                    inputField -> {
                      InputModelBuilder<Object> inputBuilder =
                          InputModel.builder().facetField(inputField);
                      inputBuilder.name(inputField.getSimpleName().toString());
                      inputBuilder.isMandatory(!isOptional(inputField.asType(), processingEnv));
                      DataType<Object> dataType =
                          inputField
                              .asType()
                              .accept(new DeclaredTypeVisitor<>(this, true, inputField), null);
                      inputBuilder.type(dataType);
                      inputBuilder.isBatched(
                          Optional.ofNullable(inputField.getAnnotation(Batch.class)).isPresent());
                      Optional<Input> inputAnno =
                          Optional.ofNullable(inputField.getAnnotation(Input.class));
                      Set<InputSource> sources = new LinkedHashSet<>();
                      if (inputAnno.isPresent()) {
                        sources.add(InputSource.CLIENT);
                      }
                      if (inputField.getAnnotation(Inject.class) != null) {
                        sources.add(InputSource.SESSION);
                      }
                      inputBuilder.sources(sources);
                      return inputBuilder.build();
                    })
                .collect(toImmutableList()),
            dependencyFields.stream()
                .map(depField -> toDependencyModel(vajramInfoLite.vajramId(), depField))
                .collect(toImmutableList()),
            vajramClass);
    note("VajramInfo: %s".formatted(vajramInfo));
    return vajramInfo;
  }

  private DependencyModel toDependencyModel(String vajramId, VariableElement depField) {
    Dependency dependency = depField.getAnnotation(Dependency.class);
    DependencyModelBuilder depBuilder = DependencyModel.builder().facetField(depField);
    depBuilder.name(depField.getSimpleName().toString());
    depBuilder.isMandatory(!isOptional(depField.asType(), processingEnv));
    Optional<TypeMirror> vajramReqType =
        getTypeFromAnnotationMember(dependency::withVajramReq)
            .filter(
                typeMirror ->
                    !((QualifiedNameable) typeUtils.asElement(typeMirror))
                        .getQualifiedName()
                        .equals(getTypeElement(VajramRequest.class.getName()).getQualifiedName()));
    Optional<TypeMirror> vajramType =
        getTypeFromAnnotationMember(dependency::onVajram)
            .filter(
                typeMirror ->
                    !((QualifiedNameable) typeUtils.asElement(typeMirror))
                        .getQualifiedName()
                        .equals(getTypeElement(Vajram.class.getName()).getQualifiedName()));
    TypeMirror vajramOrReqType =
        vajramReqType
            .or(() -> vajramType)
            .orElseThrow(
                () -> {
                  error(
                      "At least one of `onVajram` or `withVajramReq` is needed in dependency declaration '%s' of vajram '%s'"
                          .formatted(depField.getSimpleName(), vajramId),
                      depField);
                  return new RuntimeException("Invalid Dependency specification");
                });
    if (vajramReqType.isPresent() && vajramType.isPresent()) {
      error(
          ("Both `withVajramReq` and `onVajram` cannot be set."
                  + " Please set only one of them for dependency '%s' of vajram '%s'."
                  + " Found withVajramReq=%s and onVajram=%s")
              .formatted(depField.getSimpleName(), vajramId, vajramReqType.get(), vajramType.get()),
          depField);
    } else {
      DataType<?> declaredDataType =
          new DeclaredTypeVisitor<>(this, true, depField).visit(depField.asType());
      TypeElement vajramOrReqElement =
          (TypeElement) processingEnv.getTypeUtils().asElement(vajramOrReqType);
      VajramInfoLite depVajramId = getVajramInfoLite(vajramOrReqElement);
      depBuilder
          .depVajramId(vajramID(depVajramId.vajramId()))
          .depReqClassQualifiedName(getVajramReqClassName(vajramOrReqElement))
          .canFanout(dependency.canFanout());
      if (!declaredDataType.equals(depVajramId.responseType())) {
        error(
            "Declared dependency type %s does not match dependency vajram response type %s"
                .formatted(declaredDataType, depVajramId.responseType()),
            depField);
      }
      depBuilder.responseType(declaredDataType);
      return depBuilder.build();
    }
    error(
        ("Invalid dependency spec of dependency '%s' of vajram '%s'."
                + " Found withVajramReq=%s and onVajram=%s")
            .formatted(depField.getSimpleName(), vajramId, vajramReqType.get(), vajramType.get()),
        depField);
    throw new RuntimeException("Invalid Dependency specification");
  }

  TypeElement getTypeElement(String name) {
    return Optional.ofNullable(elementUtils.getTypeElement(name))
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Could not find type element with name %s".formatted(name)));
  }

  private VajramInfoLite getVajramInfoLite(TypeElement vajramOrReqClass) {
    String vajramClassSimpleName = vajramOrReqClass.getSimpleName().toString();
    if (isRawAssignable(vajramOrReqClass.asType(), VajramRequest.class)) {
      TypeMirror responseType = getResponseType(vajramOrReqClass, VajramRequest.class);
      TypeElement responseTypeElement = (TypeElement) typeUtils.asElement(responseType);
      return new VajramInfoLite(
          vajramClassSimpleName.substring(
              0, vajramClassSimpleName.length() - REQUEST_SUFFIX.length()),
          new DeclaredTypeVisitor<>(this, false, responseTypeElement).visit(responseType));
    } else if (isRawAssignable(vajramOrReqClass.asType(), Vajram.class)) {
      TypeMirror responseType = getResponseType(vajramOrReqClass, Vajram.class);
      TypeElement responseTypeElement = (TypeElement) typeUtils.asElement(responseType);
      VajramDef vajramDef = vajramOrReqClass.getAnnotation(VajramDef.class);
      if (vajramDef == null) {
        throw new IllegalArgumentException(
            "Vajram class %s does not have @VajramDef annotation. This should not happen"
                .formatted(vajramOrReqClass));
      }
      return new VajramInfoLite(
          vajramClassSimpleName,
          new DeclaredTypeVisitor<>(this, false, responseTypeElement).visit(responseType));
    } else {
      throw new IllegalArgumentException(
          "Unknown class hierarchy of vajram class %s. Expected %s or %s"
              .formatted(vajramOrReqClass, Vajram.class, VajramRequest.class));
    }
  }

  private String getVajramReqClassName(TypeElement vajramClass) {
    if (isRawAssignable(vajramClass.asType(), Vajram.class)) {
      return vajramClass.getQualifiedName().toString() + REQUEST_SUFFIX;
    } else if (isRawAssignable(vajramClass.asType(), VajramRequest.class)) {
      return vajramClass.getQualifiedName().toString();
    } else {
      throw new AssertionError("This should not happen!");
    }
  }

  private Optional<TypeMirror> getTypeFromAnnotationMember(Supplier<Class<?>> runnable) {
    try {
      @SuppressWarnings("unused")
      var unused = runnable.get();
      throw new AssertionError();
    } catch (MirroredTypeException mte) {
      return Optional.ofNullable(mte.getTypeMirror());
    }
  }

  private TypeMirror getResponseType(TypeElement vajramDef, Class<?> targetClass) {
    int typeParamIndex = 0;
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
            if (typeElement.getQualifiedName().contentEquals(targetClass.getName())) {
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
      if (typeParameters.size() > typeParamIndex) {
        return typeParameters.get(typeParamIndex);
      } else {
        error(
            "Incorrect number of parameter types on Vajram interface. Expected 1, Found %s"
                .formatted(typeParameters),
            vajramDef);
      }
    }
    error(
        "Unable to infer response type for Vajram %s".formatted(vajramDef.getQualifiedName()),
        vajramDef);
    throw new RuntimeException();
  }

  void note(CharSequence message) {
    if (DEBUG) {
      processingEnv
          .getMessager()
          .printMessage(Kind.NOTE, "[%s] %s".formatted(getTimestamp(), message));
    }
  }

  public void error(String message, @Nullable Element element) {
    processingEnv.getMessager().printMessage(Kind.ERROR, message, element);
  }

  private String getTimestamp() {
    String ist = "IST";
    return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
        Clock.system(
                ZoneId.of(
                    Optional.ofNullable(ZoneId.SHORT_IDS.get(ist))
                        .orElseThrow(() -> new IllegalStateException("Could not find Zone" + ist))))
            .instant());
  }

  public static String getFacetUtilClassName(String vajramName) {
    return vajramName + FACET_UTIL;
  }

  public static String getRequestClassName(String vajramName) {
    return vajramName + REQUEST_SUFFIX;
  }

  public static String getVajramImplClassName(String vajramId) {
    return vajramId + IMPL;
  }

  public static String getAllFacetsClassname(String vajramName) {
    return vajramName + FACETS_CLASS_SUFFIX;
  }

  public static String getCommonFacetsClassname(String vajramName) {
    return vajramName + COMMON_FACETS;
  }

  public static String getBatchedFacetsClassname(String vajramName) {
    return vajramName + BATCHABLE_FACETS;
  }

  public static TypeName getMethodReturnType(Method method) {
    if (method.getGenericReturnType() instanceof ParameterizedType) {
      return toTypeName(method.getGenericReturnType());
    } else {
      return TypeName.get(method.getReturnType());
    }
  }

  public TypeName toTypeName(DataType<?> dataType) {
    return TypeName.get(toTypeMirror(dataType));
  }

  public TypeMirror toTypeMirror(DataType<?> dataType) {
    return dataType.javaModelType(processingEnv);
  }

  public static TypeName toTypeName(Type typeArg) {
    if (typeArg instanceof ParameterizedType parameterizedType) {
      final Type rawType = parameterizedType.getRawType();
      final Type[] typeArgs = parameterizedType.getActualTypeArguments();
      return ParameterizedTypeName.get(
          (ClassName) toTypeName(rawType),
          Arrays.stream(typeArgs).map(Utils::toTypeName).toArray(TypeName[]::new));
    } else {
      if (typeArg instanceof Class<?>) {
        return ClassName.get(Primitives.wrap((Class<?>) typeArg));
      } else {
        return ClassName.bestGuess(typeArg.getTypeName());
      }
    }
  }

  public static List<? extends TypeMirror> getTypeParameters(TypeMirror returnType) {
    return returnType.accept(
        new SimpleTypeVisitor14<List<? extends TypeMirror>, Void>() {
          @Override
          public List<? extends TypeMirror> visitDeclared(DeclaredType t, Void unused) {
            return t.getTypeArguments();
          }
        },
        null);
  }

  /**
   * Return true if the raw type (without generics) of {@code from} can be assigned to the raw type
   * of {@code to}
   */
  public boolean isRawAssignable(TypeMirror from, Class<?> to) {
    return typeUtils.isAssignable(
        typeUtils.erasure(from), typeUtils.erasure(getTypeElement(to.getName()).asType()));
  }

  public TypeMirror box(TypeMirror type) {
    if (type instanceof PrimitiveType p) {
      return typeUtils.boxedClass(p).asType();
    } else {
      return type;
    }
  }

  /**
   * Creates a class builder with the given class name. If the className is a blank string, then the
   * builder represents an anonymous class.
   *
   * @param className fully qualifield class name
   * @return a class builder with the given class name, with the {@link Generated} annotation
   *     applied on the class
   */
  public TypeSpec.Builder classBuilder(String className) {
    TypeSpec.Builder classBuilder;
    if (className.isBlank()) {
      classBuilder = TypeSpec.anonymousClassBuilder("");
    } else {
      classBuilder = TypeSpec.classBuilder(className);
    }
    return classBuilder.addAnnotation(
        AnnotationSpec.builder(Generated.class).addMember("by", "$S", generator.getName()).build());
  }

  /**
   * Infer facet name provided through @Using annotation. If @Using annotation is not present, then
   * infer facet name from the parameter name
   *
   * @param parameter the bind parameter in the resolver method
   * @return facet name in the form of String
   */
  public String inferFacetName(VariableElement parameter) {
    String usingInputName;
    if (Objects.nonNull(parameter.getAnnotation(Using.class))) {
      usingInputName = parameter.getAnnotation(Using.class).value();
    } else {
      usingInputName = parameter.getSimpleName().toString();
    }

    return usingInputName;
  }
}
