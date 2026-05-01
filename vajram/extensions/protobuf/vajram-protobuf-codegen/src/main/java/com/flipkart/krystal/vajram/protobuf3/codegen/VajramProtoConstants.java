package com.flipkart.krystal.vajram.protobuf3.codegen;

/**
 * Proto3-specific suffixes used by Krystal codegen and any downstream codegen modules (e.g. the
 * lattice gRPC service generator) that consume proto3 outputs.
 *
 * <p>The "_Proto3" suffix matches {@code Protobuf3.modelClassesSuffix()} so that the generated
 * proto schema's message name (e.g. {@code Foo_Proto3}) lines up with the generated framework
 * wrapper class (e.g. {@code Foo_ImmutProto3}).
 */
public final class VajramProtoConstants {

  public static final String VAJRAM_REQ_PROTO_FILE_SUFFIX = "_Req.models.proto3.proto";

  public static final String VAJRAM_REQ_PROTO_OUTER_CLASS_SUFFIX = "_Req_ModelsProto3";
  public static final String VAJRAM_REQ_PROTO_MSG_SUFFIX = "_Req_Proto3";

  public static final String MODELS_PROTO_FILE_SUFFIX = ".models.proto3.proto";
  public static final String MODELS_PROTO_OUTER_CLASS_SUFFIX = "_ModelsProto3";
  public static final String MODELS_PROTO_MSG_SUFFIX = "_Proto3";
  public static final String MODELS_PROTO_UTILS_SUFFIX = "_Proto3Utils";

  private VajramProtoConstants() {}
}
