package com.flipkart.krystal.krystex.logicdecorators.observability;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetContainer;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Failure;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@ToString
@Slf4j
public final class DefaultKryonExecutionReport implements KryonExecutionReport {
  @Getter private final Instant startTime;
  private final boolean verbose;
  private final Clock clock;

  @Getter
  private final Map<KryonExecution, LogicExecInfo> mainLogicExecInfos = new LinkedHashMap<>();

  public DefaultKryonExecutionReport(Clock clock) {
    this(clock, false);
  }

  public DefaultKryonExecutionReport(Clock clock, boolean verbose) {
    this.clock = clock;
    this.startTime = clock.instant();
    this.verbose = verbose;
  }

  @Override
  public void reportMainLogicStart(
      KryonId kryonId, KryonLogicId kryonLogicId, ImmutableList<? extends FacetContainer> inputs) {
    KryonExecution kryonExecution =
        new KryonExecution(
            kryonId, inputs.stream().map(this::extractAndConvertInputs).collect(toImmutableList()));
    if (mainLogicExecInfos.containsKey(kryonExecution)) {
      log.error("Cannot start the same kryon execution multiple times: {}", kryonExecution);
      return;
    }
    mainLogicExecInfos.put(
        kryonExecution,
        new LogicExecInfo(
            this, kryonId, inputs, startTime.until(clock.instant(), ChronoUnit.MILLIS)));
  }

  @Override
  public void reportMainLogicEnd(
      KryonId kryonId, KryonLogicId kryonLogicId, LogicExecResults logicExecResults) {
    KryonExecution kryonExecution =
        new KryonExecution(
            kryonId,
            logicExecResults.responses().stream()
                .map(LogicExecResponse::request)
                .map(this::extractAndConvertInputs)
                .collect(toImmutableList()));
    LogicExecInfo logicExecInfo = mainLogicExecInfos.get(kryonExecution);
    if (logicExecInfo == null) {
      log.error(
          "'reportMainLogicEnd' called without calling 'reportMainLogicStart' first for: {}",
          kryonExecution);
      return;
    }
    if (logicExecInfo.getResult() != null) {
      log.error("Cannot end the same kryon execution multiple times: {}", kryonExecution);
      return;
    }
    logicExecInfo.endTimeMs = startTime.until(clock.instant(), ChronoUnit.MILLIS);
    logicExecInfo.setResult(convertResult(logicExecResults));
  }

  private record KryonExecution(
      KryonId kryonId, ImmutableList<ImmutableMap<Integer, Object>> inputs) {
    @Override
    public String toString() {
      return "%s(%s)".formatted(kryonId.value(), inputs);
    }
  }

  private ImmutableMap<Integer, Object> extractAndConvertInputs(FacetContainer facets) {
    Map<Integer, Object> inputMap = new LinkedHashMap<>();
    for (Entry<Integer, ? extends FacetValue<Object>> e : facets._asMap().entrySet()) {
      FacetValue<Object> value = e.getValue();
      if (!(value instanceof Errable<Object>)) {
        continue;
      }
      Object collect = convertErrable((Errable<Object>) value);
      if (collect != null) {
        inputMap.put(e.getKey(), collect);
      }
    }
    return ImmutableMap.copyOf(inputMap);
  }

  private ImmutableMap<Integer, Object> extractAndConvertDependencyResults(FacetContainer facets) {
    Map<Integer, Object> inputMap = new LinkedHashMap<>();
    for (Entry<Integer, ? extends FacetValue<Object>> e : facets._asMap().entrySet()) {
      FacetValue<Object> value = e.getValue();
      if (!(value instanceof Results<?, ?> results)) {
        continue;
      }
      Map<ImmutableMap<Integer, Object>, Object> collect = convertResult(results);
      inputMap.put(e.getKey(), collect);
    }
    return ImmutableMap.copyOf(inputMap);
  }

  private Object convertErrable(Errable<?> voe) {
    if (voe instanceof Failure<?> f) {
      return verbose ? getStackTraceAsString(f.error()) : f.error().toString();
    } else {
      //noinspection unchecked
      return ((Errable<Object>) voe).valueOpt().orElse("null");
    }
  }

  private Map<ImmutableMap<Integer, Object>, Object> convertResult(Results<?, ?> results) {
    return results.responses().stream()
        .collect(
            Collectors.toMap(
                e -> extractAndConvertInputs(e.request()), e -> convertErrable(e.response())));
  }

  private Map<ImmutableMap<Integer, Object>, Object> convertResult(
      LogicExecResults logicExecResults) {
    return logicExecResults.responses().stream()
        .collect(
            Collectors.toMap(
                e -> extractAndConvertInputs(e.request()), e -> convertErrable(e.response())));
  }

  @ToString
  @Getter
  static final class LogicExecInfo {

    private final String kryonId;
    private final ImmutableList<ImmutableMap<Integer, Object>> inputsList;
    private final @Nullable ImmutableList<ImmutableMap<Integer, Object>> dependencyResults;
    private @Nullable Object result;
    @Getter private final long startTimeMs;
    @Getter private long endTimeMs;

    LogicExecInfo(
        DefaultKryonExecutionReport kryonExecutionReport,
        KryonId kryonId,
        ImmutableCollection<? extends FacetContainer> inputList,
        long startTimeMs) {
      this.startTimeMs = startTimeMs;
      ImmutableList<ImmutableMap<Integer, Object>> dependencyResults;
      this.kryonId = kryonId.value();
      this.inputsList =
          inputList.stream()
              .map(kryonExecutionReport::extractAndConvertInputs)
              .collect(toImmutableList());
      dependencyResults =
          inputList.stream()
              .map(kryonExecutionReport::extractAndConvertDependencyResults)
              .filter(map -> !map.isEmpty())
              .collect(toImmutableList());
      this.dependencyResults = dependencyResults.isEmpty() ? null : dependencyResults;
    }

    public void setResult(Map<ImmutableMap<Integer, Object>, Object> result) {
      if (inputsList.size() <= 1 && result.size() == 1) {
        this.result = result.values().iterator().next();
      } else {
        this.result = result;
      }
    }
  }
}
