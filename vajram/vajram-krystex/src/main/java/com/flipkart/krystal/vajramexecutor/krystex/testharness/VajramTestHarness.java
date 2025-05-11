package com.flipkart.krystal.vajramexecutor.krystex.testharness;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.flipkart.krystal.krystex.caching.RequestLevelCache;
import com.flipkart.krystal.krystex.caching.TestRequestLevelCache;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test harness is a collection of software/test data used by developers for unit testing. It is
 * responsible for test drivers and stubs. In the context of Vajrams, VajramTestHarness is
 * responsible for preparing the krystex executor for test by providing the necessary stubbing
 * capability for all the dependency Vajrams of the given dependant Vajram in test.
 */
public class VajramTestHarness {

  private final Map<VajramID, Map<ImmutableFacetValues, Errable<Object>>> vajramIdMockData;
  private final KrystexVajramExecutorConfig kryonExecutorConfigBuilder;
  private final TestRequestLevelCache requestLevelCache;

  @Inject
  public VajramTestHarness(
      KrystexVajramExecutorConfig krystexVajramExecutorConfig,
      TestRequestLevelCache requestLevelCache) {
    this.kryonExecutorConfigBuilder = krystexVajramExecutorConfig;
    this.requestLevelCache = requestLevelCache;
    this.vajramIdMockData = new HashMap<>();
  }

  public static VajramTestHarness prepareForTest(
      KrystexVajramExecutorConfig executorConfig, TestRequestLevelCache requestLevelCache) {
    return new VajramTestHarness(executorConfig, requestLevelCache);
  }

  @SuppressWarnings("unchecked")
  public <T> VajramTestHarness withMock(ImmutableFacetValues facetValues, Errable<T> response) {
    this.vajramIdMockData
        .computeIfAbsent(facetValues._vajramID(), _k -> new LinkedHashMap<>())
        .computeIfAbsent(facetValues, _k -> (Errable<Object>) response);
    return this;
  }

  public KrystexVajramExecutorConfig buildConfig() {
    vajramIdMockData.forEach(
        (vajramID, vajramRequestErrableMap) ->
            vajramRequestErrableMap.forEach(
                (objectVajramRequest, objectErrable) ->
                    requestLevelCache.primeCache(objectVajramRequest, objectErrable.toFuture())));
    KryonExecutorConfigBuilder configBuilder =
        kryonExecutorConfigBuilder.kryonExecutorConfigBuilder();
    KryonDecoratorConfig kryonDecoratorConfig =
        configBuilder.build().kryonDecoratorConfigs().get(RequestLevelCache.DECORATOR_TYPE);
    if (kryonDecoratorConfig == null) {
      kryonExecutorConfigBuilder
          .kryonExecutorConfigBuilder()
          .kryonDecoratorConfig(
              RequestLevelCache.DECORATOR_TYPE,
              new KryonDecoratorConfig(
                  RequestLevelCache.DECORATOR_TYPE,
                  executionContext -> vajramIdMockData.containsKey(executionContext.vajramID()),
                  executionContext -> RequestLevelCache.DECORATOR_TYPE,
                  kryonExecutionContext -> requestLevelCache));
    }
    return kryonExecutorConfigBuilder;
  }
}
