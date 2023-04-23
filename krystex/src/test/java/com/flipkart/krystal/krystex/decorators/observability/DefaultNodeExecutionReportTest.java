package com.flipkart.krystal.krystex.decorators.observability;

import static com.flipkart.krystal.data.ValueOrError.withValue;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.decorators.observability.DefaultNodeExecutionReport.LogicExecInfo;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultNodeExecutionReportTest {
  public static final Instant CREATE_TIME = Instant.ofEpochMilli(0);
  private static final Instant START_TIME = Instant.ofEpochMilli(1);
  private static final Instant END_TIME = Instant.ofEpochMilli(100);
  private FakeClock clock;
  private DefaultNodeExecutionReport nodeExecutionReport;

  @BeforeEach
  void setUp() {
    clock = new FakeClock(CREATE_TIME);
    nodeExecutionReport = new DefaultNodeExecutionReport(clock);
  }

  @Test
  void reportStartAndEnd_success() {
    NodeId nodeId = new NodeId("node_1");
    NodeLogicId nodeLogicId = new NodeLogicId(nodeId, "node_1__logic_1");
    ImmutableList<Inputs> inputsList =
        ImmutableList.of(
            new Inputs(ImmutableMap.of("i1", withValue("v1"))),
            new Inputs(ImmutableMap.of("i1", withValue("v2"))));

    clock.setInstant(START_TIME);
    nodeExecutionReport.reportMainLogicStart(nodeId, nodeLogicId, inputsList);

    String result = "r1";
    clock.setInstant(END_TIME);
    nodeExecutionReport.reportMainLogicEnd(
        nodeId,
        nodeLogicId,
        new Results<>(
            inputsList.stream().collect(toImmutableMap(identity(), inputs -> withValue(result)))));

    assertThat(nodeExecutionReport.getMainLogicExecInfos().size()).isEqualTo(1);
    LogicExecInfo logicExecInfo =
        nodeExecutionReport.getMainLogicExecInfos().values().stream().findFirst().orElseThrow();

    assertThat(logicExecInfo.getNodeId()).isEqualTo(nodeId.value());
    assertThat(logicExecInfo.getStartTimeMs()).isEqualTo(START_TIME.toEpochMilli());
    assertThat(logicExecInfo.getEndTimeMs()).isEqualTo(END_TIME.toEpochMilli());
    assertThat(logicExecInfo.getResult())
        .isEqualTo(
            ImmutableMap.of(ImmutableMap.of("i1", "v1"), "r1", ImmutableMap.of("i1", "v2"), "r1"));
    assertThat(logicExecInfo.getInputsList())
        .isEqualTo(ImmutableList.of(ImmutableMap.of("i1", "v1"), ImmutableMap.of("i1", "v2")));
  }

  @Test
  void reportMainLogicStart_calledTwice_hasNoEffect() {
    NodeId nodeId = new NodeId("node_1");
    NodeLogicId nodeLogicId = new NodeLogicId(nodeId, "node_1__logic_1");
    ImmutableList<Inputs> inputsList =
        ImmutableList.of(
            new Inputs(ImmutableMap.of("i1", withValue("v1"))),
            new Inputs(ImmutableMap.of("i1", withValue("v2"))));

    clock.setInstant(START_TIME);
    nodeExecutionReport.reportMainLogicStart(nodeId, nodeLogicId, inputsList);

    clock.setInstant(END_TIME);
    nodeExecutionReport.reportMainLogicStart(nodeId, nodeLogicId, inputsList);

    assertThat(nodeExecutionReport.getMainLogicExecInfos().size()).isEqualTo(1);
    LogicExecInfo logicExecInfo =
        nodeExecutionReport.getMainLogicExecInfos().values().stream().findFirst().orElseThrow();

    assertThat(logicExecInfo.getNodeId()).isEqualTo(nodeId.value());
    assertThat(logicExecInfo.getStartTimeMs()).isEqualTo(START_TIME.toEpochMilli());
  }

  @Test
  void reportMainLogicEnd_calledTwice_hasNoEffect() {
    NodeId nodeId = new NodeId("node_1");
    NodeLogicId nodeLogicId = new NodeLogicId(nodeId, "node_1__logic_1");
    ImmutableList<Inputs> inputsList =
        ImmutableList.of(
            new Inputs(ImmutableMap.of("i1", withValue("v1"))),
            new Inputs(ImmutableMap.of("i1", withValue("v2"))));

    clock.setInstant(START_TIME);
    nodeExecutionReport.reportMainLogicStart(nodeId, nodeLogicId, inputsList);

    String result = "r1";
    clock.setInstant(END_TIME);
    nodeExecutionReport.reportMainLogicEnd(
        nodeId,
        nodeLogicId,
        new Results<>(
            inputsList.stream().collect(toImmutableMap(identity(), inputs -> withValue(result)))));

    clock.setInstant(END_TIME.plusMillis(100));
    nodeExecutionReport.reportMainLogicEnd(
        nodeId,
        nodeLogicId,
        new Results<>(
            inputsList.stream().collect(toImmutableMap(identity(), inputs -> withValue(result)))));

    LogicExecInfo logicExecInfo =
        nodeExecutionReport.getMainLogicExecInfos().values().stream().findFirst().orElseThrow();

    assertThat(logicExecInfo.getNodeId()).isEqualTo(nodeId.value());
    assertThat(logicExecInfo.getEndTimeMs()).isEqualTo(END_TIME.toEpochMilli());
  }

  @Test
  void reportMainLogicEnd_calledWithoutCallingStart_hasNoEffect() {
    NodeId nodeId = new NodeId("node_1");
    NodeLogicId nodeLogicId = new NodeLogicId(nodeId, "node_1__logic_1");
    ImmutableList<Inputs> inputsList =
        ImmutableList.of(
            new Inputs(ImmutableMap.of("i1", withValue("v1"))),
            new Inputs(ImmutableMap.of("i1", withValue("v2"))));

    clock.setInstant(END_TIME);
    String result = "r1";
    nodeExecutionReport.reportMainLogicEnd(
        nodeId,
        nodeLogicId,
        new Results<>(
            inputsList.stream().collect(toImmutableMap(identity(), inputs -> withValue(result)))));
    assertThat(nodeExecutionReport.getMainLogicExecInfos().isEmpty()).isTrue();
  }
}
