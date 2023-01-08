package com.flipkart.krystal.krystex.node;

import static com.flipkart.krystal.data.ValueOrError.withValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.RequestId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KrystalNodeExecutorTest {

  private KrystalNodeExecutor krystalNodeExecutor;
  private NodeDefinitionRegistry nodeDefinitionRegistry;
  private LogicDefinitionRegistry logicDefinitionRegistry;

  private static <T> T timedGet(CompletableFuture<T> future)
      throws InterruptedException, ExecutionException, TimeoutException {
    return future.get(5, TimeUnit.HOURS);
  }

  @BeforeEach
  void setUp() {
    this.logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.nodeDefinitionRegistry = new NodeDefinitionRegistry(logicDefinitionRegistry);
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
            newComputeLogic(
                    "nodeLogic", Collections.emptySet(), dependencyValues -> "computed_value")
                .nodeLogicId());

    Object result =
        timedGet(
            krystalNodeExecutor
                .executeNode(nodeDefinition.nodeId(), Inputs.empty(), new RequestId("req_1"))
                .responseFuture());
    assertEquals("computed_value", result);
  }

  @Test
  void requestExecution_unboundInputs_success() throws Exception {
    String logicId = "requestExecution_noDependencies_success_nodeName";
    NodeId nodeId =
        nodeDefinitionRegistry
            .newNodeDefinition(
                logicId,
                newComputeLogic(
                        logicId,
                        Set.of("a", "b", "c"),
                        inputs ->
                            "computed_values: a=%s;b=%s;c=%s"
                                .formatted(
                                    inputs.getInputValue("a").value().orElseThrow(),
                                    inputs.getInputValue("b").value().orElseThrow(),
                                    inputs.getInputValue("c").value().orElseThrow()))
                    .nodeLogicId(),
                ImmutableMap.of(),
                ImmutableList.of())
            .nodeId();
    Object result =
        timedGet(
            krystalNodeExecutor
                .executeNode(
                    nodeId,
                    new Inputs(
                        ImmutableMap.of("a", withValue(1), "b", withValue(2), "c", withValue("3"))),
                    new RequestId("r"))
                .responseFuture());
    assertEquals("computed_values: a=1;b=2;c=3", result);
  }

  @Test
  void requestExecution_singleDependency_success() throws Exception {
    NodeDefinition n1 =
        nodeDefinitionRegistry.newNodeDefinition(
            "n1",
            newComputeLogic(
                    "n1_logic", Collections.emptySet(), dependencyValues -> "dependency_value")
                .nodeLogicId());

    NodeDefinition n2 =
        nodeDefinitionRegistry.newNodeDefinition(
            "n2",
            newComputeLogic(
                    "n2_logic",
                    ImmutableSet.of("dep"),
                    dependencyValues ->
                        dependencyValues.getInputValue("dep").value().orElseThrow()
                            + ":computed_value")
                .nodeLogicId(),
            ImmutableMap.of("dep", n1.nodeId()),
            ImmutableList.of());

    Object results =
        timedGet(
            krystalNodeExecutor
                .executeNode(n2.nodeId(), Inputs.empty(), new RequestId("r1"))
                .responseFuture());

    assertEquals("dependency_value:computed_value", results);
  }

  @Test
  void requestExecution_multiLevelDependencies_success() throws Exception {
    String l1Dep = "requestExecution_multiLevelDependencies_level1";
    nodeDefinitionRegistry.newNodeDefinition(
        l1Dep,
        newComputeLogic(l1Dep, Collections.emptySet(), dependencyValues -> "l1").nodeLogicId());

    String l2Dep = "requestExecution_multiLevelDependencies_level2";
    nodeDefinitionRegistry.newNodeDefinition(
        l2Dep,
        newComputeLogic(
                l2Dep,
                ImmutableSet.of("input"),
                dependencyValues ->
                    dependencyValues.getInputValue("input").value().orElseThrow() + ":l2")
            .nodeLogicId(),
        ImmutableMap.of("input", new NodeId(l1Dep)));

    String l3Dep = "requestExecution_multiLevelDependencies_level3";
    nodeDefinitionRegistry.newNodeDefinition(
        l3Dep,
        newComputeLogic(
                l3Dep,
                ImmutableSet.of("input"),
                dependencyValues ->
                    dependencyValues.getInputValue("input").value().orElseThrow() + ":l3")
            .nodeLogicId(),
        ImmutableMap.of("input", new NodeId(l2Dep)));

    String l4Dep = "requestExecution_multiLevelDependencies_level4";
    nodeDefinitionRegistry.newNodeDefinition(
        l4Dep,
        newComputeLogic(
                l4Dep,
                ImmutableSet.of("input"),
                dependencyValues ->
                    dependencyValues.getInputValue("input").value().orElseThrow() + ":l4")
            .nodeLogicId(),
        ImmutableMap.of("input", new NodeId(l3Dep)));

    Object results =
        timedGet(
            krystalNodeExecutor
                .executeNode(
                    nodeDefinitionRegistry
                        .newNodeDefinition(
                            "requestExecution_multiLevelDependencies_final",
                            newComputeLogic(
                                    "requestExecution_multiLevelDependencies_final",
                                    ImmutableSet.of("input"),
                                    dependencyValues ->
                                        dependencyValues
                                                .getInputValue("input")
                                                .value()
                                                .orElseThrow()
                                            + ":final")
                                .nodeLogicId(),
                            ImmutableMap.of("input", new NodeId(l4Dep)))
                        .nodeId(),
                    Inputs.empty(),
                    new RequestId("r"))
                .responseFuture());
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
                        newComputeLogic(
                                "shutdown_preventsNewExecutionRequests",
                                ImmutableSet.of(),
                                dependencyValues -> "")
                            .nodeLogicId())
                    .nodeId(),
                Inputs.empty(),
                new RequestId("")));
  }

  private <T> MainLogicDefinition<T> newComputeLogic(
      String nodeId, Set<String> inputs, Function<Inputs, T> logic) {
    ComputeLogicDefinition<T> def =
        new ComputeLogicDefinition<>(new NodeLogicId(nodeId), inputs, logic);
    logicDefinitionRegistry.addMainLogic(def);
    return def;
  }
}
