package com.flipkart.krystal.krystex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flipkart.krystal.krystex.node.KrystalNodeExecutor;
import com.flipkart.krystal.krystex.node.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.node.NodeDefinition;
import com.flipkart.krystal.krystex.node.NodeDefinitionRegistry;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeInputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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

  private static <T> T timedGet(CompletableFuture<T> future)
      throws InterruptedException, ExecutionException, TimeoutException {
    return future.get(5, TimeUnit.HOURS);
  }

  @BeforeEach
  void setUp() {
    nodeDefinitionRegistry = new NodeDefinitionRegistry(new LogicDefinitionRegistry());
    this.krystalNodeExecutor = new KrystalNodeExecutor(nodeDefinitionRegistry, "test");
  }

  @AfterEach
  void tearDown() {
    this.krystalNodeExecutor.close();
  }

  @Test
  void requestExecution_noDependencies_success() throws Exception {
    NodeDefinition nodeDefinition =
        nodeDefinitionRegistry.newNodeDefinition(
            "node",
            nodeDefinitionRegistry
                .logicDefinitionRegistry()
                .newComputeLogic("nodeLogic", dependencyValues -> "computed_value")
                .nodeLogicId());

    String result =
        timedGet(
            krystalNodeExecutor.executeNode(
                nodeDefinition.nodeId(), new NodeInputs(), new RequestId("req_1")));
    assertEquals("computed_value", result);
  }

  @Test
  void requestExecution_unboundInputs_success() throws Exception {
    String logicId = "requestExecution_noDependencies_success_nodeName";
    NodeId nodeId =
        nodeDefinitionRegistry
            .newNodeDefinition(
                logicId,
                nodeDefinitionRegistry
                    .logicDefinitionRegistry()
                    .newComputeLogic(
                        logicId,
                        Set.of("a", "b", "c"),
                        dependencyValues ->
                            "computed_values: a=%s;b=%s;c=%s"
                                .formatted(
                                    dependencyValues.getValue("a").value().orElseThrow(),
                                    dependencyValues.getValue("b").value().orElseThrow(),
                                    dependencyValues.getValue("c").value().orElseThrow()))
                    .nodeLogicId(),
                ImmutableMap.of(),
                ImmutableList.of())
            .nodeId();
    String result =
        timedGet(
            krystalNodeExecutor.executeNode(
                nodeId,
                new NodeInputs(
                    ImmutableMap.of(
                        "a",
                        new SingleValue<Object>(1),
                        "b",
                        new SingleValue<Object>(2),
                        "c",
                        new SingleValue<Object>("3"))),
                new RequestId("r")));
    assertEquals("computed_values: a=1;b=2;c=3", result);
  }

  @Test
  void requestExecution_singleDependency_success() throws Exception {
    LogicDefinitionRegistry logicDefinitionRegistry =
        nodeDefinitionRegistry.logicDefinitionRegistry();
    NodeDefinition n1 =
        nodeDefinitionRegistry.newNodeDefinition(
            "n1",
            logicDefinitionRegistry
                .newComputeLogic("n1_logic", dependencyValues -> "dependency_value")
                .nodeLogicId());

    NodeDefinition n2 =
        nodeDefinitionRegistry.newNodeDefinition(
            "n2",
            logicDefinitionRegistry
                .newComputeLogic(
                    "n2_logic",
                    ImmutableSet.of("dep"),
                    dependencyValues ->
                        dependencyValues.get("dep").orElseThrow() + ":computed_value")
                .nodeLogicId(),
            ImmutableMap.of("dep", n1.nodeId()),
            ImmutableList.of());

    String results =
        timedGet(
            krystalNodeExecutor.executeNode(n2.nodeId(), new NodeInputs(), new RequestId("r1")));

    assertEquals("dependency_value:computed_value", results);
  }

  @Test
  void requestExecution_multiLevelDependencies_success() throws Exception {
    LogicDefinitionRegistry logicDefinitionRegistry =
        nodeDefinitionRegistry.logicDefinitionRegistry();
    String l1Dep = "requestExecution_multiLevelDependencies_level1";
    nodeDefinitionRegistry.newNodeDefinition(
        l1Dep,
        logicDefinitionRegistry.newComputeLogic(l1Dep, dependencyValues -> "l1").nodeLogicId());

    String l2Dep = "requestExecution_multiLevelDependencies_level2";
    nodeDefinitionRegistry.newNodeDefinition(
        l2Dep,
        logicDefinitionRegistry
            .newComputeLogic(
                l2Dep,
                ImmutableSet.of("input"),
                dependencyValues -> dependencyValues.getOrThrow("input") + ":l2")
            .nodeLogicId(),
        ImmutableMap.of("input", new NodeId(l1Dep)));

    String l3Dep = "requestExecution_multiLevelDependencies_level3";
    nodeDefinitionRegistry.newNodeDefinition(
        l3Dep,
        logicDefinitionRegistry
            .newComputeLogic(
                l3Dep,
                ImmutableSet.of("input"),
                dependencyValues -> dependencyValues.getOrThrow("input") + ":l3")
            .nodeLogicId(),
        ImmutableMap.of("input", new NodeId(l2Dep)));

    String l4Dep = "requestExecution_multiLevelDependencies_level4";
    nodeDefinitionRegistry.newNodeDefinition(
        l4Dep,
        logicDefinitionRegistry
            .newComputeLogic(
                l4Dep,
                ImmutableSet.of("input"),
                dependencyValues -> dependencyValues.getOrThrow("input") + ":l4")
            .nodeLogicId(),
        ImmutableMap.of("input", new NodeId(l3Dep)));

    String results =
        timedGet(
            krystalNodeExecutor.executeNode(
                nodeDefinitionRegistry
                    .newNodeDefinition(
                        "requestExecution_multiLevelDependencies_final",
                        logicDefinitionRegistry
                            .newComputeLogic(
                                "requestExecution_multiLevelDependencies_final",
                                ImmutableSet.of("input"),
                                dependencyValues -> dependencyValues.getOrThrow("input") + ":final")
                            .nodeLogicId(),
                        ImmutableMap.of("input", new NodeId(l4Dep)))
                    .nodeId(),
                new NodeInputs(),
                new RequestId("r")));
    assertEquals("l1:l2:l3:l4:final", results);
  }

  @Test
  void close_preventsNewExecutionRequests() {
    krystalNodeExecutor.close();
    assertThrows(
        Exception.class,
        () ->
            krystalNodeExecutor.executeNode(
                nodeDefinitionRegistry
                    .newNodeDefinition(
                        "shutdown_preventsNewExecutionRequests",
                        nodeDefinitionRegistry
                            .logicDefinitionRegistry()
                            .newComputeLogic(
                                "shutdown_preventsNewExecutionRequests",
                                ImmutableSet.of(),
                                dependencyValues -> ImmutableList.of(""))
                            .nodeLogicId())
                    .nodeId(),
                new NodeInputs(),
                new RequestId("")));
  }
}
