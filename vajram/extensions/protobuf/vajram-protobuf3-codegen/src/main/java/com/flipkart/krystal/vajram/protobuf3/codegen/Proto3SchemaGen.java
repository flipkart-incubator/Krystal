package com.flipkart.krystal.vajram.protobuf3.codegen;

import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.vajram.protobuf.codegen.util.BaseProtoSchemaGen;

/** Proto3 specialisation of {@link BaseProtoSchemaGen}. */
final class Proto3SchemaGen extends BaseProtoSchemaGen {

  Proto3SchemaGen(ModelsCodeGenContext codeGenContext) {
    super(codeGenContext, Proto3SchemaConfig.INSTANCE);
  }
}
