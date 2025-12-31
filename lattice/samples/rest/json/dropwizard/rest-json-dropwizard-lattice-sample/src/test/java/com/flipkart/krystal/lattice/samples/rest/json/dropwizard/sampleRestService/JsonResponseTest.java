package com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.lattice.samples.rest.json.dropwizard.sampleRestService.models.ByteArray;
import com.flipkart.krystal.lattice.samples.rest.json.sampleRestService.models.JsonResponse_ImmutJson;
import com.google.common.base.Charsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonResponseTest {

  @Test
  void jsonSerde_success() throws Exception {
    JsonResponse_ImmutJson immutJson =
        JsonResponse_ImmutJson._builder()
            .string("Hello")
            .optionalInteger(42)
            .nullableIntegerMayFailConditionally(30)
            .nullableInteger(43)
            .optionalIntArray(List.of(1, 4, 5, 2))
            .mandatoryInt(5)
            .defaultInt(89)
            .mandatoryStringPartialConstruction("hihihi")
            .mapTypedField(Map.of("X", "A", "Y", "B", "Z", "C"))
            .byteArray(new ByteArray(new byte[] {23, 45, 23, 56, 67, 64, 45, 45, 3, 45, 56}))
            ._build();
    byte[] serializedPayload = immutJson._serialize();
    System.out.println(new String(serializedPayload, Charsets.UTF_8));
    JsonResponse_ImmutJson deserialized = new JsonResponse_ImmutJson(serializedPayload);
    assertThat(deserialized).isEqualTo(immutJson);
  }
}
