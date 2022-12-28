package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.datatypes.TypeUtils.getJavaType;
import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.inputs.ValueOrError;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.EqualsAndHashCode;

public class VajramCodeGenerator {

  private final String packageName;
  private final String requestClassName;
  private final String vajramName;
  private final ClassLoader classLoader;
  private final Vajram<?> vajram;
  private final String inputUtilClassName;
  private final String allInputsClassName;

  public VajramCodeGenerator(Vajram<?> vajram, ClassLoader classLoader) {
    this.vajram = vajram;
    this.packageName = vajram.getClass().getPackageName();
    this.vajramName = vajram.getClass().getSimpleName();
    this.classLoader = classLoader;
    this.requestClassName = getRequestClassName(vajramName);
    this.inputUtilClassName = "InputUtil";
    this.allInputsClassName = "AllInputs";
  }

  private static String getRequestClassName(String vajramName) {
    return (vajramName.toLowerCase().endsWith("vajram")
            ? vajramName.substring(0, vajramName.length() - 6)
            : vajramName)
        + "Request";
  }

  public String codeGenVajramRequest() {
    List<? extends Input<?>> inputs =
        vajram.getInputDefinitions().stream()
            .filter(vajramInputDefinition -> vajramInputDefinition instanceof Input<?>)
            .map(vajramInputDefinition -> (Input<?>) vajramInputDefinition)
            .toList();
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
    @SuppressWarnings("rawtypes")
    Builder asMap =
        methodBuilder("asMap")
            .returns(new TypeToken<ImmutableMap<String, ValueOrError<?>>>() {}.getType())
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .addStatement(
                "$T builder = new $T<>();",
                new TypeToken<Map<String, ValueOrError<?>>>() {}.getType(),
                new TypeToken<HashMap>() {}.getType());
    TypeSpec.Builder builderClass =
        classBuilder("Builder")
            .addModifiers(PUBLIC, STATIC)
            .addAnnotation(EqualsAndHashCode.class)
            .addMethod(constructorBuilder().addModifiers(PRIVATE).build()); // <private Builder(){}>
    Set<String> inputNames = new LinkedHashSet<>();
    for (Input<?> input : inputs) {
      String inputJavaName = toJavaName(input.name());
      inputNames.add(inputJavaName);
      Type javaType = getJavaType(input.type());
      requestClass.addField(FieldSpec.builder(javaType, inputJavaName, PRIVATE, FINAL).build());
      builderClass.addField(FieldSpec.builder(javaType, inputJavaName, PRIVATE).build());
      requestConstructor.addParameter(ParameterSpec.builder(javaType, inputJavaName).build());
      requestConstructor.addStatement("this.$L = $L", inputJavaName, inputJavaName);
      requestClass.addMethod(getterCode(input, inputJavaName, javaType));

      asMap.addStatement(
          "builder.put($S, new $T<>($L()))", input.name(), ValueOrError.class, inputJavaName);

      builderClass.addMethod(
          // public inputName(){return this.inputName;}
          methodBuilder(inputJavaName)
              .addModifiers(PUBLIC)
              .returns(javaType)
              .addStatement("return this.$L", inputJavaName) // Return
              .build());

      builderClass.addMethod(
          // public inputName(Type inputName){this.inputName = inputName; return this;}
          methodBuilder(inputJavaName)
              .returns(builderClassType)
              .addModifiers(PUBLIC)
              .addParameter(ParameterSpec.builder(javaType, inputJavaName).build())
              .addStatement("this.$L = $L", inputJavaName, inputJavaName) // Set value
              .addStatement("return this", inputJavaName) // Return
              .build());
    }

    asMap.addStatement("return $T.copyOf(builder)", ImmutableMap.class);

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
    try {
      JavaFile.builder(
              packageName,
              requestClass
                  .addMethod(requestConstructor.build())
                  .addMethod(asMap.build())
                  .addType(builderClass.build())
                  .build())
          .build()
          .writeTo(writer);
    } catch (IOException ignored) {

    }
    return writer.toString();
  }

  private static MethodSpec getterCode(
      VajramInputDefinition<?> input, String inputJavaName, Type javaType) {
    return methodBuilder(inputJavaName)
        .returns(input.isOptional() ? optional(javaType) : javaType)
        .addModifiers(PUBLIC)
        .addCode(
            input.isOptional()
                // public Optional<Type> inputName(){
                //    return Optional.ofNullable(this.inputName);
                // }
                ? CodeBlock.builder()
                    .addStatement("return $T.ofNullable(this.$L)", Optional.class, inputJavaName)
                    .build()
                // public Type inputName(){return this.inputName;}
                : CodeBlock.builder().addStatement("return this.$L", inputJavaName).build())
        .build();
  }

  public String codeGenInputUtil() {
    if (vajram instanceof IOVajram<?>
        && vajram.getInputDefinitions().stream().anyMatch(VajramInputDefinition::needsModulation)) {
      return codeGenModulatedInputUtil();
    } else {
      return codeGenSimpleInputUtil();
    }
  }

  private String codeGenSimpleInputUtil() {
    TypeSpec.Builder inputUtilClass =
        classBuilder(inputUtilClassName)
            .addModifiers(FINAL)
            .addMethod(constructorBuilder().addModifiers(PRIVATE).build());
    TypeSpec.Builder allInputsClass =
        classBuilder(allInputsClassName)
            .addModifiers(FINAL, STATIC)
            .addAnnotation(EqualsAndHashCode.class);
    MethodSpec.Builder allInputsConstructor = constructorBuilder();

    vajram
        .getInputDefinitions()
        .forEach(
            inputDef -> {
              String inputJavaName = toJavaName(inputDef.name());
              Type javaType = getJavaType(inputDef.type());
              allInputsClass.addField(javaType, inputJavaName, PRIVATE, FINAL);
              allInputsConstructor.addParameter(javaType, inputJavaName);
              allInputsConstructor.addStatement("this.$L = $L", inputJavaName, inputJavaName);
              allInputsClass.addMethod(getterCode(inputDef, inputJavaName, javaType));
            });

    StringWriter writer = new StringWriter();
    try {
      JavaFile.builder(
              packageName,
              inputUtilClass
                  .addType(allInputsClass.addMethod(allInputsConstructor.build()).build())
                  .build())
          .build()
          .writeTo(writer);
    } catch (IOException ignored) {

    }
    return writer.toString();
  }

  private String codeGenModulatedInputUtil() {
    return null;
  }

  public String getRequestClassName() {
    return requestClassName;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getInputUtilClassName() {
    return inputUtilClassName;
  }

  private static String toJavaName(String inputName) {
    return LOWER_UNDERSCORE.to(LOWER_CAMEL, inputName);
  }

  private static Type optional(Type javaType) {
    return new ParameterizedType() {
      @Override
      public Type[] getActualTypeArguments() {
        return new Type[] {javaType};
      }

      @Override
      public Type getRawType() {
        return Optional.class;
      }

      @Override
      public Type getOwnerType() {
        return null;
      }
    };
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

  private void codeGenVajramImpl(Vajram<?> vajram) {
    ImmutableCollection<VajramInputDefinition> inputDefinitions = vajram.getInputDefinitions();
  }
}
