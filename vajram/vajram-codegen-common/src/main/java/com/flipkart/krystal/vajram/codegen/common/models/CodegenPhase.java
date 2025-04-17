package com.flipkart.krystal.vajram.codegen.common.models;

/**
 * Code generation phases followed by the vajram-codegen plugin. The phases are executed in the same
 * order as defined in this enum
 */
public enum CodegenPhase {
  /**
   * In this phase schemas in 3rd party IDLs are generated.
   *
   * <p>Examples: Protobuf, Thrift
   */
  SCHEMAS,
  /**
   * In this phase java models which are accessible to devs when writing business logic are
   * generated. These models might refer to the models generated from schemas which are generated in
   * the {@link #SCHEMAS} phase.
   */
  MODELS,
  /**
   * In this phase those java classes are generated which are not used by devs directly to write
   * code. These classes are free to refer to any models generated in any of the phases. This is the
   * stage in which all the java files generated in all the phases are compiled.
   */
  FINAL
}
