package com.flipkart.krystal.vajram.graphql.api;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableSet;
import graphql.com.google.common.collect.ImmutableMap;
import graphql.execution.ExecutionContext;
import graphql.normalized.ExecutableNormalizedField;
import graphql.normalized.ExecutableNormalizedOperation;
import graphql.schema.*;
import java.util.*;

public class QueryAnalyseUtil {

  private static final String FIRST_NODE = "QueryGraphQLAggregator";
  public static final String DATA_FETCHER = "dataFetcher";
  public static final String VAJRAM_ID = "vajramId";
  public static final String REF_FETCHER = "refFetcher";
  public static final String ENTITY_FETCHER = "entityFetcher";
  public static final String GRAPH = "graph";
  // New constant for tracking metadata fields
  public static final String METADATA_FIELDS = "metadataFields";

  public static ImmutableSet<DependentChain> getNodeExecutionConfigBasedOnQuery(
      ExecutionContext executionContext,
      Map<String, Map<String, List<String>>> reverseEntityTypeToFieldResolverMap,
      Map<String, Map<String, String>> entityToRefToTypeMap,
      Map<String, Map<String, String>> entityTypeToReferenceFetcher) {

    VajramKryonGraph vajramKryonGraph = executionContext.getGraphQLContext().get(GRAPH);
    GraphQLSchema graphQLSchema = executionContext.getGraphQLSchema();
    ExecutableNormalizedOperation executableNormalizedOperation =
        executionContext.getNormalizedQueryTree().get();
    ImmutableMap<FieldCoordinates, Collection<ExecutableNormalizedField>> queriedFields =
        executableNormalizedOperation.getCoordinatesToNormalizedFields().asMap();
    Set<VajramID> vajramsToExecute = new HashSet<>();
    Set<DependentChain> dependentChainToSkip = new HashSet<>();
    String queriedEntity =
        executionContext.getGraphQLSchema().getQueryType().getName().toUpperCase();

    Set<String> metadataFields = new HashSet<>();

    // Scan for metadata fields at all levels
    scanForMetadataFields(executableNormalizedOperation.getTopLevelFields(), metadataFields);

    // Store the metadata fields in the context for later use
    if (!metadataFields.isEmpty()) {
      executionContext.getGraphQLContext().put(METADATA_FIELDS, metadataFields);
    }

    for (FieldCoordinates entry : queriedFields.keySet()) {
      // Metadata fields have already been handled
      if (entry.getFieldName().startsWith("__")) {
        continue;
      }

      GraphQLAppliedDirective graphQLAppliedDirective =
          graphQLSchema.getFieldDefinition(entry).getAppliedDirective(DATA_FETCHER);

      if (graphQLAppliedDirective != null) {

        vajramsToExecute.add(
            VajramID.vajramID(
                graphQLAppliedDirective.getArgument(VAJRAM_ID).getValue().toString()));
      }
      GraphQLAppliedDirective graphQLAppliedDirectiveRef =
          graphQLSchema.getFieldDefinition(entry).getAppliedDirective(REF_FETCHER);
      if (graphQLAppliedDirectiveRef != null) {
        vajramsToExecute.add(
            VajramID.vajramID(
                graphQLAppliedDirectiveRef.getArgument(ENTITY_FETCHER).getValue().toString()));
      }
    }

    setSkipDependentChains(
        vajramsToExecute,
        dependentChainToSkip,
        vajramKryonGraph,
        queriedEntity,
        reverseEntityTypeToFieldResolverMap,
        entityToRefToTypeMap,
        entityTypeToReferenceFetcher);
    return ImmutableSet.copyOf(dependentChainToSkip);
  }

  /**
   * Recursively scan the query for metadata fields like __typename. This ensures we detect them
   * regardless of their position in the query
   */
  private static void scanForMetadataFields(
      Collection<ExecutableNormalizedField> fields, Set<String> metadataFields) {
    for (ExecutableNormalizedField field : fields) {
      if (field.getName().startsWith("__")) {
        metadataFields.add(field.getName());
      }

      // Recursively scan child fields
      if (!field.getChildren().isEmpty()) {
        scanForMetadataFields(field.getChildren(), metadataFields);
      }
    }
  }

  private static void setSkipDependentChains(
      Set<VajramID> vajramsToExecute,
      Set<DependentChain> dependentChainToSkip,
      VajramKryonGraph vajramNodeGraph,
      String queriedEntity,
      Map<String, Map<String, List<String>>> reverseEntityTypeToFieldResolverMap,
      Map<String, Map<String, String>> entityToRefToTypeMap,
      Map<String, Map<String, String>> entityTypeToReferenceFetcher) {

    var facetsByName =
        vajramNodeGraph.getVajramDefinition(VajramID.vajramID(FIRST_NODE)).facetsByName();
    for (Map.Entry<String, Map<String, List<String>>> entityToFieldEntry :
        reverseEntityTypeToFieldResolverMap.entrySet()) {
      if (queriedEntity.equalsIgnoreCase(entityToFieldEntry.getKey())) {
        List<Dependency> depList = new ArrayList<>();
        depList.add((Dependency) facetsByName.get(queriedEntity.toLowerCase()));
        setSkipDependentChainPerEntity(
            vajramsToExecute,
            dependentChainToSkip,
            vajramNodeGraph,
            entityToFieldEntry.getKey(),
            depList,
            entityToRefToTypeMap,
            reverseEntityTypeToFieldResolverMap,
            entityTypeToReferenceFetcher);
      } else {
        for (String fieldTypeDep : entityToFieldEntry.getValue().keySet()) {
          DependentChain dependentChain =
              vajramNodeGraph.computeDependentChain(
                  FIRST_NODE,
                  (Dependency)
                      requireNonNull(facetsByName.get(entityToFieldEntry.getKey().toLowerCase())),
                  (Dependency) requireNonNull(facetsByName.get(fieldTypeDep)));
          dependentChainToSkip.add(dependentChain);
        }
      }
    }
  }

  private static void setSkipDependentChainPerEntity(
      Set<VajramID> vajramsToExecute,
      Set<DependentChain> dependentChainToSkip,
      VajramKryonGraph vajramNodeGraph,
      String entity,
      List<Dependency> dependencyList,
      Map<String, Map<String, String>> entityToRefToTypeMap,
      Map<String, Map<String, List<String>>> reverseEntityTypeToFieldResolverMap,
      Map<String, Map<String, String>> entityTypeToReferenceFetcher) {
    for (String vajram : reverseEntityTypeToFieldResolverMap.get(entity).keySet()) {
      if (!vajramsToExecute.contains(VajramID.vajramID(vajram))) {
        Dependency mostRecentDependency = null;
        Dependency[] depChain = new Dependency[dependencyList.size()];
        if (dependencyList.size() > 1) {
          for (int i = 1; i < dependencyList.size(); i++) {
            mostRecentDependency = dependencyList.get(i);
            depChain[i - 1] = mostRecentDependency;
          }
        }
        if (mostRecentDependency instanceof DependencySpec<?, ?, ?> dependencySpec) {
          VajramID mostRecentVajram = dependencySpec.onVajramID();
          var facetsByName = vajramNodeGraph.getVajramDefinition(mostRecentVajram).facetsByName();
          depChain[dependencyList.size() - 1] = (Dependency) facetsByName.get(vajram);
          DependentChain dependentChain =
              vajramNodeGraph.computeDependentChain(FIRST_NODE, dependencyList.get(0), depChain);
          dependentChainToSkip.add(dependentChain);
        } else {
          throw new UnsupportedOperationException(
              "Unknown dependency type: " + mostRecentDependency);
        }
      }
    }
    if (!dependencyList.isEmpty()) {
      Dependency mostRecentDependency = dependencyList.get(dependencyList.size() - 1);
      if (mostRecentDependency instanceof DependencySpec<?, ?, ?> dependencySpec) {
        VajramID mostRecentVajram = dependencySpec.onVajramID();
        var facetsByName = vajramNodeGraph.getVajramDefinition(mostRecentVajram).facetsByName();
        for (Map.Entry<String, String> refFieldVajram :
            entityTypeToReferenceFetcher.get(entity).entrySet()) {

          List<Dependency> newDepList = new ArrayList<>(dependencyList);
          newDepList.add((Dependency) facetsByName.get(refFieldVajram.getKey()));
          setSkipDependentChainPerEntity(
              vajramsToExecute,
              dependentChainToSkip,
              vajramNodeGraph,
              entityToRefToTypeMap.get(entity).get(refFieldVajram.getKey()).toUpperCase(),
              newDepList,
              entityToRefToTypeMap,
              reverseEntityTypeToFieldResolverMap,
              entityTypeToReferenceFetcher);
        }
      }
    } else {
      throw new IllegalStateException("Empty dependency not yet handled");
    }
  }
}