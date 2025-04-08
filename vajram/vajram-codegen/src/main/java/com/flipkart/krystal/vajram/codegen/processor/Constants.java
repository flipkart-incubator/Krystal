package com.flipkart.krystal.vajram.codegen.processor;

import com.flipkart.krystal.vajram.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGeneratorProvider;

class Constants {

  static final VajramCodeGeneratorProvider DEFAULT_VAJRAM_CODEGEN_PROVIDER =
      VajramCodeGenerator::new;
  static final ModelsCodeGeneratorProvider DEFAULT_MODELS_CODEGEN_PROVIDER =
      JavaModelsGenerator::new;

  private Constants() {}
}
