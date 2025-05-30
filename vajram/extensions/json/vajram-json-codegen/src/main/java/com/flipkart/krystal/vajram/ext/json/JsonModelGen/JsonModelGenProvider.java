package com.flipkart.krystal.vajram.ext.json;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.vajram.ext.json.JsonModelGen.ModelsJsonGen;

public class JsonModelGenProvider implements ModelsCodeGeneratorProvider {

  @Override
  public CodeGenerator create(ModelsCodeGenContext codeGenContext) {
    return new ModelsJsonGen(codeGenContext);
  }
}
