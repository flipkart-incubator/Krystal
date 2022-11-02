package com.flipkart.krystal.krystex;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import java.util.Set;
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
        nodeDefinitionRegistry.newNonBlockingNode(
            "requestExecution_noDependencies_success_nodeName",
            emptySet(),
            dependencyValues -> "computed_value");

    Result<String> result = krystalTaskExecutor.requestExecution(nodeDefinition);
    assertEquals("computed_value", result.future().get(5, TimeUnit.MINUTES));
  }

  @Test
  void requestExecution_singleDependency_success() throws Exception {
    String dependencyNodeId = "requestExecution_singleDependency_dependencyNode";

    nodeDefinitionRegistry.newNonBlockingNode(
        dependencyNodeId, emptySet(), dependencyValues -> "dependency_value");

    Result<String> result =
        krystalTaskExecutor.requestExecution(
            nodeDefinitionRegistry.newNonBlockingNode(
                "requestExecution_singleDependency_requiredNode",
                Set.of(dependencyNodeId),
                dependencyValues -> dependencyValues.get(dependencyNodeId) + ":computed_value"));
    assertEquals("dependency_value:computed_value", result.future().get(5, TimeUnit.MINUTES));
  }

  @Test
  void requestExecution_multiLevelDependencies_success() throws Exception {
    String l1Dep = "requestExecution_multiLevelDependencies_level1";
    nodeDefinitionRegistry.newNonBlockingNode(l1Dep, emptySet(), dependencyValues -> "l1");

    String l2Dep = "requestExecution_multiLevelDependencies_level2";
    nodeDefinitionRegistry.newNonBlockingNode(
        l2Dep, Set.of(l1Dep), dependencyValues -> dependencyValues.get(l1Dep) + ":l2");

    String l3Dep = "requestExecution_multiLevelDependencies_level3";
    nodeDefinitionRegistry.newNonBlockingNode(
        l3Dep, Set.of(l2Dep), dependencyValues -> dependencyValues.get(l2Dep) + ":l3");

    String l4Dep = "requestExecution_multiLevelDependencies_level4";
    nodeDefinitionRegistry.newNonBlockingNode(
        l4Dep, Set.of(l3Dep), dependencyValues -> dependencyValues.get(l3Dep) + ":l4");

    Result<String> result =
        krystalTaskExecutor.requestExecution(
            nodeDefinitionRegistry.newNonBlockingNode(
                "requestExecution_multiLevelDependencies_final",
                Set.of(l4Dep),
                dependencyValues -> dependencyValues.get(l4Dep) + ":final"));
    assertEquals("l1:l2:l3:l4:final", result.future().get(5, TimeUnit.MINUTES));
  }

  @Test
  void shutdown_preventsNewExecutionRequests() {
    krystalTaskExecutor.shutdown();
    assertThrows(
        IllegalStateException.class,
        () ->
            krystalTaskExecutor.requestExecution(
                new NonBlockingNodeDefinition<String>(
                    "shutdown_preventsNewExecutionRequests", emptySet()) {
                  @Override
                  protected String nonBlockingLogic(ImmutableMap<String, ?> dependencyValues) {
                    return "";
                  }
                }));
  }
}
