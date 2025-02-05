package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.facets.FacetType.INJECTION;
import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.flipkart.krystal.vajram.codegen.Constants.BATCHES_VAR;
import static com.flipkart.krystal.vajram.codegen.Constants.BATCH_FACETS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.COMMON_FACETS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.FACETS_LIST;
import static com.flipkart.krystal.vajram.codegen.Constants.FACETVALUES_VAR;
import static com.flipkart.krystal.vajram.codegen.Constants.FACET_DEFINITIONS_VAR;
import static com.flipkart.krystal.vajram.codegen.Constants.FACET_ID_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.FACET_SPEC_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.GET_INPUT_RESOLVERS;
import static com.flipkart.krystal.vajram.codegen.Constants.GET_SIMPLE_INPUT_RESOLVERS;
import static com.flipkart.krystal.vajram.codegen.Constants.INCOMING_FACETS;
import static com.flipkart.krystal.vajram.codegen.Constants.METHOD_EXECUTE;
import static com.flipkart.krystal.vajram.codegen.Constants.METHOD_EXECUTE_COMPUTE;
import static com.flipkart.krystal.vajram.codegen.Constants.RESOLVER_REQUEST;
import static com.flipkart.krystal.vajram.codegen.Constants.RESOLVER_REQUESTS;
import static com.flipkart.krystal.vajram.codegen.Constants.RESOLVER_RESULT;
import static com.flipkart.krystal.vajram.codegen.Constants.RESOLVER_RESULTS;
import static com.flipkart.krystal.vajram.codegen.Constants._FACETS_CLASS;
import static com.flipkart.krystal.vajram.codegen.Utils.annotations;
import static com.flipkart.krystal.vajram.codegen.Utils.getFacetsInterfaceName;
import static com.flipkart.krystal.vajram.codegen.Utils.getImmutFacetsClassname;
import static com.flipkart.krystal.vajram.codegen.Utils.getImmutRequestInterfaceName;
import static com.flipkart.krystal.vajram.codegen.Utils.getImmutRequestPojoName;
import static com.flipkart.krystal.vajram.codegen.Utils.getRequestInterfaceName;
import static com.flipkart.krystal.vajram.codegen.Utils.getTypeParameters;
import static com.flipkart.krystal.vajram.codegen.Utils.getVajramImplClassName;
import static com.flipkart.krystal.vajram.codegen.models.ParsedVajramData.fromVajram;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.MethodSpec.overriding;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.data.FacetValuesContainer;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.flipkart.krystal.data.ImmutableFacetValuesContainer;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.facets.resolution.ResolutionTarget;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.batching.Batch;
import com.flipkart.krystal.vajram.batching.BatchEnabledFacetValues;
import com.flipkart.krystal.vajram.batching.BatchEnabledImmutableFacetValues;
import com.flipkart.krystal.vajram.batching.BatchedFacets;
import com.flipkart.krystal.vajram.codegen.models.DependencyModel;
import com.flipkart.krystal.vajram.codegen.models.FacetGenModel;
import com.flipkart.krystal.vajram.codegen.models.GivenFacetModel;
import com.flipkart.krystal.vajram.codegen.models.ParsedVajramData;
import com.flipkart.krystal.vajram.codegen.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.models.VajramInfoLite;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.FacetIdNameMapping;
import com.flipkart.krystal.vajram.facets.FacetValidation;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.One2OneCommand;
import com.flipkart.krystal.vajram.facets.Using;
import com.flipkart.krystal.vajram.facets.resolution.AbstractFanoutInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.AbstractOne2OneInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.FanoutInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.resolution.One2OneInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.sdk.Resolve;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.flipkart.krystal.vajram.facets.specs.MandatoryFacetDefaultSpec;
import com.flipkart.krystal.vajram.facets.specs.MandatoryFanoutDepSpec;
import com.flipkart.krystal.vajram.facets.specs.MandatoryOne2OneDepSpec;
import com.flipkart.krystal.vajram.facets.specs.OptionalFacetDefaultSpec;
import com.flipkart.krystal.vajram.facets.specs.OptionalFanoutDepSpec;
import com.flipkart.krystal.vajram.facets.specs.OptionalOne2OneDepSpec;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public class VajramCodeGenerator {

  @Getter private final String packageName;
  private final VajramInfo vajramInfo;
  @Getter private final String vajramName;
  private final Map<VajramID, VajramInfoLite> vajramDefs;
  private final Map<Integer, FacetGenModel> facetModels;
  private final Map<String, FacetGenModel> facetModelsByName;
  private final boolean needsBatching;
  private final Map<Integer, Boolean> depFanoutMap;
  private @MonotonicNonNull ParsedVajramData parsedVajramData;
  private final Utils util;

  public VajramCodeGenerator(VajramInfo vajramInfo, Utils util) {
    this.vajramInfo = vajramInfo;
    this.vajramName = vajramInfo.vajramId().vajramId();
    this.packageName = vajramInfo.packageName();
    this.util = util;
    // All parsed Vajram data loaded from all Vajram class files with vajram name as key
    this.vajramDefs =
        vajramInfo.dependencies().stream()
            .collect(
                toMap(
                    dependencyModel -> dependencyModel.depVajramInfoLite().vajramId(),
                    dep -> dep.depVajramInfoLite(),
                    (o1, o2) -> o1));

    // All the present Vajram -> VajramFacetDefinitions map with name as key
    this.facetModels =
        vajramInfo
            .facetStream()
            .collect(
                toMap(
                    FacetGenModel::id,
                    Function.identity(),
                    (o1, o2) -> o1,
                    () ->
                        new LinkedHashMap<
                            Integer, FacetGenModel>())); // need ordered map for dependencies
    this.facetModelsByName =
        vajramInfo
            .facetStream()
            .collect(
                toMap(
                    FacetGenModel::name,
                    Function.identity(),
                    (o1, o2) -> o1,
                    () ->
                        new LinkedHashMap<
                            String, FacetGenModel>())); // need ordered map for dependencies
    this.needsBatching = vajramInfo.givenFacets().stream().anyMatch(GivenFacetModel::isBatched);
    depFanoutMap =
        vajramInfo.dependencies().stream()
            .collect(toMap(DependencyModel::id, DependencyModel::canFanout));
  }

  public int inferFacetId(VariableElement parameter) {
    Using using = parameter.getAnnotation(Using.class);
    if (using != null) {
      return using.value();
    }
    String paramName = parameter.getSimpleName().toString();
    FacetGenModel facetGenModel = facetModelsByName.get(paramName);
    if (facetGenModel == null) {
      String message = "Unknown facet with name %s".formatted(paramName);
      util.error(message, parameter);
      throw new VajramValidationException(message);
    }
    return facetGenModel.id();
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
                        ParameterizedTypeName.get(ImmutableList.class, Facet.class)
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
      int dependencyId = checkNotNull(resolver.getAnnotation(Resolve.class)).dep();
      resolverMap.computeIfAbsent(dependencyId, _k -> new ArrayList<>()).add(resolver);
    }

    // Initialize few common attributes and data structures
    final ClassName batchFacetsClassName = getBatchFacetsClassName();
    final ClassName commonFacetsClassName = getCommonFacetsClassName();
    final TypeName vajramResponseType =
        util.toTypeName(getParsedVajramData().vajramInfo().responseType());

    //    methodSpecs.add(createFacetDefinitions());
    methodSpecs.add(createInputResolvers());
    //    createResolvers(resolverMap, depFanoutMap).ifPresent(methodSpecs::add);

    if (util.isRawAssignable(
        getParsedVajramData().vajramInfo().vajramClass().asType(), IOVajram.class)) {
      methodSpecs.add(
          ioVajramExecuteMethod(
              batchFacetsClassName,
              commonFacetsClassName,
              vajramResponseType.box().annotated(AnnotationSpec.builder(Nullable.class).build())));
    } else {
      methodSpecs.add(computeVajramExecuteMethod(vajramResponseType));
    }

    ClassName requestInterfaceType = getRequestInterfaceType();
    ClassName immutRequestType = ClassName.get(packageName, getImmutRequestPojoName(vajramName));
    ClassName immutFacetsType = ClassName.get(packageName, getImmutFacetsClassname(vajramName));

    StringWriter writer = new StringWriter();
    try {
      JavaFile.builder(
              packageName,
              vajramImplClass
                  .addMethods(methodSpecs)
                  .addMethod(
                      MethodSpec.overriding(
                              util.getMethodToOverride(Vajram.class, "newRequestBuilder", 0))
                          .returns(immutRequestType.nestedClass("Builder"))
                          .addModifiers(PUBLIC)
                          .addStatement("return $T._builder()", immutRequestType)
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
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return writer.toString();
  }

  private MethodSpec createInputResolvers() {
    MethodSpec.Builder getInputResolversMethod =
        methodBuilder(GET_INPUT_RESOLVERS)
            .addModifiers(PUBLIC)
            .returns(ParameterizedTypeName.get(ImmutableCollection.class, InputResolver.class))
            .addAnnotation(Override.class);
    CodeBlock.Builder resolveMethodToObjConvCode = CodeBlock.builder();
    getInputResolversMethod.addCode(
        "return $T.<$T>builder().addAll($L())",
        ImmutableList.class,
        InputResolver.class,
        GET_SIMPLE_INPUT_RESOLVERS);

    Map<Integer, List<ExecutableElement>> resolverMap = new HashMap<>();

    for (ExecutableElement resolver : getParsedVajramData().resolvers()) {
      resolverMap
          .computeIfAbsent(resolver.getAnnotation(Resolve.class).dep(), _k -> new ArrayList<>())
          .add(resolver);
    }
    resolverMap.forEach(
        (depName, resolverMethods) -> {
          @SuppressWarnings("method.invocation")
          DependencyModel dep =
              vajramInfo.dependencies().stream()
                  .filter(d -> d.id() == depName)
                  .findAny()
                  .orElseThrow();

          for (ExecutableElement resolverMethod : resolverMethods) {
            Resolve resolve = resolverMethod.getAnnotation(Resolve.class);
            if (resolve == null) {
              throw new AssertionError("Cannot happen");
            }
            boolean fanoutResolver =
                util.isRawAssignable(resolverMethod.getReturnType(), FanoutCommand.class);

            List<String> resolvedInputNames =
                stream(resolve.depInputs())
                    .mapToObj(
                        value ->
                            checkNotNull(dep.depVajramInfoLite().facetIdNameMapping().get(value)))
                    .toList();

            Class<?> inputResolverInterfaceClass;
            if (fanoutResolver) {
              inputResolverInterfaceClass = FanoutInputResolver.class;
            } else {
              inputResolverInterfaceClass = One2OneInputResolver.class;
            }
            MethodSpec.Builder resolveMethod =
                MethodSpec.overriding(
                        util.getMethodToOverride(inputResolverInterfaceClass, "resolve", 2),
                        (DeclaredType)
                            checkNotNull(
                                    util.processingEnv()
                                        .getElementUtils()
                                        .getTypeElement(
                                            checkNotNull(
                                                inputResolverInterfaceClass.getCanonicalName())))
                                .asType(),
                        util.processingEnv().getTypeUtils())
                    .addCode(buildInputResolver(resolverMethod, fanoutResolver).build());
            List<FacetGenModel> usedFacets =
                resolverMethod.getParameters().stream()
                    .map(this::inferFacetId)
                    .map(key -> checkNotNull(facetModels.get(key)))
                    .toList();
            resolveMethodToObjConvCode.add(
                ".add($L)",
                anonymousClassBuilder(
                        "$T.of($L), new $T($T.$L, $T.of($L))",
                        ImmutableSet.class,
                        CodeBlock.of(
                            usedFacets.stream().map(_f -> "$T.$L").collect(joining(",")),
                            usedFacets.stream()
                                .flatMap(
                                    facet ->
                                        Stream.of(
                                            getFacetsInterfaceType(),
                                            facet.name() + FACET_SPEC_SUFFIX))
                                .toArray()),
                        ResolutionTarget.class,
                        getFacetsInterfaceType(),
                        dep.name() + FACET_SPEC_SUFFIX,
                        ImmutableSet.class,
                        CodeBlock.of(
                            resolvedInputNames.stream().map(_f -> "$T.$L").collect(joining(",")),
                            resolvedInputNames.stream()
                                .flatMap(
                                    inputName ->
                                        Stream.of(
                                            ClassName.get(
                                                dep.depReqPackageName(),
                                                getRequestInterfaceName(
                                                    dep.depVajramInfoLite().vajramId().vajramId())),
                                            inputName + FACET_SPEC_SUFFIX))
                                .toArray()))
                    .addSuperinterface(
                        fanoutResolver
                            ? AbstractFanoutInputResolver.class
                            : AbstractOne2OneInputResolver.class)
                    .addMethod(resolveMethod.build())
                    .build());
          }
        });
    resolveMethodToObjConvCode.add(".build()");

    return getInputResolversMethod.addStatement(resolveMethodToObjConvCode.build()).build();
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

  /**
   * Method to generate "executeCompute" function code for ComputeVajrams Supported DataAccessSpec
   * => VajramID only.
   *
   * @param vajramResponseType Vajram response type
   * @return generated code for "executeCompute" {@link MethodSpec}
   */
  private MethodSpec computeVajramExecuteMethod(TypeName vajramResponseType) {

    MethodSpec.Builder executeBuilder =
        methodBuilder(METHOD_EXECUTE_COMPUTE)
            .addModifiers(PUBLIC)
            .addParameter(
                ParameterizedTypeName.get(ImmutableList.class, FacetValues.class), FACETS_LIST)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(ImmutableMap.class),
                    ClassName.get(FacetValues.class),
                    ParameterizedTypeName.get(
                        ClassName.get(Errable.class), vajramResponseType.box())))
            .addAnnotation(Override.class);
    if (needsBatching) {
      String message = "Batching is not supported in ComputeVajrams";
      util.error(
          message,
          getParsedVajramData().vajramInfo().givenFacets().stream()
              .filter(GivenFacetModel::isBatched)
              .findAny()
              .<Element>map(GivenFacetModel::facetField)
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
                return _facetValuesList.stream().collect(
                     $T.toImmutableMap($T.identity(),
                     $L -> {
                        $T $L = ($T)$L;
                """,
                ImmutableMap.class,
                Function.class,
                INCOMING_FACETS,
                getFacetsInterfaceType(),
                FACETVALUES_VAR,
                getFacetsInterfaceType(),
                INCOMING_FACETS);
    ExecutableElement outputLogic = getParsedVajramData().outputLogic();

    outputLogic
        .getParameters()
        .forEach(
            parameter -> {
              generateFacetLocalVariable(outputLogic, parameter, returnBuilder, FACETVALUES_VAR);
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

      returnBuilder.add("\nreturn $L(\n", getParsedVajramData().outputLogic().getSimpleName());
      methodCallSuffix = ");\n";
    } else {
      returnBuilder.add(
          "\nreturn $T.errableFrom(() -> $L(\n",
          Errable.class,
          getParsedVajramData().outputLogic().getSimpleName());
      methodCallSuffix = "));\n";
    }

    List<String> outputLogicParamsCode = new ArrayList<>();
    List<Object> outputLogicParamsCodeArgs = new ArrayList<>();
    for (VariableElement param : outputLogic.getParameters()) {
      generateLogicParams(param, outputLogicParamsCode, outputLogicParamsCodeArgs);
    }
    returnBuilder.add(
        outputLogicParamsCode.stream().collect(joining(", ")), outputLogicParamsCodeArgs.toArray());

    returnBuilder.add(methodCallSuffix);
    returnBuilder.add("}));\n");
    executeBuilder.addCode(returnBuilder.build());
  }

  private void generateLogicParams(
      VariableElement param,
      List<String> outputLogicParamsCode,
      List<Object> outputLogicParamsCodeArgs) {
    int facetId = inferFacetId(param);
    FacetGenModel facetGenModel = facetModels.get(facetId);
    if (facetGenModel == null) {
      throw new AssertionError("Cannot happen");
    }
    if (facetGenModel.isBatched()) {
      String message =
          "Cannot use batch facet '%s' as direct input param for output logic".formatted(facetId);
      util.error(message, param);
      throw new VajramValidationException(message);
    } else {
      outputLogicParamsCode.add("$L");
      outputLogicParamsCodeArgs.add(facetGenModel.name());
    }
  }

  /**
   * Method to generate "execute" function code for IOVajrams
   *
   * @param batchableInputs Generated Vajramspecific InputUtil.BatchedInputs class
   * @param commonFacets Generated Vajram specific InputUtil.CommonInputs class
   * @param vajramResponseType Vajram response type
   * @return generated code for "execute" {@link MethodSpec}
   */
  private MethodSpec ioVajramExecuteMethod(
      ClassName batchableInputs, ClassName commonFacets, TypeName vajramResponseType) {

    MethodSpec.Builder executeMethodBuilder =
        methodBuilder(METHOD_EXECUTE)
            .addModifiers(PUBLIC)
            .addParameter(
                ParameterizedTypeName.get(ImmutableList.class, FacetValues.class), FACETS_LIST)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(ImmutableMap.class),
                    ClassName.get(FacetValues.class),
                    ParameterizedTypeName.get(
                        ClassName.get(CompletableFuture.class), vajramResponseType)))
            .addAnnotation(Override.class);

    CodeBlock.Builder codeBuilder = CodeBlock.builder();
    if (needsBatching)
      batchedExecuteMethodBuilder(
          batchableInputs, commonFacets, vajramResponseType, codeBuilder, executeMethodBuilder);
    else {
      nonBatchedExecuteMethodBuilder(executeMethodBuilder, true);
    }
    return executeMethodBuilder.build();
  }

  private void batchedExecuteMethodBuilder(
      ClassName batchableInputs,
      ClassName commonFacets,
      TypeName vajramResponseType,
      CodeBlock.Builder codeBuilder,
      MethodSpec.Builder executeMethodBuilder) {
    ExecutableElement outputLogic = getParsedVajramData().outputLogic();

    TypeMirror returnType = outputLogic.getReturnType();
    checkState(
        util.isRawAssignable(util.processingEnv().getTypeUtils().erasure(returnType), Map.class),
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
    String batchingExecutePrepareParams =
        """
            if($facetValuesList:L.isEmpty()) {
              return $imMap:T.of();
            }
            $map:T<$inputBatching:T, $facetValues:T> _batches = new $hashMap:T<>();
            $commonInput:T _common = (($unmodInput:T)$facetValuesList:L.get(0))._common();
            for ($facetValues:T $facetsVar:L : $facetValuesList:L) {
              $unmodInput:T _castFacets = ($unmodInput:T) $facetsVar:L;
              $inputBatching:T _batch = _castFacets._batchElement();
              _batches.put(_batch, $facetsVar:L);
            }
        """;
    Map<String, Object> valueMap = new HashMap<>();
    valueMap.put("facetValues", ClassName.get(FacetValues.class));
    valueMap.put("facetValuesList", FACETS_LIST);
    valueMap.put("facetsVar", FACETVALUES_VAR);
    valueMap.put("unmodInput", ClassName.get(packageName, getFacetsInterfaceName(vajramName)));
    valueMap.put("inputBatching", batchableInputs);
    valueMap.put("commonInput", commonFacets);
    valueMap.put("facetJavaType", vajramResponseType);
    valueMap.put("outputLogicMethod", outputLogic.getSimpleName());
    valueMap.put("modInput", ClassName.get(BatchedFacets.class));
    valueMap.put("imMap", ClassName.get(ImmutableMap.class));
    valueMap.put("imList", ClassName.get(ImmutableList.class));
    valueMap.put("hashMap", ClassName.get(HashMap.class));
    valueMap.put("arrayList", ClassName.get(ArrayList.class));
    valueMap.put("comFuture", ClassName.get(CompletableFuture.class));
    valueMap.put("linkHashMap", ClassName.get(LinkedHashMap.class));
    valueMap.put("map", ClassName.get(Map.class));
    valueMap.put("list", ClassName.get(List.class));
    valueMap.put("valErr", Errable.class);
    valueMap.put("function", ClassName.get(Function.class));
    valueMap.put("optional", ClassName.get(Optional.class));
    codeBuilder.addNamed(batchingExecutePrepareParams, valueMap);
    for (VariableElement param : outputLogic.getParameters()) {
      if (param.getSimpleName().contentEquals(BATCHES_VAR)) {
        TypeMirror paramType = param.asType();
        List<? extends TypeMirror> batchTypeParams = getTypeParameters(paramType);
        TypeMirror expected =
            checkNotNull(
                    util.processingEnv()
                        .getElementUtils()
                        .getTypeElement(getBatchFacetsClassName().canonicalName()))
                .asType();
        if (!util.isRawAssignable(paramType, ImmutableList.class)
            || batchTypeParams.size() != 1
            || !util.processingEnv().getTypeUtils().isSameType(expected, batchTypeParams.get(0))) {
          throw util.errorAndThrow(
              "Batch of facetValues param must be of ImmutableList<"
                  + expected
                  + "> . Found: "
                  + paramType,
              param);
        }
      } else {
        generateFacetLocalVariable(outputLogic, param, codeBuilder, "_common");
      }
    }

    codeBuilder.add("var _output = $L(", outputLogic.getSimpleName());
    List<String> outputLogicParamsCode = new ArrayList<>();
    List<Object> outputLogicParamsCodeArgs = new ArrayList<>();
    for (VariableElement param : outputLogic.getParameters()) {
      if (param.getSimpleName().contentEquals(BATCHES_VAR)) {
        outputLogicParamsCode.add("$T.copyOf($L.keySet())");
        outputLogicParamsCodeArgs.add(ImmutableList.class);
        outputLogicParamsCodeArgs.add(BATCHES_VAR);
      } else {
        generateLogicParams(param, outputLogicParamsCode, outputLogicParamsCodeArgs);
      }
    }
    codeBuilder.add(
        outputLogicParamsCode.stream().collect(joining(", ")), outputLogicParamsCodeArgs.toArray());
    codeBuilder.add(");");
    codeBuilder.addNamed(
        """
        $map:T<$facetValues:T, $comFuture:T<$facetJavaType:T>> _returnValue = new $linkHashMap:T<>();

        _output.forEach((_batch, _result) -> _returnValue.put(
              $optional:T.ofNullable(_batches.get(_batch)).orElseThrow(),
              _result.<$facetJavaType:T>thenApply($function:T.identity())));
        return $imMap:T.copyOf(_returnValue);
    """,
        valueMap);
    executeMethodBuilder.addCode(codeBuilder.build());
  }

  /**
   * Method to generate resolver code for facetDef binding
   *
   * @return {@link CodeBlock.Builder} with resolver code
   */
  private CodeBlock.Builder buildInputResolver(ExecutableElement method, boolean fanoutResolver) {
    Resolve resolve =
        checkNotNull(
            method.getAnnotation(Resolve.class), "Resolver method must have 'Resolve' annotation");
    FacetGenModel facetGenModel = checkNotNull(facetModels.get(resolve.dep()));

    if (!(facetGenModel instanceof DependencyModel dependencyModel)) {
      String message = "No facet definition found for " + facetGenModel.name();
      util.error(message, method);
      throw new VajramValidationException(message);
    }
    VajramID depVajramName = dependencyModel.depVajramInfoLite().vajramId();
    String depRequestPackage = dependencyModel.depReqPackageName();

    // check if the facetDef is satisfied by facetDef or other resolved variables
    ClassName requestBuilderType =
        ClassName.get(depRequestPackage, getImmutRequestInterfaceName(depVajramName.vajramId()))
            .nestedClass("Builder");
    CodeBlock.Builder codeBuilder;
    if (fanoutResolver) {
      codeBuilder =
          CodeBlock.builder()
              .addStatement("var $2L = (($1T)arg0)", requestBuilderType, RESOLVER_REQUEST);
    } else {
      codeBuilder =
          CodeBlock.builder()
              .addStatement(
                  "var $2L = (($1T)arg0)",
                  ParameterizedTypeName.get(ClassName.get(ImmutableList.class), requestBuilderType),
                  RESOLVER_REQUESTS);
    }
    codeBuilder.addStatement("var $L = (($T)arg1)", FACETVALUES_VAR, getFacetsInterfaceType());
    // TODO : add validation if fanout, then method should accept dependencyDef response for the
    // bind type parameter else error
    // Iterate over the method params and call respective binding methods
    method
        .getParameters()
        .forEach(
            parameter -> {
              generateFacetLocalVariable(method, parameter, codeBuilder, FACETVALUES_VAR);
            });

    buildResolverInvocation(
        codeBuilder,
        method,
        dependencyModel,
        resolve.depInputs(),
        fanoutResolver,
        depVajramName,
        requestBuilderType);
    return codeBuilder;
  }

  private void generateFacetLocalVariable(
      ExecutableElement method,
      VariableElement parameter,
      CodeBlock.Builder codeBuilder,
      String facetsVar) {
    final int usingFacetId = inferFacetId(parameter);
    // check if the bind param has multiple resolvers
    FacetGenModel usingFacetModel = checkNotNull(facetModels.get(usingFacetId));
    if (usingFacetModel instanceof DependencyModel) {
      generateDependencyResolutions(method, usingFacetId, codeBuilder, parameter);
    } else if (usingFacetModel instanceof GivenFacetModel givenFacetModel) {
      TypeMirror facetType = util.toTypeMirror(givenFacetModel.dataType());
      String variable = usingFacetModel.name();
      TypeMirror parameterTypeMirror = parameter.asType();
      final TypeName parameterType = TypeName.get(parameterTypeMirror);
      if (givenFacetModel.isMandatory()) {
        if (!util.processingEnv().getTypeUtils().isAssignable(facetType, parameterTypeMirror)) {
          String message =
              String.format(
                  "Incorrect facet type being consumed. Expected '%s', found '%s'"
                      .formatted(facetType, parameterType),
                  usingFacetModel.name());
          util.error(message, parameter);
          throw new VajramValidationException(message);
        }
        codeBuilder.add(
            CodeBlock.builder()
                .addStatement(
                    "var $L = $T.validateMandatoryFacet($L.$L(), $S, $S)",
                    variable,
                    FacetValidation.class,
                    facetsVar,
                    usingFacetModel.name(),
                    vajramInfo.vajramId().vajramId(),
                    usingFacetModel.name())
                .build());
      } else if (util.isRawAssignable(parameterTypeMirror, Optional.class)) {
        codeBuilder.add(
            CodeBlock.builder()
                .addStatement(
                    "var $L = $T.ofNullable($L.$L())",
                    variable,
                    Optional.class,
                    facetsVar,
                    usingFacetModel.name())
                .build());
      } else {
        String message =
            String.format(
                "Optional dependency %s must have type as Optional", usingFacetModel.name());
        util.error(message, parameter);
        throw new VajramValidationException(message);
      }
    } else {
      String message = "No facetDef resolver found for " + usingFacetModel.name();
      util.error(message, parameter);
      throw new VajramValidationException(message);
    }
  }

  /**
   * Method to generate resolver code for dependencyDef bindings
   *
   * @param method Dependency resolver method
   * @param usingFacetId The bind param name in the resolver method
   * @param ifBlockBuilder The {@link CodeBlock.Builder}
   * @param parameter the bind parameter in the resolver method
   */
  private void generateDependencyResolutions(
      ExecutableElement method,
      final int usingFacetId,
      CodeBlock.Builder ifBlockBuilder,
      VariableElement parameter) {
    FacetGenModel usingFacetDef = checkNotNull(facetModels.get(usingFacetId));

    TypeMirror parameterType = parameter.asType();
    String usingFacetName = usingFacetDef.name();
    //    ReturnType returnType
    if (usingFacetDef instanceof DependencyModel dependencyModel) {
      String variableName = usingFacetName;
      final VajramInfoLite depVajramInfoLite = dependencyModel.depVajramInfoLite();
      String requestClass = dependencyModel.depReqClassQualifiedName();
      TypeName boxedDepType = util.toTypeName(depVajramInfoLite.responseType()).box();
      TypeName unboxedDepType =
          boxedDepType.isBoxedPrimitive() ? boxedDepType.unbox() : boxedDepType;
      String logicName = method.getSimpleName().toString();
      if (depFanoutMap.getOrDefault(usingFacetId, false)) {
        // fanout case
        boolean typeMismatch = false;
        if (!util.isRawAssignable(parameterType, FanoutDepResponses.class)) {
          typeMismatch = true;
        } else if (getTypeParameters(parameterType).size() != 2) {
          typeMismatch = true;
        }
        if (typeMismatch) {
          // the parameter data type must be RequestResponse<Req, Resp>
          String message =
              """
                The fanout dependency ('%s') can be consumed only via the DependencyResponse<ReqType,RespType> class. \
                Found '%s' instead"""
                  .formatted(dependencyModel.name(), parameterType);
          util.error(message, parameter);
          throw new VajramValidationException(message);
        }
        String depValueAccessorCode = "$T $L = $L.$L()";
        ifBlockBuilder.addStatement(
            depValueAccessorCode,
            ParameterizedTypeName.get(
                ClassName.get(FanoutDepResponses.class),
                Utils.toClassName(requestClass),
                boxedDepType),
            variableName,
            FACETVALUES_VAR,
            usingFacetName);
      } else {
        // This means the dependency being consumed used is not a fanout dependency
        String depValueAccessorCode = """
              $1T $2L =
                $3L.$4L()""";
        if (usingFacetDef.isMandatory()) {
          if (unboxedDepType.equals(TypeName.get(parameterType))) {
            // This means this dependencyDef being consumed is a non fanout mandatory dependency and
            // the dev has requested the value directly. So we extract the only value from
            // dependencyDef response and provide it.
            ifBlockBuilder.addStatement(
                depValueAccessorCode + ".response().valueOrThrow()",
                unboxedDepType,
                variableName,
                FACETVALUES_VAR,
                usingFacetName);
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
                depValueAccessorCode + ".response()",
                ParameterizedTypeName.get(ClassName.get(Errable.class), boxedDepType),
                variableName,
                FACETVALUES_VAR,
                usingFacetName);
          } else if (util.isRawAssignable(parameterType, Optional.class)) {
            // This means this dependencyDef in "Using" annotation is not a fanout and the dev has
            // requested an 'Optional'. So we retrieve the only Errable from the dependencyDef
            // response, extract the optional and provide it.
            String code = depValueAccessorCode + ".response().valueOpt()";
            ifBlockBuilder.addStatement(
                code,
                ParameterizedTypeName.get(ClassName.get(Optional.class), boxedDepType),
                variableName,
                FACETVALUES_VAR,
                usingFacetName);
          } else if (util.isRawAssignable(parameterType, One2OneDepResponse.class)) {
            // This means this dependencyDef in "Using" annotation is not a fanout and the dev has
            // requested an 'Optional'. So we retrieve the only Errable from the dependencyDef
            // response, extract the optional and provide it.
            ifBlockBuilder.addStatement(
                depValueAccessorCode,
                ParameterizedTypeName.get(ClassName.get(Optional.class), boxedDepType),
                variableName,
                FACETVALUES_VAR,
                usingFacetName);
          } else {
            String message =
                ("A resolver ('%s') must not access an optional dependency ('%s') directly."
                        + "Use Optional<>, Errable<>, or DependencyResponse<> instead")
                    .formatted(logicName, usingFacetName);
            util.error(message, parameter);
            throw new VajramValidationException(message);
          }
        }
      }
    }
  }

  /**
   * Method to generate resolver code for variables having single resolver.
   *
   * <p>Fanout case
   *
   * <p>- MultiRequest of normal type => fanout loop and create facetValues
   *
   * <p>- MultiRequest of Vajram Request - DependencyCommand.MultiExecute<NormalType>
   *
   * <p>Non - fanout
   *
   * <p>- Normal datatype - Vajram Request => toInputValues() - DependencyCommand.executeWith
   *
   * @param methodCodeBuilder {@link Builder}
   * @param resolverMethod Resolve method
   * @param depFacetIds Resolve facetValues
   */
  private void buildResolverInvocation(
      CodeBlock.Builder methodCodeBuilder,
      ExecutableElement resolverMethod,
      DependencyModel dependencyModel,
      int[] depFacetIds,
      boolean fanoutResolver,
      VajramID depVajramId,
      ClassName requestBuilderType) {

    /*
    TODO In the following code, we are assuming that if the
     resolver method is returning SingleExecute<T>, MultiExecute<T>, or just T,
     the T is exactly matching the resolved inputs data type. If the developer makes an error,
     then the generated code will fail at runtime with ClassCastException. We need to add a validation
     here which proactively fails if the data type mismatches.
    */
    String resolverResultVar = fanoutResolver ? RESOLVER_RESULTS : RESOLVER_RESULT;
    methodCodeBuilder.add("var $L = $L(", resolverResultVar, resolverMethod.getSimpleName());
    ImmutableList<Integer> resolverSources = getResolverSources(resolverMethod).asList();
    for (int i = 0; i < resolverSources.size(); i++) {
      int facetId = resolverSources.get(i);
      String facetName = checkNotNull(facetModels.get(facetId)).name();
      methodCodeBuilder.add("$L", facetName);
      if (i != resolverMethod.getParameters().size() - 1) {
        methodCodeBuilder.add(", ");
      }
    }
    methodCodeBuilder.add(");\n");

    TypeMirror returnType = util.box(resolverMethod.getReturnType());
    boolean resovlerReturnedDepCommand = false;
    if (util.isRawAssignable(returnType, DependencyCommand.class)) {
      resovlerReturnedDepCommand = true;
      methodCodeBuilder.beginControlFlow("if($L.shouldSkip())", resolverResultVar);
      methodCodeBuilder.addStatement(
          "\t return $T.skip($L.doc(), $L.skipCause())",
          ResolverCommand.class,
          resolverResultVar,
          resolverResultVar);
      methodCodeBuilder.add("} else {\n\t");

      TypeMirror actualReturnType = getTypeParameters(returnType).get(0);
      if (util.isRawAssignable(returnType, One2OneCommand.class)) {
        methodCodeBuilder.beginControlFlow(
            "for($T $L: $L)", requestBuilderType, RESOLVER_REQUEST, RESOLVER_REQUESTS);
        if (util.isRawAssignable(actualReturnType, Request.class)) {
          for (int depFacetId : depFacetIds) {
            String depInputName =
                checkNotNull(
                    checkNotNull(vajramDefs.get(depVajramId)).facetIdNameMapping().get(depFacetId));
            methodCodeBuilder.addStatement(
                "$L.ifPresent(_r -> $L.$L(_r.$L()))",
                RESOLVER_RESULT,
                RESOLVER_REQUEST,
                depInputName,
                depInputName);
          }
        } else {
          if (depFacetIds.length != 1) {
            throw util.errorAndThrow(
                "Resolver method which resolves multiple dependency inputs must return a Request Builder object",
                resolverMethod);
          }
          String depFacetName =
              checkNotNull(
                  checkNotNull(vajramDefs.get(depVajramId))
                      .facetIdNameMapping()
                      .get(depFacetIds[0]));
          methodCodeBuilder.addStatement(
              "$L.ifPresent($L::$L)", RESOLVER_RESULT, RESOLVER_REQUEST, depFacetName);
        }
        methodCodeBuilder.endControlFlow();
      } else if (util.isRawAssignable(returnType, FanoutCommand.class)) {
        // TODO : add missing validations if any (??)
        if (!dependencyModel.canFanout()) {
          String message =
              """
              Dependency '%s' is not a fanout dependency, yet the resolver method returns a MultiExecute command.\
               This is not allowed. Return a SingleExecute command, a single value, or mark the dependency as `canFanout = true`."""
                  .formatted(dependencyModel.name());
          util.error(message, resolverMethod);
          throw new VajramValidationException(message);
        }
        methodCodeBuilder.addStatement(
            "var $L = new $T()",
            RESOLVER_REQUESTS,
            ParameterizedTypeName.get(ClassName.get(ArrayList.class), requestBuilderType));
        if (util.isRawAssignable(actualReturnType, ImmutableRequest.Builder.class)) {
          if (depFacetIds.length <= 1) {
            throw util.errorAndThrow(
                "Resolver method that returns a request builder object must resolve multiple dependency inputs. Otherwise this can lead to unnecessary creation of request builder objects.",
                resolverMethod);
          }
        }
        methodCodeBuilder.beginControlFlow(
            "for($T $L: $L.inputs())", actualReturnType, RESOLVER_RESULT, RESOLVER_RESULTS);
        if (util.isRawAssignable(actualReturnType, ImmutableRequest.Builder.class)) {
          /*
          TODO: Add validation that this vajram request is of the same type as the request of the dependency Vajram
          */
          methodCodeBuilder.add("$L.add($L._newCopy()", RESOLVER_REQUESTS, RESOLVER_REQUEST);
          for (int depFacetId : depFacetIds) {
            String depFacetName =
                checkNotNull(
                    checkNotNull(vajramDefs.get(depVajramId)).facetIdNameMapping().get(depFacetId));
            methodCodeBuilder.add(".$L($L.$L())", depFacetName, RESOLVER_RESULT, depFacetName);
          }
          methodCodeBuilder.add(");");
        } else {
          // TODO : Add validation for the type parameter of the MultiExecute to match the type of
          // the input being resolved
          if (depFacetIds.length > 1) {
            throw util.errorAndThrow(
                "Resolver method which resolves multiple dependency inputs must return a %s object"
                    .formatted(ImmutableRequest.Builder.class),
                resolverMethod);
          }
          // Here we are assuming that the method is returning an MultiExecute of the type of the
          // input being resolved. If this assumption is incorrect, the generated wrapper class will
          // fail compilation.
          String depFacetName =
              checkNotNull(
                  checkNotNull(vajramDefs.get(depVajramId))
                      .facetIdNameMapping()
                      .get(depFacetIds[0]));
          methodCodeBuilder.addStatement(
              "$L.add($L._newCopy().$L($L))",
              RESOLVER_REQUESTS,
              RESOLVER_REQUEST,
              depFacetName,
              RESOLVER_RESULT);
        }
        methodCodeBuilder.endControlFlow();
      }
    } else if (util.isRawAssignable(returnType, Request.class)) {
      methodCodeBuilder.beginControlFlow(
          "for($T $L: $L)", requestBuilderType, RESOLVER_REQUEST, RESOLVER_REQUESTS);
      for (int depFacetId : depFacetIds) {
        String depFacetName =
            checkNotNull(
                checkNotNull(vajramDefs.get(depVajramId)).facetIdNameMapping().get(depFacetId));
        methodCodeBuilder.addStatement(
            "$L.$L($L.$L())", RESOLVER_REQUEST, depFacetName, RESOLVER_RESULT, depFacetName);
      }
      methodCodeBuilder.endControlFlow();
    } else {
      methodCodeBuilder.beginControlFlow(
          "for($T $L: $L)", requestBuilderType, RESOLVER_REQUEST, RESOLVER_REQUESTS);
      if (depFacetIds.length != 1) {
        throw util.errorAndThrow(
            "Resolver method which resolves multiple dependency inputs must return a Request object",
            resolverMethod);
      }
      String depFacetName =
          checkNotNull(
              checkNotNull(vajramDefs.get(depVajramId)).facetIdNameMapping().get(depFacetIds[0]));
      methodCodeBuilder.addStatement(
          """
            $L.$L($L)
            """,
          RESOLVER_REQUEST,
          depFacetName,
          RESOLVER_RESULT);
      methodCodeBuilder.endControlFlow();
    }

    if (fanoutResolver) {
      methodCodeBuilder.addStatement(
          "return $T.executeWithRequests($T.copyOf($L))",
          ResolverCommand.class,
          ImmutableList.class,
          RESOLVER_REQUESTS);
    } else {
      methodCodeBuilder.addStatement(
          "return $T.executeWithRequests($L)", ResolverCommand.class, RESOLVER_REQUESTS);
    }
    if (resovlerReturnedDepCommand) {
      // close the else block of "if($L.shouldSkip())"
      methodCodeBuilder.endControlFlow();
    }
  }

  public void codeGenVajramRequest() {
    ImmutableList<GivenFacetModel> inputDefs = vajramInfo.givenFacets();
    ClassName requestInterfaceType = getRequestInterfaceType();
    ClassName immutReqInterfaceType =
        ClassName.get(packageName, getImmutRequestInterfaceName(vajramName));

    TypeName boxedVajramReturnType = util.toTypeName(vajramInfo.responseType()).box();
    TypeSpec.Builder requestInterface =
        util.interfaceBuilder(requestInterfaceType.simpleName())
            .addModifiers(PUBLIC)
            .addSuperinterface(
                ParameterizedTypeName.get(ClassName.get(Request.class), boxedVajramReturnType))
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "ClassReferencesSubclass")
                    .build());
    List<GivenFacetModel> inputs =
        vajramInfo.givenFacets().stream().filter(i -> i.facetTypes().contains(INPUT)).toList();

    addFacetConstants(requestInterface, inputs, CodeGenParams.builder().isRequest(true).build());
    createFacetMembers(
        requestInterface,
        requestInterfaceType,
        vajramInfo.facetStream().filter(facet -> facet.facetTypes().contains(INPUT)).toList(),
        CodeGenParams.builder().isRequest(true).build());

    TypeSpec.Builder immutReqInterface =
        util.interfaceBuilder(immutReqInterfaceType.simpleName())
            .addModifiers(PUBLIC)
            .addSuperinterface(requestInterfaceType)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(ImmutableRequest.class), boxedVajramReturnType));
    TypeSpec.Builder builderInterface =
        util.interfaceBuilder("Builder")
            .addModifiers(PUBLIC, STATIC)
            .addSuperinterface(requestInterfaceType)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(ImmutableRequest.Builder.class), boxedVajramReturnType));
    ClassName builderInterfaceType = immutReqInterfaceType.nestedClass("Builder");

    createFacetMembers(
        builderInterface,
        immutReqInterfaceType,
        vajramInfo.facetStream().filter(facet -> facet.facetTypes().contains(INPUT)).toList(),
        CodeGenParams.builder().isRequest(true).isBuilder(true).build());

    ClassName immutRequestPojoType =
        ClassName.get(packageName, getImmutRequestPojoName(vajramName));
    ClassName pojoRequestBuilderType = immutRequestPojoType.nestedClass("Builder");

    TypeSpec.Builder immutRequestPojo =
        util.classBuilder(immutRequestPojoType.simpleName())
            .addModifiers(PUBLIC, FINAL)
            .addSuperinterface(immutReqInterfaceType);

    createFacetMembers(
        immutRequestPojo,
        immutRequestPojoType,
        vajramInfo.facetStream().filter(facet -> facet.facetTypes().contains(INPUT)).toList(),
        CodeGenParams.builder().isRequest(true).withImpl(true).build());

    TypeSpec.Builder builderClass =
        util.classBuilder("Builder")
            .addModifiers(PUBLIC, STATIC, FINAL)
            .addSuperinterface(builderInterfaceType);
    createFacetMembers(
        builderClass,
        immutRequestPojoType,
        vajramInfo.facetStream().filter(facet -> facet.facetTypes().contains(INPUT)).toList(),
        CodeGenParams.builder().isRequest(true).isBuilder(true).withImpl(true).build());

    List<GivenFacetModel> clientProvidedInputs =
        inputDefs.stream().filter(facetDef -> facetDef.facetTypes().contains(INPUT)).toList();
    util.generateSourceFile(
        requestInterfaceType.canonicalName(),
        JavaFile.builder(
                packageName,
                requestInterface
                    .addMethods(
                        createFacetContainerMethods(
                            clientProvidedInputs,
                            requestInterfaceType,
                            immutReqInterfaceType,
                            CodeGenParams.builder().isRequest(true).build()))
                    .build())
            .build()
            .toString(),
        vajramInfo.vajramClass());

    util.generateSourceFile(
        immutReqInterfaceType.canonicalName(),
        JavaFile.builder(
                packageName,
                immutReqInterface
                    .addMethods(
                        createFacetContainerMethods(
                            clientProvidedInputs,
                            immutReqInterfaceType,
                            immutReqInterfaceType,
                            CodeGenParams.builder().isRequest(true).build()))
                    .addType(
                        builderInterface
                            .addMethods(
                                createFacetContainerMethods(
                                    clientProvidedInputs,
                                    immutReqInterfaceType,
                                    immutReqInterfaceType,
                                    CodeGenParams.builder()
                                        .isRequest(true)
                                        .isBuilder(true)
                                        .build()))
                            .build())
                    .build())
            .build()
            .toString(),
        vajramInfo.vajramClass());
    util.generateSourceFile(
        immutRequestPojoType.canonicalName(),
        JavaFile.builder(
                packageName,
                immutRequestPojo
                    .addMethods(
                        createFacetContainerMethods(
                            clientProvidedInputs,
                            immutRequestPojoType,
                            immutRequestPojoType,
                            CodeGenParams.builder().isRequest(true).withImpl(true).build()))
                    .addMethod(
                        methodBuilder("_builder")
                            .addModifiers(PUBLIC, STATIC)
                            .returns(pojoRequestBuilderType)
                            .addStatement("return new $T()", pojoRequestBuilderType)
                            .build())
                    .addType(
                        builderClass
                            .addMethods(
                                createFacetContainerMethods(
                                    clientProvidedInputs,
                                    immutRequestPojoType,
                                    immutRequestPojoType,
                                    CodeGenParams.builder()
                                        .isRequest(true)
                                        .isBuilder(true)
                                        .withImpl(true)
                                        .build()))
                            .build())
                    .build())
            .build()
            .toString(),
        vajramInfo.vajramClass());
  }

  private void createFacetMembers(
      TypeSpec.Builder classSpec,
      ClassName enclosingClassType,
      List<? extends FacetGenModel> eligibleFacets,
      CodeGenParams codeGenParams) {
    if (codeGenParams.wrapsRequest() && codeGenParams.isRequest()) {
      throw new IllegalArgumentException("A request cannot wrap another request - this is a bug");
    }

    // TODO : Add checks for subsetRequest
    MethodSpec.Builder fullConstructor = constructorBuilder();
    ClassName requestInterfacetType =
        ClassName.get(packageName, getImmutRequestPojoName(vajramName));
    ClassName immutRequestType =
        ClassName.get(packageName, getImmutRequestInterfaceName(vajramName));
    if (codeGenParams.withImpl()) {
      classSpec.addAnnotations(
          codeGenParams.isBuilder()
              ? List.of(
                  AnnotationSpec.builder(ToString.class)
                      .addMember("doNotUseGetters", "true")
                      .build())
              : Utils.recordAnnotations());
      if (codeGenParams.wrapsRequest()) {
        ClassName requestOrBuilderType =
            codeGenParams.isBuilder() ? immutRequestType.nestedClass("Builder") : immutRequestType;
        fullConstructor.addParameter(
            ParameterSpec.builder(requestOrBuilderType, "_request").build());
        fullConstructor.addStatement("this._request = _request");
        FieldSpec.Builder field =
            FieldSpec.builder(requestOrBuilderType, "_request", PRIVATE, FINAL);
        if (!codeGenParams.isBuilder()) {
          field.addModifiers(FINAL);
        }
        classSpec.addField(field.build());
      }
      if (codeGenParams.isFacetsSubset() && codeGenParams.isBuilder()) {
        MethodSpec.Builder requestConstructor = constructorBuilder();
        requestConstructor.addParameter(
            ParameterSpec.builder(requestInterfacetType, "_request")
                .addAnnotation(NonNull.class)
                .build());
        for (FacetGenModel facet : eligibleFacets) {
          if (facet.facetTypes().contains(INPUT)) {
            requestConstructor.addStatement(
                "this.$L = $T.$L.getFromRequest(_request)",
                facet.name(),
                getRequestInterfaceType(),
                facet.name() + FACET_SPEC_SUFFIX);
          }
        }
        classSpec.addMethod(requestConstructor.build());
      }
    }

    for (FacetGenModel facet : eligibleFacets) {
      FacetJavaType facetFieldType = util.getFacetFieldType(facet);
      ParameterSpec facetParam =
          ParameterSpec.builder(
                  facetFieldType
                      .javaTypeName(facet)
                      .annotated(annotations(facetFieldType.typeAnnotations(facet, codeGenParams))),
                  facet.name())
              .build();
      boolean isInput = facet.facetTypes().contains(INPUT);
      if (codeGenParams.withImpl()) {
        if ((isInput ? !codeGenParams.wrapsRequest() : true)) {
          FieldSpec.Builder facetField =
              FieldSpec.builder(
                  facetFieldType
                      .javaTypeName(facet)
                      .annotated(annotations(facetFieldType.typeAnnotations(facet, codeGenParams))),
                  facet.name(),
                  PRIVATE);
          if (codeGenParams.isBuilder()) {
            facetField.initializer(facetFieldType.fieldInitializer(facet));
          } else {
            facetField.addModifiers(FINAL);
          }
          classSpec.addField(facetField.build());
          fullConstructor.addParameter(facetParam);
          fullConstructor.addStatement("this.$L = $L", facet.name(), facet.name());
        }
      }
      // Getter
      createFacetGetter(classSpec, facet, codeGenParams);
      if (codeGenParams.isBuilder()) {
        MethodSpec.Builder setterBuilder =
            methodBuilder(facet.name())
                .returns(enclosingClassType.nestedClass("Builder"))
                .addModifiers(PUBLIC)
                .addParameter(facetParam);
        if (codeGenParams.withImpl()) {
          setterBuilder
              .addStatement(
                  isInput && codeGenParams.wrapsRequest()
                      ? CodeBlock.of("this._request.$L($L)", facet.name(), facet.name())
                      : CodeBlock.of("this.$L = $L", facet.name(), facet.name())) // Set value
              .addStatement("return this", facet.name()); // Return
        } else {
          setterBuilder.addModifiers(ABSTRACT);
        }
        classSpec.addMethod(
            // Setter
            // public Builder inputName(Type inputName){this.inputName = inputName; return this;}
            setterBuilder.build());
      }
    }
    if (codeGenParams.withImpl()) {
      classSpec.addMethod(fullConstructor.build());
    }

    if (codeGenParams.isBuilder()
        && codeGenParams.withImpl()
        && !fullConstructor.parameters.isEmpty()) {
      // Make sure there is always a no-arg constructor for the builder
      classSpec.addMethod(
          constructorBuilder()
              .addCode(
                  codeGenParams.wrapsRequest()
                      ? CodeBlock.builder()
                          .addStatement("this._request = $T._builder()", requestInterfacetType)
                          .build()
                      : CodeBlock.builder().build())
              .build());
    }
    if (codeGenParams.isBuilder()
        && codeGenParams.withImpl()
        && !(codeGenParams.isRequest() || codeGenParams.isFacetsSubset())) {
      // Make sure Builder always has a constructor which accepts only the request irrespective of
      // type of Facet class
      // See Vajram#facetsFromRequest
      classSpec.addMethod(
          constructorBuilder()
              .addParameter(ParameterSpec.builder(getRequestInterfaceType(), "_request").build())
              .addStatement("this._request = _request._asBuilder()")
              .build());
    }
  }

  private void createFacetGetter(
      TypeSpec.Builder clazz, FacetGenModel facet, CodeGenParams codeGenParams) {
    MethodSpec.Builder method;
    FacetJavaType fieldType = util.getFacetFieldType(facet);
    FacetJavaType returnType = util.getReturnType(facet, codeGenParams);
    // Non-dependency facet (Example: GivenFacet)
    method =
        methodBuilder(facet.name())
            .addModifiers(PUBLIC)
            .returns(
                facet instanceof DependencyModel dep
                    ? util.wrapWithFacetValueClass(dep)
                    : returnType
                        .javaTypeName(facet)
                        .annotated(annotations(returnType.typeAnnotations(facet, codeGenParams))));
    if (codeGenParams.withImpl()) {
      method.addStatement(fieldType.fieldGetterCode(facet, codeGenParams));
    } else {
      method.addModifiers(ABSTRACT);
    }
    clazz.addMethod(method.build());
  }

  private ImmutableList<MethodSpec> createFacetContainerMethods(
      List<? extends FacetGenModel> eligibleFacets,
      ClassName enclosingClassName,
      ClassName immutableClassName,
      CodeGenParams codeGenParams) {

    // TODO : add validations for subsetRequest

    List<MethodSpec.Builder> methodBuilders = new ArrayList<>();

    String constructorParamString =
        (codeGenParams.isRequest()
                ? eligibleFacets.stream().map(FacetGenModel::name)
                : codeGenParams.wrapsRequest()
                    ? Stream.concat(
                        Stream.of("%s"),
                        eligibleFacets.stream()
                            .filter(f -> !f.facetTypes().contains(INPUT))
                            .map(FacetGenModel::name))
                    : eligibleFacets.stream().map(FacetGenModel::name))
            .collect(joining(", "));
    {
      MethodSpec.Builder methodBuilder = methodBuilder("_build").returns(immutableClassName);
      if (codeGenParams.withImpl()) {
        methodBuilder.addStatement(
            codeGenParams.isBuilder()
                ? "return new %s(%s)"
                    .formatted(
                        immutableClassName.simpleName(),
                        constructorParamString.formatted(
                            codeGenParams.wrapsRequest()
                                ? "_request._build()"
                                : "_batchable._build(), _common._build()"))
                : "return this");
      } else {
        methodBuilder.addModifiers(ABSTRACT);
      }
      methodBuilders.add(methodBuilder);
    }

    {
      MethodSpec.Builder methodBuilder =
          methodBuilder("_asBuilder").returns(immutableClassName.nestedClass("Builder"));
      if (codeGenParams.withImpl()) {
        methodBuilder.addStatement(
            codeGenParams.isBuilder()
                ? "return this"
                : "return new Builder(%s)"
                    .formatted(
                        constructorParamString.formatted(
                            codeGenParams.wrapsRequest()
                                ? "_request._asBuilder()"
                                : "_batchable._asBuilder(), _common._asBuilder()")));
      } else {
        methodBuilder.addModifiers(ABSTRACT);
      }
      methodBuilders.add(methodBuilder);
    }

    {
      MethodSpec.Builder methodBuilder =
          methodBuilder("_newCopy")
              .returns(
                  codeGenParams.isBuilder()
                      ? enclosingClassName.nestedClass("Builder")
                      : enclosingClassName);
      if (codeGenParams.withImpl()) {
        methodBuilder.addStatement(
            codeGenParams.isBuilder()
                ? "return new Builder(%s)"
                    .formatted(
                        constructorParamString.formatted(
                            codeGenParams.wrapsRequest()
                                ? "_request._newCopy()"
                                : "_batchable._newCopy(), _common._newCopy()"))
                : "return this");
      } else {
        methodBuilder.addModifiers(ABSTRACT);
      }
      methodBuilders.add(methodBuilder);
    }

    ImmutableList.Builder<MethodSpec> list = ImmutableList.builder();
    for (MethodSpec.Builder b : methodBuilders) {
      if (b != null) {
        MethodSpec build = b.addModifiers(PUBLIC).addAnnotation(Override.class).build();
        list.add(build);
      }
    }
    return list.build();
  }

  public void codeGenFacets() {
    basicFacetsClasses();
    boolean doInputsNeedBatching = vajramInfo.facetStream().anyMatch(FacetGenModel::isBatched);
    if (doInputsNeedBatching) {
      batchFacetsClasses();
    }
  }

  private void basicFacetsClasses() {
    boolean doInputsNeedBatching = vajramInfo.facetStream().anyMatch(FacetGenModel::isBatched);

    List<FacetGenModel> allFacets = vajramInfo.facetStream().toList();

    ClassName facetsType = getFacetsInterfaceType();
    ClassName immutableFacetsType = ClassName.get(packageName, getImmutFacetsClassname(vajramName));

    TypeSpec.Builder facetsInterface =
        util.interfaceBuilder(getFacetsInterfaceName(vajramName))
            .addModifiers(PUBLIC)
            .addSuperinterface(
                doInputsNeedBatching ? BatchEnabledFacetValues.class : FacetValues.class)
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "ClassReferencesSubclass")
                    .build());
    addFacetConstants(
        facetsInterface, vajramInfo.facetStream().toList(), CodeGenParams.builder().build());
    allFacets.forEach(
        facet -> {
          createFacetGetter(
              facetsInterface, facet, CodeGenParams.builder().wrapsRequest(true).build());
        });

    TypeSpec.Builder immutFacetsClass =
        util.classBuilder(getImmutFacetsClassname(vajramName))
            .addModifiers(PUBLIC, FINAL)
            .addSuperinterface(facetsType)
            .addSuperinterface(
                doInputsNeedBatching
                    ? BatchEnabledImmutableFacetValues.class
                    : ImmutableFacetValues.class);
    createFacetMembers(
        immutFacetsClass,
        immutableFacetsType,
        allFacets,
        CodeGenParams.builder().wrapsRequest(true).withImpl(true).build());

    TypeSpec.Builder facetsBuilderClass =
        util.classBuilder("Builder")
            .addModifiers(PUBLIC, STATIC, FINAL)
            .addSuperinterface(facetsType)
            .addSuperinterface(
                doInputsNeedBatching
                    ? BatchEnabledImmutableFacetValues.Builder.class
                    : FacetValuesBuilder.class);
    createFacetMembers(
        facetsBuilderClass,
        immutableFacetsType,
        allFacets,
        CodeGenParams.builder().wrapsRequest(true).isBuilder(true).withImpl(true).build());
    if (doInputsNeedBatching) {
      ClassName batchImmutFacetsType = getBatchFacetsClassName();
      ClassName commonImmutFacetsType = getCommonFacetsClassName();
      codegenBatchableFacets(
          facetsInterface,
          batchImmutFacetsType,
          commonImmutFacetsType,
          CodeGenParams.builder().build());
      codegenBatchableFacets(
          immutFacetsClass,
          batchImmutFacetsType,
          commonImmutFacetsType,
          CodeGenParams.builder().withImpl(true).build());
      codegenBatchableFacets(
          facetsBuilderClass,
          batchImmutFacetsType,
          commonImmutFacetsType,
          CodeGenParams.builder().withImpl(true).build());
    }

    try {
      StringWriter writer = new StringWriter();
      JavaFile.builder(
              packageName,
              facetsInterface
                  .addMethod(
                      methodBuilder("_build")
                          .addModifiers(PUBLIC, ABSTRACT)
                          .returns(immutableFacetsType)
                          .addAnnotation(Override.class)
                          .build())
                  .addMethod(
                      methodBuilder("_asBuilder")
                          .addModifiers(PUBLIC, ABSTRACT)
                          .returns(immutableFacetsType.nestedClass("Builder"))
                          .addAnnotation(Override.class)
                          .build())
                  .build())
          .build()
          .writeTo(writer);
      util.generateSourceFile(
          packageName() + '.' + getFacetsInterfaceName(vajramName()),
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
                          immutableFacetsType,
                          CodeGenParams.builder().wrapsRequest(true).withImpl(true).build()))
                  .addMethod(
                      methodBuilder("_builder")
                          .addModifiers(PUBLIC, STATIC)
                          .returns(immutableFacetsType.nestedClass("Builder"))
                          .addStatement(
                              "return new $T()", immutableFacetsType.nestedClass("Builder"))
                          .build())
                  .addType(
                      facetsBuilderClass
                          .addMethods(
                              createFacetContainerMethods(
                                  allFacets,
                                  immutableFacetsType,
                                  immutableFacetsType,
                                  CodeGenParams.builder()
                                      .wrapsRequest(true)
                                      .isBuilder(true)
                                      .withImpl(true)
                                      .build()))
                          .build())
                  .build())
          .build()
          .writeTo(writer);
      util.generateSourceFile(
          packageName() + '.' + getImmutFacetsClassname(vajramName()),
          writer.toString(),
          vajramInfo.vajramClass());
    } catch (IOException e) {
      util.error(String.valueOf(e.getMessage()), vajramInfo.vajramClass());
    }
  }

  private void batchFacetsClasses() {
    ClassName allFacetsType = getFacetsInterfaceType();
    ClassName allImmutFacetsType = ClassName.get(packageName, getImmutFacetsClassname(vajramName));
    ClassName batchImmutFacetsType = getBatchFacetsClassName();
    ClassName commonImmutFacetsType = getCommonFacetsClassName();

    List<FacetGenModel> batchedFacets =
        vajramInfo.facetStream().filter(t -> t.isBatched()).toList();
    List<FacetGenModel> commonFacets =
        vajramInfo.facetStream().filter(t -> !t.isBatched()).toList();

    TypeSpec.Builder batchImmutFacetsClass =
        util.classBuilder(batchImmutFacetsType.simpleName())
            .addModifiers(FINAL)
            .addSuperinterface(ImmutableFacetValuesContainer.class)
            .addField(allImmutFacetsType, FACETVALUES_VAR, PRIVATE, FINAL)
            .addMethod(
                constructorBuilder()
                    .addParameter(allFacetsType, FACETVALUES_VAR)
                    .addStatement("this.$L = $L._build()", FACETVALUES_VAR, FACETVALUES_VAR)
                    .build())
            .addAnnotation(
                AnnotationSpec.builder(EqualsAndHashCode.class)
                    .addMember("onlyExplicitlyIncluded", "true")
                    .build())
            .addMethod(
                overriding(util.getMethodToOverride(FacetValuesContainer.class, "_facets", 0))
                    .addStatement(
                        """
                        return $T._facets.stream()
                          .filter($T::isBatched)
                          .collect($T.toImmutableSet())
                        """,
                        getFacetsInterfaceType(),
                        FacetSpec.class,
                        ImmutableSet.class)
                    .build());

    for (FacetGenModel facet : batchedFacets) {
      CodeGenParams codeGenParams =
          CodeGenParams.builder().isSubsetBatch(true).withImpl(true).build();
      FacetJavaType returnType = util.getReturnType(facet, codeGenParams);
      MethodSpec.Builder getter =
          methodBuilder(facet.name())
              .returns(
                  returnType
                      .javaTypeName(facet)
                      .annotated(annotations(returnType.typeAnnotations(facet, codeGenParams))))
              .addStatement(returnType.fieldGetterCode(facet, codeGenParams));
      if (facet.isGiven()) {
        getter.addAnnotation(EqualsAndHashCode.Include.class);
      }
      batchImmutFacetsClass.addMethod(getter.build());
    }

    TypeSpec.Builder commonImmutFacetsClass =
        util.classBuilder(commonImmutFacetsType.simpleName())
            .addModifiers(FINAL)
            .addSuperinterface(ImmutableFacetValuesContainer.class)
            .addField(allFacetsType, FACETVALUES_VAR, PRIVATE, FINAL)
            .addMethod(
                constructorBuilder()
                    .addParameter(allFacetsType, FACETVALUES_VAR)
                    .addStatement("this.$L = $L._build()", FACETVALUES_VAR, FACETVALUES_VAR)
                    .build())
            .addAnnotation(
                AnnotationSpec.builder(EqualsAndHashCode.class)
                    .addMember("onlyExplicitlyIncluded", "true")
                    .build())
            .addMethod(
                overriding(util.getMethodToOverride(FacetValuesContainer.class, "_facets", 0))
                    .addStatement(
                        """
                        return $T._facets.stream()
                          .filter(spec -> !spec.isBatched())
                          .collect($T.toImmutableSet())
                        """,
                        getFacetsInterfaceType(),
                        ImmutableSet.class)
                    .build());

    for (FacetGenModel facet : commonFacets) {
      CodeGenParams codeGenParams =
          CodeGenParams.builder().isSubsetCommon(true).withImpl(true).build();
      FacetJavaType returnType = util.getReturnType(facet, codeGenParams);
      MethodSpec.Builder getter =
          methodBuilder(facet.name())
              .returns(
                  returnType
                      .javaTypeName(facet)
                      .annotated(annotations(returnType.typeAnnotations(facet, codeGenParams))))
              .addStatement(returnType.fieldGetterCode(facet, codeGenParams));
      if (facet.isGiven()) {
        getter.addAnnotation(EqualsAndHashCode.Include.class);
      }
      commonImmutFacetsClass.addMethod(getter.build());
    }

    TypeSpec.Builder allFacetsInterface =
        util.interfaceBuilder(getFacetsInterfaceName(vajramName))
            .addModifiers(PUBLIC)
            .addSuperinterface(BatchEnabledFacetValues.class)
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "ClassReferencesSubclass")
                    .build());

    addFacetConstants(
        allFacetsInterface, vajramInfo.facetStream().toList(), CodeGenParams.builder().build());
    // Add all the getters ( of batch and non bacth facetValues) to allFacetClass
    for (var input : batchedFacets) {
      createFacetGetter(allFacetsInterface, input, CodeGenParams.builder().build());
    }

    for (var input : commonFacets) {
      createFacetGetter(allFacetsInterface, input, CodeGenParams.builder().build());
    }

    util.generateSourceFile(
        batchImmutFacetsType.canonicalName(),
        JavaFile.builder(packageName(), batchImmutFacetsClass.build()).build().toString(),
        vajramInfo.vajramClass());
    util.generateSourceFile(
        commonImmutFacetsType.canonicalName(),
        JavaFile.builder(packageName(), commonImmutFacetsClass.build()).build().toString(),
        vajramInfo.vajramClass());
  }

  private ClassName getFacetsInterfaceType() {
    return ClassName.get(packageName(), getFacetsInterfaceName(vajramName));
  }

  private ClassName getRequestInterfaceType() {
    return ClassName.get(packageName(), getRequestInterfaceName(vajramName));
  }

  private ClassName getBatchFacetsClassName() {
    return ClassName.get(packageName(), vajramName + BATCH_FACETS_SUFFIX);
  }

  private ClassName getCommonFacetsClassName() {
    return ClassName.get(packageName(), vajramName + COMMON_FACETS_SUFFIX);
  }

  private void codegenBatchableFacets(
      TypeSpec.Builder allFacetsType,
      ClassName batchFacetsType,
      ClassName commonFacetsType,
      CodeGenParams codeGenParams) {
    MethodSpec.Builder batchElementMethod =
        overriding(util.getMethodToOverride(BatchEnabledFacetValues.class, "_batchElement", 0))
            .returns(batchFacetsType);
    MethodSpec.Builder commonMethod =
        overriding(util.getMethodToOverride(BatchEnabledFacetValues.class, "_common", 0))
            .returns(commonFacetsType);
    if (codeGenParams.withImpl()) {
      batchElementMethod.addStatement("return new $T(this)", batchFacetsType);
      commonMethod.addStatement("return new $T(this)", commonFacetsType);
    } else {
      batchElementMethod.addModifiers(ABSTRACT);
      commonMethod.addModifiers(ABSTRACT);
    }
    allFacetsType.addMethod(batchElementMethod.build()).addMethod(commonMethod.build());
  }

  private void addFacetConstants(
      TypeSpec.Builder classBuilder,
      List<? extends FacetGenModel> facetValues,
      CodeGenParams codeGenParams) {
    List<FieldSpec> specFields = new ArrayList<>(facetValues.size());
    List<FieldSpec> idFields = new ArrayList<>();
    int facetCount = 0;
    for (FacetGenModel facet : facetValues) {
      facetCount++;
      FieldSpec facetIdField =
          FieldSpec.builder(int.class, facet.name() + FACET_ID_SUFFIX)
              .addModifiers(PUBLIC, STATIC, FINAL)
              .initializer("$L", facet.id())
              .addJavadoc(
                  """
                  ******************************************************<br>
                  Facet id of the facet $S  <br>
                  ******************************************************
                  """,
                  facet.name())
              .build();
      idFields.add(facetIdField);

      TypeAndName facetType = util.getTypeName(util.getDataType(facet));
      TypeAndName boxedFacetType = util.box(facetType);
      ClassName vajramReqClass = getRequestInterfaceType();
      ClassName specType =
          ClassName.get(
              codeGenParams.isRequest()
                  ? InputMirrorSpec.class
                  : facet instanceof DependencyModel vajramDepDef
                      ? vajramDepDef.canFanout()
                          ? vajramDepDef.isMandatory()
                              ? MandatoryFanoutDepSpec.class
                              : OptionalFanoutDepSpec.class
                          : vajramDepDef.isMandatory()
                              ? MandatoryOne2OneDepSpec.class
                              : OptionalOne2OneDepSpec.class
                      : facet.isMandatory()
                          ? MandatoryFacetDefaultSpec.class
                          : OptionalFacetDefaultSpec.class);
      List<TypeName> collectClassNames = new ArrayList<>();
      CodeBlock.Builder initializerCodeBlock = CodeBlock.builder();
      initializerCodeBlock
          .add(
              """
                    new $T<>(
                      $N,
                      $S,
                    """,
              specType,
              facetIdField,
              facet.name())
          .add(
              util.getJavaTypeCreationCode(
                      (JavaType<?>) facet.dataType(), collectClassNames, facet.facetField())
                  + ",",
              collectClassNames.toArray());
      if (facet instanceof GivenFacetModel && !codeGenParams.isRequest()) {
        initializerCodeBlock.add("$T.of(", ImmutableSet.class);
        List<String> params = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (facet.facetTypes().contains(INPUT)) {
          params.add("$T.$L");
          args.addAll(List.of(FacetType.class, "INPUT"));
        }
        if (facet.facetTypes().contains(INJECTION)) {
          params.add("$T.$L");
          args.addAll(List.of(FacetType.class, "INJECTION"));
        }
        initializerCodeBlock.add(params.stream().collect(joining(",")) + "),", args.toArray());
      }
      initializerCodeBlock.add("""
              $T.class,
            """, vajramReqClass);
      if (facet instanceof DependencyModel vajramDepDef) {
        ClassName depReqClass = ClassName.bestGuess(vajramDepDef.depReqClassQualifiedName());
        initializerCodeBlock.add(
            """
                $T.class,
                $T.$L($S),
              """,
            depReqClass,
            VajramID.class,
            "vajramID",
            vajramDepDef.depVajramInfoLite().vajramId().vajramId());
      }
      String docComment = util.processingEnv().getElementUtils().getDocComment(facet.facetField());
      if (docComment == null) {
        docComment = "";
      }
      initializerCodeBlock.add("$S,", docComment);
      if (codeGenParams.isRequest()) {
        initializerCodeBlock.add(
            """
              _request -> (($T)_request).$L(),
              (_request, _value) -> {
                if(_value != null) {
                  (($T) _request).$L(_value);
                }
              }
            )
            """,
            ClassName.get(packageName(), getRequestInterfaceName(vajramName)),
            facet.name(),
            ClassName.get(packageName(), getImmutRequestInterfaceName(vajramName), "Builder"),
            facet.name());
      } else {
        initializerCodeBlock.add(
            """
                $L,
                $T.$L(() -> $T.$L.class.getDeclaredField($S)),
                _facetValues -> (($T)_facetValues).$L(),
                (_facetValues, _value) -> {
                  if(_value != null) {
                    (($T) _facetValues).$L(_value);
                  }
                }
              )
            """,
            facet.facetField().getAnnotation(Batch.class) != null,
            FacetSpec.class,
            "parseFacetTags",
            vajramInfo.vajramClass(),
            _FACETS_CLASS,
            facet.name(),
            ClassName.get(packageName(), getFacetsInterfaceName(vajramName)),
            facet.name(),
            ClassName.get(packageName(), getImmutFacetsClassname(vajramName), "Builder"),
            facet.name());
      }
      specFields.add(
          FieldSpec.builder(
                  facet instanceof DependencyModel vajramDepDef
                      ? ParameterizedTypeName.get(
                          specType,
                          boxedFacetType.typeName(),
                          vajramReqClass,
                          ClassName.bestGuess(vajramDepDef.depReqClassQualifiedName()))
                      : ParameterizedTypeName.get(
                          specType, boxedFacetType.typeName(), vajramReqClass),
                  facet.name() + FACET_SPEC_SUFFIX)
              .addModifiers(PUBLIC, STATIC, FINAL)
              .addAnnotation(
                  AnnotationSpec.builder(FacetIdNameMapping.class)
                      .addMember("id", "$L", facet.id())
                      .addMember("name", "$S", facet.name())
                      .build())
              .initializer(initializerCodeBlock.build())
              .build());
    }
    for (int i = 0; i < facetCount; i++) {
      classBuilder.addField(idFields.get(i)).addField(specFields.get(i));
    }

    ParameterizedTypeName facetsFieldType =
        ParameterizedTypeName.get(
            ClassName.get(ImmutableSet.class),
            ParameterizedTypeName.get(
                codeGenParams.isRequest()
                    ? ClassName.get(InputMirrorSpec.class)
                    : ClassName.get(FacetSpec.class),
                WildcardTypeName.subtypeOf(Object.class),
                getRequestInterfaceType()));
    FieldSpec facetsField =
        FieldSpec.builder(facetsFieldType, "_facets", PUBLIC, STATIC, FINAL)
            .initializer(
                specFields.stream().map(specField -> "$N").collect(joining(", ", "$T.of(", ")")),
                Stream.concat(Stream.of(ImmutableSet.class), specFields.stream()).toArray())
            .build();
    classBuilder.addField(facetsField);

    classBuilder.addMethod(
        overriding(util.getMethodToOverride(FacetValuesContainer.class, "_facets", 0))
            .addModifiers(PUBLIC, DEFAULT)
            .returns(facetsFieldType)
            .addStatement("return $N", facetsField)
            .build());
  }

  ImmutableSet<Integer> getResolverSources(ExecutableElement resolve) {
    return resolve.getParameters().stream().map(v -> inferFacetId(v)).collect(toImmutableSet());
  }
}
