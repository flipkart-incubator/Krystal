package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.datatypes.TypeUtils.getJavaType;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.CONVERTER;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getInputUtilClassName;
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
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.DOT;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.COMMA;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.Vajrams;
import com.flipkart.krystal.vajram.codegen.models.AbstractInput;
import com.flipkart.krystal.vajram.codegen.models.DependencyDef;
import com.flipkart.krystal.vajram.codegen.models.InputDef;
import com.flipkart.krystal.vajram.codegen.models.VajramDef;
import com.flipkart.krystal.vajram.codegen.models.VajramDependencyDef;
import com.flipkart.krystal.vajram.codegen.models.VajramInputFile;
import com.flipkart.krystal.vajram.codegen.models.VajramInputsDef;
import com.flipkart.krystal.vajram.codegen.utils.CodegenUtils;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.inputs.BindFrom;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputSource;
import com.flipkart.krystal.vajram.inputs.InputValuesAdaptor;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.modulation.UnmodulatedInput;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@Slf4j
public class VajramCodeGenerator {

    private static final List<Class> SIMPLE_TYPES = ImmutableList.of(String.class, Integer.class, Double.class, Boolean.class,
            Character.class, Byte.class, Float.class, Long.class, Void.class);
    private static final Pattern COMPILE = Pattern.compile("\\.");
    public static final char DOT_SEPARATOR = '.';
    public static final String RESPONSE = "response";
    public static final String VARIABLE = "variable";
    public static final String DEP_RESPONSE = "depResponse";
    public static final String REQUEST = "request";
    public static final String RESPONSES_SUFFIX = "Responses";
    public static final String RESPONSE_SUFFIX = "Response";
    public static final String METHOD_GET_INPUTS_CONVERTOR = "getInputsConvertor";
    public static final String METHOD_EXECUTE = "execute";
    public static final String METHOD_RESOLVE_INPUT_OF_DEPENDENCY = "resolveInputOfDependency";
    public static final String METHOD_EXECUTE_COMPUTE = "executeCompute";
    public static final String GET_INPUT_DEFINITIONS = "getInputDefinitions";
    public static final String ALL_INPUTS = "AllInputs";
    public static final String INPUTS_LIST = "inputsList";
    private final String packageName;
    private final String requestClassName;
    private final VajramInputFile vajramInputFile;
    private final String vajramName;
    private final Map<String, VajramDef> vajramDefs;
    private final ImmutableCollection<VajramInputDefinition> inputDefinitions;
    private final Map<String, VajramInputDefinition> inputDefsMap;

    public String getVajramName() {
        return vajramName;
    }
    public VajramCodeGenerator(VajramInputFile vajramInputFile, Map<String, VajramDef> vajramDefs) {
        this.vajramInputFile = vajramInputFile;
        Path filePath = vajramInputFile.inputFilePath().relativeFilePath();
        Path parentDir = filePath.getParent();
        this.vajramName = vajramInputFile.vajramName();
        this.packageName =
            IntStream.range(0, parentDir.getNameCount())
                .mapToObj(i -> parentDir.getName(i).toString())
                .collect(Collectors.joining(DOT));
        this.requestClassName = CodegenUtils.getRequestClassName(vajramName);
        this.vajramDefs = Collections.unmodifiableMap(vajramDefs);
        //InputDefinitions loaded from vajram.yaml file
        this.inputDefinitions = vajramInputFile.vajramInputsDef().allInputsDefinitions();
        // All the VajramInputDefinitions map with name as key
        this.inputDefsMap = inputDefinitions.stream()
              .collect(Collectors.toMap(VajramInputDefinition::name, Function.identity(),
                      (o1, o2) -> o1, LinkedHashMap::new)); // need ordered map for dependencies
    }

    /**
     * Method to generate VajramImpl class
     *  Input dependency code gen
     *  Resolve method code gen
     *  Vajram logic code gen
     *    Compute vajram execute
     *    IO vajram executeBlocking
     * @param classLoader
     * @return Class code as string
     */
  public String codeGenVajramImpl(ClassLoader classLoader) {
      final TypeSpec.Builder vajramImplClass = createVajramImplClass();
      List<MethodSpec> methodSpecs = new ArrayList<>();
      // Add superclass
      vajramImplClass.addModifiers(PUBLIC)
              .superclass(ClassName.bestGuess(vajramName).box()).build();

      // Map of all the resolved variables to the methods resolving them
      Map<String, List<Method>> resolverMap = new HashMap<>();
      VajramDef vajramDef = vajramDefs.get(vajramName);
      vajramDef.resolveMethods().forEach(method -> resolverMap.put(method.getAnnotation(Resolve.class).value(), new ArrayList<>()));
      for (Method resolve : vajramDef.resolveMethods()) {
          String key = resolve.getAnnotation(Resolve.class).value();
          resolverMap.get(key).add(resolve);
      }

      // Initialize few common attributes and data structures
      final ClassName inputsNeedingModulation = ClassName.get(vajramDef.packageName(),
              CodegenUtils.getInputUtilClassName(vajramDef.vajramName()), "InputsNeedingModulation");
      final ClassName commonInputs = ClassName.get(vajramDef.packageName(),
              CodegenUtils.getInputUtilClassName(vajramDef.vajramName()), "CommonInputs");
      final Type vajramResponseType = ((ParameterizedType) ((Class<? extends Vajram>) vajramDef.vajramClass())
              .getGenericSuperclass()).getActualTypeArguments()[0];

      MethodSpec inputDefinitionsMethod = createInputDefinitions(classLoader);
      methodSpecs.add(inputDefinitionsMethod);
      Optional<MethodSpec> inputResolverMethod = createResolvers(resolverMap);
      inputResolverMethod.ifPresent(methodSpecs::add);

      if (vajramDef.vajramClass().getSuperclass() == IOVajram.class) {
          methodSpecs.add(createIOVajramExecuteMethod(inputsNeedingModulation, commonInputs, vajramResponseType));
          methodSpecs.add(createInputConvertersMethod(inputsNeedingModulation, commonInputs));
      } else {
          methodSpecs.add(createComputeVajramExecuteMethod(vajramResponseType, resolverMap));
      }

      StringWriter writer = new StringWriter();
      try {
          JavaFile.builder(
                          packageName,
                          vajramImplClass.addMethods(methodSpecs)
                                  .build())
                  .indent("  ")
                  .build()
                  .writeTo(writer);
      } catch (IOException ignored) {

      }
      return writer.toString();
  }

    /**
     * Method to generate "executeCompute" function code for ComputeVajrams
     * Supported DataAccessSpec => VajramID only.
     * @param vajramResponseType Vajram response type
     * @param resolverMap Map of all the resolved variables to the methods resolving them
     * @return generated code for "executeCompute" {@link MethodSpec}
     */
    private MethodSpec createComputeVajramExecuteMethod(Type vajramResponseType,
            Map<String, List<Method>> resolverMap) {
        Builder executeBuilder = methodBuilder(METHOD_EXECUTE_COMPUTE).addModifiers(PUBLIC)
                .addParameter(ParameterizedTypeName.get(ImmutableList.class, Inputs.class), INPUTS_LIST)
                .returns(ParameterizedTypeName.get(ClassName.get(ImmutableMap.class), ClassName.get(Inputs.class),
                        ClassName.bestGuess(vajramResponseType.getTypeName())))
                .addAnnotation(Override.class);
        CodeBlock.Builder returnBuilder = CodeBlock.builder()
                .add("""
                    return inputsList.stream().collect(
                         ImmutableMap.toImmutableMap(java.util.function.Function.identity(),
                         element -> {
                    """);
        List<CodeBlock> inputCodeBlocks = new ArrayList<>();
        inputDefsMap.values().forEach(inputDef -> {
            if (resolverMap.containsKey(inputDef.name())) {
                CodeBlock.Builder codeBlock = CodeBlock.builder();
                if (inputDef instanceof Dependency<?> inputDefDependency) {
                    DataAccessSpec dataAccessSpec = inputDefDependency.dataAccessSpec();
                    if (dataAccessSpec instanceof VajramID vajramID) {
                        String depVajramClass = vajramID.className()
                                .orElseThrow(() -> new RuntimeException("Vajram class missing in VajramInputDefinition"));
                        String[] splits = COMPILE.split(depVajramClass);
                        String depPackageName = Arrays.stream(splits, 0, splits.length - 1)
                                .collect(Collectors.joining(DOT));
                        String depRequestClass = CodegenUtils.getRequestClassName(splits[splits.length-1]);
                        VajramDef vajramDef = vajramDefs.get(splits[splits.length-1]);
                        final Type typeArgument = ((ParameterizedType) ((Class<? extends Vajram>) vajramDef.vajramClass())
                                .getGenericSuperclass()).getActualTypeArguments()[0];
                        final String variableName = CodegenUtils.toJavaName(inputDef.name());
                        if (resolverMap.get(inputDef.name()).size() > 1) {
                            final String depVariableName = variableName + RESPONSES_SUFFIX;
                            // handle iterable
                            codeBlock.addNamed("""
                                 DependencyResponse<$request:T, $response:T> $depResponse:L =
                                      new DependencyResponse<>(
                                          element.<$response:T>getDepValue($variable:S).values().entrySet().stream()
                                              .collect(
                                                  ImmutableMap.toImmutableMap(
                                                      e -> $request:T.from(e.getKey()), java.util.Map.Entry::getValue)));""",
                                    ImmutableMap.of(REQUEST, ClassName.get(depPackageName, depRequestClass),
                                    RESPONSE, ClassName.bestGuess(typeArgument.getTypeName()), VARIABLE, variableName,
                                    DEP_RESPONSE, depVariableName));
                            inputCodeBlocks.add(CodeBlock.builder().add(depVariableName).build());
                        } else if (resolverMap.get(inputDef.name()).size() == 1) {
                            final String depVariableName = variableName + RESPONSE_SUFFIX;
                            codeBlock.addStatement("ImmutableMap<Inputs, ValueOrError<$T>> $L = \n"
                                    + " element.getInputValueOrThrow($S)", ClassName.bestGuess(typeArgument.getTypeName()),
                                   variableName, inputDef.name());
                            codeBlock.addNamed("""
                                    DependencyResponse<$request:T, $response:T> $depResponse:L =
                                        new DependencyResponse<>(
                                            $variable:L.entrySet().stream()
                                                .collect(
                                                    ImmutableMap.toImmutableMap(
                                                        e -> $request:T.from(e.getKey()), java.util.Map.Entry::getValue)));
                                    """, ImmutableMap.of(REQUEST, ClassName.get(depPackageName, depRequestClass),
                                    RESPONSE, ClassName.bestGuess(typeArgument.getTypeName()), VARIABLE, variableName,
                                    DEP_RESPONSE, depVariableName));
                            inputCodeBlocks.add(CodeBlock.builder().add(depVariableName).build());
                        }
                    }
                }
                returnBuilder.add(codeBlock.build());
            } else {
                // call vajram logic method with all input values
                if (inputDef.isMandatory()) {
                    inputCodeBlocks.add(CodeBlock.builder().add("element.getInputValueOrThrow($S)", inputDef.name()).build());
                } else {
                    inputCodeBlocks.add(CodeBlock.builder().add("element.getInputValueOrDefault($S, null)", inputDef.name()).build());
                }
            }
        });
        returnBuilder.add("return $L(new $T.AllInputs(\n", vajramDefs.get(vajramName).vajramLogic().getName(),
                ClassName.get(packageName, CodegenUtils.getInputUtilClassName(vajramName)));
        // merge the code blocks for inputs
        for (int i = 0; i<inputCodeBlocks.size(); i++) {
            // for formatting
            returnBuilder.add("\t\t");
            returnBuilder.add(inputCodeBlocks.get(i));
            if (i != inputCodeBlocks.size()-1) {
                returnBuilder.add(",\n");
            }
        }
        returnBuilder.add("));\n");
        returnBuilder.add("}));\n");
        executeBuilder.addCode(returnBuilder.build());

        return executeBuilder.build();
    }

    /**
     * Method to generate "getInputsConvertor" function
     * @param inputsNeedingModulation Generated Vajram specific InputUtil.InputsNeedingModulation class
     * @param commonInputs Generated Vajram specific InputUtil.CommonInputs class
     * @return {@link MethodSpec}
     */
    private MethodSpec createInputConvertersMethod(ClassName inputsNeedingModulation, ClassName commonInputs) {
        Builder inputConvertersBuilder = methodBuilder(METHOD_GET_INPUTS_CONVERTOR).addModifiers(PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(InputsConverter.class), inputsNeedingModulation, commonInputs))
                .addAnnotation(Override.class);
        inputConvertersBuilder.addCode(CodeBlock.builder().addStatement("return $T.CONVERTER", ClassName.get(packageName,
                CodegenUtils.getInputUtilClassName(vajramName))).build());
        return inputConvertersBuilder.build();
    }

    /**
     * Method to generate "execute" function code for IOVajrams
     * @param inputsNeedingModulation Generated Vajramspecific InputUtil.InputsNeedingModulation class
     * @param commonInputs Generated Vajram specific InputUtil.CommonInputs class
     * @param vajramResponseType Vajram response type
     * @return generated code for "execute" {@link MethodSpec}
     */
    private MethodSpec createIOVajramExecuteMethod(
            ClassName inputsNeedingModulation, ClassName commonInputs, Type vajramResponseType) {

                Builder executeMethodBuilder = methodBuilder(METHOD_EXECUTE).addModifiers(PUBLIC)
                .addParameter(ParameterizedTypeName.get(ImmutableList.class, Inputs.class), INPUTS_LIST)
                .returns(
                        ParameterizedTypeName.get(ClassName.get(ImmutableMap.class), ClassName.get(Inputs.class),
                                ParameterizedTypeName.get(ClassName.get(CompletableFuture.class),
                                        ClassName.bestGuess(vajramResponseType.getTypeName()))))
                .addAnnotation(Override.class);

        String codeBlock = """
                    Map<$inputModulation:T, Inputs> mapping = new HashMap<>();
                    List<$inputModulation:T> ims = new ArrayList<>();
                    $commonInput:T commonInputs = null;
                    for (Inputs inputs : inputsList) {
                      UnmodulatedInput<$inputModulation:T, $commonInput:T> allInputs =
                          getInputsConvertor().apply(inputs);
                      commonInputs = allInputs.commonInputs();
                      $inputModulation:T im = allInputs.inputsNeedingModulation();
                      mapping.put(im, inputs);
                      ims.add(im);
                    }
                    Map<Inputs, CompletableFuture<$returnType:T>> returnValue = new LinkedHashMap<>();
                                
                    if (commonInputs != null) {
                      var results = $vajramLogicMethod:L(new ModulatedInput<>(ImmutableList.copyOf(ims), commonInputs));
                      results.forEach((im, future) -> returnValue.put(mapping.get(im), future));
                    }
                    return ImmutableMap.copyOf(returnValue);
                """;
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        Map<String, Object> valueMap =  new HashMap<>();
        valueMap.put("inputModulation", inputsNeedingModulation);
        valueMap.put("commonInput", commonInputs);
        valueMap.put("returnType", ClassName.bestGuess(vajramResponseType.getTypeName()));
        valueMap.put("vajramLogicMethod", vajramDefs.get(vajramName).vajramLogic().getName());
        codeBuilder.addNamed(codeBlock, valueMap);
        executeMethodBuilder.addCode(codeBuilder.build());
        return executeMethodBuilder.build();
    }

    /**
     * Method to generate "resolveInputOfDependency" function code for Vajrams.
     * If there are no resolvers defined in the Vajram, {@link Optional}.empty() is returned.
     * @param resolverMap Map of all the resolved variables to the methods resolving them
     * @return generated code for "resolveInputOfDependency" {@link MethodSpec}
     */
    public Optional<MethodSpec> createResolvers(Map<String, List<Method>> resolverMap) {
      String dependency = "dependency";
      Builder resolveInputsBuilder = methodBuilder(METHOD_RESOLVE_INPUT_OF_DEPENDENCY).addModifiers(
                      PUBLIC).addParameter(String.class, dependency)
              .addParameter(ParameterizedTypeName.get(ImmutableSet.class, String.class), "resolvableInputs").addParameter(Inputs.class, "inputs").returns(
                      ParameterizedTypeName.get(ClassName.get(DependencyCommand.class), ClassName.get(Inputs.class)));
      VajramDef vajramDef = vajramDefs.get(vajramName);
      if (Objects.nonNull(vajramDef)) {
          resolveInputsBuilder.beginControlFlow("switch ($L) ", dependency);
          if (vajramDef.resolveMethods().isEmpty()) {
              return Optional.empty();
          }
          // get all resolved variable names
          List<String> resolvedVariables = vajramDef.resolveMethods().stream()
                  .map(method -> method.getAnnotation(Resolve.class).value()).toList();

          resolverMap.forEach((variable, methods) -> {
              CodeBlock.Builder caseBuilder = CodeBlock.builder().beginControlFlow("case $S -> ", variable);
              methods.forEach(method -> {
                  Resolve resolve = method.getAnnotation(Resolve.class);
                  String[] inputs = resolve.inputs();
                  // TODO : confirm if only 1 param will be there per method??
                  Parameter methodParam = method.getParameters()[0];
                  if (inputs.length == 1) {
                      CodeBlock.Builder ifBlockBuilder = buildSingleInputResolver(resolvedVariables, resolverMap,
                              method, inputs, methodParam);
                      caseBuilder.add(ifBlockBuilder.build());
                  }
                  // TODO : handle multiple inputs
              });
              caseBuilder.endControlFlow();
              resolveInputsBuilder.addCode(caseBuilder.build());
          });
          resolveInputsBuilder.endControlFlow();
          resolveInputsBuilder.addStatement("throw new IllegalArgumentException()");
      } else {
          resolveInputsBuilder.addStatement("throw new IllegalArgumentException()");
      }
      return Optional.of(resolveInputsBuilder.build());
  }

    /**
     * Method to generate resolver code for input binding
     * @param resolvedVariables all resolved variable names
     * @param resolverMap Map of all the resolved variables to the methods resolving them
     * @param method Vajram resolve method
     * @param inputs Resolve annotation inputs
     * @param methodParam Resolve method parameter
     * @return {@link CodeBlock.Builder} with resolver code
     */
    private CodeBlock.Builder buildSingleInputResolver(List<String> resolvedVariables, Map<String, ? extends List<Method>> resolverMap, Method method, String[] inputs,
            Parameter methodParam) {
        // check if the input is satisfied by input or other resolved variables
        String bindParamName = methodParam.getAnnotation(BindFrom.class).value();
        Parameter parameter = stream(method.getParameters()).filter(
                param -> inputDefsMap.containsKey(bindParamName) ||
                resolvedVariables.contains(bindParamName)).findFirst()
                .orElseThrow(() -> new RuntimeException("Parameter binding incorrect for input - " + inputs[0]));
        CodeBlock.Builder ifBlockBuilder = CodeBlock.builder();
        ifBlockBuilder.beginControlFlow("if ($T.of($S).equals(resolvableInputs))", Set.class,
                inputs[0]);
        // check if the bind param has multiple resolvers
        if (resolverMap.containsKey(bindParamName) && resolverMap.get(bindParamName).size() > 1) {
            buildResolverForMultipleBindings(method, inputs, bindParamName, ifBlockBuilder);
        } else {
            buildResolverForSingleBindings(method, inputs, parameter, ifBlockBuilder);
        }
        ifBlockBuilder.endControlFlow();
        return ifBlockBuilder;
    }

    /**
     * Method to generate resolver code for variables having single resolver.
     * @param method Resolve method
     * @param inputs Resolve inputs
     * @param parameter Resolve method parameter
     * @param ifBlockBuilder {@link CodeBlock.Builder}
     */
    private void buildResolverForSingleBindings(Method method, String[] inputs, Parameter parameter,
            CodeBlock.Builder ifBlockBuilder) {
        String variableName = parameter.getName();
        boolean controlFLowStarted = false;
        // Identify resolve method return type
        final Type returnType = method.getReturnType();
        if (method.getReturnType() == DependencyCommand.class) {
            // Method returns DependencyCommand so add code block for skip() operation
            final Type actualTypeArgument = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
            ifBlockBuilder.addStatement("DependencyCommand<$T> $L = super.$L(inputs.getInputValueOrThrow($S))",
                    actualTypeArgument, variableName, method.getName(), parameter.getAnnotation(BindFrom.class).value());
            ifBlockBuilder.beginControlFlow("if($L instanceof DependencyCommand.Skip<$T> skip)",
                    parameter.getName(), actualTypeArgument);
            ifBlockBuilder.addStatement("\t return skip.cast()");
            ifBlockBuilder.add("} else {\n\t");
            controlFLowStarted = true;
        } else {
            // wrap with dependency command
            ifBlockBuilder.addStatement("$T value = super.$L(inputs.getInputValueOrThrow($S))",
                    returnType, method.getName(), parameter.getAnnotation(BindFrom.class).value());
            ifBlockBuilder.addStatement("""
                    DependencyCommand<$T> $L =
                                  Optional.ofNullable(value)
                                      .map(DependencyCommand::executeWith)
                                      .orElse(DependencyCommand.executeWith(null))""",
                    returnType, variableName);
        }
        Class<?> klass = Primitives.wrap(parameter.getType());
        if (SIMPLE_TYPES.contains(klass)) {
            // handle simple execute with primitive return types
            ifBlockBuilder.addStatement("return DependencyCommand.executeWith(new Inputs(\n "
                            + "ImmutableMap.of($S, ValueOrError.withValue($L))))",
                    inputs[0], variableName);
        } else if (((ParameterizedType) klass.getGenericInterfaces()[0]).getRawType() == Collection.class){
             String code = """
                return DependencyCommand.multiExecuteWith(
                    $L.inputs().stream()
                        .map(
                            elements ->
                                new Inputs(
                                    ImmutableMap.of($S, ValueOrError.withValue(elements))))
                    .toList())""";
            ifBlockBuilder.addStatement(code, variableName, parameter.getAnnotation(BindFrom.class).value());
        } else {
            // handle the object return type
            ifBlockBuilder.addStatement("return DependencyCommand.executeWith($L.toInputValues())",
                    variableName);
        }
        if (controlFLowStarted) {
            ifBlockBuilder.endControlFlow();
        }
    }

    /**
     * Method to generate resolver code for variables having multiple resolvers.
     * @param method Resolve method
     * @param inputs Resolve inputs
     * @param bindParamName Resolve method parameter
     * @param ifBlockBuilder
     */
    private void buildResolverForMultipleBindings(Method method, String[] inputs,
            String bindParamName, CodeBlock.Builder ifBlockBuilder) {
        VajramInputDefinition vajramInputDef = inputDefsMap.get(bindParamName);
        if (vajramInputDef instanceof Dependency<?> inputDefDependency) {
            DataAccessSpec dataAccessSpec = inputDefDependency.dataAccessSpec();
            if (dataAccessSpec instanceof VajramID vajramID) {
                String vajramClass = vajramID.className()
                        .orElseThrow(() -> new RuntimeException("Vajram class missing in vajram input deifinition"));
                String[] splits = COMPILE.split(vajramClass);
                String depPackageName = Arrays.stream(splits, 0, splits.length - 1)
                        .collect(Collectors.joining(DOT));
                String requestClass = CodegenUtils.getRequestClassName(splits[splits.length-1]);
                String variableName = "computedValues";
                ifBlockBuilder.addStatement("""
                                DependencyResponse<$1T, $2T> $3L =\s
                                 new DependencyResponse<>(inputs.<$4T>getDepValue($5S)
                                      .values().entrySet().stream()
                                      .collect(ImmutableMap.toImmutableMap(e -> $6T.from(e.getKey()), 
                                      java.util.Map.Entry::getValue)))""",
                                ClassName.get(depPackageName, requestClass), ClassName.get(Primitives.wrap(method.getReturnType())), variableName,
                                ClassName.get(Primitives.wrap(method.getReturnType())),
                        bindParamName, ClassName.get(depPackageName, requestClass));
                ifBlockBuilder.addStatement("""
                    return DependencyCommand.multiExecuteWith($L.values().stream()
                        .filter(element -> element.value().isPresent())
                        .map(element -> element.value().get())
                        .map(super::$L)
                        .map(t -> ValueOrError.withValue((Object) t))
                        .map(voe -> new Inputs(ImmutableMap.of($S, voe)))
                        .collect(ImmutableList.toImmutableList()))""",
                    variableName, method.getName(), inputs[0]
                );
            }
        }
    }

    /**
     * Method to generate code for "getInputDefinitions" function
     * @param classLoader : Classloader with generated Vajram models
     * @return {@link MethodSpec}
     */
    private MethodSpec createInputDefinitions(ClassLoader classLoader) {
        // Method : getInputDefinitions
        Builder inputDefinitionsBuilder = methodBuilder(GET_INPUT_DEFINITIONS)
                .addModifiers(PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(ImmutableList.class),
                        ClassName.get(VajramInputDefinition.class)));

        Collection<CodeBlock> codeBlocks = new ArrayList<>(inputDefinitions.size());
        // Input and Dependency code block
        inputDefinitions.forEach( vajramInputDefinition ->  {
            CodeBlock.Builder inputDefBuilder = CodeBlock.builder();

            if (vajramInputDefinition instanceof Input input) {
                buildVajramInput(inputDefBuilder, input);
            } else if (vajramInputDefinition instanceof Dependency dependency) {
                buildVajramDependency(classLoader, inputDefBuilder, dependency);
            }
            codeBlocks.add(inputDefBuilder.build());
        });
        CodeBlock.Builder returnCode = CodeBlock.builder()
                .add("return ImmutableList.of(\n").add(CodeBlock.join(codeBlocks, ",\n\t")).add("\n);");
        inputDefinitionsBuilder.addCode(returnCode.build());

        return inputDefinitionsBuilder.build();
    }

    /**
     * Method to generate VajramDependency code blocks
     * @param classLoader : Classloader with generated Vajram models
     * @param inputDefBuilder : {@link CodeBlock.Builder}
     * @param dependency : Vajram dependency
     */
    private static void buildVajramDependency(ClassLoader classLoader,
            CodeBlock.Builder inputDefBuilder, Dependency dependency) {
        inputDefBuilder.add("Dependency.builder()")
                .add(".name($S)", dependency.name());
        DataAccessSpec dataAccessSpec = dependency.dataAccessSpec();
        if (dataAccessSpec instanceof VajramID vajramID) {
            String code = ".dataAccessSpec($1T.vajramID($2S))";
            if (vajramID.className().isPresent()) {
                try {
                    Optional<String> vajramId = Vajrams.getVajramIdString(
                            (Class<? extends Vajram>) classLoader.loadClass(vajramID.className().get()));
                    vajramId.ifPresent(s -> inputDefBuilder.add(code, ClassName.get(VajramID.class), s));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                inputDefBuilder.add(code, ClassName.get(VajramID.class), vajramID.vajramId());
            }
        }
        if(dependency.isMandatory()) {
            inputDefBuilder.add(".isMandatory()");
        }
        // build() as last step
        inputDefBuilder.add(".build()");
    }

    /**
     * Method to generate VajramInput code blocks
     * @param inputDefBuilder : {@link CodeBlock.Builder}
     * @param input :  Vajram Input
     */
    private static void buildVajramInput(CodeBlock.Builder inputDefBuilder, Input input) {
        inputDefBuilder.add("Input.builder()")
               .add(".name($S)", input.name());
        // handle input type
        Set<InputSource> inputSources = input.sources();
        if (!inputSources.isEmpty()) {
            inputDefBuilder.add(".sources(");
            ClassName className = ClassName.get(InputSource.class);
            String sources = inputSources.stream().map(inputSource -> {
                if (inputSource == InputSource.CLIENT) {
                    return "InputSource.CLIENT";
                } else if (inputSource == InputSource.SESSION) {
                    return "InputSource.SESSION";
                }
                else {
                    throw new IllegalArgumentException("Incorrect source defined in vajram config");
                }
            }).collect(Collectors.joining(COMMA));
            inputDefBuilder.add(sources).add(")");
        }
        // handle data type
        DataType dataType = input.type();
        inputDefBuilder.add(".type(");
        if (dataType instanceof JavaType<?> javaType) {
            // custom handling
            ClassName className ;
            if (!javaType.enclosingClasses().isEmpty() || javaType.simpleName().isPresent()) {
                assert javaType.packageName().isPresent();
                className =
                        ClassName.get(javaType.packageName().get(),
                                String.join(DOT, javaType.enclosingClasses()),
                                javaType.simpleName().get());
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
        if (input.isMandatory()) {
           inputDefBuilder.add(".isMandatory()");
       }
        // last line
        inputDefBuilder.add(".build()");
    }

    public String codeGenVajramRequest() {
        VajramInputsDef vajramInputsDef = vajramInputFile.vajramInputsDef();
        ImmutableList<InputDef> inputDefs = vajramInputsDef.inputs();
        Builder requestConstructor = constructorBuilder().addModifiers(PRIVATE);
        ClassName builderClassType = ClassName.get(packageName + DOT_SEPARATOR + requestClassName, "Builder");
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
                new TypeToken<Map<String, InputValue<Object>>>() {}.getType(),
                new TypeToken<HashMap>() {}.getType());
    Builder fromInputValues =
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
      ClassName className;
      if (simpleName.isPresent()) {
        List<String> classNames =
            Stream.concat(javaType.enclosingClasses().stream(), Stream.of(simpleName.get()))
                .toList();
        className =
            ClassName.get(
                javaType.packageName().orElse(""),
                classNames.get(0),
                classNames.subList(1, classNames.size()).toArray(String[]::new));
      } else {
        className = ClassName.bestGuess(javaType.className());
      }
      if (!javaType.typeParameters().isEmpty()) {
        return new TypeAndName(
            ParameterizedTypeName.get(
                className,
                javaType.typeParameters().stream()
                    .map(VajramCodeGenerator::getTypeName)
                    .map(TypeAndName::typeName)
                    .toArray(TypeName[]::new)));
      } else {
        return new TypeAndName(className);
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
        classBuilder(ALL_INPUTS)
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
      int lastDotIndex = depVajramClass.lastIndexOf(DOT_SEPARATOR);
      String depRequestClass = CodegenUtils.getRequestClassName(depVajramClass.substring(lastDotIndex + 1));
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
              ClassName.get(packageName, getInputUtilClassName(vajramName), imClassName));
      TypeSpec.Builder inputsNeedingModulation =
          classBuilder(imClassName)
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
              ClassName.get(packageName, getInputUtilClassName(vajramName), ciClassName));
      TypeSpec.Builder commonInputs =
          classBuilder(ciClassName)
              .addModifiers(STATIC)
              .addSuperinterface(InputValuesAdaptor.class)
              .addAnnotations(recordAnnotations())
              .addMethod(ciFromAndTo.to())
              .addMethod(ciFromAndTo.from());
      ClassName imType = ClassName.get(packageName, getInputUtilClassName(vajramName), imClassName);
      ClassName ciType = ClassName.get(packageName, getInputUtilClassName(vajramName), ciClassName);
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
          FieldSpec.builder(parameterizedTypeName, CONVERTER)
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
    return classBuilder(CodegenUtils.getInputUtilClassName(vajramName))
        .addModifiers(FINAL)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build());
  }

    private TypeSpec.Builder createVajramImplClass() {
        return classBuilder(CodegenUtils.getVajramImplClassName(vajramName));
    }

  public String getRequestClassName() {
    return requestClassName;
  }

  public String getPackageName() {
    return packageName;
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

  private record FromAndTo(MethodSpec from, MethodSpec to) {}

  private record TypeAndName(TypeName typeName, Optional<Type> type) {

    public TypeAndName(TypeName typeName) {
      this(typeName, Optional.empty());
    }
  }
}
