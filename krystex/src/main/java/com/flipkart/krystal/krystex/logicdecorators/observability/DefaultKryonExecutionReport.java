package com.flipkart.krystal.krystex.logicdecorators.observability;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
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
  private static final String SHA_256 = "SHA-256";

  @Getter
  private final Map<KryonExecution, LogicExecInfo> mainLogicExecInfos = new LinkedHashMap<>();

  @Getter private final Map<String, Object> dataMap = new HashMap<>();

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
      KryonId kryonId, KryonLogicId kryonLogicId, ImmutableList<Facets> inputs) {
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
      KryonId kryonId, KryonLogicId kryonLogicId, Results<Object> result) {
    KryonExecution kryonExecution =
        new KryonExecution(
            kryonId,
            result.values().keySet().stream()
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
    logicExecInfo.setResult(convertResult(result));
  }

  private record KryonExecution(
      KryonId kryonId, ImmutableList<ImmutableMap<String, Object>> inputs) {
    @Override
    public String toString() {
      return "%s(%s)".formatted(kryonId.value(), inputs);
    }
  }

  private ImmutableMap<String, Object> extractAndConvertInputs(Facets facets) {
    Map<String, Object> inputMap = new LinkedHashMap<>();
    for (Entry<String, FacetValue<Object>> e : facets.values().entrySet()) {
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

  private ImmutableMap<String, Object> extractAndConvertDependencyResults(Facets facets) {
    Map<String, Object> inputMap = new LinkedHashMap<>();
    for (Entry<String, FacetValue<Object>> e : facets.values().entrySet()) {
      FacetValue<Object> value = e.getValue();
      if (!(value instanceof Results<Object>)) {
        continue;
      }
      Map<ImmutableMap<String, Object>, Object> collect = convertResult((Results<Object>) value);
      inputMap.put(e.getKey(), collect);
    }
    return ImmutableMap.copyOf(inputMap);
  }

  private Object convertErrable(Errable<Object> voe) {
    String sha256;
    if (voe.error().isPresent()) {
      Throwable throwable = voe.error().get();
      String stackTraceAsString = getStackTraceAsString(throwable);
      sha256 = verbose ? hashValues(stackTraceAsString) : hashValues(throwable.toString());
      dataMap.put(sha256, verbose ? stackTraceAsString : throwable.toString());
      return sha256;
    } else {
      Object value = voe.value().orElse("null");
      sha256 = hashValues(value);
      dataMap.put(sha256, value);
      return sha256;
    }
  }

  private Map<ImmutableMap<String, Object>, Object> convertResult(Results<Object> results) {
    return results.values().entrySet().stream()
        .collect(
            Collectors.toMap(
                e -> extractAndConvertInputs(e.getKey()), e -> convertErrable(e.getValue())));
  }

  public static <T> String hashValues(T input) {
    StringBuilder concatenatedValues = new StringBuilder();
    if (input != null) {
      concatenatedValues.append(input.toString());
    }
    return hashString(concatenatedValues.toString());
  }

  private static String hashString(String appendedInput) {
    try {
      MessageDigest digest = MessageDigest.getInstance(SHA_256);
      byte[] encodedHash = digest.digest(appendedInput.getBytes(StandardCharsets.UTF_8));
      String encodedString = Base64.getEncoder().encodeToString(encodedHash);

      return encodedString;
    } catch (NoSuchAlgorithmException e) {
      log.error("Error: could not hash inputs because of exception");
      return "";
    }
  }

  @ToString
  @Getter
  static final class LogicExecInfo {

    private final String kryonId;
    private final ImmutableList<ImmutableMap<String, Object>> inputsList;
    private final @Nullable ImmutableList<ImmutableMap<String, Object>> dependencyResults;
    private @Nullable Object result;
    @Getter private final long startTimeMs;
    @Getter private long endTimeMs;

    LogicExecInfo(
        DefaultKryonExecutionReport kryonExecutionReport,
        KryonId kryonId,
        ImmutableCollection<Facets> inputList,
        long startTimeMs) {
      this.startTimeMs = startTimeMs;
      ImmutableList<ImmutableMap<String, Object>> dependencyResults;
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

    public void setResult(Map<ImmutableMap<String, Object>, Object> result) {
      if (inputsList.size() <= 1 && result.size() == 1) {
        this.result = result.values().iterator().next();
      } else {
        this.result = result;
      }
    }
  }
}
