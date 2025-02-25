package com.flipkart.krystal.krystex.logicdecorators.observability;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.Failure;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.InputMirror;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@ToString
@Slf4j
public final class DefaultKryonExecutionReport implements KryonExecutionReport {
  @JsonProperty @Getter private final Instant startTime;
  private final boolean verbose;
  private final Clock clock;
  private static final String SHA_256 = "SHA-256";
  private static final @NonNull MessageDigest digest = initMessageDigest();

  static MessageDigest initMessageDigest() {
    try {
      return MessageDigest.getInstance(SHA_256);
    } catch (NoSuchAlgorithmException e) {
      log.error("Error could not hash inputs because of exception ", e);
      throw new IllegalStateException(e);
    }
  }

  @JsonProperty @Getter
  private final Map<KryonExecution, LogicExecInfo> mainLogicExecInfos = new LinkedHashMap<>();

  @JsonProperty @Getter private final Map<String, Object> dataMap = new HashMap<>();

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
      VajramID vajramID, KryonLogicId kryonLogicId, ImmutableList<? extends FacetValues> inputs) {

    KryonExecution kryonExecution =
        new KryonExecution(
            vajramID,
            inputs.stream()
                .map(facets -> extractAndConvertFacets(facets))
                .collect(toImmutableList()));
    if (mainLogicExecInfos.containsKey(kryonExecution)) {
      log.error("Cannot start the same kryon execution multiple times: {}", kryonExecution);
      return;
    }
    mainLogicExecInfos.put(
        kryonExecution,
        new LogicExecInfo(
            this, vajramID, inputs, startTime.until(clock.instant(), ChronoUnit.MILLIS)));
  }

  @Override
  public void reportMainLogicEnd(
      VajramID vajramID, KryonLogicId kryonLogicId, LogicExecResults logicExecResults) {
    KryonExecution kryonExecution =
        new KryonExecution(
            vajramID,
            logicExecResults.responses().stream()
                .map(LogicExecResponse::facetValues)
                .map(facets -> extractAndConvertFacets(facets))
                .collect(toImmutableList()));
    LogicExecInfo logicExecInfo = mainLogicExecInfos.get(kryonExecution);
    if (logicExecInfo == null) {
      log.error(
          "'reportMainLogicEnd' called without calling 'reportMainLogicStart' first for: {}",
          kryonExecution);
      return;
    }
    if (logicExecInfo.result() != null) {
      log.error("Cannot end the same kryon execution multiple times: {}", kryonExecution);
      return;
    }
    logicExecInfo.endTimeMs = startTime.until(clock.instant(), ChronoUnit.MILLIS);
    logicExecInfo.setResult(convertResult(logicExecResults));
  }

  private record KryonExecution(
      VajramID vajramID, ImmutableList<ImmutableMap<Facet, String>> inputs) {
    @Override
    public String toString() {
      return "%s(%s)".formatted(vajramID.value(), inputs);
    }
  }

  private ImmutableMap<InputMirror, String> extractAndConvertFacets(Request<?> request) {
    Map<InputMirror, String> inputMap = new LinkedHashMap<>();
    request._facets().stream()
        .forEach(
            (inputDef) -> {
              inputMap.put(inputDef, convertValue(inputDef.getFromRequest(request)));
            });
    return ImmutableMap.copyOf(inputMap);
  }

  private ImmutableMap<Facet, String> extractAndConvertFacets(FacetValues facetValues) {
    Map<Facet, String> inputMap = new LinkedHashMap<>();
    facetValues._facets().stream()
        .forEach(
            facetDef -> {
              FacetValue value = facetDef.getFacetValue(facetValues);
              if (!(value instanceof Errable<?>)) {
                return;
              }
              String collect = convertErrable((Errable<?>) value);
              if (collect != null) {
                inputMap.put(facetDef, collect);
              }
            });
    return ImmutableMap.copyOf(inputMap);
  }

  private ImmutableMap<Facet, Object> extractAndConvertDependencyResults(
      FacetValues facetValues, ImmutableSet<? extends Facet> facetDefs) {
    {
      Map<Facet, Object> inputMap = new LinkedHashMap<>();
      facetDefs.stream()
          .forEach(
              facetDef -> {
                FacetValue value = facetDef.getFacetValue(facetValues);
                if (!(value instanceof FanoutDepResponses depResponses)) {
                  return;
                }
                inputMap.put(facetDef, convertResult(depResponses));
              });
      return ImmutableMap.copyOf(inputMap);
    }
  }

  private String convertErrable(Errable<?> voe) {
    String sha256;
    if (voe instanceof Failure<?> f) {
      Throwable throwable = f.error();
      String stackTraceAsString = getStackTraceAsString(throwable);
      sha256 = verbose ? hashValues(stackTraceAsString) : hashValues(throwable.toString());
      dataMap.put(sha256, verbose ? stackTraceAsString : throwable.toString());
    } else {
      sha256 = convertValue(voe.valueOpt().orElse(null));
    }
    return sha256;
  }

  private String convertValue(@Nullable Object value) {
    if (value == null) {
      value = "null";
    }
    String sha256 = hashValues(value);
    dataMap.put(sha256, value);
    return sha256;
  }

  private Map<ImmutableMap<InputMirror, String>, Object> convertResult(
      FanoutDepResponses<?, ?> depResponses) {
    return depResponses.requestResponsePairs().stream()
        .collect(
            toMap(
                e -> extractAndConvertFacets(e.request()), //
                e -> convertErrable(e.response())));
  }

  private Map<ImmutableMap<Facet, String>, String> convertResult(
      LogicExecResults logicExecResults) {
    return logicExecResults.responses().stream()
        .collect(
            toMap(
                e -> extractAndConvertFacets(e.facetValues()),
                e -> convertErrable(e.response()),
                (o1, o2) -> o1));
  }

  public static <T> String hashValues(@Nullable T input) {
    return hashString(String.valueOf(input));
  }

  private static String hashString(String appendedInput) {
    byte[] encodedHash = digest.digest(appendedInput.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(encodedHash);
  }

  @ToString
  @Getter
  static final class LogicExecInfo {

    @JsonProperty private final String kryonId;
    @JsonProperty private final ImmutableList<ImmutableMap<Facet, String>> inputsList;

    @JsonProperty
    private final @Nullable ImmutableList<ImmutableMap<Facet, Object>> dependencyResults;

    @JsonProperty private @Nullable Object result;
    @JsonProperty private final long startTimeMs;
    @JsonProperty private long endTimeMs;

    LogicExecInfo(
        DefaultKryonExecutionReport kryonExecutionReport,
        VajramID vajramID,
        ImmutableCollection<? extends FacetValues> inputList,
        long startTimeMs) {
      this.startTimeMs = startTimeMs;
      ImmutableList<ImmutableMap<Facet, Object>> dependencyResults;
      this.kryonId = vajramID.value();
      this.inputsList =
          inputList.stream()
              .map(facets -> kryonExecutionReport.extractAndConvertFacets(facets))
              .collect(toImmutableList());
      dependencyResults =
          inputList.stream()
              .map(
                  facets ->
                      kryonExecutionReport.extractAndConvertDependencyResults(
                          facets, facets._facets()))
              .filter(map -> !map.isEmpty())
              .collect(toImmutableList());
      this.dependencyResults = dependencyResults.isEmpty() ? null : dependencyResults;
    }

    public void setResult(Map<ImmutableMap<Facet, String>, String> result) {
      if (inputsList.size() <= 1 && result.size() == 1) {
        this.result = result.values().iterator().next();
      } else {
        this.result = result;
      }
    }
  }
}
