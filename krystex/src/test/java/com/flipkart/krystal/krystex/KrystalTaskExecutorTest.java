package com.flipkart.krystal.krystex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KrystalTaskExecutorTest {

  private KrystalTaskExecutor krystalTaskExecutor;
  private NodeDefinitionRegistry nodeDefinitionRegistry;

  @BeforeEach
  void setUp() {
    this.nodeDefinitionRegistry = new NodeDefinitionRegistry();
    this.krystalTaskExecutor = new KrystalTaskExecutor(nodeDefinitionRegistry);
  }

  @Test
  void requestExecution_noDependencies_success() throws Exception {
    NonBlockingNodeDefinition<String> nodeDefinition =
        nodeDefinitionRegistry.newUnboundNonBlockingNode(
            "requestExecution_noDependencies_success_nodeName",
            dependencyValues -> "computed_value");

    ImmutableList<Result<String>> results = krystalTaskExecutor.requestExecution(nodeDefinition);
    assertEquals(1, results.size());
    assertEquals("computed_value", results.get(0).future().get(5, TimeUnit.MINUTES));
  }

  @Test
  void requestExecution_singleDependency_success() throws Exception {
    String dependencyNodeId = "requestExecution_singleDependency_dependencyNode";

    nodeDefinitionRegistry.newUnboundNonBlockingNode(
        dependencyNodeId, dependencyValues -> "dependency_value");

    ImmutableList<Result<Object>> results =
        krystalTaskExecutor.requestExecution(
            nodeDefinitionRegistry.newNonBlockingNode(
                "requestExecution_singleDependency_requiredNode",
                ImmutableMap.of(dependencyNodeId, dependencyNodeId),
                dependencyValues -> dependencyValues.get(dependencyNodeId) + ":computed_value"));
    assertEquals(1, results.size());
    assertEquals(
        "dependency_value:computed_value", results.get(0).future().get(5, TimeUnit.MINUTES));
  }

  @Test
  void requestExecution_multiLevelDependencies_success() throws Exception {
    String l1Dep = "requestExecution_multiLevelDependencies_level1";
    nodeDefinitionRegistry.newUnboundNonBlockingNode(l1Dep, dependencyValues -> "l1");

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

    ImmutableList<Result<String>> results =
        krystalTaskExecutor.requestExecution(
            nodeDefinitionRegistry.newNonBlockingNode(
                "requestExecution_multiLevelDependencies_final",
                ImmutableMap.of("input", l4Dep),
                dependencyValues -> dependencyValues.get("input") + ":final"));
    assertEquals(1, results.size());
    assertEquals("l1:l2:l3:l4:final", results.get(0).future().get(5, TimeUnit.MINUTES));
  }

  @Test
  void shutdown_preventsNewExecutionRequests() {
    krystalTaskExecutor.shutdown();
    assertThrows(
        IllegalStateException.class,
        () ->
            nodeDefinitionRegistry.newNonBlockingNode(
                "shutdown_preventsNewExecutionRequests",
                ImmutableMap.of(),
                dependencyValues -> ImmutableList.of("")));
  }
}
