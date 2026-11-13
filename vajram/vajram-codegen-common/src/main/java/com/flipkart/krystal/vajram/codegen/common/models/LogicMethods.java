package com.flipkart.krystal.vajram.codegen.common.models;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import javax.lang.model.element.ExecutableElement;

/**
 * @param outputLogics output logic method(s)
 * @param resolvers dependency input resolvers
 * @param computeFieldMethods The {@code @ComputeField} methods by the name of the field they
 *     compute
 */
public record LogicMethods(
    OutputLogics outputLogics,
    List<ExecutableElement> resolvers,
    ImmutableMap<String, ExecutableElement> computeFieldMethods) {
  public sealed interface OutputLogics {
    record NoBatching(ExecutableElement output) implements OutputLogics {}

    record WithBatching(ExecutableElement batchedOutput, ExecutableElement unbatchOutput)
        implements OutputLogics {}
  }
}
