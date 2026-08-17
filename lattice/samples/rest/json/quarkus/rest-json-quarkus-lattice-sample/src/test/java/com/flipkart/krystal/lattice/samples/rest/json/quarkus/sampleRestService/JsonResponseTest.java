package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService;

import static com.flipkart.krystal.data.Errable.withValue;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.NonNil;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.InnerDataV2;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.InnerDataV2_ImmutJson;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.InnerDataV2_ImmutPojo;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.InnerData_ImmutJson;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.InnerData_ImmutPojo;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.JsonResponse_ImmutJson;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.JsonResponse_ImmutPojo;
import com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService.models.Priority;
import com.flipkart.krystal.model.array.SimpleByteArray;
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
            .byteArray(
                SimpleByteArray.copyOf(new byte[] {23, 45, 23, 56, 67, 64, 45, 45, 3, 45, 56}))
            .nestedData(InnerData_ImmutJson._builder().value("Hello").count(11)._build())
            .priority(Priority.HIGH)
            ._build();
    byte[] serializedPayload = immutJson._serialize().readAllBytes();
    System.out.println(new String(serializedPayload, UTF_8));
    JsonResponse_ImmutJson deserialized = new JsonResponse_ImmutJson(serializedPayload);
    assertThat(deserialized).isEqualTo(immutJson);
  }

  @Test
  void unknownEnumValue_deserializesToUnknown() throws Exception {
    // Build a valid response with a known enum value, serialize it, then replace the enum value
    JsonResponse_ImmutJson original =
        JsonResponse_ImmutJson._builder()
            .string("test")
            .optionalInteger(1)
            .nullableIntegerMayFailConditionally(2)
            .nullableInteger(3)
            .mandatoryInt(4)
            .mandatoryStringPartialConstruction("sp")
            .byteArray(SimpleByteArray.copyOf(new byte[] {1}))
            .nestedData(InnerData_ImmutJson._builder().value("v").count(1)._build())
            .priority(Priority.HIGH)
            ._build();
    String json = new String(original._serialize().readAllBytes(), UTF_8);

    // Replace "HIGH" with a non-existent enum value
    String modifiedJson = json.replace("\"HIGH\"", "\"NONEXISTENT\"");

    // Deserialize — unknown enum should fall back to UNKNOWN
    JsonResponse_ImmutJson deserialized = new JsonResponse_ImmutJson(modifiedJson.getBytes(UTF_8));
    assertThat(deserialized.priority()).isEqualTo(Priority.UNKNOWN);
  }

  @Test
  void nestedPojo_build_success() {
    JsonResponse_ImmutPojo.Builder immutJsonBuilder =
        JsonResponse_ImmutPojo._builder()
            .string("Hello")
            .optionalInteger(42)
            .nullableIntegerMayFailConditionally(30)
            .nullableInteger(43)
            .optionalIntArray(List.of(1, 4, 5, 2))
            .mandatoryInt(5)
            .defaultInt(89)
            .mandatoryStringPartialConstruction("hihihi")
            .mapTypedField(Map.of("X", "A", "Y", "B", "Z", "C"))
            .byteArray(
                SimpleByteArray.copyOf(new byte[] {23, 45, 23, 56, 67, 64, 45, 45, 3, 45, 56}))
            .nestedData(InnerData_ImmutPojo._builder().value("Hello").count(11))
            .priority(Priority.MEDIUM);
    assertThat(immutJsonBuilder._build()).isEqualTo(immutJsonBuilder._newCopy()._build());
  }

  @Test
  void nestedPojoList_build_success() {
    JsonResponse_ImmutPojo.Builder immutJsonBuilder =
        JsonResponse_ImmutPojo._builder()
            .string("Hello")
            .optionalInteger(42)
            .nullableIntegerMayFailConditionally(30)
            .nullableInteger(43)
            .optionalIntArray(List.of(1, 4, 5, 2))
            .mandatoryInt(5)
            .defaultInt(89)
            .mandatoryStringPartialConstruction("hihihi")
            .mapTypedField(Map.of("X", "A", "Y", "B", "Z", "C"))
            .byteArray(
                SimpleByteArray.copyOf(new byte[] {23, 45, 23, 56, 67, 64, 45, 45, 3, 45, 56}))
            .nestedDataList(
                List.of(
                    InnerData_ImmutPojo._builder().value("Hello").count(11)._build(),
                    InnerData_ImmutPojo._builder().value("Hello Again").count(34)._build()))
            .priority(Priority.LOW);
    assertThat(immutJsonBuilder._build()).isEqualTo(immutJsonBuilder._newCopy()._build());
  }

  // --- Errable field tests ---

  @Test
  void errableField_withValue_serializesAndDeserializesInnerValue() throws Exception {
    JsonResponse_ImmutJson original =
        JsonResponse_ImmutJson._builder()
            .string("errable-test")
            .mandatoryInt(1)
            .errableMessage(withValue("hello errable"))
            ._build();

    assertThat(original.errableMessage()).isEqualTo(withValue("hello errable"));

    byte[] bytes = original._serialize().readAllBytes();
    String json = new String(bytes, UTF_8);
    // The inner value should appear directly in the JSON
    assertThat(json).contains("\"errableMessage\":\"hello errable\"");

    JsonResponse_ImmutJson deserialized = new JsonResponse_ImmutJson(bytes);
    assertThat(deserialized.errableMessage()).isEqualTo(withValue("hello errable"));
  }

  @Test
  void errableField_absent_deserializesToNil() throws Exception {
    // Build without setting errableMessage — field should default to Nil
    JsonResponse_ImmutJson original =
        JsonResponse_ImmutJson._builder().string("no-errable").mandatoryInt(2)._build();

    assertThat(original.errableMessage()).isEqualTo(Errable.nil());

    byte[] bytes = original._serialize().readAllBytes();
    String json = new String(bytes, UTF_8);
    // Nil Errable serializes as null JSON value
    assertThat(json).contains("\"errableMessage\":null");

    JsonResponse_ImmutJson deserialized = new JsonResponse_ImmutJson(bytes);
    assertThat(deserialized.errableMessage()).isEqualTo(Errable.nil());
  }

  @Test
  void errableField_setViaErrableSetter_withValue() {
    JsonResponse_ImmutJson built =
        JsonResponse_ImmutJson._builder()
            .string("errable-setter")
            .mandatoryInt(3)
            .errableMessage(withValue("via errable setter"))
            ._build();

    assertThat(built.errableMessage()).isEqualTo(withValue("via errable setter"));
  }

  @Test
  void errableField_setViaErrableSetter_nil() {
    JsonResponse_ImmutJson built =
        JsonResponse_ImmutJson._builder()
            .string("errable-nil-setter")
            .mandatoryInt(4)
            .errableMessage(Errable.nil())
            ._build();

    assertThat(built.errableMessage()).isEqualTo(Errable.nil());
  }

  @Test
  void errableField_setViaErrableSetter_failure() {
    RuntimeException cause = new RuntimeException("upstream failure");
    JsonResponse_ImmutJson built =
        JsonResponse_ImmutJson._builder()
            .string("errable-failure")
            .mandatoryInt(5)
            .errableMessage(Errable.withError(cause))
            ._build();

    assertThat(built.errableMessage()).isNotInstanceOf(NonNil.class);
  }

  @Test
  void errableField_pojo_withValue_roundTrip() {
    JsonResponse_ImmutPojo built =
        JsonResponse_ImmutPojo._builder()
            .string("pojo-errable")
            .mandatoryInt(6)
            .errableMessage(withValue("pojo value"))
            ._build();

    assertThat(built.errableMessage()).isEqualTo(withValue("pojo value"));
    assertThat(built).isEqualTo(built._newCopy()._build());
  }

  @Test
  void errableField_pojo_nil_roundTrip() {
    JsonResponse_ImmutPojo built =
        JsonResponse_ImmutPojo._builder().string("pojo-nil").mandatoryInt(7)._build();

    assertThat(built.errableMessage()).isEqualTo(Errable.nil());
    assertThat(built).isEqualTo(built._newCopy()._build());
  }

  // --- Errable-container field tests (List<Errable<T>>, Errable<List<T>>,
  // Errable<List<Errable<T>>>, Map<K,Errable<T>>, Errable<Map<K,T>>,
  // Errable<Map<K,Errable<T>>>), for T = primitive/Enum/Model ---

  private static JsonResponse_ImmutJson.Builder baseBuilder(String label) {
    return JsonResponse_ImmutJson._builder().string(label).mandatoryInt(1);
  }

  @Test
  void errableContainerFields_json_roundTrip_withValues() throws Exception {
    InnerDataV2_ImmutJson inner1 = InnerDataV2_ImmutJson._builder().value("a").count(1)._build();
    InnerDataV2_ImmutJson inner2 = InnerDataV2_ImmutJson._builder().value("b").count(2)._build();

    JsonResponse_ImmutJson original =
        baseBuilder("errable-containers")
            .errableInts(List.of(withValue(1), withValue(2)))
            .errablePriorities(List.of(withValue(Priority.HIGH)))
            .errableInnerDataList(List.of(withValue(inner1)))
            .errableIntList(withValue(List.of(1, 2, 3)))
            .errablePriorityList(withValue(List.of(Priority.LOW)))
            .errableInnerDataListWhole(withValue(List.of(inner1, inner2)))
            .errableListOfErrableInts(withValue(List.of(withValue(1))))
            .errableListOfErrablePriorities(withValue(List.of(withValue(Priority.MEDIUM))))
            .errableListOfErrableInnerData(withValue(List.of(withValue(inner1))))
            .errableIntMap(Map.of("a", withValue(1)))
            .errablePriorityMap(Map.of("a", withValue(Priority.HIGH)))
            .errableInnerDataMap(Map.of("a", withValue(inner1)))
            .errableIntMapWhole(withValue(Map.of("a", 1)))
            .errablePriorityMapWhole(withValue(Map.of("a", Priority.LOW)))
            .errableInnerDataMapWhole(withValue(Map.of("a", inner1)))
            .errableMapOfErrableInts(withValue(Map.of("a", withValue(1))))
            .errableMapOfErrablePriorities(withValue(Map.of("a", withValue(Priority.MEDIUM))))
            .errableMapOfErrableInnerData(withValue(Map.of("a", withValue(inner1))))
            ._build();

    byte[] bytes = original._serialize().readAllBytes();
    JsonResponse_ImmutJson d = new JsonResponse_ImmutJson(bytes);

    assertThat(d.errableInts()).containsExactly(withValue(1), withValue(2));
    assertThat(d.errablePriorities()).containsExactly(withValue(Priority.HIGH));
    assertThat(d.errableInnerDataList())
        .extracting(Errable::value)
        .extracting(InnerDataV2::count)
        .containsExactly(1);

    assertThat(d.errableIntList()).isEqualTo(withValue(List.of(1, 2, 3)));
    assertThat(d.errablePriorityList()).isEqualTo(withValue(List.of(Priority.LOW)));
    assertThat(d.errableInnerDataListWhole().value())
        .extracting(InnerDataV2::count)
        .containsExactly(1, 2);

    assertThat(d.errableListOfErrableInts()).isEqualTo(withValue(List.of(withValue(1))));
    assertThat(d.errableListOfErrablePriorities())
        .isEqualTo(withValue(List.of(withValue(Priority.MEDIUM))));
    assertThat(d.errableListOfErrableInnerData().value())
        .extracting(e -> e.value().count())
        .containsExactly(1);

    assertThat(d.errableIntMap()).isEqualTo(Map.of("a", withValue(1)));
    assertThat(d.errablePriorityMap()).isEqualTo(Map.of("a", withValue(Priority.HIGH)));
    assertThat(d.errableInnerDataMap().get("a").value().count()).isEqualTo(1);

    assertThat(d.errableIntMapWhole()).isEqualTo(withValue(Map.of("a", 1)));
    assertThat(d.errablePriorityMapWhole()).isEqualTo(withValue(Map.of("a", Priority.LOW)));
    assertThat(d.errableInnerDataMapWhole().value().get("a").count()).isEqualTo(1);

    assertThat(d.errableMapOfErrableInts()).isEqualTo(withValue(Map.of("a", withValue(1))));
    assertThat(d.errableMapOfErrablePriorities())
        .isEqualTo(withValue(Map.of("a", withValue(Priority.MEDIUM))));
    assertThat(d.errableMapOfErrableInnerData().value().get("a").value().count()).isEqualTo(1);

    assertThat(d).isEqualTo(original);
  }

  @Test
  void errableContainerFields_json_defaults_areEmpty() {
    JsonResponse_ImmutJson d = baseBuilder("errable-container-defaults")._build();

    assertThat(d.errableInts()).isEmpty();
    assertThat(d.errablePriorities()).isEmpty();
    assertThat(d.errableInnerDataList()).isEmpty();
    assertThat(d.errableIntList()).isEqualTo(Errable.nil());
    assertThat(d.errableIntMap()).isEmpty();
    assertThat(d.errablePriorityMap()).isEmpty();
    assertThat(d.errableInnerDataMap()).isEmpty();
    assertThat(d.errableIntMapWhole()).isEqualTo(Errable.nil());
  }

  @Test
  void errableContainerFields_json_nilElementsRoundTrip() throws Exception {
    InnerDataV2_ImmutJson inner1 = InnerDataV2_ImmutJson._builder().value("a").count(1)._build();

    JsonResponse_ImmutJson original =
        baseBuilder("errable-container-nil-elements")
            .errableInts(List.of(withValue(1), Errable.nil()))
            .errablePriorities(List.of(Errable.nil(), withValue(Priority.HIGH)))
            .errableInnerDataList(List.of(Errable.nil(), withValue(inner1)))
            .errableIntMap(Map.of("keep", withValue(9), "nil", Errable.nil()))
            .errableInnerDataMap(Map.of("keep", withValue(inner1), "nil", Errable.nil()))
            ._build();

    byte[] bytes = original._serialize().readAllBytes();
    JsonResponse_ImmutJson d = new JsonResponse_ImmutJson(bytes);

    // Unlike protobuf, JSON round-trips per-element Nil exactly (nulls are addressable).
    assertThat(d.errableInts()).containsExactly(withValue(1), Errable.nil());
    assertThat(d.errablePriorities()).containsExactly(Errable.nil(), withValue(Priority.HIGH));
    assertThat(d.errableInnerDataList().get(0)).isEqualTo(Errable.nil());
    assertThat(d.errableInnerDataList().get(1).value().count()).isEqualTo(1);
    assertThat(d.errableIntMap()).isEqualTo(Map.of("keep", withValue(9), "nil", Errable.nil()));
    assertThat(d.errableInnerDataMap().get("nil")).isEqualTo(Errable.nil());
    assertThat(d.errableInnerDataMap().get("keep").value().count()).isEqualTo(1);

    assertThat(d).isEqualTo(original);
  }

  @Test
  void errableContainerFields_json_wholeContainerNil_roundTrip() throws Exception {
    JsonResponse_ImmutJson original =
        baseBuilder("errable-whole-nil")
            .errableIntList(Errable.nil())
            .errableIntMapWhole(Errable.nil())
            ._build();

    byte[] bytes = original._serialize().readAllBytes();
    JsonResponse_ImmutJson d = new JsonResponse_ImmutJson(bytes);

    // Unlike protobuf, JSON can represent an absent field, so Nil round-trips as Nil exactly.
    assertThat(d.errableIntList()).isEqualTo(Errable.nil());
    assertThat(d.errableIntMapWhole()).isEqualTo(Errable.nil());
    assertThat(d).isEqualTo(original);
  }

  @Test
  void errableContainerFields_pojo_builderRoundTrip() {
    InnerDataV2_ImmutPojo inner1 = InnerDataV2_ImmutPojo._builder().value("a").count(3)._build();

    JsonResponse_ImmutPojo built =
        JsonResponse_ImmutPojo._builder()
            .string("pojo-errable-containers")
            .mandatoryInt(1)
            .errableInts(List.of(withValue(1), withValue(2)))
            .errableInnerDataList(List.of(withValue(inner1)))
            .errableIntList(withValue(List.of(1, 2)))
            .errableInnerDataListWhole(withValue(List.of(inner1)))
            .errableListOfErrableInts(withValue(List.of(withValue(1), Errable.nil())))
            .errableIntMap(Map.of("a", withValue(1)))
            .errableInnerDataMap(Map.of("a", withValue(inner1)))
            .errableIntMapWhole(withValue(Map.of("a", 1)))
            .errableMapOfErrableInts(withValue(Map.of("a", withValue(1))))
            ._build();

    assertThat(built.errableInts()).containsExactly(withValue(1), withValue(2));
    assertThat(built.errableInnerDataList())
        .extracting(Errable::value)
        .extracting(InnerDataV2::count)
        .containsExactly(3);
    assertThat(built.errableIntList()).isEqualTo(withValue(List.of(1, 2)));
    assertThat(built.errableInnerDataListWhole().value())
        .extracting(InnerDataV2::count)
        .containsExactly(3);
    assertThat(built.errableListOfErrableInts())
        .isEqualTo(withValue(List.of(withValue(1), Errable.nil())));
    assertThat(built.errableIntMap()).isEqualTo(Map.of("a", withValue(1)));
    assertThat(built.errableInnerDataMap().get("a").value().count()).isEqualTo(3);
    assertThat(built.errableIntMapWhole()).isEqualTo(withValue(Map.of("a", 1)));
    assertThat(built.errableMapOfErrableInts()).isEqualTo(withValue(Map.of("a", withValue(1))));

    assertThat(built).isEqualTo(built._newCopy()._build());
  }
}
