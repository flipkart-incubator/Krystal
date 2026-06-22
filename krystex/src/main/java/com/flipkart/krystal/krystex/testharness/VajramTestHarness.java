package com.flipkart.krystal.krystex.testharness;

import static com.flipkart.krystal.krystex.caching.RequestLevelCache.KRYON_DECORATOR_TYPE;
import static com.flipkart.krystal.krystex.caching.RequestLevelCache.OUTPUT_LOGIC_DECORATOR_TYPE;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.flipkart.krystal.krystex.KrystalExecutorConfig.KrystalExecutorConfigBuilder;
import com.flipkart.krystal.krystex.caching.TestRequestLevelCache;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
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
  private final KrystalExecutorConfigBuilder krystalExecutorConfigBuilder;
  private final TestRequestLevelCache requestLevelCache;

  @Inject
  public VajramTestHarness(
      KrystalExecutorConfigBuilder krystexVajramExecutorConfig,
      TestRequestLevelCache requestLevelCache) {
    this.krystalExecutorConfigBuilder = krystexVajramExecutorConfig;
    this.requestLevelCache = requestLevelCache;
    this.vajramIdMockData = new HashMap<>();
  }

  public static VajramTestHarness prepareForTest(
      KrystalExecutorConfigBuilder executorConfig, TestRequestLevelCache requestLevelCache) {
    return new VajramTestHarness(executorConfig, requestLevelCache);
  }

  @SuppressWarnings("unchecked")
  public <T> VajramTestHarness withMock(ImmutableFacetValues facetValues, Errable<T> response) {
    this.vajramIdMockData
        .computeIfAbsent(facetValues._vajramID(), _k -> new LinkedHashMap<>())
        .computeIfAbsent(facetValues, _k -> (Errable<Object>) response);
    return this;
  }

  public KrystalExecutorConfigBuilder buildConfig() {
    if (krystalExecutorConfigBuilder.hasKryonDecorator(KRYON_DECORATOR_TYPE)) {
      throw new IllegalStateException(
          "ConfigBuilder already has a decorator of type "
              + KRYON_DECORATOR_TYPE
              + ". Harness cannot prime mock data");
    }

    vajramIdMockData.forEach(
        (vajramID, vajramRequestErrableMap) ->
            vajramRequestErrableMap.forEach(
                (objectVajramRequest, objectErrable) ->
                    requestLevelCache.primeCache(objectVajramRequest, objectErrable.toFuture())));
    krystalExecutorConfigBuilder.kryonDecoratorConfig(
        new KryonDecoratorConfig(
            KRYON_DECORATOR_TYPE,
            executionContext -> vajramIdMockData.containsKey(executionContext.vajramID()),
            executionContext -> KRYON_DECORATOR_TYPE,
            kryonExecutionContext -> requestLevelCache.kryonDecorator()));
    krystalExecutorConfigBuilder.outputLogicDecoratorConfig(
        new OutputLogicDecoratorConfig(
            OUTPUT_LOGIC_DECORATOR_TYPE,
            executionContext -> vajramIdMockData.containsKey(executionContext.vajramID()),
            executionContext -> OUTPUT_LOGIC_DECORATOR_TYPE,
            kryonExecutionContext -> requestLevelCache.outputLogicDecorator()));
    return krystalExecutorConfigBuilder;
  }
}
