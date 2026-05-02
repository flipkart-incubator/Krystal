package com.flipkart.krystal.vajram.protobuf.codegen.util;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.vajram.protobuf.codegen.util.types.OptionalFieldType;
import com.flipkart.krystal.vajram.protobuf.codegen.util.types.ProtoFieldType;
import com.flipkart.krystal.vajram.protobuf.util.SerializableProtoModel;
import javax.lang.model.element.Element;

/**
 * Per-protocol configuration that the shared schema/models generators consult to emit protocol-
 * specific text.
 *
 * @param protocolClass the {@link SerdeProtocol} class this config corresponds to (used for
 *     filtering in the codegen pipeline).
 * @param protocolInstance the canonical singleton instance of the protocol.
 * @param schemaHeader the leading directive line in generated {@code .proto} files, e.g. {@code
 *     "syntax = \"proto3\";"} or {@code "edition = \"2024\";"}.
 * @param fileSuffix the suffix used for generated {@code .proto} files (e.g. {@code
 *     ".models.proto3.proto"} for proto3 or {@code ".models.proto"} for edition 2024).
 * @param outerClassSuffix the value placed in {@code option java_outer_classname} (e.g. {@code
 *     "_ModelsProto3"} or {@code "_ModelsProto"}).
 * @param messageSuffix the suffix appended to the model-root name to form the proto message name
 *     (e.g. {@code "_Proto3"} or {@code "_Proto"}). Note: this matches the framework wrapper class
 *     suffix returned by {@link SerdeProtocol#modelClassesSuffix()} for round-trip consistency
 *     between the generated {@code .proto} schema and the generated Krystal wrapper class.
 * @param utilsSuffix the suffix appended to enum model names to form the generated enum-utils class
 *     (e.g. {@code "_Proto3Utils"} or {@code "_ProtoUtils"}).
 * @param serializableProtoSubInterface the protocol-specific sub-interface of {@link
 *     SerializableProtoModel} that generated wrapper classes should declare (e.g. {@code
 *     SerializableProto3Model.class} or {@code SerializableProto2024eModel.class}). The default
 *     {@code _serdeProtocol()} on this sub-interface returns this protocol's instance.
 * @param emitJavaMultipleFiles whether to emit {@code option java_multiple_files = true;} in
 *     generated {@code .proto} files. True for proto3 (where the default is single-file mode);
 *     false for editions 2024+ (which reject the option since the default is already multi-file).
 */
public record ProtoSchemaConfig(
    Class<? extends ModelProtocol> protocolClass,
    SerdeProtocol protocolInstance,
    String schemaHeader,
    String fileSuffix,
    String outerClassSuffix,
    String messageSuffix,
    String utilsSuffix,
    Class<? extends SerializableProtoModel> serializableProtoSubInterface,
    PresenceWrapper presenceWrapper,
    boolean emitJavaMultipleFiles) {

  /**
   * Wraps a {@link ProtoFieldType} so it is rendered with the protocol-appropriate explicit-
   * presence marking. In proto3 this wraps the field in {@link OptionalFieldType} (which prints the
   * {@code optional} keyword); in editions (2024+) singular fields already have explicit presence
   * by default so the wrapper is the identity function.
   */
  @FunctionalInterface
  public interface PresenceWrapper {
    ProtoFieldType wrap(ProtoFieldType type, CodeGenUtility util, Element element);
  }
}
