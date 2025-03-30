package com.flipkart.krystal.vajram.codegen;

import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGeneratorProvider;

class Constants {

  static final VajramCodeGeneratorProvider DEFAULT_CODE_GENERATOR_PROVIDER =
      DefaultVajramCodeGenerator::new;

  private Constants() {}
}
