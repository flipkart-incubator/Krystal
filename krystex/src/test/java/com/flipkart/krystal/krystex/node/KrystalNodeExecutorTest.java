package com.flipkart.krystal.krystex.node;

import static com.flipkart.krystal.data.ValueOrError.valueOrError;
import static com.flipkart.krystal.data.ValueOrError.withValue;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.ForkJoinExecutorPool;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
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

  @BeforeEach
  void setUp() {
    this.logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.nodeDefinitionRegistry = new NodeDefinitionRegistry(logicDefinitionRegistry);
    this.krystalNodeExecutor =
        new KrystalNodeExecutor(
            nodeDefinitionRegistry,
            new LogicDecorationOrdering(ImmutableSet.of()),
            new ForkJoinExecutorPool(1),
            "test");
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

    CompletableFuture<Object> future =
        krystalNodeExecutor.executeNode(nodeDefinition.nodeId(), Inputs.empty(), "req_1");
    krystalNodeExecutor.flush();
    assertEquals("computed_value", timedGet(future));
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
                    .nodeLogicId())
            .nodeId();
    CompletableFuture<Object> future =
        krystalNodeExecutor.executeNode(
            nodeId,
            new Inputs(ImmutableMap.of("a", withValue(1), "b", withValue(2), "c", withValue("3"))),
            "r");
    krystalNodeExecutor.flush();
    assertEquals("computed_values: a=1;b=2;c=3", timedGet(future));
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
                        dependencyValues
                                .getDepValue("dep")
                                .values()
                                .values()
                                .iterator()
                                .next()
                                .value()
                                .orElseThrow()
                            + ":computed_value")
                .nodeLogicId(),
            ImmutableMap.of("dep", n1.nodeId()));

    CompletableFuture<Object> future =
        krystalNodeExecutor.executeNode(n2.nodeId(), Inputs.empty(), "r1");
    krystalNodeExecutor.flush();
    assertEquals("dependency_value:computed_value", timedGet(future));
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
                    dependencyValues
                            .getDepValue("input")
                            .values()
                            .values()
                            .iterator()
                            .next()
                            .value()
                            .orElseThrow()
                        + ":l2")
            .nodeLogicId(),
        ImmutableMap.of("input", new NodeId(l1Dep)));

    String l3Dep = "requestExecution_multiLevelDependencies_level3";
    nodeDefinitionRegistry.newNodeDefinition(
        l3Dep,
        newComputeLogic(
                l3Dep,
                ImmutableSet.of("input"),
                dependencyValues -> {
                  return dependencyValues
                          .getDepValue("input")
                          .values()
                          .values()
                          .iterator()
                          .next()
                          .value()
                          .orElseThrow()
                      + ":l3";
                })
            .nodeLogicId(),
        ImmutableMap.of("input", new NodeId(l2Dep)));

    String l4Dep = "requestExecution_multiLevelDependencies_level4";
    nodeDefinitionRegistry.newNodeDefinition(
        l4Dep,
        newComputeLogic(
                l4Dep,
                ImmutableSet.of("input"),
                dependencyValues ->
                    dependencyValues
                            .getDepValue("input")
                            .values()
                            .values()
                            .iterator()
                            .next()
                            .value()
                            .orElseThrow()
                        + ":l4")
            .nodeLogicId(),
        ImmutableMap.of("input", new NodeId(l3Dep)));

    Inputs inputs = Inputs.empty();
    CompletableFuture<Object> future =
        krystalNodeExecutor.executeNode(
            nodeDefinitionRegistry
                .newNodeDefinition(
                    "requestExecution_multiLevelDependencies_final",
                    newComputeLogic(
                            "requestExecution_multiLevelDependencies_final",
                            ImmutableSet.of("input"),
                            dependencyValues ->
                                dependencyValues
                                        .getDepValue("input")
                                        .values()
                                        .values()
                                        .iterator()
                                        .next()
                                        .value()
                                        .orElseThrow()
                                    + ":final")
                        .nodeLogicId(),
                    ImmutableMap.of("input", new NodeId(l4Dep)))
                .nodeId(),
            inputs,
            "r");
    krystalNodeExecutor.flush();
    assertEquals("l1:l2:l3:l4:final", timedGet(future));
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
                ""));
  }

  /* So that bad testcases do not hang indefinitely.*/
  private static <T> T timedGet(CompletableFuture<T> future)
      throws InterruptedException, ExecutionException, TimeoutException {
    return future.get(1, TimeUnit.SECONDS);
  }

  private <T> MainLogicDefinition<T> newComputeLogic(
      String nodeId, Set<String> inputs, Function<Inputs, T> logic) {
    ComputeLogicDefinition<T> def =
        new ComputeLogicDefinition<>(
            new NodeLogicId(nodeId),
            inputs,
            inputsList ->
                inputsList.stream().collect(toImmutableMap(identity(), valueOrError(logic))),
            ImmutableMap.of());
    logicDefinitionRegistry.addMainLogic(def);
    return def;
  }
}
