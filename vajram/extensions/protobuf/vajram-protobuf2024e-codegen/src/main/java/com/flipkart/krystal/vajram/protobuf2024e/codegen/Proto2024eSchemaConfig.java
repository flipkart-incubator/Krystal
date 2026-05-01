package com.flipkart.krystal.vajram.protobuf2024e.codegen;

import static com.flipkart.krystal.vajram.protobuf2024e.Protobuf2024e.PROTOBUF_2024_E;
import static com.flipkart.krystal.vajram.protobuf2024e.codegen.Proto2024eConstants.MODELS_PROTO_FILE_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf2024e.codegen.Proto2024eConstants.MODELS_PROTO_MSG_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf2024e.codegen.Proto2024eConstants.MODELS_PROTO_OUTER_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.protobuf2024e.codegen.Proto2024eConstants.MODELS_PROTO_UTILS_SUFFIX;

import com.flipkart.krystal.vajram.protobuf.codegen.util.ProtoSchemaConfig;
import com.flipkart.krystal.vajram.protobuf2024e.Protobuf2024e;
import com.flipkart.krystal.vajram.protobuf2024e.SerializableProto2024eModel;

/** Edition-2024 flavoured {@link ProtoSchemaConfig} singleton. */
final class Proto2024eSchemaConfig {

  static final ProtoSchemaConfig INSTANCE =
      new ProtoSchemaConfig(
          Protobuf2024e.class,
          PROTOBUF_2024_E,
          // Edition 2024 uses `edition = "2024";` instead of `syntax = "proto3";`. With edition
          // defaults, singular fields have explicit presence by default - no `optional` keyword
          // needed - and enums are open, repeated fields are packed, messages are length-prefixed,
          // UTF-8 is verified, and JSON format is allowed.
          "edition = \"2024\";",
          MODELS_PROTO_FILE_SUFFIX,
          MODELS_PROTO_OUTER_CLASS_SUFFIX,
          MODELS_PROTO_MSG_SUFFIX,
          MODELS_PROTO_UTILS_SUFFIX,
          SerializableProto2024eModel.class,
          // Singular fields in edition 2024 already have explicit presence - no need to wrap.
          (type, util, element) -> type);

  private Proto2024eSchemaConfig() {}
}
