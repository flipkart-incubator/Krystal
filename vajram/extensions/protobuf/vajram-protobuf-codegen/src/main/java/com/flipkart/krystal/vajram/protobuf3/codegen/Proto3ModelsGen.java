package com.flipkart.krystal.vajram.protobuf3.codegen;

import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.vajram.protobuf.codegen.util.BaseProtoModelsGen;

/**
 * Proto3 specialisation of {@link BaseProtoModelsGen}. Generates a {@code Foo_ImmutProto3} class
 * for every proto3 model root: it implements the {@code Foo_Immut} interface and {@link
 * SerializableModel}, wrapping the proto-generated {@code Foo_Proto3} message.
 */
public final class Proto3ModelsGen extends BaseProtoModelsGen {

  public Proto3ModelsGen(ModelsCodeGenContext codeGenContext) {
    super(codeGenContext, Proto3SchemaConfig.INSTANCE);
  }
}
