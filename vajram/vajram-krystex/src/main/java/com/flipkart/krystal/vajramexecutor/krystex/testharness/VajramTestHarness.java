package com.flipkart.krystal.vajramexecutor.krystex.testharness;

import static com.flipkart.krystal.vajram.VajramID.vajramID;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.testharness.mock_repository.MockData;
import java.util.LinkedHashMap;
import java.util.Map;

public class VajramTestHarness {
  // TODO put strict validation that same request object cannot be mocked twice in this class
  private Map<String, Map<VajramRequest<Object>, ValueOrError<Object>>> vajramIdMockData;
  private KryonExecutorConfigBuilder kryonExecutorBuilder;

  private VajramTestHarness(KryonExecutorConfigBuilder kryonExecutorBuilder) {
    this.kryonExecutorBuilder = kryonExecutorBuilder;
  }

  public static VajramTestHarness harnessForTest(KryonExecutorConfigBuilder kryonExecutorBuilder) {
    return new VajramTestHarness(kryonExecutorBuilder);
  }

  public <T> VajramTestHarness withMock(VajramRequest<T> request, ValueOrError<T> response) {
    String requestName = request.getClass().getSimpleName();
    int index = requestName.lastIndexOf('R');
    String vajramId = requestName.substring(0, index);
    // TODO add to existing vajramId
    //noinspection unchecked
    this.vajramIdMockData.put(
        vajramId, Map.of((VajramRequest<Object>) request, (ValueOrError<Object>) response));
    return this;
  }

  public VajramTestHarness withMock(MockData<?> mockData) {
    //noinspection unchecked
    withMock(
        (VajramRequest<Object>) mockData.request(), (ValueOrError<Object>) mockData.response());
    return this;
  }

  public KryonExecutorConfig buildConfig() {
    // TODO get existing decorators for a given kryon and append the vajramPrimer
    return kryonExecutorBuilder
        .kryonDecoratorsProvider(kryonId -> getVajramMocker(kryonId.value()))
        .build();
  }

  private VajramMocker getVajramMocker(String kryonId) {
    VajramMocker vajramMocker = null;
    // TODO replace for loop
    for (Map.Entry<String, Map<VajramRequest<Object>, ValueOrError<Object>>> entry :
        this.vajramIdMockData.entrySet()) {
      if (entry.getKey().equals(kryonId)) {
        Map<VajramRequest<Object>, ValueOrError<Object>> mockDataMap = entry.getValue();
        vajramMocker = new VajramMocker(vajramID(entry.getKey()), mockDataMap, false);
      }
    }
    return vajramMocker;
  }
}
