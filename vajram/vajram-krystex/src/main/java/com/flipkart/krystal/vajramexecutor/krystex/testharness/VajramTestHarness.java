package com.flipkart.krystal.vajramexecutor.krystex.testharness;

import static com.flipkart.krystal.vajram.VajramID.vajramID;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajramexecutor.krystex.testharness.mock_repository.MockData;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/***
 *Test harness is a collection of software or test data used by developers for unit testing.
 *It is responsible for test drivers and stubs. In the context of Vajrams, VajramTestHarness is
 * responsible for preparing the krystex executor for test by providing the necessary stubbing
 * capability of the dependancy vajrams for the given vajram in test.
 */
@Slf4j
public class VajramTestHarness {
  private Map<String, Map<VajramRequest<Object>, ValueOrError<Object>>> vajramIdMockData;
  private KryonExecutorConfigBuilder kryonExecutorConfigBuilder;

  private VajramTestHarness(KryonExecutorConfigBuilder kryonExecutorBuilder) {
    this.kryonExecutorConfigBuilder = kryonExecutorBuilder;
  }

  public static VajramTestHarness prepareForTest(KryonExecutorConfigBuilder kryonExecutorBuilder) {
    return new VajramTestHarness(kryonExecutorBuilder);
  }

  public <T> VajramTestHarness withMock(VajramRequest<T> request, ValueOrError<T> response) {
    String requestName = request.getClass().getSimpleName();
    int index = requestName.lastIndexOf('R');
    String vajramId = requestName.substring(0, index);
    Map<VajramRequest<Object>, ValueOrError<Object>> mockDataMap =
        this.vajramIdMockData.get(vajramId);
    if (Objects.isNull(mockDataMap)) {
      this.vajramIdMockData.put(
          vajramId, Map.of((VajramRequest<Object>) request, (ValueOrError<Object>) response));
    } else {
      ValueOrError<Object> valueOrError = mockDataMap.get((VajramRequest<Object>) request);
      if (Objects.isNull(valueOrError)) {
        mockDataMap.put((VajramRequest<Object>) request, (ValueOrError<Object>) response);
        this.vajramIdMockData.put(vajramId, mockDataMap);
      } else {
        log.info("Ignoring mock data since the given request object is already mocked");
      }
    }
    return this;
  }

  public VajramTestHarness withMock(MockData<?> mockData) {
    //noinspection unchecked
    withMock(
        (VajramRequest<Object>) mockData.request(), (ValueOrError<Object>) mockData.response());
    return this;
  }

  private VajramPrimer getVajramMocker(String kryonId) {
    Map<VajramRequest<Object>, ValueOrError<Object>> mockDataMap =
        this.vajramIdMockData.get(kryonId);
    return new VajramPrimer(vajramID(kryonId), mockDataMap, false);
  }

  public KryonExecutorConfig buildConfig() {
    // TODO get existing decorators for a given kryon and append the vajramPrimer
    return kryonExecutorConfigBuilder
        .kryonDecoratorsProvider(kryonId -> getVajramMocker(kryonId.value()))
        .build();
  }
}
