package com.flipkart.krystal.vajram.codegen.processor;

import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGeneratorProvider;

class Constants {

  static final VajramCodeGeneratorProvider DEFAULT_VAJRAM_CODEGEN_PROVIDER =
      VajramCodeGenerator::new;

  private Constants() {}
}
