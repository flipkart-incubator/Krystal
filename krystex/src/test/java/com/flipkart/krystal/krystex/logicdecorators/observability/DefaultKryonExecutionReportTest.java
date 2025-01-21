package com.flipkart.krystal.krystex.logicdecorators.observability;

import static com.flipkart.krystal.data.Errable.withValue;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ImmutableFacetsMap;
import com.flipkart.krystal.data.SimpleRequestBuilder;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.logicdecorators.observability.DefaultKryonExecutionReport.LogicExecInfo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultKryonExecutionReportTest {
  public static final Instant CREATE_TIME = Instant.ofEpochMilli(0);
  private static final Instant START_TIME = Instant.ofEpochMilli(1);
  private static final Instant END_TIME = Instant.ofEpochMilli(100);
  private FakeClock clock;
  private DefaultKryonExecutionReport kryonExecutionReport;

  @BeforeEach
  void setUp() {
    clock = new FakeClock(CREATE_TIME);
    kryonExecutionReport = new DefaultKryonExecutionReport(clock);
  }

  @Test
  void reportStartAndEnd_success() {
    KryonId kryonId = new KryonId("kryon_1");
    KryonLogicId kryonLogicId = new KryonLogicId(kryonId, "kryon_1__logic_1");
    ImmutableList<Facets> facetsList =
        ImmutableList.of(
            new ImmutableFacetsMap(new SimpleRequestBuilder<>(ImmutableMap.of(1, withValue("v1")))),
            new ImmutableFacetsMap(
                new SimpleRequestBuilder<>(ImmutableMap.of(1, withValue("v2")))));

    clock.setInstant(START_TIME);
    kryonExecutionReport.reportMainLogicStart(kryonId, kryonLogicId, facetsList);

    String result = "r1";
    clock.setInstant(END_TIME);
    kryonExecutionReport.reportMainLogicEnd(
        kryonId,
        kryonLogicId,
        new LogicExecResults(
            facetsList.stream()
                .map(facets -> new LogicExecResponse(facets, withValue(result)))
                .collect(toImmutableList())));

    assertThat(kryonExecutionReport.mainLogicExecInfos().size()).isEqualTo(1);
    LogicExecInfo logicExecInfo =
        kryonExecutionReport.mainLogicExecInfos().values().stream().findFirst().orElseThrow();

    assertThat(logicExecInfo.kryonId()).isEqualTo(kryonId.value());
    assertThat(logicExecInfo.startTimeMs()).isEqualTo(START_TIME.toEpochMilli());
    assertThat(logicExecInfo.endTimeMs()).isEqualTo(END_TIME.toEpochMilli());
    assertThat(logicExecInfo.result()).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<ImmutableMap<Integer, String>, String> mapResult =
        (Map<ImmutableMap<Integer, String>, String>) logicExecInfo.result();
    Map<ImmutableMap<Integer, String>, String> derefedMap = new LinkedHashMap<>();
    mapResult.forEach(
        (key, resultRef) -> {
          Map<Integer, String> map = new LinkedHashMap<>();
          key.forEach(
              (inputName, valueRef) -> {
                map.put(inputName, (String) kryonExecutionReport.dataMap().get(valueRef));
              });
          derefedMap.put(
              ImmutableMap.copyOf(map), (String) kryonExecutionReport.dataMap().get(resultRef));
        });
    assertThat(derefedMap)
        .isEqualTo(ImmutableMap.of(ImmutableMap.of(1, "v1"), "r1", ImmutableMap.of(1, "v2"), "r1"));

    ImmutableList<ImmutableMap<Integer, String>> inputsList = logicExecInfo.inputsList();

    List<Map<Integer, String>> dereffedInputList = new ArrayList<>();
    for (ImmutableMap<Integer, String> inputs : inputsList) {
      Map<Integer, String> map = new LinkedHashMap<>();
      inputs.forEach(
          (inputName, valueRef) -> {
            map.put(inputName, (String) kryonExecutionReport.dataMap().get(valueRef));
          });
      dereffedInputList.add(map);
    }
    assertThat(dereffedInputList)
        .isEqualTo(ImmutableList.of(ImmutableMap.of(1, "v1"), ImmutableMap.of(1, "v2")));
  }

  @Test
  void reportMainLogicStart_calledTwice_hasNoEffect() {
    KryonId kryonId = new KryonId("kryon_1");
    KryonLogicId kryonLogicId = new KryonLogicId(kryonId, "kryon_1__logic_1");
    ImmutableList<Facets> facetsList =
        ImmutableList.of(
            new ImmutableFacetsMap(new SimpleRequestBuilder<>(ImmutableMap.of(1, withValue("v1")))),
            new ImmutableFacetsMap(
                new SimpleRequestBuilder<>(ImmutableMap.of(2, withValue("v2")))));

    clock.setInstant(START_TIME);
    kryonExecutionReport.reportMainLogicStart(kryonId, kryonLogicId, facetsList);

    clock.setInstant(END_TIME);
    kryonExecutionReport.reportMainLogicStart(kryonId, kryonLogicId, facetsList);

    assertThat(kryonExecutionReport.mainLogicExecInfos().size()).isEqualTo(1);
    LogicExecInfo logicExecInfo =
        kryonExecutionReport.mainLogicExecInfos().values().stream().findFirst().orElseThrow();

    assertThat(logicExecInfo.kryonId()).isEqualTo(kryonId.value());
    assertThat(logicExecInfo.startTimeMs()).isEqualTo(START_TIME.toEpochMilli());
  }

  @Test
  void reportMainLogicEnd_calledTwice_hasNoEffect() {
    KryonId kryonId = new KryonId("kryon_1");
    KryonLogicId kryonLogicId = new KryonLogicId(kryonId, "kryon_1__logic_1");
    ImmutableList<Facets> facetsList =
        ImmutableList.of(
            new ImmutableFacetsMap(new SimpleRequestBuilder<>(ImmutableMap.of(1, withValue("v1")))),
            new ImmutableFacetsMap(
                new SimpleRequestBuilder<>(ImmutableMap.of(2, withValue("v2")))));

    clock.setInstant(START_TIME);
    kryonExecutionReport.reportMainLogicStart(kryonId, kryonLogicId, facetsList);

    String result = "r1";
    clock.setInstant(END_TIME);
    kryonExecutionReport.reportMainLogicEnd(
        kryonId,
        kryonLogicId,
        new LogicExecResults(
            facetsList.stream()
                .map(x -> new LogicExecResponse(x, withValue(result)))
                .collect(toImmutableList())));

    clock.setInstant(END_TIME.plusMillis(100));
    kryonExecutionReport.reportMainLogicEnd(
        kryonId,
        kryonLogicId,
        new LogicExecResults(
            facetsList.stream()
                .map(x -> new LogicExecResponse(x, withValue(result)))
                .collect(toImmutableList())));

    LogicExecInfo logicExecInfo =
        kryonExecutionReport.mainLogicExecInfos().values().stream().findFirst().orElseThrow();

    assertThat(logicExecInfo.kryonId()).isEqualTo(kryonId.value());
    assertThat(logicExecInfo.endTimeMs()).isEqualTo(END_TIME.toEpochMilli());
  }

  @Test
  void reportMainLogicEnd_calledWithoutCallingStart_hasNoEffect() {
    KryonId kryonId = new KryonId("kryon_1");
    KryonLogicId kryonLogicId = new KryonLogicId(kryonId, "kryon_1__logic_1");
    ImmutableList<Facets> facetsList =
        ImmutableList.of(
            new ImmutableFacetsMap(new SimpleRequestBuilder<>(ImmutableMap.of(1, withValue("v1")))),
            new ImmutableFacetsMap(
                new SimpleRequestBuilder<>(ImmutableMap.of(2, withValue("v2")))));

    clock.setInstant(END_TIME);
    String result = "r1";
    kryonExecutionReport.reportMainLogicEnd(
        kryonId,
        kryonLogicId,
        new LogicExecResults(
            facetsList.stream()
                .map(x -> new LogicExecResponse(x, withValue(result)))
                .collect(toImmutableList())));
    assertThat(kryonExecutionReport.mainLogicExecInfos().isEmpty()).isTrue();
  }
}
