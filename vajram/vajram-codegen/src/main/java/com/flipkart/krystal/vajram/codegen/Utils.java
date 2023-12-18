package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.codegen.Constants.COMMON_INPUTS;
import static com.flipkart.krystal.vajram.codegen.Constants.INPUTS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.INPUTS_NEEDING_MODULATION;
import static com.flipkart.krystal.vajram.codegen.DeclaredTypeVisitor.isOptional;
import static com.google.common.collect.ImmutableList.toImmutableList;

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
import com.flipkart.krystal.vajram.inputs.InputSource;
import com.google.common.primitives.Primitives;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.processing.FilerException;
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

public class Utils {

  private static final boolean DEBUG = false;

  public static final String DOT = ".";
  public static final String COMMA = ",";
  public static final String REQUEST_SUFFIX = "Request";
  public static final String IMPL = "Impl";
  public static final String INPUT_UTIL = "InputUtil";
  public static final String CONVERTER = "CONVERTER";

  private final ProcessingEnvironment processingEnv;
  private final Types typeUtils;
  private final Elements elementUtils;

  public Utils(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
    this.typeUtils = processingEnv.getTypeUtils();
    this.elementUtils = processingEnv.getElementUtils();
  }

  List<TypeElement> getVajramClasses(RoundEnvironment roundEnv) {
    return roundEnv.getElementsAnnotatedWith(VajramDef.class).stream()
        .filter(element -> element.getKind() == ElementKind.CLASS)
        .map(executableElement -> (TypeElement) executableElement)
        .toList();
  }

  VajramCodeGenerator createCodeGenerator(VajramInfo vajramInfo) {
    Map<VajramID, VajramInfoLite> map = new HashMap<>();
    for (DependencyModel depModel : vajramInfo.dependencies()) {
      map.put(
          depModel.depVajramId(),
          new VajramInfoLite(depModel.depVajramId().vajramId(), depModel.responseType()));
    }
    return new VajramCodeGenerator(vajramInfo, map, processingEnv);
  }

  void generateSourceFile(String className, String code, TypeElement vajramDefinition) {
    try {
      JavaFileObject requestFile =
          processingEnv.getFiler().createSourceFile(className, vajramDefinition);
      note("Successfully Create source file %s".formatted(className));
      try (PrintWriter out = new PrintWriter(requestFile.openWriter())) {
        out.println(code);
      }
    } catch (FilerException e) {
      // Since we do multiple passes (codeGenVajramModels, and compileJava) where this annotation
      // processor is executed, we might end up creating the same file multiple times. This is not
      // an error, hence we ignore this exceeption
      //      note("Could not create source file for %s. Due to exception %s".formatted(className,
      // e));
    } catch (Exception e) {
      error(
          "Error creating java file for className: %s. Error: %s".formatted(className, e),
          vajramDefinition);
    }
  }

  public VajramInfo computeVajramInfo(TypeElement vajramClass) {
    VajramInfoLite vajramInfoLite = getVajramInfoLite(vajramClass);
    List<? extends Element> enclosedElements = vajramClass.getEnclosedElements();
    List<VariableElement> fields = ElementFilter.fieldsIn(enclosedElements);
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
                      InputModelBuilder<Object> inputBuilder = InputModel.builder();
                      inputBuilder.name(inputField.getSimpleName().toString());
                      inputBuilder.isMandatory(!isOptional(inputField.asType(), processingEnv));
                      DataType<?> dataType =
                          inputField
                              .asType()
                              .accept(
                                  new DeclaredTypeVisitor(processingEnv, true, inputField), null);
                      inputBuilder.type(dataType);
                      Optional<Input> inputAnno =
                          Optional.ofNullable(inputField.getAnnotation(Input.class));
                      Set<InputSource> sources = new LinkedHashSet<>();
                      if (inputAnno.isPresent()) {
                        inputBuilder.needsModulation(inputAnno.get().modulated());
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
    DependencyModelBuilder depBuilder = DependencyModel.builder();
    depBuilder.name(depField.getSimpleName().toString());
    depBuilder.isMandatory(!isOptional(depField.asType(), processingEnv));
    Optional<TypeMirror> vajramReqType =
        getTypeFromAnnotationMember(dependency::withVajramReq)
            .filter(
                typeMirror ->
                    !((QualifiedNameable) typeUtils.asElement(typeMirror))
                        .getQualifiedName()
                        .equals(
                            elementUtils
                                .getTypeElement(VajramRequest.class.getName())
                                .getQualifiedName()));
    Optional<TypeMirror> vajramType =
        getTypeFromAnnotationMember(dependency::onVajram)
            .filter(
                typeMirror ->
                    !((QualifiedNameable) typeUtils.asElement(typeMirror))
                        .getQualifiedName()
                        .equals(
                            elementUtils
                                .getTypeElement(Vajram.class.getName())
                                .getQualifiedName()));
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
          new DeclaredTypeVisitor(processingEnv, true, depField).visit(depField.asType());
      TypeElement vajramOrReqElement =
          (TypeElement) processingEnv.getTypeUtils().asElement(vajramOrReqType);
      VajramInfoLite depVajramId = getVajramInfoLite(vajramOrReqElement);
      depBuilder
          .depVajramId(vajramID(depVajramId.vajramId()))
          .depReqClassName(getVajramReqClassName(vajramOrReqElement))
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

  private VajramInfoLite getVajramInfoLite(TypeElement vajramOrReqClass) {
    String vajramClassSimpleName = vajramOrReqClass.getSimpleName().toString();
    if (isRawAssignable(vajramOrReqClass.asType(), VajramRequest.class)) {
      TypeMirror responseType = getResponseType(vajramOrReqClass, VajramRequest.class);
      TypeElement responseTypeElement = (TypeElement) typeUtils.asElement(responseType);
      return new VajramInfoLite(
          vajramClassSimpleName.substring(
              0, vajramClassSimpleName.length() - REQUEST_SUFFIX.length()),
          new DeclaredTypeVisitor(processingEnv, false, responseTypeElement).visit(responseType));
    } else if (isRawAssignable(vajramOrReqClass.asType(), Vajram.class)) {
      TypeMirror responseType = getResponseType(vajramOrReqClass, Vajram.class);
      TypeElement responseTypeElement = (TypeElement) typeUtils.asElement(responseType);
      VajramDef vajramDef = vajramOrReqClass.getAnnotation(VajramDef.class);
      String vajramId = vajramDef.value();
      if (vajramId.isEmpty()) {
        vajramId = vajramClassSimpleName;
      }
      return new VajramInfoLite(
          vajramId,
          new DeclaredTypeVisitor(processingEnv, false, responseTypeElement).visit(responseType));
    } else {
      throw new IllegalArgumentException(
          "Unknown class hierarchy of vajram class %s. Expected %s or %s"
              .formatted(vajramOrReqClass, Vajram.class, VajramRequest.class));
    }
  }

  private String getVajramReqClassName(TypeElement vajramClass) {
    if (isRawAssignable(vajramClass.asType(), Vajram.class)) {
      return vajramClass.getQualifiedName().toString() + "Request";
    } else if (isRawAssignable(vajramClass.asType(), VajramRequest.class)) {
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
    error("Unable to infer response type for Vajram", vajramDef);
    throw new RuntimeException();
  }

  void note(CharSequence message) {
    if (DEBUG) {
      processingEnv
          .getMessager()
          .printMessage(Kind.NOTE, "[%s] %s".formatted(getTimestamp(), message));
    }
  }

  public void error(String message, Element element) {
    processingEnv.getMessager().printMessage(Kind.ERROR, message, element);
  }

  private String getTimestamp() {
    return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
        Clock.systemDefaultZone().instant().atZone(ZoneId.systemDefault()));
  }

  public static String getInputUtilClassName(String vajramName) {
    return vajramName + INPUT_UTIL;
  }

  public static String getRequestClassName(String vajramName) {
    return vajramName + REQUEST_SUFFIX;
  }

  public static String getVajramImplClassName(String vajramId) {
    return vajramId + IMPL;
  }

  public static String getAllInputsClassname(String vajramName) {
    return vajramName + INPUTS_CLASS_SUFFIX;
  }

  public static String getCommonInputsClassname(String vajramName) {
    return vajramName + COMMON_INPUTS;
  }

  public static String getInputModulationClassname(String vajramName) {
    return vajramName + INPUTS_NEEDING_MODULATION;
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
   * @return true of the raw type (without generics) of {@code from} can be assigned to the raw type
   *     of {@code to}
   */
  public boolean isRawAssignable(TypeMirror from, Class<?> to) {
    return typeUtils.isAssignable(
        typeUtils.erasure(from),
        typeUtils.erasure(elementUtils.getTypeElement(to.getName()).asType()));
  }

  public TypeMirror box(TypeMirror type) {
    if (type instanceof PrimitiveType p) {
      return typeUtils.boxedClass(p).asType();
    } else {
      return type;
    }
  }
}