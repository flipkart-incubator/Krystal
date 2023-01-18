package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.datatypes.TypeUtils.getJavaType;
import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Arrays.stream;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.codegen.models.AbstractInput;
import com.flipkart.krystal.vajram.codegen.models.DependencyDef;
import com.flipkart.krystal.vajram.codegen.models.InputDef;
import com.flipkart.krystal.vajram.codegen.models.VajramDependencyDef;
import com.flipkart.krystal.vajram.codegen.models.VajramInputFile;
import com.flipkart.krystal.vajram.codegen.models.VajramInputsDef;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputSource;
import com.flipkart.krystal.vajram.inputs.InputValuesAdaptor;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.modulation.UnmodulatedInput;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

public class VajramCodeGenerator {

  private final String packageName;
  private final String requestClassName;
  private final VajramInputFile vajramInputFile;
  private final String allInputsClassName;
  private final String vajramName;

  public VajramCodeGenerator(VajramInputFile vajramInputFile) {
    this.vajramInputFile = vajramInputFile;
    Path filePath = vajramInputFile.srcRelativeFilePath();
    Path parentDir = filePath.getParent();
    this.vajramName = vajramInputFile.vajramName();
    this.packageName =
        IntStream.range(0, parentDir.getNameCount())
            .mapToObj(i -> parentDir.getName(i).toString())
            .collect(Collectors.joining("."));
    this.requestClassName = getRequestClassName(vajramName);
    this.allInputsClassName = "AllInputs";
  }

  private static String getRequestClassName(String vajramName) {
    return (vajramName.toLowerCase().endsWith("vajram")
            ? vajramName.substring(0, vajramName.length() - 6)
            : vajramName)
        + "Request";
  }

  public String codeGenVajramImpl() {
      ImmutableCollection<VajramInputDefinition> inputDefs = vajramInputFile.vajramInputsDef().allInputsDefinitions();
      final TypeSpec.Builder vajramImplClass = createVajramImplClass();

      // Add superclass
      vajramImplClass.addModifiers(PUBLIC)
              .superclass(ClassName.bestGuess(vajramName).box()).build();

      // Method : getInputDefinitions
      MethodSpec.Builder inputDefinitionsBuilder = methodBuilder("getInputDefinitions")
              .addModifiers(PUBLIC)
              .returns(ParameterizedTypeName.get(ClassName.get(ImmutableList.class),
                      ClassName.get(VajramInputDefinition.class)));

      List<CodeBlock> codeBlocks = new ArrayList<>(inputDefs.size());
      // Input and Dependency code block
      inputDefs.forEach( vajramInputDefinition ->  {

          CodeBlock.Builder inputDefBuilder = CodeBlock.builder();
          if(vajramInputDefinition instanceof Input) {
              Input input = (Input) vajramInputDefinition;
              inputDefBuilder.add("Input.builder()")
                     .add(".name($S)", vajramInputDefinition.name());

            // handle input type
             Set<InputSource> inputSources = input.sources();
             if (!inputSources.isEmpty()) {
                 inputDefBuilder.add(".sources(");
                 ClassName className = ClassName.get(InputSource.class);
                 String sources = inputSources.stream().map( inputSource -> {
                     if (inputSource == InputSource.CLIENT) {
                         return "InputSource.CLIENT";
                     } else if (inputSource == InputSource.SESSION) {
                         return "InputSource.SESSION";
                     }
                     else {
                         throw new IllegalArgumentException("Incorrect source defined in vajram config");
                     }
                 }).collect(Collectors.joining(","));
                 inputDefBuilder.add(sources).add(")");
             }
             // handle data type
              DataType dataType = input.type();
              inputDefBuilder.add(".type(");
              if (dataType instanceof JavaType<?>) {
                  // custom handling
                  JavaType javaType = (JavaType) dataType;
                  ClassName className ;
                  if (!javaType.enclosingClasses().isEmpty() || javaType.simpleName().isPresent()) {
                      className =
                              ClassName.get((String) javaType.packageName().get(),
                                      (String) javaType.enclosingClasses().stream().collect(Collectors.joining(".")),
                                      (String) javaType.simpleName().get());
                  } else {
                      className= ClassName.bestGuess(javaType.className());
                  }
                  inputDefBuilder.add("$1T.java($2T.class)", ClassName.get(JavaType.class), className);
              } else {
                  String simpleName  = dataType.getClass().getSimpleName();
                  String name = simpleName.substring(0, simpleName.length()-4).toLowerCase();
                  inputDefBuilder.add("$T.$L()", ClassName.get(dataType.getClass().getPackageName(), dataType.getClass().getSimpleName()), name);
              }
              inputDefBuilder.add(")");
              if (vajramInputDefinition.isMandatory()) {
                 inputDefBuilder.add(".isMandatory()");
             }

             // last line
              inputDefBuilder.add(".build()");
          } else if (vajramInputDefinition instanceof Dependency) {
              Dependency dependency = (Dependency) vajramInputDefinition;
              inputDefBuilder.add("Dependency.builder()")
                      .add(".name($S)", dependency.name());
              DataAccessSpec dataAccessSpec = dependency.dataAccessSpec();
              if (dataAccessSpec instanceof VajramID) {
                  VajramID vajramID = (VajramID) dataAccessSpec;
                  inputDefBuilder.add(".dataAccessSpec(").add(
                                  CodeBlock.builder().add("$1T.vajramID($2S)",
                                          ClassName.get(VajramID.class),
                                                  vajramID.vajramId()).build())
                          .add(")");
              }
              if(vajramInputDefinition.isMandatory()) {
                  inputDefBuilder.add(".isMandatory()");
              }
              // build() as last step
              inputDefBuilder.add(".build()");
          }
          codeBlocks.add(inputDefBuilder.build());
      });
      CodeBlock.Builder returnCode = CodeBlock.builder()
              .add("return ImmutableList.of(\n").add(CodeBlock.join(codeBlocks, ",\n\t")).add("\n);");
      inputDefinitionsBuilder.addCode(returnCode.build());

      StringWriter writer = new StringWriter();
      try {
          JavaFile.builder(
                          packageName,
                          vajramImplClass
                                  .addMethod(inputDefinitionsBuilder.build())
                                  .build())
                  .indent("  ")
                  .build()
                  .writeTo(writer);
      } catch (IOException ignored) {

      }
      return writer.toString();
  }

  public String codeGenVajramRequest() {
    VajramInputsDef vajramInputsDef = vajramInputFile.vajramInputsDef();
    ImmutableList<InputDef> inputDefs = vajramInputsDef.inputs();
    MethodSpec.Builder requestConstructor = constructorBuilder().addModifiers(PRIVATE);
    ClassName builderClassType = ClassName.get(packageName + "." + requestClassName, "Builder");
    TypeSpec.Builder requestClass =
        classBuilder(requestClassName)
            .addModifiers(PUBLIC)
            .addSuperinterface(VajramRequest.class)
            .addAnnotation(EqualsAndHashCode.class)
            .addMethod(
                methodBuilder("builder")
                    .addModifiers(PUBLIC, STATIC)
                    .returns(builderClassType)
                    .addStatement("return new Builder()")
                    .build());
    TypeSpec.Builder builderClass =
        classBuilder("Builder")
            .addModifiers(PUBLIC, STATIC)
            .addAnnotation(EqualsAndHashCode.class)
            .addMethod(constructorBuilder().addModifiers(PRIVATE).build()); // <private Builder(){}>
    Set<String> inputNames = new LinkedHashSet<>();
    for (InputDef input : inputDefs) {
      Input<?> inputDefinition = input.toInputDefinition();
      if (!inputDefinition.sources().contains(InputSource.CLIENT)) {
        continue;
      }
      String inputJavaName = toJavaName(input.getName());
      inputNames.add(inputJavaName);
      TypeAndName javaType = getTypeName(inputDefinition.type());
      requestClass.addField(
          FieldSpec.builder(wrapPrimitive(javaType).typeName(), inputJavaName, PRIVATE, FINAL)
              .build());
      builderClass.addField(FieldSpec.builder(javaType.typeName(), inputJavaName, PRIVATE).build());
      requestConstructor.addParameter(
          ParameterSpec.builder(javaType.typeName(), inputJavaName).build());
      requestConstructor.addStatement("this.$L = $L", inputJavaName, inputJavaName);
      requestClass.addMethod(getterCodeForInput(input, inputJavaName, javaType));

      builderClass.addMethod(
          // public inputName(){return this.inputName;}
          methodBuilder(inputJavaName)
              .addModifiers(PUBLIC)
              .returns(javaType.typeName())
              .addStatement("return this.$L", inputJavaName) // Return
              .build());

      builderClass.addMethod(
          // public inputName(Type inputName){this.inputName = inputName; return this;}
          methodBuilder(inputJavaName)
              .returns(builderClassType)
              .addModifiers(PUBLIC)
              .addParameter(ParameterSpec.builder(javaType.typeName(), inputJavaName).build())
              .addStatement("this.$L = $L", inputJavaName, inputJavaName) // Set value
              .addStatement("return this", inputJavaName) // Return
              .build());
    }

    builderClass.addMethod(
        // public Request build(){
        //   return new Request(input1, input2, input3)
        // }
        methodBuilder("build")
            .returns(ClassName.get(packageName, requestClassName))
            .addModifiers(PUBLIC)
            .addStatement(
                "return new %s(%s)".formatted(requestClassName, String.join(", ", inputNames)))
            .build());
    StringWriter writer = new StringWriter();
    FromAndTo fromAndTo =
        fromAndToMethods(
            inputDefs.stream()
                .filter(
                    inputDef -> inputDef.toInputDefinition().sources().contains(InputSource.CLIENT))
                .toList(),
            ClassName.get(packageName, requestClassName));
    try {
      JavaFile.builder(
              packageName,
              requestClass
                  .addMethod(requestConstructor.build())
                  .addMethod(fromAndTo.from())
                  .addMethod(fromAndTo.to())
                  .addType(builderClass.build())
                  .build())
          .build()
          .writeTo(writer);
    } catch (IOException ignored) {

    }
    return writer.toString();
  }

  private static TypeAndName wrapPrimitive(TypeAndName javaType) {
    if (javaType.type().isPresent() && javaType.type().get() instanceof Class<?> clazz) {
      Class<?> wrapped = Primitives.wrap(clazz);
      return new TypeAndName(ClassName.get(wrapped), Optional.of(wrapped));
    }
    return javaType;
  }

  private static TypeAndName unwrapPrimitive(TypeAndName javaType) {
    if (javaType.type().isPresent() && javaType.type().get() instanceof Class<?> clazz) {
      Class<?> unwrapped = Primitives.unwrap(clazz);
      return new TypeAndName(TypeName.get(unwrapped), Optional.of(unwrapped));
    }
    return javaType;
  }

  private FromAndTo fromAndToMethods(
      List<? extends AbstractInput> inputDefs, ClassName enclosingClass) {
    @SuppressWarnings("rawtypes")
    Builder toInputValues =
        methodBuilder("toInputValues")
            .returns(Inputs.class)
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .addStatement(
                "$T builder = new $T<>()",
                new TypeToken<Map<String, InputValue<?>>>() {}.getType(),
                new TypeToken<HashMap>() {}.getType());
    MethodSpec.Builder fromInputValues =
        methodBuilder("from")
            .returns(enclosingClass)
            .addModifiers(PUBLIC, STATIC)
            .addParameter(Inputs.class, "values");
    for (AbstractInput input : inputDefs) {
      String inputJavaName = toJavaName(input.getName());
      toInputValues.addStatement(
          "builder.put($S, $T.withValue($L()))",
          input.getName(),
          ValueOrError.class,
          inputJavaName);
    }
    toInputValues.addStatement("return new $T(builder)", Inputs.class);

    List<String> inputNames = inputDefs.stream().map(AbstractInput::getName).toList();
    fromInputValues.addStatement(
        "return new $T(%s)"
            .formatted(
                inputNames.stream()
                    .map(s -> "values.getInputValueOrDefault($S, null)")
                    .collect(Collectors.joining(", "))),
        Stream.concat(Stream.of(enclosingClass), inputNames.stream()).toArray());
    return new FromAndTo(fromInputValues.build(), toInputValues.build());
  }

  private static TypeAndName getTypeName(DataType dataType) {
    if (dataType instanceof JavaType<?> javaType) {
      Optional<String> simpleName = javaType.simpleName();
      if (simpleName.isPresent()) {
        List<String> classNames =
            Stream.concat(javaType.enclosingClasses().stream(), Stream.of(simpleName.get()))
                .toList();
        return new TypeAndName(
            ClassName.get(
                javaType.packageName().orElse(""),
                classNames.get(0),
                classNames.subList(1, classNames.size()).toArray(String[]::new)));
      } else {
        return new TypeAndName(ClassName.bestGuess(javaType.className()));
      }
    } else {
      Optional<Type> javaType = getJavaType(dataType);
      return new TypeAndName(
          javaType
              .map(type -> (type instanceof Class<?> clazz) ? Primitives.wrap(clazz) : type)
              .map(TypeName::get)
              .orElseThrow(
                  () -> {
                    throw new IllegalArgumentException(
                        "Could not determine java Type of %s".formatted(dataType));
                  }),
          javaType);
    }
  }

  private static MethodSpec getterCodeForInput(
      AbstractInput input, String name, TypeAndName typeAndName) {
    boolean wrapWithOptional = input instanceof InputDef && !input.isMandatory();
    return methodBuilder(name)
        .returns(
            wrapWithOptional
                ? optional(wrapPrimitive(typeAndName).typeName())
                : unwrapPrimitive(typeAndName).typeName())
        .addModifiers(PUBLIC)
        .addCode(
            wrapWithOptional
                // public Optional<Type> inputName(){
                //    return Optional.ofNullable(this.inputName);
                // }
                ? CodeBlock.builder()
                    .addStatement("return $T.ofNullable(this.$L)", Optional.class, name)
                    .build()
                // public Type inputName(){return this.inputName;}
                : CodeBlock.builder().addStatement("return this.$L", name).build())
        .build();
  }

  public String codeGenInputUtil() {
    boolean doInputsNeedModulation =
        vajramInputFile.vajramInputsDef().allInputsDefinitions().stream()
            .anyMatch(VajramInputDefinition::needsModulation);
    if (doInputsNeedModulation) {
      return codeGenModulatedInputUtil();
    } else {
      return codeGenSimpleInputUtil();
    }
  }

  private String codeGenSimpleInputUtil() {
    TypeSpec.Builder inputUtilClass = createInputUtilClass();
    TypeSpec.Builder allInputsClass =
        classBuilder(allInputsClassName)
            .addModifiers(FINAL, STATIC)
            .addAnnotations(recordAnnotations());

    VajramInputsDef vajramInputsDef = vajramInputFile.vajramInputsDef();
    vajramInputsDef
        .inputs()
        .forEach(
            inputDef -> {
              String inputJavaName = toJavaName(inputDef.getName());
              TypeAndName javaType = getTypeName(inputDef.toInputDefinition().type());
              allInputsClass.addField(javaType.typeName(), inputJavaName, PRIVATE, FINAL);
              allInputsClass.addMethod(getterCodeForInput(inputDef, inputJavaName, javaType));
            });

    vajramInputsDef
        .dependencies()
        .forEach(dependencyDef -> addDependencyOutputs(allInputsClass, dependencyDef));

    StringWriter writer = new StringWriter();
    try {
      JavaFile.builder(packageName, inputUtilClass.addType(allInputsClass.build()).build())
          .build()
          .writeTo(writer);
    } catch (IOException ignored) {

    }
    return writer.toString();
  }

  private static void addDependencyOutputs(
      TypeSpec.Builder enclosingClass, DependencyDef dependencyDef) {
    String inputJavaName = toJavaName(dependencyDef.getName());
    if (dependencyDef instanceof VajramDependencyDef vajramDepSpec) {
      String depVajramClass = vajramDepSpec.getVajramClass();
      int lastDotIndex = depVajramClass.lastIndexOf('.');
      String depRequestClass = getRequestClassName(depVajramClass.substring(lastDotIndex + 1));
      String depPackageName = depVajramClass.substring(0, lastDotIndex);
      TypeName javaType =
          ParameterizedTypeName.get(
              ClassName.get(DependencyResponse.class),
              ClassName.get(depPackageName, depRequestClass),
              getTypeName(vajramDepSpec.toDataType()).typeName());
      enclosingClass.addField(javaType, inputJavaName, PRIVATE, FINAL);
      enclosingClass.addMethod(
          getterCodeForInput(dependencyDef, inputJavaName, new TypeAndName(javaType)));
    }
  }

  private String codeGenModulatedInputUtil() {
    StringWriter writer = new StringWriter();
    try {
      TypeSpec.Builder inputUtilClass = createInputUtilClass();

      VajramInputsDef vajramInputsDef = vajramInputFile.vajramInputsDef();
      String imClassName = "InputsNeedingModulation";
      String ciClassName = "CommonInputs";
      FromAndTo imFromAndTo =
          fromAndToMethods(
              vajramInputsDef.inputs().stream().filter(InputDef::isNeedsModulation).toList(),
              ClassName.get(packageName, getInputUtilClassName(), imClassName));
      TypeSpec.Builder inputsNeedingModulation =
          TypeSpec.classBuilder(imClassName)
              .addModifiers(STATIC)
              .addSuperinterface(InputValuesAdaptor.class)
              .addAnnotations(recordAnnotations())
              .addMethod(imFromAndTo.to())
              .addMethod(imFromAndTo.from());

      FromAndTo ciFromAndTo =
          fromAndToMethods(
              Stream.concat(
                      vajramInputsDef.inputs().stream().filter(i -> !i.isNeedsModulation()),
                      vajramInputsDef.dependencies().stream())
                  .toList(),
              ClassName.get(packageName, getInputUtilClassName(), ciClassName));
      TypeSpec.Builder commonInputs =
          TypeSpec.classBuilder(ciClassName)
              .addModifiers(STATIC)
              .addSuperinterface(InputValuesAdaptor.class)
              .addAnnotations(recordAnnotations())
              .addMethod(ciFromAndTo.to())
              .addMethod(ciFromAndTo.from());
      ClassName imType = ClassName.get(packageName, getInputUtilClassName(), imClassName);
      ClassName ciType = ClassName.get(packageName, getInputUtilClassName(), ciClassName);
      vajramInputsDef
          .inputs()
          .forEach(
              inputDef -> {
                String inputJavaName = toJavaName(inputDef.getName());
                TypeAndName javaType = getTypeName(inputDef.toInputDefinition().type());
                if (inputDef.isNeedsModulation()) {
                  inputsNeedingModulation.addField(
                      javaType.typeName(), inputJavaName, PRIVATE, FINAL);
                  inputsNeedingModulation.addMethod(
                      getterCodeForInput(inputDef, inputJavaName, javaType));
                } else {
                  commonInputs.addField(javaType.typeName(), inputJavaName, PRIVATE, FINAL);
                  commonInputs.addMethod(getterCodeForInput(inputDef, inputJavaName, javaType));
                }
              });
      vajramInputsDef
          .dependencies()
          .forEach(dependencyDef -> addDependencyOutputs(commonInputs, dependencyDef));
      TypeName parameterizedTypeName =
          ParameterizedTypeName.get(ClassName.get(InputsConverter.class), imType, ciType);
      CodeBlock.Builder initializer =
          CodeBlock.builder()
              .add(
                  "$L",
                  TypeSpec.anonymousClassBuilder("")
                      .addSuperinterface(parameterizedTypeName)
                      .addMethod(
                          methodBuilder("apply")
                              .addModifiers(PUBLIC)
                              .returns(
                                  ParameterizedTypeName.get(
                                      ClassName.get(UnmodulatedInput.class), imType, ciType))
                              .addParameter(Inputs.class, "inputValues")
                              .addStatement(
                                  "return new $T<>($T.from(inputValues),$T.from(inputValues))",
                                  UnmodulatedInput.class,
                                  imType,
                                  ciType)
                              .build())
                      .build());
      FieldSpec.Builder converter =
          FieldSpec.builder(parameterizedTypeName, "CONVERTER")
              .addModifiers(STATIC)
              .initializer(initializer.build());
      JavaFile.builder(
              packageName,
              inputUtilClass
                  .addType(inputsNeedingModulation.build())
                  .addType(commonInputs.build())
                  .addField(converter.build())
                  .build())
          .build()
          .writeTo(writer);
    } catch (IOException ignored) {

    }
    return writer.toString();
  }

  private static List<AnnotationSpec> recordAnnotations() {
    return annotations(EqualsAndHashCode.class, AllArgsConstructor.class, ToString.class);
  }

  private static List<AnnotationSpec> annotations(Class<?>... annotations) {
    return stream(annotations).map(aClass -> AnnotationSpec.builder(aClass).build()).toList();
  }

  private TypeSpec.Builder createInputUtilClass() {
    return classBuilder(getInputUtilClassName())
        .addModifiers(FINAL)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build());
  }

    private TypeSpec.Builder createVajramImplClass() {
        return classBuilder(getVajramImplClassName());
    }

  public String getRequestClassName() {
    return requestClassName;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getInputUtilClassName() {
    return (vajramName.toLowerCase().endsWith("vajram")
            ? vajramName.substring(0, vajramName.length() - 6)
            : vajramName)
        + "InputUtil";
  }

    public String getVajramImplClassName() {
        return vajramName + "Impl";
    }

  private static String toJavaName(String inputName) {
    if (!inputName.contains("_")) {
      return inputName;
    }
    return LOWER_UNDERSCORE.to(LOWER_CAMEL, inputName);
  }

  private static TypeName optional(TypeName javaType) {
    return ParameterizedTypeName.get(ClassName.get(Optional.class), javaType);
  }

  private MethodSpec resolveInputOfDependency(Vajram<?> vajram) {
    return null;
  }

  private MethodSpec vajramLogic(Vajram<?> vajram) {
    String packageName = vajram.getClass().getPackageName();
    String vajramName = vajram.getClass().getSimpleName();
    JavaFile.builder(
        packageName,
        classBuilder(vajramName)
            .addMethod(resolveInputOfDependency(vajram))
            .addMethod(vajramLogic(vajram))
            .build());
    return null;
  }

//  private void codeGenVajramImpl() {
//    ImmutableCollection<VajramInputDefinition> inputDefinitions =
//        vajramInputFile.vajramInputsDef().allInputsDefinitions();
//  }

  private record FromAndTo(MethodSpec from, MethodSpec to) {}

  private record TypeAndName(TypeName typeName, Optional<Type> type) {

    public TypeAndName(TypeName typeName) {
      this(typeName, Optional.empty());
    }
  }
}
