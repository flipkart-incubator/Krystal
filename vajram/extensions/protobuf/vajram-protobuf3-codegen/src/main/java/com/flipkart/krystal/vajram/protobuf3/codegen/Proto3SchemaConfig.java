package com.flipkart.krystal.vajram.protobuf3.codegen;

import static com.flipkart.krystal.vajram.protobuf3.Protobuf3.PROTOBUF_3;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.MODELS_PROTO_FILE_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.MODELS_PROTO_MSG_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.MODELS_PROTO_OUTER_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.MODELS_PROTO_UTILS_SUFFIX;

import com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoSchemaConfig;
import com.flipkart.krystal.vajram.protobuf.codegen.util.types.OptionalFieldType;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;
import com.flipkart.krystal.vajram.protobuf3.SerializableProto3Model;

/** Proto3-flavoured {@link ProtoSchemaConfig} singleton. */
final class Proto3SchemaConfig {

  static final ProtoSchemaConfig INSTANCE =
      new ProtoSchemaConfig(
          Protobuf3.class,
          PROTOBUF_3,
          "syntax = \"proto3\";",
          MODELS_PROTO_FILE_SUFFIX,
          MODELS_PROTO_OUTER_CLASS_SUFFIX,
          MODELS_PROTO_MSG_SUFFIX,
          MODELS_PROTO_UTILS_SUFFIX,
          SerializableProto3Model.class,
          // proto3 needs an explicit `optional` keyword to opt singular fields into explicit
          // presence semantics. Skip the wrap if the field is already optional (e.g. the model
          // method returns Optional<T>, which getProtobufType already rendered as `optional T`).
          (type, util, element) ->
              type instanceof OptionalFieldType ? type : new OptionalFieldType(type, util, element),
          /* emitJavaMultipleFiles= */ true);

  private Proto3SchemaConfig() {}
}
