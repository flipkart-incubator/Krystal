package com.flipkart.krystal.lattice.samples.grpc.proto.sampleProtoService;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Proto3LatticeSampleResponseTest {

  @Test
  void allFields_serializeAndDeserialize_roundTrip() throws Exception {
    // Build SubMessages for map and list fields
    SubMessage_Immut sub1 = SubMessage_ImmutProto._builder().count(10)._build();
    SubMessage_Immut sub2 = SubMessage_ImmutProto._builder().count(20)._build();

    // Build ProtoMessages for list and nested fields
    ProtoMessage_Immut protoMsg = ProtoMessage_ImmutProto._builder().count(42)._build();
    ProtoMessage_Immut protoMsg2 = ProtoMessage_ImmutProto._builder().count(99)._build();

    // Build the full response with all fields populated
    Proto3LatticeSampleResponse_ImmutProto original =
        Proto3LatticeSampleResponse_ImmutProto._builder()
            .string("hello")
            .optionalInteger(7)
            .nullableIntegerMayFailConditionally(3)
            .nullableInteger(5)
            .optionalIntArray(List.of(1, 2, 3))
            .mandatoryInt(100)
            .defaultInt(200)
            .mandatoryStringPartialConstruction("partial")
            .mapTypedField(Map.of("a", "alpha", "b", "beta"))
            .protoMessage(protoMsg)
            .protoMessages(List.of(protoMsg, protoMsg2))
            .subMessage(sub1)
            .subMessages(List.of(sub1, sub2))
            .namedSubMessages(Map.of("first", sub1, "second", sub2))
            .status(Status.COMPLETED)
            .optionalStatus(Status.IN_PROGRESS)
            .statuses(List.of(Status.PENDING, Status.FAILED))
            ._build();

    // Serialize to bytes
    byte[] serialized = original._serialize();
    assertThat(serialized).isNotEmpty();

    // Deserialize from bytes
    Proto3LatticeSampleResponse_ImmutProto deserialized =
        new Proto3LatticeSampleResponse_ImmutProto(serialized);

    // Verify all scalar and string fields
    assertThat(deserialized.string()).isEqualTo("hello");
    assertThat(deserialized.optionalInteger()).hasValue(7);
    assertThat(deserialized.nullableIntegerMayFailConditionally()).isEqualTo(3);
    assertThat(deserialized.nullableInteger()).isEqualTo(5);
    assertThat(deserialized.mandatoryInt()).isEqualTo(100);
    assertThat(deserialized.defaultInt()).isEqualTo(200);
    assertThat(deserialized.mandatoryStringPartialConstruction()).isEqualTo("partial");

    // Verify list of primitives
    assertThat(deserialized.optionalIntArray()).containsExactly(1, 2, 3);

    // Verify map of scalars
    assertThat(deserialized.mapTypedField()).containsEntry("a", "alpha").containsEntry("b", "beta");

    // Verify nested model (ProtoMessage)
    assertThat(deserialized.protoMessage()).isNotNull();
    assertThat(requireNonNull(deserialized.protoMessage()).count()).isEqualTo(42);

    // Verify list of models (ProtoMessage)
    assertThat(deserialized.protoMessages()).hasSize(2);
    assertThat(deserialized.protoMessages().get(0).count()).isEqualTo(42);
    assertThat(deserialized.protoMessages().get(1).count()).isEqualTo(99);

    // Verify nested model (SubMessage)
    assertThat(deserialized.subMessage()).isNotNull();
    assertThat(requireNonNull(deserialized.subMessage()).count()).isEqualTo(10);

    // Verify list of models (SubMessage)
    assertThat(deserialized.subMessages()).hasSize(2);
    assertThat(deserialized.subMessages().get(0).count()).isEqualTo(10);
    assertThat(deserialized.subMessages().get(1).count()).isEqualTo(20);

    // Verify map of models (Map<String, SubMessage>)
    assertThat(deserialized.namedSubMessages()).hasSize(2);
    assertThat(deserialized.namedSubMessages().get("first").count()).isEqualTo(10);
    assertThat(deserialized.namedSubMessages().get("second").count()).isEqualTo(20);

    // Verify enum fields
    assertThat(deserialized.status()).isEqualTo(Status.COMPLETED);
    assertThat(deserialized.optionalStatus()).isEqualTo(Status.IN_PROGRESS);
    assertThat(deserialized.statuses()).containsExactly(Status.PENDING, Status.FAILED);

    // Verify equality between original and deserialized
    assertThat(deserialized).isEqualTo(original);
  }

  @Test
  void defaultFields_serializeAndDeserialize_roundTrip() throws Exception {
    // Build with only mandatory fields — others use defaults
    Proto3LatticeSampleResponse_ImmutProto original =
        Proto3LatticeSampleResponse_ImmutProto._builder()
            .string("minimal")
            .mandatoryInt(1)
            .mandatoryStringPartialConstruction("hello")
            .status(Status.UNKNOWN)
            ._build();

    byte[] serialized = original._serialize();
    Proto3LatticeSampleResponse_ImmutProto deserialized =
        new Proto3LatticeSampleResponse_ImmutProto(serialized);

    assertThat(deserialized.string()).isEqualTo("minimal");
    assertThat(deserialized.mandatoryInt()).isEqualTo(1);
    assertThat(deserialized.defaultInt()).isZero();
    assertThat(deserialized.optionalIntArray()).isEmpty();
    assertThat(deserialized.protoMessages()).isEmpty();
    assertThat(deserialized.subMessages()).isEmpty();
    assertThat(deserialized.namedSubMessages()).isEmpty();
    assertThat(deserialized.mapTypedField()).isEmpty();
    assertThat(deserialized.optionalInteger()).isEmpty();
    assertThat(deserialized.nullableInteger()).isNull();
    assertThat(deserialized.protoMessage()).isNull();
    assertThat(deserialized.subMessage()).isNull();

    assertThat(deserialized).isEqualTo(original);
  }

  @Test
  void mapWithModelValues_builderRoundTrip() {
    // Build via builder, then verify via _build()
    SubMessage_Immut sub1 = SubMessage_ImmutProto._builder().count(5)._build();
    SubMessage_Immut sub2 = SubMessage_ImmutProto._builder().count(15)._build();

    Proto3LatticeSampleResponse_Immut built =
        Proto3LatticeSampleResponse_ImmutProto._builder()
            .string("map-test")
            .mandatoryInt(1)
            .namedSubMessages(Map.of("x", sub1, "y", sub2))
            .status(Status.PENDING)
            ._build();

    assertThat(built.namedSubMessages()).hasSize(2);
    assertThat(built.namedSubMessages().get("x").count()).isEqualTo(5);
    assertThat(built.namedSubMessages().get("y").count()).isEqualTo(15);
  }

  @Test
  void mapWithModelValues_copyConstructor() {
    SubMessage_Immut sub = SubMessage_ImmutProto._builder().count(77)._build();

    Proto3LatticeSampleResponse_ImmutProto original =
        Proto3LatticeSampleResponse_ImmutProto._builder()
            .string("copy-test")
            .mandatoryInt(1)
            .mandatoryStringPartialConstruction("")
            .namedSubMessages(Map.of("key", sub))
            .status(Status.FAILED)
            ._build();

    // Copy via copy constructor (new ImmutProto from model interface)
    Proto3LatticeSampleResponse_ImmutProto copy =
        new Proto3LatticeSampleResponse_ImmutProto(original);

    assertThat(copy.string()).isEqualTo("copy-test");
    assertThat(copy.namedSubMessages()).hasSize(1);
    assertThat(copy.namedSubMessages().get("key").count()).isEqualTo(77);
    assertThat(copy).isEqualTo(original);
  }

  @Test
  void mapWithModelValues_overwriteInBuilder() {
    SubMessage_Immut sub1 = SubMessage_ImmutProto._builder().count(1)._build();
    SubMessage_Immut sub2 = SubMessage_ImmutProto._builder().count(2)._build();

    // First set, then overwrite with a different map, then build
    Proto3LatticeSampleResponse_Immut built =
        Proto3LatticeSampleResponse_ImmutProto._builder()
            .string("overwrite-test")
            .mandatoryInt(1)
            .namedSubMessages(Map.of("a", sub1))
            .namedSubMessages(Map.of("b", sub2))
            .status(Status.UNKNOWN)
            ._build();

    assertThat(built.namedSubMessages()).hasSize(1);
    assertThat(built.namedSubMessages().get("b").count()).isEqualTo(2);
    assertThat(built.namedSubMessages()).doesNotContainKey("a");
  }

  @Test
  void mapWithModelValues_nullClearsMap() {
    SubMessage_Immut sub = SubMessage_ImmutProto._builder().count(1)._build();

    // Set map, then clear with null, then build — should have empty map
    Proto3LatticeSampleResponse_Immut built =
        Proto3LatticeSampleResponse_ImmutProto._builder()
            .string("null-test")
            .mandatoryInt(1)
            .namedSubMessages(Map.of("a", sub))
            .namedSubMessages(null)
            .status(Status.UNKNOWN)
            ._build();

    assertThat(built.namedSubMessages()).isEmpty();
  }

  @Test
  void subMessageWithNestedProtoMessages_serializeAndDeserialize() throws Exception {
    ProtoMessage_Immut pm1 = ProtoMessage_ImmutProto._builder().count(11)._build();
    ProtoMessage_Immut pm2 = ProtoMessage_ImmutProto._builder().count(22)._build();

    SubMessage_Immut sub =
        SubMessage_ImmutProto._builder()
            .count(99)
            .protoMessage(pm1)
            .protoMessages(List.of(pm1, pm2))
            ._build();

    Proto3LatticeSampleResponse_ImmutProto original =
        Proto3LatticeSampleResponse_ImmutProto._builder()
            .string("nested-test")
            .mandatoryInt(1)
            .namedSubMessages(Map.of("nested", sub))
            .mandatoryStringPartialConstruction("hello")
            .subMessages(List.of(sub))
            .subMessage(sub)
            .status(Status.IN_PROGRESS)
            ._build();

    byte[] serialized = original._serialize();
    Proto3LatticeSampleResponse_ImmutProto deserialized =
        new Proto3LatticeSampleResponse_ImmutProto(serialized);

    // Verify deeply nested fields survive round-trip
    SubMessage deserializedSub = deserialized.namedSubMessages().get("nested");
    assertThat(deserializedSub.count()).isEqualTo(99);
    assertThat(deserializedSub.protoMessage()).isNotNull();
    assertThat(requireNonNull(deserializedSub.protoMessage()).count()).isEqualTo(11);
    assertThat(deserializedSub.protoMessages()).hasSize(2);
    assertThat(deserializedSub.protoMessages().get(0).count()).isEqualTo(11);
    assertThat(deserializedSub.protoMessages().get(1).count()).isEqualTo(22);

    assertThat(deserialized).isEqualTo(original);
  }
}
