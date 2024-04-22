package com.flipkart.krystal.krystex.logicdecorators.observability;

import static com.flipkart.krystal.data.Errable.withValue;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.data.FacetMap;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.FacetsMap;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.data.SimpleRequestBuilder;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.logicdecorators.observability.DefaultKryonExecutionReport.LogicExecInfo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
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
            new FacetMap(ImmutableMap.of(1, withValue("v1"))),
            new FacetMap(ImmutableMap.of(2, withValue("v2"))));

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

    assertThat(kryonExecutionReport.getMainLogicExecInfos().size()).isEqualTo(1);
    LogicExecInfo logicExecInfo =
        kryonExecutionReport.getMainLogicExecInfos().values().stream().findFirst().orElseThrow();

    assertThat(logicExecInfo.getKryonId()).isEqualTo(kryonId.value());
    assertThat(logicExecInfo.getStartTimeMs()).isEqualTo(START_TIME.toEpochMilli());
    assertThat(logicExecInfo.getEndTimeMs()).isEqualTo(END_TIME.toEpochMilli());
    assertThat(logicExecInfo.getResult())
        .isEqualTo(ImmutableMap.of(ImmutableMap.of(1, "v1"), "r1", ImmutableMap.of(1, "v2"), "r1"));
    assertThat(logicExecInfo.getInputsList())
        .isEqualTo(ImmutableList.of(ImmutableMap.of(1, "v1"), ImmutableMap.of(2, "v2")));
  }

  @Test
  void reportMainLogicStart_calledTwice_hasNoEffect() {
    KryonId kryonId = new KryonId("kryon_1");
    KryonLogicId kryonLogicId = new KryonLogicId(kryonId, "kryon_1__logic_1");
    ImmutableList<Facets> facetsList =
        ImmutableList.of(
            new FacetsMap(
                new SimpleRequestBuilder<>(ImmutableMap.of(1, withValue("v1"))), ImmutableMap.of()),
            new FacetsMap(
                new SimpleRequestBuilder<>(ImmutableMap.of(2, withValue("v2"))),
                ImmutableMap.of()));

    clock.setInstant(START_TIME);
    kryonExecutionReport.reportMainLogicStart(kryonId, kryonLogicId, facetsList);

    clock.setInstant(END_TIME);
    kryonExecutionReport.reportMainLogicStart(kryonId, kryonLogicId, facetsList);

    assertThat(kryonExecutionReport.getMainLogicExecInfos().size()).isEqualTo(1);
    LogicExecInfo logicExecInfo =
        kryonExecutionReport.getMainLogicExecInfos().values().stream().findFirst().orElseThrow();

    assertThat(logicExecInfo.getKryonId()).isEqualTo(kryonId.value());
    assertThat(logicExecInfo.getStartTimeMs()).isEqualTo(START_TIME.toEpochMilli());
  }

  @Test
  void reportMainLogicEnd_calledTwice_hasNoEffect() {
    KryonId kryonId = new KryonId("kryon_1");
    KryonLogicId kryonLogicId = new KryonLogicId(kryonId, "kryon_1__logic_1");
    ImmutableList<Facets> facetsList =
        ImmutableList.of(
            new FacetMap(ImmutableMap.of(1, withValue("v1"))),
            new FacetMap(ImmutableMap.of(2, withValue("v2"))));

    clock.setInstant(START_TIME);
    kryonExecutionReport.reportMainLogicStart(kryonId, kryonLogicId, facetsList);

    String result = "r1";
    clock.setInstant(END_TIME);
    kryonExecutionReport.reportMainLogicEnd(
        kryonId,
        kryonLogicId,
        new Results<>(
            facetsList.stream().collect(toImmutableMap(identity(), inputs -> withValue(result)))));

    clock.setInstant(END_TIME.plusMillis(100));
    kryonExecutionReport.reportMainLogicEnd(
        kryonId,
        kryonLogicId,
        new Results<>(
            facetsList.stream().collect(toImmutableMap(identity(), inputs -> withValue(result)))));

    LogicExecInfo logicExecInfo =
        kryonExecutionReport.getMainLogicExecInfos().values().stream().findFirst().orElseThrow();

    assertThat(logicExecInfo.getKryonId()).isEqualTo(kryonId.value());
    assertThat(logicExecInfo.getEndTimeMs()).isEqualTo(END_TIME.toEpochMilli());
  }

  @Test
  void reportMainLogicEnd_calledWithoutCallingStart_hasNoEffect() {
    KryonId kryonId = new KryonId("kryon_1");
    KryonLogicId kryonLogicId = new KryonLogicId(kryonId, "kryon_1__logic_1");
    ImmutableList<Facets> facetsList =
        ImmutableList.of(
            new FacetMap(ImmutableMap.of(1, withValue("v1"))),
            new FacetMap(ImmutableMap.of(2, withValue("v2"))));

    clock.setInstant(END_TIME);
    String result = "r1";
    kryonExecutionReport.reportMainLogicEnd(
        kryonId,
        kryonLogicId,
        new Results<>(
            facetsList.stream().collect(toImmutableMap(identity(), inputs -> withValue(result)))));
    assertThat(kryonExecutionReport.getMainLogicExecInfos().isEmpty()).isTrue();
  }
}
