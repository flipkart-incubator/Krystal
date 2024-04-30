package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.flipkart.krystal.vajram.codegen.Constants.ARRAY_LIST;
import static com.flipkart.krystal.vajram.codegen.Constants.COMMON_INPUT;
import static com.flipkart.krystal.vajram.codegen.Constants.COM_FUTURE;
import static com.flipkart.krystal.vajram.codegen.Constants.DEP_REQ_PARAM;
import static com.flipkart.krystal.vajram.codegen.Constants.DEP_RESP;
import static com.flipkart.krystal.vajram.codegen.Constants.DEP_RESPONSE;
import static com.flipkart.krystal.vajram.codegen.Constants.FACETS;
import static com.flipkart.krystal.vajram.codegen.Constants.FACET_DEFINITIONS_VAR;
import static com.flipkart.krystal.vajram.codegen.Constants.FACET_NAME_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.FACET_SPEC_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.FUNCTION;
import static com.flipkart.krystal.vajram.codegen.Constants.GET_FACET_DEFINITIONS;
import static com.flipkart.krystal.vajram.codegen.Constants.HASH_MAP;
import static com.flipkart.krystal.vajram.codegen.Constants.ILLEGAL_ARGUMENT;
import static com.flipkart.krystal.vajram.codegen.Constants.IM_LIST;
import static com.flipkart.krystal.vajram.codegen.Constants.IM_MAP;
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
import static com.flipkart.krystal.vajram.codegen.Constants.RESPONSE;
import static com.flipkart.krystal.vajram.codegen.Constants.RESPONSES_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.RETURN_TYPE;
import static com.flipkart.krystal.vajram.codegen.Constants.SKIPPED_EXCEPTION;
import static com.flipkart.krystal.vajram.codegen.Constants.UNMOD_INPUT;
import static com.flipkart.krystal.vajram.codegen.Constants.VAJRAM_LOGIC_METHOD;
import static com.flipkart.krystal.vajram.codegen.Constants.VAL_ERR;
import static com.flipkart.krystal.vajram.codegen.Constants.VARIABLE;
import static com.flipkart.krystal.vajram.codegen.Utils.COMMA;
import static com.flipkart.krystal.vajram.codegen.Utils.DOT;
import static com.flipkart.krystal.vajram.codegen.Utils.getAllFacetsClassname;
import static com.flipkart.krystal.vajram.codegen.Utils.getBatchedFacetsClassname;
import static com.flipkart.krystal.vajram.codegen.Utils.getCommonFacetsClassname;
import static com.flipkart.krystal.vajram.codegen.Utils.getTypeParameters;
import static com.flipkart.krystal.vajram.codegen.Utils.getVajramImplClassName;
import static com.flipkart.krystal.vajram.codegen.models.ParsedVajramData.fromVajram;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil.getQualifiedInputsComparator;
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
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.FacetsBuilder;
import com.flipkart.krystal.data.ImmutableFacets;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.data.Responses;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.utils.SkippedExecutionException;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.batching.BatchableFacets;
import com.flipkart.krystal.vajram.batching.BatchableSupplier;
import com.flipkart.krystal.vajram.batching.BatchedFacets;
import com.flipkart.krystal.vajram.codegen.models.DependencyModel;
import com.flipkart.krystal.vajram.codegen.models.FacetGenModel;
import com.flipkart.krystal.vajram.codegen.models.InputModel;
import com.flipkart.krystal.vajram.codegen.models.ParsedVajramData;
import com.flipkart.krystal.vajram.codegen.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.models.VajramInfoLite;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.DependencyDef;
import com.flipkart.krystal.vajram.facets.InputDef;
import com.flipkart.krystal.vajram.facets.InputSource;
import com.flipkart.krystal.vajram.facets.MultiExecute;
import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.flipkart.krystal.vajram.facets.SingleExecute;
import com.flipkart.krystal.vajram.facets.Using;
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
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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

@SuppressWarnings({"OverlyComplexClass", "HardcodedLineSeparator"})
@Slf4j
public class VajramCodeGenerator {
  private final String packageName;
  private final ProcessingEnvironment processingEnv;
  private final String requestClassName;
  private final VajramInfo vajramInfo;
  private final String vajramName;
  private final Map<VajramID, VajramInfoLite> vajramDefs;
  private final Map<String, FacetGenModel> facetModels;
  private final Map<String, FacetGenModel> facetModelsByName;
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
    this.facetModelsByName =
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
      usingInputName =
          Optional.ofNullable(parameter.getAnnotation(Using.class).value()).orElseThrow();
    } else {
      String paramName = parameter.getSimpleName().toString();
      FacetGenModel facetGenModel = facetModelsByName.get(paramName);
      if (facetGenModel == null) {
        String message = "Unknown facet with name %s".formatted(paramName);
        util.error(message, parameter);
        throw new VajramValidationException(message);
      }
      usingInputName = facetGenModel.name();
    }

    return usingInputName;
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
    Map<Integer, List<ExecutableElement>> resolverMap = new HashMap<>();
    for (ExecutableElement resolver : getParsedVajramData().resolvers()) {
      int dependencyId =
          checkNotNull(
              vajramInfo
                  .facetIdsByName()
                  .get(checkNotNull(resolver.getAnnotation(Resolve.class)).depName()));
      resolverMap.computeIfAbsent(dependencyId, _k -> new ArrayList<>()).add(resolver);
    }
    // Iterate all the resolvers and figure fanout
    // dep inputDef data type and method return type =>
    // 1. depInput = T, if (resolverReturnType is iterable of T || iterable of vajramRequest ||
    // multiExecute) => fanout
    Map<String, Boolean> depFanoutMap =
        vajramInfo.dependencies().stream()
            .collect(Collectors.toMap(DependencyModel::name, DependencyModel::canFanout));

    // Initialize few common attributes and data structures
    final ClassName inputBatch =
        ClassName.get(getParsedVajramData().packageName(), getBatchedFacetsClassname(vajramName));
    final ClassName commonInputs =
        ClassName.get(getParsedVajramData().packageName(), getCommonFacetsClassname(vajramName));
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
    } catch (IOException ignored) {

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
    return resolve.getParameters().stream().map(this::inferFacetName).collect(toImmutableSet());
  }

  /**
   * Method to generate "executeCompute" function code for ComputeVajrams Supported DataAccessSpec
   * => VajramID only.
   *
   * @param vajramResponseType Vajram response type
   * @return generated code for "executeCompute" {@link MethodSpec}
   */
  private MethodSpec createComputeVajramExecuteMethod(TypeName vajramResponseType) {

    Builder executeBuilder =
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

  private void nonBatchedExecuteMethodBuilder(Builder executeBuilder, boolean isIOVajram) {
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
    ClassName allFacetsClass = ClassName.get(packageName, getAllFacetsClassname(vajramName));
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
                    String facetName = inferFacetName(param);
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
                                        .orElseThrow(() -> new $illegalArgument:T("Missing mandatory dependencyDef '$variable:L' in vajram '$vajram:L'"))""",
                            ImmutableMap.of(
                                RESPONSE,
                                boxedResponseType,
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
    Builder inputConvertersBuilder =
        methodBuilder(METHOD_GET_FACETS_CONVERTOR)
            .addModifiers(PUBLIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(BatchableSupplier.class), batchableInputs, commonInputs))
            .addAnnotation(Override.class);
    inputConvertersBuilder.addCode(
        CodeBlock.builder()
            .addStatement(
                "return $T::new", ClassName.get(packageName, getAllFacetsClassname(vajramName)))
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

    Builder executeMethodBuilder =
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
      valueMap.put(FACETS, ClassName.get(Facets.class));
      valueMap.put(UNMOD_INPUT, ClassName.get(packageName, getAllFacetsClassname(vajramName)));
      valueMap.put(INPUT_BATCHING, batchableInputs);
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
      Map</*depId*/ Integer, List<ExecutableElement>> resolverMap,
      Map</*depName*/ String, Boolean> depFanoutMap) {
    String resolverIdVarName = "resolverId";
    Builder resolveInputsBuilder =
        methodBuilder(METHOD_RESOLVE_INPUT_OF_DEPENDENCY)
            .addModifiers(PUBLIC)
            .addParameter(int.class, resolverIdVarName)
            .addParameter(Facets.class, FACETS)
            .addParameter(Facets.class, DEP_REQ_PARAM)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(DependencyCommand.class), ClassName.get(Facets.class)));
    if (Objects.nonNull(getParsedVajramData())) {
      resolveInputsBuilder.beginControlFlow("switch ($L) ", resolverIdVarName);
      if (getParsedVajramData().resolvers().isEmpty()) {
        return Optional.empty();
      }
      record ResolverDetails(QualifiedInputs qualifiedInputs, ExecutableElement resolverMethod) {}
      TreeSet<ResolverDetails> resolverDetails =
          new TreeSet<>(
              Comparator.comparing(
                  ResolverDetails::qualifiedInputs, getQualifiedInputsComparator()));
      resolverMap.forEach(
          (depId, methods) ->
              methods.forEach(
                  method -> {
                    resolverDetails.add(
                        new ResolverDetails(
                            new QualifiedInputs(
                                depId,
                                method.getParameters().stream()
                                    .map(p -> p.getSimpleName().toString())
                                    .collect(toImmutableSet())),
                            method));
                  }));

      int resolverId = 1;
      for (ResolverDetails resolverDetail : resolverDetails) {
        AtomicBoolean fanout = new AtomicBoolean(false);
        // TODO : confirm if this logic is correct for all parameters for a resolve method
        resolverDetail
            .resolverMethod()
            .getParameters()
            .forEach(
                parameter -> {
                  String facetName = inferFacetName(parameter);
                  if (!fanout.get()
                      && depFanoutMap.containsKey(
                          facetName)) { // if fanout is already set skip resetting it.
                    fanout.set(depFanoutMap.getOrDefault(facetName, false));
                  }
                  // validating if the bind parameter has a resolver binding or defined as
                  // inputDef
                  if (!(facetModels.containsKey(facetName))) {
                    throw util.errorAndThrow(
                        "Parameter binding incorrect for inputDef - " + facetName, parameter);
                  }
                });
        CodeBlock.Builder caseBuilder =
            CodeBlock.builder().beginControlFlow("case $S -> ", resolverId++);
        caseBuilder.add(
            buildInputResolver(resolverDetail.resolverMethod(), depFanoutMap, fanout.get())
                .build());
        caseBuilder.endControlFlow();
        resolveInputsBuilder.addCode(caseBuilder.build());
      }
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
   * @param isParamFanoutDependency Variable mentioning if the resolved variable uses a fanout
   *     dependencyDef
   * @return {@link CodeBlock.Builder} with resolver code
   */
  private CodeBlock.Builder buildInputResolver(
      ExecutableElement method,
      Map<String, Boolean> depFanoutMap,
      boolean isParamFanoutDependency) {
    Resolve resolve =
        checkNotNull(
            method.getAnnotation(Resolve.class), "Resolver method must have 'Resolve' annotation");
    String[] facets = resolve.depInputs();
    String depName = resolve.depName();

    // check if the inputDef is satisfied by inputDef or other resolved variables
    CodeBlock.Builder ifBlockBuilder = CodeBlock.builder();
    // TODO : add validation if fanout, then method should accept dependencyDef response for the
    // bind
    // type parameter else error
    // Iterate over the method params and call respective binding methods
    method
        .getParameters()
        .forEach(
            parameter -> {
              final String usingInputName = inferFacetName(parameter);
              // check if the bind param has multiple resolvers
              if (facetModels.get(usingInputName) instanceof DependencyModel) {
                generateDependencyResolutions(
                    method, usingInputName, ifBlockBuilder, depFanoutMap, parameter);
              } else if (facetModels.containsKey(usingInputName)) {
                FacetGenModel facetGenModel = facetModels.get(usingInputName);
                String variable = toJavaName(usingInputName);
                final TypeName parameterType = TypeName.get(parameter.asType());
                if (facetGenModel != null && facetGenModel.isMandatory()) {
                  ifBlockBuilder.add(
                      CodeBlock.builder()
                          .addStatement(
                              "$T $L = $L.getInputValueOrThrow($S)",
                              parameterType,
                              variable,
                              FACETS,
                              usingInputName)
                          .build());
                } else {
                  if (util.isRawAssignable(parameter.asType(), Optional.class)) {
                    ifBlockBuilder.add(
                        CodeBlock.builder()
                            .addStatement(
                                "$T $L = $L.getInputValueOpt($S)",
                                parameterType,
                                variable,
                                FACETS,
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
                }
              } else {
                String message = "No inputDef resolver found for " + usingInputName;
                util.error(message, parameter);
                throw new VajramValidationException(message);
              }
            });
    boolean isFanOut = isParamFanoutDependency || depFanoutMap.getOrDefault(depName, false);
    buildFinalResolvers(method, facets, ifBlockBuilder, isFanOut);
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
      final String usingInputName,
      CodeBlock.Builder ifBlockBuilder,
      Map<String, Boolean> depFanoutMap,
      VariableElement parameter) {
    FacetGenModel facetDef = facetModels.get(usingInputName);
    Resolve resolve =
        checkNotNull(method.getAnnotation(Resolve.class), "Resolver method cannot be null");
    String resolvedDep = resolve.depName();
    // fanout case
    if (depFanoutMap.containsKey(usingInputName)
        && depFanoutMap.getOrDefault(usingInputName, false)
        && util.isRawAssignable(parameter.asType(), DependencyResponse.class)) {
      // the parameter data type must be DependencyResponse
      String message =
          "Dependency resolution of %s is fanout but the resolver method is not of type DependencyResponse"
              .formatted(resolvedDep);
      util.error(message, method);
      throw new VajramValidationException(message);
    }
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
      if (util.isRawAssignable(parameter.asType(), DependencyResponse.class)) {
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
            ClassName.get(Entry.class));
      } else {
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
          if (unboxedDepType.equals(TypeName.get(parameter.asType()))) {
            // This means this dependencyDef in "Using" annotation is not a fanout and the dev has
            // requested the value directly. So we extract the only value from dependencyDef
            // response
            // and
            // provide it.
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
            String message =
                ("A resolver ('%s') must not access an optional dependencyDef ('%s') directly."
                        + "Use Optional<>, Errable<>, or DependencyResponse<> instead")
                    .formatted(resolverName, usingInputName);
            util.error(message, parameter);
            throw new VajramValidationException(message);
          }
        } else {
          // dependency is optional then accept only errable and optional in resolver
          if (util.isRawAssignable(parameter.asType(), Errable.class)) {
            // This means this dependencyDef in "Using" annotation is not a fanout and the dev has
            // requested the 'Errable'. So we extract the only Errable from dependencyDef
            // response and provide it.
            ifBlockBuilder.addStatement(
                depValueAccessorCode,
                ParameterizedTypeName.get(ClassName.get(Errable.class), boxedDepType),
                variableName,
                boxedDepType,
                usingInputName);
          } else if (util.isRawAssignable(parameter.asType(), Optional.class)) {
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
                "Unrecognized parameter type %s in resolver %s of vajram %s"
                    .formatted(parameter.asType(), resolverName, this.vajramName);
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
   * @param method Resolve method
   * @param depFacetNames Resolve facets
   * @param ifBlockBuilder {@link CodeBlock.Builder}
   * @param isFanOut Variable mentioning if the resolved variable uses a fanout dependencyDef
   */
  private void buildFinalResolvers(
      ExecutableElement method,
      String[] depFacetNames,
      CodeBlock.Builder ifBlockBuilder,
      boolean isFanOut) {

    String variableName = "resolverResult";
    boolean controlFLowStarted = false;
    // Identify resolve method return type
    final TypeName methodReturnType = TypeName.get(method.getReturnType());

    // call the resolve method
    ifBlockBuilder.add("$T $L = $L(", methodReturnType, variableName, method.getSimpleName());
    ImmutableList<String> resolverSources = getResolverSources(method).asList();
    for (int i = 0; i < resolverSources.size(); i++) {
      String facetName = resolverSources.get(i);
      ifBlockBuilder.add("$L", toJavaName(facetName));
      if (i != method.getParameters().size() - 1) {
        ifBlockBuilder.add(", ");
      }
    }
    ifBlockBuilder.add(");\n");

    if (util.isRawAssignable(method.getReturnType(), DependencyCommand.class)) {
      ifBlockBuilder.beginControlFlow("if($L.shouldSkip())", variableName);
      ifBlockBuilder.addStatement(
          "\t return $T.skipExecution($L.doc())", SingleExecute.class, variableName);
      ifBlockBuilder.add("} else {\n\t");
      controlFLowStarted = true;
    }
    // TODO : add missing validations if any (??)
    TypeMirror returnType = util.box(method.getReturnType());
    if (util.isRawAssignable(returnType, MultiExecute.class)) {
      String code =
          """
              return $T.executeFanoutWith(
                  $L.inputs().stream()
                      .map(
                          element ->
                              $L._toBuilder().$L($T.withValue(element)))
                  .toList())""";
      ifBlockBuilder.addStatement(
          code, MultiExecute.class, variableName, DEP_REQ_PARAM, depFacetNames[0], Errable.class);
    } else if (isFanOut) {
      if (util.isRawAssignable(returnType, Iterable.class)) {
        if (util.isRawAssignable(getTypeParameters(returnType).get(0), ImmutableRequest.class)) {
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
                                  $T._toBuilder()._set(
                                      $L, $T.withValue(element)))
                      .toList())""";
          ifBlockBuilder.addStatement(
              code,
              MultiExecute.class,
              variableName,
              Facets.class,
              depFacetNames[0],
              Errable.class);
        }
      } else {
        String message =
            "Incorrect vajram resolver "
                + vajramName
                + ": Fanout resolvers must return an iterable";
        util.error(message, method);
        throw new VajramValidationException(message);
      }
    } else {
      if (util.isRawAssignable(returnType, ImmutableRequest.class)) {
        ifBlockBuilder.addStatement(
            "return $T.executeWith($L.toInputValues())", SingleExecute.class, variableName);
      } else if (util.isRawAssignable(returnType, SingleExecute.class)) {
        ifBlockBuilder.addStatement(
            """
          return $T.executeWith($T._toBuilder()._set(
           $L, $T.withValue(
              $L.inputs().iterator().next().orElse(null))))
        """,
            SingleExecute.class,
            Facets.class,
            depFacetNames[0],
            Errable.class,
            variableName);

      } else {
        ifBlockBuilder.addStatement(
            "return $T.executeWith($L._toBuilder()._set($L, $T.withValue($L)))",
            SingleExecute.class,
            Facets.class,
            depFacetNames[0],
            Errable.class,
            variableName);
      }
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
    Builder facetDefinitionsBuilder =
        methodBuilder(GET_FACET_DEFINITIONS)
            .addModifiers(PUBLIC)
            .returns(ParameterizedTypeName.get(ImmutableList.class, VajramFacetDefinition.class))
            .addAnnotation(Override.class);
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
          codeBlocks.add(inputDefBuilder.build());
        });

    facetDefinitionsBuilder.beginControlFlow("if(this.$L == null)", FACET_DEFINITIONS_VAR);
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
        .add(".id($L)", dependencyDef.id())
        .add(".name($S)", dependencyDef.name());
    String code = ".dataAccessSpec($1T.vajramID($2S))";
    inputDefBuilder.add(
        code, ClassName.get(VajramID.class), dependencyDef.depVajramId().vajramId());
    inputDefBuilder.add(".isMandatory($L)", dependencyDef.isMandatory());
    // build() as last step
    inputDefBuilder.add(".build()");
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
        .add(".id($S)", inputDef.id())
        .add(".name($L)", inputDef.name());
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
    DataType<?> inputType = inputDef.dataType();
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
    // last line
    inputDefBuilder.add(".build()");
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
    ClassName requestClassType = ClassName.get(packageName, getRequestClassName());
    ClassName builderClassType =
        ClassName.get(packageName + Constants.DOT_SEPARATOR + requestClassType, "Builder");
    TypeSpec.Builder requestClass =
        util.classBuilder(getRequestClassName())
            .addModifiers(PUBLIC, FINAL)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(ImmutableRequest.class),
                    util.toTypeName(vajramInfo.responseType()).box()))
            .addAnnotation(EqualsAndHashCode.class)
            .addMethod(
                methodBuilder("_builder")
                    .addModifiers(PUBLIC, STATIC)
                    .returns(builderClassType)
                    .addStatement("return new Builder()")
                    .build());
    addFacetConstants(
        requestClass,
        vajramInfo.inputs().stream().filter(i -> i.facetTypes().contains(INPUT)).toList());
    TypeSpec.Builder builderClass =
        util.classBuilder("Builder")
            .addModifiers(PUBLIC, STATIC, FINAL)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(RequestBuilder.class),
                    util.toTypeName(vajramInfo.responseType()).box()));
    createFacetMembers(requestClass, requestClassType, true, false);
    createFacetMembers(builderClass, requestClassType, true, true);

    StringWriter writer = new StringWriter();
    List<InputModel<?>> clientProvidedInputs =
        inputDefs.stream().filter(inputDef -> inputDef.facetTypes().contains(INPUT)).toList();
    try {
      JavaFile.builder(
              packageName,
              requestClass
                  .addMethods(
                      createFacetContainerMethods(
                          clientProvidedInputs, requestClassType, true, false))
                  .addType(
                      builderClass
                          .addMethods(
                              createFacetContainerMethods(
                                  clientProvidedInputs, requestClassType, true, true))
                          .build())
                  .build())
          .build()
          .writeTo(writer);
    } catch (IOException ignored) {

    }
    return writer.toString();
  }

  private void createFacetMembers(
      TypeSpec.Builder clazz, ClassName enclosingClassType, boolean isRequest, boolean isBuilder) {
    Builder fullConstructor = constructorBuilder().addModifiers(PRIVATE);
    clazz.addAnnotations(isBuilder ? annotations(ToString.class) : recordAnnotations());
    List<FacetGenModel> eligibleFacets =
        vajramInfo
            .facetStream()
            .filter(
                facet ->
                    isRequest
                        ? facet.facetTypes().contains(INPUT)
                        : !facet.facetTypes().contains(INPUT))
            .toList();
    for (FacetGenModel facet : eligibleFacets) {
      String facetJavaName = toJavaName(facet.name());
      TypeAndName facetType = getTypeName(getDataType(facet));
      TypeAndName boxedFacetType = boxPrimitive(facetType);

      {
        FieldSpec.Builder facetField =
            FieldSpec.builder(
                isRequest
                    ? boxedFacetType
                        .typeName()
                        .annotated(AnnotationSpec.builder(Nullable.class).build())
                    : facet instanceof DependencyModel dep && dep.canFanout()
                        ? ParameterizedTypeName.get(
                            ClassName.get(Responses.class),
                            toClassName(dep.depReqClassQualifiedName()),
                            boxedFacetType.typeName())
                        : ParameterizedTypeName.get(
                            ClassName.get(Errable.class), boxedFacetType.typeName()),
                facetJavaName,
                PRIVATE);
        if (!isBuilder) {
          facetField.addModifiers(FINAL);
        }
        clazz.addField(facetField.build());
      }
      ParameterSpec facetParam =
          ParameterSpec.builder(
                  boxedFacetType
                      .typeName()
                      .annotated(AnnotationSpec.builder(Nullable.class).build()),
                  facetJavaName)
              .build();
      fullConstructor.addParameter(facetParam);
      fullConstructor.addStatement("this.$L = $L", facetJavaName, facetJavaName);
      if (isRequest) {
        if (isBuilder) {
          clazz.addMethod(
              // public InputType inputName(){return this.inputName;}
              methodBuilder(facetJavaName)
                  .addModifiers(PUBLIC)
                  .returns( isRequest?
                      boxedFacetType
                          .typeName()
                          .annotated(AnnotationSpec.builder(Nullable.class).build()): is)
                  .addStatement("return this.$L", facetJavaName) // Return
                  .build());

          clazz.addMethod(
              // public Builder inputName(Type inputName){this.inputName = inputName; return this;}
              methodBuilder(facetJavaName)
                  .returns(enclosingClassType.nestedClass("Builder"))
                  .addModifiers(PUBLIC)
                  .addParameter(facetParam)
                  .addStatement("this.$L = $L", facetJavaName, facetJavaName) // Set value
                  .addStatement("return this", facetJavaName) // Return
                  .build());
        } else {
          clazz.addMethod(getterCodeForInput(facet, facetJavaName, facetType));
        }
      }
    }
    clazz.addMethod(fullConstructor.build());
    if (isBuilder) {
      clazz.addMethod(constructorBuilder().addModifiers(PRIVATE).build());
    }
  }

  private static FieldSpec.Builder createFacetSpecField(
      FacetGenModel facet,
      TypeAndName boxedFacetType,
      ClassName vajramReqClass,
      String facetJavaName) {
    return FieldSpec.builder(
            ParameterizedTypeName.get(
                ClassName.get(VajramFacetSpec.class), boxedFacetType.typeName(), vajramReqClass),
            facetJavaName + FACET_SPEC_SUFFIX)
        .addModifiers(STATIC, FINAL)
        .initializer(
            "new $T<>($L, $S, $T.class)",
            VajramFacetSpec.class,
            facet.id(),
            facet.name(),
            vajramReqClass);
  }

  private static FieldSpec.Builder createFacetNameField(FacetGenModel facet, String facetJavaName) {
    return FieldSpec.builder(String.class, facetJavaName + FACET_NAME_SUFFIX)
        .addModifiers(STATIC, FINAL)
        .initializer("\"$L\"", facet.name());
  }

  private DataType<?> getDataType(FacetGenModel abstractInput) {
    if (abstractInput instanceof InputModel<?> inputDef) {
      return inputDef.dataType();
    } else if (abstractInput instanceof DependencyModel dep) {
      return dep.dataType();
    } else {
      throw new UnsupportedOperationException(
          "Unable to extract datatype from facet : %s".formatted(abstractInput));
    }
  }

  private TypeAndName boxPrimitive(TypeAndName javaType) {
    if (javaType.type().isEmpty() || !javaType.type().get().getKind().isPrimitive()) {
      return javaType;
    }
    TypeMirror boxed =
        processingEnv.getTypeUtils().boxedClass((PrimitiveType) javaType.type().get()).asType();
    return new TypeAndName(
        TypeName.get(boxed).annotated(javaType.annotationSpecs()),
        Optional.of(boxed),
        javaType.annotationSpecs());
  }

  private TypeAndName unboxPrimitive(TypeAndName javaType) {
    if (javaType.type().isPresent()) {
      PrimitiveType primitiveType;
      try {
        primitiveType = processingEnv.getTypeUtils().unboxedType(javaType.type().get());
      } catch (IllegalArgumentException ignored) {
        // This means the type is not a boxed type
        return javaType;
      }
      return new TypeAndName(
          TypeName.get(primitiveType), Optional.of(primitiveType), javaType.annotationSpecs());
    }
    return javaType;
  }

  private ImmutableList<MethodSpec> createFacetContainerMethods(
      List<? extends FacetGenModel> facetDefs,
      ClassName enclosingClassName,
      boolean isRequest,
      boolean isBuilder) {
    List<MethodSpec.Builder> methodBuilders = new ArrayList<>();

    String facetIdParamName = "facetId";
    {
      Builder getMethod =
          methodBuilder("_get")
              .returns(ParameterizedTypeName.get(Errable.class, Object.class))
              .addParameter(int.class, facetIdParamName)
              .beginControlFlow("return switch ($L)", facetIdParamName);
      facetDefs.forEach(
          facetDef ->
              getMethod.addStatement(
                  "case $L -> $T.withValue($L)", facetDef.id(), Errable.class, facetDef.name()));
      methodBuilders.add(
          getMethod
              .addStatement(
                  "default -> throw new $T($S + $L)",
                  IllegalArgumentException.class,
                  "Unrecognized facet id",
                  facetIdParamName)
              .endControlFlow()
              .addCode(";"));
    }

    {
      methodBuilders.add(
          methodBuilder("_asMap")
              .returns(
                  ParameterizedTypeName.get(
                      ClassName.get(isBuilder ? Map.class : ImmutableMap.class),
                      ClassName.get(Integer.class),
                      ParameterizedTypeName.get(
                          isRequest ? Errable.class : FacetValue.class, Object.class)))
              .addStatement(
                  "return $T.of(%s)"
                      .formatted(
                          facetDefs.stream()
                              .map(f -> "$L, Errable.withValue($L)")
                              .collect(Collectors.joining(", "))),
                  Stream.concat(
                          Stream.of(ImmutableMap.class),
                          facetDefs.stream()
                              .<Object>mapMulti(
                                  (facetGenModel, consumer) -> {
                                    consumer.accept(facetGenModel.id());
                                    consumer.accept(facetGenModel.name());
                                  }))
                      .toArray()));
    }

    {
      methodBuilders.add(
          methodBuilder("_build")
              .returns(enclosingClassName)
              .addStatement(
                  isBuilder
                      ? "return new %s(%s)"
                          .formatted(
                              enclosingClassName.simpleName(),
                              facetDefs.stream()
                                  .map(FacetGenModel::name)
                                  .collect(Collectors.joining(", ")))
                      : "return this"));
    }

    {
      methodBuilders.add(
          methodBuilder("_asBuilder")
              .returns(enclosingClassName.nestedClass("Builder"))
              .addStatement(
                  isBuilder
                      ? "return this"
                      : "return new Builder(%s)"
                          .formatted(
                              facetDefs.stream()
                                  .map(FacetGenModel::name)
                                  .collect(Collectors.joining(", ")))));
    }

    {
      methodBuilders.add(
          methodBuilder("_newCopy")
              .returns(isBuilder ? enclosingClassName.nestedClass("Builder") : enclosingClassName)
              .addStatement(
                  isBuilder
                      ? "return new Builder(%s)"
                          .formatted(
                              facetDefs.stream()
                                  .map(FacetGenModel::name)
                                  .collect(Collectors.joining(", ")))
                      : "return this"));
    }

    if (!isRequest) {
      methodBuilders.add(
          methodBuilder("_getErrable")
              .returns(ParameterizedTypeName.get(Errable.class, Object.class))
              .addParameter(int.class, facetIdParamName)
              .addStatement("return _get($L)", facetIdParamName));
    }
    if (isBuilder) {
      { // _set
        String facetValueParamName = "facetValue";
        Builder setMethod;
        methodBuilders.add(
            setMethod =
                methodBuilder("_set")
                    .returns(enclosingClassName.nestedClass("Builder"))
                    .addParameter(int.class, facetIdParamName)
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get(FacetValue.class),
                            WildcardTypeName.subtypeOf(Object.class)),
                        facetValueParamName)
                    .addAnnotation(
                        AnnotationSpec.builder(SuppressWarnings.class)
                            .addMember("value", "$S", "unchecked")
                            .build())
                    .beginControlFlow("switch ($L)", facetIdParamName));
        if (isRequest) {
          facetDefs.stream()
              .filter(f -> f instanceof InputModel<?>)
              .map(f -> (InputModel<?>) f)
              .forEach(
                  input ->
                      setMethod.addStatement(
                          "case $L -> this.$L((($T)$L).valueOpt().orElse(null))",
                          input.id(),
                          input.name(),
                          ParameterizedTypeName.get(
                              ClassName.get(Errable.class),
                              TypeName.get(input.dataType().javaModelType(util.getProcessingEnv()))
                                  .box()),
                          facetValueParamName));
        } else {
          facetDefs.forEach(
              facet -> {
                if (facet instanceof InputModel<?> input && input.facetTypes().contains(INPUT)) {
                  setMethod.addStatement(
                      "case $L -> this._request().$L((($T)$L).valueOpt().orElse(null))",
                      input.id(),
                      input.name(),
                      ParameterizedTypeName.get(
                          ClassName.get(Errable.class),
                          TypeName.get(input.dataType().javaModelType(util.getProcessingEnv()))
                              .box()),
                      facetValueParamName);
                } else if (facet instanceof InputModel<?> input) {
                  setMethod.addStatement(
                      "case $L -> this.$L((($T)$L))",
                      input.id(),
                      input.name(),
                      ParameterizedTypeName.get(
                          ClassName.get(Errable.class),
                          TypeName.get(input.dataType().javaModelType(util.getProcessingEnv()))
                              .box()),
                      facetValueParamName);
                }
              });
        }
        setMethod
            .addStatement(
                "default -> throw new $T($S + $L)",
                IllegalArgumentException.class,
                "Unrecognized facet id",
                facetIdParamName)
            .endControlFlow()
            .addStatement("return this");
      }
    }

    ImmutableList.Builder<MethodSpec> list = ImmutableList.builder();
    for (Builder b : methodBuilders) {
      if (b != null) {
        MethodSpec build = b.addModifiers(PUBLIC).addAnnotation(Override.class).build();
        list.add(build);
      }
    }
    return list.build();
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

  private MethodSpec getterCodeForInput(FacetGenModel facet, String name, TypeAndName typeAndName) {
    boolean wrapWithOptional =
        !facet.isMandatory()
            && (facet instanceof InputModel<?>
                || (facet instanceof DependencyModel dependencyDef && !dependencyDef.canFanout()));
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

  public void codeGenInputUtil() {
    boolean doInputsNeedBatching =
        vajramInfo
            .facetStream()
            .filter(d -> d instanceof InputModel<?>)
            .map(d -> (InputModel<?>) d)
            .anyMatch(InputModel::isBatched);
    if (doInputsNeedBatching) {
      codeGenBatchedInputUtil();
    } else {
      codeGenSimpleInputUtil();
    }
  }

  private void codeGenSimpleInputUtil() {
    TypeSpec.Builder facetsClass =
        util.classBuilder(getAllFacetsClassname(vajramName))
            .addModifiers(FINAL)
            .addSuperinterface(ImmutableFacets.class)
            .addAnnotations(recordAnnotations());
    TypeSpec.Builder facetsBuilderClass =
        util.classBuilder("Builder")
            .addModifiers(STATIC, FINAL)
            .addSuperinterface(FacetsBuilder.class)
            .addAnnotations(recordAnnotations());
    ClassName allFacetsType = ClassName.get(packageName, getAllFacetsClassname(vajramName));

    List<FacetGenModel> allFacets = vajramInfo.facetStream().toList();
    addFacetConstants(facetsClass, allFacets);
    createFacetMembers(facetsClass, allFacetsType, false, false);
    createFacetMembers(facetsBuilderClass, allFacetsType, false, true);
    List<FieldTypeName> fieldsList = new ArrayList<>();
    vajramInfo
        .inputs()
        .forEach(
            inputDef -> {
              String inputJavaName = toJavaName(inputDef.name());
              TypeAndName inputType =
                  getTypeName(
                      inputDef.dataType(), List.of(AnnotationSpec.builder(Nullable.class).build()));
              TypeAndName boxedInputType = boxPrimitive(inputType);
              facetsClass.addField(boxedInputType.typeName(), inputJavaName, PRIVATE, FINAL);
              facetsClass.addMethod(getterCodeForInput(inputDef, inputJavaName, inputType));
              fieldsList.add(new FieldTypeName(boxedInputType.typeName(), inputJavaName));
            });

    vajramInfo
        .dependencies()
        .forEach(
            dependencyDef -> {
              String inputJavaName = toJavaName(dependencyDef.name());
              TypeAndName depType = getDependencyOutputsType(dependencyDef);
              TypeAndName boxedDepType = boxPrimitive(depType);
              facetsClass.addField(boxedDepType.typeName(), inputJavaName, PRIVATE, FINAL);
              facetsClass.addMethod(getterCodeForInput(dependencyDef, inputJavaName, depType));
              fieldsList.add(new FieldTypeName(boxedDepType.typeName(), inputJavaName));
            });

    // generate all args constructor and add to class
    generateConstructor(fieldsList).ifPresent(facetsClass::addMethod);

    StringWriter writer = new StringWriter();
    try {
      JavaFile.builder(
              packageName,
              facetsClass
                  .addMethods(createFacetContainerMethods(allFacets, allFacetsType, false, false))
                  .addType(
                      facetsBuilderClass
                          .addMethods(
                              createFacetContainerMethods(allFacets, allFacetsType, false, true))
                          .build())
                  .build())
          .build()
          .writeTo(writer);
    } catch (IOException ignored) {

    }
    util.generateSourceFile(
        getPackageName() + '.' + getAllFacetsClassname(getVajramName()),
        writer.toString(),
        vajramInfo.vajramClass());
  }

  private Optional<MethodSpec> generateConstructor(List<FieldTypeName> fieldsList) {
    // by default no args constructor is created so no need to generate
    if (fieldsList.isEmpty()) {
      return Optional.empty();
    }
    Builder constructor = constructorBuilder();
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
    DataType<?> depResponseType = dependencyDef.dataType();
    if (dependencyDef.canFanout()) {
      return new TypeAndName(
          ParameterizedTypeName.get(
              ClassName.get(DependencyResponse.class),
              toClassName(dependencyDef.depReqClassQualifiedName()),
              boxPrimitive(getTypeName(depResponseType)).typeName()));
    } else {
      return getTypeName(depResponseType, List.of(AnnotationSpec.builder(Nullable.class).build()));
    }
  }

  private static ClassName toClassName(String depReqClassName) {
    int lastDotIndex = depReqClassName.lastIndexOf(DOT);
    return ClassName.get(
        depReqClassName.substring(0, lastDotIndex), depReqClassName.substring(lastDotIndex + 1));
  }

  private void codeGenBatchedInputUtil() {
    StringWriter batchCode = new StringWriter();
    StringWriter commonCode = new StringWriter();
    StringWriter allFacetsCode = new StringWriter();
    VajramInfo vajramFacetsDef = vajramInfo;
    String batchClassName = getBatchedFacetsClassname(vajramName);
    String commonClassName = getCommonFacetsClassname(vajramName);
    ClassName batchFacetsType = ClassName.get(packageName, batchClassName);
    ClassName commonFacetsType = ClassName.get(packageName, commonClassName);
    List<InputModel<?>> batchedFacets =
        vajramFacetsDef.inputs().stream().filter(InputModel::isBatched).toList();
    TypeSpec.Builder batchFacetsClass =
        util.classBuilder(batchClassName)
            .addModifiers(FINAL)
            .addSuperinterface(ImmutableFacets.class)
            .addAnnotations(recordAnnotations())
            .addMethods(
                createFacetContainerMethods(
                    batchedFacets, ClassName.get(packageName, batchClassName), false, false));
    addFacetConstants(batchFacetsClass, batchedFacets);

    List<? extends FacetGenModel> commonFacets =
        Stream.concat(
                vajramFacetsDef.inputs().stream().filter(inputDef1 -> !inputDef1.isBatched()),
                vajramFacetsDef.dependencies().stream())
            .toList();
    TypeSpec.Builder commonFacetsClass =
        util.classBuilder(commonClassName)
            .addModifiers(FINAL)
            .addSuperinterface(ImmutableFacets.class)
            .addAnnotations(recordAnnotations())
            .addMethods(
                createFacetContainerMethods(
                    commonFacets, ClassName.get(packageName, commonClassName), false, false));
    addFacetConstants(commonFacetsClass, commonFacets);

    List<FieldTypeName> ciFieldsList = new ArrayList<>();
    List<FieldTypeName> imFieldsList = new ArrayList<>();
    vajramFacetsDef
        .inputs()
        .forEach(
            inputDef -> {
              String inputJavaName = toJavaName(inputDef.name());
              TypeAndName inputType =
                  getTypeName(
                      inputDef.dataType(), List.of(AnnotationSpec.builder(Nullable.class).build()));
              TypeAndName boxedInputType = boxPrimitive(inputType);
              if (inputDef.isBatched()) {
                batchFacetsClass.addField(boxedInputType.typeName(), inputJavaName, PRIVATE, FINAL);
                batchFacetsClass.addMethod(getterCodeForInput(inputDef, inputJavaName, inputType));
                imFieldsList.add(new FieldTypeName(boxedInputType.typeName(), inputJavaName));
              } else {
                commonFacetsClass.addField(
                    boxedInputType.typeName(), inputJavaName, PRIVATE, FINAL);
                commonFacetsClass.addMethod(getterCodeForInput(inputDef, inputJavaName, inputType));
                ciFieldsList.add(new FieldTypeName(boxedInputType.typeName(), inputJavaName));
              }
            });
    vajramFacetsDef
        .dependencies()
        .forEach(
            dependencyDef -> {
              TypeAndName depType = getDependencyOutputsType(dependencyDef);
              String inputJavaName = toJavaName(dependencyDef.name());
              TypeAndName boxedDepType = boxPrimitive(depType);
              commonFacetsClass.addField(boxedDepType.typeName(), inputJavaName, PRIVATE, FINAL);
              commonFacetsClass.addMethod(
                  getterCodeForInput(dependencyDef, inputJavaName, depType));
              ciFieldsList.add(new FieldTypeName(boxedDepType.typeName(), inputJavaName));
            });
    // create constructors
    generateConstructor(ciFieldsList).ifPresent(commonFacetsClass::addMethod);
    generateConstructor(imFieldsList).ifPresent(batchFacetsClass::addMethod);

    String allFacetsClassname = getAllFacetsClassname(vajramName);
    TypeSpec.Builder allFacetsClass =
        TypeSpec.classBuilder(allFacetsClassname)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(BatchableFacets.class), batchFacetsType, commonFacetsType))
            .addField(
                FieldSpec.builder(batchFacetsType, "_batchable")
                    .addModifiers(PRIVATE, FINAL)
                    .build())
            .addField(
                FieldSpec.builder(commonFacetsType, "_common").addModifiers(PRIVATE, FINAL).build())
            .addMethod(
                constructorBuilder()
                    .addParameter(batchFacetsType, "batchable")
                    .addParameter(commonFacetsType, "common")
                    .addStatement("this._batchable = batchable")
                    .addStatement("this._common = common")
                    .build())
            .addMethod(
                methodBuilder("_batchable")
                    .returns(batchFacetsType)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addStatement("return this._batchable")
                    .build())
            .addMethod(
                methodBuilder("_common")
                    .returns(commonFacetsType)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addStatement("return this._common")
                    .build());

    TypeName parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(BatchableSupplier.class), batchFacetsType, commonFacetsType);
    try {
      JavaFile.builder(packageName, batchFacetsClass.build()).build().writeTo(batchCode);
    } catch (IOException ignored) {
    }
    try {
      JavaFile.builder(packageName, commonFacetsClass.build()).build().writeTo(commonCode);
    } catch (IOException ignored) {
    }
    try {
      JavaFile.builder(packageName, allFacetsClass.build()).build().writeTo(allFacetsCode);
    } catch (IOException ignored) {
    }
    util.generateSourceFile(
        packageName + '.' + batchClassName, batchCode.toString(), vajramInfo.vajramClass());
    util.generateSourceFile(
        packageName + '.' + commonClassName, commonCode.toString(), vajramInfo.vajramClass());
    util.generateSourceFile(
        packageName + '.' + allFacetsClassname, allFacetsCode.toString(), vajramInfo.vajramClass());
  }

  private static List<AnnotationSpec> recordAnnotations() {
    return annotations(EqualsAndHashCode.class, ToString.class);
  }

  private static List<AnnotationSpec> annotations(Class<?>... annotations) {
    return stream(annotations).map(aClass -> AnnotationSpec.builder(aClass).build()).toList();
  }

  private void addFacetConstants(
      TypeSpec.Builder classBuilder, List<? extends FacetGenModel> facets) {
    List<FieldSpec> specFields = new ArrayList<>(facets.size());
    List<FieldSpec> nameFields = new ArrayList<>();
    for (FacetGenModel facet : facets) {
      if (facet.facetTypes().contains(INPUT)) {
        continue;
      }
      String facetJavaName = toJavaName(facet.name());
      TypeAndName facetType = getTypeName(getDataType(facet));
      TypeAndName boxedFacetType = boxPrimitive(facetType);
      ClassName vajramReqClass = ClassName.get(packageName, requestClassName);
      nameFields.add(createFacetNameField(facet, facetJavaName).build());
      if (facet instanceof InputModel<?> inputDef) {
        specFields.add(
            createFacetSpecField(inputDef, boxedFacetType, vajramReqClass, facetJavaName).build());
      } else if (facet instanceof DependencyModel vajramDepDef) {
        ClassName depReqClass = ClassName.bestGuess(vajramDepDef.depReqClassQualifiedName());
        ClassName specType =
            ClassName.get(
                vajramDepDef.canFanout()
                    ? VajramDepFanoutTypeSpec.class
                    : VajramDepSingleTypeSpec.class);

        specFields.add(
            FieldSpec.builder(
                    ParameterizedTypeName.get(
                        specType, boxedFacetType.typeName(), vajramReqClass, depReqClass),
                    facetJavaName + FACET_SPEC_SUFFIX)
                .addModifiers(STATIC, FINAL)
                .initializer(
                    "new $T<>($L, $S, $T.class, $T.class)",
                    specType,
                    0,
                    facet.name(),
                    vajramReqClass,
                    depReqClass)
                .build());
      }
    }
    classBuilder.addFields(specFields).addFields(nameFields);
  }

  public String getRequestClassName() {
    return requestClassName;
  }

  public String getPackageName() {
    return packageName;
  }

  private String toJavaName(String name) {
    return name;
  }

  private static TypeName optional(TypeName javaType) {
    return ParameterizedTypeName.get(ClassName.get(Optional.class), javaType.box());
  }

  private record TypeAndName(
      TypeName typeName, Optional<TypeMirror> type, List<AnnotationSpec> annotationSpecs) {
    private TypeAndName(TypeName typeName) {
      this(typeName, Optional.empty(), List.of());
    }
  }

  private record FieldTypeName(TypeName typeName, String name) {}
}
