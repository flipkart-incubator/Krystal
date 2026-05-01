package com.flipkart.krystal.vajram.protobuf2024e.codegen;

import lombok.experimental.UtilityClass;

/**
 * Edition-2024-specific suffixes used by Krystal codegen and any downstream codegen modules that
 * consume edition-2024 outputs.
 *
 * <p>Proto message and enum suffixes use no leading underscore so they form valid TitleCase
 * identifiers when concatenated with a (de-underscored) model name - e.g. {@code FooProto} and
 * {@code FooReqProto}. Editions 2024+ enforce TitleCase strictly. Java class names that retain
 * underscores (e.g. the {@code _Immut} wrapper interfaces, {@code java_outer_classname} values) are
 * unaffected by this convention.
 */
@UtilityClass
public final class Proto2024eConstants {

  public static final String VAJRAM_REQ_PROTO_FILE_SUFFIX = "_Req.models.proto";

  public static final String VAJRAM_REQ_PROTO_OUTER_CLASS_SUFFIX = "_Req_ModelsProto";
  public static final String VAJRAM_REQ_PROTO_MSG_SUFFIX = "ReqProto";

  public static final String MODELS_PROTO_FILE_SUFFIX = ".models.proto";
  public static final String MODELS_PROTO_OUTER_CLASS_SUFFIX = "_ModelsProto";
  public static final String MODELS_PROTO_MSG_SUFFIX = "Proto";
  public static final String MODELS_PROTO_UTILS_SUFFIX = "_ProtoUtils";
}
