package com.flipkart.krystal.vajram.protobuf3.codegen;

/**
 * Proto3-specific suffixes used by Krystal codegen and any downstream codegen modules (e.g. the
 * lattice gRPC service generator) that consume proto3 outputs.
 *
 * <p>Proto message and enum suffixes use no leading underscore so they form valid TitleCase
 * identifiers when concatenated with a (de-underscored) model name - e.g. {@code FooProto3} and
 * {@code FooReqProto3}. Java class names that retain underscores (e.g. the {@code _Immut} wrapper
 * interfaces, {@code java_outer_classname} values) are unaffected by this convention.
 */
public final class VajramProtoConstants {

  public static final String VAJRAM_REQ_PROTO_FILE_SUFFIX = "_Req.models.proto3.proto";

  public static final String VAJRAM_REQ_PROTO_OUTER_CLASS_SUFFIX = "_Req_ModelsProto3";
  public static final String VAJRAM_REQ_PROTO_MSG_SUFFIX = "ReqProto3";

  public static final String MODELS_PROTO_FILE_SUFFIX = ".models.proto3.proto";
  public static final String MODELS_PROTO_OUTER_CLASS_SUFFIX = "_ModelsProto3";
  public static final String MODELS_PROTO_MSG_SUFFIX = "Proto3";
  public static final String MODELS_PROTO_UTILS_SUFFIX = "_Proto3Utils";

  private VajramProtoConstants() {}
}
