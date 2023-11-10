package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.datatypes.TypeUtils.getJavaType;
import static com.flipkart.krystal.vajram.Vajrams.getVajramIdString;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.COMMA;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.CONVERTER;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.DOT;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getAllInputsClassname;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getClassGenericArgumentsType;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getCommonInputsClassname;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getInputModulationClassname;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getInputUtilClassName;
import static com.flipkart.krystal.vajram.codegen.utils.CodegenUtils.getMethodReturnType;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.ARRAY_LIST;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.COMMON_INPUT;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.COM_FUTURE;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.DEP_COMMAND;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.DEP_RESP;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.DEP_RESPONSE;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.FUNCTION;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.GET_INPUT_DEFINITIONS;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.HASH_MAP;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.ILLEGAL_ARGUMENT;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.IM_LIST;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.IM_MAP;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUTS;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUTS_LIST;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUT_DEFINITIONS_VAR;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUT_MODULATION;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUT_MODULATION_CODE_BLOCK;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUT_MODULATION_FUTURE_CODE_BLOCK;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUT_NAME_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.utils.Constants.INPUT_SPEC_SUFFIX;
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
import static com.flipkart.krystal.vajram.codegen.utils.Constants.OPTIONAL;
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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
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
import com.flipkart.krystal.vajram.codegen.utils.Constants;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputSource;
import com.flipkart.krystal.vajram.inputs.InputValuesAdaptor;
import com.flipkart.krystal.vajram.inputs.MultiExecute;
import com.flipkart.krystal.vajram.inputs.SingleExecute;
import com.flipkart.krystal.vajram.inputs.Using;
import com.flipkart.krystal.vajram.inputs.VajramDepFanoutTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramDepSingleTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import com.flipkart.krystal.vajram.inputs.resolution.Resolve;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"HardcodedLineSeparator", "OverlyComplexClass", "OverlyCoupledClass"})
@Slf4j
public class VajramCodeGenerator {
  private final String packageName;
  private final String requestClassName;
  private final VajramInputFile vajramInputFile;
  private final String vajramName;
  private final Map<String, ParsedVajramData> vajramDefs;
  private final Map<String, ImmutableList<VajramInputDefinition>> vajramInputsDefinitions;
  private final Map<String, VajramInputDefinition> inputDefsMap;
  private final Map<String, ClassName> clsDeps = new HashMap<>();
  private final boolean needsModulation;

  public VajramCodeGenerator(
      VajramInputFile vajramInputFile,
      Map<String, ParsedVajramData> vajramDefs,
      Map<String, ImmutableList<VajramInputDefinition>> vajramInputsDefinitions) {
    this.vajramInputFile = vajramInputFile;
    Path filePath = vajramInputFile.inputFilePath().relativeFilePath();
    Path parentDir =
        checkNotNull(filePath.getParent(), "File path %s does not have a parent dir", filePath);
    this.vajramName = vajramInputFile.vajramName();
    this.packageName =
        IntStream.range(0, parentDir.getNameCount())
            .mapToObj(i -> parentDir.getName(i).toString())
            .collect(Collectors.joining(DOT));
    this.requestClassName = CodegenUtils.getRequestClassName(vajramName);
    // All parsed Vajram data loaded from all Vajram class files with vajram name as key
    this.vajramDefs = Collections.unmodifiableMap(vajramDefs);
    this.vajramInputsDefinitions = Collections.unmodifiableMap(vajramInputsDefinitions);
    // All the present Vajram -> VajramInputDefinitions map with name as key
    this.inputDefsMap =
        vajramInputFile
            .vajramInputsDef()
            .allInputsStream()
            .map(AbstractInput::toInputDefinition)
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
    clsDeps.put(SINGLE_EXEC_CMD, ClassName.get(SingleExecute.class));
    clsDeps.put(MULTI_EXEC_CMD, ClassName.get(MultiExecute.class));
    clsDeps.put(FUNCTION, ClassName.get(Function.class));
    clsDeps.put(VAL_ERR, ClassName.get(ValueOrError.class));
    clsDeps.put(DEP_RESP, ClassName.get(DependencyResponse.class));
    clsDeps.put(INPUT_SRC, ClassName.get(InputSource.class));
  }

  public String getVajramName() {
    return vajramName;
  }

  /**
   * Method to generate VajramImpl class Input dependency code gen Resolve method code gen Vajram
   * logic code gen Compute vajram execute IO vajram executeBlocking
   *
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
    for (Method resolve : getParsedVajramData().resolveMethods()) {
      String key = checkNotNull(resolve.getAnnotation(Resolve.class)).depName();
      resolverMap.computeIfAbsent(key, _k -> new ArrayList<>()).add(resolve);
    }
    // Iterate all the resolvers and figure fanout
    // dep input data type and method return type =>
    // 1. depInput = T, if (resolverReturnType is iterable of T || iterable of vajramRequest ||
    // multiExecute) => fanout
    Map<String, Boolean> depFanoutMap = new HashMap<>();
    getParsedVajramData()
        .resolveMethods()
        .forEach(
            method -> {
              Resolve resolve = checkNotNull(method.getAnnotation(Resolve.class));
              String[] inputs = resolve.depInputs();
              String depName = resolve.depName();
              assert depName != null;

              final Optional<VajramInputDefinition> definition =
                  vajramInputsDefinitions
                      .getOrDefault(getParsedVajramData().vajramName(), ImmutableList.of())
                      .stream()
                      .filter(vajramInputDefinition -> vajramInputDefinition.name().equals(depName))
                      .findFirst();
              definition.ifPresent(
                  def -> {
                    if (def instanceof Dependency<?> dependency
                        && dependency.dataAccessSpec() instanceof VajramID vajramID) {
                      String depVajramClass =
                          vajramID
                              .className()
                              .orElseThrow(
                                  () ->
                                      new VajramValidationException(
                                          "Vajram class missing in VajramInputDefinition for :"
                                              + vajramName));
                      String[] splits = Constants.DOT_PATTERN.split(depVajramClass);
                      String depVajramClassName = splits[splits.length - 1];
                      ParsedVajramData vajramData =
                          checkNotNull(
                              vajramDefs.get(depVajramClassName),
                              "Could not find ParsedVajramData for %s",
                              depVajramClassName);
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
            getParsedVajramData().packageName(),
            getInputUtilClassName(getParsedVajramData().vajramName()),
            getInputModulationClassname(vajramName));
    final ClassName commonInputs =
        ClassName.get(
            getParsedVajramData().packageName(),
            getInputUtilClassName(getParsedVajramData().vajramName()),
            getCommonInputsClassname(vajramName));
    final TypeName vajramResponseType =
        getClassGenericArgumentsType(getParsedVajramData().vajramClass());

    MethodSpec inputDefinitionsMethod = createInputDefinitions(classLoader);
    methodSpecs.add(inputDefinitionsMethod);
    Optional<MethodSpec> inputResolverMethod = createResolvers(resolverMap, depFanoutMap);
    inputResolverMethod.ifPresent(methodSpecs::add);

    if (IOVajram.class.isAssignableFrom(getParsedVajramData().vajramClass())) {
      methodSpecs.add(
          createIOVajramExecuteMethod(
              inputsNeedingModulation,
              commonInputs,
              vajramResponseType.annotated(AnnotationSpec.builder(Nullable.class).build())));
    } else {
      methodSpecs.add(
          createComputeVajramExecuteMethod(
              vajramResponseType, inputsNeedingModulation, commonInputs));
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

  private ParsedVajramData getParsedVajramData() {
    // All InputDefinitions loaded from all vajram.yaml file with vajram name as key
    return checkNotNull(
        vajramDefs.get(vajramName), "Could not find ParsedVajramData for vajram %s", vajramName);
  }

  private static ImmutableSet<String> getResolverSources(Method resolve) {
    return stream(resolve.getParameters())
        .filter(parameter -> parameter.getAnnotationsByType(Using.class).length > 0)
        .map(parameter -> parameter.getAnnotation(Using.class))
        .map(Using::value)
        .collect(toImmutableSet());
  }

  /**
   * Method to generate "executeCompute" function code for ComputeVajrams Supported DataAccessSpec
   * => VajramID only.
   *
   * @param vajramResponseType Vajram response type
   * @param inputsNeedingModulation ClassName for the inputModulation class for the Vajram
   * @param commonInputs ClassName for the commonInputs class for the Vajram
   * @return generated code for "executeCompute" {@link MethodSpec}
   */
  private MethodSpec createComputeVajramExecuteMethod(
      TypeName vajramResponseType, ClassName inputsNeedingModulation, ClassName commonInputs) {

    Builder executeBuilder =
        methodBuilder(METHOD_EXECUTE_COMPUTE)
            .addModifiers(PUBLIC)
            .addParameter(ParameterizedTypeName.get(ImmutableList.class, Inputs.class), INPUTS_LIST)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(ImmutableMap.class),
                    ClassName.get(Inputs.class),
                    ParameterizedTypeName.get(
                        ClassName.get(ValueOrError.class), vajramResponseType)))
            .addAnnotation(Override.class);
    if (needsModulation) {
      CodeBlock.Builder codeBuilder = CodeBlock.builder();
      Map<String, Object> valueMap = new HashMap<>();
      valueMap.put(INPUTS, ClassName.get(Inputs.class));
      valueMap.put(UNMOD_INPUT, ClassName.get(UnmodulatedInput.class));
      valueMap.put(INPUT_MODULATION, inputsNeedingModulation);
      valueMap.put(COMMON_INPUT, commonInputs);
      valueMap.put(RETURN_TYPE, vajramResponseType);
      valueMap.put(VAJRAM_LOGIC_METHOD, getParsedVajramData().vajramLogic().getName());
      valueMap.put(MOD_INPUT, ClassName.get(ModulatedInput.class));
      valueMap.put(IM_MAP, ClassName.get(ImmutableMap.class));
      valueMap.put(IM_LIST, ClassName.get(ImmutableList.class));
      valueMap.put(HASH_MAP, ClassName.get(HashMap.class));
      valueMap.put(ARRAY_LIST, ClassName.get(ArrayList.class));
      valueMap.put(COM_FUTURE, ClassName.get(CompletableFuture.class));
      valueMap.put(LINK_HASH_MAP, ClassName.get(LinkedHashMap.class));
      valueMap.put(MAP, ClassName.get(Map.class));
      valueMap.put(LIST, ClassName.get(List.class));
      valueMap.put(VAL_ERR, ValueOrError.class);
      valueMap.put(FUNCTION, ClassName.get(Function.class));
      valueMap.put(OPTIONAL, ClassName.get(Optional.class));

      checkState(
          Map.class.isAssignableFrom(getParsedVajramData().vajramLogic().getReturnType()),
          "Any vajram supporting input modulation must return map",
          vajramName);
      Type type =
          ((ParameterizedType) getParsedVajramData().vajramLogic().getGenericReturnType())
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
      nonModulatedComputeMethodBuilder(executeBuilder, false);
    }
    return executeBuilder.build();
  }

  private void nonModulatedComputeMethodBuilder(Builder executeBuilder, boolean isIOVajram) {
    CodeBlock.Builder returnBuilder =
        CodeBlock.builder()
            .add(
                """
                return inputsList.stream().collect(
                     $T.toImmutableMap($T.identity(),
                     element -> {
                """,
                ImmutableMap.class,
                Function.class);
    List<CodeBlock> inputCodeBlocks = new ArrayList<>();
    inputDefsMap
        .values()
        .forEach(
            inputDef -> {
              if (inputDef instanceof Dependency<?> inputDefDependency) {
                DataAccessSpec depAccessSpec = inputDefDependency.dataAccessSpec();
                if (depAccessSpec instanceof VajramID depVajramId) {
                  String depVajramClass =
                      depVajramId
                          .className()
                          .orElseThrow(
                              () ->
                                  new VajramValidationException(
                                      "Vajram class missing in VajramInputDefinition for :"
                                          + vajramName));
                  String[] splits = Constants.DOT_PATTERN.split(depVajramClass);
                  String depPackageName =
                      stream(splits, 0, splits.length - 1).collect(Collectors.joining(DOT));
                  String depVajramClassName = splits[splits.length - 1];
                  String depRequestClass = CodegenUtils.getRequestClassName(depVajramClassName);
                  ParsedVajramData parsedVajramData =
                      checkNotNull(
                          vajramDefs.get(depVajramClassName),
                          "Could not find ParsedVajramData for %s",
                          depVajramClass);
                  final TypeName typeArgument =
                      getClassGenericArgumentsType(parsedVajramData.vajramClass());
                  final String variableName = CodegenUtils.toJavaName(inputDef.name());
                  final String depVariableName = variableName + RESPONSES_SUFFIX;
                  if (inputDefDependency.canFanout()) {
                    CodeBlock.Builder codeBlock = CodeBlock.builder();
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
                            DependencyResponse.class,
                            REQUEST,
                            ClassName.get(depPackageName, depRequestClass),
                            RESPONSE,
                            typeArgument,
                            VARIABLE,
                            inputDef.name(),
                            DEP_RESPONSE,
                            depVariableName,
                            IM_MAP,
                            ImmutableMap.class,
                            SKIPPED_EXCEPTION,
                            SkippedExecutionException.class));
                    inputCodeBlocks.add(CodeBlock.builder().add(depVariableName).build());
                    returnBuilder.add(codeBlock.build());
                  } else {
                    if (inputDefDependency.isMandatory()) {
                      inputCodeBlocks.add(
                          CodeBlock.builder()
                              .addNamed(
                                  """
                                      element.<$response:T>getDepValue($variable:S)
                                          .values()
                                          .entrySet()
                                          .iterator()
                                          .next()
                                          .getValue()
                                          .getValueOrThrow()
                                          .orElseThrow(() -> new $illegalArgument:T("Missing mandatory dependency '$variable:L' in vajram '$vajram:L'"))""",
                                  ImmutableMap.of(
                                      RESPONSE,
                                      typeArgument,
                                      VARIABLE,
                                      inputDef.name(),
                                      ILLEGAL_ARGUMENT,
                                      IllegalArgumentException.class,
                                      "vajram",
                                      vajramName))
                              .build());
                    } else {
                      inputCodeBlocks.add(
                          CodeBlock.builder()
                              .addNamed(
                                  """
                                    element.<$response:T>getDepValue($variable:S)
                                        .values()
                                        .entrySet()
                                        .iterator()
                                        .next()
                                        .getValue()
                                        .value()
                                        .orElse(null)""",
                                  ImmutableMap.of(
                                      RESPONSE, typeArgument, VARIABLE, inputDef.name()))
                              .build());
                    }
                  }
                } else {
                  throw new UnsupportedOperationException(
                      "Unknown data access spec %s".formatted(depAccessSpec));
                }
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
    if (isIOVajram) {
      Class<?> returnType = getParsedVajramData().vajramLogic().getReturnType();
      if (!CompletableFuture.class.isAssignableFrom(returnType)) {
        // TODO: Validate IOVajram response type is CompletableFuture<Type>"
        throw new VajramValidationException(
            "The VajramLogic of non-modulated IO vajram %s must return a CompletableFuture"
                .formatted(vajramName));
      }
      returnBuilder.add(
          "\nreturn ($L(new $T(\n",
          getParsedVajramData().vajramLogic().getName(),
          ClassName.get(
              packageName, getInputUtilClassName(vajramName), getAllInputsClassname(vajramName)));
    } else {
      returnBuilder.add(
          "\nreturn $T.valueOrError(() -> $L(new $T(\n",
          ValueOrError.class,
          getParsedVajramData().vajramLogic().getName(),
          ClassName.get(
              packageName, getInputUtilClassName(vajramName), getAllInputsClassname(vajramName)));
    }
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
   * @return generated code for "execute" {@link MethodSpec}
   */
  private MethodSpec createIOVajramExecuteMethod(
      ClassName inputsNeedingModulation, ClassName commonInputs, TypeName vajramResponseType) {

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
      valueMap.put(VAJRAM_LOGIC_METHOD, getParsedVajramData().vajramLogic().getName());
      valueMap.put(MOD_INPUT, ClassName.get(ModulatedInput.class));
      valueMap.put(IM_MAP, ClassName.get(ImmutableMap.class));
      valueMap.put(IM_LIST, ClassName.get(ImmutableList.class));
      valueMap.put(HASH_MAP, ClassName.get(HashMap.class));
      valueMap.put(ARRAY_LIST, ClassName.get(ArrayList.class));
      valueMap.put(COM_FUTURE, ClassName.get(CompletableFuture.class));
      valueMap.put(LINK_HASH_MAP, ClassName.get(LinkedHashMap.class));
      valueMap.put(MAP, ClassName.get(Map.class));
      valueMap.put(LIST, ClassName.get(List.class));
      valueMap.put(VAL_ERR, ValueOrError.class);
      valueMap.put(FUNCTION, ClassName.get(Function.class));
      valueMap.put(OPTIONAL, ClassName.get(Optional.class));

      checkState(
          Map.class.isAssignableFrom(getParsedVajramData().vajramLogic().getReturnType()),
          "Any vajram supporting input modulation must return map. Vajram: %s",
          getParsedVajramData().vajramName());

      Type type =
          ((ParameterizedType) getParsedVajramData().vajramLogic().getGenericReturnType())
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
      nonModulatedComputeMethodBuilder(executeMethodBuilder, true);
    }
    return executeMethodBuilder.build();
  }

  /**
   * Method to generate "resolveInputOfDependency" function code for Vajrams. If there are no
   * resolvers defined in the Vajram, {@link Optional}.empty() is returned.
   *
   * @param depFanoutMap Map of all the dependencies and their resolvers defintions are fanout or
   *     not
   * @return generated code for "resolveInputOfDependency" {@link MethodSpec}
   */
  private Optional<MethodSpec> createResolvers(
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
    if (Objects.nonNull(getParsedVajramData())) {
      resolveInputsBuilder.beginControlFlow("switch ($L) ", dependency);
      if (getParsedVajramData().resolveMethods().isEmpty()) {
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
                            String bindParamName =
                                checkNotNull(parameter.getAnnotation(Using.class)).value();
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
                      buildInputResolver(method, depFanoutMap, fanout.get());
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
   * @param method Vajram resolve method
   * @param depFanoutMap Map of all the dependencies and their resolvers defintions are fanout or
   *     not
   * @param isParamFanoutDependency Variable mentioning if the resolved variable uses a fanout
   *     dependency
   * @return {@link CodeBlock.Builder} with resolver code
   */
  private CodeBlock.Builder buildInputResolver(
      Method method, Map<String, Boolean> depFanoutMap, boolean isParamFanoutDependency) {
    Resolve resolve =
        checkNotNull(
            method.getAnnotation(Resolve.class), "Resolver method must have 'Resolve' annotation");
    String[] inputs = resolve.depInputs();
    String depName = resolve.depName();
    // check if the input is satisfied by input or other resolved variables
    CodeBlock.Builder ifBlockBuilder = CodeBlock.builder();
    ifBlockBuilder.beginControlFlow(
        "if ($T.of($S).equals(resolvableInputs))", Set.class, String.join(",", inputs));
    // TODO : add validation if fanout, then method should accept dependency response for the bind
    // type parameter else error
    // Iterate over the method params and call respective binding methods
    stream(method.getParameters())
        .forEach(
            parameter -> {
              String usingInputName =
                  checkNotNull(
                          parameter.getAnnotation(Using.class),
                          "Resolver method params must have 'Using' annotation. Vajram: %s, method %s, param: %s",
                          vajramName,
                          method.getName(),
                          parameter.getName())
                      .value();
              // check if the bind param has multiple resolvers
              if (inputDefsMap.get(usingInputName) instanceof Dependency<?>) {
                generateDependencyResolutions(
                    method, usingInputName, ifBlockBuilder, depFanoutMap, parameter);
              } else if (inputDefsMap.containsKey(usingInputName)) {
                VajramInputDefinition inputDefinition = inputDefsMap.get(usingInputName);
                String variable = toJavaName(usingInputName);
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
                              usingInputName)
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
                                usingInputName)
                            .build());
                  } else {
                    throw new VajramValidationException(
                        String.format(
                            "Optional input dependency %s must have type as Optional",
                            usingInputName));
                  }
                }
              } else {
                throw new VajramValidationException(
                    "No input resolver found for " + usingInputName);
              }
            });
    boolean isFanOut = isParamFanoutDependency || depFanoutMap.getOrDefault(depName, false);
    buildFinalResolvers(method, inputs, ifBlockBuilder, isFanOut);
    ifBlockBuilder.endControlFlow();
    return ifBlockBuilder;
  }

  /**
   * Method to generate resolver code for dependency bindings
   *
   * @param method Dependency resolver method
   * @param usingInputName The bind param name in the resolver method
   * @param ifBlockBuilder The {@link CodeBlock.Builder}
   * @param depFanoutMap Map of all the dependencies and their resolvers defintions are fanout or
   *     not
   * @param parameter the bind parameter in the resolver method
   */
  private void generateDependencyResolutions(
      Method method,
      String usingInputName,
      CodeBlock.Builder ifBlockBuilder,
      Map<String, Boolean> depFanoutMap,
      Parameter parameter) {
    VajramInputDefinition vajramInputDef = inputDefsMap.get(usingInputName);
    Resolve resolve =
        checkNotNull(method.getAnnotation(Resolve.class), "Resolver method cannot be null");
    String resolvedDep = resolve.depName();
    // fanout case
    if (depFanoutMap.containsKey(usingInputName)
        && depFanoutMap.get(usingInputName)
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
        String depVajramClass =
            vajramID
                .className()
                .orElseThrow(
                    () ->
                        new VajramValidationException(
                            "Vajram class missing in vajram input definition"));

        String variableName = CodegenUtils.toJavaName(usingInputName);
        String[] splits = Constants.DOT_PATTERN.split(depVajramClass);
        String depVajramClassName = splits[splits.length - 1];
        final ParsedVajramData parsedVajramData =
            checkNotNull(
                vajramDefs.get(depVajramClassName),
                "Could not find parsed vajram data for class %s",
                depVajramClassName);
        String depPackageName =
            stream(splits, 0, splits.length - 1).collect(Collectors.joining(DOT));
        String requestClass = CodegenUtils.getRequestClassName(depVajramClassName);

        TypeName usingDepType = getClassGenericArgumentsType(parsedVajramData.vajramClass());
        if (usingDepType.isBoxedPrimitive()) {
          usingDepType = usingDepType.unbox();
        }
        String resolverName = method.getName();
        if (DependencyResponse.class.isAssignableFrom(parameter.getType())) {
          String depValueAccessorCode =
              """
              $1T $2L =
               new $3T<>(inputs.<$4T>getDepValue($5S)
                    .values().entrySet().stream()
                    .collect($6T.toImmutableMap(e -> $7T.from(e.getKey()),
                    $8T::getValue)))""";
          ifBlockBuilder.addStatement(
              depValueAccessorCode,
              ParameterizedTypeName.get(
                  ClassName.get(DependencyResponse.class),
                  ClassName.get(depPackageName, requestClass),
                  usingDepType),
              variableName,
              DependencyResponse.class,
              usingDepType,
              usingInputName,
              ImmutableMap.class,
              ClassName.get(depPackageName, requestClass),
              ClassName.get(Map.Entry.class));
        } else {
          String depValueAccessorCode =
              """
              $1T $2L =
                inputs.<$3T>getDepValue($4S)
                   .values()
                   .entrySet()
                   .iterator()
                   .next()
                   .getValue()""";
          if (usingDepType.equals(TypeName.get(parameter.getType()))) {
            // This means this dependency in "Using" annotation is not a fanout and the dev has
            // requested the value directly. So we extract the only value from dependency response
            // and
            // provide it.
            if (vajramInputDef.isMandatory()) {
              String code =
                  depValueAccessorCode
                      + """
                              .getValueOrThrow().orElseThrow(() ->
                                  new $5T("Received null value for mandatory dependency '$6L' of vajram '$7L'"))""";
              ifBlockBuilder.addStatement(
                  code,
                  usingDepType,
                  variableName,
                  usingDepType,
                  usingInputName,
                  IllegalArgumentException.class,
                  usingInputName,
                  vajramName);
            } else {
              throw new VajramValidationException(
                  ("A resolver ('%s') must not access an optional dependency ('%s') directly."
                          + "Use Optional<>, ValueOrError<>, or DependencyResponse<> instead")
                      .formatted(resolverName, usingInputName));
            }
          } else if (ValueOrError.class.isAssignableFrom(parameter.getType())) {
            // This means this dependency in "Using" annotation is not a fanout and the dev has
            // requested the 'ValueOrError'. So we extract the only ValueOrError from dependency
            // response and provide it.
            ifBlockBuilder.addStatement(
                depValueAccessorCode,
                ParameterizedTypeName.get(ClassName.get(ValueOrError.class), usingDepType),
                variableName,
                usingDepType,
                usingInputName);
          } else if (Optional.class.isAssignableFrom(parameter.getType())) {
            // This means this dependency in "Using" annotation is not a fanout and the dev has
            // requested an 'Optional'. So we retrieve the only ValueOrError from the dependency
            // response, extract the optional and provide it.
            String code = depValueAccessorCode + ".value()";
            ifBlockBuilder.addStatement(
                code,
                ParameterizedTypeName.get(ClassName.get(Optional.class), usingDepType),
                variableName,
                usingDepType,
                usingInputName);
          } else {
            throw new VajramValidationException(
                "Unrecognized parameter type %s in resolver %s of vajram %s"
                    .formatted(parameter.getType(), resolverName, this.vajramName));
          }
        }
      }
    }
  }

  /**
   * Method to generate resolver code for variables having single resolver. Fanout case - Iterable
   * of normal type => fanout loop and create inputs - Iterable Vajram Request -
   * DependencyCommand.MultiExecute<NormalType> Non- fanout - Normal datatype - Vajram Request =>
   * toInputValues() - DependencyCommand.executeWith
   *
   * @param method Resolve method
   * @param inputs Resolve inputs
   * @param ifBlockBuilder {@link CodeBlock.Builder}
   * @param isFanOut Variable mentioning if the resolved variable uses a fanout dependency
   */
  private void buildFinalResolvers(
      Method method, String[] inputs, CodeBlock.Builder ifBlockBuilder, boolean isFanOut) {

    String variableName = "resolverResult";
    boolean controlFLowStarted = false;
    // Identify resolve method return type
    final TypeName methodReturnType = getMethodReturnType(method);

    // call the resolve method
    ifBlockBuilder.add("$T $L = $L(", methodReturnType, variableName, method.getName());
    ImmutableList<String> resolverSources = getResolverSources(method).asList();
    for (int i = 0; i < resolverSources.size(); i++) {
      String bindName = resolverSources.get(i);
      ifBlockBuilder.add("$L", CodegenUtils.toJavaName(bindName));
      if (i != method.getParameters().length - 1) {
        ifBlockBuilder.add(", ");
      }
    }
    ifBlockBuilder.add(");\n");

    if (DependencyCommand.class.isAssignableFrom(method.getReturnType())) {
      ifBlockBuilder.beginControlFlow("if($L.shouldSkip())", variableName);
      ifBlockBuilder.addStatement(
          "\t return $T.skipExecution($L.doc())", SingleExecute.class, variableName);
      ifBlockBuilder.add("} else {\n\t");
      controlFLowStarted = true;
    }
    // TODO : add missing validations if any (??)
    Class<?> klass = Primitives.wrap(method.getReturnType());
    if (MultiExecute.class.isAssignableFrom(klass)) {
      String code =
          """
              return $T.executeFanoutWith(
                  $L.inputs().stream()
                      .map(
                          element ->
                              new $T(
                                  $T.of($S, $T.withValue(element))))
                  .toList())""";
      ifBlockBuilder.addStatement(
          code,
          MultiExecute.class,
          variableName,
          Inputs.class,
          ImmutableMap.class,
          inputs[0],
          ValueOrError.class);
    } else if (isFanOut) {
      if (Iterable.class.isAssignableFrom(klass)) {
        if (VajramRequest.class.isAssignableFrom(
            (Class<?>)
                ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0])) {
          String code =
              """
                  return $T.executeFanoutWith(
                      $L.stream()
                          .map(
                              element ->
                                  element.toInputValues()))
                      .toList())""";
          ifBlockBuilder.addStatement(code, MultiExecute.class, variableName);
        } else {
          String code =
              """
                  return $T.executeFanoutWith(
                      $L.stream()
                          .map(
                              element ->
                                  new $T(
                                      $T.of($S, $T.withValue(element))))
                      .toList())""";
          ifBlockBuilder.addStatement(
              code,
              MultiExecute.class,
              variableName,
              Inputs.class,
              ImmutableMap.class,
              inputs[0],
              ValueOrError.class);
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
            "return $T.executeWith($L.toInputValues())", SingleExecute.class, variableName);
      } else if (SingleExecute.class.isAssignableFrom(klass)) {
        ifBlockBuilder.addStatement(
            """
          return $T.executeWith(new Inputs(
           $T.of($S, $T.withValue(
              $L.inputs().iterator().next().orElse(null)))))
        """,
            SingleExecute.class,
            ImmutableMap.class,
            inputs[0],
            ValueOrError.class,
            variableName);

      } else {
        ifBlockBuilder.addStatement(
            "return $T.executeWith(new Inputs(\n " + "$T.of($S, $T.withValue($L))))",
            SingleExecute.class,
            ImmutableMap.class,
            inputs[0],
            ValueOrError.class,
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
            .returns(ParameterizedTypeName.get(ImmutableList.class, VajramInputDefinition.class));
    ImmutableList<VajramInputDefinition> inputDefinitions =
        vajramInputsDefinitions.getOrDefault(vajramName, ImmutableList.of());
    Collection<CodeBlock> codeBlocks = new ArrayList<>(inputDefinitions.size());
    // Input and Dependency code block
    inputDefinitions.forEach(
        vajramInputDefinition -> {
          CodeBlock.Builder inputDefBuilder = CodeBlock.builder();
          if (vajramInputDefinition instanceof Input<?> input) {
            buildVajramInput(inputDefBuilder, input);
          } else if (vajramInputDefinition instanceof Dependency<?> dependency) {
            buildVajramDependency(classLoader, inputDefBuilder, dependency);
          }
          codeBlocks.add(inputDefBuilder.build());
        });

    inputDefinitionsBuilder.beginControlFlow("if(this.$L == null)", INPUT_DEFINITIONS_VAR);
    inputDefinitionsBuilder.addCode(
        CodeBlock.builder()
            .add("this.$L = $T.of(\n", INPUT_DEFINITIONS_VAR, ImmutableList.class)
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
      ClassLoader classLoader, CodeBlock.Builder inputDefBuilder, Dependency<?> dependency) {
    inputDefBuilder
        .add("$T.builder()", ClassName.get(Dependency.class))
        .add(".name($S)", dependency.name());
    DataAccessSpec dataAccessSpec = dependency.dataAccessSpec();
    if (dataAccessSpec instanceof VajramID vajramID) {
      String code = ".dataAccessSpec($1T.vajramID($2S))";
      if (vajramID.className().isPresent()) {
        try {
          //noinspection unchecked
          Optional<String> vajramId =
              getVajramIdString(
                  (Class<? extends Vajram<?>>) classLoader.loadClass(vajramID.className().get()));
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
      inputDefBuilder.addNamed(sources, ImmutableMap.of(INPUT_SRC, InputSource.class)).add(")");
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
    if (input.tags() != null && !input.tags().isEmpty()) {
      inputDefBuilder.add(".tags($T.of(", ClassName.get(Map.class));
      String tags =
          input.tags().values().stream()
              .filter(tag -> tag != null && tag.tagValue() != null)
              .map(
                  tag -> {
                    return String.format("\"%s\", \"%s\"", tag.tagKey(), tag.tagValue());
                  })
              .collect(Collectors.joining(COMMA));
      inputDefBuilder.add(tags).add("))");
    }
    // last line
    inputDefBuilder.add(".build()");
  }

  public String codeGenVajramRequest() {
    VajramInputsDef vajramInputsDef = vajramInputFile.vajramInputsDef();
    ImmutableList<InputDef> inputDefs = vajramInputsDef.inputs();
    Builder requestConstructor = constructorBuilder().addModifiers(PRIVATE);
    ClassName builderClassType =
        ClassName.get(packageName + Constants.DOT_SEPARATOR + requestClassName, "Builder");
    TypeSpec.Builder requestClass =
        classBuilder(requestClassName)
            .addModifiers(PUBLIC, FINAL)
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
            .addModifiers(PUBLIC, STATIC, FINAL)
            .addAnnotation(EqualsAndHashCode.class)
            .addMethod(constructorBuilder().addModifiers(PRIVATE).build());
    Set<String> inputNames = new LinkedHashSet<>();
    List<AbstractInput> allInputs = vajramInputsDef.allInputsStream().toList();
    List<FieldSpec.Builder> inputNameFields = new ArrayList<>(allInputs.size());
    List<FieldSpec.Builder> inputSpecFields = new ArrayList<>(allInputs.size());
    for (AbstractInput abstractInput : allInputs) {
      String inputJavaName = toJavaName(abstractInput.getName());
      TypeAndName javaType = getTypeName(abstractInput.toDataType());
      ClassName vajramClassName = ClassName.get(packageName, vajramName);

      String inputNameFieldName = inputJavaName + INPUT_NAME_SUFFIX;
      FieldSpec.Builder inputNameField =
          FieldSpec.builder(String.class, inputNameFieldName)
              .initializer("\"$L\"", abstractInput.getName());

      FieldSpec.Builder inputSpecField;
      if (abstractInput instanceof VajramDependencyDef vajramDepDef) {
        ClassName specType =
            ClassName.get(
                vajramDepDef.canFanout()
                    ? VajramDepFanoutTypeSpec.class
                    : VajramDepSingleTypeSpec.class);
        inputSpecField =
            FieldSpec.builder(
                    ParameterizedTypeName.get(
                        specType,
                        javaType.typeName(),
                        vajramClassName,
                        ClassName.bestGuess(vajramDepDef.getVajramClass())),
                    inputJavaName + INPUT_SPEC_SUFFIX)
                .initializer(
                    "new $T<>($L, $T.class, $T.class)",
                    specType,
                    inputNameFieldName,
                    vajramClassName,
                    ClassName.bestGuess(vajramDepDef.getVajramClass()));

      } else {
        inputSpecField =
            FieldSpec.builder(
                    ParameterizedTypeName.get(
                        ClassName.get(VajramInputTypeSpec.class),
                        javaType.typeName(),
                        vajramClassName),
                    inputJavaName + INPUT_SPEC_SUFFIX)
                .initializer(
                    "new $T<>($L, $T.class)",
                    VajramInputTypeSpec.class,
                    inputNameFieldName,
                    vajramClassName);
      }
      inputNameFields.add(inputNameField.addModifiers(STATIC, FINAL));
      inputSpecFields.add(inputSpecField.addModifiers(STATIC, FINAL));
      if (abstractInput instanceof InputDef input
          && input.toInputDefinition().sources().contains(InputSource.CLIENT)) {
        inputSpecField.addModifiers(PUBLIC);
        inputNameField.addModifiers(PUBLIC);
      } else {
        continue;
      }
      inputNames.add(inputJavaName);

      requestClass.addField(
          FieldSpec.builder(
                  wrapPrimitive(javaType)
                      .typeName()
                      .annotated(AnnotationSpec.builder(Nullable.class).build()),
                  inputJavaName,
                  PRIVATE,
                  FINAL)
              .build());
      builderClass.addField(
          FieldSpec.builder(
                  javaType.typeName().annotated(AnnotationSpec.builder(Nullable.class).build()),
                  inputJavaName,
                  PRIVATE)
              .build());
      requestConstructor.addParameter(
          ParameterSpec.builder(
                  javaType.typeName().annotated(AnnotationSpec.builder(Nullable.class).build()),
                  inputJavaName)
              .build());
      requestConstructor.addStatement("this.$L = $L", inputJavaName, inputJavaName);
      requestClass.addMethod(getterCodeForInput(input, inputJavaName, javaType));

      builderClass.addMethod(
          // public inputName(){return this.inputName;}
          methodBuilder(inputJavaName)
              .addModifiers(PUBLIC)
              .returns(
                  javaType.typeName().annotated(AnnotationSpec.builder(Nullable.class).build()))
              .addStatement("return this.$L", inputJavaName) // Return
              .build());

      builderClass.addMethod(
          // public inputName(Type inputName){this.inputName = inputName; return this;}
          methodBuilder(inputJavaName)
              .returns(builderClassType)
              .addModifiers(PUBLIC)
              .addParameter(
                  ParameterSpec.builder(
                          javaType
                              .typeName()
                              .annotated(AnnotationSpec.builder(Nullable.class).build()),
                          inputJavaName)
                      .build())
              .addStatement("this.$L = $L", inputJavaName, inputJavaName) // Set value
              .addStatement("return this", inputJavaName) // Return
              .build());
    }

    requestClass.addFields(inputNameFields.stream().map(FieldSpec.Builder::build)::iterator);
    requestClass.addFields(inputSpecFields.stream().map(FieldSpec.Builder::build)::iterator);

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
    //noinspection rawtypes
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
          "builder.put($S, $T.withValue(this.$L))",
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
    return getTypeName(dataType, List.of());
  }

  private static TypeAndName getTypeName(DataType dataType, List<AnnotationSpec> typeAnnotations) {
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
                        .toArray(TypeName[]::new))
                .annotated(typeAnnotations));
      } else {
        return new TypeAndName(className.annotated(typeAnnotations));
      }
    } else {
      Optional<Type> javaType = getJavaType(dataType);
      return new TypeAndName(
          javaType
              .map(type -> (type instanceof Class<?> clazz) ? Primitives.wrap(clazz) : type)
              .map(TypeName::get)
              .orElseThrow(
                  () -> {
                    return new IllegalArgumentException(
                        "Could not determine java Type of %s".formatted(dataType));
                  })
              .annotated(typeAnnotations),
          javaType);
    }
  }

  private static MethodSpec getterCodeForInput(
      AbstractInput input, String name, TypeAndName typeAndName) {
    boolean wrapWithOptional =
        !input.isMandatory()
            && (input instanceof InputDef
                || (input instanceof DependencyDef dependencyDef && !dependencyDef.canFanout()));
    return methodBuilder(name)
        .returns(
            (wrapWithOptional
                ? optional(
                    wrapPrimitive(typeAndName)
                        .typeName()
                        // Remove @Nullable because Optional<@Nullable T> is not useful.
                        .withoutAnnotations())
                : unwrapPrimitive(typeAndName)
                    .typeName()
                    // Remove @Nullable because getter has null check
                    // and will never return null.
                    .withoutAnnotations()))
        .addModifiers(PUBLIC)
        .addCode(
            !wrapWithOptional
                ? CodeBlock.of(
                    """
                      if($L == null) {
                        throw new IllegalStateException("The input '$L' is not optional, but has null value. This should not happen");
                      }""",
                    name,
                    name)
                : CodeBlock.builder().build())
        .addCode(
            wrapWithOptional
                /*Generates:
                  public Optional<Type> inputName(){
                    return Optional.ofNullable(this.inputName);
                  }
                */
                ? CodeBlock.builder()
                    .addStatement("return $T.ofNullable(this.$L)", Optional.class, name)
                    .build()
                /*Generates:
                  public Type inputName(){
                    return this.inputName;
                  }
                */
                : CodeBlock.builder().addStatement("return this.$L", name).build())
        .build();
  }

  public String codeGenInputUtil() {
    boolean doInputsNeedModulation =
        vajramInputFile
            .vajramInputsDef()
            .allInputsStream()
            .map(AbstractInput::toInputDefinition)
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
              TypeAndName javaType =
                  getTypeName(
                      inputDef.toInputDefinition().type(),
                      List.of(AnnotationSpec.builder(Nullable.class).build()));
              allInputsClass.addField(javaType.typeName(), inputJavaName, PRIVATE, FINAL);
              allInputsClass.addMethod(getterCodeForInput(inputDef, inputJavaName, javaType));
              fieldsList.add(new FieldTypeName(javaType.typeName(), inputJavaName));
            });

    vajramInputsDef
        .dependencies()
        .forEach(
            dependencyDef -> {
              TypeAndName typeAndName = getDependencyOutputsType(dependencyDef);
              String inputJavaName = toJavaName(dependencyDef.getName());
              allInputsClass.addField(typeAndName.typeName(), inputJavaName, PRIVATE, FINAL);
              allInputsClass.addMethod(
                  getterCodeForInput(dependencyDef, inputJavaName, typeAndName));
              fieldsList.add(new FieldTypeName(typeAndName.typeName(), inputJavaName));
            });

    // generate all args constructor and add to class
    generateConstructor(fieldsList).ifPresent(allInputsClass::addMethod);

    StringWriter writer = new StringWriter();
    try {
      JavaFile.builder(packageName, inputUtilClass.addType(allInputsClass.build()).build())
          .build()
          .writeTo(writer);
    } catch (IOException ignored) {

    }
    return writer.toString();
  }

  private Optional<MethodSpec> generateConstructor(List<FieldTypeName> fieldsList) {
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

  private static TypeAndName getDependencyOutputsType(DependencyDef dependencyDef) {
    if (dependencyDef instanceof VajramDependencyDef vajramDepSpec) {
      if (vajramDepSpec.canFanout()) {
        String depVajramClass = vajramDepSpec.getVajramClass();
        int lastDotIndex = depVajramClass.lastIndexOf(Constants.DOT_SEPARATOR);
        String depRequestClass =
            CodegenUtils.getRequestClassName(depVajramClass.substring(lastDotIndex + 1));
        String depPackageName = depVajramClass.substring(0, lastDotIndex);
        return new TypeAndName(
            ParameterizedTypeName.get(
                ClassName.get(DependencyResponse.class),
                ClassName.get(depPackageName, depRequestClass),
                getTypeName(vajramDepSpec.toDataType()).typeName()));
      } else {
        return getTypeName(
            vajramDepSpec.toDataType(), List.of(AnnotationSpec.builder(Nullable.class).build()));
      }
    } else {
      throw new UnsupportedOperationException(
          "Unknown dependency type %s".formatted(dependencyDef.getClass()));
    }
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
                TypeAndName javaType =
                    getTypeName(
                        inputDef.toInputDefinition().type(),
                        List.of(AnnotationSpec.builder(Nullable.class).build()));
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
                TypeAndName typeAndName = getDependencyOutputsType(dependencyDef);
                String inputJavaName = toJavaName(dependencyDef.getName());
                commonInputs.addField(typeAndName.typeName(), inputJavaName, PRIVATE, FINAL);
                commonInputs.addMethod(
                    getterCodeForInput(dependencyDef, inputJavaName, typeAndName));
                ciFieldsList.add(new FieldTypeName(typeAndName.typeName(), inputJavaName));
              });
      // create constructors
      generateConstructor(ciFieldsList).ifPresent(commonInputs::addMethod);
      generateConstructor(imFieldsList).ifPresent(inputsNeedingModulation::addMethod);

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
                    ParameterizedTypeName.get(ImmutableList.class, VajramInputDefinition.class)
                        .annotated(AnnotationSpec.builder(Nullable.class).build()),
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
