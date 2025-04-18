package com.flipkart.krystal.vajram.codegen.common.models;

/**
 * Code generation phases followed by the vajram-codegen plugin. The phases are executed in the same
 * order as defined in this enum
 */
public enum CodegenPhase {

  /**
   * In this phase java models and other classes which are accessible to devs when writing business
   * logic are generated.
   */
  MODELS,
  /**
   * In this phase those java classes are generated which are not used by devs directly to write
   * code. These classes are free to refer to any models generated in any phase. This is the stage
   * in which all the java files generated in all the phases are compiled.
   */
  FINAL
}
