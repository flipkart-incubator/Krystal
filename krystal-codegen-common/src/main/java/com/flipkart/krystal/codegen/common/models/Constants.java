package com.flipkart.krystal.codegen.common.models;

import com.squareup.javapoet.CodeBlock;

public final class Constants {
  public static final String CODEGEN_PHASE_KEY = "krystal.codegen.phase";
  public static final String LOG_LEVEL = "krystal.codegen.logLevel";

  public static final String IMMUT_SUFFIX = "Immut";

  public static final CodeBlock EMPTY_CODE_BLOCK = CodeBlock.builder().build();

  private Constants() {}
}
