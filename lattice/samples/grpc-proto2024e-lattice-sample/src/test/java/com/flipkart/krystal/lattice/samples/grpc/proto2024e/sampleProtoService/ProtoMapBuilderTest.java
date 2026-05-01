package com.flipkart.krystal.lattice.samples.grpc.proto2024e.sampleProtoService;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.model.ImmutableModel.Builder;
import com.flipkart.krystal.model.map.ModelsMapBuilder;
import com.flipkart.krystal.model.map.UnmodifiableModelsMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProtoMapBuilderTest {

  @Test
  void protoBuilderMapMutation_putModel_success() {
    Proto2024eLatticeSampleResponse_ImmutProto.Builder builder =
        Proto2024eLatticeSampleResponse_ImmutProto._builder().string("test").mandatoryInt(1);

    ModelsMapBuilder<String, SubMessage, SubMessage_Immut, Builder> mapBuilder =
        builder.namedSubMessages().modelsBuilder();

    SubMessage_Immut sub1 = SubMessage_ImmutProto._builder().count(10)._build();
    mapBuilder.putModel("a", sub1);

    // Verify mutation is reflected in the builder's getter (live view)
    assertThat(builder.namedSubMessages()).hasSize(1);
    assertThat(builder.namedSubMessages().get("a").count()).isEqualTo(10);

    // Add another entry
    SubMessage_Immut sub2 = SubMessage_ImmutProto._builder().count(20)._build();
    mapBuilder.putModel("b", sub2);

    assertThat(builder.namedSubMessages()).hasSize(2);
    assertThat(builder.namedSubMessages().get("b").count()).isEqualTo(20);

    // Build and verify
    Proto2024eLatticeSampleResponse_Immut built = builder._build();
    assertThat(built.namedSubMessages()).hasSize(2);
    assertThat(built.namedSubMessages().get("a").count()).isEqualTo(10);
    assertThat(built.namedSubMessages().get("b").count()).isEqualTo(20);
  }

  @Test
  void protoBuilderMapMutation_putBuilder_success() {
    Proto2024eLatticeSampleResponse_ImmutProto.Builder builder =
        Proto2024eLatticeSampleResponse_ImmutProto._builder().string("test").mandatoryInt(1);

    ModelsMapBuilder<String, SubMessage, SubMessage_Immut, Builder> mapBuilder =
        builder.namedSubMessages().modelsBuilder();

    SubMessage_ImmutProto.Builder subBuilder = SubMessage_ImmutProto._builder().count(42);
    mapBuilder.putBuilder("key", subBuilder);

    Proto2024eLatticeSampleResponse_Immut built = builder._build();
    assertThat(built.namedSubMessages()).hasSize(1);
    assertThat(built.namedSubMessages().get("key").count()).isEqualTo(42);
  }

  @Test
  void protoBuilderMapMutation_remove_success() {
    Proto2024eLatticeSampleResponse_ImmutProto.Builder builder =
        Proto2024eLatticeSampleResponse_ImmutProto._builder()
            .string("test")
            .mandatoryInt(1)
            .namedSubMessages(
                Map.of(
                    "a", SubMessage_ImmutProto._builder().count(1)._build(),
                    "b", SubMessage_ImmutProto._builder().count(2)._build()));

    ModelsMapBuilder<String, SubMessage, SubMessage_Immut, Builder> mapBuilder =
        builder.namedSubMessages().modelsBuilder();

    assertThat(mapBuilder.size()).isEqualTo(2);

    mapBuilder.remove("a");

    assertThat(builder.namedSubMessages()).hasSize(1);
    assertThat(builder.namedSubMessages()).doesNotContainKey("a");
    assertThat(builder.namedSubMessages().get("b").count()).isEqualTo(2);
  }

  @Test
  void protoBuilderMapMutation_clear_success() {
    Proto2024eLatticeSampleResponse_ImmutProto.Builder builder =
        Proto2024eLatticeSampleResponse_ImmutProto._builder()
            .string("test")
            .mandatoryInt(1)
            .namedSubMessages(Map.of("x", SubMessage_ImmutProto._builder().count(5)._build()));

    ModelsMapBuilder<String, SubMessage, SubMessage_Immut, Builder> mapBuilder =
        builder.namedSubMessages().modelsBuilder();

    assertThat(mapBuilder.isEmpty()).isFalse();

    mapBuilder.clear();

    // Same mapBuilder reference reflects the clear (live view)
    assertThat(mapBuilder.isEmpty()).isTrue();
    assertThat(builder.namedSubMessages()).isEmpty();

    // Verify we can add again after clearing via the same mapBuilder
    mapBuilder.putModel("new", SubMessage_ImmutProto._builder().count(42)._build());
    assertThat(mapBuilder.size()).isEqualTo(1);
    assertThat(builder.namedSubMessages()).hasSize(1);
    assertThat(builder.namedSubMessages().get("new").count()).isEqualTo(42);
  }

  @Test
  void protoBuilderMapMutation_putAllModels_success() {
    Proto2024eLatticeSampleResponse_ImmutProto.Builder builder =
        Proto2024eLatticeSampleResponse_ImmutProto._builder().string("test").mandatoryInt(1);

    ModelsMapBuilder<String, SubMessage, SubMessage_Immut, Builder> mapBuilder =
        builder.namedSubMessages().modelsBuilder();

    SubMessage_Immut sub1 = SubMessage_ImmutProto._builder().count(1)._build();
    SubMessage_Immut sub2 = SubMessage_ImmutProto._builder().count(2)._build();
    mapBuilder.putAllModels(Map.of("one", sub1, "two", sub2));

    assertThat(builder.namedSubMessages()).hasSize(2);

    Proto2024eLatticeSampleResponse_Immut built = builder._build();
    assertThat(built.namedSubMessages().get("one").count()).isEqualTo(1);
    assertThat(built.namedSubMessages().get("two").count()).isEqualTo(2);
  }

  @Test
  void protoBuilderMapMutation_liveView_reflectsMutations() {
    Proto2024eLatticeSampleResponse_ImmutProto.Builder builder =
        Proto2024eLatticeSampleResponse_ImmutProto._builder().string("test").mandatoryInt(1);

    // Get the map view — should be empty initially
    UnmodifiableModelsMap<String, SubMessage, SubMessage_Immut> mapView =
        builder.namedSubMessages();
    assertThat(mapView).isEmpty();

    // Mutate via modelsBuilder
    ModelsMapBuilder<String, SubMessage, SubMessage_Immut, Builder> mapBuilder =
        mapView.modelsBuilder();
    mapBuilder.putModel("live", SubMessage_ImmutProto._builder().count(99)._build());

    // Re-read from builder — should reflect the mutation
    assertThat(builder.namedSubMessages()).hasSize(1);
    assertThat(builder.namedSubMessages().get("live").count()).isEqualTo(99);
  }

  @Test
  void protoBuilderMapMutation_getModel_returnsModel() {
    Proto2024eLatticeSampleResponse_ImmutProto.Builder builder =
        Proto2024eLatticeSampleResponse_ImmutProto._builder()
            .string("test")
            .mandatoryInt(1)
            .namedSubMessages(Map.of("k", SubMessage_ImmutProto._builder().count(55)._build()));

    ModelsMapBuilder<String, SubMessage, SubMessage_Immut, Builder> mapBuilder =
        builder.namedSubMessages().modelsBuilder();

    SubMessage model = mapBuilder.getModel("k");
    assertThat(model).isNotNull();
    assertThat(model.count()).isEqualTo(55);
  }

  @Test
  void protoBuilderMapMutation_multipleEntries_thenBuild() {
    Proto2024eLatticeSampleResponse_ImmutProto.Builder builder =
        Proto2024eLatticeSampleResponse_ImmutProto._builder().string("test").mandatoryInt(1);

    ModelsMapBuilder<String, SubMessage, SubMessage_Immut, Builder> mapBuilder =
        builder.namedSubMessages().modelsBuilder();

    // Add multiple entries via builder
    mapBuilder.putModel("first", SubMessage_ImmutProto._builder().count(10)._build());
    mapBuilder.putModel("second", SubMessage_ImmutProto._builder().count(20)._build());
    mapBuilder.putModel("third", SubMessage_ImmutProto._builder().count(30)._build());

    Proto2024eLatticeSampleResponse_Immut built = builder._build();
    assertThat(built.namedSubMessages()).hasSize(3);
    assertThat(built.namedSubMessages().get("first").count()).isEqualTo(10);
    assertThat(built.namedSubMessages().get("second").count()).isEqualTo(20);
    assertThat(built.namedSubMessages().get("third").count()).isEqualTo(30);
  }
}
