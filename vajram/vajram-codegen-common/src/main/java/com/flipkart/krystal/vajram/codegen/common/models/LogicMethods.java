package com.flipkart.krystal.vajram.codegen.common.models;

import java.util.List;
import javax.lang.model.element.ExecutableElement;

public record LogicMethods(OutputLogics outputLogics, List<ExecutableElement> resolvers) {
  public sealed interface OutputLogics {
    record NoBatching(ExecutableElement output) implements OutputLogics {}

    record WithBatching(ExecutableElement batchedOutput, ExecutableElement unbatchOutput)
        implements OutputLogics {}
  }
}
