package com.flipkart.krystal.vajramexecutor.krystex.testharness;

import static com.flipkart.krystal.vajram.VajramID.vajramID;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Test harness is a collection of software/test data used by developers for unit testing. It is
 * responsible for test drivers and stubs. In the context of Vajrams, VajramTestHarness is
 * responsible for preparing the krystex executor for test by providing the necessary stubbing
 * capability for all the dependancy Vajrams of the given dependant Vajram in test.
 */
public class VajramTestHarness {

  private Map<String, Map<VajramRequest<Object>, Errable<Object>>> vajramIdMockData;
  private KrystexVajramExecutorConfig kryonExecutorConfigBuilder;

  private VajramTestHarness(KrystexVajramExecutorConfig kryonExecutorBuilder) {
    this.kryonExecutorConfigBuilder = kryonExecutorBuilder;
    this.vajramIdMockData = new HashMap<>();
  }

  public static VajramTestHarness prepareForTest(KrystexVajramExecutorConfig kryonExecutorBuilder) {
    return new VajramTestHarness(kryonExecutorBuilder);
  }

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
    kryonExecutorConfigBuilder
        .kryonExecutorConfigBuilder()
        .requestScopedKryonDecoratorConfig(
            VajramPrimer.class.getName(),
            new KryonDecoratorConfig(
                VajramPrimer.class.getName(),
                executionContext ->
                    vajramIdMockData.containsKey(executionContext.kryonId().value()),
                executionContext -> VajramPrimer.class.getName(),
                kryonExecutionContext -> {
                  return new VajramPrimer(this.vajramIdMockData, true);
                }));
    return kryonExecutorConfigBuilder;
  }
}
