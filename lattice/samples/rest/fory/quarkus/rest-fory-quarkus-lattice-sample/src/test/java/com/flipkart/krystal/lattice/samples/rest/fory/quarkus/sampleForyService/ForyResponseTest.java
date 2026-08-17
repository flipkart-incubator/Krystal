package com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.NonNil;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyInnerData_ImmutFory;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyInnerData_ImmutPojo;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyRequest_ImmutFory;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyResponse_ImmutFory;
import com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService.models.ForyResponse_ImmutPojo;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ForyResponseTest {

  @Test
  void forySerde_roundTrip_success() throws Exception {
    ForyResponse_ImmutFory original =
        ForyResponse_ImmutFory._builder()
            .message("Hello Fory!")
            .optionalInteger(42)
            .nullableInteger(99)
            .mandatoryInt(5)
            .defaultInt(17)
            .intList(List.of(1, 2, 3))
            .stringMap(Map.of("key1", "val1", "key2", "val2"))
            .nestedData(ForyInnerData_ImmutFory._builder().value("inner").count(10)._build())
            ._build();

    byte[] serialized = original._serialize().readAllBytes();
    assertThat(serialized).isNotEmpty();

    ForyResponse_ImmutFory deserialized = new ForyResponse_ImmutFory(serialized);
    assertThat(deserialized.message()).isEqualTo("Hello Fory!");
    assertThat(deserialized.optionalInteger()).hasValue(42);
    assertThat(deserialized.nullableInteger()).isEqualTo(99);
    assertThat(deserialized.mandatoryInt()).isEqualTo(5);
    assertThat(deserialized.defaultInt()).isEqualTo(17);
    assertThat(deserialized.intList()).containsExactly(1, 2, 3);
    assertThat(deserialized.stringMap()).containsEntry("key1", "val1");
    assertThat(deserialized.nestedData().value()).isEqualTo("inner");
    assertThat(deserialized.nestedData().count()).isEqualTo(10);
  }

  @Test
  void forySerde_withNullOptionalFields_success() throws Exception {
    ForyResponse_ImmutFory original =
        ForyResponse_ImmutFory._builder().message("Sparse").mandatoryInt(1)._build();

    byte[] serialized = original._serialize().readAllBytes();
    ForyResponse_ImmutFory deserialized = new ForyResponse_ImmutFory(serialized);

    assertThat(deserialized.message()).isEqualTo("Sparse");
    assertThat(deserialized.mandatoryInt()).isEqualTo(1);
    assertThat(deserialized.path()).isNull();
    assertThat(deserialized.optionalInteger()).isEmpty();
    assertThat(deserialized.nullableInteger()).isNull();
  }

  @Test
  void forySerde_nestedDataList_success() throws Exception {
    ForyResponse_ImmutFory original =
        ForyResponse_ImmutFory._builder()
            .message("list test")
            .mandatoryInt(3)
            .nestedDataList(
                List.of(
                    ForyInnerData_ImmutFory._builder().value("A").count(1)._build(),
                    ForyInnerData_ImmutFory._builder().value("B").count(2)._build()))
            ._build();

    byte[] serialized = original._serialize().readAllBytes();
    ForyResponse_ImmutFory deserialized = new ForyResponse_ImmutFory(serialized);

    assertThat(deserialized.nestedDataList()).hasSize(2);
    assertThat(deserialized.nestedDataList().get(0).value()).isEqualTo("A");
    assertThat(deserialized.nestedDataList().get(1).count()).isEqualTo(2);
  }

  @Test
  void forySerde_namedInnerData_success() throws Exception {
    ForyResponse_ImmutFory original =
        ForyResponse_ImmutFory._builder()
            .message("map test")
            .mandatoryInt(4)
            .namedInnerData(
                Map.of(
                    "first",
                    ForyInnerData_ImmutFory._builder().value("X").count(10)._build(),
                    "second",
                    ForyInnerData_ImmutFory._builder().value("Y").count(20)._build()))
            ._build();

    byte[] serialized = original._serialize().readAllBytes();
    ForyResponse_ImmutFory deserialized = new ForyResponse_ImmutFory(serialized);

    assertThat(deserialized.namedInnerData()).hasSize(2);
    assertThat(deserialized.namedInnerData().get("first").value()).isEqualTo("X");
    assertThat(deserialized.namedInnerData().get("second").count()).isEqualTo(20);
  }

  @Test
  void foryRequest_serde_success() throws Exception {
    ForyRequest_ImmutFory original =
        ForyRequest_ImmutFory._builder()
            .mandatoryInput(7)
            .mandatoryLongInput(99L)
            .optionalInput(42)
            .repeatedInts(List.of(10, 20, 30))
            .innerData(ForyInnerData_ImmutFory._builder().value("nested").count(5)._build())
            ._build();

    byte[] serialized = original._serialize().readAllBytes();
    ForyRequest_ImmutFory deserialized = new ForyRequest_ImmutFory(serialized);

    assertThat(deserialized.mandatoryInput()).isEqualTo(7);
    assertThat(deserialized.mandatoryLongInput()).isEqualTo(99L);
    assertThat(deserialized.optionalInput()).isEqualTo(42);
    assertThat(deserialized.repeatedInts()).containsExactly(10, 20, 30);
    assertThat(deserialized.innerData().value()).isEqualTo("nested");
  }

  @Test
  void pojoBuilder_success() {
    ForyResponse_ImmutPojo.Builder builder =
        ForyResponse_ImmutPojo._builder()
            .message("Pojo test")
            .mandatoryInt(2)
            .nestedData(ForyInnerData_ImmutPojo._builder().value("v").count(1));
    assertThat(builder._build()).isEqualTo(builder._newCopy()._build());
  }

  // --- Errable field tests ---

  @Test
  void errableField_withValue_serializesAndDeserializes() throws Exception {
    ForyResponse_ImmutFory original =
        ForyResponse_ImmutFory._builder()
            .message("errable-test")
            .mandatoryInt(1)
            .errableNote(Errable.withValue("hello fory errable"))
            ._build();

    assertThat(original.errableNote()).isEqualTo(Errable.withValue("hello fory errable"));

    byte[] bytes = original._serialize().readAllBytes();
    ForyResponse_ImmutFory deserialized = new ForyResponse_ImmutFory(bytes);

    assertThat(deserialized.errableNote()).isEqualTo(Errable.withValue("hello fory errable"));
  }

  @Test
  void errableField_absent_deserializesToNil() throws Exception {
    ForyResponse_ImmutFory original =
        ForyResponse_ImmutFory._builder().message("no-errable").mandatoryInt(2)._build();

    assertThat(original.errableNote()).isEqualTo(Errable.nil());

    byte[] bytes = original._serialize().readAllBytes();
    ForyResponse_ImmutFory deserialized = new ForyResponse_ImmutFory(bytes);

    assertThat(deserialized.errableNote()).isEqualTo(Errable.nil());
  }

  @Test
  void errableField_setViaErrableSetter_withValue() {
    ForyResponse_ImmutFory built =
        ForyResponse_ImmutFory._builder()
            .message("errable-setter")
            .mandatoryInt(3)
            .errableNote(Errable.withValue("via errable setter"))
            ._build();

    assertThat(built.errableNote()).isEqualTo(Errable.withValue("via errable setter"));
  }

  @Test
  void errableField_setViaErrableSetter_nil() {
    ForyResponse_ImmutFory built =
        ForyResponse_ImmutFory._builder()
            .message("errable-nil")
            .mandatoryInt(4)
            .errableNote(Errable.nil())
            ._build();

    assertThat(built.errableNote()).isEqualTo(Errable.nil());
  }

  @Test
  void errableField_setViaErrableSetter_failure() {
    RuntimeException cause = new RuntimeException("upstream failure");
    ForyResponse_ImmutFory built =
        ForyResponse_ImmutFory._builder()
            .message("errable-failure")
            .mandatoryInt(5)
            .errableNote(Errable.withError(cause))
            ._build();

    assertThat(built.errableNote()).isNotInstanceOf(NonNil.class);
  }

  @Test
  void errableField_pojo_withValue_roundTrip() {
    ForyResponse_ImmutPojo built =
        ForyResponse_ImmutPojo._builder()
            .message("pojo-errable")
            .mandatoryInt(6)
            .errableNote(Errable.withValue("pojo value"))
            ._build();

    assertThat(built.errableNote()).isEqualTo(Errable.withValue("pojo value"));
    assertThat(built).isEqualTo(built._newCopy()._build());
  }

  @Test
  void errableField_pojo_nil_roundTrip() {
    ForyResponse_ImmutPojo built =
        ForyResponse_ImmutPojo._builder().message("pojo-nil").mandatoryInt(7)._build();

    assertThat(built.errableNote()).isEqualTo(Errable.nil());
    assertThat(built).isEqualTo(built._newCopy()._build());
  }

  @Test
  void errableModelField_withValue_serdeAndNewCopy() throws Exception {
    ForyResponse_ImmutFory original =
        ForyResponse_ImmutFory._builder()
            .message("errable-model")
            .mandatoryInt(8)
            .errableInnerData(
                Errable.withValue(
                    ForyInnerData_ImmutFory._builder().value("inner").count(1)._build()))
            ._build();

    assertThat(original.errableInnerData().value().value()).isEqualTo("inner");
    assertThat(original._newCopy()._build().errableInnerData())
        .isEqualTo(original.errableInnerData());

    byte[] bytes = original._serialize().readAllBytes();
    ForyResponse_ImmutFory deserialized = new ForyResponse_ImmutFory(bytes);
    assertThat(deserialized.errableInnerData().value().value()).isEqualTo("inner");
    assertThat(deserialized.errableInnerData().value().count()).isEqualTo(1);
  }

  @Test
  void errableModelField_nil_serdeAndNewCopy() throws Exception {
    ForyResponse_ImmutFory original =
        ForyResponse_ImmutFory._builder().message("errable-model-nil").mandatoryInt(9)._build();

    assertThat(original.errableInnerData()).isEqualTo(Errable.nil());
    assertThat(original._newCopy()._build().errableInnerData()).isEqualTo(Errable.nil());

    byte[] bytes = original._serialize().readAllBytes();
    ForyResponse_ImmutFory deserialized = new ForyResponse_ImmutFory(bytes);
    assertThat(deserialized.errableInnerData()).isEqualTo(Errable.nil());
  }

  @Test
  void errableListOfErrableModel_withValues_serdeAndNewCopy() throws Exception {
    ForyResponse_ImmutFory original =
        ForyResponse_ImmutFory._builder()
            .message("errable-list")
            .mandatoryInt(10)
            .nestedDataErrableList(
                Errable.withValue(
                    List.of(
                        Errable.withValue(
                            ForyInnerData_ImmutFory._builder().value("A").count(1)._build()),
                        Errable.nil())))
            ._build();

    assertThat(original.nestedDataErrableList().value()).hasSize(2);
    assertThat(original.nestedDataErrableList().value().get(0).value().value()).isEqualTo("A");
    assertThat(original.nestedDataErrableList().value().get(1)).isEqualTo(Errable.nil());
    assertThat(original._newCopy()._build().nestedDataErrableList())
        .isEqualTo(original.nestedDataErrableList());

    byte[] bytes = original._serialize().readAllBytes();
    ForyResponse_ImmutFory deserialized = new ForyResponse_ImmutFory(bytes);
    assertThat(deserialized.nestedDataErrableList().value().get(0).value().count()).isEqualTo(1);
  }

  @Test
  void errablePojoModelField_withValue_newCopyRoundTrip() {
    ForyResponse_ImmutPojo built =
        ForyResponse_ImmutPojo._builder()
            .message("errable-model-pojo")
            .mandatoryInt(11)
            .errableInnerData(
                Errable.withValue(
                    ForyInnerData_ImmutPojo._builder().value("inner").count(2)._build()))
            ._build();

    assertThat(built).isEqualTo(built._newCopy()._build());
    assertThat(built.errableInnerData().value().count()).isEqualTo(2);
  }
}
