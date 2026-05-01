package com.flipkart.krystal.vajram.protobuf2024e.codegen;

import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.vajram.protobuf.codegen.util.BaseProtoModelsGen;

/**
 * Edition-2024 specialisation of {@link BaseProtoModelsGen}. Generates a {@code Foo_ImmutProto}
 * class for every edition-2024 model root: it implements the {@code Foo_Immut} interface and {@link
 * SerializableModel}, wrapping the proto-generated {@code Foo_Proto} message.
 */
public final class Proto2024eModelsGen extends BaseProtoModelsGen {

  public Proto2024eModelsGen(ModelsCodeGenContext codeGenContext) {
    super(codeGenContext, Proto2024eSchemaConfig.INSTANCE);
  }
}
