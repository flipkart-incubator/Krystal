package com.flipkart.krystal.vajram.codegen.processor;

import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;
import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility.annotations;
import static com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility.getFacetsInterfaceName;
import static com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility.getImmutFacetsClassName;
import static com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility.getImmutRequestInterfaceName;
import static com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility.getImmutRequestPojoName;
import static com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility.getRequestInterfaceName;
import static com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility.getTypeParameters;
import static com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility.getVajramImplClassName;
import static com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility.recordAnnotations;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.BATCHED_OUTPUT_VAR;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.BATCHES_VAR;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.BATCH_ITEM_FACETS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.BATCH_KEY_FACETS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.BATCH_KEY_NAME;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.EMPTY_CODE_BLOCK;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACETS_LIST;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACET_NAME_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACET_SPEC_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACET_VALUES_VAR;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.GET_INPUT_RESOLVERS;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.GET_SIMPLE_INPUT_RESOLVERS;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.INCOMING_FACETS;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.OUTPUT_LOGIC_INPUT_VAR;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.QUALIFIED_FACET_SEPARATOR;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.RESOLVER_REQUEST;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.RESOLVER_REQUESTS;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.RESOLVER_RESULT;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.RESOLVER_RESULTS;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.VAJRAM_ID_CONSTANT_NAME;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants._INPUTS_CLASS;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants._INTERNAL_FACETS_CLASS;
import static com.flipkart.krystal.vajram.codegen.common.models.ParsedVajramData.fromVajramInfo;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.MethodSpec.overriding;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.annos.TraitDependency;
import com.flipkart.krystal.concurrent.Futures;
import com.flipkart.krystal.core.OutputLogicExecutionResults;
import com.flipkart.krystal.core.VajramID;
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
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.facets.FacetUtils;
import com.flipkart.krystal.facets.resolution.ResolutionTarget;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.ModelRoot.ModelType;
import com.flipkart.krystal.serial.ReservedSerialIds;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.TraitRequestRoot;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramDefRoot;
import com.flipkart.krystal.vajram.VajramRequestRoot;
import com.flipkart.krystal.vajram.batching.BatchEnabledFacetValues;
import com.flipkart.krystal.vajram.batching.BatchEnabledImmutableFacetValues;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.batching.BatchedFacets;
import com.flipkart.krystal.vajram.batching.BatchesGroupedBy;
import com.flipkart.krystal.vajram.codegen.common.models.CodeGenParams;
import com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.DefaultFacetModel;
import com.flipkart.krystal.vajram.codegen.common.models.DependencyModel;
import com.flipkart.krystal.vajram.codegen.common.models.FacetGenModel;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType;
import com.flipkart.krystal.vajram.codegen.common.models.LogicMethods;
import com.flipkart.krystal.vajram.codegen.common.models.LogicMethods.OutputLogics;
import com.flipkart.krystal.vajram.codegen.common.models.LogicMethods.OutputLogics.NoBatching;
import com.flipkart.krystal.vajram.codegen.common.models.LogicMethods.OutputLogics.WithBatching;
import com.flipkart.krystal.vajram.codegen.common.models.ParsedVajramData;
import com.flipkart.krystal.vajram.codegen.common.models.TypeAndName;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfoLite;
import com.flipkart.krystal.vajram.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenContext;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.FacetIdNameMapping;
import com.flipkart.krystal.vajram.facets.FacetValidation;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.One2OneCommand;
import com.flipkart.krystal.vajram.facets.resolution.AbstractFanoutInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.AbstractOne2OneInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.FanoutInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.resolution.One2OneInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
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
import com.google.common.collect.Sets;
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
import com.squareup.javapoet.WildcardTypeName;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Documented;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public class VajramCodeGenerator implements CodeGenerator {

  private static final ImmutableSet<String> MODEL_METHOD_ANNOTATIONS =
      ImmutableSet.<@NonNull String>copyOf(
          Stream.of(IfAbsent.class, SerialId.class)
              .<@NonNull String>map(aClass -> requireNonNull(aClass.getCanonicalName()))
              .toList());

  private static final ImmutableSet<String> MODEL_CLASS_ANNOTATIONS =
      ImmutableSet.<@NonNull String>copyOf(
          Stream.of(ReservedSerialIds.class)
              .<@NonNull String>map(aClass -> requireNonNull(aClass.getCanonicalName()))
              .toList());

  private final String packageName;
  private final VajramInfo currentVajramInfo;

  private final Map<Integer, FacetGenModel> facetModels;
  private final Map<String, FacetGenModel> facetModelsByName;
  private final boolean needsBatching;
  private final Map<Integer, Boolean> depFanoutMap;
  private final CodegenPhase codegenPhase;
  private @MonotonicNonNull ParsedVajramData parsedVajramData;
  private final CodeGenUtility util;
  private final String vajramName;
  private final Types typeUtils;
  private final Elements elementUtils;

  public VajramCodeGenerator(VajramCodeGenContext creationContext) {
    this.currentVajramInfo = creationContext.vajramInfo();
    this.packageName = creationContext.vajramInfo().lite().packageName();
    this.util = creationContext.util();
    this.typeUtils = util.processingEnv().getTypeUtils();
    this.elementUtils = util.processingEnv().getElementUtils();
    // noinspection Convert2MethodRef
    this.facetModels =
        creationContext
            .vajramInfo()
            .facetStream()
            .collect(
                toMap(
                    FacetGenModel::id,
                    Function.identity(),
                    (o1, o2) -> o1,
                    () -> new LinkedHashMap<>()));
    // noinspection Convert2MethodRef
    this.facetModelsByName =
        creationContext
            .vajramInfo()
            .facetStream()
            .collect(
                toMap(
                    FacetGenModel::name,
                    Function.identity(),
                    (o1, o2) -> o1,
                    () -> new LinkedHashMap<>()));
    this.needsBatching =
        creationContext.vajramInfo().givenFacets().stream().anyMatch(DefaultFacetModel::isBatched);
    this.depFanoutMap =
        creationContext.vajramInfo().dependencies().stream()
            .collect(toMap(DependencyModel::id, DependencyModel::canFanout));
    this.codegenPhase = creationContext.codegenPhase();
    this.vajramName = currentVajramInfo.vajramName();
  }

  @Override
  public void generate() {
    validate();
    if (CodegenPhase.MODELS.equals(codegenPhase)) {
      vajramRequest();
      // Generate facets classes only for Vajrams and not for Traits
      if (currentVajramInfo.vajramClass().getAnnotation(Vajram.class) != null) {
        vajramFacets();
      }
    } else if (CodegenPhase.FINAL.equals(codegenPhase)) {
      vajramWrapper();
    }
  }

  private void validate() {
    // Validate that IfAbsent.Strategy values are only applied to facets with
    // compatible data types
    validateIfAbsentStrategyApplicability();

    // Validate that none of the SerialId annotations on facets clash with the ReservedSerialIds
    // annotation on the Vajram
    validateSerialIdReservations();
  }

  private void vajramRequest() {
    ClassName requestInterfaceType = currentVajramInfo.lite().requestInterfaceType();
    ClassName immutReqInterfaceType = currentVajramInfo.lite().immutReqInterfaceType();
    List<DefaultFacetModel> inputs =
        currentVajramInfo.givenFacets().stream()
            .filter(facetDef -> facetDef.facetType().equals(INPUT))
            .toList();

    TypeSpec.Builder requestInterface =
        util.interfaceBuilder(
                requestInterfaceType.simpleName(),
                currentVajramInfo.vajramClass().getQualifiedName().toString())
            .addModifiers(PUBLIC)
            .addAnnotation(
                currentVajramInfo.lite().isTrait()
                    ? TraitRequestRoot.class
                    : VajramRequestRoot.class)
            .addAnnotation(
                AnnotationSpec.builder(ModelRoot.class)
                    .addMember(
                        "type", CodeBlock.of("$T.$L", ModelType.class, ModelType.REQUEST.name()))
                    .addMember("suffixSeparator", CodeBlock.of("$S", ""))
                    .addMember("builderExtendsModelRoot", "true")
                    .build())
            .addAnnotations(
                currentVajramInfo.vajramClass().getAnnotationMirrors().stream()
                    .filter(
                        annotationMirror ->
                            MODEL_CLASS_ANNOTATIONS.contains(
                                ((QualifiedNameable)
                                        annotationMirror.getAnnotationType().asElement())
                                    .getQualifiedName()
                                    .toString()))
                    .map(AnnotationSpec::get)
                    .toList())
            .addSuperinterfaces(currentVajramInfo.requestInterfaceSuperTypes());

    // Add VAJRAM_ID constant to the request interface
    FieldSpec vajramIdField =
        FieldSpec.builder(VajramID.class, VAJRAM_ID_CONSTANT_NAME)
            .addModifiers(PUBLIC, STATIC, FINAL)
            .initializer("$T.vajramID($S)", VajramID.class, vajramName)
            .addJavadoc("The VajramID for this Vajram")
            .build();
    requestInterface.addField(vajramIdField);

    // Add _vajramID() method to the request interface
    requestInterface.addMethod(
        overriding(util.getMethod(FacetValuesContainer.class, "_vajramID", 0))
            .addModifiers(DEFAULT)
            .addStatement("return " + VAJRAM_ID_CONSTANT_NAME)
            .build());

    facetConstants(requestInterface, inputs, CodeGenParams.builder().isRequest(true).build());
    modelsClassMembers(
        requestInterface,
        requestInterfaceType,
        inputs,
        CodeGenParams.builder().isRequest(true).isModelRoot(true).build());

    util.generateSourceFile(
        requestInterfaceType.canonicalName(),
        JavaFile.builder(
                packageName,
                requestInterface
                    .addMethods(
                        facetContainerMethods(
                            inputs,
                            requestInterfaceType,
                            immutReqInterfaceType,
                            CodeGenParams.builder().isRequest(true).build()))
                    .build())
            .build()
            .toString(),
        currentVajramInfo.vajramClass());
  }

  public void vajramFacets() {
    basicFacetsClasses();
    boolean doInputsNeedBatching =
        currentVajramInfo.facetStream().anyMatch(FacetGenModel::isBatched);
    if (doInputsNeedBatching) {
      batchFacetsClasses();
    }
  }

  /**
   * Method to generate VajramImpl class Input dependencyDef code gen Resolve method code gen Vajram
   * logic code gen Compute vajramDef execute IO vajramDef executeBlocking
   */
  public void vajramWrapper() {
    initParsedVajramData();
    final TypeSpec.Builder vajramWrapperClass =
        util.classBuilder(
                getVajramImplClassName(vajramName),
                currentVajramInfo.vajramClass().getQualifiedName().toString())
            .addModifiers(PUBLIC, FINAL);
    List<MethodSpec> methodSpecs = new ArrayList<>();
    // Add superType
    if (currentVajramInfo.lite().isTrait()) {
      vajramWrapperClass.addSuperinterface(ClassName.get(currentVajramInfo.vajramClass()));
    } else {
      vajramWrapperClass.superclass(ClassName.get(currentVajramInfo.vajramClass()));
    }

    ClassName requestInterfaceType = getRequestInterfaceType();
    ClassName immutRequestType = ClassName.get(packageName, getImmutRequestPojoName(vajramName));
    ClassName immutFacetsType = ClassName.get(packageName, getImmutFacetsClassName(vajramName));

    if (currentVajramInfo.lite().isVajram()) {
      // Map of all the resolved dependencies to the methods resolving them
      Map<String, List<ExecutableElement>> resolverMap = new HashMap<>();
      LogicMethods logicMethods = getParsedVajramData().logicMethods();

      List<ExecutableElement> resolvers =
          logicMethods != null ? logicMethods.resolvers() : List.of();
      for (ExecutableElement resolver : resolvers) {
        String dependency =
            util.extractFacetName(
                vajramName, checkNotNull(resolver.getAnnotation(Resolve.class)).dep(), resolver);
        resolverMap.computeIfAbsent(dependency, _k -> new ArrayList<>()).add(resolver);
      }

      // Initialize few common attributes and data structures
      MethodSpec inputResolversMethod = createInputResolvers();
      if (inputResolversMethod != null) {
        methodSpecs.add(inputResolversMethod);
      }

      if (util.isRawAssignable(
          getParsedVajramData().vajramInfo().vajramClass().asType(), IOVajramDef.class)) {
        methodSpecs.add(ioVajramExecuteMethod());
      } else {
        methodSpecs.add(computeVajramExecuteMethod());
      }
      vajramWrapperClass.addMethod(
          methodBuilder("facetsFromRequest")
              .returns(immutFacetsType.nestedClass("Builder"))
              .addModifiers(PUBLIC)
              .addParameter(
                  ParameterizedTypeName.get(
                      ClassName.get(Request.class), WildcardTypeName.subtypeOf(Object.class)),
                  "request")
              .addStatement(
                  "return new $T(($T)$L)",
                  immutFacetsType.nestedClass("Builder"),
                  currentVajramInfo.conformsToTraitOrSelf().requestInterfaceType(),
                  "request")
              .build());
    }
    StringWriter writer = new StringWriter();
    try {
      JavaFile.builder(
              packageName,
              vajramWrapperClass
                  .addMethods(methodSpecs)
                  .addMethod(
                      overriding(util.getMethod(VajramDefRoot.class, "newRequestBuilder", 0))
                          .returns(immutRequestType.nestedClass("Builder"))
                          .addModifiers(PUBLIC)
                          .addStatement("return $T._builder()", immutRequestType)
                          .build())
                  .addMethod(
                      overriding(util.getMethod(VajramDefRoot.class, "requestRootType", 0))
                          .returns(
                              ParameterizedTypeName.get(
                                  ClassName.get(Class.class), requestInterfaceType))
                          .addModifiers(PUBLIC)
                          .addStatement("return $T.class", requestInterfaceType)
                          .build())
                  .build())
          .build()
          .writeTo(writer);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    String className =
        currentVajramInfo.lite().packageName()
            + '.'
            + getVajramImplClassName(currentVajramInfo.lite().vajramId().id());
    try {
      util.generateSourceFile(className, writer.toString(), currentVajramInfo.vajramClass());
    } catch (Exception e) {
      StringWriter exception = new StringWriter();
      e.printStackTrace(new PrintWriter(exception));
      util.error(
          "Error while generating file for class %s. Exception: %s".formatted(className, exception),
          currentVajramInfo.vajramClass());
    }
  }

  private int inferFacetId(VariableElement parameter) {
    String facetName = parameter.getSimpleName().toString();
    FacetGenModel facetGenModel = facetModelsByName.get(facetName);
    if (facetGenModel == null) {
      throw util.errorAndThrow("Unknown facet with name %s".formatted(facetName), parameter);
    }
    return facetGenModel.id();
  }

  private @Nullable MethodSpec createInputResolvers() {
    LogicMethods logicMethods = getParsedVajramData().logicMethods();
    List<ExecutableElement> resolvers = logicMethods != null ? logicMethods.resolvers() : List.of();
    if (resolvers.isEmpty()) {
      return null;
    }

    MethodSpec.Builder getInputResolversMethod =
        overriding(util.getMethod(VajramDef.class, GET_INPUT_RESOLVERS, 0));

    CodeBlock.Builder resolveMethodToObjConvCode = CodeBlock.builder();
    getInputResolversMethod.addCode(
        "return $T.<$T>builder().addAll($L())",
        ImmutableList.class,
        InputResolver.class,
        GET_SIMPLE_INPUT_RESOLVERS);

    Map<String, List<ExecutableElement>> resolverMap = new HashMap<>();

    for (ExecutableElement resolver : resolvers) {
      resolverMap
          .computeIfAbsent(
              util.extractFacetName(
                  vajramName,
                  requireNonNull(resolver.getAnnotation(Resolve.class)).dep(),
                  resolver),
              _k -> new ArrayList<>())
          .add(resolver);
    }
    resolverMap.forEach(
        (depName, resolverMethods) -> {
          @SuppressWarnings("method.invocation")
          DependencyModel dep =
              currentVajramInfo.dependencies().stream()
                  .filter(d -> d.name().equals(depName))
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
                    .map(
                        di ->
                            util.extractFacetName(
                                dep.depVajramInfo().vajramId().id(), di, resolverMethod))
                    .toList();

            Class<?> inputResolverInterfaceClass;
            if (fanoutResolver) {
              inputResolverInterfaceClass = FanoutInputResolver.class;
            } else {
              inputResolverInterfaceClass = One2OneInputResolver.class;
            }
            MethodSpec.Builder resolveMethod =
                overriding(
                        util.getMethod(inputResolverInterfaceClass, "resolve", 2),
                        (DeclaredType)
                            checkNotNull(
                                    elementUtils.getTypeElement(
                                        checkNotNull(
                                            inputResolverInterfaceClass.getCanonicalName())))
                                .asType(),
                        typeUtils)
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
                            usedFacets.stream().map(_f -> "$T.$L").collect(Collectors.joining(",")),
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
                            resolvedInputNames.stream()
                                .map(_f -> "$T.$L")
                                .collect(Collectors.joining(",")),
                            resolvedInputNames.stream()
                                .flatMap(
                                    inputName ->
                                        Stream.of(
                                            ClassName.get(
                                                dep.depReqPackageName(),
                                                getRequestInterfaceName(
                                                    dep.depVajramInfo().vajramId().id())),
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
          fromVajramInfo(currentVajramInfo, util)
              .orElseThrow(
                  () ->
                      util.errorAndThrow(
                          """
                      Could not load Vajram class for vajramDef %s.
                      ParsedVajram Data should never be accessed in model generation phase."""
                              .formatted(currentVajramInfo.lite().vajramId()),
                          currentVajramInfo.vajramClass()));
    }
    return parsedVajramData;
  }

  private ParsedVajramData getParsedVajramData() {
    if (codegenPhase != CodegenPhase.FINAL) {
      throw new IllegalStateException(
          "getParsedVajramData can only be invoked in the FINAL phase. This is a bug in "
              + this.getClass());
    }
    // This should not happen since this method is only ever called after
    // initParsedVajramData is called. But we still implement the best effort
    // fallback
    return parsedVajramData != null ? parsedVajramData : initParsedVajramData();
  }

  /**
   * Method to generate "executeCompute" function code for ComputeVajrams Supported DataAccessSpec
   * => VajramID only.
   *
   * @return generated code for "executeCompute" {@link MethodSpec}
   */
  private MethodSpec computeVajramExecuteMethod() {

    MethodSpec.Builder executeBuilder =
        overriding(util.getMethod(VajramDef.class, "execute", 1))
            .returns(OutputLogicExecutionResults.class);
    if (needsBatching) {
      util.error(
          "Batching is not supported in ComputeVajrams",
          getParsedVajramData().vajramInfo().givenFacets().stream()
              .filter(DefaultFacetModel::isBatched)
              .findAny()
              .<Element>map(DefaultFacetModel::facetField)
              .orElse(getParsedVajramData().vajramInfo().vajramClass()));
    } else { // TODO : Need non batched IO vajramDef to test this
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
                if($L.facetValues().isEmpty()) {
                  return $T.empty();
                }
                return new $T<>(_logicInput.facetValues().stream()
                    .collect(
                       $T.toImmutableMap($T.identity(),
                       $L -> {
                          $T $L = ($T)$L;
                """,
                OUTPUT_LOGIC_INPUT_VAR,
                OutputLogicExecutionResults.class,
                OutputLogicExecutionResults.class,
                ImmutableMap.class,
                Function.class,
                INCOMING_FACETS,
                getFacetsInterfaceType(),
                FACET_VALUES_VAR,
                getFacetsInterfaceType(),
                INCOMING_FACETS);
    if (!(requireNonNull(getParsedVajramData().logicMethods(), "Vajrams must have output logic.")
            .outputLogics()
        instanceof NoBatching noBatchingOutputLogic)) {
      util.error(
          "Cannot generate execute method if @Output logic is missing",
          getParsedVajramData().vajramInfo().vajramClass());
      return;
    }

    ExecutableElement outputLogic = noBatchingOutputLogic.output();

    outputLogic
        .getParameters()
        .forEach(
            parameter ->
                generateFacetLocalVariable(
                    outputLogic, parameter, returnBuilder, FACET_VALUES_VAR));

    String methodCallSuffix;
    if (isIOVajram) {
      TypeMirror returnType = outputLogic.getReturnType();
      if (!util.isRawAssignable(returnType, CompletableFuture.class)) {
        // TODO: Validate IOVajram response type is CompletableFuture<Type>"
        util.error(
            "The OutputLogic of non-batched IO vajramDef %s must return a CompletableFuture"
                .formatted(vajramName),
            outputLogic);
      }

      returnBuilder.add("\nreturn $L(\n", outputLogic.getSimpleName());
      methodCallSuffix = ");\n";
    } else {
      returnBuilder.add(
          "\nreturn $T.errableFrom(() -> $L(\n", Errable.class, outputLogic.getSimpleName());
      methodCallSuffix = ")).toFuture();\n";
    }

    List<String> outputLogicParamsCode = new ArrayList<>();
    List<Object> outputLogicParamsCodeArgs = new ArrayList<>();
    for (VariableElement param : outputLogic.getParameters()) {
      generateLogicParams(param, outputLogicParamsCode, outputLogicParamsCodeArgs);
    }
    returnBuilder.add(
        String.join(", ", outputLogicParamsCode), outputLogicParamsCodeArgs.toArray());

    returnBuilder.add(methodCallSuffix);
    returnBuilder.add("})));\n");
    executeBuilder.addCode(returnBuilder.build());
  }

  private CodeBlock generateLogicParams(VariableElement param) {
    int facetId = inferFacetId(param);
    FacetGenModel facetGenModel = facetModels.get(facetId);
    if (facetGenModel == null) {
      throw new AssertionError("Cannot happen");
    }
    if (facetGenModel.isBatched()) {
      util.error(
          "Cannot use batch facet '%s' as direct input param for output logic".formatted(facetId),
          param);
    }
    return CodeBlock.of("$L", facetGenModel.name());
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
      util.error(
          "Cannot use batch facet '%s' as direct input param for output logic".formatted(facetId),
          param);
    } else {
      outputLogicParamsCode.add("$L");
      outputLogicParamsCodeArgs.add(facetGenModel.name());
    }
  }

  /**
   * Method to generate "execute" function code for IOVajrams
   *
   * @return generated code for "execute" {@link MethodSpec}
   */
  private MethodSpec ioVajramExecuteMethod() {

    MethodSpec.Builder executeMethodBuilder =
        overriding(util.getMethod(VajramDef.class, "execute", 1))
            .returns(OutputLogicExecutionResults.class);

    if (needsBatching) {
      batchedExecuteMethodBuilder(executeMethodBuilder);
    } else {
      nonBatchedExecuteMethodBuilder(executeMethodBuilder, true);
    }
    return executeMethodBuilder.build();
  }

  private void batchedExecuteMethodBuilder(MethodSpec.Builder executeMethodBuilder) {
    if (!(requireNonNull(getParsedVajramData().logicMethods(), "Vajrams must have output logic.")
            .outputLogics()
        instanceof OutputLogics.WithBatching withBatchingOutputLogic)) {
      util.error(
          "Cannot generate execute method if either of @Output.Batched or @Output.Unbatch is missing",
          getParsedVajramData().vajramInfo().vajramClass());
      return;
    }
    CodeBlock.Builder codeBuilder = CodeBlock.builder();

    ImmutableMap<String, FacetGenModel> facets =
        currentVajramInfo
            .facetStream()
            .collect(ImmutableMap.toImmutableMap(FacetGenModel::name, Function.identity()));

    TypeMirror batchedOutputReturnType = withBatchingOutputLogic.batchedOutput().getReturnType();
    if (!util.isRawAssignable(batchedOutputReturnType, CompletableFuture.class)) {
      throw util.errorAndThrow(
          "The OutputLogic of batched IO vajramDef %s must return a CompletableFuture"
              .formatted(vajramName),
          withBatchingOutputLogic.batchedOutput());
    }
    TypeMirror batchedOutputActualType = getTypeParameters(batchedOutputReturnType).get(0);

    TypeMirror unbatchOutputReturnType = withBatchingOutputLogic.unbatchOutput().getReturnType();
    TypeMirror vajramResponseType =
        util.box(
            getParsedVajramData()
                .vajramInfo()
                .lite()
                .responseType()
                .javaModelType(util.processingEnv()));
    TypeName vajramResponseTypeName =
        TypeName.get(vajramResponseType).annotated(AnnotationSpec.builder(Nullable.class).build());

    TypeMirror unbatchOutputExpectedType =
        typeUtils.getDeclaredType(
            requireNonNull(
                elementUtils.getTypeElement(requireNonNull(Map.class.getCanonicalName()))),
            requireNonNull(elementUtils.getTypeElement(getBatchItemIfaceName().canonicalName()))
                .asType(),
            typeUtils.getDeclaredType(
                requireNonNull(
                    elementUtils.getTypeElement(requireNonNull(Errable.class.getCanonicalName()))),
                vajramResponseType));
    if (!typeUtils.isAssignable(unbatchOutputReturnType, unbatchOutputExpectedType)) {
      util.error(
          """
              @Output.Unbatch return type is not as expected. Expected:
              %s.
              Found:
              %s"""
              .formatted(unbatchOutputExpectedType, unbatchOutputReturnType),
          withBatchingOutputLogic.unbatchOutput());
    }

    Map<String, Object> valueMap = new HashMap<>();
    valueMap.put("facetValues", ClassName.get(FacetValues.class));
    valueMap.put("batchKeyName", BATCH_KEY_NAME);

    valueMap.put("facetValuesList", FACETS_LIST);
    valueMap.put("logicInput", OUTPUT_LOGIC_INPUT_VAR);
    valueMap.put("facetsVar", FACET_VALUES_VAR);
    valueMap.put("facetsInterface", ClassName.get(packageName, getFacetsInterfaceName(vajramName)));
    valueMap.put("inputBatching", getBatchItemIfaceName());
    valueMap.put("commonInput", getCommonFacetsClassName());
    valueMap.put("vajramResponseType", vajramResponseTypeName);
    valueMap.put("futuresUtil", Futures.class);
    valueMap.put("batchedOutputLogic", withBatchingOutputLogic.batchedOutput().getSimpleName());
    valueMap.put("unbatchOutputLogic", withBatchingOutputLogic.unbatchOutput().getSimpleName());
    valueMap.put("unbatchOutputExpectedType", unbatchOutputExpectedType);
    valueMap.put("modInput", BatchedFacets.class);
    valueMap.put("imMap", ImmutableMap.class);
    valueMap.put("imList", ImmutableList.class);
    valueMap.put("comFuture", CompletableFuture.class);
    valueMap.put("linkHashMap", LinkedHashMap.class);
    valueMap.put("map", Map.class);
    valueMap.put("list", List.class);
    valueMap.put("valErr", Errable.class);
    valueMap.put("function", Function.class);
    valueMap.put("optional", Optional.class);
    valueMap.put("outputLogicExecutionResult", OutputLogicExecutionResults.class);

    codeBuilder.addNamed(
        """
            var $facetValuesList:L = $logicInput:L.facetValues();
            if($facetValuesList:L.isEmpty()) {
              return $outputLogicExecutionResult:T.empty();
            }
            $map:T<$inputBatching:T, $facetValues:T> _batchItems = new $linkHashMap:T<>();
            $commonInput:T $batchKeyName:L = (($facetsInterface:T)$facetValuesList:L.get(0)).$batchKeyName:L();
            for ($facetValues:T $facetsVar:L : $facetValuesList:L) {
              $facetsInterface:T _castFacets = ($facetsInterface:T) $facetsVar:L;
              $inputBatching:T _batch = _castFacets._batchItem();
              _batchItems.put(_batch, $facetsVar:L);
            }
        """,
        valueMap);
    Map<String, VariableElement> batchedOutputLogicParams =
        withBatchingOutputLogic.batchedOutput().getParameters().stream()
            .collect(toMap(v -> v.getSimpleName().toString(), v -> v));
    Map<String, VariableElement> unbatchOutputLogicParams =
        withBatchingOutputLogic.unbatchOutput().getParameters().stream()
            .collect(toMap(v -> v.getSimpleName().toString(), v -> v));
    for (String paramName :
        Sets.union(batchedOutputLogicParams.keySet(), unbatchOutputLogicParams.keySet())) {
      VariableElement p1 = batchedOutputLogicParams.get(paramName);
      VariableElement p2 = unbatchOutputLogicParams.get(paramName);
      List<VariableElement> list = new ArrayList<>();
      if (p1 != null && p2 != null && !typeUtils.isSameType(p1.asType(), p2.asType())) {
        util.error(
            "The same parameter cannot be accessed via different types in @Output.Batched and @Output.Unbatch methods.",
            p1,
            p2);
      }

      if (p1 != null) {
        list.add(p1);
      }
      if (p2 != null) {
        list.add(p2);
      }
      LogicParam logicParam = new LogicParam(list);
      validateParamsAndCreateParamCode(
          codeBuilder, withBatchingOutputLogic, logicParam, batchedOutputActualType, facets);
    }

    {
      List<CodeBlock> batchedOutputParamCodes = new ArrayList<>();
      for (VariableElement param : withBatchingOutputLogic.batchedOutput().getParameters()) {
        if (param.getSimpleName().contentEquals(BATCHES_VAR)) {
          batchedOutputParamCodes.add(
              CodeBlock.of("$T.copyOf($L.keySet())", ImmutableList.class, BATCHES_VAR));
        } else {
          batchedOutputParamCodes.add(generateLogicParams(param));
        }
      }
      valueMap.put(
          "batchedOutputParams", batchedOutputParamCodes.stream().collect(CodeBlock.joining(",")));
    }

    {
      List<CodeBlock> unbatchOutputParamsCode = new ArrayList<>();
      for (VariableElement param : withBatchingOutputLogic.unbatchOutput().getParameters()) {
        if (param.getSimpleName().contentEquals(BATCHES_VAR)) {
          unbatchOutputParamsCode.add(
              CodeBlock.of("$T.copyOf($L.keySet())", ImmutableList.class, BATCHES_VAR));
        } else if (param.getSimpleName().contentEquals(BATCHED_OUTPUT_VAR)) {
          if (typeUtils.isSameType(
              typeUtils.erasure(param.asType()),
              typeUtils.erasure(
                  requireNonNull(
                          elementUtils.getTypeElement(
                              requireNonNull(Errable.class.getCanonicalName())))
                      .asType()))) {
            unbatchOutputParamsCode.add(
                CodeBlock.of(
                    "$T.errableFrom($L, $L)", Errable.class, BATCHED_OUTPUT_VAR, "_throwable"));
            valueMap.put("nullCheckCode", "");
          } else {
            valueMap.put("nullCheckCode", "if(_batchedOutput != null) ");
            unbatchOutputParamsCode.add(CodeBlock.of("$L", BATCHED_OUTPUT_VAR));
          }
        } else {
          unbatchOutputParamsCode.add(generateLogicParams(param));
        }
      }
      valueMap.put(
          "unbatchOutputParams", unbatchOutputParamsCode.stream().collect(CodeBlock.joining(",")));
    }

    codeBuilder.addNamed(
        """
                var _batchedOutputFuture = $batchedOutputLogic:L($batchedOutputParams:L);
                $map:T<$facetValues:T, $comFuture:T<$vajramResponseType:T>> _returnValue = new $linkHashMap:T<>();

                _batchItems
                    .values()
                    .forEach(
                        _facetValues -> {
                          _returnValue.put(_facetValues, new $comFuture:T<>());
                        });
                _batchedOutputFuture.whenCompleteAsync(
                    (_batchedOutput, _throwable) -> {
                      $unbatchOutputExpectedType:T _unbatchedOutput = $imMap:T.of();
                      $nullCheckCode:L{
                        try {
                          _unbatchedOutput = $unbatchOutputLogic:L($unbatchOutputParams:L);
                        } catch (Throwable _e) {
                          _throwable = _e;
                        }
                      }
                      for(var _entry : _batchItems.entrySet()) {
                        var _batchItem = _entry.getKey();
                        var _facetValues = _entry.getValue();
                        var _result = _unbatchedOutput.get(_batchItem);
                        if (_result != null) {
                          $futuresUtil:T.linkFutures(
                              _result.toFuture(),
                              _returnValue.computeIfAbsent(
                                  _facetValues, _k -> new $comFuture:T<>()));
                        } else {
                          if (_throwable == null) {
                            _returnValue
                                .computeIfAbsent(_facetValues, _k -> new $comFuture:T<>())
                                .complete(null);
                          } else {
                            _returnValue
                              .computeIfAbsent(_facetValues, _k -> new $comFuture:T<>())
                              .completeExceptionally(_throwable);
                          }
                        }
                      }
                    }, $logicInput:L.graphExecutor());
                return new $outputLogicExecutionResult:T<>($imMap:T.copyOf(_returnValue));
            """,
        valueMap);
    executeMethodBuilder.addCode(codeBuilder.build());
  }

  private record LogicParam(ImmutableList<VariableElement> elements) {

    LogicParam(List<VariableElement> elements) {
      this(ImmutableList.copyOf(elements));
    }

    LogicParam {
      if (elements.isEmpty()) {
        throw new IllegalArgumentException("elements cannot be empty");
      }
    }

    String getSimpleName() {
      return element().getSimpleName().toString();
    }

    TypeMirror asType() {
      return element().asType();
    }

    private VariableElement element() {
      return elements.get(0);
    }

    public Element[] elementArray() {
      return elements.toArray(Element[]::new);
    }
  }

  private void validateParamsAndCreateParamCode(
      CodeBlock.Builder codeBuilder,
      WithBatching withBatchingOutputLogic,
      LogicParam param,
      TypeMirror batchedOutputActualType,
      ImmutableMap<String, FacetGenModel> facets) {
    if (param.getSimpleName().contentEquals(BATCHES_VAR)) {
      TypeMirror paramType = param.asType();
      List<? extends TypeMirror> batchTypeParams = getTypeParameters(paramType);
      TypeMirror expected =
          checkNotNull(
                  util.processingEnv()
                      .getElementUtils()
                      .getTypeElement(getBatchItemIfaceName().canonicalName()))
              .asType();
      if (!util.processingEnv()
              .getTypeUtils()
              .isSameType(
                  requireNonNull(typeUtils.asElement(paramType)).asType(),
                  requireNonNull(
                          util.processingEnv()
                              .getElementUtils()
                              .getTypeElement(
                                  requireNonNull(ImmutableCollection.class.getCanonicalName())))
                      .asType())
          || batchTypeParams.size() != 1
          || !typeUtils.isSameType(expected, batchTypeParams.get(0))) {
        util.error(
            "'_batchItems' param must be of type ImmutableCollection<"
                + expected
                + "> . Found: "
                + paramType,
            param.elementArray());
      }
    } else if (param.getSimpleName().contentEquals(BATCHED_OUTPUT_VAR)) {
      DeclaredType errableOfBatcheOutputType =
          util.processingEnv()
              .getTypeUtils()
              .getDeclaredType(
                  requireNonNull(
                      util.processingEnv()
                          .getElementUtils()
                          .getTypeElement(requireNonNull(Errable.class.getCanonicalName()))),
                  batchedOutputActualType);
      if (!typeUtils.isAssignable(batchedOutputActualType, param.asType())
          && !util.processingEnv()
              .getTypeUtils()
              .isAssignable(errableOfBatcheOutputType, param.asType())) {
        util.error(
            "'_batchedOutput' param must be compatible with type parameter of the "
                + "Future returned by the @Output.Batched method: Expected \n'"
                + batchedOutputActualType
                + "'\n or \n'"
                + errableOfBatcheOutputType
                + "'\n Found: \n"
                + param.asType(),
            param.elementArray());
      }
    } else {
      FacetGenModel facet = facets.get(param.getSimpleName());
      if (facet == null) {
        util.error(
            "No facet with the name "
                + param.getSimpleName()
                + " exists in the vajramDef "
                + currentVajramInfo.lite().vajramId(),
            param.elementArray());
      } else if (!facet.isUsedToGroupBatches()) {
        util.error(
            "Facet '"
                + facet.name()
                + "' can be accessed individually in the output logic only if it has been annotated as @"
                + BatchesGroupedBy.class.getSimpleName(),
            param.elementArray());
      }
      generateFacetLocalVariable(
          withBatchingOutputLogic.batchedOutput(), param.element(), codeBuilder, BATCH_KEY_NAME);
    }
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
    String depName = util.extractFacetName(vajramName, resolve.dep(), method);
    FacetGenModel facetGenModel = checkNotNull(facetModelsByName.get(depName));

    if (!(facetGenModel instanceof DependencyModel dependencyModel)) {
      util.error("No facet definition found for " + facetGenModel.name(), method);
      return CodeBlock.builder();
    }
    VajramID depVajramName = dependencyModel.depVajramInfo().vajramId();
    String depRequestPackage = dependencyModel.depReqPackageName();

    // check if the facetDef is satisfied by facetDef or other resolved variables
    ClassName requestBuilderType =
        ClassName.get(depRequestPackage, getImmutRequestInterfaceName(depVajramName.id()))
            .nestedClass("Builder");
    CodeBlock.Builder codeBuilder;
    if (fanoutResolver) {
      codeBuilder =
          CodeBlock.builder()
              .addStatement("var $2L = (($1T)_depRequest)", requestBuilderType, RESOLVER_REQUEST);
    } else {
      codeBuilder =
          CodeBlock.builder()
              .addStatement(
                  "var $2L = (($1T)_depRequests)",
                  ParameterizedTypeName.get(ClassName.get(ImmutableList.class), requestBuilderType),
                  RESOLVER_REQUESTS);
    }
    codeBuilder.addStatement(
        "var $L = (($T)_rawFacetValues)", FACET_VALUES_VAR, getFacetsInterfaceType());
    // TODO : add validation if fanout, then method should accept dependencyDef
    // response for the
    // bind type parameter else error
    // Iterate over the method params and call respective binding methods
    method
        .getParameters()
        .forEach(
            parameter ->
                generateFacetLocalVariable(method, parameter, codeBuilder, FACET_VALUES_VAR));

    buildResolverInvocation(
        codeBuilder,
        method,
        dependencyModel,
        stream(resolve.depInputs())
            .map(
                di ->
                    util.extractFacetName(
                        dependencyModel.depVajramInfo().vajramId().id(), di, method))
            .collect(LinkedHashSet<String>::new, LinkedHashSet::add, LinkedHashSet::addAll),
        fanoutResolver,
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
    } else if (usingFacetModel instanceof DefaultFacetModel defaultFacetModel) {
      TypeMirror facetType = defaultFacetModel.dataType().javaModelType(util.processingEnv());
      String variable = usingFacetModel.name();
      TypeMirror parameterTypeMirror = parameter.asType();
      final TypeName parameterType = TypeName.get(parameterTypeMirror);
      if (defaultFacetModel.isMandatoryOnServer()) {
        if (!typeUtils.isAssignable(facetType, parameterTypeMirror)) {
          util.error(
              "Incorrect facet type being consumed. Expected '%s', found '%s'"
                  .formatted(facetType, parameterType),
              parameter);
        }
        codeBuilder.add(
            CodeBlock.builder()
                .addStatement(
                    "var $L = $T.validateMandatoryFacet($L.$L(), $S, $S)",
                    variable,
                    FacetValidation.class,
                    facetsVar,
                    usingFacetModel.name(),
                    currentVajramInfo.lite().vajramId().id(),
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
        util.error(
            String.format(
                "Optional dependency %s must have type as Optional", usingFacetModel.name()),
            parameter);
      }
    } else {
      util.error("No facetDef resolver found for " + usingFacetModel.name(), parameter);
    }
  }

  /**
   * Method to generate resolver code for dependencyDef bindings
   *
   * @param method Dependency resolver method
   * @param usingFacetId The bind param name in the resolver method
   * @param codeBuilder The {@link CodeBlock.Builder}
   * @param parameter the bind parameter in the resolver method
   */
  private void generateDependencyResolutions(
      ExecutableElement method,
      final int usingFacetId,
      CodeBlock.Builder codeBuilder,
      VariableElement parameter) {
    FacetGenModel usingFacetDef = checkNotNull(facetModels.get(usingFacetId));

    TypeMirror parameterType = parameter.asType();
    String usingFacetName = usingFacetDef.name();
    // ReturnType returnType
    if (usingFacetDef instanceof DependencyModel dependencyModel) {
      String variableName = usingFacetName;
      final VajramInfoLite depVajramInfoLite = dependencyModel.depVajramInfo();
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
          util.error(
              """
                  The fanout dependency ('%s') can be consumed only via the FanoutDepResponses<ReqType,RespType> class. \
                  Found '%s' instead"""
                  .formatted(dependencyModel.name(), parameterType),
              parameter);
        }
        String depValueAccessorCode = "$T $L = $L.$L()";
        codeBuilder.addStatement(
            depValueAccessorCode,
            util.responsesType(dependencyModel),
            variableName,
            FACET_VALUES_VAR,
            usingFacetName);
      } else {
        // This means the dependency being consumed used is not a fanout dependency
        String depAccessLhs = "$1T $2L";
        String depAccessRhs = "$3L.$4L()";
        String depValueAccessorCode = depAccessLhs + " = " + depAccessRhs;
        if (usingFacetDef.isMandatoryOnServer()) {
          if (unboxedDepType.equals(TypeName.get(parameterType))) {
            // This means this dependencyDef being consumed is a non fanout mandatory
            // dependency and
            // the dev has requested the value directly. So we extract the only value from
            // dependencyDef response and provide it.
            codeBuilder.addStatement(
                depAccessLhs
                    + "= $5T.validateMandatoryFacet("
                    + depAccessRhs
                    + ".response().valueOpt().orElse(null), $6S, $7S)",
                unboxedDepType,
                variableName,
                FACET_VALUES_VAR,
                usingFacetName,
                FacetValidation.class,
                currentVajramInfo.lite().vajramId().id(),
                usingFacetName);
          } else {
            // This means the dependency being consumed is mandatory, but the type of the
            // parameter
            // is not matching the type of the facet
            util.error(
                "A resolver must consume a mandatory dependency directly using its type (%s). Found '%s' instead"
                    .formatted(unboxedDepType, parameterType),
                parameter);
          }
        } else {
          // dependency is optional then accept only errable and optional in resolver
          if (util.isRawAssignable(parameterType, Errable.class)) {
            // This means this dependencyDef in "Using" annotation is not a fanout and the
            // dev has
            // requested the 'Errable'. So we extract the only Errable from dependencyDef
            // response and provide it.
            codeBuilder.addStatement(
                depValueAccessorCode + ".response()",
                ParameterizedTypeName.get(ClassName.get(Errable.class), boxedDepType),
                variableName,
                FACET_VALUES_VAR,
                usingFacetName);
          } else if (util.isRawAssignable(parameterType, Optional.class)) {
            // This means this dependencyDef being consumed is not a fanout and the
            // dev has  requested an 'Optional'. So we retrieve the Errable from the
            // response, extract the optional and provide it.
            String code = depValueAccessorCode + ".response().valueOpt()";
            codeBuilder.addStatement(
                code,
                ParameterizedTypeName.get(ClassName.get(Optional.class), boxedDepType),
                variableName,
                FACET_VALUES_VAR,
                usingFacetName);
          } else if (util.isRawAssignable(parameterType, One2OneDepResponse.class)) {
            // This means this dependencyDef being consumed is not a fanout and the  dev has
            // requested a 'One2OneDepResponse'.
            codeBuilder.addStatement(
                depValueAccessorCode,
                ParameterizedTypeName.get(ClassName.get(Optional.class), boxedDepType),
                variableName,
                FACET_VALUES_VAR,
                usingFacetName);
          } else {
            util.error(
                ("A resolver ('%s') must not access an optional dependency ('%s') directly."
                        + "Use Optional<>, Errable<>, or DependencyResponse<> instead")
                    .formatted(logicName, usingFacetName),
                parameter);
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
   * @param methodCodeBuilder {@link CodeBlock.Builder}
   * @param resolverMethod Resolve method
   * @param depInputNames Resolve facetValues
   */
  private void buildResolverInvocation(
      CodeBlock.Builder methodCodeBuilder,
      ExecutableElement resolverMethod,
      DependencyModel dependencyModel,
      Set<String> depInputNames,
      boolean fanoutResolver,
      ClassName requestBuilderType) {

    /*
     * TODO In the following code, we are assuming that if the
     * resolver method is returning SingleExecute<T>, MultiExecute<T>, or just T,
     * the T is exactly matching the resolved inputs data type. If the developer
     * makes an error,
     * then the generated code will fail at runtime with ClassCastException. We need
     * to add a validation
     * here which proactively fails if the data type mismatches.
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
    boolean resolverReturnedDepCommand = false;
    if (util.isRawAssignable(returnType, DependencyCommand.class)) {
      resolverReturnedDepCommand = true;
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
          for (String depInputName : depInputNames) {
            methodCodeBuilder.addStatement(
                "$L.ifPresent(_r -> $L.$L(_r.$L()))",
                RESOLVER_RESULT,
                RESOLVER_REQUEST,
                depInputName,
                depInputName);
          }
        } else {
          if (depInputNames.size() != 1) {
            util.error(
                "Resolver method which resolves multiple dependency inputs must return a Request Builder object",
                resolverMethod);
          }
          String depFacetName = depInputNames.iterator().next();

          methodCodeBuilder.addStatement(
              "$L.ifPresent($L::$L)", RESOLVER_RESULT, RESOLVER_REQUEST, depFacetName);
        }
        methodCodeBuilder.endControlFlow();
      } else if (util.isRawAssignable(returnType, FanoutCommand.class)) {
        // TODO : add missing validations if any (??)
        if (!dependencyModel.canFanout()) {

          util.error(
              """
                  Dependency '%s' is not a fanout dependency, yet the resolver method returns a MultiExecute command.\
                  This is not allowed. Return a SingleExecute command, a single value, or mark the dependency as `canFanout = true`."""
                  .formatted(dependencyModel.name()),
              resolverMethod);
        }
        methodCodeBuilder.addStatement(
            "var $L = new $T()",
            RESOLVER_REQUESTS,
            ParameterizedTypeName.get(ClassName.get(ArrayList.class), requestBuilderType));
        if (util.isRawAssignable(actualReturnType, ImmutableRequest.Builder.class)) {
          if (depInputNames.size() <= 1) {
            util.error(
                "Resolver method that returns a request builder object must resolve multiple dependency inputs. Otherwise this can lead to unnecessary creation of request builder objects.",
                resolverMethod);
          }
        }
        methodCodeBuilder.beginControlFlow(
            "for($T $L: $L.inputs())", actualReturnType, RESOLVER_RESULT, RESOLVER_RESULTS);
        if (util.isRawAssignable(actualReturnType, ImmutableRequest.Builder.class)) {
          /*
           * TODO: Add validation that this vajramDef request is of the same type as the
           * request of the dependency Vajram
           */
          methodCodeBuilder.add("$L.add($L._newCopy()", RESOLVER_REQUESTS, RESOLVER_REQUEST);
          for (String depFacetName : depInputNames) {
            methodCodeBuilder.add(".$L($L.$L())", depFacetName, RESOLVER_RESULT, depFacetName);
          }
          methodCodeBuilder.add(");");
        } else {
          // TODO : Add validation for the type parameter of the MultiExecute to match the
          // type of
          // the input being resolved
          if (depInputNames.size() > 1) {
            util.error(
                "Resolver method which resolves multiple dependency inputs must return a %s object"
                    .formatted(ImmutableRequest.Builder.class),
                resolverMethod);
          }
          // Here we are assuming that the method is returning an MultiExecute of the type
          // of the
          // input being resolved. If this assumption is incorrect, the generated wrapper
          // class will
          // fail compilation.
          String depFacetName = depInputNames.iterator().next();
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
      for (String depFacetName : depInputNames) {
        methodCodeBuilder.addStatement(
            "$L.$L($L.$L())", RESOLVER_REQUEST, depFacetName, RESOLVER_RESULT, depFacetName);
      }
      methodCodeBuilder.endControlFlow();
    } else {
      methodCodeBuilder.beginControlFlow(
          "for($T $L: $L)", requestBuilderType, RESOLVER_REQUEST, RESOLVER_REQUESTS);
      if (depInputNames.size() != 1) {
        util.error(
            "Resolver method which resolves multiple dependency inputs must return a Request object",
            resolverMethod);
      }
      String depFacetName = depInputNames.iterator().next();
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
    if (resolverReturnedDepCommand) {
      // close the else block of "if($L.shouldSkip())"
      methodCodeBuilder.endControlFlow();
    }
  }

  private void modelsClassMembers(
      TypeSpec.Builder classSpec,
      ClassName enclosingClassType,
      List<? extends FacetGenModel> eligibleFacets,
      CodeGenParams codeGenParams) {
    if (codeGenParams.wrapsRequest() && codeGenParams.isRequest()) {
      throw new IllegalArgumentException("A request cannot wrap another request - this is a bug");
    }

    // TODO : Add checks for subsetRequest
    MethodSpec.Builder fullConstructor = constructorBuilder();
    ClassName immutRequestType = currentVajramInfo.conformsToTraitOrSelf().immutReqInterfaceType();
    if (codeGenParams.withImpl()) {
      classSpec.addAnnotations(
          codeGenParams.isBuilder()
              ? List.of(
                  AnnotationSpec.builder(ToString.class)
                      .addMember("doNotUseGetters", "true")
                      .build())
              : recordAnnotations());
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
            ParameterSpec.builder(
                    currentVajramInfo.conformsToTraitOrSelf().requestInterfaceType(), "_request")
                .addAnnotation(NonNull.class)
                .build());
        for (FacetGenModel facet : eligibleFacets) {
          if (facet.facetType().equals(INPUT)) {
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
      boolean isInput = facet.facetType().equals(INPUT);
      if (codeGenParams.withImpl()) {
        if (!isInput || !codeGenParams.wrapsRequest()) {
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

        boolean usePlatformDefault = util.usePlatformDefault(facet);

        if (codeGenParams.isBuilderRoot()) {
          String documentation = facet.documentation();
          setterBuilder.addJavadoc(
              "Sets the facet '$L'$L\n@param $L the value to set\n@return this builder for method chaining",
              facet.name(),
              documentation == null ? "\n" : ":\n<p>" + documentation,
              facet.name());
        }

        if (codeGenParams.withImpl()) {
          if (isInput && codeGenParams.wrapsRequest()) {
            // For facets that are wrapped in a request
            setterBuilder.addStatement("this._request.$L($L)", facet.name(), facet.name());
          } else {
            // For facets that are not wrapped in a request
            if (usePlatformDefault) {
              setterBuilder.addStatement(
                  "this.$L = $L != null ? $L : $T.$L.getPlatformDefaultValue()",
                  facet.name(),
                  facet.name(),
                  facet.name(),
                  getRequestInterfaceType(),
                  facet.name() + FACET_SPEC_SUFFIX);
            } else {
              setterBuilder.addStatement("this.$L = $L", facet.name(), facet.name());
            }
          }
          setterBuilder.addStatement("return this");
        } else {
          setterBuilder.addModifiers(ABSTRACT);
        }
        classSpec.addMethod(
            // Setter
            // public Builder inputName(Type inputName){this.inputName = inputName; return
            // this;}
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
                          .addStatement(
                              "this._request = $T._builder()",
                              currentVajramInfo.lite().immutReqPojoType())
                          .build()
                      : CodeBlock.builder().build())
              .build());
    }
    if (codeGenParams.isBuilder()
        && codeGenParams.withImpl()
        && !(codeGenParams.isRequest() || codeGenParams.isFacetsSubset())) {
      // Make sure Builder always has a constructor which accepts only the request
      // irrespective of
      // type of Facet class
      // See Vajram#facetsFromRequest
      classSpec.addMethod(
          constructorBuilder()
              .addParameter(
                  ParameterSpec.builder(
                          currentVajramInfo.conformsToTraitOrSelf().requestInterfaceType(),
                          "_request")
                      .build())
              .addStatement("this._request = _request._asBuilder()")
              .build());
    }
  }

  private void createFacetGetter(
      TypeSpec.Builder clazz, FacetGenModel facet, CodeGenParams codeGenParams) {
    MethodSpec.Builder method;
    FacetJavaType fieldType = util.getFacetFieldType(facet);
    FacetJavaType returnType = util.getFacetReturnType(facet, codeGenParams);
    // Non-dependency facet (Example: GivenFacet)
    method =
        methodBuilder(facet.name())
            .addModifiers(PUBLIC)
            .returns(
                returnType
                    .javaTypeName(facet)
                    .annotated(annotations(returnType.typeAnnotations(facet, codeGenParams))));

    if (codeGenParams.isModelRoot()) {
      String documentation = facet.documentation();
      if (documentation != null && !documentation.trim().isEmpty()) {
        method.addJavadoc("$L", documentation);
      }
      method.addAnnotations(
          facet.facetField().getAnnotationMirrors().stream()
              .filter(
                  annotationMirror ->
                      MODEL_METHOD_ANNOTATIONS.contains(
                          ((QualifiedNameable) annotationMirror.getAnnotationType().asElement())
                              .getQualifiedName()
                              .toString()))
              .map(AnnotationSpec::get)
              .toList());
    }

    if (codeGenParams.withImpl()) {
      method.addStatement(fieldType.fieldGetterCode(facet, codeGenParams));
    } else {
      method.addModifiers(ABSTRACT);
    }
    clazz.addMethod(method.build());
  }

  /**
   * Validates that none of the SerialId annotations on facets clash with the ReservedSerialIds
   * annotation on the Vajram.
   *
   * <p>This ensures that facets don't use serial IDs that are explicitly reserved for backward
   * compatibility reasons.
   */
  private void validateSerialIdReservations() {
    ReservedSerialIds reservedSerialIds =
        currentVajramInfo.vajramClass().getAnnotation(ReservedSerialIds.class);

    // If there's no ReservedSerialIds annotation, there's nothing to validate
    if (reservedSerialIds == null) {
      return;
    }

    // Get the set of reserved serial IDs
    Set<Integer> reservedIdSet =
        stream(reservedSerialIds.value()).boxed().collect(toImmutableSet());

    // Check each facet's SerialId annotation
    for (FacetGenModel facet : facetModelsByName.values()) {
      SerialId facetSerialId = facet.facetField().getAnnotation(SerialId.class);
      if (facetSerialId != null && reservedIdSet.contains(facetSerialId.value())) {
        util.error(
            String.format(
                "SerialId %d on facet '%s' in Vajram '%s' conflicts with a reserved ID. "
                    + "This ID is reserved via the @ReservedSerialIds annotation on the Vajram class.",
                facetSerialId.value(),
                facet.name(),
                currentVajramInfo.vajramClass().getSimpleName()),
            facet.facetField());
      }
    }
  }

  /**
   * Validates that the Strategy values in the IfAbsent annotation are only applied to facets with
   * data types that match the whitelisted types specified in the ApplicableToTypes annotation on
   * each enum value.
   */
  private void validateIfAbsentStrategyApplicability() {

    for (FacetGenModel facet : facetModelsByName.values()) {
      Element facetField = facet.facetField();
      IfAbsent ifAbsent = facetField.getAnnotation(IfAbsent.class);
      if (ifAbsent != null) {
        IfAbsentThen ifAbsentThen = ifAbsent.value();
        if (ifAbsentThen.usePlatformDefault()) {
          if (facet.facetType().equals(DEPENDENCY)) {
            util.error(
                "Defaulting to a platform default value is not supported for dependency facets.",
                facetField);
          }
        }
      }
    }
  }

  private ImmutableList<MethodSpec> facetContainerMethods(
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
                            .filter(f -> !f.facetType().equals(INPUT))
                            .map(FacetGenModel::name))
                    : eligibleFacets.stream().map(FacetGenModel::name))
            .collect(Collectors.joining(", "));
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
                                : "_batchable._build(), _batchKey._build()"))
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
                                : "_batchable._asBuilder(), _batchKey._asBuilder()")));
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
                                : "_batchable._newCopy(), _batchKey._newCopy()"))
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

  private void basicFacetsClasses() {
    boolean doInputsNeedBatching =
        currentVajramInfo.facetStream().anyMatch(FacetGenModel::isBatched);

    List<FacetGenModel> allFacets = currentVajramInfo.facetStream().toList();

    ClassName facetsType = getFacetsInterfaceType();
    ClassName immutableFacetsType = ClassName.get(packageName, getImmutFacetsClassName(vajramName));

    TypeSpec.Builder facetsInterface =
        util.interfaceBuilder(
                getFacetsInterfaceName(vajramName),
                currentVajramInfo.vajramClass().getQualifiedName().toString())
            .addModifiers(PUBLIC)
            .addSuperinterface(
                doInputsNeedBatching ? BatchEnabledFacetValues.class : FacetValues.class);

    // Add _vajramID() method to the facets interface
    facetsInterface.addMethod(
        overriding(util.getMethod(FacetValuesContainer.class, "_vajramID", 0))
            .addModifiers(DEFAULT)
            .addStatement("return $T." + VAJRAM_ID_CONSTANT_NAME, getRequestInterfaceType())
            .build());
    facetConstants(
        facetsInterface, currentVajramInfo.facetStream().toList(), CodeGenParams.builder().build());
    allFacets.forEach(
        facet ->
            createFacetGetter(
                facetsInterface, facet, CodeGenParams.builder().wrapsRequest(true).build()));

    TypeSpec.Builder immutFacetsClass =
        util.classBuilder(
                getImmutFacetsClassName(vajramName),
                currentVajramInfo.vajramClass().getQualifiedName().toString())
            .addModifiers(PUBLIC, FINAL)
            .addSuperinterface(facetsType)
            .addSuperinterface(
                doInputsNeedBatching
                    ? BatchEnabledImmutableFacetValues.class
                    : ImmutableFacetValues.class);
    modelsClassMembers(
        immutFacetsClass,
        immutableFacetsType,
        allFacets,
        CodeGenParams.builder().wrapsRequest(true).withImpl(true).build());

    TypeSpec.Builder facetsBuilderClass =
        util.classBuilder("Builder", currentVajramInfo.vajramClass().getQualifiedName().toString())
            .addModifiers(PUBLIC, STATIC, FINAL)
            .addSuperinterface(facetsType)
            .addSuperinterface(
                doInputsNeedBatching
                    ? BatchEnabledImmutableFacetValues.Builder.class
                    : FacetValuesBuilder.class);
    modelsClassMembers(
        facetsBuilderClass,
        immutableFacetsType,
        allFacets,
        CodeGenParams.builder().wrapsRequest(true).isBuilder(true).withImpl(true).build());
    if (doInputsNeedBatching) {
      codegenBatchableFacets(facetsInterface, CodeGenParams.builder().build());
      codegenBatchableFacets(immutFacetsClass, CodeGenParams.builder().withImpl(true).build());
      codegenBatchableFacets(facetsBuilderClass, CodeGenParams.builder().withImpl(true).build());
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
          packageName + '.' + getFacetsInterfaceName(vajramName),
          writer.toString(),
          currentVajramInfo.vajramClass());
    } catch (IOException e) {
      util.error(String.valueOf(e.getMessage()), currentVajramInfo.vajramClass());
    }
    try {
      StringWriter writer = new StringWriter();
      JavaFile.builder(
              packageName,
              immutFacetsClass
                  .addMethods(
                      facetContainerMethods(
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
                              facetContainerMethods(
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
          packageName + '.' + getImmutFacetsClassName(vajramName),
          writer.toString(),
          currentVajramInfo.vajramClass());
    } catch (IOException e) {
      util.error(String.valueOf(e.getMessage()), currentVajramInfo.vajramClass());
    }
  }

  private void batchFacetsClasses() {
    ClassName allFacetsType = getFacetsInterfaceType();
    ClassName batchItemsIfaceType = getBatchItemIfaceName();

    ClassName commonImmutFacetsType = getCommonFacetsClassName();

    List<FacetGenModel> batchedFacets =
        currentVajramInfo.facetStream().filter(FacetGenModel::isBatched).toList();
    List<FacetGenModel> commonFacets =
        currentVajramInfo.facetStream().filter(FacetGenModel::isUsedToGroupBatches).toList();

    TypeSpec.Builder batchItemsIface =
        util.interfaceBuilder(
                batchItemsIfaceType.simpleName(),
                currentVajramInfo.vajramClass().getQualifiedName().toString())
            .addSuperinterface(ImmutableFacetValuesContainer.class)
            .addSuperinterface(Model.class)
            .addAnnotation(
                AnnotationSpec.builder(ModelRoot.class)
                    .addMember("type", "$T.$L", ModelType.class, ModelType.NONE.name())
                    .addMember("suffixSeparator", "$S", "")
                    .build());

    for (FacetGenModel facet : batchedFacets) {
      CodeGenParams codeGenParams =
          CodeGenParams.builder().isSubsetBatch(true).wrapsFacets(true).build();
      FacetJavaType returnType = util.getFacetReturnType(facet, codeGenParams);
      batchItemsIface.addMethod(
          methodBuilder(facet.name())
              .addModifiers(PUBLIC, ABSTRACT)
              .addAnnotations(
                  facet.facetField().getAnnotationMirrors().stream()
                      .filter(
                          annotationMirror ->
                              IfAbsent.class
                                  .getCanonicalName()
                                  .equals(
                                      ((QualifiedNameable)
                                              annotationMirror.getAnnotationType().asElement())
                                          .getQualifiedName()
                                          .toString()))
                      .map(AnnotationSpec::get)
                      .toList())
              .returns(
                  returnType
                      .javaTypeName(facet)
                      .annotated(annotations(returnType.typeAnnotations(facet, codeGenParams))))
              .build());
    }
    batchItemsIface
        .addMethod(
            overriding(util.getMethod(FacetValuesContainer.class, "_facets", 0))
                .addModifiers(DEFAULT)
                .addStatement(
                    """
                        return $T._facets.stream()
                          .filter($T::isBatched)
                          .collect($T.toImmutableSet())
                        """,
                    getFacetsInterfaceType(),
                    FacetSpec.class,
                    ImmutableSet.class)
                .build())
        .addMethod(
            overriding(util.getMethod(FacetValuesContainer.class, "_vajramID", 0))
                .addModifiers(DEFAULT)
                .addStatement("return $T." + VAJRAM_ID_CONSTANT_NAME, getRequestInterfaceType())
                .build())
        .addMethod(
            methodBuilder("_pojoBuilder")
                .addModifiers(PUBLIC, STATIC)
                .returns(
                    ClassName.get(
                        packageName, getBatchItemIfaceName().simpleName() + "ImmutPojo", "Builder"))
                .addStatement(
                    "return $T._builder()",
                    ClassName.get(packageName, getBatchItemIfaceName().simpleName() + "ImmutPojo"))
                .build())
        .addMethod(
            methodBuilder("_fromFacets")
                .addModifiers(PUBLIC, STATIC)
                .returns(batchItemsIfaceType)
                .addParameter(allFacetsType, FACET_VALUES_VAR)
                .addCode("return _pojoBuilder()")
                .addCode(
                    "$L",
                    batchedFacets.stream()
                        .map(
                            f -> CodeBlock.of(".$L($L.$L())", f.name(), FACET_VALUES_VAR, f.name()))
                        .collect(CodeBlock.joining("")))
                .addCode("._build();")
                .build());

    TypeSpec.Builder commonImmutFacetsClass =
        util.classBuilder(
                commonImmutFacetsType.simpleName(),
                currentVajramInfo.vajramClass().getQualifiedName().toString())
            .addModifiers(FINAL)
            .addSuperinterface(ImmutableFacetValuesContainer.class)
            .addField(allFacetsType, FACET_VALUES_VAR, PRIVATE, FINAL)
            .addMethod(
                constructorBuilder()
                    .addParameter(allFacetsType, FACET_VALUES_VAR)
                    .addStatement("this.$L = $L._build()", FACET_VALUES_VAR, FACET_VALUES_VAR)
                    .build())
            .addAnnotation(
                AnnotationSpec.builder(EqualsAndHashCode.class)
                    .addMember("onlyExplicitlyIncluded", "true")
                    .build())
            .addMethod(
                overriding(util.getMethod(FacetValuesContainer.class, "_facets", 0))
                    .addStatement(
                        """
                        return $T._facets.stream()
                          .filter(spec -> !spec.isBatched())
                          .collect($T.toImmutableSet())
                        """,
                        getFacetsInterfaceType(),
                        ImmutableSet.class)
                    .build())
            .addMethod(
                overriding(util.getMethod(FacetValuesContainer.class, "_vajramID", 0))
                    .addStatement("return $T." + VAJRAM_ID_CONSTANT_NAME, getRequestInterfaceType())
                    .build());

    for (FacetGenModel facet : commonFacets) {
      CodeGenParams codeGenParams =
          CodeGenParams.builder().isSubsetCommon(true).withImpl(true).build();
      FacetJavaType returnType = util.getFacetReturnType(facet, codeGenParams);
      MethodSpec.Builder getter =
          methodBuilder(facet.name())
              .returns(
                  returnType
                      .javaTypeName(facet)
                      .annotated(annotations(returnType.typeAnnotations(facet, codeGenParams))))
              .addStatement(returnType.fieldGetterCode(facet, codeGenParams));
      if (facet.isGiven()) {
        getter.addAnnotation(Include.class);
      }
      commonImmutFacetsClass.addMethod(getter.build());
    }

    util.generateSourceFile(
        batchItemsIfaceType.canonicalName(),
        JavaFile.builder(packageName, batchItemsIface.build()).build().toString(),
        currentVajramInfo.vajramClass());
    util.generateSourceFile(
        commonImmutFacetsType.canonicalName(),
        JavaFile.builder(packageName, commonImmutFacetsClass.build()).build().toString(),
        currentVajramInfo.vajramClass());
  }

  private ClassName getFacetsInterfaceType() {
    return ClassName.get(packageName, getFacetsInterfaceName(vajramName));
  }

  private ClassName getRequestInterfaceType() {
    return ClassName.get(packageName, getRequestInterfaceName(vajramName));
  }

  private ClassName getBatchItemIfaceName() {
    return ClassName.get(packageName, vajramName + BATCH_ITEM_FACETS_SUFFIX);
  }

  private ClassName getCommonFacetsClassName() {
    return ClassName.get(packageName, vajramName + BATCH_KEY_FACETS_SUFFIX);
  }

  private void codegenBatchableFacets(TypeSpec.Builder allFacetsType, CodeGenParams codeGenParams) {
    MethodSpec.Builder batchElementMethod =
        overriding(util.getMethod(BatchEnabledFacetValues.class, "_batchItem", 0))
            .returns(getBatchItemIfaceName());
    MethodSpec.Builder commonMethod =
        overriding(util.getMethod(BatchEnabledFacetValues.class, BATCH_KEY_NAME, 0))
            .returns(getCommonFacetsClassName());
    if (codeGenParams.withImpl()) {
      batchElementMethod.addStatement("return $T._fromFacets(this)", getBatchItemIfaceName());
      commonMethod.addStatement("return new $T(this)", getCommonFacetsClassName());
    } else {
      batchElementMethod.addModifiers(ABSTRACT);
      commonMethod.addModifiers(ABSTRACT);
    }
    allFacetsType.addMethod(batchElementMethod.build()).addMethod(commonMethod.build());
  }

  private void facetConstants(
      TypeSpec.Builder classBuilder,
      List<? extends FacetGenModel> facetValues,
      CodeGenParams codeGenParams) {
    List<FieldSpec> specFields = new ArrayList<>(facetValues.size());
    List<FieldSpec> idFields = new ArrayList<>();
    int facetCount = 0;
    for (FacetGenModel facet : facetValues) {
      facetCount++;
      String facetDoc = facet.documentation();
      FieldSpec facetIdField =
          FieldSpec.builder(String.class, facet.name() + FACET_NAME_SUFFIX)
              .addModifiers(PUBLIC, STATIC, FINAL)
              .initializer("$S", vajramName + QUALIFIED_FACET_SEPARATOR + facet.name())
              .addJavadoc(facetDoc != null ? CodeBlock.of(facetDoc) : EMPTY_CODE_BLOCK)
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
                          ? vajramDepDef.isMandatoryOnServer()
                              ? MandatoryFanoutDepSpec.class
                              : OptionalFanoutDepSpec.class
                          : vajramDepDef.isMandatoryOnServer()
                              ? MandatoryOne2OneDepSpec.class
                              : OptionalOne2OneDepSpec.class
                      : facet.isMandatoryOnServer()
                          ? MandatoryFacetDefaultSpec.class
                          : OptionalFacetDefaultSpec.class);
      List<TypeName> collectClassNames = new ArrayList<>();
      FieldSpec.Builder fieldSpec =
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
                      .build());

      CodeBlock.Builder initializerCodeBlock = CodeBlock.builder();
      initializerCodeBlock
          .add(
              """
                  new $T<>(
                    $L,
                    $S,
                    $T.$L,
                  """,
              specType,
              facet.id(),
              facet.name(),
              getRequestInterfaceType(),
              VAJRAM_ID_CONSTANT_NAME)
          .add(
              util.getJavaTypeCreationCode(facet.dataType(), collectClassNames) + ",",
              collectClassNames.toArray());
      if (facet instanceof DefaultFacetModel && !codeGenParams.isRequest()) {
        if (!DEPENDENCY.equals(facet.facetType())) {
          initializerCodeBlock.add("$T.$L,", FacetType.class, facet.facetType().name());
        }
      }
      initializerCodeBlock.add("""
                $T.class,
              """, vajramReqClass);
      if (facet instanceof DependencyModel vajramDepDef) {
        ClassName depReqClass = ClassName.bestGuess(vajramDepDef.depReqClassQualifiedName());
        ClassName depReqInterfaceClass =
            ClassName.get(
                vajramDepDef.depReqPackageName(),
                getRequestInterfaceName(vajramDepDef.depVajramInfo().vajramId().id()));
        initializerCodeBlock.add(
            """
                  $T.class,
                  $T.$L,
                """,
            depReqClass,
            depReqInterfaceClass,
            VAJRAM_ID_CONSTANT_NAME);
      }
      String docComment = util.processingEnv().getElementUtils().getDocComment(facet.facetField());
      if (docComment == null) {
        docComment = "";
      }
      initializerCodeBlock.add("$S,", docComment);
      if (codeGenParams.isRequest()) {
        VajramInfoLite conformsToTraitInfoOrSelf = currentVajramInfo.conformsToTraitOrSelf();
        initializerCodeBlock.add(
            """
                  $T.fieldTagsParser(() -> $T.class.getDeclaredField($S)),
                  _request -> (($T)_request).$L(),
                  (_request, _value) -> {
                    if(_value != null) {
                      (($T) _request).$L(_value);
                    }
                  }
                )
                """,
            FacetUtils.class,
            getRequestInterfaceType(),
            facet.name() + FACET_SPEC_SUFFIX,
            conformsToTraitInfoOrSelf.requestInterfaceType(),
            facet.name(),
            conformsToTraitInfoOrSelf.immutReqInterfaceType().nestedClass("Builder"),
            facet.name());
        fieldSpec.addAnnotations(
            facet.facetField().getAnnotationMirrors().stream()
                .filter(
                    annotationMirror ->
                        annotationMirror
                                .getAnnotationType()
                                .asElement()
                                .getAnnotation(Documented.class)
                            != null)
                .map(AnnotationSpec::get)
                .toList());
      } else {
        initializerCodeBlock.add(
            """
                    $L,
                    $T.fieldTagsParser(
                      () -> $T.$L.class.getDeclaredField($S),
                      () -> $T.class.getDeclaredField($S)),
                    _facetValues -> (($T)_facetValues).$L(),
                    (_facetValues, _value) -> {
                      if(_value != null) {
                        (($T) _facetValues).$L(_value);
                      }
                    }
                  )
                """,
            facet.facetField().getAnnotation(Batched.class) != null,
            FacetUtils.class,
            currentVajramInfo.vajramClass(),
            facet.facetType().equals(INPUT) ? _INPUTS_CLASS : _INTERNAL_FACETS_CLASS,
            facet.name(),
            getFacetsInterfaceType(),
            facet.name() + FACET_SPEC_SUFFIX,
            ClassName.get(packageName, getFacetsInterfaceName(vajramName)),
            facet.name(),
            ClassName.get(packageName, getImmutFacetsClassName(vajramName), "Builder"),
            facet.name());
        if (facet instanceof DependencyModel dep && dep.depVajramInfo().isTrait()) {
          fieldSpec.addAnnotation(TraitDependency.class);
        }
      }
      specFields.add(fieldSpec.initializer(initializerCodeBlock.build()).build());
    }
    for (int i = 0; i < facetCount; i++) {
      classBuilder.addField(idFields.get(i)).addField(specFields.get(i));
    }

    ParameterizedTypeName facetsFieldType =
        ParameterizedTypeName.get(
            ClassName.get(ImmutableSet.class),
            WildcardTypeName.subtypeOf(
                ParameterizedTypeName.get(
                    codeGenParams.isRequest()
                        ? ClassName.get(InputMirrorSpec.class)
                        : ClassName.get(FacetSpec.class),
                    WildcardTypeName.subtypeOf(Object.class),
                    WildcardTypeName.subtypeOf(getRequestInterfaceType()))));
    FieldSpec facetsField =
        FieldSpec.builder(facetsFieldType, "_facets", PUBLIC, STATIC, FINAL)
            .initializer(
                specFields.stream()
                    .map(specField -> "$N")
                    .collect(Collectors.joining(", ", "$T.of(", ")")),
                Stream.concat(Stream.of(ImmutableSet.class), specFields.stream()).toArray())
            .build();
    classBuilder.addField(facetsField);

    classBuilder.addMethod(
        overriding(util.getMethod(FacetValuesContainer.class, "_facets", 0))
            .addModifiers(PUBLIC, DEFAULT)
            .returns(facetsFieldType)
            .addStatement("return $N", facetsField)
            .build());
  }

  ImmutableSet<Integer> getResolverSources(ExecutableElement resolve) {
    return resolve.getParameters().stream().map(this::inferFacetId).collect(toImmutableSet());
  }
}
