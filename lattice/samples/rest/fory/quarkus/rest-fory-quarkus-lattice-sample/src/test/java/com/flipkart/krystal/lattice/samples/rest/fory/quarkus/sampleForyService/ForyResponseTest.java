package com.flipkart.krystal.lattice.samples.rest.fory.quarkus.sampleForyService;

import static org.assertj.core.api.Assertions.assertThat;

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
}
