package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.codegen.Constants.ARRAY_LIST;
import static com.flipkart.krystal.vajram.codegen.Constants.COMMON_INPUT;
import static com.flipkart.krystal.vajram.codegen.Constants.COM_FUTURE;
import static com.flipkart.krystal.vajram.codegen.Constants.DEP_RESP;
import static com.flipkart.krystal.vajram.codegen.Constants.DEP_RESPONSE;
import static com.flipkart.krystal.vajram.codegen.Constants.FACETS_FIELDS_VAR;
import static com.flipkart.krystal.vajram.codegen.Constants.FACET_DEFINITIONS_VAR;
import static com.flipkart.krystal.vajram.codegen.Constants.FACET_NAME_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.FACET_SPEC_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.FUNCTION;
import static com.flipkart.krystal.vajram.codegen.Constants.GET_FACET_DEFINITIONS;
import static com.flipkart.krystal.vajram.codegen.Constants.HASH_MAP;
import static com.flipkart.krystal.vajram.codegen.Constants.IM_LIST;
import static com.flipkart.krystal.vajram.codegen.Constants.IM_MAP;
import static com.flipkart.krystal.vajram.codegen.Constants.INPUTS;
import static com.flipkart.krystal.vajram.codegen.Constants.INPUTS_LIST;
import static com.flipkart.krystal.vajram.codegen.Constants.INPUT_BATCHING;
import static com.flipkart.krystal.vajram.codegen.Constants.INPUT_BATCHING_FUTURE_CODE_BLOCK;
import static com.flipkart.krystal.vajram.codegen.Constants.INPUT_SRC;
import static com.flipkart.krystal.vajram.codegen.Constants.LINK_HASH_MAP;
import static com.flipkart.krystal.vajram.codegen.Constants.LIST;
import static com.flipkart.krystal.vajram.codegen.Constants.MAP;
import static com.flipkart.krystal.vajram.codegen.Constants.METHOD_EXECUTE;
import static com.flipkart.krystal.vajram.codegen.Constants.METHOD_EXECUTE_COMPUTE;
import static com.flipkart.krystal.vajram.codegen.Constants.METHOD_GET_FACETS_CONVERTOR;
import static com.flipkart.krystal.vajram.codegen.Constants.METHOD_RESOLVE_INPUT_OF_DEPENDENCY;
import static com.flipkart.krystal.vajram.codegen.Constants.MOD_INPUT;
import static com.flipkart.krystal.vajram.codegen.Constants.OPTIONAL;
import static com.flipkart.krystal.vajram.codegen.Constants.REQUEST;
import static com.flipkart.krystal.vajram.codegen.Constants.RESOLVABLE_INPUTS;
import static com.flipkart.krystal.vajram.codegen.Constants.RESPONSE;
import static com.flipkart.krystal.vajram.codegen.Constants.RESPONSES_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.RETURN_TYPE;
import static com.flipkart.krystal.vajram.codegen.Constants.SKIPPED_EXCEPTION;
import static com.flipkart.krystal.vajram.codegen.Constants.STACKTRACELESS_ARGUMENT;
import static com.flipkart.krystal.vajram.codegen.Constants.UNMOD_INPUT;
import static com.flipkart.krystal.vajram.codegen.Constants.VAJRAM_LOGIC_METHOD;
import static com.flipkart.krystal.vajram.codegen.Constants.VAL_ERR;
import static com.flipkart.krystal.vajram.codegen.Constants.VARIABLE;
import static com.flipkart.krystal.vajram.codegen.Constants._FACETS_CLASS;
import static com.flipkart.krystal.vajram.codegen.Utils.COMMA;
import static com.flipkart.krystal.vajram.codegen.Utils.CONVERTER;
import static com.flipkart.krystal.vajram.codegen.Utils.DOT;
import static com.flipkart.krystal.vajram.codegen.Utils.getAllFacetsClassname;
import static com.flipkart.krystal.vajram.codegen.Utils.getBatchedFacetsClassname;
import static com.flipkart.krystal.vajram.codegen.Utils.getCommonFacetsClassname;
import static com.flipkart.krystal.vajram.codegen.Utils.getFacetUtilClassName;
import static com.flipkart.krystal.vajram.codegen.Utils.getTypeParameters;
import static com.flipkart.krystal.vajram.codegen.Utils.getVajramImplClassName;
import static com.flipkart.krystal.vajram.codegen.models.ParsedVajramData.fromVajram;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.Arrays.stream;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.except.SkippedExecutionException;
import com.flipkart.krystal.except.StackTracelessException;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.batching.BatchedFacets;
import com.flipkart.krystal.vajram.batching.FacetsConverter;
import com.flipkart.krystal.vajram.batching.UnBatchedFacets;
import com.flipkart.krystal.vajram.codegen.models.DependencyModel;
import com.flipkart.krystal.vajram.codegen.models.FacetGenModel;
import com.flipkart.krystal.vajram.codegen.models.InputModel;
import com.flipkart.krystal.vajram.codegen.models.ParsedVajramData;
import com.flipkart.krystal.vajram.codegen.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.models.VajramInfoLite;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.DependencyDef;
import com.flipkart.krystal.vajram.facets.FacetContainer;
import com.flipkart.krystal.vajram.facets.FacetValuesAdaptor;
import com.flipkart.krystal.vajram.facets.InputDef;
import com.flipkart.krystal.vajram.facets.InputSource;
import com.flipkart.krystal.vajram.facets.MultiExecute;
import com.flipkart.krystal.vajram.facets.SingleExecute;
import com.flipkart.krystal.vajram.facets.VajramDepFanoutTypeSpec;
import com.flipkart.krystal.vajram.facets.VajramDepSingleTypeSpec;
import com.flipkart.krystal.vajram.facets.VajramFacetDefinition;
import com.flipkart.krystal.vajram.facets.VajramFacetSpec;
import com.flipkart.krystal.vajram.facets.resolution.sdk.Resolve;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"HardcodedLineSeparator", "OverlyComplexClass"})
@Slf4j
public class VajramCodeGenerator {

  private final String packageName;
  private final ProcessingEnvironment processingEnv;
  private final String requestClassName;
  private final VajramInfo vajramInfo;
  private final String vajramName;
  private final Map<VajramID, VajramInfoLite> vajramDefs;
  private final Map<String, FacetGenModel> facetModels;
  private final boolean needsBatching;
  private @MonotonicNonNull ParsedVajramData parsedVajramData;
  private final Utils util;

  public VajramCodeGenerator(
      VajramInfo vajramInfo,
      Map<VajramID, VajramInfoLite> vajramDefs,
      ProcessingEnvironment processingEnv,
      Utils util) {
    this.vajramInfo = vajramInfo;
    this.vajramName = vajramInfo.vajramId().vajramId();
    this.packageName = vajramInfo.packageName();
    this.processingEnv = processingEnv;
    this.util = util;
    this.requestClassName = Utils.getRequestClassName(vajramName);
    // All parsed Vajram data loaded from all Vajram class files with vajram name as key
    this.vajramDefs = Collections.unmodifiableMap(vajramDefs);
    // All the present Vajram -> VajramFacetDefinitions map with name as key
    this.facetModels =
        vajramInfo
            .facetStream()
            .collect(
                Collectors.toMap(
                    FacetGenModel::name,
                    Function.identity(),
                    (o1, o2) -> o1,
                    LinkedHashMap::new)); // need ordered map for dependencies
    this.needsBatching = vajramInfo.inputs().stream().anyMatch(InputModel::isBatched);
  }

  public String getVajramName() {
    return vajramName;
  }

  /**
   * Method to generate VajramImpl class Input dependencyDef code gen Resolve method code gen Vajram
   * logic code gen Compute vajram execute IO vajram executeBlocking
   *
   * @return Class code as string
   */
  public String codeGenVajramImpl() {
    initParsedVajramData();
    final TypeSpec.Builder vajramImplClass =
        util.classBuilder(getVajramImplClassName(vajramName))
            .addField(
                FieldSpec.builder(
                        ParameterizedTypeName.get(ImmutableList.class, VajramFacetDefinition.class)
                            .annotated(AnnotationSpec.builder(Nullable.class).build()),
                        FACET_DEFINITIONS_VAR)
                    .addModifiers(PRIVATE)
                    .build());
    List<MethodSpec> methodSpecs = new ArrayList<>();
    // Add superclass
    vajramImplClass
        .addModifiers(PUBLIC, FINAL)
        .superclass(ClassName.get(vajramInfo.vajramClass()).box())
        .build();

    // Map of all the resolved variables to the methods resolving them
    Map<String, List<ExecutableElement>> resolverMap = new HashMap<>();
    for (ExecutableElement resolver : getParsedVajramData().resolvers()) {
      String key = checkNotNull(resolver.getAnnotation(Resolve.class)).depName();
      resolverMap.computeIfAbsent(key, _k -> new ArrayList<>()).add(resolver);
    }
    // Iterate all the resolvers and figure fanout
    // dep inputDef data type and method return type =>
    // 1. depInput = T, if (resolverReturnType is iterable of T || iterable of vajramRequest ||
    // multiExecute) => fanout
    final Map<String, Boolean> depFanoutMap =
        vajramInfo.dependencies().stream()
            .collect(Collectors.toMap(DependencyModel::name, DependencyModel::canFanout));

    // Initialize few common attributes and data structures
    final ClassName inputBatch =
        ClassName.get(
            getParsedVajramData().packageName(),
            getFacetUtilClassName(getParsedVajramData().vajramInfo().vajramId().vajramId()),
            getBatchedFacetsClassname(vajramName));
    final ClassName commonInputs =
        ClassName.get(
            getParsedVajramData().packageName(),
            getFacetUtilClassName(getParsedVajramData().vajramInfo().vajramId().vajramId()),
            getCommonFacetsClassname(vajramName));
    final TypeName vajramResponseType =
        util.toTypeName(getParsedVajramData().vajramInfo().responseType());

    MethodSpec facetDefinitionsMethod = createFacetDefinitions();
    methodSpecs.add(facetDefinitionsMethod);
    Optional<MethodSpec> inputResolverMethod = createResolvers(resolverMap, depFanoutMap);
    inputResolverMethod.ifPresent(methodSpecs::add);

    if (util.isRawAssignable(
        getParsedVajramData().vajramInfo().vajramClass().asType(), IOVajram.class)) {
      methodSpecs.add(
          createIOVajramExecuteMethod(
              inputBatch,
              commonInputs,
              vajramResponseType.box().annotated(AnnotationSpec.builder(Nullable.class).build())));
    } else {
      methodSpecs.add(createComputeVajramExecuteMethod(vajramResponseType));
    }
    if (needsBatching) {
      methodSpecs.add(createBatchFacetConvertersMethod(inputBatch, commonInputs));
    }

    StringWriter writer = new StringWriter();
    try {
      JavaFile.builder(packageName, vajramImplClass.addMethods(methodSpecs).build())
          .indent("  ")
          .build()
          .writeTo(writer);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return writer.toString();
  }

  private @NonNull ParsedVajramData initParsedVajramData() {
    if (parsedVajramData == null) {
      this.parsedVajramData =
          fromVajram(vajramInfo, util)
              .orElseThrow(
                  () ->
                      new VajramValidationException(
                          """
                            Could not load Vajram class for vajram %s.
                            ParsedVajram Data should never be accessed in model generation phase."""
                              .formatted(vajramInfo.vajramId())));
    }
    return parsedVajramData;
  }

  private ParsedVajramData getParsedVajramData() {
    // This should not happen since this method is only ever called after
    // initParsedVajramData is called. But we still implement the best effort fallback
    return Optional.ofNullable(parsedVajramData).orElseGet(this::initParsedVajramData);
  }

  private ImmutableSet<String> getResolverSources(ExecutableElement resolve) {
    return resolve.getParameters().stream().map(util::inferFacetName).collect(toImmutableSet());
  }

  /**
   * Method to generate "executeCompute" function code for ComputeVajrams Supported DataAccessSpec
   * => VajramID only.
   *
   * @param vajramResponseType Vajram response type
   * @return generated code for "executeCompute" {@link MethodSpec}
   */
  private MethodSpec createComputeVajramExecuteMethod(TypeName vajramResponseType) {

    MethodSpec.Builder executeBuilder =
        methodBuilder(METHOD_EXECUTE_COMPUTE)
            .addModifiers(PUBLIC)
            .addParameter(ParameterizedTypeName.get(ImmutableList.class, Facets.class), INPUTS_LIST)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(ImmutableMap.class),
                    ClassName.get(Facets.class),
                    ParameterizedTypeName.get(
                        ClassName.get(Errable.class), vajramResponseType.box())))
            .addAnnotation(Override.class);
    if (needsBatching) {
      String message = "Batching is not supported in ComputeVajrams";
      util.error(
          message,
          getParsedVajramData().vajramInfo().inputs().stream()
              .filter(InputModel::isBatched)
              .findAny()
              .<Element>map(InputModel::facetField)
              .orElse(getParsedVajramData().vajramInfo().vajramClass()));
      throw new VajramValidationException(message);
    } else { // TODO : Need non batched IO vajram to test this
      nonBatchedExecuteMethodBuilder(executeBuilder, false);
    }
    return executeBuilder.build();
  }

  private void nonBatchedExecuteMethodBuilder(
      MethodSpec.Builder executeBuilder, boolean isIOVajram) {
    CodeBlock.Builder returnBuilder =
        CodeBlock.builder()
            .add(
                """
                return facetsList.stream().collect(
                     $T.toImmutableMap($T.identity(),
                     element -> {
                """,
                ImmutableMap.class,
                Function.class);
    List<CodeBlock> inputCodeBlocks = new ArrayList<>();
    ClassName allFacetsClass =
        ClassName.get(
            packageName, getFacetUtilClassName(vajramName), getAllFacetsClassname(vajramName));
    List<? extends VariableElement> outputLogicParams =
        getParsedVajramData().outputLogic().getParameters();
    boolean outputLogicNeedsAllFacetsObject;
    List<FacetGenModel> neededFacetsInOrder;
    if (outputLogicParams.size() == 1
        && util.getProcessingEnv()
            .getTypeUtils()
            .isAssignable(
                outputLogicParams.get(0).asType(),
                util.getTypeElement(allFacetsClass.canonicalName()).asType())) {
      // Since the execute method consumes the all facets class, it means the output logic needs all
      // facets of the vajram except the output facet.
      neededFacetsInOrder = facetModels.values().stream().toList();
      outputLogicNeedsAllFacetsObject = true;
    } else {
      neededFacetsInOrder =
          outputLogicParams.stream()
              .map(
                  param -> {
                    String facetName = util.inferFacetName(param);
                    FacetGenModel facet = facetModels.get(facetName);
                    if (facet == null) {
                      String message = "Could not find a facet with name %s".formatted(facetName);
                      util.error(message, param);
                      throw new VajramValidationException(message);
                    }
                    return facet;
                  })
              .toList();
      outputLogicNeedsAllFacetsObject = false;
    }

    neededFacetsInOrder.forEach(
        inputDef -> {
          if (inputDef instanceof DependencyModel dependencyModel) {
            VajramID depVajramId = dependencyModel.depVajramId();
            String depRequestClass = dependencyModel.depReqClassQualifiedName();
            VajramInfoLite depVajramInfo =
                checkNotNull(
                    vajramDefs.get(depVajramId),
                    "Could not find ParsedVajramData for %s",
                    depVajramId);
            final TypeName boxedResponseType = util.toTypeName(depVajramInfo.responseType()).box();
            final String variableName = toJavaName(inputDef.name());
            final String depVariableName = variableName + RESPONSES_SUFFIX;
            if (dependencyModel.canFanout()) {
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
                      toClassName(depRequestClass),
                      RESPONSE,
                      boxedResponseType,
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
              if (dependencyModel.isMandatory()) {
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
                                        .orElseThrow(() -> new $stacktracelessArgument:T("Missing mandatory dependency '$variable:L' in vajram '$vajram:L'"))""",
                            ImmutableMap.of(
                                RESPONSE,
                                boxedResponseType,
                                VARIABLE,
                                inputDef.name(),
                                STACKTRACELESS_ARGUMENT,
                                StackTracelessException.class,
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
                                      .getValue()""",
                            ImmutableMap.of(RESPONSE, boxedResponseType, VARIABLE, inputDef.name()))
                        .build());
              }
            }
          } else {
            // call output logic method with all inputDef values
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
    String methodCallSuffix;
    if (isIOVajram) {
      TypeMirror returnType = getParsedVajramData().outputLogic().getReturnType();
      if (!util.isRawAssignable(returnType, CompletableFuture.class)) {
        // TODO: Validate IOVajram response type is CompletableFuture<Type>"
        String errorMessage =
            "The OutputLogic of non-batched IO vajram %s must return a CompletableFuture"
                .formatted(vajramName);
        util.error(errorMessage, getParsedVajramData().outputLogic());
        throw new VajramValidationException(errorMessage);
      }
      if (outputLogicNeedsAllFacetsObject) {
        returnBuilder.add(
            "\nreturn ($L(new $T(\n",
            getParsedVajramData().outputLogic().getSimpleName(),
            allFacetsClass);
        methodCallSuffix = ")));\n";
      } else {
        returnBuilder.add("\nreturn $L(\n", getParsedVajramData().outputLogic().getSimpleName());
        methodCallSuffix = ");\n";
      }
    } else {
      if (outputLogicNeedsAllFacetsObject) {
        returnBuilder.add(
            "\nreturn $T.errableFrom(() -> $L(new $T(\n",
            Errable.class,
            getParsedVajramData().outputLogic().getSimpleName(),
            allFacetsClass);
        methodCallSuffix = ")));\n";
      } else {
        returnBuilder.add(
            "\nreturn $T.errableFrom(() -> $L(\n",
            Errable.class,
            getParsedVajramData().outputLogic().getSimpleName());
        methodCallSuffix = "));\n";
      }
    }
    // merge the code blocks for facets
    for (int i = 0; i < inputCodeBlocks.size(); i++) {
      // for formatting
      returnBuilder.add("\t\t");
      returnBuilder.add(inputCodeBlocks.get(i));
      if (i != inputCodeBlocks.size() - 1) {
        returnBuilder.add(",\n");
      }
    }
    returnBuilder.add(methodCallSuffix);
    returnBuilder.add("}));\n");
    executeBuilder.addCode(returnBuilder.build());
  }

  /**
   * Method to generate "getFacetsConvertor" function
   *
   * @param batchableInputs Generated Vajram specific InputUtil.BatchableInputs class
   * @param commonInputs Generated Vajram specific InputUtil.CommonInputs class
   * @return {@link MethodSpec}
   */
  private MethodSpec createBatchFacetConvertersMethod(
      ClassName batchableInputs, ClassName commonInputs) {
    MethodSpec.Builder inputConvertersBuilder =
        methodBuilder(METHOD_GET_FACETS_CONVERTOR)
            .addModifiers(PUBLIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(Optional.class),
                    ParameterizedTypeName.get(
                        ClassName.get(FacetsConverter.class), batchableInputs, commonInputs)))
            .addAnnotation(Override.class);
    inputConvertersBuilder.addCode(
        CodeBlock.builder()
            .addStatement(
                "return $T.of($T.%s)".formatted(CONVERTER),
                Optional.class,
                ClassName.get(packageName, getFacetUtilClassName(vajramName)))
            .build());
    return inputConvertersBuilder.build();
  }

  /**
   * Method to generate "execute" function code for IOVajrams
   *
   * @param batchableInputs Generated Vajramspecific InputUtil.BatchedInputs class
   * @param commonFacets Generated Vajram specific InputUtil.CommonInputs class
   * @param vajramResponseType Vajram response type
   * @return generated code for "execute" {@link MethodSpec}
   */
  private MethodSpec createIOVajramExecuteMethod(
      ClassName batchableInputs, ClassName commonFacets, TypeName vajramResponseType) {

    MethodSpec.Builder executeMethodBuilder =
        methodBuilder(METHOD_EXECUTE)
            .addModifiers(PUBLIC)
            .addParameter(ParameterizedTypeName.get(ImmutableList.class, Facets.class), INPUTS_LIST)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(ImmutableMap.class),
                    ClassName.get(Facets.class),
                    ParameterizedTypeName.get(
                        ClassName.get(CompletableFuture.class), vajramResponseType)))
            .addAnnotation(Override.class);

    CodeBlock.Builder codeBuilder = CodeBlock.builder();
    if (needsBatching) {
      ExecutableElement outputLogic = getParsedVajramData().outputLogic();

      Map<String, Object> valueMap = new HashMap<>();
      valueMap.put(INPUTS, ClassName.get(Facets.class));
      valueMap.put(UNMOD_INPUT, ClassName.get(UnBatchedFacets.class));
      valueMap.put(INPUT_BATCHING, batchableInputs);
      valueMap.put(
          Constants.FACET_UTIL, ClassName.get(packageName, getFacetUtilClassName(vajramName)));
      valueMap.put(COMMON_INPUT, commonFacets);
      valueMap.put(RETURN_TYPE, vajramResponseType);
      valueMap.put(VAJRAM_LOGIC_METHOD, outputLogic.getSimpleName());
      valueMap.put(MOD_INPUT, ClassName.get(BatchedFacets.class));
      valueMap.put(IM_MAP, ClassName.get(ImmutableMap.class));
      valueMap.put(IM_LIST, ClassName.get(ImmutableList.class));
      valueMap.put(HASH_MAP, ClassName.get(HashMap.class));
      valueMap.put(ARRAY_LIST, ClassName.get(ArrayList.class));
      valueMap.put(COM_FUTURE, ClassName.get(CompletableFuture.class));
      valueMap.put(LINK_HASH_MAP, ClassName.get(LinkedHashMap.class));
      valueMap.put(MAP, ClassName.get(Map.class));
      valueMap.put(LIST, ClassName.get(List.class));
      valueMap.put(VAL_ERR, Errable.class);
      valueMap.put(FUNCTION, ClassName.get(Function.class));
      valueMap.put(OPTIONAL, ClassName.get(Optional.class));

      TypeMirror returnType = outputLogic.getReturnType();
      checkState(
          util.isRawAssignable(processingEnv.getTypeUtils().erasure(returnType), Map.class),
          "A vajram supporting input batching must return map. Vajram: %s",
          vajramName);
      TypeMirror mapValue = getTypeParameters(returnType).get(1);
      if (!util.isRawAssignable(mapValue, CompletableFuture.class)) {
        String message =
            """
                Batched IO Vajram should return a map whose value type must be `CompletableFuture`.
                Violating vajram: %s"""
                .formatted(vajramName);
        util.error(message, outputLogic);
        throw new VajramValidationException(message);
      }
      codeBuilder.addNamed(INPUT_BATCHING_FUTURE_CODE_BLOCK, valueMap);
      executeMethodBuilder.addCode(codeBuilder.build());
    } else {
      nonBatchedExecuteMethodBuilder(executeMethodBuilder, true);
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
      Map<String, ? extends List<ExecutableElement>> resolverMap,
      Map<String, Boolean> depFanoutMap) {
    String dependencyDef = "dependencyDef";
    MethodSpec.Builder resolveInputsBuilder =
        methodBuilder(METHOD_RESOLVE_INPUT_OF_DEPENDENCY)
            .addModifiers(PUBLIC)
            .addParameter(String.class, dependencyDef)
            .addParameter(
                ParameterizedTypeName.get(ImmutableSet.class, String.class), RESOLVABLE_INPUTS)
            .addParameter(Facets.class, INPUTS)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(DependencyCommand.class), ClassName.get(Facets.class)));
    if (Objects.nonNull(getParsedVajramData())) {
      resolveInputsBuilder.beginControlFlow("switch ($L) ", dependencyDef);
      if (getParsedVajramData().resolvers().isEmpty()) {
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
                  method
                      .getParameters()
                      .forEach(
                          parameter -> {
                            String bindParamName = util.inferFacetName(parameter);
                            if (!fanout.get()
                                && depFanoutMap.containsKey(
                                    bindParamName)) { // if fanout is already set skip resetting it.
                              fanout.set(depFanoutMap.get(bindParamName));
                            }
                            // validating if the bind parameter has a resolver binding or defined as
                            // inputDef
                            if (!(facetModels.containsKey(bindParamName)
                                || resolvedVariables.contains(bindParamName))) {
                              String message =
                                  "Parameter binding incorrect for inputDef - " + bindParamName;
                              util.error(message, parameter);
                              throw new VajramValidationException(message);
                            }
                          });
                  CodeBlock.Builder ifBlockBuilder = buildInputResolver(method, depFanoutMap);
                  caseBuilder.add(ifBlockBuilder.build());
                });
            caseBuilder.endControlFlow();
            resolveInputsBuilder.addCode(caseBuilder.build());
          });
      resolveInputsBuilder.endControlFlow();
      resolveInputsBuilder.addStatement(
          "throw new $T($S)",
          ClassName.get(VajramValidationException.class),
          "Unresolvable dependencyDef");
    } else {
      resolveInputsBuilder.addStatement(
          "throw new $T($S)",
          ClassName.get(VajramValidationException.class),
          "Unresolvable dependencyDef");
    }
    return Optional.of(resolveInputsBuilder.build());
  }

  /**
   * Method to generate resolver code for inputDef binding
   *
   * @param method Vajram resolve method
   * @param depFanoutMap Map of all the dependencies and their resolvers defintions are fanout or
   *     not
   * @return {@link CodeBlock.Builder} with resolver code
   */
  private CodeBlock.Builder buildInputResolver(
      ExecutableElement method, Map<String, Boolean> depFanoutMap) {
    Resolve resolve =
        checkNotNull(
            method.getAnnotation(Resolve.class), "Resolver method must have 'Resolve' annotation");
    String[] facets = resolve.depInputs();
    String depName = resolve.depName();
    // check if the inputDef is satisfied by inputDef or other resolved variables
    CodeBlock.Builder ifBlockBuilder = CodeBlock.builder();
    ifBlockBuilder.beginControlFlow(
        "if ($T.of($S).equals(resolvableInputs))", Set.class, String.join(",", facets));
    // TODO : add validation if fanout, then method should accept dependencyDef response for the
    // bind
    // type parameter else error
    // Iterate over the method params and call respective binding methods
    method
        .getParameters()
        .forEach(
            parameter -> {
              String usingInputName = util.inferFacetName(parameter);
              // check if the bind param has multiple resolvers
              FacetGenModel facetGenModel = facetModels.get(usingInputName);
              if (facetGenModel instanceof DependencyModel) {
                generateDependencyResolutions(
                    method, usingInputName, ifBlockBuilder, depFanoutMap, parameter);
              } else if (facetGenModel instanceof InputModel<?> inputModel) {
                TypeMirror facetType = util.toTypeMirror(inputModel.type());
                String variable = toJavaName(usingInputName);
                TypeMirror parameterTypeMirror = parameter.asType();
                final TypeName parameterType = TypeName.get(parameterTypeMirror);
                if (inputModel.isMandatory()) {
                  if (!util.getProcessingEnv()
                      .getTypeUtils()
                      .isAssignable(facetType, parameterTypeMirror)) {
                    String message =
                        String.format(
                            "Incorrect facet type being consumed. Expected '%s', found '%s'"
                                .formatted(facetType, parameterType),
                            usingInputName);
                    util.error(message, parameter);
                    throw new VajramValidationException(message);
                  }
                  ifBlockBuilder.add(
                      CodeBlock.builder()
                          .addStatement(
                              "$T $L = $L.getInputValueOrThrow($S)",
                              parameterType,
                              variable,
                              INPUTS,
                              usingInputName)
                          .build());
                } else if (util.isRawAssignable(parameterTypeMirror, Optional.class)) {
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
                  String message =
                      String.format(
                          "Optional inputDef dependencyDef %s must have type as Optional",
                          usingInputName);
                  util.error(message, parameter);
                  throw new VajramValidationException(message);
                }
              } else {
                String message = "No inputDef resolver found for " + usingInputName;
                util.error(message, parameter);
                throw new VajramValidationException(message);
              }
            });
    boolean isFanOut = depFanoutMap.getOrDefault(depName, false);
    buildFinalResolvers(method, facets, ifBlockBuilder, depName, isFanOut);
    ifBlockBuilder.endControlFlow();
    return ifBlockBuilder;
  }

  /**
   * Method to generate resolver code for dependencyDef bindings
   *
   * @param method Dependency resolver method
   * @param usingInputName The bind param name in the resolver method
   * @param ifBlockBuilder The {@link CodeBlock.Builder}
   * @param depFanoutMap Map of all the dependencies and their resolvers defintions are fanout or
   *     not
   * @param parameter the bind parameter in the resolver method
   */
  private void generateDependencyResolutions(
      ExecutableElement method,
      String usingInputName,
      CodeBlock.Builder ifBlockBuilder,
      Map<String, Boolean> depFanoutMap,
      VariableElement parameter) {
    FacetGenModel facetDef = facetModels.get(usingInputName);
    Resolve resolve =
        checkNotNull(method.getAnnotation(Resolve.class), "Resolver method cannot be null");
    String resolvedDep = resolve.depName();

    TypeMirror parameterType = parameter.asType();
    //    ReturnType returnType
    if (facetDef instanceof DependencyModel dependencyModel) {
      String variableName = toJavaName(usingInputName);
      final VajramInfoLite vajramInfoLite =
          checkNotNull(
              vajramDefs.get(dependencyModel.depVajramId()),
              "Could not find parsed vajram data for class %s",
              dependencyModel.depVajramId());
      String requestClass = dependencyModel.depReqClassQualifiedName();
      TypeName boxedDepType = util.toTypeName(vajramInfoLite.responseType()).box();
      TypeName unboxedDepType =
          boxedDepType.isBoxedPrimitive() ? boxedDepType.unbox() : boxedDepType;
      String resolverName = method.getSimpleName().toString();
      if (depFanoutMap.getOrDefault(usingInputName, false)) {
        // fanout case
        boolean typeMismatch = false;
        if (!util.isRawAssignable(parameterType, DependencyResponse.class)) {
          typeMismatch = true;
        } else if (getTypeParameters(parameterType).size() != 2) {
          typeMismatch = true;
        }
        if (typeMismatch) {
          // the parameter data type must be DependencyResponse<Req, Resp>
          String message =
              """
                  A fanout dependency ('%s') can be consumed only via the DependencyResponse<ReqType,RespType> class. \
                  Found '%s' instead"""
                  .formatted(resolvedDep, parameterType);
          util.error(message, parameter);
          throw new VajramValidationException(message);
        }
        String depValueAccessorCode =
            """
            $1T $2L =
             new $3T<>(facets.<$4T>getDepValue($5S)
                  .values().entrySet().stream()
                  .collect($6T.toImmutableMap(e -> $7T.from(e.getKey()),
                  $8T::getValue)))""";
        ifBlockBuilder.addStatement(
            depValueAccessorCode,
            ParameterizedTypeName.get(
                ClassName.get(DependencyResponse.class), toClassName(requestClass), boxedDepType),
            variableName,
            DependencyResponse.class,
            boxedDepType,
            usingInputName,
            ImmutableMap.class,
            toClassName(requestClass),
            ClassName.get(Map.Entry.class));
      } else {
        // This means the dependency being consumed used is not a fanout dependency
        String depValueAccessorCode =
            """
            $1T $2L =
              facets.<$3T>getDepValue($4S)
                 .values()
                 .entrySet()
                 .iterator()
                 .next()
                 .getValue()""";
        if (facetDef.isMandatory()) {
          if (unboxedDepType.equals(TypeName.get(parameterType))) {
            // This means this dependencyDef being consumed is a non fanout mandatory dependency and
            // the dev has requested the value directly. So we extract the only value from
            // dependencyDef response and provide it.
            String code =
                depValueAccessorCode
                    + """
                            .getValueOrThrow().orElseThrow(() ->
                                new $5T("Received null value for mandatory dependencyDef '$6L' of vajram '$7L'"))""";
            ifBlockBuilder.addStatement(
                code,
                unboxedDepType,
                variableName,
                boxedDepType,
                usingInputName,
                IllegalArgumentException.class,
                usingInputName,
                vajramName);
          } else {
            // This means the dependency being consumed is mandatory, but the type of the parameter
            // is not matching the type of the facet
            String message =
                "A resolver must consume a mandatory dependency directly using its type (%s). Found '%s' instead"
                    .formatted(unboxedDepType, parameterType);
            util.error(message, parameter);
            throw new VajramValidationException(message);
          }
        } else {
          // dependency is optional then accept only errable and optional in resolver
          if (util.isRawAssignable(parameterType, Errable.class)) {
            // This means this dependencyDef in "Using" annotation is not a fanout and the dev has
            // requested the 'Errable'. So we extract the only Errable from dependencyDef
            // response and provide it.
            ifBlockBuilder.addStatement(
                depValueAccessorCode,
                ParameterizedTypeName.get(ClassName.get(Errable.class), boxedDepType),
                variableName,
                boxedDepType,
                usingInputName);
          } else if (util.isRawAssignable(parameterType, Optional.class)) {
            // This means this dependencyDef in "Using" annotation is not a fanout and the dev has
            // requested an 'Optional'. So we retrieve the only Errable from the dependencyDef
            // response, extract the optional and provide it.
            String code = depValueAccessorCode + ".value()";
            ifBlockBuilder.addStatement(
                code,
                ParameterizedTypeName.get(ClassName.get(Optional.class), boxedDepType),
                variableName,
                boxedDepType,
                usingInputName);
          } else {
            String message =
                ("A resolver ('%s') must not access an optional dependencyDef ('%s') directly."
                        + "Use Optional<>, Errable<>, or DependencyResponse<> instead")
                    .formatted(resolverName, usingInputName);
            util.error(message, parameter);
            throw new VajramValidationException(message);
          }
        }
      }
    }
  }

  /**
   * Method to generate resolver code for variables having single resolver. Fanout case - Iterable
   * of normal type => fanout loop and create facets - Iterable Vajram Request -
   * DependencyCommand.MultiExecute<NormalType> Non- fanout - Normal datatype - Vajram Request =>
   * toInputValues() - DependencyCommand.executeWith
   *
   * @param resolverMethod Resolve method
   * @param facets Resolve facets
   * @param ifBlockBuilder {@link CodeBlock.Builder}
   * @param depName the name of the dependency facet
   * @param isFanOut Variable mentioning if the resolved variable uses a fanout dependencyDef
   */
  private void buildFinalResolvers(
      ExecutableElement resolverMethod,
      String[] facets,
      CodeBlock.Builder ifBlockBuilder,
      String depName,
      boolean isFanOut) {

    String variableName = "resolverResult";
    boolean controlFLowStarted = false;
    // Identify resolve method return type
    final TypeName methodReturnType = TypeName.get(resolverMethod.getReturnType());

    // call the resolve method
    /*
    TODO In the following code, we are assuming that if the
     resolver method is returning SingleExecute<T>, MultiExecute<T>, or just T,
     the T is exactly matching the resolved inputs data type. If the developer makes an error,
     then the generated code will fail at runtime with ClassCastException. We need to add a validation
     here which proactively fails if the data type mismatches.
    */
    ifBlockBuilder.add(
        "$T $L = $L(", methodReturnType, variableName, resolverMethod.getSimpleName());
    ImmutableList<String> resolverSources = getResolverSources(resolverMethod).asList();
    for (int i = 0; i < resolverSources.size(); i++) {
      String bindName = resolverSources.get(i);
      ifBlockBuilder.add("$L", toJavaName(bindName));
      if (i != resolverMethod.getParameters().size() - 1) {
        ifBlockBuilder.add(", ");
      }
    }
    ifBlockBuilder.add(");\n");

    TypeMirror returnType = util.box(resolverMethod.getReturnType());
    if (util.isRawAssignable(returnType, DependencyCommand.class)) {
      ifBlockBuilder.beginControlFlow("if($L.shouldSkip())", variableName);
      ifBlockBuilder.addStatement(
          "\t return $T.skipExecution($L.doc())", SingleExecute.class, variableName);
      ifBlockBuilder.add("} else {\n\t");
      controlFLowStarted = true;
      if (util.isRawAssignable(returnType, SingleExecute.class)) {
        ifBlockBuilder.addStatement(
            """
          return $T.executeWith(new $T(
           $T.of($S, $T.withValue(
              $L.inputs().iterator().next().orElse(null)))))
        """,
            SingleExecute.class,
            Facets.class,
            ImmutableMap.class,
            facets[0],
            Errable.class,
            variableName);

      } else if (util.isRawAssignable(returnType, MultiExecute.class)) {
        // TODO : add missing validations if any (??)
        if (!isFanOut) {
          String message =
              """
                  Dependency '%s' is not a fanout dependency, yet the resolver method returns a MultiExecute command.\
                   This is not allowed. Return a SingleExecute command, a single value, or mark the dependency as `canFanout = true`."""
                  .formatted(depName);
          util.error(message, resolverMethod);
          throw new VajramValidationException(message);
        } else if (util.isRawAssignable(
            getTypeParameters(returnType).get(0), VajramRequest.class)) {
          // TODO: Add validation that this vajram request is of the same type as the request of the
          // dependency Vajram
          String code =
              """
                  return $T.executeFanoutWith(
                      $L.inputs()
                          .stream()
                          .map(
                              element ->
                                element
                                  .filter(Optional::isPresent)
                                  .map(VajramRequest::toFacetValues())
                                  .orElse(Facets.empty()))))
                      .toList())""";
          ifBlockBuilder.addStatement(code, MultiExecute.class, variableName);
        } else {
          // Here we are assuming that the method is returning an MultiExecute of the type of the
          // input being resolved. If this assumption is incorrect, the generated wrapper class will
          // fail compilation.
          // TODO : Add validation for the type of the iterable to match the type of the input being
          // resolved
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
              Facets.class,
              ImmutableMap.class,
              facets[0],
              Errable.class);
        }
      }
    } else if (util.isRawAssignable(returnType, VajramRequest.class)) {
      ifBlockBuilder.addStatement(
          "return $T.executeWith($L.toFacetValues())", SingleExecute.class, variableName);
    } else {
      ifBlockBuilder.addStatement(
          "return $T.executeWith(new $T(\n " + "$T.of($S, $T.withValue($L))))",
          SingleExecute.class,
          Facets.class,
          ImmutableMap.class,
          facets[0],
          Errable.class,
          variableName);
    }
    if (controlFLowStarted) {
      ifBlockBuilder.endControlFlow();
    }
  }

  /**
   * Method to generate code for "getFacetDefinitions" function
   *
   * @return {@link MethodSpec}
   */
  private MethodSpec createFacetDefinitions() {
    // Method : getFacetDefinitions
    MethodSpec.Builder facetDefinitionsBuilder =
        methodBuilder(GET_FACET_DEFINITIONS)
            .addModifiers(PUBLIC)
            .returns(ParameterizedTypeName.get(ImmutableList.class, VajramFacetDefinition.class))
            .addAnnotation(Override.class);
    facetDefinitionsBuilder.beginControlFlow("if(this.$L == null)", FACET_DEFINITIONS_VAR);
    facetDefinitionsBuilder.addStatement(
        """
            var $L =
                    $T.stream($L.$L.class.getDeclaredFields())
                        .collect($T.toMap($T::getName, $T.identity()));""",
        FACETS_FIELDS_VAR,
        Arrays.class,
        getVajramName(),
        _FACETS_CLASS,
        Collectors.class,
        Field.class,
        Function.class);
    List<FacetGenModel> facetGenModels = vajramInfo.facetStream().toList();
    Collection<CodeBlock> codeBlocks = new ArrayList<>(facetGenModels.size());
    // Input and Dependency code block
    facetGenModels.forEach(
        facetGenModel -> {
          CodeBlock.Builder inputDefBuilder = CodeBlock.builder();
          if (facetGenModel instanceof InputModel<?> inputDef) {
            buildVajramInput(inputDefBuilder, inputDef);
          } else if (facetGenModel instanceof DependencyModel dependencyDef) {
            buildVajramDependency(inputDefBuilder, dependencyDef);
          }
          inputDefBuilder.add(
              ".tags($T.parseFacetTags($L.get($S)))",
              VajramFacetDefinition.class,
              FACETS_FIELDS_VAR,
              facetGenModel.name());
          inputDefBuilder.add(".build()");
          codeBlocks.add(inputDefBuilder.build());
        });
    facetDefinitionsBuilder.addCode(
        CodeBlock.builder()
            .add("this.$L = $T.of(\n", FACET_DEFINITIONS_VAR, ImmutableList.class)
            .add(CodeBlock.join(codeBlocks, ",\n\t"))
            .add("\n);\n")
            .build());
    facetDefinitionsBuilder.endControlFlow();
    facetDefinitionsBuilder.addStatement("return $L", FACET_DEFINITIONS_VAR);
    return facetDefinitionsBuilder.build();
  }

  /**
   * Method to generate VajramDependency code blocks
   *
   * @param inputDefBuilder : {@link CodeBlock.Builder}
   * @param dependencyDef : Vajram dependencyDef
   */
  private static void buildVajramDependency(
      CodeBlock.Builder inputDefBuilder, DependencyModel dependencyDef) {
    inputDefBuilder
        .add("$T.builder()", ClassName.get(DependencyDef.class))
        .add(".name($S)", dependencyDef.name());
    String code = ".dataAccessSpec($1T.vajramID($2S))";
    inputDefBuilder.add(
        code, ClassName.get(VajramID.class), dependencyDef.depVajramId().vajramId());
    inputDefBuilder.add(".isMandatory($L)", dependencyDef.isMandatory());
  }

  /**
   * Method to generate VajramInput code blocks
   *
   * @param inputDefBuilder : {@link CodeBlock.Builder}
   * @param inputDef : Vajram Input
   */
  private void buildVajramInput(CodeBlock.Builder inputDefBuilder, InputModel<?> inputDef) {
    inputDefBuilder
        .add("$T.builder()", ClassName.get(InputDef.class))
        .add(".name($S)", inputDef.name());
    // handle inputDef type
    Set<InputSource> inputSources = inputDef.sources();
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
    DataType<?> inputType = inputDef.type();
    inputDefBuilder.add(".type(");
    if (inputType instanceof JavaType<?> javaType) {
      List<TypeName> collectClassNames = new ArrayList<>();
      inputDefBuilder.add(
          getJavaTypeCreationCode(javaType, collectClassNames, inputDef.facetField()),
          (Object[]) collectClassNames.toArray(TypeName[]::new));
    } else {
      util.error("Unrecognised data type %s".formatted(inputType), inputDef.facetField());
    }
    inputDefBuilder.add(")");
    inputDefBuilder.add(".isMandatory($L)", inputDef.isMandatory());
    inputDefBuilder.add(".isBatched($L)", inputDef.isBatched());
  }

  private String getJavaTypeCreationCode(
      JavaType<?> javaType, List<TypeName> collectClassNames, VariableElement facetField) {
    TypeMirror typeMirror = javaType.javaModelType(processingEnv);
    collectClassNames.add(ClassName.get(JavaType.class));
    if (javaType.typeParameters().isEmpty()) {
      collectClassNames.add(TypeName.get(typeMirror));
      return "$T.create($T.class)";
    } else {
      collectClassNames.add(TypeName.get(processingEnv.getTypeUtils().erasure(typeMirror)));
      collectClassNames.add(ClassName.get(List.class));
      return "$T.create($T.class, $T.of("
          + javaType.typeParameters().stream()
              .map(
                  dataType -> {
                    if (!(dataType instanceof JavaType<?> typeParamType)) {
                      util.error("Unrecognised data type %s".formatted(dataType), facetField);
                      return "";
                    } else {
                      return getJavaTypeCreationCode(typeParamType, collectClassNames, facetField);
                    }
                  })
              .collect(Collectors.joining(","))
          + "))";
    }
  }

  public String codeGenVajramRequest() {
    ImmutableList<InputModel<?>> inputDefs = vajramInfo.inputs();
    MethodSpec.Builder requestConstructor = constructorBuilder().addModifiers(PRIVATE);
    ClassName builderClassType =
        ClassName.get(packageName + Constants.DOT_SEPARATOR + requestClassName, "Builder");
    TypeSpec.Builder requestClass =
        util.classBuilder(requestClassName)
            .addModifiers(PUBLIC, FINAL)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(VajramRequest.class),
                    util.toTypeName(vajramInfo.responseType()).box()))
            .addAnnotation(EqualsAndHashCode.class)
            .addMethod(
                methodBuilder("builder")
                    .addModifiers(PUBLIC, STATIC)
                    .returns(builderClassType)
                    .addStatement("return new Builder()")
                    .build());
    TypeSpec.Builder builderClass =
        util.classBuilder("Builder")
            .addModifiers(PUBLIC, STATIC, FINAL)
            .addAnnotation(EqualsAndHashCode.class)
            .addMethod(constructorBuilder().addModifiers(PRIVATE).build());
    Set<String> inputNames = new LinkedHashSet<>();
    List<FacetGenModel> facets = vajramInfo.facetStream().toList();
    List<FieldSpec.Builder> inputNameFields = new ArrayList<>(facets.size());
    List<FieldSpec.Builder> inputSpecFields = new ArrayList<>(facets.size());
    for (FacetGenModel facet : facets) {
      String facetJavaName = toJavaName(facet.name());
      TypeAndName facetType = getTypeName(getDataType(facet));
      TypeAndName boxedFacetType = boxPrimitive(facetType);
      ClassName vajramReqClass = ClassName.get(packageName, requestClassName);
      String inputNameFieldName = facetJavaName + FACET_NAME_SUFFIX;
      FieldSpec.Builder inputNameField =
          FieldSpec.builder(String.class, inputNameFieldName).initializer("\"$L\"", facet.name());

      FieldSpec.Builder inputSpecField = null;
      if (!(facet instanceof DependencyModel)) {
        // If vajrams A dependson B, and B depends on C, adding C's dependencyDef spec in B's
        // request
        // will leak C's request class into A's classpath which is not ideal.
        // This is the reason dependencyDef Facet spec fields are generated in InputUtilClass
        // instead
        // of Request class to avoid dependencyDef leakage from dependendency vajrams to client
        // vajrams.
        inputSpecField =
            FieldSpec.builder(
                    ParameterizedTypeName.get(
                        ClassName.get(VajramFacetSpec.class),
                        boxedFacetType.typeName(),
                        vajramReqClass),
                    facetJavaName + FACET_SPEC_SUFFIX)
                .addModifiers(STATIC, FINAL)
                .initializer(
                    "new $T<>($L, $T.class)",
                    VajramFacetSpec.class,
                    inputNameFieldName,
                    vajramReqClass);
        inputSpecFields.add(inputSpecField);
      }
      inputNameFields.add(inputNameField.addModifiers(STATIC, FINAL));
      if (facet instanceof InputModel<?> inputDef
          && inputDef.sources().contains(InputSource.CLIENT)) {
        if (inputSpecField != null) {
          inputSpecField.addModifiers(PUBLIC);
        }
        inputNameField.addModifiers(PUBLIC);
      } else {
        continue;
      }
      inputNames.add(facetJavaName);

      requestClass.addField(
          FieldSpec.builder(
                  boxedFacetType
                      .typeName()
                      .annotated(AnnotationSpec.builder(Nullable.class).build()),
                  facetJavaName,
                  PRIVATE,
                  FINAL)
              .build());
      builderClass.addField(
          FieldSpec.builder(
                  boxedFacetType
                      .typeName()
                      .annotated(AnnotationSpec.builder(Nullable.class).build()),
                  facetJavaName,
                  PRIVATE)
              .build());
      requestConstructor.addParameter(
          ParameterSpec.builder(
                  boxedFacetType
                      .typeName()
                      .annotated(AnnotationSpec.builder(Nullable.class).build()),
                  facetJavaName)
              .build());
      requestConstructor.addStatement("this.$L = $L", facetJavaName, facetJavaName);
      requestClass.addMethod(getterCodeForInput(inputDef, facetJavaName, facetType));

      builderClass.addMethod(
          // public InputType inputName(){return this.inputName;}
          methodBuilder(facetJavaName)
              .addModifiers(PUBLIC)
              .returns(
                  boxedFacetType
                      .typeName()
                      .annotated(AnnotationSpec.builder(Nullable.class).build()))
              .addStatement("return this.$L", facetJavaName) // Return
              .build());

      builderClass.addMethod(
          // public Builder inputName(Type inputName){this.inputName = inputName; return this;}
          methodBuilder(facetJavaName)
              .returns(builderClassType)
              .addModifiers(PUBLIC)
              .addParameter(
                  ParameterSpec.builder(
                          boxedFacetType
                              .typeName()
                              .annotated(AnnotationSpec.builder(Nullable.class).build()),
                          facetJavaName)
                      .build())
              .addStatement("this.$L = $L", facetJavaName, facetJavaName) // Set value
              .addStatement("return this", facetJavaName) // Return
              .build());
    }

    requestClass.addFields(inputNameFields.stream().map(FieldSpec.Builder::build).toList());
    requestClass.addFields(inputSpecFields.stream().map(FieldSpec.Builder::build).toList());

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
                .filter(inputDef -> inputDef.sources().contains(InputSource.CLIENT))
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
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return writer.toString();
  }

  private DataType<?> getDataType(FacetGenModel abstractInput) {
    if (abstractInput instanceof InputModel<?> inputDef) {
      return inputDef.type();
    } else if (abstractInput instanceof DependencyModel dep) {
      return dep.responseType();
    } else {
      throw new UnsupportedOperationException(
          "Unable to extract datatype from facet : %s".formatted(abstractInput));
    }
  }

  private TypeAndName boxPrimitive(TypeAndName javaType) {
    if (javaType.type().isPresent() && javaType.type().get().getKind().isPrimitive()) {
      TypeMirror boxed =
          processingEnv.getTypeUtils().boxedClass((PrimitiveType) javaType.type().get()).asType();
      return new TypeAndName(
          TypeName.get(boxed).annotated(javaType.annotationSpecs()),
          Optional.of(boxed),
          javaType.annotationSpecs());
    }
    return javaType;
  }

  private TypeAndName unboxPrimitive(TypeAndName javaType) {
    if (javaType.type().isPresent()) {
      PrimitiveType primitiveType;
      try {
        primitiveType = processingEnv.getTypeUtils().unboxedType(javaType.type().get());
      } catch (IllegalArgumentException e) {
        // This means the type is not a boxed type
        log.info("", e);
        return javaType;
      }
      return new TypeAndName(
          TypeName.get(primitiveType), Optional.of(primitiveType), javaType.annotationSpecs());
    }
    return javaType;
  }

  private FromAndTo fromAndToMethods(
      List<? extends FacetGenModel> facetDefs, ClassName enclosingClass) {
    MethodSpec.Builder toFacetValues =
        methodBuilder("toFacetValues")
            .returns(Facets.class)
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .addStatement(
                "return new $T($T.of(%s))"
                    .formatted(
                        facetDefs.stream()
                            .map(f -> "$S, $T.withValue(this.$L)")
                            .collect(Collectors.joining(","))),
                Stream.of(
                        Stream.of(Facets.class, ImmutableMap.class),
                        facetDefs.stream()
                            .flatMap(
                                facet ->
                                    Stream.of(
                                        facet.name(), Errable.class, toJavaName(facet.name()))))
                    .flatMap(Function.identity())
                    .toArray());

    MethodSpec.Builder fromFacetValues =
        methodBuilder("from")
            .returns(enclosingClass)
            .addModifiers(PUBLIC, STATIC)
            .addParameter(Facets.class, "values");

    List<String> inputNames = facetDefs.stream().map(FacetGenModel::name).toList();
    fromFacetValues.addStatement(
        "return new $T(%s)"
            .formatted(
                inputNames.stream()
                    .map(s -> "values.getInputValueOrDefault($S, null)")
                    .collect(Collectors.joining(", "))),
        Stream.concat(Stream.of(enclosingClass), inputNames.stream()).toArray());
    return new FromAndTo(fromFacetValues.build(), toFacetValues.build());
  }

  private TypeAndName getTypeName(DataType<?> dataType) {
    return getTypeName(dataType, List.of());
  }

  private TypeAndName getTypeName(DataType<?> dataType, List<AnnotationSpec> typeAnnotations) {
    TypeMirror javaModelType = dataType.javaModelType(processingEnv);
    return new TypeAndName(
        TypeName.get(javaModelType).annotated(typeAnnotations),
        Optional.of(javaModelType),
        typeAnnotations);
  }

  private MethodSpec getterCodeForInput(InputModel<?> facet, String name, TypeAndName typeAndName) {
    boolean wrapWithOptional = !facet.isMandatory();
    return methodBuilder(name)
        .returns(
            (wrapWithOptional
                ? optional(
                    boxPrimitive(typeAndName)
                        .typeName()
                        // Remove @Nullable because Optional<@Nullable T> is not useful.
                        .withoutAnnotations())
                : unboxPrimitive(typeAndName)
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
                        throw new IllegalStateException("The inputDef '$L' is not optional, but has null value. This should not happen");
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

  private MethodSpec getterCodeForDependency(
      DependencyModel dependencyDef, String name, TypeAndName typeAndName) {
    boolean wrapWithErrable = !dependencyDef.isMandatory() && !dependencyDef.canFanout();
    return methodBuilder(name)
        .returns(
            wrapWithErrable
                ? typeAndName.typeName()
                : unboxPrimitive(typeAndName)
                    .typeName()
                    // Remove @Nullable because getter has null check
                    // and will never return null.
                    .withoutAnnotations())
        .addModifiers(PUBLIC)
        .addCode(
            !wrapWithErrable
                ? CodeBlock.of(
                    """
                  if($L == null) {
                    throw new IllegalStateException("The dependency '$L' is not optional, but has null value. This should not happen");
                  }""",
                    name,
                    name)
                : CodeBlock.builder().build())
        .addCode(CodeBlock.builder().addStatement("return this.$L", name).build())
        .build();
  }

  public String codeGenInputUtil() {
    boolean doInputsNeedBatching =
        vajramInfo
            .facetStream()
            .filter(d -> d instanceof InputModel<?>)
            .map(d -> (InputModel<?>) d)
            .anyMatch(InputModel::isBatched);
    if (doInputsNeedBatching) {
      return codeGenBatchedInputUtil();
    } else {
      return codeGenSimpleInputUtil();
    }
  }

  private String codeGenSimpleInputUtil() {
    TypeSpec.Builder inputUtilClass = createInputUtilClass();
    TypeSpec.Builder allInputsClass =
        util.classBuilder(getAllFacetsClassname(vajramName))
            .addModifiers(FINAL, STATIC)
            .addSuperinterface(FacetContainer.class)
            .addAnnotations(recordAnnotations());
    List<FieldTypeName> fieldsList = new ArrayList<>();
    vajramInfo
        .inputs()
        .forEach(
            inputDef -> {
              String inputJavaName = toJavaName(inputDef.name());
              TypeAndName inputType =
                  getTypeName(
                      inputDef.type(), List.of(AnnotationSpec.builder(Nullable.class).build()));
              TypeAndName boxedInputType = boxPrimitive(inputType);
              allInputsClass.addField(boxedInputType.typeName(), inputJavaName, PRIVATE, FINAL);
              allInputsClass.addMethod(getterCodeForInput(inputDef, inputJavaName, inputType));
              fieldsList.add(new FieldTypeName(boxedInputType.typeName(), inputJavaName));
            });

    vajramInfo
        .dependencies()
        .forEach(
            dependencyDef -> {
              String inputJavaName = toJavaName(dependencyDef.name());
              TypeAndName depType = getDependencyOutputsType(dependencyDef);
              allInputsClass.addField(
                  boxPrimitive(depType).typeName(), inputJavaName, PRIVATE, FINAL);
              allInputsClass.addMethod(
                  getterCodeForDependency(dependencyDef, inputJavaName, depType));
              fieldsList.add(new FieldTypeName(depType.typeName(), inputJavaName));
            });

    // generate all args constructor and add to class
    generateConstructor(fieldsList).ifPresent(allInputsClass::addMethod);

    StringWriter writer = new StringWriter();
    try {
      JavaFile.builder(packageName, inputUtilClass.addType(allInputsClass.build()).build())
          .build()
          .writeTo(writer);
    } catch (IOException e) {
      throw new RuntimeException(e);
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

  private TypeAndName getDependencyOutputsType(DependencyModel dependencyDef) {
    boolean wrapWithErrable = !dependencyDef.isMandatory() && !dependencyDef.canFanout();
    DataType<?> depResponseType = dependencyDef.responseType();
    if (dependencyDef.canFanout()) {
      return new TypeAndName(
          ParameterizedTypeName.get(
              ClassName.get(DependencyResponse.class),
              toClassName(dependencyDef.depReqClassQualifiedName()),
              boxPrimitive(getTypeName(depResponseType)).typeName()));
    } else if (wrapWithErrable) {
      return new TypeAndName(
          ParameterizedTypeName.get(
              ClassName.get(Errable.class), boxPrimitive(getTypeName(depResponseType)).typeName()));
    } else {
      return getTypeName(depResponseType);
    }
  }

  private static ClassName toClassName(String depReqClassName) {
    int lastDotIndex = depReqClassName.lastIndexOf(DOT);
    return ClassName.get(
        depReqClassName.substring(0, lastDotIndex), depReqClassName.substring(lastDotIndex + 1));
  }

  private String codeGenBatchedInputUtil() {
    StringWriter writer = new StringWriter();
    try {
      TypeSpec.Builder inputUtilClass = createInputUtilClass();
      VajramInfo vajramFacetsDef = vajramInfo;
      String imClassName = getBatchedFacetsClassname(vajramName);
      String ciClassName = getCommonFacetsClassname(vajramName);
      FromAndTo imFromAndTo =
          fromAndToMethods(
              vajramFacetsDef.inputs().stream().filter(InputModel::isBatched).toList(),
              ClassName.get(packageName, getFacetUtilClassName(vajramName), imClassName));
      TypeSpec.Builder inputsNeedingBatching =
          util.classBuilder(imClassName)
              .addModifiers(STATIC)
              .addSuperinterface(FacetValuesAdaptor.class)
              .addAnnotations(recordAnnotations())
              .addMethod(imFromAndTo.to())
              .addMethod(imFromAndTo.from());

      FromAndTo ciFromAndTo =
          fromAndToMethods(
              Stream.concat(
                      vajramFacetsDef.inputs().stream().filter(inputDef -> !inputDef.isBatched()),
                      vajramFacetsDef.dependencies().stream())
                  .toList(),
              ClassName.get(packageName, getFacetUtilClassName(vajramName), ciClassName));
      TypeSpec.Builder commonInputs =
          util.classBuilder(ciClassName)
              .addModifiers(STATIC)
              .addSuperinterface(FacetValuesAdaptor.class)
              .addAnnotations(recordAnnotations())
              .addMethod(ciFromAndTo.to())
              .addMethod(ciFromAndTo.from());
      ClassName batchFacetsType =
          ClassName.get(packageName, getFacetUtilClassName(vajramName), imClassName);
      ClassName commonFacetsType =
          ClassName.get(packageName, getFacetUtilClassName(vajramName), ciClassName);
      List<FieldTypeName> ciFieldsList = new ArrayList<>();
      List<FieldTypeName> imFieldsList = new ArrayList<>();
      vajramFacetsDef
          .inputs()
          .forEach(
              inputDef -> {
                String inputJavaName = toJavaName(inputDef.name());
                TypeAndName inputType =
                    getTypeName(
                        inputDef.type(), List.of(AnnotationSpec.builder(Nullable.class).build()));
                TypeAndName boxedInputType = boxPrimitive(inputType);
                if (inputDef.isBatched()) {
                  inputsNeedingBatching.addField(
                      boxedInputType.typeName(), inputJavaName, PRIVATE, FINAL);
                  inputsNeedingBatching.addMethod(
                      getterCodeForInput(inputDef, inputJavaName, inputType));
                  imFieldsList.add(new FieldTypeName(boxedInputType.typeName(), inputJavaName));
                } else {
                  commonInputs.addField(boxedInputType.typeName(), inputJavaName, PRIVATE, FINAL);
                  commonInputs.addMethod(getterCodeForInput(inputDef, inputJavaName, inputType));
                  ciFieldsList.add(new FieldTypeName(boxedInputType.typeName(), inputJavaName));
                }
              });
      vajramFacetsDef
          .dependencies()
          .forEach(
              dependencyDef -> {
                TypeAndName depType = getDependencyOutputsType(dependencyDef);
                String inputJavaName = toJavaName(dependencyDef.name());
                commonInputs.addField(
                    boxPrimitive(depType).typeName(), inputJavaName, PRIVATE, FINAL);
                commonInputs.addMethod(
                    getterCodeForDependency(dependencyDef, inputJavaName, depType));
                ciFieldsList.add(new FieldTypeName(depType.typeName(), inputJavaName));
              });
      // create constructors
      generateConstructor(ciFieldsList).ifPresent(commonInputs::addMethod);
      generateConstructor(imFieldsList).ifPresent(inputsNeedingBatching::addMethod);

      TypeName parameterizedTypeName =
          ParameterizedTypeName.get(
              ClassName.get(FacetsConverter.class), batchFacetsType, commonFacetsType);
      CodeBlock.Builder initializer =
          CodeBlock.builder()
              .add(
                  "$L",
                  util.classBuilder("")
                      .addSuperinterface(parameterizedTypeName)
                      .addMethod(
                          methodBuilder("getBatched")
                              .addModifiers(PUBLIC)
                              .returns(batchFacetsType)
                              .addParameter(Facets.class, "facetValues")
                              .addStatement("return $T.from(facetValues)", batchFacetsType)
                              .build())
                      .addMethod(
                          methodBuilder("getCommon")
                              .addModifiers(PUBLIC)
                              .returns(commonFacetsType)
                              .addParameter(Facets.class, "facetValues")
                              .addStatement("return $T.from(facetValues)", commonFacetsType)
                              .build())
                      .build());
      FieldSpec.Builder converter =
          FieldSpec.builder(parameterizedTypeName, CONVERTER)
              .addModifiers(STATIC)
              .initializer(initializer.build());
      JavaFile.builder(
              packageName,
              inputUtilClass
                  .addType(inputsNeedingBatching.build())
                  .addType(commonInputs.build())
                  .addField(converter.build())
                  .build())
          .build()
          .writeTo(writer);
    } catch (IOException e) {
      throw new RuntimeException(e);
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
    TypeSpec.Builder classBuilder =
        util.classBuilder(getFacetUtilClassName(vajramName))
            .addModifiers(FINAL)
            .addMethod(constructorBuilder().addModifiers(PRIVATE).build());
    List<FacetGenModel> facets = vajramInfo.facetStream().toList();
    List<FieldSpec.Builder> depSpecFields = new ArrayList<>(facets.size());
    for (FacetGenModel facet : facets) {
      String facetJavaName = toJavaName(facet.name());
      TypeAndName facetType = getTypeName(getDataType(facet));
      TypeAndName boxedFacetType = boxPrimitive(facetType);
      ClassName vajramReqClass = ClassName.get(packageName, requestClassName);
      String inputNameFieldName = facetJavaName + FACET_NAME_SUFFIX;

      if (facet instanceof DependencyModel vajramDepDef) {
        FieldSpec.Builder inputSpecField;
        ClassName depReqClass = ClassName.bestGuess(vajramDepDef.depReqClassQualifiedName());
        ClassName specType =
            ClassName.get(
                vajramDepDef.canFanout()
                    ? VajramDepFanoutTypeSpec.class
                    : VajramDepSingleTypeSpec.class);
        inputSpecField =
            FieldSpec.builder(
                    ParameterizedTypeName.get(
                        specType, boxedFacetType.typeName(), vajramReqClass, depReqClass),
                    facetJavaName + FACET_SPEC_SUFFIX)
                .initializer(
                    "new $T<>($T.$L, $T.class, $T.class)",
                    specType,
                    vajramReqClass,
                    inputNameFieldName,
                    vajramReqClass,
                    depReqClass);

        depSpecFields.add(inputSpecField.addModifiers(STATIC, FINAL));
      }
    }
    return classBuilder.addFields(depSpecFields.stream().map(FieldSpec.Builder::build).toList());
  }

  public String getRequestClassName() {
    return requestClassName;
  }

  public String getPackageName() {
    return packageName;
  }

  private static String toJavaName(String inputName) {
    return inputName;
  }

  private static TypeName optional(TypeName javaType) {
    return ParameterizedTypeName.get(ClassName.get(Optional.class), javaType);
  }

  private record FromAndTo(MethodSpec from, MethodSpec to) {}

  private record TypeAndName(
      TypeName typeName, Optional<TypeMirror> type, List<AnnotationSpec> annotationSpecs) {
    private TypeAndName(TypeName typeName) {
      this(typeName, Optional.empty(), List.of());
    }
  }

  private record FieldTypeName(TypeName typeName, String name) {}
}
