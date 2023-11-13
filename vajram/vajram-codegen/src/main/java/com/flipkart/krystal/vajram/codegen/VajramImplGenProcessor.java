package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.Dependency.DependencyType.VAJRAM;
import static com.flipkart.krystal.vajram.codegen.FacetFieldTypeVisitor.isOptional;
import static com.flipkart.krystal.vajram.codegen.VajramCodeGenFacade.INPUTS_FILE_EXTENSION;
import static com.flipkart.krystal.vajram.codegen.VajramCodeGenFacade.toVajramInfo;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getVajramImplClassName;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Dependency.DependencyType;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.codegen.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.models.VajramInfoLite;
import com.flipkart.krystal.vajram.inputs.Dependency.DependencyBuilder;
import com.flipkart.krystal.vajram.inputs.Input.InputBuilder;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.TypeName;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.checkerframework.checker.nullness.qual.Nullable;

@SupportedAnnotationTypes("com.flipkart.krystal.vajram.VajramDef")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class VajramImplGenProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    List<TypeElement> vajramDefinitions =
        roundEnv.getElementsAnnotatedWith(VajramDef.class).stream()
            .filter(element -> element.getKind() == ElementKind.CLASS)
            .map(executableElement -> (TypeElement) executableElement)
            .toList();
    note(
        "Vajram Defs received by VajramAnnoProcessor: %s"
            .formatted(
                vajramDefinitions.stream()
                    .map(Objects::toString)
                    .collect(
                        joining(lineSeparator(), '[' + lineSeparator(), lineSeparator() + ']'))));
    for (TypeElement vajramDefinition : vajramDefinitions) {
      String qualifiedVajramClassName = vajramDefinition.getQualifiedName().toString();
      @Nullable VajramInfo vajramInfo;
      try {
        String packageName =
            qualifiedVajramClassName.substring(0, qualifiedVajramClassName.lastIndexOf('.'));
        String vajramName =
            qualifiedVajramClassName.substring(qualifiedVajramClassName.lastIndexOf('.') + 1);
        File vajramInputFile =
            new File(
                processingEnv
                    .getFiler()
                    .getResource(
                        StandardLocation.SOURCE_PATH,
                        packageName,
                        vajramName + INPUTS_FILE_EXTENSION)
                    .toUri());
        vajramInfo = toVajramInfo(vajramInputFile, vajramName, packageName);
        note(
            "Found .vajram.yaml file: %s. Falling back to yaml based model generation"
                .formatted(vajramInputFile));
      } catch (IOException ignored) {
        vajramInfo = null;
      }
      if (vajramInfo == null) {
        note("Did not find .vajram.yaml file. Will use annotated fields to generate models");
        List<? extends Element> enclosedElements = vajramDefinition.getEnclosedElements();
        List<VariableElement> fields = ElementFilter.fieldsIn(enclosedElements);
        List<VariableElement> inputFields =
            fields.stream()
                .filter(variableElement -> variableElement.getAnnotation(Input.class) != null)
                .toList();
        List<VariableElement> dependencyFields =
            fields.stream()
                .filter(variableElement -> variableElement.getAnnotation(Dependency.class) != null)
                .toList();
        PackageElement enclosingElement = (PackageElement) vajramDefinition.getEnclosingElement();
        String packageName = enclosingElement.getQualifiedName().toString();
        vajramInfo =
            new VajramInfo(
                vajramDefinition.getSimpleName().toString(),
                packageName,
                inputFields.stream()
                    .map(
                        inputField -> {
                          InputBuilder<Object> inputBuilder =
                              com.flipkart.krystal.vajram.inputs.Input.builder();
                          inputBuilder.name(inputField.getSimpleName().toString());
                          inputBuilder.isMandatory(!isOptional(inputField.asType(), processingEnv));
                          DataType<?> dataType =
                              inputField
                                  .asType()
                                  .accept(
                                      new FacetFieldTypeVisitor(processingEnv, true, inputField),
                                      null);
                          inputBuilder.type(dataType);
                          inputBuilder.needsModulation(
                              inputField.getAnnotation(Input.class).modulated());

                          return inputBuilder.build();
                        })
                    .collect(toImmutableList()),
                dependencyFields.stream()
                    .map(
                        inputField -> {
                          DependencyBuilder<Object> depBuilder =
                              com.flipkart.krystal.vajram.inputs.Dependency.builder();
                          depBuilder.name(inputField.getSimpleName().toString());
                          depBuilder.isMandatory(!isOptional(inputField.asType(), processingEnv));
                          DataType<?> dataType =
                              inputField
                                  .asType()
                                  .accept(
                                      new FacetFieldTypeVisitor(processingEnv, true, inputField),
                                      null);
                          Dependency dependency = inputField.getAnnotation(Dependency.class);
                          DependencyType type = dependency.type();
                          if (VAJRAM.equals(type)) {
                            depBuilder
                                .dataAccessSpec(VajramID.vajramID(dependency.value(), dataType))
                                .canFanout(dependency.canFanout());
                          } else {
                            throw new UnsupportedOperationException(
                                "Unknown dependency type '%s'".formatted(type));
                          }
                          return depBuilder.build();
                        })
                    .collect(toImmutableList()),
                getResponseType(vajramDefinition));
        note("VajramInfo: %s".formatted(vajramInfo));
      }

      VajramCodeGenerator vajramCodeGenerator =
          new VajramCodeGenerator(
              vajramInfo,
              vajramInfo.dependencies().stream()
                  .map(VajramImplGenProcessor::getVajramId)
                  .collect(
                      toMap(
                          VajramID::vajramId,
                          vajramId ->
                              new VajramInfoLite(
                                  vajramId.vajramId(),
                                  TypeName.get(
                                      vajramId.responseType().javaType().orElseThrow())))));

      generateSourceFile(
          vajramCodeGenerator.getPackageName()
              + '.'
              + getVajramImplClassName(vajramInfo.vajramName()),
          vajramCodeGenerator.codeGenVajramImpl(
              Optional.ofNullable(VajramID.class.getClassLoader())
                  // classLoader cannot be null here
                  .orElseThrow()),
          vajramDefinition);
    }
    return true;
  }

  private static VajramID getVajramId(com.flipkart.krystal.vajram.inputs.Dependency<?> dependency) {
    return ((VajramID) dependency.dataAccessSpec());
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

  private void generateSourceFile(String className, String code, TypeElement vajramDefinition) {
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

  private void note(CharSequence message) {
    processingEnv.getMessager().printMessage(Kind.NOTE, message);
  }

  private void log(Kind kind, String message, TypeElement element) {
    processingEnv.getMessager().printMessage(kind, message, element);
  }
}
