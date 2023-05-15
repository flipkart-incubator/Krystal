package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.datatypes.TypeUtils.getJavaType;
import static com.flipkart.krystal.vajram.Vajrams.getVajramIdString;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.COMMA;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.CONVERTER;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.DOT;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getAllInputsClassname;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getCommonInputsClassname;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getInputModulationClassname;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getInputUtilClassName;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getVajramImplClassName;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.ARRAY_LIST;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.COMMON_INPUT;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.COM_FUTURE;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.DEP_COMMAND;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.DEP_RESP;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.DEP_RESPONSE;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.DOT_SEPARATOR;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.FUNCTION;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.GET_INPUT_DEFINITIONS;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.HASH_MAP;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.IM_LIST;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.IM_MAP;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUTS;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUTS_LIST;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUT_DEFINITIONS_VAR;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUT_MODULATION;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUT_MODULATION_CODE_BLOCK;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUT_MODULATION_FUTURE_CODE_BLOCK;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUT_SRC;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.LINK_HASH_MAP;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.LIST;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.MAP;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.METHOD_EXECUTE;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.METHOD_EXECUTE_COMPUTE;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.METHOD_GET_INPUTS_CONVERTOR;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.METHOD_RESOLVE_INPUT_OF_DEPENDENCY;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.MOD_INPUT;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.MULTI_EXEC_CMD;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.REQUEST;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.RESOLVABLE_INPUTS;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.RESPONSE;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.RESPONSES_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.RETURN_TYPE;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.SINGLE_EXEC_CMD;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.SKIPPED_EXCEPTION;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.UNMOD_INPUT;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.VAJRAM_LOGIC_METHOD;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.VAL_ERR;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.VARIABLE;
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
import com.flipkart.krystal.utils.SkippedExecutionException;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.codegen.models.AbstractInput;
import com.flipkart.krystal.vajram.codegen.models.DependencyDef;
import com.flipkart.krystal.vajram.codegen.models.InputDef;
import com.flipkart.krystal.vajram.codegen.models.ParsedVajramData;
import com.flipkart.krystal.vajram.codegen.models.VajramDependencyDef;
import com.flipkart.krystal.vajram.codegen.models.VajramInputFile;
import com.flipkart.krystal.vajram.codegen.models.VajramInputsDef;
import com.flipkart.krystal.vajram.codegen.utils.CodegenUtils;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.DependencyCommand.MultiExecute;
import com.flipkart.krystal.vajram.inputs.DependencyCommand.SingleExecute;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputSource;
import com.flipkart.krystal.vajram.inputs.InputValuesAdaptor;
import com.flipkart.krystal.vajram.inputs.Resolve;
import com.flipkart.krystal.vajram.inputs.Using;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.UnmodulatedInput;
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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@Slf4j
public class VajramCodeGenerator {

  public static final Pattern COMPILE = Pattern.compile("\\.");

  private final String packageName;
  private final String requestClassName;
  private final VajramInputFile vajramInputFile;
  private final String vajramName;
  private final Map<String, ParsedVajramData> vajramDefs;
  private final Map<String, ImmutableList<VajramInputDefinition>> vajramInputsDefinitions;
  private final Map<String, VajramInputDefinition> inputDefsMap;
  private final Map<String, ClassName> clsDeps = new HashMap<>();
  private final boolean needsModulation;

  public String getVajramName() {
    return vajramName;
  }

  public VajramCodeGenerator(
      VajramInputFile vajramInputFile,
      Map<String, ParsedVajramData> vajramDefs,
      Map<String, ImmutableList<VajramInputDefinition>> vajramInputsDefinitions) {
    this.vajramInputFile = vajramInputFile;
    Path filePath = vajramInputFile.inputFilePath().relativeFilePath();
    Path parentDir = filePath.getParent();
    this.vajramName = vajramInputFile.vajramName();
    this.packageName =
        IntStream.range(0, parentDir.getNameCount())
            .mapToObj(i -> parentDir.getName(i).toString())
            .collect(Collectors.joining(DOT));
    this.requestClassName = CodegenUtils.getRequestClassName(vajramName);
    // All parsed Vajram data loaded from all Vajram class files with vajram name as key
    this.vajramDefs = Collections.unmodifiableMap(vajramDefs);
    // All InputDefinitions loaded from all vajram.yaml file with vajram name as key
    this.vajramInputsDefinitions = Collections.unmodifiableMap(vajramInputsDefinitions);
    // All the present Vajram -> VajramInputDefinitions map with name as key
    this.inputDefsMap =
        vajramInputFile.vajramInputsDef().allInputsDefinitions().stream()
            .collect(
                Collectors.toMap(
                    VajramInputDefinition::name,
                    Function.identity(),
                    (o1, o2) -> o1,
                    LinkedHashMap::new)); // need ordered map for dependencies
    this.needsModulation =
        vajramInputFile.vajramInputsDef().inputs().stream().anyMatch(InputDef::isNeedsModulation);
    clsDeps.put(INPUTS, ClassName.get(Inputs.class));
    clsDeps.put(UNMOD_INPUT, ClassName.get(UnmodulatedInput.class));
    clsDeps.put(MOD_INPUT, ClassName.get(ModulatedInput.class));
    clsDeps.put(IM_MAP, ClassName.get(ImmutableMap.class));
    clsDeps.put(IM_LIST, ClassName.get(ImmutableList.class));
    clsDeps.put(DEP_COMMAND, ClassName.get(DependencyCommand.class));
    clsDeps.put(
        SINGLE_EXEC_CMD,
        ClassName.get(
            DependencyCommand.class.getPackageName(),
            DependencyCommand.class.getSimpleName(),
            SingleExecute.class.getSimpleName()));
    clsDeps.put(
        MULTI_EXEC_CMD,
        ClassName.get(
            DependencyCommand.class.getPackageName(),
            DependencyCommand.class.getSimpleName(),
            MultiExecute.class.getSimpleName()));
    clsDeps.put(FUNCTION, ClassName.get(Function.class));
    clsDeps.put(VAL_ERR, ClassName.get(ValueOrError.class));
    clsDeps.put(DEP_RESP, ClassName.get(DependencyResponse.class));
    clsDeps.put(INPUT_SRC, ClassName.get(InputSource.class));
  }

  /**
   * Method to generate VajramImpl class Input dependency code gen Resolve method code gen Vajram
   * logic code gen Compute vajram execute IO vajram executeBlocking
   *
   * @param classLoader
   * @return Class code as string
   */
  public String codeGenVajramImpl(ClassLoader classLoader) {
    final TypeSpec.Builder vajramImplClass = createVajramImplClass();
    List<MethodSpec> methodSpecs = new ArrayList<>();
    // Add superclass
    vajramImplClass
        .addModifiers(PUBLIC, FINAL)
        .superclass(ClassName.bestGuess(vajramName).box())
        .build();

    // Map of all the resolved variables to the methods resolving them
    Map<String, List<Method>> resolverMap = new HashMap<>();
    ParsedVajramData parsedVajramData = vajramDefs.get(vajramName);
    parsedVajramData
        .resolveMethods()
        .forEach(
            method ->
                resolverMap.put(method.getAnnotation(Resolve.class).depName(), new ArrayList<>()));
    for (Method resolve : parsedVajramData.resolveMethods()) {
      String key = resolve.getAnnotation(Resolve.class).depName();
      resolverMap.get(key).add(resolve);
    }
    // Iterate all the resolvers and figure fanout
    // dep input data type and method return type =>
    // 1. depInput = T, if (resolverReturnType is iterable of T || iterable of vajramRequest ||
    // multiExecute) => fanout
    Map<String, Boolean> depFanoutMap = new HashMap<>();
    parsedVajramData
        .resolveMethods()
        .forEach(
            method -> {
              Resolve resolve = method.getAnnotation(Resolve.class);
              String[] inputs = resolve.depInputs();
              String depName = resolve.depName();
              assert depName != null;
              ImmutableList<VajramInputDefinition> vajramInputDefinitions = vajramInputsDefinitions.get(parsedVajramData.vajramName());
              final Optional<VajramInputDefinition> definition =
                  vajramInputDefinitions.stream()
                      .filter(vajramInputDefinition -> vajramInputDefinition.name().equals(depName))
                      .findFirst();
              definition.ifPresent(
                  def -> {
                    if (def instanceof Dependency dependency
                        && dependency.dataAccessSpec() instanceof VajramID vajramID) {
                      String depVajramClass =
                          vajramID
                              .className()
                              .orElseThrow(
                                  () ->
                                      new VajramValidationException(
                                          "Vajram class missing in VajramInputDefinition for :"
                                              + vajramName));
                      String[] splits = COMPILE.split(depVajramClass);
                      String depPackageName =
                          stream(splits, 0, splits.length - 1).collect(Collectors.joining(DOT));
                      String depRequestClass =
                          CodegenUtils.getRequestClassName(splits[splits.length - 1]);
                      ParsedVajramData vajramData = vajramDefs.get(splits[splits.length - 1]);
                      depFanoutMap.put(
                          depName,
                          CodegenUtils.isDepResolverFanout(
                              vajramData.vajramClass(), method, inputs, vajramData.fields()));
                      }
                  });
            });

    // Initialize few common attributes and data structures
    final ClassName inputsNeedingModulation =
        ClassName.get(
            parsedVajramData.packageName(),
            getInputUtilClassName(parsedVajramData.vajramName()),
            getInputModulationClassname(vajramName));
    final ClassName commonInputs =
        ClassName.get(
            parsedVajramData.packageName(),
            getInputUtilClassName(parsedVajramData.vajramName()),
            getCommonInputsClassname(vajramName));
    //noinspection unchecked
    final TypeName vajramResponseType =
        CodegenUtils.getClassGenericArgumentsType(parsedVajramData.vajramClass());

    MethodSpec inputDefinitionsMethod = createInputDefinitions(classLoader);
    methodSpecs.add(inputDefinitionsMethod);
    Optional<MethodSpec> inputResolverMethod = createResolvers(resolverMap, depFanoutMap);
    inputResolverMethod.ifPresent(methodSpecs::add);

    if (IOVajram.class.isAssignableFrom(parsedVajramData.vajramClass())) {
      methodSpecs.add(
          createIOVajramExecuteMethod(
              inputsNeedingModulation, commonInputs, vajramResponseType, resolverMap));
    } else {
      methodSpecs.add(
          createComputeVajramExecuteMethod(
              vajramResponseType, resolverMap, inputsNeedingModulation, commonInputs));
    }
    if (needsModulation) {
      methodSpecs.add(createInputConvertersMethod(inputsNeedingModulation, commonInputs));
    }

    StringWriter writer = new StringWriter();
    try {
      JavaFile.builder(packageName, vajramImplClass.addMethods(methodSpecs).build())
          .indent("  ")
          .build()
          .writeTo(writer);
    } catch (IOException ignored) {

    }
    return writer.toString();
  }

  /**
   * Method to fetch vajram input definitions. The input defs are first fetched from config file. If
   * its not found, then the VajramImpl class is loaded and "getInputDefinitions" method is invoked
   * to fetch the vajram input definitions.
   *
   * @param classLoader Classloader
   * @param parsedVajramData Vajram details
   * @return List of {@link VajramInputDefinition} for the Vajram.
   */
  @SuppressWarnings("unchecked")
  private ImmutableList<VajramInputDefinition> getVajramInputDefinitions(
      ClassLoader classLoader, ParsedVajramData parsedVajramData) {
    ImmutableList<VajramInputDefinition> vajramInputDefinitions =
        vajramInputsDefinitions.get(parsedVajramData.vajramName());
    if (Objects.isNull(vajramInputDefinitions) || vajramInputDefinitions.isEmpty()) {
      try {
        Class<? extends Vajram> parsedVajramImpl =
            (Class<? extends Vajram>)
                classLoader.loadClass(
                    parsedVajramData.packageName()
                        + DOT
                        + getVajramImplClassName(parsedVajramData.vajramName()));
        Method getInputDefinitions = parsedVajramImpl.getDeclaredMethod("getInputDefinitions");
        vajramInputDefinitions =
            (ImmutableList<VajramInputDefinition>) getInputDefinitions.invoke(parsedVajramImpl);

      } catch (ClassNotFoundException
          | NoSuchMethodException
          | InvocationTargetException
          | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    return vajramInputDefinitions;
  }

  /**
   * Method to generate "executeCompute" function code for ComputeVajrams Supported DataAccessSpec
   * => VajramID only.
   *
   * @param vajramResponseType Vajram response type
   * @param resolverMap Map of all the resolved variables to the methods resolving them
   * @param inputsNeedingModulation ClassName for the inputModulation class for the Vajram
   * @param commonInputs ClassName for the commonInputs class for the Vajram
   * @return generated code for "executeCompute" {@link MethodSpec}
   */
  private MethodSpec createComputeVajramExecuteMethod(
      TypeName vajramResponseType,
      Map<String, ? extends Collection<Method>> resolverMap,
      ClassName inputsNeedingModulation,
      ClassName commonInputs) {

    Builder executeBuilder =
        methodBuilder(METHOD_EXECUTE_COMPUTE)
            .addModifiers(PUBLIC)
            .addParameter(ParameterizedTypeName.get(ImmutableList.class, Inputs.class), INPUTS_LIST)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(ImmutableMap.class),
                    ClassName.get(Inputs.class),
                    ParameterizedTypeName.get(clsDeps.get(VAL_ERR), vajramResponseType)))
            .addAnnotation(Override.class);
    if (needsModulation) {
      CodeBlock.Builder codeBuilder = CodeBlock.builder();
      Map<String, Object> valueMap = new HashMap<>();
      valueMap.put(INPUTS, ClassName.get(Inputs.class));
      valueMap.put(UNMOD_INPUT, ClassName.get(UnmodulatedInput.class));
      valueMap.put(INPUT_MODULATION, inputsNeedingModulation);
      valueMap.put(COMMON_INPUT, commonInputs);
      valueMap.put(RETURN_TYPE, vajramResponseType);
      valueMap.put(VAJRAM_LOGIC_METHOD, vajramDefs.get(vajramName).vajramLogic().getName());
      valueMap.put(MOD_INPUT, ClassName.get(ModulatedInput.class));
      valueMap.put(IM_MAP, ClassName.get(ImmutableMap.class));
      valueMap.put(IM_LIST, ClassName.get(ImmutableList.class));
      valueMap.put(HASH_MAP, ClassName.get(HashMap.class));
      valueMap.put(ARRAY_LIST, ClassName.get(ArrayList.class));
      valueMap.put(COM_FUTURE, ClassName.get(CompletableFuture.class));
      valueMap.put(LINK_HASH_MAP, ClassName.get(LinkedHashMap.class));
      valueMap.put(MAP, ClassName.get(Map.class));
      valueMap.put(LIST, ClassName.get(List.class));
      valueMap.put(VAL_ERR, clsDeps.get(VAL_ERR));
      // Any vajram supporting input modulation must return map
      assert Map.class.isAssignableFrom(vajramDefs.get(vajramName).vajramLogic().getReturnType());
      Type type =
          ((ParameterizedType) vajramDefs.get(vajramName).vajramLogic().getGenericReturnType())
              .getActualTypeArguments()[1];
      // TODO : check if this is needed for compute vajrams or should throw error
      if (type instanceof ParameterizedType
          && CompletableFuture.class.isAssignableFrom(
              (Class<?>) ((ParameterizedType) type).getRawType())) {
        codeBuilder.addNamed(INPUT_MODULATION_FUTURE_CODE_BLOCK, valueMap);
      } else {
        codeBuilder.addNamed(INPUT_MODULATION_CODE_BLOCK, valueMap);
      }
      executeBuilder.addCode(codeBuilder.build());
    } else { // TODO : Need non modulated IO vajram to test this
      simpleComputeMethodBuilder(resolverMap, executeBuilder);
    }
    return executeBuilder.build();
  }

  private void simpleComputeMethodBuilder(
      Map<String, ? extends Collection<Method>> resolverMap, Builder executeBuilder) {
    CodeBlock.Builder returnBuilder =
        CodeBlock.builder()
            .add(
                """
                    return inputsList.stream().collect(
                         $T.toImmutableMap($T.identity(),
                         element -> {
                    """,
                clsDeps.get(IM_MAP),
                clsDeps.get(FUNCTION));
    List<CodeBlock> inputCodeBlocks = new ArrayList<>();
    inputDefsMap
        .values()
        .forEach(
            inputDef -> {
              if (resolverMap.containsKey(inputDef.name())) {
                CodeBlock.Builder codeBlock = CodeBlock.builder();
                if (inputDef instanceof Dependency<?> inputDefDependency) {
                  DataAccessSpec dataAccessSpec = inputDefDependency.dataAccessSpec();
                  if (dataAccessSpec instanceof VajramID vajramID) {
                    String depVajramClass =
                        vajramID
                            .className()
                            .orElseThrow(
                                () ->
                                    new VajramValidationException(
                                        "Vajram class missing in VajramInputDefinition for :"
                                            + vajramName));
                    String[] splits = COMPILE.split(depVajramClass);
                    String depPackageName =
                        stream(splits, 0, splits.length - 1).collect(Collectors.joining(DOT));
                    String depRequestClass =
                        CodegenUtils.getRequestClassName(splits[splits.length - 1]);
                    ParsedVajramData parsedVajramData = vajramDefs.get(splits[splits.length - 1]);
                    final TypeName typeArgument =
                        CodegenUtils.getClassGenericArgumentsType(parsedVajramData.vajramClass());
                    final String variableName = CodegenUtils.toJavaName(inputDef.name());
                    final String depVariableName = variableName + RESPONSES_SUFFIX;
                    codeBlock.addNamed(
                        """
                            $depResp:T<$request:T, $response:T> $depResponse:L =
                                 new $depResp:T<>(
                                     element.<$response:T>getDepValue($variable:S).values().entrySet().stream()
                                         .filter(
                                             e ->
                                                 e.getValue()
                                                     .error()
                                                     .filter(t -> t instanceof $skippedException:T)
                                                     .isEmpty())
                                         .collect(
                                             $imMap:T.toImmutableMap(
                                                 e -> $request:T.from(e.getKey()), java.util.Map.Entry::getValue)));
                             """,
                        ImmutableMap.of(
                            DEP_RESP,
                            clsDeps.get(DEP_RESP),
                            REQUEST,
                            ClassName.get(depPackageName, depRequestClass),
                            RESPONSE,
                            typeArgument,
                            VARIABLE,
                            inputDef.name(),
                            DEP_RESPONSE,
                            depVariableName,
                            IM_MAP,
                            clsDeps.get(IM_MAP),
                            SKIPPED_EXCEPTION,
                            SkippedExecutionException.class));
                    inputCodeBlocks.add(CodeBlock.builder().add(depVariableName).build());
                  }
                }
                returnBuilder.add(codeBlock.build());
              } else {
                // call vajram logic method with all input values
                if (inputDef.isMandatory()) {
                  inputCodeBlocks.add(
                      CodeBlock.builder()
                          .add("element.getInputValueOrThrow($S)", inputDef.name())
                          .build());
                } else {
                  inputCodeBlocks.add(
                      CodeBlock.builder()
                          .add("element.getInputValueOrDefault($S, null)", inputDef.name())
                          .build());
                }
              }
            });
    returnBuilder.add(
        "\nreturn $T.valueOrError(() -> $L(new $T(\n",
        clsDeps.get(VAL_ERR),
        vajramDefs.get(vajramName).vajramLogic().getName(),
        ClassName.get(
            packageName, getInputUtilClassName(vajramName), getAllInputsClassname(vajramName)));
    // merge the code blocks for inputs
    for (int i = 0; i < inputCodeBlocks.size(); i++) {
      // for formatting
      returnBuilder.add("\t\t");
      returnBuilder.add(inputCodeBlocks.get(i));
      if (i != inputCodeBlocks.size() - 1) {
        returnBuilder.add(",\n");
      }
    }
    returnBuilder.add(")));\n");
    returnBuilder.add("}));\n");
    executeBuilder.addCode(returnBuilder.build());
  }

  /**
   * Method to generate "getInputsConvertor" function
   *
   * @param inputsNeedingModulation Generated Vajram specific InputUtil.InputsNeedingModulation
   *     class
   * @param commonInputs Generated Vajram specific InputUtil.CommonInputs class
   * @return {@link MethodSpec}
   */
  private MethodSpec createInputConvertersMethod(
      ClassName inputsNeedingModulation, ClassName commonInputs) {
    Builder inputConvertersBuilder =
        methodBuilder(METHOD_GET_INPUTS_CONVERTOR)
            .addModifiers(PUBLIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(InputsConverter.class), inputsNeedingModulation, commonInputs))
            .addAnnotation(Override.class);
    inputConvertersBuilder.addCode(
        CodeBlock.builder()
            .addStatement(
                "return $T.CONVERTER",
                ClassName.get(packageName, getInputUtilClassName(vajramName)))
            .build());
    return inputConvertersBuilder.build();
  }

  /**
   * Method to generate "execute" function code for IOVajrams
   *
   * @param inputsNeedingModulation Generated Vajramspecific InputUtil.InputsNeedingModulation class
   * @param commonInputs Generated Vajram specific InputUtil.CommonInputs class
   * @param vajramResponseType Vajram response type
   * @param resolverMap Map of the resolved variables to their resolver methods
   * @return generated code for "execute" {@link MethodSpec}
   */
  private MethodSpec createIOVajramExecuteMethod(
      ClassName inputsNeedingModulation,
      ClassName commonInputs,
      TypeName vajramResponseType,
      Map<String, ? extends Collection<Method>> resolverMap) {

    Builder executeMethodBuilder =
        methodBuilder(METHOD_EXECUTE)
            .addModifiers(PUBLIC)
            .addParameter(ParameterizedTypeName.get(ImmutableList.class, Inputs.class), INPUTS_LIST)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(ImmutableMap.class),
                    ClassName.get(Inputs.class),
                    ParameterizedTypeName.get(
                        ClassName.get(CompletableFuture.class), vajramResponseType)))
            .addAnnotation(Override.class);

    CodeBlock.Builder codeBuilder = CodeBlock.builder();
    if (needsModulation) {
      Map<String, Object> valueMap = new HashMap<>();
      valueMap.put(INPUTS, ClassName.get(Inputs.class));
      valueMap.put(UNMOD_INPUT, ClassName.get(UnmodulatedInput.class));
      valueMap.put(INPUT_MODULATION, inputsNeedingModulation);
      valueMap.put(COMMON_INPUT, commonInputs);
      valueMap.put(RETURN_TYPE, vajramResponseType);
      valueMap.put(VAJRAM_LOGIC_METHOD, vajramDefs.get(vajramName).vajramLogic().getName());
      valueMap.put(MOD_INPUT, ClassName.get(ModulatedInput.class));
      valueMap.put(IM_MAP, ClassName.get(ImmutableMap.class));
      valueMap.put(IM_LIST, ClassName.get(ImmutableList.class));
      valueMap.put(HASH_MAP, ClassName.get(HashMap.class));
      valueMap.put(ARRAY_LIST, ClassName.get(ArrayList.class));
      valueMap.put(COM_FUTURE, ClassName.get(CompletableFuture.class));
      valueMap.put(LINK_HASH_MAP, ClassName.get(LinkedHashMap.class));
      valueMap.put(MAP, ClassName.get(Map.class));
      valueMap.put(LIST, ClassName.get(List.class));
      valueMap.put(VAL_ERR, clsDeps.get(VAL_ERR));
      // Any vajram supporting input modulation must return map
      assert Map.class.isAssignableFrom(vajramDefs.get(vajramName).vajramLogic().getReturnType());
      Type type =
          ((ParameterizedType) vajramDefs.get(vajramName).vajramLogic().getGenericReturnType())
              .getActualTypeArguments()[1];
      if (type instanceof ParameterizedType
          && CompletableFuture.class.isAssignableFrom(
              (Class<?>) ((ParameterizedType) type).getRawType())) {
        codeBuilder.addNamed(INPUT_MODULATION_FUTURE_CODE_BLOCK, valueMap);
      } else {
        codeBuilder.addNamed(INPUT_MODULATION_CODE_BLOCK, valueMap);
      }
      executeMethodBuilder.addCode(codeBuilder.build());
    } else {
      simpleComputeMethodBuilder(resolverMap, executeMethodBuilder);
    }
    return executeMethodBuilder.build();
  }

  /**
   * Method to generate "resolveInputOfDependency" function code for Vajrams. If there are no
   * resolvers defined in the Vajram, {@link Optional}.empty() is returned.
   *
   * @param resolverMap Map of all the resolved variables to the methods resolving them
   * @param depFanoutMap Map of all the dependencies and their resolvers defintions are fanout or
   *     not
   * @return generated code for "resolveInputOfDependency" {@link MethodSpec}
   */
  public Optional<MethodSpec> createResolvers(
      Map<String, ? extends List<Method>> resolverMap, Map<String, Boolean> depFanoutMap) {
    String dependency = "dependency";
    Builder resolveInputsBuilder =
        methodBuilder(METHOD_RESOLVE_INPUT_OF_DEPENDENCY)
            .addModifiers(PUBLIC)
            .addParameter(String.class, dependency)
            .addParameter(
                ParameterizedTypeName.get(ImmutableSet.class, String.class), RESOLVABLE_INPUTS)
            .addParameter(Inputs.class, INPUTS)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(DependencyCommand.class), ClassName.get(Inputs.class)));
    ParsedVajramData parsedVajramData = vajramDefs.get(vajramName);
    if (Objects.nonNull(parsedVajramData)) {
      resolveInputsBuilder.beginControlFlow("switch ($L) ", dependency);
      if (parsedVajramData.resolveMethods().isEmpty()) {
        return Optional.empty();
      }
      // get all resolved variable names
      Set<String> resolvedVariables = resolverMap.keySet();

      resolverMap.forEach(
          (variable, methods) -> {
            CodeBlock.Builder caseBuilder =
                CodeBlock.builder().beginControlFlow("case $S -> ", variable);
            methods.forEach(
                method -> {
                  AtomicBoolean fanout = new AtomicBoolean(false);
                  // TODO : confirm if this logic is correct for all parameters for a resolve method
                  stream(method.getParameters())
                      .forEach(
                          parameter -> {
                            String bindParamName = parameter.getAnnotation(Using.class).value();
                            if (!fanout.get()
                                && depFanoutMap.containsKey(
                                    bindParamName)) { // if fanout is already set skip resetting it.
                              fanout.set(depFanoutMap.get(bindParamName));
                            }
                            // validating if the bind parameter has a resolver binding or defined as
                            // input
                            if (!(inputDefsMap.containsKey(bindParamName)
                                || resolvedVariables.contains(bindParamName))) {
                              throw new VajramValidationException(
                                  "Parameter binding incorrect for input - " + bindParamName);
                            }
                          });
                  CodeBlock.Builder ifBlockBuilder =
                      buildInputResolver(
                          resolvedVariables, resolverMap, method, depFanoutMap, fanout.get());
                  caseBuilder.add(ifBlockBuilder.build());
                });
            caseBuilder.endControlFlow();
            resolveInputsBuilder.addCode(caseBuilder.build());
          });
      resolveInputsBuilder.endControlFlow();
      resolveInputsBuilder.addStatement(
          "throw new $T($S)",
          ClassName.get(VajramValidationException.class),
          "Unresolvable dependency");
    } else {
      resolveInputsBuilder.addStatement(
          "throw new $T($S)",
          ClassName.get(VajramValidationException.class),
          "Unresolvable dependency");
    }
    return Optional.of(resolveInputsBuilder.build());
  }

  /**
   * Method to generate resolver code for input binding
   *
   * @param resolvedVariables all resolved variable names
   * @param resolverMap Map of all the resolved variables to the methods resolving them
   * @param method Vajram resolve method
   * @param depFanoutMap Map of all the dependencies and their resolvers defintions are fanout or
   *     not
   * @param isParamFanoutDependency Variable mentioning if the resolved variable uses a fanout
   *     dependency
   * @return {@link CodeBlock.Builder} with resolver code
   */
  private CodeBlock.Builder buildInputResolver(
      Collection<String> resolvedVariables,
      Map<String, ? extends List<Method>> resolverMap,
      Method method,
      Map<String, Boolean> depFanoutMap,
      boolean isParamFanoutDependency) {
    Resolve resolve = method.getAnnotation(Resolve.class);
    String[] inputs = resolve.depInputs();
    // check if the input is satisfied by input or other resolved variables
    CodeBlock.Builder ifBlockBuilder = CodeBlock.builder();
    ifBlockBuilder.beginControlFlow(
        "if ($T.of($S).equals(resolvableInputs))", Set.class, String.join(",", inputs));
    Map<String, Type> paramToVariableMap = new HashMap<>();
    // TODO : add validation if fanout, then method should accept dependency response for the bind
    // type parameter else error
    // Iterate over the method params and call respective binding methods
    stream(method.getParameters())
        .forEach(
            parameter -> {
              String bindParamName = parameter.getAnnotation(Using.class).value();
              // check if the bind param has multiple resolvers
              if (resolverMap.containsKey(bindParamName)) {
                paramToVariableMap.put(
                    bindParamName,
                    generateDependencyResolutions(
                            method, inputs, bindParamName, ifBlockBuilder, depFanoutMap, parameter)
                        .orElseThrow(
                            () -> {
                              throw new VajramValidationException(
                                  "No input resolver found for " + bindParamName);
                            }));
              } else if (inputDefsMap.containsKey(bindParamName)) {
                VajramInputDefinition inputDefinition = inputDefsMap.get(bindParamName);
                String variable = toJavaName(bindParamName);
                final TypeName parameterType =
                    CodegenUtils.getType(parameter.getParameterizedType());
                if (inputDefinition.isMandatory()) {
                  ifBlockBuilder.add(
                      CodeBlock.builder()
                          .addStatement(
                              "$T $L = $L.getInputValueOrThrow($S)",
                              parameterType,
                              variable,
                              INPUTS,
                              bindParamName)
                          .build());
                } else {
                  if (Optional.class.isAssignableFrom(parameter.getType())) {
                    ifBlockBuilder.add(
                        CodeBlock.builder()
                            .addStatement(
                                "$T $L = $L.getInputValueOpt($S)",
                                parameterType,
                                variable,
                                INPUTS,
                                bindParamName)
                            .build());
                  } else {
                    throw new VajramValidationException(
                        String.format(
                            "Optional input dependency %s must have type as Optional",
                            bindParamName));
                  }
                }
                paramToVariableMap.put(bindParamName, Primitives.wrap(parameter.getType()));
              } else {
                throw new VajramValidationException("No input resolver found for " + bindParamName);
              }
            });
    boolean isFanOut =
        isParamFanoutDependency || depFanoutMap.getOrDefault(resolve.depName(), false);
    buildFinalResolvers(method, inputs, paramToVariableMap, ifBlockBuilder, isFanOut);
    ifBlockBuilder.endControlFlow();
    return ifBlockBuilder;
  }

  /**
   * Method to generate resolver code for dependency bindings
   *
   * @param method Dependency resolver method
   * @param inputs Inputs to the dependency resolver method
   * @param bindParamName The bind param name in the resolver method
   * @param ifBlockBuilder The {@link CodeBlock.Builder}
   * @param depFanoutMap Map of all the dependencies and their resolvers defintions are fanout or
   *     not
   * @param parameter the bind parameter in the resolver method
   * @return the {@link Type} of the resolver method response
   */
  private Optional<Type> generateDependencyResolutions(
      Method method,
      String[] inputs,
      String bindParamName,
      CodeBlock.Builder ifBlockBuilder,
      Map<String, Boolean> depFanoutMap,
      Parameter parameter) {
    VajramInputDefinition vajramInputDef = inputDefsMap.get(bindParamName);
    Resolve resolve = method.getAnnotation(Resolve.class);
    assert resolve != null;
    String resolvedDep = resolve.depName();
    // fanout case
    if (depFanoutMap.containsKey(bindParamName)
        && depFanoutMap.get(bindParamName)
        && !DependencyResponse.class.isAssignableFrom(parameter.getType())) {
      // the parameter data type must be DependencyResponse
      log.error(
          "Dependency resolution of {} is fanout but the resolver method is not of type DependencyResponse.",
          resolvedDep);
      throw new VajramValidationException(
          "Dependency resolution of "
              + resolvedDep
              + " is fanout but the resolver method is not of type DependencyResponse");
    }
    //    ReturnType returnType
    if (vajramInputDef instanceof Dependency<?> inputDefDependency) {
      DataAccessSpec dataAccessSpec = inputDefDependency.dataAccessSpec();
      if (dataAccessSpec instanceof VajramID vajramID) {
        String vajramClass =
            vajramID
                .className()
                .orElseThrow(
                    () ->
                        new VajramValidationException(
                            "Vajram class missing in vajram input definition"));

        String variableName = CodegenUtils.toJavaName(bindParamName);
        String[] splits = COMPILE.split(vajramClass);
        final ParsedVajramData parsedVajramData = vajramDefs.get(splits[splits.length - 1]);
        final Field field = parsedVajramData.fields().getOrDefault(variableName, null);
        String depPackageName =
            stream(splits, 0, splits.length - 1).collect(Collectors.joining(DOT));
        String requestClass = CodegenUtils.getRequestClassName(splits[splits.length - 1]);

        if (DependencyResponse.class.isAssignableFrom(parameter.getType())) {
          TypeName typeArgument =
              CodegenUtils.getClassGenericArgumentsType(parsedVajramData.vajramClass());
          ifBlockBuilder.addStatement(
              """
                  $1T $2L =\s
                   new $3T<>(inputs.<$4T>getDepValue($5S)
                        .values().entrySet().stream()
                        .collect($6T.toImmutableMap(e -> $7T.from(e.getKey()),
                        $8T::getValue)))""",
              ParameterizedTypeName.get(
                  clsDeps.get(DEP_RESP), ClassName.get(depPackageName, requestClass), typeArgument),
              variableName,
              clsDeps.get(DEP_RESP),
              typeArgument,
              bindParamName,
              clsDeps.get(IM_MAP),
              ClassName.get(depPackageName, requestClass),
              ClassName.get(Map.Entry.class));
          return Optional.of(DependencyResponse.class);
        } else {
          // convert to parameter data type from dependency response
          String code =
              """
                  $1T $2L =\s
                   new $3T<>(inputs.<$4T>getDepValue($5S)
                        .values().entrySet().stream()
                        .collect($6T.toImmutableMap(e -> $7T.from(e.getKey()),
                        $8T::getValue)))
                        .values().iterator().next().value().orElse(null)""";
          ifBlockBuilder.addStatement(
              code,
              CodegenUtils.getMethodReturnType(method),
              variableName,
              clsDeps.get(DEP_RESP),
              CodegenUtils.getMethodReturnType(method),
              bindParamName,
              clsDeps.get(IM_MAP),
              ClassName.get(depPackageName, requestClass),
              ClassName.get(Map.Entry.class));
          return Optional.of(method.getReturnType());
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Method to generate resolver code for variables having single resolver. Fanout case - Iterable
   * of normal type => fanout loop and create inputs - Iterable Vajram Request -
   * DependencyCommand.MultiExecute<NormalType> Non- fanout - Normal datatype - Vajram Request =>
   * toInputValues() - DependencyCommand.executeWith
   *
   * @param method Resolve method
   * @param inputs Resolve inputs
   * @param paramToVariableMap All the input variables with key as bindParam name
   * @param ifBlockBuilder {@link CodeBlock.Builder}
   * @param isFanOut Variable mentioning if the resolved variable uses a fanout dependency
   */
  private void buildFinalResolvers(
      Method method,
      String[] inputs,
      Map<String, Type> paramToVariableMap,
      CodeBlock.Builder ifBlockBuilder,
      boolean isFanOut) {

    String variableName = "resolverResult";
    boolean controlFLowStarted = false;
    // Identify resolve method return type
    final TypeName methodReturnType = CodegenUtils.getMethodReturnType(method);

    // call the resolve method
    ifBlockBuilder.add("$T $L = $L(", methodReturnType, variableName, method.getName());
    for (int i = 0; i < method.getParameters().length; i++) {
      Parameter parameter = method.getParameters()[i];
      String bindName = parameter.getAnnotation(Using.class).value();
      ifBlockBuilder.add("$L", CodegenUtils.toJavaName(bindName));
      if (i != method.getParameters().length - 1) {
        ifBlockBuilder.add(", ");
      }
    }
    ifBlockBuilder.add(");\n");

    if (DependencyCommand.class.isAssignableFrom(method.getReturnType())) {
      ifBlockBuilder.beginControlFlow("if($L.shouldSkip())", variableName);
      ifBlockBuilder.addStatement(
          "\t return $T.skipSingleExecute($L.doc())", clsDeps.get(SINGLE_EXEC_CMD), variableName);
      ifBlockBuilder.add("} else {\n\t");
      controlFLowStarted = true;
    }
    // TODO : add missing validations if any (??)
    Class<?> klass = Primitives.wrap(method.getReturnType());
    if (DependencyCommand.MultiExecute.class.isAssignableFrom(klass)) {
      String code =
          """
              return $T.multiExecuteWith(
                  $L.inputs().stream()
                      .map(
                          element ->
                              new $T(
                                  $T.of($S, $T.withValue(element))))
                  .toList())""";
      ifBlockBuilder.addStatement(
          code,
          clsDeps.get(DEP_COMMAND),
          variableName,
          clsDeps.get(INPUTS),
          clsDeps.get(IM_MAP),
          inputs[0],
          clsDeps.get(VAL_ERR));
    } else if (isFanOut) {
      if (Iterable.class.isAssignableFrom(klass)) {
        if (VajramRequest.class.isAssignableFrom(
            (Class<?>)
                ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0])) {
          String code =
              """
                  return $T.multiExecuteWith(
                      $L.stream()
                          .map(
                              element ->
                                  element.toInputValues()))
                      .toList())""";
          ifBlockBuilder.addStatement(code, clsDeps.get(DEP_COMMAND), variableName);
        } else {
          String code =
              """
                  return $T.multiExecuteWith(
                      $L.stream()
                          .map(
                              element ->
                                  new $T(
                                      $T.of($S, $T.withValue(element))))
                      .toList())""";
          ifBlockBuilder.addStatement(
              code,
              clsDeps.get(DEP_COMMAND),
              variableName,
              clsDeps.get(INPUTS),
              clsDeps.get(IM_MAP),
              inputs[0],
              clsDeps.get(VAL_ERR));
        }
      } else {
        throw new VajramValidationException(
            "Incorrect vajram resolver "
                + vajramName
                + ": Fanout resolvers must return an iterable");
      }
    } else {
      if (VajramRequest.class.isAssignableFrom(klass)) {
        ifBlockBuilder.addStatement(
            "return $T.singleExecuteWith($L.toInputValues())",
            clsDeps.get(DEP_COMMAND),
            variableName);
      } else if (DependencyCommand.SingleExecute.class.isAssignableFrom(klass)) {
        ifBlockBuilder.addStatement(
            """
                  return $T.singleExecuteWith(new Inputs(
                   $T.of($S, $T.withValue(
                      $L.inputs().iterator().next().orElse(null)))))
                """,
            clsDeps.get(DEP_COMMAND),
            clsDeps.get(IM_MAP),
            inputs[0],
            clsDeps.get(VAL_ERR),
            variableName);

      } else {
        ifBlockBuilder.addStatement(
            "return $T.singleExecuteWith(new Inputs(\n " + "$T.of($S, $T.withValue($L))))",
            clsDeps.get(DEP_COMMAND),
            clsDeps.get(IM_MAP),
            inputs[0],
            clsDeps.get(VAL_ERR),
            variableName);
      }
      // TODO  : check how to handle this => if
      //      if(DependencyCommand.Execute.class.isAssignableFrom(klass)) {
      //
      //      }
    }
    if (controlFLowStarted) {
      ifBlockBuilder.endControlFlow();
    }
  }

  /**
   * Method to generate code for "getInputDefinitions" function
   *
   * @param classLoader : Classloader with generated Vajram models
   * @return {@link MethodSpec}
   */
  private MethodSpec createInputDefinitions(ClassLoader classLoader) {
    // Method : getInputDefinitions
    Builder inputDefinitionsBuilder =
        methodBuilder(GET_INPUT_DEFINITIONS)
            .addModifiers(PUBLIC)
            .returns(
                ParameterizedTypeName.get(
                    clsDeps.get(IM_LIST), ClassName.get(VajramInputDefinition.class)));
    ImmutableList<VajramInputDefinition> inputDefinitions = vajramInputsDefinitions.get(vajramName);

    Collection<CodeBlock> codeBlocks = new ArrayList<>(inputDefinitions.size());
    // Input and Dependency code block
    inputDefinitions.forEach(
        vajramInputDefinition -> {
          CodeBlock.Builder inputDefBuilder = CodeBlock.builder();
          if (vajramInputDefinition instanceof Input input) {
            buildVajramInput(inputDefBuilder, input);
          } else if (vajramInputDefinition instanceof Dependency dependency) {
            buildVajramDependency(classLoader, inputDefBuilder, dependency);
          }
          codeBlocks.add(inputDefBuilder.build());
        });

    inputDefinitionsBuilder.beginControlFlow("if(this.$L == null)", INPUT_DEFINITIONS_VAR);
    inputDefinitionsBuilder.addCode(
        CodeBlock.builder()
            .add("this.$L = $T.of(\n", INPUT_DEFINITIONS_VAR, clsDeps.get(IM_LIST))
            .add(CodeBlock.join(codeBlocks, ",\n\t"))
            .add("\n);\n")
            .build());
    inputDefinitionsBuilder.endControlFlow();
    inputDefinitionsBuilder.addStatement("return $L", INPUT_DEFINITIONS_VAR);
    return inputDefinitionsBuilder.build();
  }

  /**
   * Method to generate VajramDependency code blocks
   *
   * @param classLoader : Classloader with generated Vajram models
   * @param inputDefBuilder : {@link CodeBlock.Builder}
   * @param dependency : Vajram dependency
   */
  private static void buildVajramDependency(
      ClassLoader classLoader, CodeBlock.Builder inputDefBuilder, Dependency dependency) {
    inputDefBuilder
        .add("$T.builder()", ClassName.get(Dependency.class))
        .add(".name($S)", dependency.name());
    DataAccessSpec dataAccessSpec = dependency.dataAccessSpec();
    if (dataAccessSpec instanceof VajramID vajramID) {
      String code = ".dataAccessSpec($1T.vajramID($2S))";
      if (vajramID.className().isPresent()) {
        try {
          Optional<String> vajramId =
              getVajramIdString(
                  (Class<? extends Vajram>) classLoader.loadClass(vajramID.className().get()));
          vajramId.ifPresent(s -> inputDefBuilder.add(code, ClassName.get(VajramID.class), s));
        } catch (ClassNotFoundException e) {
          throw new VajramValidationException(e);
        }
      } else {
        inputDefBuilder.add(code, ClassName.get(VajramID.class), vajramID.vajramId());
      }
    }
    if (dependency.isMandatory()) {
      inputDefBuilder.add(".isMandatory()");
    }
    // build() as last step
    inputDefBuilder.add(".build()");
  }

  /**
   * Method to generate VajramInput code blocks
   *
   * @param inputDefBuilder : {@link CodeBlock.Builder}
   * @param input : Vajram Input
   */
  private void buildVajramInput(CodeBlock.Builder inputDefBuilder, Input<?> input) {
    inputDefBuilder.add("$T.builder()", ClassName.get(Input.class)).add(".name($S)", input.name());
    // handle input type
    Set<InputSource> inputSources = input.sources();
    if (!inputSources.isEmpty()) {
      inputDefBuilder.add(".sources(");
      String sources =
          inputSources.stream()
              .map(
                  inputSource -> {
                    if (inputSource == InputSource.CLIENT) {
                      return "$inputSrc:T.CLIENT";
                    } else if (inputSource == InputSource.SESSION) {
                      return "$inputSrc:T.SESSION";
                    } else {
                      throw new IllegalArgumentException(
                          "Incorrect source defined in vajram config");
                    }
                  })
              .collect(Collectors.joining(COMMA));
      inputDefBuilder
          .addNamed(sources, ImmutableMap.of(INPUT_SRC, clsDeps.get(INPUT_SRC)))
          .add(")");
    }
    // handle data type
    DataType dataType = input.type();
    inputDefBuilder.add(".type(");
    if (dataType instanceof JavaType<?> javaType) {
      // custom handling
      ClassName className;
      if (!javaType.enclosingClasses().isEmpty() || javaType.simpleName().isPresent()) {
        assert javaType.packageName().isPresent();
        className =
            ClassName.get(
                javaType.packageName().get(),
                String.join(DOT, javaType.enclosingClasses()),
                javaType.simpleName().get());
      } else {
        className = ClassName.bestGuess(javaType.className());
      }
      inputDefBuilder.add("$1T.java($2T.class)", ClassName.get(JavaType.class), className);
    } else {
      String simpleName = dataType.getClass().getSimpleName();
      String name = simpleName.substring(0, simpleName.length() - 4).toLowerCase();
      // support Boolean.bool() type
      if (simpleName.toLowerCase().contains("boolean")) {
        name = "bool";
      }
      final TypeAndName typeName = getTypeName(dataType);
      if (ParameterizedTypeName.class.isAssignableFrom(typeName.typeName().getClass())) {
        final TypeName innerType =
            ((ParameterizedTypeName) typeName.typeName()).typeArguments.get(0);
        inputDefBuilder.add(
            "$T.$L($T.$L())",
            ClassName.get(dataType.getClass().getPackageName(), simpleName),
            name,
            ClassName.get(
                dataType.getClass().getPackageName(),
                ((ClassName) innerType).simpleName() + "Type"),
            ((ClassName) innerType).simpleName().toLowerCase());
      } else {
        inputDefBuilder.add(
            "$T.$L()", ClassName.get(dataType.getClass().getPackageName(), simpleName), name);
      }
    }
    inputDefBuilder.add(")");
    if (input.isMandatory()) {
      inputDefBuilder.add(".isMandatory()");
    }
    if (input.needsModulation()) {
      inputDefBuilder.add(".needsModulation()");
    }
    // last line
    inputDefBuilder.add(".build()");
  }

  public String codeGenVajramRequest() {
    VajramInputsDef vajramInputsDef = vajramInputFile.vajramInputsDef();
    ImmutableList<InputDef> inputDefs = vajramInputsDef.inputs();
    Builder requestConstructor = constructorBuilder().addModifiers(PRIVATE);
    ClassName builderClassType =
        ClassName.get(packageName + DOT_SEPARATOR + requestClassName, "Builder");
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
    Builder toInputValues =
        methodBuilder("toInputValues")
            .returns(Inputs.class)
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .addStatement(
                "$T builder = new $T<>($L)",
                new TypeToken<Map<String, InputValue<Object>>>() {}.getType(),
                new TypeToken<HashMap>() {}.getType(),
                inputDefs.size());
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
    String className = getAllInputsClassname(vajramName);
    TypeSpec.Builder allInputsClass =
        classBuilder(className).addModifiers(FINAL, STATIC).addAnnotations(recordAnnotations());
    List<FieldTypeName> fieldsList = new ArrayList<>();
    VajramInputsDef vajramInputsDef = vajramInputFile.vajramInputsDef();
    vajramInputsDef
        .inputs()
        .forEach(
            inputDef -> {
              String inputJavaName = toJavaName(inputDef.getName());
              TypeAndName javaType = getTypeName(inputDef.toInputDefinition().type());
              allInputsClass.addField(javaType.typeName(), inputJavaName, PRIVATE, FINAL);
              allInputsClass.addMethod(getterCodeForInput(inputDef, inputJavaName, javaType));
              fieldsList.add(new FieldTypeName(javaType.typeName(), inputJavaName));
            });

    vajramInputsDef
        .dependencies()
        .forEach(
            dependencyDef -> {
              Optional<TypeName> javaType = getDependencyOutputsType(allInputsClass, dependencyDef);
              javaType.ifPresent(
                  type -> {
                    String inputJavaName = toJavaName(dependencyDef.getName());
                    allInputsClass.addField(type, inputJavaName, PRIVATE, FINAL);
                    allInputsClass.addMethod(
                        getterCodeForInput(dependencyDef, inputJavaName, new TypeAndName(type)));
                    fieldsList.add(new FieldTypeName(type, inputJavaName));
                  });
            });

    // generate all args constructor and add to class
    generateConstructor(className, fieldsList).ifPresent(allInputsClass::addMethod);

    StringWriter writer = new StringWriter();
    try {
      JavaFile.builder(packageName, inputUtilClass.addType(allInputsClass.build()).build())
          .build()
          .writeTo(writer);
    } catch (IOException ignored) {

    }
    return writer.toString();
  }

  private Optional<MethodSpec> generateConstructor(
      String className, List<FieldTypeName> fieldsList) {
    // by default no args constructor is created so no need to generate
    if (fieldsList.isEmpty()) {
      return Optional.empty();
    }
    MethodSpec.Builder constructor = constructorBuilder();
    fieldsList.forEach(
        fieldTypeName -> {
          constructor.addParameter(fieldTypeName.typeName(), fieldTypeName.name());
          constructor.addCode(
              CodeBlock.builder()
                  .addStatement("this.$L = $L", fieldTypeName.name(), fieldTypeName.name())
                  .build());
        });

    return Optional.of(constructor.build());
  }

  private static Optional<TypeName> getDependencyOutputsType(
      TypeSpec.Builder enclosingClass, DependencyDef dependencyDef) {
    String inputJavaName = toJavaName(dependencyDef.getName());
    if (dependencyDef instanceof VajramDependencyDef vajramDepSpec) {
      String depVajramClass = vajramDepSpec.getVajramClass();
      int lastDotIndex = depVajramClass.lastIndexOf(DOT_SEPARATOR);
      String depRequestClass =
          CodegenUtils.getRequestClassName(depVajramClass.substring(lastDotIndex + 1));
      String depPackageName = depVajramClass.substring(0, lastDotIndex);
      TypeName javaType =
          ParameterizedTypeName.get(
              ClassName.get(DependencyResponse.class),
              ClassName.get(depPackageName, depRequestClass),
              getTypeName(vajramDepSpec.toDataType()).typeName());
      return Optional.of(javaType);
    }
    return Optional.empty();
  }

  private String codeGenModulatedInputUtil() {
    StringWriter writer = new StringWriter();
    try {
      TypeSpec.Builder inputUtilClass = createInputUtilClass();
      VajramInputsDef vajramInputsDef = vajramInputFile.vajramInputsDef();
      String imClassName = getInputModulationClassname(vajramName);
      String ciClassName = getCommonInputsClassname(vajramName);
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
                      vajramInputsDef.inputs().stream().filter(input -> !input.isNeedsModulation()),
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
      List<FieldTypeName> ciFieldsList = new ArrayList<>();
      List<FieldTypeName> imFieldsList = new ArrayList<>();
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
                  imFieldsList.add(new FieldTypeName(javaType.typeName(), inputJavaName));
                } else {
                  commonInputs.addField(javaType.typeName(), inputJavaName, PRIVATE, FINAL);
                  commonInputs.addMethod(getterCodeForInput(inputDef, inputJavaName, javaType));
                  ciFieldsList.add(new FieldTypeName(javaType.typeName(), inputJavaName));
                }
              });
      vajramInputsDef
          .dependencies()
          .forEach(
              dependencyDef -> {
                Optional<TypeName> javaTypeName =
                    getDependencyOutputsType(commonInputs, dependencyDef);
                javaTypeName.ifPresent(
                    type -> {
                      String inputJavaName = toJavaName(dependencyDef.getName());
                      commonInputs.addField(type, inputJavaName, PRIVATE, FINAL);
                      commonInputs.addMethod(
                          getterCodeForInput(dependencyDef, inputJavaName, new TypeAndName(type)));
                      ciFieldsList.add(new FieldTypeName(type, inputJavaName));
                    });
              });
      // create constructors
      generateConstructor(ciClassName, ciFieldsList).ifPresent(commonInputs::addMethod);
      generateConstructor(imClassName, imFieldsList).ifPresent(inputsNeedingModulation::addMethod);

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
    return annotations(EqualsAndHashCode.class, ToString.class);
  }

  private static List<AnnotationSpec> annotations(Class<?>... annotations) {
    return stream(annotations).map(aClass -> AnnotationSpec.builder(aClass).build()).toList();
  }

  private TypeSpec.Builder createInputUtilClass() {
    return classBuilder(getInputUtilClassName(vajramName))
        .addModifiers(FINAL)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build());
  }

  private TypeSpec.Builder createVajramImplClass() {
    return classBuilder(CodegenUtils.getVajramImplClassName(vajramName))
        .addField(
            FieldSpec.builder(
                    ParameterizedTypeName.get(ImmutableList.class, VajramInputDefinition.class),
                    INPUT_DEFINITIONS_VAR)
                .addModifiers(PRIVATE)
                .build());
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

  private record FromAndTo(MethodSpec from, MethodSpec to) {}

  private record TypeAndName(TypeName typeName, Optional<Type> type) {

    private TypeAndName(TypeName typeName) {
      this(typeName, Optional.empty());
    }
  }

  private record FieldTypeName(TypeName typeName, String name) {}
}
