package com.flipkart.krystal.krystex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KrystalNodeExecutorTest {

  private KrystalNodeExecutor krystalNodeExecutor;
  private NodeDefinitionRegistry nodeDefinitionRegistry;

  private static ImmutableList<String> timedGet(CompletableFuture<ImmutableList<String>> allResults)
      throws InterruptedException, ExecutionException, TimeoutException {
    return allResults.get(5, TimeUnit.HOURS);
  }

  @BeforeEach
  void setUp() {
    this.nodeDefinitionRegistry = new NodeDefinitionRegistry();
    this.krystalNodeExecutor = new KrystalNodeExecutor(nodeDefinitionRegistry, "test");
  }

  @AfterEach
  void tearDown() {
    this.krystalNodeExecutor.close();
  }

  @Test
  void requestExecution_noDependencies_success() throws Exception {
    NonBlockingNodeDefinition<String> nodeDefinition =
        nodeDefinitionRegistry.newNonBlockingNode(
            "requestExecution_noDependencies_success_nodeName",
            dependencyValues -> "computed_value");

    ImmutableList<String> results =
        timedGet(krystalNodeExecutor.execute(nodeDefinition).getAllResults());
    assertEquals(List.of("computed_value"), results);
  }

  @Test
  void requestExecution_unboundInputs_success() throws Exception {
    NonBlockingNodeDefinition<String> nodeDefinition =
        nodeDefinitionRegistry.newNonBlockingNode(
            "requestExecution_noDependencies_success_nodeName",
            Set.of("a", "b", "c"),
            dependencyValues ->
                "computed_values: a=%s;b=%s;c=%s"
                    .formatted(
                        dependencyValues.get("a"),
                        dependencyValues.get("b"),
                        dependencyValues.get("c")));

    Node<String> node = krystalNodeExecutor.execute(nodeDefinition);
    krystalNodeExecutor.provideInputs(node, Map.of("a", 1, "b", 2, "c", "3"));
    ImmutableList<String> results = timedGet(node.getAllResults());
    assertEquals(List.of("computed_values: a=1;b=2;c=3"), results);
  }

  @Test
  void requestExecution_singleDependency_success() throws Exception {
    String dependencyNodeId = "requestExecution_singleDependency_dependencyNode";

    nodeDefinitionRegistry.newNonBlockingNode(
        dependencyNodeId, dependencyValues -> "dependency_value");

    ImmutableList<String> results =
        timedGet(
            krystalNodeExecutor
                .execute(
                    nodeDefinitionRegistry.newNonBlockingNode(
                        "requestExecution_singleDependency_requiredNode",
                        ImmutableMap.of("input", dependencyNodeId),
                        dependencyValues ->
                            dependencyValues.get("input") + ":computed_value"))
                .getAllResults());
    assertEquals(List.of("dependency_value:computed_value"), results);
  }

  @Test
  void requestExecution_multiLevelDependencies_success() throws Exception {
    String l1Dep = "requestExecution_multiLevelDependencies_level1";
    nodeDefinitionRegistry.newNonBlockingNode(l1Dep, dependencyValues -> "l1");

    String l2Dep = "requestExecution_multiLevelDependencies_level2";
    nodeDefinitionRegistry.newNonBlockingNode(
        l2Dep,
        ImmutableMap.of("input", l1Dep),
        dependencyValues -> dependencyValues.get("input") + ":l2");

    String l3Dep = "requestExecution_multiLevelDependencies_level3";
    nodeDefinitionRegistry.newNonBlockingNode(
        l3Dep,
        ImmutableMap.of("input", l2Dep),
        dependencyValues -> dependencyValues.get("input") + ":l3");

    String l4Dep = "requestExecution_multiLevelDependencies_level4";
    nodeDefinitionRegistry.newNonBlockingNode(
        l4Dep,
        ImmutableMap.of("input", l3Dep),
        dependencyValues -> dependencyValues.get("input") + ":l4");

    ImmutableList<String> results =
        timedGet(
            krystalNodeExecutor
                .execute(
                    nodeDefinitionRegistry.newNonBlockingNode(
                        "requestExecution_multiLevelDependencies_final",
                        ImmutableMap.of("input", l4Dep),
                        dependencyValues -> dependencyValues.get("input") + ":final"))
                .getAllResults());
    assertEquals(List.of("l1:l2:l3:l4:final"), results);
  }

  @Test
  void stopAcceptingRequests_preventsNewExecutionRequests() {
    krystalNodeExecutor.stopAcceptingRequests();
    assertThrows(
        IllegalStateException.class,
        () ->
            krystalNodeExecutor.execute(
                nodeDefinitionRegistry.newNonBlockingNode(
                    "shutdown_preventsNewExecutionRequests",
                    ImmutableMap.of(),
                    dependencyValues -> ImmutableList.of(""))));
  }

  @Test
  void close_preventsNewExecutionRequests() {
    krystalNodeExecutor.close();
    assertThrows(
        IllegalStateException.class,
        () ->
            krystalNodeExecutor.execute(
                nodeDefinitionRegistry.newNonBlockingNode(
                    "shutdown_preventsNewExecutionRequests",
                    ImmutableMap.of(),
                    dependencyValues -> ImmutableList.of(""))));
  }
}
