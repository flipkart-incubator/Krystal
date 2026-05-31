package com.flipkart.krystal.codegen.common.models;

import static com.flipkart.krystal.model.PlainJavaObject.POJO;

import com.squareup.javapoet.CodeBlock;

public final class Constants {
  public static final String CODEGEN_PHASE_KEY = "krystal.codegen.phase";
  public static final String MODULE_ROOT_PATH_KEY = "krystal.codegen.moduleRootPath";
  public static final String LOG_LEVEL = "krystal.codegen.logLevel";

  public static final String IMMUT_SUFFIX = "Immut";
  public static final String IMMUT_POJO_SUFFIX = "_" + IMMUT_SUFFIX + POJO.modelClassesSuffix();

  public static final CodeBlock EMPTY_CODE_BLOCK = CodeBlock.builder().build();
  public static final String SHARED_MODELS_SUB_PACKAGE = "shared_models";

  private Constants() {}
}
