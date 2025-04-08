package com.flipkart.krystal.vajram.codegen.common.models;

/**
 * Code generation phases followed by the vajram-codegen plugin. The phases are executed in the same
 * order as defined in this enum
 */
public enum CodegenPhase {
  /** In this phase schemas in IDLs like protobuf etc. are generated. */
  SCHEMAS,
  /**
   * In this phase java models are generated. These models might refer to the models generated from
   * schemas which are generated in the {@link #SCHEMAS} phase.
   */
  MODELS,
  /** In this phase Vajram Wrapper classes are generated. */
  WRAPPERS
}
