package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.except.KrystalCompletionException.wrapAsCompletionException;
import static com.flipkart.krystal.facets.resolution.ResolverCommand.skip;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.tags.ElementTags.emptyTags;
import static com.flipkart.krystal.vajram.facets.FacetValidation.validateMandatoryFacet;
import static com.flipkart.krystal.vajram.utils.VajramLoader.loadVajrams;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValue.SingleFacetValue;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.BasicFacetInfo;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.kryon.VajramKryonDefinition;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.krystex.resolution.Resolver;
import com.flipkart.krystal.krystex.resolution.ResolverLogic;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramDefRoot;
import com.flipkart.krystal.vajram.exception.MandatoryFacetMissingException;
import com.flipkart.krystal.vajram.exception.MandatoryFacetsMissingException;
import com.flipkart.krystal.vajram.exception.VajramDefinitionException;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.resolution.FanoutInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.resolution.One2OneInputResolver;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/** The execution graph encompassing all registered vajrams. */
@Slf4j
public final class VajramGraph {

  @Getter private final KryonDefinitionRegistry kryonDefinitionRegistry;

  private final LogicDefRegistryDecorator logicRegistryDecorator;

  private final ImmutableMap<VajramID, VajramDefinition> vajramDefinitions;
  private final ImmutableMap<Class<? extends VajramDefRoot>, VajramDefinition> definitionByDefType;
  private final ImmutableMap<Class<? extends Request<?>>, VajramDefinition> definitionByReqType;

  /**
   * Maps every vajramId to its corresponding kryonId all of whose dependencies have also been
   * loaded recursively. The mapped kryon id represents the complete executable sub-graph of the
   * vajram.
   */
  private final Set<VajramID> vajramExecutables =
      Collections.synchronizedSet(new LinkedHashSet<>());

  @lombok.Builder
  private VajramGraph(Set<String> packagePrefixes, Set<Class<? extends VajramDefRoot>> classes) {
    LogicDefinitionRegistry logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.kryonDefinitionRegistry = new KryonDefinitionRegistry(logicDefinitionRegistry);
    this.logicRegistryDecorator = new LogicDefRegistryDecorator(logicDefinitionRegistry);

    Map<VajramID, VajramDefinition> vajramDefinitions = new LinkedHashMap<>();
    Map<Class<? extends VajramDefRoot>, VajramDefinition> definitionByDefType =
        new LinkedHashMap<>();
    Map<Class<? extends Request<?>>, VajramDefinition> definitionByReqType = new LinkedHashMap<>();
    for (VajramDefRoot<Object> vajramDef : loadVajrams(packagePrefixes, classes)) {
      registerVajram(vajramDef, vajramDefinitions, definitionByDefType, definitionByReqType);
    }
    this.vajramDefinitions = ImmutableMap.copyOf(vajramDefinitions);
    this.definitionByDefType = ImmutableMap.copyOf(definitionByDefType);
    this.definitionByReqType = ImmutableMap.copyOf(definitionByReqType);
    loadKryons();
  }

  public ImmutableMap<VajramID, VajramDefinition> vajramDefinitions() {
    return ImmutableMap.copyOf(vajramDefinitions);
  }

  /**
   * Returns a new {@link DependentChain} representing the given strings which are passed in trigger
   * order (from [Start] to immediate dependant.)
   *
   * @param firstVajramId The first vajram/kryon in the DependantChain
   * @param firstDependency The dependency of the first vajram/kryon in the DependencyChain
   * @param subsequentDependencies an array representing the dependency names in the DependantChain
   *     in trigger order
   */
  public DependentChain computeDependentChain(
      String firstVajramId, Dependency firstDependency, Dependency... subsequentDependencies) {
    DependentChain currentDepChain =
        kryonDefinitionRegistry
            .getDependentChainsStart()
            .extend(vajramID(firstVajramId), firstDependency);
    for (Dependency dependency : subsequentDependencies) {
      currentDepChain = currentDepChain.extend(dependency.ofVajramID(), dependency);
    }
    return currentDepChain;
  }

  /**
   * Registers vajrams that need to be executed at a later point. This is a necessary step for
   * vajram execution.
   *
   * @param vajramDef The vajram to be registered for future execution.
   * @param vajramDefinitions
   * @param definitionByDefType
   * @param definitionByReqType
   * @return the {@link VajramDefinition} corresponding to the registered vajram
   */
  private VajramDefinition registerVajram(
      VajramDefRoot<Object> vajramDef,
      Map<VajramID, VajramDefinition> vajramDefinitions,
      Map<Class<? extends VajramDefRoot>, VajramDefinition> definitionByDefType,
      Map<Class<? extends Request<?>>, VajramDefinition> definitionByReqType) {
    VajramDefinition vajramDefinition = new VajramDefinition(vajramDef);
    VajramID vajramID = vajramDefinition.vajramId();
    if (vajramDefinitions.containsKey(vajramID)) {
      return vajramDefinition;
    }
    vajramDefinitions.put(vajramID, vajramDefinition);
    definitionByDefType.putIfAbsent(vajramDefinition.defType(), vajramDefinition);
    definitionByReqType.putIfAbsent(vajramDefinition.reqRootType(), vajramDefinition);
    return vajramDefinition;
  }

  /**
   * If necessary, creates the kryons for the given vajram and, recursively for its dependencies,
   * and returns the {@link VajramID} of the {@link VajramKryonDefinition} corresponding to this
   * vajram.
   *
   * <p>This method should be called once all necessary vajrams have been registered using the
   * {@link #registerVajram(VajramDefRoot, Map, Map, Map)} method. If a dependency of a vajram is
   * not registered before this step, this method will throw an exception.
   */
  private void loadKryons() {
    for (VajramID vajramID : vajramDefinitions.keySet()) {
      loadKryonSubgraph(vajramID, new LinkedHashSet<>());
    }
  }

  private void loadKryonSubgraph(VajramID vajramId, Set<VajramID> loadingInProgress) {
    if (vajramExecutables.contains(vajramId)) {
      // This means the subgraph is already loaded.
      return;
    } else if (loadingInProgress.contains(vajramId)) {
      // This means the subgraph is still being loaded, but there is a cyclic dependency. Just
      // return the vajramId to prevent infinite recursion.
      return;
    }
    // add to loadingInProgress so that this can be used to prevent infinite recursion in the
    // cases where a vajram depends on itself in a cyclic dependency.
    loadingInProgress.add(vajramId);
    VajramDefinition vajramDefinition = getVajramDefinition(vajramId);
    VajramDefRoot<Object> vajramDefRoot = vajramDefinition.def();
    ImmutableSet<? extends Facet> facets = vajramDefinition.facetSpecs();

    LogicDefinition<CreateNewRequest> createNewRequest =
        new LogicDefinition<>(
            new KryonLogicId(vajramId, "%s:newRequest"),
            ImmutableSet.of(),
            emptyTags(),
            vajramDefRoot::newRequestBuilder);
    if (vajramDefinition.isTrait()) {
      kryonDefinitionRegistry.newTraitKryonDefinition(
          vajramId.id(), facets, createNewRequest, vajramDefinition.vajramTags());
    } else if (vajramDefRoot instanceof VajramDef<Object> vajramDef) {
      InputResolverCreationResult inputResolverCreationResult =
          createKryonLogicsForInputResolvers(vajramDefinition);

      createKryonDefinitionsForDependencies(vajramDefinition, loadingInProgress);
      OutputLogicDefinition<?> outputLogicDefinition =
          createKryonOutputLogic(vajramId, vajramDefinition, vajramDef);
      kryonDefinitionRegistry.newVajramKryonDefinition(
          vajramId,
          facets,
          outputLogicDefinition.kryonLogicId(),
          inputResolverCreationResult.resolversByDefinition(),
          createNewRequest,
          new LogicDefinition<>(
              new KryonLogicId(vajramId, "%s:facetsFromRequest"),
              ImmutableSet.of(),
              emptyTags(),
              vajramDef::facetsFromRequest),
          vajramDef::executeGraph,
          vajramDefinition.vajramTags());
    }
    vajramExecutables.add(vajramId);
  }

  private InputResolverCreationResult createKryonLogicsForInputResolvers(
      VajramDefinition vajramDefinition) {
    VajramID vajramId = vajramDefinition.vajramId();
    ImmutableSet<FacetSpec> facetDefinitions = vajramDefinition.facetSpecs();

    // Create kryon definitions for all input resolvers defined in this vajram
    ImmutableMap<ResolverDefinition, InputResolver> inputResolvers =
        vajramDefinition.inputResolvers();

    Map<ResolverDefinition, Resolver> resolverDefinitions = new LinkedHashMap<>();
    inputResolvers.forEach(
        (resolverId, inputResolver) -> {
          ResolverDefinition inputResolverDefinition = inputResolver.definition();
          Facet dependency = inputResolverDefinition.target().dependency();
          if (dependency == null) {
            throw new IllegalStateException();
          }
          String depName = dependency.name();

          ImmutableSet<? extends Facet> sources = inputResolver.definition().sources();
          ImmutableCollection<FacetSpec> sourceFacets =
              facetDefinitions.stream().filter(sources::contains).collect(toImmutableList());
          LogicDefinition<ResolverLogic> inputResolverLogic =
              logicRegistryDecorator.newResolverLogic(
                  vajramId.id(),
                  "%s:dep(%s):inputResolver(%s)"
                      .formatted(
                          vajramId,
                          dependency,
                          String.join(
                              ",",
                              inputResolver.definition().target().targetInputs().stream()
                                  .map(BasicFacetInfo::name)
                                  .toList())),
                  sources,
                  (depRequests, facets) -> {
                    ResolverCommand resolverCommand;
                    try {
                      validateMandatory(vajramId, facets, sourceFacets);
                      if (inputResolver instanceof One2OneInputResolver singleInputResolver) {
                        resolverCommand = singleInputResolver.resolve(depRequests, facets);
                      } else if (inputResolver instanceof FanoutInputResolver fanoutInputResolver) {
                        if (depRequests.size() != 1) {
                          throw new IllegalStateException(
                              "The vajram "
                                  + vajramId.id()
                                  + " can have at most one fanout resolver");
                        }
                        resolverCommand = fanoutInputResolver.resolve(depRequests.get(0), facets);
                      } else {
                        throw new UnsupportedOperationException(
                            "Unsupported input resolver type: " + inputResolver);
                      }
                    } catch (Throwable t) {
                      resolverCommand =
                          skip(
                              "Got exception '%s' while executing the resolver of the dependency '%s'"
                                  .formatted(t, depName),
                              t);
                    }
                    return resolverCommand;
                  });
          Resolver resolver =
              new Resolver(inputResolverLogic.kryonLogicId(), inputResolver.definition());
          resolverDefinitions.put(resolver.definition(), resolver);
        });
    return new InputResolverCreationResult(ImmutableMap.copyOf(resolverDefinitions));
  }

  private void validateMandatory(
      VajramID vajramID, FacetValues facetValues, ImmutableCollection<FacetSpec> sourceFacets) {
    Map<String, Throwable> missingMandatoryValues = new HashMap<>();
    for (Facet facet : sourceFacets) {
      if (!facet
          .tags()
          .getAnnotationByType(IfAbsent.class)
          .map(ifAbsent -> FAIL.equals(ifAbsent.value()))
          .orElse(false)) {
        continue;
      }
      Facet mandatoryFacet = facet;
      FacetValue facetValue = mandatoryFacet.getFacetValue(facetValues);
      Errable<?> value;
      if (facetValue instanceof SingleFacetValue<?> errableFacetValue) {
        value = errableFacetValue.asErrable();
      } else if (facetValue instanceof FanoutDepResponses<?, ?> fanoutDepResponses) {
        var requestResponsePairs = fanoutDepResponses.requestResponsePairs();
        if (requestResponsePairs.stream()
            .allMatch(reqResp -> reqResp.response().valueOpt().isEmpty())) {
          missingMandatoryValues.put(
              mandatoryFacet.name(),
              new MandatoryFacetMissingException(vajramID.id(), mandatoryFacet.name()));
        }
        continue;
      } else {
        continue;
      }
      Optional<Throwable> error = value.errorOpt();
      if (error.isPresent()) {
        missingMandatoryValues.put(mandatoryFacet.name(), error.get());
      } else {
        try {
          validateMandatoryFacet(
              value.valueOpt().orElse(null), vajramID.id(), mandatoryFacet.name());
        } catch (Throwable e) {
          missingMandatoryValues.put(mandatoryFacet.name(), e);
        }
      }
    }
    if (missingMandatoryValues.isEmpty()) {
      return;
    }
    throw new MandatoryFacetsMissingException(vajramID, missingMandatoryValues);
  }

  private OutputLogicDefinition<?> createKryonOutputLogic(
      VajramID vajramID, VajramDefinition vajramDefinition, VajramDef<Object> vajramDef) {
    VajramID vajramId = vajramDefinition.vajramId();
    ImmutableSet<FacetSpec> facetSpecs = vajramDefinition.facetSpecs();
    ImmutableSet<Facet> kryonOutputLogicSources =
        facetSpecs.stream()
            .filter(vajramDefinition.outputLogicSources()::contains)
            .collect(toImmutableSet());
    KryonLogicId outputLogicName = new KryonLogicId(vajramID, "%s:outputLogic".formatted(vajramId));

    // Step 4: Create and register Kryon for the output logic
    OutputLogic<@Nullable Object> outputLogicCode =
        input -> {
          List<ExecutionItem> inputsList = input.facetValueResponses();
          List<ExecutionItem> validInputs = new ArrayList<>(inputsList.size());
          inputsList.forEach(
              inputs -> {
                try {
                  validateMandatory(vajramId, inputs.facetValues(), facetSpecs);
                  validInputs.add(inputs);
                } catch (Throwable e) {
                  inputs.response().completeExceptionally(wrapAsCompletionException(e));
                }
              });
          try {
            vajramDef.execute(input.withFacetValueResponses(validInputs));
          } catch (Throwable e) {
            validInputs.forEach(i -> i.response().completeExceptionally(wrapAsCompletionException(e)));
          }
        };
    return logicRegistryDecorator.newOutputLogic(
        vajramDef instanceof IOVajramDef<?>,
        outputLogicName,
        kryonOutputLogicSources,
        outputLogicCode,
        vajramDefinition.outputLogicTags());
  }

  private void createKryonDefinitionsForDependencies(
      VajramDefinition vajramDefinition, Set<VajramID> loadingInProgress) {
    List<DependencySpec> dependencies = new ArrayList<>();
    for (Facet facet : vajramDefinition.facetSpecs()) {
      if (facet instanceof DependencySpec definition) {
        dependencies.add(definition);
      }
    }
    // Create and register sub graphs for dependencies of this vajram
    for (DependencySpec dependency : dependencies) {
      var accessSpec = dependency.onVajramID();
      VajramDefinition dependencyVajram = vajramDefinitions.get(accessSpec);
      if (dependencyVajram == null) {
        throw new VajramDefinitionException(
            "Unable to find vajram for vajramId %s".formatted(accessSpec));
      }
      loadKryonSubgraph(dependencyVajram.vajramId(), loadingInProgress);
    }
  }

  private record InputResolverCreationResult(
      ImmutableMap<ResolverDefinition, Resolver> resolversByDefinition) {}

  public VajramDefinition getVajramDefinition(VajramID vajramId) {
    VajramDefinition vajramDefinition = vajramDefinitions.get(vajramId);
    if (vajramDefinition == null) {
      throw new IllegalArgumentException(
          "Could not find vajram definition for id %s".formatted(vajramId));
    }
    return vajramDefinition;
  }

  public VajramID getVajramIdByVajramDefType(Class<? extends VajramDefRoot<?>> vajramDefClass) {
    VajramDefinition vajramDefinition = definitionByDefType.get(vajramDefClass);
    if (vajramDefinition == null) {
      throw new IllegalArgumentException(
          "Could not find vajram definition for class %s".formatted(vajramDefClass));
    }
    return vajramDefinition.vajramId();
  }

  public VajramID getVajramIdByVajramReqType(Class<? extends Request> vajramReqClass) {
    VajramDefinition vajramDefinition = definitionByReqType.get(vajramReqClass);
    if (vajramDefinition == null) {
      throw new IllegalArgumentException(
          "Could not find vajram definition for request type %s".formatted(vajramReqClass));
    }
    return vajramDefinition.vajramId();
  }

  public Optional<Class<? extends Request<?>>> getVajramReqByVajramId(VajramID vajramID) {
    return Optional.ofNullable(vajramDefinitions.get(vajramID)).map(VajramDefinition::reqRootType);
  }

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  public static final class VajramGraphBuilder {
    private final Set<String> packagePrefixes = new LinkedHashSet<>();
    private final Set<Class<? extends VajramDefRoot>> classes = new LinkedHashSet<>();

    public VajramGraphBuilder loadFromPackage(String packagePrefix) {
      packagePrefixes.add(packagePrefix);
      return this;
    }

    @SafeVarargs
    public final VajramGraphBuilder loadClasses(Class<? extends VajramDefRoot>... classes) {
      this.classes.addAll(Arrays.asList(classes));
      return this;
    }

    /**********************************    MAKE PRIVATE   *****************************************/

    @SuppressWarnings({"UnusedMethod", "UnusedVariable", "unused"})
    // Make this private so that client use loadFromPackage instead.
    private VajramGraphBuilder packagePrefixes(Set<String> packagePrefixes) {
      return this;
    }

    @SuppressWarnings({"UnusedMethod", "UnusedVariable", "unused"})
    // Make this private so that client use loadFromPackage instead.
    private VajramGraphBuilder classes(Set<Class<? extends VajramDefRoot>> packagePrefixes) {
      return this;
    }
  }
}
