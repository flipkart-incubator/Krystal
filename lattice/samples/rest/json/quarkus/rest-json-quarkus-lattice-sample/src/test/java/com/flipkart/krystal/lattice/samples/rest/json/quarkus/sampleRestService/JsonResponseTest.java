package com.flipkart.krystal.lattice.samples.rest.json.quarkus.sampleRestService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.NonNil;
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
            .errableMessage("hello errable")
            ._build();

    assertThat(original.errableMessage()).isEqualTo(Errable.withValue("hello errable"));

    byte[] bytes = original._serialize().readAllBytes();
    String json = new String(bytes, UTF_8);
    // The inner value should appear directly in the JSON
    assertThat(json).contains("\"errableMessage\":\"hello errable\"");

    JsonResponse_ImmutJson deserialized = new JsonResponse_ImmutJson(bytes);
    assertThat(deserialized.errableMessage()).isEqualTo(Errable.withValue("hello errable"));
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
            .errableMessage(Errable.withValue("via errable setter"))
            ._build();

    assertThat(built.errableMessage()).isEqualTo(Errable.withValue("via errable setter"));
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
            .errableMessage("pojo value")
            ._build();

    assertThat(built.errableMessage()).isEqualTo(Errable.withValue("pojo value"));
    assertThat(built).isEqualTo(built._newCopy()._build());
  }

  @Test
  void errableField_pojo_nil_roundTrip() {
    JsonResponse_ImmutPojo built =
        JsonResponse_ImmutPojo._builder().string("pojo-nil").mandatoryInt(7)._build();

    assertThat(built.errableMessage()).isEqualTo(Errable.nil());
    assertThat(built).isEqualTo(built._newCopy()._build());
  }
}
