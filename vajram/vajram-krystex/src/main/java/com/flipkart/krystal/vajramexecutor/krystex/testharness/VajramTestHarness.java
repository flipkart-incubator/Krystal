package com.flipkart.krystal.vajramexecutor.krystex.testharness;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.krystex.caching.RequestLevelCache;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Test harness is a collection of software/test data used by developers for unit testing. It is
 * responsible for test drivers and stubs. In the context of Vajrams, VajramTestHarness is
 * responsible for preparing the krystex executor for test by providing the necessary stubbing
 * capability for all the dependancy Vajrams of the given dependant Vajram in test.
 */
public class VajramTestHarness {

  private final Map<String, Map<VajramRequest<Object>, Errable<Object>>> vajramIdMockData;
  private final KrystexVajramExecutorConfig kryonExecutorConfigBuilder;
  private final RequestLevelCache requestLevelCache;

  @Inject
  public VajramTestHarness(
      KrystexVajramExecutorConfig kryonExecutorBuilder, RequestLevelCache requestLevelCache) {
    this.kryonExecutorConfigBuilder = kryonExecutorBuilder;
    this.requestLevelCache = requestLevelCache;
    this.vajramIdMockData = new HashMap<>();
  }

  public static VajramTestHarness prepareForTest(
      KrystexVajramExecutorConfig kryonExecutorBuilder, RequestLevelCache requestLevelCache) {
    return new VajramTestHarness(kryonExecutorBuilder, requestLevelCache);
  }

  @SuppressWarnings("unchecked")
  public <T> VajramTestHarness withMock(VajramRequest<T> request, Errable<T> response) {
    String requestName = request.getClass().getSimpleName();
    int index = requestName.lastIndexOf('R');
    String vajramId = requestName.substring(0, index);
    Map<VajramRequest<Object>, Errable<Object>> mockDataMap = this.vajramIdMockData.get(vajramId);
    if (Objects.isNull(mockDataMap)) {
      this.vajramIdMockData.put(
          vajramId, Map.of((VajramRequest<Object>) request, (Errable<Object>) response));
    } else {
      Errable<Object> errable = mockDataMap.get((VajramRequest<Object>) request);
      if (Objects.isNull(errable)) {
        mockDataMap.put((VajramRequest<Object>) request, (Errable<Object>) response);
        this.vajramIdMockData.put(vajramId, mockDataMap);
      }
    }
    return this;
  }

  public KrystexVajramExecutorConfig buildConfig() {
    vajramIdMockData.forEach(
        (s, vajramRequestErrableMap) -> {
          vajramRequestErrableMap.forEach(
              (objectVajramRequest, objectErrable) -> {
                requestLevelCache.primeCache(
                    s, objectVajramRequest.toFacetValues(), objectErrable.toFuture());
              });
        });
    KryonDecoratorConfig kryonDecoratorConfig =
        kryonExecutorConfigBuilder
            .kryonExecutorConfigBuilder()
            .build()
            .requestScopedKryonDecoratorConfigs()
            .get(RequestLevelCache.DECORATOR_TYPE);
    if (kryonDecoratorConfig == null) {
      kryonExecutorConfigBuilder
          .kryonExecutorConfigBuilder()
          .requestScopedKryonDecoratorConfig(
              RequestLevelCache.DECORATOR_TYPE,
              new KryonDecoratorConfig(
                  RequestLevelCache.DECORATOR_TYPE,
                  executionContext ->
                      vajramIdMockData.containsKey(executionContext.kryonId().value()),
                  executionContext -> RequestLevelCache.DECORATOR_TYPE,
                  kryonExecutionContext -> {
                    return requestLevelCache;
                  }));
    }
    return kryonExecutorConfigBuilder;
  }
}
