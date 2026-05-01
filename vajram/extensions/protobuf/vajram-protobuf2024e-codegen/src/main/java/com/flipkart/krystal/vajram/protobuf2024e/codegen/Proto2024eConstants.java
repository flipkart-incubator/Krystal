package com.flipkart.krystal.vajram.protobuf2024e.codegen;

/**
 * Edition-2024-specific suffixes used by Krystal codegen and any downstream codegen modules that
 * consume edition-2024 outputs.
 *
 * <p>The "_Proto" suffix matches {@code Protobuf2024e.modelClassesSuffix()} so that the generated
 * proto schema's message name (e.g. {@code Foo_Proto}) lines up with the generated framework
 * wrapper class (e.g. {@code Foo_ImmutProto}).
 */
public final class Proto2024eConstants {

  public static final String VAJRAM_REQ_PROTO_FILE_SUFFIX = "_Req.models.proto";

  public static final String VAJRAM_REQ_PROTO_OUTER_CLASS_SUFFIX = "_Req_ModelsProto";
  public static final String VAJRAM_REQ_PROTO_MSG_SUFFIX = "_Req_Proto";

  public static final String MODELS_PROTO_FILE_SUFFIX = ".models.proto";
  public static final String MODELS_PROTO_OUTER_CLASS_SUFFIX = "_ModelsProto";
  public static final String MODELS_PROTO_MSG_SUFFIX = "_Proto";
  public static final String MODELS_PROTO_UTILS_SUFFIX = "_ProtoUtils";

  private Proto2024eConstants() {}
}
