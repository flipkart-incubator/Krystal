package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.flipkart.krystal.vajram.codegen.Constants.ARRAY_LIST;
import static com.flipkart.krystal.vajram.codegen.Constants.BATCHES_VAR;
import static com.flipkart.krystal.vajram.codegen.Constants.BATCHING_EXECUTE_PREPARE_PARAMS;
import static com.flipkart.krystal.vajram.codegen.Constants.BATCHING_EXECUTE_PREPARE_RESULTS;
import static com.flipkart.krystal.vajram.codegen.Constants.BATCH_FACETS;
import static com.flipkart.krystal.vajram.codegen.Constants.BATCH_IMMUT_FACETS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.COMMON_INPUTS;
import static com.flipkart.krystal.vajram.codegen.Constants.COMMON_IMMUT_FACETS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.COMMON_INPUT;
import static com.flipkart.krystal.vajram.codegen.Constants.COM_FUTURE;
import static com.flipkart.krystal.vajram.codegen.Constants.FACETS_VAR;
import static com.flipkart.krystal.vajram.codegen.Constants.FACETS_FIELDS_VAR;
import static com.flipkart.krystal.vajram.codegen.Constants.FACET_DEFINITIONS_VAR;
import static com.flipkart.krystal.vajram.codegen.Constants.FACET_ID_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.FACET_SPEC_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.FUNCTION;
import static com.flipkart.krystal.vajram.codegen.Constants.GET_FACET_DEFINITIONS;
import static com.flipkart.krystal.vajram.codegen.Constants.GET_INPUT_RESOLVERS;
import static com.flipkart.krystal.vajram.codegen.Constants.GET_SIMPLE_INPUT_RESOLVERS;
import static com.flipkart.krystal.vajram.codegen.Constants.HASH_MAP;
import static com.flipkart.krystal.vajram.codegen.Constants.IM_LIST;
import static com.flipkart.krystal.vajram.codegen.Constants.IM_MAP;
import static com.flipkart.krystal.vajram.codegen.Constants.INCOMING_FACETS;
import static com.flipkart.krystal.vajram.codegen.Constants.FACETS_LIST;
import static com.flipkart.krystal.vajram.codegen.Constants.INPUT_BATCHING;
import static com.flipkart.krystal.vajram.codegen.Constants.INPUT_SRC;
import static com.flipkart.krystal.vajram.codegen.Constants.LINK_HASH_MAP;
import static com.flipkart.krystal.vajram.codegen.Constants.LIST;
import static com.flipkart.krystal.vajram.codegen.Constants.MAP;
import static com.flipkart.krystal.vajram.codegen.Constants.METHOD_EXECUTE;
import static com.flipkart.krystal.vajram.codegen.Constants.METHOD_EXECUTE_COMPUTE;
import static com.flipkart.krystal.vajram.codegen.Constants.METHOD_GET_FACETS_CONVERTOR;
import static com.flipkart.krystal.vajram.codegen.Constants.MOD_INPUT;
import static com.flipkart.krystal.vajram.codegen.Constants.OPTIONAL;
import static com.flipkart.krystal.vajram.codegen.Constants.RESOLVER_REQUEST;
import static com.flipkart.krystal.vajram.codegen.Constants.RESOLVER_REQUESTS;
import static com.flipkart.krystal.vajram.codegen.Constants.RESOLVER_RESULT;
import static com.flipkart.krystal.vajram.codegen.Constants.RESOLVER_RESULTS;
import static com.flipkart.krystal.vajram.codegen.Constants.RETURN_TYPE;
import static com.flipkart.krystal.vajram.codegen.Constants.UNMOD_INPUT;
import static com.flipkart.krystal.vajram.codegen.Constants.VAJRAM_LOGIC_METHOD;
import static com.flipkart.krystal.vajram.codegen.Constants.VAL_ERR;
import static com.flipkart.krystal.vajram.codegen.Constants._FACETS_CLASS;
import static com.flipkart.krystal.vajram.codegen.Utils.getFacetsInterfaceName;
import static com.flipkart.krystal.vajram.codegen.Utils.getImmutFacetsClassname;
import static com.flipkart.krystal.vajram.codegen.Utils.getImmutRequestClassName;
import static com.flipkart.krystal.vajram.codegen.Utils.getRequestInterfaceName;
import static com.flipkart.krystal.vajram.codegen.Utils.getTypeParameters;
import static com.flipkart.krystal.vajram.codegen.Utils.getVajramImplClassName;
import static com.flipkart.krystal.vajram.codegen.models.ParsedVajramData.fromVajram;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.data.DependencyResponses;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.FacetsBuilder;
import com.flipkart.krystal.data.ImmutableFacets;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.resolution.ResolverCommand;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.batching.BatchableFacets;
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
import com.flipkart.krystal.vajram.facets.FacetIdNameMapping;
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
import com.flipkart.krystal.vajram.facets.resolution.AbstractFanoutInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.AbstractSingleInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.FanoutInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.resolution.SingleInputResolver;
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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
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
  private final VajramInfo vajramInfo;
  private final String vajramName;
  private final Map<VajramID, VajramInfoLite> vajramDefs;
  private final Map<Integer, FacetGenModel> facetModels;
  private final Map<String, FacetGenModel> facetModelsByName;
  private final boolean needsBatching;
  private final Map<Integer, Boolean> depFanoutMap;
  private @MonotonicNonNull ParsedVajramData parsedVajramData;
  private final Utils util;

  public VajramCodeGenerator(
      VajramInfo vajramInfo, ProcessingEnvironment processingEnv, Utils util) {
    this.vajramInfo = vajramInfo;
    this.vajramName = vajramInfo.vajramId().vajramId();
    this.packageName = vajramInfo.packageName();
    this.processingEnv = processingEnv;
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
                    LinkedHashMap::new)); // need ordered map for dependencies
    this.facetModelsByName =
        vajramInfo
            .facetStream()
            .collect(
                toMap(
                    FacetGenModel::name,
                    Function.identity(),
                    (o1, o2) -> o1,
                    LinkedHashMap::new)); // need ordered map for dependencies
    this.needsBatching = vajramInfo.inputs().stream().anyMatch(InputModel::isBatched);
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
   * Infer facet name provided through @Using annotation. If @Using annotation is not present, then
   * infer facet name from the parameter name
   *
   * @param parameter the bind parameter in the resolver method
   * @return facet name in the form of String
   */
  //  public String inferFacetName(VariableElement parameter) {
  //    String usingInputName;
  //    if (Objects.nonNull(parameter.getAnnotation(Using.class))) {
  //      usingInputName =
  //          Optional.ofNullable(parameter.getAnnotation(Using.class).value()).orElseThrow();
  //    } else {
  //      String paramName = parameter.getSimpleName().toString();
  //      FacetGenModel facetGenModel = facetModelsByName.get(paramName);
  //      if (facetGenModel == null) {
  //        String message = "Unknown facet with name %s".formatted(paramName);
  //        util.error(message, parameter);
  //        throw new VajramValidationException(message);
  //      }
  //      usingInputName = facetGenModel.name();
  //    }
  //
  //    return usingInputName;
  //  }

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
      int dependencyId = checkNotNull(resolver.getAnnotation(Resolve.class)).dep();
      resolverMap.computeIfAbsent(dependencyId, _k -> new ArrayList<>()).add(resolver);
    }

    // Initialize few common attributes and data structures
    final ClassName inputBatch =
        ClassName.get(getParsedVajramData().packageName(), getImmutFacetsClassname(vajramName))
            .nestedClass(BATCH_IMMUT_FACETS_CLASS_SUFFIX);
    final ClassName commonInputs =
        ClassName.get(getParsedVajramData().packageName(), getImmutFacetsClassname(vajramName))
            .nestedClass(COMMON_IMMUT_FACETS_CLASS_SUFFIX);
    final TypeName vajramResponseType =
        util.toTypeName(getParsedVajramData().vajramInfo().responseType());

    methodSpecs.add(createFacetDefinitions());
    methodSpecs.add(createInputResolvers());
    //    createResolvers(resolverMap, depFanoutMap).ifPresent(methodSpecs::add);

    if (util.isRawAssignable(
        getParsedVajramData().vajramInfo().vajramClass().asType(), IOVajram.class)) {
      methodSpecs.add(
          createIOVajramExecuteMethod(
              inputBatch,
              commonInputs,
              vajramResponseType.box().annotated(AnnotationSpec.builder(Nullable.class).build())));
      if (needsBatching) {
        methodSpecs.add(createBatchFacetConvertersMethod(inputBatch, commonInputs));
      } else {
        methodSpecs.add(createBatchFacetConvertersMethodException());
      }
    } else {
      methodSpecs.add(createComputeVajramExecuteMethod(vajramResponseType));
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
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return writer.toString();
  }

  private MethodSpec createInputResolvers() {
    Builder getInputResolversMethod =
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
          .computeIfAbsent((resolver.getAnnotation(Resolve.class)).dep(), _k -> new ArrayList<>())
          .add(resolver);
    }
    resolverMap.forEach(
        (depName, resolverMethods) -> {
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
            boolean fanoutResolver = canFanout(resolverMethod);

            int[] resolvedInputs = resolve.depInputs();
            TypeElement inputResolverInterface;
            if (fanoutResolver) {
              inputResolverInterface =
                  checkNotNull(
                      util.getProcessingEnv()
                          .getElementUtils()
                          .getTypeElement(FanoutInputResolver.class.getName()));

            } else {
              inputResolverInterface =
                  checkNotNull(
                      util.getProcessingEnv()
                          .getElementUtils()
                          .getTypeElement(SingleInputResolver.class.getName()));
            }
            Builder resolveMethod =
                MethodSpec.overriding(
                        inputResolverInterface.getEnclosedElements().stream()
                            .filter(element -> element instanceof ExecutableElement)
                            .map(element -> (ExecutableElement) element)
                            .filter(element -> element.getSimpleName().contentEquals("resolve"))
                            .findFirst()
                            .orElseThrow(),
                        (DeclaredType) inputResolverInterface.asType(),
                        util.getProcessingEnv().getTypeUtils())
                    .addCode(buildInputResolver(resolverMethod, fanoutResolver).build());
            resolveMethodToObjConvCode.add(
                ".add($L)",
                anonymousClassBuilder(
                        "$T.of($L), new $T($L, $T.$L($S), $T.of($L))",
                        ImmutableSet.class,
                        resolverMethod.getParameters().stream()
                            .map(variableElement -> inferFacetId(variableElement))
                            .map(String::valueOf)
                            .collect(joining(", ")),
                        QualifiedInputs.class,
                        dep.id(),
                        VajramID.class,
                        "vajramID",
                        dep.depVajramInfoLite().vajramId().vajramId(),
                        ImmutableSet.class,
                        stream(resolvedInputs).mapToObj(String::valueOf).collect(joining(", ")))
                    .addSuperinterface(
                        fanoutResolver
                            ? AbstractFanoutInputResolver.class
                            : AbstractSingleInputResolver.class)
                    .addMethod(resolveMethod.build())
                    .build());
          }
        });
    resolveMethodToObjConvCode.add(".build()");

    return getInputResolversMethod.addStatement(resolveMethodToObjConvCode.build()).build();
  }

  private boolean canFanout(ExecutableElement resolverMethod) {
    return util.isRawAssignable(resolverMethod.getReturnType(), MultiExecute.class);
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

  private ImmutableSet<Integer> getResolverSources(ExecutableElement resolve) {
    return resolve.getParameters().stream().map(v -> inferFacetId(v)).collect(toImmutableSet());
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
            .addParameter(ParameterizedTypeName.get(ImmutableList.class, Facets.class), FACETS_LIST)
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
                return _facetsList.stream().collect(
                     $T.toImmutableMap($T.identity(),
                     $L -> {
                        $T $L = ($T)$L;
                """,
                ImmutableMap.class,
                Function.class,
                INCOMING_FACETS,
                getFacetsInterfaceType(),
                FACETS_VAR,
                getFacetsInterfaceType(),
                INCOMING_FACETS);
    ExecutableElement outputLogic = getParsedVajramData().outputLogic();

    outputLogic
        .getParameters()
        .forEach(
            parameter -> {
              generateFacetLocalVariable(outputLogic, parameter, returnBuilder);
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
    // merge the code blocks for facets
    //    for (int i = 0; i < inputCodeBlocks.size(); i++) {
    //      // for formatting
    //      returnBuilder.add("\t\t");
    //      returnBuilder.add(inputCodeBlocks.get(i));
    //      if (i != inputCodeBlocks.size() - 1) {
    //        returnBuilder.add(",\n");
    //      }
    //    }

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

  private MethodSpec createBatchFacetConvertersMethodException() {
    Builder inputConvertersBuilder =
        methodBuilder(METHOD_GET_FACETS_CONVERTOR)
            .addModifiers(PUBLIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(BatchableSupplier.class),
                    TypeName.get(Facets.class),
                    TypeName.get(Facets.class)))
            .addAnnotation(Override.class);
    inputConvertersBuilder.addCode(
        CodeBlock.builder()
            .addStatement(
                "throw new $T(\"This IO Vajram does not have any Batch Facets\")",
                RuntimeException.class)
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
            .addParameter(ParameterizedTypeName.get(ImmutableList.class, Facets.class), FACETS_LIST)
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
      valueMap.put("facets", ClassName.get(Facets.class));
      valueMap.put("facetsList", FACETS_LIST);
      valueMap.put("facetsVar", FACETS_VAR);
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
      // Create outputLogic input arguments mapping
      // TODO : add type check/assignment validations
      StringBuilder outputLogicInputArgs = new StringBuilder();
      //      boolean isBatchedFacetsInput = false;
      for (VariableElement param : outputLogic.getParameters()) {
        //        if (param.getSimpleName().contentEquals("_batchedFacets")) {
        //          if (outputLogic.getParameters().size() > 1) {
        //            String message = "BatchedFacets input can only be used as single input";
        //            util.error(message, param);
        //            throw new VajramValidationException(message);
        //          }
        //          outputLogicInputArgs.append(
        //              "new $modInput:T<>(ImmutableList.copyOf(mapping.keySet()), commonFacets)");
        //          isBatchedFacetsInput = true;
        //        } else
        if (param.getSimpleName().contentEquals(BATCHES_VAR)) {
          TypeMirror paramType = param.asType();
          List<? extends TypeMirror> batchTypeParams = getTypeParameters(paramType);
          TypeMirror expected =
              processingEnv
                  .getElementUtils()
                  .getTypeElement(getPackageName() + "." + getBatchFacetsInterfaceName())
                  .asType();
          TypeMirror actual = batchTypeParams.get(0);
          if (!util.isRawAssignable(paramType, ImmutableList.class)
              || batchTypeParams.size() != 1
              || !Objects.equals(expected, actual)) {
            throw util.errorAndThrow(
                "Batch of facets param must be of ImmutableList of type "
                    + expected
                    + ". Found: "
                    + actual,
                param);
          }
          outputLogicInputArgs.append("ImmutableList.copyOf(_batches.keySet()), ");
        } else {
          generateFacetLocalVariable(outputLogic, param, codeBuilder);
          FacetGenModel facetGenModel = facetModels.get(inferFacetId(param));
          //          if (facetGenModel == null) {
          //            throw new AssertionError();
          //          }
          //          if (facetGenModel.isBatched()) {
          //            String message =
          //                "Cannot use batch facet - %s - as direct input param for output logic"
          //                    .formatted(facetGenModel.name());
          //            util.error(message, param);
          //            throw new VajramValidationException(message);
          //          } else {
          outputLogicInputArgs.append("commonFacets." + facetGenModel.name() + "()").append(", ");
          //          }
        }
      }
      //      if (!isBatchedFacetsInput) {
      outputLogicInputArgs.delete(outputLogicInputArgs.length() - 2, outputLogicInputArgs.length());
      //      }
      //      valueMap.put(VAJRAM_LOGIC_INPUT_ARGS, outputLogicInputArgs.toString());
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
      codeBuilder.addNamed(BATCHING_EXECUTE_PREPARE_PARAMS, valueMap);
      codeBuilder.add("var _output = $L(", outputLogic.getSimpleName());
      codeBuilder.add(
          outputLogicParamsCode.stream().collect(joining(", ")),
          outputLogicParamsCodeArgs.toArray());
      codeBuilder.add(");");
      codeBuilder.addNamed(BATCHING_EXECUTE_PREPARE_RESULTS, valueMap);
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
   * @return generated code for "resolveInputOfDependency" {@link MethodSpec}
   */
  //  private Optional<MethodSpec> createResolvers(
  //      Map</*depId*/ Integer, List<ExecutableElement>> resolverMap,
  //      Map</*depId*/ Integer, Boolean> depFanoutMap) {
  //    String resolverIdVarName = "resolverId";
  //    MethodSpec.Builder resolveInputsBuilder =
  //        methodBuilder(METHOD_RESOLVE_INPUT_OF_DEPENDENCY)
  //            .addModifiers(PUBLIC)
  //            .addParameter(int.class, resolverIdVarName)
  //            .addParameter(getFacetsInterfaceType(), FACETS)
  //            .addParameter(Request.class, DEP_REQ_PARAM)
  //            .returns(
  //                ParameterizedTypeName.get(
  //                    ClassName.get(DependencyCommand.class), ClassName.get(Facets.class)));
  //        if (Objects.nonNull(getParsedVajramData())) {
  //          resolveInputsBuilder.beginControlFlow("switch ($L) ", resolverIdVarName);
  //          if (getParsedVajramData().resolvers().isEmpty()) {
  //            return Optional.empty();
  //          }
  //          record ResolverDetails(QualifiedInputs qualifiedInputs, ExecutableElement
  //     resolverMethod) {}
  //          TreeSet<ResolverDetails> resolverDetails =
  //              new TreeSet<>(
  //                  Comparator.comparing(
  //                      ResolverDetails::qualifiedInputs, getQualifiedInputsComparator()));
  //          resolverMap.forEach(
  //              (depId, methods) ->
  //                  methods.forEach(
  //                      method -> {
  //                        resolverDetails.add(
  //                            new ResolverDetails(
  //                                new QualifiedInputs(
  //                                    depId,
  //                                    method.getParameters().stream()
  //                                        .map(p -> p.getSimpleName().toString())
  //                                        .collect(toImmutableSet())),
  //                                method));
  //                      }));
  //
  //          int resolverId = 1;
  //          for (ResolverDetails resolverDetail : resolverDetails) {
  //            AtomicBoolean fanout = new AtomicBoolean(false);
  //            // TODO : confirm if this logic is correct for all parameters for a resolve method
  //            resolverDetail
  //                .resolverMethod()
  //                .getParameters()
  //                .forEach(
  //                    parameter -> {
  //                      String facetName = inferFacetName(parameter);
  //                      if (!fanout.get()
  //                          && depFanoutMap.containsKey(
  //                              facetName)) { // if fanout is already set skip resetting it.
  //                        fanout.set(depFanoutMap.getOrDefault(facetName, false));
  //                      }
  //                      // validating if the bind parameter has a resolver binding or defined as
  //                      // inputDef
  //                      if (!(facetModels.containsKey(facetName))) {
  //                        throw util.errorAndThrow(
  //                            "Parameter binding incorrect for inputDef - " + facetName,
  // parameter);
  //                      }
  //                    });
  //            CodeBlock.Builder caseBuilder =
  //                CodeBlock.builder().beginControlFlow("case $L -> ", resolverId++);
  //            caseBuilder.add(
  //                buildInputResolver(resolverDetail.resolverMethod(), depFanoutMap,
  // fanout.get())
  //                    .build());
  //            caseBuilder.endControlFlow();
  //            resolveInputsBuilder.addCode(caseBuilder.build());
  //          }
  //          resolveInputsBuilder.endControlFlow();
  //          resolveInputsBuilder.addStatement(
  //              "throw new $T($S)",
  //              ClassName.get(VajramValidationException.class),
  //              "Unresolvable dependencyDef");
  //        } else {
  //          resolveInputsBuilder.addStatement(
  //              "throw new $T($S)",
  //              ClassName.get(VajramValidationException.class),
  //              "Unresolvable dependencyDef");
  //        }
  //        return Optional.of(resolveInputsBuilder.build());

  //    return Optional.of(resolveInputsBuilder.addStatement("return null").build());
  //  }

  /**
   * Method to generate resolver code for inputDef binding
   *
   * @param method Vajram resolve method
   * @param fanoutResolver
   * @return {@link CodeBlock.Builder} with resolver code
   */
  private CodeBlock.Builder buildInputResolver(ExecutableElement method, boolean fanoutResolver) {
    Resolve resolve =
        checkNotNull(
            method.getAnnotation(Resolve.class), "Resolver method must have 'Resolve' annotation");
    int[] facets = resolve.depInputs();
    int depId = resolve.dep();
    FacetGenModel facetGenModel = checkNotNull(facetModels.get(depId));
    String facetName = facetGenModel.name();

    if (!(facetGenModel instanceof DependencyModel dependencyModel)) {
      String message = "No facet definition found for " + facetName;
      util.error(message, method);
      throw new VajramValidationException(message);
    }
    VajramID depVajramName = dependencyModel.depVajramInfoLite().vajramId();
    String depRequestPackage = dependencyModel.depReqPackageName();

    // check if the inputDef is satisfied by inputDef or other resolved variables
    ClassName requestBuilderType =
        ClassName.get(depRequestPackage, getImmutRequestClassName(depVajramName.vajramId()))
            .nestedClass("Builder");
    CodeBlock.Builder codeBuilder;
    if (fanoutResolver) {
      codeBuilder =
          CodeBlock.builder()
              .addStatement("$1T $2L = (($1T)arg0)", requestBuilderType, RESOLVER_REQUEST);
    } else {
      codeBuilder =
          CodeBlock.builder()
              .addStatement(
                  "$1T $2L = (($1T)arg0)",
                  ParameterizedTypeName.get(ClassName.get(ImmutableList.class), requestBuilderType),
                  RESOLVER_REQUESTS);
    }
    codeBuilder.addStatement(
        "$T $L = (($T)arg1)", getFacetsInterfaceType(), FACETS_VAR, getFacetsInterfaceType());
    // TODO : add validation if fanout, then method should accept dependencyDef response for the
    // bind type parameter else error
    // Iterate over the method params and call respective binding methods
    method
        .getParameters()
        .forEach(
            parameter -> {
              generateFacetLocalVariable(method, parameter, codeBuilder);
            });

    boolean isFanOutDep = depFanoutMap.getOrDefault(depId, false);
    buildFinalResolvers(
        method,
        facets,
        codeBuilder,
        facetName,
        isFanOutDep,
        fanoutResolver,
        depVajramName,
        requestBuilderType);
    return codeBuilder;
  }

  private void generateFacetLocalVariable(
      ExecutableElement method, VariableElement parameter, CodeBlock.Builder codeBuilder) {
    final int usingFacetId = inferFacetId(parameter);
    // check if the bind param has multiple resolvers
    FacetGenModel usingFacetModel = facetModels.get(usingFacetId);
    if (usingFacetModel instanceof DependencyModel) {
      generateDependencyResolutions(method, usingFacetId, codeBuilder, parameter);
    } else if (usingFacetModel instanceof InputModel<?> inputModel) {
      TypeMirror facetType = util.toTypeMirror(inputModel.dataType());
      String variable = toJavaName(usingFacetModel.name());
      TypeMirror parameterTypeMirror = parameter.asType();
      final TypeName parameterType = TypeName.get(parameterTypeMirror);
      if (inputModel.isMandatory()) {
        if (!util.getProcessingEnv().getTypeUtils().isAssignable(facetType, parameterTypeMirror)) {
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
                    "$T $L = $L.$L()", parameterType, variable, FACETS_VAR, usingFacetModel.name())
                .build());
      } else if (util.isRawAssignable(parameterTypeMirror, Optional.class)) {
        codeBuilder.add(
            CodeBlock.builder()
                .addStatement(
                    "$T $L = $L.$L()", parameterType, variable, FACETS_VAR, usingFacetModel.name())
                .build());
      } else {
        String message =
            String.format(
                "Optional inputDef dependencyDef %s must have type as Optional",
                usingFacetModel.name());
        util.error(message, parameter);
        throw new VajramValidationException(message);
      }
    } else {
      String message = "No inputDef resolver found for " + usingFacetModel.name();
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
   * @param depFanoutMap Map of all the dependencies and their resolvers defintions are fanout or
   *     not
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
      String variableName = toJavaName(usingFacetName);
      final VajramInfoLite depVajramInfoLite = dependencyModel.depVajramInfoLite();
      String requestClass = dependencyModel.depReqClassQualifiedName();
      TypeName boxedDepType = util.toTypeName(depVajramInfoLite.responseType()).box();
      TypeName unboxedDepType =
          boxedDepType.isBoxedPrimitive() ? boxedDepType.unbox() : boxedDepType;
      String logicName = method.getSimpleName().toString();
      if (depFanoutMap.getOrDefault(usingFacetId, false)) {
        // fanout case
        boolean typeMismatch = false;
        if (!util.isRawAssignable(parameterType, DependencyResponses.class)) {
          typeMismatch = true;
        } else if (getTypeParameters(parameterType).size() != 2) {
          typeMismatch = true;
        }
        if (typeMismatch) {
          // the parameter data type must be DependencyResponse<Req, Resp>
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
                ClassName.get(DependencyResponses.class), toClassName(requestClass), boxedDepType),
            variableName,
            FACETS_VAR,
            usingFacetName);
      } else {
        // This means the dependency being consumed used is not a fanout dependency
        String depValueAccessorCode =
            """
              $1T $2L =
                _facets.$3L()
                   .values()
                   .entrySet()
                   .iterator()
                   .next()
                   .getValue()""";
        if (usingFacetDef.isMandatory()) {
          if (unboxedDepType.equals(TypeName.get(parameterType))) {
            // This means this dependencyDef being consumed is a non fanout mandatory dependency and
            // the dev has requested the value directly. So we extract the only value from
            // dependencyDef response and provide it.
            String code =
                depValueAccessorCode
                    + """
                              .getValueOrThrow().orElseThrow(() ->
                                  new $4T("Received null value for mandatory dependencyDef '$5L' of vajram '$6L'"))""";
            ifBlockBuilder.addStatement(
                code,
                unboxedDepType,
                variableName,
                usingFacetName,
                IllegalArgumentException.class,
                usingFacetName,
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
                boxedDepType);
          } else if (util.isRawAssignable(parameterType, Optional.class)) {
            // This means this dependencyDef in "Using" annotation is not a fanout and the dev has
            // requested an 'Optional'. So we retrieve the only Errable from the dependencyDef
            // response, extract the optional and provide it.
            String code = depValueAccessorCode + ".value()";
            ifBlockBuilder.addStatement(
                code,
                ParameterizedTypeName.get(ClassName.get(Optional.class), boxedDepType),
                variableName,
                boxedDepType);
          } else {
            String message =
                ("A resolver ('%s') must not access an optional dependencyDef ('%s') directly."
                        + "Use Optional<>, Errable<>, or DependencyResponse<> instead")
                    .formatted(logicName, usingFacetName);
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
   * Method to generate resolver code for variables having single resolver.
   *
   * <p>Fanout case
   *
   * <p>- MultiRequest of normal type => fanout loop and create facets
   *
   * <p>- MultiRequest of Vajram Request - DependencyCommand.MultiExecute<NormalType>
   *
   * <p>Non - fanout
   *
   * <p>- Normal datatype - Vajram Request => toInputValues() - DependencyCommand.executeWith
   *
   * @param resolverMethod Resolve method
   * @param depFacetIds Resolve facets
   * @param methodCodeBuilder {@link CodeBlock.Builder}
   * @param depName the name of the dependency facet
   * @param isFanOutDep Variable mentioning if the resolved variable uses a fanout dependencyDef
   * @param fanoutResolver
   * @param requestBuilderType
   */
  private void buildFinalResolvers(
      ExecutableElement resolverMethod,
      int[] depFacetIds,
      CodeBlock.Builder methodCodeBuilder,
      String depName,
      boolean isFanOutDep,
      boolean fanoutResolver,
      VajramID depVajramId,
      ClassName requestBuilderType) {

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
    String resolverResultVar = fanoutResolver ? RESOLVER_RESULTS : RESOLVER_RESULT;
    methodCodeBuilder.add(
        "$T $L = $L(", methodReturnType, resolverResultVar, resolverMethod.getSimpleName());
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
          "\t return $T.skip($L.doc())", ResolverCommand.class, resolverResultVar);
      methodCodeBuilder.add("} else {\n\t");

      TypeMirror actualReturnType = getTypeParameters(returnType).get(0);
      if (util.isRawAssignable(returnType, SingleExecute.class)) {
        methodCodeBuilder.beginControlFlow(
            "for($T $L: $L)", requestBuilderType, RESOLVER_REQUEST, RESOLVER_REQUESTS);
        if (util.isRawAssignable(actualReturnType, Request.class)) {
          for (int depFacetId : depFacetIds) {
            String depInputName =
                checkNotNull(
                    checkNotNull(vajramDefs.get(depVajramId)).facetIdNameMapping().get(depFacetId));
            methodCodeBuilder.addStatement(
                "$L.$L($L.input().$L())",
                RESOLVER_REQUEST,
                depInputName,
                RESOLVER_RESULT,
                depInputName);
          }
        } else {
          if (depFacetIds.length != 1) {
            throw util.errorAndThrow(
                "Resolver method which resolves multiple dependency inputs must return a Request object",
                resolverMethod);
          }
          String depFacetName =
              checkNotNull(
                  checkNotNull(vajramDefs.get(depVajramId))
                      .facetIdNameMapping()
                      .get(depFacetIds[0]));
          methodCodeBuilder.addStatement(
              "$L.$L($L.input())", RESOLVER_REQUEST, depFacetName, RESOLVER_RESULT);
        }
        methodCodeBuilder.endControlFlow();
      } else if (util.isRawAssignable(returnType, MultiExecute.class)) {
        // TODO : add missing validations if any (??)
        if (!isFanOutDep) {
          String message =
              """
              Dependency '%s' is not a fanout dependency, yet the resolver method returns a MultiExecute command.\
               This is not allowed. Return a SingleExecute command, a single value, or mark the dependency as `canFanout = true`."""
                  .formatted(depName);
          util.error(message, resolverMethod);
          throw new VajramValidationException(message);
        }
        methodCodeBuilder.addStatement(
            "$T $L = new $T<>()",
            ParameterizedTypeName.get(ClassName.get(List.class), requestBuilderType),
            RESOLVER_REQUESTS,
            ArrayList.class);
        methodCodeBuilder.beginControlFlow(
            "for($T $L: $L.inputs())", actualReturnType, RESOLVER_RESULT, RESOLVER_RESULTS);
        if (util.isRawAssignable(actualReturnType, Request.class)) {
          /*
          TODO: Add validation that this vajram request is of the same type as the request of the dependency Vajram
          */
          for (int depFacetId : depFacetIds) {
            String depFacetName =
                checkNotNull(
                    checkNotNull(vajramDefs.get(depVajramId)).facetIdNameMapping().get(depFacetId));
            methodCodeBuilder.addStatement(
                "$L.add($L._newCopy().$L($L.$L()))",
                RESOLVER_REQUESTS,
                RESOLVER_REQUEST,
                depFacetName,
                RESOLVER_RESULT,
                depFacetName);
          }
        } else {
          // Here we are assuming that the method is returning an MultiExecute of the type of the
          // input being resolved. If this assumption is incorrect, the generated wrapper class will
          // fail compilation.
          // TODO : Add validation for the type parameter of the MultiExecute to match the type of
          // the input being resolved
          if (depFacetIds.length != 1) {
            throw util.errorAndThrow(
                "Resolver method which resolves multiple dependency inputs must return a Request object",
                resolverMethod);
          }
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
          "return $T.executeWithRequests($L)", ResolverCommand.class, "arg0");
    }
    if (resovlerReturnedDepCommand) {
      // close the else block of "if($L.shouldSkip())"
      methodCodeBuilder.endControlFlow();
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
        .add(".id($L)", dependencyDef.id())
        .add(".name($S)", dependencyDef.name());
    String code = ".dataAccessSpec($1T.vajramID($2S))";
    inputDefBuilder.add(
        code, ClassName.get(VajramID.class), dependencyDef.depVajramInfoLite().vajramId());
    inputDefBuilder.add(".isMandatory($L)", dependencyDef.isMandatory());
    inputDefBuilder.add(".isBatched($L)", dependencyDef.isBatched());
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
              .collect(joining(","));
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
              .collect(joining(","))
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
    } catch (IOException e) {
      throw new RuntimeException(e);
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
    } catch (IOException e) {
      throw new RuntimeException(e);
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
    // TODO : Add checks for subsetRequest
    Builder fullConstructor = constructorBuilder();
    clazz.addAnnotations(
        codeGenParams.isBuilder ? annotations(ToString.class) : recordAnnotations());
    ClassName requestInterfacetType =
        ClassName.get(packageName, getRequestInterfaceName(vajramName));
    ClassName immutRequestType = ClassName.get(packageName, getImmutRequestClassName(vajramName));
    ClassName batchFacetsType =
        ClassName.get(packageName, getImmutFacetsClassname(vajramName))
            .nestedClass(BATCH_IMMUT_FACETS_CLASS_SUFFIX);
    ClassName commonFacetsType =
        ClassName.get(packageName, getImmutFacetsClassname(vajramName))
            .nestedClass(COMMON_IMMUT_FACETS_CLASS_SUFFIX);
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

    if (codeGenParams.subsetRequest && codeGenParams.isBuilder) {
      Builder requestConstructor = constructorBuilder();
      requestConstructor.addParameter(
          ParameterSpec.builder(requestInterfacetType, "_request")
              .addAnnotation(NonNull.class)
              .build());
      for (FacetGenModel facet : eligibleFacets) {
        if (facet.facetTypes().contains(INPUT)) {
          switch (getReturnType(facet)) {
            case actual ->
                requestConstructor.addStatement(
                    "this.$L = _request.$L()", facet.name(), facet.name());
            case optional ->
                requestConstructor.addStatement(
                    "this.$L = _request.$L().orElse(null)", facet.name(), facet.name());
            case errable, responses ->
                throw new UnsupportedOperationException(
                    "request to facet conversion for BatchableRequest constituents facets class is not implemented yet");
          }
        }
      }
      clazz.addMethod(requestConstructor.build());
    }

    for (FacetGenModel facet : eligibleFacets) {
      TypeAndName facetType = getTypeName(getDataType(facet));
      TypeAndName boxedFacetType = boxPrimitive(facetType);

      FacetJavaType facetJavaType = getFacetFieldType(facet);
      boolean isInput = facet.facetTypes().contains(INPUT);
      ParameterSpec facetParam =
          ParameterSpec.builder(
                  getJavaTypeName(facetJavaType, facet)
                      .annotated(
                          annotations(
                              switch (facetJavaType) {
                                case actual, optional -> Nullable.class;
                                case errable, responses -> NonNull.class;
                              })),
                  facet.name())
              .build();
      if (!codeGenParams.isUnBatched && (isInput ? !codeGenParams.wrapsRequest : true)) {
        FieldSpec.Builder facetField =
            FieldSpec.builder(
                (switch (facetJavaType) {
                      case actual, optional -> boxedFacetType.typeName();
                      case errable -> errable(facetType);
                      case responses -> responsesType((DependencyModel) facet);
                    })
                    .annotated(
                        annotations(
                            switch (facetJavaType) {
                              case actual, optional -> Nullable.class;
                              case errable, responses -> NonNull.class;
                            })),
                facet.name(),
                PRIVATE);
        if (codeGenParams.isBuilder) {
          switch (facetJavaType) {
            case errable -> facetField.initializer("$T.nil()", Errable.class);
            case responses -> facetField.initializer("$T.empty()", DependencyResponses.class);
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
    if (codeGenParams.isBuilder && !(codeGenParams.isRequest || codeGenParams.subsetRequest)) {
      // Make sure Builder always has a constructor which accepts only the request irrespective of
      // type of Facet class
      // See Vajram#facetsFromRequest
      clazz.addMethod(
          constructorBuilder()
              .addParameter(
                  ParameterSpec.builder(
                          ClassName.get(packageName, getRequestInterfaceName(vajramName)),
                          "request")
                      .build())
              .addCode(
                  codeGenParams.isUnBatched
                      ? CodeBlock.builder()
                          .addStatement(
                              "this._batchable = new BatchImmutableFacets.Builder(request._asBuilder());")
                          .addStatement(
                              "this._common = new CommonImmutableFacets.Builder(request._asBuilder());")
                          .build()
                      : CodeBlock.builder()
                          .addStatement("this._request = request._asBuilder()")
                          .build())
              .build());
    }
  }

  /**
   * @param isRequest
   * @param isBuilder
   * @param wrapsRequest
   * @param subsetRequest {@code true} when the facets class being generated is a subset of the
   *     complete set of facets (For example: BatchFacets and CommonFacets)
   * @param isUnBatched {@code true} when the facets class is of a batchable vajram
   * @param withImpl
   * @param devAccessible {@code true} when the generated class can be accessed by the developer.
   *     Ex: Request class, Batch class. (Facets class and commonFacets class are not accessible to
   *     the user)
   */
  @lombok.Builder(toBuilder = true)
  private record CodeGenParams(
      boolean isRequest,
      boolean isBuilder,
      boolean wrapsRequest,
      boolean subsetRequest,
      boolean isUnBatched,
      boolean withImpl,
      boolean devAccessible) {

    private static final CodeGenParams DEFAULT = CodeGenParams.builder().build();
  }

  private void createFacetGetter(
      TypeSpec.Builder clazz, FacetGenModel facet, CodeGenParams codeGenParams) {
    TypeAndName facetType = getTypeName(getDataType(facet));
    Builder method;
    if (!(facet instanceof DependencyModel dep) || !dep.canFanout()) {
      // Non-fanout facet
      FacetJavaType facetJavaType = getReturnType(facet);
      TypeAndName boxableType =
          new TypeAndName(
              facetType
                  .typeName()
                  // Remove @Nullable because Optional<@Nullable T> is not useful.
                  .withoutAnnotations(),
              facetType.type(),
              List.of());

      final boolean facetInCurrentClass =
          !codeGenParams.isUnBatched
              && (codeGenParams.isRequest
                  || !facet.facetTypes().contains(INPUT)
                  || !codeGenParams.wrapsRequest);
      method =
          methodBuilder(facet.name())
              .addModifiers(PUBLIC)
              .returns(
                  (TypeName)
                      (codeGenParams.isRequest || codeGenParams.subsetRequest
                          ? switch (facetJavaType) {
                            case actual ->
                                unboxPrimitive(facetType)
                                    .typeName()
                                    // Remove @Nullable because getter has null check
                                    // and will never return null.
                                    .withoutAnnotations();
                            case optional -> optional(boxableType);
                            case errable -> errable(boxableType);
                            case responses ->
                                throw new AssertionError(
                                    "Non fanout facet cannot have this response type");
                          }
                          :
                          // Facets classes must always return a facetValue
                          errable(boxableType)));
      if (codeGenParams.withImpl) {
        method
            .addCode(
                FacetJavaType.actual.equals(facetJavaType) && facetInCurrentClass
                    ? CodeBlock.of(
                        """
                        if($L == null) {
                          throw new IllegalStateException(
                              "The facet '$L' is not optional, but has null value. \
                        This should not happen");
                        }""",
                        facet.name(),
                        facet.name())
                    : CodeBlock.builder().build())
            .addStatement(
                facetInCurrentClass
                    ? switch (facetJavaType) {
                      case optional ->
                          CodeBlock.of(
                              "return $T.ofNullable(this.$L)", Optional.class, facet.name());
                      case actual, errable -> CodeBlock.of("return this.$L", facet.name());
                      case responses -> throw new AssertionError();
                    }
                    : codeGenParams.wrapsRequest
                        ? CodeBlock.of(
                            "return $T.with(this._request.$L())", Errable.class, facet.name())
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
                : CodeBlock.of("return this.$L", dep.name()));
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
        ClassName.get(DependencyResponses.class),
        boxPrimitive(requestType).typeName(),
        boxPrimitive(facetType).typeName());
  }

  private static FieldSpec.Builder createFacetIdField(FacetGenModel facet, String facetJavaName) {
    return FieldSpec.builder(int.class, facetJavaName + FACET_ID_SUFFIX)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addAnnotation(
            AnnotationSpec.builder(FacetIdNameMapping.class)
                .addMember("id", "$L", facet.id())
                .addMember("name", "$S", facet.name())
                .build())
        .initializer("$L", facet.id());
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

  private ImmutableList<MethodSpec> createFacetContainerMethods(
      List<? extends FacetGenModel> eligibleFacets,
      ClassName enclosingClassName,
      CodeGenParams codeGenParams) {

    if (codeGenParams.wrapsRequest && codeGenParams.isUnBatched) {
      throw new IllegalArgumentException("Unbatched class does not wrap request - this is a bug");
    }
    // TODO : add validations for subsetRequest

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
            .collect(joining(", "));
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
                FacetJavaType facetJavaType = getReturnType(facet);
                if (facet.facetTypes().contains(INPUT) && codeGenParams.wrapsRequest) {
                  setMethod.addStatement(
                      "case $L -> this._request.$L((($T)$L).valueOpt().orElse(null))",
                      facet.id(),
                      facet.name(),
                      errable(getTypeName(facet.dataType())),
                      facetValueParamName);
                } else {
                  setMethod.addStatement(
                      switch (facetJavaType) {
                        case actual, optional ->
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
                                FacetJavaType.errable.equals(facetJavaType)
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
                                case optional, actual -> "$L, $T.withValue($L())";
                                default -> "$L, $L()";
                              };
                            })
                        .collect(joining(",\n"))),
            Stream.concat(
                    Stream.of(ImmutableMap.class),
                    eligibleFacets.stream()
                        .<Object>mapMulti(
                            (facetGenModel, consumer) -> {
                              FacetJavaType facetJavaType = getReturnType(facetGenModel);
                              consumer.accept(facetGenModel.id());
                              if (FacetJavaType.optional.equals(facetJavaType)
                                  || FacetJavaType.actual.equals(facetJavaType)) {
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
            FacetJavaType facetJavaType = getReturnType(facetDef);
            getMethod.addStatement(
                switch (facetJavaType) {
                  case responses, errable ->
                      CodeBlock.of("case $L -> $L()", facetDef.id(), facetDef.name());
                  case actual, optional ->
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

  private MethodSpec getterCodeForInput(InputModel<?> facet, String name, TypeAndName typeAndName) {
    boolean wrapWithOptional = !facet.isMandatory();
    return methodBuilder(name)
        .returns(
            (wrapWithOptional
                ? optional(
                    new TypeAndName(
                        boxPrimitive(typeAndName)
                            .typeName()
                            // Remove @Nullable because Optional<@Nullable T> is not useful.
                            .withoutAnnotations()))
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

  enum FacetJavaType {
    actual,
    boxed_actual,
    optional,
    errable,
    responses;
  }

  private TypeName getJavaTypeName(FacetJavaType javaType, FacetGenModel facet) {
    TypeAndName actualType = getTypeName(getDataType(facet));
    return switch (javaType) {
      case actual -> actualType.typeName();
      case boxed_actual -> boxPrimitive(actualType).typeName();
      case optional -> optional(boxPrimitive(actualType));
      case errable -> errable(boxPrimitive(actualType));
      case responses -> {
        if (facet instanceof DependencyModel dep) {
          yield responsesType(dep);
        } else {
          throw new AssertionError("FacetJavaType.responses is only supported for DependencyModel");
        }
      }
    };
  }

  private static FacetJavaType getFacetFieldType(FacetGenModel facet) {
    FacetJavaType fieldType;
    if (facet instanceof DependencyModel dep && dep.canFanout()) {
      // Fanout dependency
      return FacetJavaType.responses;
    } else if (facet.facetTypes().contains(INPUT)) {
      // Input
      fieldType = FacetJavaType.actual;
    } else {
      // Non fanout dependency
      fieldType = FacetJavaType.errable;
    }
    return fieldType;
  }

  private static FacetJavaType getReturnType(FacetGenModel facet) {
    FacetJavaType facetJavaType;
    if (facet instanceof DependencyModel dep && dep.canFanout()) {
      // Fanout dependency
      return FacetJavaType.responses;
    } else if (facet.facetTypes().contains(INPUT)) {
      // Input
      facetJavaType = facet.isMandatory() ? FacetJavaType.actual : FacetJavaType.optional;
    } else {
      // NonFanout dependency
      if (facet.isMandatory()) {
        facetJavaType = FacetJavaType.actual;
      } else {
        facetJavaType = FacetJavaType.errable;
      }
    }
    return facetJavaType;
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

    ClassName facetsType = getFacetsInterfaceType();
    ClassName immutableFacetsType = ClassName.get(packageName, getImmutFacetsClassname(vajramName));

    TypeSpec.Builder facetsInterface =
        interfaceBuilder(getFacetsInterfaceName(vajramName))
            .addModifiers(PUBLIC)
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

  private TypeAndName getDependencyOutputsType(DependencyModel dependencyDef) {
    boolean wrapWithErrable = !dependencyDef.isMandatory() && !dependencyDef.canFanout();
    DataType<?> depResponseType = dependencyDef.dataType();
    if (dependencyDef.canFanout()) {
      return new TypeAndName(
          ParameterizedTypeName.get(
              ClassName.get(DependencyResponses.class),
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
    int lastDotIndex = depReqClassName.lastIndexOf('.');
    return ClassName.get(
        depReqClassName.substring(0, lastDotIndex), depReqClassName.substring(lastDotIndex + 1));
  }

  private void batchFacetsClasses() {

    StringWriter interfaceAllCode = new StringWriter();
    StringWriter immutAllFacetsCode = new StringWriter();

    ClassName allFacetsType = getFacetsInterfaceType();
    ClassName batchFacetsType = allFacetsType.nestedClass(BATCH_FACETS);
    ClassName commonFacetsType = allFacetsType.nestedClass(COMMON_INPUTS);
    ClassName allImmutFacetsType = ClassName.get(packageName, getImmutFacetsClassname(vajramName));
    ClassName allFacetsBuilderType = allImmutFacetsType.nestedClass("Builder");
    ClassName batchImmutFacetsType =
        allImmutFacetsType.nestedClass(BATCH_IMMUT_FACETS_CLASS_SUFFIX);
    ClassName batchBuilderType = batchImmutFacetsType.nestedClass("Builder");
    ClassName commonImmutFacetsType =
        allImmutFacetsType.nestedClass(COMMON_IMMUT_FACETS_CLASS_SUFFIX);
    ClassName commonBuilderType = commonImmutFacetsType.nestedClass("Builder");

    List<FacetGenModel> batchedFacets =
        vajramInfo.facetStream().filter(t -> t.isBatched()).toList();
    List<FacetGenModel> commonFacets =
        vajramInfo.facetStream().filter(t -> !t.isBatched()).toList();

    TypeSpec.Builder batchFacetsInterface =
        interfaceBuilder(BATCH_FACETS)
            .addModifiers(PUBLIC, STATIC)
            .addSuperinterface(Facets.class)
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "ClassReferencesSubclass")
                    .build());

    for (var input : batchedFacets) {
      createFacetGetter(
          batchFacetsInterface, input, CodeGenParams.builder().isRequest(false).build());
    }

    TypeSpec.Builder batchImmutFacetsClass =
        util.classBuilder(BATCH_IMMUT_FACETS_CLASS_SUFFIX)
            .addModifiers(FINAL, STATIC)
            .addSuperinterface(ImmutableFacets.class)
            .addSuperinterface(batchFacetsType)
            .addMethod(
                methodBuilder("_builder")
                    .addModifiers(PUBLIC, STATIC)
                    .returns(batchBuilderType)
                    .addStatement("return new Builder()")
                    .build());
    createFacetMembers(
        batchImmutFacetsClass,
        batchImmutFacetsType,
        batchedFacets,
        CodeGenParams.builder().subsetRequest(true).build());

    TypeSpec.Builder batchFacetsBuilderClass =
        util.classBuilder("Builder")
            .addModifiers(STATIC, FINAL)
            .addSuperinterface(batchFacetsType)
            .addSuperinterface(FacetsBuilder.class);
    createFacetMembers(
        batchFacetsBuilderClass,
        batchImmutFacetsType,
        batchedFacets,
        CodeGenParams.builder().isBuilder(true).subsetRequest(true).build());

    TypeSpec.Builder commonFacetsInterface =
        interfaceBuilder(COMMON_INPUTS)
            .addModifiers(PUBLIC, STATIC)
            .addSuperinterface(Facets.class)
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "ClassReferencesSubclass")
                    .build());

    for (var input : commonFacets) {
      createFacetGetter(commonFacetsInterface, input, CodeGenParams.builder().build());
    }

    TypeSpec.Builder commonImmutFacetsClass =
        util.classBuilder(COMMON_IMMUT_FACETS_CLASS_SUFFIX)
            .addModifiers(FINAL, STATIC)
            .addSuperinterface(ImmutableFacets.class)
            .addSuperinterface(commonFacetsType)
            .addMethod(
                methodBuilder("_builder")
                    .addModifiers(PUBLIC, STATIC)
                    .returns(commonBuilderType)
                    .addStatement("return new Builder()")
                    .build());

    createFacetMembers(
        commonImmutFacetsClass,
        commonImmutFacetsType,
        vajramInfo.facetStream().filter(facetGenModel -> !facetGenModel.isBatched()).toList(),
        CodeGenParams.builder().subsetRequest(true).build());

    TypeSpec.Builder commonFacetsBuilderClass =
        util.classBuilder("Builder")
            .addModifiers(STATIC, FINAL)
            .addSuperinterface(commonFacetsType)
            .addSuperinterface(FacetsBuilder.class);

    createFacetMembers(
        commonFacetsBuilderClass,
        commonImmutFacetsType,
        vajramInfo.facetStream().filter(facetGenModel -> !facetGenModel.isBatched()).toList(),
        CodeGenParams.builder().isBuilder(true).subsetRequest(true).build());

    TypeSpec.Builder allFacetsInterface =
        interfaceBuilder(getFacetsInterfaceName(vajramName))
            .addModifiers(PUBLIC)
            .addSuperinterface(BatchableFacets.class)
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "ClassReferencesSubclass")
                    .build());

    addFacetConstants(
        allFacetsInterface,
        vajramInfo.facetStream().filter(f -> !f.facetTypes().contains(INPUT)).toList());
    // Add all the getters ( of batch and non bacth facets) to allFacetClass
    for (var input : batchedFacets) {
      createFacetGetter(allFacetsInterface, input, CodeGenParams.builder().build());
    }

    for (var input : commonFacets) {
      createFacetGetter(allFacetsInterface, input, CodeGenParams.builder().build());
    }

    TypeSpec.Builder allImmutFacetsClass =
        codegenBatchableFacets(
                TypeSpec.classBuilder(getImmutFacetsClassname(vajramName))
                    .addSuperinterface(ClassName.get(BatchableImmutableFacets.class))
                    .addSuperinterface(allFacetsType),
                batchImmutFacetsType,
                commonImmutFacetsType)
            .addMethod(
                methodBuilder("_builder")
                    .addModifiers(PUBLIC, STATIC)
                    .returns(allFacetsBuilderType)
                    .addStatement("return new Builder()")
                    .build());

    createFacetMembers(
        allImmutFacetsClass,
        allImmutFacetsType,
        vajramInfo.facetStream().toList(),
        CodeGenParams.builder().isUnBatched(true).build());

    TypeSpec.Builder allFacetsBuilderClass =
        codegenBatchableFacets(
            TypeSpec.classBuilder("Builder")
                .addSuperinterface(allFacetsType)
                .addSuperinterface(ClassName.get(BatchableFacetsBuilder.class))
                .addModifiers(STATIC),
            batchBuilderType,
            commonBuilderType);
    createFacetMembers(
        allFacetsBuilderClass,
        allImmutFacetsType,
        vajramInfo.facetStream().toList(),
        CodeGenParams.builder().isUnBatched(true).isBuilder(true).build());

    try {
      JavaFile.builder(
              packageName,
              allFacetsInterface
                  .addMethod(
                      methodBuilder("_build")
                          .addModifiers(PUBLIC, ABSTRACT)
                          .returns(allImmutFacetsType)
                          .build())
                  .addMethod(
                      methodBuilder("_asBuilder")
                          .addModifiers(PUBLIC, ABSTRACT)
                          .returns(allImmutFacetsType.nestedClass("Builder"))
                          .build())
                  .addMethod(
                      methodBuilder("_builder")
                          .addModifiers(PUBLIC, STATIC)
                          .returns(allFacetsBuilderType)
                          .addStatement(
                              "return new $T()", allImmutFacetsType.nestedClass("Builder"))
                          .build())
                  .addMethod(
                      methodBuilder("_batchable")
                          .returns(batchFacetsType)
                          .addAnnotation(Override.class)
                          .addModifiers(PUBLIC, ABSTRACT)
                          .build())
                  .addMethod(
                      methodBuilder("_common")
                          .returns(commonFacetsType)
                          .addAnnotation(Override.class)
                          .addModifiers(PUBLIC, ABSTRACT)
                          .build())
                  .addType(
                      batchFacetsInterface
                          .addMethod(
                              methodBuilder("_build")
                                  .addModifiers(PUBLIC, ABSTRACT)
                                  .returns(batchImmutFacetsType)
                                  .build())
                          .addMethod(
                              methodBuilder("_asBuilder")
                                  .addModifiers(PUBLIC, ABSTRACT)
                                  .returns(batchImmutFacetsType.nestedClass("Builder"))
                                  .build())
                          .addMethod(
                              methodBuilder("_builder")
                                  .addModifiers(PUBLIC, STATIC)
                                  .returns(batchBuilderType)
                                  .addStatement(
                                      "return new $T()",
                                      batchImmutFacetsType.nestedClass("Builder"))
                                  .build())
                          .build())
                  .addType(
                      commonFacetsInterface
                          .addMethod(
                              methodBuilder("_build")
                                  .addModifiers(PUBLIC, ABSTRACT)
                                  .returns(commonImmutFacetsType)
                                  .build())
                          .addMethod(
                              methodBuilder("_asBuilder")
                                  .addModifiers(PUBLIC, ABSTRACT)
                                  .returns(commonImmutFacetsType.nestedClass("Builder"))
                                  .build())
                          .addMethod(
                              methodBuilder("_builder")
                                  .addModifiers(PUBLIC, STATIC)
                                  .returns(commonBuilderType)
                                  .addStatement(
                                      "return new $T()",
                                      commonImmutFacetsType.nestedClass("Builder"))
                                  .build())
                          .build())
                  .build())
          .build()
          .writeTo(interfaceAllCode);
      JavaFile.builder(
              packageName,
              allImmutFacetsClass
                  .addMethods(
                      createFacetContainerMethods(
                          vajramInfo.facetStream().toList(),
                          allImmutFacetsType,
                          CodeGenParams.builder().isUnBatched(true).build()))
                  .addType(
                      allFacetsBuilderClass
                          .addMethods(
                              createFacetContainerMethods(
                                  vajramInfo.facetStream().toList(),
                                  allImmutFacetsType,
                                  CodeGenParams.builder()
                                      .isUnBatched(true)
                                      .isBuilder(true)
                                      .build()))
                          .build())
                  .addType(
                      batchImmutFacetsClass
                          .addMethods(
                              createFacetContainerMethods(
                                  batchedFacets,
                                  batchImmutFacetsType,
                                  CodeGenParams.builder().subsetRequest(true).build()))
                          .addType(
                              batchFacetsBuilderClass
                                  .addMethods(
                                      createFacetContainerMethods(
                                          batchedFacets,
                                          batchImmutFacetsType,
                                          CodeGenParams.builder()
                                              .subsetRequest(true)
                                              .isBuilder(true)
                                              .build()))
                                  .build())
                          .build())
                  .addType(
                      commonImmutFacetsClass
                          .addMethods(
                              createFacetContainerMethods(
                                  commonFacets,
                                  commonImmutFacetsType,
                                  CodeGenParams.builder().subsetRequest(true).build()))
                          .addType(
                              commonFacetsBuilderClass
                                  .addMethods(
                                      createFacetContainerMethods(
                                          commonFacets,
                                          commonImmutFacetsType,
                                          CodeGenParams.builder()
                                              .subsetRequest(true)
                                              .isBuilder(true)
                                              .build()))
                                  .build())
                          .build())
                  .build())
          .build()
          .writeTo(immutAllFacetsCode);
    } catch (IOException e) {
      util.error(String.valueOf(e.getMessage()), vajramInfo.vajramClass());
    }

    util.generateSourceFile(
        packageName + '.' + getFacetsInterfaceName(vajramName),
        interfaceAllCode.toString(),
        vajramInfo.vajramClass());
    util.generateSourceFile(
        packageName + '.' + getImmutFacetsClassname(vajramName),
        immutAllFacetsCode.toString(),
        vajramInfo.vajramClass());
  }

  private ClassName getFacetsInterfaceType() {
    return ClassName.get(packageName, getFacetsInterfaceName(vajramName));
  }

  private String getBatchFacetsInterfaceName() {
    return getFacetsInterfaceName(vajramName) + "." + BATCH_FACETS;
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
    List<FieldSpec> idFields = new ArrayList<>();
    int facetCount = 0;
    for (FacetGenModel facet : facets) {
      facetCount++;
      FieldSpec facetIdField =
          createFacetIdField(facet, facet.name())
              .addJavadoc(
                  """
                  ******************************************************<br>
                  Facet id of the facet $S  <br>
                  ******************************************************
                  """,
                  facet.name())
              .build();
      idFields.add(facetIdField);

      TypeAndName facetType = getTypeName(getDataType(facet));
      TypeAndName boxedFacetType = boxPrimitive(facetType);
      ClassName vajramReqClass = ClassName.get(packageName, getRequestInterfaceName(vajramName));
      if (facet instanceof InputModel<?> inputDef) {
        specFields.add(
            FieldSpec.builder(
                    ParameterizedTypeName.get(
                        ClassName.get(VajramFacetSpec.class),
                        boxedFacetType.typeName(),
                        vajramReqClass),
                    facet.name() + FACET_SPEC_SUFFIX)
                .addModifiers(PUBLIC, STATIC, FINAL)
                .initializer(
                    "new $T<>($N, $S, $T.class)",
                    VajramFacetSpec.class,
                    facetIdField,
                    ((FacetGenModel) inputDef).name(),
                    vajramReqClass)
                .build());
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
                    facet.name() + FACET_SPEC_SUFFIX)
                .addModifiers(PUBLIC, STATIC, FINAL)
                .initializer(
                    "new $T<>($N, $S, $T.class, $T.class)",
                    specType,
                    facetIdField,
                    facet.name(),
                    vajramReqClass,
                    depReqClass)
                .build());
      }
    }
    for (int i = 0; i < facetCount; i++) {
      classBuilder.addField(idFields.get(i)).addField(specFields.get(i));
    }
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
