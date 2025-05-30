package com.flipkart.krystal.vajram.ext.json.JsonModelGen;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.vajram.codegen.processor.JavaModelsGenerator;
import com.flipkart.krystal.vajram.ext.json.JsonModelGenProvider;
import com.flipkart.krystal.vajram.json.Json;

public final class ModelsJsonGen implements CodeGenerator {
  private final ModelsCodeGenContext codeGenContext;
  private final CodeGenUtility util;

  public ModelsJsonGen(ModelsCodeGenContext codeGenContext) {
    this.codeGenContext = codeGenContext;
    this.util = codeGenContext.util();
  }

  @Override
  public void generate() {

  }
}
