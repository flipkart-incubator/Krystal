package com.flipkart.krystal.vajram.codegen.common.spi;

import com.flipkart.krystal.vajram.codegen.common.models.VajramValidationException;

public interface VajramCodeGenerator {
  void generate() throws VajramValidationException;
}
