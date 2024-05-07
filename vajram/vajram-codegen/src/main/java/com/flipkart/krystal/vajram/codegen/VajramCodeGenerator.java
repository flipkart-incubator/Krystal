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
import static com.flipkart.krystal.vajram.codegen.Constants.GET_INPUT_RESOLVERS;
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
import static com.flipkart.krystal.vajram.codegen.Utils.getImmutFacetsClassname;
import static com.flipkart.krystal.vajram.codegen.Utils.getFacetsInterfaceName;
import static com.flipkart.krystal.vajram.codegen.Utils.getBatchedFacetsClassname;
import static com.flipkart.krystal.vajram.codegen.Utils.getCommonFacetsClassname;
import static com.flipkart.krystal.vajram.codegen.Utils.getImmutRequestClassName;
import static com.flipkart.krystal.vajram.codegen.Utils.getRequestInterfaceName;
import static com.flipkart.krystal.vajram.codegen.Utils.getTypeParameters;
import static com.flipkart.krystal.vajram.codegen.Utils.getVajramImplClassName;
import static com.flipkart.krystal.vajram.codegen.models.ParsedVajramData.fromVajram;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil.getQualifiedInputsComparator;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static java.util.Arrays.stream;
import static javax.lang.model.element.Modifier.ABSTRACT;
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
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.data.Responses;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.utils.SkippedExecutionException;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDefinitionException;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.batching.BatchableFacetsBuilder;
import com.flipkart.krystal.vajram.batching.BatchableImmutableFacets;
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
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.resolution.sdk.Resolve;
import com.google.common.collect.ImmutableCollection;
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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
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

    // Map of all the resolved dependencies to the methods resolving them
    Map<Integer, List<ExecutableElement>> resolverMap = new HashMap<>();
    for (ExecutableElement resolver : getParsedVajramData().resolvers()) {
      int dependencyId =
          checkNotNull(
              vajramInfo
                  .facetIdsByName()
                  .get(checkNotNull(resolver.getAnnotation(Resolve.class)).depName()));
      resolverMap.computeIfAbsent(dependencyId, _k -> new ArrayList<>()).add(resolver);
    }

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

    methodSpecs.add(createFacetDefinitions());
    methodSpecs.add(createInputResolvers(resolverMap, depFanoutMap));
    createResolvers(resolverMap, depFanoutMap).ifPresent(methodSpecs::add);

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

    ClassName requestInterfaceType =
        ClassName.get(packageName, getRequestInterfaceName(vajramName));
    ClassName immutRequestType = ClassName.get(packageName, getImmutRequestClassName(vajramName));
    ClassName immutFacetsType = ClassName.get(packageName, getImmutFacetsClassname(vajramName));

    StringWriter writer = new StringWriter();
    try {
      JavaFile.builder(
              packageName,
              vajramImplClass
                  .addMethods(methodSpecs)
                  .addMethod(
                      methodBuilder("newRequestBuilder")
                          .returns(immutRequestType.nestedClass("Builder"))
                          .addModifiers(PUBLIC)
                          .addStatement("return $T._builder()", requestInterfaceType)
                          .build())
                  .addMethod(
                      methodBuilder("facetsFromRequest")
                          .returns(immutFacetsType.nestedClass("Builder"))
                          .addModifiers(PUBLIC)
                          .addParameter(
                              ParameterizedTypeName.get(
                                  ClassName.get(Request.class),
                                  WildcardTypeName.subtypeOf(Object.class)),
                              "request")
                          .addStatement(
                              "return new $T(($T)$L)",
                              immutFacetsType.nestedClass("Builder"),
                              requestInterfaceType,
                              "request")
                          .build())
                  .build())
          .indent("  ")
          .build()
          .writeTo(writer);
    } catch (IOException ignored) {

    }
    return writer.toString();
  }

  private MethodSpec createInputResolvers(
      Map<Integer, List<ExecutableElement>> resolverMap, Map<String, Boolean> depFanoutMap) {
    Builder getInputResolversMethod =
        methodBuilder(GET_INPUT_RESOLVERS)
            .addModifiers(PUBLIC)
            .returns(ParameterizedTypeName.get(ImmutableCollection.class, InputResolver.class))
            .addAnnotation(Override.class);

    getInputResolversMethod.addStatement(
        "$T inputResolvers = new $T<>(getSimpleInputResolvers())",
        ParameterizedTypeName.get(List.class, InputResolver.class),
        ArrayList.class);

    resolverMap.forEach(
        (depId, resolverMethods) -> {
          DependencyModel dep =
              vajramInfo.dependencies().stream()
                  .filter(d -> d.id() == depId)
                  .findAny()
                  .orElseThrow();
          for (ExecutableElement resolverMethod : resolverMethods) {
            if (canFanout(resolverMethod, dep)) {}
          }
        });

    return getInputResolversMethod
        .addStatement("return $T.copyOf(inputResolvers)", ImmutableList.class)
        .build();
  }

  private boolean canFanout(ExecutableElement resolverMethod, DependencyModel dep) {
    // Iterate all the resolvers and figure fanout
    // dep inputDef data type and method return type =>
    // 1. depInput = T, if (resolverReturnType is iterable of T || iterable of vajramRequest ||
    // multiExecute) => fanout
    Resolve resolve =
        checkNotNull(
            resolverMethod.getAnnotation(Resolve.class),
            "Resolver method must have 'Resolve' annotation");
    String[] facets = resolve.depInputs();
    String depName = resolve.depName();
    TypeMirror returnType = resolverMethod.getReturnType();
    Types typeUtils = util.getProcessingEnv().getTypeUtils();
    if (util.isRawAssignable(returnType, Request.class)) {
      if (typeUtils.isAssignable(
          returnType, util.getTypeElement(dep.depReqClassQualifiedName()).asType())) {
        return false;
      } else {
        throw new VajramDefinitionException(
            """
             The resolver %s of dep facet %s resolves the vajram named %s
             but does not return the corresponding request of type %s. "
             This is an error.
             """
                .formatted(
                    resolverMethod.getSimpleName(),
                    depName,
                    dep.depVajramId(),
                    dep.depReqClassQualifiedName()));
      }
    } else if (util.isRawAssignable(returnType, DependencyCommand.class)) {
      return util.isRawAssignable(returnType, MultiExecute.class);
    }
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
    ClassName allFacetsClass = ClassName.get(packageName, getFacetsInterfaceName(vajramName));
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
            "return $T.errableFrom(() -> $L(($T)element));",
            Errable.class,
            getParsedVajramData().outputLogic().getSimpleName(),
            allFacetsClass);
        methodCallSuffix = "";
      } else {
        returnBuilder.add(
            "\nreturn $T.errableFrom(() -> $L(\n",
            Errable.class,
            getParsedVajramData().outputLogic().getSimpleName());
        methodCallSuffix = "));\n";
      }
    }
    // merge the code blocks for facets
    //    for (int i = 0; i < inputCodeBlocks.size(); i++) {
    //      // for formatting
    //      returnBuilder.add("\t\t");
    //      returnBuilder.add(inputCodeBlocks.get(i));
    //      if (i != inputCodeBlocks.size() - 1) {
    //        returnBuilder.add(",\n");
    //      }
    //    }
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
                "return $T::new", ClassName.get(packageName, getImmutFacetsClassname(vajramName)))
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
      valueMap.put(UNMOD_INPUT, ClassName.get(packageName, getImmutFacetsClassname(vajramName)));
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
            CodeBlock.builder().beginControlFlow("case $L -> ", resolverId++);
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
    // bind type parameter else error
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

  //  case 1 -> {
  //    List<Integer> numbers = ((ChainAdderFacets) facets).numbers();
  //    MultiExecute<List<Integer>> resolverResult = numbersForSubChainer(numbers);
  //    if (resolverResult.shouldSkip()) {
  //      return SingleExecute.skipExecution(resolverResult.doc());
  //    } else {
  //      return MultiExecute.executeFanoutWith(
  //          List.copyOf(
  //              resolverResult.inputs().stream()
  //                  .map(
  //                      element ->
  //                          (ChainAdderRequest)
  //                              ((ChainAdderRequest) dependecyRequest)
  //                                  ._asBuilder()
  //                                  .numbers(element.orElse(null)))
  //                  .toList()));
  //    }
  //  }

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
          return $T.executeWith(dependecyRequest._toBuilder().$L(
              $L.inputs().iterator().next().orElse(null)))
        """,
            SingleExecute.class,
            depFacetNames[0],
            variableName);

      } else {
        ifBlockBuilder.addStatement(
            "return $T.executeWith(dependecyRequest._toBuilder().$L($L))",
            SingleExecute.class,
            depFacetNames[0],
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
        .add(".id($L)", inputDef.id())
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
              .collect(Collectors.joining(","));
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

  public void codeGenVajramRequest() {
    ImmutableList<InputModel<?>> inputDefs = vajramInfo.inputs();
    ClassName requestInterfaceType =
        ClassName.get(packageName, getRequestInterfaceName(vajramName));
    ClassName immutRequestClassType =
        ClassName.get(packageName, getImmutRequestClassName(vajramName));
    ClassName builderClassType = immutRequestClassType.nestedClass("Builder");
    TypeSpec.Builder requestInterface =
        interfaceBuilder(getRequestInterfaceName(vajramName))
            .addModifiers(PUBLIC)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(Request.class), util.toTypeName(vajramInfo.responseType()).box()))
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "ClassReferencesSubclass")
                    .build());
    List<InputModel<?>> inputs =
        vajramInfo.inputs().stream().filter(i -> i.facetTypes().contains(INPUT)).toList();

    addFacetConstants(requestInterface, inputs);
    for (var input : inputs) {
      createFacetGetter(requestInterface, input, CodeGenParams.builder().isRequest(true).build());
    }

    TypeSpec.Builder immutRequestClass =
        util.classBuilder(getImmutRequestClassName(vajramName))
            .addModifiers(PUBLIC, FINAL)
            .addSuperinterface(requestInterfaceType)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(ImmutableRequest.class),
                    util.toTypeName(vajramInfo.responseType()).box()));
    createFacetMembers(
        immutRequestClass,
        immutRequestClassType,
        vajramInfo.facetStream().filter(facet -> facet.facetTypes().contains(INPUT)).toList(),
        CodeGenParams.builder().isRequest(true).build());

    TypeSpec.Builder builderClass =
        util.classBuilder("Builder")
            .addModifiers(PUBLIC, STATIC, FINAL)
            .addSuperinterface(requestInterfaceType)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(RequestBuilder.class),
                    util.toTypeName(vajramInfo.responseType()).box()));
    createFacetMembers(
        builderClass,
        immutRequestClassType,
        vajramInfo.facetStream().filter(facet -> facet.facetTypes().contains(INPUT)).toList(),
        CodeGenParams.builder().isRequest(true).isBuilder(true).build());

    List<InputModel<?>> clientProvidedInputs =
        inputDefs.stream().filter(inputDef -> inputDef.facetTypes().contains(INPUT)).toList();
    try {
      StringWriter writer = new StringWriter();
      JavaFile.builder(
              packageName,
              requestInterface
                  .addMethod(
                      methodBuilder("_build")
                          .addModifiers(PUBLIC, ABSTRACT)
                          .returns(immutRequestClassType)
                          .build())
                  .addMethod(
                      methodBuilder("_asBuilder")
                          .addModifiers(PUBLIC, ABSTRACT)
                          .returns(immutRequestClassType.nestedClass("Builder"))
                          .build())
                  .addMethod(
                      methodBuilder("_builder")
                          .addModifiers(PUBLIC, STATIC)
                          .returns(builderClassType)
                          .addStatement(
                              "return new $T()", immutRequestClassType.nestedClass("Builder"))
                          .build())
                  .build())
          .build()
          .writeTo(writer);
      util.generateSourceFile(
          getPackageName() + '.' + getRequestInterfaceName(vajramName),
          writer.toString(),
          vajramInfo.vajramClass());
    } catch (IOException ignored) {

    }
    try {
      StringWriter writer = new StringWriter();

      JavaFile.builder(
              packageName,
              immutRequestClass
                  .addMethods(
                      createFacetContainerMethods(
                          clientProvidedInputs,
                          immutRequestClassType,
                          CodeGenParams.builder().isRequest(true).build()))
                  .addType(
                      builderClass
                          .addMethods(
                              createFacetContainerMethods(
                                  clientProvidedInputs,
                                  immutRequestClassType,
                                  CodeGenParams.builder().isRequest(true).isBuilder(true).build()))
                          .build())
                  .build())
          .build()
          .writeTo(writer);
      util.generateSourceFile(
          getPackageName() + '.' + getImmutRequestClassName(vajramName),
          writer.toString(),
          vajramInfo.vajramClass());
    } catch (IOException ignored) {

    }
  }

  private void createFacetMembers(
      TypeSpec.Builder clazz,
      ClassName enclosingClassType,
      List<? extends FacetGenModel> eligibleFacets,
      CodeGenParams codeGenParams) {
    if (codeGenParams.wrapsRequest && codeGenParams.isRequest) {
      throw new IllegalArgumentException("A request cannot wrap another request - this is a bug");
    }
    if (codeGenParams.isUnBatched && codeGenParams.wrapsRequest) {
      throw new IllegalArgumentException("A batchable does not wrap request - this is a bug");
    }

    if (codeGenParams.isUnBatched && codeGenParams.isRequest) {
      throw new IllegalArgumentException("A batchable should not be a request - this is a bug");
    }
    Builder fullConstructor = constructorBuilder();
    clazz.addAnnotations(
        codeGenParams.isBuilder ? annotations(ToString.class) : recordAnnotations());
    ClassName requestInterfacetType =
        ClassName.get(packageName, getRequestInterfaceName(vajramName));
    ClassName immutRequestType = ClassName.get(packageName, getImmutRequestClassName(vajramName));
    ClassName batchFacetsType = ClassName.get(packageName, getBatchedFacetsClassname(vajramName));
    ClassName commonFacetsType = ClassName.get(packageName, getCommonFacetsClassname(vajramName));
    if (codeGenParams.wrapsRequest) {
      ClassName requestOrBuilderType =
          codeGenParams.isBuilder ? immutRequestType.nestedClass("Builder") : immutRequestType;
      fullConstructor.addParameter(ParameterSpec.builder(requestOrBuilderType, "_request").build());
      fullConstructor.addStatement("this._request = _request");
      FieldSpec.Builder field = FieldSpec.builder(requestOrBuilderType, "_request", PRIVATE, FINAL);
      if (!codeGenParams.isBuilder) {
        field.addModifiers(FINAL);
      }
      clazz.addField(field.build());
    } else if (codeGenParams.isUnBatched) {
      ClassName batchOrBuilderType =
          codeGenParams.isBuilder ? batchFacetsType.nestedClass("Builder") : batchFacetsType;
      ClassName commonOrBuilderType =
          codeGenParams.isBuilder ? commonFacetsType.nestedClass("Builder") : commonFacetsType;
      fullConstructor.addParameter(ParameterSpec.builder(batchOrBuilderType, "_batchable").build());
      fullConstructor.addParameter(ParameterSpec.builder(commonOrBuilderType, "_common").build());
      fullConstructor.addStatement("this._batchable = _batchable");
      fullConstructor.addStatement("this._common = _common");

      FieldSpec.Builder batchField = FieldSpec.builder(batchOrBuilderType, "_batchable", PRIVATE);
      FieldSpec.Builder commonField = FieldSpec.builder(commonOrBuilderType, "_common", PRIVATE);

      if (!codeGenParams.isBuilder) {
        batchField.addModifiers(FINAL);
        commonField.addModifiers(FINAL);
      }
      clazz.addField(batchField.build());
      clazz.addField(commonField.build());
    }
    for (FacetGenModel facet : eligibleFacets) {
      TypeAndName facetType = getTypeName(getDataType(facet));
      TypeAndName boxedFacetType = boxPrimitive(facetType);

      ReturnType returnType = getReturnType(facet);
      boolean isInput = facet.facetTypes().contains(INPUT);
      ParameterSpec facetParam =
          ParameterSpec.builder(
                  (switch (returnType) {
                        case simple, optional -> boxedFacetType.typeName();
                        case errable -> errable(facetType);
                        case responses -> responsesType((DependencyModel) facet);
                      })
                      .annotated(
                          annotations(
                              switch (returnType) {
                                case simple, optional -> Nullable.class;
                                case errable, responses -> NonNull.class;
                              })),
                  facet.name())
              .build();
      if (!codeGenParams.isUnBatched && (isInput ? !codeGenParams.wrapsRequest : true)) {
        FieldSpec.Builder facetField =
            FieldSpec.builder(
                (switch (returnType) {
                      case simple, optional -> boxedFacetType.typeName();
                      case errable -> errable(facetType);
                      case responses -> responsesType((DependencyModel) facet);
                    })
                    .annotated(
                        annotations(
                            switch (returnType) {
                              case simple, optional -> Nullable.class;
                              case errable, responses -> NonNull.class;
                            })),
                facet.name(),
                PRIVATE);
        if (codeGenParams.isBuilder) {
          switch (returnType) {
            case errable -> facetField.initializer("$T.nil()", Errable.class);
            case responses -> facetField.initializer("$T.empty()", Responses.class);
            default -> {}
          }
        } else {
          facetField.addModifiers(FINAL);
        }
        clazz.addField(facetField.build());
        fullConstructor.addParameter(facetParam);
        fullConstructor.addStatement("this.$L = $L", facet.name(), facet.name());
      }
      // Getter
      createFacetGetter(clazz, facet, codeGenParams.toBuilder().withImpl(true).build());
      if (codeGenParams.isBuilder) {
        clazz.addMethod(
            // Setter
            // public Builder inputName(Type inputName){this.inputName = inputName; return this;}
            methodBuilder(facet.name())
                .returns(enclosingClassType.nestedClass("Builder"))
                .addModifiers(PUBLIC)
                .addParameter(facetParam)
                .addStatement(
                    codeGenParams.isUnBatched
                        ? CodeBlock.of(
                            "this.%s.$L($L)"
                                .formatted(facet.isBatched() ? "_batchable" : "_common"),
                            facet.name(),
                            facet.name())
                        : isInput && codeGenParams.wrapsRequest
                            ? CodeBlock.of("this._request.$L($L)", facet.name(), facet.name())
                            : CodeBlock.of("this.$L = $L", facet.name(), facet.name())) // Set value
                .addStatement("return this", facet.name()) // Return
                .build());
      }
    }
    clazz.addMethod(fullConstructor.build());
    if (codeGenParams.isBuilder && !fullConstructor.parameters.isEmpty()) {
      // Make sure there is always a no-arg constructor for the builder
      clazz.addMethod(
          constructorBuilder()
              .addCode(
                  codeGenParams.wrapsRequest
                      ? CodeBlock.builder()
                          .addStatement("this._request = $T._builder()", requestInterfacetType)
                          .build()
                      : codeGenParams.isUnBatched
                          ? CodeBlock.builder()
                              .addStatement("this._batchable = $T._builder()", batchFacetsType)
                              .addStatement("this._common = $T._builder()", commonFacetsType)
                              .build()
                          : CodeBlock.builder().build())
              .build());
    }
    if (codeGenParams.isBuilder && codeGenParams.wrapsRequest) {
      // Make sure Builder always has a constructor which accepts only the request
      // See Vajram#facetsFromRequest
      clazz.addMethod(
          constructorBuilder()
              .addParameter(
                  ParameterSpec.builder(
                          ClassName.get(packageName, getRequestInterfaceName(vajramName)),
                          "request")
                      .build())
              .addCode(
                  CodeBlock.builder().addStatement("this._request = request._asBuilder()").build())
              .build());
    }
  }

  @lombok.Builder(toBuilder = true)
  private record CodeGenParams(
      boolean isRequest,
      boolean isBuilder,
      boolean wrapsRequest,
      boolean isUnBatched,
      boolean withImpl) {

    private static final CodeGenParams DEFAULT = CodeGenParams.builder().build();
  }

  private void createFacetGetter(
      TypeSpec.Builder clazz, FacetGenModel facet, CodeGenParams codeGenParams) {
    TypeAndName facetType = getTypeName(getDataType(facet));
    Builder method;
    if (!(facet instanceof DependencyModel dep) || !dep.canFanout()) {
      // Non-fanout facet
      ReturnType returnType = getReturnType(facet);
      TypeAndName boxableType =
          new TypeAndName(
              facetType
                  .typeName()
                  // Remove @Nullable because Optional<@Nullable T> is not useful.
                  .withoutAnnotations(),
              facetType.type(),
              List.of());

      boolean facetInCurrentClass =
          !codeGenParams.isUnBatched
              && (codeGenParams.isRequest
                  || !facet.facetTypes().contains(INPUT)
                  || !codeGenParams.wrapsRequest);
      method =
          methodBuilder(facet.name())
              .addModifiers(PUBLIC)
              .returns(
                  switch (returnType) {
                    case simple ->
                        unboxPrimitive(facetType)
                            .typeName()
                            // Remove @Nullable because getter has null check
                            // and will never return null.
                            .withoutAnnotations();
                    case optional -> optional(boxableType);
                    case errable -> errable(boxableType);
                    case responses -> throw new AssertionError();
                  });
      if (codeGenParams.withImpl) {
        method
            .addCode(
                ReturnType.simple.equals(returnType) && facetInCurrentClass
                    ? CodeBlock.of(
                        """
                      if($L == null) {
                        throw new IllegalStateException(
                            "The facet '$L' is not optional, but has null value.\
                             This should not happen");
                      }""",
                        facet.name(),
                        facet.name())
                    : CodeBlock.builder().build())
            .addStatement(
                facetInCurrentClass
                    ? switch (returnType) {
                      case optional ->
                          CodeBlock.of(
                              "return $T.ofNullable(this.$L)", Optional.class, facet.name());
                      case simple, errable -> CodeBlock.of("return this.$L", facet.name());
                      case responses -> throw new AssertionError();
                    }
                    : codeGenParams.wrapsRequest
                        ? CodeBlock.of("return this._request.$L()", facet.name())
                        : /*Unbatched*/ CodeBlock.of(
                            "return this.%s.$L()"
                                .formatted(facet.isBatched() ? "_batchable" : "_common"),
                            facet.name()));
      }
    } else {
      // Fanout dependency
      method =
          methodBuilder(dep.name())
              .addModifiers(PUBLIC)
              .returns(wrapWithFacetValueClass(dep, facetType));
      if (codeGenParams.withImpl) {
        method.addStatement(
            codeGenParams.isUnBatched
                ? CodeBlock.of(
                    "return this.%s.$L()".formatted(facet.isBatched() ? "_batchable" : "_common"),
                    dep.name())
                : CodeBlock.of("return this.$L()", dep.name()));
      }
    }
    if (!codeGenParams.withImpl) {
      method.addModifiers(ABSTRACT);
    }
    clazz.addMethod(method.build());
  }

  private TypeName wrapWithFacetValueClass(FacetGenModel facet, TypeAndName facetType) {
    return facet instanceof DependencyModel dep && dep.canFanout()
        ? responsesType(dep)
        : errable(facetType);
  }

  private ParameterizedTypeName responsesType(TypeName reqClass, Class<?> responseClass) {
    return responsesType(new TypeAndName(reqClass), new TypeAndName(ClassName.get(responseClass)));
  }

  private ParameterizedTypeName responsesType(DependencyModel dep) {
    return responsesType(
        new TypeAndName(toClassName(dep.depReqClassQualifiedName())), getTypeName(dep.dataType()));
  }

  private ParameterizedTypeName responsesType(TypeAndName requestType, TypeAndName facetType) {
    return ParameterizedTypeName.get(
        ClassName.get(Responses.class),
        boxPrimitive(requestType).typeName(),
          boxPrimitive(facetType).typeName());
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
        .addModifiers(PUBLIC, STATIC, FINAL)
        .initializer(
            "new $T<>($L, $S, $T.class)",
            VajramFacetSpec.class,
            facet.id(),
            facet.name(),
            vajramReqClass);
  }

  private static FieldSpec.Builder createFacetNameField(FacetGenModel facet, String facetJavaName) {
    return FieldSpec.builder(String.class, facetJavaName + FACET_NAME_SUFFIX)
        .addModifiers(PUBLIC, STATIC, FINAL)
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
      List<? extends FacetGenModel> eligibleFacets,
      ClassName enclosingClassName,
      CodeGenParams codeGenParams) {

    if (codeGenParams.wrapsRequest && codeGenParams.isUnBatched) {
      throw new IllegalArgumentException("Unbatched class does not wrap request - this is a bug");
    }

    List<Builder> methodBuilders = new ArrayList<>();

    String facetIdParamName = "facetId";
    methodBuilders.add(_getMethod(eligibleFacets, codeGenParams.isRequest));
    methodBuilders.add(
        _asMapMethod(eligibleFacets, codeGenParams.isRequest, codeGenParams.isBuilder));

    String constructorParamString =
        (codeGenParams.isRequest
                ? eligibleFacets.stream().map(FacetGenModel::name)
                : codeGenParams.wrapsRequest
                    ? Stream.concat(
                        Stream.of("%s"),
                        eligibleFacets.stream()
                            .filter(f -> !f.facetTypes().contains(INPUT))
                            .map(FacetGenModel::name))
                    : codeGenParams.isUnBatched
                        ? Stream.of("%s")
                        : eligibleFacets.stream().map(FacetGenModel::name))
            .collect(Collectors.joining(", "));
    {
      methodBuilders.add(
          methodBuilder("_build")
              .returns(enclosingClassName)
              .addStatement(
                  codeGenParams.isBuilder
                      ? "return new %s(%s)"
                          .formatted(
                              enclosingClassName.simpleName(),
                              constructorParamString.formatted(
                                  codeGenParams.wrapsRequest
                                      ? "_request._build()"
                                      : "_batchable._build(), _common._build()"))
                      : "return this"));
    }

    {
      methodBuilders.add(
          methodBuilder("_asBuilder")
              .returns(enclosingClassName.nestedClass("Builder"))
              .addStatement(
                  codeGenParams.isBuilder
                      ? "return this"
                      : "return new Builder(%s)"
                          .formatted(
                              constructorParamString.formatted(
                                  codeGenParams.wrapsRequest
                                      ? "_request._asBuilder()"
                                      : "_batchable._asBuilder(), _common._asBuilder()"))));
    }

    {
      methodBuilders.add(
          methodBuilder("_newCopy")
              .returns(
                  codeGenParams.isBuilder
                      ? enclosingClassName.nestedClass("Builder")
                      : enclosingClassName)
              .addStatement(
                  codeGenParams.isBuilder
                      ? "return new Builder(%s)"
                          .formatted(
                              constructorParamString.formatted(
                                  codeGenParams.wrapsRequest
                                      ? "_request._newCopy()"
                                      : "_batchable._newCopy(), _common._newCopy()"))
                      : "return this"));
    }

    if (!codeGenParams.isRequest) {
      methodBuilders.add(_getErrableMethod());
    }
    if (!codeGenParams.isRequest) {
      methodBuilders.add(_getDepResponsesMethod());
    }
    if (codeGenParams.isBuilder) {
      // _set
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
                      facetValueParamName));
      if (!eligibleFacets.isEmpty()) {
        setMethod
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "unchecked")
                    .build())
            .beginControlFlow("switch ($L)", facetIdParamName);
        if (codeGenParams.isRequest) {
          eligibleFacets.stream()
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
          eligibleFacets.forEach(
              facet -> {
                ReturnType returnType = getReturnType(facet);
                if (facet.facetTypes().contains(INPUT) && codeGenParams.wrapsRequest) {
                  setMethod.addStatement(
                      "case $L -> this._request.$L((($T)$L).valueOpt().orElse(null))",
                      facet.id(),
                      facet.name(),
                      errable(getTypeName(facet.dataType())),
                      facetValueParamName);
                } else {
                  setMethod.addStatement(
                      switch (returnType) {
                        case simple, optional ->
                            CodeBlock.of(
                                codeGenParams.isUnBatched
                                    ? facet.isBatched()
                                        ? "case $L -> this._batchable.$L((($T)$L).valueOpt().orElse(null))"
                                        : "case $L -> this._common.$L((($T)$L).valueOpt().orElse(null))"
                                    : "case $L -> this.$L((($T)$L).valueOpt().orElse(null))",
                                facet.id(),
                                facet.name(),
                                errable(getTypeName(facet.dataType())),
                                facetValueParamName);
                        case errable, responses ->
                            CodeBlock.of(
                                codeGenParams.isUnBatched
                                    ? facet.isBatched()
                                        ? "case $L -> this._batchable.$L(($T)$L)"
                                        : "case $L -> this._common.$L(($T)$L)"
                                    : "case $L -> this.$L(($T)$L)",
                                facet.id(),
                                facet.name(),
                                ReturnType.errable.equals(returnType)
                                    ? errable(getTypeName(facet.dataType()))
                                    : responsesType((DependencyModel) facet),
                                facetValueParamName);
                      });
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
      } else {
        setMethod.addStatement(
            "throw new $T($S + $L)",
            IllegalArgumentException.class,
            "Unrecognized facet id",
            facetIdParamName);
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

  private Builder _getDepResponsesMethod() {
    String facetIdParamName = "facetId";
    ParameterizedTypeName responsesType =
        responsesType(ParameterizedTypeName.get(Request.class, Object.class), Object.class);
    return methodBuilder("_getDepResponses")
        .returns(responsesType)
        .addParameter(int.class, facetIdParamName)
        .addStatement("return ($T)_get($L)", responsesType, facetIdParamName);
  }

  private Builder _getErrableMethod() {
    String facetIdParamName = "facetId";
    return methodBuilder("_getErrable")
        .returns(errable(Object.class))
        .addParameter(int.class, facetIdParamName)
        .addStatement("return ($T)_get($L)", errable(Object.class), facetIdParamName);
  }

  private static Builder _asMapMethod(
      List<? extends FacetGenModel> eligibleFacets, boolean isRequest, boolean isBuilder) {
    return methodBuilder("_asMap")
        .returns(
            ParameterizedTypeName.get(
                ClassName.get(isBuilder ? Map.class : ImmutableMap.class),
                ClassName.get(Integer.class),
                ParameterizedTypeName.get(
                    ClassName.get(isRequest ? Errable.class : FacetValue.class),
                    WildcardTypeName.subtypeOf(Object.class))))
        .addStatement(
            """
            return $T.of(
            %s)
            """
                .formatted(
                    eligibleFacets.stream()
                        .map(
                            facetDef -> {
                              return switch (getReturnType(facetDef)) {
                                case optional, simple -> "$L, $T.withValue($L())";
                                default -> "$L, $L()";
                              };
                            })
                        .collect(Collectors.joining(",\n"))),
            Stream.concat(
                    Stream.of(ImmutableMap.class),
                    eligibleFacets.stream()
                        .<Object>mapMulti(
                            (facetGenModel, consumer) -> {
                              ReturnType returnType = getReturnType(facetGenModel);
                              consumer.accept(facetGenModel.id());
                              if (ReturnType.optional.equals(returnType)
                                  || ReturnType.simple.equals(returnType)) {
                                consumer.accept(Errable.class);
                              }
                              consumer.accept(facetGenModel.name());
                            }))
                .toArray());
  }

  private Builder _getMethod(List<? extends FacetGenModel> eligibleFacets, boolean isRequest) {
    String facetIdParamName = "facetId";
    Builder getMethod =
        methodBuilder("_get")
            .returns(
                isRequest
                    ? errable(Object.class)
                    : ParameterizedTypeName.get(
                        ClassName.get(FacetValue.class), WildcardTypeName.subtypeOf(Object.class)))
            .addParameter(int.class, facetIdParamName);
    if (!eligibleFacets.isEmpty()) {
      getMethod.beginControlFlow("return switch ($L)", facetIdParamName);
      eligibleFacets.forEach(
          facetDef -> {
            ReturnType returnType = getReturnType(facetDef);
            getMethod.addStatement(
                switch (returnType) {
                  case responses, errable ->
                      CodeBlock.of("case $L -> $L()", facetDef.id(), facetDef.name());
                  case simple, optional ->
                      CodeBlock.of(
                          "case $L -> $T.withValue($L())",
                          facetDef.id(),
                          Errable.class,
                          facetDef.name());
                });
          });
      getMethod
          .addStatement(
              "default -> throw new $T($S + $L)",
              IllegalArgumentException.class,
              "Unrecognized facet id",
              facetIdParamName)
          .endControlFlow()
          .addCode(";");
    } else {
      getMethod.addStatement(
          "throw new $T($S + $L)",
          IllegalArgumentException.class,
          "Unrecognized facet id",
          facetIdParamName);
    }
    return getMethod;
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

  enum ReturnType {
    simple,
    optional,
    errable,
    responses,
  }

  private static ReturnType getReturnType(FacetGenModel facet) {
    ReturnType returnType;
    if (facet instanceof DependencyModel dep && dep.canFanout()) {
      return ReturnType.responses;
    } else if (!facet.isMandatory()) {
      if (!facet.facetTypes().contains(INPUT)) {
        returnType = ReturnType.errable;
      } else {
        returnType = ReturnType.optional;
      }
    } else {
      returnType = ReturnType.simple;
    }
    return returnType;
  }

  public void codeGenFacets() {
    boolean doInputsNeedBatching = vajramInfo.facetStream().anyMatch(FacetGenModel::isBatched);
    if (doInputsNeedBatching) {
      batchFacetsClasses();
    } else {
      simpleFacetsClasses();
    }
  }

  private void simpleFacetsClasses() {
    List<FacetGenModel> allFacets = vajramInfo.facetStream().toList();

    ClassName facetsType = ClassName.get(packageName, getFacetsInterfaceName(vajramName));
    ClassName immutableFacetsType = ClassName.get(packageName, getImmutFacetsClassname(vajramName));

    TypeSpec.Builder facetsInterface =
        interfaceBuilder(getFacetsInterfaceName(vajramName))
            .addSuperinterface(Facets.class)
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "ClassReferencesSubclass")
                    .build());
    addFacetConstants(
        facetsInterface,
        vajramInfo.facetStream().filter(f -> !f.facetTypes().contains(INPUT)).toList());
    allFacets.forEach(
        facet -> {
          createFacetGetter(
              facetsInterface, facet, CodeGenParams.builder().wrapsRequest(true).build());
        });

    TypeSpec.Builder immutFacetsClass =
        util.classBuilder(getImmutFacetsClassname(vajramName))
            .addModifiers(FINAL)
            .addSuperinterface(facetsType)
            .addSuperinterface(ImmutableFacets.class);
    createFacetMembers(
        immutFacetsClass,
        immutableFacetsType,
        allFacets,
        CodeGenParams.builder().wrapsRequest(true).build());

    TypeSpec.Builder facetsBuilderClass =
        util.classBuilder("Builder")
            .addModifiers(STATIC, FINAL)
            .addSuperinterface(facetsType)
            .addSuperinterface(FacetsBuilder.class);
    createFacetMembers(
        facetsBuilderClass,
        immutableFacetsType,
        allFacets,
        CodeGenParams.builder().wrapsRequest(true).isBuilder(true).build());

    try {
      StringWriter writer = new StringWriter();
      JavaFile.builder(
              packageName,
              facetsInterface
                  .addMethod(
                      methodBuilder("_build")
                          .addModifiers(PUBLIC, ABSTRACT)
                          .returns(immutableFacetsType)
                          .build())
                  .addMethod(
                      methodBuilder("_asBuilder")
                          .addModifiers(PUBLIC, ABSTRACT)
                          .returns(immutableFacetsType.nestedClass("Builder"))
                          .build())
                  .addMethod(
                      methodBuilder("_builder")
                          .addModifiers(PUBLIC, STATIC)
                          .returns(immutableFacetsType.nestedClass("Builder"))
                          .addStatement(
                              "return new $T()", immutableFacetsType.nestedClass("Builder"))
                          .build())
                  .build())
          .build()
          .writeTo(writer);
      util.generateSourceFile(
          getPackageName() + '.' + getFacetsInterfaceName(getVajramName()),
          writer.toString(),
          vajramInfo.vajramClass());
    } catch (IOException e) {
      util.error(String.valueOf(e.getMessage()), vajramInfo.vajramClass());
    }
    try {
      StringWriter writer = new StringWriter();
      JavaFile.builder(
              packageName,
              immutFacetsClass
                  .addMethods(
                      createFacetContainerMethods(
                          allFacets,
                          immutableFacetsType,
                          CodeGenParams.builder().wrapsRequest(true).build()))
                  .addType(
                      facetsBuilderClass
                          .addMethods(
                              createFacetContainerMethods(
                                  allFacets,
                                  immutableFacetsType,
                                  CodeGenParams.builder()
                                      .wrapsRequest(true)
                                      .isBuilder(true)
                                      .build()))
                          .build())
                  .build())
          .build()
          .writeTo(writer);
      util.generateSourceFile(
          getPackageName() + '.' + getImmutFacetsClassname(getVajramName()),
          writer.toString(),
          vajramInfo.vajramClass());
    } catch (IOException e) {
      util.error(String.valueOf(e.getMessage()), vajramInfo.vajramClass());
    }
  }

  private static ClassName toClassName(String depReqClassName) {
    int lastDotIndex = depReqClassName.lastIndexOf('.');
    return ClassName.get(
        depReqClassName.substring(0, lastDotIndex), depReqClassName.substring(lastDotIndex + 1));
  }

  private void batchFacetsClasses() {
    StringWriter batchCode = new StringWriter();
    StringWriter commonCode = new StringWriter();
    StringWriter allFacetsCode = new StringWriter();

    String batchClassName = getBatchedFacetsClassname(vajramName);
    String commonClassName = getCommonFacetsClassname(vajramName);

    ClassName batchFacetsType = ClassName.get(packageName, batchClassName);
    ClassName batchBuilderType = batchFacetsType.nestedClass("Builder");

    ClassName commonFacetsType = ClassName.get(packageName, commonClassName);
    ClassName commonBuilderType = commonFacetsType.nestedClass("Builder");

    ClassName allFacetsType = ClassName.get(packageName, getImmutFacetsClassname(vajramName));
    ClassName allFacetsBuilderType = allFacetsType.nestedClass("Builder");

    List<InputModel<?>> batchedFacets =
        vajramInfo.inputs().stream().filter(InputModel::isBatched).toList();
    List<FacetGenModel> commonFacets =
        vajramInfo.facetStream().filter(t -> !t.isBatched()).toList();
    TypeSpec.Builder batchFacetsClass =
        util.classBuilder(batchClassName)
            .addModifiers(FINAL)
            .addSuperinterface(ImmutableFacets.class)
            .addMethod(
                methodBuilder("_builder")
                    .addModifiers(PUBLIC, STATIC)
                    .returns(batchBuilderType)
                    .addStatement("return new Builder()")
                    .build());
    createFacetMembers(batchFacetsClass, batchFacetsType, batchedFacets, CodeGenParams.DEFAULT);

    TypeSpec.Builder batchFacetsBuilderClass =
        util.classBuilder("Builder")
            .addModifiers(STATIC, FINAL)
            .addSuperinterface(FacetsBuilder.class);
    createFacetMembers(
        batchFacetsBuilderClass,
        batchFacetsType,
        batchedFacets,
        CodeGenParams.builder().isBuilder(true).build());

    TypeSpec.Builder commonFacetsClass =
        util.classBuilder(commonClassName)
            .addModifiers(FINAL)
            .addSuperinterface(ImmutableFacets.class)
            .addMethod(
                methodBuilder("_builder")
                    .addModifiers(PUBLIC, STATIC)
                    .returns(commonBuilderType)
                    .addStatement("return new Builder()")
                    .build());

    createFacetMembers(
        commonFacetsClass,
        commonFacetsType,
        vajramInfo.facetStream().filter(facetGenModel -> !facetGenModel.isBatched()).toList(),
        CodeGenParams.DEFAULT);

    TypeSpec.Builder commonFacetsBuilderClass =
        util.classBuilder("Builder")
            .addModifiers(STATIC, FINAL)
            .addSuperinterface(FacetsBuilder.class);

    createFacetMembers(
        commonFacetsBuilderClass,
        commonFacetsType,
        vajramInfo.facetStream().filter(facetGenModel -> !facetGenModel.isBatched()).toList(),
        CodeGenParams.builder().isBuilder(true).build());

    TypeSpec.Builder allFacetsClass =
        codegenBatchableFacets(
                TypeSpec.classBuilder(getImmutFacetsClassname(vajramName))
                    .addSuperinterface(ClassName.get(BatchableImmutableFacets.class)),
                batchFacetsType,
                commonFacetsType)
            .addMethod(
                methodBuilder("_builder")
                    .addModifiers(PUBLIC, STATIC)
                    .returns(allFacetsBuilderType)
                    .addStatement("return new Builder()")
                    .build());
    addFacetConstants(
        allFacetsClass,
        vajramInfo.facetStream().filter(f -> !f.facetTypes().contains(INPUT)).toList());
    createFacetMembers(
        allFacetsClass,
        allFacetsType,
        vajramInfo.facetStream().toList(),
        CodeGenParams.builder().isUnBatched(true).build());

    TypeSpec.Builder allFacetsBuilderClass =
        codegenBatchableFacets(
            TypeSpec.classBuilder("Builder")
                .addSuperinterface(ClassName.get(BatchableFacetsBuilder.class))
                .addModifiers(STATIC),
            batchBuilderType,
            commonBuilderType);
    createFacetMembers(
        allFacetsBuilderClass,
        allFacetsType,
        vajramInfo.facetStream().toList(),
        CodeGenParams.builder().isUnBatched(true).isBuilder(true).build());

    try {
      JavaFile.builder(
              packageName,
              batchFacetsClass
                  .addMethods(
                      createFacetContainerMethods(
                          batchedFacets, batchFacetsType, CodeGenParams.DEFAULT))
                  .addType(
                      batchFacetsBuilderClass
                          .addMethods(
                              createFacetContainerMethods(
                                  batchedFacets,
                                  batchFacetsType,
                                  CodeGenParams.builder().isBuilder(true).build()))
                          .build())
                  .build())
          .build()
          .writeTo(batchCode);
      JavaFile.builder(
              packageName,
              commonFacetsClass
                  .addMethods(
                      createFacetContainerMethods(
                          commonFacets, commonFacetsType, CodeGenParams.DEFAULT))
                  .addType(
                      commonFacetsBuilderClass
                          .addMethods(
                              createFacetContainerMethods(
                                  commonFacets,
                                  commonFacetsType,
                                  CodeGenParams.builder().isBuilder(true).build()))
                          .build())
                  .build())
          .build()
          .writeTo(commonCode);
      JavaFile.builder(
              packageName,
              allFacetsClass
                  .addMethods(
                      createFacetContainerMethods(
                          vajramInfo.facetStream().toList(),
                          allFacetsType,
                          CodeGenParams.builder().isUnBatched(true).build()))
                  .addType(
                      allFacetsBuilderClass
                          .addMethods(
                              createFacetContainerMethods(
                                  vajramInfo.facetStream().toList(),
                                  allFacetsType,
                                  CodeGenParams.builder()
                                      .isUnBatched(true)
                                      .isBuilder(true)
                                      .build()))
                          .build())
                  .build())
          .build()
          .writeTo(allFacetsCode);
    } catch (IOException e) {
      util.error(String.valueOf(e.getMessage()), vajramInfo.vajramClass());
    }
    util.generateSourceFile(
        packageName + '.' + batchClassName, batchCode.toString(), vajramInfo.vajramClass());
    util.generateSourceFile(
        packageName + '.' + commonClassName, commonCode.toString(), vajramInfo.vajramClass());
    util.generateSourceFile(
        packageName + '.' + getImmutFacetsClassname(vajramName),
        allFacetsCode.toString(),
        vajramInfo.vajramClass());
  }

  private TypeSpec.Builder codegenBatchableFacets(
      TypeSpec.Builder allFacetsType, ClassName batchFacetsType, ClassName commonFacetsType) {
    return allFacetsType
        .addModifiers(FINAL)
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
      String facetJavaName = facet.name();
      nameFields.add(createFacetNameField(facet, facetJavaName).build());

      TypeAndName facetType = getTypeName(getDataType(facet));
      TypeAndName boxedFacetType = boxPrimitive(facetType);
      ClassName vajramReqClass = ClassName.get(packageName, getRequestInterfaceName(vajramName));
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
                .addModifiers(PUBLIC, STATIC, FINAL)
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

  public String getPackageName() {
    return packageName;
  }

  private String toJavaName(String name) {
    return name;
  }

  private TypeName optional(TypeAndName javaType) {
    return ParameterizedTypeName.get(
        ClassName.get(Optional.class), boxPrimitive(javaType).typeName());
  }

  private TypeName errable(Class<?> clazz) {
    return errable(new TypeAndName(ClassName.get(clazz)));
  }

  private TypeName errable(TypeAndName javaType) {
    return ParameterizedTypeName.get(
        ClassName.get(Errable.class), boxPrimitive(javaType).typeName());
  }

  private record TypeAndName(
      TypeName typeName, Optional<TypeMirror> type, List<AnnotationSpec> annotationSpecs) {
    private TypeAndName(TypeName typeName) {
      this(typeName, Optional.empty(), List.of());
    }
  }
}
