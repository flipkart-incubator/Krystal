package com.flipkart.krystal.vajramexecutor.krystex.testharness;

import static com.flipkart.krystal.vajram.VajramID.vajramID;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.krystex.kryon.KryonDecorator;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.vajram.VajramRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Test harness is a collection of software/test data used by developers for unit testing. It is
 * responsible for test drivers and stubs. In the context of Vajrams, VajramTestHarness is
 * responsible for preparing the krystex executor for test by providing the necessary stubbing
 * capability for all the dependancy Vajrams of the given dependant Vajram in test.
 */
public class VajramTestHarness {

  private Map<String, Map<VajramRequest<Object>, Errable<Object>>> vajramIdMockData;
  private KryonExecutorConfigBuilder kryonExecutorConfigBuilder;

  private VajramTestHarness(KryonExecutorConfigBuilder kryonExecutorBuilder) {
    this.kryonExecutorConfigBuilder = kryonExecutorBuilder;
    this.vajramIdMockData = new HashMap<>();
  }

  public static VajramTestHarness prepareForTest(KryonExecutorConfigBuilder kryonExecutorBuilder) {
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

  private List<KryonDecorator> getVajramMocker(String kryonId) {
    Map<VajramRequest<Object>, Errable<Object>> mockDataMap = this.vajramIdMockData.get(kryonId);
    return Objects.isNull(mockDataMap)
        ? List.of()
        : List.of(new VajramPrimer(vajramID(kryonId), mockDataMap, true));
  }

  public KryonExecutorConfig buildConfig() {
    Function<KryonId, List<KryonDecorator>> kryonIdListFunction =
        kryonExecutorConfigBuilder.build().kryonDecoratorsProvider();
    return kryonExecutorConfigBuilder
        .kryonDecoratorsProvider(
            kryonId -> {
              List<KryonDecorator> list = new ArrayList<>(kryonIdListFunction.apply(kryonId));
              list.addAll(getVajramMocker(kryonId.value()));
              return list;
            })
        .build();
  }
}
